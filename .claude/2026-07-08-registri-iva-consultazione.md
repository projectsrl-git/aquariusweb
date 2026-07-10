# 2026-07-08 — Registri IVA (bollati): consultazione read-only

## Table semantics (verified in BOLLATI.PRG — the analysis part)
- Register rows are LOADED from prima nota (MOV_CONT) per soc/anno/MONTH:
  each legacy load deletes and regenerates that month ("ANNULLO DEL
  BOLLATO PRECEDENTE"). The web view therefore shows the registers as
  last loaded per month — stated in the page subtitle.
- **U_IVA_CL** = vendite + corrispettivi rows (CORRISP writes rows with
  IVA_FATNOT='C'; vendite rows are 'F' fattura / 'N' nota di accredito).
- **U_IVF_CL** = acquisti rows (IVF_ prefix; carries IVF_IND100 'S' =
  indeducibile 100%, IVF_DEDUCIBILE bit, IVF_BDOG bit = bolla doganale,
  IVF_NUMFAT = numero fattura fornitore, IVF_NUMPRO = protocollo).
- IVA_CHIAVE / IVF_CHIAVE (numeric) = the register PROTOCOL number
  (assigned from VNUMPRT at load).
- **U_IVA_TO** = period totals per aliquota, precomputed by the load;
  ITO_CLIFOR decoded with verbatim evidence: C=clienti, F=fornitori,
  D=fatture differite da DDT, E=fatture CEE, R=reverse charge,
  A=autofatture (PRG comment r.719). ITO_FATNOT F/N/C.
- **U_IVA_CS / U_IVA_TS** = sospensione/esigibilita' differita
  (NEEDS_DOMAIN: expose only if Impresind uses the regime).
- **U_IVA_CR** = liquidazione per year (monthly columns, riporti,
  chiusure, versamenti) — OUT_OF_SCOPE (fiscal logic → Opus).
- **U_IVA_FE / U_IVA_ES** = plafond per fornitore / esenzione —
  OUT_OF_SCOPE (plafond excluded by mandate).
- ***_DTDOC** variants = parallel registers for liquidazione per data
  documento (PUB_LIQDTDOC) — NEEDS_DOMAIN, not exposed.

## Module
`/contabilita/registri-iva/{vendite|acquisti|corrispettivi}`:
- nav pills between the three registers; month filter (or whole year) +
  free-text search (ragione sociale / numero fattura / codice);
- sortable columns (whitelist enum SortKey), native OFFSET/FETCH
  pagination (StockBalanceDao pattern), default order = protocol;
- "Totali per aliquota" card computed live on the FILTERED set (GROUP BY
  codiva) with grand total row;
- acquisti-only column with Indeducibile / Parz. deduc. / Dogana badges.
- Menu: synthetic entry "Registri IVA" in the contabilita L1 (as the
  prompt requires — no FORM_TO_URL guessing); existing synthetic indexes
  renumbered.
- No drill-down to /contabilita/primanota/{nreg}: rows carry MOV_NUMPRO
  but the detail route keys on numero REGISTRAZIONE — linking would need
  verifying MOV_NREGIS vs MOV_NUMPRO on real data first (noted).

## To confirm at first deploy
Maven build; U_IVA_CL/U_IVF_CL volumes (per-month partitioning keeps
queries small); that IVA_MESE is always zero-padded '01'..'12' (loader
uses WMESE); itDate rendering of IVA_DTFATT strings; the corrispettivi
tab on real data (FATNOT='C' population).
