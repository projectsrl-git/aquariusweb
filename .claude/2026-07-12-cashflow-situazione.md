# 2026-07-12 — Situazione cash flow (read-only, da partite aperte)

/contabilita/cashflow: the forward pendant of the scadenziario, built
on the SAME repositories (PartitaCliente/FornitoreRepository.findAperte,
residuo=PAR_TOTIM−PAR_PAGATO, scadenza PAR_DTSCAD yyyy/MM/dd) — no new
decoding, per mandate.

Buckets coherent with the scadenziario widths, forward direction:
Scaduto/oggi, 1–30, 31–60, 61–90, oltre 90, PLUS a dedicated "Senza
data" band for missing/malformed due dates — surfaced instead of
silently zeroed. Evidence this matters: MENU_CASH_OUTFLOW.SCX runs
UPDATE PART_FOR SET PAR_DTSCAD=DBO.RIBALTA2(...) at load (verbatim) —
the legacy REWRITES bad dates in the table; the web only shows them.
Netto per fascia + progressive cumulato (liquidity curve; senza-data
band excluded from the cumulato). Negative residuals (note accredito,
anticipi) kept with their sign, as the legacy grids do. Per-party
tables (clienti/fornitori) with per-band columns, sorted by total.

DECLARED LIMIT vs legacy: the legacy also projects ordini da evadere
(ORD/ORF) and DDT non fatturati (BOL/BFO) simulating payment terms;
ecashlib.prg is EMPTY in the repo (0 bytes) and tesoreria.PRG is a
35-byte stub, so the projection rules are not reconstructible — the
web ships the FAT/FAF component only, stated in the page subtitle and
tracked NEEDS_DOMAIN (with questions on effetti/RiBa split PRGs and
split payment) in cashflow/README.md + tracker.

Menu: "Cash flow" synthetic entry right after Scadenziario in the
contabilita case, indexes renumbered. MINOR bump 0.20.0 -> 0.21.0.
No Excel export in v1 (noted as easy extension once the scope question
- partite only vs full projection - is answered).
