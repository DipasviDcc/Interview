package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.answering.AnswerGenerator;
import org.example.config.SupportProperties;
import org.example.model.*;
import org.example.service.AnswerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "Support", description = "Ask questions and inspect the loaded knowledge base")
public class SupportController {
    private final AnswerService service;
    private final KnowledgeBase kb;
    private final SupportProperties properties;
    private final AnswerGenerator generator;
    public SupportController(AnswerService service, KnowledgeBase kb, SupportProperties properties, AnswerGenerator generator) {
        this.service = service; this.kb = kb; this.properties = properties; this.generator = generator;
    }
    @PostMapping("/ask")
    @Operation(summary = "Answer a question with citations or abstain")
    public AskResponse ask(@Valid @RequestBody AskRequest request) { return service.ask(request.question()); }

    @GetMapping("/api/kb")
    @Operation(summary = "Read all loaded knowledge-base articles")
    public List<KbDocument> knowledgeBase() { return kb.documents(); }

    @GetMapping("/api/status")
    @Operation(summary = "Inspect readiness and configured answering mode")
    public Status status() {
        return new Status("ready", kb.documents().size(), properties.mode().name().toLowerCase(java.util.Locale.ROOT),
                properties.mode() == SupportProperties.Mode.OPENAI && generator.available());
    }
    public record Status(String status, int documentCount, String answerMode, boolean openaiAvailable) {}
}
