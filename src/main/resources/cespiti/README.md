# Cespiti — mappa archivi e decodifiche (analisi Fable 5, sessione 8)

Fonte: form legacy `MENU_AMMCES` (gestione anagrafica), `MENU_stat_cesp`
("Visualizzazione / stampa cespiti", caption verbatim), `MENU_AMMCAT`
(categorie), menu `gestionecespiti` / `gestioneammortamenti` in tbl_menu,
schema `docs/db_schema/aquarius_schema_full.csv`.

## Tabelle

| Tabella | Ruolo | Chiave logica |
|---|---|---|
| `u_amm_at` | Anagrafica cespiti (28 col) | `AMM_CODCES` (16) |
| `u_amm_ca` | Categorie di ammortamento (8 col) | `AMM_CODAMM` (10) |
| `U_QUO_AM` | Quote di ammortamento per anno (21 col) | `QUO_CODCES` + `QUO_ANNRIF` |
| `u_amm_ad` | Movimenti/documenti collegati al cespite (15 col) | `AMD_CODCES` + `AMD_SEQUEN` |

## Fatti verificati (con evidenza)

- **Nessuna dimensione società**: nessuna colonna soc su nessuna delle
  quattro tabelle; in `MENU_stat_cesp` e `MENU_AMMCES` il filtro
  `PUB_CODSOC` è applicato SOLO alle query sui `CONTI`
  (`x_soci = "con_soc = ..."`), mai agli archivi cespiti. L'archivio è
  unico per installazione. Il viewer web lo dichiara nel sottotitolo e
  non filtra per società.
- **Ordine legacy** dell'elenco: `order by amm_codcat, amm_codces`
  (query verbatim in MENU_stat_cesp).
- `AMM_DATCES` non vuota = cespite **ceduto/dismesso** (con
  `AMM_VALCES` valore di cessione e `AMM_PLUMIN` plus/minusvalenza).
- Tre canali di ammortamento in parallelo su anagrafica e quote:
  **ordinario / anticipato / accelerato** (`*ORD`, `*ANT`, `*ACC` con
  percentuale, valore e fondo ciascuno); `TOTAMM` = totale ammortizzato,
  `VALRES` = residuo.
- `QUO_FLGCGE` (bit) = quota **trasferita in contabilità generale**
  (gestita da `MENU_QUO_AM_COGE` sblocca/blocca e dal trasferimento
  `MENU_TFCQUOTEAMM`).
- `u_amm_ad` è alimentata da `MENU_AGG_DETT_CESPITI` ("Aggiornamento
  dettagli cespiti da movimenti contabili"): porta `AMD_NREGIS` +
  `AMD_ANNO` → drill possibile verso `/contabilita/primanota/{nreg}`,
  con il caveat che la rotta usa l'esercizio in sessione (se la
  registrazione è di un anno diverso il dettaglio può non trovarla —
  dichiarato in UI).
- `AMM_FLGUSO = 'S'` = bene acquistato **usato** (badge in UI; incide
  sulla % del primo anno nel calcolo legacy).

## Esclusioni (mandato)

Tutta la parte di scrittura/calcolo resta sul gestionale ed è riservata
a Opus: generazione/cancellazione quote (`MENU_QUO_AM_GEN/CANC`),
simulazione (`MENU_QUO_AM_SIMU`), trasferimenti in coge, parametro IRES,
legame prima nota-cespite, `POPOLA_CESPITI_CATEGORIE_DA_CONTI_E_MOV_CONT`
e `ALLINEA_CESPITI_QUOTE_AMM` (procedure di supporto).

## Quote per anno — semantica verificata (sessione 9)

Evidenze da `ALLINEA_CESPITI_QUOTE_AMM.PRG` e `MENU_quo_am_gen.SCX`
(generatore quote; identica logica in `MENU_quo_am_simu.SCX`):

- **`QUO_VALORD/VALANT/VALACC` = quota DELL'ESERCIZIO** per canale;
  **`QUO_FONORD/FONANT/FONACC` = fondo PROGRESSIVO** a fine esercizio:
  `MENU_quo_am_gen`: `_X_QUO_FONORD = _X_QUO_VALORD + QUO_FONORD`
  (quota dell'anno + fondo dell'anno precedente; primo anno:
  `= _X_QUO_VALORD`).
- **`QUO_TOTAMM` = totale delle quote DELL'ESERCIZIO** (non il
  progressivo): `ALLINEA_CESPITI_QUOTE_AMM.PRG:80`
  `REPLACE QUO_TOTAMM WITH QUO_VALORD + QUO_VALANT + QUO_VALACC`.
  Il progressivo è la somma dei fondi.
- L'allineamento anagrafica↔quote confronta i progressivi anagrafici
  con l'ULTIMA riga quote (`order by quo_annrif DESC`, r.31):
  `_DELTA_FONORD = M.AMM_VALORD - QUO_FONORD` (r.52) → sull'ultima
  riga, `QUO_FONORD` deve coincidere con `AMM_VALORD` dell'anagrafica
  (idem ANT/ACC); `QUO_VALRES` riallineato a `AMM_VALRES` (r.81).
- **`QUO_VARFIS` = variazione fiscale = quota ANTICIPATA
  dell'esercizio** (`MENU_quo_am_gen`: `_X_QUO_VARFIS = _X_QUO_VALANT`):
  l'ammortamento anticipato è una variazione extracontabile.
- **`QUO_IMPDIF` = imposte differite = VARFIS × aliquota IRES**
  (`_X_QUO_IMPDIF = (_X_QUO_VARFIS * _W_PERC_IRES) / 100`, parametro
  da `MENU_para_ires`).

Ricaduta UI (dettaglio cespite): colonna "Fondo ord. progressivo"
(=FONORD) aggiunta; la colonna QUO_TOTAMM è etichettata "Quote
esercizio" (era ambiguamente "Totale ammortizzato"); Var. fiscale e
Imp. differite esposte così come sono.
