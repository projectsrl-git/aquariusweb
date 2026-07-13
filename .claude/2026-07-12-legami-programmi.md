# 2026-07-12 — Catalogo oggetti + grafo legami + viewer (sessione 12)

## Graph build (commit 1: the CSVs)
Perimeter = menu-reachable closure: 885 forms referenced by tbl_menu
entries (parsed for Caption + METHODS memos), forms they open, 366
called PRGs (123 are libraries, decomposed into 975 PROCEDURE nodes
with adjacent-comment descriptions), FRX print targets, 1050 menu-voce
nodes (type MENU — declared deviation from the mandate enum: the
viewer needs labelled menu parents), 62 WEB nodes from MIGRATED_TO.
3925 objects, 5618 links.

Link extraction: DO FORM (OPENS_FORM), DO x / SET PROCEDURE|LIBRARY
(CALLS_PRG), REPORT FORM (PRINTS_REPORT), name() calls to library
procedures with name>=5 chars (CALLS_PROC, 1315 links; short names
excluded as false-positive prone), tracker new_location + audit
evidenza_web (MIGRATED_TO, 64), &macro/EXECSCRIPT (UNRESOLVED, never
invented). ~603 DO targets that match neither a disk PRG nor a unique
library procedure were NOT forced into links (local form procedures or
out-of-closure) — counted in the migration README.

## Viewer (commit 2)
/utilita/legami (pattern migrazione-viewer): node-centric card with
description ("not invented" note when the source has none), "usato da"
parents (menu voci shown with full path), children grouped by
link_type, MIGRATED_TO highlighted. Every node is clickable and
re-centers the view; navigation breadcrumb kept in a trail param
(capped at 10). Search box, orphans view (FORM/PRG with no mapped
parent = dismissal candidates). Download endpoints (whitelist) for
program_objects/links.csv AND the existing tracker/audit CSVs, as
attachments — so the user can edit and pass them back in chat.
ProgramGraphService: lazy immutable load, reverse index, reuses the
RFC-4180 parser. No heavy JS dependency (mandate: optional only).

MINOR bump 0.21.0 -> 0.22.0. To confirm at deploy: first-load time of
the graph (3925+5618 rows, lazy singleton), memory footprint.
