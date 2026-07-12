package com.aquarius.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Log di sistema esclusivo della versione web: traccia ogni richiesta utente con
 * i tempi di elaborazione (request→response). Scrive sul logger dedicato
 * "AQUARIUS_ACTIVITY" (file aquarius-web-activity.log via logback-spring.xml).
 */
@Component
public class ActivityLoggingInterceptor implements HandlerInterceptor {

    private static final Logger ACTIVITY = LoggerFactory.getLogger("AQUARIUS_ACTIVITY");
    private static final String START_ATTR = "aqActivityStartNanos";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        long ms = (start instanceof Long) ? (System.nanoTime() - (Long) start) / 1_000_000L : -1L;

        String user = "-";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            user = auth.getName();
        }

        String qs = request.getQueryString();
        ACTIVITY.info("user={} method={} uri={}{} status={} ms={}{}",
            user,
            request.getMethod(),
            request.getRequestURI(),
            qs != null ? "?" + qs : "",
            response.getStatus(),
            ms,
            ex != null ? " error=" + ex.getClass().getSimpleName() : "");
    }
}
