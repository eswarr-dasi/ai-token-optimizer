package com.eswarr.tokenoptimizer.service;

import com.eswarr.tokenoptimizer.exception.BudgetExceededException;
import com.eswarr.tokenoptimizer.model.BudgetConfig;
import com.eswarr.tokenoptimizer.repository.BudgetConfigRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces per-team token budgets in real-time.
 *
 * Before every LLM call:
 *  1. Load team budget config from cache (Redis) or DB
 *  2. Check if daily limit is exceeded
 *  3. If hardBlock=true and limit exceeded -> throw BudgetExceededException
 *  4. If usage >= alertThreshold -> fire webhook alert (async, non-blocking)
 *
 * After every LLM call:
 *  5. Increment usage counters atomically in DB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetEnforcerService {

    private final BudgetConfigRepository budgetConfigRepository;
    private final WebClient.Builder webClientBuilder;
    private final MeterRegistry meterRegistry;

    /**
     * Pre-flight check — call BEFORE forwarding request to LLM.
     * Throws BudgetExceededException if hard limit is active and exceeded.
     */
    public void checkBudget(String teamId) {
        Optional<BudgetConfig> configOpt = budgetConfigRepository.findByTeamIdAndActiveTrue(teamId);
        if (configOpt.isEmpty()) {
            log.debug("No budget config for teamId={}, allowing request", teamId);
            return;
        }

        BudgetConfig config = configOpt.get();

        if (config.isDailyLimitExceeded()) {
            meterRegistry.counter("budget.exceeded", "teamId", teamId).increment();
            log.warn("Budget exceeded for teamId={} usage={}/{}", teamId,
                config.getCurrentDailyUsage(), config.getDailyTokenLimit());
            throw new BudgetExceededException(teamId, config.getCurrentDailyUsage(), config.getDailyTokenLimit());
        }

        // Fire alert if nearing threshold (async — don't block the request)
        double usagePct = config.getDailyUsagePercent();
        if (usagePct >= config.getAlertThreshold().doubleValue()) {
            fireAlertAsync(config, usagePct).subscribe();
        }
    }

    /**
     * Post-call accounting — call AFTER receiving LLM response.
     */
    @Transactional
    public void recordUsage(String teamId, long tokensUsed) {
        budgetConfigRepository.incrementDailyUsage(teamId, tokensUsed);
        budgetConfigRepository.incrementMonthlyUsage(teamId, tokensUsed);
        meterRegistry.counter("budget.tokens.used", "teamId", teamId).increment(tokensUsed);
        log.debug("Recorded {} tokens for teamId={}", tokensUsed, teamId);
    }

    /**
     * Fire webhook alert asynchronously — non-blocking.
     */
    private Mono<Void> fireAlertAsync(BudgetConfig config, double usagePct) {
        if (config.getAlertWebhookUrl() == null) return Mono.empty();

        Map<String, Object> payload = Map.of(
            "teamId",       config.getTeamId(),
            "usagePercent", String.format("%.1f%%", usagePct * 100),
            "currentUsage", config.getCurrentDailyUsage(),
            "dailyLimit",   config.getDailyTokenLimit(),
            "message",      "Token budget alert: " + String.format("%.0f%%", usagePct * 100) + " of daily limit used"
        );

        return webClientBuilder.build()
            .post()
            .uri(config.getAlertWebhookUrl())
            .bodyValue(payload)
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess(r -> log.info("Alert sent for teamId={}", config.getTeamId()))
            .doOnError(e -> log.error("Alert webhook failed for teamId={}: {}", config.getTeamId(), e.getMessage()))
            .onErrorResume(e -> Mono.empty())
            .then();
    }

    public Optional<BudgetConfig> getBudgetStatus(String teamId) {
        return budgetConfigRepository.findByTeamIdAndActiveTrue(teamId);
    }
}
