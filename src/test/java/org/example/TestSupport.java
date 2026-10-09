package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.answering.*;
import org.example.config.SupportProperties;
import org.example.model.KbDocument;
import org.example.retrieval.*;
import org.example.service.*;
import java.nio.file.Path;
import java.util.List;

final class TestSupport {
    static final ObjectMapper MAPPER = new ObjectMapper();
    static SupportProperties properties() {
        return new SupportProperties(Path.of("kb.json"), SupportProperties.Mode.EXTRACTIVE, 3, 0.12, 0.85, 1200,
                new SupportProperties.Openai("gpt-4.1-mini", 20, 1, 1200));
    }
    static List<KbDocument> sample() {
        return KnowledgeBaseLoader.load(Path.of("src/test/resources/sample-kb.json"), MAPPER);
    }
    static AnswerService service(List<KbDocument> documents, AnswerGenerator generator) {
        var properties = properties();
        var analyzer = new TextAnalyzer();
        var retriever = new TfIdfRetriever(documents, analyzer);
        var checker = new SupportChecker(analyzer, retriever, properties);
        return new AnswerService(retriever, checker, generator, new AnswerValidator(checker), properties);
    }
    static AnswerService service() { return service(sample(), new ExtractiveAnswerGenerator()); }
}
