# 2026-07-03 — Fix menu links missing context path (404 on /magazzino/...)

## Problem
Clicking a dynamic sidebar item (e.g. Magazzino > Valorizzazione a data) went
to http://localhost:8080/magazzino/valorizzazione (404) instead of
/aquariusweb/magazzino/valorizzazione. The context path was missing.

## Root cause
The sidebar is rendered client-side from /api/menu/tree. Each node.url is an
absolute server path (/magazzino/valorizzazione, ...). In layout.html
renderNode() did `item.href = node.url` verbatim, so under the /aquariusweb
context the links pointed at the server root. This is the same context-path
class of bug fixed earlier, in a spot the previous audit missed (URLs built
in JS from server data, not static @{...} links).

The CTX helper existed but was local to loadMenu(); renderNode() (a separate
function) could not see it.

## Fix
- Hoisted the context helper to script scope as MENU_CTX (shared by loadMenu
  and renderNode).
- renderNode: `item.href = MENU_CTX + node.url`.
- Menu fetch still uses MENU_CTX (unchanged behaviour).

Audited the rest: conti/tree.html builds hrefs from `@{/conti/}` (already
context-aware); no other JS-built absolute hrefs remain.

## Files touched
- src/main/resources/templates/layout.html

## Verification (sandbox)
- node --check on the menu script: OK.
- No remaining JS `.href = '/...'` without context in templates/static.
- git apply --check clean on a fresh clone of HEAD 42d572b.

## Not verified (confirm on deploy)
- Clicking sidebar items now navigates to /aquariusweb/... and opens the page.
