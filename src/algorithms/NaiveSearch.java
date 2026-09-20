package algorithms;

public class NaiveSearch {

        public static int search(String text, String pattern) {

            int n = text.length();
            int m = pattern.length();

            for (int i = 0; i <= n - m; i++) {

                int j = 0;

                while (j < m && text.charAt(i + j) == pattern.charAt(j)) {
                    j++;
                }

                if (j == m) {
                    return i;
                }
            }

            return -1;
        }
    }

