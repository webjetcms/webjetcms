# AGENTS: top-level utilities (root `sk.iway.iwcm`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md)</div>

Low-level, wide-use utility classes available across all packages.

## What Belongs Here / Not Here

- Put here: pure, side-effect-light, broadly reusable helpers (formatting, encoding, parsing) devoid of feature context.
- Not here: domain/path/group logic (common), configuration/persistence infrastructure (system), DataTable life cycle behavior, feature-specific transformations (components/editor/doc).
- If utility begins to accumulate domain branching, relocate to common.

## Key Classes

- `Tools`: Broad utility (dates, formatting, HTTP, string replace, network, SSL trust overrides). Mutable date formats re-init via `Tools.reinitialize()` after config refresh.
- `JsonTools`: JSON flattening, escaping, safe HTML preparation before embedding into JSON payloads.
- `PageParams`: Parsing of URL or request parameters into structured form.
- `Identity`: Represents logged-in user identity (used by layout/services for permission checks).

## Core Patterns

### Configuration-Coupled Utilities

- `Tools` reads date/time formats & other settings from `Constants`; reinitialize after constants reload to keep patterns synchronized.

### HTML / JSON Safety

- `JsonTools.prepare4Json` strips includes, normalizes image/link tags adding thumbnail attributes for admin previews.

### Collation / Localization

- `Tools.slovakCollator` for locale-aware sorting (case/diacritic sensitivity tuned); prefer when ordering user-facing Slovak strings.

### Networking

- Fluent Apache HttpClient usage appears (Request fluent API); ensure response entities fully consumed/closed to avoid connection leaks.

## Conventions

- Avoid adding domain/business logic; keep purely technical helpers.
- Favor composition over adding yet another static method when functionality already exists elsewhere.

## Pitfalls

- Overuse of static methods can complicate testing; wrap in adapter if mocking needed.
- Forgetting `Tools.reinitialize()` after changing date/time constants yields stale formatting.

## Extending

1. Ensure new utility does not belong in a narrower package.
2. Provide JavaDoc with clear side-effect notes and thread-safety expectations.
3. If stateful (rare), document initialization order.

## Testing Checklist

- Date/time formatting matches configured patterns after reinitialize.
- JSON preparation keeps expected tags while stripping unsafe segments.

## Update Process

- Log any removal/renaming of widely-used methods here to guide refactor efforts.
