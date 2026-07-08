# 2026-07-08 — Bilancio: switch vista "Sezioni contrapposte"

Aggiunto uno switch di vista sul bilancio (param `?vista=mastri|contrapposte`,
default mastri):
- **Mastri e gruppi**: la vista ad albero con TAB/sub-TAB (esistente).
- **Sezioni contrapposte**: Attività|Passività affiancate su due colonne, e sotto
  Costi|Ricavi affiancate (come la vista originaria pre-raggruppamento), con saldo
  per conto e totale di sezione nel footer.
Switch a `btn-group` accanto al toggle C/F (che ora preserva anche `vista`).
Nuovo fragment `sezioneFlat`; le foglie C/F rispettano il toggle anche qui (i
totali di sezione includono comunque tutto → quadratura invariata).
Controller: nuovo param `vista`. Solo `bilancio.html` + `ContabilitaController`.
Versione 0.8.0.
