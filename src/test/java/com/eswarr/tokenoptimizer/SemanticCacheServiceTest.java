package com.eswarr.tokenoptimizer;

import com.eswarr.tokenoptimizer.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private EmbeddingService service;  // test the EmbeddingService directly

    private EmbeddingService realEmbeddingService;

    @BeforeEach
    void setUp() {
        realEmbeddingService = new EmbeddingService(null); // null embedding client, using for math only
    }

    @Test
    void cosineSimilarity_identicalVectors_returns1() {
        float[] vec = {1.0f, 0.5f, 0.25f, 0.1f};
        double sim = realEmbeddingService.cosineSimilarity(vec, vec);
        assertThat(sim).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void cosineSimilarity_oppositeVectors_returns0() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.0f, 1.0f};
        double sim = realEmbeddingService.cosineSimilarity(a, b);
        assertThat(sim).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void cosineSimilarity_similarVectors_returnsHighScore() {
        float[] a = {0.9f, 0.1f, 0.5f};
        float[] b = {0.85f, 0.15f, 0.48f};
        double sim = realEmbeddingService.cosineSimilarity(a, b);
        assertThat(sim).isGreaterThan(0.99);
    }

    @Test
    void normalizeText_collapsesWhitespace() {
        String input = "  Hello   World  \n\n  Test  ";
        String normalized = realEmbeddingService.normalizeText(input);
        assertThat(normalized).isEqualTo("hello world test");
    }

    @Test
    void normalizeText_lowercases() {
        String normalized = realEmbeddingService.normalizeText("HELLO WORLD");
        assertThat(normalized).isEqualTo("hello world");
    }

    @Test
    void sha256_sameInput_returnsSameHash() {
        String h1 = realEmbeddingService.sha256("hello world");
        String h2 = realEmbeddingService.sha256("hello world");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void sha256_differentInput_returnsDifferentHash() {
        String h1 = realEmbeddingService.sha256("hello world");
        String h2 = realEmbeddingService.sha256("hello earth");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void sha256_normalizedEquivalents_returnSameHash() {
        String h1 = realEmbeddingService.sha256("Hello World");
        String h2 = realEmbeddingService.sha256("hello world");
        assertThat(h1).isEqualTo(h2);
    }
}
