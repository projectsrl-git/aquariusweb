# 2026-07-12 — Statistiche fatturato vendite (patch A)

/vendite/statistiche on top of the existing invoice tables (no list
rebuilt): VenditeStatsDao (native, tenantDataSource) aggregates
U_FAT_TT heads (measure = ORD_IMPONIB, alongside t_netto) and U_FAT_DD
rows (per article: ORD_QTAORD + ORD_VALORE = row value net of
discounts; join TAGGANCIO=DAGGANCIO as InvoiceRowRepository).

Views: per month (fiscal year + optional month range from
ORD_DATORD 'yyyy/MM/dd' SUBSTRING(6,2)), per customer (top 100 by
imponibile), per article (top 100 by row value), multi-year comparison
(totals per year + anno x mese matrix, last 5 years,
TRY_CONVERT(INT, ORD_ANNO) guard). KPI cards; Excel export
(VenditeStatsExcelExporter, SXSSF pattern from BilancioExcelExporter:
sheets Per mese / Per cliente / Per articolo).

Honesty choices: note di accredito are NOT netted or excluded (no sign
assumptions); the "per articolo" measure is labelled as row value;
legacy filters causale/zona/operatore/c.costo not reproduced (noted in
tracker as future extension). Menu: NEW synthetic case "clienti" with
"Statistiche vendite" entry + separator.

To confirm at deploy: ORD_DATORD format on real data ('yyyy/MM/dd'
assumed per CLAUDE.md); U_FAT_TT volumes for the multi-year scan;
whether Impresind wants causale filtering to exclude specific document
types from the totals.
