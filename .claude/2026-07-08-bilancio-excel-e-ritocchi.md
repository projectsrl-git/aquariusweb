# 2026-07-08 — Bilancio: export Excel + ritocchi sintesi/quadratura

- **Export Excel** (4): pulsante "Esporta Excel" → `GET /contabilita/bilancio/export`
  (`ResponseEntity<byte[]>`, POI, come la valorizzazione magazzino). Nuovo
  `BilancioExcelExporter`: foglio "Dettaglio" (una riga per conto: Sezione/Conto/
  Descrizione/Tipo/Prog.Dare/Prog.Avere/Saldo, celle numeriche) + foglio "Sintesi"
  (totali + quadratura). Il controller ricalcola i dati con la stessa
  classificazione della vista (`buildBilancioExportData`).
- **UX** (1/2/3): "Il bilancio quadra" più grande (fs-5); nel riquadro quadratura
  la voce "Differenza" (0,00) rinominata "Quadratura"; card di sintesi SP e CE
  ora con bordo (`border`).

Solo `bilancio.html` + `ContabilitaController` + nuovo `BilancioExcelExporter`.
Versione 0.9.0.
