# AGENTS: users package (`sk.iway.iwcm.users`)

<!-- Navigation (edit in AGENTS-NAV.md) -->
<div align="right">[Root](AGENTS.md) | [System](AGENTS-system.md) | [DataTable](AGENTS-datatable.md) | [Editor](AGENTS-editor.md) | [Doc](AGENTS-doc.md) | [Components](AGENTS-components.md) | [Common](AGENTS-common.md) | [Utils](AGENTS-utils.md)</div>

Handles user security metadata (password policy, permission groups) and DTO mapping for lightweight identity exposure.

## What Belongs Here / Not Here

- Put here: password hashing strategies, permission group entities/mappers, lightweight user DTO mapping.
- Not here: session management (elsewhere), feature-level permission checks (implemented in those services), generic cryptographic primitives (utils) unless user-specific.
- Promote shared security infra into system if it broadens beyond users.

## Core Elements

- `PasswordSecurity` / `PasswordSecurityAlgorithm`: Encapsulate hashing & policy (iterations, salt strategy). Abstract algorithms for future upgrades.
- `PermissionGroupBean`: Defines a group/role; editing reflected in UI permission toggles.
- `PermissionGroupEditorFields`: DataTable/editor bridge for permission group entities.
- `UserBasicDto` + `UserBasicDtoMapper`: Lightweight user projection for contexts where full user entity is unnecessary.

## Password & Security Patterns

- Centralize hashing in `PasswordSecurity`; never duplicate hashing logic in controllers/services.
- When upgrading algorithm: maintain backward compatibility by detecting legacy hash format and rehashing on successful login.
- Keep iteration counts & algorithm identifiers configurable (via Constants) to adjust strength without redeploying code.

## Permission Handling

- Group membership drives visibility (e.g., noperms CSS in layout). Ensure consistent naming to map permissions -> CSS classes.
- Editing groups should trigger cache/invalidation of any permission-derived UI fragments.

## DTO Mapping

- Mapper isolates external representation: add new user fields here first; avoid exposing sensitive hashes/tokens.

## Conventions

- Suffix `*Dto` for transfer objects, `*Mapper` for mapping classes, `*EditorFields` for UI field wrappers.
- Separate algorithm selection from hashing execution to allow runtime strategy adjustments.

## Pitfalls

- Hardcoding algorithm names makes future migrations painful; rely on constants/config.
- Leaking full user entity to external API increases attack surface; prefer `UserBasicDto`.
- Failing to rehash upgraded passwords on login leaves weaker legacy hashes in place.

## Extending

1. Add new permission attribute: extend `PermissionGroupBean`, update editor fields & UI permission mapping.
2. Introduce new hashing algorithm: implement in `PasswordSecurityAlgorithm`, register, add migration flag.
3. Add user field for display: add to DTO + mapper; adjust caches/invalidation as needed.

## Testing Checklist

- Password verification passes for legacy + new hashes after upgrade path.
- Permission group edits reflect immediately in UI restricted elements.
- DTO mapping never includes credential or sensitive token fields.

## Update Process

- Document algorithm migrations here including fallback & rehash-on-login strategy.
