# AGENTS: doc package (`sk.iway.iwcm.doc`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Manages hierarchical website content (documents/pages) and groups (folders), their templates, publishing life cycle, history, and navigation trees.

## What Belongs Here / Not Here

- Put here: document/group/template domain logic, history & publishing orchestration, tree/navigation builders, related caching (*DB classes).
- Not here: editor WYSIWYG specifics (editor), generic utilities (utils/common), feature widget rendering (components).
- Extract generic tree utilities if needed elsewhere.

## Core Domain Objects

- `DocDetails` / `DocBasic`: Rich vs light representation of a page/document.
- `GroupDetails`: Folder/group metadata (domain, path, permissions).
- `TemplateDetails` / `TemplatesDB`: Presentation layer templates & lookup cache.
- `DocHistory` + `HistoryDB`: Version snapshots for rollback & audit.
- `DocPublishService`: Orchestrates publish state transitions (draft -> approved -> live) including history entry creation.
- `GroupsDB`: In-memory/cache accessor for group structure & domain-specific filtering.

## Supporting Classes

- Converters (e.g., `DocDetailsConverter`, `GroupDetailsConverter`): JPA / DTO conversions and null-protection (`DocDetailsNotNullConverter`).
- Editor field models (`DocEditorFields`, `GroupEditorField`, `TemplateDetailEditorFields`): Bridge raw entity to DataTable/editor UI fields.
- Tree / UI integration (`GroupsTreeService`, `GroupsTreeRestController`, `DocumentsJsTreeItem`, `GroupsJsTreeItem`): Build hierarchical JSON for jsTree/navigation.
- `NavbarService`: Builds breadcrumb / navigation structures for current context.
- Listeners (`ShowDocListener`): Hook points when rendering documents.

## Key Patterns

### Caching & Lookup

- Centralized through *DB classes (`GroupsDB`, `TemplatesDB`) offering singleton-style access; ensure invalidation when underlying data changes (call provided clear/reload methods rather than re-instantiating).

### Publishing Life Cycle

- `DocPublishService` handles state changes; avoid manual field toggling to keep history consistent.
- History entry: create before mutating persistent state to allow rollback.

### Group & Domain Resolution

- Use `GroupsDB.getInstance().getUserRootDomainNames(user)` for domain-scoped group lists.
- Always supply/derive domain when generating public URLs to avoid cross-domain leakage.

### Tree Building

- Services assemble lightweight tree node DTOs (*JsTreeItem) with id/text/children flags; lazy-load deeper levels on demand.
- Keep recursion guarded (max depth or cycle detection if mirroring/clone features used).

### Template Binding

- `TemplatesDB` caches templates keyed by id/path; flush when uploading or editing template files to allow immediate reuse.

## Conventions

- Distinguish *Details (full object) vs lighter projections (*Basic) for performance-sensitive list views.
- Methods prefixed get/prepare/build for retrieval/assembly; avoid side effects in simple getters.
- Keep converters side-effect free; log & return safe defaults instead of throwing on partial data.

## Pitfalls

- Forgetting to update history before publish -> gaps in version chain.
- Directly altering cached `GroupsDB` internal collections breaks invariants; use exposed mutation/invalidation methods.
- Unsynchronized template cache invalidation can cause stale design in user sessions; ensure single point of flush.

## Extending

1. New metadata field: add to entity + converter + editor field model.
2. Add persistence & include in history snapshot logic (ensure rollback restores it).
3. Reflect in tree DTOs only if needed by navigation UI.

## Testing Checklist

- Create/edit/publish sequence persists history entries.
- Tree JSON generation excludes unauthorized groups for limited user.
- Template cache flush reflects new template on next render.

## Update Process

- When adding new publish states or history attributes, document minimal migration & rollback strategy here.
