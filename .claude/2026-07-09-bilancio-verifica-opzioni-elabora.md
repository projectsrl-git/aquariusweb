# 2026-07-09 — Bilancio di verifica: opzioni + Elabora + due voci (Fase 1)

Consolidamento del bilancio sul modello del form legacy "Stampa bilancio di
verifica" (menu_stbilanc). FASE 1 = struttura + opzioni di visualizzazione.

## Cosa
- **(1)** Due voci di menu sintetiche: "Bilancio di Verifica" (vista=mastri) e
  "…a sezioni contrapposte" (vista=contrapposte). Rimosso lo switch on-page.
- **(3)** Flusso a due passi: la pagina mostra prima il **form opzioni**; il
  bilancio si calcola/mostra solo dopo "**Elabora**" (param `elabora=true`), come
  lo "Stampa" legacy.
- **(2)** La vista contrapposta ora riporta **mastri e sottogruppi** (albero
  compatto a due colonne, solo Saldo) — nuovo fragment `sezioneTreeCompact`.
- **(4.2)** Le due viste condividono lo stesso set di opzioni.
- Opzioni di visualizzazione con **regole di coerenza** (dal codice legacy):
  - `cfMode` (radio, **R1**): normale | solo C/F | no dettaglio C/F (scelta singola).
  - `nonZero` "Non stampa conti a zero" (default on).
  - `contiOrdine` "Stampa conti d'ordine" (default on; off → esclude mastro 04).
  - `soloSottogruppi` "Stampa solo i sottogruppi" (**R2**: via JS disabilita il
    radio "no dettaglio C/F", implicito; ferma il rendering alle foglie).
- **(4.3/5)** Il pulsante Elabora è un submit → l'overlay di caricamento globale
  (già in layout) scatta da sé.

## Interdipendenze legacy (da menu_stbilanc, replicate)
- **R1**: "solo C/F" e "no dettaglio C/F" mutuamente esclusivi → radio group.
- **R2**: "solo sottogruppi" rende implicito "no dettaglio C/F" (+ disabilita saldi
  d'apertura) → JS `applyR2()`.
- **R3**: mese dal > 1 ⇒ saldo d'apertura al mese (FASE 2, non ancora implementato).

## Rinviato a FASE 2 (calcolo, da riconciliare col gestionale)
Mese dal/al, "considera saldi patrimoniali anno precedente", "includi
previsionali", "esclude ratei/risconti previsionali". Cambiano i numeri.

Versione 0.13.0.
