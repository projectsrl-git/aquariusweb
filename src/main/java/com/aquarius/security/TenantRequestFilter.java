package com.aquarius.security;

import com.aquarius.multitenancy.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Per ogni richiesta autenticata, popola il {@link TenantContext} con il
 * tenant del {@link AquariusPrincipal} corrente.
 *
 * Si registra dopo Spring Security nella filter chain (vedi SecurityConfig),
 * quindi quando arriva qui la Authentication è già nel SecurityContext.
 *
 * Pulisce il ThreadLocal in finally per evitare leak fra richieste sullo
 * stesso thread del pool Tomcat.
 */
@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AquariusPrincipal p) {
                TenantContext.set(p.getTenantId());
            }
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
