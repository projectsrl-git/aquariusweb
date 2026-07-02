package com.aquarius.security;

import org.springframework.stereotype.Component;

/**
 * Verifica le password "legacy" Aquarius VFP.
 *
 * Le password originali sono offuscate dalla funzione VFP custom chiamata
 * {@code DECODE()}, definita in {@code prg/UTILITA.PRG}:
 *
 * <pre>
 * FUNCTION ENCODE                FUNCTION DECODE
 * PARA V1                        PARA V1
 * V2=""                          V2=""
 * FOR X=1 TO LEN(V1)             FOR X=1 TO LEN(V1)
 *    V2=V2+CHR(ASC(...)-3)          V2=V2+CHR(ASC(...)+3)
 * NEXT                           NEXT
 * RETURN(V2)                     RETURN(V2)
 * </pre>
 *
 * Cifrario di Cesare con shift di 3 (encode = -3, decode = +3). Non è un hash
 * crittografico: è reversibile. Va bene per noi solo come fallback durante la
 * convivenza VFP/web, finché tutti gli operatori non si sono creati una
 * password BCrypt via reset.
 *
 * Comportamento di PASS.SCX (esattamente replicato):
 *
 * <pre>
 *   IF alltrim(THISFORM.V_PASS) = alltrim(DECODE(alltrim(PASSWORD)))
 * </pre>
 *
 * Esempio numerico: KLBJF → NOEMI
 *   K(75)+3=N(78), L(76)+3=O(79), B(66)+3=E(69), J(74)+3=M(77), F(70)+3=I(73)
 */
@Component
public class LegacyPasswordVerifier {

    /**
     * @param rawPassword  password digitata dall'utente
     * @param storedLegacy contenuto della colonna res_oper.PASSWORD (offuscata
     *                     da ENCODE(), può avere trailing whitespace se la
     *                     colonna è CHAR padded)
     * @return true se le due corrispondono secondo l'algoritmo legacy
     */
    public boolean matches(CharSequence rawPassword, String storedLegacy) {
        if (rawPassword == null || storedLegacy == null) {
            return false;
        }
        String storedTrimmed = storedLegacy.trim();
        String inputTrimmed = rawPassword.toString().trim();
        if (storedTrimmed.isEmpty() || inputTrimmed.isEmpty()) {
            return false;
        }
        String decoded = decode(storedTrimmed);
        // Replica esatta del confronto VFP: alltrim() su entrambi
        return decoded.trim().equals(inputTrimmed);
    }

    /**
     * Inverte {@code ENCODE()}: per ogni codice carattere del input, aggiunge 3.
     * Equivalente di {@code DECODE()} in UTILITA.PRG.
     *
     * NB: opera sui codepoint a byte (low 16-bit, abbastanza per ASCII).
     * Niente wraparound: se sotto/sopra range produce caratteri di controllo,
     * come faceva VFP.
     */
    static String decode(String encoded) {
        StringBuilder sb = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            sb.append((char) (encoded.charAt(i) + 3));
        }
        return sb.toString();
    }

    /**
     * Funzione complementare {@code ENCODE()}: per ogni codice carattere
     * sottrae 3. Esposta per testing e per la coerenza con il VFP, ma in
     * pratica Aquarius web NON dovrebbe MAI scrivere su res_oper.PASSWORD
     * (è territorio del VFP, vedi strategia dati 1.3).
     */
    static String encode(String plain) {
        StringBuilder sb = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            sb.append((char) (plain.charAt(i) - 3));
        }
        return sb.toString();
    }
}
