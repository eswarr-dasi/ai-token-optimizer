package com.eswarr.tokenoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * Normalized LLM response envelope returned by the gateway.
 * Enriched with cost metadata, cache status, and routing info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    /** The generated text content */
    private String content;

    /** Model that produced this response */
    private String modelUsed;

    /** Provider that served this response */
    private String provider;

    // Token accounting
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    /** Estimated cost in USD */
    private double estimatedCostUsd;

    /** Whether this response was served from semantic cache */
    private boolean cacheHit;

    /** Similarity score if cache hit (1.0 = exact match) */
    private double cacheSimilarity;

    /** Whether the prompt was compressed before sending */
    private boolean promptCompressed;

    /** Tokens saved by prompt compression */
    private int compressionSavedTokens;

    /** Model routing decision: FAST | BALANCED | POWERFUL */
    private String routingDecision;

    /** Gateway processing latency in milliseconds */
    private long gatewayLatencyMs;

    /** LLM provider latency in milliseconds (0 if cache hit) */
    private long providerLatencyMs;

    /** Finish reason: stop | length | content_filter */
    private String finishReason;

    private Instant timestamp;

    @Builder.Default
    private boolean success = true;

    private String errorMessage;

    /** Total savings metadata for analytics */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingsInfo {
        private double savedCostUsd;
        private int savedTokens;
        private long savedLatencyMs;
        private String savingStrategy; // CACHE | ROUTING | COMPRESSION | COMBINED
    }

    private SavingsInfo savings;
}
