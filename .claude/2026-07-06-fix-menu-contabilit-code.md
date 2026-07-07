# 2026-07-06 — Fix voci menu contabilità non attive (codice L1 "contabilit")

## Sintomo
Le voci sintetiche sotto Contabilità — Storico contabile, Bilancio, Partitario
clienti, Partitario fornitori (e Piano dei conti albero/lista) — non comparivano
attive nel menu, nonostante gli endpoint funzionanti e la security aperta. Il
problema persisteva dopo più deploy (cache menu azzerata dal restart Tomcat),
quindi NON era un problema di cache.

## Causa
Il codice L1 (colonna `MENU` di `tbl_menu`) della sezione contabilità è
letteralmente **`"contabilit"`** — non `"contabilita"`. Non è un troncamento: la
colonna è varchar(50); è proprio il valore scelto nel menu VFP legacy (il
docstring di `LegacyMenu` lo elencava già tra gli esempi, e `guessIcon` lo
gestiva con `case "contabilit", "contabilita"`).

`injectSyntheticEntries()` e `hasSyntheticEntries()` matchavano però solo
`"contabilita"`, quindi:
- `guessIcon` matchava "contabilit" → icona calcolatrice OK, sezione visibile;
- l'iniezione delle voci sintetiche NON scattava → voci assenti.

Effetto: la sezione compariva (con Primanota, che arriva dal container legacy
"Prima nota"), ma senza le voci web-only.

## Fix
Allineato `injectSyntheticEntries()` (case) e `hasSyntheticEntries()` a
riconoscere `"contabilit"` oltre a `"contabilita"`, coerentemente con `guessIcon`.
Documentata la gotcha in CLAUDE.md §4.

## Nota di processo
Diagnosi guidata dal fatto che `guessIcon` già conosceva "contabilit": segnale
che il codice reale era quello e che l'iniezione era rimasta indietro. Per
diagnosi future: la riga di log `Menu '...' L1 '...' (menu=XXX): N voci L2`
rivela il codice L1 reale e il numero di voci nel popup.

## Verifica (sandbox)
- Brace/paren MenuService OK. Nessun altro file toccato.
- Build Maven ed esecuzione non verificabili in sandbox → confermare al deploy:
  dopo il restart Tomcat (cache menu svuotata) le 4 voci devono comparire attive
  sotto Contabilità.
