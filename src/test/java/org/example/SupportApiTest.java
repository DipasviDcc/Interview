package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"support.kb-path=src/test/resources/sample-kb.json", "support.mode=extractive"})
@AutoConfigureMockMvc
class SupportApiTest {
    @Autowired MockMvc mvc;

    @Test void requiredJsonContractAndCitationMembership() throws Exception {
        var result = mvc.perform(post("/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How do I reset my password?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("answer"))
                .andExpect(jsonPath("$.citations[0].id").value("doc_1"))
                .andExpect(jsonPath("$.citations[0].title").isString())
                .andExpect(jsonPath("$.citations[0].snippet").isString())
                .andExpect(jsonPath("$.debug.retrieved_ids").isArray())
                .andExpect(jsonPath("$.debug.support_score").isNumber()).andReturn();
        var body = TestSupport.MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(body.properties()).extracting(e -> e.getKey())
                .containsExactlyInAnyOrder("question", "decision", "answer", "citations", "debug");
    }

    @Test void unsupportedQueryIsAValid200Response() throws Exception {
        mvc.perform(post("/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Can support change my email address for me?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decision").value("abstain"))
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test void badInputReturns400() throws Exception {
        for (String body : new String[]{"{}", "{\"question\":\"   \"}", "{\"question\":null}", "not json", "{\"question\":123}", "{\"question\":true}",
                "{\"question\":\"" + "x".repeat(1001) + "\"}"}) {
            mvc.perform(post("/ask").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        }
    }

    @Test void knowledgeBaseIsReadOnlyAndSwaggerDescribesAllEndpoints() throws Exception {
        mvc.perform(get("/api/kb")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("doc_1"));
        mvc.perform(post("/api/kb").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.answerMode").value("extractive"))
                .andExpect(jsonPath("$.openaiAvailable").value(false));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/ask'].post").exists())
                .andExpect(jsonPath("$.paths['/api/kb'].get").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
