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
| Warehouse valuation | `/magazzino/valorizzazione` | FIFO + FX + Pareto + Excel |
| Custom reports | `/custom-reports…` | self-service SQL reports (foundation for "Project-JDBCapy") |

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
