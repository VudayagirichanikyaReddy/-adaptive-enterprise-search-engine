package engine;

import java.util.LinkedHashSet;
import java.util.Set;

/** Decides which algorithm suits a query and dataset. */
public class QueryAnalyzer {

    public static QueryAnalysis analyze(
            String query,
            SpellCorrector corrector,
            int documentCount) {

        String original = (query == null) ? "" : query.trim();
        String[] patterns = extractPatterns(query);

        // Multiple words -> Aho-Corasick
        if (patterns.length > 1) {
            return new QueryAnalysis(
                    QueryAnalysis.MULTI_PATTERN,
                    QueryAnalysis.AHO_CORASICK,
                    "Multiple search patterns detected",
                    patterns,
                    original,
                    original,
                    false);
        }

        // Check for spelling correction
        if (patterns.length == 1 && !corrector.contains(patterns[0])) {

            String suggestion = corrector.findClosest(patterns[0]);

            if (suggestion != null) {
                return new QueryAnalysis(
                        QueryAnalysis.SPELL_CORRECTION,
                        QueryAnalysis.EDIT_DISTANCE,
                        "Possible spelling mistake detected; closest vocabulary match found",
                        new String[]{suggestion},
                        original,
                        suggestion,
                        true);
            }
        }

        // Large dataset -> Suffix Array
        if (patterns.length == 1 && documentCount >= 100) {
            return new QueryAnalysis(
                    QueryAnalysis.SINGLE_PATTERN,
                    QueryAnalysis.SUFFIX_ARRAY,
                    "Large document collection detected; Suffix Array selected",
                    patterns,
                    original,
                    original,
                    false);
        }

        // Small/medium dataset -> KMP
        String reason = "Single search pattern detected; KMP selected for the current dataset size";

        return new QueryAnalysis(
                QueryAnalysis.SINGLE_PATTERN,
                QueryAnalysis.KMP,
                reason,
                patterns,
                original,
                original,
                false);
    }

    /** Trim, lowercase, split on whitespace, drop empties and duplicates. */
    private static String[] extractPatterns(String query) {

        Set<String> unique = new LinkedHashSet<>();

        if (query != null) {
            for (String word : query.trim().toLowerCase().split("\\s+")) {
                if (!word.isEmpty()) {
                    unique.add(word);
                }
            }
        }

        return unique.toArray(new String[0]);
    }
}