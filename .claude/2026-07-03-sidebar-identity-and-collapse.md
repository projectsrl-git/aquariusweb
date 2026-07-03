# 2026-07-03 — Sidebar: identity block on top + collapsible menu

## Goal (two UI changes requested)
1. Move the tenant + user + fiscal-year block from the footer to the top,
   between the Aquarius brand and the menu.
2. Allow collapsing the sidebar to an icons-only rail.

## Approach
Both changes are confined to layout.html (markup + CSS + a small JS block);
no server-side or behavioural change.

### 1. Identity block on top
- New `.sidebar-identity` block right after `.sidebar-brand`, containing
  tenant (bold), user, and the fiscal-year badge (unchanged link to
  /select-year). The footer now holds only the Esci button and the app
  version. Removed the now-unused `.sidebar-foot .tenant/.user` CSS.

### 2. Collapsible sidebar
- A toggle button (#sidebarToggle) in the brand row. Clicking toggles the
  `collapsed` class on #sidebar (and `sidebar-collapsed` on body as a
  fallback selector). Width goes 260px -> 64px (new --sidebar-w-collapsed),
  main.content margin follows; both animated (transition).
- Collapsed mode hides every textual label (.menu-label, .ident-label,
  .brand-text, static section headers, chevrons), centers icons, rotates the
  toggle chevron, and hides nested submenus (they don't make sense at 64px).
  Every menu item and tool link got a th:title so the icon shows a tooltip
  when collapsed.
- State persists in sessionStorage (key aqSidebarCollapsed) and is restored
  on load. This is a real Tomcat app (not a sandboxed artifact), so
  sessionStorage is fine here; wrapped in try/catch to degrade gracefully.

## Files touched
- src/main/resources/templates/layout.html

## Verification (sandbox)
- node --check on the layout script block: OK.
- aside/main are direct siblings, so both the adjacent-sibling and the
  body-class margin selectors apply.
- git apply --check clean on a fresh clone of HEAD cadec67.

## Not verified (confirm on deploy)
- Visual result of expand/collapse and that the persisted state restores
  across page loads in the browser.
