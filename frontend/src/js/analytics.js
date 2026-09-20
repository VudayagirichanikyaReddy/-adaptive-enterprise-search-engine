/* Analytics dashboard: renders the latest real backend response. No mock data. */
(() => {
    const NS_PER_MS = 1e6;
    const ms = ns => ns / NS_PER_MS;
    const fmt = v => v.toFixed(3);

    function render(d) {
        const A = d.benchmarks || [];
        if (!A.length) { $("#empty").hidden = false; $("#live").hidden = true; return; }
        $("#empty").hidden = true;
        $("#live").hidden = false;

        const fastest = A.reduce((m, a) => a.averageTimeNs < m.averageTimeNs ? a : m);
        const slowest = Math.max(...A.map(a => a.averageTimeNs));

        $("#summary").innerHTML = `Query: <b>“${esc(d.query)}”</b> · updated ${new Date(d.savedAt).toLocaleTimeString()}`;

        $("#stats").innerHTML = [
            ["Search query", esc(d.query)],
            ["Documents searched", d.documentsSearched],
            ["Overall search time", fmt(d.searchTimeMs) + " ms"],
            ["Fastest algorithm", `${fastest.algorithm} · ${fmt(ms(fastest.averageTimeNs))} ms`]
        ].map(s => `<div class="card stat"><span class="muted">${s[0]}</span><b style="font-size:20px;word-break:break-word">${s[1]}</b></div>`).join("");

        // Bar chart: average execution time (ns converted to ms)
        $("#bars").innerHTML = A.map(a => `<div style="margin-bottom:14px"><div class="meta" style="justify-content:space-between;color:var(--ink)"><span>${a.algorithm}</span><span>${fmt(ms(a.averageTimeNs))} ms</span></div><div class="bar"><i style="width:${a.averageTimeNs / slowest * 100}%"></i></div></div>`).join("");

        // Column chart: speed relative to the slowest algorithm (derived from the same real numbers)
        const W = 560, H = 240, P = 10, slot = (W - P) / A.length, bw = 56;
        const rel = A.map(a => slowest / Math.max(a.averageTimeNs, 1));
        const top = Math.max(...rel);
        $("#lines").innerHTML = `<svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Speed relative to the slowest algorithm">` +
            A.map((a, i) => {
                const h = rel[i] / top * (H - 70), x = P + i * slot + slot / 2 - bw / 2, y = H - 30 - h;
                return `<rect x="${x}" y="${y}" width="${bw}" height="${h}" rx="6" fill="${a.algorithm === fastest.algorithm ? "var(--ok)" : "var(--accent)"}"/>` +
                    `<text x="${x + bw / 2}" y="${y - 6}" text-anchor="middle">${rel[i].toFixed(2)}×</text>` +
                    `<text x="${x + bw / 2}" y="${H - 10}" text-anchor="middle">${a.algorithm}</text>`;
            }).join("") + `</svg>`;

        $("#table").innerHTML = `<tr><th>Algorithm</th><th>Documents Matched</th><th>Average Time</th></tr>` +
            A.map(a => `<tr><td>${a.algorithm}</td><td>${a.documentsMatched}</td><td>${fmt(ms(a.averageTimeNs))} ms <span class="muted">(${a.averageTimeNs.toLocaleString()} ns)</span></td></tr>`).join("");
    }

    const latest = Api.getLatest();
    if (!latest) { $("#empty").hidden = false; return; }   // no search run yet
    render(latest);

    // Re-run the same query against the backend to refresh the numbers
    $("#rerun").onclick = async () => {
        const btn = $("#rerun"); btn.disabled = true; $("#err").hidden = true;
        try {
            await Api.search(latest.query);          // saves the fresh response
            render(Api.getLatest());
        } catch (e) {
            $("#err").hidden = false;
            $("#err").textContent = "Cannot reach the search backend. Start ApiServerMain and try again.";
        }
        btn.disabled = false;
    };
})();