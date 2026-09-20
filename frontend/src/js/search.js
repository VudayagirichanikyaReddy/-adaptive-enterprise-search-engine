/* Home, results and document-view pages. Uses Api only, never DOCS directly. */
const params = new URLSearchParams(location.search);
const RECENT_KEY = "ese-recent";
const getRecent = () => { try { return JSON.parse(localStorage.getItem(RECENT_KEY)) || []; } catch (e) { return []; } };
const saveRecent = q => { try { localStorage.setItem(RECENT_KEY, JSON.stringify([q, ...getRecent().filter(x => x !== q)].slice(0, 5))); } catch (e) {} };

function initSearchBox(value = "") {
    const box = $("#sbox");
    box.innerHTML = `<form class="sbar" role="search">${ic("search")}<input id="q" autocomplete="off" placeholder="Search documents, policies, notes…" aria-label="Search" value="${esc(value)}"><button type="button" aria-label="Voice search (visual only)">${ic("mic")}</button><button class="btn p">Search</button></form><div class="sugg" id="sugg" hidden></div>`;
    const q = $("#q"), sg = $("#sugg");
    q.oninput = () => { // suggestions come from popular searches + titles (backend: /api/suggest)
        const v = q.value.toLowerCase().trim();
        const list = v ? [...POPULAR, ...DOCS.map(d => d.title)].filter(s => s.toLowerCase().includes(v)).slice(0, 5) : [];
        sg.hidden = !list.length;
        sg.innerHTML = list.map(s => `<div>${ic("search")}${highlight(s, v)}</div>`).join("");
        sg.querySelectorAll("div").forEach(d => d.onclick = () => goSearch(d.textContent));
    };
    $("form", box).onsubmit = e => { e.preventDefault(); goSearch(q.value); };
    document.addEventListener("click", e => { if (!box.contains(e.target)) sg.hidden = true; });
}
const chips = (arr, ico) => arr.map(s => `<a class="chip" href="search.html?q=${encodeURIComponent(s)}">${ic(ico)}${s}</a>`).join("");

function homePage() {
    initSearchBox();
    const recent = getRecent();
    $("#recent").innerHTML = recent.length ? chips(recent, "clock") : '<span class="muted">Your searches will appear here.</span>';
    $("#popular").innerHTML = chips(POPULAR, "trend");
}

async function resultsPage() {
    const q = params.get("q") || "";
    initSearchBox(q);
    if (!q) { $("#results").innerHTML = '<div class="card">Enter a search term to begin.</div>'; return; }
    saveRecent(q);
    const state = {type: "All", sort: "relevance"};
    const run = async () => {
        $("#results").innerHTML = '<p class="muted">Searching…</p>';
        const r = await Api.search(q, state);
        const a = r.algorithm;
        $("#results").innerHTML = `<div class="summary"><div><b>Search Results</b><div class="muted">${r.total} results found in ${r.timeMs} ms for “${esc(q)}”</div></div></div>
    <details class="card algo" open><summary>Algorithm Selected: ${a.name}</summary><p class="muted" style="margin:8px 0 0">Reason: ${a.reason} · Pattern type: ${a.pattern}</p></details>
    ${r.results.map(d => `<a class="card res" href="document.html?id=${d.id}&q=${encodeURIComponent(q)}&algo=${encodeURIComponent(d.algorithm)}"><h3><span>${highlight(d.title, q)}</span></h3><p>${highlight(d.snippet, q)}</p><div class="meta"><span class="tag">${d.type}</span><span>${d.category}</span><span>${d.date}</span><span>Algorithm: ${d.algorithm}</span><span>Matches: ${d.matches}</span><span>Execution: ${d.execMs} ms</span><span class="rel">Relevance: ${d.relevance}%</span></div></a>`).join("") || '<div class="card">No documents match. Try a different term or check spelling.</div>'}`;
    };
    $("#filters").innerHTML = `<div class="card side"><h4>Show</h4>${["All","PDF","DOCX","TXT"].map((t, i) => `<label><input type="radio" name="t" value="${t}" ${i ? "" : "checked"}>${{All:"All",PDF:"PDFs",DOCX:"Documents",TXT:"Text files"}[t]}</label>`).join("")}<h4>Sort by</h4><select id="sort"><option value="relevance">Relevance</option><option value="date">Date (newest)</option></select></div>`;
    document.querySelectorAll("[name=t]").forEach(el => el.onchange = () => { state.type = el.value; run(); });
    $("#sort").onchange = e => { state.sort = e.target.value; run(); };
    run();
}

async function documentPage() {
    const q = params.get("q") || "", d = await Api.getDocument(params.get("id"));
    if (!d) { $("#doc").innerHTML = '<div class="card">Document not found. <a href="documents.html" style="color:var(--accent)">Browse documents</a></div>'; return; }
    const n = q ? q.toLowerCase().split(/\s+/).reduce((c, t) => c + (d.text.toLowerCase().split(t).length - 1), 0) : 0;
    const algo = params.get("algo") || "—";
    $("#doc").innerHTML = `<a class="muted" href="javascript:history.back()">← Back to results</a><h2 class="pt">${d.title}</h2><div class="layout" style="padding-top:12px"><div class="card fulltext">${highlight(d.text, q)}</div><aside class="card"><h3>Search metadata</h3><table><tr><td>Type</td><td>${d.type}</td></tr><tr><td>Category</td><td>${d.category}</td></tr><tr><td>Date</td><td>${d.date}</td></tr><tr><td>Pages</td><td>${d.pages}</td></tr><tr><td>Search term</td><td>${esc(q) || "—"}</td></tr><tr><td>Match count</td><td>${n}</td></tr><tr><td>Relevance</td><td class="rel">${Math.min(99, 40 + n * 9)}%</td></tr><tr><td>Algorithm</td><td>${esc(algo)}</td></tr></table></aside></div>`;
}
const view = document.body.dataset.view;
({home: homePage, results: resultsPage, document: documentPage}[view] || (() => {}))();