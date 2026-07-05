# 2026-07-05 — Voci menu contabilità come sintetiche

## Problema
Dopo aver applicato la slice contabilità, le voci Primanota/Storico/Bilancio/
Partitari NON comparivano nella sidebar. Erano mappate solo via FORM_TO_URL
(menu_primanot → /contabilita/primanota), che funziona SOLO se il tbl_menu
legacy ha righe che puntano a quei form. In Impresind quelle righe non ci sono
(o hanno nomi diversi), quindi niente da tradurre → nessuna voce.

## Soluzione
Iniettate come voci SINTETICHE sotto il top-level "contabilita", con lo stesso
meccanismo già usato per magazzino/parametri (injectSyntheticEntries). Così
compaiono sempre, a prescindere dal menu legacy:
- Primanota, Storico contabile, Bilancio, Partitario clienti, Partitario
  fornitori, poi separatore, poi gli shortcut Piano dei conti già esistenti.
hasSyntheticEntries copre già "contabilita", quindi il top-level resta visibile
anche se le sue voci L2 legacy non sono raggiungibili.

Nota: le entry FORM_TO_URL contabili restano (innocue) come fallback se un
domani il tbl_menu puntasse davvero a quei form.

## Dipendenza
Le voci sintetiche compaiono se esiste il top-level "contabilita" nel menu
legacy — presente (gli shortcut Piano dei conti già si vedevano).

## Verifica (sandbox)
- Java brace/paren OK.
- Patch ribasata sopra supplier+accounting: git apply --check pulito.
- Le 5 voci presenti nel MenuService risultante.

## Non verificato (deploy)
- Comparsa effettiva nella sidebar dopo evictAll/riavvio (il menu è in cache:
  potrebbe servire un riavvio o l'invalidazione cache).
