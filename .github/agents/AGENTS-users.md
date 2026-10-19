# AGENTS: Users, authentication and permissions

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

User functionality spans legacy identity helpers, administration JPA controllers and Spring authentication. These paths describe current core; customer projects may use an older CMS or a custom authentication provider.

## Source map

| Concern | Entry points |
| --- | --- |
| Session identity and permission flags | `src/main/java/sk/iway/iwcm/Identity.java`, `src/main/java/sk/iway/iwcm/users/UsersDB.java` |
| Legacy user model and user groups | `src/main/java/sk/iway/iwcm/users/UserDetails.java`, `UserGroupsDB.java`, `UserGroupDetails.java` in that directory |
| Administration user CRUD and self-edit | `src/main/java/sk/iway/iwcm/components/users/userdetail/` |
| Administration permission groups | `src/main/java/sk/iway/iwcm/users/PermissionGroupBean.java`, `src/main/java/sk/iway/iwcm/components/users/permgroups/` |
| Login orchestration and password policy | `src/main/java/sk/iway/iwcm/common/LogonTools.java`, `src/main/java/sk/iway/Password.java` |
| Password hashes and reset flow | `src/main/java/sk/iway/iwcm/users/PasswordSecurity.java`, `UserChangePasswordService.java` in that directory |
| Spring authentication and authorization | `src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java`, `WebjetAuthentificationProvider.java`, `services/WebjetSecurityService.java` in that directory |
| Recognized administrator devices | `src/main/java/sk/iway/iwcm/users/devices/` |

## Identity and scope

- Obtain the current `Identity` through `UsersDB.getCurrentUser(request/session)` or the existing controller's `getUser()`. The result may be `null`; WebJET's `Identity` is distinct from Spring Security's authentication object.
- `UsersDB.getDomainId()` returns the current cloud domain in cloud mode and `1` otherwise. `UsersDB.getDomainIdSqlWhere(...)` similarly omits domain separation outside cloud mode. Do not apply generic website-domain assumptions to user accounts.
- `UserDetailsController` enforces administrator/public-user editing rights and validates stored target records, submitted state and cloud ownership. Keep that logic when extending account editing; a submitted role/domain value is not proof of permission.
- `UserDetailsSelfController` at `/admin/rest/user-self` is a restricted administrator self-edit API. It does not grant general user-management rights or provide a public registration API.

## Permissions

- User groups (`UserGroupsDB`) and administration permission groups (`PermissionGroupBean`) serve different purposes. Permission groups combine module rights and editable page/folder scopes; adding a new module permission does not normally require a new field on `PermissionGroupBean`.
- Use existing keys from module/menu definitions and inspect `src/main/java/sk/iway/iwcm/admin/layout/MenuService.java` for the permission tree. `Identity.isEnabledItem(key)` checks WebJET's enabled/disabled permission flags; folder/page ownership still needs the relevant feature checks.
- Spring endpoints commonly use `@PreAuthorize("@WebjetSecurityService.hasPermission('permission-key')")`. `hasPermission` requires an administrator and the corresponding authority. It accepts `|` for alternatives or `&` for all listed rights; combining both separators is rejected.
- `WebjetSecurityService.isLogged()`, `isAdmin()` and `isInUserGroup(...)` cover different policies. Choose the policy explicitly, and check individual records where necessary.
- Menu visibility and `noperms` CSS are presentation only. Secure the endpoint and its list/search/action paths. DataTable subclasses must follow the [DataTable permission rules](AGENTS-datatable.md).
- Permission/group edits and session authentication are separate operations. Check how the existing save path updates live identities/authorities; do not assume that a UI refresh reloads all permissions in every session.

## Passwords and authentication

- `PasswordSecurity` delegates to `users/crypto/Bcrypt.java` and `Sha512.java` through `PasswordSecurityAlgorithm`. New hash generation follows `passwordHashAlgorithm`; bcrypt cost uses `bcryptSaltRounds`. Verification recognizes the stored bcrypt prefix and retains SHA-512 compatibility.
- `LogonTools.isPasswordCorrect` additionally supports legacy encrypted credentials. Preserve the established login/password-change path when modifying authentication; do not replace it with a direct comparison or a new controller-local hashing implementation.
- Password policy lives in `sk.iway.Password` and the user services, rather than solely in the hashing helper. `UserDetailsService.savePassword` handles the unchanged-password marker, hash/salt storage, password history, auditing and invalidation of other sessions.
- Changing the configured hash algorithm does not itself rewrite existing credentials. Verify a concrete migration path before promising rehashing on login; the hashing strategy interface does not provide a universal migration callback.
- Use `UserChangePasswordService` for the established reset workflow and `LogonTools` for successful-login checks/callbacks. Current integrations also include native second-factor logic, OAuth2 (`system/spring/oauth2/`), passkeys (`system/spring/passkey/`) and API-token authentication (`system/spring/ApiTokenAuthFilter.java`); availability depends on configuration and version.

## User data exposed to clients

- `UserDetailsEntity` deliberately separates credential persistence from ordinary entity reads/writes. Preserve its password/salt and API-key handling; do not add credential fields to generic entity serialization.
- `UserBasicDto` and its MapStruct mapper provide a small user projection with names, email, photo, ID and login. It excludes password material but still contains personal data; choose the fields and access policy for the consuming endpoint.
- `UsersDB.getUserCached` has explicit invalidation through `removeUserFromCache`. Use the established save service so changes do not leave cached or session identities stale.

## Administrator devices and sessions

- `DeviceService` owns account-scoped persistence, confirmation/blocking rules and auditing; `AdminDeviceService` integrates browser cookies, email proofs, login checks and session invalidation. Scope device lookups by authenticated `userId`, not by a browser-supplied ID or IP address.
- Recognition, confirmation and blocking are separate states. `reportedAt` records a block; ordinary confirmation cannot unblock a device and cleanup preserves blocked records. Device identity comes from the account/browser-token association, not browser labels or location.
- Email confirmation requires a valid account-owned proof; a completed native second factor can also confirm an unblocked device. Blocked-browser login requires the session/cookie-bound unblock challenge. Preserve proof expiry, attempt accounting and resend checks in the existing services/repository. The code-attempt claim is atomic, but the subsequent entity save is separate; do not describe the entire service as one transaction.
- `DeviceEntity` disables shared JPA caching and hides token/proof fields with `@JsonIgnore`. Keep secrets out of JSON, logs and audit messages; browser-provided location is advisory.
- Completed interactive administrator logins bind a device ID to session state. `AdminDeviceRestController` exposes account-scoped history at `/admin/rest/security/login-events`; blocking uses the established session-invalidation path. Do not bypass `AdminDeviceService.requireVerification` when extending an interactive login flow.

## Verification

Use existing tests for the path being changed: `src/test/java/sk/iway/iwcm/components/users/userdetail/` covers user/self-edit controllers; `src/test/java/sk/iway/iwcm/users/devices/` covers device services. Exercise foreign account IDs, unauthorized role changes, password-unchanged behavior and proof rejection where relevant. Database tests use the project's existing database infrastructure.
