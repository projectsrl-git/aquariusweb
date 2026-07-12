package com.aquarius.service;

import com.aquarius.repository.tenant.VenditeStatsDao.Bucket;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
 * Export Excel delle statistiche fatturato (pattern BilancioExcelExporter):
 * fogli "Per mese", "Per cliente", "Per articolo". Valori numerici in
 * celle numeriche; nessuna compensazione tra tipi documento.
 */
@Service
public class VenditeStatsExcelExporter {

    public byte[] export(String anno, List<Bucket> byMonth,
                         List<Bucket> byCustomer, List<Bucket> byArticle)
            throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = headerStyle(wb);
            sheet(wb, "Per mese " + anno, head,
                new String[]{"Mese", "Fatture", "Imponibile", "Totale netto"},
                byMonth, false);
            sheet(wb, "Per cliente " + anno, head,
                new String[]{"Cliente", "Ragione sociale", "Fatture", "Imponibile", "Totale netto"},
                byCustomer, true);
            sheetArticle(wb, "Per articolo " + anno, head, byArticle);
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    private void sheet(SXSSFWorkbook wb, String name, CellStyle head,
                       String[] cols, List<Bucket> rows, boolean withLabel) {
        Sheet sh = wb.createSheet(name);
        Row h = sh.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(head);
        }
        int r = 1;
        for (Bucket b : rows) {
            Row row = sh.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(b.getKey());
            if (withLabel) row.createCell(c++).setCellValue(b.getLabel());
            row.createCell(c++).setCellValue(b.getDocumenti());
            num(row.createCell(c++), b.getImponibile());
            num(row.createCell(c), b.getTotale());
        }
        for (int i = 0; i < cols.length; i++) sh.setColumnWidth(i, i == 1 ? 10000 : 4200);
    }

    private void sheetArticle(SXSSFWorkbook wb, String name, CellStyle head, List<Bucket> rows) {
        Sheet sh = wb.createSheet(name);
        String[] cols = {"Articolo", "Descrizione", "Righe", "Quantità", "Valore righe"};
        Row h = sh.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = h.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(head);
        }
        int r = 1;
        for (Bucket b : rows) {
            Row row = sh.createRow(r++);
            row.createCell(0).setCellValue(b.getKey());
            row.createCell(1).setCellValue(b.getLabel());
            row.createCell(2).setCellValue(b.getDocumenti());
            num(row.createCell(3), b.getQuantita());
            num(row.createCell(4), b.getTotale());
        }
        for (int i = 0; i < cols.length; i++) sh.setColumnWidth(i, i == 1 ? 10000 : 4200);
    }

    private static void num(Cell cell, BigDecimal v) {
        if (v != null) cell.setCellValue(v.doubleValue());
    }

    private CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); st.setFont(f);
        return st;
    }
}
