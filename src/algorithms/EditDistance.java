package algorithms;

/** Levenshtein edit distance using dynamic programming. */
public class EditDistance {

    /**
     * dp[i][j] = minimum edits to turn the first i characters of a
     * into the first j characters of b.
     */
    public static int distance(String a, String b) {

        int n = a.length();
        int m = b.length();

        int[][] dp = new int[n + 1][m + 1];

        // Base cases: turning a prefix into "" costs i deletions,
        // and building a prefix from "" costs j insertions.
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {

                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,        // deletion
                                dp[i][j - 1] + 1),       // insertion
                        dp[i - 1][j - 1] + cost);         // substitution (or match)
            }
        }
        return dp[n][m];
    }
}