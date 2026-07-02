package com.aquarius.context;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;

/**
 * Contesto contabile della sessione utente: equivalente moderno della
 * variabile globale {@code PUB_ANNO} di Visual FoxPro in Aquarius.
 *
 * <p>Tutte le query sulle tabelle contabili ({@code CONTI}, {@code MOV_CONT},
 * scadenziari, partitari, ecc.) devono filtrare per anno contabile +
 * società. Questo bean tiene la scelta dell'utente per l'intera sessione HTTP.</p>
 *
 * <p>{@code @Scope(SCOPE_SESSION)} crea un'istanza per sessione HTTP;
 * {@code proxyMode = TARGET_CLASS} permette di iniettarlo in singleton bean
 * (controller, service) via proxy CGLIB.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Login con username/password (HttpSession aperta)</li>
 *   <li>Spring inietta un FiscalContext vuoto (isSet() == false)</li>
 *   <li>FiscalContextInterceptor redirige a /select-year</li>
 *   <li>User sceglie l'anno → POST a /select-year → FiscalContext popolato</li>
 *   <li>Tutti i controller usano {@link #getFiscalYear()} e
 *       {@link #getSocietyCode()} nelle query</li>
 * </ol>
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
@ToString
public class FiscalContext implements Serializable {

    /** Codice anno contabile (es. "2026"). Null = non ancora scelto. */
    private String fiscalYear;

    /** Descrizione human-readable dell'anno (es. "Esercizio 2026 - Aperto"). */
    private String fiscalYearDescription;

    /**
     * Codice società. Default {@code "01"} — equivalente del {@code PUB_CODSOC}
     * di Visual FoxPro. Non significativo per la multi-tenancy moderna (quella
     * è data dal subdomain), ma necessario perché TUTTI i filtri legacy
     * ({@code CON_SOC = '01'}, {@code MOV_SOC = '01'}, ecc.) lo aspettano.
     */
    private String societyCode = "01";

    /** True se l'anno contabile è stato scelto. */
    public boolean isSet() {
        return fiscalYear != null && !fiscalYear.isBlank();
    }

    /** Resetta il contesto (es. su logout o cambio anno). */
    public void clear() {
        this.fiscalYear = null;
        this.fiscalYearDescription = null;
    }
}
