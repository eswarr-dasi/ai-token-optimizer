package com.eswarr.tokenoptimizer.repository;

import com.eswarr.tokenoptimizer.model.TokenUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface TokenUsageRepository extends JpaRepository<TokenUsageRecord, String> {

    List<TokenUsageRecord> findByAgentIdAndCreatedAtBetween(
        String agentId, Instant from, Instant to);

    List<TokenUsageRecord> findByTeamIdAndCreatedAtBetween(
        String teamId, Instant from, Instant to);

    @Query("SELECT SUM(r.totalTokens) FROM TokenUsageRecord r WHERE r.teamId = :teamId AND r.createdAt >= :from")
    Long sumTokensByTeamSince(@Param("teamId") String teamId, @Param("from") Instant from);

    @Query("SELECT SUM(r.totalTokens) FROM TokenUsageRecord r WHERE r.agentId = :agentId AND r.createdAt >= :from")
    Long sumTokensByAgentSince(@Param("agentId") String agentId, @Param("from") Instant from);

    @Query("SELECT r.modelId, COUNT(r), SUM(r.totalTokens), SUM(r.estimatedCostUsd) " +
           "FROM TokenUsageRecord r WHERE r.teamId = :teamId AND r.createdAt BETWEEN :from AND :to " +
           "GROUP BY r.modelId ORDER BY SUM(r.totalTokens) DESC")
    List<Object[]> getUsageBreakdownByModel(
        @Param("teamId") String teamId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(r) FROM TokenUsageRecord r WHERE r.teamId = :teamId AND r.cacheHit = true AND r.createdAt >= :from")
    Long countCacheHitsByTeamSince(@Param("teamId") String teamId, @Param("from") Instant from);

    @Query("SELECT SUM(r.estimatedCostUsd) FROM TokenUsageRecord r WHERE r.teamId = :teamId AND r.createdAt >= :from")
    Double sumCostByTeamSince(@Param("teamId") String teamId, @Param("from") Instant from);

    @Query("SELECT r.agentId, SUM(r.totalTokens) as tokens FROM TokenUsageRecord r " +
           "WHERE r.teamId = :teamId AND r.createdAt >= :from GROUP BY r.agentId ORDER BY tokens DESC")
    List<Object[]> getTopAgentsByUsage(@Param("teamId") String teamId, @Param("from") Instant from);
}
