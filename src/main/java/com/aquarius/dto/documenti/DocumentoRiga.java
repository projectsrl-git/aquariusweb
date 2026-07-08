package com.aquarius.dto.documenti;

import lombok.Data;

import java.math.BigDecimal;

/** Riga (DD) di un documento del cruscotto unificato. */
@Data
public class DocumentoRiga {
    private BigDecimal sequenza;     // ORD_SEQUEN
    private String codiceArticolo;   // ORD_CODART
    private String estensione;       // ORD_MAGA
    private String descrizione;      // ORD_DESART
    private String descrizione2;     // ORD_DES2
    private BigDecimal quantita;     // ORD_QTAORD
    private BigDecimal prezzoNetto;  // ORD_PRZNET
    private BigDecimal valoreRiga;   // ORD_VALORE
    private String codiceIva;        // ORD_IVA

    public boolean isCommento() {
        return descrizione != null && descrizione.trim().startsWith("*** COMMENTO ***");
    }
}
