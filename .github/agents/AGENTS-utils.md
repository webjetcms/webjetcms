# AGENTS: Core Utilities

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

The root `sk.iway.iwcm` package contains long-standing framework classes, not just pure utilities. Reuse an existing API after checking its behavior and callers; keep new feature-specific logic in the relevant feature package.

## Entry Points

Paths are relative to the core repository root. API availability and behavior must be checked against the installed version in customer projects.

| Source | Responsibility |
| --- | --- |
| `src/main/java/sk/iway/iwcm/Tools.java` | String/number/date helpers, request values, URLs, file paths, HTTP operations and Spring bean access |
| `src/main/java/sk/iway/iwcm/FileTools.java` | File reading/writing/copying, filename helpers and related CMS file operations |
| `src/main/java/sk/iway/iwcm/JsonTools.java` | JSON serialization/helpers and legacy preview HTML transformations |
| `src/main/java/sk/iway/iwcm/PageParams.java` | Parameters of dynamically included components |
| `src/main/java/sk/iway/iwcm/Identity.java` | Session user identity extending `UserDetails`, including administration and permission state |
| `src/main/java/sk/iway/iwcm/Constants.java` | Runtime configuration; see [System](AGENTS-system.md) |
| `src/main/java/sk/iway/iwcm/i18n/Prop.java` | Language selection, translation bundles and database text overrides |
| `src/main/java/sk/iway/iwcm/DB.java` | Legacy database access and HTML filtering |
| `src/main/java/sk/iway/iwcm/Logger.java` | Existing application logging facade |

## Tools Contracts

- Parsing helpers such as `getIntValue(...)` take fallback values. Choose a fallback that preserves the caller's contract; `0` and `-1` often have different meanings.
- Date/time helpers use `FastDateFormat` instances configured from `dateFormat`, `dateTimeFormat` and `timeFormat`. `Tools.reinitialize()` reloads those patterns. Preserve the established configuration lifecycle rather than adding per-request initialization.
- `Tools.slovakCollator` is a shared Slovak `Collator` configured with `SECONDARY` strength. Use locale-appropriate ordering for the feature and do not mutate the shared collator's settings.
- Request helpers (`getRequestParameter`, `getRequestParameterValues`, `getRequestParameterNames`) apply `Tools.escapeHtml(...)` by default. Their `Unsafe` variants or `sanitize=false` overloads return raw data; select deliberately for the intended output and parsing context.
- `Tools.getRealPath(...)` resolves CMS virtual paths and external static storage. It does not replace path authorization; see [Shared helpers](AGENTS-common.md).
- HTTP/file helpers can perform I/O or depend on request/configuration state. Inspect timeout, resource-closing and error behavior before reusing them. The presence of legacy `doNotVerifyCertificates(...)` methods is not a reason to disable TLS verification in new integrations.

## JSON, Escaping and HTML Sanitization

These operations have different purposes:

| Operation | Actual behavior |
| --- | --- |
| `JsonTools.objectToJSON(object)` | Jackson serialization with `JavaTimeModule`; date timestamps are enabled and instant timestamps use milliseconds rather than nanoseconds |
| `JsonTools.prepare4Json(html, request)` | Regex-based preview transformation: removes includes/selected markup and styles, rewrites local images to thumbnails and local links to absolute URLs |
| `JsonTools.escape(text)` | Replaces double quotes only; it is not a complete JSON string encoder |
| `Tools.escapeHtml(text)` | Uses `ResponseUtils.filter(...)`, then restores `&amp;` to `&`; it follows WebJET's existing escaping convention rather than sanitizing an HTML document |
| `DB.filterHtml(text)` | Escapes via `ResponseUtils.filter(...)` when `<` or `>` occurs; it does not implement an HTML allowlist |
| `AllowSafeHtmlAttributeConverter.sanitize(html)` | Uses the OWASP HTML sanitizer with the policy defined in that converter |

- Serialize JSON with Jackson/the REST response pipeline instead of building quoted JSON by concatenation. JSON serialization does not itself make a value safe inside an HTML page or inline script.
- `prepare4Json(...)` neither encodes JSON nor removes all executable HTML. Never use it as an XSS security boundary.
- For untrusted rich HTML, inspect `src/main/java/sk/iway/iwcm/system/jpa/AllowSafeHtmlAttributeConverter.java` and the feature's intended allowed markup. Its policy preserves selected formatting, styles, links, images and attributes; it is not a full-page editor policy.
- `src/main/java/sk/iway/iwcm/system/jpa/XssAttributeConverter.java` auto-applies to JPA strings and filters on entity reads, subject to `DB.isHtmlAllowed("jpaentity")`. `AllowSafeHtmlAttributeConverter` also sanitizes on reads when enabled. Both leave database writes unchanged.
- `src/main/java/sk/iway/iwcm/system/jpa/AllowHtmlAttributeConverter.java` passes values through unchanged. Using it deliberately disables the default string filtering for that field; it does not make the HTML safe.
- Preserve the intended encoding stage to avoid double escaping. HTML text, HTML attributes, JavaScript, URLs and JSON need handling appropriate to their destination.

## Component Parameters

`PageParams` parses the comma-separated parameter syntax used by `!INCLUDE(...)!` components, including quoted values. It is not a general URL query-string parser.

- `new PageParams(request)` reads the `includePageParams` request attribute. It can restore saved parameters using the `__ppState` request parameter and session state.
- `new PageParams(String)` parses an explicit component-parameter string.
- Use its typed getters and defaults for component settings; use the surrounding controller/request-binding conventions for HTTP input.
- `persistToSession(request)` saves parameters and returns the hidden field used by subsequent form submissions. Retain this only when the component workflow needs that state.

## Translation Bundles

- Resolve text with `Prop.getInstance(request).getText(key)` for the request language, or `Prop.getInstance(language)` when a language is explicit. Preserve existing translation keys and parameter placeholders.
- Core administration texts live in `src/main/webapp/WEB-INF/classes/text-webjet9.properties` (Slovak), `text_cz-webjet9.properties` (Czech) and `text_en-webjet9.properties` (English). Use the target project's supported languages and installation suffix for customer additions.
- `Prop` loads the base `text*.properties`, then skin/installation bundles selected through `defaultSkin`, `propInstallNames`, installation and log-installation names. Database text overrides are applied afterwards. A changed file can therefore be masked by a database override or cached translations.
- Inspect `src/main/java/sk/iway/iwcm/components/translation_keys/` for translation editing and missing-key handling. Reuse the established refresh path instead of clearing translation state on every request.

## Identity and Verification

Obtain the current user through the existing `UsersDB`/controller facilities; an `Identity` constructed in isolation does not establish an authenticated request. See [Users](AGENTS-users.md) and [Shared helpers](AGENTS-common.md) for permissions and session/Spring Security coordination.

Existing focused tests include `src/test/java/sk/iway/iwcm/ToolsTest.java`, `src/test/java/sk/iway/iwcm/JsonToolsTest.java` and `src/test/java/sk/iway/iwcm/FileToolsTest.java`. For changed utility behavior, verify the caller's defaults, null handling, encoding and side effects rather than relying on the helper's name.
