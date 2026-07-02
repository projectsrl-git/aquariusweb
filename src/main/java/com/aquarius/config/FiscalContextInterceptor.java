package com.aquarius.config;

import com.aquarius.context.FiscalContext;
import com.aquarius.service.FiscalYearService;
import com.aquarius.service.FiscalYearService.FiscalYear;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Intercettore che garantisce un {@link FiscalContext} valorizzato per ogni
 * richiesta autenticata. Comportamento:
 *
 * <ol>
 *   <li>Se la URL è esclusa (login/logout, statici, select-year stesso),
 *       passa sempre.</li>
 *   <li>Se non autenticato, lascia gestire a Spring Security.</li>
 *   <li>Se {@code FiscalContext} non è settato: prova ad
 *       <b>auto-impostare</b> l'anno corrente {@link FiscalYearService#defaultYear()}.
 *       Se riesce, prosegue silenziosamente con la richiesta originale.</li>
 *   <li>Solo se l'auto-impostazione fallisce (nessun anno in PARA per la
 *       società corrente), redirige a {@code /select-year}.</li>
 * </ol>
 *
 * <p>L'utente vede la pagina di selezione solo se vuole cambiare anno
 * manualmente (link in sidebar) o se mancano del tutto gli anni configurati.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FiscalContextInterceptor implements HandlerInterceptor {

    private final FiscalContext fiscalContext;
    private final FiscalYearService fiscalYearService;

    private static final Set<String> ALWAYS_ALLOWED_PREFIXES = Set.of(
        "/login", "/logout", "/select-year",
        "/css/", "/js/", "/img/", "/images/", "/static/", "/webjars/",
        "/favicon.ico", "/error", "/actuator/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        if (isAlwaysAllowed(uri)) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        if (fiscalContext.isSet()) return true;

        // ── Auto-impostazione silenziosa: anno corrente dall'elenco PARA ──
        Optional<FiscalYear> defaultY = fiscalYearService.defaultYear();
        if (defaultY.isPresent()) {
            FiscalYear y = defaultY.get();
            fiscalContext.setFiscalYear(y.getYear());
            fiscalContext.setFiscalYearDescription(y.getDescription());
            log.info("FiscalContext auto-impostato per {}: anno={} ({})",
                     auth.getName(), y.getYear(), y.getDescription());
            return true;
        }

        // Fallback: nessun anno configurato in PARA — chiediamo all'utente
        log.warn("Nessun anno contabile in PARA per società {} — redirect a /select-year",
                 fiscalContext.getSocietyCode());
        String fullUrl = uri + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        String redirect = URLEncoder.encode(fullUrl, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/select-year?redirect=" + redirect);
        return false;
    }

    private boolean isAlwaysAllowed(String uri) {
        if (uri == null) return false;
        for (String prefix : ALWAYS_ALLOWED_PREFIXES) {
            if (uri.equals(prefix) || uri.startsWith(prefix)) return true;
        }
        return false;
    }
}
