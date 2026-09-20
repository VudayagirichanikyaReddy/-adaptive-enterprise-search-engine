package algorithms;

public class KMPSearch {

    public static int search(String text, String pattern) {

        int n = text.length();
        int m = pattern.length();

        if (m == 0) {
            return 0;
        }

        int[] lps = buildLPS(pattern);

        int i = 0;
        int j = 0;

        while (i < n) {

            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;

                if (j == m) {
                    return i - j;
                }

            } else {

                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return -1;
    }

    private static int[] buildLPS(String pattern) {

        int m = pattern.length();

        int[] lps = new int[m];

        int length = 0;
        int i = 1;

        while (i < m) {

            if (pattern.charAt(i) == pattern.charAt(length)) {

                length++;
                lps[i] = length;
                i++;

            } else {

                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }
}