# 2026-07-08 — Overlay di caricamento globale + bordi SP/CE

- **Bordi (1)**: le card di sintesi Stato Patrimoniale e Conto Economico passano
  da `border` a `border border-2` (contorno più marcato).
- **Loader (2)**: overlay full-screen "Caricamento in corso…" in `layout.html`,
  mostrato al click su link di navigazione interni e al submit dei form (copre
  l'attesa della pagina lenta, es. bilancio), nascosto su `pageshow` quando la
  nuova pagina è pronta. Esclusi download/`target=_blank`/`data-no-loader`
  (es. il pulsante Esporta Excel ha `data-no-loader`). È globale: vale per tutte
  le pagine.

`layout.html` + `bilancio.html`. Versione 0.9.1.
