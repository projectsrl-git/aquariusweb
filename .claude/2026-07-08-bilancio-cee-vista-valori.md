# 2026-07-08 — Bilancio CEE: vista valori (prospetto IV direttiva)

Il viewer di Fable 5 mostrava solo struttura/mappatura. Aggiunta la **vista
valori** del prospetto CEE — in sola lettura, senza reimplementare il calcolo.

## Approccio (plug&play)
`BILNEW` contiene già i valori (`CORRENTE`/`PRECEDENTE`) scritti dall'ultimo
"Calcolo bilancio cee" eseguito sul gestionale. Il web li LEGGE e li mostra come
il prospetto legacy (RIGA / DESCRIZIONE / Esercizio in corso / precedente),
formattando per `TIPO_DATO` (I=commento/intestazione, V=dettaglio, T=totale).
NIENTE ricalcolo lato web (resta sul gestionale).

## Implementazione
- `CeeStructureDao`: +DTO `CeeValueRow` +metodo `values(soc)` (legge BILNEW con
  CORRENTE/PRECEDENTE, ordine numerico via PADL/zero-pad a 10).
- `CeeBalanceController` → `GET /contabilita/bilancio-cee` (read-only tenant).
  Segnala se non ci sono valori (calcolo mai eseguito).
- Template `contabilita/bilancio-cee.html`: prospetto con formattazione per
  TIPO_DATO, indentazione preservata (`white-space:pre-wrap`), link alla
  struttura/mappatura.
- Menu: la voce sintetica "Bilancio CEE" ora punta alla vista valori
  (`/contabilita/bilancio-cee`); la struttura è raggiungibile dal pulsante.

## Note
- Valori = ultimo calcolo del gestionale (nota in pagina). Il ricalcolo dal web
  sarebbe scrittura → Opus, in futuro.
- Confine attivo/passivo dentro SP resta implicito nella numerazione (INCERTO da
  analisi Fable 5); qui non serve perché il prospetto è la sequenza legacy.

Versione 0.10.0.
