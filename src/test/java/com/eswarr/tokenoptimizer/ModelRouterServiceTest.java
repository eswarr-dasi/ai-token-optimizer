package com.eswarr.tokenoptimizer;

import com.eswarr.tokenoptimizer.model.LlmRequest;
import com.eswarr.tokenoptimizer.model.ModelTier;
import com.eswarr.tokenoptimizer.service.ModelRouterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ModelRouterServiceTest {

    private ModelRouterService routerService;

    @BeforeEach
    void setUp() {
        routerService = new ModelRouterService(new SimpleMeterRegistry());
    }

    @Test
    void simpleClassificationTask_routesToFast() {
        LlmRequest request = buildRequest("classify this text as positive or negative");
        ModelTier tier = routerService.route(request);
        assertThat(tier).isEqualTo(ModelTier.FAST);
    }

    @Test
    void complexAnalysisTask_routesAwayFromFast() {
        LlmRequest request = buildRequest("analyze and synthesize the multi-step reasoning " +
            "required to critique the architectural design decisions in this complex distributed system");
        ModelTier tier = routerService.route(request);
        assertThat(tier).isNotEqualTo(ModelTier.FAST);
    }

    @Test
    void shortSummaryTask_routesToFast() {
        LlmRequest request = buildRequest("summarize this paragraph in one sentence");
        ModelTier tier = routerService.route(request);
        assertThat(tier).isEqualTo(ModelTier.FAST);
    }

    @Test
    void codeGenerationTask_routesToBalancedOrPowerful() {
        LlmRequest request = buildRequest("write code to implement a binary search tree");
        ModelTier tier = routerService.route(request);
        assertThat(tier).isIn(ModelTier.BALANCED, ModelTier.POWERFUL);
    }

    @Test
    void highPriorityRequest_routesToBetterModel() {
        LlmRequest request = LlmRequest.builder()
            .agentId("agent-1")
            .teamId("team-1")
            .messages(List.of(new LlmRequest.Message("user", "summarize this")))
            .priority("HIGH")
            .build();
        ModelTier tier = routerService.route(request);
        assertThat(tier).isNotEqualTo(ModelTier.FAST);
    }

    @Test
    void confidenceScore_simplePrompt_isHigh() {
        LlmRequest request = buildRequest("extract the date from this invoice");
        double score = routerService.computeFastTierConfidence(request);
        assertThat(score).isGreaterThan(0.85);
    }

    @Test
    void confidenceScore_longComplexPrompt_isLow() {
        String longPrompt = "analyze and critique ".repeat(200);
        LlmRequest request = buildRequest(longPrompt);
        double score = routerService.computeFastTierConfidence(request);
        assertThat(score).isLessThan(0.5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"summarize", "classify", "format", "translate", "extract"})
    void simpleKeywords_boostFastConfidence(String keyword) {
        LlmRequest req = buildRequest(keyword + " this document");
        double score = routerService.computeFastTierConfidence(req);
        assertThat(score).isGreaterThan(0.85);
    }

    private LlmRequest buildRequest(String userMessage) {
        return LlmRequest.builder()
            .agentId("test-agent")
            .teamId("test-team")
            .messages(List.of(new LlmRequest.Message("user", userMessage)))
            .build();
    }
}
