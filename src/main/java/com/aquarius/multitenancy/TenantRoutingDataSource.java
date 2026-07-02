package com.aquarius.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * DataSource che, ad ogni getConnection(), guarda il tenant corrente
 * nel {@link TenantContext} e instrada verso il connection pool del
 * tenant giusto.
 *
 * I target sono configurati una volta in fase di startup (vedi
 * {@code TenantDataSourceConfig}). Se in futuro vogliamo aggiungere/togliere
 * tenant a caldo, basta esporre setTargetDataSources(...) + afterPropertiesSet().
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantRoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        String tenant = TenantContext.get();
        if (tenant == null) {
            // Nessun tenant nel contesto: di solito è un errore di programmazione
            // (chiamata DB tenant-scoped senza essere passati da un filter).
            // Logghiamo a WARN e ritorniamo null: lo Spring sceglierà il default.
            log.warn("Nessun tenant nel TenantContext al momento della query. "
                   + "Verrà usato il DataSource di default.");
        }
        return tenant;
    }
}
