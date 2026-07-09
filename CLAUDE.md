# CLAUDE.md — AquariusWeb

> **Read this first.** This file is the memory bridge between chat sessions and
> the repository. It records the mission, the architecture, the legacy domain
> knowledge painfully reverse-engineered from the VFP sources, and the hard
> constraints. Keep it TRUE: every change must update it (see `.claude/README.md`
> for the working agreement).

---

## 1. Mission

**AquariusWeb is the progressive re-dressing of a 20+ year old Visual FoxPro
ERP ("Aquarius", by Project S.r.l.) as a modern Java web application — without
ever stopping or breaking the legacy system.**

Strategy **"1.3 plug&play"**, the project's prime directive:

- The web app runs **in parallel** with the VFP client, **on the same SQL
  Server database**.
- Legacy tables (`CONTI`, `U_MAG_MO`, `PARA`, `res_oper`, `tbl_menu`, …) are
  **read** (and occasionally updated field-by-field via explicit whitelists)
  but their **structure is NEVER altered**. No new columns, no triggers, no
  index changes on legacy objects.
- Every new table the web app needs is prefixed **`aq_web_`** and created via
  the custom migration runner.
- Functionality is migrated **slice by slice** (one VFP form/flow at a time),
  each slice usable in production while the rest stays on VFP.

The VFP source (forms `.scx/.sct`, programs `.prg`, menus) is on GitHub:
`https://github.com/projectsrl-git/aquarius` — it is the **authority** on
business rules. When in doubt, read the VFP source, do not guess.
(Analysis technique: `.SCT` files are binary-ish; extract strings with
`re.findall(rb'[\x20-\x7e\r\n\t]{8,}', data)` and look for `ControlSource`,
`Caption`, `M.<FIELD>` patterns. `.PRG` files are plain text.)

## 2. Stack & deployment

| Layer | Choice | Notes |
|---|---|---|
| Runtime | Java 17, Spring Boot **2.7.18** (javax.*) | Boot 3 impossible: needs Hibernate 6, whose SQLServer2012 support is gone |
| Packaging | **WAR on external Tomcat 9** | `spring-boot-starter-tomcat` is `provided`; `AquariusApplication extends SpringBootServletInitializer`. `mvn spring-boot:run` still works for dev. `finalName=AquariusWeb` → context path `/AquariusWeb` |
| View | Thymeleaf + layout decorator (`layout.html`), Bootstrap 5 + Bootstrap Icons (CDN), Chart.js (CDN) | |
| Tenant DBs | SQL Server 2012 via **jTDS 1.3.1** | legacy, read-mostly |
| System DB | H2 file-based, `MODE=MSSQLServer` | tenants, super-admins, web credentials, app version |
| Migrations | **custom `SqlMigrationRunner`** (~150 LOC) | NOT Flyway (Community ≥7 dropped SQL Server 2012). `V<n>__<desc>.sql` under `db/migration/{system,tenant}` |
| Excel | Apache POI 5.2.5 (SXSSF) | warehouse valuation export |

**Configuration**: `application.properties` contains NO real hosts/credentials
(public repo). Everything sensitive is `${ENV_VAR:CHANGE_ME}`. Real values go
in env vars (Tomcat `setenv`) or the gitignored
`config/application-local.properties` (loaded via `spring.config.import`).
Runtime files (H2 data, logs) live under `${AQUARIUSWEB_HOME:.}`.

## 3. Architecture map (packages under `com.aquarius`)

- **`multitenancy`** — `TenantContext` (ThreadLocal), `TenantRoutingDataSource`
  (AbstractRoutingDataSource keyed by tenant id), `TenantsProperties` (binds
  `aquarius.tenants.*`). Tenant chosen in the login combobox, carried by
  `TenantRequestFilter`/`TenantAwareAuthenticationFilter`.
- **`config`** — two manually-defined persistence units:
  `SystemDataSourceConfig` (H2, JdbcTemplate-oriented) and
  `TenantDataSourceConfig` (`tenantDataSource`, `tenantEntityManagerFactory`,
  `tenantTransactionManager` — every tenant-side `@Transactional` must name
  it). `SqlMigrationRunner` + `SystemMigrationsConfig` + `TenantMigrationsRunner`.
  `SecurityConfig`, `WebMvcConfig` (registers `FiscalContextInterceptor`).
- **`security`** — legacy login against `res_oper`:
  `AquariusAuthenticationProvider` + `LegacyPasswordVerifier` (the legacy
  password scheme is a Caesar +3 shift — see §7 Security). Web-specific
  credentials in system DB (`WebUserCredentialsRepository`). Principal:
  `AquariusPrincipal` (username, tenant display name).
- **`context`** — `FiscalContext`: session-scoped (CGLIB proxy) bean = the
  modern `PUB_ANNO`/`PUB_CODSOC`. Auto-set to current year by
  `FiscalContextInterceptor` (silent; redirects to `/select-year` ONLY if no
  year exists in PARA). Year switchable from the sidebar badge.
- **`service`** — `MenuService` (see §4), `BreadcrumbService` (DFS over the
  menu tree → crumbs for any URL), `FiscalYearService` (years from PARA prefix
  `ANN`+society), `ParameterCategoryCatalog` (340 PARA categories from
  `data/param-categories.csv`, auto-extracted from VFP PARAGEST calls),
  `AccountTreeService` (§5 PDC), `WarehouseValuationService`/`ExcelExporter`
  (§6), `TenantService`, `AppVersionService`, `CustomReportService`.
- **`controller`** — one per slice + `GlobalModelAdvice` (injects
  `appVersion`, `currentFiscalYear`, … into every model).
- **`entity`** — tenant: `Customer`, `Account`, `ParameterItem`,
  `OperatorUser`, `LegacyMenu`, `CustomReport`; system: `Tenant`,
  `SuperAdmin`, `WebUserCredentials`. Legacy entities are WIDE: always
  `@DynamicUpdate` + explicit `copyEditableFields()` whitelist on save
  (never bind the whole entity from the form).
- **`repository`** — Spring Data JPA on the tenant EM; plain-SQL DAOs
  (`NamedParameterJdbcTemplate` on `@Qualifier("tenantDataSource")`) where
  window functions/CTEs are needed (`WarehouseValuationDao`).

## 4. The menu system (how VFP forms map to URLs)

Legacy `tbl_menu` drives the sidebar. `MenuService`:
- parses each row's `COMANDO` (`do form form\menu_cli000 linked`, PARAGEST
  invocations, submenu openers) and authorization column `UTENTI`
  (`.SER.AMB.` dotted user list);
- `FORM_TO_URL` map: VFP form name → web URL (e.g. `menu_cli000` → `/clienti`,
  `menu_pdc000` → `/conti/tree`). Extend it as slices land;
- PARAGEST commands auto-map to `/parametri/{prefix}`;
- `injectSyntheticEntries()` adds web-only entries at the top of an L1 menu
  (currently: parametri, contabilita, magazzino) — remember to also extend
  `hasSyntheticEntries()`.
- **GOTCHA: the accounting L1 `MENU` code is `"contabilit"`, NOT `"contabilita"`**
  (that's the literal value stored in `tbl_menu`, not a truncation — the column
  is varchar(50)). `guessIcon`, `injectSyntheticEntries` and
  `hasSyntheticEntries` must all match `"contabilit"` (they match both spellings
  for safety). If only some of them do, the section renders (icon + primanota)
  but the synthetic entries — Storico, Bilancio, Partitari, Piano dei conti —
  silently don't appear. Always match the real L1 codes; verify against the log
  line `Menu '...' L1 '...' (menu=XXX): ...`.

## 4b. Versioning

- Versione = `<version>` in `pom.xml` (filtrata in `application.properties` come
  `app.version=@project.version@`). `AppVersionService` la registra su
  `aq_web_app_version` (SYSTEM DB) a ogni deploy con versione nuova.
- **Regola** (semver adattato a prodotto interno):
  - PATCH (0.0.x): fix e ritocchi, nessuna nuova funzionalità utente.
  - MINOR (0.x.0): nuovo modulo/funzionalità utente-visibile (anagrafica,
    prospetto, viewer, ecc.).
  - MAJOR (x.0.0): milestone di prodotto (es. prima release con data-entry
    contabile completo).
  - I build deployati NON usano `-SNAPSHOT` (quello resta per il lavoro locale).
- **Commit id**: catturato a BUILD-TIME dal plugin `git-commit-id`
  (`app.commit=@git.commit.id.abbrev@`), mostrato in footer come `v0.2.0 · <hash>`.
  Risolve il problema "non conosco il commit prima di committare": il valore è
  il commit da cui è stato fatto il build (l'ultimo noto). Se `.git` manca o il
  token non si risolve, `AppVersionService.getCommit()` ritorna vuoto (guardia).
- Log versioni:
  - 0.2.0 — shared list UX, primanota raggruppata, fix menu contabilita',
    autocomplete storico, bilancio a sezioni, migration tracker+viewer.
  - 0.3.0 — magazzino / distinta base / produzione standard,
    consultazione (+ sessione 1: articoli, ordini, DDT, fatture+proforma,
    cruscotto ristampa documenti).
  - 0.3.1 — cesello dettaglio righe documento: righe espandibili
    + esplosione commenti dal CLOB ORD_NOTE (per ora sulle fatture, poi
    ordini/DDT/proforma).
  - 0.4.0 — acquisti (ordini/carichi da fornitore) +
    anagrafiche agenti/banche/capi area, consultazione.
  - 0.5.0 — analisi DEEP parametri aziendali (catalogo
    MENU_AZI000→U_AZI_*) + viewer read-only /parametri-aziendali.
  - 0.5.1 — fix bilancio (toggle C/F, TAB SP/CE,
    quadratura, allineamento totali) + fix bug /documenti (ORDER BY duplicato).
  - 0.6.0 — analisi DEEP bilancio CEE (BILNEW/U_INT_TT/
    U_COR_TT + pseudocodice ceecont per Opus) + viewer struttura e
    mappatura con pannello anomalie.
  - 0.7.0 — bilancio raggruppato per mastri/gruppi
    (Bilancio di verifica, Prog. Dare/Avere/Saldo); quadratura stabile col toggle C/F.
  - 0.7.1 — bilancio: banner quadratura esteso + sub-tab
    Attività/Passività e Costi/Ricavi.
  - 0.8.0 — bilancio: switch vista mastri/gruppi ↔ sezioni
    contrapposte (Attività|Passività, Costi|Ricavi).
  - 0.8.1 — menu Bilancio CEE attivato (voce sintetica);
    sintesi bilancio: Differenza in testa + riquadro quadratura prominente.
  - 0.9.0 — bilancio: export Excel + ritocchi sintesi
    (Differenza in testa, riquadro quadratura, bordi card).
  - **0.9.1 (corrente)** — overlay "Caricamento in corso" globale;
    bordi più marcati sulle card di sintesi SP/CE.

## 4c. Migration tracker (.scx logic ↔ web)

`src/main/resources/migration/scx_migration_tracker.csv` relaziona la logica dei
form VFP (`.scx`: metodi, bottoni, eventi, proprietà) con la controparte web, o
il motivo di non-migrazione (set standard di `reason_code`). Sta nel CLASSPATH
perché il viewer web lo legge a runtime (`/utilita/migrazione`, voce menu
Strumenti). Schema + legenda in `src/main/resources/migration/README.md` — nomi
colonna stabili. Ogni sessione appende i membri `.scx` analizzati/migrati (righe
aggiornate, non cancellate).

Parsing `.scx` (VFP form = DBF + `.sct` FPT memo): ogni record è un oggetto del
form; il codice sta nel memo `METHODS`, la config in `PROPERTIES` (RecordSource /
ControlSource / Caption → tabelle, campi, label). Descrittori di campo da offset
32 del DBF (32 byte ciascuno); i campi memo puntano a blocchi nel `.sct` (block
size ai byte 6-7). I form contengono MOLTA logica nei bottoni/validazioni
(es. `menu_cli000_2016_04_04.scx` → 677 oggetti, 311 con codice): non trascurarli.

## 5. Domain knowledge (hard-won, do not re-derive)

### Fiscal year & society
- `PUB_ANNO` (VFP global) ≙ `FiscalContext.fiscalYear`. Fiscal years live in
  **PARA** with code `ANN` + **society code (2 ch)** + **year (4 ch)**, e.g.
  `ANN012026`. Parse = last 4 chars.
- `PUB_CODSOC` is effectively always `'01'` — meaningless for multi-tenancy
  (that's the tenant DB) but REQUIRED in every legacy filter
  (`CON_SOC='01'`, …). Default in `FiscalContext.societyCode`.

### Piano dei conti (CONTI) — positional hierarchy
- The hierarchy is **positional on fixed-length zero-padded codes**, NOT via
  `CON_CODPADRE` (empty in real data). Segment lengths come from
  **`U_AZI_AN.AZI_MASTRO` / `AZI_SOTTOG`** (per society; VFP defaults 3/5;
  APPLILIB sets `PUB_MASTRO = AZI_MASTRO + 1` as cut position).
- Classification: non-zero prefix ≤ mastroLen → mastro; ≤ sottogLen →
  sottogruppo; else conto. Parent = fixed-length prefix cut, zero-padded.
- Implemented in `AccountTreeService` (cascade: positional → `CON_CODPADRE` →
  dot-pattern → zero-strip; diagnostics logged). Payload via
  `GET /conti/tree-data` (flat `[id,code,descr,type,posbil,level,parentIdx]`),
  rendered client-side by the reusable **`static/js/aq-tree.js`** component
  (lazy DOM, live filter, expand-to-level, `#c=CODE` deep links). Reuse this
  pattern for any future hierarchy (cost centers, BOMs).

### Bilancio classification (CON_POSBIL) — sezioni contrapposte
- `CONTI.CON_POSBIL`: **`'P'` = Stato Patrimoniale**, **`'E'` = Conto Economico**
  (mapped as `Account.balancePosition`). This is the SP/CE split.
- Within a section the side is the **sign of the account balance** (saldo =
  totDare − totAvere), exactly as the legacy report `fanni210.prg`:
  - Patrimoniale + saldo Dare (≥0) → **Attività**; saldo Avere (<0) → **Passività**
  - Economico + saldo Dare (≥0) → **Costi**; saldo Avere (<0) → **Ricavi**
- `/contabilita/bilancio` renders a prospetto a sezioni contrapposte
  (Attività|Passività, Costi|Ricavi) with a synthesis header (totals per side +
  Risultato d'esercizio = Ricavi − Costi). `?cf=false` hides customer/supplier
  accounts (CON_TIPOCO C/F). Accounts without CON_POSBIL are surfaced in a
  "da classificare" panel, not hidden.
- Account autocomplete for the storico picker: `GET /conti/autocomplete?q=`
  (JSON `[{code,description}]`, searches code OR description) + a vanilla-JS
  combobox in `storico.html`.

### Warehouse (U_MAG_MO / U_MAG_GG / u_vva_ch)
- `MOV_SEGNO` (`'+'/'-'`) carries the sign; stock at a date = signed sum of
  movements. `U_MAG_GG` holds only CURRENT stock and can have **multiple rows
  per article** (pre-aggregate before joining).
- `MOV_DTDOCU`/`MOV_DTREGI` are **varchar `yyyy/MM/dd`**, with some dirty
  rows. Rules: bind date params as strings in the same format (lexicographic
  compare is correct + SARGable); never `TRY_CONVERT` in production queries
  (data cleanup is the customer's job); the date-column selector is
  substituted in the SQL text from enum `DateBase{DOCU,REGI}` — **never a
  runtime `CASE`** (type-coercion trap).
- Currency: `MOV_PREACQ` is in `MOV_VALUTA`; rate = as-of join on `u_vva_ch`
  (`OUTER APPLY TOP 1 … WHERE VVA_DATA <= mov_date ORDER BY VVA_DATA DESC`).
  Conversion applies to ALL currencies (EUR has rate 1). Always multiply.
- FIFO valuation dashboard: `/magazzino/valorizzazione` (+ `/dati`, `/strati`,
  `/prezzi`, `/articoli`, `/export`). Metrics in
  `WarehouseValuationService` (KPI, ABC/Pareto, anomaly panel, price
  volatility + linear-regression 6-month projection — ALWAYS presented to
  users as an indication, not a forecast).

### Misc legacy conventions
- Legacy char columns are space-padded → `safeTrim` everywhere.
- Legacy login: operator codes like `SER`, password Caesar +3
  (`LegacyPasswordVerifier`).
- H2 system DB runs in MSSQL mode: use `SELECT TOP 1`, not `LIMIT`.

## 6. Slices implemented so far

| Slice | URLs | Notes |
|---|---|---|
| Login + tenant switch | `/login` | legacy `res_oper` + web credentials |
| Dashboard + sidebar menu | `/` | menu from `tbl_menu`, fiscal-year badge |
| Customers (registry, near-complete) | `/clienti…` | 19 tabs mirroring `MENU_CLI000`, **18 active + 1 placeholder** (Distinte RID: CLI_*RID columns absent from U_CLI_AN). 163 entity fields, whitelist save. Reusable `fragments/form-shell.html`, `FormTab`, `BreadcrumbService` |
| Parameters (PARA) | `/parametri…` | 340 categories catalog, generic CRUD |
| Fiscal year context | `/select-year` | PUB_ANNO analog, silent auto-set |
| Chart of accounts | `/conti`, `/conti/tree`, detail/edit | positional tree, aq-tree.js |
| Suppliers (registry) | `/fornitori…` | Replica of VFP MENU_FOR000 on U_FOR_AN; 7 active tabs, 44 verified fields; reuses the customer form-shell framework |
| Contabilità (read-only) | `/contabilita…` | Primanota (**grouped by registration**: tipo operazione + involved-accounts badges + clickable metrics), storico (mastrino), bilancio, partitari cli/for. Read-only su MOV_CONT / PART_CLI / PART_FOR / PARA / CONTI. Data-entry primanota = slice futura |
| Warehouse valuation | `/magazzino/valorizzazione` | FIFO + FX + Pareto + Excel |
| Custom reports | `/custom-reports…` | self-service SQL reports (foundation for "Project-JDBCapy") |
| Articles (read-only) | `/articoli` | Consultation of U_ART_PR (VFP MENU_ART000): shared list pattern + 4-card detail. ~30 of 254 columns mapped, no edit path (maintenance stays on VFP). ART_COORD1..4 in the form are memvars — real column is ART_COORD |
| Customer orders (read-only) | `/ordini` | U_ORD_TT + U_ORD_DD (VFP MENU_ORD000), scoped to FiscalContext year. TT↔DD join: TAGGANCIO=DAGGANCIO hook (fallback ORS_ triple key). Row value = ORD_PRZNET × ORD_QTAORD (APPLILIB) |
| DDT (read-only) | `/ddt` | U_BOL_TT + U_BOL_DD (VFP menu_BOL000), year-scoped. GOTCHA: BOL columns reuse the ORD_ prefix (ORD_NUMDDT = DDT number; ORD_NUMORD = linked order). Join TAGGANCIO=DAGGANCIO |
| Invoices + proforma (read-only) | `/fatture`, `/proforma` | U_FAT_TT/DD + U_FAP_TT/DD (VFP MENU_FAT000 / menu_FAP000), year-scoped. GOTCHA: ORD_NUMORD/ORD_DATORD on these tables are the invoice's OWN number/date; U_FAP_* = fatture PROFORMA (not acquisto). FE flags (ORD_TRASME/ORD_IDSDI) read-only |
| Ristampa documenti (read-only) | `/documenti` | Unified dashboard over ALL document archives (VFP MENU_RISTAMPA_DOC) + traceability ordine↔DDT↔fattura. DocumentArchiveDao: table names from DocumentType enum only (controlled identifiers, cf. WarehouseValuationDao.DateBase). GOTCHA: on every document table ORD_NUMORD/ORD_DATORD are the doc's OWN number/date; on DDT rows the linked customer order is the ORC pair (ORS_NUMORC/ORS_DATORC) |
| Magazzino movimenti+giacenze (read-only) | `/magazzino/movimenti`, `/magazzino/giacenze` | U_MAG_MO / U_MAG_GG. Causale movimento = MOV_TOP via PARA 'TOP'+codice. Giacenze SEMPRE pre-aggregate (SUM GROUP BY MAG_ANAART+MAG_CODMAG, StockBalanceDao nativo: count di gruppi con HAVING richiede derived table, non JPQL). Anomalie esposte: Negativa, Zero con storico. Menu giacenze via FUNCTION_TO_URL (=determina_form_giacenze()) |
| Distinta base (read-only) | `/distinte` | U_DIS_TT/DD. DIT_GRUPPO = codice ARTICOLO padre (join U_ART_PR.ART_CODPRI). DIS_ESPLOD='X' = sotto-distinta (navigazione un livello alla volta via /distinte/articolo/{code}). Esplosione multi-livello e costing DEFERRED |
| Produzione STANDARD (read-only) | `/produzione` | PRODUZIONE (albero IDNODE/PARENT): radici = PARENT='' AND TIPO='STD' (filtro legacy VERIFICATO). PROD_ORDINI/PROD_LEGAMI/PROD_AVANZA linkate per IDPRG (nessuna colonna soc/anno). GRUPRD via PARA 'PRD'+codice. Solo standard per mandato |
| Ordini a fornitore (read-only) | `/ordini-fornitore` | U_ORF_TT/DD, speculare a /ordini. Controparte in ORD_CODCLI/ORD_RAGSOC (riuso legacy su documenti acquisto). MENU_ORF000 gestisce anche le PROPOSTE via flag di lancio senza colonna discriminante → lista intero archivio (NEEDS_DOMAIN) |
| Carichi da fornitore (read-only) | `/ddt-fornitore` | U_BFO_TT/DD (NON U_BOF_*: quello e' il flusso bollette fiscali verso clienti — BOFCONSE apre U_CLI_AN). ORD_TIPO=9 = reso da cliente (badge). Righe: ordine fornitore via coppia ORC, fattura fornitore via MOV_NUMFAT/DATFAT |
| Agenti / Banche / Capi area (read-only) | `/agenti`, `/banche`, `/capi-area` | U_AGE_AN, U_BAN_AN, U_CAR_AN. GOTCHA: U_CAR_AN = "agenti per capo area" (provvigioni), non vettori (vettori = PARA VET in /parametri/VET); condizioni pagamento = PARA CPA in /parametri/CPA (U_PAG_AN e' un archivio morto: incassi su ordine, solo replica+pulizia) |
| Parametri aziendali (analisi+viewer, read-only) | `/parametri-aziendali` | Catalogo DEEP di MENU_AZI000 (841 parametri) in resources/parametri/ + viewer con popover scopo/funzionamento/evidenze. ARCHITETTURA CHIAVE: APPLILIB.AQUADOCU carica ogni AZI_X in PUB_X allo startup (712 mappature) — gli usi reali dei parametri sono sulle PUB_*. Valori via SELECT TOP 1 * metadata-driven (colonne assenti → n.d.). Scrittura parametri = Opus |
| Bilancio CEE — struttura (read-only) | `/contabilita/bilancio-cee-struttura` | BILNEW+U_INT_TT+U_COR_TT. GOTCHA: nessun albero in BILNEW (ordine=BIL_CODRIG, TIPO_DATO I/V/T, gerarchia totali in U_COR_TT come edge list con segno); U_INT_TT ha doppia destinazione dare/avere (INT_CODRIA usata se saldo negativo, es. banche in passivo); VAL(codrig)>=21600 = conto economico (ABS). Il CALCOLO (ceecont) e' di Opus: pseudocodice in resources/cee/README.md |

## 7. Security notes (PUBLIC repository!)

- **No credentials/hosts in tracked files.** Enforced via env placeholders +
  gitignored local override. Re-check every patch.
- `LegacyPasswordVerifier` necessarily documents that legacy VFP passwords
  are a Caesar +3 shift. **This is a disclosure**: anyone with DB access can
  decode operator passwords. Flagged to the owner; mitigations: keep the repo
  private, or accept the risk knowing DB access is already the crown jewels.
  Web-side logins additionally use bcrypt credentials in the system DB.
- Never commit `data/` (H2 files contain credential hashes) or `logs/`.

## 8. Known Thymeleaf/Spring pitfalls (cost real debugging time)

1. `<style>` blocks in a child template's `<head>` are **discarded** by the
   layout decorator → global CSS lives in `layout.html`.
2. A fragment parameter named `title` clashes with the `~{::title}` selector.
3. **Recursive fragments with parameters are forbidden** (they explode with
   null params even when defined in a separate file; `th:if` guards are not a
   primary fix). For trees/recursion: JSON endpoint + client-side component
   (the aq-tree pattern).
4. Tenant-side `@Transactional` must specify
   `transactionManager = "tenantTransactionManager"`.
5. Wide legacy entities: `@DynamicUpdate` + explicit editable-field copy.
6. **Pageable `Sort` on aggregate/GROUP BY queries is unsafe.** Hibernate 5
   (Boot 2.7) appends `ORDER BY <property>` referring to the entity, which
   breaks on SQL Server when the property is aggregated (e.g. `MAX(date)`) or
   not in the `GROUP BY`. → Use `ListParams.toPageableNoSort()` and put an
   explicit `ORDER BY` inside the query, driven by `:orderCol`/`:asc` params
   with `CASE` expressions (see `MovContabileRepository.searchRegistrationHeads`).
   Keep all `CASE` branches the same column type (here all varchar).
7. **Paginated JPQL with `GROUP BY` needs an explicit `countQuery`** — the
   auto-generated count wraps the grouped select and counts rows-per-group,
   not groups. Provide `countQuery = "SELECT COUNT(DISTINCT key) ..."`.
8. To let Pageable `Sort` work on a normal (non-grouped) query, the JPQL must
   **not** carry a fixed `ORDER BY` — it silently overrides the Sort. Removed
   the fixed `ORDER BY` from `Customer/Supplier/Partita*.search`.

## Shared list UX (ALL search/list screens)

Sorting, pagination and page-size are centralized so every list behaves the
same and no controller re-implements them:
- **`com.aquarius.web.ListParams`** — validates `page`/`size`/`sort`/`dir`.
  `PAGE_SIZE_OPTIONS = {20,50,100,200}`, `DEFAULT_SIZE = 20`. `sort` is checked
  against a per-list **whitelist** (guards against arbitrary property injection).
  `of(...)`, `toPageable()`, `toPageableNoSort()`, `isAsc()`, getters.
- **`fragments/list-tools.html`** — three reusable fragments:
  `sortableHeader(baseUrl,field,label,q,size,sort,dir,align)` (clickable header
  with asc/desc toggle + caret), `pager(baseUrl,page,q,size,sort,dir,pageObj)`
  (first/prev/info/next/last + rows-per-page `<select>`, `totalElements`), and
  `itDate(value)` (formats `yyyy/MM/dd` or `yyyy-MM-dd` → `dd/MM/yyyy`, robust
  to short/blank values).
- **Controllers** accept `q,page,size,sort,dir`; build a `ListParams` with the
  whitelist + default sort/dir; add to the model: `pageObj`,`size`,`sort`,`dir`
  (plus the domain list). Search forms carry `size/sort/dir` as hidden inputs.
- **Dates** are rendered `dd/MM/yyyy` via `itDate` everywhere (legacy stores
  them as `yyyy/MM/dd` strings; still filtered/ordered as strings).
- Applied to: primanota, partitari (cli/for), clienti, fornitori. New lists
  MUST reuse this (see Fable 5 handoff).

### Primanota is grouped by registration
The primanota list shows **one row per registration** (not per movement).
`searchRegistrationHeads` paginates the distinct `registrationNo` (GROUP BY),
then `findRowsForRegistrations` loads that page's rows, aggregated by
`RegistrazioneRow.fromMovements` into: tipo operazione, importo (sum Dare), and
the list of involved accounts (badges, trimmed codes → `CONTI` descriptions).

**Tipo operazione description comes from `PARA`, not `TAB_TOPCONT`.** The legacy
resolves it as `PARA.DESCRI WHERE CODICE = 'TOP' + ALLTRIM(MOV_TOP)` (see
contabilelib: `select mov_cont.*, para_top.descri ... JOIN para_top`). The web
uses `ParameterRepository.findByPrefix("TOP")` and looks up with the `"TOP"`
prefix prepended to the trimmed `MOV_TOP` code.

**Customers/suppliers are identified by account type, not by MOV_CCLI/MOV_CFOR**
(which are almost always empty). An account is a customer when
`CONTI.CON_TIPOCO = 'C'` and a supplier when `= 'F'`. The "TOP 5 clienti/
fornitori" metric joins `MovContabile`↔`Account` on the account code (SQL Server
ignores trailing spaces in varchar `=`, so legacy padding is a non-issue) and
filters `accountType IN ('C','F')`.

The header shows clickable grouping **metrics**: by period (month), TOP 5
customer/supplier accounts, TOP 5 operation types — each links back into a
filtered view. Metric amounts are pre-formatted in the controller (`formatIt`,
`Locale.ITALY`) — never instantiate `BigDecimal` in the template.

## 9. Roadmap (next slices, in rough priority order)

1. **Suppliers** (`menu_for000`) — twin of customers, reuse the form-shell
   framework as-is.
2. **Prima nota** (`menu_prima_nota.scx`) — the accounting god-form: header +
   current row + rows grid + IVA section + deadlines; needs PDC picker
   (reuse `/conti/tree-data`), TOP/CPA/IVA parameter lookups.
3. Registrazioni contabili list (`menu_registrazioni_contabili.scx`).
4. Authorization flags per operator (`_ANACLI_VIS/INS/MOD/ANN` pattern from
   `UTENTI` column).
5. PARA-driven dropdown lookups inside forms.
6. Situazioni contabili (mastrino/partitari/scadenziari — report-style).

## 10. Working agreement

Chat sessions deliver **git patches, not archives** — one zip per change with
`<date>-<slug>.patch` + loose `COMMIT_MSG.txt`. Every change also adds
`.claude/YYYY-MM-DD-<slug>.md` and updates this file. Full rules:
**`.claude/README.md`**. Repo language: **English** (chat can be Italian; UI
strings are Italian business language — that is product copy, not repo prose).
