# Cash flow — analisi legacy e scelte del viewer (sessione 11)

## Modello legacy (evidenza: screenshot dei form forniti dall'utente)

"Situazione Cash Inflow" / "Situazione Cash Outflow" proiettano TRE
fonti per lato, con data previsto pagamento calcolata dalle condizioni
di pagamento del documento:

| Lato | Tipo doc. | Fonte |
|---|---|---|
| Inflow | `ORD` | Ordini clienti da evadere |
| Inflow | `BOL` | DDT clienti non fatturati |
| Inflow | `FAT` | Fatture contabilizzate non pagate |
| Outflow | `ORF` | Ordini fornitori da evadere |
| Outflow | `BFO` | DDT di carico da fornitore non fatturati |
| Outflow | `FAF` | Fatture contabilizzate non pagate |

Viste: raggruppata per mese scadenza (±6 mesi con code "e precedenti"
/ "e successivi"), per data (±31 giorni), dettagliata (con condizioni
di pagamento, date previsto pagamento, articoli). Filtri: solo
c.pag. bonifici / solo Ri.Ba., data scadenza dal/al. I residui NEGATIVI
(note di accredito, anticipi) restano in griglia col loro segno.

## Evidenze dai sorgenti

- `MENU_CASH_OUTFLOW.SCX` esegue al caricamento
  `UPDATE PART_FOR SET PAR_DTSCAD = DBO.RIBALTA2(PAR_DTSCAD) WHERE
  SUBSTRING(PAR_DTSCAD,3,1)='/'...` — quindi (a) la componente FAF
  legge le PARTITE (`PART_FOR`), (b) esistono scadenze in formato
  anomalo che il legacy NORMALIZZA scrivendo in tabella.
- `ecashlib.prg` e' VUOTO nel repo (0 byte) e `tesoreria.PRG` e' uno
  stub di 35 byte: le regole esatte di proiezione ordini/DDT
  (simulazione condizioni di pagamento) NON sono ricostruibili dai
  sorgenti disponibili.

## Il viewer web (v1) — /contabilita/cashflow

- Fonte: SOLO partite aperte (`PartitaClienteRepository.findAperte` /
  `PartitaFornitoreRepository.findAperte`, residuo = PAR_TOTIM −
  PAR_PAGATO, scadenza PAR_DTSCAD) = componente FAT/FAF del legacy.
  Il limite e' dichiarato nel sottotitolo della pagina.
- Fasce coerenti con lo scadenziario (stesse ampiezze, direzione
  futura): Scaduto/oggi, 1–30, 31–60, 61–90, oltre 90 + fascia
  "Senza data" per scadenze assenti/anomale (esposte, NON corrette:
  niente scrittura, a differenza del legacy).
- Netto per fascia + CUMULATO progressivo (curva di liquidita' attesa);
  la fascia senza data resta fuori dal cumulato.
- Residui negativi inclusi col loro segno (comportamento legacy).

## NEEDS_DOMAIN (per Erasmo / Opus)

1. Proiezione ORD/BOL (e ORF/BFO): con quali regole il legacy calcola
   le "date previsto pagamento" dagli ordini/DDT non fatturati?
   (ecashlib vuoto nel repo — serve il sorgente o la spiegazione).
2. Effetti attivi/passivi e Ri.Ba.: i PRG TESORERIA_EXTRA_SPLIT_EFF_*
   suggeriscono un trattamento dedicato — vanno inclusi nel cash flow
   partite o viaggiano su binario separato?
3. Split payment: incide sull'importo atteso di incasso (IVA non
   incassata)? Le partite lo riflettono gia' o serve rettifica?
4. Il filtro per condizione di pagamento (bonifici/Ri.Ba.) richiede la
   condizione sulla partita o sul documento: da individuare la colonna.
