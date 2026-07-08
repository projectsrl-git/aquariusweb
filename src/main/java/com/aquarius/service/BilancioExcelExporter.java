package com.aquarius.service;

import com.aquarius.dto.BilancioLine;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Esporta il bilancio (bilancio di verifica) in Excel: un foglio "Dettaglio"
 * con una riga per conto (Sezione/Conto/Descrizione/Tipo/Dare/Avere/Saldo, valori
 * numerici) e un foglio "Sintesi" con totali e quadratura.
 */
@Service
public class BilancioExcelExporter {

    /** Contenitore leggero dei dati di bilancio da esportare. */
    public static class Data {
        public String anno;
        public List<BilancioLine> attivo, passivo, costi, ricavi, nonClassificati;
        public BigDecimal totAttivo, totPassivo, totCosti, totRicavi, risultato,
                          quadraturaSP, sbilancio;
        public boolean risultatoUtile, quadraturaOk;
    }

    public byte[] export(Data d) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            CellStyle header = headerStyle(wb);
            CellStyle num = numStyle(wb);

            Sheet det = wb.createSheet("Dettaglio");
            writeHeader(det, header, "Sezione", "Conto", "Descrizione", "Tipo",
                        "Prog. Dare", "Prog. Avere", "Saldo");
            int[] rn = {1};
            writeSection(det, num, rn, "Attività", d.attivo);
            writeSection(det, num, rn, "Passività", d.passivo);
            writeSection(det, num, rn, "Costi", d.costi);
            writeSection(det, num, rn, "Ricavi", d.ricavi);
            writeSection(det, num, rn, "Da classificare", d.nonClassificati);
            for (int c = 0; c < 7; c++) det.setColumnWidth(c, 18 * 256);
            det.setColumnWidth(2, 45 * 256);

            Sheet sin = wb.createSheet("Sintesi");
            int r = 0;
            writeKV(sin, r++, header, num, "Esercizio", null, d.anno);
            r++;
            writeKV(sin, r++, header, num, "Attività", d.totAttivo, null);
            writeKV(sin, r++, header, num, "Passività", d.totPassivo, null);
            writeKV(sin, r++, header, num, "Differenza (Att − Pass)", d.quadraturaSP, null);
            r++;
            writeKV(sin, r++, header, num, "Costi", d.totCosti, null);
            writeKV(sin, r++, header, num, "Ricavi", d.totRicavi, null);
            writeKV(sin, r++, header, num,
                    d.risultatoUtile ? "Utile d'esercizio" : "Perdita d'esercizio",
                    d.risultato, null);
            r++;
            writeKV(sin, r++, header, num, "Quadratura", d.sbilancio, null);
            writeKV(sin, r++, header, num, "Bilancio quadra", null,
                    d.quadraturaOk ? "SI" : "NO");
            sin.setColumnWidth(0, 30 * 256);
            sin.setColumnWidth(1, 20 * 256);

            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        }
    }

    private void writeSection(Sheet s, CellStyle num, int[] rn, String sezione, List<BilancioLine> righe) {
        if (righe == null) return;
        for (BilancioLine l : righe) {
            Row row = s.createRow(rn[0]++);
            row.createCell(0).setCellValue(sezione);
            row.createCell(1).setCellValue(l.getAccount() == null ? "" : l.getAccount());
            row.createCell(2).setCellValue(l.getDescription() == null ? "" : l.getDescription());
            row.createCell(3).setCellValue(
                "C".equals(l.getAccountType()) ? "Cliente"
                    : "F".equals(l.getAccountType()) ? "Fornitore" : "");
            numCell(row, 4, l.getTotDare(), num);
            numCell(row, 5, l.getTotAvere(), num);
            numCell(row, 6, l.getSaldo(), num);
        }
    }

    private void writeKV(Sheet s, int rowNum, CellStyle header, CellStyle num,
                         String key, BigDecimal value, String text) {
        Row row = s.createRow(rowNum);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(header);
        if (value != null) numCell(row, 1, value, num);
        else if (text != null) row.createCell(1).setCellValue(text);
    }

    private void writeHeader(Sheet s, CellStyle style, String... values) {
        Row row = s.createRow(0);
        for (int i = 0; i < values.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(values[i]);
            c.setCellStyle(style);
        }
    }

    private void numCell(Row row, int col, BigDecimal v, CellStyle num) {
        Cell c = row.createCell(col);
        if (v != null) c.setCellValue(v.doubleValue());
        c.setCellStyle(num);
    }

    private CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        style.setFont(f);
        return style;
    }

    private CellStyle numStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat fmt = wb.createDataFormat();
        style.setDataFormat(fmt.getFormat("#,##0.00"));
        return style;
    }
}
