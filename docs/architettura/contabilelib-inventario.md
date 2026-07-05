# CONTABILELIB — inventario della logica di prima nota

Scopo: mappare la libreria contabile VFP (`prg/contabilelib.PRG`, ~59.900
righe, 422 procedure/funzioni) PRIMA di replicarne la logica nel sistema web.
Questo NON è una specifica di implementazione: è la mappa del territorio, da
usare come base di lavoro e da validare con Erasmo (autore/manutentore).

## Avvertenza di metodo
La logica di prima nota NON è replicabile "a vista". È stratificata da anni di
casi reali (le annotazioni nel codice arrivano fino al 2024, firmate Erasmo).
Va replicata UNA CASISTICA ALLA VOLTA, con riconciliazione su DB di test
(stessa registrazione web vs VFP, confronto record-per-record fino
all'identità). Nessuna presunzione di completezza finché non dimostrata.

## Procedure chiave del flusso di posting (con dimensioni)

| Procedura | Righe | Ruolo |
|-----------|------:|-------|
| REGISTRA                     | 793 | Posting principale: scrive MOV_CONT + derivati |
| REG_RIAGGI                   | 563 | Ri-aggiornamento/rettifica registrazioni |
| OK_REG                       | 373 | Validazione registrazione prima del posting |
| AUT_CONTROPARTITE_ILLIMITATE | 344 | Generazione automatica contropartite |
| REG_CLEAR                    | 214 | Reset area di lavoro registrazione |
| REG_ANCLFO_PART_FOR          | 151 | Genera partita fornitore (PART_FOR) |
| REG_DETIVA                   | 133 | Dettaglio/righe IVA |
| REG_ANCLFO_PART_CLI          | 136 | Genera partita cliente (PART_CLI) |
| CALCOLO_IMPOSTA              | 125 | Calcolo imposta IVA |
| COD_PAGAM                    | 125 | Genera scadenze dal codice pagamento |
| QUA_REG                      |  90 | Quadratura Dare/Avere |
| REG_NUMRPN                   |  40 | Preleva/incrementa numero reg. (contatore PARA) |
| SCORPORO_IMPOSTA             |  28 | Scorporo IVA da lordo |

## Matrice delle causali (TAB_CAUCONT)
Il comportamento di REGISTRA è pilotato dai flag della causale contabile
(TAB_CAUCONT, 57 colonne). I flag NON sono letti per nome-colonna in REGISTRA:
la causale è caricata e riversata in variabili di lavoro. Ogni flag è un ramo.
Flag principali (semantica desunta, DA CONFERMARE con Erasmo):

| Flag | Effetto sul posting |
|------|---------------------|
| CAU_TIPMOV | Natura/tipo movimento |
| CAU_SEZION | Sezione Dare/Avere di default |
| CAU_TRAIVA | Genera riga IVA (registro) |
| CAU_TIREIV | Tipo registro IVA (acquisti/vendite/corrispettivi) |
| CAU_IVAPRE | IVA preventiva/prevalenza |
| CAU_MOVPAR | Genera partita (PART_CLI / PART_FOR) |
| CAU_DATSCA | Richiede scadenza → genera SCADENZE via COD_PAGAM |
| CAU_NUMEFF / CAU_MOVPOR | Effetti / portafoglio |
| CAU_MOVAPE | Movimento di apertura esercizio |
| CAU_PROAUT | Protocollo automatico |
| CAU_MOVCES | Movimenta cespiti |
| CAU_MOVRIT | Movimenta ritenute |
| CAU_INDFID | Incide sul fido cliente |
| CAU_AGENTE | Provvigioni agente |
| CAU_CONIVA / CAU_CONTO1..n | Contropartite predefinite della causale |

## Casistiche note (stratificazioni viste nel codice REGISTRA)
Rami speciali già presenti, ciascuno un caso da gestire:
- Cespiti (variabili _XCX_MOV_*, CODICECESPITE)
- Anticipi fornitori/clienti con alert (Erasmo 2022–2024)
- Autofattura di integrazione IVA / reverse charge (_SYSDOC_SET_AUTOFATTURA_FORN)
- Compensazioni cliente/fornitore (REG_ANCLFO_COMPENSAZIONI)
- Pagamenti/incassi da griglia prima nota (AUT_PAGAMENTI_INCASSI_DA_GRIGLIA_PRIMA_NOTA)
- Scorporo imposta / calcolo imposta (lordo↔netto)

## Ordine di replica proposto (dal più semplice)
1. **Giroconto puro** — nessuna IVA, nessuna partita, nessuna scadenza.
   Solo righe D/A che quadrano. È il caso minimo per validare il posting base
   e l'allocazione numero (REG_NUMRPN → contatore PARA).
2. **Prima nota con IVA** (CAU_TRAIVA) senza partita — genera riga registro IVA.
3. **Fattura con partita** (CAU_MOVPAR) — genera PART_CLI/PART_FOR.
4. **Fattura con partita + scadenze** (CAU_DATSCA + COD_PAGAM).
5. Casi speciali (ritenute, cespiti, reverse charge, compensazioni) — uno a uno.

Per ogni step: implementare, poi RICONCILIARE su DB test (web vs VFP), poi
segnare la causale come "verificata". Registro esplicito delle causali coperte.

## Punti da chiudere con Erasmo
- Semantica esatta di ciascun flag CAU_* (la tabella sopra è desunta).
- Lock/atomicità sul contatore PARA NUMREGPN<soc> condiviso col client VFP.
- Regole di contropartita automatica (AUT_CONTROPARTITE_ILLIMITATE).
- Casi fiscali particolari attivi in Impresind (reverse charge, split payment,
  ritenute) e loro causali.
