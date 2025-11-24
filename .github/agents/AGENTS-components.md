# AGENTS: components package (`sk.iway.iwcm.components`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Collection of pluggable feature modules (apps/widgets) each encapsulated in its own subdirectory (e.g., banner, blog, forms, gallery, news, seo, sitemap, stat). They integrate into templates via tags/macros or dynamic includes and often expose admin DataTable CRUD endpoints.

## Structure & Conventions

- One folder per component: keep internal naming consistent with folder (e.g., `banner/` -> classes prefixed `Banner`).
- Each component may define: DB access (repository or legacy SimpleQuery), DTO/editor field classes, service logic, JSP fragments, localization keys, optional scheduled tasks.
- Configuration keys live in `Constants`; prefer a `componentName.` prefix to avoid clashes.
- Reusable UI or logic should be extracted upward (system/common) rather than duplicated across components.

## What Belongs Here / Not Here

- Put here: feature-specific services, repositories, DTOs, JSP/tag fragments, localized strings, scheduled jobs tied to the feature domain.
- Not here: generic reusable UI (move to shared UI lib in JS/TS workspace), cross-cutting helpers (common/utils), infra concerns (system), base CRUD patterns (datatable unless behavior is unique), duplicated image/file manipulation (prefer central utility).
- Promote code to higher-level package if you plausibly need it in another component within 30 days.

## Initialization & Configuration

- Some components register themselves via module metadata (ModuleInfo / Modules); ensure new component updates module registry if it needs enable/disable toggling.
- Feature flags: guard optional logic with appropriate `Constants.getBoolean` checks.

## Data Access Patterns

- Legacy components may still use `SimpleQuery`; new code should favor Spring Data repositories where feasible.
- Cache frequently read small lookups in static maps with explicit clear methods (avoid silent memory growth).

## Template Integration

- Provide well-named JSP fragments or tag libraries; avoid embedding business logic directly in JSP - delegate to a service.
- Use standardized thumbnail / media handling via shared utilities so caching & resizing remain consistent.

## i18n

- All user-visible strings go through `Prop.getInstance(request)` with `componentName.*` keys.
- Add new keys to appropriate language bundles; keep naming hierarchical (e.g., `gallery.upload.error.*`).

## Error Handling

- Throw domain-specific exceptions only if meaningful to callers; otherwise log and return neutral fallback (empty list, default DTO) to avoid front-end crashes.

## Extension Steps (Adding a New Component)

1. Create folder under `components/` with clear name.
2. Add service class(es) + repository (or migrate legacy queries) for core logic.
3. Provide editor/DataTable integration if CRUD needed.
4. Expose JSP/tag entry points; document usage signature in component README (optional).
5. Register module if togglable; add config keys to `Constants` (with defaults) and document.
6. Add i18n keys and minimal tests (service logic, edge cases).

## Pitfalls

- Duplicating similar logic (e.g., image resizing) instead of reusing central utilities causes inconsistent caching.
- Referencing other component internals directly couples modules; use public service facades if cross-component interaction required.
- Missing invalidation on cached structures leads to stale admin displays.

## Testing Checklist

- CRUD endpoints enforce permissions & domain constraints.
- Templates render with component disabled (feature flag off) gracefully.
- i18n fallback works for missing translations.

## Update Process

- When deprecating a component, mark in module registry and add migration note here.
