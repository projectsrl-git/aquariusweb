# 2026-07-04 — Fix invisible active tab + full-page detail cards

## Two UI issues
1. Clicking a form tab made its label vanish (white text on white) instead
   of highlighting it.
2. The customer detail view showed only 2 cards; it should show all data
   areas as full-page cards and let the user scroll.

## Fix 1 — invisible active tab
Root cause: `.form-shell-nav .nav-link.active` set
`background: var(--aq-primary); color: white`, but `--aq-primary` was NEVER
defined in :root (only --aq-blue/--aq-dark/--aq-darker existed). The unresolved
var left the background transparent -> white text on a light pane -> invisible.
The same undefined var was also used by `.quality-item:hover`.
Fix: define `--aq-primary: #0c4a6e` (the brand navy) in :root. One line,
resolves the tab highlight and every other `var(--aq-primary)` use.

## Fix 2 — detail cards for all areas
Rewrote clienti/detail.html: instead of two fixed cards, render a card per
data area (Anagrafica, Sede e contatti, IVA, Commercio, Coordinate bancarie,
Fido, Sede legale, Referente, Web, Annotazioni), two columns, natural vertical
scroll. Each optional card renders only when it has at least one populated
field (th:if), so customers without e.g. legal-seat data don't get empty
boxes. Actions bar (Torna alla lista / Modifica) moved to the top.
Added a small `.detail-card-title` class (was repeated inline styles).

## Files touched
- src/main/resources/templates/layout.html (define --aq-primary; add
  .detail-card-title)
- src/main/resources/templates/clienti/detail.html (rewritten)

## Verification (sandbox)
- <style> brace balance 97/97; detail.html div balance 45/45; th:block 1/1.
- Every customer.X referenced in detail.html maps to an existing entity
  property.
- git apply --check clean on a fresh clone of HEAD 74bfa73.

## Not verified (confirm on deploy)
- Visual: active tab now highlighted navy; detail page shows all populated
  cards and scrolls.
