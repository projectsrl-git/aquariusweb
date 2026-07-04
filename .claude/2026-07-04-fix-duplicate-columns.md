# 2026-07-04 — Hotfix: duplicate @Column mappings broke startup

## Problem
After the customer-tabs patch (e581084) the app failed to start:
  org.hibernate.MappingException: Repeated column in mapping for entity
  com.aquarius.entity.tenant.Customer column: cli_impfid
Hibernate refuses two properties mapped to the same column. The tenant
EntityManagerFactory failed, cascading to every bean -> no web server.

## Root cause (my error in the previous patch)
Five columns were already mapped by the original 7 tabs and I re-added them
for the new tabs, creating duplicate @Column mappings:
  CLI_IMPFID  creditLimit (existing)      vs creditAmount (new)
  CLI_CLFIDO  creditLimitEnabled (existing) vs creditClass (new)
  CLI_EMAIL   email (existing)            vs emailGroup (new)
  CLI_EMAIL1  emailAlt1 (existing)        vs emailGroup1 (new)
  CLI_EMAIL2  emailAlt2 (existing)        vs emailGroup2 (new)
The previous verification checked "every th:field maps to a property" but NOT
"no column is mapped twice" — that gap is what slipped through.

## Fix
- Removed the 5 duplicate fields (my new ones) from Customer.java and their
  setters from copyEditableFields.
- Remapped the affected form tabs to the pre-existing properties:
    Fido tab       -> creditLimit, creditLimitEnabled
    Gruppi E-mail  -> email, emailAlt1, emailAlt2
  (In the VFP form these tabs are just other views of the same columns.)

## Verification (sandbox)
- No duplicate @Column across Customer.java (added this check permanently).
- Java brace/paren scan OK; every th:field maps to a property; edit.html
  div balance 232/232.
- git apply --check clean on a fresh clone of HEAD e581084.

## Convention added
Entity review must include a duplicate-column scan:
  grep -oE 'name = "[A-Z0-9_]+"' <entity> | sort | uniq -d   (must be empty)

## Not verified (confirm on deploy)
- App starts and the tenant EntityManagerFactory builds; customer edit tabs
  load and save.
