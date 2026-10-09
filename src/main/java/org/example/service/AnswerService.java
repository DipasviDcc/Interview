package org.example.service;

import org.example.answering.*;
import org.example.config.SupportProperties;
import org.example.model.AskResponse;
import org.example.retrieval.TfIdfRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnswerService {
    public static final String FALLBACK = "I don't have enough supporting information in the knowledge base to answer that. Please contact human support.";
    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);
    private final TfIdfRetriever retriever;
    private final SupportChecker checker;
    private final AnswerGenerator generator;
    private final AnswerValidator validator;
    private final SupportProperties properties;
    private final ExtractiveAnswerGenerator fallback = new ExtractiveAnswerGenerator();

    public AnswerService(TfIdfRetriever retriever, SupportChecker checker, AnswerGenerator generator,
                         AnswerValidator validator, SupportProperties properties) {
        this.retriever = retriever;
        this.checker = checker;
        this.generator = generator;
        this.validator = validator;
        this.properties = properties;
    }

    public AskResponse ask(String question) {
        var hits = retriever.retrieve(question, properties.topK());
        var support = checker.check(question, hits);
        var debug = new AskResponse.Debug(hits.stream().map(hit -> hit.document().id()).toList(), support.score());
        if (!support.supported()) return abstain(question, debug);
        AnswerDraft draft;
        try {
            draft = generator.generate(question, support.evidence());
        } catch (RuntimeException e) {
            // SDK exception messages can contain request/response data. Log type only, never secrets or question text.
            log.warn("Answer provider unavailable ({}); using checked extractive fallback", e.getClass().getSimpleName());
            draft = fallback.generate(question, support.evidence());
        }
        var evidence = validator.validate(question, draft, support.evidence());
        if (evidence.isEmpty()) return abstain(question, debug);
        var citations = evidence.stream().map(e -> new AskResponse.Citation(e.document().id(),
                e.document().title(), e.snippet())).toList();
        String answer = evidence.stream().map(e -> e.snippet()).collect(Collectors.joining("\n\n"));
        return new AskResponse(question, "answer", answer, citations, debug);
    }

    private AskResponse abstain(String question, AskResponse.Debug debug) {
        return new AskResponse(question, "abstain", FALLBACK, List.of(), debug);
    }
}
