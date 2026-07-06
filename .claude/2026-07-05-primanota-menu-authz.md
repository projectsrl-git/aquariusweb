# 2026-07-05 — Prima nota: la voce unificata rispetta le abilitazioni UTENTI

## Problema
Trasformando il sottomenu "Prima nota" in una voce unica, avevo perso il
filtro di autorizzazione: la voce compariva a chiunque vedesse il menu
Contabilità, anche a utenti che nel legacy NON erano abilitati ad alcuna
voce di prima nota (il tbl_menu filtra per campo UTENTI, per foglia, a DB).

## Correzione
Nella trasformazione (toNodeFromL2 / isPrimaNotaContainer), la lista
`children` contiene SOLO le foglie che l'utente può vedere (già filtrate per
UTENTI dal LegacyMenuRepository). Ora la voce unificata viene generata solo se
c'è almeno una foglia non-separatore visibile; altrimenti il container
restituisce null (il chiamante già lo gestisce, if l2Node != null). Così un
utente non abilitato alla prima nota non vede la voce.

## Granularità per-azione (rimandata, per decisione)
La mappa foglia→capability (READ/CREATE/UPDATE/DELETE/LOCK) e le azioni
condizionate nella pagina saranno costruite con la CRUD di scrittura, quando
le azioni esistono davvero. Oggi la voce apre la sola consultazione read-only,
che corrisponde al permesso di ricerca/consultazione del legacy. Enforcement
server-side degli endpoint di scrittura idem: alla slice data-entry.

## Verifica (sandbox)
- LegacyMenu.isSeparator() esiste → check type-safe.
- Chiamante gestisce già il return null.
- Ramo non-primanota invariato.
- Java brace/paren OK; ribasata su supplier+accounting+primanota-menu:
  git apply --check pulito.

## Non verificato (deploy)
- Con utente reale abilitato solo a "Ricerca prima nota": la voce compare e
  apre la consultazione. Con utente non abilitato: la voce non compare.
- Cache menu: riavvio/evict per applicare.
