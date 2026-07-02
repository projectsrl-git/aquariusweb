package com.aquarius.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mini-migratore SQL versionato, sostituto di Flyway.
 *
 * Funziona così:
 *  1. Si assicura che esista la tabella di history (col nome configurato).
 *  2. Legge le versioni applicate.
 *  3. Scansiona il classpath per file {@code V<n>__<descrizione>.sql}.
 *  4. Per ogni script PENDING (versione non in history):
 *     - Splitta il contenuto sui delimitatori {@code ^GO$} (case-insensitive,
 *       riga-anchored). È la convenzione standard di SQL Server / SSMS per
 *       separare i batch. Anche H2 in MODE=MSSQLServer la accetta perché GO
 *       lo gestiamo NOI lato Java, non viene mai mandato al DB.
 *     - Esegue ogni batch con {@code Statement.execute()}.
 *     - Inserisce il record in history (versione + descrizione + timestamp).
 *
 * Naming convention:
 *   V1__init_schema.sql       → version=1, description="init schema"
 *   V2__seed_admin_user.sql   → version=2, description="seed admin user"
 *   V10__add_index.sql        → version=10, ...
 *
 * Limitazioni esplicite (KISS):
 *  - Niente checksum dei file: se modifichi una migrazione già applicata,
 *    il migrator non se ne accorge. Per fix, crea V<N+1>__fix.sql.
 *  - Niente rollback automatico: se uno script fallisce a metà, lo stato
 *    del DB è quello al momento del fallimento (alcuni DDL sono comunque
 *    auto-commit su SQL Server, indipendentemente da transazioni).
 *  - Niente lock di concorrenza: assumiamo che l'avvio dell'app sia
 *    serializzato (un solo processo a fare le migrazioni).
 *
 * Per il use case "porting graduale di Aquarius VFP" queste limitazioni
 * sono ampiamente accettabili. Migrazioni piccole, controllate, locali.
 */
@Slf4j
public class SqlMigrationRunner {

    private static final Pattern FILENAME_PATTERN =
        Pattern.compile("V(\\d+)__(.+)\\.sql", Pattern.CASE_INSENSITIVE);

    /**
     * Separator T-SQL standard: una riga che contiene SOLO la parola GO
     * (con eventuali whitespace). Case-insensitive, multiline.
     */
    private static final Pattern GO_SEPARATOR =
        Pattern.compile("(?im)^\\s*GO\\s*$");

    private final DataSource dataSource;
    private final String location;        // es: "classpath:db/migration/system"
    private final String historyTable;    // es: "aq_web_schema_history"
    private final String label;           // per i log: "SYSTEM DB" o "TENANT 'impresind'"

    public SqlMigrationRunner(DataSource dataSource, String location,
                              String historyTable, String label) {
        this.dataSource = dataSource;
        this.location = location;
        this.historyTable = historyTable;
        this.label = label;
    }

    /**
     * Esegue la migrazione. Idempotente.
     *
     * @return numero di script applicati questa volta
     */
    public int migrate() {
        try {
            ensureHistoryTable();
            Set<Integer> applied = getAppliedVersions();
            List<MigrationScript> scripts = scanMigrations();

            int count = 0;
            for (MigrationScript script : scripts) {
                if (applied.contains(script.version)) {
                    log.debug("{}: V{} già applicata, salto", label, script.version);
                    continue;
                }
                log.info("{}: applico V{} - {}", label, script.version, script.description);
                applyMigration(script);
                count++;
            }
            if (count == 0) {
                log.info("{}: nessuna migrazione nuova da applicare ({} totali)",
                         label, scripts.size());
            } else {
                log.info("{}: {} migrazione/i applicate", label, count);
            }
            return count;
        } catch (Exception ex) {
            log.error("{}: migrazione FALLITA - {}", label, ex.getMessage(), ex);
            // Non rilanciamo: l'app prosegue (best-effort).
            return -1;
        }
    }

    /**
     * Crea la tabella di history se non esiste, usando una query che funziona
     * sia su SQL Server che su H2 in MODE=MSSQLServer.
     */
    private void ensureHistoryTable() throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            // Check via INFORMATION_SCHEMA. UPPER() per portabilità: H2 di default
            // uppercasea i nomi delle tabelle al CREATE, SQL Server li lascia
            // mixed case. UPPER su entrambi i lati = match case-insensitive.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE UPPER(TABLE_NAME) = UPPER(?)")) {
                ps.setString(1, historyTable);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) return;
                }
            }
            // Non esiste: la creiamo. Sintassi compatibile SQL Server + H2/MSSQLServer.
            String ddl = "CREATE TABLE " + historyTable + " (" +
                         "  version INT NOT NULL PRIMARY KEY," +
                         "  description VARCHAR(200) NOT NULL," +
                         "  applied_at DATETIME NOT NULL DEFAULT GETDATE()" +
                         ")";
            try (Statement st = c.createStatement()) {
                st.execute(ddl);
            }
            log.info("{}: creata tabella history '{}'", label, historyTable);
        }
    }

    private Set<Integer> getAppliedVersions() throws SQLException {
        Set<Integer> result = new HashSet<>();
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT version FROM " + historyTable)) {
            while (rs.next()) {
                result.add(rs.getInt(1));
            }
        }
        return result;
    }

    private List<MigrationScript> scanMigrations() throws IOException {
        PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(location + "/V*__*.sql");

        List<MigrationScript> list = new ArrayList<>();
        for (Resource r : resources) {
            String filename = r.getFilename();
            if (filename == null) continue;
            Matcher m = FILENAME_PATTERN.matcher(filename);
            if (!m.matches()) {
                log.warn("{}: file '{}' non rispetta il pattern V<n>__<desc>.sql, saltato",
                         label, filename);
                continue;
            }
            int version = Integer.parseInt(m.group(1));
            String description = m.group(2).replace('_', ' ');
            list.add(new MigrationScript(version, description, r));
        }
        list.sort(Comparator.comparingInt(s -> s.version));
        return list;
    }

    private void applyMigration(MigrationScript script) throws Exception {
        // Carica il contenuto del file
        String content;
        try (var is = script.resource.getInputStream()) {
            content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }

        // Splitta su ^GO$. Ogni batch è eseguito separatamente.
        String[] batches = GO_SEPARATOR.split(content);

        try (Connection c = dataSource.getConnection()) {
            // Eseguiamo ogni batch in autocommit (i DDL su SQL Server sono
            // comunque auto-commit di fatto). Niente transazione esplicita.
            for (String batch : batches) {
                String trimmed = batch.trim();
                if (trimmed.isEmpty()) continue;
                try (Statement st = c.createStatement()) {
                    st.execute(trimmed);
                }
            }

            // Registra in history
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO " + historyTable +
                    " (version, description) VALUES (?, ?)")) {
                ps.setInt(1, script.version);
                ps.setString(2, script.description);
                ps.executeUpdate();
            }
        }
    }

    private static class MigrationScript {
        final int version;
        final String description;
        final Resource resource;

        MigrationScript(int version, String description, Resource resource) {
            this.version = version;
            this.description = description;
            this.resource = resource;
        }
    }
}
