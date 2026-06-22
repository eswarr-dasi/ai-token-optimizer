package com.eswarr.tokenoptimizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Normalized LLM request envelope that the gateway processes.
 * Wraps the original provider-specific request payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmRequest {

    /** Calling agent identifier (from X-Agent-Id header) */
    private String agentId;

    /** Team this agent belongs to (from X-Team-Id header) */
    private String teamId;

    /** Target LLM provider: OPENAI | ANTHROPIC | GEMINI */
    private String provider;

    /** Model override — if null, ModelRouter will decide */
    private String modelOverride;

    /** The messages array (OpenAI-compatible format) */
    private List<Message> messages;

    /** Optional system prompt (separate from messages for compression) */
    private String systemPrompt;

    /** Max tokens for the response */
    private Integer maxTokens;

    /** Temperature (0.0 - 2.0) */
    private Double temperature;

    /** Additional provider-specific parameters */
    private Map<String, Object> extraParams;

    /** Whether to bypass cache for this request */
    @Builder.Default
    private boolean bypassCache = false;

    /** Priority hint: LOW | NORMAL | HIGH (affects routing) */
    @Builder.Default
    private String priority = "NORMAL";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;    // system | user | assistant
        private String content;
    }

    /** Returns the full prompt text for embedding/caching */
    public String getFullPromptText() {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null) sb.append(systemPrompt).append("\n");
        messages.forEach(m -> sb.append(m.getRole()).append(": ").append(m.getContent()).append("\n"));
        return sb.toString().trim();
    }
}
