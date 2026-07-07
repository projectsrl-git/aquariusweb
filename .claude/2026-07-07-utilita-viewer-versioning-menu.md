# 2026-07-07 — Migration viewer + versioning + fix menu/footer

Patch CUMULATIVA da 9f3a66e: include anche lo slice storico/bilancio (non ancora
pushato) + le seguenti novità.

## 1. Viewer migration tracker (voce menu Strumenti)
- CSV spostato nel classpath: `src/main/resources/migration/scx_migration_tracker.csv`
  (+ README.md co-locato) così è leggibile a runtime. Sostituisce la posizione
  docs/migration/ della bozza precedente.
- `MigrationTrackerService` (parser CSV quote-aware) + `MigrationRow` DTO.
- `MigrationController` → `GET /utilita/migrazione` con filtri client-side
  (testo, stato, form, motivo) e riepilogo per stato.
- Voce "Tracciamento migrazione" nella sezione Strumenti della sidebar
  (web-only, sempre visibile — niente dipendenza dai codici menu legacy).

## 2. Versioning
- Regola in CLAUDE.md §4b: semver adattato (PATCH fix / MINOR feature / MAJOR
  milestone), niente -SNAPSHOT sui build deployati.
- Bump pom 0.1.0-SNAPSHOT → **0.2.0**.
- Commit id a build-time: plugin `git-commit-id` →
  `app.commit=@git.commit.id.abbrev@`; `AppVersionService.getCommit()` con
  guardia se il token non si risolve; mostrato in footer come `v0.2.0 · <hash>`.
  Risolve il "non conosco il commit futuro": mostra il commit del build (ultimo
  noto).

## 3/4. Menu navigabile + footer visibile
- `#sidebar`: `min-height:100vh` → `height:100vh; max-height:100vh`. Così
  `.sidebar-nav` (flex:1, overflow-y:auto) scrolla e `.sidebar-foot` (con la
  versione) resta ancorato e visibile. Risolve sia lo scroll (3.1) sia la
  versione fuori pagina (4).
- Accordion (3.2): aprendo un ramo si chiudono i fratelli aperti allo stesso
  livello (e i loro discendenti) → resta aperto un solo ramo.

## Verifica (sandbox)
- Brace/paren OK su tutti i Java; div balance OK sui template; CSV a 12 colonne
  parsato; pom.xml XML valido; patch applica pulita su 9f3a66e (clone fresco).
- NON verificabile in sandbox (confermare al primo deploy): build Maven — in
  particolare il plugin git-commit-id 5.0.0 (se non risolve, il footer omette il
  commit, guardia attiva; il build non deve fallire). Esecuzione su SQL Server.
