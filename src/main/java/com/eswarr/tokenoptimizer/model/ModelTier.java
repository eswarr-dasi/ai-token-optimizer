package com.eswarr.tokenoptimizer.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the three cost/quality tiers for model routing.
 * The ModelRouterService selects the appropriate tier based on task complexity.
 */
@Getter
@RequiredArgsConstructor
public enum ModelTier {

    /**
     * FAST tier — cheap, low-latency models for simple tasks.
     * Use for: classification, short summarization, simple Q&A, formatting.
     * Examples: gpt-4o-mini, claude-3-haiku, gemini-1.5-flash
     */
    FAST(
        "gpt-4o-mini",
        "claude-3-haiku-20240307",
        "gemini-1.5-flash",
        0.000015,   // cost per input token (USD)
        0.000060    // cost per output token (USD)
    ),

    /**
     * BALANCED tier — mid-range models for moderate complexity.
     * Use for: multi-step reasoning, code generation, analysis.
     * Examples: gpt-4o, claude-3-sonnet, gemini-1.5-pro
     */
    BALANCED(
        "gpt-4o",
        "claude-3-sonnet-20240229",
        "gemini-1.5-pro",
        0.000005,
        0.000015
    ),

    /**
     * POWERFUL tier — frontier models for maximum quality.
     * Use for: complex reasoning, long-context analysis, creative tasks.
     * Examples: gpt-4-turbo, claude-3-opus, gemini-ultra
     */
    POWERFUL(
        "gpt-4-turbo",
        "claude-3-opus-20240229",
        "gemini-ultra",
        0.000030,
        0.000060
    );

    private final String openAiModel;
    private final String anthropicModel;
    private final String googleModel;
    private final double inputCostPerToken;
    private final double outputCostPerToken;

    public double estimateCost(int inputTokens, int outputTokens) {
        return (inputTokens * inputCostPerToken) + (outputTokens * outputCostPerToken);
    }

    public String getModelForProvider(String provider) {
        return switch (provider.toUpperCase()) {
            case "OPENAI"    -> openAiModel;
            case "ANTHROPIC" -> anthropicModel;
            case "GOOGLE"    -> googleModel;
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
}
