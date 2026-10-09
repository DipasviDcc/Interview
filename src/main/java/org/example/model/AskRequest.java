package org.example.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @Schema(example = "How do I reset my password?")
        @NotBlank(message = "question must not be blank")
        @Size(max = 1000, message = "question must be at most 1000 characters") String question) {}
