# Migration tracker — .scx logic ↔ web

`scx_migration_tracker.csv` mette in relazione ogni pezzo di logica dei form
legacy VFP (`*.scx`: metodi, bottoni, eventi, proprietà rilevanti) con la sua
controparte web, oppure ne documenta il motivo di non-migrazione. È letto a
runtime dal viewer web (voce menu Strumenti › "Tracciamento migrazione",
`/utilita/migrazione`), quindi vive nel classpath: modificalo QUI.

## Colonne (schema stabile — non rinominare)
`form_file, object_name, object_type, base_class, member, member_kind, purpose,
source_tables, status, new_location, reason_code, reason_detail`
- `member`: metodo/evento o `PROPERTY:<nome>` (es. `Click`, `Init`, `PROPERTY:RecordSource`)
- `source_tables`: tabelle legacy toccate, `;`-separate (es. `U_CLI_AN;CONTI`)
- `status`: `MIGRATED | PARTIAL | NOT_MIGRATED | NOT_APPLICABLE`
- `new_location`: dove vive nel web (controller#m / template / endpoint / js); vuoto se non migrato
- `reason_code` + `reason_detail`: solo se NON `MIGRATED`

## Reason codes (set standard)
`WRITE_SIDE` (scrittura/posting, riservata Opus) · `OUT_OF_SCOPE` (fuori dalla
consultazione read-only) · `PRINT_REPORT` (stampa/report .frx) · `LEGACY_UX`
(UX legacy non necessaria) · `DUPLICATE` (già coperto altrove) · `DEFERRED`
(rinviato) · `NEEDS_DOMAIN` (serve Erasmo/cliente) · `DATA_QUALITY` (dati legacy
da sistemare) · `VFP_ONLY` (focus/tasti/OLE senza equivalente) · `NOT_APPLICABLE`
(setup UI VFP senza logica di business).

## Workflow
Per ogni form analizzato: parsa l'.scx (ricetta in CLAUDE.md §4b), e per ogni
metodo con codice / proprietà rilevante aggiungi UNA riga. Non cancellare righe:
aggiorna `status`/`new_location` quando un pezzo viene migrato in seguito.


## Catalogo oggetti e grafo dei legami (sessione 12)

- `program_objects.csv` — `object_id,file,object_type,name,description,new_location`.
  Tipi: `FORM` (descrizione = Caption), `PRG` (descrizione = commento di testa),
  `PROCEDURE` (una riga per ogni PROCEDURE/FUNCTION di un PRG-libreria, id
  `prg:<file>::<NOME>`, descrizione = commento adiacente), `FRX`, `MENU`
  (le voci di menu come nodi genitore; deviazione dichiarata rispetto all'enum
  del mandato: servono al viewer per mostrare i genitori con etichetta), `WEB`
  (endpoint AquariusWeb, target dei MIGRATED_TO). `new_location` = ponte
  legacy→web quando presente.
- `program_links.csv` — `parent_id,child_id,link_type,evidence` con
  `link_type` ∈ MENU_TO_FORM, OPENS_FORM, CALLS_PRG (DO x / SET PROCEDURE),
  CALLS_PROC (chiamata a procedura di libreria: DO diretto o `nome()` con
  nome ≥5 caratteri per evitare falsi positivi), PRINTS_REPORT, MIGRATED_TO
  (dal tracker `new_location` e dall'audit `evidenza_web`), UNRESOLVED
  (chiamate dinamiche &macro/EXECSCRIPT — mai inventate).

Perimetro: chiusura raggiungibile dal menu (885 form referenziati dalle voci +
form aperti da questi + 366 PRG chiamati, di cui 123 librerie scomposte in 975
procedure). I ~603 target `DO <nome>` che non corrispondono a file su disco né
a procedure di libreria univoche (nomi locali al form, o fuori chiusura) NON
sono stati forzati a legami. Viewer: `/utilita/legami`.
