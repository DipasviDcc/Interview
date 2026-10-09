package org.example.service;

import org.example.retrieval.TextAnalyzer;
import org.example.retrieval.TfIdfRetriever;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Pattern;

public final class PassageSelector {
    // Treat instructions addressed to the model as data, never as support evidence.
    private static final Pattern INSTRUCTION = Pattern.compile(
            "(?is)ignore.{0,60}(instruction|prompt|rule)|system\\s*prompt|developer\\s*message|"
            + "(?:assistant|chatbot|support answers?)\\s+(?:should|must|shall)|you are (?:an? |the )?"
            + "(?:assistant|chatbot)|reveal.{0,30}(?:secret|key)|<\\|(?:system|assistant)|"
            + "(?:answer|respond|output)\\s+(?:with|only)|(?:legal|tax) advice");
    private final TextAnalyzer analyzer;
    private final TfIdfRetriever retriever;
    private final int maxChars;

    public PassageSelector(TextAnalyzer analyzer, TfIdfRetriever retriever, int maxChars) {
        this.analyzer = analyzer;
        this.retriever = retriever;
        this.maxChars = maxChars;
    }

    public Optional<String> select(String question, String text) {
        if (text.length() <= maxChars && !INSTRUCTION.matcher(text).find()) return Optional.of(text.trim());
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        iterator.setText(text);
        var spans = new ArrayList<int[]>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            spans.add(new int[]{start, end});
        }
        String best = null;
        double bestCoverage = 0;
        // Keep complete sentences and adjacent qualifications in a contiguous source window.
        for (int i = 0; i < spans.size(); i++) {
            for (int j = i; j < spans.size(); j++) {
                String window = text.substring(spans.get(i)[0], spans.get(j)[1]).trim();
                if (window.length() > maxChars || INSTRUCTION.matcher(window).find()) break;
                double coverage = retriever.coverage(analyzer.terms(question), analyzer.terms(window));
                if (coverage > bestCoverage || (coverage == bestCoverage && best != null && window.length() > best.length())) {
                    best = window;
                    bestCoverage = coverage;
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
