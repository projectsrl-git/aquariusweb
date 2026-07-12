# 2026-07-12 — Cespiti: semantica quote verificata + colonne dettaglio

Short session on top of the session-8 module (register/detail already
delivered and applied; Opus meanwhile shipped scadenziario + bilancio
N-1, priorities #1/#9 of the audit).

Analysis (appended to cespiti/README.md with file:line evidence, mined
from ALLINEA_CESPITI_QUOTE_AMM.PRG and MENU_quo_am_gen/simu):
- QUO_VAL* = year quota per channel; QUO_FON* = PROGRESSIVE fund
  (gen: _X_QUO_FONORD = _X_QUO_VALORD + QUO_FONORD, first year = VALORD);
- QUO_TOTAMM = total of the YEAR's quotas (allinea r.80), NOT the
  progressive -> the detail column was ambiguously labelled "Totale
  ammortizzato": relabelled "Quote esercizio" with tooltip;
- alignment PRG checks anagrafica progressives against the LAST quota
  row (order annrif DESC) and realigns quota+fund deltas (r.52-81);
- QUO_VARFIS = anticipated quota (fiscal-only variation);
  QUO_IMPDIF = VARFIS x IRES rate (para_ires).

UI: detail quota table gains "Fondo ord. progr." (FONORD) and
"Var. fisc. / Imp. diff." columns (muted when zero), entity fields
added; MenuService untouched per mandate (Opus scadenziario patch
pending). PATCH bump 0.18.0 -> 0.18.1.
