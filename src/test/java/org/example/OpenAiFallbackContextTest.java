package org.example;

import org.example.model.KnowledgeBase;
import org.example.service.AnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"support.kb-path=src/test/resources/sample-kb.json", "support.mode=openai", "OPENAI_API_KEY="})
class OpenAiFallbackContextTest {
    @Autowired AnswerService service;
    @Autowired KnowledgeBase kb;

    @Test void openAiModeStartsWithoutSecretsAndUsesCoreFlow() {
        assertThat(kb.documents()).hasSize(6);
        assertThat(service.ask("How do I reset my password?").decision()).isEqualTo("answer");
        assertThat(service.ask("Can support change my email address for me?").decision()).isEqualTo("abstain");
    }
}
