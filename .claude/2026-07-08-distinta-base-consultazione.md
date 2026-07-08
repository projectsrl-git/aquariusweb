# 2026-07-08 — Distinta base (consultazione)

## Goal
Read-only consultation of BOMs: list + detail with FIRST-LEVEL components.
VFP: menu leaf `gestionediba` → `menu_dis000`; semantics from
DISTINTA_BASE_LIB.PRG.

## Domain findings (verified)
- **`DIT_GRUPPO` is the parent ARTICLE code**: the library joins
  `U_DIS_TT.DIT_GRUPPO = U_ART_PR.ART_CODPRI` and looks BOMs up with
  `dit_gruppo = <codice>`. A BOM "belongs to" the product article.
- Header↔rows: `TAGGANCIO = DAGGANCIO`; rows ordered by `DIS_SEQUEN`.
- **`DIS_ESPLOD = 'X'`** marks a component as an explodable sub-BOM (the
  component article has its own distinta).

## Implementation
- `/distinte` — `BomHead` + `BomHeadRepository.search` (article,
  description, customer) + shared fragments.
- `/distinte/{id}` — header card + first-level components (`BomRow`),
  each linked to `/articoli?q=<code>`; components with `DIS_ESPLOD='X'`
  get an "Apri sotto-distinta" badge → `/distinte/articolo/{code}`
  (redirect to the unique BOM whose `DIT_GRUPPO` = code, else to the
  filtered list). One level at a time by design.
- Menu: `menu_dis000 → /distinte`.

## Explicitly NOT here (tracker)
Recursive multi-level explosion and cost/price recomputation
(DEFERRED — calcolo, non consultazione); BOM editing, duplication,
deletion (WRITE_SIDE).

## To confirm at first deploy
Maven build; whether Impresind uses per-customer BOM variants
(DIT_CODCLI) — the column is shown when populated.
