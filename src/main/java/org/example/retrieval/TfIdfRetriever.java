package org.example.retrieval;

import org.example.model.KbDocument;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable, in-memory TF-IDF index with title boost and cosine similarity. */
public final class TfIdfRetriever {
    public record Hit(KbDocument document, double score) {}
    private final TextAnalyzer analyzer;
    private final List<KbDocument> documents;
    private final Map<String, Double> idf;
    private final Map<String, Map<String, Double>> vectors;
    public TfIdfRetriever(List<KbDocument> documents, TextAnalyzer analyzer) {
        this.documents = List.copyOf(documents);
        this.analyzer = analyzer;
        Map<String, Integer> frequency = new HashMap<>();
        for (var doc : documents) analyzer.terms(doc.title() + " " + doc.text())
                .forEach(term -> frequency.merge(term, 1, Integer::sum));
        idf = new HashMap<>();
        frequency.forEach((term, count) -> idf.put(term, Math.log((documents.size() + 1.0) / (count + 1.0)) + 1));
        vectors = new HashMap<>();
        for (var doc : documents) {
            vectors.put(doc.id(), vector(doc.title() + " " + doc.title() + " " + doc.text()));
        }
    }
    public List<Hit> retrieve(String question, int topK) {
        var query = vector(question);
        return documents.stream().map(doc -> new Hit(doc, dot(query, vectors.get(doc.id()))))
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(hit -> hit.document().id()))
                .limit(topK).toList();
    }
    public double coverage(Set<String> query, Set<String> evidence) {
        if (query.isEmpty()) return 0;
        double all = query.stream().mapToDouble(this::weight).sum();
        double covered = query.stream().filter(evidence::contains).mapToDouble(this::weight).sum();
        return covered / all;
    }
    private double weight(String term) {
        // Unknown query words retain weight so missing requested facts cannot disappear from the score.
        return idf.getOrDefault(term, Math.log(documents.size() + 1.0) + 1);
    }
    private Map<String, Double> vector(String text) {
        Map<String, Long> counts = analyzer.tokens(text).stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Double> values = new HashMap<>();
        counts.forEach((term, count) -> values.put(term, (1 + Math.log(count)) * weight(term)));
        double norm = Math.sqrt(values.values().stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) values.replaceAll((term, value) -> value / norm);
        return values;
    }
    private double dot(Map<String, Double> a, Map<String, Double> b) {
        return Math.min(1, a.entrySet().stream().mapToDouble(e -> e.getValue() * b.getOrDefault(e.getKey(), 0.0)).sum());
    }
}
