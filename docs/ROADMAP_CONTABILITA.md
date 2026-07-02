# Roadmap Slice 4 — Contabilità Generale

Documento di pianificazione del porting delle funzioni di contabilità generale di Aquarius, basato sugli screenshot in [`DOCS/screenshot_schermate_contabilta.zip`](https://github.com/projectsrl-git/aquarius/tree/main/DOCS) del repo legacy.

---

## Modulo Contabilità — overview

Dal menu legacy (sezione "Contabilità", chiave `contabilit`, **22 voci**) e dagli screenshot allegati emergono **2 grandi aree** in scope per la prima fase:

### Area A — Prima Nota (5 funzioni, una slice intera)

Le 4 schermate del menu prima nota:

| Funzione | Tab. principale | Note |
|---|---|---|
| 1. **Menu Prima Nota** | — | Toolbar e shortcut alle 4 azioni sotto |
| 2. **Inserimento prima nota** | `MOV_CONT` (492K righe) | Form complesso con header + righe Dare/Avere bilanciate + sezione IVA + scadenze. Fattura fornitore + Bonifico fornitore visti negli screenshot. |
| 3. **Aggiornamento prima nota** | `MOV_CONT` | Stesso form dell'inserimento, in modalità modifica. |
| 4. **Ricerca prima nota** | `MOV_CONT` | Lista filtrabile per data, conto, descrizione, partita. |
| 5. **Annullo prima nota** | `MOV_CONT` (soft-delete?) | Da verificare se la cancellazione è fisica o flag. |

### Area B — Situazioni Contabili (10+ viste, slice di reportistica)

Dagli screenshot:

| Funzione | Tabelle |
|---|---|
| Situazione contabile (saldo a video) | `MOV_CONT`, `CONTI` |
| Storico contabile | `MOV_CONT` join `CONTI` |
| Piano dei conti | `CONTI` (130K righe) |
| Anagrafica fornitore (view contabile) | `U_FOR_AN` |
| Scadenziario fornitori (a video) | `PART_FOR` (40K righe) |
| Scadenziario fornitori (stampa) | `PART_FOR` |
| Estrazione Excel scadenziario | `PART_FOR` |
| Partitario fornitori | `PART_FOR2` (25K righe) |

---

## Modello dati di riferimento

Le 4 tabelle chiave (tutte legacy, read-only secondo strategia 1.3):

| Tabella | Righe Impresind | Ruolo |
|---|---|---|
| **`MOV_CONT`** | 492.515 | Movimenti contabili (testate + righe prima nota) |
| **`CONTI`** | 129.998 | Piano dei conti (multi-società, multi-esercizio) |
| **`PART_FOR`** | 40.415 | Partite/scadenze fornitori aperte |
| **`PART_FOR2`** | 25.766 | Partite fornitori storiche / chiuse (?) |
| **`PART_CLI2`** | 31.920 | Partite clienti |
| **`PART_CLI_STO`** | n/d | Partite clienti storiche |
| `U_FOR_AN` | — | Anagrafica fornitori |
| `U_IVA_TO` | — | Codici IVA |
| `LOG_CONT` | 159.426 | Log operazioni contabili (audit) |

Da inserire nella prossima iterazione di QUERIES_PER_PIANIFICAZIONE: schema completo di `MOV_CONT`, `CONTI`, `PART_FOR`.

---

## Piano in sotto-slice

### Slice 4.1 — Read-only: piano dei conti + ricerca prima nota (1-2 settimane)

- Entity `Conto` mappata su `CONTI`
- Entity `PrimaNotaMovimento` mappata su `MOV_CONT`
- Page **`/contabilita/piano-conti`** — albero/lista del piano dei conti
- Page **`/contabilita/prima-nota`** — ricerca con filtri (data, conto, descrizione)
- Page **`/contabilita/prima-nota/{id}`** — dettaglio testata + righe

Niente scritture. Il VFP continua a essere l'unico che inserisce/modifica prima nota.

### Slice 4.2 — Situazioni e reportistica (1-2 settimane)

- Page **`/contabilita/storico-contabile`** — vista cronologica con filtri
- Page **`/contabilita/scadenziario`** — partite aperte fornitori, con estrazione Excel
- Page **`/contabilita/partitario`** — partitario per fornitore/cliente
- Tutti questi sono read-only su `MOV_CONT` + `PART_*`. Export Excel via SXSSF (POI streaming) per gestire dataset grandi.

### Slice 4.3 — Inserimento/Modifica prima nota (3+ settimane)

Questa è la slice più rischiosa: il form deve scrivere su `MOV_CONT` in concorrenza col VFP. Decisioni preliminari:

1. **Ownership della scrittura**: nella prima fase web e VFP scrivono in parallelo. Servono:
   - lock applicativo sulla testata (es. un esercizio in modifica non può essere toccato da nessuno per N minuti)
   - validazione integrità contabile lato web (dare = avere, IVA quadrata)
   - `created_by_app` flag su `MOV_CONT` per distinguere chi ha scritto (richiede ALTER TABLE — qui valutiamo, magari aq_web_audit_log invece)
2. **Replicare la logica VFP**: estrarre i `.prg` rilevanti (`PRIMANOTALIB.PRG` se esiste, `CONTABILELIB.PRG`) per capire le regole di calcolo, numerazione protocollo, gestione IVA.
3. **UI**: il form legacy è denso, con shortcut a 11 entità correlate (clienti, fornitori, piano conti, storico, partitario, scadenziario, ecc.). Il porting web può semplificare con tab anziché modal stack.

### Slice 4.4 — Altre funzioni (1 voce ciascuna, da prioritizzare con cliente)

Restano nel menu Contabilità queste voci non ancora indirizzate:
- Centri di costo (`gestioneccosto`)
- Partitari (clienti — `gestionepartitari`)
- Bilanci (`gestionebilanci`)
- Controllo di gestione
- Bollati (stampa registri)
- Tesoreria
- Budget
- Acquisti/cessioni intracomunitarie (modello INTRA)
- Ritenute acconto professionisti
- Compensi amministratore
- Cespiti
- Anni contabili (gestione esercizi)
- Procedure extracontabili
- Compensazioni clienti/fornitori
- Business unit (`gestionepdvcontabile`)
- Gestione del personale
- Trasferimenti contabili

Da decidere assieme quali sono prioritarie. Suggerimento: dopo Slice 4.1-4.3 fare un check-in.

---

## Cose da chiarire col cliente

1. **Esercizio contabile**: l'header dell'app mostra "Anno: 2026". Va replicato come selettore in alto (l'utente cambia esercizio senza rifare login)? È un parametro che entra nelle query (filtro su `MOV_CONT.MOV_ANNO`)?
2. **Multi-società** all'interno dello stesso tenant: lo screenshot mostra "IMPRESIND S.r.l. su NB-SERGIO". Sembra che Impresind sia un tenant ma anche un selettore di società. Verificare la colonna `MOV_CONT.MOV_CODSOC`.
3. **Concorrenza VFP/web sulla prima nota**: il cliente accetta che entrambi scrivano in parallelo? O preferisce una fase "VFP-only, web read-only" per N mesi, poi switchover?
4. **Pareggio contabile**: la web app deve rifiutare un inserimento sbilanciato (Dare ≠ Avere)? Il VFP lo fa?
5. **Numerazione protocollo**: come si gestisce in modo da non avere collisioni VFP/web?
