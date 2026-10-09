package org.example;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.sun.net.httpserver.HttpServer;
import org.example.answering.*;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class OpenAiAnswerGeneratorTest {
    @Test void officialSdkSendsStructuredResponsesRequestAndValidatesResult() throws Exception {
        var requestBody = new AtomicReference<String>();
        var responseText = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = responseText.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        var client = OpenAIOkHttpClient.builder().apiKey("local-test-placeholder")
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                .timeout(Duration.ofSeconds(2)).maxRetries(0).build();
        try {
            var doc = TestSupport.sample().get(0);
            var draft = new AnswerDraft(false, List.of(new AnswerDraft.Claim(doc.id(), doc.text())));
            responseText.set(response(draft));
            var generator = new OpenAiAnswerGenerator(client, TestSupport.MAPPER, "gpt-4.1-mini", 1200);
            var service = TestSupport.service(TestSupport.sample(), generator);
            assertThat(service.ask("How do I reset my password?").decision()).isEqualTo("answer");
            var sent = TestSupport.MAPPER.readTree(requestBody.get());
            assertThat(sent.at("/text/format/type").asText()).isEqualTo("json_schema");
            assertThat(sent.get("store").asBoolean()).isFalse();
            assertThat(sent.get("instructions").asText()).contains("untrusted data");
            responseText.set(response(new AnswerDraft(false, List.of(new AnswerDraft.Claim("fictional", "Invented policy.")))));
            assertThat(service.ask("How do I reset my password?").decision()).isEqualTo("abstain");
        } finally {
            client.close();
            server.stop(0);
        }
    }

    @Test void missingKeyUsesOfflinePath() {
        var generator = new OpenAiAnswerGenerator(null, TestSupport.MAPPER, "gpt-4.1-mini", 1200);
        assertThat(generator.available()).isFalse();
        assertThat(TestSupport.service(TestSupport.sample(), generator).ask("How do I reset my password?").decision()).isEqualTo("answer");
    }

    private String response(AnswerDraft draft) throws Exception {
        return TestSupport.MAPPER.writeValueAsString(Map.of("id", "resp_local", "object", "response", "created_at", 1,
                "status", "completed", "model", "gpt-4.1-mini", "output", List.of(Map.of(
                        "id", "msg_local", "type", "message", "role", "assistant", "status", "completed", "content", List.of(Map.of(
                                "type", "output_text", "text", TestSupport.MAPPER.writeValueAsString(draft), "annotations", List.of()))))));
    }
}
