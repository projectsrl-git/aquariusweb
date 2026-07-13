# 2026-07-13 — Viewer legami: rimossa card SVG inline (vista nodo vuota)

Il fix precedente (&amp; nell'SVG) non ha risolto: la vista nodo restava vuota.
La card SVG inline (th:utext) era comunque la regressione rispetto alla versione
Fable5 funzionante. Rimossa la card SVG inline dalla vista nodo e l'attributo
graphSvg dal controller. La vista nodo torna alla struttura provata (header +
"usato da" + "apre/chiama/stampa/migra") con TUTTE le migliorie di griglia
(contatori-filtro, paginazione, ricerca in-linea, fix menu-orfani, title dei form).

Il grafo del vicinato verrà ri-aggiunto in modo ISOLATO (endpoint che serve l'SVG
come risorsa image/svg+xml separata, embeddata via <object>), così un SVG eventualmente
problematico non può rompere la pagina del nodo.
neighborhoodSvg()/helper restano nel controller (riuso per l'endpoint isolato).

Versione 0.23.2. Solo check sintattici (sandbox senza Maven).
