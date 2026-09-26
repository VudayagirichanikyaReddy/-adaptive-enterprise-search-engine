/* Analytics dashboard: renders real benchmark data from the Java backend. */

(() => {
    const NS_PER_MS = 1e6;

    const ms = ns => ns / NS_PER_MS;

    const fmt = v => Number(v).toFixed(3);

    function render(d, benchmark) {

        const A = benchmark.benchmarks || [];

        if (!A.length) {
            $("#empty").hidden = false;
            $("#live").hidden = true;
            return;
        }

        $("#empty").hidden = true;
        $("#live").hidden = false;

        const fastest = A.reduce(
            (m, a) => a.averageTimeNs < m.averageTimeNs ? a : m
        );

        const slowest = Math.max(
            ...A.map(a => a.averageTimeNs)
        );

        const savedAt = d.savedAt
            ? new Date(d.savedAt).toLocaleTimeString()
            : new Date().toLocaleTimeString();

        $("#summary").innerHTML =
            `Query: <b>“${esc(d.query)}”</b> · updated ${savedAt}`;

        $("#stats").innerHTML = [
            ["Search query", esc(d.query)],
            ["Documents searched", d.documentsSearched],
            ["Overall search time", fmt(d.searchTimeMs) + " ms"],
            [
                "Fastest algorithm",
                `${fastest.algorithm} · ${fmt(ms(fastest.averageTimeNs))} ms`
            ]
        ]
            .map(s =>
                `<div class="card stat">
                <span class="muted">${s[0]}</span>
                <b style="font-size:20px;word-break:break-word">${s[1]}</b>
            </div>`
            )
            .join("");

        /* Average execution time */

        $("#bars").innerHTML = A.map(a => {

            const width = slowest > 0
                ? (a.averageTimeNs / slowest) * 100
                : 0;

            return `
                <div style="margin-bottom:14px">
                    <div class="meta"
                         style="justify-content:space-between;color:var(--ink)">
                        <span>${a.algorithm}</span>
                        <span>${fmt(ms(a.averageTimeNs))} ms</span>
                    </div>

                    <div class="bar">
                        <i style="width:${width}%"></i>
                    </div>
                </div>
            `;

        }).join("");

        /* Relative speed chart */

        const W = 560;
        const H = 240;
        const P = 10;
        const slot = (W - P) / A.length;
        const bw = 56;

        const rel = A.map(a =>
            slowest / Math.max(a.averageTimeNs, 1)
        );

        const top = Math.max(...rel);

        $("#lines").innerHTML =
            `<svg class="chart"
                  viewBox="0 0 ${W} ${H}"
                  role="img"
                  aria-label="Speed relative to the slowest algorithm">` +

            A.map((a, i) => {

                const h = rel[i] / top * (H - 70);

                const x =
                    P +
                    i * slot +
                    slot / 2 -
                    bw / 2;

                const y =
                    H -
                    30 -
                    h;

                return `
                    <rect
                        x="${x}"
                        y="${y}"
                        width="${bw}"
                        height="${h}"
                        rx="6"
                        fill="${
                    a.algorithm === fastest.algorithm
                        ? "var(--ok)"
                        : "var(--accent)"
                }"
                    />

                    <text
                        x="${x + bw / 2}"
                        y="${y - 6}"
                        text-anchor="middle">
                        ${rel[i].toFixed(2)}×
                    </text>

                    <text
                        x="${x + bw / 2}"
                        y="${H - 10}"
                        text-anchor="middle">
                        ${a.algorithm}
                    </text>
                `;

            }).join("") +

            `</svg>`;

        /* Detailed table */

        $("#table").innerHTML =
            `<tr>
                <th>Algorithm</th>
                <th>Documents Matched</th>
                <th>Average Time</th>
            </tr>` +

            A.map(a =>
                `<tr>
                    <td>${a.algorithm}</td>
                    <td>${a.documentsMatched}</td>
                    <td>
                        ${fmt(ms(a.averageTimeNs))} ms
                        <span class="muted">
                            (${a.averageTimeNs.toLocaleString()} ns)
                        </span>
                    </td>
                </tr>`
            ).join("");
    }


    /* Get the latest search */

    const latest = Api.getLatest();

    if (!latest) {
        $("#empty").hidden = false;
        return;
    }


    /* Run benchmark separately */

    async function loadBenchmark(query) {

        const res = await fetch(
            `http://localhost:8080/benchmark?q=${encodeURIComponent(query)}`
        );

        if (!res.ok) {
            throw new Error(
                "Benchmark request failed: " + res.status
            );
        }

        return await res.json();
    }


    /* Initial analytics load */

    (async () => {

        try {

            $("#err").hidden = true;

            const benchmark =
                await loadBenchmark(latest.query);

            render(latest, benchmark);

        } catch (e) {

            console.error(e);

            $("#err").hidden = false;

            $("#err").textContent =
                "Cannot load benchmark data. Make sure ApiServerMain is running.";

        }

    })();


    /* Re-run benchmark */

    $("#rerun").onclick = async () => {

        const btn = $("#rerun");

        btn.disabled = true;

        $("#err").hidden = true;

        try {

            const benchmark =
                await loadBenchmark(latest.query);

            render(latest, benchmark);

        } catch (e) {

            console.error(e);

            $("#err").hidden = false;

            $("#err").textContent =
                "Cannot reach the benchmark backend. Start ApiServerMain and try again.";

        }

        btn.disabled = false;
    };

})();