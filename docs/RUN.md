# Come eseguire AquariusWeb

> Documento di riferimento per build, esecuzione, configurazione runtime.

---

## 1. Build dell'artefatto

Prerequisiti: **Java 17** e **Maven 3.6+** installati.

```bash
cd AquariusWeb/
mvn clean package
```

Output: `target/AquariusWeb.jar` (~60 MB inclusi di tutte le dipendenze Spring/Hibernate/jTDS).

Per saltare i test (build più veloce):

```bash
mvn clean package -DskipTests
```

---

## 2. Esecuzione

### Modalità "classica" (jar standalone)

```bash
java -Xmx512m -jar target/AquariusWeb.jar
```

> Nota: il `-Xmx256m` che hai chiesto **non è abbastanza** per Spring Boot 2.7 con Hibernate + 2 EntityManagerFactory + connection pool. Servono almeno 384 MB, raccomando **512 MB**. Sotto, l'app potrebbe partire ma andare in OutOfMemory al primo accesso a un tenant.

L'app starta su `http://localhost:8080`. Console H2 dev: `http://localhost:8080/h2-console`.

Per cambiare la porta:

```bash
java -Xmx512m -jar target/AquariusWeb.jar --server.port=9090
```

### Modalità sviluppo (sorgente, hot-reload Maven)

```bash
mvn spring-boot:run
```

Equivalente, ma ricompila al volo se cambi codice. Buono in dev, da non usare in prod.

### Come servizio Linux (systemd)

Esempio di unit file per produzione (`/etc/systemd/system/aquariusweb.service`):

```ini
[Unit]
Description=AquariusWeb ERP
After=network.target

[Service]
Type=simple
User=aquarius
Group=aquarius
WorkingDirectory=/opt/aquariusweb
ExecStart=/usr/bin/java -Xmx512m -jar /opt/aquariusweb/AquariusWeb.jar
Restart=on-failure
RestartSec=10
EnvironmentFile=/etc/aquariusweb/connection.env

[Install]
WantedBy=multi-user.target
```

Vedi sotto per `connection.env`.

---

## 3. Parametri di connessione al DB — DOVE e COME

Le impostazioni vivono in `src/main/resources/application.properties`, sezione **`aquarius.tenants.*`**. Estratto:

```properties
# Tenant 1 — Impresind
aquarius.tenants.impresind.name=Impresind
aquarius.tenants.impresind.url=${IMPRESIND_DB_URL:jdbc:jtds:sqlserver://192.168.50.35:1433/IMPRESIND_TEST}
aquarius.tenants.impresind.username=${IMPRESIND_DB_USER:sviluppo}
aquarius.tenants.impresind.password=${IMPRESIND_DB_PWD:sviluppo}
aquarius.tenants.impresind.driver-class-name=net.sourceforge.jtds.jdbc.Driver

# Tenant 2 — Tremonti
aquarius.tenants.tremonti.url=${TREMONTI_DB_URL:jdbc:jtds:sqlserver://192.168.50.35:1433/TREMONTI_TEST}
aquarius.tenants.tremonti.username=${TREMONTI_DB_USER:sviluppo}
aquarius.tenants.tremonti.password=${TREMONTI_DB_PWD:sviluppo}
```

La sintassi `${ENV_VAR:valore-di-default}` significa: usa `ENV_VAR` se è definita nell'ambiente, altrimenti usa il default scritto a destra. Da qui le 3 modalità per cambiare le credenziali:

### Modalità A — edit del file (DEV)

Cambia direttamente `application.properties` nel sorgente. Ricompila (`mvn package`) e rilancia. Pro: zero magia. Contro: credenziali finiscono nel jar.

### Modalità B — properties esterno (PROD raccomandata)

Crea `/opt/aquariusweb/application.properties` (FUORI dal jar) con solo le proprietà che vuoi sovrascrivere:

```properties
aquarius.tenants.impresind.url=jdbc:jtds:sqlserver://prod-sql-01:1433/IMPRESIND
aquarius.tenants.impresind.username=aquarius_app
aquarius.tenants.impresind.password=il-vero-password
```

Lancia indicando il file:

```bash
java -Xmx512m -jar AquariusWeb.jar \
     --spring.config.additional-location=file:/opt/aquariusweb/application.properties
```

Spring fa il merge: le proprietà nel file esterno sovrascrivono quelle del jar. Il file esterno **non è nel jar**, può avere permessi 600 (solo lettura per l'utente del servizio).

### Modalità C — variabili d'ambiente (PROD raccomandata se usi systemd o container)

Esporti le ENV_VAR menzionate nelle properties:

```bash
export IMPRESIND_DB_URL='jdbc:jtds:sqlserver://prod-sql-01:1433/IMPRESIND'
export IMPRESIND_DB_USER='aquarius_app'
export IMPRESIND_DB_PWD='il-vero-password'
export TREMONTI_DB_URL='jdbc:jtds:sqlserver://prod-sql-02:1433/TREMONTI'
# ... ecc
java -Xmx512m -jar AquariusWeb.jar
```

Per systemd, raggruppale in `/etc/aquariusweb/connection.env`:

```env
IMPRESIND_DB_URL=jdbc:jtds:sqlserver://prod-sql-01:1433/IMPRESIND
IMPRESIND_DB_USER=aquarius_app
IMPRESIND_DB_PWD=il-vero-password
TREMONTI_DB_URL=...
TREMONTI_DB_USER=...
TREMONTI_DB_PWD=...
```

E il `.service` la include con `EnvironmentFile=/etc/aquariusweb/connection.env` (vedi sopra).

Pro: zero credenziali sul filesystem app, zero nel jar, zero negli ENV visibili a `ps`. Compatibile con secret managers (Vault, AWS Secrets Manager) tramite hook systemd o entrypoint di container.

---

## 4. Cosa succede al primo avvio contro un DB nuovo

Quando AquariusWeb si connette a un tenant per la prima volta, `TenantMigrationsRunner` (CommandLineRunner custom, vedi `config/`) esegue le 3 migrazioni in `db/migration/tenant/`:

   - `V1__init_aq_web_schema.sql` → crea `aq_web_user_credentials`
   - `V2__bootstrap_web_credentials_for_admin.sql` → crea credenziali web per l'operatore `admin` di `res_oper` (se esiste)
   - `V3__custom_reports.sql` → crea `aq_web_custom_reports`

Le tabelle legacy (`res_oper`, `tbl_menu`, `U_CLI_AN`, `MOV_CONT`, ...) restano **intatte**. Il tracking delle migrazioni applicate è in `aq_web_schema_history` (prefisso `aq_web_` come tutte le nostre tabelle).

Al secondo avvio, il migrator vede che le migrazioni sono già applicate e non fa nulla.

Se aggiungiamo nuove tabelle/colonne in futuro (es. quando facciamo Slice 4 contabilità), basterà aggiungere `V4__qualcosa.sql` nei sorgenti; il migrator le applica al primo riavvio.

**Nota**: NON usiamo Flyway. La sua Community Edition (da Flyway 7.0) non supporta SQL Server 2008/2012, che è il DB del cliente. Il nostro `SqlMigrationRunner` custom (~200 LOC, vedi `config/`) fa esattamente lo stesso lavoro: legge gli `V<n>__*.sql` dal classpath, ne traccia l'applicazione in una history table, salta quelli già fatti. Separator dei batch nei file: una riga con solo `GO` (convenzione T-SQL standard).

---

## 5. Verificare che il login funzioni

Dopo `mvn spring-boot:run` o `java -jar`:

1. Apri `http://localhost:8080` — viene rediretto a `/login`
2. Seleziona **Impresind** dal dropdown
3. Inserisci **admin** / **admin** (deve esistere un operatore legacy con CODICE='admin' in `res_oper`)
4. Sei sulla dashboard

Se non hai un operatore `admin` in `res_oper`:
- Soluzione veloce: cambia `aquarius.dev.default-admin-username=admin` in properties per puntare a un altro codice operatore esistente, ricompila, riavvia
- Soluzione pulita: modifica `V2__bootstrap_web_credentials_for_admin.sql` per il tuo codice operatore preferito (es. SERGIO)

---

## 6. Health check / troubleshooting

| Sintomo | Cosa controllare |
|---|---|
| "Login fallito" → credenziali corrette | l'utente esiste in `res_oper.CODICE`? E ha una entry in `aq_web_user_credentials`? |
| "Cannot connect" sul tenant | URL JDBC corretto? Server raggiungibile? `telnet 192.168.50.35 1433`. Firewall? |
| "OutOfMemoryError: Java heap space" | aumenta `-Xmx` a 512m o 768m |
| H2 console vuoto / errore | URL JDBC: `jdbc:h2:file:./data/aquarius_system` (NON `mem:`), user `sa`, password vuota |
| Schema history corrotto | Cancella `aq_web_schema_history` (tenant DB) o `system_schema_history` (system DB) e riavvia: il migrator riapplica tutto. Non farlo se ci sono già dati in produzione: rischi duplicati. |
| "Schema validation failed" all'avvio | `res_oper` ha colonne mappate diverse da quelle in DB — verifica con `INFORMATION_SCHEMA.COLUMNS` |

Per logging più verboso aggiungi:
```bash
java -jar AquariusWeb.jar --logging.level.com.aquarius=TRACE \
                          --logging.level.org.hibernate.SQL=DEBUG
```

---

## 7. Riepilogo: dove sta cosa

| Cosa | File |
|---|---|
| URL/credenziali DB tenant | `src/main/resources/application.properties` § `aquarius.tenants.*` |
| Tenant attivi | stessa sezione `aquarius.tenants.*` (basta aggiungere `aquarius.tenants.<nuovoid>.*`) |
| Tenant di default nella combobox login | `aquarius.default-tenant=impresind` |
| Porta server | `server.port=8080` |
| Posizione H2 system DB | `aquarius.system.datasource.url=jdbc:h2:file:./data/aquarius_system;...` |
| Log dir | `logging.file.name=logs/aquarius.log` |
| Schema migrations | `src/main/resources/db/migration/{system,tenant}/V*.sql` |
| Mappa form-VFP → URL web | `MenuService.FORM_TO_URL` (cresce ad ogni slice) |
