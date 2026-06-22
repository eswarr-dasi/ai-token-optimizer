package com.eswarr.tokenoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Represents a cached LLM response stored in Redis.
 * Indexed by a semantic embedding vector for similarity search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntry implements Serializable {

    /** Redis key: "cache:{sha256_of_normalized_prompt}" */
    private String key;

    /** Original prompt text (normalized) */
    private String prompt;

    /** Embedding vector for semantic similarity lookup */
    private float[] embeddingVector;

    /** Serialized LLM response JSON */
    private String responseJson;

    /** Model that produced this response */
    private String modelId;

    /** Token counts */
    private int promptTokens;
    private int completionTokens;

    /** Number of times this cache entry has been served */
    private long hitCount;

    /** Similarity score of the nearest neighbor at creation time */
    private float similarityScore;

    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;

    /** Tags for cache invalidation by domain/context */
    private List<String> tags;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void recordHit() {
        this.hitCount++;
        this.lastAccessedAt = Instant.now();
    }

    /** Estimated cost saved by this cache hit (in USD) */
    public double estimateSavedCostUsd(double costPerToken) {
        return (promptTokens + completionTokens) * costPerToken;
    }
}
