# 2026-07-03 — Complete customer registry tabs (18/19 active)

## Goal
Finish the customer master form: turn the 12 placeholder tabs into real,
editable forms, matching the VFP MENU_CLI000 form.

## Source of truth
Read the VFP form MENU_CLI000.SCT (3291 CLI_ references) from the aquarius
repo to recover the 19 tab captions and the CLI_ fields per area, then
CROSS-CHECKED every field against the real schema
(docs/db_schema/aquarius_schema_full.csv, table U_CLI_AN, 254 columns).
Only fields that actually exist in U_CLI_AN were mapped — no invented columns.

## Result
- 123 new fields mapped across 13 tab areas, all verified against the schema
  with correct types (varchar/numeric/bit/text -> String/BigDecimal/Boolean/text).
- 12 tabs activated: Vettori, Contabili, Posticipi riba, Fido, Rif. cliente,
  Web, Legali, Produzione, Gruppi E-mail, Testi, Fatturare a, Intrastat,
  Fattura elettronica.
- 1 tab kept as placeholder: **Distinte RID** — none of the CLI_*RID columns
  from the VFP form exist in the current U_CLI_AN schema, so there is nothing
  to bind. Documented rather than faked.

## Fields dropped (in VFP form but NOT in U_CLI_AN schema)
CLI_IVASPTRAS, CLI_FACTOR, CLI_NOINCA, CLI_SCASSA, CLI_BLOCCO, CLI_CODRIF,
CLI_CODDES, CLI_EPEC, CLI_FLGDPR, CLI_DTGDPR, CLI_BOVIRT, CLI_TISPE,
CLI_ETICUSTOM, CLI_FATMMIN, CLI_SERVINTRA, CLI_CLDOG, CLI_NOCLDOG, CLI_PA,
all CLI_*RID, CLI_FATELETT, CLI_IDSDI, CLI_FEPICF, CLI_IDNSO. (Left out on
purpose — the plug&play rule forbids inventing columns.)

## Files touched
- entity/tenant/Customer.java: +123 @Column fields (Lombok @Data -> getters/setters).
- controller/CustomerController.java: buildCustomerFormTabs() now marks the 12
  tabs active; copyEditableFields() extended with the 123 setters
  (whitelist-based save, @DynamicUpdate still applies).
- templates/clienti/edit.html: 13 placeholder panes replaced with real form
  fields (text/number/checkbox/textarea by column type). Distinte RID stays a
  placeholder pane.

## Verification (sandbox)
- Java brace/paren scan on entity + controller: OK.
- Every th:field in edit.html (159) maps to an existing entity property: OK.
- edit.html <div> balance 232/232; all 19 tab ids unique (no duplication).
- git apply --check clean on a fresh clone of HEAD 7a03157.

## Not verified (confirm on deploy)
- Full round-trip save of the new fields against the live U_CLI_AN (types,
  bit<->Boolean, numeric scale). Recommend testing a save on one customer
  per tab area before rollout.
