package com.eswarr.tokenoptimizer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Generates text embeddings for semantic similarity comparison.
 * Embeddings are cached in Redis to avoid redundant API calls.
 * Uses Spring AI's EmbeddingClient (OpenAI text-embedding-3-small by default).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;

    /**
     * Generate an embedding vector for the given text.
     * Result is cached by text hash to avoid re-embedding identical prompts.
     */
    @Cacheable(value = "embeddings", key = "#root.target.sha256(#text)")
    public float[] embed(String text) {
        try {
            String normalized = normalizeText(text);
            List<Double> embedding = embeddingClient.embed(normalized);
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }
            log.debug("Generated embedding of dimension {} for text length {}", result.length, text.length());
            return result;
        } catch (Exception e) {
            log.error("Failed to generate embedding: {}", e.getMessage());
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    /**
     * Compute cosine similarity between two embedding vectors.
     * Returns a value in [0, 1] where 1.0 = identical semantics.
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Normalize text: lowercase, collapse whitespace, strip special chars.
     * Ensures semantically identical prompts produce identical embeddings.
     */
    public String normalizeText(String text) {
        return text.toLowerCase()
                   .replaceAll("[\\r\\n\\t]+", " ")
                   .replaceAll("\\s{2,}", " ")
                   .trim();
    }

    /** SHA-256 hash of text — used as embedding cache key */
    public String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizeText(text).getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
}
