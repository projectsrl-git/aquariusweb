# 2026-07-08 — Articles registry (read-only consultation)

## Problem / goal
Expose the article registry (`U_ART_PR`, 254 columns — the most important
legacy master table) as a read-only web consultation module, replicating the
consolidated shared-list pattern (ListParams + list-tools fragments). Web
counterpart of the VFP form `MENU_ART000` ("Anagrafica articoli", 12 tabs,
986 objects, 415 with code).

## Approach
- Pure consultation: every column mapped `insertable=false, updatable=false`,
  no save path, no edit view. Article maintenance stays on the VFP client.
- Mapped ~30 of 254 columns — the ones meaningful for consultation (logical
  key, descriptions, UM, sale prices 1–5, standard cost, IVA code,
  classification, supplier, barcode, location/stock levels, obsolete flag,
  notes). All verified against `docs/db_schema/aquarius_schema_full.csv`.
  Note: the VFP form binds `M.art_coord1..4` which do NOT exist as columns
  (memvars); the real column is `ART_COORD` (varchar 50).
- List: free-text search (code / description / extended description / barcode
  / supplier), sortable whitelisted columns, badges for UM / IVA / commodity
  category / Obsoleto, code+extension rendered as `code/ext`.
- Detail: four cards mirroring the form's core tabs (Anagrafica, Prezzi,
  Classificazione e fornitore, Magazzino e note).
- Menu: `tbl_menu` already contains the leaf (`gestionearticoli` →
  `do form form\menu_art000 linked`), so a single `FORM_TO_URL` entry
  (`menu_art000` → `/articoli`) makes it clickable — no synthetic entries.

## Files touched
- `entity/tenant/Article.java` (new)
- `repository/tenant/ArticleRepository.java` (new)
- `controller/ArticleController.java` (new)
- `templates/articoli/list.html`, `templates/articoli/detail.html` (new)
- `service/MenuService.java` (FORM_TO_URL + menu_art000)
- `src/main/resources/migration/scx_migration_tracker.csv` (22 rows for MENU_ART000.SCX)
- `CLAUDE.md` (slice table)

## Key decisions & trade-offs
- No edit path at all (unlike Customers/Suppliers): articles have heavy
  cross-module implications (BOM, pricing, warehouse); consultation-first.
- Specialist tabs (Editoria, Web/e-commerce, Etichette, Dati statistici,
  Di.Ba. buttons) intentionally not ported — tracked in the migration CSV
  with reason codes.
- Multi-supplier article links (Page5 grid) reduced to the primary supplier
  code (`ART_CODFOR`); full article-supplier registry is a future slice.

## Compatibility notes
- Read-only on U_ART_PR; no schema changes; no new tables.
- Sandbox verification only (no Maven/JDK/SQL Server): Java brace balance,
  duplicate-@Column grep, fragment-arity check, CSV column-count check.
  Maven build and live-DB behaviour to be confirmed at first deploy.
