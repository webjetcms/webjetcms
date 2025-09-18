# AGENTS: Repository Knowledge Index

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Concise architecture & conventions index. Each section links to a deeper per-package AGENTS-* file (<=250 lines each). Keep this file short (target <=200 lines) and update when adding/removing packages or cross-cutting patterns.

Based on [agents.md](https://agents.md) and [Prompting Your Prompt's Prompt](https://brennanmceachran.com/blog/meta-prompt-for-agents-md)

## Monorepo & Tooling (If Using JS/TS Workspace)

High-level expectations when a JS/TS monorepo (e.g., Turborepo or pnpm workspaces) coexists with this Java codebase (frontend/admin apps, shared UI libs, API clients).

- Single package for design system / UI primitives; do NOT create ad‑hoc React/Next components inside app folders if they are reusable.
- Central type source-of-truth flows outward; never redefine domain types per app.
- Lint & type gates: ESLint (strict), TypeScript `noImplicitAny`, build breaks on drift.
- Prefer inference: avoid `as SomeType` casts; if a value is uncertain, parse/validate with Zod and refine types.
- Shared build scripts: root scripts drive type generation, codegen, and schema sync.

### Type Authority Flow (Preferred Pattern)

`Drizzle schema (table definitions)` -> generate static types -> extend with Zod (refinements: JSONB parsing, branded IDs) -> validators exported -> consumed by tRPC routers -> UI imports tRPC inference types (no manual re-declare) -> forms/components rely on inferred input/output types.

Rules:
1. Never duplicate an entity/interface manually inside UI or API layer.
2. If DB column changes -> Drizzle type diff -> cascades to Zod -> tRPC -> UI compile errors (desired early failure).
3. Use Zod `.transform` or `.pipe` for JSONB/text columns instead of casting.
4. Introduce brand types (e.g., `UserId & { __brand: 'UserId' }`) for opaque identifiers to avoid accidental mixups.

### Placement Decision Guide (JS/TS Assets)

| Put It Here | When | Avoid Putting In |
|-------------|------|------------------|
| `packages/ui` | Reusable visual components (buttons, modals, form controls) | Next.js app `components/` if reused elsewhere |
| `packages/validators` | Zod schemas derived from Drizzle + refinements | API route file directly |
| `packages/api` | tRPC router composition / API client | UI package or app layer |
| `apps/web` (Next.js) | Page-level layouts, route handlers assembling features | Shared logic/libs |
| `packages/config` | Shared config (ESLint, TS base, Tailwind, PostCSS) | Individual app overrides |
| `packages/icons` | Central SVG/icon components (tree-shakable) | Scattered inline SVGs in pages |

Heuristic: if another app could want it within 30 days, place in a package now.

### Coding Standards (TS)

- Strict TypeScript (`strict: true`, `exactOptionalPropertyTypes`, `noUncheckedIndexedAccess`).
- Zero `any` (except deliberate escape-hatch with comment `// intentional-any: reason`).
- Avoid non-null assertions (`!`) – model possibility or pre-validate.
- Prefer union narrowing & discriminated unions over enums when practical.
- Re-export public types from a single barrel (`index.ts`) per package; no deep internal imports from apps.
- Runtime validation only at boundaries (network, persistence, user input). Internal modules pass trusted types without re-parse.

### Enforcement Tooling Suggestions

- Add CI step: `tsc --noEmit`, `eslint .`, `turbo run build --dry=json` (detect orphan tasks).
- Script to diff Drizzle schema vs. generated types; fail if drift.
- Optional: generate a dependency graph (e.g., `madge`, `depcruise`) to forbid app -> app imports.
- Pre-commit hook: lint staged files + typecheck lightweight (tsc --project tsconfig.packages.json --noEmit).

If any of the above conflicts with existing repository patterns, align docs & code incrementally—do not create parallel standards.

## Package Map

| Area | Purpose | Doc |
|------|---------|-----|
| System | Infra: config, auditing, JPA helpers | `AGENTS-system.md` |
| DataTable | Generic CRUD/filter/notify pipeline | `AGENTS-datatable.md` |
| Editor | WYSIWYG / page services & approval | `AGENTS-editor.md` |
| Doc | Pages, groups, templates, history | `AGENTS-doc.md` |
| Users | Password policy, permissions, user DTOs | `AGENTS-users.md` |
| Components | Modular feature apps / widgets | `AGENTS-components.md` |
| Common | Mid-level shared helpers (domain/path) | `AGENTS-common.md` |
| Utils (root) | Low level generic helpers | `AGENTS-utils.md` |

## Core Architectural Layers

1. Utilities (root/common) supply generic + domain helpers.
2. System layer adds infrastructure (config, auditing, translation, domain scoping).
3. DataTable subsystem standardizes admin CRUD + validation.
4. Editor & Doc layers implement content life cycle, approval, history, tree navigation.
5. Components provide optional feature modules built atop preceding layers.

## Cross-Cutting Conventions

### Configuration

- All runtime config accessed via `Constants.get*`; refresh triggers `Tools.reinitialize()` for date/time formats.
- Use distinct key prefixes (`componentName.*`) to avoid collisions.

### Domain & Multi-Tenancy

- Derive current domain with `CloudToolsForCore.getDomainName()`.
- For domain-scoped persistence implement `DomainIdRepository` and rely on System layer predicate injection.

### Error Handling Pattern

- Business/User errors: domain-specific exception (e.g., `EditorException`) -> structured response.
- System errors: log (`Logger.error`) + neutral/generic message to client.
- Prefer a consistent response DTO (e.g., DataTableError / ApiResponse) with `success|error` fields.

### Validation

- Bean Validation first; enrich errors via custom checks then merge into binding result / response.

### History & Audit

- Doc changes recorded via `DocPublishService` & `DocHistory`; auditing for entities through `AuditEntityListener`.
- When adding mutable entity fields, ensure both history and audit coverage.

### Caching Strategy

- *DB singletons (`GroupsDB`, `TemplatesDB`) cache structured data; always call explicit clear/refresh methods after updates.
- Avoid ad-hoc static caches in components; centralize or document invalidation.

### DataTable Life Cycle Hooks

- Override only documented protected hook methods; maintain `REGEX_PREFIX` alignment with frontend search.

### ThreadLocal Usage

- Limited to DataTable thread state (`ThreadBean`) - do not introduce new ThreadLocals without documenting cleanup.

### Security & Permissions

- Permission groups drive UI hiding (noperms CSS). Add new permission -> update group beans + related CSS mapping.
- Password hashing evolution managed by `PasswordSecurityAlgorithm` (rehash on login pattern).

### JSON & HTML Safety

- Use `JsonTools.prepare4Json` before embedding rich HTML in JSON.
- Strip or normalize external links/images to controlled thumbnail paths for previews.

### Internationalization (i18n)

- Fetch localized texts via `Prop.getInstance(request)`; key prefix per module/component.
- Expose active language to frontend through `LayoutService` when necessary.

### AI / Streaming Integration

- Standard structured AI result object pattern: `{ success, error, content, totalTokens, finishReason }`.
- Streaming: accumulate decoded chunks, update UI incrementally, close resources promptly.

## Extension Playbook (High-Level)

1. New Component: scaffold folder, service + repo, DataTable integration, config keys, i18n, tests.
2. New Data Entity in Admin: subclass `DatatableRestControllerV2`, annotate columns, implement hooks, add frontend config.
3. New Doc Field: entity + converter + editor field + history inclusion + audit coverage.
4. New Permission: extend `PermissionGroupBean`, update UI permission mapping + noperms CSS generation.
5. New Utility: verify it doesn’t belong in existing helper; document in utils or common AGENTS file.

## Pitfalls Overview

- Skipping domain scoping -> cross-tenant leakage.
- Forgetting history/audit updates on doc changes.
- Regex prefix drift between frontend and backend.
- Silent cache staleness (no explicit invalidation call).
- Direct template/business logic inside JSP rather than services.
- Static method sprawl in utilities hurting testability.

## Update Process

- When adding/removing a package-level AGENTS file: update Package Map table and this layer list.
- Keep per-package docs <=250 lines; trim older sections if exceeding.
- Record significant architectural shifts (e.g., new caching layer, AI provider swap) succinctly here with date.

---
Generated/maintained to accelerate onboarding & AI-assisted refactors. Keep actionable; avoid duplicating exhaustive API details.
