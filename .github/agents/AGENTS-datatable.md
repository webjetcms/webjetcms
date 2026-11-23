# AGENTS: Administration DataTables

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

The administration uses jQuery DataTables and Editor through WebJET's `WJ.DataTable` wrapper. Paths and signatures below describe current core; verify them against the installed CMS in a customer project.

## Source map

| Concern | Entry point |
| --- | --- |
| REST lifecycle, filtering, import and permissions | `src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java` |
| Column/editor metadata | `src/main/java/sk/iway/iwcm/system/datatable/annotations/`, `DataTableColumnType.java`, `DataTableColumnsFactory.java` in the same parent package |
| Transient editor state | `src/main/java/sk/iway/iwcm/system/datatable/BaseEditorFields.java` |
| Domain-aware repository interface | `src/main/java/sk/iway/iwcm/system/datatable/spring/DomainIdRepository.java` |
| Editor exception responses | `src/main/java/sk/iway/iwcm/system/spring/DatatableExceptionHandlerV2.java` |
| Frontend wrapper and field types | `src/main/webapp/admin/v9/npm_packages/webjetdatatables/` |
| Integration documentation | `docs/sk/developer/datatables/README.md`, `restcontroller.md`, `domainid.md`, `export-import.md` in that directory |

## Starting a screen

1. Find a nearby screen with the same persistence and permission needs. A JPA-backed screen typically has an entity, repository, REST controller and administration HTML page; some controllers instead adapt legacy services or non-database data.
2. Annotate entity/editor fields with `@DataTableColumn`, optionally `@DataTableColumnEditor`, `@DataTableColumnEditorAttr` and `@DataTableColumnNested`. Keep UI-only values in transient editor fields and map them explicitly in the controller.
3. Extend `DatatableRestControllerV2<Entity, Long>` and pass the repository. Add `@RestController`, `@Datatable`, `@RequestMapping` and an explicit `@PreAuthorize` permission expression. `@Datatable` selects the DataTable exception handler.
4. Passing `Entity.class` as the second constructor argument enables a new instance for `GET /-1`; pair this with frontend `fetchOnCreate: true` when constructor defaults are needed.
5. Configure `WJ.DataTable` with the URL, columns and paging mode. Check the actual HTML/module registration, translations and repository scanning in the target project.

`src/main/java/sk/iway/iwcm/components/users/permgroups/PermissionGroupRestController.java` is an example of mapping `editorFields` in both directions. Use the installed `wj-datatable` skill for a full implementation workflow when available.

## Lifecycle and extension points

`POST /editor` accepts a `DatatableRequest` with an action and a map of rows. It coordinates create, edit, delete, duplicate and import operations; it delegates ordinary writes to `add`, `edit` and `delete`.

- Create: prepare generated ID, `beforeSave(entity)`, validation and item permission check, then `insertItem`. Its default implementation runs `processToEntity(CREATE)`, `repo.save` and `processFromEntity(CREATE)`. `afterSave(entity, saved)` follows the write.
- Edit: align the submitted ID with the route ID, `beforeSave`, validation and item permission check, then `editItem`. The default fetches the existing row through `getOne`, merges permitted fields, runs `processToEntity(EDIT)`, saves and runs `processFromEntity(EDIT)`; `afterSave` follows.
- Delete: item permission check, `beforeDelete(entity)`, deletion and `afterDelete(entity, id)`. The default `deleteItem` also invokes `beforeDelete` on a repository reference, so do not assume this hook runs exactly once or perform irreversible work there.
- Save/delete/duplicate paths also publish `DatatableEvent` events. Overriding the low-level methods changes this behavior; inspect callers before bypassing the default path.

Common override signatures are:

| Hook | Purpose |
| --- | --- |
| `processFromEntity(T, ProcessItemAction)` | Populate response/editor values; handle a missing row where applicable |
| `processToEntity(T, ProcessItemAction)` | Copy editor values into the persisted model |
| `beforeSave(T)`, `afterSave(T, T)` | Work before validation/save or after a successful write |
| `beforeDelete(T)`, `afterDelete(T, long)` | Veto deletion or perform follow-up work |
| `getOptions(DatatablePageImpl<T>)` | Supply select options |
| `addSpecSearch(Map<String, String>, List<Predicate>, Root<T>, CriteriaBuilder)` | Extend specification search conditions |
| `checkAccessAllowed(HttpServletRequest)`, `checkItemPerms(T, Long)` | Controller-wide and individual-row authorization |
| `validateEditor(HttpServletRequest, DatatableRequest<Long, T>, Identity, Errors, Long, T)` | Validate each Editor request row |

There are additional import, duplication, action and row-reorder hooks; use their exact signatures from the base class rather than inventing `beforeSave(entity, action)` overloads.

## Validation and merge behavior

- Entity constraints use Jakarta Bean Validation. `validateEditor` runs through the `/editor` binder; add field errors using paths such as `errorField.name`. Direct `/add` and `/edit/{id}` calls do not use this Editor-specific hook, so required business invariants must also hold in the underlying write path.
- Use `throwError` for a translated Editor error and the existing `DatatableResponse`/`DatatableFieldError` contract. Many Editor validation failures use an HTTP 200 response containing `error`/`fieldErrors`; status alone does not prove success.
- `copyEntityIntoOriginal` uses `system/spring/NullAwareBeanUtils`, but is not simply a non-null copy: it protects identifiers, skips hidden/disabled editor fields and can clear numeric/date fields or fields marked `alwaysCopyProperties`. Import handling preserves omitted columns. Verify these cases when changing annotations or overrides.
- Generated IDs are cleared for create requests; edit IDs are checked against the requested target. Preserve these protections in custom write implementations.
- Notifications, import flags, imported columns, redirects and reload requests are kept in the controller's `ThreadBean`. Use `addNotify`, `setForceReload`, `isImporting`, `isExporting` and related APIs; nested calls such as `getOne` can reset this state.

## Permissions and domain scope

- The base `checkAccessAllowed` and `checkItemPerms` return `true`. A subclass must supply its required policy. `WebjetSecurityService.checkAccessAllowedOnController` handles the supported class-level WebJET permission expressions and invokes the controller hook.
- Row checks do not filter the full list automatically. Apply visibility restrictions to list/search queries and enforce ownership again for direct row requests, actions, export/import and reorder operations.
- Automatic DataTable domain reads/searches are enabled at controller construction only when the repository implements `DomainIdRepository` and the deployment is cloud or enables `enableStaticFilesExternalDir`. The effective ID comes from protected `getDomainId()`.
- `/editor` also sets/checks submitted `domainId` for `DomainIdRepository` independently of that read-filter flag. This does not scope arbitrary repository calls, custom endpoint implementations or all direct write paths. Inspect each route before relying on the integration.

## Filtering and verification

- `searchItem` uses Spring Data specifications when the repository implements `JpaSpecificationExecutor`; otherwise it uses query by example. `addSpecSearch` affects the specification search, not an ordinary `getAllItems` query; `getAllItemsIncludeSpecSearch` is available when a list must apply those conditions too.
- Search parameters support exact/start/end markers (`^value$`, `^value`, `value$`) and range/date-range formats. `regex:` is handled in the example matcher, but is not a promise of arbitrary regular-expression support in every repository/database path. Keep frontend search conventions aligned with the implementation.
- Verify the changed behavior with the existing test infrastructure: defaults, create/edit/delete, validation feedback, omitted/cleared values, list/search permissions, foreign IDs/domains and relevant import/export actions. Existing backend coverage starts at `src/test/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2Test.java`; use the matching browser tests for UI behavior.
