package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import documents.document;
import engine.QueryAnalysis;
import engine.SearchEngine;
import results.SearchResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Exposes SearchEngine over HTTP: GET /search?q=...  and  GET /document?id=... */
public class SearchApiServer {

    private final SearchEngine engine;

    public SearchApiServer(SearchEngine engine) {
        this.engine = engine;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/search", this::handleSearch);
        server.createContext("/document", this::handleDocument);
        server.start();
    }

    // ---------------- GET /search?q=machine ----------------
    private void handleSearch(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;

        String q = param(ex, "q");
        if (q == null || q.trim().isEmpty()) {
            send(ex, 400, "{\"error\":\"Missing query parameter q\"}");
            return;
        }
        q = q.trim();

        QueryAnalysis analysis = engine.analyzeQuery(q);

        long t0 = System.nanoTime();
        List<SearchResult> matches = engine.findMatchingDocuments(q, analysis);
        double searchMs = (System.nanoTime() - t0) / 1e6;
        List<SearchResult> benchmarks = engine.runAllAlgorithms(q);

        document[] docs = engine.getDocuments();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"query\":").append(str(q))
                .append(",\"originalQuery\":").append(str(analysis.getOriginalQuery()))
                .append(",\"correctedQuery\":").append(str(analysis.getCorrectedQuery()))
                .append(",\"correctionApplied\":").append(analysis.isCorrectionApplied())
                .append(",\"queryType\":").append(str(analysis.getQueryType()))
                .append(",\"selectedAlgorithm\":").append(str(analysis.getSelectedAlgorithm()))
                .append(",\"selectionReason\":").append(str(analysis.getReason()))
                .append(",\"documentsSearched\":").append(docs.length)
                .append(",\"algorithm\":").append(str(analysis.getAlgorithmDisplayName()))
                .append(",\"total\":").append(matches.size())
                .append(",\"searchTimeMs\":").append(ms(searchMs))
                .append(",\"results\":[");

        for (int i = 0; i < matches.size(); i++) {
            SearchResult r = matches.get(i);
            String content = docs[r.getDocumentId() - 1].getContent();
            String lower = content.toLowerCase();

            // centre the snippet on the earliest pattern hit (corrected word if corrected)
            int first = -1;
            for (String p : analysis.getPatterns()) {
                int at = lower.indexOf(p);
                if (at >= 0 && (first < 0 || at < first)) first = at;
            }
            int from = Math.max(0, first - 60);
            String snippet = content.substring(from, Math.min(content.length(), from + 200))
                    .replaceAll("\\s+", " ");

            if (i > 0) sb.append(',');
            sb.append("{\"documentId\":").append(r.getDocumentId())
                    .append(",\"title\":").append(str(r.getTitle()))
                    .append(",\"snippet\":").append(str(snippet))
                    .append(",\"algorithm\":").append(str(r.getAlgorithm()))
                    .append(",\"matches\":").append(r.getMatches())
                    .append(",\"executionTime\":").append(r.getExecutionTime())
                    .append(",\"executionTimeMs\":").append(ms(r.getExecutionTime() / 1e6))
                    .append('}');
        }

        sb.append("],\"benchmarks\":[");
        for (int i = 0; i < benchmarks.size(); i++) {
            SearchResult b = benchmarks.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"algorithm\":").append(str(b.getAlgorithm()))
                    .append(",\"documentsMatched\":").append(b.getMatches())
                    .append(",\"averageTimeNs\":").append(b.getExecutionTime())
                    .append('}');
        }
        sb.append("]}");

        send(ex, 200, sb.toString());
    }

    // ---------------- GET /document?id=1 ----------------
    private void handleDocument(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        try {
            int id = Integer.parseInt(param(ex, "id"));
            document d = engine.getDocuments()[id - 1];
            send(ex, 200, "{\"id\":" + id + ",\"title\":" + str(d.getTitle())
                    + ",\"content\":" + str(d.getContent()) + "}");
        } catch (RuntimeException e) {
            send(ex, 404, "{\"error\":\"Document not found\"}");
        }
    }

    // ---------------- helpers ----------------
    private boolean preflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private String param(HttpExchange ex, String name) {
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return null;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(name) && kv.length == 2)
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    private void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String ms(double v) { return String.format(Locale.ROOT, "%.3f", v); }

    private String str(String s) {   // JSON string with escaping
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}