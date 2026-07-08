# 2026-07-08 — DDT / transport documents (read-only consultation)

## Problem / goal
Expose DDTs (`U_BOL_TT` header + `U_BOL_DD` rows) as a read-only consultation
module. Web counterpart of the VFP form `menu_BOL000` ("Documenti di
trasporto").

## Approach
- Same pattern as the orders slice: two read-only entities, ListParams list
  scoped to society + fiscal year, header detail + rows table.
- **Column-name gotcha documented**: U_BOL_* tables reuse the `ORD_` prefix
  for their own columns (`ORD_NUMDDT`/`ORD_DATDDT` are the DDT number/date;
  `ORD_NUMORD`/`ORD_DATORD` on the SAME header are the LINKED order refs).
- Header↔rows join: `TT.TAGGANCIO = DD.DAGGANCIO` — the "aggancio" hook key
  found in the legacy SQL (`LEFT OUTER JOIN U_BOL_DD ON U_BOL_TT.TAGGANCIO =
  U_BOL_DD.DAGGANCIO`). No triple-key fallback here (the aggancio join is the
  form the legacy itself uses for BOL); empty-rows message tells the user to
  check the hook key on historical data.
- Delivered qty = `ORD_QTAORD` on U_BOL_DD (summed by APPLILIB to compute
  order fulfilment). Row shows linked invoice (`MOV_NUMFAT`/`MOV_DATFAT`).
- Status badge: Fatturato / Da fatturare from `ORD_DATFAT` non-empty.
- Menu: `tbl_menu` leaf exists (`gestioneddt11` → `menu_bol000`); added
  `FORM_TO_URL` entry `menu_bol000` → `/ddt`.

## Files touched
- `entity/tenant/DdtHead.java`, `entity/tenant/DdtRow.java` (new)
- `repository/tenant/DdtHeadRepository.java`, `DdtRowRepository.java` (new)
- `controller/DdtController.java` (new)
- `templates/ddt/list.html`, `templates/ddt/detail.html` (new)
- `service/MenuService.java` (FORM_TO_URL + menu_bol000)
- `src/main/resources/migration/scx_migration_tracker.csv` (7 rows menu_BOL000.SCX)
- `CLAUDE.md` (slice table)

## Key decisions & trade-offs
- Causale shown as raw code badge (no PARA lookup guessed — the CAU prefix
  convention for DDT causali is unverified; NEEDS_DOMAIN if a description is
  wanted).
- Order-fulfilment cross-navigation (DDT row → order row via ORD_LEGSYS)
  deferred.

## Compatibility notes
- Read-only on U_BOL_TT/U_BOL_DD; no schema changes; no new tables.
- Sandbox verification only; Maven build and live-DB behaviour to be
  confirmed at first deploy.
