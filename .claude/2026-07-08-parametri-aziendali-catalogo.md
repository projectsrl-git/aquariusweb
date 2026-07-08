# 2026-07-08 — Analisi DEEP parametri aziendali (catalogo)

## Goal
For EVERY parameter exposed by the legacy form MENU_AZI000.SCX ("Definizione
dati azienda"), determine purpose and behavior by digging into the VFP
sources, and publish the result as a runtime-readable catalog.

## Key architectural finding (the reading key for everything else)
**APPLILIB.PRG, function AQUADOCU** (application startup) loads every
`AZI_X` column of U_AZI_AN/PA/PB into a **public variable `PUB_X`**
(712 verified `PUB_X = AZI_X` mappings). The rest of the ERP reads the
PUB_* variables, almost never the tables. Exception: print/FE libraries
(ristampelib, OFFSTAM*, CONTABILELIB, FATTURAZIONE_ELETTRONICA_LIB) read
some registry columns directly from the tables for document headers/XML.
Therefore the REAL usage of a parameter is found by searching its PUB_*.

## Numbers
- 845 parameter controls parsed from the form (1717 objects, 14 top tabs,
  103 pages incl. nested pageframes). Page captions extracted from the
  pageframe records (`PageN.Caption`), labels attached geometrically.
- 743 columns resolved against the Impresind schema (234 AN, 249 PA,
  260 PB); 98 form columns ABSENT in the Impresind DB (other release /
  vertical) — included in the catalog with an explicit note.
- Catalog: **841 rows** — confidence 622 ALTA / 66 MEDIA / 153 INCERTO.
- 31 columns have no usage in current sources outside replication/cleanup
  (possible orphans) — marked INCERTO, not interpreted.

## Deliverable
`src/main/resources/parametri/parametri_aziendali_catalog.csv` (UTF-8,
quote-aware, 11 stable columns:
group,object_name,label,table_column,type,allowed_values,purpose,
how_it_works,used_in,confidence,notes) + README.md with schema, discovered
architecture, confidence criteria and reproducible method.
- `purpose` = the form's own descriptive caption (business language written
  by the authors) — for checkboxes it is a full sentence.
- `how_it_works` = the AZI→PUB load chain + a cited code excerpt
  (conditional constructs preferred) with file.function.
- `allowed_values` = optiongroup options (16 list parameters).

## Honesty rules applied
No invented interpretations: INCERTO wherever the label could not be
attached, the column is absent at Impresind, or no current-source usage
exists. Backup/dated library copies and pure-infrastructure files
(REPLIB/REPLIBS/PULISCI_ARCHIVI/ALLARGA_TOP_CAUS) are excluded from
evidence.
