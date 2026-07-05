# 2026-07-04 — Contabilità (consultazione read-only): 4 aree

## Scope
Prima slice di contabilità, tutta in SOLA LETTURA su tabelle legacy:
1. Primanota — elenco registrazioni + dettaglio righe Dare/Avere.
2. Storico contabile — mastrino di un conto con saldo scalare.
3. Bilancio — totali Dare/Avere e saldo per conto.
4. Partitari clienti e fornitori — partite aperte con residuo.
Il DATA-ENTRY della primanota (scrittura su MOV_CONT) sarà una slice a parte:
tocca il principio plug&play (scrivere su tabelle legacy) e va progettato
con cura (probabile staging in aq_web_ o scrittura controllata).

## Source of truth
Schema (docs/db_schema): MOV_CONT (118 col), PART_CLI (colonne PAR_*),
PART_FOR (93 col). VFP: MENU_FOR000 non c'entra; per la semantica dei
movimenti letti i PRG (contabilelib) — confermato MOV_TMOV = 'D' (Dare) /
'A' (Avere), partita doppia. Ogni scrittura = righe con stesso
MOV_SOC+MOV_ANNO+MOV_NREGIS. Solo colonne verificate mappate.

## Model (read-only entities)
- MovContabile (MOV_CONT): sottoinsieme ~17 col utili a primanota/storico/
  bilancio. Nessun campo scrivibile.
- PartitaCliente (PART_CLI), PartitaFornitore (PART_FOR): stesse colonne
  (PART_FOR riusa PAR_CODCLI come codice fornitore). getResidual()=totale-pagato
  (Transient).
Tutte le date sono varchar (yyyy/MM/dd), filtrate/ordinate come stringhe.

## Queries (repositories)
- MovContabileRepository: searchRegistrations (primanota, paginata),
  findRegistrationRows (righe di una registrazione), findLedger (mastrino),
  findBilancio (GROUP BY conto con SUM CASE Dare/Avere, proiezione BilancioRow).
- Partita{Cliente,Fornitore}Repository: search paginata + findByParty.
- AccountRepository: +findAllByYearAndSociety per la mappa conto→descrizione.

## Controller /contabilita (ContabilitaController)
Routes: /primanota, /primanota/{nreg}, /storico?conto=, /bilancio,
/partitari/clienti, /partitari/fornitori. Anno e società presi da
FiscalContext (l'interceptor garantisce sia sempre valorizzato: redirige a
/select-year altrimenti — nessuna guardia extra necessaria). Saldo scalare e
totali calcolati in Java; descrizioni conto risolte via mappa.

## DTO
LedgerRow (movimento + dare/avere derivati + saldo progressivo),
BilancioLine (conto + descrizione + totali + saldo).

## Templates (contabilita/)
primanota, registrazione (partita doppia con totali + warning sbilancio),
storico (mastrino con saldo scalare), bilancio (KPI + tabella con link al
mastrino), partitari (condiviso clienti/fornitori, residuo colorato).

## Menu
MenuService FORM_TO_URL: menu_primanot(2)→/contabilita/primanota,
viewpartitario/partitario_soprad→/contabilita/partitari/clienti.

## Verification (sandbox)
- Java brace/paren scan OK su 11 file.
- Nessun @Column duplicato nelle 3 entity.
- Ogni property referenziata nei 5 template esiste nell'entity.
- div balance: primanota 5/5, registrazione 7/7, storico 8/8, bilancio 20/20,
  partitari 5/5.
- git apply --check clean su clone fresca di HEAD 74bfa73.

## Not verified (confirm on deploy)
- Query reali su MOV_CONT/PART_* (volumi, performance del GROUP BY bilancio).
- Che le voci di menu contabili risolvano (dipende dai COMANDO in tbl_menu;
  se non compaiono servirà voce sintetica).
- Ordinamento date-as-string: valido se le date sono yyyy/MM/dd; da confermare
  sul dato reale (se fossero dd/MM/yyyy l'ordinamento andrebbe rivisto).
