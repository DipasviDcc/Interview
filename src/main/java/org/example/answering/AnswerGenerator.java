package org.example.answering;

import org.example.model.Evidence;
import java.util.List;

public interface AnswerGenerator {
    AnswerDraft generate(String question, List<Evidence> approvedEvidence);
    default boolean available() { return true; }
}
