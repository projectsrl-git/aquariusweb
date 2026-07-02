package com.aquarius.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configurazione isolata per il {@link PasswordEncoder}.
 *
 * Tenuta SEPARATA da {@link SecurityConfig} per spezzare un ciclo di
 * dipendenze a livello di bean:
 *
 *   SecurityConfig ──(constructor)──► AquariusAuthenticationProvider
 *           ▲                                    │
 *           │                            (constructor)
 *           │                                    │
 *           └────── PasswordEncoder ◄────────────┘
 *
 * Se PasswordEncoder è definito dentro SecurityConfig, Spring Boot 2.6+
 * rifiuta l'avvio per ciclo. Spostandolo qui (ad un livello diverso
 * dell'albero di dipendenze) il ciclo si spezza:
 *
 *   PasswordEncoderConfig ──► PasswordEncoder ──► AquariusAuthenticationProvider ──► SecurityConfig
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength=10 (default) → ~50ms per hash su hardware moderno.
        // Stesso encoder usato per super-admin (SYSTEM DB) e operatori web (TENANT DB).
        return new BCryptPasswordEncoder();
    }
}
