# AquariusWeb

Porting Java/Spring Boot del gestionale ERP Aquarius VFP di Project SRL.

Stack: **Spring Boot 2.7.18 · Java 17 · Thymeleaf · Spring Security · JPA · jTDS · Bootstrap 5 · Flyway**

In più rispetto a CReaM: **multi-tenant database-per-tenant** con routing automatico al DB della società scelta al login.

---

## Esecuzione rapida

```bash
mvn clean package           # produce target/AquariusWeb.jar
java -Xmx512m -jar target/AquariusWeb.jar
```

Browser: [http://localhost:8080](http://localhost:8080). Login con società **Impresind**, operatore **admin**, password **admin**.

📖 **Per build, run, configurazione DB, override env var, troubleshooting** → vedi [`docs/RUN.md`](docs/RUN.md)

---

## Strategia dati: 1.3 (plug & play sul DB legacy)

AquariusWeb gira **in parallelo al VFP, sullo stesso database**. Le tabelle legacy (`res_oper`, `U_CLI_AN`, ...) restano com'erano e AquariusWeb le legge senza modificarle. Le funzioni web-only stanno in tabelle nuove con prefisso `aq_web_`. Il VFP continua a funzionare come se la web app non ci fosse.

Documento di riferimento: [`docs/STRATEGIA_DATI.md`](docs/STRATEGIA_DATI.md).

---

## Migrazioni schema — Flyway

Niente più seed via codice Java. Tutto passa da **Flyway** con migrazioni SQL versionate:

```
src/main/resources/db/migration/
├── system/                                  ← SYSTEM DB (H2 in dev)
│   ├── V1__init_system_schema.sql           tabelle tenants + super_admins
│   └── V2__seed_initial_tenants.sql         insert iniziali (Impresind, Tremonti, super-admin)
└── tenant/                                  ← TENANT DBs (uno per società)
    ├── V1__init_aq_web_schema.sql           aq_web_user_credentials
    ├── V2__bootstrap_web_credentials_for_admin.sql   abilitazione operatore 'admin'
    └── V3__custom_reports.sql               aq_web_custom_reports
```

**Naming convention**: `V<numero>__<descrizione>.sql`. Versioni numeriche crescenti, descrizione in snake_case. Per modifiche future basta aggiungere `V4__...sql` ecc.

**Esecuzione**:
- Le migrazioni del SYSTEM DB partono automaticamente al boot (Spring Boot autoconfig Flyway sul `@Primary` datasource).
- Le migrazioni dei TENANT DB sono orchestrate da `TenantFlywayMigrator` (CommandLineRunner @Order(0)) che cicla su ogni tenant configurato e applica le V*.sql in `db/migration/tenant`.
- I tenant DB esistenti partono da `baseline=0` ("Existing Aquarius legacy schema"). Tabella di tracking: `aq_web_flyway_history` (sempre prefisso `aq_web_` per coerenza con strategia 1.3).

---

## Tenant configurati

| Tenant id | Nome visualizzato | Database (test) |
|---|---|---|
| `impresind` | Impresind | `IMPRESIND_TEST` su `192.168.50.35:1433` |
| `tremonti`  | Tremonti  | `TREMONTI_TEST` su `192.168.50.35:1433` (TODO: confermare) |

Modifica `application.properties` (sezione `aquarius.tenants.*`) per riconfigurare. In produzione le credenziali DB vanno in variabili d'ambiente o secret store.

---

## Prima esecuzione

```bash
mvn spring-boot:run
```

Cosa succede al primo avvio:
1. **SYSTEM DB** (H2): Flyway applica V1+V2 → tabelle `tenants`, `super_admins` + dati iniziali.
2. **Per ciascun TENANT DB**: `TenantFlywayMigrator` applica V1+V2+V3 → crea `aq_web_user_credentials`, abilita l'operatore legacy `admin` (se esiste in `res_oper`), crea `aq_web_custom_reports`.

Browser → [http://localhost:8080](http://localhost:8080).

**Credenziali primo login (web)**:

| Campo | Valore |
|---|---|
| Società | Impresind |
| Operatore | `admin` (deve esistere in `res_oper` del tenant) |
| Password | `admin` (must-reset al primo accesso) |

H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console), JDBC URL `jdbc:h2:file:./data/aquarius_system`.

---

## Feature implementate

### Slice 0+1 — Auth
- Login multi-tenant a 3 campi (Società/Operatore/Password)
- Routing DataSource per società
- BCrypt + fallback DECODE() legacy (stub)
- `OperatorUser` allineato allo schema reale (99 colonne, mappate quelle che servono)

### Slice 2 — Menu dinamico ✅
- Lettura `tbl_menu` legacy del tenant (1.527 voci in Impresind)
- Filtraggio per UTENTI (lista codici separati da `.`)
- Costruzione albero ricorsivo seguendo MENU column + pattern `submenu NAME LIST# with 'subkey'`
- Top-level hardcoded (Clienti / Fornitori / … / Contabilità / …) per replicare la menubar VFP
- Endpoint `GET /api/menu/tree` → sidebar JS rendering multi-livello con collapse
- Mappa `formName → URL` in `MenuService.FORM_TO_URL`: cresce a ogni slice di porting

### Slice 3 — Anagrafica clienti (parziale) ✅
- Entity `Customer` su `U_CLI_AN` (legacy, read-mostly, 17 colonne mappate su 80+)
- Lista paginata con ricerca su ragione sociale/codice/P.IVA → `/clienti`
- Pagina dettaglio → `/clienti/{id}`
- Mappa Google (port da CReaM) — placeholder, slice futura

### Porting da CReaM
- ✅ **Query personalizzate** (`/custom-reports`)
- ✅ **Assistente AI** (`/help`)
- ⏳ **Mappa Google clienti** — pagina placeholder, full porting in roadmap

## Prossime slice

- 🔄 **Slice 4 — Contabilità Generale** (Prima nota + Situazioni contabili) — vedi [`docs/ROADMAP_CONTABILITA.md`](docs/ROADMAP_CONTABILITA.md)
- ⏳ Fornitori (`U_FOR_AN`)
- ⏳ Magazzino, Produzione, Statistiche

---

## Struttura

```
src/main/java/com/aquarius/
├── AquariusApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── SystemDataSourceConfig.java          @Primary, H2 system DB
│   ├── TenantDataSourceConfig.java          routing DataSource per tenant
│   └── TenantFlywayMigrator.java            applies db/migration/tenant per ogni tenant
├── controller/
│   ├── LoginController.java
│   ├── DashboardController.java
│   ├── CustomReportController.java
│   ├── HelpController.java
│   └── HelpApiController.java
├── entity/
│   ├── system/        Tenant, SuperAdmin
│   └── tenant/        OperatorUser (= res_oper), WebUserCredentials, CustomReport
├── multitenancy/      TenantContext, TenantRoutingDataSource, TenantsProperties
├── repository/
├── security/          principal, auth provider, tenant filter
└── service/           TenantService, CustomReportService

src/main/resources/
├── application.properties
├── db/migration/system/        V1, V2
├── db/migration/tenant/        V1, V2, V3
├── templates/
│   ├── layout.html
│   ├── login.html
│   ├── dashboard.html
│   ├── custom-reports/  list, form, detail
│   └── help/            chat
└── static/
```

---

## Documentazione

- [`docs/RUN.md`](docs/RUN.md) — **come compilare, eseguire, configurare il DB** (le tre domande tipiche)
- [`docs/STRATEGIA_DATI.md`](docs/STRATEGIA_DATI.md) — la decisione cardine: 1.3 plug & play
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — decisioni architetturali tecnologiche
- [`docs/MULTITENANCY.md`](docs/MULTITENANCY.md) — deep-dive routing DataSource
- [`docs/PORTING_DA_CREAM.md`](docs/PORTING_DA_CREAM.md) — stato porting feature CReaM
- [`docs/ROADMAP_CONTABILITA.md`](docs/ROADMAP_CONTABILITA.md) — piano Slice 4 (Contabilità Generale)
- [`docs/QUERIES_PER_PIANIFICAZIONE.md`](docs/QUERIES_PER_PIANIFICAZIONE.md) — query da lanciare sui tenant
- [`docs/db_schema/aquarius_schema_full.csv`](docs/db_schema/aquarius_schema_full.csv) — schema completo (822 tabelle, 27.767 colonne) per riferimento DAO

---

## Verificato? No, dichiariamolo

Questo scaffold è **scritto ma non compilato** nell'ambiente di generazione (Maven Central non accessibile dalla sandbox). Aspettati 1-2 piccoli aggiustamenti al primo `mvn compile`. Errori tipici: import mancanti, firme leggermente diverse delle dipendenze. Niente di strutturale.
