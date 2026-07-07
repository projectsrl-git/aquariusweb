# 2026-07-07 — Storico autocomplete + Bilancio a sezioni contrapposte

Copre i punti 1 e 2 (2.1, 2.2) della richiesta. I punti 3.x e il piano dei conti
a sezioni sono pianificati come slice successive (vedi note in chat).

## Punto 1 — Autocomplete conto nello storico
- Nuovo endpoint JSON `GET /conti/autocomplete?q=` (AccountController,
  @ResponseBody): cerca per codice OPPURE descrizione (riusa
  `searchByYearAndSociety`), ritorna `[{code, description}]` (max 15).
- `storico.html`: il campo conto è ora un combobox live (vanilla JS, nessuna
  dipendenza): digitando ≥2 caratteri appare la tendina; selezione con
  mouse/frecce/Invio; un hidden `conto` porta il codice al server. Fallback: se
  si digita un codice e si invia senza selezionare, il codice è ricavato dal
  testo (parte prima di " — ").

## Punto 2 — Bilancio a sezioni contrapposte (SP / CE)
Classificazione (da CONTI + report VFP fanni210.prg):
- `CON_POSBIL`: 'P' = Stato Patrimoniale, 'E' = Conto Economico.
- Lato per segno del saldo (Dare − Avere): P + Dare→Attività, P + Avere→Passività;
  E + Dare→Costi, E + Avere→Ricavi.

Implementazione:
- `bilancio()` arricchisce ogni riga con sezione (CON_POSBIL) e tipo conto
  (CON_TIPOCO) via mappa `accountByCode()`, filtra i conti a saldo zero, e
  smista in 4 bucket (attivo/passivo/costi/ricavi) + "non classificati".
- `BilancioLine` esteso con `section`, `accountType`, `getDisplayAmount()`
  (saldo assoluto), `isDareSide()`, `isPatrimoniale/isEconomico`,
  `isCustomerOrSupplier()`.
- Totali per commercialista: Attività, Passività, Costi, Ricavi + Risultato
  d'esercizio (Ricavi − Costi, utile/perdita). Sostituiscono i vecchi
  totale Dare / totale Avere / sbilancio.
- `bilancio.html` riscritto: quadro di sintesi (SP e CE), prospetto a due
  colonne per sezione (fragment `sezione`), risultato in evidenza, pannello
  "conti senza posizione di bilancio" (segnalati, non nascosti).

### 2.1 — Toggle clienti/fornitori
`?cf=true|false` (switch in testa): con cf=false i conti CON_TIPOCO C/F sono
esclusi dal prospetto.

### 2.2 — Totali in testa
Non più Dare/Avere/sbilancio ma i totali di SP (Attività/Passività) e CE
(Costi/Ricavi) + Risultato d'esercizio.

## Verifica (sandbox)
- Brace/paren lessicale OK (AccountController, ContabilitaController).
- Div balance OK (bilancio 50/50, storico 9/9). Getter BilancioLine ↔ template.
- Build Maven ed esecuzione non verificabili in sandbox → confermare al deploy:
  in particolare che CON_POSBIL sia popolato sui conti (i conti senza posbil
  finiscono, correttamente, nel pannello "da classificare").
