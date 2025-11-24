# AGENTS: system package (`sk.iway.iwcm.system`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Infrastructure layer: configuration (ConstantsV9), module registry, auditing, JPA helpers, translation, Spring wiring helpers. Provides conventions reused across admin/editor/components.

## What Belongs Here / Not Here

- Put here: cross-cutting infrastructure (auditing listeners, configuration loaders, Spring wiring helpers, translation base), generic JPA helpers.
- Not here: feature/business rules (components/editor/doc), low-level generic utilities (utils), mid-level domain helpers (common).
- Promote infra concern here if reused by multiple feature packages and has no feature semantics.

## Key Subpackages

- annotations/: Custom annotations (e.g., column editors) consumed by factories.
- audit/: Entity listeners (e.g., AuditEntityListener) for admin log/trace.
- jpa/: Helpers like `JpaTools`, converters, `NullAwareBeanUtils`.
- spring/: Spring-centric utilities (bean lookup, context helpers).
- translation/: I18n and dynamic translation tooling.

DataTable-specific material moved to `AGENTS-datatable.md`.

## Core Patterns

### Configuration Access

- Use `Constants.get*(key)` instead of static finals; values can be reloaded.
- Re-run `Tools.reinitialize()` after constants reload to refresh date/time formats.

### Domain Scoping

- Domain-aware repositories implement `DomainIdRepository`.
- Controllers gate domain predicates with `(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir"))`.

### Validation

- Standard Validator injected; merge constraint violations manually as needed via `BeanPropertyBindingResult`.

### Reflection & Dynamic Access

- `BeanWrapperImpl` for property access.
- `NullAwareBeanUtils.copyProperties` merges non-null edits into originals.

### Auditing

- `AuditEntityListener` attaches life cycle hooks; register on entities requiring audit trail.

### JSON / Serialization

- Custom Jackson deserializers convert full path strings to domain objects.

### Error Handling

- Use domain-specific exceptions for user messages; log others and map to uniform response objects.

## Conventions

- Prefer `Comparator.nullsFirst` when ordering potentially nullable domain/group fields.
- Keep AGENTS docs concise (<=250 lines each) and actionable.

## Pitfalls

- Omitting `DomainIdRepository` can allow cross-domain edits.
- Leaking ThreadLocal state if custom threading introduced (avoid manual threads in request scope).

## Related Global Classes

- `Constants` / `ConstantsV9`: configuration sources.
- `Tools`: ubiquitous utilities (string/date/network, etc.).
- `JsonTools`: JSON flattening and safe escaping.

## Update Process

- When adding a new infrastructure extension point, append a short bullet here (<=5 lines) describing purpose and usage.
