# Multi-tenancy — implementazione dettagliata

Documento di riferimento per capire (e mantenere) il routing multi-tenant di Aquarius.

---

## TL;DR

Una società = un database. La scelta del DB avviene **per ogni richiesta** in base al tenant scelto al login, salvato come parte del principal in `SecurityContextHolder`.

Tre componenti chiave:

1. **`TenantContext`** (ThreadLocal) — porta il tenantId attraverso il thread che gestisce la richiesta
2. **`TenantRoutingDataSource`** (estende `AbstractRoutingDataSource`) — ad ogni `getConnection()` chiede a `TenantContext` chi siamo e ritorna il pool del tenant giusto
3. **`TenantRequestFilter`** + **`TenantAwareAuthenticationFilter`** — popolano `TenantContext` (post-login dal principal; in fase di login dal parametro del form)

---

## Flusso completo di una richiesta

### 1. Primo accesso anonimo a `/login` (GET)

```
Browser → GET /login
  ↓
Spring Security: anonymous, /login è permitAll
  ↓
LoginController.loginPage(model)
  ↓
TenantService.listEnabled()
  ↓  [@Transactional("systemTransactionManager")]
TenantRepository.findByEnabledTrueOrderByDisplayNameAsc()
  ↓
SYSTEM EntityManager → systemDataSource
  ↓
return List<Tenant>
  ↓
login.html: combobox popolata
```

Niente `TenantContext` è coinvolto: `systemDataSource` è `@Primary`, non passa dal routing.

### 2. POST `/login` con (tenant, username, password)

```
Browser → POST /login (form: tenant=impresind, username=admin, password=...)
  ↓
TenantAwareAuthenticationFilter.attemptAuthentication()
  ↓
  TenantContext.set("impresind")       ← qui!
  ↓
  super.attemptAuthentication()
  ↓
  AuthenticationManager.authenticate()
  ↓
  AquariusAuthenticationProvider.authenticate(auth)
  ↓
  TenantContext.get() → "impresind" ✓
  ↓
  TenantRepository.findById("impresind")   [SYSTEM DB, non routed]
  ↓
  OperatorUserRepository.findByCodeIgnoreCase(username)
  ↓
  TENANT EntityManager → tenantDataSource (routing)
  ↓
  TenantRoutingDataSource.determineCurrentLookupKey() → "impresind"
  ↓
  HikariDataSource for "impresind" → SQL Server di IMPRESIND
  ↓
  SELECT * FROM res_oper WHERE CODICE = 'admin'
  ↓
  password check → ok
  ↓
  return Authentication(AquariusPrincipal{tenantId=impresind, username=admin, ...})
  ↓
SuccessHandler → redirect /dashboard
```

### 3. GET `/dashboard` (autenticato)

```
Browser → GET /dashboard
  ↓
Spring Security: SecurityContext contiene Authentication
  ↓
TenantRequestFilter.doFilterInternal()
  ↓
  TenantContext.set(principal.getTenantId())   ← qui!
  ↓
  chain.doFilter()
  ↓
  DashboardController.dashboard(principal, model)
  ↓
  [eventuali query tenant-scoped]
  ↓
  view dashboard.html
  ↓
  [finally] TenantContext.clear()
```

---

## Configurazione

I tenant sono dichiarati in **due posti** sovrapposti:

### A. `application.properties` (bootstrap statico)

```properties
aquarius.tenants.impresind.name=IMPRESIND SAS
aquarius.tenants.impresind.url=jdbc:jtds:sqlserver://...
aquarius.tenants.impresind.username=sviluppo
aquarius.tenants.impresind.password=...
aquarius.tenants.impresind.driver-class-name=net.sourceforge.jtds.jdbc.Driver

aquarius.default-tenant=impresind
```

Questa è la sorgente di verità per le **connection string** (HikariCP pool config). Va in un secret store in produzione.

Letto da `TenantsProperties` (`@ConfigurationProperties("aquarius")`).

### B. `tenants` table del SYSTEM DB (UI super-admin)

```sql
CREATE TABLE tenants (
    tenant_id     VARCHAR(64) PRIMARY KEY,
    display_name  VARCHAR(200) NOT NULL,
    db_type       VARCHAR(40),
    enabled       BIT NOT NULL,
    logo_path     VARCHAR(250),
    created_at    DATETIME
);
```

Solo metadati (no connection string). Serve per:
- popolare la combobox del login (filtrando `enabled = true`)
- abilitare/disabilitare un tenant a runtime da UI super-admin senza redeploy
- avere il display name corretto in header dopo il login

`DataSeeder` allinea automaticamente la tabella alla configurazione properties al primo avvio.

**Perché due posti**: separazione tra dati "operativi" (display, enabled flag, modificabili da UI) e dati "infrastrutturali" (connection string, credenziali DB, modificabili solo da deployment).

---

## Cosa succede se TenantContext non è settato

`TenantRoutingDataSource.determineCurrentLookupKey()` ritorna `null` se nessun tenant è nel contesto. `AbstractRoutingDataSource` userà allora `defaultTargetDataSource` (il primo tenant configurato, vedi `TenantDataSourceConfig`).

Questo è **pericoloso**: significa che una query mal scritta (es. una rotta che dimentica il filter, o un job scheduler senza setup contesto) finirebbe sul tenant di default invece di fallire chiaramente. Lo log è a WARN.

In una versione più "paranoica", si può:
- ritornare un id fittizio `"__no_tenant__"` non registrato → fallirebbe con eccezione
- o lanciare un'`IllegalStateException` esplicita in `determineCurrentLookupKey`

Per ora non lo facciamo perché il `LoginController` (e altre rotte pre-auth) hanno bisogno di accedere al `systemDataSource` senza un tenant settato — e quel datasource è `@Primary`, quindi non è coinvolto nel routing. Tutto OK.

---

## Edge case e cose a cui stare attenti

### 1. Self-invocation e `@Transactional`

Spring intercetta `@Transactional` via proxy AOP. Se un metodo @Transactional ne chiama un altro `@Transactional` **della stessa classe** con `this.metodo()`, il proxy è bypassato e la transazione non viene aperta.

Soluzione adottata in `DataSeeder`: `@Autowired @Lazy DataSeeder self` e poi `self.metodo()`. Brutto ma corretto. Alternativa pulita: separare in più bean (es. un `SystemSeeder` e un `TenantSeeder` distinti).

### 2. Thread pool e leak del ThreadLocal

I thread di Tomcat sono riusati. Se non puliamo `TenantContext` alla fine della richiesta, il prossimo utente che capita su quello stesso thread vede ancora il tenant del precedente. Disastro.

Pulizia garantita dal `finally` in `TenantRequestFilter.doFilterInternal()`. **Mai dimenticarlo.**

### 3. Job asincroni / `@Async` / `@Scheduled`

I thread di un task scheduler **non hanno** il `TenantContext` settato per natura: non c'è una HTTP request a popolarli. Soluzioni:

- per job globali (tipo cleanup giornaliero): scrivere il job in modo che cicli esplicitamente su tutti i tenant attivi, settando il contesto manualmente con try/finally
- per job innescati da utente (es. export pesante): catturare il tenant al momento della submission e passarlo come argomento esplicito al task

Helper futuro: `TenantAwareTaskDecorator` (decora i `Runnable` salvando/ripristinando il `TenantContext`) — da aggiungere se i job asincroni proliferano.

### 4. Dialect Hibernate condiviso fra tenant

Con `AbstractRoutingDataSource` c'è **una sola** `EntityManagerFactory` per tutti i tenant, quindi un solo `hibernate.dialect`. In Aquarius web tutti i tenant sono SQL Server, quindi non è un problema.

Se in futuro servirà supportare tenant su engine diversi (es. un cliente che esce da SQL Server e va su Postgres), si dovrà passare a **Hibernate native multi-tenancy** (`MultiTenantConnectionProvider` + `CurrentTenantIdentifierResolver`), che gestisce nativamente dialect e SQL diversi per tenant. Documentazione: [Hibernate User Guide § 16](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#multitenacy).

### 5. Aggiungere un tenant a runtime

`AbstractRoutingDataSource.setTargetDataSources()` accetta una nuova mappa; chiamando `afterPropertiesSet()` Spring rinfresca i lookup. Per aggiungere un tenant a caldo:

```java
@Service
public class TenantAdminService {

    private final TenantRoutingDataSource routingDs;
    private final Map<Object, Object> targets = new ConcurrentHashMap<>();

    public void registerTenant(String id, DataSource ds) {
        targets.put(id, ds);
        routingDs.setTargetDataSources(new HashMap<>(targets));
        routingDs.afterPropertiesSet();
    }
}
```

Non implementato in slice 1: aggiungere quando servirà l'UI super-admin per creare nuovi tenant.

### 6. Connessione cross-tenant (super-admin)

Il super-admin vede TUTTI i tenant (es. per fare report aggregati). Pattern:
- super-admin si autentica contro `super_admins` del SYSTEM DB (un secondo `AuthenticationProvider` da aggiungere)
- per query cross-tenant, ciclare esplicitamente sui tenant attivi e aggregare i risultati lato applicativo

Non implementato in slice 1.

---

## Glossario

| Termine | Significato |
|---|---|
| Tenant | Una società/azienda servita dall'istanza Aquarius. Ha il proprio DB. |
| System DB | Il database centrale con i metadati: lista tenant + super-admin |
| Tenant DB | Il database di una specifica società: contiene `res_oper` + tutte le tabelle business |
| Tenant context | Il `tenantId` correntemente "attivo" per il thread che sta gestendo la richiesta |
| Routing datasource | Il bean Spring che, ad ogni `getConnection()`, sceglie il pool del tenant attuale |
| Principal | L'oggetto utente nella sessione autenticata, esteso a portare il tenantId |
