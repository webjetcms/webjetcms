# AGENTS: Shared CMS Helpers

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

`src/main/java/sk/iway/iwcm/common/` contains shared CMS integration helpers. It is an established mixed package: methods can depend on the request, session, configuration, database, caches and filesystem. Check the implementation and callers before treating a helper as stateless or generally safe to reuse.

## Entry Points

All paths below are relative to the core repository root. In customer projects, verify these APIs against the installed WebJET version.

| Source | Responsibility |
| --- | --- |
| `src/main/java/sk/iway/iwcm/common/CloudToolsForCore.java` | Current domain name/ID, legacy domain SQL conditions and cloud/multiweb behavior |
| `src/main/java/sk/iway/iwcm/common/FilePathTools.java` | External static-file directories, domain storage roots and HTTP resource streaming |
| `src/main/java/sk/iway/iwcm/common/WriteTagToolsForCore.java` | Rendered-content transformations, form spam protection and custom component/template paths |
| `src/main/java/sk/iway/iwcm/common/DocTools.java` | URL/slug helpers, content-code replacement, editable documents and HTML diffs |
| `src/main/java/sk/iway/iwcm/common/AdminTools.java` | Administration helpers such as recent pages and file versions |
| `src/main/java/sk/iway/iwcm/common/UploadFileTools.java` | Page upload directories, upload type/size restrictions and gallery integration |
| `src/main/java/sk/iway/iwcm/common/FileBrowserTools.java` | Forbidden filename/path symbols and metadata handling |
| `src/main/java/sk/iway/iwcm/common/LogonTools.java` | Login checks, permission setup and coordination of WebJET/Spring authentication state |

## Domain Context

- `CloudToolsForCore.getDomainName()` uses the current `RequestBean` registered by `SetCharacterEncodingFilter`; without an available domain it returns `unknown`. Configured fixed-domain URLs can change its result for administrators.
- `getDomainId()` normally returns `1` outside cloud/external static-file mode. In those modes it resolves the domain through `GroupsDB`, with configured fixed/common-domain exceptions. A domain name and a domain ID are not interchangeable.
- `getDomainIdSqlWhere(...)` supports legacy SQL and changes behavior with cloud/external-file configuration. Do not assume it always adds a restrictive predicate.
- For domain-scoped JPA repositories, read [System](AGENTS-system.md) and [DataTable](AGENTS-datatable.md). Resolving a domain in a helper does not automatically scope every query.
- Background work must establish or pass the intended context explicitly; a request-dependent helper cannot infer which website a scheduled job belongs to.

## Files and Paths

- `FilePathTools` maps configured external directories (`cloudStaticFilesDir`) and streams files through `MultipartFileSender`. It is not a general path sanitizer or an authorization check.
- Resolve CMS virtual paths through `Tools.getRealPath(...)`; inspect `src/main/java/sk/iway/iwcm/io/IwcmFile.java` and `src/main/java/sk/iway/iwcm/io/IwcmFsDB.java` when a feature must work with WebJET's file/database storage abstraction. Avoid assuming every CMS resource is an ordinary local file.
- File operations need their own validated path and permission checks. Existing entry points include `FileBrowserTools.hasForbiddenSymbol(...)`, `UploadFileTools.isFileAllowed(...)` and `UsersDB.isFolderWritable(...)` in `src/main/java/sk/iway/iwcm/users/UsersDB.java`. Each covers a different part of the operation; path resolution alone does not grant access.
- `UploadFileTools.isFileAllowed(...)` checks filename restrictions, configured size/type limits and user context. Follow the surrounding upload controller's checks as well.

## Rendering and Custom Overrides

- `WriteTagToolsForCore.getCustomPath(...)` resolves `/components/`, `/templates/` and `/apps/` overrides using installation/language context; `getCustomPage(...)` and `getCustomPageAdmin(...)` provide related resolution paths. Inspect existing callers before adding an override.
- Its rendering helpers also rewrite multidomain links, process `!WRITE(...)!` content and apply form spam protection. It is broader than an administration CSS/JS loader.
- `DocTools.updateCodes(...)` participates in page-content substitution. Preserve the surrounding rendering lifecycle when changing it; transformations can affect many components.
- These text transformations do not replace HTML sanitization or context-appropriate output encoding; see [Utilities](AGENTS-utils.md).

## Authentication Integration

Use the existing login flow and read [Users](AGENTS-users.md) before changing authentication.

- `LogonTools.setUserToSession(...)` updates the WebJET identity and Spring Security state. It does not run browser post-login callbacks.
- `afterSuccessLogon(request, response)` reads the session identity and runs login interceptors, browser recognition and the configured `afterLogonMethod` once per account/request. Call it only after all password, factor and device checks succeed, before redirecting/forwarding. API-token and HTTP Basic authentication must not call this browser hook.
- During pending authentication checks, `clearUserFromSession(...)` clears both the WebJET session identity and Spring Security context. Removing only `Constants.USER_KEY` leaves other authentication state behind.
- The legacy `afterLogon(...)` method invokes only the configured callback and is deprecated; it is not equivalent to `afterSuccessLogon(...)`.

## Changes and Verification

Keep feature logic in its feature package; reuse these helpers where their actual contract fits. Preserve existing call signatures and configuration behavior unless the requested change requires otherwise.

Focused test examples are `src/test/java/sk/iway/iwcm/common/LogonToolsAfterSuccessLogonTest.java`, `src/test/java/sk/iway/iwcm/common/UploadFileToolsTest.java` and `src/test/java/sk/iway/iwcm/common/FileBrowserToolsTest.java`. Test domain and permission boundaries when the change affects them, using the project's configured infrastructure.
