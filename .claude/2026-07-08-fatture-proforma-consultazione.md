# 2026-07-08 — Sales invoices + proforma invoices (read-only consultation)

## Problem / goal
Expose sales invoices (`U_FAT_TT`/`U_FAT_DD`) and proforma invoices
(`U_FAP_TT`/`U_FAP_DD`) as read-only consultation modules. Web counterparts
of the VFP forms `MENU_FAT000` ("Fatture di vendita") and `menu_FAP000`
("Fatture proforma").

## Domain corrections vs. the session handoff
- **U_FAP_* is "fatture PROFORMA", not "fatture acquisto"**: both the
  `tbl_menu.DBF` entries (`gestionefattureproforma` → `menu_fap000`,
  label "Aggiornamento fatture proforma/note informative") and the form
  caption ("Fatture proforma") confirm it.
- **On the invoice tables `ORD_NUMORD`/`ORD_DATORD` are the invoice's OWN
  number/date** (legacy lookups: `ord_numord = ALLTRIM(Z_NUMFAT)`, invoice
  lists `order by ord_numord desc`). The ORD_ prefix reuse strikes again.

## Approach
- Same pattern as orders/DDT: read-only entities, ListParams list scoped to
  society + fiscal year, TAGGANCIO=DAGGANCIO header↔rows join, detail with
  rows table. FAT and FAP are structurally identical → the proforma module
  is a twin of the invoice module (same field names, same model attribute
  names, twin templates).
- FE (fatturazione elettronica) surfaced read-only: `ORD_TRASME` →
  "Trasmessa"/"Non trasmessa" badge, `ORD_IDSDI` shown in detail. FE
  generation/sending stays with Erasmo's FATTURAZIONE_ELETTRONICA_LIB
  (tracked NEEDS_DOMAIN).
- Amount cards: imponibile / imposta / totale (computed in entity) / netto
  a pagare (`t_netto`).

## Files touched
- `entity/tenant/InvoiceHead.java`, `InvoiceRow.java`,
  `ProformaHead.java`, `ProformaRow.java` (new)
- `repository/tenant/InvoiceHeadRepository.java`, `InvoiceRowRepository.java`,
  `ProformaHeadRepository.java`, `ProformaRowRepository.java` (new)
- `controller/InvoiceController.java`, `ProformaController.java` (new)
- `templates/fatture/{list,detail}.html`, `templates/proforma/{list,detail}.html` (new)
- `service/MenuService.java` (FORM_TO_URL + menu_fat000, menu_fap000)
- `src/main/resources/migration/scx_migration_tracker.csv` (10 rows)
- `CLAUDE.md` (slice table)

## Key decisions & trade-offs
- Contabilizzazione (posting) and FE generation intentionally untouched —
  WRITE_SIDE / NEEDS_DOMAIN, reserved for Opus sessions with Erasmo.
- Scadenze/partite links from the invoice deferred (partitari already give
  the payment-side view).

## Compatibility notes
- Read-only on U_FAT_*/U_FAP_*; no schema changes; no new tables.
- Sandbox verification only; Maven build and live-DB behaviour to be
  confirmed at first deploy.
