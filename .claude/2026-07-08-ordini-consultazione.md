# 2026-07-08 — Customer orders (read-only consultation)

## Problem / goal
Expose customer orders (`U_ORD_TT` header + `U_ORD_DD` rows) as a read-only
consultation module: paginated header list + full detail with rows. Web
counterpart of the VFP form `MENU_ORD000` ("Ordini clienti", 536 objects).

## Approach
- Two read-only entities (`OrderHead`, `OrderRow`), every column
  `insertable=false, updatable=false`; no write path.
- **TT↔DD link**: primary hook `TT.TAGGANCIO = DD.DAGGANCIO` — the "aggancio"
  key present on ALL legacy document tables (ORD/BOL/FAT/FAP) and used by the
  modern legacy SQL joins found in the PRGs. Fallback (empty hook on very old
  records): triple key from `ristampelib_originale.prg`
  `ORS_DATORD = ORD_DATORD AND ORS_NUMORD = ORD_NUMORD AND ORS_CODCLI =
  ORD_CODCLI` (the DD carries denormalized header keys with `ORS_` prefix).
  SQL Server ignores trailing spaces in varchar `=`, so no trimming needed.
- **Row value semantics from APPLILIB** (`CALCOLA_VALORE_RIGA_DOCUMENTO`):
  `ORD_VALORE = ORD_PRZNET × ORD_QTAORD`. Exposed columns: sequence, article
  (code/extension), descriptions, ordered qty, gross/net price, row value,
  IVA badge, fulfilled flag. Comment rows (`*** COMMENTO ***` convention,
  seen in CONTABILELIB filters) are rendered muted.
- List scoped to **current society + fiscal year** via `FiscalContext`
  (`ORD_CODSOC`/`ORD_ANNO`), mirroring the legacy `PUB_ANNO` behaviour and
  the ContabilitaController pattern. Free-text search on number / customer
  code / name / reference; status badge from `ORD_CHIUSO` / `ORD_EVATOT` /
  `ORD_EVAPAR` (Chiuso / Evaso / Evaso parz. / Aperto).
- Header detail: Testata + Importi cards; total = imponibile + imposta
  computed in the entity (never BigDecimal math in templates).
- Menu: `tbl_menu` leaf exists (`gestioneordiniclienti` → `menu_ord000`);
  added `FORM_TO_URL` entry `menu_ord000` → `/ordini`.

## Files touched
- `entity/tenant/OrderHead.java`, `entity/tenant/OrderRow.java` (new)
- `repository/tenant/OrderHeadRepository.java`, `OrderRowRepository.java` (new)
- `controller/OrderController.java` (new)
- `templates/ordini/list.html`, `templates/ordini/detail.html` (new)
- `service/MenuService.java` (FORM_TO_URL + menu_ord000)
- `src/main/resources/migration/scx_migration_tracker.csv` (14 rows MENU_ORD000.SCX)
- `CLAUDE.md` (slice table)

## Key decisions & trade-offs
- Quantity exposed is `ORD_QTAORD` (the one used by the legacy row-value
  formula); the additional `ORD_QUANTP/F/D/X/O/Y` columns have unclear
  semantics in the sources and are left unmapped (NEEDS_DOMAIN if needed).
- Order fulfilment drill-down to DDT rows (`ORD_LEGSYS` linkage) deferred to
  the DDT slice.
- Fiscal-year scoping means older orders are reachable by switching year
  from the sidebar badge — same UX as the legacy client.

## Compatibility notes
- Read-only on U_ORD_TT/U_ORD_DD; no schema changes; no new tables.
- Sandbox verification only: Java brace balance, duplicate-@Column grep,
  fragment-arity and CSV checks. Maven build and live-DB behaviour (incl.
  performance of the LIKE search on large order archives) to be confirmed
  at first deploy.
