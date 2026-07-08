# 2026-07-08 — Bilancio CEE: viewer struttura + mappatura (patch 2)

## What
Read-only viewer at /contabilita/bilancio-cee-struttura showing the CEE
prospetto STRUCTURE (no computed values — the engine is Opus's):
- BILNEW rows in legacy print order (BIL_CODRIG), with I/V/T badges
  (verbatim legend of menu_cee000) and SP/CE section derived from the
  legacy 21600 threshold;
- per V-row: the accounts flowing in from U_INT_TT (dare + avere, with an
  "avere" badge on the alternative rows — negative-balance rule);
- per T-row: its composition from U_COR_TT (signed edges) and, for every
  row, where it flows into;
- anomalies panel = the three ceecont checks (unmapped accounts — capped
  at 200 with total count; mappings to non-existent rows; broken total
  edges, flagged as calculation-blocking).

## How
CeeStructureDao (NamedParameterJdbcTemplate on tenantDataSource, pattern
of DocumentArchiveDao): structure/mappings/totalEdges + the three anomaly
queries; PADL emulated with RIGHT(REPLICATE(' ',10)+LTRIM(RTRIM(x)),10)
as in the legacy comparisons. Controller builds in-memory indexes
(voce→conti, totale→componenti, riga→destinazioni) and an optional text
filter on code/description. Menu: FORM_TO_URL menu_cee000 / menu_ceepdc /
menu_ceetot / menu_ceecori / menu_ceeric → the viewer (the legacy leaves
are all structure/mapping consultation-or-edit forms; edits stay legacy).

## To confirm at first deploy
Maven build; BILNEW/U_INT_TT/U_COR_TT volumes at Impresind (queries are
per-society, expected small); whether CONTI join year semantics matches
(mappings show current-year descriptions with INT_DESCRI fallback);
rendering of <details> panels with many mapped accounts.
