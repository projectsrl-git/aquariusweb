# 2026-07-08 — Acquisti (ORF/BFO) + anagrafiche minori (consultazione)

## Goal
Bulk of read-only consultation modules mirroring existing patterns:
supplier orders, supplier inbound documents, and simple lookup registries
(agenti, banche, capi area).

## Archive corrections (verified in the sources — IMPORTANT)
1. **Supplier inbound DDT = `U_BFO_*`, not `U_BOF_*`.** The handoff said
   U_BOF_TT/DD ("entrata merce"), but the sources show U_BOF_* is the
   customer-facing "bollette fiscali" flow: BOFCONSE ("Riepilogo delle
   consegne") opens U_CLI_AN, and the menu labels are "Scarico con
   bolletta fiscale" / "Stampa giornaliera del consegnato (fiscale)".
   The supplier-inbound archive is U_BFO_* (MENU_BFO000 "Carico da
   fornitore", gestioneddt6) — already used by the Ristampa dashboard
   (types BFO/RDC). Module built on U_BFO_*; U_BOF_* tracked NEEDS_DOMAIN.
2. **`U_CAR_AN` is NOT vettori.** MENU_CAR000 caption: "Gestione agenti
   per capo area"; labels: codice/nome capo area, codice/nome agente,
   "% provvigione calcolata su imponibile del venduto". Route `/capi-area`.
   Vettori are the PARA 'VET' category, already served by /parametri/VET.
3. **`U_PAG_AN` is NOT condizioni di pagamento** — columns are order
   advances/receipts (PAG_CODCLI, PAG_NUMORD, PAG_DATINC, PAG_VALORE) and
   the ONLY references in the codebase are replication (REPLIB) and
   cleanup (PULISCI_ARCHIVI, menu_faichiu): a dead archive. No module
   built (NOT_APPLICABLE). Payment terms are PARA 'CPA' — already served
   by /parametri/CPA.

## Modules
- `/ordini-fornitore` — U_ORF_TT/DD, mirror of /ordini. Ordered vs
  received qty (ORD_QTAEV), row delivery date, Chiuso/Aperto badge,
  linked customer order (ORD_NUMORC/DATORC, back-to-back / conto lavoro).
  NOTE: MENU_ORF000 also manages "proposte ordini" via a launch flag with
  no visible discriminator column → the list shows the whole archive
  (NEEDS_DOMAIN: ask Erasmo how proposals are distinguished).
- `/ddt-fornitore` — U_BFO_TT/DD, mirror of /ddt. ORD_NUMORD/ORD_DATORD
  as the document's own number/date (Ristampa convention); ORD_TIPO=9
  badge "Reso da cliente" (verified RDC discriminator) instead of hiding
  those rows; rows show linked purchase order (ORS_NUMORC/ORS_DATORC) and
  supplier invoice refs (MOV_NUMFAT/MOV_DATFAT) as text.
- `/agenti` — U_AGE_AN list+detail (code, name, address, CF/PI, base
  commission %, currency, insert date). Advanced commission tiers
  (AGE_SCAPROV0-9) not exposed.
- `/banche` — U_BAN_AN list+detail (ABI/CAB/CIN, IBAN, SWIFT, notes).
  Linked accounting accounts (BAN_CONCON/CONRIC) deferred to accounting.
- `/capi-area` — U_CAR_AN list (association table: list is the whole
  story, no detail page).

Menu (FORM_TO_URL): menu_orf000, menu_bfo000, menu_age000, menu_ban000,
menu_car000.

## Also in this patch
Cosmetic fix: the shared `sortableHeader` fragment expects
align='text-end' but five session-1 list templates passed 'end' (headers
were not right-aligned). Fixed in articoli/ordini/ddt/fatture/proforma.

## To confirm at first deploy
Maven build; U_BFO volume/pagination; whether ORF proposals pollute the
supplier-order list in practice (if so, we isolate them once Erasmo
confirms the discriminator); exact column population of ban_iban/ban_swift
on real data.
