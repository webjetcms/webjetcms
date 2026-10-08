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

- `users.devices`: `DeviceEntity` stores browser recognition and its latest notice in `user_login_devices`, using a generated `device_id` and a unique user/token pair. Scope account-facing lookups by `userId`; user IDs already identify users across domains. `DeviceService` returns entities and uses ordinary repository saves without service-level transactions, explicit locks or retries. Re-detection replaces the notice on the same device and invalidates both email proofs. Confirmation requires an account-owned, single-use 256-bit email token (24 hours) or six-digit email code (10 minutes, five attempts, 60-second resend interval); ID-only confirmation is forbidden. The read-only report link still uses the device ID. JSON exposes `createDate`, `lastSeen` and a computed `expiresAt`; `@JsonIgnore` excludes `userId`, `tokenHash`, verification hashes, deadlines and attempts. `lastSeen` is the latest successful recognized login; browser, OS and IP remain the detection snapshot. Disable shared JPA caching. `AdminDeviceService` supplies the administration-specific authentication, cookie and email integration.
- Device blocking reuses `reportedAt`; cleanup preserves blocked records and ordinary confirmation cannot unblock them. Successful blocking removes its dashboard notice immediately; active-notice queries exclude reported devices. `DeviceService` audits persisted detection, confirmation (email, second factor or unblock code), and blocking under `Adminlog.TYPE_USER_DEVICE`, without logging verification secrets or repeated no-op transitions. `AdminDeviceService.requireVerification` runs after credentials/native 2FA and before successful-login callbacks for interactive admin login paths. It stores `PendingLogin` outside both authentication contexts; `/admin/logon/device/` verifies the session/cookie-bound email code (15-minute challenge, 10-minute code, five attempts, 60-second resend). Successful native 2FA auto-confirms only unblocked devices. Cookie removal is intentionally treated as a new browser. API tokens and Basic auth bypass browser detection.
- Completed administrator logins bind the owned `deviceId` to the HTTP session and `SessionDetails` for cluster propagation. Dashboard bootstrap adds fresh `deviceConfirmed` values with one account-scoped device query; do not infer device identity from IP address or browser labels. Legacy sessions without a device ID retain ordinary logout. The personal-session tab lists sessions and logout actions only. Its separate devices tab lazily reads `GET /admin/rest/security/login-events?page=N`: 20 retained devices per page, sorted by `lastSeen` and ID descending and scoped to the authenticated administrator, including confirmed and blocked records without active sessions. It confirms unconfirmed devices through the existing email proof action and blocks any unblocked device, signing out all known matching sessions through existing `SessionHolder.invalidateSession`. Blocked rows offer no ordinary confirmation; unblocking requires the login email challenge.

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
