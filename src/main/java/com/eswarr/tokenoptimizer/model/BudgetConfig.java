package com.eswarr.tokenoptimizer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Per-team or per-agent token budget configuration.
 * Enforced in real-time by BudgetEnforcerService.
 */
@Entity
@Table(name = "budget_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String teamId;

    /** Maximum tokens allowed per day across all agents in the team */
    private long dailyTokenLimit;

    /** Maximum tokens allowed per month */
    private long monthlyTokenLimit;

    /** Alert webhook URL when threshold is breached */
    private String alertWebhookUrl;

    /** Fraction of limit at which to send alert (e.g. 0.80 = alert at 80%) */
    @Column(precision = 4, scale = 2)
    private BigDecimal alertThreshold;

    /** Whether to hard-block requests after limit or just alert */
    private boolean hardBlock;

    /** Current day's token usage (reset by BudgetResetScheduler) */
    private long currentDailyUsage;

    /** Current month's token usage */
    private long currentMonthlyUsage;

    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDailyLimitExceeded() {
        return hardBlock && currentDailyUsage >= dailyTokenLimit;
    }

    public double getDailyUsagePercent() {
        return dailyTokenLimit > 0 ? (double) currentDailyUsage / dailyTokenLimit : 0.0;
    }
}
