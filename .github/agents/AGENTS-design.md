# AGENTS: Design

## Design Context

### Users

WebJET CMS editors and administrators managing content, configuration, and structured data in a browser-based enterprise administration interface. Their primary goal is to complete frequent editing tasks quickly, reliably, and without requiring technical knowledge.

### Brand Personality

Professional, dependable, and efficient. The interface should inspire confidence and remain calm and predictable during complex administrative work.

### Aesthetic Direction

Preserve the established WebJET CMS design system: compact light-mode administration, clear hierarchy, restrained use of brand blue, consistent Bootstrap-based controls, and Tabler icons. Prefer surgical refinements over introducing new visual patterns.

### Design Principles

- Keep controls compact, aligned, and visually consistent with the surrounding administration UI.
- Make keyboard focus, state, and interaction outcomes unmistakable.
- Meet WCAG 2.2 accessibility requirements without changing familiar workflows.
- Reuse existing tokens, spacing, typography, buttons, and icons.
- Optimize for fast, dependable daily administration rather than decorative novelty.

## Design System Source of Truth

- The main administration stylesheet entry point is [`src/main/webapp/admin/v9/src/scss/ninja.scss`](../../src/main/webapp/admin/v9/src/scss/ninja.scss).
- The JavaScript entry point [`src/main/webapp/admin/v9/src/js/app.js`](../../src/main/webapp/admin/v9/src/js/app.js) imports `ninja.scss`; Rspack emits it as `dist/css/main.style.css`.
- Never edit compiled files in `dist/` or third-party/minified CSS. Override vendor styles in the appropriate project SCSS layer.
- `inline.scss` is a separately loaded bundle for the inline editor. `blind-friendly.scss` is a separate simplified presentation. Do not place general administration styles in either file.
- The UI foundation is Bootstrap 5, extended by DataTables Editor, Tabler Icons, Quill, Dropzone, Toastr, and selected jQuery UI components.

## SCSS Architecture

`ninja.scss` defines Bootstrap overrides before importing Bootstrap and vendor packages, then loads project layers in this order:

1. `2-helpers/` — shared variables, CSS custom properties, functions, mixins, placeholders, and animations.
2. `3-base/` — global foundations and shared overrides for typography, forms, tables, modals, alerts, dropdowns, trees, and Quill.
3. `4-layout/` — administration shell classes prefixed with `ly-`, including header, sidebar, content, and containers.
4. `5-modules/` — reusable feature modules prefixed with `md-`, including navigation, tabs, breadcrumbs, upload, notifications, and AI UI.
5. `6-pages/` — page-specific rules prefixed with `pg-`; use only when a rule is genuinely page-specific.
6. `8-views/` — conditional presentation modes such as print, iframe/editor, browser/device variants, and high contrast.
7. `_hotfix.scss` — loaded last; reserve it for temporary compatibility fixes, not durable component styling.

Place a change at the narrowest reusable level. Prefer an existing component or module selector over a page override, and scope vendor overrides to the affected WebJET component.

## Core Tokens

Tokens are defined at the top of `ninja.scss`. Shared runtime CSS properties are exported from `2-helpers/_variables.scss` as `--wj-*` variables.

| Purpose | Token | Value |
|---|---|---|
| Primary action | `$primary` / `--wj-primary` | `#0063FB` |
| Primary hover | `$primary-hover` / `--wj-primary-hover` | `#0054D5` |
| Main dark/UI foreground | `$secondary` / `--wj-secondary` | `#13151B` |
| Body text | `$body-color` | `#272727` |
| Input text | `$input-color` | `#23262E` |
| Secondary text | `$gray-text` / `--wj-gray-text` | `#686F83` (5.01:1 on white) |
| Light surface | `$nice-gray` / `--wj-nice-gray` | `#F3F3F6` |
| Borders/dividers | `$nice-gray-100`, `$nice-gray-200`, `$nice-gray-300` | `#DDDFE6`, `#B2B8C8`, `#868EA5` |
| Success | `$success` | `#00BE9F` |
| Warning | `$warning` | `#FABD00` |
| Danger semantic / button | `$danger`, `$danger-btn-bg` | `#E00028`, `#FF4B58` |
| AI accent | `$ai-color`, `$ai-color-dark`, `$ai-color-hover` | `#8A3EFF`, `#4D009F`, `#E3DBFF` |

- Use semantic tokens instead of introducing near-duplicate hard-coded colors.
- Prefer to use `var(--wj-*)` for runtime CSS properties instead of SCSS variables when possible.
- All standard border radii are `6px` (`$border-radius`, `$border-radius-sm`, and `$border-radius-lg`).
- Default grid gutter is `30px`.
- Input borders use `$nice-gray-300`; focused input borders use `#62697C`; disabled inputs use `$nice-gray`.
- Tooltips use `$secondary` as their background. Their arrows are intentionally transparent.

## Typography and Icons

- The administration font is **Asap**, with local font faces for weights 100, 400, and 700 and a `sans-serif` fallback.
- Body text is `16px`; compact controls commonly use the Bootstrap small size configured as `14px` with line-height `1.5`.
- Use the existing hierarchy and weights from the nearest component. Do not add a new font family for a feature.
- Use Tabler Icons through `ti ti-*`; filled icons use the configured `tabler-icons-filled` font.
- New icon-only controls require an accessible name. Decorative icons inside named controls must be hidden from assistive technology.

## Component Conventions

- Build controls from Bootstrap classes and existing WebJET variants before adding custom styles: `btn-primary`, `btn-outline-secondary`, `btn-success`, `btn-warning`, and `btn-danger`.
- Primary blue is reserved for the main action or selected state. Secondary actions normally use the outlined dark variant.
- Success, warning, and danger buttons intentionally use the dark `$secondary` foreground for readable contrast.
- DataTables and DataTables Editor are shared application infrastructure. Their stable project overrides belong primarily in `3-base/_table.scss` and `3-base/_modal.scss`.
- Forms and shared focus behavior belong in `3-base/_form.scss`. Keep labels, validation, required-state semantics, input groups, and buttons aligned with existing DataTables Editor forms.
- Use `ly-` for layout, `md-` for reusable modules, and `pg-` for page-only rules. Preserve existing BEM-like names within those namespaces.
- Keep compact table toolbars on the established 31px control rhythm. Use the existing spacing increments around toolbar buttons instead of inventing a separate toolbar layout.
- Standard modal widths are `$modal-lg: 900px` and `$modal-xl: 1210px`. DataTables Editor modal behavior is responsive and shared; avoid fixed page-specific modal dimensions.

## Interaction and Accessibility

- The project replaces Bootstrap's default button focus width, so never assume the vendor focus ring is present.
- Shared buttons use a `2px` `$secondary` `:focus-visible` outline, a `2px` offset, and a white outer ring. Preserve or improve this pattern; never remove keyboard focus without an equivalent visible replacement.
- Validate every interactive control in default, hover, active, `:focus-visible`, selected/pressed, and disabled states.
- Maintain at least 4.5:1 contrast for normal text and 3:1 for UI component boundaries, icons, and focus indicators.
- Do not communicate state by color alone. Expose toggle and selection state with native semantics or suitable ARIA attributes.
- Icon-only buttons need accessible names and must remain keyboard focusable. Tooltips must work on hover and keyboard focus and be dismissible with Escape.
- Modal and popover interactions must move focus into the component, keep keyboard interaction logical, and restore focus to the invoking control when closed.
- Prefer `:focus-visible` for keyboard indication. Do not make hover the only way to reveal information or actions.
- New motion must be purposeful and must respect `prefers-reduced-motion`.

## Layout and Responsive Behavior

- The desktop sidebar is `220px` wide and the fixed header is `48px` high.
- Shared breakpoints are `$bp-dinosaur: 576px`, `$bp-mobile: 768px`, `$bp-tablet: 992px`, `$bp-laptop: 1200px`, and `$bp-desktop: 1600px`.
- Below the laptop breakpoint the sidebar moves off-canvas and the header changes to a compact mobile/tablet arrangement.
- Reuse the breakpoint variables and existing responsive mixins. Do not add a nearby one-off media-query breakpoint unless the component has a demonstrated layout requirement.
- Test dense table and modal layouts at desktop, below 1200px, and below 768px when the change can affect wrapping or keyboard order.

## Implementation and Verification

1. Inspect the nearest existing component and its interaction states before writing CSS.
2. Reuse an existing SCSS or `--wj-*` token; introduce a new semantic token only when it will be reused.
3. Put the rule in the correct architecture layer and scope it to avoid unrelated Bootstrap or vendor components.
4. Build administration assets from `src/main/webapp/admin/v9/` with `npm run dev` for verification; use `npm run prod` for the production build when requested.
5. Run the relevant E2E scenario from `src/test/webapp/` and add a regression assertion for accessibility or layout behavior when practical.
