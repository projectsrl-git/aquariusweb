# 2026-07-05 — Prima nota: menu unificato (CRUD unica) + voci contabilità

## Contesto
Lo screenshot ha mostrato che il tbl_menu legacy ESPONE già un sottomenu
"Prima nota" con molte voci frammentate (Inserimento effettiva/previsionale,
Aggiornamento, Ricerca, Annullo, Duplica, Blocco/sblocco, Import...), tutte
grigie "(da migrare)" perché i loro form non erano mappati.

## Decisione (utente)
Nel nuovo sistema quelle voci NON restano separate: sono tutte operazioni di
un'UNICA CRUD sulla registrazione. Quindi il sottomenu frammentato va
sostituito da una sola voce "Prima nota".

## Implementazione (MenuService)
- toNodeFromL2: quando il container L2 è la "Prima nota"
  (isPrimaNotaContainer, match su label), le foglie legacy vengono sostituite
  da UNA voce web unica → /contabilita/primanota (la CRUD; oggi consultazione,
  domani con le azioni di scrittura sulla stessa pagina).
- injectSyntheticEntries "contabilita": restano Storico contabile, Bilancio,
  Partitario clienti/fornitori + shortcut Piano dei conti. La Primanota NON è
  più sintetica (arriva dal container trasformato) per evitare doppioni.

## Nota onesta
La CRUD di SCRITTURA (inserimento/modifica/duplica/annullo=storno) non esiste
ancora: la voce unica oggi apre la consultazione read-only. Le azioni di
scrittura saranno aggiunte alla stessa pagina nella slice data-entry (dipende
da CONTABILELIB + contatore PARA, da chiudere con Erasmo). Così la voce di menu
è già quella definitiva; cresce la pagina, non il menu.

## Verifica (sandbox)
- Java brace/paren OK; isPrimaNotaContainer definito+usato; safeLower presente.
- Ribasata su supplier+accounting: git apply --check pulito.

## Sostituisce
La patch precedente aquariusweb-2026-07-05-menu-contabilita (che iniettava
anche una Primanota sintetica): NON applicare quella; questa la ingloba.

## Non verificato (deploy)
- Match del container per label "prima nota" sul dato reale (se la label nel
  tbl_menu differisce, adeguare isPrimaNotaContainer).
- Cache menu: serve riavvio/evict per vedere il cambiamento.
