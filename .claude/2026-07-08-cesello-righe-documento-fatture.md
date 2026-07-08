# 2026-07-08 — Cesello dettaglio righe documento (FATTURE, riferimento)

Rifinitura (cesello, non nuovo modulo) sul dettaglio documento. Fatta PRIMA sulle
fatture come riferimento, per validare la parte non testabile (lettura del CLOB
`ORD_NOTE`, tipo SQL Server `text`, via jTDS/Hibernate) prima di replicarla a
ordini/DDT/proforma.

## Cosa fa
1. **Esplosione commenti**: le righe `*** COMMENTO ***` (convenzione legacy su
   `ORD_DESART`) ora mostrano il testo reale del commento, che sta nel CLOB
   `ORD_NOTE` — coerente con `FATTURAZIONE_ELETTRONICA_LIB.PRG`
   (`ORD_DESART='*** COMMENTO ***' → SUBSTRING(ORD_NOTE,1,1000)`).
2. **Righe espandibili**: click sulla riga → si apre un pannello con i campi di
   dettaglio finora nascosti, raggruppati (UM/peso, misure sp/h/l, ordine
   origine, destinazione, note, note interne). Mostrati solo se valorizzati.

## Implementazione
- `InvoiceRow`: +campi `um, umFinal, weightPerUm, thickness, height, length,
  sourceOrderNo, sourceOrderDate, destination, note (ORD_NOTE), noteInternal
  (ORD_NOTEBO)`. I due CLOB mappati con `columnDefinition="text"` (read-only).
  +`getCommentText()` (nota esplosa o placeholder) +`isHasDetail()`.
- Nuovo fragment condiviso `fragments/doc-row.html` (`detailRow(r,cols)` +
  `toggleScript`), riusabile da ordini/DDT/proforma.
- `fatture/detail.html`: riga cliccabile (`doc-main-row`), commento esploso,
  riga-dettaglio via fragment, script incluso una volta.

## Da confermare al deploy (RISCHIO non testabile)
- Che `ORD_NOTE`/`ORD_NOTEBO` (colonne `text`) si leggano correttamente come
  String via jTDS/Hibernate su SQL Server 2012 (mapping `columnDefinition="text"`,
  entity read-only). Se ok su fatture → replico identico su ordini/DDT/proforma.
- Se Hibernate desse noia sul tipo `text`, valutare `@Lob` come alternativa.

## Prossimo passo
Replicare a `ordini`/`ddt`/`proforma` (stessi campi/fragment), adattando le
colonne che differiscono per tabella (es. `ORS_NUMORC` assente su `U_ORD_DD`).
