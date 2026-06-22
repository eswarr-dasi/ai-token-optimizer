package com.eswarr.tokenoptimizer.scheduler;

import com.eswarr.tokenoptimizer.repository.BudgetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

/**
 * Scheduled jobs to reset token budget counters.
 * Daily reset at midnight UTC, monthly reset on the 1st.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetResetScheduler {

    private final BudgetConfigRepository budgetConfigRepository;

    /**
     * Reset daily usage counters for all active teams at midnight UTC.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void resetDailyBudgets() {
        log.info("Resetting daily token budgets for all teams");
        budgetConfigRepository.resetAllDailyUsage();
        log.info("Daily budget reset complete");
    }

    /**
     * Reset monthly usage counters on the 1st of each month at midnight UTC.
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    @Transactional
    public void resetMonthlyBudgets() {
        log.info("Resetting monthly token budgets — month: {}", LocalDate.now().getMonthValue());
        budgetConfigRepository.resetAllMonthlyUsage();
        log.info("Monthly budget reset complete");
    }

    /**
     * Check and alert on teams nearing their daily limit every 15 minutes.
     */
    @Scheduled(fixedRate = 900_000) // 15 minutes
    @Transactional(readOnly = true)
    public void checkBudgetAlerts() {
        var nearingLimit = budgetConfigRepository.findBudgetsNearingDailyLimit();
        if (!nearingLimit.isEmpty()) {
            log.warn("{} team(s) are nearing their daily token budget limit",
                nearingLimit.size());
            nearingLimit.forEach(config ->
                log.warn("  - teamId={} usage={}/{} ({:.0f}%)",
                    config.getTeamId(),
                    config.getCurrentDailyUsage(),
                    config.getDailyTokenLimit(),
                    config.getDailyUsagePercent() * 100));
        }
    }
}
