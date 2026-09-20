package engine;

import algorithms.EditDistance;
import documents.document;

import java.util.Set;
import java.util.TreeSet;

/** Suggests corrections using a vocabulary built from the real documents. */
public class SpellCorrector {

    private static final int MIN_WORD_LENGTH = 3;   // never "correct" 1-2 letter words

    // TreeSet = unique words, kept sorted, so ties are always broken alphabetically
    private final Set<String> vocabulary = new TreeSet<>();

    public SpellCorrector(document[] documents) {
        for (document doc : documents) {
            addWords(doc.getTitle());
            addWords(doc.getContent());
        }
    }

    // lowercase, replace punctuation with spaces, split into words
    private void addWords(String text) {
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!word.isEmpty()) vocabulary.add(word);
        }
    }

    public boolean contains(String word) {
        return vocabulary.contains(word);
    }

    public int size() {
        return vocabulary.size();
    }

    /** Maximum allowed distance depends on word length. */
    public int maxDistance(String word) {
        return word.length() <= 4 ? 1 : 2;
    }

    /**
     * Returns the closest vocabulary word within the threshold,
     * or null when nothing is close enough (no forced correction).
     */
    public String findClosest(String word) {

        if (word.length() < MIN_WORD_LENGTH) return null;

        int limit = maxDistance(word);
        String best = null;
        int bestDistance = limit + 1;

        for (String candidate : vocabulary) {

            // Edit distance is at least the difference in length, so skip early
            if (Math.abs(candidate.length() - word.length()) > limit) continue;

            int d = EditDistance.distance(word, candidate);

            if (d < bestDistance) {          // strictly smaller: first (alphabetical) wins ties
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }
}