# 2026-07-03 — Fix context-path bugs in WAR deployment (post-login 404)

## Problem
After login the app redirected to /dashboard (404) instead of
/aquariusweb/dashboard. Under Tomcat the app runs in the context
/aquariusweb, but several absolute paths ignored it. Confirmed from
localhost_access_log: POST /aquariusweb/login -> 302, then GET /dashboard -> 404.

## Root cause
Absolute paths that don't go through the servlet context:
- SecurityConfig success handler used res.sendRedirect("/dashboard").
  HttpServletResponse.sendRedirect does NOT prepend the context path.
- Four JS fetch() calls used absolute URLs ('/api/menu/tree',
  '/custom-reports/validate', '/custom-reports/{id}/execute',
  '/api/help/chat') — they resolve against the server root, not the app.

Spring MVC controller returns ("redirect:/...") are fine: Spring prepends
the context automatically. Thymeleaf @{...} links in templates are fine too.
The FiscalContextInterceptor already used request.getContextPath().

## Fix
- SecurityConfig: res.sendRedirect(req.getContextPath() + "/dashboard").
- layout.html: expose the context via <meta name="_ctx" th:content="@{/}">;
  the menu loader reads it (CTX) and prefixes the fetch.
- custom-reports/form.html, custom-reports/detail.html, help/chat.html:
  those <script> blocks are th:inline="javascript", so the fetch URL now uses
  the Thymeleaf expression /*[[@{/...}]]*/ which renders with the context.

## Files touched
- src/main/java/com/aquarius/config/SecurityConfig.java
- src/main/resources/templates/layout.html
- src/main/resources/templates/custom-reports/form.html
- src/main/resources/templates/custom-reports/detail.html
- src/main/resources/templates/help/chat.html

## Note / convention (add to future reviews)
In a WAR under a context, NEVER use absolute paths that bypass the context:
- Java servlet redirects: prepend request.getContextPath().
- JS fetch/XHR: use /*[[@{/path}]]*/ in th:inline scripts, or read the
  <meta name="_ctx"> value. Thymeleaf @{...} and Spring "redirect:/" are safe.

## Verification (sandbox)
- Java brace scan OK; node --check OK on the 3 inline-script templates.
- No remaining absolute fetch()/href without context in templates.
- git apply --check clean on a fresh clone of HEAD e67cd01.

## Not verified (confirm on deploy)
- Login now lands on /aquariusweb/dashboard; sidebar menu, custom-report
  validate/execute and help chat all call the /aquariusweb-prefixed URLs.
