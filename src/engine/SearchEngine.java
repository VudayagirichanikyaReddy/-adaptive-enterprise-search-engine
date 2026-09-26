package engine;

import results.SearchResult;
import java.util.ArrayList;
import java.util.List;
import algorithms.AhoCorasick;
import algorithms.KMPSearch;
import algorithms.NaiveSearch;
import algorithms.RabinKarp;
import algorithms.ZAlgorithm;
import algorithms.SuffixArray;
import algorithms.LCPArray;
import java.util.HashMap;
import java.util.Map;
import documents.document;

import java.util.Set;

public class SearchEngine {

    private document[] documents;
    private List<SearchResult> results = new ArrayList<>();
    private final SpellCorrector spellCorrector;

    // Cache Suffix Arrays so they are built only once per document.
    private final Map<Integer, SuffixArray> suffixArrayCache =
            new HashMap<>();

    /** Lets one benchmark routine run any of the single-pattern algorithms. */
    private interface Matcher {
        int find(String text, String pattern);
    }

    public SearchEngine(document[] documents) {
        this.documents = documents;
        this.spellCorrector = new SpellCorrector(documents);
    }

    public document[] getDocuments() {
        return documents;
    }

    // =====================================================
    // QUERY ANALYSIS + ALGORITHM SELECTION
    // =====================================================

    /** Analyse a query (pure logic, no searching). */
    public QueryAnalysis analyzeQuery(String query) {
        return QueryAnalyzer.analyze(
                query,
                spellCorrector,
                documents.length
        );
    }

    /** Analyse, then search with whichever strategy was selected. */
    public List<SearchResult> findMatchingDocuments(String query) {
        return findMatchingDocuments(query, analyzeQuery(query));
    }

    public List<SearchResult> findMatchingDocuments(
            String query,
            QueryAnalysis analysis) {

        String[] patterns = analysis.getPatterns();

        if (patterns.length == 0) {
            return new ArrayList<>();
        }

        if (QueryAnalysis.AHO_CORASICK.equals(
                analysis.getSelectedAlgorithm())) {

            return findWithAhoCorasick(patterns);
        }

        if (QueryAnalysis.SUFFIX_ARRAY.equals(
                analysis.getSelectedAlgorithm())) {

            return findWithSuffixArray(patterns[0]);
        }

        // KMP, and also SPELL_CORRECTION:
        // Edit Distance already produced the corrected word.
        return findWithKMP(patterns[0]);
    }

    // =====================================================
    // KMP SEARCH
    // =====================================================

    // SINGLE_PATTERN / SPELL_CORRECTION:
    // existing KMP class. matches = occurrences in the document.
    private List<SearchResult> findWithKMP(String pattern) {

        List<SearchResult> found = new ArrayList<>();

        for (int i = 0; i < documents.length; i++) {

            String text =
                    documents[i].getContent().toLowerCase();

            long start = System.nanoTime();

            int count = 0;
            int from = 0;
            int idx;

            while (from <= text.length()
                    && (idx = KMPSearch.search(
                    text.substring(from),
                    pattern)) != -1) {

                count++;
                from += idx + 1;
            }

            long time = System.nanoTime() - start;

            if (count > 0) {
                found.add(new SearchResult(
                        i + 1,
                        documents[i].getTitle(),
                        "KMP",
                        count,
                        time
                ));
            }
        }

        return found;
    }

    // =====================================================
    // SUFFIX ARRAY SEARCH
    // =====================================================

    /*
     * Suffix Arrays are expensive to build.
     *
     * Instead of creating a new SuffixArray for every search,
     * we build one for each document only when it is needed
     * and store it in suffixArrayCache.
     */
    private List<SearchResult> findWithSuffixArray(String pattern) {

        List<SearchResult> found = new ArrayList<>();

        for (int i = 0; i < documents.length; i++) {

            SuffixArray suffixArray =
                    suffixArrayCache.get(i);

            if (suffixArray == null) {

                suffixArray =
                        new SuffixArray(
                                documents[i].getContent()
                        );

                suffixArrayCache.put(i, suffixArray);
            }

            int occurrences =
                    suffixArray.countOccurrences(pattern);

            if (occurrences > 0) {

                found.add(new SearchResult(
                        i + 1,
                        documents[i].getTitle(),
                        "Suffix Array",
                        occurrences,
                        0
                ));
            }
        }

        return found;
    }

    // =====================================================
    // AHO-CORASICK SEARCH
    // =====================================================

    // MULTI_PATTERN:
    // Build the automaton once, scan each document once.
    // matches = how many different keywords were found.
    private List<SearchResult> findWithAhoCorasick(
            String[] patterns) {

        AhoCorasick ac = new AhoCorasick();

        for (String p : patterns) {
            ac.addKeyword(p);
        }

        ac.build();

        List<SearchResult> found = new ArrayList<>();

        for (int i = 0; i < documents.length; i++) {

            long start = System.nanoTime();

            Set<String> hits =
                    ac.search(
                            documents[i]
                                    .getContent()
                                    .toLowerCase()
                    );

            long time = System.nanoTime() - start;

            if (!hits.isEmpty()) {

                found.add(new SearchResult(
                        i + 1,
                        documents[i].getTitle(),
                        "Aho-Corasick",
                        hits.size(),
                        time
                ));
            }
        }

        return found;
    }

    // =====================================================
    // BENCHMARKS (used by Analytics)
    // =====================================================

    /**
     * One SearchResult per algorithm.
     *
     * Single keyword:
     * Naive, KMP, Rabin-Karp, Z and Suffix Array.
     *
     * Multiple keywords:
     * Naive, KMP, Rabin-Karp, Z and Aho-Corasick.
     */
    public List<SearchResult> runAllAlgorithms(String query) {

        QueryAnalysis analysis = analyzeQuery(query);

        String[] patterns = analysis.getPatterns();

        List<SearchResult> out = new ArrayList<>();

        if (patterns.length <= 1) {

            String q = (patterns.length == 1)
                    ? patterns[0]
                    : query.toLowerCase().trim();

            out.add(searchNaive(q));
            out.add(searchKMP(q));
            out.add(searchRabinKarp(q));
            out.add(searchZ(q));
            out.add(searchSuffixArray(q));

            return out;
        }

        out.add(benchmarkPatterns(
                "Naive Search",
                NaiveSearch::search,
                patterns
        ));

        out.add(benchmarkPatterns(
                "KMP",
                KMPSearch::search,
                patterns
        ));

        out.add(benchmarkPatterns(
                "Rabin-Karp",
                RabinKarp::search,
                patterns
        ));

        out.add(benchmarkPatterns(
                "Z Algorithm",
                ZAlgorithm::search,
                patterns
        ));

        out.add(benchmarkAhoCorasick(patterns));

        return out;
    }

    // =====================================================
    // MULTI-PATTERN BENCHMARK
    // =====================================================

    // 10 warm-up rounds, then the average of 100 timed rounds.
    // A document is matched if ANY keyword is found.
    private SearchResult benchmarkPatterns(
            String name,
            Matcher matcher,
            String[] patterns) {

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (document doc : documents) {

                String text =
                        doc.getContent().toLowerCase();

                for (String p : patterns) {
                    matcher.find(text, p);
                }
            }
        }

        int matched = 0;

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            for (document doc : documents) {

                String text =
                        doc.getContent().toLowerCase();

                boolean hit = false;

                for (String p : patterns) {

                    if (matcher.find(text, p) != -1) {
                        hit = true;
                    }
                }

                if (i == 0 && hit) {
                    matched++;
                }
            }
        }

        long averageTime =
                (System.nanoTime() - start) / 100;

        return new SearchResult(
                name,
                matched,
                averageTime
        );
    }

    // =====================================================
    // AHO-CORASICK BENCHMARK
    // =====================================================

    // Aho-Corasick timed the same way.
    // The automaton is rebuilt every round because a real query
    // has to build it, so the build cost is included.
    private SearchResult benchmarkAhoCorasick(
            String[] patterns) {

        // Warm-up
        for (int i = 0; i < 10; i++) {

            AhoCorasick ac = new AhoCorasick();

            for (String p : patterns) {
                ac.addKeyword(p);
            }

            ac.build();

            for (document doc : documents) {

                ac.search(
                        doc.getContent().toLowerCase()
                );
            }
        }

        int matched = 0;

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            AhoCorasick ac = new AhoCorasick();

            for (String p : patterns) {
                ac.addKeyword(p);
            }

            ac.build();

            for (document doc : documents) {

                Set<String> hits =
                        ac.search(
                                doc.getContent().toLowerCase()
                        );

                if (i == 0 && !hits.isEmpty()) {
                    matched++;
                }
            }
        }

        long averageTime =
                (System.nanoTime() - start) / 100;

        return new SearchResult(
                "Aho-Corasick",
                matched,
                averageTime
        );
    }

    // =====================================================
    // SINGLE KEYWORD SEARCH (console)
    // =====================================================

    public void search(String query) {

        query = query.toLowerCase().trim();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("              SINGLE KEYWORD SEARCH");
        System.out.println("==============================================");
        System.out.println("Query: " + query);

        searchNaive(query);
        searchKMP(query);
        searchRabinKarp(query);
        searchZ(query);
    }

    // =====================================================
    // NAIVE
    // =====================================================

    private SearchResult searchNaive(String query) {

        int matches = 0;

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (document doc : documents) {

                NaiveSearch.search(
                        doc.getContent().toLowerCase(),
                        query
                );
            }
        }

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            for (document doc : documents) {

                int result =
                        NaiveSearch.search(
                                doc.getContent().toLowerCase(),
                                query
                        );

                if (i == 0 && result != -1) {
                    matches++;
                }
            }
        }

        long end = System.nanoTime();

        long averageTime =
                (end - start) / 100;

        System.out.println("\nNaive Search");
        System.out.println("Matches       : " + matches);
        System.out.println("Average Time  : " + averageTime + " ns");

        return new SearchResult(
                "Naive Search",
                matches,
                averageTime
        );
    }

    // =====================================================
    // KMP
    // =====================================================

    private SearchResult searchKMP(String query) {

        int matches = 0;

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (document doc : documents) {

                KMPSearch.search(
                        doc.getContent().toLowerCase(),
                        query
                );
            }
        }

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            for (document doc : documents) {

                int result =
                        KMPSearch.search(
                                doc.getContent().toLowerCase(),
                                query
                        );

                if (i == 0 && result != -1) {
                    matches++;
                }
            }
        }

        long end = System.nanoTime();

        long averageTime =
                (end - start) / 100;

        System.out.println("\nKMP Search");
        System.out.println("Matches       : " + matches);
        System.out.println("Average Time  : " + averageTime + " ns");

        return new SearchResult(
                "KMP",
                matches,
                averageTime
        );
    }

    // =====================================================
    // RABIN-KARP
    // =====================================================

    private SearchResult searchRabinKarp(String query) {

        int matches = 0;

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (document doc : documents) {

                RabinKarp.search(
                        doc.getContent().toLowerCase(),
                        query
                );
            }
        }

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            for (document doc : documents) {

                int result =
                        RabinKarp.search(
                                doc.getContent().toLowerCase(),
                                query
                        );

                if (i == 0 && result != -1) {
                    matches++;
                }
            }
        }

        long end = System.nanoTime();

        long averageTime =
                (end - start) / 100;

        System.out.println("\nRabin-Karp Search");
        System.out.println("Matches       : " + matches);
        System.out.println("Average Time  : " + averageTime + " ns");

        return new SearchResult(
                "Rabin-Karp",
                matches,
                averageTime
        );
    }

    // =====================================================
    // SUFFIX ARRAY
    // =====================================================

    /*
     * Suffix Arrays are now cached.
     *
     * The first benchmark/search builds one SuffixArray
     * per document. Future searches reuse those objects.
     *
     * This avoids rebuilding 1460 suffix arrays for every
     * warm-up and benchmark iteration.
     */
    private SearchResult searchSuffixArray(String query) {

        int matches = 0;

        // Build/cache suffix arrays once.
        for (int i = 0; i < documents.length; i++) {

            if (!suffixArrayCache.containsKey(i)) {

                SuffixArray sa =
                        new SuffixArray(
                                documents[i].getContent()
                        );

                suffixArrayCache.put(i, sa);
            }
        }

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (SuffixArray sa :
                    suffixArrayCache.values()) {

                sa.search(query);
            }
        }

        long start = System.nanoTime();

        // Timed search
        for (int i = 0; i < 100; i++) {

            for (SuffixArray sa :
                    suffixArrayCache.values()) {

                int result =
                        sa.search(query);

                if (i == 0 && result != -1) {
                    matches++;
                }
            }
        }

        long end = System.nanoTime();

        long averageTime =
                (end - start) / 100;

        return new SearchResult(
                "Suffix Array",
                matches,
                averageTime
        );
    }

    // =====================================================
    // LCP ARRAY
    // =====================================================

    private void buildLCPForDocuments() {

        for (document doc : documents) {

            SuffixArray sa =
                    new SuffixArray(
                            doc.getContent()
                    );

            new LCPArray(
                    doc.getContent(),
                    sa.getSuffixArray()
            );
        }
    }

    // =====================================================
    // Z ALGORITHM
    // =====================================================

    private SearchResult searchZ(String query) {

        int matches = 0;

        // Warm-up
        for (int i = 0; i < 10; i++) {

            for (document doc : documents) {

                ZAlgorithm.search(
                        doc.getContent().toLowerCase(),
                        query
                );
            }
        }

        long start = System.nanoTime();

        // Timed benchmark
        for (int i = 0; i < 100; i++) {

            for (document doc : documents) {

                int result =
                        ZAlgorithm.search(
                                doc.getContent().toLowerCase(),
                                query
                        );

                if (i == 0 && result != -1) {
                    matches++;
                }
            }
        }

        long end = System.nanoTime();

        long averageTime =
                (end - start) / 100;

        System.out.println("\nZ Algorithm");
        System.out.println("Matches       : " + matches);
        System.out.println("Average Time  : " + averageTime + " ns");

        return new SearchResult(
                "Z Algorithm",
                matches,
                averageTime
        );
    }

    // =====================================================
    // AHO-CORASICK (console)
    // =====================================================

    public void multiKeywordSearch(String[] keywords) {

        AhoCorasick ahoCorasick =
                new AhoCorasick();

        long buildStart =
                System.nanoTime();

        for (String keyword : keywords) {

            keyword = keyword.trim().toLowerCase();

            if (!keyword.isEmpty()) {
                ahoCorasick.addKeyword(keyword);
            }
        }

        ahoCorasick.build();

        long buildTime =
                System.nanoTime() - buildStart;

        long searchStart =
                System.nanoTime();

        int documentsMatched = 0;

        for (document doc : documents) {

            Set<String> foundKeywords =
                    ahoCorasick.search(
                            doc.getContent().toLowerCase()
                    );

            if (!foundKeywords.isEmpty()) {

                documentsMatched++;

                System.out.println(
                        "\nDocument: " + doc.getTitle()
                );

                System.out.println(
                        "Keywords Found: " + foundKeywords
                );
            }
        }

        long searchTime =
                System.nanoTime() - searchStart;

        System.out.println();

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "          AHO-CORASICK SEARCH"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Keywords          : "
                        + String.join(", ", keywords)
        );

        System.out.println(
                "Documents Matched : "
                        + documentsMatched
        );

        System.out.println(
                "Build Time        : "
                        + buildTime + " ns"
        );

        System.out.println(
                "Search Time       : "
                        + searchTime + " ns"
        );

        System.out.println(
                "Total Time        : "
                        + (buildTime + searchTime)
                        + " ns"
        );
    }
}