package org.example.answering;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/** Internal structured model output; never serialized directly to the public API. */
public class AnswerDraft {
    public boolean abstain;
    public List<Claim> claims;

    public static class Claim {
        public String documentId;
        @JsonPropertyDescription("Copy one complete approved excerpt verbatim, preserving all conditions and qualifications.")
        public String quote;
        public Claim() {}
        public Claim(String documentId, String quote) { this.documentId = documentId; this.quote = quote; }
    }
    public AnswerDraft() {}
    public AnswerDraft(boolean abstain, List<Claim> claims) { this.abstain = abstain; this.claims = claims; }
}
