# AGENTS: Configuration and infrastructure

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

Paths below refer to the current core checkout. In customer projects, verify the installed CMS version and the project's own Spring, persistence and configuration setup before using these APIs.

## Source map

| Concern | Entry points |
| --- | --- |
| Runtime configuration and defaults | `src/main/java/sk/iway/iwcm/Constants.java`, `src/main/java/sk/iway/iwcm/system/ConstantsV9.java` |
| Persisted configuration | `src/main/java/sk/iway/iwcm/system/ConfDB.java`, `src/main/java/sk/iway/iwcm/components/configuration/` |
| Servlet and Spring startup | `src/main/java/sk/iway/iwcm/InitServlet.java`, `src/main/java/sk/iway/iwcm/system/spring/SpringAppInitializer.java` |
| MVC converters and views | `src/main/java/sk/iway/iwcm/system/spring/BaseSpringConfig.java`, `src/main/java/sk/iway/webjet/v9/V9SpringConfig.java` |
| JPA configuration | `src/main/java/sk/iway/webjet/v9/V9JpaDBConfig.java`, `src/main/java/sk/iway/iwcm/system/spring/BaseJpaDBConfig.java` |
| Entity audit | `src/main/java/sk/iway/iwcm/system/adminlog/AuditEntityListener.java` |
| Application events | `src/main/java/sk/iway/iwcm/system/spring/events/` |
| Database updates | `src/main/java/sk/iway/iwcm/system/UpdateDatabase.java`, `src/main/webapp/WEB-INF/sql/` |

## Configuration

- Read runtime values through `Constants.getString`, `getInt`, `getBoolean` and the other typed accessors. Check the definition and consumers of a key before changing its value or default.
- `ConstantsV9` adds core defaults and module metadata; it is not a separate configuration store. Customer defaults belong in the target project's established initialization/configuration files.
- `ConfDB.setName(name, value)` updates the runtime value and, except for local-only keys, its database override. The overload with `refreshCluster` can notify other nodes. `ConfDB.setRuntimeValue` changes only the current node; `Constants.set*` alone is not a persistence API.
- Follow `ConfigurationService` for permission-filtered configuration editing, removal of overrides and display of default/stored/runtime differences. Do not expose raw configuration values through a new generic endpoint.
- A changed runtime value does not automatically rebuild consumers initialized at startup. Inspect the consumer's refresh/restart requirements instead of assuming that a call to `Tools.reinitialize()` reloads the application.

## Spring and persistence

- Startup registers explicit configuration classes and scan packages. `SpringAppInitializer` reads `springAddPackages`; `V9JpaDBConfig` separately lists repository packages. Adding a Java package alone does not prove that its beans, entities and repositories will be discovered.
- Current core uses Jakarta persistence/validation/servlet APIs, Spring Data JPA and EclipseLink. Follow the installed version in customer code; do not substitute Hibernate-specific annotations or copy `javax` imports from an older example without checking compatibility.
- `V9JpaDBConfig` defines `webjet2022EntityManager` and `webjet2022TransactionManager` against the `iwcm` datasource. Additional datasources and their transaction managers must be resolved from the project configuration.
- `src/main/java/sk/iway/iwcm/system/jpa/JpaTools.java` provides both request-managed legacy entity managers and Spring-managed lookup (`getSpringEntityManager(Class<?>)`). Existing JDBC/`SimpleQuery`, ActiveRecord and Spring repositories coexist; preserve the transaction and connection ownership of the path being changed.
- A repository save does not make a multi-step service workflow atomic. Use the transaction manager for the actual persistence unit when a service requires a transaction.
- `UpdateDatabase` processes XML update files, including installation-specific files. Follow the target project's existing migration convention and supported databases; core SQL filenames are not customer defaults.

## Domain and request scope

- `src/main/java/sk/iway/iwcm/common/CloudToolsForCore.java` provides `getDomainName()`, `getDomainId()` and SQL scope helpers. Their behavior depends on deployment/configuration; some models, notably users outside cloud deployments, have different scope rules.
- `DomainIdRepository` supplies explicitly scoped query methods. It does not inject a domain filter into every repository operation. The conditional DataTable integration is documented in [DataTable](AGENTS-datatable.md); custom queries and services must preserve the intended scope themselves.
- `src/main/java/sk/iway/iwcm/SetCharacterEncodingFilter.java` manages request metadata and legacy entity-manager cleanup. Do not retain request/user/domain state in singleton fields or assume it is available in a background thread.

## Audit and events

- JPA auditing is opt-in: attach `@EntityListeners(AuditEntityListener.class)` and choose an audit type with `@EntityListenersType(Adminlog.TYPE_...)`. The listener and audit-type annotation are in `sk.iway.iwcm.system.adminlog`.
- The listener records create/update/delete activity through `Adminlog`; configuration such as `auditJpaDisabledEntities` and `auditHideProperties` affects what is recorded. Direct SQL and bulk updates do not acquire entity-listener auditing automatically.
- Use the feature's established audit path for changes outside entity lifecycle callbacks. Keep credentials and verification secrets out of audit messages.
- `WebjetEvent<T>` and Spring `@EventListener` support feature callbacks. DataTable lifecycle events have their own types in `system/datatable/events/`; inspect the actual publisher and payload before adding a listener.

## Security, translation and serialization

- `src/main/java/sk/iway/iwcm/system/spring/SpringSecurityConf.java` configures authentication and delegates additional rules to Spring configuration classes. Endpoint permissions also use `WebjetSecurityService`; see [Users](AGENTS-users.md). Do not infer authorization from an `/admin/` URL or a hidden button.
- Use `src/main/java/sk/iway/iwcm/i18n/Prop.java` (`Prop.getInstance(request).getText(key)`) for localized messages. `system/translation/` implements translation services; it is not the language-bundle loader.
- Match the existing serialization path. MVC converter settings are in `BaseSpringConfig`; standalone helpers may use different Jackson APIs. Return objects through the configured converter instead of manually escaping every field as JSON.
- Bean Validation and DataTable error handling have separate entry points; use the feature's existing response contract instead of introducing a universal `ApiResponse` wrapper.
