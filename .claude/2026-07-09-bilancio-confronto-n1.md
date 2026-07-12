# 2026-07-09 — Bilancio di verifica: confronto anno precedente (N-1)

Opzione "Confronta con anno precedente": accanto a Saldo mostra il Saldo anno-1
e la Variazione, a livello mastro/gruppo/sottoconto (vista mastri, full-width).

- Controller: param `confrontoN1`; se attivo carica i saldi dell'anno-1 (stesso
  periodo mese dal/al) via findBilancio/findBilancioPeriodo, mappa conto→saldoN1,
  li assegna alle righe. `buildGroups` accumula saldoN1 su mastri/gruppi.
- DTO: `BilancioLine.saldoN1` (+getVariazione); `BilancioGroup.saldoN1`
  (+addSaldoN1/getVariazione).
- Template: colonne "Saldo anno prec." e "Variazione" (verde/rosso) condizionali
  a confrontoN1; colspan righe vuote adattato (4→6).

Nota: confronta i conti presenti nel bilancio dell'anno corrente (con attività).
Conti attivi solo in N-1 non compaiono. Coerente col periodo selezionato.
Versione 0.17.0.
