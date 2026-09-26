package engine;

/** Result of analysing a query: what kind it is and which algorithm to use. */
public class QueryAnalysis {

    public static final String SINGLE_PATTERN   = "SINGLE_PATTERN";
    public static final String MULTI_PATTERN    = "MULTI_PATTERN";
    public static final String SPELL_CORRECTION = "SPELL_CORRECTION";

    public static final String KMP          = "KMP";
    public static final String AHO_CORASICK = "AHO_CORASICK";
    public static final String EDIT_DISTANCE = "EDIT_DISTANCE";
    public static final String SUFFIX_ARRAY = "SUFFIX_ARRAY";

    private final String queryType;
    private final String selectedAlgorithm;
    private final String reason;
    private final String[] patterns;          // what will actually be searched
    private final String originalQuery;
    private final String correctedQuery;
    private final boolean correctionApplied;

    public QueryAnalysis(String queryType, String selectedAlgorithm, String reason,
                         String[] patterns, String originalQuery,
                         String correctedQuery, boolean correctionApplied) {
        this.queryType = queryType;
        this.selectedAlgorithm = selectedAlgorithm;
        this.reason = reason;
        this.patterns = patterns;
        this.originalQuery = originalQuery;
        this.correctedQuery = correctedQuery;
        this.correctionApplied = correctionApplied;
    }

    public String getQueryType() { return queryType; }
    public String getSelectedAlgorithm() { return selectedAlgorithm; }
    public String getReason() { return reason; }
    public String[] getPatterns() { return patterns; }
    public String getOriginalQuery() { return originalQuery; }
    public String getCorrectedQuery() { return correctedQuery; }
    public boolean isCorrectionApplied() { return correctionApplied; }

    /** Human-friendly name used in the existing "algorithm" JSON field. */
    public String getAlgorithmDisplayName() {
        if (AHO_CORASICK.equals(selectedAlgorithm)) return "Aho-Corasick";
        if (EDIT_DISTANCE.equals(selectedAlgorithm)) return "Edit Distance + KMP";
        if (SUFFIX_ARRAY.equals(selectedAlgorithm)) return "Suffix Array";
        return "KMP";
    }
}