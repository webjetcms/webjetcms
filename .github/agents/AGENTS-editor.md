# AGENTS: editor package (`sk.iway.iwcm.editor`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

## Scope

- Backend services powering the WYSIWYG/editor admin: content approval, link/media checks, webpage CRUD, multi-group operations, notifications, appstore integration.

## What Belongs Here / Not Here

- Put here: services and utilities strictly tied to rich-text page editing, approval workflow, link/media validation, editor-facing REST endpoints.
- Not here: generic CRUD scaffolding (datatable), low-level path/domain helpers (common/utils), unrelated feature widgets (components).
- If logic becomes page-neutral (usable by components) consider extraction to common.

## Key Services (editor/service)

- EditorService: Central operations on page entities (create/update/publish) coordinating approval & locking.
- ApproveService: Approval workflow (status transitions, permissions checks, notification triggers).
- WebpagesService: Higher-level facade around Groups/Docs for listing & filtering pages.
- MediaService: File/media management (thumbnails, storage decisions) respecting domain & security.
- GroupsService / MultigroupService: Hierarchical group structure logic & multi-assignment handling.
- LinkCheckService: Validation of internal/external links within content.
- NotifyService: Aggregates user-facing notifications (ties into ThreadBean notify in DataTables when invoked via admin UI).

## Supporting Layers

- facade/: Simplified entry points for UI layer (thin wrappers around service complexity).
- rest/: REST controllers exposing JSON endpoints (naming: *RestController, returns DataTableResponse or ApiResponse pattern for uniform error handling).
- util/: Helper utilities (sanitization, diffing, content transforms) specific to editor domain.

## Core Patterns

1. Permission & Identity
   - UsersDB.getCurrentUser(request) is primary identity fetch; enforce admin/editor role checks early in service methods.
2. Locking & Concurrency
   - Editor locking integrates with system.datatable.editorlocking; always acquire lock before mutating content; release in finally block.
3. Approval Workflow
   - State transitions centralized in ApproveService; callers should not directly toggle status fields to keep audit consistent.
4. Notifications
   - Use NotifyService to queue messages; in DataTable context these surface via ThreadBean -> DatatableResponse.notify.
5. Multi-Domain
   - Always resolve current domain via CloudToolsForCore.getDomainName() for content-scoped operations (media paths, URL generation).
6. i18n
   - Prop.getInstance(request) for localized strings; expose language codes to frontend via LayoutService when needed.
7. Error Handling
   - Throw EditorException for user-displayable issues; allow generic exceptions to be logged then mapped to DataTableError.
8. JSON / Serialization
   - For complex rich-text operations rely on JsonTools.prepare4Json before embedding HTML in JSON payloads.
9. Streaming / AI Integration
   - When integrating AI-generated content, use structured result object (AiExecutionResult) with success/error/metrics to keep editor UI consistent.

## Extension Points

- Add new services in editor/service with @Service annotation; expose via rest/ with clear URL prefix (/admin/rest/editor/*).
- Introduce utilities under util/ for reusable transformations; keep pure/static where side-effects unnecessary.
- To augment approval, subclass or wrap ApproveService; avoid scattering approval logic across unrelated services.

## Conventions

- Method names reflect action + domain (publishPage, approveRevision, checkLinks).
- Keep service class responsibilities focused; if class exceeds ~500 LOC consider extracting helper.
- Prefer constructor injection (@Autowired optional) to allow unit testing with mocks.

## Pitfalls

- Skipping lock acquisition can cause silent overwrite in concurrent edits.
- Direct entity field mutation bypassing ApproveService leads to inconsistent audit trail.
- Forgetting domain context in media operations may leak files across tenants.

## How To Add A New Editor Feature

1. Define service method in appropriate service or create new one.
2. Implement permission + domain checks at top.
3. Reuse Tools/JsonTools for formatting & sanitization.
4. Expose through REST controller returning DataTableResponse or ApiResponse.
5. Update frontend to consume structured result; include error path tests.

## Testing Tips

- Unit test service logic with mock UsersDB / repository beans.
- Integration test approval transitions: create draft -> submit -> approve -> publish.
- Validate notification propagation: assert NotifyBean entries appear in REST response.

## Related System Dependencies

- DatatableRestControllerV2 for uniform CRUD scaffolding.
- Constants / Prop for configuration and language.
- CloudToolsForCore for domain scoping.

## Update Process

- Document new workflow steps here (add to Approval Workflow bullet) staying concise.
