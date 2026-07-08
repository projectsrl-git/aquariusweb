# 2026-07-08 — Produzione STANDARD (consultazione)

## Goal
Read-only consultation of STANDARD production programs only (mandate:
exclude generica/molle/tessuti and the specialized PRODBOBI/PRODPEDANA/
PRODSPAL/PRODMACC forms). VFP: menu `gestioneproduzionestandard`, list
form `STD_PROGRAMMAZIONE`.

## Domain findings (verified in STD_PROGRAMMAZIONE sources)
- **The STANDARD discriminator exists and is `TIPO = 'STD'`** on the
  `PRODUZIONE` table, combined with `PARENT = ''` to select the ROOT
  nodes of the program tree (PRODUZIONE is hierarchical: IDNODE/PARENT).
  No guessing needed — the legacy query is
  `PARENT = '' and TIPO = 'STD' and …`.
- `PROD_ORDINI` (linked customer orders), `PROD_LEGAMI` (components),
  `PROD_AVANZA` (progress per phase FASELA) all link by **IDPRG**.
  None of the PROD_* tables has society/year columns.
- `GRUPRD` (production group, on PROD_ORDINI) decodes via
  `PARA 'PRD'+code → DESCRI` (legacy `seek_para('PRD', …)`).

## Implementation
- `/produzione` — `ProductionProgram` + `searchStandard`
  (`parent = '' AND type = 'STD'`, free text on number/article/description);
  status badge derived from FLGCLOSED/COMPLETE/DTINEF
  (Chiuso / Completato / In corso / Pianificato).
- `/produzione/{id}` — program header (planned vs actual period, phase),
  linked customer orders with GRUPRD badge + PARA description, components
  (PRDFIN badge "Prodotto finito"), progress table (planned vs produced
  quantity, produced highlighted when >= planned). Article links to
  `/articoli?q=`.
- Menu: `std_programmazione → /produzione`. `std_avanza` is data-entry
  and intentionally NOT mapped (its consultation lives in the detail).

## Explicitly NOT here (tracker)
Program creation/editing and order attach/detach (WRITE_SIDE); progress
recording (WRITE_SIDE — STD_AVANZA); impegni/disimpegni and warehouse
movements; non-standard production variants (OUT_OF_SCOPE by mandate);
pianificazione and acquisti forms of the same menu (separate concerns,
not consultation of programs).

## To confirm at first deploy
Maven build; volume of PRODUZIONE root STD rows (pagination is in place);
that historical programs carry TIPO='STD' consistently (older rows may
predate the 2013 TIPO filter — if the list looks incomplete, tell me and
we diagnose on real data).
