# 2026-07-08 — Ristampa documenti: unified dashboard + traceability

## Problem / goal
Port the most-used module of the legacy client: `MENU_RISTAMPA_DOC`
("Ristampa documenti") — one dashboard to consult ALL produced documents,
plus the links between them (ordine ↔ DDT ↔ fattura). Read-only: physical
reprint (.frx), e-mail sending and FE generation stay on the VFP client.

## Legacy architecture ported
The legacy picks the archive dynamically (`_V_ARCH_TT`/`_V_ARCH_DD` from a
CASE on `_V_TIPDOC`) and runs ONE query shape on it:

    select TT.*, para_top/para_age lookups
    from <TT>
    left outer join para para_top on para_top.codice = 'TOP'+ord_caus
    left outer join para para_age on para_age.codice = 'AGE'+ord_age
    where soc/anno/periodo/numeri/cliente/agente [+ riferimento LIKE]
      [+ taggancio IN (select daggancio from <DD> where ord_codart = ...)]

Web port: `DocumentArchiveDao` (NamedParameterJdbcTemplate on
`tenantDataSource`, same pattern as `WarehouseValuationDao`). Table names
are controlled identifiers from the `DocumentType` enum — user input only
travels as bound parameters. OFFSET/FETCH pagination + separate count.

## Type catalog (verified against tbl_menu, form CASE, and schema CSV)
Included (9): ORD, OFF (U_OFF_TT), ORF (U_ORF_TT), BOL, BFO (U_BFO_TT),
RDC (U_BFO_TT + `ord_tipo = 9`), FAP, FAT, INR (VENDITE_TT). Party C/F per
type from `_V_CLIFOR`.
Excluded and tracked: RFI/ORT/REC/REF/BOF/BCL share a table with another
type WITHOUT a discriminator visible in the sources (NEEDS_DOMAIN — likely
distinguished only at print time); FAF → U_FAF_TT does not exist in the
Impresind DB (NOT_APPLICABLE).

## Domain findings (important, verified)
1. **The Ristampa grid shows `ORD_NUMORD`/`ORD_DATORD` as Numero/Data for
   ALL types** (single grid binding, no per-type switch) — i.e. on every
   document table these columns are the document's OWN number/date.
2. Consequently, on DDT rows the linked customer order is the **ORC pair**
   `ORS_NUMORC`/`ORS_DATORC` ("ORdine Cliente" — legacy matches it against
   `U_ORD_T2.ORD_NUMORD`), while `ORS_NUMORD`/`ORS_DATORD` are the DDT
   header's own keys. **This patch also fixes the DDT slice accordingly**:
   `DdtHead.linkedOrder*` now maps `ORD_NUMORC`/`ORD_DATORC` (header) and
   `DdtRow.linkedOrder*` maps `ORS_NUMORC`/`ORS_DATORC`; the DDT's internal
   number (`ORD_NUMORD`) is now exposed as `internalNumber`. `ORD_NUMDDT`
   remains the number shown in `/ddt` (the fiscal DDT number used by the
   accounting side: ristampelib matches `mov_numdoc = ORS_NUMDDT`).
3. Causale description = `PARA 'TOP'+ORD_CAUS → DESCRI`, agente =
   `PARA 'AGE'+ORD_AGE → DESCRI` — verbatim from the legacy query, and
   consistent with the primanota TOP rule already in CLAUDE.md.

## Traceability ("Documenti collegati")
Verified for the sales chain, reconstructed from U_BOL_DD references:
- ORD → DDTs where `ORS_NUMORC/ORS_DATORC` match; invoices from those rows'
  `MOV_NUMFAT/MOV_DATFAT`.
- BOL → orders (distinct ORC pairs) + invoices (distinct MOV_NUMFAT pairs).
- FAT → DDTs where `MOV_NUMFAT/MOV_DATFAT` match; orders from those rows.
Target ids resolved with TOP 1 lookups by numero+data; unresolved links are
shown without a hyperlink. Other types show an honest "nessun legame
tracciato" note (no link columns verified in the sources).

## UI
Filter panel like the legacy mask (tipo, anno default = FiscalContext,
periodo dal/al, numero dal/al, soggetto, riferimento, articolo esatto via
subquery). Results grid with causale badge + description, imponibile and
"Valore con IVA" (computed in the DTO). Local sortable headers and pager
that preserve ALL filters (the shared list-tools fragments only propagate
q/size/sort/dir — their contract is untouched). Generic detail for every
type (testata + righe + documenti collegati) with an "Apri nel modulo
dedicato" button for ORD/BOL/FAP/FAT.

## Files touched
- `dto/documenti/DocumentType.java`, `DocumentoTestata.java`,
  `DocumentoRiga.java`, `DocumentoCollegato.java` (new)
- `repository/tenant/DocumentArchiveDao.java` (new)
- `controller/DocumentiController.java` (new)
- `templates/documenti/list.html`, `templates/documenti/detail.html` (new)
- `entity/tenant/DdtHead.java`, `DdtRow.java` (FIX: linked order = ORC pair)
- `service/MenuService.java` (FORM_TO_URL + menu_ristampa_doc; tbl_menu leaf
  `gestioneristampadocumenti` reachable from L1 clienti AND fornitori)
- `src/main/resources/migration/scx_migration_tracker.csv` (9 rows MENU_RISTAMPA_DOC.SCX)
- `CLAUDE.md` (slice table + ORC-pair gotcha)

## Compatibility notes
- Read-only everywhere; no schema changes; no new tables.
- Sandbox verification only. To confirm at first deploy: TAGGANCIO
  population on historical data, OFFSET/FETCH performance on the largest
  archives, and the RDC `ord_tipo = 9` filter on real data.
