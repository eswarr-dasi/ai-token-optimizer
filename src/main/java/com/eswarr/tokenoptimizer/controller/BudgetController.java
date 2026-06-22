package com.eswarr.tokenoptimizer.controller;

import com.eswarr.tokenoptimizer.model.BudgetConfig;
import com.eswarr.tokenoptimizer.repository.BudgetConfigRepository;
import com.eswarr.tokenoptimizer.service.BudgetEnforcerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Budget management REST API.
 * Create, update, and query per-team token budgets.
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetEnforcerService budgetService;
    private final BudgetConfigRepository budgetConfigRepository;

    /** GET /api/budget/{teamId} — get current budget status */
    @GetMapping("/{teamId}")
    public ResponseEntity<?> getBudgetStatus(@PathVariable String teamId) {
        Optional<BudgetConfig> config = budgetService.getBudgetStatus(teamId);
        if (config.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BudgetConfig c = config.get();
        return ResponseEntity.ok(Map.of(
            "teamId",              c.getTeamId(),
            "dailyTokenLimit",     c.getDailyTokenLimit(),
            "currentDailyUsage",   c.getCurrentDailyUsage(),
            "dailyUsagePercent",   String.format("%.1f%%", c.getDailyUsagePercent() * 100),
            "monthlyTokenLimit",   c.getMonthlyTokenLimit(),
            "currentMonthlyUsage", c.getCurrentMonthlyUsage(),
            "hardBlock",           c.isHardBlock(),
            "alertThreshold",      c.getAlertThreshold()
        ));
    }

    /** POST /api/budget — create or update a team budget */
    @PostMapping
    public ResponseEntity<BudgetConfig> upsertBudget(@RequestBody BudgetConfig config) {
        Optional<BudgetConfig> existing = budgetConfigRepository.findByTeamIdAndActiveTrue(config.getTeamId());
        if (existing.isPresent()) {
            BudgetConfig e = existing.get();
            e.setDailyTokenLimit(config.getDailyTokenLimit());
            e.setMonthlyTokenLimit(config.getMonthlyTokenLimit());
            e.setAlertWebhookUrl(config.getAlertWebhookUrl());
            e.setAlertThreshold(config.getAlertThreshold() != null
                ? config.getAlertThreshold() : BigDecimal.valueOf(0.80));
            e.setHardBlock(config.isHardBlock());
            return ResponseEntity.ok(budgetConfigRepository.save(e));
        }
        if (config.getAlertThreshold() == null) {
            config.setAlertThreshold(BigDecimal.valueOf(0.80));
        }
        return ResponseEntity.ok(budgetConfigRepository.save(config));
    }

    /** DELETE /api/budget/{teamId} — deactivate a team budget */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deactivateBudget(@PathVariable String teamId) {
        budgetConfigRepository.findByTeamIdAndActiveTrue(teamId).ifPresent(c -> {
            c.setActive(false);
            budgetConfigRepository.save(c);
        });
        return ResponseEntity.noContent().build();
    }
}
