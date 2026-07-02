package com.aquarius.security;

import com.aquarius.multitenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Estende il filter standard di login per intercettare anche il parametro {@code tenant}
 * dal form e popolare il {@link TenantContext} PRIMA che l'AuthenticationManager
 * deleghi al provider.
 *
 * Necessario perché il lookup di {@code res_oper} (vedi
 * {@link AquariusAuthenticationProvider}) avviene sul DB del tenant scelto, e
 * il {@code TenantRoutingDataSource} ha bisogno del valore nel thread-local
 * AL MOMENTO della query.
 */
public class TenantAwareAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    public static final String TENANT_PARAMETER = "tenant";

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException {

        String tenant = obtainTenant(request);
        if (tenant != null && !tenant.isBlank()) {
            TenantContext.set(tenant.trim());
        }
        try {
            return super.attemptAuthentication(request, response);
        } finally {
            // Non puliamo qui: serve ancora al SuccessHandler per registrare
            // il tenant nella sessione. La pulizia avviene nel TenantRequestFilter
            // (finally) e nel SuccessHandler una volta finita la pipeline.
        }
    }

    protected String obtainTenant(HttpServletRequest request) {
        return request.getParameter(TENANT_PARAMETER);
    }
}
