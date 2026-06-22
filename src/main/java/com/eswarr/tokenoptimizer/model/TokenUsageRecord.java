package com.eswarr.tokenoptimizer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Audit record for every LLM API call processed by the gateway.
 * Stores token counts, cost, model used, and whether cache was hit.
 */
@Entity
@Table(name = "token_usage_records", indexes = {
    @Index(name = "idx_agent_id", columnList = "agentId"),
    @Index(name = "idx_team_id", columnList = "teamId"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String teamId;

    @Column(nullable = false)
    private String modelId;

    @Column(nullable = false)
    private String provider;  // OPENAI | ANTHROPIC | GEMINI

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    @Column(precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;

    private boolean cacheHit;
    private boolean promptCompressed;
    private String routingDecision;  // FAST | BALANCED | POWERFUL

    @Column(length = 64)
    private String requestHash;  // SHA-256 of normalized prompt for dedup

    private long latencyMs;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
