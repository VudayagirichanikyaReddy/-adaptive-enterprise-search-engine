package algorithms;

import java.util.Arrays;

public class SuffixArray {

    private final String text;
    private final int[] suffixArray;

    public SuffixArray(String text) {
        this.text = text.toLowerCase();
        this.suffixArray = buildSuffixArray(this.text);
    }

    private int[] buildSuffixArray(String text) {
        int n = text.length();

        if (n == 0) {
            return new int[0];
        }

        Integer[] suffixes = new Integer[n];
        int[] rank = new int[n];
        int[] newRank = new int[n];

        for (int i = 0; i < n; i++) {
            suffixes[i] = i;
            rank[i] = text.charAt(i);
        }

        for (int k = 1; k < n; k *= 2) {
            final int length = k;

            Arrays.sort(suffixes, (a, b) -> {
                if (rank[a] != rank[b]) {
                    return Integer.compare(rank[a], rank[b]);
                }

                int rankA = (a + length < n) ? rank[a + length] : -1;
                int rankB = (b + length < n) ? rank[b + length] : -1;

                return Integer.compare(rankA, rankB);
            });

            newRank[suffixes[0]] = 0;

            for (int i = 1; i < n; i++) {
                int current = suffixes[i];
                int previous = suffixes[i - 1];

                boolean different =
                        rank[current] != rank[previous] ||
                                ((current + length < n ? rank[current + length] : -1) !=
                                        (previous + length < n ? rank[previous + length] : -1));

                newRank[current] = newRank[previous] + (different ? 1 : 0);
            }

            System.arraycopy(newRank, 0, rank, 0, n);

            if (rank[suffixes[n - 1]] == n - 1) {
                break;
            }
        }

        int[] result = new int[n];

        for (int i = 0; i < n; i++) {
            result[i] = suffixes[i];
        }

        return result;
    }

    public int search(String pattern) {
        pattern = pattern.toLowerCase();

        int left = 0;
        int right = suffixArray.length - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            String suffix = text.substring(suffixArray[mid]);

            if (suffix.startsWith(pattern)) {
                return suffixArray[mid];
            }

            if (suffix.compareTo(pattern) < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    public int countOccurrences(String pattern) {
        pattern = pattern.toLowerCase();

        int first = findFirstOccurrence(pattern);

        if (first == -1) {
            return 0;
        }

        int last = findLastOccurrence(pattern);

        return last - first + 1;
    }

    private int findFirstOccurrence(String pattern) {
        int left = 0;
        int right = suffixArray.length - 1;
        int result = -1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int suffixIndex = suffixArray[mid];

            if (startsWithAt(suffixIndex, pattern)) {
                result = mid;
                right = mid - 1;
            } else if (compareSuffixWithPattern(suffixIndex, pattern) < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    private int findLastOccurrence(String pattern) {
        int left = 0;
        int right = suffixArray.length - 1;
        int result = -1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int suffixIndex = suffixArray[mid];

            if (startsWithAt(suffixIndex, pattern)) {
                result = mid;
                left = mid + 1;
            } else if (compareSuffixWithPattern(suffixIndex, pattern) < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    private boolean startsWithAt(int index, String pattern) {
        if (index + pattern.length() > text.length()) {
            return false;
        }

        return text.regionMatches(index, pattern, 0, pattern.length());
    }

    private int compareSuffixWithPattern(int index, String pattern) {
        int patternLength = pattern.length();
        int remaining = text.length() - index;
        int length = Math.min(patternLength, remaining);

        int comparison = text.regionMatches(index, pattern, 0, length)
                ? 0
                : text.substring(index, index + length).compareTo(pattern.substring(0, length));

        if (comparison != 0) {
            return comparison;
        }

        return Integer.compare(remaining, patternLength);
    }

    public int[] getSuffixArray() {
        return suffixArray.clone();
    }

    public String getText() {
        return text;
    }
}