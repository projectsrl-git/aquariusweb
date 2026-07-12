# 2026-07-12 — Cespiti: categorie con riepilogo (patch 2)

/cespiti/categorie on top of patch 1: category list (u_amm_ca) enriched
with a per-category aggregate computed live from u_amm_at (JPQL GROUP
BY): number of assets, total historical value, total depreciated,
total residual. Each row links to the register filtered by that
category. No netting or fiscal math — plain sums of the anagrafica
columns, labelled as such.
