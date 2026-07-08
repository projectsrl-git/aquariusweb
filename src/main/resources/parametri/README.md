# Catalogo parametri aziendali (MENU_AZI000 → U_AZI_AN / U_AZI_PA / U_AZI_PB)

`parametri_aziendali_catalog.csv` è il risultato dell'analisi DEEP del form
legacy `MENU_AZI000.SCX` (l'"Aggiornamento parametri aziendali" di Aquarius):
per ogni parametro esposto dal form, il catalogo dice **a cosa serve** e
**come funziona**, con le evidenze nei sorgenti VFP. È letto a runtime dal
viewer `/parametri-aziendali`.

## Schema colonne (stabile, CSV quote-aware UTF-8)

| colonna | contenuto |
|---|---|
| `group` | tab/sezione del form, con gerarchia `Tab > Sotto-tab` (dal pageframe) |
| `object_name` | OBJNAME del controllo nel form |
| `label` | etichetta utente: Caption del checkbox, o label adiacente per textbox |
| `table_column` | `TABELLA.COLONNA` del parametro, oppure `(colonna assente nel DB Impresind)` |
| `type` | `boolean` \| `text` \| `list` (dal tipo di controllo) |
| `allowed_values` | per le optiongroup: le opzioni, separate da `\|` |
| `purpose` | a cosa serve (linguaggio business; per i flag è la caption descrittiva del form) |
| `how_it_works` | comportamento tecnico pilotato, con la catena di caricamento e un estratto di codice |
| `used_in` | file/funzioni dove il parametro è usato, separati da `;` |
| `confidence` | `ALTA` \| `MEDIA` \| `INCERTO` |
| `notes` | note (colonna assente, controllo duplicato su più tab, ecc.) |

## Architettura scoperta (chiave di lettura di `how_it_works`)

Il pattern fondamentale, verificato in `APPLILIB.PRG`, funzione **AQUADOCU**
(startup applicativo): ogni colonna `AZI_X` delle tabelle `U_AZI_*` viene
caricata in una **variabile pubblica `PUB_X`** (712 mappature `PUB_X = AZI_X`).
Il resto del gestionale legge quasi sempre le `PUB_*`, non le tabelle: quindi
l'uso reale di un parametro va cercato sulla variabile PUB corrispondente.
Fanno eccezione le librerie di stampa/FE (ristampelib, OFFSTAM*, CONTABILELIB,
FATTURAZIONE_ELETTRONICA_LIB), che leggono alcune colonne anagrafiche
direttamente dalle tabelle per intestazioni e XML.

## Criteri di confidence

- **ALTA** — etichetta descrittiva nel form + catena `AZI→PUB` (AQUADOCU) +
  almeno un uso puntuale in un file corrente (preferito un costrutto
  `IF/CASE/IIF` sulla PUB), citato in `how_it_works`.
- **MEDIA** — evidenza presente ma etichetta corta/generica, oppure solo
  dichiarazioni di startup senza uso condizionale individuato.
- **INCERTO** — nessuna etichetta associabile, nessun uso nei sorgenti
  correnti (solo replica/backup: possibile parametro orfano), o colonna
  assente nel DB Impresind (parametro di altra release/verticale del form).

## Metodo (riproducibile)

1. Parsing di `MENU_AZI000.SCX` (DBF+FPT, ricetta CLAUDE.md §4c): 845 controlli
   con `ControlSource` su memvar `M.AZI_*`; caption pagine dai record pageframe
   (`PageN.Caption`); label dei textbox agganciate per prossimità geometrica
   (stessa page, stessa riga o adiacente).
2. Risoluzione colonna→tabella su `docs/db_schema/aquarius_schema_full.csv`
   (743 risolte: 234 AN, 249 PA, 260 PB; 98 colonne del form assenti a
   Impresind, incluse nel catalogo con nota).
3. Estrazione mappa `AZI→PUB` dal corpo di AQUADOCU; indicizzazione degli usi
   `PUB_*` su tutti i `prg/*.PRG` correnti (esclusi backup datati e librerie
   di sola replica: REPLIB/REPLIBS/PULISCI_ARCHIVI), preferendo i costrutti
   condizionali come evidenza.
4. Nessuna interpretazione inventata: dove l'uso non è stato trovato il
   parametro è marcato INCERTO con nota esplicita.

Il catalogo NON è un settings editabile: la modifica dei parametri è scrittura
su tabelle che pilotano il comportamento del gestionale (riservata a slice
future con Opus). Il viewer è in sola lettura.
