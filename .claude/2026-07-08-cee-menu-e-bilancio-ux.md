# 2026-07-08 — Attivazione menu CEE + UX sintesi bilancio

## Menu Bilancio CEE (1)
La voce CEE non compariva: Fable 5 l'aveva agganciata con nomi form INDOVINATI
in FORM_TO_URL (menu_cee000/ceepdc/…), che non combaciano col COMANDO reale in
tbl_menu. Sostituito con una **voce sintetica** "Bilancio CEE" sotto Contabilità
(injectSyntheticEntries), garantita a prescindere dal nome form legacy. Rimosse
le 5 entry FORM_TO_URL indovinate (evita voci mancanti o duplicate).

## UX bilancio (2)
- 2.1: la card sintesi Stato Patrimoniale mostra ora anche **Differenza**
  (Attività − Passività), colorata come il risultato, accanto ad Attività/Passività.
- 2.2: la quadratura non è più un banner sottile ma un **riquadro prominente**
  (card, stessa misura delle card di sintesi): "(Attività − Passività) = Utile,
  Differenza = 0", verde se quadra / rosso se no.

Solo `MenuService` + `bilancio.html`. Versione 0.8.1.
