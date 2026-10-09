package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AskResponse(String question,
                          @Schema(allowableValues = {"answer", "abstain"}) String decision,
                          String answer, List<Citation> citations, Debug debug) {
    public record Citation(String id, String title, String snippet) {}
    public record Debug(@JsonProperty("retrieved_ids") List<String> retrievedIds,
                        @JsonProperty("support_score") double supportScore) {}
}
