package org.example.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.nio.file.Path;

@Validated
@ConfigurationProperties("support")
public record SupportProperties(
        @NotNull Path kbPath, @NotNull Mode mode,
        @Min(1) @Max(10) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double minSimilarity,
        @DecimalMin("0.0") @DecimalMax("1.0") double minCoverage,
        @Min(200) @Max(8000) int maxPassageChars,
        @NotNull @Valid Openai openai) {
    public enum Mode { EXTRACTIVE, OPENAI }
    public record Openai(@NotBlank String model, @Min(1) @Max(120) int timeoutSeconds,
                         @Min(0) @Max(2) int maxRetries, @Min(128) @Max(4000) int maxOutputTokens) {}
}
