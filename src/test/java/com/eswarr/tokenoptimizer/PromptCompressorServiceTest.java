package com.eswarr.tokenoptimizer;

import com.eswarr.tokenoptimizer.service.PromptCompressorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PromptCompressorServiceTest {

    private PromptCompressorService compressor;

    @BeforeEach
    void setUp() {
        compressor = new PromptCompressorService();
    }

    @Test
    void compress_nullInput_returnsEmptyResult() {
        var result = compressor.compress(null);
        assertThat(result.wasCompressed()).isFalse();
    }

    @Test
    void compress_blankInput_returnsEmptyResult() {
        var result = compressor.compress("   ");
        assertThat(result.wasCompressed()).isFalse();
    }

    @Test
    void compress_verbosePhrase_replacesCorrectly() {
        String prompt = "Please make sure to validate all inputs before processing.";
        var result = compressor.compress(prompt);
        assertThat(result.compressedText()).contains("ensure");
        assertThat(result.compressedText()).doesNotContain("please make sure to");
    }

    @Test
    void compress_multipleVerbosePhrases_reducesLength() {
        String prompt = "Please note that in order to complete the task, " +
            "it is very important that you please make sure to follow the instructions.";
        var result = compressor.compress(prompt);
        assertThat(result.compressedText().length()).isLessThan(prompt.length());
    }

    @Test
    void compress_excessiveNewlines_collapsed() {
        String prompt = "Line one.\n\n\n\n\nLine two.";
        var result = compressor.compress(prompt);
        assertThat(result.compressedText()).doesNotContain("\n\n\n");
    }

    @Test
    void compress_duplicateLines_removed() {
        String prompt = "You are a helpful assistant.\nYou are a helpful assistant.\nBe concise.";
        var result = compressor.compress(prompt);
        long occurrences = result.compressedText().chars()
            .filter(c -> c == 'Y').count();
        assertThat(result.compressedText().split("You are a helpful assistant").length - 1).isLessThan(2);
    }

    @Test
    void compress_cleanPrompt_tokensSavedIsZeroOrLow() {
        String prompt = "Be concise and accurate.";
        var result = compressor.compress(prompt);
        assertThat(result.tokensSaved()).isLessThanOrEqualTo(2);
    }
}
