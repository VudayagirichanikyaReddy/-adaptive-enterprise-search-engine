package algorithms;

import java.util.*;

public class AhoCorasick {

    private static class Node {

        Map<Character, Integer> children = new HashMap<>();

        int failure = 0;

        List<String> outputs = new ArrayList<>();
    }

    private final List<Node> trie = new ArrayList<>();

    public AhoCorasick() {
        trie.add(new Node());
    }

    // Build the Trie
    public void addKeyword(String keyword) {

        int current = 0;

        for (char ch : keyword.toLowerCase().toCharArray()) {

            if (!trie.get(current).children.containsKey(ch)) {

                trie.get(current).children.put(
                        ch,
                        trie.size()
                );

                trie.add(new Node());
            }

            current = trie.get(current).children.get(ch);
        }

        trie.get(current).outputs.add(keyword.toLowerCase());
    }

    // Build failure links
    public void build() {

        Queue<Integer> queue = new LinkedList<>();

        for (int child : trie.get(0).children.values()) {

            trie.get(child).failure = 0;

            queue.add(child);
        }

        while (!queue.isEmpty()) {

            int current = queue.poll();

            for (Map.Entry<Character, Integer> entry :
                    trie.get(current).children.entrySet()) {

                char ch = entry.getKey();
                int child = entry.getValue();

                queue.add(child);

                int failure = trie.get(current).failure;

                while (failure != 0 &&
                        !trie.get(failure).children.containsKey(ch)) {

                    failure = trie.get(failure).failure;
                }

                if (trie.get(failure).children.containsKey(ch)
                        && trie.get(failure).children.get(ch) != child) {

                    trie.get(child).failure =
                            trie.get(failure).children.get(ch);

                } else {

                    trie.get(child).failure = 0;
                }

                trie.get(child).outputs.addAll(
                        trie.get(trie.get(child).failure).outputs
                );
            }
        }
    }

    // Search multiple keywords
    public Set<String> search(String text) {

        Set<String> foundKeywords = new HashSet<>();

        int current = 0;

        text = text.toLowerCase();

        for (char ch : text.toCharArray()) {

            while (current != 0 &&
                    !trie.get(current).children.containsKey(ch)) {

                current = trie.get(current).failure;
            }

            if (trie.get(current).children.containsKey(ch)) {

                current = trie.get(current).children.get(ch);

            } else {

                current = 0;
            }

            foundKeywords.addAll(
                    trie.get(current).outputs
            );
        }

        return foundKeywords;
    }
}