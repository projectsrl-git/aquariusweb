# 2026-07-03 — Maven environment profiles (DEV/SIT/UAT/PROD) + secrets layout

## Problem / goal
application.properties required hand-editing tenant URLs, credentials and
AQUARIUSWEB_HOME on every checkout. Goal: pick the environment with a Maven
flag, keep secrets out of git, and never edit config by hand repeatedly.

## Approach
Two layers, cleanly separated:

1. **Maven profiles** (`-Pdev` default, `-Psit`, `-Puat`, `-Pprod`) inject two
   build-time properties into application.properties via **resource filtering**
   (`@env.name@`, `@aquariusweb.home@`; Spring Boot uses `@...@` for Maven to
   avoid clashing with its own `${...}`). `env.name` becomes
   `spring.profiles.active`; `aquariusweb.home` becomes the default home dir.

2. **Per-environment secrets**, never committed:
   - `application.properties` imports, at runtime,
     `config/application-${spring.profiles.active}.properties` (optional:).
   - Real files `config/application-<env>.properties` are **gitignored**.
   - A single versioned template `config/application-ENV.properties.template`
     documents the keys; copy it once per environment and fill in.
   - `config/application-dev.properties` is pre-filled with the known DEV
     values (Impresind/Tremonti on IMPRESIND_TEST) but is gitignored, so it
     stays only on the dev machine.

Non-sensitive per-env overrides (Thymeleaf cache, log levels) live in
**versioned** `application-<env>.properties` inside resources — no secrets
there. Spring merges: common → profile file (jar) → secrets file (external).

## How to use
- Build for an environment:
  `mvn clean package -Pdev` (or -Psit/-Puat/-Pprod). Default is dev.
- First time on a machine, per environment:
  `copy config\application-ENV.properties.template config\application-<env>.properties`
  then fill in real URL/user/password.
- AQUARIUSWEB_HOME: defaults to "." (app dir); env var still overrides.

## Files touched
- `pom.xml`: `<profiles>` (dev default + sit/uat/prod), resource filtering of
  application.properties only.
- `src/main/resources/application.properties`: rewritten — Maven-filtered
  env/home, per-profile secrets import, placeholder tenant defaults.
- NEW versioned: `application-{dev,sit,uat,prod}.properties` (non-sensitive),
  `config/application-ENV.properties.template`.
- `.gitignore`: ignore `config/application-*.properties` but keep `*.template`;
  ignore H2 runtime `*.mv.db`/`*.trace.db`.
- Removed dead `templates/fragments/account-tree-node.html` (recursive fragment
  superseded by aq-tree.js; was still lingering in the repo).

## Security
- Verified: only the placeholder template is staged; zero real values
  (192.168.*, credentials, IMPRESIND_TEST) in the commit.
- The pre-filled DEV secrets file is gitignored and stays local.

## Not verified (confirm on deploy)
- `mvn clean package -Pdev` filtering result and Tomcat 9 run.
- That the running app reads config/application-dev.properties from the JVM
  working directory (under Tomcat, place it relative to CWD or set an absolute
  path / env vars).
