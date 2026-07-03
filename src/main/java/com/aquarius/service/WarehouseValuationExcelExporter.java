package com.aquarius.service;

import com.aquarius.dto.magazzino.OscillazionePrezzo;
import com.aquarius.dto.magazzino.ValorizzazioneRiga;
import com.aquarius.service.WarehouseValuationService.ValuationResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Export Excel della valorizzazione magazzino: 4 fogli (Dati, Metriche,
 * Anomalie, Oscillazione prezzi). Usa SXSSF (streaming) per non tenere in
 * memoria l'intero workbook con migliaia di righe.
 */
@Component
public class WarehouseValuationExcelExporter {

    public byte[] export(ValuationResult r) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            CellStyle header = headerStyle(wb);

            // ── Foglio 1: Dati ──────────────────────────────────────────
            Sheet dati = wb.createSheet("Dati");
            writeRow(dati, 0, header, "Codice articolo", "Giacenza", "Qta valorizzata",
                     "Prezzo medio", "Valore", "Stato");
            int rn = 1;
            for (ValorizzazioneRiga riga : r.righe()) {
                Row row = dati.createRow(rn++);
                cell(row, 0, riga.codiceArticolo());
                cell(row, 1, riga.giacenza());
                cell(row, 2, riga.qtaValorizzata());
                cell(row, 3, riga.prezzoMedio());
                cell(row, 4, riga.valore());
                cell(row, 5, riga.stato());
            }

            // ── Foglio 2: Metriche ──────────────────────────────────────
            Sheet met = wb.createSheet("Metriche");
            int mr = 0;
            writeRow(met, mr++, header, "Valorizzazione al", r.asOf().get("data"));
            writeRow(met, mr++, header, "Magazzino", r.asOf().get("codmag"));
            writeRow(met, mr++, header, "Base di calcolo",
                     "DOCU".equals(r.asOf().get("base")) ? "Data documento" : "Data registrazione");
            mr++;
            writeRow(met, mr++, header, "Valore totale magazzino", str(r.kpi().valoreTotale()));
            writeRow(met, mr++, header, "Articoli in giacenza", String.valueOf(r.kpi().articoliInGiacenza()));
            writeRow(met, mr++, header, "Referenze valorizzate", String.valueOf(r.kpi().referenzeValorizzate()));
            writeRow(met, mr++, header, "Copertura valorizzazione %", str(r.kpi().coperturaPct()));
            writeRow(met, mr++, header, "Valore medio per referenza", str(r.kpi().valoreMedioReferenza()));
            writeRow(met, mr++, header, "Anomalie", String.valueOf(r.kpi().numAnomalie()));
            mr++;
            writeRow(met, mr++, header, "Classe ABC", "N. articoli", "% valore");
            for (var c : r.abc().classi()) {
                writeRow(met, mr++, null, c.classe(), String.valueOf(c.articoli()), str(c.valorePct()));
            }
            mr++;
            writeRow(met, mr++, header, "Prezzi in aumento", String.valueOf(r.volatilita().inAumento()));
            writeRow(met, mr++, header, "Prezzi in calo", String.valueOf(r.volatilita().inCalo()));
            writeRow(met, mr++, header, "Prezzi stabili", String.valueOf(r.volatilita().stabili()));
            writeRow(met, mr++, header, "Variazione media prezzi 6 mesi %",
                     str(r.volatilita().variazioneMedia6mPct()));

            // ── Foglio 3: Anomalie ──────────────────────────────────────
            Sheet ano = wb.createSheet("Anomalie");
            writeRow(ano, 0, header, "Codice articolo", "Giacenza", "Qta valorizzata",
                     "Valore", "Stato");
            int an = 1;
            for (ValorizzazioneRiga riga : r.righe()) {
                if ("OK".equals(riga.stato())) continue;
                Row row = ano.createRow(an++);
                cell(row, 0, riga.codiceArticolo());
                cell(row, 1, riga.giacenza());
                cell(row, 2, riga.qtaValorizzata());
                cell(row, 3, riga.valore());
                cell(row, 4, riga.stato());
            }

            // ── Foglio 4: Oscillazione prezzi ───────────────────────────
            Sheet osc = wb.createSheet("Oscillazione prezzi");
            writeRow(osc, 0, header, "Codice articolo", "N. carichi 6m", "Prezzo min",
                     "Prezzo max", "Prezzo medio", "Dev. std", "Coeff. variazione",
                     "Trend mensile", "Prezzo proiettato +6m (indicativo)");
            int on = 1;
            for (OscillazionePrezzo o : r.oscillazione()) {
                Row row = osc.createRow(on++);
                cell(row, 0, o.codiceArticolo());
                cell(row, 1, BigDecimal.valueOf(o.nCarichi()));
                cell(row, 2, o.prezzoMin6m());
                cell(row, 3, o.prezzoMax6m());
                cell(row, 4, o.prezzoMedio6m());
                cell(row, 5, o.devStd6m());
                cell(row, 6, o.coeffVariazione());
                cell(row, 7, o.trendMensile());
                cell(row, 8, o.prezzoProiettato6m());
            }

            wb.write(bos);
            wb.dispose();
            return bos.toByteArray();
        }
    }

    private CellStyle headerStyle(SXSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        style.setFont(f);
        return style;
    }

    private void writeRow(Sheet s, int rowNum, CellStyle style, String... values) {
        Row row = s.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(values[i] == null ? "" : values[i]);
            if (style != null) c.setCellStyle(style);
        }
    }

    private void cell(Row row, int idx, String v) {
        row.createCell(idx).setCellValue(v == null ? "" : v);
    }

    private void cell(Row row, int idx, BigDecimal v) {
        Cell c = row.createCell(idx);
        if (v != null) c.setCellValue(v.doubleValue());
    }

    private static String str(BigDecimal b) {
        return b == null ? "" : b.toPlainString();
    }
}
