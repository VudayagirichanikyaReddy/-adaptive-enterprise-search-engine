/* API LAYER - talks to the Java backend (SearchApiServer). */
const API_BASE = "http://localhost:8080";
const LATEST_KEY = "ese-latest-search";   // shared state between Search and Analytics pages

const Api = {
    async search(q, {type = "All", sort = "relevance"} = {}) {
        const res = await fetch(`${API_BASE}/search?q=${encodeURIComponent(q)}`);
        if (!res.ok) throw new Error("Backend returned " + res.status);
        const d = await res.json();

        // Save the raw backend response so the Analytics page can use it.
        Api.saveLatest(d);

        const results = d.results.map(r => ({
            id: r.documentId,
            title: r.title,
            snippet: r.snippet + "…",
            algorithm: r.algorithm,
            matches: r.matches,
            execMs: r.executionTimeMs,
            // Not provided by the backend yet - placeholders so the UI still renders:
            type: "TXT", category: "—", date: "—",
            relevance: Math.min(99, 40 + r.matches * 9)
        }));
        results.sort((a, b) => b.relevance - a.relevance);

        const patternLabel = d.queryType === "MULTI_PATTERN" ? "Multi-pattern"
            : d.queryType === "SPELL_CORRECTION" ? "Single-pattern (spell-corrected)"
                : "Single-pattern";

        return {
            query: d.query,
            correction: {
                applied: !!d.correctionApplied,
                original: d.originalQuery,
                corrected: d.correctedQuery
            },
            algorithm: {name: d.algorithm, pattern: patternLabel, reason: d.selectionReason},
            total: d.total,
            timeMs: d.searchTimeMs,
            results
        };
    },

    async getDocument(id) {
        const res = await fetch(`${API_BASE}/document?id=${encodeURIComponent(id)}`);
        if (!res.ok) return null;
        const d = await res.json();
        return {id: d.id, title: d.title, text: d.content,
            type: "TXT", category: "—", date: "—", pages: "—"};
    },

    // ---- Latest-search state (sessionStorage survives page changes within the tab) ----
    saveLatest(d) {
        try { sessionStorage.setItem(LATEST_KEY, JSON.stringify({...d, savedAt: Date.now()})); } catch (e) {}
    },
    getLatest() {
        try { return JSON.parse(sessionStorage.getItem(LATEST_KEY)); } catch (e) { return null; }
    }
};