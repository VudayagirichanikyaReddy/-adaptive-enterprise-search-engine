package algorithms;

public class LCPArray {

    private final String text;
    private final int[] suffixArray;
    private final int[] lcp;

    public LCPArray(String text, int[] suffixArray) {
        this.text = text.toLowerCase();
        this.suffixArray = suffixArray.clone();
        this.lcp = buildLCPArray();
    }

    private int[] buildLCPArray() {
        int n = text.length();
        int[] result = new int[n];

        if (n == 0) {
            return result;
        }

        int[] rank = new int[n];

        for (int i = 0; i < n; i++) {
            rank[suffixArray[i]] = i;
        }

        int commonLength = 0;

        for (int i = 0; i < n; i++) {
            int suffixRank = rank[i];

            if (suffixRank == n - 1) {
                commonLength = 0;
                continue;
            }

            int nextSuffix = suffixArray[suffixRank + 1];

            while (i + commonLength < n &&
                    nextSuffix + commonLength < n &&
                    text.charAt(i + commonLength) ==
                            text.charAt(nextSuffix + commonLength)) {

                commonLength++;
            }

            result[suffixRank] = commonLength;

            if (commonLength > 0) {
                commonLength--;
            }
        }

        return result;
    }

    public int[] getLCPArray() {
        return lcp.clone();
    }

    public String getText() {
        return text;
    }
}