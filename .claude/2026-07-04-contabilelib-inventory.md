# 2026-07-04 — Inventario CONTABILELIB (mappatura, non implementazione)

## Scopo
Prima di replicare la logica di prima nota nel web, mappare CONTABILELIB
(prg/contabilelib.PRG, ~59.900 righe, 422 proc/func). Deliverable = documento
di mappatura, NON codice di posting.

## Cosa contiene il documento
docs/architettura/contabilelib-inventario.md:
- Procedure chiave del posting con dimensioni (REGISTRA 793 righe, OK_REG,
  AUT_CONTROPARTITE_ILLIMITATE, REG_ANCLFO_PART_CLI/FOR, REG_DETIVA,
  CALCOLO_IMPOSTA, COD_PAGAM, QUA_REG, REG_NUMRPN, ...).
- Matrice causali TAB_CAUCONT (57 col): i flag CAU_* che pilotano REGISTRA,
  con semantica DESUNTA (da confermare con Erasmo).
- Casistiche speciali già stratificate nel codice (cespiti, anticipi
  cli/for con alert 2022-2024, autofattura/reverse charge, compensazioni,
  pagamenti da griglia).
- Ordine di replica proposto dal più semplice (giroconto puro) ai casi fiscali
  particolari, ciascuno con riconciliazione web-vs-VFP su DB test.
- Punti aperti per Erasmo.

## Conferme dal codice
- REGISTRA è fortemente stratificata (annotazioni Erasmo fino al 2024): non
  replicabile a vista.
- I flag CAU_* non sono letti per nome in REGISTRA: la causale è caricata in
  variabili di lavoro. Ogni flag = un ramo.
- Numerazione via REG_NUMRPN = contatore PARA (già documentato in V4/model doc).

## Nessuna modifica a codice/schema
Solo documentazione (docs/ + .claude/). Nessuna entity, nessuna migration,
nessuna logica: è la base per pianificare le slice di posting.

## Prossimo passo
Con Erasmo: validare la matrice causali e partire dal giroconto puro come
prima causale da implementare + riconciliare.
