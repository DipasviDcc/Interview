package org.example.answering;

import org.example.model.Evidence;
import org.example.model.KbDocument;
import org.example.retrieval.TfIdfRetriever;
import org.example.service.SupportChecker;
import java.util.*;

public final class AnswerValidator {
    private final SupportChecker checker;
    public AnswerValidator(SupportChecker checker) { this.checker = checker; }

    public List<Evidence> validate(String question, AnswerDraft draft, List<Evidence> approved) {
        if (draft == null || draft.abstain || draft.claims == null || draft.claims.isEmpty()
                || draft.claims.size() > approved.size()) return List.of();
        var selected = new LinkedHashMap<String, Evidence>();
        for (var claim : draft.claims) {
            if (claim == null || claim.documentId == null || claim.quote == null) return List.of();
            var match = approved.stream().filter(e -> e.document().id().equals(claim.documentId)
                    && e.snippet().equals(claim.quote) && e.document().text().contains(claim.quote)).findFirst();
            if (match.isEmpty() || selected.containsKey(claim.documentId)) return List.of();
            selected.put(claim.documentId, match.get());
        }
        // Re-check every requested clause using only the excerpts actually returned by the composer.
        var hits = selected.values().stream().map(e -> new TfIdfRetriever.Hit(
                new KbDocument(e.document().id(), e.document().title(), e.snippet()), e.retrievalScore())).toList();
        return checker.check(question, hits).supported() ? List.copyOf(selected.values()) : List.of();
    }
}
