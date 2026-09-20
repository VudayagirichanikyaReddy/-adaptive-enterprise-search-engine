package algorithms;

public class RabinKarp {

    private static final int BASE = 256;
    private static final int PRIME = 101;

    public static int search(String text, String pattern) {

        int n = text.length();
        int m = pattern.length();

        if (m == 0) {
            return 0;
        }

        if (m > n) {
            return -1;
        }

        int patternHash = 0;
        int textHash = 0;
        int highestPower = 1;

        // Calculate BASE^(m-1)
        for (int i = 0; i < m - 1; i++) {
            highestPower = (highestPower * BASE) % PRIME;
        }

        // Calculate initial hash values
        for (int i = 0; i < m; i++) {
            patternHash =
                    (BASE * patternHash + pattern.charAt(i)) % PRIME;

            textHash =
                    (BASE * textHash + text.charAt(i)) % PRIME;
        }

        // Slide the pattern across the text
        for (int i = 0; i <= n - m; i++) {

            // If hash values match, verify characters
            if (patternHash == textHash) {

                int j = 0;

                while (j < m &&
                        text.charAt(i + j) == pattern.charAt(j)) {
                    j++;
                }

                if (j == m) {
                    return i;
                }
            }

            // Calculate hash for the next window
            if (i < n - m) {

                textHash =
                        (BASE * (textHash
                                - text.charAt(i) * highestPower)
                                + text.charAt(i + m))
                                % PRIME;

                if (textHash < 0) {
                    textHash += PRIME;
                }
            }
        }

        return -1;
    }
}