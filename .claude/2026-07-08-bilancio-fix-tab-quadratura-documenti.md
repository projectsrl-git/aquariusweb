# 2026-07-08 — Fix bilancio (toggle/TAB/quadratura/allineamento) + bug /documenti

## Bug bloccante /documenti (ristampa documenti)
`DocumentArchiveDao.search` costruiva `ORDER BY tt.<col> <dir>, tt.ORD_NUMORD <dir>`:
ordinando PER numero la colonna si duplicava → SQL Server "colonna specificata più
volte nell'ORDER BY" → pagina in errore. Ora il tie-breaker `ORD_NUMORD` si aggiunge
solo se non è già la colonna d'ordine.

## Bilancio
1. **Toggle "Mostra conti clienti/fornitori"**: la checkbox deselezionata non
   inviava il parametro → `defaultValue=true` lo rimetteva sempre su "mostra".
   Fix: hidden `cf` aggiornato via JS (checkbox senza name).
2. **TAB Stato Patrimoniale / Conto Economico**: le due sezioni sono ora in tab
   Bootstrap → si passa SP↔CE senza scorrere in fondo. Il quadro di sintesi resta
   sempre visibile in testa.
6. **Allineamento totali**: il totale di sezione è passato da `<tfoot>` a
   `card-footer`; con `h-100` + `align-items-stretch` le due colonne hanno pari
   altezza e i totali si allineano in fondo.
7. **Quadratura di bilancio**: banner in testa che verifica
   Attività − Passività = risultato d'esercizio (tolleranza 0,01), con importo e
   sbilancio se non quadra. Attributi `quadraturaSP/sbilancio/quadraturaOk` nel
   controller.

## Rinviato a slice dedicate (non in questa patch)
- **4. Raggruppamento per mastri/gruppi** (come il PDF "Bilancio di verifica"):
  richiede di derivare la gerarchia dai segmenti del codice conto + progressivi
  Dare/Avere/Saldo. Ristrutturazione del modello dati bilancio.
- **5. Bilancio CEE**: analisi + riproduzione della mappatura piano dei conti →
  voci CEE (nei form/tabelle legacy). Deep domain.
- **3.1/3.2. Titolo pagina = voce menu + menu che resta aperto/evidenziato sulla
  voce corrente**: passata layout/menu (delicata, Opus).

## Verifica
Sandbox: brace/paren Java OK, div/table template bilanciati, no doppio
th:classappend. Build Maven + esecuzione SQL Server da confermare al deploy.
Versione 0.4.1 (patch).
