package com.aquarius.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Gestisce lo storico versioni di AquariusWeb sul SYSTEM DB.
 *
 * Al boot della JVM:
 *  - legge {@code app.version} e {@code app.build-time} dalle properties
 *    (popolate via Maven resource filtering: {@code @project.version@} e
 *    {@code @maven.build.timestamp@});
 *  - se è diversa dall'ultima versione registrata in {@code aq_web_app_version},
 *    inserisce un nuovo record (audit-trail dei deploy);
 *  - se è uguale, non fa nulla (multi-restart sulla stessa release = un solo
 *    record).
 *
 * NB: usiamo {@link JdbcTemplate} invece di JPA repository perché il SYSTEM DB
 * è H2 in {@code MODE=MSSQLServer} (per accettare la sintassi SQL Server delle
 * migrazioni), ma Hibernate usa {@code H2Dialect} generico che genera
 * {@code LIMIT ?} — non valido in MSSQLServer-mode. Con JdbcTemplate scriviamo
 * direttamente la sintassi SQL Server compatibile ({@code TOP 1}).
 */
@Service
@Slf4j
public class AppVersionService implements CommandLineRunner {

    private final JdbcTemplate systemJdbc;

    /** Versione del pom.xml — risolta a build-time via filtering @project.version@. */
    @Value("${app.version:0.0.0-dev}")
    private String declaredVersion;

    @Value("${app.build-time:n.d.}")
    private String declaredBuildTime;

    @Value("${app.commit:}")
    private String declaredCommit;

    public AppVersionService(@Qualifier("systemDataSource") DataSource systemDataSource) {
        this.systemJdbc = new JdbcTemplate(systemDataSource);
    }

    /**
     * Eseguito da Spring Boot all'avvio. Order=1 = dopo i runner di migration
     * (Order=0). Niente {@code @Transactional}: usiamo singole esecuzioni
     * autocommit di JdbcTemplate, così se qualcosa va storto non rovinamo
     * tutta la transazione di startup.
     */
    @Override
    @Order(1)
    public void run(String... args) {
        try {
            String currentVersion = declaredVersion;

            // 1. Recupera l'ultima versione registrata
            String lastVersion = systemJdbc.query(
                "SELECT TOP 1 version FROM aq_web_app_version " +
                "ORDER BY applied_at DESC, id DESC",
                rs -> rs.next() ? rs.getString(1) : null
            );

            // 2. Se uguale alla versione corrente, niente da fare
            if (currentVersion.equals(lastVersion)) {
                log.info("Versione '{}' già registrata in aq_web_app_version", currentVersion);
                return;
            }

            // 3. Insert del nuovo record
            int rows = systemJdbc.update(
                "INSERT INTO aq_web_app_version (version, build_time, applied_at) " +
                "VALUES (?, ?, GETDATE())",
                currentVersion, declaredBuildTime
            );
            log.info("Nuova versione registrata in aq_web_app_version: {} (build {}) — {} riga/he inserita/e",
                     currentVersion, declaredBuildTime, rows);

        } catch (Exception e) {
            // Robustezza: un errore nella tabella versioni NON deve impedire
            // all'app di partire. Lo logghiamo come warn e proseguiamo.
            log.warn("Impossibile registrare la versione corrente: {}", e.getMessage());
        }
    }

    /**
     * Versione attualmente in esecuzione (letta dal pom al build-time).
     * NON tocca il DB: serve per il footer del template, deve essere veloce.
     */
    public String getCurrentVersion() {
        return declaredVersion;
    }

    public String getBuildTime() {
        return declaredBuildTime;
    }

    /**
     * Hash abbreviato del commit da cui è stato fatto il build (catturato dal
     * plugin git-commit-id). Se il token non è stato risolto (build senza .git,
     * o placeholder @...@ rimasto), ritorna stringa vuota.
     */
    public String getCommit() {
        if (declaredCommit == null) return "";
        String c = declaredCommit.trim();
        if (c.isEmpty() || c.contains("@")) return "";   // placeholder non risolto
        return c;
    }
}
