# WebJET CMS: Agent Knowledge Index

[Working guide](CONTRIBUTING.md) · [Topic navigation](AGENTS-NAV.md)

Start here, then read the topic relevant to the task. These references describe the WebJET CMS core source tree; they are also exported with the `wj-project-context` skill. Source paths below are relative to the **core repository root**, not to an installed skill directory.

## Establish the project and version first

- This repository contains the framework itself: `src/main/java/sk/iway/iwcm`, `src/main/java/sk/iway/webjet/v9`, the administration frontend and framework tests.
- A customer/basecms project normally consumes WebJET artifacts and adds its own application packages, templates and configuration. Its instructions take precedence over these shared references. Discover its CMS version, Java version, installation name, Spring/JPA scan configuration, bundles and migration mechanism before changing code.
- The stack below is the declared configuration of this core branch, checked in October 2026. It is not a compatibility promise for older releases. Verify APIs and `javax`/`jakarta` imports in the target version; do not upgrade a customer project's dependencies just to use a current-core example.
- Keep canonical knowledge in `.github/agents/`. The exporter copies it into `skills/wj-project-context/references/`; do not maintain a second copy in `.agents/skills/wj-project-context/`.

## Runtime and build stack

The source of truth is [build.gradle](../../build.gradle), the [Gradle wrapper](../../gradle/wrapper/gradle-wrapper.properties), the [admin package manifest](../../src/main/webapp/admin/v9/package.json) and the [browser-test manifest](../../src/test/webapp/package.json). Version ranges such as `7.0.+` are declarations, not resolved patch versions.

| Area | Current core configuration |
| --- | --- |
| Java / build | Java 17 source and target; Gradle wrapper 8.14; `java` and `war` plugins; Gretty 5.0.1 with Tomcat 11 |
| Web backend | Spring Framework/MVC `7.0.+`, Spring Data `4.0.+`, Spring Security `7.1.+`; servlet application initialized by `SpringAppInitializer`, without Spring Boot |
| Jakarta APIs | Servlet 6.1, JSP 4.0 and `jakarta.persistence` / `jakarta.validation` imports; Java SE APIs such as `javax.sql` still retain their names |
| Persistence | EclipseLink `5.0.0-B13`, Spring Data JPA, HikariCP; existing JDBC, `SimpleQuery` and `ActiveRecord` code remains in use. Hibernate Validator provides validation, not the JPA implementation |
| Code generation / aspects | Lombok, MapStruct and AspectJ post-compile weaving configured in Gradle |
| Server views | Thymeleaf 3.1 (`thymeleaf-spring6` integration artifact), JSP/taglibs and FreeMarker; legacy Struts/Stripes paths coexist with Spring MVC |
| JSON | Spring MVC uses Jackson 3 (`tools.jackson`); Jackson 2 (`com.fasterxml.jackson`) remains in other code. Match the converter/API used by the caller |
| Administration UI | JavaScript, jQuery 3, Bootstrap 5, DataTables/Editor, jsTree, native custom elements, Tabler icons and amCharts 5 |
| Frontend build | npm + Rspack; SCSS and Pug sources in `src/main/webapp/admin/v9`; `dist/` is generated |
| Tests | JUnit Jupiter on the JUnit 6 BOM, Spring Test and Mockito; CodeceptJS + Playwright for browser tests; Node's test runner for skills tooling |
| AI integration | CMS adapters use `com.webjetcms:webjet-ai`; `webjet-ai-local` is configured separately for local development and excluded from the standard client distribution classpath |

There is no root JS/TS workspace or shared Drizzle/Zod/tRPC type pipeline. Do not introduce React, Next.js, Vue or a new build system as an assumed project convention. Follow the implementation of the affected module.

## Repository map

| Path | Use |
| --- | --- |
| `src/main/java/sk/iway/iwcm/` | CMS services, legacy APIs, entities and REST controllers; feature applications in `components/` |
| `src/main/java/sk/iway/webjet/v9/` | Administration Spring/JPA configuration and related integration |
| `src/main/webapp/admin/v9/src/`, `views/` | Administration JS/SCSS, web components, Pug layouts and HTML/Thymeleaf pages |
| `src/main/webapp/components/` | Component JSPs, HTML views, `modinfo.properties` and assets |
| `src/main/webapp/apps/` | Application views, including Spring/Thymeleaf templates and custom app editor HTML |
| `src/main/webapp/templates/` | Website templates; distinguish tracked core fixtures from customer or ignored local templates |
| `src/main/resources/` | Poolman datasource profiles, persistence configuration, logging and classpath resources |
| `src/main/webapp/WEB-INF/classes/` | Core translation bundles (`text*.properties`) and webapp configuration |
| `src/main/webapp/WEB-INF/sql/` | Versioned XML database updates, including `autoupdate.xml` and `autoupdate-webjet9.xml` |
| `src/test/java/` | Java unit and database/integration tests |
| `src/test/webapp/` | Browser tests, page helpers, screenshots and feature-video tooling; separate npm installation |
| `docs/sk/` | Slovak product/developer documentation; parallel language trees exist under `docs/` |
| `.agents/skills/` | Maintained task-specific `wj-*` skills |
| `.github/scripts/`, `.github/workflows/skills.yml` | Skills export, setup tests and distribution workflow |

## Choose the relevant reference

| Task | Read |
| --- | --- |
| Local setup, commands, tests, contribution boundaries | [Working guide](CONTRIBUTING.md) |
| Startup, configuration, persistence, migrations, audit | [System](AGENTS-system.md) |
| Admin CRUD, columns, filtering, validation, import/export | [DataTable](AGENTS-datatable.md) |
| Page editing, saving, approval and scheduled publishing | [Editor](AGENTS-editor.md) |
| Pages, folders, templates, history and rendering | [Doc](AGENTS-doc.md) |
| Authentication, passwords, user groups and permissions | [Users](AGENTS-users.md) |
| AppStore applications, JSP components, dashboard widgets, AI | [Components](AGENTS-components.md) |
| Domain-aware helpers, files and shared application logic | [Common](AGENTS-common.md) |
| Core utilities, parameters, translation and escaping | [Utils](AGENTS-utils.md) |
| Administration layout, styles, frontend sources and interaction | [Design](AGENTS-design.md) |

Use an installed `wj-*` skill when it fits the task. The skill catalog and each `SKILL.md` define the workflow; this index does not replace them. In customer projects, do not assume every companion skill or core test helper is installed.

## Rules that affect most changes

- Trace the existing request/service/persistence path before editing. Reuse the relevant framework extension point; keep unrelated code and formatting unchanged.
- Match the surrounding Java and JavaScript style. Write new code comments, JavaDoc/JSDoc and test descriptions in English. Preserve localized UI strings, fixtures and assertions that intentionally check translated text; documentation language follows its target folder.
- Read runtime configuration through `Constants` and translations through `Prop`; see the system and utilities references for lifecycle and bundle selection. Do not hardcode a customer installation name or domain.
- Check permissions on the server and preserve CSRF protection. Menu visibility and `noperms` CSS do not authorize an operation.
- Distinguish the selected administration domain, public-request domain and MultiWeb tenant ID. A repository interface alone does not scope every query or authorize access to an ID.
- Follow the complete save workflow. Direct entity saves can bypass permission checks, history, scheduling, related records, audit or cache invalidation.
- Treat JSON serialization, HTML escaping, HTML sanitization and URL/path validation as separate operations. `JsonTools.prepare4Json` is not a general security boundary.
- Use the project's real database and existing test setup for database behavior. Do not add H2 as a substitute. If prerequisites are missing, report the verification limitation.
- Verify only the relevant build/test paths and explain any checks that could not run. Documentation-only changes do not need a CMS server or a full browser suite.

## Keeping this knowledge current

Verify behavior in source before adding a rule. Prefer a short explanation plus an exact class, method or source path over a copied API catalog. Keep each topic focused and normally below 150 lines. When adding or removing a topic, update this index, `AGENTS-NAV.md` and the core root `AGENTS.md`.

Validate links and run the skills export after changing these references; [the working guide](CONTRIBUTING.md#skills-and-knowledge-validation) contains the commands. Core commands, release bookkeeping and fixture exclusions do not automatically apply to customer projects.
