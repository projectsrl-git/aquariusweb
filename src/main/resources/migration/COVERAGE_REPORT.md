# Audit di copertura della migrazione — AquariusWeb vs menu legacy Aquarius

Fonte: `tbl_menu` (albero completo, 1051 voci eseguibili raggiungibili dagli 11 menu di
primo livello) + screenshot dei sottomenu Contabilità forniti dall'utente (131 voci
classificate a mano voce-per-voce) + `MenuService` (FORM_TO_URL, voci sintetiche) +
rotte dei controller. Dettaglio per singola voce in `menu_coverage_audit.csv`
(9 colonne, quote-aware). ~120 voci di gruppi menu ORFANI (non referenziati da alcun
livello 1) sono escluse dal conteggio; i gruppi orfani sono con ogni probabilità voci
dismesse.

Legenda: **MIGRATO** = equivalente web funzionante (consultazione); **PARZIALE** = esiste ma
con lacune indicate; **MANCANTE** = nessun equivalente web; **NON_APPLICABILE** = scrittura
(riservata a Opus per mandato), stampa legacy, utility VFP o funzione obsoleta. Le voci di
scrittura NON sono 'dimenticate': sono fuori mandato dei bulk read-only per strategia.

## Riepilogo per area

| Area | Migrato | Parziale | Mancante | Non applicabile | Totale |
|---|---:|---:|---:|---:|---:|
| Contabilità | 5 | 4 | 104 | 129 | 242 |
| Vendite / Clienti | 10 | 0 | 91 | 215 | 316 |
| Acquisti / Fornitori | 9 | 0 | 27 | 60 | 96 |
| Magazzino | 6 | 0 | 28 | 54 | 88 |
| Produzione | 2 | 0 | 107 | 41 | 150 |
| Statistiche | 0 | 0 | 36 | 6 | 42 |
| Parametri | 2 | 0 | 1 | 1 | 4 |
| Consulenza (verticale) | 2 | 0 | 24 | 40 | 66 |
| Opzioni / Sistema | 0 | 0 | 0 | 39 | 39 |
| Web legacy | 0 | 0 | 0 | 5 | 5 |
| Help | 0 | 0 | 0 | 3 | 3 |
| **Totale** | **36** | **4** | **418** | **593** | **1051** |

Nota di lettura: il conteggio è per VOCE DI MENU, non per peso funzionale. Molte voci
mancanti sono varianti di stampa della stessa consultazione già migrata; le 36 voci
MIGRATO coprono i flussi consultativi principali (documenti, magazzino, contabilità di
lettura, registri IVA, CEE, parametri).

## Mancanti prioritari (roadmap suggerita per Opus)

Criterio: valore d'uso quotidiano per Impresind (amministrazione e magazzino), fattibilità
read-only, dati già decodificati nelle sessioni di analisi.

1. **Scadenziario clienti/fornitori** (Contabilità > Situazioni) — scadenze aperte per
   cli/for con aging; è la consultazione amministrativa più richiesta. Serve: decodifica
   archivio scadenze (partite aperte — tabelle partitario già mappate in /contabilita/partitari).
2. **Visualizzazione cespiti** (Contabilità > Cespiti) — registro cespiti read-only con quote;
   la parte di scrittura/quote resta a Opus. Serve: decodifica tabelle cespiti (non ancora analizzate).
3. **Centri di costo: elenco + riepilogo + bilancio per cdc** (Contabilità) — l'intera area
   cdc è assente; il bilancio web esiste già, va aggiunta la dimensione cdc (colonne cdc in
   MOV_CONT/CONTI da verificare).
4. **Controlli di quadratura partitari** (prima nota↔partitari, storico↔partitari) — check
   read-only perfetti per il web, coerenti col pannello-anomalie già usato (CEE, valorizzazione).
5. **Controllo sequenza protocolli IVA** in versione read-only (buchi di numerazione sui
   registri già migrati /contabilita/registri-iva).
6. **Situazione incassi/pagamenti + situazione fatture cli/for** (Contabilità > Situazioni) —
   consultazioni finanziarie; base dati = partitari/scadenze.
7. **Tracciabilità documenti** (Vendite/Acquisti > Ristampa) — catena preventivo→ordine→DDT→
   fattura: le chiavi sono GIÀ decodificate (sessione 1: coppia ORC + MOV_NUMFAT su U_BOL_DD);
   è una vista da comporre, non un'analisi nuova.
8. **Situazione/Statistiche ordini** (Vendite e Acquisti) — portafoglio ordini aperto,
   evaso/inevaso: i moduli /ordini e /ordini-fornitore hanno già le colonne di evasione.
9. **Bilancio con confronto anno precedente** — il bilancio di verifica web non ha il
   confronto N-1 (il CEE sì): estensione del modulo esistente.
10. **Bilancio riclassificato** — probabilmente stessa meccanica BILNEW del CEE (righe con
    numerazione diversa): verificare con Erasmo, poi riusare il motore CEE di Opus.
11. **Budget vs consuntivo (Stampa budget)** — consultazione read-only dell'archivio budget;
    serve decodifica tabelle budget.
12. **Statistiche vendite/acquisti** (area Statistiche, 36 voci) — da prioritizzare CON
    l'utente: molte si coprono con viste aggregate sui dati documenti già mappati.

## Aree da chiarire (NEEDS_DOMAIN — domande precise)

- **Produzione non-standard** (107 voci mancanti, quasi tutte nel verticale "film protettivi
  con tappeto"): Impresind usa SOLO la produzione STANDARD (già migrata in consultazione)?
  Se sì, l'intero verticale è NON_APPLICABILE e la copertura produzione è di fatto completa.
- **Consulenza** (24 voci): area verticale attiva per Impresind o retaggio di altro cliente?
- **Gestione della tesoreria / Pagamenti incassi diversi**: perimetro effettivamente usato.
- **Bilancio di verifica civilistico vs gestionale vs standard**: differenze da chiarire con
  Erasmo per decidere se sono viste dello stesso motore.
- **Analisi dati bilancio / Allegati di bilancio**: contenuto da chiarire.

## Bug/osservazioni emersi durante l'audit (da NON correggere qui — nota per Opus)

- Nessun bug bloccante nuovo rilevato nei moduli migrati durante l'audit statico. Restano
  aperte le verifiche già tracciate: riconciliazione motore CEE col gestionale; popolamento
  IVA_MESE zero-padded sui registri; semantica MOV_NREGIS vs MOV_NUMPRO per il drill
  primanota dai registri IVA.

## Dettaglio per sottomenu (solo gruppi con voci consultative)

| Area | Sottomenu | Migr. | Parz. | Manc. | N/A |
|---|---|---:|---:|---:|---:|
| Vendite / Clienti | Agenti | 1 | 0 | 0 | 2 |
| Vendite / Clienti | Anagrafica | 1 | 0 | 6 | 14 |
| Vendite / Clienti | Banche | 1 | 0 | 0 | 1 |
| Vendite / Clienti | Business unit | 0 | 0 | 17 | 17 |
| Vendite / Clienti | Cambi | 0 | 0 | 2 | 2 |
| Vendite / Clienti | Comuni | 0 | 0 | 3 | 0 |
| Vendite / Clienti | Documenti di trasporto | 2 | 0 | 7 | 35 |
| Vendite / Clienti | Fatture clienti | 1 | 0 | 8 | 20 |
| Vendite / Clienti | Fatture proforma | 1 | 0 | 2 | 9 |
| Vendite / Clienti | Interventi/riparazioni | 0 | 0 | 4 | 10 |
| Vendite / Clienti | Listini | 0 | 0 | 5 | 40 |
| Vendite / Clienti | Ordini clienti | 1 | 0 | 12 | 14 |
| Vendite / Clienti | Portafoglio effetti | 0 | 0 | 6 | 14 |
| Vendite / Clienti | Preventivi clienti | 0 | 0 | 6 | 11 |
| Vendite / Clienti | Provvigioni | 1 | 0 | 6 | 16 |
| Vendite / Clienti | Ristampa documenti | 1 | 0 | 2 | 0 |
| Vendite / Clienti | Schede di sicurezza/SDS | 0 | 0 | 2 | 2 |
| Vendite / Clienti | Sconti | 0 | 0 | 3 | 6 |
| Consulenza (verticale) | Gestione risorse | 0 | 0 | 13 | 20 |
| Consulenza (verticale) | Studi professionali | 2 | 0 | 11 | 20 |
| Contabilità | Acquisti/cessioni intracomunitarie | 0 | 0 | 2 | 7 |
| Contabilità | Bilanci | 2 | 0 | 21 | 15 |
| Contabilità | Bollati | 0 | 0 | 37 | 10 |
| Contabilità | Budget | 0 | 0 | 1 | 5 |
| Contabilità | Business unit | 0 | 0 | 3 | 3 |
| Contabilità | Centri di costo | 0 | 0 | 4 | 3 |
| Contabilità | Cespiti | 0 | 0 | 2 | 11 |
| Contabilità | Compensi amministratore | 0 | 0 | 3 | 5 |
| Contabilità | Controllo di gestione | 0 | 0 | 10 | 6 |
| Contabilità | Gestione del personale | 0 | 0 | 1 | 5 |
| Contabilità | Partitari | 0 | 0 | 7 | 0 |
| Contabilità | Piano dei conti | 1 | 1 | 0 | 2 |
| Contabilità | Prima nota | 1 | 0 | 0 | 8 |
| Contabilità | Procedure extracontabili | 0 | 0 | 1 | 10 |
| Contabilità | Ritenute acconto professionisti | 0 | 0 | 3 | 7 |
| Contabilità | Situazioni contabili | 1 | 3 | 7 | 3 |
| Contabilità | Tesoreria | 0 | 0 | 2 | 1 |
| Acquisti / Fornitori | Anagrafica | 1 | 0 | 0 | 6 |
| Acquisti / Fornitori | Documenti da fornitori | 1 | 0 | 5 | 4 |
| Acquisti / Fornitori | Documenti di conto lavoro | 3 | 0 | 3 | 8 |
| Acquisti / Fornitori | Ordini di acquisto | 1 | 0 | 7 | 9 |
| Acquisti / Fornitori | Proposte ordini a fornitore | 1 | 0 | 1 | 5 |
| Acquisti / Fornitori | Richieste di acquisto | 0 | 0 | 1 | 4 |
| Acquisti / Fornitori | Ristampa documenti | 1 | 0 | 2 | 0 |
| Acquisti / Fornitori | Trasporti | 1 | 0 | 8 | 4 |
| Magazzino | Anagrafica articoli | 1 | 0 | 1 | 20 |
| Magazzino | Controllo e gestione del magazzino | 0 | 0 | 6 | 0 |
| Magazzino | Inventario di magazzino | 0 | 0 | 1 | 2 |
| Magazzino | Magazzino a locazioni | 0 | 0 | 3 | 2 |
| Magazzino | Magazzino cartelle sanitarie | 2 | 0 | 3 | 4 |
| Magazzino | Magazzino documenti cartacei | 2 | 0 | 2 | 4 |
| Magazzino | Movimenti di magazzino | 1 | 0 | 12 | 22 |
| Parametri | Gestione parametri | 2 | 0 | 1 | 1 |
| Produzione | Controllo qualità | 0 | 0 | 7 | 1 |
| Produzione | Distinte base | 1 | 0 | 2 | 6 |
| Produzione | Produzione bombole ossigeno e altri gas | 0 | 0 | 10 | 3 |
| Produzione | Produzione dadi pressati | 0 | 0 | 8 | 2 |
| Produzione | Produzione dispositivi industriali | 0 | 0 | 7 | 0 |
| Produzione | Produzione dispositivi medicali | 0 | 0 | 14 | 3 |
| Produzione | Produzione film protettivi con pianificazione | 0 | 0 | 14 | 3 |
| Produzione | Produzione film protettivi con tappeto prod. | 0 | 0 | 22 | 14 |
| Produzione | Produzione pannelli truciolari | 0 | 0 | 12 | 3 |
| Produzione | Produzione standard | 1 | 0 | 10 | 3 |
| Produzione | produzione | 0 | 0 | 1 | 0 |
| Statistiche | Statistiche varie | 0 | 0 | 36 | 6 |

Il dettaglio voce-per-voce, con evidenza (rotta web o pattern) e note, è in `menu_coverage_audit.csv`.
