---
name: wj-datatable
description: Skill for programming WebJET CMS datatable backend - JPA entity, Spring DATA repository, REST controller, frontend HTML page, modinfo.properties and automated tests. Use when creating new admin CRUD applications, adding datatable entities, REST endpoints, or modifying existing datatable-based admin pages.
---

# WebJET CMS DataTable Backend Programming Skill

This skill provides complete instructions for creating a datatable-based admin application in WebJET CMS. The architecture uses JPA entities with `@DataTableColumn` annotations, Spring DATA repositories, `DatatableRestControllerV2` REST controllers, and Thymeleaf/HTML frontends.

## Architecture Overview

A complete datatable application consists of:
1. **JPA Entity** - annotated with `@DataTableColumn` for automatic UI generation
2. **Spring DATA Repository** - extends `JpaRepository` + `JpaSpecificationExecutor`
3. **REST Controller** - extends `DatatableRestControllerV2`
4. **Frontend HTML** - uses `WJ.DataTable()` initialization
5. **modinfo.properties** - menu item and permissions configuration
6. **Automated test** - CodeceptJS test scenario

## 1. JPA Entity

### Required structure

```java
package sk.iway.basecms.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "contact")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC)
public class ContactEntity {

    @Id
    @Column(name = "contact_id")
    @GeneratedValue(generator = "WJGen_contact")
    @TableGenerator(name = "WJGen_contact", pkColumnValue = "contact")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title="components.contact.property.name")
    @NotBlank
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.street")
    private String street;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title="components.contact.property.country")
    private String country;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title="components.contact.property.contact")
    @Email
    private String contact;
}
```

### Critical Rules for Entities

- **NEVER use primitive types** (`int`, `long`, `boolean`) - always use wrapper objects (`Integer`, `Long`, `Boolean`). Primitive types break search/filtering because `ExampleMatcher` cannot use NULL for primitives.
- **Primary key must be `Long id`** - if the DB column has a different name, use `@Column(name = "actual_column_name")`.
- **Always use `@EntityListeners`** for automatic audit logging with appropriate `Adminlog.TYPE_*` constant.
- **Use `@Getter` and `@Setter`** from Lombok - do not write getters/setters manually.
- **Use Jakarta imports** (`jakarta.persistence.*`, `jakarta.validation.*`), NOT javax versions.

### DataTableColumnType Reference

Field types for `@DataTableColumn(inputType = ...)`:

| Type | Description |
|------|-------------|
| `ID` | Primary key column |
| `OPEN_EDITOR` | Text field that opens editor on click - use for main/name field |
| `TEXT` | Standard single-line text input |
| `TEXTAREA` | Multi-line text input |
| `NUMBER` | Numeric input |
| `SELECT` | Dropdown selection |
| `MULTISELECT` | Multi-value dropdown |
| `BOOLEAN` | Checkbox for true/false |
| `CHECKBOX` | Checkbox with custom values |
| `DATE` | Date picker |
| `DATETIME` | Date and time picker |
| `TIME_HM` | Time picker (hours:minutes) |
| `HIDDEN` | Hidden field (not shown in editor) |
| `DISABLED` | Read-only field |
| `QUILL` | Simple HTML editor |
| `WYSIWYG` | Full HTML editor |
| `JSON` | Directory/page tree selector |
| `DATATABLE` | Nested datatable |
| `ELFINDER` | File/link browser |
| `UPLOAD` | File upload |
| `PASSWORD` | Password field |
| `RADIO` | Radio button selection |
| `COLOR` | Color picker |
| `ICON` | Icon selector (Tabler Icons) |
| `ROW_REORDER` | Drag & drop row ordering |
| `STATIC_TEXT` | Static text display |

### @DataTableColumn Key Attributes

- `title` - Translation key for column header. If omitted, auto-generated as `components.entity_name.field_name`
- `tab` - Editor tab ID where this field appears
- `hidden` - Hide column from table (user cannot show it)
- `visible` - Hide column but user can show it via column selector
- `hiddenEditor` - Don't show field in editor
- `filter` - Set `false` to hide filter in table header
- `sortAfter` - Field name after which to position this field
- `className` - CSS classes: `hide-on-create`, `hide-on-edit`, `hide-on-duplicate`, `not-export`, `allow-html`, `wrap`, `disabled`
- `orderable` - Enable/disable column sorting (default: true)
- `perms` - Permission required to see this column
- `defaultValue` - Default value for new records (supports macros: `{currentDomain}`, `{currentDate}`)
- `renderFormat` - Display format: `dt-format-text`, `dt-format-date-time`, `dt-format-checkbox`, `dt-format-select`, `dt-format-boolean-true`, `dt-format-link`, `dt-format-image`, etc.

### @DataTableColumnEditor Attributes

```java
@DataTableColumn(inputType = DataTableColumnType.TEXT, title="my.field", editor = {
    @DataTableColumnEditor(
        type = "text",
        tab = "basic",
        attr = {
            @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
            @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
            @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "translation.key.for.headline")
        },
        options = {
            @DataTableColumnEditorAttr(key = "Label text", value = "option_value")
        }
    )
})
```

### Validation Annotations

```java
@NotBlank      // Non-empty, no whitespace only
@NotEmpty      // Non-empty, allows whitespace
@Size(min=5, max=255)  // Length constraint
@Email         // Email validation
@Pattern(regexp = "^.+@.+\\.")  // Regex validation
@DecimalMax(value="200")  // Max numeric value
@DecimalMin(value="100")  // Min numeric value
@Future        // Date must be in the future
@Past          // Date must be in the past
@NotNull       // Must not be null
```

### Nested/Editor Fields

For additional non-database fields that appear in the editor, use `@Transient` with `@DataTableColumnNested`:

```java
@DataTableColumnNested
@Transient
private MyEditorFields editorFields = null;
```

The editor fields class should extend `BaseEditorFields` and can contain:
- Additional display-only fields
- Row CSS styling via `addRowClass("css-class")`
- Status icons via `addStatusIcon("ti ti-icon-name")`
- Methods `fromEntity()` / `toEntity()` for data conversion

### Domain Separation (Multi-domain)

For multi-domain installations, add `domainId` field:

```java
@Column(name = "domain_id")
@DataTableColumn(inputType = DataTableColumnType.HIDDEN)
private Integer domainId;
```

And use `DomainIdRepository` instead of `JpaRepository` (see Repository section).

### Upgrading Database Schema

When you create a new table or modify an existing table (add/alter columns), you **must** add a new `UpdateDBBean` XML element at the **bottom** of the file `src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml` (before the closing `</object></java>` tags). Each entry must contain SQL for all four supported databases: **MySQL**, **MSSQL** (Microsoft SQL Server), **Oracle** and **PostgreSQL**.

#### XML Structure

```xml
<void method="add">
    <object class="sk.iway.iwcm.system.UpdateDBBean">
        <void property="author"><string>your_name</string></void>
        <void property="date"><string>DD.MM.YYYY</string></void>
        <void property="desc"><string>Short description of the change in English</string></void>
        <void property="mysql"><string>SQL FOR MYSQL</string></void>
        <void property="mssql"><string>SQL FOR MSSQL</string></void>
        <void property="oracle"><string>SQL FOR ORACLE</string></void>
        <void property="pgsql"><string>SQL FOR POSTGRESQL</string></void>
    </object>
</void>
```

#### Adding a Column (ALTER TABLE)

Data type differences across databases:

| Java Type | MySQL | MSSQL | Oracle | PostgreSQL |
|-----------|-------|-------|--------|------------|
| `String` (short) | `VARCHAR(255)` | `NVARCHAR(255)` | `NVARCHAR2(255)` | `VARCHAR(255)` |
| `String` (long text) | `TEXT` | `NTEXT` | `CLOB` | `TEXT` |
| `Integer` / `Long` | `INT` | `INT` | `INT` / `INTEGER` | `INT` |
| `Boolean` | `TINYINT(1)` | `BIT` | `NUMBER(1)` | `BOOLEAN` |
| `Date` (date only) | `DATE` | `DATE` | `DATE` | `DATE` |
| `Date` (datetime) | `DATETIME` | `DATETIME` | `TIMESTAMP` | `TIMESTAMP` |
| `Double` | `DOUBLE` | `FLOAT` | `NUMBER` | `DOUBLE PRECISION` |

Example — adding a VARCHAR column:

```xml
<void method="add">
    <object class="sk.iway.iwcm.system.UpdateDBBean">
        <void property="author"><string>your_name</string></void>
        <void property="date"><string>23.02.2026</string></void>
        <void property="desc"><string>contact - add column phone</string></void>
        <void property="mysql"><string>ALTER TABLE contact ADD COLUMN phone VARCHAR(255) NULL;</string></void>
        <void property="mssql"><string>ALTER TABLE contact ADD phone NVARCHAR(255) NULL;</string></void>
        <void property="oracle"><string>ALTER TABLE contact ADD (phone NVARCHAR2(255) NULL);</string></void>
        <void property="pgsql"><string>ALTER TABLE contact ADD COLUMN phone VARCHAR(255);</string></void>
    </object>
</void>
```

Key syntax differences for ALTER TABLE:
- **MySQL/PostgreSQL**: use `ADD COLUMN column_name`
- **MSSQL**: use `ADD column_name` (no `COLUMN` keyword)
- **Oracle**: use `ADD (column_name TYPE)` (parentheses required)
- **PostgreSQL**: does not use `NULL` keyword explicitly for nullable columns

#### Creating a New Table (CREATE TABLE)

Each database has different syntax for auto-increment primary keys:

**MySQL:**
```sql
CREATE TABLE my_table (
    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    domain_id INT DEFAULT 1 NOT NULL
);
```

**MSSQL:**
```sql
CREATE TABLE my_table (
    [id] [INT] IDENTITY(1,1) PRIMARY KEY NOT NULL,
    [name] [NVARCHAR](255) NOT NULL,
    [description] [NTEXT],
    [domain_id] [INT] DEFAULT 1 NOT NULL
) ON [PRIMARY]
```

**Oracle** (requires separate SEQUENCE + TRIGGER for auto-increment):
```sql
CREATE TABLE my_table (
    id INTEGER NOT NULL PRIMARY KEY,
    name NVARCHAR2(255) NOT NULL,
    description CLOB,
    domain_id INTEGER DEFAULT 1 NOT NULL
);
CREATE SEQUENCE S_my_table START WITH 1;
CREATE TRIGGER T_my_table BEFORE INSERT ON my_table
    FOR EACH ROW
        DECLARE
            val INTEGER|
        BEGIN
            IF :new.id IS NULL THEN
                SELECT S_my_table.nextval into val FROM dual|
                :new.id:= val|
            END IF|
        END|
;
```

**PostgreSQL** (requires separate SEQUENCE):
```sql
CREATE SEQUENCE S_my_table START WITH 1;
CREATE TABLE my_table (
    id INT DEFAULT NEXTVAL('S_my_table') PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    domain_id INT DEFAULT 1 NOT NULL
);
```

#### Important Rules

- **Oracle trigger syntax**: Use `|` (pipe) instead of `;` (semicolon) as statement delimiter inside trigger bodies. The semicolon at the very end terminates the whole block.
- **MSSQL uses `NVARCHAR`/`NTEXT`** for Unicode string support (not plain `VARCHAR`/`TEXT`).
- **Oracle uses `NVARCHAR2`/`CLOB`** for Unicode strings.
- **Always include all four `<void property="...">` elements** (mysql, mssql, oracle, pgsql), even if the SQL is identical — use an empty `<string></string>` only if the change genuinely does not apply to that database.
- **Multiple SQL statements** can be placed in a single `<string>` block, separated by semicolons.
- **Date format** in `<void property="date">` is `DD.MM.YYYY`.
- When creating a new table, also consider inserting a row into `pkey_generator` if the WebJET primary key generator is used (not needed when JPA `@TableGenerator` handles ID generation).

## 2. Spring DATA Repository

### Standard Repository

```java
package sk.iway.basecms.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContactRepository extends JpaRepository<ContactEntity, Long>, JpaSpecificationExecutor<ContactEntity> {

    public Page<ContactEntity> findAllByCountry(String country, Pageable pageable);
}
```

### Critical Rules for Repositories

- **Always extend both `JpaRepository<T, Long>` AND `JpaSpecificationExecutor<T>`** - the latter enables dynamic search/filter SQL generation.
- **Always use `Pageable`** parameter for pagination and sorting support.
- **Repository must be `public`** - otherwise it won't be accessible in client projects.
- For multi-domain support, extend `DomainIdRepository<T, Long>` instead of `JpaRepository`:

```java
import sk.iway.iwcm.system.datatable.spring.DomainIdRepository;

@Repository
public interface MyRepository extends DomainIdRepository<MyEntity, Long> {
}
```

`DomainIdRepository` already extends both `JpaRepository` and `JpaSpecificationExecutor`.

## 3. REST Controller

### Basic Implementation

```java
package sk.iway.basecms.contact;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@RequestMapping("/admin/rest/apps/contact/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_contact')")
@Datatable
public class ContactRestController extends DatatableRestControllerV2<ContactEntity, Long> {

    @SuppressWarnings("unused")
    private final ContactRepository contactRepository;

    @Autowired
    public ContactRestController(ContactRepository contactRepository) {
        super(contactRepository);
        this.contactRepository = contactRepository;
    }

    @Override
    public void getOptions(DatatablePageImpl<ContactEntity> page) {
        page.addOptions("country", getCountries(), "label", "value", false);
    }

    @Override
    public void beforeSave(ContactEntity entity) {
        // set values before saving, e.g. date, domainId
    }

    public static List<LabelValue> getCountries() {
        List<LabelValue> countries = new ArrayList<>();
        countries.add(new LabelValue("Slovenská republika", "sk"));
        countries.add(new LabelValue("Česká republika", "cz"));
        return countries;
    }
}
```

### Critical Rules for REST Controllers

- **Always use `@Datatable` annotation** - ensures correct error response handling.
- **Always use `@PreAuthorize`** - controls access permissions.
- **URL pattern**: `/admin/rest/apps/{appname}/` for custom apps, `/admin/rest/components/{name}` for components, `/admin/rest/settings/{name}` for settings.
- **Constructor** must call `super(repository)` or `super(repository, EntityClass.class)`. Passing the entity class enables `fetchOnCreate` (default values for new records from server).
- **Never override REST endpoint methods** with `@Override` - always override the `xxxItem` methods instead.

### Override Methods (Life Cycle Hooks)

All of these are optional. Override only what you need:

```java
// Called BEFORE saving (insert or edit)
public void beforeSave(T entity) { }

// Called AFTER saving entity
public void afterSave(T entity, T saved) { }

// Called BEFORE deleting
public boolean beforeDelete(T entity) { return true; }

// Called AFTER deleting
public void afterDelete(T entity, long id) { }

// Called BEFORE duplicating
public void beforeDuplicate(T entity) { }

// Called AFTER duplicating
public void afterDuplicate(T entity, Long originalId) { }

// Validation before save/delete
public void validateEditor(HttpServletRequest request, DatatableRequest<Long, T> target,
    Identity user, Errors errors, Long id, T entity) { }

// Permission check per item
public boolean checkItemPerms(T entity, Long id) { return true; }

// Process action button clicks
public boolean processAction(T entity, String action) { return false; }

// Set select box options (recommended way)
public void getOptions(DatatablePageImpl<T> page) { }

// Convert entity before returning via REST
public T processFromEntity(T entity, ProcessItemAction action) { return entity; }

// Convert entity before saving via REST
public T processToEntity(T entity, ProcessItemAction action) { return entity; }

// Custom search conditions (JPA Specification)
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates,
    Root<T> root, CriteriaBuilder builder) { }

// Custom sorting
public Pageable addSpecSort(Map<String, String> params, Pageable pageable) { }

// Data manipulation without repository
public T insertItem(T entity) { }
public T editItem(T entity, long id) { }
public boolean deleteItem(T entity, long id) { }
public T getOneItem(long id) { }
public Page<T> getAllItems(Pageable pageable) { }
public Page<T> searchItem(Map<String, String> params, Pageable pageable, T search) { }
```

### Select Box Options (Číselníky)

Options can be set in multiple ways:

**1. REST endpoint (recommended):**
```java
@Override
public void getOptions(DatatablePageImpl<ContactEntity> page) {
    page.addOptions("fieldName", listOfObjects, "labelProperty", "valueProperty", false);
}
```

**2. Static method reference in annotation:**
```java
@DataTableColumnEditorAttr(key = "method:sk.iway.pkg.Class.getList", value = "label:value")
```

**3. Enumeration reference:**
```java
@DataTableColumnEditorAttr(key = "enumeration:Enumerácia Meno", value = "string1:string2")
```

**4. Direct options in annotation:**
```java
@DataTableColumnEditorAttr(key = "Displayed Text", value = "stored_value")
```

### Custom Search (addSpecSearch)

For custom filtering beyond basic field-level search:

```java
@Override
public void addSpecSearch(Map<String, String> params, List<Predicate> predicates,
        Root<MyEntity> root, CriteriaBuilder builder) {

    String searchCustomField = params.get("searchCustomField");
    if (Tools.isNotEmpty(searchCustomField)) {
        predicates.add(builder.like(root.get("customField"), "%" + searchCustomField + "%"));
    }

    // ALWAYS call super to preserve built-in search functionality
    super.addSpecSearch(params, predicates, root, builder);
}
```

Helper methods from `SpecSearch<T>` class:
- `addSpecSearchUserFullName(value, jpaProperty, predicates, root, builder)` - search by user name
- `addSpecSearchPasswordProtected(value, jpaProperty, predicates, root, builder)` - search in comma-separated ID list
- `addSpecSearchPerexGroup(value, jpaProperty, predicates, root, builder)` - search by tag name
- `addSpecSearchIdInForeignTable(value, foreignTable, foreignId, foreignColumn, jpaProperty, predicates, root, builder)` - search in related table
- `addSpecSearchDocFullPath(value, jpaProperty, predicates, root, builder)` - search document by path

### Throwing Errors

```java
// General error message
throwError("translation.key.for.error");
throwError(List.of("error1", "error2"));

// Constraint violation on specific field
throwConstraintViolation("Error message text");

// In validateEditor - field-specific error
errors.rejectValue("errorField.fieldName", null, "Error message");

// Global error (not tied to field)
((BindingResult)errors).addError(new ObjectError("global", "Error message"));
```

### Force Reload After Save

```java
@Override
public void afterSave(MyEntity entity, MyEntity saved) {
    setForceReload(true); // Forces datatable to reload all data
}
```

### Redirect After Save

```java
@Override
public void afterSave(MyEntity entity, MyEntity saved) {
    setRedirect("/apps/myapp/admin/detail/?id=" + saved.getId());
}
```

### DatatableRestControllerAvailableGroups

For entities with permissions based on web page tree structure:

```java
@RestController
@Datatable
@RequestMapping("/admin/rest/media-group")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_media_group')")
public class MediaGroupRestController extends DatatableRestControllerAvailableGroups<MediaGroupBean, Long> {

    @Autowired
    public MediaGroupRestController(MediaGroupRepository repo) {
        super(repo, "id", "availableGroups");
    }
}
```

## 4. Frontend HTML Page

Place in `src/main/webapp/apps/{appname}/admin/index.html`:

```html
<script>
    var dataTable;

    window.domReady.add(function () {

        WJ.breadcrumb({
            id: "contact",
            tabs: [
                { url: "/apps/contact/admin/", title: WJ.translate("components.contact.title"), active: true }
            ]
        })

        let url = "/admin/rest/apps/contact";
        let columns = [(${layout.getDataTableColumns("sk.iway.basecms.contact.ContactEntity")})];

        dataTable = WJ.DataTable({
            url: url,
            serverSide: true,
            columns: columns,
            fetchOnCreate: true
        });
    });

</script>

<table id="dataTable" class="datatableInit table"></table>
```

### Key Frontend Rules

- **Always use `window.domReady.add()`** instead of `$(document).ready` - it waits for translation keys to load.
- **Always generate columns from Java annotations**: `[(${layout.getDataTableColumns("full.class.Name")})]`
- **URL mapping**: `/apps/{app}/admin/` maps to `src/main/webapp/apps/{app}/admin/index.html`
- Sub-pages: `/apps/{app}/admin/{subpage}` maps to `src/main/webapp/apps/{app}/admin/{subpage}.html`

### WJ.DataTable Main Options

| Option | Type | Description |
|--------|------|-------------|
| `url` | string | REST endpoint URL (without `/all` suffix) |
| `columns` | json | Column definitions (from Java annotations) |
| `serverSide` | boolean | Server-side pagination/sorting (recommended: `true`) |
| `tabs` | json | Editor tab definitions |
| `id` | string | Datatable ID (default: `datatableInit`) |
| `fetchOnCreate` | boolean | Fetch default values from server for new records |
| `fetchOnEdit` | boolean | Refresh record data before editing |
| `hideButtons` | string | Comma-separated buttons to hide: `create,edit,duplicate,remove,import,celledit` |
| `perms` | object | Permission-based button visibility: `{create:'perm1', edit:'perm2', remove:'perm3'}` |
| `order` | array | Default sort: `[[5, 'desc']]` |
| `paging` | boolean | Enable/disable pagination |
| `autoHeight` | boolean | Auto-calculate table height to fill window |

### Editor Tabs Definition

```javascript
let tabs = [
    { id: 'basic', title: WJ.translate('tab.basic'), selected: true },
    { id: 'advanced', title: WJ.translate('tab.advanced') },
    { id: 'custom', title: 'Custom Tab', content: '<div id="customContent"></div>' }
];
```

Tab options: `selected`, `className` (`hide-on-create`, `hide-on-edit`), `perms`, `hideOnCreate`, `hideOnEdit`, `content`.

### Server Actions

```javascript
// Execute action on selected rows
dataTable.executeAction("actionName");

// Execute without row selection
dataTable.executeAction("actionName", true, "Confirm text?", "Note text");
```

Handled on backend via `processAction(T entity, String action)`.

### Adding/Hiding Buttons

```javascript
// Hide button
dataTable.hideButton("create");
dataTable.hideButton("import");

// Add custom button
dataTable.button().add(3, {
    text: '<i class="ti ti-refresh"></i>',
    action: function(e, dt, node) {
        dataTable.executeAction("myAction");
    },
    init: function(dt, node, config) {
        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
    },
    className: 'btn btn-outline-secondary',
    attr: { 'title': 'My Button', 'data-toggle': 'tooltip' }
});
```

### JavaScript Module

Automatically loaded from `/apps/{app}/admin/{app}.js` as ES module:

```javascript
// /apps/myapp/admin/myapp.js
export function myFunction() { ... }
```

Accessible in HTML as `appModule.myFunction()`.

## 5. modinfo.properties

Place in `src/main/webapp/apps/{appname}/modinfo.properties`:

```properties
# Translation key for menu item name
leftMenuNameKey=components.contact.title
# Permission key
itemKey=cmp_contact
# Allow permission assignment to users
userItem=true
# Menu link URL
leftMenuLink=/apps/contact/admin/
# FontAwesome icon (https://tabler.io/icons/)
icon=address-book
# If true, disabled by default for all users
defaultDisabled=true
# If true, shown as customer app at the beginning
custom=true

# Optional submenu
#leftSubmenu1NameKey=components.contact.subpage.title
#leftSubmenu1Link=/apps/contact/admin/subpage/
```

## 6. Automated Test

Place in `src/test/webapp/tests/apps/{appname}.js`:

```javascript
Feature('contact');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/apps/contact/admin/");
});

Scenario('contact-zakladne testy', async ({ I, DataTables, DTE }) => {
    await DataTables.baseTest({
        dataTable: 'dataTable',
        perms: 'cmp_contact',
        createSteps: function(I, options) {
            DTE.fillField("zip", "85106");
        }
    });
});
```

## 7. Translation Keys

Add translations to `src/main/webapp/WEB-INF/classes/text-webjet9.properties` in Slovak language, `text_cz-webjet9.properties` for Czech and `text_en-webjet9.properties` for English:

```properties
components.contact.title=Contacts
components.contact.property.name=Name
components.contact.property.street=Street
```

After adding, reload translations by visiting `/admin/?userlngr=true` or restart the server.

## 8. Common Patterns

### Row Styling

In `BaseEditorFields` subclass:
```java
addRowClass("is-disabled");      // Red text - inactive
addRowClass("is-not-public");    // Red text - not public
addRowClass("is-default-page");  // Bold - default page
```

### Status Icons

```java
addStatusIcon("ti ti-map-pin");      // Show pinned icon
addStatusIcon("ti ti-lock-filled");  // Show lock icon
```

### User Name Display with Search

Use `@Transient` field with lazy loading:

```java
@Transient
@DataTableColumn(inputType = DataTableColumnType.TEXT, title="audit.user_full_name", orderable = false,
    editor = { @DataTableColumnEditor(attr = {
        @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
private String userFullName;

public String getUserFullName() {
    if (userFullName == null && userId != null && userId.intValue() > 0) {
        UserDetails user = UsersDB.getUserCached(userId.intValue());
        if (user != null) userFullName = user.getFullName();
        else userFullName = "";
    }
    return userFullName;
}
```

Search by `userFullName` is automatically handled by `DatatableRestControllerV2.addSpecSearch` when repository extends `JpaSpecificationExecutor`.

### Export/Import

Export and import is automatically available. To hide:
```javascript
dataTable.hideButton("import");
dataTable.hideButton("export");
```

To exclude a column from export:
```java
@DataTableColumn(className = "not-export")
```

### Table Footer Summary

```javascript
dataTable = WJ.DataTable({
    url: "...",
    summary: {
        mode: "all",  // "all", "visible", or "datatable"
        columns: ["visits", "sessions"],
        title: "Total"
    }
});
```

## 9. jUnit Tests

jUnit tests are in `src/test/java`. When testing JPA entities:

- **Always use Repository classes** to save and load entities - do not assume operations work without real database persistence.
- Use `@Autowired` to inject repositories in test classes.
- Verify CRUD operations (create, read, update, delete) through the repository layer.

## 10. Checklist for New Datatable Application

1. [ ] Create JPA entity with `@DataTableColumn` annotations (use `Long` not `long`)
2. [ ] Create Spring DATA repository extending `JpaRepository` + `JpaSpecificationExecutor`
3. [ ] Create REST controller extending `DatatableRestControllerV2` with `@Datatable`, `@PreAuthorize`, `@RequestMapping`
4. [ ] Create frontend HTML in `apps/{name}/admin/index.html`
5. [ ] Create `modinfo.properties` in `apps/{name}/`
6. [ ] Add translation keys to `text-webjet9.properties`, `text_cz-webjet9.properties`, `text_en-webjet9.properties`
7. [ ] Create automated test in `src/test/webapp/tests/apps/{name}.js` using skill [codeceptjs](../codeceptjs/SKILL.md).
8. [ ] Add changelog entry in `docs/sk/CHANGELOG-2026.md`
9. [ ] Check `docs/sk/ROADMAP.md` for planned item and mark as done
10. [ ] Verify export/import functionality works correctly