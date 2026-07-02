package com.aquarius.security;

import com.aquarius.entity.system.Tenant;
import com.aquarius.entity.tenant.OperatorUser;
import com.aquarius.entity.tenant.WebUserCredentials;
import com.aquarius.multitenancy.TenantContext;
import com.aquarius.repository.system.TenantRepository;
import com.aquarius.repository.tenant.OperatorUserRepository;
import com.aquarius.repository.tenant.WebUserCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Authentication provider di Aquarius.
 *
 * Flusso (strategia dati 1.3 — vedi docs/STRATEGIA_DATI.md):
 *
 *  1. Legge tenant dal {@link TenantContext} (popolato dal filter di login).
 *  2. Carica il record società dal SYSTEM DB.
 *  3. Carica l'operatore da {@code res_oper} (TENANT DB, read-only)
 *     per i controlli ereditati da PASS.SCX: esiste, non sospeso.
 *  4. Carica le credenziali web da {@code aq_web_user_credentials} (TENANT DB,
 *     gestita esclusivamente dalla web app).
 *  5. Verifica la password:
 *     - se ESISTONO credenziali web → confronto BCrypt
 *     - se NON esistono → fallback su DECODE() legacy (vedi LegacyPasswordVerifier).
 *       Quando il fallback va a buon fine, marchiamo {@code mustReset=true} per
 *       forzare l'utente a creare credenziali web al primo accesso.
 *  6. Se autenticazione OK, aggiorna {@code last_login_at} sulle credenziali web
 *     (NIENTE update su res_oper).
 *
 * NOTA: questo provider NON scrive mai su res_oper. Aquarius web è in
 * sola lettura sulla tabella legacy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AquariusAuthenticationProvider implements AuthenticationProvider {

    private final TenantRepository tenantRepository;
    private final OperatorUserRepository operatorUserRepository;
    private final WebUserCredentialsRepository webCredentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final LegacyPasswordVerifier legacyVerifier;

    @Override
    @Transactional(transactionManager = "tenantTransactionManager")
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        String username = auth.getName();
        String rawPassword = (String) auth.getCredentials();

        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadCredentialsException("Società non specificata");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
            .filter(Tenant::isEnabled)
            .orElseThrow(() -> new BadCredentialsException("Società non disponibile: " + tenantId));

        // (1) Lookup su res_oper — legacy, read-only.
        OperatorUser op = operatorUserRepository.findByCodeIgnoreCase(username)
            .orElseThrow(() -> {
                log.info("Login fallito su tenant={} username={}: utente non trovato in res_oper",
                         tenantId, username);
                return new BadCredentialsException("Credenziali non valide");
            });

        if (!op.isEnabled()) {
            throw new DisabledException("Utente sospeso");
        }

        // (2) Lookup su aq_web_user_credentials — tabella web-app.
        Optional<WebUserCredentials> webCredsOpt =
            webCredentialsRepository.findByOperatorCodeIgnoreCase(op.getCode());

        boolean passwordOk = false;
        boolean mustReset = false;

        if (webCredsOpt.isPresent()) {
            // Caso A: l'utente ha già credenziali web → verifica BCrypt.
            WebUserCredentials creds = webCredsOpt.get();
            passwordOk = passwordEncoder.matches(rawPassword, creds.getPasswordHash());
            mustReset = creds.isMustResetPassword();
            if (passwordOk) {
                // Aggiornamento ultimo login (solo su aq_web, mai su res_oper)
                creds.setLastLoginAt(LocalDateTime.now());
                webCredentialsRepository.save(creds);
            }
        } else {
            // Caso B: nessuna credenziale web ancora → fallback legacy DECODE().
            // Se passa, l'utente DEVE creare la propria credenziale web al
            // primo accesso (mustReset=true).
            passwordOk = legacyVerifier.matches(rawPassword, op.getLegacyPassword());
            mustReset = passwordOk;
            // Nota: NON creiamo automaticamente WebUserCredentials qui.
            // Lo fa il flusso "imposta password" dopo che l'utente sceglie
            // una nuova password BCrypt.
        }

        if (!passwordOk) {
            log.info("Login fallito su tenant={} username={}: password errata", tenantId, username);
            throw new BadCredentialsException("Credenziali non valide");
        }

        // Costruisce il principal: porta tenant + flag must-reset.
        // NB: trim() perché jTDS + SQL Server possono restituire VARCHAR/CHAR
        // padded con spazi a destra. Senza trim, "SER                 " si propaga
        // a tutto il sistema (URL, query, log) e rompe i matching.
        AquariusPrincipal principal = new AquariusPrincipal(
            safeTrim(op.getCode()),
            webCredsOpt.map(WebUserCredentials::getPasswordHash).orElse(null),
            tenantId,
            safeTrim(tenant.getDisplayName()),
            safeTrim(op.getFullName()),
            null,                         // esercizio: da impostare nella prossima slice
            mustReset,
            true,
            List.of("ROLE_USER")
        );

        UsernamePasswordAuthenticationToken result =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        result.setDetails(auth.getDetails());
        return result;
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
