---
name: wj-appstore-app
description: 'Create WebJET CMS AppStore applications (page components) that appear in the editor application list and can be inserted into pages. Use when: creating new applications for the page editor, adding components to the AppStore, building page-insertable apps with @WebjetAppStore annotation, creating Spring MVC components with Thymeleaf views, adding application parameters with @DataTableColumn, configuring dynamic select options, or setting up multi-tab application editors.'
---

# WebJET CMS AppStore Application Skill

This skill provides complete instructions for creating an AppStore application in WebJET CMS — a page component that appears in the editor's application list and can be inserted into web pages via `!INCLUDE(...)!`.

**This is NOT for admin datatable CRUD pages** (use `wj-datatable` skill for those). AppStore apps are frontend page components rendered within the website content.

## Architecture Overview

An AppStore application consists of:

1. **Java class** — annotated with `@WebjetComponent` + `@WebjetAppStore`, extends `WebjetComponentAbstract`
2. **Thymeleaf view template** — HTML file rendered in the page
3. **modinfo.properties** — permissions and menu configuration (optional, for admin section)
4. **JPA Entity + Repository** — only if the app manages data (optional)

### Package Structure

```
src/main/java/sk/iway/{installName}/{appname}/
  └─ MyApp.java                    // Main component class

src/main/webapp/apps/{appname}/
  ├─ modinfo.properties            // Module configuration (optional)
  └─ mvc/                          // Thymeleaf templates
      ├─ list.html                 // Default view
      └─ edit.html                 // Edit form (optional)

OR for view-only components:
src/main/webapp/components/{category}/{appname}/
  └─ view.html                     // Simple view template
```

### Package Scanning

The `@WebjetAppStore` annotation is discovered in these packages:
- `sk.iway.iwcm` — standard WebJET CMS applications
- `sk.iway.{installName}` — customer apps by installation name (config variable `installName`)
- `sk.iway.{logInstallName}` — apps by logging installation name
- Packages in config variable `springAddPackages`

Apps outside `sk.iway.iwcm` are placed at the beginning of the application list (customer apps first).

## Step-by-Step: Create a Basic Application

### 1. Create the Java Class

```java
package sk.iway.basecms.myapp;

import org.springframework.ui.Model;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;

@WebjetComponent("sk.iway.basecms.myapp.MyApp")
@WebjetAppStore(
    nameKey = "components.myapp.title",
    descKey = "components.myapp.desc",
    imagePath = "ti ti-app-window",
    galleryImages = "/components/myapp/screenshot-1.jpg"
)
@Getter
@Setter
public class MyApp extends WebjetComponentAbstract {

    @DefaultHandler
    public String view(Model model) {
        model.addAttribute("message", "Hello from MyApp");
        return "/apps/myapp/mvc/view";
    }
}
```

### 2. Create the Thymeleaf View Template

Create file at `src/main/webapp/apps/myapp/mvc/view.html`:

```html
<div class="myapp-container">
    <p data-th-text="${message}">Default message</p>
</div>
```

### 3. Add Translation Keys

Add translation keys to the language-specific properties files:

- Slovak: `src/main/webapp/WEB-INF/classes/text-webjet9.properties`
- Czech: `src/main/webapp/WEB-INF/classes/text_cz-webjet9.properties`
- English: `src/main/webapp/WEB-INF/classes/text_en-webjet9.properties`

The `webjet9` suffix is project-specific and can differ by installation (for example `basecms`).

```properties
components.myapp.title=My Application
components.myapp.desc=Description of my application
```

### 4. Insert into a Page

In the page editor, select the application from the AppStore list, or manually insert:

```
!INCLUDE(sk.iway.basecms.myapp.MyApp)!
```

## @WebjetAppStore Annotation Reference

| Parameter | Description | Example |
|-----------|-------------|---------|
| `nameKey` | Translation key for application name | `"components.myapp.title"` |
| `descKey` | Translation key for description. If omitted, uses `nameKey` with `.title` replaced by `.desc` | `"components.myapp.desc"` |
| `itemKey` | Unique identifier for access rights | `"cmp_myapp"` |
| `variant` | Variant identifier when multiple apps share same `itemKey` | `"unsubscribe"` |
| `imagePath` | Icon — file path or TablerIcons CSS class (`ti ti-icon-name`) | `"ti ti-snowflake text-danger"` |
| `galleryImages` | Comma-separated screenshot paths for description | `"/components/myapp/s1.jpg,/components/myapp/s2.jpg"` |
| `componentPath` | Comma-separated JSP files (for legacy non-Spring apps) | `"/components/search/search.jsp"` |
| `domainName` | Limit display to specific domains (comma-separated) | `"www.example.com"` |
| `commonSettings` | Show the View tab for common settings (default: `true`) | `false` |
| `custom` | Mark as customer app (auto-detected from package) | `true` |
| `customHtml` | Path to additional HTML file for editor JS hooks | `"/apps/myapp/admin/editor-component.html"` |
| `hideInAppstore` | Hide from AppStore list but allow parameter editing | `true` |

## Application Parameters

Define configurable parameters as private fields with `@DataTableColumn` annotations. These are automatically rendered as editor fields and passed via `!INCLUDE(... paramName=value)!`.

```java
@WebjetComponent("sk.iway.basecms.myapp.MyApp")
@WebjetAppStore(nameKey = "components.myapp.title", imagePath = "ti ti-app-window")
@Getter
@Setter
public class MyApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.myapp.name")
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "basic", title = "components.myapp.showHeader")
    private Boolean showHeader;

    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER, tab = "basic", title = "components.myapp.itemCount")
    private Integer itemCount;

    @DataTableColumn(inputType = DataTableColumnType.DATETIME, tab = "advanced", title = "components.myapp.startDate")
    private Date startDate;

    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "advanced", title = "components.myapp.folder",
        className = "dt-tree-dir-simple")
    private String dir;

    @DataTableColumn(inputType = DataTableColumnType.JSON, tab = "advanced", title = "components.myapp.page",
        className = "dt-tree-page")
    private DocDetails targetPage;

    @DefaultHandler
    public String view(Model model) {
        model.addAttribute("app", this);
        return "/apps/myapp/mvc/view";
    }
}
```

### Supported Field Types for AppStore Parameters

| Type | Use for |
|------|---------|
| `TEXT` | String input |
| `TEXTAREA` | Multi-line text |
| `TEXT_NUMBER` | Numeric input |
| `CHECKBOX` | Boolean toggle |
| `SELECT` | Dropdown selection |
| `DATETIME` | Date and time picker |
| `JSON` with `className = "dt-tree-dir-simple"` | Directory selector |
| `JSON` with `className = "dt-tree-page"` | Page selector |
| `JSON` with `className = "dt-tree-page-array"` | Multiple pages selector |
| `JSON` with `className = "dt-tree-group"` | Folder/group selector |
| `JSON` with `className = "dt-tree-group-array"` | Multiple groups selector |
| `IFRAME` | Embed another page in a tab |
| `BASE64` | Base64-encoded text |

### Setting Root Directory for Tree Selectors

```java
@DataTableColumn(inputType = DataTableColumnType.JSON, className = "dt-tree-dir-simple",
    title = "components.gallery.dir", editor = {
    @DataTableColumnEditor(
        attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/images/gallery")
        }
    )
})
private String dir = "/images/gallery";
```

## Handler Methods

Methods in the component class are action handlers. The `@DefaultHandler` method executes when no URL parameter matches another method name.

```java
@DefaultHandler
public String view(Model model, HttpServletRequest request) {
    // Executes by default
    return "/apps/myapp/mvc/list";
}

public String edit(@RequestParam("id") long id, Model model) {
    // Executes when URL contains ?edit (or ?edit=true)
    return "/apps/myapp/mvc/edit";
}

public String saveForm(@Valid @ModelAttribute("entity") MyEntity entity,
                       BindingResult result, Model model, HttpServletRequest request) {
    // Executes when URL contains ?saveForm (typically via form submit)
    if (!result.hasErrors()) {
        repository.save(entity);
        return "redirect:" + PathFilter.getOrigPath(request);
    }
    return "/apps/myapp/mvc/edit";
}
```

You can also force a specific handler via page parameter: `!INCLUDE(... defaultHandler=save)!`

### Available Method Parameters

Handler methods can inject:
- `Model model` — for passing data to the template
- `HttpServletRequest request`
- `HttpServletResponse response`
- `@RequestParam("name") Type param` — URL query parameters
- `@Valid @ModelAttribute("entity") Entity entity` — form-bound entity
- `BindingResult result` — validation results

## Dynamic Options (getAppOptions)

Override `getAppOptions()` to dynamically populate SELECT or CHECKBOX fields in the editor:

```java
@Override
public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
    Map<String, List<OptionDto>> options = new HashMap<>();

    // Manual option list
    List<OptionDto> styleOptions = new ArrayList<>();
    styleOptions.add(new OptionDto("Pretty Photo", "prettyPhoto", null));
    styleOptions.add(new OptionDto("Photo Swipe", "photoSwipe", null));
    options.put("style", styleOptions);  // key must match field name

    // From a static method (use in @DataTableColumnEditor annotation)
    // @DataTableColumnEditorAttr(key = "method:my.package.MyController.getOptions", value = "label:value")

    // From an enumeration
    // @DataTableColumnEditorAttr(key = "enumeration:Okresne Mestá", value = "string1:string2")

    // Helper method for bean lists
    options.put("groups", addOptions(groupList, "groupName", "groupId", false));

    return options;
}
```

## Tabs (@DataTableTabs)

Split parameter fields into multiple tabs in the editor:

```java
@WebjetComponent("sk.iway.basecms.myapp.MyApp")
@WebjetAppStore(nameKey = "components.myapp.title", imagePath = "ti ti-app-window")
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "advanced", title = "editor.tab.advanced"),
    @DataTableTab(id = "componentIframe", title = "components.myapp.photos")
})
@Getter
@Setter
public class MyApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.myapp.name")
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER, tab = "advanced", title = "components.myapp.limit")
    private Integer limit;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframe", title = "&nbsp;")
    private String iframe = "/admin/v9/apps/gallery/?dir={dir}";
}
```

The `{dir}` syntax in IFRAME URLs is replaced with the actual field value.

## Initialization Methods

### init() — Called Before Every View Rendering

```java
@Override
public void init(HttpServletRequest request, HttpServletResponse response) {
    // Set up data before the handler method executes
    // Called after parameters are set from !INCLUDE(...)! tag
}
```

### initAppEditor() — Called When Opening Application Editor

```java
@Override
public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
    // Set initial values or create resources when the editor opens
    // componentRequest contains: docId, groupId, pageTitle
    String uploadSubdir = UploadFileTools.getPageUploadSubDir(
        componentRequest.getDocId(), componentRequest.getGroupId(),
        componentRequest.getPageTitle(), "/images/gallery");
}
```

## Custom HTML and JavaScript Hooks

For advanced editor behavior, set `customHtml` in `@WebjetAppStore` and create an HTML file with JavaScript functions:

```java
@WebjetAppStore(... customHtml = "/apps/myapp/admin/editor-component.html")
```

Create `src/main/webapp/apps/myapp/admin/editor-component.html`:

```html
<script>
    // Called before getting editor data from REST
    function appBeforeXhr(data) {
        console.log("Before XHR, data=", data);
    }

    // Called after retrieving data from REST
    function appAfterXhr(response) {
        console.log("After XHR, response=", response);
    }

    // Called after datatable/editor initialization
    function appAfterInit(response, componentDatatable, componentPath, isInsert) {
        // Add event listeners, modify UI
        window.addEventListener("WJ.DTE.opened", function(e) {
            $("#DTE_Field_myField").on("change", function() {
                // React to field changes
            });
        });
    }

    // Called when inserting app - can change the component path
    function appGetComponentPath(componentPath, componentDatatable) {
        let type = $("#DTE_Field_appType").val();
        return `/components/myapp/${type}.jsp`;
    }

    // Called when inserting app - can return complete custom code
    function appGetComponentCode(componentPath, params, componentDatatable, isInsert) {
        // Return custom code or null for default !INCLUDE()!
        return null;
    }

    // Called after clicking OK - can call server REST services
    async function appCodeExecute(params) {
        // Return true for success, false to prevent insertion
        return true;
    }
</script>
```

## Conditional Display (Common Settings)

The View tab (enabled by default, disable with `commonSettings = false`) provides:

### Device Filtering

Controls which devices see the application. Set via `device` parameter:
- `phone` — mobile phones only
- `tablet` — tablets only
- `pc` — desktop only
- Combinations: `phone+pc`, `phone+tablet`
- Empty/all = show on all devices

Test with URL parameter: `?forceBrowserDetector=phone`

### Logged User Filtering

Set via `showForLoggedUser` parameter:
- Empty — always show
- `onlyLogged` — show only to logged-in users
- `onlyNotLogged` — show only to non-logged users

### Cache Time

Set via `cacheMinutes` parameter. Cache is NOT used when:
- Admin is logged in (unless `cacheStaticContentForAdmin=true`)
- `cacheMinutes < 1`
- URL contains `page` parameter (except `page=1`)
- URL contains `_disableCache=true`

## Hiding Fields/Tabs (appHideFields)

Hide specific fields or tabs in certain contexts (e.g., different Page Builder blocks):

```java
@Override
public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
    // Format: field1+field2+tab_tabId1+tab_tabId2
    this.appHideFields = "dir+tab_componentIframe";
}
```

Or directly in PageParams: `!INCLUDE(sk.iway.basecms.myapp.MyApp, appHideFields=dir+tab_componentIframe)!`

- Fields: use the Java field name directly (e.g., `dir`, `style`)
- Tabs: prefix with `tab_` followed by the tab ID (e.g., `tab_componentIframe`)

## JSP Component Path Selector

For apps that map to different JSP files, use `className = "dt-app-skip"` to exclude the field from `!INCLUDE` params, or `className = "dt-app-componentPath"` to automatically set the JSP path:

```java
@DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic",
    title = "components.myapp.type", className = "dt-app-skip dt-app-componentPath",
    editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "Type A", value = "/components/myapp/type_a.jsp"),
            @DataTableColumnEditorAttr(key = "Type B", value = "/components/myapp/type_b.jsp"),
        })
    })
private String appType;
```

## Thymeleaf View Templates

### Basic View

```html
<div class="myapp">
    <h3 data-th-text="${app.name}">Default Title</h3>
    <p data-th-text="${message}">Content</p>

    <!-- Conditional rendering -->
    <div data-th-if="${app.showHeader}">
        <span data-th-text="#{components.myapp.header}">Header</span>
    </div>

    <!-- Loop -->
    <ul data-th-each="item : ${items}">
        <li data-th-text="${item.name}">Item</li>
    </ul>

    <!-- Date formatting -->
    <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(app.startDate)}"></span>

    <!-- Current page URL (for forms) -->
    <form data-th-action="${request.getAttribute('ninja').page.urlPath}" method="post">
        <!-- form fields -->
        <button type="submit" name="saveForm">Submit</button>
    </form>
</div>
```

### Accessing Application Fields in Template

The component instance is available via `model.addAttribute("app", this)`:

```html
<span data-th-text="${app.name}">name</span>
<span data-th-text="${app.itemCount}">0</span>
<span data-th-utext="${app.htmlContent}">HTML content (unescaped)</span>
```

### Translation Keys in Templates

```html
<span data-th-text="#{components.myapp.label}">Fallback text</span>
```

## modinfo.properties

Create at `src/main/webapp/apps/{appname}/modinfo.properties` for admin menu integration:

```properties
# Translation key for menu item name
leftMenuNameKey=components.myapp.title
# Permission key (must match itemKey in @WebjetAppStore)
itemKey=cmp_myapp
# Allow permission management (true = can be enabled/disabled per user)
userItem=true
# Sort order in menu
menuOrder=100
# Admin page link
leftMenuLink=/apps/myapp/admin/
# Menu icon (TablerIcons name without ti- prefix)
icon=app-window
# If true, disabled by default and must be explicitly enabled
defaultDisabled=true
# If true, appears at top of app list (customer application)
custom=true
```

## Critical Rules

1. **@WebjetComponent value must be the fully-qualified class name** — e.g., `"sk.iway.basecms.myapp.MyApp"`

2. **Request scope** — Apps are `@Scope(SCOPE_REQUEST)`. If an app appears multiple times on a page, the same instance is reused. Be careful with state.

3. **@JsonIgnore on injected repositories** — If your class has an `@Autowired` repository field, annotate it with `@JsonIgnore`. Otherwise, the repository gets serialized into JSON causing errors:
   ```java
   @JsonIgnore
   private MyRepository myRepository;
   ```

4. **Use wrapper types, not primitives** — Use `Boolean`, `Integer`, `Float` (not `boolean`, `int`, `float`) for optional parameters that may be null.

5. **Translation keys convention** — Use `components.{appname}.{field}` pattern for field titles.

6. **View path convention** — Return paths without `.html` extension: `"/apps/myapp/mvc/view"` (Thymeleaf resolves the file).

7. **Promo list** — To feature an app at the top of the AppStore, add its key to the `appstorePromo` config variable.

8. **Security annotations** — Use `@PreAuthorize` on handler methods to restrict access:
   ```java
   @PreAuthorize("@WebjetSecurityService.isLogged()")      // logged-in users
   @PreAuthorize("@WebjetSecurityService.isAdmin()")       // admins only
   @PreAuthorize("@WebjetSecurityService.isInUserGroup('editors')") // specific group
   ```

9. **Comments in English**: Write all code comments in English.

## Reference Implementations

- **Simple app with CRUD**: `src/main/java/sk/iway/basecms/contact/ContactApp.java`
- **Parameter types demo**: `src/main/java/sk/iway/demo8/DemoComponent.java`
- **Gallery with tabs + iframe**: `src/main/java/sk/iway/iwcm/components/gallery/GalleryApp.java`
- **Rating with JSP type selector**: `src/main/java/sk/iway/iwcm/components/rating/RatingApp.java`
- **Documentation**: `docs/en/custom-apps/appstore/README.md`
