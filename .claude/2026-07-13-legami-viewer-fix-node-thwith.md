# 2026-07-13 — Viewer legami: vista nodo vuota — RISOLTO (pattern th:with)

Rimuovere l'SVG (0.23.2) non aveva risolto: la vista nodo restava vuota per TUTTI
i nodi (PRG e MENU). Causa reale nel template: nel rewrite 0.23.0 avevo cambiato
il rendering di genitori/figli da:
  th:each="pe : ${parents}" th:with="l=${pe[0]}, o=${pe[1]}"  (pattern Fable5 OK)
a un accesso DIRETTO all'array con proprietà + link multi-parametro con spazio:
  th:each="p : ${parents}" ... ${p[1].id} ... @{...(id=${p[1].id}, trail=...)}
Quella combinazione (indice diretto su Object[] con elemento Link + spazio nella
link expression multi-parametro) mandava in errore il rendering del blocco nodo.

FIX: ripristinato il pattern th:with dell'originale per genitori e figli
(estrazione l/o e cl/co) e rimosso lo spazio dopo la virgola nelle link expression
(@{/utilita/legami(id=${o.id},trail=${trailNext})}). La griglia (r[0].*) resta
invariata perché usa un solo parametro senza virgola e funziona.

Restano tutte le migliorie: griglia con tutti i dati, contatori-filtro, paginazione,
ricerca in-linea, fix menu-orfani (ingresso), title dei form. SVG inline non
ripristinato: il grafo tornerà come endpoint isolato.

Versione 0.23.3. Solo check sintattici (sandbox senza Maven).
