# Aquarius — Architecture

Decisioni architetturali e rationale dietro le scelte tecniche di Aquarius web edition.

---

## 1. Overview

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │ HTTP
┌──────▼──────────────────────────────────────────┐
│   Spring Boot Application (Aquarius)            │
│                                                 │
│  ┌────────────────────────────────────────────┐ │
│  │ Controllers (Thymeleaf MVC)                │ │
│  └─────────┬──────────────────────────────────┘ │
│            │                                    │
│  ┌─────────▼──────────────────────────────────┐ │
│  │ Service / Security                         │ │
│  └─────────┬──────────────────────────────────┘ │
│            │                                    │
│  ┌─────────▼──────────────────────────────────┐ │
│  │ Repositories (Spring Data JPA)             │ │
│  │   • system.* → SYSTEM EntityManager        │ │
│  │   • tenant.* → TENANT EntityManager        │ │
│  └─────┬──────────────────────────────┬───────┘ │
│        │                              │         │
└────────┼──────────────────────────────┼─────────┘
         │                              │
         │                              │
┌────────▼──────────┐         ┌─────────▼─────────────────┐
│  SYSTEM DB        │         │  TenantRoutingDataSource  │
│  (H2 / SQL Srv)   │         │  ┌─────────────────────┐  │
│                   │         │  │ TenantContext       │  │
│  tenants          │         │  │ (ThreadLocal)       │  │
│  super_admins     │         │  └─────────┬───────────┘  │
└───────────────────┘         │            ▼              │
                              │   ┌────────┴─────────┐    │
                              │   │ choose by tenant │    │
                              │   └─┬──────────────┬─┘    │
                              └─────┼──────────────┼──────┘
                                    │              │
                          ┌─────────▼──┐    ┌──────▼─────────┐
                          │ Tenant A   │    │ Tenant B       │
                          │ (IMPRESIND │    │ (TREMONTI      │
                          │  SQL Srv)  │    │  SQL Srv)      │
                          │            │    │                │
                          │ res_oper   │    │ res_oper       │
                          │ + business │    │ + business     │
                          └────────────┘    └────────────────┘
```

---

## 2. Decisioni chiave (e perché)

### 2.1 Strategia dati 1.3 — plug & play sul DB legacy

**La decisione cardine** del progetto: la web app gira in parallelo al VFP, sullo STESSO database, **senza modificare lo schema legacy**. Le funzioni web-only stanno in tabelle nuove con prefisso `aq_web_`, di proprietà esclusiva della web-app.

Dettagli, regole operative, e fase futura "modello dati canonico" → [`STRATEGIA_DATI.md`](STRATEGIA_DATI.md).

Conseguenze tecniche per il codice qui:
- `ddl-auto=validate` (mai `update` o `create`): Hibernate verifica che le tabelle/colonne mappate esistano, mai modifica
- entity legacy hanno `@Column(insertable=false, updatable=false)` per default → web-app read-only sui campi legacy
- entity nuove (`aq_web_*`) sono libere: Hibernate può ALTERarle, ma per disciplina si fanno via script SQL versionati
- nessun `DataSeeder` inserisce su tabelle legacy

### 2.2 Server-side rendering con Thymeleaf

Stessa scelta di CReaM. SEO non rilevante in questo caso (è un gestionale interno), ma valgono comunque:
- meno complessità frontend (niente build pipeline, npm, bundler)
- ottimo per CRUD tradizionali (95% di Aquarius è CRUD)
- integrazione nativa Spring + autorizzazioni `sec:authorize`
- riuso fragment fra detail.html, list.html, form.html

Quando aprire un'eccezione e usare React/Vue puntuale:
- workflow con stato complesso (configuratori, wizard multi-step)
- vista touch operatore (replica di `aq_movimaga_scar_touch`)
- dashboard real-time

### 2.3 Multi-tenancy database-per-tenant (NON schema-per-tenant)

Il vecchio Aquarius VFP usa **un database SQL Server per società** (vedi `DBSOCIETA.DBF` del repo legacy). Manteniamo questa scelta:

| Modello | Pro | Contro |
|---|---|---|
| **DB-per-tenant** (scelto) | isolamento totale dei dati, già usato in legacy, backup/restore per cliente facile | overhead di N connection pool, schema migration per N database |
| Schema-per-tenant | un solo DB da gestire | richiede SQL Server che supporti multi-schema bene; riscrittura totale rispetto al legacy |
| Discriminator-column | minimo overhead, multi-tenancy "logico" | rischio di leak (query mal scritte), riscrittura totale, impossibile su DB legacy esistenti |

Vedi [`MULTITENANCY.md`](MULTITENANCY.md) per i dettagli di implementazione.

### 2.4 Due EntityManagerFactory

`SYSTEM DB` (tenants + super-admin) e `TENANT DB` (business + res_oper) hanno cicli di vita diversi: il primo è globale, il secondo cambia per request. Implementarli come **due EntityManagerFactory separate**, ciascuna col proprio TransactionManager e package di entity, è il modo idiomatico Spring per dirlo.

Conseguenza: ogni repository va in `repository.system` o `repository.tenant` (non confondibili). Ogni @Transactional dichiara esplicitamente quale TM usare:

```java
@Transactional(transactionManager = "systemTransactionManager")  // per tenants/super_admins
@Transactional(transactionManager = "tenantTransactionManager")  // per business + res_oper
```

### 2.5 jTDS invece di Microsoft JDBC

Stessa scelta di CReaM. Driver lightweight (800 KB vs 10 MB di mssql-jdbc), stabile, già provato in produzione su CReaM con SQL Server reale di Project SRL.

### 2.6 Password legacy: BCrypt su tabella separata, fallback DECODE() opzionale

`res_oper.PASSWORD` legacy contiene password offuscate da una funzione VFP custom chiamata `DECODE()`. Non è hash crittografico: è una trasformazione reversibile.

Strategia (coerente con 1.3 — non si tocca `res_oper`):

1. Tabella nuova `aq_web_user_credentials` con colonne `operator_code` (= `res_oper.CODICE`), `password_hash` (BCrypt), `must_reset_password`, `reset_token`, ecc.
2. `AquariusAuthenticationProvider` fa **due lookup**:
   - `res_oper` per i controlli legacy (utente esiste, non sospeso)
   - `aq_web_user_credentials` per la verifica BCrypt
3. Se l'utente non ha ancora credenziali web → fallback su `LegacyPasswordVerifier.matches()` (placeholder, finché non recuperiamo l'algoritmo di DECODE). Se passa, l'utente viene forzato a creare le credenziali web al primo accesso.

Pro: la web app non scrive mai su `res_oper`; il VFP continua a usarla immutata. La migrazione password è soft (per ogni utente al primo login), non un big bang.

### 2.7 Lombok dappertutto

`@Data`, `@Builder`, `@RequiredArgsConstructor`. Stessa convenzione di CReaM.

Trappole da ricordare:
- entity con relazioni → escludere collezioni da `@ToString` e `@EqualsAndHashCode` per evitare cicli infiniti
- `@Data` su entity con `id` autogenerato → preferire `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` e annotare l'id con `@EqualsAndHashCode.Include`

### 2.8 No service layer (per ora)

Come CReaM. I controller parlano direttamente con i repository finché la logica è semplice. Si crea un service quando:
- la logica coinvolge più repository in una transazione
- ci sono effetti collaterali (audit, email, file su disco)
- la logica è invocata da più punti (UI, scheduler, REST)

---

## 3. Convenzioni di codice

Identiche a CReaM, con le piccole estensioni dovute al multi-tenancy:

| Aspetto | Convenzione |
|---|---|
| Package base | `com.aquarius` |
| Sotto-package | `config / controller / dto / entity / mapper / repository / security / service` |
| Sotto-sotto-package per entity multi-tenant | `entity.system` vs `entity.tenant` (stesso per repository) |
| Nome controller | `<Resource>Controller`, suffisso REST `RestController` |
| URL REST API | `/api/<resource>` |
| URL pagine UI | `/<resource>`, `/<resource>/{id}`, `/<resource>/new`, `/<resource>/{id}/edit` |
| Template | `templates/<area>/<resource>/<view>.html` (list / detail / form) |
| Naming colonne DB | mantenere maiuscolo legacy per le tabelle esistenti (es. `CLI_RAGSOC`); snake_case per le tabelle nuove (es. `password_hash`) |
| Lingua | italiano nei messaggi UI, inglese nei nomi di classi/metodi |

---

## 4. Sicurezza

| Argomento | Scelta |
|---|---|
| Hash password | BCrypt (`BCryptPasswordEncoder`, default strength) |
| Sessione | HTTP session standard Spring (cookie `JSESSIONID`) |
| CSRF | abilitato (Thymeleaf inserisce automatica il token) |
| Login | form-based via `TenantAwareAuthenticationFilter` (3 campi) |
| Logout | POST `/logout` (default Spring) |
| Autorizzazione | `@PreAuthorize` a livello method, `antMatchers` a livello URL |

Note specifiche multi-tenant:
- l'`Authentication` in `SecurityContextHolder` porta dietro il `tenantId` come parte di `AquariusPrincipal`
- nessuna richiesta autenticata può "saltare" su un altro tenant: il `TenantRequestFilter` setta `TenantContext` solo in base al principal della sessione corrente
- la tabella `super_admins` (SYSTEM DB) è separata da `res_oper` (TENANT DB): un operatore tenant **non può** diventare super-admin

---

## 5. Quando complicare

Aspettare che la complessità sia giustificata. Lista di "non farlo finché non serve":

- ❌ Microservizi — Aquarius è un monolite, e va benissimo
- ❌ Event sourcing / CQRS — nessun requisito di audit estremo per ora
- ❌ Kafka / message broker — nessun workflow asincrono nei form prioritari
- ❌ GraphQL — REST + Thymeleaf bastano
- ❌ Kubernetes — un'istanza Docker su VM gestita basta finché non si scalano i tenant a centinaia

Cose che ci aspettiamo di aggiungere col tempo:
- ✅ Flyway / Liquibase per le migrazioni schema (oggi: `hbm2ddl.auto=update`)
- ✅ Cache distribuita (Caffeine prima, Redis se necessario) per i lookup ad alta frequenza
- ✅ Actuator + Micrometer + Prometheus per metriche per-tenant
- ✅ Service layer dove la logica si complica
