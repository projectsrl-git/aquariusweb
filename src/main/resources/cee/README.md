# Bilancio CEE — analisi struttura, mappatura e algoritmo (per Opus)

Analisi DEEP del flusso "Bilancio IV direttiva CEE" del legacy (sottomenu
`gestionebilanci1`), fatta sui sorgenti VFP. Qui ci sono le REGOLE; i DATI
(le voci del prospetto e le confluenze) vivono nel DB nelle tabelle
`BILNEW`, `U_INT_TT`, `U_COR_TT` e si consultano dal viewer
`/contabilita/bilancio-cee-struttura`.

## Le tre tabelle del flusso

| tabella | ruolo |
|---|---|
| `BILNEW` | Le VOCI del prospetto CEE **e i loro valori** (CORRENTE/PRECEDENTE). Attenzione: e' una tabella-struttura generica di 39 colonne; il flusso CEE ne usa 6 (vedi `bilnew_struttura_catalog.csv`), le altre sono retaggio non letto dal flusso (INCERTO). |
| `U_INT_TT` | Mappatura CONTO → voce CEE, con **doppia destinazione** dare/avere (vedi sotto). |
| `U_COR_TT` | Confluenze riga→totale (edge list con segno): e' QUI la "gerarchia" dei totali, NON in BILNEW. |

Scoperta importante: **il prospetto CEE non ha un albero padre-figlio in
BILNEW**. La struttura e' data da (a) l'ordine di stampa = `BIL_CODRIG`
(numerico in varchar, confronti con `PADL(...,10)`), (b) il tipo riga
`TIPO_DATO` = `I` Commenti / `V` Dettagli / `T` Totali (validazione verbatim
di menu_cee000), (c) le confluenze di totale in `U_COR_TT`.

Lo spazio di numerazione di `BIL_CODRIG` codifica la sezione:
`VAL(codrig) >= 21600` = **conto economico** (i valori vengono resi in
valore assoluto a fine fase 2), `< 21600` = **stato patrimoniale**
(evidenza: ceecont.PRG, soglia 21600 + ABS). Il confine attivo/passivo
dentro lo SP non e' nei sorgenti: e' implicito nella numerazione delle voci
(dati) — INCERTO, per Opus: derivarlo dai dati o chiedere a Erasmo.

## Regola di mappatura conto → voce (U_INT_TT)

Una riga per conto: `INT_CONTO` → `INT_CODRIG` ("**Codice riga dare**") e,
opzionale, `INT_CODRIA` ("**Codice riga avere**"). Regola (help verbatim del
form menu_ceepdc): *"Se il saldo del conto contabile (dare-avere) risulta
negativo la procedura assegna il codice riga avere in alternativa se
digitato"*. Caso d'uso citato nei sorgenti: "BANCHE IN PASSIVO" — un conto
banca confluisce nell'attivo se il saldo e' positivo, nel passivo se
negativo. Quindi: un conto confluisce in UNA voce per esecuzione, ma la
voce puo' dipendere dal SEGNO del saldo.

## Algoritmo di calcolo (ceecont.PRG, "CALCOLO VALORI BILANCIO CEE")

Pseudocodice fedele, con i riferimenti alle righe del PRG:

```
INPUT: PUB_CODSOC (societa'), PUB_ANNO (esercizio),
       opzione INCLUDI_PREVISIONALI (domanda a video, r.17-22)

FASE 0 — reset (r.23-27)
  UPDATE BILNEW SET CORRENTE = 0 WHERE BIL_CODSOC = soc

FASE 1+2 — assegnazione saldi alle voci (r.29-140)
  PER OGNI riga di U_INT_TT (soc, ordinata per conto):
      saldo = CONTI.CON_IMP_D - CONTI.CON_IMP_A            # (soc, anno, conto)
      SE INCLUDI_PREVISIONALI E (PRE_IMP_D - PRE_IMP_A) <> 0:
          saldo += (PRE_IMP_D - PRE_IMP_A)                 # r.60-66
      SE il conto non esiste in CONTI: avviso 'non presente in archivi' (r.68)
      riga_dest = SE saldo >= 0 → INT_CODRIG
                  ALTRIMENTI    → INT_CODRIA se non vuota, altrimenti INT_CODRIG
                                                            # r.99-110, 'BANCHE IN PASSIVO'
      cerca BILNEW (soc, PADL(riga_dest,10)):
          SE NON TROVATA → avviso data-quality (r.121, verbatim):
             "Verificare la confluenza di <conto> in bilancio CEE in quanto
              non assegnata la riga di destinazione" e SALTA il conto
          ALTRIMENTI → CORRENTE += saldo                   # r.131

FASE 2b — valore assoluto sull'economico (r.150-180)
  PER OGNI riga BILNEW con VAL(BIL_CODRIG) >= 21600:
      CORRENTE = ABS(CORRENTE)
  # NB: casi commentati "da vedere col commercialista": 21900 (rimanenze
  # anni precedenti, cambio segno) e 24400 (variazione materie prime) —
  # oggi DISATTIVATI nel sorgente. INCERTO: decisione di business.

FASE 3 — totali dalle confluenze (r.210-270)
  PER OGNI riga di U_COR_TT (soc, ordinata per COR_RIGA):
      v = CORRENTE della voce COR_RIGA (se esiste)
      delta = (COR_SEGNO = '+') ? +v : -v                  # else = sottrae
      SE la voce COR_CONFLU NON esiste in BILNEW:
          ERRORE BLOCCANTE (r.259): "Non trovata la riga di confluenza,
          aggiornamento annullato" → RETURN
      ALTRIMENTI: CORRENTE(COR_CONFLU) += delta
  # Ogni edge viene applicato e scaricato subito: totali di totali
  # funzionano SOLO se gli edge dei componenti precedono (in ordine di
  # COR_RIGA) l'edge che porta il totale intermedio piu' in alto.
  # INCERTO: dipendenza dall'ordinamento — Opus valuti un ordinamento
  # topologico o la replica fedele dell'ordine legacy.
```

Rollover esercizio (menu_ceeanno, "Gestione nuovo esercizio"):
`PRECEDENTE = CORRENTE; CORRENTE = 0` — cosi' la stampa confronta esercizio
in corso e precedente (STBILCEE.FRX stampa entrambe le colonne; le righe
con `TIPO_DATO='T'` hanno campi con condizione di stampa dedicata).

## Controlli di data quality (per il viewer e per Opus)

1. **Conti non mappati**: un conto di CONTI (soc, anno) senza riga in
   U_INT_TT viene SILENZIOSAMENTE ESCLUSO dal bilancio CEE (il loop parte
   da U_INT_TT). E' l'anomalia piu' insidiosa.
2. **Mappature verso voci inesistenti**: INT_CODRIG (o INT_CODRIA usata)
   che non esiste in BILNEW → il conto viene saltato con avviso (verbatim
   sopra).
3. **Confluenze totali rotte**: COR_CONFLU inesistente in BILNEW →
   il calcolo si INTERROMPE.

Query SSMS pronte (pattern del progetto: validare in SSMS prima di
implementare):

```sql
-- 1) conti dell'esercizio senza mappatura CEE
SELECT c.CON_CONTO, c.CON_DESCR, c.CON_IMP_D - c.CON_IMP_A AS SALDO
FROM CONTI c
LEFT JOIN U_INT_TT i ON i.INT_CODSOC = c.CON_SOC AND i.INT_CONTO = c.CON_CONTO
WHERE c.CON_SOC = '01' AND c.CON_ANNO = '2026' AND i.INT_CONTO IS NULL
ORDER BY c.CON_CONTO;

-- 2) mappature verso voci inesistenti (dare e avere)
SELECT i.INT_CONTO, i.INT_CODRIG, i.INT_CODRIA
FROM U_INT_TT i
LEFT JOIN BILNEW bd ON bd.BIL_CODSOC = i.INT_CODSOC
      AND bd.BIL_CODRIG = RIGHT(REPLICATE(' ',10) + LTRIM(RTRIM(i.INT_CODRIG)), 10)
LEFT JOIN BILNEW ba ON ba.BIL_CODSOC = i.INT_CODSOC
      AND ba.BIL_CODRIG = RIGHT(REPLICATE(' ',10) + LTRIM(RTRIM(i.INT_CODRIA)), 10)
WHERE i.INT_CODSOC = '01'
  AND (bd.id_unique IS NULL
       OR (LTRIM(RTRIM(i.INT_CODRIA)) <> '' AND ba.id_unique IS NULL));

-- 3) confluenze totali verso voci inesistenti (bloccanti per il calcolo)
SELECT t.COR_RIGA, t.COR_CONFLU, t.COR_SEGNO
FROM U_COR_TT t
LEFT JOIN BILNEW b ON b.BIL_CODSOC = t.COR_CODSOC
      AND b.BIL_CODRIG = RIGHT(REPLICATE(' ',10) + LTRIM(RTRIM(t.COR_CONFLU)), 10)
WHERE t.COR_CODSOC = '01' AND b.id_unique IS NULL;
```

## Nota sul deliverable (deviazione dichiarata dal prompt)

Il prompt chiedeva un CSV "una riga per voce CEE": le VOCI pero' sono DATI
del DB (nessun seed nei repo), non sorgenti — un CSV statico sarebbe una
copia non verificabile e subito stantia. Deliverable adattato:
`bilnew_struttura_catalog.csv` = decodifica delle 39 COLONNE di BILNEW
(cio' che i sorgenti permettono di verificare), e le voci reali si
consultano live nel viewer, che mostra per ogni voce i conti confluenti e
le confluenze di totale, con il pannello anomalie dei tre controlli sopra.

## Menu legacy (per il tracker e le mappature FORM_TO_URL)

`gestionebilanci1`: menu_ceeanno (nuovo esercizio, WRITE), menu_cee000
(definizione righe, WRITE), menu_ceepdc (confluenza conti, WRITE),
menu_ceecori (verifica confluenza conto), menu_ceeannu / menu_ceeannt
(annulli, WRITE), menu_ceetot (confluenze totali, WRITE), menu_ceeric
(ricerca/stampa confluenze), menu_ceeret (rettifiche, WRITE), `ceecont`
(calcolo, motore → Opus), menu_stbilcee (stampa FRX), `ceesave`
(export su FLOPPY A: — obsoleto).
