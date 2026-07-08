package com.aquarius.dto.documenti;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Legame di tracciabilita' tra documenti (ordine ↔ DDT ↔ fattura),
 * ricostruito dai riferimenti denormalizzati legacy
 * (ORS_NUMORD/ORS_DATORD, ORS_NUMORC/ORS_DATORC, MOV_NUMFAT/MOV_DATFAT).
 */
@Data
@AllArgsConstructor
public class DocumentoCollegato {
    private DocumentType tipo;
    private String numero;
    private String data;       // varchar yyyy/MM/dd
    /** id_unique del documento di destinazione, null se non risolto nell'archivio. */
    private String id;
}
