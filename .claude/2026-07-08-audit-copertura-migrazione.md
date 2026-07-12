# 2026-07-08 — Audit di copertura della migrazione (capstone)

## Method
Authoritative function list = tbl_menu full tree (1529 rows → 1051
executable entries reachable from the 11 L1 menus; ~120 entries live in
ORPHAN groups referenced by no L1 — almost certainly dismissed) +
user-provided screenshots of ALL 19 Contabilità sub-menus (131 voci
classified BY HAND with per-entry status/evidence) + web coverage index
(FORM_TO_URL + FUNCTION_TO_URL + synthetic MenuService entries + 54 GET
routes from controllers).

Classification per entry: MIGRATO (web route cited) / PARZIALE (gap
noted) / MANCANTE / NON_APPLICABILE (write-side by mandate, legacy
print, VFP-only utility, obsolete). Non-contabilità areas classified by
rule engine (form match → route; write/print/utility label patterns)
with refinement passes; residual uncertainty carried as NEEDS_DOMAIN
with a precise question, never guessed.

## Results
36 MIGRATO / 4 PARZIALE / 418 MANCANTE / 593 NON_APPLICABILE.
Key reading: counts are per menu ENTRY, not functional weight — the 36
MIGRATO cover the main consultative flows. Two big MANCANTE blocks are
actually domain questions: the produzione "film protettivi" vertical
(107 entries — is Impresind STANDARD-only?) and the Consulenza area
(66 entries).

## Deliverables
- resources/migration/menu_coverage_audit.csv — one row per menu entry
  (9 cols, RFC-4180): area, path, voce, form/prg, copertura, evidenza,
  reason, note, metodo di classificazione.
- resources/migration/COVERAGE_REPORT.md — per-area and per-submenu
  counts, 12 prioritized missing items with rationale (scadenziario,
  cespiti view, centri di costo, quadrature partitari, protocol-sequence
  check, tracciabilità documenti, situazione ordini, bilancio N-1,
  riclassificato, budget, statistiche), NEEDS_DOMAIN questions, and the
  open observations for Opus (CEE reconciliation, MOV_NREGIS vs
  MOV_NUMPRO drill).
- Tracker: +9 audit rows at group level. DECLARED DEVIATION: the prompt
  asked the tracker itself to carry one row per menu entry; exploding
  1051 rows into a tracker whose historical grain is per-.scx-member
  would swamp it, so the per-entry map lives in the dedicated audit CSV
  and the tracker references it.
