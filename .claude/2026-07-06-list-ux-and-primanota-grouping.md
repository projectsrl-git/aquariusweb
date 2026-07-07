# 2026-07-06 — Shared list UX + primanota grouped by registration

## Obiettivo (richiesta utente, 8 punti)
1. Primanota: aggiungere colonne Tipo Operazione (codice+descrizione) e Conti
   coinvolti (badge con descrizione conto).
2. Ordinamento per colonna.
3. Date in formato DD/MM/YYYY.
4. Righe per pagina configurabili, default 20.
5. Punti 2-4 validi per TUTTE le ricerche → refactoring condiviso.
6. Metriche di raggruppamento cliccabili in testa: per periodo, TOP 5
   clienti/fornitori, TOP 5 tipi operazione.
7. Aggiornare CLAUDE.md.
8. (Fable 5 — vedi prompt separato consegnato in chat.)

## Infrastruttura condivisa (punto 5)
- `com.aquarius.web.ListParams`: paginazione+ordinamento centralizzati, con
  whitelist dei campi ordinabili e opzioni pagina {20,50,100,200}, default 20.
- `fragments/list-tools.html`: sortableHeader, pager (con selettore righe),
  itDate (yyyy/MM/dd o yyyy-MM-dd → dd/MM/yyyy).
- Applicato a: primanota, partitari cli/for, clienti, fornitori.
- Rimosso l'ORDER BY fisso dalle query search di Customer/Supplier/Partita*
  (altrimenti sovrascrive il Sort del Pageable).

## Primanota raggruppata (punti 1, 6)
- La lista mostra UNA riga per registrazione (prima: una per movimento).
- `MovContabileRepository.searchRegistrationHeads`: GROUP BY registrationNo,
  paginata, ordinamento via :orderCol/:asc con CASE (compatibile GROUP BY),
  countQuery esplicita (COUNT DISTINCT).
- `findRowsForRegistrations` + `RegistrazioneRow.fromMovements`: costruisce la
  testata con tipo operazione, importo (somma Dare), conti coinvolti (badge).
- Descrizione tipo operazione da PARA (CODICE='TOP'+MOV_TOP → DESCRI).
- Metriche: amountByPeriod, topCustomerAccounts, topOperationTypes (TOP 5 via
  Pageable). Importi pre-formattati nel controller (formatIt, Locale.ITALY):
  niente `new BigDecimal` nel template (fragile in SpEL).

## Pitfall risolti (documentati in CLAUDE.md §8)
- Sort del Pageable su query aggregate/GROUP BY → SQL non valido su SQL Server.
  Soluzione: toPageableNoSort() + ORDER BY CASE nella query.
- Paginazione JPQL con GROUP BY → serve countQuery esplicita.
- L'importo (totDare) è alias di aggregato → NON ordinabile via Pageable:
  escluso dalla whitelist primanota; header "Importo" reso non cliccabile.

## Verifica (sandbox)
- Brace/paren OK su tutti i Java (ListParams, RegistrazioneRow,
  MovContabileRepository, ContabilitaController, Customer/SupplierController).
- Div balance OK su tutti i template (primanota 22/22, partitari, storico,
  registrazione, clienti, fornitori).
- Firme fragment ↔ chiamate verificate (sortableHeader 8 arg, pager 7 arg).
- Alias proiezione ↔ getter verificati (RegHead, MetricRow).
- Duplicate @Column sulle nuove entity: nessuno.
- git apply --check su clone fresco: pulito.

## NON verificato (richiede deploy su DB reale)
- Esecuzione delle query aggregate su SQL Server 2012 (in particolare l'ORDER
  BY con CASE multi-ramo e SUBSTRING nel GROUP BY di amountByPeriod).
- Che TAB_TOPCONT sia popolata per la società/anno correnti.
- Che il Maven build compili (nel sandbox non c'è Maven/JDK completo; solo
  check sintattici). Da confermare al primo deploy.

---

## Correzione (stessa data) — lookup tipo operazione e clienti/fornitori

Due bug emersi al primo deploy della primanota raggruppata:

1. **Tipo operazione senza descrizione.** La descrizione NON sta in TAB_TOPCONT
   ma nella tabella PARA: `CODICE = 'TOP' + ALLTRIM(MOV_TOP)`, descrizione =
   `PARA.DESCRI` (fonte VFP: contabilelib, join `mov_cont`/`para_top`). Rimossa
   l'entity `TipoOperazione`; `operationTypeNameMap()` ora usa
   `ParameterRepository.findByPrefix("TOP")` e il lookup antepone "TOP" al
   codice MOV_TOP.
2. **TOP 5 clienti/fornitori vuoto.** Cliente/fornitore si identificano dal tipo
   conto `CONTI.CON_TIPOCO` = 'C'/'F', non da MOV_CCLI/MOV_CFOR (quasi sempre
   vuoti). `topCustomerAccounts` ora fa join `MovContabile`↔`Account` sul codice
   conto e filtra `accountType IN ('C','F')`.

NB: consegnato come patch CUMULATIVA (list-ux + questi fix) perché la patch
list-ux non era ancora stata pushata nel repo (HEAD = ea41579).
