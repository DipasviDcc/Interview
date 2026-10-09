package org.example.answering;

import org.example.model.Evidence;
import java.util.List;

public final class ExtractiveAnswerGenerator implements AnswerGenerator {
    @Override
    public AnswerDraft generate(String question, List<Evidence> approvedEvidence) {
        return new AnswerDraft(false, approvedEvidence.stream()
                .map(e -> new AnswerDraft.Claim(e.document().id(), e.snippet())).toList());
    }
}
