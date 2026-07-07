package com.aquarius.web;

import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/**
 * Parametri condivisi per tutte le liste/ricerche (paginazione + ordinamento
 * + dimensione pagina). Centralizza la logica così ogni controller lista la
 * riusa senza duplicare: page, size (con default e opzioni), sort/dir con
 * whitelist dei campi ordinabili (per evitare injection su property arbitrarie).
 *
 * Uso tipico nel controller:
 *   ListParams lp = ListParams.of(page, size, sort, dir,
 *                                  Set.of("data","numero","importo"), "data", "desc");
 *   Page&lt;X&gt; res = repo.search(q, lp.toPageable());
 *   model.addAttribute("list", res);
 *   lp.addToModel(model, "/clienti", q);
 */
@Getter
public class ListParams {

    /** Opzioni di righe-per-pagina offerte all'utente. */
    public static final List<Integer> PAGE_SIZE_OPTIONS = List.of(20, 50, 100, 200);
    public static final int DEFAULT_SIZE = 20;

    private final int page;
    private final int size;
    private final String sort;   // property (già validata contro whitelist)
    private final String dir;    // "asc" | "desc"

    private ListParams(int page, int size, String sort, String dir) {
        this.page = page;
        this.size = size;
        this.sort = sort;
        this.dir = dir;
    }

    /**
     * Costruisce i parametri validando sort contro la whitelist e size contro
     * le opzioni ammesse; applica i default se i valori sono assenti/non validi.
     */
    public static ListParams of(Integer page, Integer size, String sort, String dir,
                                Set<String> sortable, String defaultSort, String defaultDir) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || !PAGE_SIZE_OPTIONS.contains(size)) ? DEFAULT_SIZE : size;
        String srt = (sort != null && sortable.contains(sort)) ? sort : defaultSort;
        String d = "asc".equalsIgnoreCase(dir) ? "asc" : ("desc".equalsIgnoreCase(dir) ? "desc" : defaultDir);
        return new ListParams(p, s, srt, d);
    }

    public Pageable toPageable() {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        Sort srt = "asc".equals(dir) ? Sort.by(sort).ascending() : Sort.by(sort).descending();
        return PageRequest.of(page, size, srt);
    }

    /**
     * Pageable con sola paginazione (nessun Sort). Da usare quando l'ordinamento
     * è gestito esplicitamente nella query (es. query aggregate con GROUP BY,
     * dove il Sort del Pageable genererebbe SQL non valido).
     */
    public Pageable toPageableNoSort() {
        return PageRequest.of(page, size);
    }

    public boolean isAsc() {
        return "asc".equals(dir);
    }

    /** Direzione opposta per un dato campo (per i link toggle nell'header). */
    public String toggledDir(String field) {
        // Se stiamo già ordinando per quel campo asc → prossimo click desc, e viceversa.
        if (field.equals(sort)) {
            return "asc".equals(dir) ? "desc" : "asc";
        }
        return "asc";
    }

    public boolean isSortedBy(String field) {
        return field.equals(sort);
    }
}
