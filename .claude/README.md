# Working agreement — AquariusWeb chat sessions (patch delivery)

Adapted from the general chat-workflow template. Project context lives in the
repo (`CLAUDE.md` is the map); chat sessions implement changes and deliver
patches; the human applies, builds, deploys and commits.

Chat language: Italian. Everything that lands in the repo — code, comments,
`.claude/*.md`, `CLAUDE.md`, `COMMIT_MSG.txt` — is **English**.
Exception: user-facing UI strings are Italian *business* language by design
(product copy for warehouse/accounting staff, not developer jargon).

Repo: `<REPO_URL>` (fill in after first push) — default branch `main`,
base package `com.aquarius`.

## 0. What chat can and cannot do (honesty first)
- No live access to the working copy: work happens on a sandbox copy
  (re-cloned from `<REPO_URL>` each turn once the repo is public, or restored
  from the last known state).
- The sandbox has **no SQL Server, no Tomcat, no real Maven build**.
  Verification is static: Java brace/paren balance scan, `node --check` on
  inline `<script>` blocks and static `.js`, grep checks for the hard
  constraints below. Real build (`mvn clean package`), browser behaviour and
  live-DB queries are confirmed by the human on deploy — the delivery note
  must say explicitly what was NOT verified.
- Deliverables are **git-diff patches**, never commits, never full archives.

## 1. Start of every task — re-align on the current HEAD
HEAD moves between turns (the human commits). Before generating each patch:
1. Get the current state (clone `<REPO_URL>`, or ask for `CLAUDE.md` +
   relevant `.claude/*.md` if unreachable).
2. Read `CLAUDE.md` and skim `.claude/` for specs relevant to the task.
3. Verify the patch applies on the real current HEAD (§3f).

## 2. Hard constraints (project-specific)
- **Plug&play (prime directive)**: legacy tables are never altered; writes to
  legacy rows only via explicit field whitelists; new tables prefixed
  `aq_web_` and created via `SqlMigrationRunner` scripts.
- Tenant-side `@Transactional` must name `tenantTransactionManager`.
- Global CSS in `layout.html` only (child `<head>` styles are discarded).
- No fragment parameter named `title`; **no recursive Thymeleaf fragments
  with parameters** — use the JSON + client-component pattern (aq-tree).
- Legacy date columns (`MOV_DT*`) are varchar `yyyy/MM/dd`: bind params as
  strings in that format; date-column selection by SQL-text substitution from
  a fixed enum, never a runtime `CASE`; no `TRY_CONVERT` in production SQL.
- **No secrets in tracked files** (public repo): env placeholders +
  gitignored `config/application-local.properties` only.
- New Maven dependencies: only mainstream, justified in the change doc.
- WAR packaging invariants: `spring-boot-starter-tomcat` stays `provided`;
  `AquariusApplication` keeps both `configure()` and `main()`.

## 3. Per-task workflow
Each change ships as **one `.zip`** containing exactly a git-diff
**`.patch`** and a loose **`COMMIT_MSG.txt`**:

a. **Implement** minimally and focused.
b. **Docs in the same change**: create `.claude/YYYY-MM-DD-<slug>.md`
   (problem/goal, approach, files touched, key decisions & trade-offs,
   compatibility notes) and **update `CLAUDE.md`** (status tables, new
   conventions, remove stale info).
c. **Verify in sandbox** and report honestly: Java static scan, `node --check`
   on every touched template's inline scripts, grep the §2 constraints
   against the diff.
d. **Patch = `git diff` of the changed files only** (code + `.claude/` doc +
   `CLAUDE.md`). Nothing generated, no runtime data.
e. **`COMMIT_MSG.txt` is a loose file, NOT inside the diff** (it changes
   every turn and would break re-applying). Format: imperative subject
   ≤ 72 chars, blank line, wrapped body, verification result at the end.
f. **Prove it applies**: `git apply --check` (and `--stat`) against a clean
   current HEAD before delivering.
g. **Package**: zip `.patch` + `COMMIT_MSG.txt`, hand over. No full-source
   archives from now on.

## 4. Human side
1. `git apply <file>.patch` from the repo root (the zip also carries
   `COMMIT_MSG.txt` to the root; it is gitignored).
2. `mvn clean package` → deploy `target/AquariusWeb.war` to Tomcat 9
   (or `mvn spring-boot:run` for dev).
3. `git commit -F COMMIT_MSG.txt`, push.
4. Hard-refresh the browser after JS/template changes.

## 5. Patch mechanics & gotchas
- Repo is normalized to LF (`.gitattributes`), which removes CRLF apply
  failures; if a plain apply fails on a CRLF working tree, use
  `git apply --3way` or `git apply --cached` as fallback.
- Multiple independent patches off the same base coexist, but each one must
  be checked against the *current* HEAD.

## 6. Reporting back (every turn)
Short summary: what changed, files touched, verification output, what could
NOT be verified in the sandbox, and the `git apply --check` result with the
HEAD it was checked against.
