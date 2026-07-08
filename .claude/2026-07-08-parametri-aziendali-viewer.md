# 2026-07-08 — Viewer parametri aziendali (read-only)

## Goal
Read-only consultation of the company parameters, grouped as in the legacy
form, with an info popover per parameter fed by the DEEP-analysis catalog.
Route `/parametri-aziendali`. NOT an editable settings page: writing the
parameters drives ERP behavior and is reserved to future slices (Opus).

## Implementation (pattern: migration tracker viewer)
- `CompanyParameterCatalogService` — loads the catalog CSV from the
  classpath with an RFC-4180 quote-aware parser (same approach as
  MigrationTrackerService).
- `CompanyParameterValuesDao` — current values via
  `SELECT TOP 1 * FROM U_AZI_{AN,PA,PB} WHERE AZI_CODSOC = :soc`,
  metadata-driven (ResultSetMetaData → Map column→formatted value): no
  700-column entity, and columns absent in this DB release simply do not
  appear (the viewer shows "n.d." with an explanatory tooltip). Table
  names are controlled identifiers (fixed list).
- `CompanyParametersController` — merges catalog + values; free-text
  search (label/column/purpose/how/group), top-tab filter, confidence
  filter; groups preserved in form order.
- Template — Bootstrap accordion per top tab; per row: label, current
  value (Attivo/Non attivo badge for booleans), type badge, table.column,
  sub-section, and an (i) button opening a Bootstrap popover with
  "A cosa serve / Valori possibili / Come funziona / Usato in / note".
  Popover content is built client-side with text ESCAPING (no HTML from
  the CSV is interpreted). INCERTO badge on uncertain rows.
- Menu: FORM_TO_URL `menu_azi000 → /parametri-aziendali` (legacy leaf
  "Definizione dati azienda") + synthetic entry "Parametri aziendali"
  under the "parametri" L1 (next to "Tutti i parametri").

## To confirm at first deploy
Maven build; that SELECT TOP 1 * on the three U_AZI_* returns one row per
society (the legacy keeps one row per soc); popover rendering with very
long `used_in` lists; performance is a non-issue (catalog in memory,
3 single-row queries).
