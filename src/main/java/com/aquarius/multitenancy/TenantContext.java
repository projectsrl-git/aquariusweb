package com.aquarius.multitenancy;

/**
 * Contesto del tenant corrente, tenuto in un ThreadLocal.
 *
 * Viene popolato:
 *  - dal LoginFilter (su POST /login, prima dell'autenticazione)
 *  - dal TenantRequestFilter (su ogni richiesta successiva al login,
 *    leggendo il tenant dall'Authentication in SecurityContext)
 *
 * Viene letto da:
 *  - {@link TenantRoutingDataSource#determineCurrentLookupKey()}
 *    per scegliere il connection pool corretto.
 *
 * IMPORTANTE: ogni filter che lo popola DEVE chiamare {@link #clear()}
 * al termine della richiesta (in un blocco finally) per evitare leak
 * fra richieste nello stesso thread del pool.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
