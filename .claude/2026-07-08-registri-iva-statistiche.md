# 2026-07-08 — Statistiche IVA (patch 2)

/contabilita/registri-iva/statistiche on top of the registers patch.
Source: U_IVA_TO (period totals precomputed by the legacy bollati load),
grain soc+anno+mese+codiva+clifor+fatnot.

- Filters: lato (vendite = CLIFOR C,D; acquisti = F,A; cee = E,R; tutti)
  and month.
- Two aggregated views: per month and per IVA code — in BOTH, fatture
  (F), note di accredito (N) and corrispettivi (C) are kept as separate
  rows: the source stores absolute amounts per type and the sign
  conventions for netting F−N are business logic (the liquidazione),
  which is Opus's — the viewer deliberately does NOT net them.
- A raw-detail <details> table shows the U_IVA_TO grain transparently
  with decoded lato labels (Clienti / Fornitori / Fatt. differite / CEE /
  Reverse charge / Autofatture).
- Nav pills link the three registers and the stats page both ways.

## To confirm at first deploy
That U_IVA_TO is populated at Impresind (it is written by every bollati
load); whether the E/R rows should be listed under vendite or acquisti
for Impresind's flows (they are written by both loaders depending on the
CEE flag of the IVA code — the dedicated 'cee' filter sidesteps the
ambiguity honestly).
