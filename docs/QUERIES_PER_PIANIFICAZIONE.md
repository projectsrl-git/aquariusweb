# Query per pianificazione

Sezione "Fabiano, per aiutarmi a raffinare il modello, lanciami queste e mandami i risultati".

Ogni query qui ha un **perché**: cosa ne faccio del risultato.

Lancia ciascuna su **ogni tenant** (Impresind, Tremonti) e segna da quale viene la risposta — alcune cose potrebbero differire fra le due aziende.

---

## 1. Schema corrente di `res_oper`

**Perché:** lo script `sc_sql/res_oper.SQL` del repo legacy mostra solo lo schema originale del 2000-qualcosa. Dopo ci sono **64 file `AGG_RES_OPER_*.sql`** con `ALTER TABLE ... ADD`. Non so quali sono stati applicati alla produzione né nell'ordine giusto. La mia entity `OperatorUser` ne mappa una parte (`RES_SOSPESO`, `RES_EMAIL`, `SOCIETA`...), ma potrebbe esserci di più o di meno.

```sql
SELECT
    ORDINAL_POSITION    AS pos,
    COLUMN_NAME         AS nome,
    DATA_TYPE           AS tipo,
    CHARACTER_MAXIMUM_LENGTH AS lung,
    IS_NULLABLE         AS nullable,
    COLUMN_DEFAULT      AS [default]
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'res_oper'
ORDER BY ORDINAL_POSITION;
```

**Cosa farò con il risultato:** estendo `OperatorUser.java` aggiungendo i `@Column` mancanti. Le colonne che non userò esplicitamente (ce ne saranno) le metto comunque come `@Transient` o le ignoro, ma voglio almeno vederle per sapere cosa c'è.

---

## 2. Conta utenti e stato

**Perché:** capire l'ordine di grandezza (10 utenti? 200?) per dimensionare la migrazione password e la complessità del modulo "gestione utenti".

```sql
SELECT
    COUNT(*) AS utenti_totali,
    SUM(CASE WHEN IN_USO = 1 THEN 1 ELSE 0 END) AS in_uso,
    SUM(CASE WHEN RES_SOSPESO = 1 THEN 1 ELSE 0 END) AS sospesi,
    SUM(CASE WHEN RES_EMAIL IS NULL OR LTRIM(RTRIM(RES_EMAIL)) = '' THEN 1 ELSE 0 END) AS senza_email,
    SUM(CASE WHEN [PASSWORD] IS NULL OR LTRIM(RTRIM([PASSWORD])) = '' THEN 1 ELSE 0 END) AS senza_password
FROM res_oper;
```

**Cosa farò con il risultato:** decide se la migrazione password va fatta a tappeto (tutti reset) o caso per caso. Se ci sono utenti `senza_email` non si può fare reset via SMTP per loro → serve un piano alternativo.

---

## 3. Tabelle di autorizzazione/ruoli

**Perché:** la mia analisi del repo VFP ha trovato i nomi `autorizzazioni`, `utentixruoli`, `user_menu` come tabelle definite. Voglio sapere se in produzione esistono davvero, sono popolate, e che schema hanno.

```sql
SELECT
    t.TABLE_NAME,
    (SELECT SUM(p.rows)
       FROM sys.partitions p
       JOIN sys.tables sysT ON p.object_id = sysT.object_id
       WHERE sysT.name = t.TABLE_NAME AND p.index_id IN (0, 1)) AS rows_count
FROM INFORMATION_SCHEMA.TABLES t
WHERE t.TABLE_TYPE = 'BASE TABLE'
  AND (
       t.TABLE_NAME LIKE 'autoriz%'
    OR t.TABLE_NAME LIKE '%[_]menu%'
    OR t.TABLE_NAME LIKE 'user[_]%'
    OR t.TABLE_NAME LIKE '%[_]ruol%'
    OR t.TABLE_NAME LIKE '%[_]role%'
  )
ORDER BY t.TABLE_NAME;
```

**Cosa farò con il risultato:** se esistono ed è popolate, le mappo. Se sono vuote o assenti, il controllo accessi parte da zero su Aquarius web (Spring Security + ruoli semplici). Mi cambia molto la Slice 2 (sidebar dinamica): se esiste `user_menu` con i ruoli, posso replicare l'autorizzazione legacy; altrimenti vedono tutti tutto e l'ACL la rifacciamo dopo.

---

## 4. Schema di `tbl_menu` se è in SQL Server

**Perché:** nel filesystem legacy `tbl_menu` è un `.DBF` (l'ho già letto e ho i 1529 record). Voglio sapere se in qualche tenant è stato anche migrato in SQL Server (alcuni clienti potrebbero averlo fatto). Se sì, usiamo quella versione. Se no, parto dal DBF.

```sql
SELECT TABLE_NAME, TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME LIKE '%menu%'
ORDER BY TABLE_NAME;
```

**Cosa farò con il risultato:** se zero righe → uso il DBF come sorgente unica per Slice 2. Se trovi `tbl_menu` (o simile) in SQL Server → mando uno SELECT * INTO #tmp + DDL per allineare il modello.

---

## 5. Censimento tabelle business reali

**Perché:** lo script `sc_sql/*.sql` del repo dice 759 tabelle "create-able". Voglio sapere quante esistono davvero in produzione e quante hanno dati. Mi serve per la prioritizzazione delle Slice di business.

```sql
SELECT
    t.name AS table_name,
    SUM(p.rows) AS row_count
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE t.is_ms_shipped = 0
GROUP BY t.name
ORDER BY row_count DESC;
```

**Cosa farò con il risultato:** ordinerei i form da migrare per "tabella di backing più popolata". Es: se `U_CLI_AN` (clienti) ha 50.000 righe e `U_PROVV` (provvigioni) ha 12, è ovvio quale priorità dare prima.

---

## 6. Sample di `res_oper` (3 righe, anonymized OK)

**Perché:** voglio vedere come sono fatti i dati veri. Se la `PASSWORD` legacy è sempre 20 caratteri (`varchar(20)` con padding spaces? o sempre <= 20?), che formato ha `CODICE` (codici brevi tipo `FB`, `ME`? o lunghi tipo `f.berto`?), che valori prende `SOCIETA`.

```sql
SELECT TOP 3
    CODICE,
    DESCRI,
    LEN(RTRIM([PASSWORD])) AS pwd_len,
    IN_USO,
    RES_SOSPESO,
    CASE
        WHEN RES_EMAIL IS NULL OR LTRIM(RTRIM(RES_EMAIL)) = '' THEN '(vuoto)'
        ELSE 'OK'
    END AS email_status,
    SOCIETA
FROM res_oper
WHERE IN_USO = 1
ORDER BY CODICE;
```

Se preferisci non condividere CODICE/DESCRI reali, sostituisci con `'***'` — mi basta la struttura dei valori.

---

## 7. La res_oper è uguale in Impresind e Tremonti?

**Perché:** se i due tenant hanno schemi DIVERSI (es. Tremonti ha colonne in più), l'entity Java deve essere il superinsieme dei due, oppure ho un problema di compatibilità da risolvere.

```sql
-- Lancia su Impresind e copia il risultato
SELECT COUNT(*) AS num_colonne, COUNT_BIG(*) * 1.0 AS placeholder
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'res_oper';
```

E confronta semplicemente i numeri tra Impresind e Tremonti. Se sono uguali → probabilmente OK. Se diversi → mandami l'output della query 1 da entrambi.

---

## 8. Quale versione Aquarius gira oggi su ciascun tenant?

**Perché:** la `VERSIONE.DBF` del repo dice `Ver.2023.05.15.00.19.36`. Ma quel file potrebbe essere vecchio nel repo e diverso dalla produzione. La produzione potrebbe avere migrazioni più recenti applicate.

```sql
-- Se esiste una tabella VERSIONE o simili
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME LIKE 'versione%' OR TABLE_NAME LIKE 'version%';

-- E se sì, cosa contiene
-- SELECT TOP 5 * FROM VERSIONE;
```

---

## Come mandarmi i risultati

Per ciascuna query, copia/incolla l'output direttamente nella prossima conversazione. Per le query con righe (1, 3, 5, 6), va benissimo anche un dump CSV o uno screenshot — purché si legga.

Se preferisci, puoi anche fare un singolo file `.sql` con tutte le query e mandarmi il `.txt` o `.csv` dell'output completo. Io ricostruisco.

---

## E il menu? Non serve una query?

No: la `tbl_menu.DBF` del repo l'ho già parsata (è il file da cui abbiamo costruito il grafo nella scorsa conversazione). Ho **1.529 record con MENU/LABEL/COMANDO/ICONA/LIVELLO_ME**. Più che sufficiente per costruire la Slice 2 (sidebar dinamica) senza che tu lanci nulla.

Quello che POTREBBE servirmi sul menu è invece: **chi vede cosa**. Cioè il contenuto effettivo della tabella `user_menu` o `autorizzazioni` in produzione (vedi query 3). Se è popolata, le voci di menu sono filtrate per utente — informazione che non sta in `tbl_menu.DBF` ma solo nel DB di produzione.
