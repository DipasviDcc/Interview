package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.example.answering.*;
import org.example.model.KnowledgeBase;
import org.example.retrieval.*;
import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import java.time.Duration;

@Configuration
public class ServiceConfiguration {
    @Bean Jackson2ObjectMapperBuilderCustomizer strictStringInputs() {
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
    @Bean KnowledgeBase knowledgeBase(SupportProperties properties, ObjectMapper mapper) {
        return new KnowledgeBase(KnowledgeBaseLoader.load(properties.kbPath(), mapper));
    }
    @Bean TextAnalyzer textAnalyzer() { return new TextAnalyzer(); }
    @Bean TfIdfRetriever retriever(KnowledgeBase kb, TextAnalyzer analyzer) {
        return new TfIdfRetriever(kb.documents(), analyzer);
    }
    @Bean SupportChecker supportChecker(TextAnalyzer analyzer, TfIdfRetriever retriever, SupportProperties properties) {
        return new SupportChecker(analyzer, retriever, properties);
    }
    @Bean AnswerValidator answerValidator(SupportChecker checker) { return new AnswerValidator(checker); }

    @Bean(destroyMethod = "close") OpenAIClient openAIClient(SupportProperties properties, Environment environment) {
        String key = environment.getProperty("OPENAI_API_KEY", "");
        if (properties.mode() != SupportProperties.Mode.OPENAI || key.isBlank()) return null;
        return OpenAIOkHttpClient.builder().apiKey(key)
                .timeout(Duration.ofSeconds(properties.openai().timeoutSeconds()))
                .maxRetries(properties.openai().maxRetries()).build();
    }

    @Bean AnswerGenerator answerGenerator(SupportProperties properties, ObjectMapper mapper,
                                          org.springframework.beans.factory.ObjectProvider<OpenAIClient> client) {
        if (properties.mode() == SupportProperties.Mode.OPENAI) {
            return new OpenAiAnswerGenerator(client.getIfAvailable(), mapper, properties.openai().model(),
                    properties.openai().maxOutputTokens());
        }
        return new ExtractiveAnswerGenerator();
    }

    @Bean OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Evidence Support API").version("1.0.0")
                .description("Answers supported by local kb.json passages. Unsupported questions abstain. "
                        + "No API key is required in extractive mode. The support score is a lexical heuristic."));
    }
}
