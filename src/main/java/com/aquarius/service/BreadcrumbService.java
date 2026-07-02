package com.aquarius.service;

import com.aquarius.dto.MenuNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Costruisce il breadcrumb di una pagina derivandolo dall'albero del menu.
 *
 * <p>Per una URL come {@code /clienti}, attraversa l'albero del menu costruito
 * da {@link MenuService}, trova il nodo foglia il cui {@code url} corrisponde,
 * e ricostruisce il percorso dalle root. Esempio risultato:</p>
 *
 * <pre>
 *   [Clienti] / [Anagrafica] / [Aggiornamento]
 * </pre>
 *
 * <p>Pensato come componente del "framework form CRUD": ogni controller chiama
 * {@link #forUrl(String, String)} e mette la lista nel Model. Il fragment
 * {@code fragments/page-header.html} la renderizza in modo uniforme.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BreadcrumbService {

    private final MenuService menuService;

    /**
     * Cerca il path nel menu per la URL data.
     *
     * @param url          la URL della pagina (es. "/clienti")
     * @param operatorCode codice operatore corrente (necessario per accedere
     *                     al menu cacheato — usa la stessa cache key di MenuService)
     * @return lista di {@link Crumb} dalla root alla foglia, o {@link List#of()}
     *         se non trovata (es. URL non collegata al menu).
     */
    public List<Crumb> forUrl(String url, String operatorCode) {
        if (url == null || url.isBlank()) return List.of();

        List<MenuNode> tree = menuService.buildMenuTree(operatorCode);
        List<Crumb> path = new ArrayList<>();
        if (findRecursive(tree, url, path)) {
            return path;
        }
        log.debug("Breadcrumb: URL '{}' non trovata nell'albero menu di '{}'",
                  url, operatorCode);
        return List.of();
    }

    /**
     * Versione "manuale": costruisce esplicitamente un breadcrumb da una
     * sequenza di label. Utile quando il path nel menu non basta (es. dettaglio
     * di un'entità: "Clienti / Anagrafica / Aggiornamento / [nome cliente]").
     */
    public List<Crumb> manual(Crumb... items) {
        return List.of(items);
    }

    /** Helper di {@link #forUrl} — DFS sull'albero, costruisce {@code path}. */
    private boolean findRecursive(List<MenuNode> nodes, String targetUrl, List<Crumb> path) {
        if (nodes == null) return false;
        for (MenuNode n : nodes) {
            if (n.isSeparator()) continue;

            // Match foglia
            if (targetUrl.equals(n.getUrl())) {
                path.add(new Crumb(n.getLabel(), n.getUrl()));
                return true;
            }

            // Discendi nei figli
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                path.add(new Crumb(n.getLabel(), null));  // container, non cliccabile
                if (findRecursive(n.getChildren(), targetUrl, path)) {
                    return true;
                }
                path.remove(path.size() - 1);  // backtrack
            }
        }
        return false;
    }

    /** Singolo elemento del breadcrumb. {@code url} null = non cliccabile. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Crumb {
        private String label;
        private String url;
    }
}
