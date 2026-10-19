# AGENTS: Administration Design

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

## Design Intent

WebJET CMS editors and administrators need to complete frequent content and data-management tasks quickly and reliably. Preserve the established compact, light administration interface: clear hierarchy, restrained brand blue, consistent controls and Tabler icons. The intended tone is professional, dependable and calm.

- Reuse the nearest existing component, tokens, spacing and interaction patterns.
- Keep controls aligned and outcomes clear, including errors, loading and disabled states.
- Make keyboard focus visible and maintain logical keyboard navigation.
- Treat WCAG 2.2 accessibility as a design requirement to verify, not a claim that every existing screen already conforms.
- Improve the requested workflow without introducing a separate visual system.

## Actual Frontend Stack and Sources

The administration uses JavaScript, jQuery, Bootstrap 5, DataTables 2 with Editor, jsTree, Tabler Icons, Quill, Dropzone, Toastr and selected jQuery UI widgets. Rspack builds the administration assets; Pug produces administration templates. CKEditor integration and custom Web Components also exist. Check the relevant package and source rather than assuming a React/Next.js or TypeScript application.

All paths below are relative to the core repository root; customer projects may consume these assets from their WebJET dependency.

| Source | Use |
| --- | --- |
| `src/main/webapp/admin/v9/package.json` | Build scripts and direct frontend dependencies |
| `src/main/webapp/admin/v9/package-lock.json` | Resolved frontend dependency versions |
| `src/main/webapp/admin/v9/npm_packages/webjetdatatables/package.json` | Bundled DataTables/Editor wrapper dependencies, including Quill |
| `src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js` | Shared `WJ.DataTable` implementation |
| `src/main/webapp/admin/v9/src/js/app.js` | Main imports, shared browser globals and lazy-loaded features |
| `src/main/webapp/admin/v9/src/js/webjet.js` | Shared `WJ` browser helpers |
| `src/main/webapp/admin/v9/src/js/web-components/` | Existing custom Web Components |
| `src/main/webapp/admin/v9/src/scss/ninja.scss` | Main SCSS entry point and Bootstrap/token overrides |
| `src/main/webapp/admin/v9/views/` | Pug layout, partials and page sources |
| `src/main/webapp/admin/v9/rspack.config.common.js` | Bundles, loaders and output configuration |
| `src/main/webapp/admin/v9/pug.render.js` | Pug rendering shared by development/production builds |
| `src/main/webapp/apps/` | Application views, including Thymeleaf administration HTML |

Edit source files, then rebuild the owning bundle. Do not hand-edit compiled `src/main/webapp/admin/v9/dist/` output or third-party/minified vendor files. `app.js` imports `ninja.scss`, emitted as `dist/css/main.style.css`; inline editing loads `inline.scss` separately through a dynamic import. Place general administration styles in the main SCSS layers.

## SCSS Structure

Paths in this section are under `src/main/webapp/admin/v9/src/scss/`. `ninja.scss` sets Bootstrap variables, imports vendor styles and then imports project layers:

| Layer | Purpose |
| --- | --- |
| `2-helpers/` | Variables, CSS custom properties, functions, mixins and animation helpers |
| `3-base/` | Shared typography, forms, tables, modals, trees, Quill and other base components |
| `4-layout/` | Administration shell, using `ly-` selectors |
| `5-modules/` | Reusable feature UI, commonly using `md-` selectors |
| `6-pages/` | Page-specific rules, using `pg-` selectors |
| `8-views/` | Print, browser/device, high-contrast and editor presentation modes |
| `_hotfix.scss` | Final overrides; reserve for temporary compatibility fixes |

Use the narrowest reusable layer and preserve the surrounding selector conventions. Shared DataTables overrides belong mainly in `3-base/_table.scss` and `3-base/_modal.scss`; forms and button focus behavior belong in `3-base/_form.scss`.

## Tokens, Typography and Layout

The primary token definitions are in `ninja.scss`; `2-helpers/_variables.scss` exports runtime `--wj-*` properties. Prefer semantic tokens and existing CSS properties over near-duplicate hard-coded values.

| Purpose | Existing token/value |
| --- | --- |
| Primary action / hover | `$primary: #0063FB`, `$primary-hover: #0054D5` |
| Main foreground | `$secondary: #13151B` |
| Secondary text | `$gray-text: #686F83` |
| Light surface | `$nice-gray: #F3F3F6` |
| Border scale | `$nice-gray-100: #DDDFE6`, `$nice-gray-200: #B2B8C8`, `$nice-gray-300: #868EA5` |
| Success / warning | `$success: #00BE9F`, `$warning: #FABD00` |
| Danger semantic / button background | `$danger: #E00028`, `$danger-btn-bg: #FF4B58` |
| AI accent | `$ai-color: #8A3EFF`, `$ai-color-dark: #4D009F`, `$ai-color-hover: #E3DBFF` |

- The font is local **Asap**, weights 400 and 700, with a sans-serif fallback. Body text is 16px; small controls use 14px with line-height 1.5. Definitions are in `3-base/_font-face.scss`, `3-base/_body.scss` and `ninja.scss`.
- Use Tabler `ti ti-*` icons; the filled font is configured as `tabler-icons-filled`. Give icon-only controls accessible names and hide decorative icons from assistive technology.
- Shared border radii are 6px; the default grid gutter is 30px. Preserve the established compact control sizing.
- Desktop sidebar width is 220px and header height is 48px. Below 1200px the sidebar moves off-canvas and the header changes arrangement; inspect `4-layout/` before altering shell dimensions.
- Breakpoints in `2-helpers/_variables.scss` are 576, 768, 992, 1200 and 1600px (`$bp-dinosaur` through `$bp-desktop`). Reuse these variables and responsive mixins.
- Shared modal sizes are `$modal-lg: 900px` and `$modal-xl: 1210px`; preserve the responsive DataTables Editor modal behavior.

## Controls and Accessibility

- Reuse Bootstrap/WebJET variants such as `btn-primary`, `btn-outline-secondary`, `btn-success`, `btn-warning` and `btn-danger`. Primary blue marks the main action or selected state; secondary actions normally use the outlined variant.
- The shared success/warning/danger buttons use dark foreground text. `btn-red` provides a solid red action with white text; `btn-white` provides an opaque white action. Both define hover, active and disabled colors through Bootstrap's `button-variant` mixin. Replace the variant class instead of combining conflicting variants.
- Bootstrap's default button focus width is disabled. The shared `:focus-visible` rules provide a 2px dark outline, 2px offset and white outer ring for buttons and close controls; preserve an equivalent visible indicator.
- Check default, hover, active, focused, selected and disabled states. Do not communicate state only through color; use native semantics or appropriate ARIA state.
- Check text and control contrast in the actual rendered context. A token alone does not guarantee accessible contrast on every surface.
- Modal dialogs need deliberate focus entry, keyboard containment where appropriate and return to the invoking control. Popovers and tooltips need keyboard access and dismissal appropriate to the component.
- Keep labels, required state and validation feedback consistent with DataTables Editor forms. New motion must respect `prefers-reduced-motion`.

## Verification

Run commands from `src/main/webapp/admin/v9/`: `npm run dev` builds development assets; `npm run prod` builds production assets; `npm run watch` runs the development watcher with reload support (`watch:plain` omits that wrapper).

Inspect the affected page in the running CMS, including keyboard use and responsive layouts. For table/modal changes, check desktop, below 1200px and below 768px where wrapping or focus order can change. Existing browser tests live under `src/test/webapp/`; follow [Working guide](CONTRIBUTING.md) and the applicable test skill for the target scenario.
