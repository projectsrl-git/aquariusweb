package com.aquarius.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoint dell'assistente integrato — porting da CReaM
 * ({@code com.cream.controller.HelpApiController}).
 *
 * Implementazione attuale: matching per parole chiave con risposte pre-scritte
 * (come l'originale CReaM). Punto di estensione per integrare un vero LLM
 * (es. Anthropic Claude, OpenAI) → vedi commento in {@link #generateResponse}.
 *
 * Il contenuto delle risposte è specifico di Aquarius (ERP), non di CReaM (CRM).
 * Da espandere progressivamente man mano che le slice della web app vanno in
 * produzione e si vuole spiegare all'utente come usarle.
 */
@RestController
@RequestMapping("/api/help")
public class HelpApiController {

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.getOrDefault("message", "");
        String response = generateResponse(userMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("timestamp", new Date());
        return result;
    }

    /**
     * Routing semplice per parole chiave.
     *
     * Estensione futura: sostituire l'if/else con una chiamata a un LLM
     * (Anthropic API o simile). Vedi /docs/AI_ASSISTANT.md per il piano.
     */
    private String generateResponse(String message) {
        String msg = message.toLowerCase();

        if (containsAny(msg, "login", "password", "accedere", "credenziali")) {
            return "**Accesso al sistema:**\n\n" +
                   "1. Seleziona la **Società** dal menu a tendina (Impresind, Tremonti, …).\n" +
                   "2. Inserisci il codice **Operatore** (lo stesso del vecchio Aquarius VFP).\n" +
                   "3. Inserisci la **Password** web.\n\n" +
                   "Al primo accesso la password coincide con quella settata dall'admin. " +
                   "Ti verrà chiesto di cambiarla immediatamente.";
        }

        if (containsAny(msg, "query", "report", "personaliz", "sql")) {
            return "**Query personalizzate:**\n\n" +
                   "Dal menu vai su **'Query personalizzate'**. Puoi:\n" +
                   "- creare nuove query SELECT sulle tabelle Aquarius\n" +
                   "- usare parametri nominali (`:nome_parametro`)\n" +
                   "- eseguire la query e scaricare il risultato in CSV\n\n" +
                   "Per ragioni di sicurezza solo SELECT sono permessi (no DELETE/UPDATE/DROP).";
        }

        if (containsAny(msg, "società", "tenant", "azienda", "cambiare ditta")) {
            return "**Multi-tenancy:**\n\n" +
                   "Aquarius web supporta più società sullo stesso server: ognuna ha il suo " +
                   "database isolato. Per cambiare società devi rifare il login: " +
                   "click su **Esci** in basso a sinistra, e nel form di login seleziona l'altra società.";
        }

        if (containsAny(msg, "vfp", "foxpro", "vecchio", "legacy", "parallelo")) {
            return "**Convivenza con il VFP legacy:**\n\n" +
                   "Aquarius web e Aquarius VFP girano sullo stesso database. " +
                   "Tutte le tabelle legacy (res_oper, U_CLI_AN, ...) restano gestite dal VFP. " +
                   "La web app aggiunge solo tabelle nuove con prefisso `aq_web_` " +
                   "(es. credenziali web, query personalizzate) e per il resto LEGGE dal DB esistente. " +
                   "Puoi continuare a usare il VFP per le funzioni non ancora portate.";
        }

        if (containsAny(msg, "menu", "navigazion", "trovare")) {
            return "**Navigazione:**\n\n" +
                   "Il menu laterale a sinistra è popolato (slice 2 in arrivo) a partire dalla " +
                   "vecchia `tbl_menu` del VFP, filtrato per ruolo. Le voci anagrafiche/gestionali " +
                   "saranno cliccabili man mano che le rispettive slice della migrazione completano.";
        }

        if (containsAny(msg, "help", "aiuto", "come", "cosa puoi")) {
            return "**Cosa puoi chiedermi:**\n\n" +
                   "- come fare il login\n" +
                   "- come creare e usare query personalizzate\n" +
                   "- come funziona il multi-tenancy\n" +
                   "- come la web app convive con il VFP legacy\n" +
                   "- come funziona il menu\n\n" +
                   "Sono un assistente basato su parole chiave (per ora). " +
                   "L'integrazione con un vero AI è in roadmap.";
        }

        // Default
        return "Non ho una risposta pronta per questa domanda — sono un assistente basato su " +
               "parole chiave e copro solo un sottoinsieme di argomenti. Prova a chiedermi di:\n\n" +
               "- *login*, *password*, *credenziali*\n" +
               "- *query personalizzate*, *report*\n" +
               "- *società*, *tenant*\n" +
               "- *VFP*, *legacy*, *parallelo*\n" +
               "- *menu*, *navigazione*\n\n" +
               "Oppure scrivi *help* per la lista completa.";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
