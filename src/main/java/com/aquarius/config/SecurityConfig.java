package com.aquarius.config;

import com.aquarius.security.AquariusAuthenticationProvider;
import com.aquarius.security.TenantAwareAuthenticationFilter;
import com.aquarius.security.TenantRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

/**
 * Pipeline di sicurezza di Aquarius.
 *
 * Confronto con CReaM:
 *  - stesso pattern @EnableWebSecurity + formLogin
 *  - in più: filter custom per gestire il parametro "tenant" del form
 *  - in più: TenantRequestFilter per popolare il TenantContext ad ogni richiesta
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AquariusAuthenticationProvider authProvider;
    private final TenantRequestFilter tenantRequestFilter;

    // NB: il @Bean PasswordEncoder NON è qui ma in PasswordEncoderConfig,
    // per spezzare il ciclo SecurityConfig ⇄ AquariusAuthenticationProvider.

    @Bean
    public AuthenticationManager authenticationManager() {
        // Un solo provider: il nostro
        return new ProviderManager(List.of(authProvider));
    }

    @Bean
    public TenantAwareAuthenticationFilter tenantAwareAuthFilter(AuthenticationManager am) {
        TenantAwareAuthenticationFilter f = new TenantAwareAuthenticationFilter();
        f.setAuthenticationManager(am);
        f.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login", "POST"));
        // Redirect dopo successo: dashboard
        f.setAuthenticationSuccessHandler((req, res, auth) -> res.sendRedirect("/dashboard"));
        // Su fallimento: /login?error=...
        SimpleUrlAuthenticationFailureHandler fh = new SimpleUrlAuthenticationFailureHandler("/login?error");
        f.setAuthenticationFailureHandler(fh);
        return f;
    }

    /**
     * Disabilita la registrazione automatica di {@code tenantAwareAuthFilter}
     * come servlet filter standalone. Spring Boot 2.x rileva ogni @Bean che
     * implementa javax.servlet.Filter e lo aggiunge AUTOMATICAMENTE alla
     * servlet filter chain di Tomcat — il che significa che il filter
     * girerebbe DUE VOLTE su ogni request (una in servlet chain, una in
     * SecurityFilterChain via addFilterAt sotto).
     *
     * La doppia esecuzione causa effetti imprevedibili tra cui 403 spuriI
     * su request che dovrebbero passare (es. GET /login).
     */
    @Bean
    public FilterRegistrationBean<TenantAwareAuthenticationFilter> disableTenantAwareAuthFilterAutoReg(
            TenantAwareAuthenticationFilter f) {
        FilterRegistrationBean<TenantAwareAuthenticationFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Stessa motivazione di {@link #disableTenantAwareAuthFilterAutoReg}: il
     * {@code TenantRequestFilter} è @Component (per @Autowired) ma deve girare
     * SOLO nella SecurityFilterChain via addFilterAfter, NON come servlet filter.
     */
    @Bean
    public FilterRegistrationBean<TenantRequestFilter> disableTenantRequestFilterAutoReg(
            TenantRequestFilter f) {
        FilterRegistrationBean<TenantRequestFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TenantAwareAuthenticationFilter loginFilter) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .antMatchers(
                    "/static/**", "/css/**", "/js/**", "/img/**", "/images/**",
                    "/login", "/error"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // NB: NON usiamo .formLogin() perché aggiungerebbe il default
            // UsernamePasswordAuthenticationFilter SULLA STESSA POSIZIONE
            // del nostro loginFilter custom. Sarebbero due filter sovrapposti
            // su POST /login, il default fallirebbe perché ignora il campo "tenant".
            // Usiamo l'exceptionHandling per il redirect a /login degli anonimi
            // e mettiamo solo il nostro filter sulla pipeline.
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            // Filter custom: intercetta POST /login, popola TenantContext, autentica
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
            // Dopo l'auth: il filter che popola TenantContext per ogni richiesta
            .addFilterAfter(tenantRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
