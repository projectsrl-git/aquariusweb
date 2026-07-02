# Porting feature da CReaM

Documento di stato delle 3 feature di CReaM richieste per Aquarius web.

---

## ✅ Custom Reports — Implementato

**Stato**: Slice 1 — completo, sia server che template essenziali (list/form/detail).

**Porting da**:
- `cream/entity/CustomReport.java` → `aquarius/entity/tenant/CustomReport.java`
- `cream/repository/CustomReportRepository.java` → `aquarius/repository/tenant/CustomReportRepository.java`
- `cream/service/CustomReportService.java` → `aquarius/service/CustomReportService.java`
- `cream/controller/CustomReportController.java` → `aquarius/controller/CustomReportController.java`
- `templates/reports/custom/{list,form,result}.html` → `templates/custom-reports/{list,form,detail}.html` (semplificati ma funzionali)

**Differenze chiave rispetto a CReaM**:

| Aspetto | CReaM | Aquarius |
|---|---|---|
| Datasource per la query | unico (l'app è mono-tenant) | tenant routing: la query gira sul DB della società corrente |
| `JdbcTemplate` | bean Spring `@Primary` | costruito esplicitamente sul `@Qualifier("tenantDataSource")` |
| Tabella | `custom_reports` | `aq_web_custom_reports` (prefisso strategia 1.3) |
| Posizionamento | mono-DB | per-tenant (ogni società ha le sue) |
| `created_by` | FK a User entity | varchar(20) = `res_oper.CODICE` (no FK fisica, no entity dependency) |
| Migrazione schema | `ddl-auto=update` | Flyway: `db/migration/tenant/V3__custom_reports.sql` |

**Sicurezza** (invariata rispetto a CReaM): regex pattern che blocca DROP/DELETE/UPDATE/EXEC/sp_/xp_, parser di parametri nominali `:nome` → PreparedStatement.

**Endpoint disponibili**:
- `GET /custom-reports` — lista
- `GET /custom-reports/new` — form creazione
- `GET /custom-reports/{id}` — dettaglio + esecuzione interattiva
- `POST /custom-reports/{id}/execute` — esecuzione AJAX (JSON)
- `POST /custom-reports/validate` — validazione AJAX
- `GET /custom-reports/tables` — lista tabelle del tenant (per autocomplete in form)
- `GET /custom-reports/tables/{name}/columns` — colonne di una tabella

**Template non portati** (da CReaM): `parameters.html` separato (gestito direttamente in `detail.html`), templating "ART" originale. Versione Aquarius più snella: form, list, detail. CSV export incluso.

---

## ✅ AI Help Assistant — Scheletro implementato

**Stato**: Slice 1 — architettura funzionante, contenuto di base. Da espandere.

**Porting da**:
- `cream/controller/HelpController.java` → `aquarius/controller/HelpController.java` (snellito: solo la pagina chat, no manual)
- `cream/controller/HelpApiController.java` → `aquarius/controller/HelpApiController.java` (logica keyword-match identica, contenuto rifatto)
- `templates/help/chat.html` → portato e adattato (markdown light per `**bold**` e `code`)

**Cosa NON è stato portato**:
- `templates/help/manual.html` (596 LOC): manuale HTML statico, CReaM-specific. Per Aquarius andrebbe rifatto da zero perché parla di opportunità, ticket, ecc. che qui non esistono. **Per ora omesso** — l'utente può chiedere all'assistente.
- `manual_content_part1.js` (610 LOC): contenuto del manuale CReaM. Idem.

**Differenze**:
- CReaM aveva ~10 risposte hard-coded su CRM. Aquarius ha ~6 risposte iniziali (login, query, multi-tenancy, parallelo VFP, menu, help). Da espandere man mano.

**Estensione futura — integrazione AI vera**:

Il commento in `HelpApiController.generateResponse()` indica il punto in cui sostituire la logica keyword-match con una chiamata a un LLM esterno. Possibili integrazioni:

- **Anthropic Claude API** — un POST a `https://api.anthropic.com/v1/messages` con il messaggio utente come prompt, eventualmente arricchito con context del menu / schema DB / manuale aziendale come system prompt
- **OpenAI** — equivalente con `/v1/chat/completions`
- **Local LLM** — Ollama / LM Studio per inferenza self-hosted (utile per dati sensibili)

Dipendenza chiave (Anthropic SDK): aggiungere a pom.xml
```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.x.x</version>
</dependency>
```

Configurazione: `application.properties` con `aquarius.ai.api-key=${ANTHROPIC_API_KEY:}` da secret store.

**Decisione architetturale aperta**: se l'AI deve avere accesso ai dati del tenant (es. rispondere "quanti clienti ho in lombardia?" eseguendo SQL), va integrato con tool/function calling. Discussione non triviale → da pianificare in slice dedicata quando ci arriviamo.

---

## ⏳ Customer Map (Google Maps) — Rimandata a Slice 3+

**Stato**: NON implementata in Slice 1. Dipende da entity `Customer` (mappatura di `U_CLI_AN`) che non esiste ancora.

**Porting plan** (per quando arriviamo alla Slice "Clienti"):

1. **Creare entity `Customer`** mappata su `U_CLI_AN` (tenant-scoped, read-mostly come da strategia 1.3)
2. **Creare repository `CustomerRepository`**
3. **Portare `cream/controller/CustomersMapController.java`** (254 LOC) → adattare:
   - rimuovere dipendenze a `TerritoryRepository`, `SalesAgentRepository` (entity CReaM-specific che non porteremo)
   - usare le colonne `U_CLI_AN.CLI_LOCALI` (città) e `CLI_PROVIN` (provincia)
   - mantenere la map statica `CITY_COORDINATES` per geocoding offline (sostituibile con API Google Geocoding se serve copertura più ampia)
4. **Portare i 3 template**:
   - `customers-map-google.html` (versione con Google Maps JS API — quella "molto bella")
   - `customers-map-simple.html` (versione fallback senza Google)
   - `customers-map.html` (entry point)
5. **API key**: richiede Google Maps JavaScript API key in `application.properties`:
   ```properties
   google.maps.api.key=${GOOGLE_MAPS_API_KEY:}
   ```
   In CReaM la chiave è hardcoded — da spostare in secret store/env var per produzione.

**Stima**: 1 giornata di sviluppo una volta che `Customer` entity esiste. Si sbloccherà con Slice 3.

---

## Cronologia decisioni

| Data | Decisione |
|---|---|
| 2026-06-01 | Custom Reports portati identici (server + 3 template essenziali) |
| 2026-06-01 | AI Help portato come scheletro keyword-based, integrazione LLM rimandata |
| 2026-06-01 | Customer Map rimandata perché dipende da Customer entity (Slice 3) |
