# 2026-07-08 — Bilancio CEE: analisi DEEP struttura + mappatura + algoritmo

## Goal
Decode the legacy CEE reclassification (BILNEW / U_INT_TT / ceecont.PRG)
so Opus can implement the calculation engine on verified ground. Analysis
only — no calculation, no writes.

## Findings (all cited in the deliverables)
1. **BILNEW is structure AND values.** Of its 39 columns the CEE flow uses
   six: BIL_CODSOC, BIL_CODRIG, TIPO_DATO, DESCRIZION, CORRENTE (computed,
   reset at each run), PRECEDENTE (rolled over by menu_ceeanno:
   PRECEDENTE=CORRENTE; CORRENTE=0). The other 33 are legacy of a generic
   structure table — never read by the CEE flow (marked INCERTO).
2. **No parent-child tree in BILNEW.** The prospetto structure is:
   print order = BIL_CODRIG (numeric-in-varchar, PADL 10 comparisons);
   row type TIPO_DATO with verbatim validation "I=Commenti, V=Dettagli di
   riga, T=Totali" (menu_cee000) and a dedicated print style for
   TIPO_DATO='T' in STBILCEE.FRX; the totals "hierarchy" lives in
   **U_COR_TT** as a signed edge list riga→totale.
3. **BIL_CODRIG number space encodes the section**: VAL(codrig) >= 21600 =
   conto economico (values ABS-ed at end of phase 2), < 21600 = stato
   patrimoniale. The attivo/passivo boundary inside SP is data, not code
   (INCERTO for Opus).
4. **U_INT_TT dual target**: INT_CODRIG = "Codice riga dare" (saldo >= 0),
   INT_CODRIA = "Codice riga avere" used when the saldo is NEGATIVE, if
   filled (form help verbatim; source comment "BANCHE IN PASSIVO"). One
   rule per account; the destination depends on the balance sign.
5. **Algorithm (ceecont.PRG)** in faithful pseudocode (README): reset;
   per-account saldo = CON_IMP_D - CON_IMP_A (+ previsionali PRE_IMP_D -
   PRE_IMP_A on option); accumulate into the dare/avere row; ABS on
   economic rows; totals by applying each U_COR_TT edge in COR_RIGA order
   (segno '+' adds, anything else subtracts; missing CONFLU row ABORTS).
   Ordering caveat for chained totals flagged INCERTO. Commented-out
   business cases (21900 rimanenze, 24400 variazioni — "da vedere col
   commercialista") documented as INCERTO.
6. **Data quality** (the three checks, with ready SSMS queries in the
   README): accounts absent from U_INT_TT are SILENTLY excluded; mappings
   to non-existent BILNEW rows skip the account with the verbatim warning;
   broken COR_CONFLU aborts the run.

## Deliverables (patch 1)
- `resources/cee/bilnew_struttura_catalog.csv` — 39 BILNEW columns decoded
  (usage in the CEE flow, semantics, evidence, confidence).
- `resources/cee/mappatura_conti_cee.csv` — U_INT_TT + U_COR_TT rules
  decoded per column with evidence.
- `resources/cee/README.md` — architecture, mapping rule, full pseudocode
  for Opus, data-quality checks + SSMS queries, declared deviation: the
  prompt asked one CSV row per CEE voce, but the voci are DB data with no
  seed in the repos — a static CSV would be a stale unverifiable copy, so
  the real rows are shown live by the viewer (patch 2).

## Tracker
9 rows: menu_cee000/ceepdc/ceetot (PARTIAL, WRITE_SIDE), ceecont
(DEFERRED→Opus with pseudocode), menu_ceeanno/ceeret (WRITE_SIDE),
menu_stbilcee (PRINT_REPORT), CEESAVE (NOT_APPLICABLE: floppy export),
menu_ceecori (PARTIAL: equivalent = viewer anomalies panel).
