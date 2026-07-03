/**
 * aq-tree.js — componente tree-view riusabile di AquariusWeb.
 *
 * Progettato per gerarchie grandi (testato su ~7000 nodi): il DOM dei figli
 * viene creato solo alla prima espansione (lazy), il filtro lavora
 * sull'array dati e ricostruisce solo il sottoalbero visibile.
 *
 * Consuma il payload di /conti/tree-data (o qualsiasi payload compatibile):
 *   { meta: {...}, nodes: [[id, code, descr, tipo, posbil, level, parentIdx], ...] }
 *
 * Riusabile per qualunque gerarchia futura (centri di costo, distinte base,
 * gruppi merceologici): basta servire lo stesso formato.
 *
 * API:
 *   const tree = AqTree.mount(containerEl, payload, { href: id => url });
 *   tree.expandToLevel(1|2); tree.expandAll(); tree.collapseAll();
 *   tree.filter("cassa"); tree.revealCode("0010100000042");
 */
(function (global) {
    'use strict';

    const F = { ID: 0, CODE: 1, DESCR: 2, TYPE: 3, POSBIL: 4, LEVEL: 5, PARENT: 6 };

    function mount(container, payload, opts) {
        opts = opts || {};
        const nodes = payload.nodes || [];
        const n = nodes.length;
        const hrefFn = opts.href || (id => '#');

        // ── Indici ──────────────────────────────────────────────────────
        const children = new Array(n).fill(null);   // idx → [childIdx]
        const roots = [];
        for (let i = 0; i < n; i++) {
            const p = nodes[i][F.PARENT];
            if (p >= 0 && p < n) {
                (children[p] || (children[p] = [])).push(i);
            } else {
                roots.push(i);
            }
        }
        // Conteggio discendenti foglia (per i badge dei nodi strutturali)
        const leafCount = new Array(n).fill(0);
        for (let i = n - 1; i >= 0; i--) {  // nodes è ordinato per code: i figli vengono dopo i padri… non garantito; uso doppio pass
            if (!children[i]) leafCount[i] = 1;
        }
        // pass bottom-up generico (ordina per profondità non necessario: iterazione fino a fixpoint sarebbe lenta;
        // calcolo con post-order esplicito)
        (function computeCounts() {
            const stack = roots.map(r => [r, false]);
            while (stack.length) {
                const [idx, processed] = stack.pop();
                const kids = children[idx];
                if (!kids) { leafCount[idx] = 1; continue; }
                if (!processed) {
                    stack.push([idx, true]);
                    for (const k of kids) stack.push([k, false]);
                } else {
                    let sum = 0;
                    for (const k of kids) sum += leafCount[k];
                    leafCount[idx] = sum;
                }
            }
        })();

        const byCode = new Map();
        for (let i = 0; i < n; i++) byCode.set(nodes[i][F.CODE], i);

        // ── Rendering ───────────────────────────────────────────────────
        const ul = document.createElement('ul');
        ul.className = 'aqt-root';
        container.innerHTML = '';
        container.appendChild(ul);

        const liByIdx = new Array(n).fill(null);

        function esc(s) {
            return String(s).replace(/[&<>"]/g,
                c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
        }

        function badge(node) {
            const t = node[F.TYPE], b = node[F.POSBIL];
            let html = '';
            if (b === 'P') html += '<span class="aqt-badge aqt-b-pat" title="Patrimoniale">P</span>';
            if (b === 'E') html += '<span class="aqt-badge aqt-b-eco" title="Economico">E</span>';
            if (t === 'C') html += '<span class="aqt-badge aqt-b-cli" title="Clienti">CLI</span>';
            if (t === 'F') html += '<span class="aqt-badge aqt-b-for" title="Fornitori">FOR</span>';
            if (t === 'I') html += '<span class="aqt-badge aqt-b-iva" title="IVA">IVA</span>';
            return html;
        }

        function buildLi(idx, highlight) {
            const node = nodes[idx];
            const kids = children[idx];
            const li = document.createElement('li');
            li.className = 'aqt-node aqt-lvl' + node[F.LEVEL];
            li.dataset.idx = idx;

            const toggleHtml = kids
                ? '<span class="aqt-toggle" role="button" aria-expanded="false"><i class="bi bi-chevron-right"></i></span>'
                : '<span class="aqt-spacer"></span>';
            const iconHtml = kids
                ? '<i class="bi bi-folder2 aqt-icon"></i>'
                : '<i class="bi bi-circle-fill aqt-dot"></i>';
            const countHtml = kids
                ? `<span class="aqt-count" title="Conti foglia nel ramo">${leafCount[idx]}</span>`
                : '';

            let code = esc(node[F.CODE]);
            let descr = esc(node[F.DESCR] || '—');
            if (highlight) {
                const re = new RegExp('(' + highlight.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ')', 'ig');
                code = code.replace(re, '<mark>$1</mark>');
                descr = descr.replace(re, '<mark>$1</mark>');
            }

            li.innerHTML =
                `<div class="aqt-row">${toggleHtml}${iconHtml}` +
                `<a class="aqt-link" href="${hrefFn(node[F.ID])}">` +
                `<code class="aqt-code">${code}</code>` +
                `<span class="aqt-descr">${descr}</span></a>` +
                `${badge(node)}${countHtml}</div>`;

            liByIdx[idx] = li;
            return li;
        }

        function expand(li) {
            const idx = +li.dataset.idx;
            const kids = children[idx];
            if (!kids) return;
            let sub = li.querySelector(':scope > ul');
            if (!sub) {  // lazy: crea il DOM dei figli alla prima apertura
                sub = document.createElement('ul');
                sub.className = 'aqt-children';
                for (const k of kids) sub.appendChild(buildLi(k));
                li.appendChild(sub);
            }
            sub.style.display = '';
            const t = li.querySelector(':scope > .aqt-row .aqt-toggle');
            if (t) t.setAttribute('aria-expanded', 'true');
        }

        function collapse(li) {
            const sub = li.querySelector(':scope > ul');
            if (sub) sub.style.display = 'none';
            const t = li.querySelector(':scope > .aqt-row .aqt-toggle');
            if (t) t.setAttribute('aria-expanded', 'false');
        }

        function renderRoots() {
            ul.innerHTML = '';
            liByIdx.fill(null);
            for (const r of roots) ul.appendChild(buildLi(r));
        }

        // Toggle via event delegation
        container.addEventListener('click', e => {
            const toggle = e.target.closest('.aqt-toggle');
            if (!toggle) return;
            const li = toggle.closest('.aqt-node');
            const isOpen = toggle.getAttribute('aria-expanded') === 'true';
            isOpen ? collapse(li) : expand(li);
        });

        // ── API pubblica ────────────────────────────────────────────────
        function walk(li, fn) {
            fn(li);
            const sub = li.querySelector(':scope > ul');
            if (sub) for (const child of sub.children) walk(child, fn);
        }

        function expandToLevel(maxLevel) {
            renderRoots();
            // BFS espandendo i nodi con level < maxLevel… i mastri sono level 1:
            // expandToLevel(1) = mostra mastri espansi fino ai sottogruppi.
            const expandRec = li => {
                const idx = +li.dataset.idx;
                if (nodes[idx][F.LEVEL] <= maxLevel && children[idx]) {
                    expand(li);
                    const sub = li.querySelector(':scope > ul');
                    if (sub) for (const c of sub.children) expandRec(c);
                }
            };
            for (const li of ul.children) expandRec(li);
        }

        function expandAll() { expandToLevel(99); }
        function collapseAll() { renderRoots(); }

        let filterTimer = null;
        function filter(q) {
            clearTimeout(filterTimer);
            filterTimer = setTimeout(() => doFilter(q), 150);
        }

        function doFilter(q) {
            q = (q || '').trim().toLowerCase();
            if (!q) { renderRoots(); notice(''); return; }

            const MAX = 800;
            const matched = [];
            for (let i = 0; i < n && matched.length < MAX; i++) {
                if (nodes[i][F.CODE].toLowerCase().includes(q) ||
                    (nodes[i][F.DESCR] || '').toLowerCase().includes(q)) {
                    matched.push(i);
                }
            }
            // Visibili = match + tutti gli antenati
            const visible = new Set(matched);
            for (const m of matched) {
                let p = nodes[m][F.PARENT];
                while (p >= 0 && !visible.has(p)) { visible.add(p); p = nodes[p][F.PARENT]; }
            }
            // Render filtrato: solo i rami visibili, tutto espanso
            ul.innerHTML = '';
            function renderFiltered(idxList, parentEl) {
                for (const i of idxList) {
                    if (!visible.has(i)) continue;
                    const li = buildLi(i, q);
                    parentEl.appendChild(li);
                    const kids = children[i];
                    if (kids) {
                        const visKids = kids.filter(k => visible.has(k));
                        if (visKids.length) {
                            const sub = document.createElement('ul');
                            sub.className = 'aqt-children';
                            li.appendChild(sub);
                            const t = li.querySelector('.aqt-toggle');
                            if (t) t.setAttribute('aria-expanded', 'true');
                            renderFiltered(visKids, sub);
                        }
                    }
                }
            }
            renderFiltered(roots, ul);
            notice(matched.length >= MAX
                ? `Primi ${MAX} risultati mostrati — restringi la ricerca`
                : `${matched.length} risultati`);
        }

        let noticeEl = null;
        function notice(text) {
            if (!noticeEl) {
                noticeEl = document.createElement('div');
                noticeEl.className = 'aqt-notice';
                container.parentNode.insertBefore(noticeEl, container);
            }
            noticeEl.textContent = text;
            noticeEl.style.display = text ? '' : 'none';
        }

        /** Espande il percorso fino a un codice e lo evidenzia (deep-link). */
        function revealCode(code) {
            const idx = byCode.get(code);
            if (idx === undefined) return false;
            const path = [];
            let p = nodes[idx][F.PARENT];
            while (p >= 0) { path.unshift(p); p = nodes[p][F.PARENT]; }
            renderRoots();
            for (const a of path) {
                const li = liByIdx[a];
                if (li) expand(li);
            }
            const target = liByIdx[idx];
            if (target) {
                target.scrollIntoView({ block: 'center', behavior: 'smooth' });
                target.querySelector('.aqt-row').classList.add('aqt-flash');
                setTimeout(() => {
                    const r = target.querySelector('.aqt-row');
                    if (r) r.classList.remove('aqt-flash');
                }, 2500);
            }
            return true;
        }

        // ── Avvio: mastri visibili, chiusi ──────────────────────────────
        renderRoots();

        // Deep-link: #c=CODICE
        const hash = new URLSearchParams((location.hash || '').replace(/^#/, ''));
        if (hash.get('c')) revealCode(hash.get('c'));

        return { expandAll, collapseAll, expandToLevel, filter, revealCode, meta: payload.meta };
    }

    global.AqTree = { mount };
})(window);
