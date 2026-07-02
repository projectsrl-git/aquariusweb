# Strategia dati 1.3 — Plug & play sul DB legacy, evoluzione progressiva

> Documento di riferimento per la scelta architetturale cardine di Aquarius web.
> Tutte le decisioni di mapping entità, schema, e migrazioni discendono da qui.

---

## TL;DR

**Aquarius web vive in parallelo al VFP, sullo stesso DB legacy, senza toccarne lo schema.**

- ✅ Le tabelle Aquarius esistenti (res_oper, U_CLI_AN, ...) sono **letture-orientate**: mappate in entity JPA SOLO sulle colonne che servono, mai modificate da Hibernate, scritte solo dove la web-app ne ha esplicito ownership funzionale.
- ✅ Le funzioni web-only (BCrypt, audit log, preferenze UI) vanno in **tabelle nuove** con prefisso `aq_web_`, di proprietà esclusiva della web-app.
- ❌ MAI un `ALTER TABLE` su una tabella legacy.
- ❌ MAI `ddl-auto=update` o `create` in produzione (e nemmeno in dev contro DB reali): `validate` è l'unica modalità accettata.

---

## Le tre opzioni considerate e perché 1.3

| | 1.1 — Mappare 1:1 il DB attuale | 1.2 — Re-ingegnerizzare il modello dati | **1.3 — Ibrido: prima 1.1, poi evolvi** |
|---|---|---|---|
| **Idea** | Web app è un nuovo client per il DB legacy. Stesse regole, stessi vincoli. | Disegnare un modello nuovo (3NF pulito, naming moderno) e la web app gira su quello. | Partire come 1.1 (plug&play), affiancare un modello nuovo dopo, sostituire le CRUD una alla volta. |
| **Time-to-value** | Veloce: zero migrazione dati. | Lentissimo: prima bisogna progettare lo schema, poi migrare 50k+ record per tabella. | Veloce nel primo step, poi linearmente progressivo. |
| **Rischio cutover** | Basso: si può girare in parallelo con VFP per mesi. | Altissimo: o tutto funziona, o si rollback al VFP. | Basso → basso → basso, mai un big bang. |
| **Debito tecnico nel codice** | Alto: nomi colonne legacy (`CLI_RAGSOC`, `RES_SOSPESO`) finiscono nei DTO API e nei template HTML. | Basso da subito. | Alto inizialmente, ripagato gradualmente man mano che si migra. |
| **Operatività in transizione** | Web e VFP scrivono sulle stesse tabelle → vincoli concorrenza, locking. | VFP spento al cutover. | VFP funziona come prima, web app aggiunge funzioni senza toglierne. |
| **Opzionalità futura** | Bassa: cambiare modello dopo costa quanto rifarlo. | Bloccata sulla scelta del nuovo modello, comunque sia. | **Massima**: tieni l'opzione aperta, scegli il modello quando avrai capito di più del dominio reale. |

**Scelta: 1.3.** Le ragioni che pesano di più:
1. Time-to-value: in 2-3 mesi puoi avere già il login + 1-2 form di business utilizzabili in produzione.
2. Rischio: non c'è mai un momento "ora dobbiamo spegnere il VFP". L'utente sceglie cosa fare nel vecchio e cosa nel nuovo.
3. Apprendimento del dominio: l'analisi del repo ha mostrato 759 tabelle e 1.529 voci di menu. Pretendere oggi di sapere come ri-modellare il dominio "bene" è ingegneria fantasy. Costruisci, osserva l'uso, modella dopo.

---

## Le regole operative che derivano da 1.3

### Regola 1 — Nessuna modifica DDL alle tabelle legacy

L'entity JPA `OperatorUser` mappa `res_oper`. Le sue colonne sono un **sottoinsieme** di quelle vere della tabella. Hibernate con `ddl-auto=validate` accetta che la tabella abbia più colonne di quelle mappate dall'entity, ma non il contrario. Bene: lasciamo a Hibernate il ruolo di verifica, nient'altro.

Concretamente:
- Mai aggiungere a `OperatorUser` un campo annotato `@Column(name="NUOVA_COLONNA")` se quella colonna non esiste già in `res_oper`.
- Per ogni informazione web-only di cui hai bisogno, aggiungi una **nuova tabella** `aq_web_qualcosa` con FK logica (non fisica) verso la chiave legacy.

### Regola 2 — Annotare le entity legacy come "read-mostly"

Sull'entity legacy, marcare i campi con `insertable=false, updatable=false` se sono di sola lettura (la web app non li deve scrivere). Esempio in `OperatorUser`:

```java
@Column(name = "PASSWORD", length = 20, insertable = false, updatable = false)
private String legacyPassword;

@Column(name = "RES_SOSPESO", insertable = false, updatable = false)
private Boolean suspended;
```

Questo previene errori sciocchi: anche se uno sviluppatore distratto scrivesse `user.setSuspended(true); repository.save(user)`, Hibernate ignorerebbe il campo perché non-updatable. Doppia sicurezza.

In casi specifici la web app può essere autorizzata a scrivere su una tabella legacy (es. il prossimo step: voler creare un nuovo cliente da web → INSERT in `U_CLI_AN`). In quel caso si scelgono ESPLICITAMENTE le colonne scrivibili togliendo `insertable=false` solo da quelle, e si testa che il VFP continui a vedere correttamente il record (campi obbligatori non mappati → valori di default OK?).

### Regola 3 — Prefisso `aq_web_` per tutto ciò che la web app aggiunge

Tutte le tabelle nuove sul tenant DB hanno prefisso `aq_web_`. Esempi attuali e previsti:

- `aq_web_user_credentials` — BCrypt hash, last_login, must_reset (esiste già nella slice 1)
- `aq_web_audit_log` — eventi di sicurezza/accesso (slice futura)
- `aq_web_user_preferences` — preferenze UI per operatore (es. tema chiaro/scuro)
- `aq_web_menu` — eventuale copia editabile del menu legacy (slice 2, se serve)

Il prefisso fa due cose: (1) elimina ambiguità su "chi possiede questa tabella" (2) rende banale uno script di disinstallazione (`DROP TABLE` su tutte le `aq_web_*` e tornano i conti del legacy).

### Regola 4 — Solo schemi additivi e idempotenti

Lo script di creazione tabelle nuove (vedi `WebSchemaInitializer`) usa sempre `IF NOT EXISTS`. Riavviare l'app due volte non fa danni. Aggiornare la web app a una versione nuova → lo script si arricchisce, ma le tabelle esistenti non vengono toccate.

Per modifiche di colonne esistenti su tabelle `aq_web_*` (es. allungare un varchar), usare migrazioni esplicite numerate (futuro: Flyway/Liquibase). MAI lasciare a `ddl-auto=update` la responsabilità.

### Regola 5 — Read path legacy / write path coordinato

Operativamente, le scritture concorrenti tra VFP e web app sulla stessa riga sono il pericolo più serio (lost updates). Per ogni tabella legacy che la web app vuole iniziare a scrivere:

1. Censire chi scrive: VFP (sì, quale form/prg?), trigger SQL Server, job batch
2. Decidere chi è "owner" di quella scrittura nella fase X
3. Loggare le scritture web (`aq_web_audit_log`) così se c'è un problema si capisce chi è stato

In pratica: si parte SEMPRE con la web app in sola lettura su una tabella legacy, e SOLO quando la funzione corrispondente del VFP viene effettivamente disattivata (cioè: gli operatori NON la usano più), la web app prende il write ownership. Questo richiede una checklist per ogni "switchover funzionale", non un big bang.

---

## Cosa cambia rispetto alle versioni precedenti del progetto

Le versioni 0.1.0 dello scaffold avevano:
- 3 colonne nuove (`PASSWORD_HASH`, `MIGRATED_TO_BCRYPT`, `LAST_LOGIN_AT`) aggiunte alla tabella legacy `res_oper`
- `ddl-auto=update` per farle aggiungere automaticamente

Entrambe sono state RIMOSSE in attuazione di 1.3:
- Le 3 colonne sono migrate a una tabella separata `aq_web_user_credentials`
- `ddl-auto` è ora `validate`
- Lo schema delle tabelle nuove è creato da uno script idempotente eseguito da `WebSchemaInitializer` al primo avvio

---

## Fase 2 (futura, non ancora pianificata): il modello dati nuovo affianco

Quando una buona parte del legacy sarà coperta dalla web app, e capiremo il dominio meglio, potremo:

1. Disegnare il **modello dati canonico** (chiama-lo `aq_v2_*` o `core_*`, scegliamo prefisso) — magari con un'analisi di dominio guidata, magari distillando lo schema legacy in qualcosa di più asciutto.
2. Costruire la **sincronizzazione bidirezionale legacy ↔ canonico** (CDC, trigger, o aggiornamento applicativo).
3. Spostare una CRUD per volta dal modello legacy a quello nuovo, in stile **strangler fig**: la stessa pagina web cambia repository ma non interfaccia, e l'utente non se ne accorge.
4. Dopo che TUTTE le CRUD sono migrate al modello nuovo, e tutti gli script batch / report / integrazioni con altri sistemi sono stati aggiornati, si può spegnere il modello legacy.

È un percorso lungo. Per ora basta sapere che è **possibile** senza buttare via niente del lavoro fatto nella fase plug&play.

---

## Riferimenti incrociati

- **`OperatorUser.java`** — esempio canonico di entity legacy mappata read-mostly
- **`WebUserCredentials.java`** — esempio canonico di tabella web-only
- **`WebSchemaInitializer.java`** — meccanismo di creazione idempotente delle tabelle nuove
- **`docs/MIGRATIONS_SCHEMA.sql`** — versione "manuale" dello script di creazione
- **`docs/ARCHITECTURE.md`** — visione d'insieme dell'architettura tecnologica
