---
description: "Scaffold a new WebJET CMS AppStore application (page component) with all required files — Java class, Thymeleaf view, and modinfo.properties."
agent: "agent"
argument-hint: "App name and purpose, e.g. 'WeatherWidget - displays weather forecast for a configured city'"
---

# Create a new WebJET CMS AppStore Application

Use the `/wj-appstore-app` skill to follow the full procedure.

## Requirements

Create a complete AppStore application based on user input. Generate all required files:

1. **Java class** with `@WebjetComponent`, `@WebjetAppStore`, extending `WebjetComponentAbstract`
2. **Thymeleaf view template** (`.html` file)
3. **modinfo.properties** for permissions and menu config

## Questions to Ask

Before generating code, confirm:
- **App name** — Java class name and package (e.g., `sk.iway.basecms.weather.WeatherApp`)
- **Purpose** — What does the app display on the page?
- **Parameters** — What configurable fields should the editor expose? (e.g., city name, units, display style)
- **Icon** — TablerIcons icon name (browse at https://tabler.io/icons), default: `ti ti-app-window`
- **Data source** — Does it need a JPA entity/repository, or is it view-only/external API?

## Output Structure

Generate files following this layout:

```
src/main/java/sk/iway/{package}/{AppName}.java
src/main/webapp/apps/{appname}/mvc/view.html
src/main/webapp/apps/{appname}/modinfo.properties
```

## Rules

- Use fully-qualified class name in `@WebjetComponent`
- Use wrapper types (`Boolean`, `Integer`) not primitives for parameters
- Add `@JsonIgnore` on any injected repository fields
- Use `@Getter` and `@Setter` from Lombok
- Use Jakarta imports (`jakarta.persistence.*`)
- Translation keys follow pattern `components.{appname}.{field}`
- View paths returned without `.html` extension
- Include `@DataTableColumn` annotations for all configurable parameters
