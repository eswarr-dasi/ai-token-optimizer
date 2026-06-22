package com.eswarr.tokenoptimizer;

import com.eswarr.tokenoptimizer.exception.BudgetExceededException;
import com.eswarr.tokenoptimizer.model.BudgetConfig;
import com.eswarr.tokenoptimizer.repository.BudgetConfigRepository;
import com.eswarr.tokenoptimizer.service.BudgetEnforcerService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetEnforcerServiceTest {

    @Mock private BudgetConfigRepository budgetConfigRepository;
    @Mock private WebClient.Builder webClientBuilder;

    private BudgetEnforcerService service;

    @BeforeEach
    void setUp() {
        service = new BudgetEnforcerService(budgetConfigRepository, webClientBuilder, new SimpleMeterRegistry());
    }

    @Test
    void checkBudget_noBudgetConfig_allowsRequest() {
        when(budgetConfigRepository.findByTeamIdAndActiveTrue("team-1")).thenReturn(Optional.empty());
        assertThatCode(() -> service.checkBudget("team-1")).doesNotThrowAnyException();
    }

    @Test
    void checkBudget_withinLimit_allowsRequest() {
        BudgetConfig config = buildConfig(100_000, 50_000, true);
        when(budgetConfigRepository.findByTeamIdAndActiveTrue("team-1")).thenReturn(Optional.of(config));
        assertThatCode(() -> service.checkBudget("team-1")).doesNotThrowAnyException();
    }

    @Test
    void checkBudget_hardBlockExceeded_throwsBudgetExceededException() {
        BudgetConfig config = buildConfig(100_000, 100_001, true);
        when(budgetConfigRepository.findByTeamIdAndActiveTrue("team-1")).thenReturn(Optional.of(config));
        assertThatThrownBy(() -> service.checkBudget("team-1"))
            .isInstanceOf(BudgetExceededException.class)
            .hasMessageContaining("team-1");
    }

    @Test
    void checkBudget_softLimitExceeded_allowsRequestStill() {
        BudgetConfig config = buildConfig(100_000, 100_001, false); // hardBlock=false
        when(budgetConfigRepository.findByTeamIdAndActiveTrue("team-1")).thenReturn(Optional.of(config));
        assertThatCode(() -> service.checkBudget("team-1")).doesNotThrowAnyException();
    }

    @Test
    void budgetExceedException_hasCorrectUsagePercent() {
        BudgetExceededException ex = new BudgetExceededException("team-x", 90_000, 100_000);
        org.assertj.core.api.Assertions.assertThat(ex.getUsagePercent()).isEqualTo(0.9);
    }

    private BudgetConfig buildConfig(long limit, long usage, boolean hardBlock) {
        BudgetConfig config = new BudgetConfig();
        config.setTeamId("team-1");
        config.setDailyTokenLimit(limit);
        config.setCurrentDailyUsage(usage);
        config.setMonthlyTokenLimit(3_000_000);
        config.setAlertThreshold(BigDecimal.valueOf(0.80));
        config.setHardBlock(hardBlock);
        config.setActive(true);
        return config;
    }
}
