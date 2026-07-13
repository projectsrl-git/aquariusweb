# 2026-07-13 — Hotfix viewer legami: vista nodo vuota (SVG malformato)

BUG: aprendo il dettaglio di un nodo (es. /utilita/legami?id=prg:aq_browseview)
la scheda risultava vuota (solo header + ricerca).

CAUSA: neighborhoodSvg() costruiva gli href dei nodi cliccabili con '&' NON
escapato ("?id=...&trail=..."). L'SVG inline diventava XML non valido; il parser
"foreign content" del browser sbagliava e ingoiava il resto del contenuto del nodo.

FIX:
- href SVG ora usa '&amp;trail=' (XML valido). Verificato: l'SVG generato ora è
  well-formed (parse XML ok).
- Branch id robusto: si fa return SOLO se il nodo esiste; se l'id non è nel
  catalogo si prosegue alla griglia con un avviso "oggetto non trovato" (niente
  più fall-through su variabili di griglia non valorizzate).

Versione 0.23.1. Solo check sintattici (sandbox senza Maven).
