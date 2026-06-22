package com.eswarr.tokenoptimizer.exception;

/**
 * Thrown when a team's token budget is exceeded and hardBlock = true.
 */
public class BudgetExceededException extends RuntimeException {

    private final String teamId;
    private final long currentUsage;
    private final long limit;

    public BudgetExceededException(String teamId, long currentUsage, long limit) {
        super(String.format(
            "Token budget exceeded for team '%s': %d/%d tokens used (%.1f%%). " +
            "Increase daily limit or wait for reset at midnight UTC.",
            teamId, currentUsage, limit, (double) currentUsage / limit * 100));
        this.teamId       = teamId;
        this.currentUsage = currentUsage;
        this.limit        = limit;
    }

    public String getTeamId()       { return teamId; }
    public long   getCurrentUsage() { return currentUsage; }
    public long   getLimit()        { return limit; }
    public double getUsagePercent() { return (double) currentUsage / limit; }
}
