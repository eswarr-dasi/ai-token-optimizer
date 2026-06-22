package com.eswarr.tokenoptimizer.controller;

import com.eswarr.tokenoptimizer.service.TokenAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Analytics REST API — provides usage stats, cost reports, and savings breakdowns.
 * Powers the Grafana dashboard and management dashboards.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TokenAnalyticsService analyticsService;

    /**
     * GET /api/analytics/savings/{teamId}?days=30
     * Returns a full cost savings report for the team over N days.
     */
    @GetMapping("/savings/{teamId}")
    public ResponseEntity<TokenAnalyticsService.SavingsReport> getSavingsReport(
            @PathVariable String teamId,
            @RequestParam(defaultValue = "30") int days) {

        Instant to   = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);

        TokenAnalyticsService.SavingsReport report = analyticsService.generateSavingsReport(teamId, from, to);
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/analytics/models/{teamId}?days=7
     * Returns token usage broken down by model for the team.
     */
    @GetMapping("/models/{teamId}")
    public ResponseEntity<List<TokenAnalyticsService.ModelUsageBreakdown>> getModelBreakdown(
            @PathVariable String teamId,
            @RequestParam(defaultValue = "7") int days) {

        Instant to   = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);

        List<TokenAnalyticsService.ModelUsageBreakdown> breakdown =
            analyticsService.getModelBreakdown(teamId, from, to);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * GET /api/analytics/agents/{teamId}?days=7&limit=10
     * Returns the top N agents by token consumption.
     */
    @GetMapping("/agents/{teamId}")
    public ResponseEntity<List<TokenAnalyticsService.AgentUsageSummary>> getTopAgents(
            @PathVariable String teamId,
            @RequestParam(defaultValue = "7") int days) {

        List<TokenAnalyticsService.AgentUsageSummary> agents =
            analyticsService.getTopAgents(teamId, days);
        return ResponseEntity.ok(agents);
    }
}
