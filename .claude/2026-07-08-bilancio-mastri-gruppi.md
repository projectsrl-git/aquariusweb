# 2026-07-08 — Bilancio raggruppato per mastri/gruppi (Bilancio di verifica)

Ristrutturazione del bilancio in albero **Mastro → Gruppo → Sottoconto** con
progressivi **Prog. Dare / Prog. Avere / Saldo** (come il PDF "Bilancio di
verifica"). Risolve anche il punto 1.2 (quadratura col toggle C/F).

## Struttura codice conto (verificata su dati Impresind)
Piano dei conti multilivello: **Mastro = primi 2 char**, **Gruppo = primi 4 char**,
sottoconti = codici lunghi (10-11). Mastri e gruppi esistono come righe `CONTI`
(con descrizione), quindi l'albero si costruisce per **prefisso** (parameter-free,
niente PUB_MASTRO hardcoded): foglia → gruppo (primi 4) → mastro (primi 2), con
descrizione presa dall'anagrafica `CONTI`.

## Modello
- Nuovo DTO `BilancioGroup` (nodo albero: code, description, level, totDare,
  totAvere, saldo, subGroups, lines, hasHiddenCF).
- Controller: `buildGroups(lines, accByCode, showCF)` costruisce l'albero per
  sezione. I progressivi Dare/Avere sono aggregati da TUTTE le righe (C/F
  inclusi); `showCF` filtra solo la VISUALIZZAZIONE delle foglie C/F, non i
  totali → i totali di sezione e la quadratura NON dipendono più dal toggle
  (fix 1.2). `descOf()` risolve le descrizioni mastro/gruppo.

## Template
- Sezioni full-width dentro i TAB SP/CE: albero mastro (riga bold) → gruppo
  (indentato) → sottoconti, colonne Prog. Dare / Prog. Avere / Saldo, totale di
  sezione nel footer. Quando C/F è off, un gruppo con conti C/F nascosti mostra
  una nota ("importi inclusi nel totale del gruppo").
- Quadratura (1/1.1): il banner mostra ora la DIFFERENZA (Attività−Passività) −
  utile, che è **0,00 quando quadra** (verde), sbilancio quando non quadra (rosso).

## Verifica
Sandbox: brace/paren Java OK (controller + BilancioGroup), div/table/th:block
template bilanciati. Build Maven + esecuzione SQL Server da confermare al deploy.
Versione 0.6.0.

## Note / prossimi
- Il livello intermedio "sottogruppo" (codici 7-9 char, rari) non è reso come
  livello a sé: le sue foglie confluiscono nel gruppo di primi 4 char. Se servisse
  esplicito, si aggiunge un livello.
