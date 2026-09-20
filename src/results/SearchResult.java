package results;

public class SearchResult {

    private int documentId;
    private String title;
    private String algorithm;
    private int matches;
    private long executionTime;

    // Original constructor, kept so existing code still compiles
    public SearchResult(String algorithm, int matches, long executionTime) {
        this(-1, "", algorithm, matches, executionTime);
    }

    // New constructor used by the API layer
    public SearchResult(int documentId, String title,
                        String algorithm, int matches, long executionTime) {
        this.documentId = documentId;
        this.title = title;
        this.algorithm = algorithm;
        this.matches = matches;
        this.executionTime = executionTime;
    }

    public int getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getAlgorithm() { return algorithm; }
    public int getMatches() { return matches; }
    public long getExecutionTime() { return executionTime; }
}