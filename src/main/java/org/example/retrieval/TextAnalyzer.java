package org.example.retrieval;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Small, language-level normalization; contains no document IDs or sample answers. */
public final class TextAnalyzer {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP = Set.of(("a an the i me my mine we our you your he she it its they their "
            + "this that these those is are am was were be been being do does did can could would should may might "
            + "must will shall have has had of to from for at in on with by as and or if then than into through "
            + "before after about during between when what which who whom how where why please tell explain "
            + "know want need get help information some any there here also long take takes taking").split(" "));
    private static final Map<String, String> FORMS = Map.ofEntries(
            Map.entry("forgot", "forget"), Map.entry("forgotten", "forget"),
            Map.entry("lost", "lose"), Map.entry("sent", "send"),
            Map.entry("closed", "close"), Map.entry("closure", "close"),
            Map.entry("changed", "change"), Map.entry("changes", "change"),
            Map.entry("enabled", "enable"), Map.entry("removed", "remove"),
            Map.entry("expires", "expire"), Map.entry("expired", "expire"),
            Map.entry("expiration", "expire"), Map.entry("expiry", "expire"), Map.entry("valid", "expire"),
            Map.entry("complete", "finish"), Map.entry("completed", "finish"),
            Map.entry("completes", "finish"), Map.entry("completion", "finish"),
            Map.entry("processing", "process"), Map.entry("settings", "setting"),
            Map.entry("changing", "change"), Map.entry("resetting", "reset"));
    public List<String> tokens(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("(?<=\\d)(?=\\p{L})", " ");
        var result = new ArrayList<String>();
        var matcher = TOKEN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP.contains(token)) result.add(stem(token));
        }
        return result;
    }
    public Set<String> terms(String text) { return new LinkedHashSet<>(tokens(text)); }
    private String stem(String token) {
        if (FORMS.containsKey(token)) return FORMS.get(token);
        if (token.length() > 4 && token.endsWith("ies")) return token.substring(0, token.length() - 3) + "y";
        if (token.length() > 4 && token.endsWith("s") && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        if (token.length() > 5 && token.endsWith("ed")) return token.substring(0, token.length() - 2);
        return token;
    }
}
