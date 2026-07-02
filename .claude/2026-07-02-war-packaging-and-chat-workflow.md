# 2026-07-02 — WAR packaging (Tomcat 9) + chat workflow bootstrap

## Problem / goal
1. The app was packaged as an executable JAR with embedded Tomcat; production
   deployment target is an **external Tomcat 9**. Convert to WAR and remove
   embedded-container leftovers.
2. Bootstrap the repository for its first **public GitHub** commit: sanitize
   secrets, add ignore/attribute files, and establish the patch-based chat
   workflow (`CLAUDE.md` + `.claude/`).

## Approach & key decisions
- **WAR conversion** (minimal, canonical):
  - `pom.xml`: `<packaging>war</packaging>`; `spring-boot-starter-tomcat`
    added with scope `provided` (excluded from the WAR, still available so
    `mvn spring-boot:run` keeps working in dev — deliberate: we did NOT
    remove `main()`, it is the standard dual-mode setup, not dead code).
  - `AquariusApplication` now `extends SpringBootServletInitializer` and
    overrides `configure()`.
  - `finalName=AquariusWeb` (pre-existing) → `target/AquariusWeb.war` →
    context path `/AquariusWeb`. All templates/JS already use context-relative
    URLs (`@{...}`, `request.getContextPath()`), verified by grep.
  - Removed `server.port` (belongs to Tomcat now).
- **Secrets sanitization** (public repo): `application.properties` rewritten
  in English; tenant DB defaults changed from real LAN host/credentials to
  non-functional `CHANGE_ME` placeholders; real values come from env vars
  (Tomcat `setenv`) or the new gitignored
  `config/application-local.properties` (`spring.config.import=optional:...`).
  Runtime paths (H2 data dir, logs) parameterized via `${AQUARIUSWEB_HOME:.}`
  so Tomcat deployments can point them at a writable directory.
  Stale comments referring to Flyway migrators corrected to the actual
  `SqlMigrationRunner` classes. Security logging DEBUG → INFO.
- **Repo bootstrap**: `.gitignore` (target, data, logs, IDE, local overrides,
  `COMMIT_MSG.txt`), `.gitattributes` (LF normalization — prerequisite of the
  patch workflow), `CLAUDE.md` (full memory bridge: mission, stack, domain
  knowledge, pitfalls, roadmap), `.claude/README.md` (working agreement:
  patch + `COMMIT_MSG.txt` per change, docs updated in the same diff).

## Files touched
- `pom.xml` — packaging war, provided tomcat starter, English description.
- `src/main/java/com/aquarius/AquariusApplication.java` — servlet initializer.
- `src/main/resources/application.properties` — rewritten (see above).
- NEW: `CLAUDE.md`, `.claude/README.md`, `.claude/2026-07-02-war-packaging-and-chat-workflow.md`,
  `.gitignore`, `.gitattributes`.

## Compatibility
- Dev flow changes: `java -jar target/AquariusWeb.jar` is replaced by
  `mvn spring-boot:run` (or deploying the WAR). Local DB settings must be
  provided once via `config/application-local.properties` (gitignored) or env
  vars, since tracked defaults are now placeholders.
- No behavioural change in application code paths; URLs unchanged
  (context-relative everywhere).

## Not verified in sandbox (confirm on deploy)
- Real `mvn clean package` and WAR startup under Tomcat 9.
- H2/logs directory creation under `${AQUARIUSWEB_HOME}` on the target machine.

## Security note (raised to the owner)
`LegacyPasswordVerifier` documents the legacy Caesar +3 password scheme; in a
public repo this is a disclosure. Decision pending: keep repo private vs
accept (DB access already implies full compromise). Web-side auth additionally
uses bcrypt credentials in the system DB.
