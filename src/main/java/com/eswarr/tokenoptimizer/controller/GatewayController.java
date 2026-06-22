package com.eswarr.tokenoptimizer.controller;

import com.eswarr.tokenoptimizer.model.LlmRequest;
import com.eswarr.tokenoptimizer.model.LlmResponse;
import com.eswarr.tokenoptimizer.model.ModelTier;
import com.eswarr.tokenoptimizer.model.TokenUsageRecord;
import com.eswarr.tokenoptimizer.repository.TokenUsageRepository;
import com.eswarr.tokenoptimizer.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Main gateway endpoint — acts as a transparent proxy to LLM providers.
 *
 * All requests to POST /v1/chat/completions pass through this controller.
 * The pipeline is:
 *  1. Extract agent/team context from headers
 *  2. Check budget (BudgetEnforcerService)
 *  3. Compress system prompt (PromptCompressorService)
 *  4. Check semantic cache (SemanticCacheService)
 *  5a. Cache hit  -> return cached response immediately
 *  5b. Cache miss -> route to model tier, forward to LLM
 *  6. Store response in cache
 *  7. Record usage in DB + update budget counters
 *  8. Return enriched LlmResponse with savings metadata
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class GatewayController {

    private final SemanticCacheService cacheService;
    private final ModelRouterService   routerService;
    private final BudgetEnforcerService budgetService;
    private final PromptCompressorService compressorService;
    private final LlmProxyService      proxyService;
    private final TokenUsageRepository usageRepository;
    private final MeterRegistry        meterRegistry;

    @PostMapping("/chat/completions")
    public Mono<ResponseEntity<LlmResponse>> chatCompletions(
            @RequestBody @Valid LlmRequest request,
            @RequestHeader(value = "X-Agent-Id", defaultValue = "default-agent") String agentId,
            @RequestHeader(value = "X-Team-Id",  defaultValue = "default-team")  String teamId) {

        request.setAgentId(agentId);
        request.setTeamId(teamId);

        long gatewayStart = System.currentTimeMillis();

        // Step 1: Budget check (throws BudgetExceededException if hard limit hit)
        budgetService.checkBudget(teamId);

        // Step 2: Compress system prompt
        String originalSystem = request.getSystemPrompt();
        PromptCompressorService.CompressionResult compression =
            compressorService.compress(originalSystem);
        if (compression.wasCompressed()) {
            request.setSystemPrompt(compression.compressedText());
        }

        // Step 3: Check semantic cache
        String promptText = request.getFullPromptText();

        return cacheService.lookup(promptText)
            .flatMap(cachedOpt -> {
                if (cachedOpt.isPresent() && !request.isBypassCache()) {
                    // Cache HIT — return immediately
                    var entry = cachedOpt.get();
                    long gatewayMs = System.currentTimeMillis() - gatewayStart;

                    LlmResponse response = LlmResponse.builder()
                        .content(entry.getResponseJson())
                        .modelUsed(entry.getModelId())
                        .provider(request.getProvider())
                        .promptTokens(entry.getPromptTokens())
                        .completionTokens(entry.getCompletionTokens())
                        .totalTokens(entry.getPromptTokens() + entry.getCompletionTokens())
                        .cacheHit(true)
                        .cacheSimilarity(entry.getSimilarityScore())
                        .promptCompressed(compression.wasCompressed())
                        .compressionSavedTokens(compression.tokensSaved())
                        .routingDecision("CACHE")
                        .gatewayLatencyMs(gatewayMs)
                        .providerLatencyMs(0)
                        .timestamp(Instant.now())
                        .success(true)
                        .build();

                    recordUsage(request, response);
                    meterRegistry.counter("gateway.requests", "type", "cache_hit").increment();
                    return Mono.just(ResponseEntity.ok(response));
                }

                // Cache MISS — route to model and call LLM
                ModelTier tier = routerService.route(request);
                long providerStart = System.currentTimeMillis();

                return proxyService.forward(request, tier)
                    .map(llmResponse -> {
                        long gatewayMs  = System.currentTimeMillis() - gatewayStart;
                        long providerMs = System.currentTimeMillis() - providerStart;

                        llmResponse.setRoutingDecision(tier.name());
                        llmResponse.setPromptCompressed(compression.wasCompressed());
                        llmResponse.setCompressionSavedTokens(compression.tokensSaved());
                        llmResponse.setGatewayLatencyMs(gatewayMs);
                        llmResponse.setProviderLatencyMs(providerMs);
                        llmResponse.setCacheHit(false);

                        // Store in cache async (don't block response)
                        cacheService.store(promptText, llmResponse.getContent(),
                            llmResponse.getModelUsed(), llmResponse.getPromptTokens(),
                            llmResponse.getCompletionTokens()).subscribe();

                        recordUsage(request, llmResponse);
                        meterRegistry.counter("gateway.requests", "type", "llm_call").increment();
                        return ResponseEntity.ok(llmResponse);
                    });
            });
    }

    private void recordUsage(LlmRequest request, LlmResponse response) {
        ModelTier tier = ModelTier.FAST; // default for cost estimation
        try { tier = ModelTier.valueOf(response.getRoutingDecision()); } catch (Exception ignored) {}

        double cost = tier.estimateCost(response.getPromptTokens(), response.getCompletionTokens());

        TokenUsageRecord record = TokenUsageRecord.builder()
            .agentId(request.getAgentId())
            .teamId(request.getTeamId())
            .modelId(response.getModelUsed())
            .provider(response.getProvider() != null ? response.getProvider() : "OPENAI")
            .promptTokens(response.getPromptTokens())
            .completionTokens(response.getCompletionTokens())
            .totalTokens(response.getTotalTokens())
            .estimatedCostUsd(BigDecimal.valueOf(cost))
            .cacheHit(response.isCacheHit())
            .promptCompressed(response.isPromptCompressed())
            .routingDecision(response.getRoutingDecision())
            .latencyMs(response.getGatewayLatencyMs())
            .build();

        usageRepository.save(record);
        budgetService.recordUsage(request.getTeamId(), response.getTotalTokens());
    }
}
