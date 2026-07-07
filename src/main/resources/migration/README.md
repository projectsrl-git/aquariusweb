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
