package com.eswarr.tokenoptimizer.service;

import com.eswarr.tokenoptimizer.model.LlmRequest;
import com.eswarr.tokenoptimizer.model.ModelTier;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Routes each LLM request to the most cost-effective model tier.
 *
 * Strategy: CONFIDENCE_BASED (default)
 *   1. Estimate task complexity from prompt signals
 *   2. Assign confidence score to FAST tier
 *   3. If confidence >= threshold -> FAST (cheap)
 *   4. If confidence < threshold -> BALANCED or POWERFUL
 *
 * Saves 20-40% on token costs by avoiding expensive frontier models
 * for simple classification, formatting, and short-answer tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouterService {

    private final MeterRegistry meterRegistry;

    @Value("${token-optimizer.routing.confidence-threshold:0.85}")
    private double confidenceThreshold;

    @Value("${token-optimizer.routing.strategy:CONFIDENCE_BASED}")
    private String routingStrategy;

    /**
     * Determine the optimal ModelTier for this request.
     */
    public ModelTier route(LlmRequest request) {
        if (request.getModelOverride() != null) {
            log.debug("Model override specified, skipping routing");
            return ModelTier.BALANCED; // use override directly in proxy
        }

        ModelTier tier = switch (routingStrategy) {
            case "COST_FIRST"    -> ModelTier.FAST;
            case "QUALITY_FIRST" -> ModelTier.POWERFUL;
            default              -> confidenceBasedRoute(request);
        };

        meterRegistry.counter("routing.decisions", "tier", tier.name()).increment();
        log.debug("Routing decision: {} for agentId={}", tier, request.getAgentId());
        return tier;
    }

    /**
     * Score the prompt complexity and route accordingly.
     * Uses heuristic signals — production would train a classifier.
     */
    private ModelTier confidenceBasedRoute(LlmRequest request) {
        double fastConfidence = computeFastTierConfidence(request);

        if (fastConfidence >= confidenceThreshold) {
            return ModelTier.FAST;
        } else if (fastConfidence >= confidenceThreshold * 0.6) {
            return ModelTier.BALANCED;
        } else {
            return ModelTier.POWERFUL;
        }
    }

    /**
     * Heuristic confidence score for FAST tier (0.0 - 1.0).
     * Higher = more likely a cheap model can handle it.
     */
    double computeFastTierConfidence(LlmRequest request) {
        double score = 1.0;
        String prompt = request.getFullPromptText().toLowerCase();

        // Penalize for complexity signals
        int promptLen = prompt.length();
        if (promptLen > 4000)  score -= 0.30;
        else if (promptLen > 2000) score -= 0.15;

        // Complex reasoning keywords
        String[] complexKeywords = {"analyze", "compare", "debate", "synthesize",
            "critique", "architect", "design system", "step by step reasoning",
            "chain of thought", "complex", "nuanced", "multi-step"};
        for (String kw : complexKeywords) {
            if (prompt.contains(kw)) { score -= 0.12; }
        }

        // Simple task keywords (boost confidence for FAST)
        String[] simpleKeywords = {"summarize", "classify", "format", "translate",
            "fix typo", "rewrite", "yes or no", "true or false", "extract", "list"};
        for (String kw : simpleKeywords) {
            if (prompt.contains(kw)) { score += 0.08; }
        }

        // Code generation is medium complexity
        if (prompt.contains("write code") || prompt.contains("implement") || prompt.contains("function")) {
            score -= 0.20;
        }

        // HIGH priority hint -> use better model
        if ("HIGH".equals(request.getPriority())) score -= 0.25;

        return Math.max(0.0, Math.min(1.0, score));
    }
}
