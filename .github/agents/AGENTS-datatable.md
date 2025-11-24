# AGENTS: datatable subsystem (`sk.iway.iwcm.system.datatable`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Users](AGENTS-users.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Generic CRUD + filtering + validation + notification pipeline powering admin DataTables Editor screens.

## What Belongs Here / Not Here

- Put here: abstractions & base controllers for tabular CRUD, editor annotations, thread-scoped notification/state objects.
- Not here: entity-specific business logic (services in system/editor/components), generic utilities, feature-specific caching.
- Move shared CRUD patterns here when duplicated across >1 feature controller.

## Core Life Cycle

1. insertItem
   - `processToEntity(CREATE)` -> `repo.save` -> `processFromEntity(CREATE)` to enrich DTO/editor fields.
2. editItem
   - Fetch original (`getOne`), preserve ThreadBean flags (notify list, forceReload, importing), merge non-null edits, `processToEntity(EDIT)`, save, `processFromEntity(EDIT)`.
3. Regex Filtering
   - Client prefixes value with `regex:` (see `REGEX_PREFIX`); backend interprets remainder as pattern.

## Thread State (`ThreadLocal<ThreadBean>`)

- Holds notify messages, forceReload flag, importing flag.
- Always restored after entity fetch to avoid losing queued notifications.

## Validation Strategy

- Standard Bean Validation; explicit `BeanPropertyBindingResult` lets controller merge constraint violations and custom domain errors into unified response.

## Domain Scoping

- If cloud or external static files enabled and repository implements `DomainIdRepository`, queries add implicit `domainId` predicate to prevent cross-domain access.

## Reflection Utilities

- `BeanWrapperImpl` for dynamic property access.
- `NullAwareBeanUtils.copyProperties` merges only non-null values from client DTO to persistent entity.

## Notifications

- `NotifyBean` objects queued in thread data; serialized into response to inform frontend (toast/messages panel).

## Extension Hooks (override in subclass)

- `beforeSave(entity, action)` / `afterSave(entity, action)` style template methods (search for protected methods in base).
- Column customization via `@DataTableColumnEditor` & `@DataTableColumnEditorAttr` annotations.

## JSON / (De)Serialization

- Custom Jackson deserializers (e.g., `DocDetailsFullPathDeserializer`) translate full path strings to entities.

## Error Handling

- Domain/user errors: throw `EditorException` (mapped to friendly response).
- System errors: logged via `Logger.error`; transformed into `DataTableError` structure.

## Performance Considerations

- Uses EclipseLink `ReadAllQuery` and `ExpressionBuilder` for dynamic conditions when Specifications are insufficient.
- Minimize reflection per-row; cache metadata if extending heavily.

## Conventions

- Keep controller subclasses thin: delegate complex logic to services.
- Preserve `REGEX_PREFIX` constant alignment with frontend; coordinate if changing.
- Provide `entityClass` in constructor to allow default instance population (requires frontend `fetchOnCreate: true`).

## Pitfalls

- Missing `DomainIdRepository` implementation -> unintended cross-domain edits.
- Not restoring ThreadBean flags -> lost notifications or incorrect forceReload behavior.
- Forgetting to copy non-null fields before `processToEntity` leads to overwriting persisted values with nulls.

## How To Add A New DataTable Entity

1. Create JPA entity & repository (implement `DomainIdRepository` if domain-scoped).
2. Subclass `DatatableRestControllerV2<Entity, Long>` passing repo and `Entity.class`.
3. Annotate fields with editor annotations for UI customization.
4. Override hooks for domain logic/audit as needed.
5. Configure frontend WJ.DataTable (ajax source, `fetchOnCreate` when entityClass provided, regex search support if needed).

## Testing Checklist

- Create & edit: verify default values present when `entityClass` supplied.
- Regex search: prefix input with `regex:` and confirm pattern filter applied.
- Domain isolation: ensure records from another domain are excluded.
- Notification propagation: trigger action that queues NotifyBean; assert appears in JSON response.

## Update Process

- New annotation: extend `DataTableColumnsFactory` & document here.
- Changing regex mechanics: update `REGEX_PREFIX` both backend & frontend; add migration note.
