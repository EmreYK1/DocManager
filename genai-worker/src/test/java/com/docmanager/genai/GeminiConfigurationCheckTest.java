package com.docmanager.genai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GeminiConfigurationCheckTest {

    @Test
    void reportsMissingKeyWithoutFailing(CapturedOutput output) {
        new GeminiConfigurationCheck("  ", "test-model").run(new DefaultApplicationArguments());

        assertThat(output).contains("GEMINI_API_KEY ist nicht gesetzt");
    }

    @Test
    void logsModelButNeverTheKey(CapturedOutput output) {
        new GeminiConfigurationCheck("secret-key", "test-model").run(new DefaultApplicationArguments());

        assertThat(output).contains("test-model").doesNotContain("secret-key");
    }
}
