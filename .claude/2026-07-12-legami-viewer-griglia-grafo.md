# 2026-07-12 — Viewer "Legami tra programmi": griglia + grafo SVG

Migliorie al viewer /utilita/legami (ProgramGraphController + legami.html + service).
- (2.1) Landing mostra SUBITO tutti i dati: contatori in testa + griglia sotto,
  senza dover cercare.
- (2.2) Griglia paginata, righe per pagina configurabili (20/50/100/200, def 20).
- (2.3) Contatori per tipo CLICCABILI = filtro (?tipo=...), + card "Tutti".
- (2.4) Ricerca in-linea su qualunque campo (nome/descrizione/file/id/new_location)
  via ProgramGraphService.filter(type, q); cerca su TUTTI i dati, non solo la pagina.
- (2.5) I MENU non sono più "orfani": sono nodi di ingresso (richiamati dalla
  gestione menu). Nel dettaglio e in griglia mostrano "ingresso", non l'alert orfano.
- (2.6) Dettaglio nodo: **grafo SVG del vicinato (1 hop)** — centro + genitori a
  sinistra + figli a destra, nodi cliccabili che ri-centrano la vista (server-side
  SVG, nessuna libreria pesante). Reso via neighborhoodSvg() nel controller.
- (3.1) I form mostrano sempre il titolo/descrizione (Caption) accanto a nome e file,
  sia in griglia sia nel dettaglio.
- Service: +filter(type,q), +parentCount/childCount. search()/orphans() restano
  (non più usati dal controller). Download CSV invariato.

NOTA: il grafo è la versione integrata in-app del call-graph viewer standalone; se
si vuole l'esploratore full-graph dark separato, è un secondo step.
Versione 0.23.0. Solo check sintattici (sandbox senza Maven).
