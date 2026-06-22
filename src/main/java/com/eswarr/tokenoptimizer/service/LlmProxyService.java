package com.eswarr.tokenoptimizer.service;

import com.eswarr.tokenoptimizer.model.LlmRequest;
import com.eswarr.tokenoptimizer.model.LlmResponse;
import com.eswarr.tokenoptimizer.model.ModelTier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;

/**
 * Forwards optimized requests to the actual LLM provider.
 * Supports OpenAI-compatible APIs (OpenAI, Azure OpenAI, Anthropic via compatibility layer).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmProxyService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${token-optimizer.providers.openai.api-key:}")
    private String openAiApiKey;

    @Value("${token-optimizer.providers.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${token-optimizer.providers.anthropic.api-key:}")
    private String anthropicApiKey;

    /**
     * Forward request to the LLM provider and return a normalized LlmResponse.
     */
    public Mono<LlmResponse> forward(LlmRequest request, ModelTier tier) {
        String provider = request.getProvider() != null ? request.getProvider() : "OPENAI";
        String model = request.getModelOverride() != null
            ? request.getModelOverride()
            : tier.getModelForProvider(provider);

        Timer.Sample timerSample = Timer.start(meterRegistry);

        return switch (provider.toUpperCase()) {
            case "OPENAI"    -> forwardToOpenAi(request, model, timerSample);
            case "ANTHROPIC" -> forwardToAnthropic(request, model, timerSample);
            default          -> forwardToOpenAi(request, model, timerSample); // default to OpenAI-compat
        };
    }

    private Mono<LlmResponse> forwardToOpenAi(LlmRequest request, String model, Timer.Sample sample) {
        Map<String, Object> body = buildOpenAiRequestBody(request, model);

        return webClientBuilder.build()
            .post()
            .uri(openAiBaseUrl + "/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> parseOpenAiResponse(json, model, "OPENAI"))
            .doOnSuccess(r -> {
                sample.stop(meterRegistry.timer("llm.provider.latency", "provider", "openai"));
                meterRegistry.counter("llm.requests", "provider", "openai", "model", model).increment();
            })
            .doOnError(e -> log.error("OpenAI call failed: {}", e.getMessage()));
    }

    private Mono<LlmResponse> forwardToAnthropic(LlmRequest request, String model, Timer.Sample sample) {
        Map<String, Object> body = buildAnthropicRequestBody(request, model);

        return webClientBuilder.build()
            .post()
            .uri("https://api.anthropic.com/v1/messages")
            .header("x-api-key", anthropicApiKey)
            .header("anthropic-version", "2023-06-01")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> parseAnthropicResponse(json, model))
            .doOnSuccess(r -> sample.stop(meterRegistry.timer("llm.provider.latency", "provider", "anthropic")));
    }

    private Map<String, Object> buildOpenAiRequestBody(LlmRequest req, String model) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (req.getSystemPrompt() != null) {
            messages.add(Map.of("role", "system", "content", req.getSystemPrompt()));
        }
        if (req.getMessages() != null) {
            req.getMessages().forEach(m -> messages.add(Map.of("role", m.getRole(), "content", m.getContent())));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        if (req.getMaxTokens() != null) body.put("max_tokens", req.getMaxTokens());
        if (req.getTemperature() != null) body.put("temperature", req.getTemperature());
        return body;
    }

    private Map<String, Object> buildAnthropicRequestBody(LlmRequest req, String model) {
        Map<String, Object> body = new HashMap<>(buildOpenAiRequestBody(req, model));
        body.put("max_tokens", req.getMaxTokens() != null ? req.getMaxTokens() : 1024);
        return body;
    }

    private LlmResponse parseOpenAiResponse(JsonNode json, String model, String provider) {
        JsonNode choice = json.path("choices").path(0);
        JsonNode usage  = json.path("usage");
        return LlmResponse.builder()
            .content(choice.path("message").path("content").asText())
            .modelUsed(model)
            .provider(provider)
            .promptTokens(usage.path("prompt_tokens").asInt())
            .completionTokens(usage.path("completion_tokens").asInt())
            .totalTokens(usage.path("total_tokens").asInt())
            .finishReason(choice.path("finish_reason").asText())
            .timestamp(Instant.now())
            .success(true)
            .build();
    }

    private LlmResponse parseAnthropicResponse(JsonNode json, String model) {
        JsonNode usage = json.path("usage");
        return LlmResponse.builder()
            .content(json.path("content").path(0).path("text").asText())
            .modelUsed(model)
            .provider("ANTHROPIC")
            .promptTokens(usage.path("input_tokens").asInt())
            .completionTokens(usage.path("output_tokens").asInt())
            .totalTokens(usage.path("input_tokens").asInt() + usage.path("output_tokens").asInt())
            .finishReason(json.path("stop_reason").asText())
            .timestamp(Instant.now())
            .success(true)
            .build();
    }
}
