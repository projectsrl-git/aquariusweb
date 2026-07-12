# 2026-07-09 — Bilancio Fase 2 (mese dal/al) + ricerca in-linea + timer loader

## Fase 2 — periodo mese dal/al
- Form: input "Mese dal" / "Mese al" (1..12, default 1/12).
- Repository: `findBilancioPeriodo(soc, anno, meseDal, meseAl)` — stessa
  aggregazione movimenti di `findBilancio` ma filtrata per mese
  (`SUBSTRING(registrationDate,6,2) BETWEEN meseDal AND meseAl`, date yyyy/MM/dd).
- Controller: clamp 1..12 + dal<=al; se 1..12 usa la query annuale invariata
  (risultato identico a prima), altrimenti la query periodo. Badge "Periodo mese
  X–Y" nel risultato quando ristretto.
- **DA RICONCILIARE**: è il periodo dei MOVIMENTI. Per mese dal>1 NON aggiunge
  ancora i saldi d'apertura patrimoniali (R3) né l'opzione "anno precedente"/
  previsionali/ratei — quelle restano il prossimo passo fiscale, da confrontare
  col gestionale.

## (2) Ricerca in-linea
Box "Cerca conto o descrizione" nel risultato: evidenzia le righe che matchano
(conto o descrizione), conta i risultati, attiva la TAB del primo match e ci
scrolla. Client-side, su `.card-tint-sp/ce tr`.

## (1) Timer progressivo nel loader
`layout.html`: l'overlay mostra i secondi che avanzano (0,1 s → …) mentre attende,
timer avviato su show() e fermato su pageshow. Globale.

Versione 0.15.0.
