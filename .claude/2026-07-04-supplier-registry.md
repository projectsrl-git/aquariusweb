# 2026-07-04 — Supplier registry (anagrafica fornitori)

## Goal
Add the supplier master, reusing the customer framework (form-shell, FormTab,
whitelist copy). Replica of the VFP form MENU_FOR000.

## Source of truth
Read MENU_FOR000.SCT (1841 FOR_ references) for the tab layout, then
cross-checked every field against the schema (U_FOR_AN, 195 columns). Of 149
FOR_ fields in the form, 144 exist in the schema; the working set kept is 44
core fields across 7 tabs (only verified columns, no invented ones).

## Tabs (7, all active)
Anagrafica, Indirizzo e contatti, IVA, Commerciali, Dati contabili,
Dati bancari, Rif. fornitore. Simpler than customers (no Posticipi matrix,
no Produzione, etc. — those weren't in scope / not core).

## Implementation (mirrors customers)
- entity/tenant/Supplier.java: @Table(U_FOR_AN), @DynamicUpdate, @Data.
  Key: id_unique (PK), FOR_CODSOC (society), FOR_CODCON (code) — all
  immutable from web. 44 mapped fields, verified no duplicate @Column.
- repository/tenant/SupplierRepository.java: paged search on
  businessName/code/vatNumber.
- controller/SupplierController.java: /fornitori list, detail, edit, save.
  copyEditableFields whitelist (44 setters). Same transaction manager and
  breadcrumb wiring as customers.
- templates/fornitori/{list,detail,edit}.html: list mirrors clienti; detail
  uses full-page cards (optional cards render only when populated); edit uses
  the shared form-shell nav + footer fragments.
- MenuService FORM_TO_URL: menu_for000 -> /fornitori.

## Verification (sandbox)
- Java brace/paren scan OK on entity, controller, repository, MenuService.
- No duplicate @Column in Supplier (the check that caught the customer bug).
- Every th:field / supplier.X in the 3 templates maps to an entity property.
- div balance: list 5/5, detail 32/32, edit 65/65.
- git apply --check clean on a fresh clone of HEAD 74bfa73.

## Not verified (confirm on deploy)
- App starts; /fornitori lists U_FOR_AN; edit tabs load and a save round-trips.
- The Fornitori menu item resolves via menu_for000 (depends on the legacy
  tbl_menu COMANDO actually pointing at menu_for000; if the sidebar item
  doesn't appear, we may need a synthetic entry like customers).
