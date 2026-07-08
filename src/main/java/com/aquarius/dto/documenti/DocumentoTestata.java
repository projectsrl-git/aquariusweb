package com.aquarius.dto.documenti;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Riga del cruscotto "Ristampa documenti": testata unificata di un documento
 * di QUALSIASI archivio del catalogo {@link DocumentType}. Popolata dal
 * {@code DocumentArchiveDao} (le colonne legacy condividono i nomi ORD_*
 * su tutte le tabelle documento).
 */
@Data
public class DocumentoTestata {
    private DocumentType tipo;
    private String id;            // id_unique
    private String aggancio;      // TAGGANCIO
    private String anno;          // ORD_ANNO
    private String numero;        // ORD_NUMORD (per il tipo BOL resta il numero interno; il numero DDT e' nel modulo /ddt)
    private String data;          // ORD_DATORD (varchar yyyy/MM/dd)
    private String codiceSoggetto;   // ORD_CODCLI (cliente o fornitore secondo il tipo)
    private String ragioneSociale;   // ORD_RAGSOC
    private String causale;          // ORD_CAUS
    private String causaleDescrizione; // PARA 'TOP'+ORD_CAUS -> DESCRI
    private String riferimento;      // ORD_RIFERI
    private String agente;           // ORD_AGE
    private String agenteDescrizione; // PARA 'AGE'+ORD_AGE -> DESCRI
    private BigDecimal imponibile;   // ORD_IMPONIB
    private BigDecimal imposta;      // ORD_IMPOSTA
    private String valuta;           // ORD_VALUTA

    public BigDecimal getTotale() {
        BigDecimal a = imponibile == null ? BigDecimal.ZERO : imponibile;
        BigDecimal b = imposta == null ? BigDecimal.ZERO : imposta;
        return a.add(b);
    }
}
