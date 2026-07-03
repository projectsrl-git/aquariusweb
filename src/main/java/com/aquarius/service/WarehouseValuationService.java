package com.aquarius.service;

import com.aquarius.dto.magazzino.OscillazionePrezzo;
import com.aquarius.dto.magazzino.StratoFifo;
import com.aquarius.dto.magazzino.ValorizzazioneRiga;
import com.aquarius.repository.tenant.WarehouseValuationDao;
import com.aquarius.repository.tenant.WarehouseValuationDao.DateBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service della dashboard "Valorizzazione magazzino a una data".
 * Orchestra le query del {@link WarehouseValuationDao} e calcola in Java le
 * metriche §5 della specifica (dataset dell'ordine delle migliaia di righe):
 * KPI, analisi ABC/Pareto, ripartizione anomalie, sintesi volatilità prezzi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseValuationService {

    private final WarehouseValuationDao dao;

    /** Soglia "stabile" per il trend: |trend mensile| < 1% del prezzo medio. */
    private static final BigDecimal STABLE_THRESHOLD_PCT = new BigDecimal("0.01");

    // ════════════════════════════════════════════════════════════════════
    //  API
    // ════════════════════════════════════════════════════════════════════

    public ValuationResult valorizzaMagazzino(LocalDate dataRif, String codMag, DateBase base) {
        List<ValorizzazioneRiga> righe = dao.valorizza(dataRif, codMag, null, base);
        List<OscillazionePrezzo> osc = dao.oscillazionePrezzi(dataRif, codMag, null, base);
        return assemble(dataRif, codMag, base, righe, osc);
    }

    public ValuationResult valorizzaArticolo(LocalDate dataRif, String codMag,
                                             String articolo, DateBase base) {
        List<ValorizzazioneRiga> righe = dao.valorizza(dataRif, codMag, articolo, base);
        List<OscillazionePrezzo> osc = dao.oscillazionePrezzi(dataRif, codMag, articolo, base);
        return assemble(dataRif, codMag, base, righe, osc);
    }

    public List<StratoFifo> strati(LocalDate dataRif, String codMag, String articolo, DateBase base) {
        return dao.strati(dataRif, codMag, articolo, base);
    }

    public List<OscillazionePrezzo> oscillazioneArticolo(LocalDate dataRif, String codMag,
                                                          String articolo, DateBase base) {
        return dao.oscillazionePrezzi(dataRif, codMag, articolo, base);
    }

    public List<String> elencoMagazzini() {
        return dao.elencoMagazzini();
    }

    public List<String> cercaArticoli(String codMag, String prefix) {
        return dao.cercaArticoli(codMag, prefix);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Assemblaggio metriche
    // ════════════════════════════════════════════════════════════════════

    private ValuationResult assemble(LocalDate dataRif, String codMag, DateBase base,
                                     List<ValorizzazioneRiga> righe,
                                     List<OscillazionePrezzo> osc) {
        Kpi kpi = kpi(righe);
        Abc abc = abc(righe);
        Map<String, Long> anomalie = anomalie(righe);
        Volatilita vol = volatilita(osc);

        Map<String, String> asOf = new LinkedHashMap<>();
        asOf.put("data", dataRif.toString());
        asOf.put("codmag", codMag);
        asOf.put("base", base.name());

        return new ValuationResult(asOf, kpi, abc, anomalie, vol, righe, osc);
    }

    Kpi kpi(List<ValorizzazioneRiga> righe) {
        BigDecimal valoreTotale = BigDecimal.ZERO;
        BigDecimal qtaValTot = BigDecimal.ZERO;
        BigDecimal giacenzaPosTot = BigDecimal.ZERO;
        int referenzeValorizzate = 0;
        int anomalie = 0;

        for (ValorizzazioneRiga r : righe) {
            valoreTotale = valoreTotale.add(nz(r.valore()));
            if (nz(r.qtaValorizzata()).signum() > 0) {
                referenzeValorizzate++;
                qtaValTot = qtaValTot.add(r.qtaValorizzata());
            }
            if (nz(r.giacenza()).signum() > 0) {
                giacenzaPosTot = giacenzaPosTot.add(r.giacenza());
            }
            if (!"OK".equals(r.stato())) anomalie++;
        }

        BigDecimal copertura = giacenzaPosTot.signum() > 0
            ? qtaValTot.multiply(new BigDecimal("100"))
                       .divide(giacenzaPosTot, 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal valoreMedio = referenzeValorizzate > 0
            ? valoreTotale.divide(BigDecimal.valueOf(referenzeValorizzate), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new Kpi(
            valoreTotale.setScale(2, RoundingMode.HALF_UP),
            righe.size(),
            referenzeValorizzate,
            copertura,
            valoreMedio,
            anomalie
        );
    }

    Abc abc(List<ValorizzazioneRiga> righe) {
        List<ValorizzazioneRiga> conValore = righe.stream()
            .filter(r -> nz(r.valore()).signum() > 0)
            .sorted(Comparator.comparing(ValorizzazioneRiga::valore).reversed())
            .toList();

        BigDecimal totale = conValore.stream()
            .map(ValorizzazioneRiga::valore)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AbcClasse> classi = new ArrayList<>();
        List<String> paretoLabels = new ArrayList<>();
        List<BigDecimal> paretoValori = new ArrayList<>();
        List<BigDecimal> paretoCum = new ArrayList<>();

        if (totale.signum() > 0) {
            int nA = 0, nB = 0, nC = 0;
            BigDecimal vA = BigDecimal.ZERO, vB = BigDecimal.ZERO, vC = BigDecimal.ZERO;
            BigDecimal cum = BigDecimal.ZERO;
            final int PARETO_TOP = 25;

            for (int i = 0; i < conValore.size(); i++) {
                ValorizzazioneRiga r = conValore.get(i);
                cum = cum.add(r.valore());
                BigDecimal cumPct = cum.multiply(new BigDecimal("100"))
                                       .divide(totale, 1, RoundingMode.HALF_UP);
                if (i < PARETO_TOP) {
                    paretoLabels.add(r.codiceArticolo());
                    paretoValori.add(r.valore().setScale(2, RoundingMode.HALF_UP));
                    paretoCum.add(cumPct);
                }
                if (cumPct.compareTo(new BigDecimal("80")) <= 0)      { nA++; vA = vA.add(r.valore()); }
                else if (cumPct.compareTo(new BigDecimal("95")) <= 0) { nB++; vB = vB.add(r.valore()); }
                else                                                   { nC++; vC = vC.add(r.valore()); }
            }
            classi.add(new AbcClasse("A", nA, pct(vA, totale)));
            classi.add(new AbcClasse("B", nB, pct(vB, totale)));
            classi.add(new AbcClasse("C", nC, pct(vC, totale)));
        }
        return new Abc(classi, paretoLabels, paretoValori, paretoCum);
    }

    Map<String, Long> anomalie(List<ValorizzazioneRiga> righe) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (ValorizzazioneRiga r : righe) {
            out.merge(r.stato(), 1L, Long::sum);
        }
        return out;
    }

    Volatilita volatilita(List<OscillazionePrezzo> osc) {
        int su = 0, giu = 0, stabili = 0;
        BigDecimal sommaVarPct = BigDecimal.ZERO;
        int nVar = 0;
        List<OscillazionePrezzo> ordinabili = new ArrayList<>();

        for (OscillazionePrezzo o : osc) {
            if (o.nCarichi() < 2 || o.trendMensile() == null
                || o.prezzoMedio6m() == null || o.prezzoMedio6m().signum() <= 0) {
                continue;
            }
            BigDecimal sogliaAssoluta = o.prezzoMedio6m().multiply(STABLE_THRESHOLD_PCT);
            if (o.trendMensile().abs().compareTo(sogliaAssoluta) < 0) stabili++;
            else if (o.trendMensile().signum() > 0) su++;
            else giu++;

            // variazione % attesa su 6 mesi rispetto al prezzo medio
            BigDecimal var6m = o.trendMensile().multiply(new BigDecimal("6"))
                .multiply(new BigDecimal("100"))
                .divide(o.prezzoMedio6m(), 1, RoundingMode.HALF_UP);
            sommaVarPct = sommaVarPct.add(var6m);
            nVar++;

            if (o.coeffVariazione() != null) ordinabili.add(o);
        }

        BigDecimal variazioneMedia = nVar > 0
            ? sommaVarPct.divide(BigDecimal.valueOf(nVar), 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        List<OscillazionePrezzo> topVolatili = ordinabili.stream()
            .sorted(Comparator.comparing(OscillazionePrezzo::coeffVariazione).reversed())
            .limit(10)
            .toList();

        return new Volatilita(su, giu, stabili, variazioneMedia, topVolatili);
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    private static BigDecimal pct(BigDecimal parte, BigDecimal totale) {
        return totale.signum() == 0 ? BigDecimal.ZERO
            : parte.multiply(new BigDecimal("100")).divide(totale, 1, RoundingMode.HALF_UP);
    }

    // ════════════════════════════════════════════════════════════════════
    //  DTO risultato
    // ════════════════════════════════════════════════════════════════════

    public record Kpi(BigDecimal valoreTotale, int articoliInGiacenza,
                      int referenzeValorizzate, BigDecimal coperturaPct,
                      BigDecimal valoreMedioReferenza, int numAnomalie) {}

    public record AbcClasse(String classe, int articoli, BigDecimal valorePct) {}

    public record Abc(List<AbcClasse> classi, List<String> paretoLabels,
                      List<BigDecimal> paretoValori, List<BigDecimal> paretoCumPct) {}

    public record Volatilita(int inAumento, int inCalo, int stabili,
                             BigDecimal variazioneMedia6mPct,
                             List<OscillazionePrezzo> topVolatili) {}

    public record ValuationResult(Map<String, String> asOf, Kpi kpi, Abc abc,
                                  Map<String, Long> anomalie, Volatilita volatilita,
                                  List<ValorizzazioneRiga> righe,
                                  List<OscillazionePrezzo> oscillazione) {}
}
