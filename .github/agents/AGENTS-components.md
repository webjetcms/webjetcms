# AGENTS: Applications and components

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

Feature modules under `src/main/java/sk/iway/iwcm/components/` include public page applications, admin CRUD screens and supporting services. These are distinct extension types, with different rendering and authorization paths.

Paths below describe current WebJET core. For customer projects, use the project's package, resources and installed CMS APIs; do not assume every current-core application or dependency exists in an older release.

## Choose the extension type

| Requirement | Starting points |
|---|---|
| Application inserted into page content | `@WebjetComponent`, `@WebjetAppStore`, `WebjetComponentAbstract`; use the `wj-appstore-app` skill. |
| Parameter editor for an existing JSP application | An AppStore class with `componentPath` pointing at the existing JSP; public rendering remains JSP. |
| Administration CRUD screen | Entity/repository/controller plus DataTable view and module metadata; see [DataTable](AGENTS-datatable.md) and `wj-datatable`. |
| Administration home widget | `src/main/java/sk/iway/iwcm/components/welcome/`, `src/main/webapp/admin/v9/src/js/dashboard/` and `wj-dashboard-widget`. |
| Reusable website layout block | PageBuilder HTML and the `wj-page-builder` skill; this does not require an AppStore Java class. |

## Page application contract

- `src/main/java/sk/iway/iwcm/system/annotations/WebjetComponent.java` is a Spring component stereotype with request scope. Use a stable bean name consistent with the include name, normally the fully qualified class name.
- `src/main/java/sk/iway/iwcm/components/WebjetComponentAbstract.java` implements the shared application contract: initialization, editor options and common settings such as device/visibility/cache/wrapper attributes.
- `src/main/java/sk/iway/iwcm/system/annotations/WebjetAppStore.java` provides catalogue metadata: localized title/description, icon/screenshots, optional permission `itemKey`, settings/visibility flags and optional custom editor HTML. It does not by itself implement public rendering.
- Application parameters are fields annotated with `@DataTableColumn`; multi-tab editors use `@DataTableTabs`. `initAppEditor(ComponentRequest, HttpServletRequest)` prepares editor values, while `getAppOptions(...)` returns dynamic `OptionDto` lists.
- `src/main/java/sk/iway/iwcm/editor/rest/ComponentsService.java` binds include parameters to the bean and prepares the application editor. Keep field names compatible with includes already saved in page content.
- `src/main/java/sk/iway/iwcm/system/spring/webjet_component/WebjetComponentResolver.java` binds `PageParams`, invokes initialization, selects the handler and renders its result. `@DefaultHandler` identifies the default handler; the resolver also supports named handlers and the legacy `render` convention.
- `src/main/java/sk/iway/iwcm/system/spring/webjet_component/WebjetViewResolver.java` resolves views and installation-specific overrides. Existing JSP, FreeMarker and Thymeleaf resolvers coexist; follow the target application's view engine. New Spring/Thymeleaf examples return a view such as `/apps/form/mvc/multistep-form` for an `.html` resource.

Verified examples:

- `src/main/java/sk/iway/iwcm/components/multistep_form/mvc/MultistepFormApp.java`: Spring application with injected repositories, editor parameters, `@DefaultHandler` and a Thymeleaf view in `src/main/webapp/apps/form/mvc/`.
- `src/main/java/sk/iway/iwcm/components/langredirect/LanguageRedirectApp.java`: class-name include, tabbed parameter editor and dynamic language options.
- `src/main/java/sk/iway/iwcm/components/menu/MenuApp.java`: parameter editor whose `componentPath` keeps the public renderer at `/components/menu/menu_ul_li.jsp`.

A class-name include and a JSP include are both supported. Do not convert existing stored JSP includes into Java class includes merely because an AppStore editor class exists.

## Discovery, metadata and placement

- `src/main/java/sk/iway/iwcm/editor/appstore/AppManager.java` combines existing applications with scanned `@WebjetAppStore` classes. `getPackageNames()` includes core, installation-specific packages and configured `springAddPackages`; customer packages must be discoverable.
- `itemKey` and `variant` participate in replacing/matching catalogue entries. Preserve existing keys when migrating an editor and inspect the catalogue replacement logic before creating another entry with the same key.
- `src/main/java/sk/iway/iwcm/system/Modules.java` reads `modinfo.properties` and related metadata for admin navigation/module permissions. AppStore annotation registration and admin menu registration are separate concerns.
- Public JSP/resources generally live in `src/main/webapp/components/`; newer app views and custom editor HTML also live in `src/main/webapp/apps/`. Admin v9 view sources are in `src/main/webapp/admin/v9/views/`. Match the existing module instead of introducing a parallel frontend structure.
- Read `docs/en/custom-apps/appstore/README.md` and `docs/en/custom-apps/spring-mvc/README.md` for application examples. Validate their API assumptions against the installed version.

## Persistence and authorization

- Modules mix Spring Data/JPA repositories and legacy DB/JDBC helpers. Follow the existing feature's lifecycle and domain rules; introducing a repository alone does not migrate its old cache, audit or permission behavior.
- For domain-scoped application data, see [System](AGENTS-system.md) and [DataTable](AGENTS-datatable.md). Public rendering, custom repository queries and non-DataTable endpoints need explicit domain/access handling appropriate to that feature.
- AppStore visibility, `itemKey` and hidden menu items do not authorize public form submissions or REST operations. Follow the existing endpoint's server-side permission, input validation and CSRF handling.
- Put user-visible texts in the project's language bundles and resolve them with `Prop` or the view's translation support. Keep established module keys stable.
- Use existing `Constants` configuration and file/image helpers. Avoid introducing static caches for request-specific parameters or user/domain-specific responses.

## AI integration boundary

The AI feature is split between CMS integration and the `com.webjetcms:webjet-ai` library declared in `build.gradle`; local model support uses `webjet-ai-local`, included for core development but excluded from published client dependencies/distribution runtime by the build configurations. Verify the target project's dependency/version before using library APIs.

- `src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java` creates the managed `AiClient` through provider discovery, including application-defined `AiProvider` beans, and closes it on shutdown.
- CMS provider adapters, trusted configuration and include handling are under `src/main/java/sk/iway/iwcm/components/ai/providers/`; REST/task orchestration is under `src/main/java/sk/iway/iwcm/components/ai/rest/`.
- `src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java` is the CMS-facing provider contract. `src/main/java/sk/iway/iwcm/components/ai/dto/AssistantResponseDTO.java` carries response text, error, token count and generated/temporary file information. Do not assume an `AiExecutionResult` or a universal `{success, content, finishReason}` response.
- Preserve permission checks, prompt handling, temporary-file lifecycle and cancellation/streaming behavior when changing an adapter; protocol implementation may belong in the external library rather than this repository.

## Verification starting points

Use `src/test/webapp/tests/webpages/appstore.js` for catalogue/editor behavior, `src/test/webapp/tests/webpages/webpage-spring.js` for Spring component integration, and the matching feature directory under `src/test/webapp/tests/apps/`. Verify both editing/reopening the stored include and rendering it on the public page; for writable features, include relevant permission/domain cases. Customer applications use their own tests and fixtures.
