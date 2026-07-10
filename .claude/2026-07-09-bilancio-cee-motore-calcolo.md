# 2026-07-09 — Bilancio CEE: motore di calcolo autonomo (scrive BILNEW)

Il web ora RICALCOLA il bilancio CEE da sé (non serve più il gestionale).
Autorizzato perché BILNEW è **volatile**: azzerata/riscritta a ogni ricalcolo.

## Algoritmo (replica fedele di ceecont.PRG, da resources/cee/README.md)
`CeeCalculationService.recalcola(soc, anno, previsionali)`:
1. Carica le voci BILNEW (nodi in memoria, CORRENTE=0).
2. **FASE 1+2**: per ogni mappatura U_INT_TT, saldo conto = CON_IMP_D − CON_IMP_A
   (+previsionali se richiesto); riga destinazione = INT_CODRIG se saldo≥0,
   altrimenti INT_CODRIA se presente ("banche in passivo"); CORRENTE(riga)+=saldo.
   Conti senza saldo o senza riga valida → warning, saltati.
3. **FASE 2b**: ABS sulle voci economiche (VAL(codrig) ≥ 21600).
4. **FASE 3**: totali da U_COR_TT (edge con segno, ordine COR_RIGA);
   se una riga di confluenza manca in BILNEW → **abort, nulla scritto**
   ("aggiornamento annullato", come il legacy).
5. Scrittura in blocco: UPDATE BILNEW SET CORRENTE per ogni riga (società).

Lookup robusti su codici via chiave numerica normalizzata (zero-pad legacy PADL).

## Implementazione
- `CeeStructureDao`: +`accountBalances(soc,anno,prev)`, +`updateCorrente(soc,map)`
  (batch UPDATE su BILNEW). Riusa `mappings()` e `totalEdges()` (ordinato COR_RIGA).
- `CeeCalculationService`: l'algoritmo (in memoria, scrittura finale, abort-safe).
- `CeeBalanceController`: `POST /contabilita/bilancio-cee/ricalcola`
  (@Transactional tenant), flash esito + warning.
- Template: pulsante "Ricalcola bilancio CEE" + alert esito/errore/avvisi.

## DA VERIFICARE al deploy (fiscale!)
- **Riconciliare** i valori col «Calcolo bilancio cee» del gestionale sulla stessa
  società/esercizio (lanciare entrambi, confrontare BILNEW). È la validazione
  chiave: l'algoritmo è replicato dai sorgenti ma non ancora verificato sui numeri.
- Punti INCERTI dall'analisi Fable 5: dipendenza dall'ordine in FASE 3 (qui:
  ordine legacy COR_RIGA); casi 21900/24400 disattivati nel sorgente (non gestiti);
  confine attivo/passivo dentro SP (non serve al calcolo).

Versione 0.11.0.
