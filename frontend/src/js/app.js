/* Shared shell + small pages (Algorithms, Documents, About). */
const $ = (s, r = document) => r.querySelector(s);
const ICON = {search:'<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',doc:'<path d="M6 3h8l4 4v14H6z"/><path d="M14 3v4h4"/>',chart:'<path d="M4 20V10M10 20V4M16 20v-8M22 20H2"/>',cpu:'<rect x="6" y="6" width="12" height="12" rx="2"/><path d="M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4"/>',info:'<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.01"/>',moon:'<path d="M20 14A8 8 0 1110 4a7 7 0 0010 10z"/>',mic:'<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0014 0M12 18v3"/>',clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',trend:'<path d="M3 17l6-6 4 4 8-8"/>'};
const ic = n => `<svg class="i" viewBox="0 0 24 24">${ICON[n]}</svg>`;
const esc = s => s.replace(/[&<>]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]));
const highlight = (text, q) => {
    const terms = q.split(/\s+/).filter(Boolean).map(t => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
    const safe = esc(text);
    return terms.length ? safe.replace(new RegExp("(" + terms.join("|") + ")", "gi"), "<mark>$1</mark>") : safe;
};
const goSearch = q => { if (q.trim()) location.href = "search.html?q=" + encodeURIComponent(q.trim()); };

function mountShell() {
    const page = document.body.dataset.page;
    const links = [["Search","index.html","search","search"],["Documents","documents.html","doc","documents"],["Analytics","analytics.html","chart","analytics"],["Algorithms","algorithms.html","cpu","algorithms"],["About","about.html","info","about"]];
    $("#nav").innerHTML = `<div class="wrap"><a class="brand" href="index.html">${ic("search")}<span>Enterprise Search Engine</span></a><nav aria-label="Main">${links.map(l => `<a href="${l[1]}" class="${l[3] === page ? "on" : ""}">${ic(l[2])}<span>${l[0]}</span></a>`).join("")}</nav><button id="theme" aria-label="Toggle theme">${ic("moon")}</button></div>`;
    const set = t => { document.documentElement.dataset.theme = t; try { localStorage.setItem("ese-theme", t); } catch (e) {} };
    try { set(localStorage.getItem("ese-theme") || "light"); } catch (e) {}
    $("#theme").onclick = () => set(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
}

function renderAlgorithms() {
    $("#app").innerHTML = `<div class="wrap"><h2 class="pt">Algorithms</h2><p class="muted">The DSA techniques behind the search engine.</p><div class="cards">${ALGOS.map(a => `<article class="card"><h3>${a.name}</h3><p class="muted" style="margin:0">${a.purpose}</p><dl><dt>Complexity</dt><dd><span class="tag">${a.time}</span></dd><dt>Used for</dt><dd>${a.use}</dd></dl></article>`).join("")}</div></div>`;
}
function renderDocuments() {
    $("#app").innerHTML = `<div class="wrap"><h2 class="pt">Documents</h2><p class="muted">${DOCS.length} indexed documents (mock data).</p><div class="card" style="margin:20px 0 60px;overflow-x:auto"><table><tr><th>Title</th><th>Type</th><th>Category</th><th>Pages</th><th>Date</th></tr>${DOCS.map(d => `<tr><td><a href="document.html?id=${d.id}" style="color:var(--accent)">${d.title}</a></td><td><span class="tag">${d.type}</span></td><td>${d.category}</td><td>${d.pages}</td><td>${d.date}</td></tr>`).join("")}</table></div></div>`;
}
function renderAbout() {
    $("#app").innerHTML = `<div class="wrap" style="max-width:720px">
        <h2 class="pt">About</h2>
        <div class="card" style="margin:20px 0 60px">
            <p>
                Adaptive Enterprise Search Engine is a Java-based search system
                developed as a B.Tech Data Structures and Algorithms project.
                It dynamically selects suitable searching techniques based on
                the user's query and supports single-pattern and multi-pattern
                searching using Naive Search, KMP, Rabin-Karp, Z Algorithm,
                Aho-Corasick, and Suffix Array.
            </p>
            <p>
                The system also includes spell correction, ranked results,
                algorithm benchmarking, and performance analytics. The goal is
                to demonstrate how different data structures and string-matching
                algorithms can work together to create an adaptive document
                retrieval system.
            </p>
            <p class="muted">
                The search engine is powered by a Java backend and uses the
                CISI information-retrieval dataset containing 1,460 indexed
                documents.
            </p>
        </div>
    </div>`;
}

mountShell();
const pg = document.body.dataset.page;
if (!document.body.dataset.view) ({algorithms: renderAlgorithms, documents: renderDocuments, about: renderAbout}[pg] || (() => {}))();