# 2026-07-12 — Controlli documenti vendite (patch B)

/vendite/controlli with three read-only checks, criteria DECLARED in
UI and code:
1. DDT non/parzialmente fatturati — grounded on the session-1 verified
   mechanism: invoicing STAMPS the DDT ROWS with MOV_NUMFAT/MOV_DATFAT
   (U_BOL_DD). LEFT JOIN head→rows, HAVING stamped < total; stato
   NON_FATTURATO / PARZIALE; causale shown (some causali legitimately
   never invoice: user judges, NEEDS_DOMAIN for the exclusion list).
2. Invoice number sequence — gaps via LEAD() on distinct
   TRY_CONVERT(INT, ORD_NUMORD) per year (SQL Server 2012 supports
   LEAD); duplicates with the TIPORD series involved (different series
   may be legitimate); non-numeric numbers surfaced as data quality,
   not silently dropped.
3. IVA sales register protocol gaps — IVA_CHIAVE on U_IVA_CL (non-C
   rows), same LEAD approach; the legacy PRNPROT works on MOV_CONT
   instead (MOV_NDOC, verbatim comments) → documented in tracker as
   NEEDS_DOMAIN on which sequence is fiscally authoritative.
CONTROLLO_BOLLEFATTURE turned out to target FTP/punto-vendita archives
(u_ftp_an, casse) — different domain, NEEDS_DOMAIN question filed.

Menu: "Controlli vendite" added to the new clienti synthetic case.
Summary KPI cards (green when clean), anomaly-panel pattern.
To confirm at deploy: LEAD() plan on U_FAT_TT volumes; FOR XML PATH
series aggregation; whether gaps should be computed per TIPORD.
