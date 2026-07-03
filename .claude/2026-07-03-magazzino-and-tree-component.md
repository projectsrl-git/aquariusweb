# 2026-07-03 — Warehouse valuation slice + reusable tree component

Adds two developed-but-uncommitted slices missing from the initial import:
the client-side chart-of-accounts tree component and the warehouse valuation
dashboard. Delivered as full files (patch workflow hit repeated CRLF/base
issues on the Windows working copy).

## Added
- Chart of accounts: static/js/aq-tree.js + aq-tree.css (reusable tree),
  AccountTreeApiController (GET /conti/tree-data), AccountTreeService rewritten
  to VFP positional semantics (U_AZI_AN AZI_MASTRO/AZI_SOTTOG, defaults 3/5),
  AccountRepository TreeRow projection + native structure query,
  AccountController.tree() as shell, conti/tree.html fetches JSON.
  Removed the old recursive fragment templates/fragments/account-tree-node.html.
- Warehouse valuation: dto/magazzino/*, WarehouseValuationDao (FIFO + as-of FX,
  DateBase enum SQL substitution, yyyy/MM/dd string binding),
  WarehouseValuationService (KPI/ABC/anomalies/volatility),
  WarehouseValuationExcelExporter (POI SXSSF), WarehouseValuationController,
  templates/magazzino/valorizzazione.html, synthetic menu entry, dashboard CSS.
- .claude/README.md: real repo URL recorded.

## Delete manually
- src/main/resources/templates/fragments/account-tree-node.html
