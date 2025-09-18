# AGENTS: common package (`sk.iway.iwcm.common`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Utils](AGENTS-utils.md)</div>

Shared higher-level utilities sitting above low-level generic helpers: domain tools, file/path normalization, document helper logic invoked by multiple packages.

## What Belongs Here / Not Here

- Put here: helpers reused by multiple higher-level packages (system, editor, doc, components) that still contain mild domain awareness (paths, groups, domains) but are not pure primitives.
- Not here: low-level stateless generic routines (put in root utils), feature-specific logic (components), persistence infra (system), UI layer code.
- Migrate code upward/downward if usage scope broadens or narrows.

## Representative Classes

- `CloudToolsForCore`: Domain resolution (current domain name/id) and multi-domain related helpers.
- `DocTools`: Document-related convenience (classification, formatting, path adjustments).
- `FilePathTools`: Normalization & security filtering of file system paths.
- `WriteTagToolsForCore`: Determines inclusion of custom CSS/JS assets in admin layout.

## Core Patterns

### Domain Awareness

- Rely on `CloudToolsForCore` for any logic needing current domain; do not duplicate request parsing.

### Path Safety

- Always sanitize user-supplied paths via `FilePathTools` to avoid directory traversal or invalid mount references.

### UI Asset Overrides

- `WriteTagToolsForCore` provides custom admin asset resolution; new override points should extend it rather than reimplement logic inside JSPs.

## Conventions

- Keep methods small and stateless; avoid hidden caching unless explicitly documented.
- When adding new helpers ensure they are broadly reusable (package-agnostic) or place them in more specific package.

## Pitfalls

- Introducing request-dependent static state leads to cross-request leakage; keep request data passed explicitly.
- Bypassing path sanitation risks security issues; centralize in `FilePathTools`.

## Update Process

- Document newly added helper classes here with a one-line purpose summary.
