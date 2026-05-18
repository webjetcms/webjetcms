---
description: "Use when creating or modifying classes with @WebjetAppStore annotation, building page components for the editor AppStore, or working with WebjetComponentAbstract subclasses. Suggests using the wj-appstore-app skill for complete application scaffolding."
applyTo: "src/main/java/**/*App.java, src/main/java/**/*Component.java"
---

# WebJET AppStore Application Guidelines

When creating or modifying a class annotated with `@WebjetAppStore` or extending `WebjetComponentAbstract`:

- Use the `/wj-appstore-app` skill for complete scaffolding of new AppStore applications.
- The `@WebjetComponent` value **must** be the fully-qualified class name.
- Always use wrapper types (`Boolean`, `Integer`) not primitives for application parameters.
- Annotate injected repositories with `@JsonIgnore` to prevent JSON serialization errors.
- Use `@DataTableColumn` on private fields to auto-generate editor UI for application parameters.
- Return view paths without `.html` extension (e.g., `"/apps/myapp/mvc/view"`).
- Override `getAppOptions()` for dynamic SELECT/CHECKBOX options in the editor.
- Use `@DefaultHandler` on the main view method; other public methods become URL-parameter handlers.

## Security

- Use `@PreAuthorize("@WebjetSecurityService.isLogged()")` for logged-in-only methods.
- Use `@PreAuthorize("@WebjetSecurityService.isAdmin()")` for admin-only methods.

## File Conventions

| File | Location |
|------|----------|
| Java class | `src/main/java/sk/iway/{installName}/{appname}/` |
| View templates | `src/main/webapp/apps/{appname}/mvc/` |
| modinfo.properties | `src/main/webapp/apps/{appname}/modinfo.properties` |
| Custom editor HTML | `src/main/webapp/apps/{appname}/admin/editor-component.html` |
