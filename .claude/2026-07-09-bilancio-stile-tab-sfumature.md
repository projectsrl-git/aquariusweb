# 2026-07-09 — Bilancio: TAB marcate, sotto-TAB, box totali, sfumature SP/CE

Solo stile (nessuna logica toccata). Rende il bilancio meno "asettico" senza
eccessi.
- TAB principali Stato Patrimoniale / Conto Economico più marcate (font più
  grande, grassetto, bordo attivo colorato per sezione).
- Attività/Passività e Costi/Ricavi ora sono **sotto-TAB** (nav-tabs) invece di
  pillole/bottoni.
- Box dei totali (sintesi SP/CE) più evidenti: shadow + accento laterale colorato.
- **Due sfumature leggere e distinte**: SP = blu-ardesia (#3f6098), CE =
  verde-salvia (#2f7d59), applicate sia ai box totali sia alla griglia (header
  card, righe mastro/gruppo, footer) via classi `.card-tint-sp` / `.card-tint-ce`
  e param `tint` dei fragment `sezioneTree`/`sezioneTreeCompact`.

Solo `bilancio.html` (style block + classi). Versione 0.13.1.
