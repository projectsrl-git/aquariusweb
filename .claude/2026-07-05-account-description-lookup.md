# 2026-07-05 — Fix descrizione conto vuota in primanota/bilancio

## Problema
Nel dettaglio registrazione (e nel bilancio) la colonna "Descrizione conto"
era vuota, pur avendo i codici conto corretti (es. 6061000002).

## Causa
Disallineamento di trim tra chiave mappa e chiave di lookup:
- accountNameMap() usa a.getCode().trim() come chiave.
- Il lookup usava r.account / r.getAccount() RAW (MOV_CONTO è char(13),
  space-padded nel legacy), quindi "6061000002   " non trovava "6061000002".

## Correzione
- registrazione.html: lookup con r.account.trim()
  (${r.account != null ? (accountNames.get(r.account.trim()) ?: '') : ''}).
- ContabilitaController.bilancio(): names.get(r.getAccount().trim()).
- Lo storico già usava conto.trim() sul lato input → invariato.

## Verifica (sandbox)
- Controller brace/paren OK.
- Ribasata su supplier+accounting+primanota-menu+authz: git apply --check pulito.

## Non verificato (deploy)
- Descrizioni visibili sul dato reale. Se restassero vuote, la causa
  alternativa sarebbe che i conti in CONTI non sono valorizzati per l'anno
  corrente (CON_ANNO) — ma il tree conti funziona con lo stesso filtro, quindi
  il trim è la causa più probabile.

## Nota (Problema 2 utente — voci menu)
Verificato che il MenuService espone già sotto Contabilità: Prima nota,
Storico contabile, Bilancio, Partitario clienti/fornitori, Piano dei conti
(albero+lista). Se lo screenshot ne mostrava meno era per cache menu / patch
non ancora applicata. Nessuna modifica necessaria: appaiono dopo deploy+riavvio.
