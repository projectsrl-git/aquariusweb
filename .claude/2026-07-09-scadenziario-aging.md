# 2026-07-09 — Scadenziario clienti/fornitori con aging

Consultazione read-only delle partite APERTE con aging degli importi.
- Fonte: PART_CLI / PART_FOR. Aperto = totale − pagato (`getResidual()`),
  scadenza `PAR_DTSCAD`. Nuovo `findAperte(soc,anno)` sui due repository
  (COALESCE total <> paid).
- `ScadenziarioController` → `/contabilita/scadenziario?tipo=clienti|fornitori`:
  aggrega per anagrafica, aging su giorni di scaduto a oggi in fasce
  (a scadere, 0-30, 31-60, 61-90, oltre 90). DTO `ScadenzaRiga`.
- Template `contabilita/scadenziario.html`: toggle clienti/fornitori, sintesi
  (a scadere/scaduto/totale), tabella per anagrafica con fasce colorate + totali.
- Menu: voce sintetica "Scadenziario" sotto Contabilità.

Versione 0.18.0 (consegnata insieme a 0.17.0 confronto N-1).
