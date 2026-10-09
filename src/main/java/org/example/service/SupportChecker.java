package org.example.service;

import org.example.config.SupportProperties;
import org.example.model.Evidence;
import org.example.retrieval.TextAnalyzer;
import org.example.retrieval.TfIdfRetriever;
import java.util.*;
import java.util.regex.Pattern;

/** Deterministic support gate. Lexical support is a conservative heuristic, not semantic proof. */
public final class SupportChecker {
    public record Result(boolean supported, double score, List<Evidence> evidence, String reason) {}
    private static final Pattern NUMBER = Pattern.compile("(?<![\\p{L}\\p{N}])\\d+(?:\\.\\d+)?(?![\\p{L}\\p{N}])");
    private static final Pattern QUANTITY = Pattern.compile("(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(seconds?|minutes?|hours?|days?|weeks?|months?|years?)\\b");
    private static final Pattern ACTOR = Pattern.compile("(?i)\\b(?:can|could|does|do|will|may|must|should)\\s+(?:the\\s+)?(support(?:\\s+agents?)?|users?|customers?|admins?|administrators?|staff|agents?)\\b");
    private static final Pattern TIME_QUESTION = Pattern.compile("(?i)\\b(?:how long|when|expire\\w*|expiry|expiration|valid|processing time|duration)\\b");
    private static final Pattern TIME_FACT = Pattern.compile("(?i)\\b\\d+\\s*(?:second|minute|hour|day|week|month|year)s?\\b|\\b(?:immediately|instantly|until|indefinitely)\\b");
    private static final Pattern GUARANTEE = Pattern.compile("(?i)\\b(?:guarantee\\w*|promise\\w*|definitely|certainly|always)\\b");
    private static final Pattern UNCERTAIN = Pattern.compile("(?i)\\b(?:not|no|cannot|can't|never|usually|may|might|can take longer|typically)\\b");
    private static final Pattern NEGATIVE = Pattern.compile("(?i)\\b(?:not|no|cannot|can't|never)\\b");
    private final TextAnalyzer analyzer;
    private final TfIdfRetriever retriever;
    private final PassageSelector selector;
    private final SupportProperties properties;

    public SupportChecker(TextAnalyzer analyzer, TfIdfRetriever retriever, SupportProperties properties) {
        this.analyzer = analyzer;
        this.retriever = retriever;
        this.properties = properties;
        this.selector = new PassageSelector(analyzer, retriever, properties.maxPassageChars());
    }

    public Result check(String question, List<TfIdfRetriever.Hit> hits) {
        var candidates = new ArrayList<Evidence>();
        for (var hit : hits) {
            if (hit.score() >= properties.minSimilarity()) {
                selector.select(question, hit.document().text()).ifPresent(snippet ->
                        candidates.add(new Evidence(hit.document(), snippet, hit.score())));
            }
        }
        if (candidates.isEmpty()) return rejected(0, "no usable retrieved evidence");
        var chosen = new LinkedHashMap<String, Evidence>();
        double weakestScore = 1;
        // Every explicit subquestion must independently be supported by one passage.
        for (String clause : question.split("(?i)\\?+|\\s+and\\s+|\\s+also\\s+")) {
            var terms = analyzer.terms(clause);
            if (terms.isEmpty()) continue;
            var eligible = new ArrayList<Evidence>();
            double bestCoverage = 0;
            for (var evidence : candidates) {
                double coverage = retriever.coverage(terms, analyzer.terms(evidence.snippet()));
                bestCoverage = Math.max(bestCoverage, coverage);
                if (coverage >= properties.minCoverage() && requestedFactsPresent(clause, evidence.snippet())) {
                    eligible.add(evidence);
                }
            }
            if (eligible.isEmpty()) return rejected(bestCoverage, "requested fact or clause lacks support");
            if (conflicting(eligible)) return rejected(0, "retrieved sources disagree on quantities or negation");
            Evidence best = eligible.get(0);
            chosen.put(best.document().id(), best);
            double coverage = retriever.coverage(terms, analyzer.terms(best.snippet()));
            weakestScore = Math.min(weakestScore, 0.65 * coverage + 0.35 * best.retrievalScore());
        }
        if (chosen.isEmpty()) return rejected(0, "question has no searchable terms");
        return new Result(true, round(weakestScore), List.copyOf(chosen.values()), "supported");
    }

    private boolean requestedFactsPresent(String question, String evidence) {
        if (!numbers(evidence).containsAll(numbers(question))) return false;
        if (!quantities(evidence).containsAll(quantities(question))) return false;
        if (!actorMatchesAction(question, evidence)) return false;
        if (TIME_QUESTION.matcher(question).find() && !TIME_FACT.matcher(evidence).find()) return false;
        if (GUARANTEE.matcher(question).find()
                && (!GUARANTEE.matcher(evidence).find() || UNCERTAIN.matcher(evidence).find())) return false;
        return true;
    }

    private boolean actorMatchesAction(String question, String evidence) {
        var matcher = ACTOR.matcher(question);
        if (!matcher.find()) return true;
        var actor = analyzer.terms(matcher.group(1));
        var action = analyzer.terms(question);
        action.removeAll(actor);
        if (action.isEmpty()) return false;
        for (String sentence : evidence.split("(?<=[.!?])\\s+")) {
            var terms = analyzer.terms(sentence);
            if (terms.containsAll(actor) && retriever.coverage(action, terms) >= properties.minCoverage()) return true;
        }
        return false;
    }

    private Set<String> quantities(String text) {
        var values = new HashSet<String>();
        var matcher = QUANTITY.matcher(text);
        while (matcher.find()) values.add(matcher.group(1) + ":" + matcher.group(2).toLowerCase(Locale.ROOT).replaceAll("s$", ""));
        return values;
    }

    private boolean conflicting(List<Evidence> candidates) {
        if (candidates.size() < 2) return false;
        Evidence first = candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            String a = first.snippet(), b = candidates.get(i).snippet();
            if (a.equals(b)) continue;
            if (NEGATIVE.matcher(a).find() != NEGATIVE.matcher(b).find()) return true;
            if (!numbers(a).equals(numbers(b)) && !numbers(a).isEmpty() && !numbers(b).isEmpty()) return true;
        }
        return false;
    }

    private Set<String> numbers(String text) {
        var values = new HashSet<String>();
        var matcher = NUMBER.matcher(text);
        while (matcher.find()) values.add(matcher.group());
        return values;
    }

    private Result rejected(double score, String reason) {
        return new Result(false, round(score), List.of(), reason);
    }
    private double round(double value) { return Math.round(value * 10000.0) / 10000.0; }
}
