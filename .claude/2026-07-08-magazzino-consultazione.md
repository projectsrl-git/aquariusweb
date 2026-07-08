# 2026-07-08 — Magazzino: movimenti e giacenze (consultazione)

## Goal
Read-only consultation of warehouse movements (`U_MAG_MO`) and current
stock (`U_MAG_GG`). VFP counterparts: `menu_movimenti_mag` and
`menu_giacenze` (the latter reached in the legacy menu through the
dispatcher function `=determina_form_giacenze()`, which opens
MENU_GIACENZE on non-medical installs — verified in APPLILIB).

## Domain findings (verified)
- The movement causale is **`MOV_TOP`**, decoded via
  `PARA 'TOP'+MOV_TOP → DESCRI` — verbatim from the legacy form query
  (`left outer join para as PARA_TOP on PARA_TOP.codice = 'TOP'+MOV_TOP`).
  Same TOP rule as primanota and the documents dashboard.
- `MOV_ANAART` is the article key (same key as the FIFO valuation).
- Giacenze: the legacy PRGs ALWAYS pre-aggregate
  `SUM(MAG_GIACEN) GROUP BY MAG_ANAART[, MAG_CODMAG]` — multiple signed
  rows per article (CLAUDE.md pitfall). `MAG_ANAART` is the group key.
- Dates are varchar `yyyy/MM/dd`; `MOV_DTREGI` is dirty on old records →
  compared as strings only, never converted.

## Implementation
- `/magazzino/movimenti` — `WarehouseMovement` entity (subset of the 161
  columns) + `WarehouseMovementRepository.search` (society+fiscal year,
  free text also matching the TOP code) + shared list fragments. TOP badge
  with description from `ParameterRepository.findByPrefix("TOP")`.
- `/magazzino/giacenze` — `StockBalanceDao` (native SQL on
  tenantDataSource, pattern WarehouseValuationDao) because counting GROUP
  BY groups WITH a HAVING needs a derived table (not expressible in
  JPQL/Hibernate 5). Sort keys from a whitelist enum. Default hides
  zero-total groups; a switch shows them, and anomalies are surfaced with
  badges: **Negativa** (total < 0) and **Zero con storico** (phantom
  rows: zero total, physical rows present). Local sort/pager links
  preserve the `tutte` toggle (shared fragments carry only q/size/sort/dir).
- Menu: `menu_movimenti_mag → /magazzino/movimenti` via FORM_TO_URL;
  giacenze via the NEW minimal `FUNCTION_TO_URL` mechanism in MenuService
  (commands of the form `=funzione()`), mapping
  `determina_form_giacenze → /magazzino/giacenze`.

## Not migrated (tracker)
Mass updates on movements (WRITE_SIDE), per-period print forms
(PRINT_REPORT territory), valuation (DUPLICATE of the FIFO dashboard),
punctual period/range filters (DEFERRED to the filter framework, Opus).

## To confirm at first deploy
Maven build; OFFSET/FETCH performance of the giacenze aggregation on the
full U_MAG_GG; that TOP codes on old movements resolve in PARA.
