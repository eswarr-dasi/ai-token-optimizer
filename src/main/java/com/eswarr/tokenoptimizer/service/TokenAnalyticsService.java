package com.eswarr.tokenoptimizer.service;

import com.eswarr.tokenoptimizer.model.TokenUsageRecord;
import com.eswarr.tokenoptimizer.repository.TokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.math.BigDecimal;

/**
 * Analytics service — aggregates token usage data for dashboards and reports.
 * Provides cost savings calculations, usage trends, and model breakdowns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenAnalyticsService {

    private final TokenUsageRepository usageRepository;

    /**
     * Generate a full cost savings report for a team over a date range.
     */
    public SavingsReport generateSavingsReport(String teamId, Instant from, Instant to) {
        List<TokenUsageRecord> records = usageRepository.findByTeamIdAndCreatedAtBetween(teamId, from, to);

        long totalRequests     = records.size();
        long cacheHits         = records.stream().filter(TokenUsageRecord::isCacheHit).count();
        long totalTokens       = records.stream().mapToLong(TokenUsageRecord::getTotalTokens).sum();
        long compressionSaved  = records.stream()
            .filter(TokenUsageRecord::isPromptCompressed)
            .mapToLong(r -> (long)(r.getPromptTokens() * 0.20)) // avg 20% saved
            .sum();

        double totalCost       = records.stream()
            .mapToDouble(r -> r.getEstimatedCostUsd().doubleValue())
            .sum();

        // Estimate what cost would have been without optimization
        double cacheHitRate    = totalRequests > 0 ? (double) cacheHits / totalRequests : 0;
        double savedByCache    = totalCost * cacheHitRate * 0.95; // 95% cost saved on cache hits
        double savedByRouting  = totalCost * 0.25; // estimate: avg 25% saved by routing to cheaper models
        double savedByCompression = compressionSaved * 0.000015; // avg cost per token

        double totalSaved      = savedByCache + savedByRouting + savedByCompression;
        double originalCost    = totalCost + totalSaved;

        return SavingsReport.builder()
            .teamId(teamId)
            .from(from)
            .to(to)
            .totalRequests(totalRequests)
            .cacheHits(cacheHits)
            .cacheHitRate(cacheHitRate)
            .totalTokensUsed(totalTokens)
            .totalCostUsd(totalCost)
            .estimatedOriginalCostUsd(originalCost)
            .totalSavedUsd(totalSaved)
            .savedByCacheUsd(savedByCache)
            .savedByRoutingUsd(savedByRouting)
            .savedByCompressionUsd(savedByCompression)
            .savingsPercent(originalCost > 0 ? totalSaved / originalCost * 100 : 0)
            .build();
    }

    /**
     * Get usage grouped by model for a team.
     */
    public List<ModelUsageBreakdown> getModelBreakdown(String teamId, Instant from, Instant to) {
        List<Object[]> raw = usageRepository.getUsageBreakdownByModel(teamId, from, to);
        return raw.stream().map(row -> new ModelUsageBreakdown(
            (String) row[0],
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue(),
            ((Number) row[3]).doubleValue()
        )).toList();
    }

    /**
     * Get top agents by token consumption for a team.
     */
    public List<AgentUsageSummary> getTopAgents(String teamId, int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> raw = usageRepository.getTopAgentsByUsage(teamId, from);
        return raw.stream().map(row -> new AgentUsageSummary(
            (String) row[0],
            ((Number) row[1]).longValue()
        )).toList();
    }

    // --- DTOs ---

    @lombok.Builder
    public record SavingsReport(
        String teamId, Instant from, Instant to,
        long totalRequests, long cacheHits, double cacheHitRate,
        long totalTokensUsed, double totalCostUsd,
        double estimatedOriginalCostUsd, double totalSavedUsd,
        double savedByCacheUsd, double savedByRoutingUsd, double savedByCompressionUsd,
        double savingsPercent
    ) {}

    public record ModelUsageBreakdown(String modelId, long requestCount, long totalTokens, double totalCostUsd) {}
    public record AgentUsageSummary(String agentId, long totalTokens) {}
}
