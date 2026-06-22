package com.eswarr.tokenoptimizer.repository;

import com.eswarr.tokenoptimizer.model.BudgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetConfigRepository extends JpaRepository<BudgetConfig, String> {

    Optional<BudgetConfig> findByTeamIdAndActiveTrue(String teamId);

    List<BudgetConfig> findAllByActiveTrue();

    @Modifying
    @Query("UPDATE BudgetConfig b SET b.currentDailyUsage = 0 WHERE b.active = true")
    void resetAllDailyUsage();

    @Modifying
    @Query("UPDATE BudgetConfig b SET b.currentMonthlyUsage = 0 WHERE b.active = true")
    void resetAllMonthlyUsage();

    @Modifying
    @Query("UPDATE BudgetConfig b SET b.currentDailyUsage = b.currentDailyUsage + :tokens WHERE b.teamId = :teamId")
    void incrementDailyUsage(@Param("teamId") String teamId, @Param("tokens") long tokens);

    @Modifying
    @Query("UPDATE BudgetConfig b SET b.currentMonthlyUsage = b.currentMonthlyUsage + :tokens WHERE b.teamId = :teamId")
    void incrementMonthlyUsage(@Param("teamId") String teamId, @Param("tokens") long tokens);

    @Query("SELECT b FROM BudgetConfig b WHERE b.active = true AND " +
           "(b.currentDailyUsage * 1.0 / b.dailyTokenLimit) >= b.alertThreshold")
    List<BudgetConfig> findBudgetsNearingDailyLimit();
}
