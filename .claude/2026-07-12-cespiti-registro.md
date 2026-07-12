# 2026-07-12 — Cespiti: analisi + registro read-only (patch 1)

Priority #2 of the coverage audit, now delivered. Analysis in
resources/cespiti/README.md; the key verified facts:
- tables u_amm_at (anagrafica), u_amm_ca (categorie), U_QUO_AM (quote
  per year), u_amm_ad (linked accounting movements);
- GOTCHA: the cespiti archive has NO company dimension — verified in
  MENU_stat_cesp/MENU_AMMCES where PUB_CODSOC filters ONLY the CONTI
  queries, never the asset tables. Single archive per installation;
  stated in the page subtitle, no soc filter applied;
- legacy list order = amm_codcat, amm_codces (verbatim query);
- AMM_DATCES non-empty = disposed (VALCES + PLUMIN plus/minusvalenza);
- three parallel depreciation channels (ordinario/anticipato/accelerato)
  on both anagrafica and quotas; QUO_FLGCGE = quota transferred to GL;
- u_amm_ad carries AMD_NREGIS/AMD_ANNO → drill to
  /contabilita/primanota/{nreg} with the declared caveat that the route
  resolves in the CURRENT fiscal year session.

Module: /cespiti list (JPA entities+repos, ListParams whitelist sort,
LOCAL sortable headers and pager propagating q+cat+stato — session-2
lesson applied from the start), /cespiti/{id} detail (anagrafica dl,
quote per year with Trasferita/Da trasferire badges, linked movements
with primanota drill). Synthetic menu entry "Cespiti" under contabilita,
indexes renumbered. Write side (quota generation, simulation, GL
transfer, IRES param, popolamento) excluded per mandate → tracker.
