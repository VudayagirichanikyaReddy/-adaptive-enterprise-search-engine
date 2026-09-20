package algorithms;

public class ZAlgorithm {

    public static int search(String text, String pattern) {

        if (pattern.length() == 0) {
            return 0;
        }

        String combined = pattern + "$" + text;

        int n = combined.length();
        int m = pattern.length();

        int[] z = new int[n];

        int left = 0;
        int right = 0;

        for (int i = 1; i < n; i++) {

            if (i <= right) {
                z[i] = Math.min(right - i + 1, z[i - left]);
            }

            while (i + z[i] < n &&
                    combined.charAt(z[i]) == combined.charAt(i + z[i])) {

                z[i]++;
            }

            if (i + z[i] - 1 > right) {
                left = i;
                right = i + z[i] - 1;
            }
        }

        for (int i = 0; i < n; i++) {

            if (z[i] == m) {
                return i - m - 1;
            }
        }

        return -1;
    }
}