package org.example;

import org.example.answering.ExtractiveAnswerGenerator;
import org.example.service.KnowledgeBaseLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class KnowledgeBaseLoaderTest {
    @TempDir Path directory;

    @Test void readsTheActualRepositoryInputFile() {
        assertThat(KnowledgeBaseLoader.load(Path.of("kb.json"), TestSupport.MAPPER)).isNotNull();
    }

    @Test void replacementFileChangesAnswersWithoutEmbeddedSampleFacts() throws Exception {
        var file = directory.resolve("replacement.json");
        Files.writeString(file, """
                [{"id":"repair-9","title":"Keyboard repairs","text":"Users can replace a damaged keyboard at a service center. Keyboard repairs cost 45 dollars."}]
                """);
        var service = TestSupport.service(KnowledgeBaseLoader.load(file, TestSupport.MAPPER), new ExtractiveAnswerGenerator());
        var response = service.ask("How can I replace a damaged keyboard?");
        assertThat(response.decision()).isEqualTo("answer");
        assertThat(response.answer()).contains("service center");
        assertThat(response.citations()).extracting(c -> c.id()).containsExactly("repair-9");
        assertThat(service.ask("How do I reset my password?").decision()).isEqualTo("abstain");
        Files.writeString(file, """
                [{"id":"new-policy","title":"Keyboard repairs","text":"Users can replace a damaged keyboard at the repair desk. Keyboard repairs cost 70 dollars."}]
                """);
        var reloaded = TestSupport.service(KnowledgeBaseLoader.load(file, TestSupport.MAPPER), new ExtractiveAnswerGenerator());
        assertThat(reloaded.ask("How can I replace a damaged keyboard?").answer()).contains("repair desk", "70 dollars").doesNotContain("45 dollars");
    }

    @Test void invalidFilesFailClearlyAndEmptyKbAbstains() throws Exception {
        var file = directory.resolve("bad.json");
        for (String json : new String[]{"not json", "{}", "[{\"id\":\"x\",\"title\":\"T\"}]",
                "[{\"id\":\"x\",\"title\":\"T\",\"text\":\"B\"},{\"id\":\"x\",\"title\":\"T\",\"text\":\"C\"}]"}) {
            Files.writeString(file, json);
            assertThatThrownBy(() -> KnowledgeBaseLoader.load(file, TestSupport.MAPPER))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot load knowledge base");
        }
        assertThatThrownBy(() -> KnowledgeBaseLoader.load(directory.resolve("missing.json"), TestSupport.MAPPER))
                .hasMessageContaining("Cannot load knowledge base");
        Files.writeString(file, "[]");
        var empty = TestSupport.service(KnowledgeBaseLoader.load(file, TestSupport.MAPPER), new ExtractiveAnswerGenerator());
        assertThat(empty.ask("Any question?").decision()).isEqualTo("abstain");
    }
}
