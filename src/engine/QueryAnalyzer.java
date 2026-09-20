package engine;

import java.util.LinkedHashSet;
import java.util.Set;

/** Decides which algorithm suits a query. All selection logic lives here. */
public class QueryAnalyzer {

    public static QueryAnalysis analyze(String query, SpellCorrector corrector) {

        String original = (query == null) ? "" : query.trim();
        String[] patterns = extractPatterns(query);

        // CASE 2: several words -> Aho-Corasick (no spell correction for now)
        if (patterns.length > 1) {
            return new QueryAnalysis(
                    QueryAnalysis.MULTI_PATTERN,
                    QueryAnalysis.AHO_CORASICK,
                    "Multiple search patterns detected",
                    patterns, original, original, false);
        }

        String reason = "Single search pattern detected";

        if (patterns.length == 1 && !corrector.contains(patterns[0])) {

            String suggestion = corrector.findClosest(patterns[0]);

            // CASE 3: not in the vocabulary, but a close word exists
            if (suggestion != null) {
                return new QueryAnalysis(
                        QueryAnalysis.SPELL_CORRECTION,
                        QueryAnalysis.EDIT_DISTANCE,
                        "Possible spelling mistake detected; closest vocabulary match found",
                        new String[]{suggestion}, original, suggestion, true);
            }
            reason = "Single search pattern detected; no close vocabulary match found";
        }

        // CASE 1: word is in the vocabulary (or nothing close exists) -> KMP
        return new QueryAnalysis(
                QueryAnalysis.SINGLE_PATTERN,
                QueryAnalysis.KMP,
                reason,
                patterns, original, original, false);
    }

    /** Trim, lowercase, split on whitespace, drop empties and duplicates. */
    private static String[] extractPatterns(String query) {
        Set<String> unique = new LinkedHashSet<>();
        if (query != null) {
            for (String word : query.trim().toLowerCase().split("\\s+")) {
                if (!word.isEmpty()) unique.add(word);
            }
        }
        return unique.toArray(new String[0]);
    }
}