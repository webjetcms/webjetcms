---
name: wj-codeceptjs
description: Skill for writing E2E (end-to-end) tests using CodeceptJS with Playwright in WebJET CMS. Use when creating new automated browser tests, testing datatable CRUD operations, testing import functionality, verifying UI elements, testing permissions/rights, or writing test scenarios for admin pages.
---

# CodeceptJS E2E Testing Skill for WebJET CMS

## Overview

WebJET CMS uses **CodeceptJS** with **Playwright** for automated E2E browser testing. Tests are written in JavaScript and located in `src/test/webapp/tests/` directory, organized by modules/applications.

## Project Structure

```
src/test/webapp/
├── codecept.conf.js          # Main configuration (helpers, plugins, login, etc.)
├── steps_file.js             # Custom I.* step definitions
├── custom_helper.js          # Custom Playwright helper methods
├── pages/
│   ├── DataTables.js         # Automated datatable CRUD testing (baseTest, importTest)
│   ├── DT.js                 # DataTable helper functions (filter, waitForLoader, etc.)
│   ├── DTE.js                # DataTable Editor helper functions (save, cancel, fillField, etc.)
│   ├── Document.js           # Document/page helper functions (switchDomain, screenshots, etc.)
│   ├── Browser.js            # Browser detection (isChromium, isFirefox)
│   ├── TempMail.js           # Temp email testing support
│   ├── Apps.js               # Application-specific helpers
│   ├── i18n.js               # Internationalization helpers
│   └── buttons.js            # Button selector definitions
├── screenshots/
│   └── base/                 # Reference screenshots for visual testing
└── tests/
    ├── admin/                # Admin login/logout tests
    ├── apps/                 # Application tests (banner, gallery, dmail, etc.)
    ├── components/           # Component tests (templates, insert-script, etc.)
    ├── settings/             # Settings tests (translation-keys, etc.)
    ├── webpages/             # Web page tests
    └── pentests/             # Penetration/security tests
```

## Critical Rules

1. **Feature naming**: Always use the format `adresár.podadresár.meno-súboru` matching the test file path:
   ```javascript
   // File: tests/apps/banner/banner.js
   Feature('apps.banner.banner');
   ```

2. **Testing data**: All created test objects **MUST** contain text `autotest` for identification. Use `I.getRandomText()` for unique suffixes:
   ```javascript
   var randomNumber = I.getRandomText();
   auto_name = 'name-autotest-' + randomNumber;
   ```

3. **No fixed waits**: Never use `I.wait(N)` for synchronization. Always use `waitFor*` methods:
   ```javascript
   // BAD
   I.click("Pridať");
   I.wait(1);
   I.see("test-text");

   // GOOD
   I.click("Pridať");
   I.waitForText("test-text", 10, container);
   ```

4. **Every save must wait**: After clicking Save, always wait for loader:
   ```javascript
   DTE.save();        // internally waits for DTE loader + DT loader
   // or manually:
   I.click("Uložiť");
   DTE.waitForLoader();
   DT.waitForLoader();
   ```

5. **Comments in English**: Write all code comments in English.

6. **Permissions testing is mandatory**: For datatable tests, always include `perms` parameter.

## Authentication

Use the pre-configured `login` injection in `Before` blocks:

```javascript
Feature('apps.my-feature');

Before(({ login }) => {
    login('admin');
});

Scenario('my test', ({ I }) => {
    I.amOnPage("/admin/v9/apps/my-app/");
    // ... test code
});
```

The `login('admin')` function handles logging in as `tester` user, manages cookies, and sets default window size. It is defined in `codecept.conf.js` autoLogin plugin.

## Running Tests

```shell
cd src/test/webapp/

# Run all tests
npm run all

# Run specific test with pause on fail
npm run pause tests/apps/banner/banner.js

# Run scenarios tagged @current
npm run current

# Run on different URL, headless
CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npm run all
```

## Available Helper Functions

### I.* (Standard Playwright + Custom WebJET)

**Core Playwright methods:**
- `I.amOnPage(url)` - navigate to page
- `I.click(locator, context?)` - click element
- `I.forceClick(locator)` - force click (use for custom checkboxes)
- `I.fillField(locator, value)` - fill input field
- `I.see(text, context?)` / `I.dontSee(text, context?)` - assert text visibility
- `I.seeElement(locator)` / `I.dontSeeElement(locator)` - assert element visibility
- `I.selectOption(locator, value)` - select dropdown option
- `I.waitForText(text, timeout, context?)` - wait for text to appear
- `I.waitForElement(locator, timeout)` - wait for element
- `I.waitForVisible(locator, timeout)` - wait for element to be visible
- `I.waitToHide(locator, timeout)` - wait for element to hide
- `I.pressKey(key)` - press keyboard key
- `I.executeScript(fn)` - execute JavaScript in browser
- `I.saveScreenshot(filename)` - save screenshot
- `I.seeInField(locator, value)` - check field value
- `I.grabTextFrom(locator)` - get text content (async)
- `I.grabNumberOfVisibleElements(locator)` - count visible elements (async)
- `I.grabCssPropertyFrom(locator, prop)` - get CSS property (async)

**Custom WebJET methods:**
- `I.getRandomText()` - returns unique random text suffix for test data
- `I.getRandomTextShort()` - returns shorter random text
- `I.formatDateTime(timestamp)` - format timestamp to date string
- `I.seeAndClick(selector)` - wait for element then click
- `await I.clickIfVisible(selector)` - click if visible, skip if not (no error)
- `I.verifyDisabled(selector)` - verify field is disabled
- `I.wjSetDefaultWindowSize()` - reset window to default size
- `I.waitForTime(time)` - wait until specific timestamp
- `I.toastrClose()` - close toastr notification
- `I.clickCss(selector, parent?)` - click using CSS selector (faster than `I.click({css: ...})`)
- `I.forceClickCss(selector, parent?)` - force click using CSS selector
- `I.jstreeClick(name)` - click node in jstree (important for web pages where link name matches directory)
- `I.jstreeNavigate(pathArray)` - navigate jstree by path array, e.g. `I.jstreeNavigate(["Aplikácie", "Bannerový systém"])`
- `I.createFolderStructure(randomNumber)` - create test folder structure
- `I.deleteFolderStructure(randomNumber)` - delete test folder structure
- `I.logout()` - logout current user
- `I.closeOtherTabs()` - close all tabs except current
- `I.openNewTab()` - open new browser tab
- `I.closeCurrentTab()` - close current tab
- `I.getDefaultPassword()` - get test user password
- `I.getDefaultDomainName()` - get default domain name
- `await I.getDataTableColumns(dataTableName)` - get datatable columns definition
- `await I.getDataTableId(dataTableName)` - get datatable DOM ID
- `await I.getTotalRows()` - get total rows in datatable
- `I.dtWaitForLoader()` - shortcut for DT.waitForLoader()
- `I.dtFilterSelect(name, value)` - shortcut for DT.filterSelect()
- `I.dtResetTable(name)` - reset datatable to default state

### DT.* (DataTable functions)

Implemented in `src/test/webapp/pages/DT.js`:

- `DT.waitForLoader(name?)` - wait for datatable processing indicator to disappear. Usage: `DT.waitForLoader()` or `DT.waitForLoader("bannerDataTable")`
- `DT.filter(name, value, type?)` - set text filter on column. Type options: `'contains'`, `'startwith'`, `'endwith'`, `'equal'`
- `DT.filterContains(name, value)` - shortcut for filter with contains type
- `DT.filterSelect(name, value)` - set select filter value, e.g. `DT.filterSelect('cookieClass', 'Neklasifikované')`
- `DT.deleteAll(name?)` - delete all currently visible records (always filter first!)
- `DT.checkPerms(perms, url, datatableId?)` - test permission enforcement on datatable
- `DT.checkTableCell(tableId, row, col, value)` - verify cell value (1-indexed)
- `DT.checkTableRow(tableId, row, values)` - verify entire row values
- `DT.addContext(key, selector)` - add new context for button selectors
- `await DT.showColumn(name, dataTable)` - show a hidden column

### DTE.* (DataTable Editor functions)

Implemented in `src/test/webapp/pages/DTE.js`:

- `DTE.waitForLoader()` - wait for editor save operation to finish
- `DTE.waitForEditor(name?)` - wait for editor dialog to appear. Default datatable: `datatableInit`
- `DTE.waitForModalClose(modalId)` - wait for editor modal to close
- `DTE.save(name?)` - click Save button and wait for save+reload
- `DTE.saveBubble()` - save via bubble editor
- `DTE.cancel(name?, clickTopButton?)` - click Cancel/Close button
- `DTE.selectOption(name, text)` - select value in dropdown, e.g. `DTE.selectOption("bannerType", "HTML kód")`
- `await DTE.selectOptionMulti(name, values)` - select multiple dropdown values (async)
- `DTE.fillField(name, value)` - fill field by backend field name (uses `#DTE_Field_name` selector)
- `DTE.appendField(name, value)` - append text to field (workaround for I.appendField issues)
- `DTE.fillQuill(name, value)` - fill Quill editor field
- `DTE.fillCkeditor(htmlCode)` - set HTML in CKEditor
- `DTE.fillCleditor(parentSelector, value)` - fill cleditor WYSIWYG

### Document.* (Document/Page functions)

- `Document.switchDomain(domain)` - switch domain, e.g. `Document.switchDomain("test23.tau27.iway.sk")`
- `Document.setConfigValue(name, value)` - set configuration variable
- `Document.resetPageBuilderMode()` - reset editor mode
- `Document.notifyClose()` - close toastr notification
- `Document.notifyCheckAndClose(text)` - verify and close toastr notification
- `Document.editorComponentOpen()` - open component settings in page editor
- `Document.editorComponentOk()` - click OK in component settings
- `Document.scrollTo(selector)` - scroll to element
- `await Document.compareScreenshotElement(selector, filename, width?, height?, tolerance?)` - visual comparison test

### Browser.* (Browser detection)

- `Browser.isChromium()` - returns true if running in Chromium
- `Browser.isFirefox()` - returns true if running in Firefox

### Assert functions (codeceptjs-chai)

```javascript
I.assertEqual(actual, expected, message?);
I.assertNotEqual(actual, expected, message?);
I.assertContain(target, value, message?);
I.assertNotContain(target, value, message?);
I.assertStartsWith(target, value, message?);
I.assertEndsWith(target, value, message?);
I.assertTrue(value, message?);
I.assertFalse(value, message?);
I.assertAbove(actual, expected, message?);
I.assertLengthOf(target, length, message?);
I.assertEmpty(target, message?);
```

## Locators

CodeceptJS supports multiple locator types:

```javascript
// By text (default for most methods)
I.click("Submit");
I.see("Welcome");

// By CSS
I.click({css: "button.btn-primary"});

// By XPath
I.click({xpath: "//button[@type='submit']"});

// By name attribute
I.fillField({name: "username"}, "tester");

// By ID (permalink)
I.click({permalink: '/foo'});  // matches id="foo"

// By class
I.click({class: 'foo'});

// Using locate() builder
I.click(locate("a").withText("Click me"));
I.click(locate(".item").withChild("span.icon"));
I.click(locate("tr").withDescendant("td").withText("Banner"));

// Within context
within("div.modal-dialog", () => {
    I.fillField("name", "test");
    I.click("Save");
});

// Or with context parameter
I.click("Save", "div.modal-dialog");
```

## Writing a DataTable CRUD Test (baseTest)

The `DataTables.baseTest()` function provides automated CRUD testing. It:
1. Creates a new record (tests required field validation)
2. Searches for the created record
3. Edits the record
4. Tests cross-domain record separation
5. Searches for the edited record
6. Deletes the record
7. Verifies audit log entries

### Minimal Example

```javascript
Feature('apps.my-app');

Before(({ login }) => {
    login('admin');
});

Scenario('zakladne testy @baseTest', async ({ I, DataTables }) => {
    I.amOnPage("/admin/v9/apps/my-app/");

    await DataTables.baseTest({
        dataTable: 'myAppTable',     // JavaScript variable name in the web page
        perms: 'cmp_my_app'          // permission name to test (MANDATORY)
    });
});
```

**IMPORTANT**: The `dataTable` value must match the **JavaScript variable name** defined in the page's pug file with `var` (not `let`):
```javascript
var myAppTable;
window.domReady.add(function() {
    myAppTable = WJ.DataTable({ ... });
});
```

### Full Example with Custom Steps

```javascript
Scenario('zakladne testy @baseTest', async ({ I, DataTables, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/my-app/");

    await DataTables.baseTest({
        dataTable: 'myAppTable',
        perms: 'cmp_my_app',

        // Override required fields (otherwise auto-detected from columns with required: true)
        requiredFields: ['name', 'email'],

        // Custom values for fields with specific format (e.g. email)
        testingData: {
            email: "test@autotest-example.com"
        },

        // Steps executed when creating new record
        createSteps: function(I, options, DT, DTE) {
            I.click({css: "#pills-dt-myAppTable-advanced-tab"});
            I.fillField("div.DTE_Field_Name_description input", "Test description");
            I.click({css: "#pills-dt-myAppTable-main-tab"});
        },

        // Steps executed after record is saved
        afterCreateSteps: function(I, options, requiredFields, DT, DTE) {
            // Can modify requiredFields or testingData for subsequent steps
        },

        // Steps executed when editing existing record
        editSteps: function(I, options, DT, DTE) {
            I.fillField("div.DTE_Field_Name_description input", "Edited description");
        },

        // Steps for searching edited record
        editSearchSteps: function(I, options, DT, DTE) {
            I.see(`${options.testingData[0]}-chan.ge`, "div.dt-scroll-body");
        },

        // Steps before deleting record
        beforeDeleteSteps: function(I, options, DT, DTE) {
            // Verify something before deletion
        },

        // Optional settings
        skipRefresh: false,              // don't refresh page after create
        skipSwitchDomain: false,         // skip cross-domain test
        switchDomainName: "mirroring.tau27.iway.sk",  // custom domain for cross-domain test
        skipDuplication: false,          // skip duplication test
        container: null,                 // CSS selector for nested datatable container
        containerModal: null,            // CSS selector for editor modal container
        extraWait: null                  // extra wait time in seconds
    });
});
```

### baseTest Options Reference

**Required:**
- `dataTable` - JavaScript variable name of the datatable in the web page

**Mandatory (for security testing):**
- `perms` - permission name for REST service access testing (use `'-'` to skip for nested tables)

**Optional:**
- `requiredFields` - array of required field names (auto-detected if not specified)
- `testingData` - object with custom values for specific fields
- `container` - CSS selector for datatable container (for nested datatables)
- `containerModal` - CSS selector for editor modal (for nested datatables)
- `skipRefresh` - `true` to skip page refresh after create
- `skipSwitchDomain` - `true` to skip cross-domain separation test
- `switchDomainName` - custom domain name for cross-domain test
- `skipDuplication` - `true` to skip duplication test
- `createSteps` - `function(I, options, DT, DTE)` - custom create steps
- `afterCreateSteps` - `function(I, options, requiredFields, DT, DTE)` - post-create steps
- `editSteps` - `function(I, options, DT, DTE)` - custom edit steps
- `editSearchSteps` - `function(I, options, DT, DTE)` - custom search-after-edit steps
- `beforeDeleteSteps` - `function(I, options, DT, DTE)` - pre-delete steps
- `extraWait` - extra wait time in seconds

**Automatically set after initialization:**
- `options.id` - datatable DOM ID
- `options.url` - current page URL
- `options.testingData` - array of filled required field values
- `options.columns` - columns definition from datatable

### How Required Fields Are Generated

Test values are auto-generated with `autotest` text:
- Regular fields: `{fieldName}-autotest-{randomText}`
- Email fields: `{fieldName}-autotest-{randomTextShort}@onetimeusemail.com`
- Custom values from `testingData` override auto-generation

## Writing a DataTable Import Test (importTest)

The `DataTables.importTest()` function tests Excel import functionality:
1. Verifies table has no existing test data
2. Imports Excel file and verifies data
3. Edits required fields (adds `-changed` suffix) and verifies
4. Re-imports with matching to verify data update

### Example

```javascript
Scenario('import test', async ({ I, DataTables }) => {
    I.waitForText('Zoznam skriptov', 5);
    await DataTables.importTest({
        dataTable: 'insertScriptTable',
        requiredFields: ['name', 'position'],
        file: 'tests/components/insert-script.xlsx',
        updateBy: 'Názov / popis skriptu - name',
        rows: [
            {
                name: "Test import"
            }
        ]
    });
});
```

### Import Test Options

- `file` - path to .xlsx file with test data (relative to `src/test/webapp/`)
- `updateBy` - column name/label for "Update existing records" matching
- `rows` - array of objects with column names and values for filtering/verification
- `preserveColumns` - array of column names not in Excel to verify they survive import unchanged

### Preparing Import Test Data

1. Create a test record in the table with many fields filled
2. Export table to Excel
3. Keep only header row and test record
4. Keep original ID (to verify it won't be overwritten)
5. Add `-import-test` suffix to values for identification
6. Save .xlsx file next to the test .js file with matching name

## Testing Permissions/Rights

### In baseTest (automatic)

```javascript
await DataTables.baseTest({
    dataTable: 'mediaGroupsTable',
    perms: 'editor_edit_media_group'
});
```

### Manual permission testing

```javascript
Scenario('test permissions', ({ I, DT }) => {
    I.amOnPage("/admin/v9/apps/my-app/");
    DT.waitForLoader();
    I.see("Expected Data");

    // Test with removed permission
    DT.checkPerms("myPermName", "/admin/v9/apps/my-app/");
});
```

### Testing button-level permissions with forceShowButton

```javascript
Scenario('button permissions', ({ I, DT, DTE }) => {
    // Verify buttons are hidden when permissions removed
    I.amOnPage("/admin/v9/apps/my-app/?removePerm=addRecord,editRecord,deleteRecord");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-create");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-edit");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-remove");

    // Verify REST service rejects operations even with forced buttons
    I.amOnPage("/admin/v9/apps/my-app/?removePerm=addRecord,editRecord,deleteRecord,forceShowButton");
    DT.waitForLoader();
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    DTE.waitForEditor();
    I.fillField("name", "test");
    DTE.save();
    I.see("nemáte právo");  // Verify error message
    DTE.cancel();
});
```

## Visual Testing (Screenshots)

```javascript
await Document.compareScreenshotElement(
    "#insertScriptTable_wrapper",           // CSS selector of element
    "autotest-insert-script-settings.png",  // screenshot filename (prefix with autotest-)
    1280,                                   // optional: window width
    270,                                    // optional: window height
    5                                       // optional: tolerance (0-100)
);
```

Reference images go to `src/test/webapp/screenshots/base/`. On first run, actual screenshot is saved to `build/test/screenshots/actual/`.

## Complete Test File Template

```javascript
Feature('apps.my-module.my-feature');

var auto_name;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage('/admin/v9/apps/my-feature/');

    if (typeof auto_name == "undefined") {
        var randomNumber = I.getRandomText();
        auto_name = 'name-autotest-' + randomNumber;
    }
});

Scenario('zakladne testy @baseTest', async ({ I, DataTables, DT, DTE }) => {
    await DataTables.baseTest({
        dataTable: 'myFeatureTable',
        perms: 'cmp_my_feature',
        createSteps: function(I, options, DT, DTE) {
            // Fill additional fields during create
        },
        editSteps: function(I, options, DT, DTE) {
            // Modify fields during edit
        }
    });
});

Scenario('specific feature test', ({ I, DT, DTE }) => {
    // Navigate and verify specific behavior
    I.waitForText("Expected text", 10);
    DT.filter("name", "search-value");
    I.click("Record name");
    DTE.waitForEditor();
    // Verify editor content
    I.seeInField("Field name", "expected value");
    DTE.cancel();
});

Scenario('domain switching', ({ I, Document }) => {
    I.see("Record from main domain");
    Document.switchDomain("test23.tau27.iway.sk");
    I.dontSee("Record from main domain");
});

Scenario('odhlasenie', ({ I }) => {
    // Logout if you changed domain or user state
    I.logout();
});
```

## REST API Testing

```javascript
Feature('api.my-service');

Before(({ I }) => {
    I.amOnPage('/logoff.do?forward=/admin/');
});

Scenario("API call", ({ I }) => {
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsKeys(['numberOfElements', 'totalPages']);
    I.seeResponseContainsJson({
        content: [{ "id": 11, "groupId": 25, "title": "Produktová stránka" }]
    });
});

Scenario("API unauthorized", ({ I }) => {
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'invalid-token'
    });
    I.seeResponseCodeIs(401);
});
```

## Best Practices Summary

1. **Feature naming**: Match file path - `Feature('apps.banner.banner')` for `tests/apps/banner/banner.js`
2. **Test data isolation**: Use `autotest` prefix + `I.getRandomText()` for all test data
3. **Create & cleanup in separate scenarios**: If creation scenario fails, cleanup scenarios still run
4. **Use proper selectors**: Narrow CSS selectors with parent context (e.g. `"#bannerDataTable td.dt-row-edit"`)
5. **Verify selectors in browser console**: Use `$("your-selector")` in F12 console to verify uniqueness
6. **No fixed waits**: Use `waitForText`, `waitForElement`, `waitForVisible`, `DT.waitForLoader()`, `DTE.waitForEditor()`, `DTE.waitForLoader()`
7. **Short focused scenarios**: Don't mix unrelated tests in one scenario
8. **Always test permissions**: Use `perms` parameter in baseTest or manual `DT.checkPerms()`
9. **Logout after domain change**: If test switches domain, add `Scenario('odhlasenie', ({I}) => { I.logout(); })`
10. **No After() function**: Don't use `After()` - it interferes with pauseOnFail debugging