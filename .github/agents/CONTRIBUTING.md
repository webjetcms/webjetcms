# Contributing to WebJET CMS

> Fast reference for setting up a working dev environment, writing code that matches project standards, and opening high‑signal pull requests.

## 1. Overview

WebJET CMS combines a Java/Spring backend with optional JS/TS workspaces (design system, admin UI, API clients). Architecture & package conventions live in `AGENTS.md` and related `AGENTS-*.md` files—read those first for domain boundaries and placement rules.

## 2. Quick Start

1. Clone & checkout a feature branch: `git checkout -b feature/<issue-id>-short-slug`.
2. Start app with local DB task (example): VS Code task `appStartLocalDB` or `./gradlew appStartDebug` with the desired `webjetDbname` property.
3. Run tests before coding (baseline): `./gradlew test`.
4. Make focused changes with docs & tests updated.
5. Ensure build + tests pass, open PR targeting `main` (or designated integration branch).

## 3. Branch & PR Flow

| Step | Purpose | Notes |
|------|---------|-------|
| Branch naming | Traceability | `feature/12345-add-x`, `fix/12345-bug-desc` |
| Draft PR early | Feedback | Link issue; CI proves direction |
| Rebase (not merge) | Linear history | `git fetch origin && git rebase origin/main` |
| Squash policy | Keep logical commits | Prefer a few narrative commits (not one giant) |
| PR description | Reviewer context | What, why, how validated, migration steps |

## 4. Commit Messages

Format:

```text
<type>(scope): short summary

Body explains motivation, behavior change, migration risk, validation.
Refs: #issue-id
```

Types: feat, fix, refactor, docs, chore, perf, test, build.
Scope: optional (e.g., datatable, editor, users, ui, infra).

## 5. Type & Schema Flow (JS/TS)

Authoritative pipeline (never bypass):

```text
Drizzle table defs -> generated static types -> Zod refinements (JSONB, branded IDs) -> export validators -> tRPC routers use inferred input/output -> UI consumes tRPC inference -> forms/components rely on inferred types only.
```

Rules:

1. Do not redefine interfaces manually downstream.
2. Schema change must trigger type errors up the stack (desired early failure).
3. No broad `any`; if uncertain, create a narrow union or parse with Zod.
4. Avoid `as` casting—model it, refine it, or validate it.
5. Brand opaque IDs (`UserId & { __brand: 'UserId' }`).

## 6. Placement Guide (JS/TS)

| Put It Here | When | Avoid Putting In |
|-------------|------|------------------|
| `packages/ui` | Reusable visual primitives | Next.js app `/components` if reused |
| `packages/validators` | Zod schemas + refinements | API route files directly |
| `packages/api` | tRPC router composition/client | UI layer |
| `packages/icons` | Shared SVG set | Scattered inline SVG |
| `apps/web` | Pages, layouts, feature assembly | Shared logic |
| `packages/config` | Shared lint/build config | Individual app duplicates |

Heuristic: If another app might need it inside 30 days, put it in a package now.

## 7. Coding Standards (TypeScript)

- `strict` + `exactOptionalPropertyTypes` + `noUncheckedIndexedAccess` enabled.
- Zero `any` (except `// intentional-any: reason`).
- Prefer inference & discriminated unions over enums.
- No non-null (`!`) unless pre-validated—prefer safe narrowing.
- Re-export public types via a package barrel (`index.ts`).
- Validate only at boundaries; internal flow passes trusted shapes.

## 8. Coding Standards (Java)

- Keep methods short, single-responsibility; extract when >40–50 LOC doing multiple things.
- Prefer constructor injection over field injection.
- Use domain-specific exceptions for user-facing errors; generic ones logged & wrapped.
- Null handling: prefer Optional or early returns; maintain `Comparator.nullsFirst` when ordering nullable fields.
- Avoid introducing new static caches without invalidation strategy.

## 9. Validation Strategy

Layered:

1. Bean Validation (annotations) – first line.
2. Programmatic checks – enrich BindingResult.
3. JSON/Zod (JS boundary) – parse external input only once.
4. Internal code trusts validated types (no duplicate parsing).

## 10. Security Checklist

- Domain scoping: every domain-specific query ensures correct domain predicate.
- Input sanitation: all embedded HTML through `JsonTools.prepare4Json`.
- Password hashing: only via `PasswordSecurity`.
- Permissions: add mapping + CSS (noperms) updates when introducing new permission.
- Avoid leaking internal stack traces—map to uniform error DTO.
- No unchecked ThreadLocal introduction.

## 11. Performance Checklist

- Avoid N+1 queries (prefer fetch joins / batch).
- Cache only when invalidation path is explicit.
- Stream large responses (AI/content generation) and promptly close resources.
- Prefer pagination + filtering server-side for DataTables.
- Profile before micro-optimizing; document bottleneck & post-change metrics.

## 12. Adding a DB Field / Entity

1. Modify JPA entity (and Liquibase/migration if used).
2. Update converters/DTOs + history/audit inclusion if mutable.
3. Add editor/DataTable annotations for UI editing.
4. Invalidate related caches (GroupsDB, TemplatesDB, etc.).
5. Add tests covering create/edit/history.

## 13. Adding a Component (Feature Module)

Follow `AGENTS-components.md` Extension Steps. Ensure:

- Unique config key prefix.
- i18n keys added.
- Service + repository or migration off legacy SimpleQuery.
- Tests: permissions, domain isolation, cache invalidation.

## 14. Documentation Expectations

- Update relevant `AGENTS-*.md` when adding infra, domain objects, caches, or hooks.
- Keep changes concise (<=5 line bullet) per addition.
- Include migration notes for breaking changes (schema/permission/template path).

## 15. CI & Tooling (Suggested)

- Java: `./gradlew test` + static analysis (SpotBugs/Checkstyle if configured).
- TS: `tsc --noEmit`, `eslint .`, `turbo run build --dry=json` for task drift.
- Schema drift script: diff Drizzle sources vs generated types (fail on mismatch).
- Dependency graph (madge/depcruise) to forbid cross-app imports.
- Pre-commit hook: lint + typecheck changed TS, run focused Java tests.

## 16. PR Checklist

Before marking Ready for Review:

- [ ] Build & tests green locally.
- [ ] Added / updated tests (happy path + at least one edge case).
- [ ] Updated `AGENTS-*.md` docs (if new infra/domain/cache/permission).
- [ ] No stray `TODO` or commented-out blocks left.
- [ ] No unchecked casts / `any` without justification.
- [ ] Domain scoping verified (where applicable).
- [ ] Security-sensitive areas reviewed (auth, file upload, HTML handling).
- [ ] Changelog / release notes prepared if user-visible change.

## 17. Communication

Prefer small, incremental PRs over “big bang” drops. Provide context (problem, approach alternatives considered, trade-offs). Ask for a focused review (e.g., “API shape” or “concurrency logic”).

## 18. Troubleshooting

| Issue | First Checks |
|-------|--------------|
| Stale cache data | Was explicit clear/invalidate called? |
| Charset issues in streaming | Confirm correct charset detection & decoder reuse |
| Cross-domain data leaking | Repository implements `DomainIdRepository`? Predicates applied? |
| Type mismatch in UI | Did Drizzle type change propagate through validators? Re-run type generation |

## 19. Style Non-Goals

We do not enforce lexical lint on domain terms (e.g., JSONB, tRPC) beyond clarity. Focus on correctness, safety, and maintainability over micro style bikeshedding.

## 20. Questions

Open a draft PR or start a discussion thread referencing the affected `AGENTS-*.md` section. Keep domain-specific clarifications documented once resolved.

---
Happy shipping! Keep the feedback loop fast and the type flow unbroken.
