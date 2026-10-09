package org.example.model;
import java.util.List;
public record KnowledgeBase(List<KbDocument> documents) {
    public KnowledgeBase { documents = List.copyOf(documents); }
}
