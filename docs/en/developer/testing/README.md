# Automated testing

For automated E2E testing the framework is used [CodeceptJS](https://codecept.io). Tests are written in JavaScript and practically control the browser in which the test is run. More information on why we chose this framework is in the section [Playwright + CodeceptJS](#playwright--codeceptjs).

## Installation

```shell
cd src/test/webapp/
npm install
```

!>**Warning:** Before running the testing, you need to compile the JS/CSS admin part of WebJET:

```shell
cd src/main/webapp/admin/v9/
npm install
npm run prod
```

and start the application server:

```shell
gradlew appRun
```

I recommend you to run each of the above commands in a separate Terminal (menu Terminal->New Terminal). You can switch between running terminals in the `Terminal`.

## Start testing

To start testing, use the following commands:

````shell
cd src/test/webapp/

#spustenie vsetkych testov
npm run all

#spustenie konkrétneho testu a zastavenie v prípade chyby
npm run pause tests/components/gallery_test.js

#Spustenie konkrétneho scenára s hodnotou ```@current``` v názve
npm run current
````

To run in firefox use the prefix `ff:` before the title:

```shell
npm run ff:all
npm run ff:pause tests/components/gallery_test.js
npm run ff:current
```

Running on another URL with browser display and browser disabled `firefox`:

```shell
CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npm run all
```

**Remark:** in Firefox we had problems with the speed of the tests. Therefore, for this browser in the file `codecept.conf.js` sets the variable `autodeayEnabled` to the value of `true` and the add-on is activated `autodelay`. The latter delays the execution of functions `amOnPage,click,forceClick` about 200ms before and 300ms after the command is called. We also identified a strange behavior of the browser, which if it is not on the foreground then the tests stop working out of nowhere and display nonsensical errors. When running the test once, the test always executed correctly. We attribute this to some optimization of JavaScript code execution in the browser when it is not active. When run with the browser not displayed, everything is fine, so always use the setting to run all tests `CODECEPT_SHOW=false`.

### Codecept UI

Codecept offers in beta a UI to view testing, you run it with the command:

```shell
npm run codeceptjs:ui
```

and then open the page in your browser `http://localhost:3001`.

### HTML report generation

**Mochawesome**

In npm is set [plugin for generating HTML reports](https://codecept.io/reports/#html). You generate it by running the command:

```shell
npm run codeceptjs --reporter mochawesome
```

and an HTML report with the test result will be generated in the /build/test/report directory. For failed tests, a screenshot is also created. The setting is in [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) in section `mocha`.

**Allure**

The report can also be generated via [allure](allure.md) by running the test:

```shell
npm run codeceptjs --plugins allure
```

Once the test is complete, you can view the results by running `allure` server:

```shell
allure serve ../../../build/test
```

## Playwright + CodeceptJS

For testing is used [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Why Playwright?

- it is the 3rd generation of the testing framework (1st generation Selenium, 2nd generation Puppeteer, 3rd generation Playwright)
- Microsoft bought the authors `Puppeteer` framework and they develop Playwright, so they have the experience
- Supported by `chromium, firefox, webkit` (2021 May Windows update will switch Edge to Chromium Core)
- can emulate resolutions, user agent, DPI

Why CodeceptJS?

- Playwright as well as `Puppeteer` is a communication (low level) protocol for controlling the browser (its automation)
- CodeceptJS is a testing framework that, among other things, can use Playwright
- test code is written in JavaScript
- the code of the tests is very [understandable](https://codecept.io/playwright/#setup)
- has advanced options [Locators](https://codecept.io/locators/#css-and-xpath) - search elements by text, css, xpath
- has [GUI](https://codecept.io/ui/) (not yet tested) for writing and displaying test results

### Configuration

The basic configuration is in the file `codecept.conf.js`. Important attributes:
- `url` (http://iwcm.interway.sk) - address (domain) of the server. You can change this via `--override` parameter and switch testing from DEV to TEST/PROD environment.
- `output` (../../../build/test) - directory to which you will generate a screenshot in case of a failed test (default to `build/test` in the root directory)
- `browser` (chromium) - the chosen browser to run the tests, can be `chromium, firefox, webkit`
- `emulate` (commented) - [Emulation](https://github.com/Microsoft/playwright/blob/master/src/deviceDescriptors.ts) facilities
- `screenshotOnFail` - enables/disables screenshot creation in case of a failed test

## Writing tests

Tests are created in subdirectories tests, where they are divided according to the individual modules/applications of WebJET. They are written in JavaScript, so you can use all the possibilities that JavaScript offers.

Example of a more complex test to test login [src/test/webapp/tests/admin/login.js](../../../../src/test/webapp/tests/admin/login.js):

!>**Warning:** to `Feature` enter the value in the format `adresár.podadresár.meno-súboru` for correct display of tests in tree structure and easy tracing of the file according to `Feature` in the log file.

```javascript
Feature('admin.login');

//Before sa vola pred kazdym Scenarom, v kazdom scenati volam /admin/ a zadavam username tester
Before(({I}) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
});

//Kazdy scenar sa spusta samostatne a samostatne sa vyhodnocuje
Scenario('zle zadane heslo', ({I}) => {
    //do pola password vyplnim zle heslo
    I.fillField("password", "wrongpassword");
    //kliknem na tlacitko
    I.click("login-submit");
    //overim, ci sa zobrazi uvedena hlaska
    I.see("Zadané meno alebo heslo je nesprávne.");
});

Scenario('prihlasenie zablokovane', ({I}) => {
    I.fillField("password", "wrongpassword");
    I.click("login-submit");
    I.see("Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované");
    I.say("Cakam 10 sekund na exspirovanie zablokovanej IP adresy");
    //je potrebne cakat 10 sekund na exspirovanie zleho hesla
    I.wait(13);
    //odkomentujte pre zobrazenie interaktivneho terminalu
    //pause();
});

Scenario('uspesne prihlasenie', ({I}) => {
    I.fillField("password", secret("************"));
    I.click("login-submit");
    I.see("WebJET 2021 info");
    //konecne som prihlaseny
    I.wait(1);
    //zobrazim dropdown s menom usera, ten vyberam cez CSS selector
    I.click({css: "li.dropdown.user"});
    //overim, ci vidim moznost Odhlasenia
    I.see("Odhlásenie");
    //kliknem na odhlasenie, vsimnite si, ze to selectujem podla textu linky
    I.click("Odhlásenie");
    //overim, ci sa zobrazi text Prihlasenie (som korektne odhlaseny)
    I.see("Prihlásenie");
});
```

Note that in the demo I am intentionally using different locators/selectors (field name, text/label, CSS selector). This is one of the advantages of CodeceptJS. More about locator options is in [Documentation](https://codecept.io/locators/#css-and-xpath).

### Locators

The selectors that select an element on the page are well described in [official documentation](https://codecept.io/locators/).

```txt
{permalink: /'foo'} matches <div id="foo">
{name: 'foo'} matches <div name="foo">
{css: 'input[type=input][value=foo]'} matches <input type="input" value="foo">
{xpath: "//input[@type='submit'][contains(@value, 'foo')]"} matches <input type="submit" value="foobar">
{class: 'foo'} matches <div class="foo">
```

### Within

Using the notation `within` you can restrict the element to which the following commands are applied:

```javascript
within("div.breadcrumb-language-select", () => {
    I.click("Slovenský jazyk");
    I.click("Chorvátsky jazyk");
});
```

at the same time, most commands also allow you to write a selector to the command, the above can also be written as:

```javascript
  I.click("Slovenský jazyk", "div.breadcrumb-language-select");
  I.click("Chorvátsky jazyk", "div.breadcrumb-language-select");
```

### Playwright methods

V [official documentation](https://codecept.io/helpers/Playwright/) is a list of all options `I` object. Short references:
- [pressKey](https://codecept.io/helpers/Playwright/#presskey)
- [click](https://codecept.io/helpers/Playwright/#click)
- [forceClick](https://codecept.io/helpers/Playwright/#forceclick) - a forced click without waiting for an event, must be used on custom `checkboxy` (otherwise it gets stuck there)
- [see](https://codecept.io/helpers/Playwright/#see) / [dontSee](https://codecept.io/helpers/Playwright/#dontsee)
- [seeElement](https://codecept.io/helpers/TestCafe/#seeelement) / [dontSeeElement](https://codecept.io/helpers/Detox/#dontseeelement)
- [fillField](https://codecept.io/helpers/Playwright/#fillfield)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [wait](https://codecept.io/helpers/Playwright/#wait)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [executeScript](https://codecept.io/helpers/Playwright/#executescript)
- [saveScreenshot](https://codecept.io/helpers/Playwright/#savescreenshot)

### WebJET additional features

We have added several useful features for WebJET:
- [I.formatDateTime(timestamp)](../../../../src/test/webapp/steps_file.js) - formats timestamp to date and time using library moment
- [I.seeAndClick(selector)](../../../../src/test/webapp/steps_file.js) - waits for the element to be displayed and then clicks on it
- [await I.clickIfVisible(selector)](../../../../src/test/webapp/custom_helper.js) - if the element is displayed click on it, if it is not displayed skip the step (do not throw an error)
- [I.verifyDisabled(selector)](../../../../src/test/webapp/custom_helper.js) - verifies that the field is inactive
- [I.wjSetDefaultWindowSize()](../../../../src/test/webapp/steps_file.js) - sets the default window size after changing it, is called automatically even after logging in the login sequence in [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js)
- [Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)](../../../../src/test/webapp/codecept.conf.js) - will perform [visual comparison](#visual-testing)
- `I.waitForTime(time)` - waiting until the specified time (timestamp).
- `I.toastrClose()` - closing the window `toastr` notifications.
- `clickCss(name, parent=null)` - performs a click just like `I.click` But `name` considered as CSS selector - execution is faster, no need to use wrapping to `{css: name}`.
- `forceClickCss(name, parent=null)` - performs a click just like `I.forceClick` But `name` considered as CSS selector - execution is faster, no need to use wrapping to `{css: name}`.

We have prepared special functions for the date table. They are implemented in [DT.js](../../../../src/test/webapp/pages/DT.js):
- `DT.waitForLoader(name)` - waits for displaying and then hiding the "Processing" information in the datatable. It is used as `DT.waitForLoader("#forms-list_processing");`
- `DT.filter(name, value, type=null)` - sets the value `value` in the text filter column `name` DATATables. If the attribute is also specified `type` sets the search type (e.g. Starts at, Ends at, Equals to).
- `DT.filterSelect(name, value)` - sets the value `value` into the select field of the column filter `name` DATATables. It is used as `DT.filterSelect('cookieClass', 'Neklasifikované');`
- `async I.getDataTableColumns(dataTableName)` - returns a DATA object with a datatable definition, used in automatic datatable testing
- `async getDataTableId(dataTableName)` - returns datatable ID, calls JS function `dataTable.DATA.id`
- [async I.getTotalRows()](../../../../src/test/webapp/custom_helper.js) - returns the total number of records in the datatable
- `DT.deleteAll(name = "datatableInit")` - deletes the currently displayed records, always use the `DT.filter` for filtering the necessary data.

For Datatable Editor implemented in [DTE.js](../../../../src/test/webapp/pages/DTE.js):
- `DTE.waitForLoader(name)` - waiting to be hidden `loadera` in the editor (save record)
- `DTE.waitForEditor(name)` - waits for the editor to display, if name is defined, the datatable with the given name is used, by default `datatableInit`
- `DTE.selectOption.(name, text)` - selects a value in the select box (in the correct way by displaying the options and then clicking on the option)
- `DTE.save(name)` - clicks the Save button in the editor, if name is defined, the datatable with the given name is used, by default `datatableInit`
- `DTE.cancel(name)` - clicks the close editor button, if name is defined, the datatable with the given name is used, by default `datatableInit`
- `DTE.fillField(name, value)` - fills in a standard field, as opposed to calling `I.fillField` it is possible to `name` parameter to directly specify the name of the field on the backend/json definition.
- `DTE.fillQuill(name, value)` - fills the value in the types field `QUILL`.
- `DTE.fillCkeditor(htmlCode)` - sets the HTML code to the currently displayed CKEditor.
- `DTE.fillCleditor(parentSelector, value)` - enters text into WYSIWYG `cleditor`. Value `parentSelector` - reference to the element in which the `cleditor` located (e.g. `#forum`), `value` - value to fill. **Warning:** it doesn't know the diacritics yet because of the use of `type` command. It is also possible to execute the following for a datatable [automated test](datatable.md).
- `DTE.appendField(name, value)` - adds text to the field in the editor, solves the problem of using `I.appendField` which is not always done correctly in the editor.

For JsTree (tree structure):
- `I.jstreeClick(name)` - clicks on the selected text in jstree (important to use especially in web pages where the link with the same name as the directory is also in the list of pages)
- `I.createFolderStructure(randomNumber)` - prepares a directory tree structure and two subdirectories for testing
- `I.deleteFolderStructure(randomNumber)` - deletes the tree structure of the directory and the two subdirectories prepared via `I.createFolder`

```javascript
//povodne ZLE riesenie s I.wait
I.click("Pridať");
```
- `I.jstreeNavigate(pathArray)` - in the field `pathArray` it is possible to define the names of individual nodes in the tree structure, which the function clicks on in turn, e.g. `I.jstreeNavigate( [ "English", "Contact" ] );`.

To verify the values in the table, you can use the functions:
- `DT.checkTableCell(name, row, col, value)` - verifies the value in the specified table (table ID) `value` in the specified row `row` and column `col`. Rows and columns start with 1.
- `DT.checkTableRow(name, row, values)` - verifies in the specified table (table ID) in the specified row `row` values in the field `values`. The lines start with 1. For example. `DT.checkTableRow("statsDataTable", 1, ["13", "2 022", "30", "533", "229", "1"]);`.

Functions implemented in `Document` facility:
- `switchDomain(domain)` - switches the domain to the specified value.
- `setConfigValue(name, value)` - sets a configuration variable with the specified name and value.
- `resetPageBuilderMode()` - deletes the remembered editor mode (standard/PageBuilder).
- `notifyClose` - will close `toastr` notification.
- `notifyCheckAndClose(text)` - verifies the text in `toastr` notification and closes it.
- `editorComponentOpen()` - opens the application settings in the page editor (window `editor_component.jsp`).
- `editorComponentOk()` - clicks OK to save the application settings.
- `scrollTo(selector)` - scrolls the content of the window to the specified element.

V `Document` object also includes functions for creating [screenshots](screenshots.md).

To test emails using [tempmail.plus](https://tempmail.plus) there is an object `TempMail`:
- `login(name, emailDomain = "fexpost.com")` - login and account setup
- `openLatestEmail()` - opens the latest email
- `closeEmail()` - closes an open email and returns to the email list
- `destroyInbox()` - deletes all emails in your inbox

### Waiting for completion

It is generally not recommended to use `I.wait` with a fixed period. The waiting time may be different on the local machine and in the CI/CD pipeline. Additionally, a fixed time may unnecessarily increase the time required to execute the test.

We recommend using the methods [waitFor\*](https://codecept.io/helpers/TestCafe/#waitforelement) especially `waitForElement`, `waitForText`, `waitForVisible` a `waitToHide`.

It is preferable to use mainly `waitForText` where we can effectively replace `I.wait` and subsequent `I.see` for one command:

```javascript
//povodne ZLE riesenie s I.wait
I.click("Pridať");
I.wait(1);
I.see("test-adresar-" + randomNumber, container);

//nove riesenie s I.waitForText, caka sa maximalne 10 sekund
I.click("Pridať");
I.waitForText("test-adresar-" + randomNumber, 10, container);
```

### Pause

If you put a command somewhere in the test code `pause()`, the execution of the tests will stop and you will see an interactive console in the Terminal where you can run commands. This way you can prepare the test steps and then simply copy the commands into the JS test file.

```shell
 ...
 Interactive shell started
 Use JavaScript syntax to try steps in action
 - Press ENTER to run the next step
 - Press TAB twice to see all available commands
 - Type exit + Enter to exit the interactive shell
 - Prefix => to run js commands
 I.
```

Pressing the TAB key twice will display a help (list of possible commands). You can enter these and watch what happens in the browser. Pressing the Enter key will advance the test to the next command. Entering `exit` the interactive terminal will be terminated and the test will continue automated further.

### Login

In the file [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) login via extension is also defined [autologin](https://codecept.io/plugins/#autologin):

```javascript
autoLogin: {
    enabled: true,
    saveToFile: true,
    inject: 'login',
    users: {
        admin: {
            login: (I) => {
            I.amOnPage('/admin/');
            I.fillField("username", "tester");
            I.fillField("password", secret("********"));
            I.click("login-submit");
            },
            check: (I) => {
                I.amOnPage('/admin/');
                I.see("WebJET 2021 info");
            }
        }
    }
}
```

It is possible to define multiple users (repeating the admin attribute), e.g. a registered user, an administrator with limited rights, etc.

Logins can be inserted into the tests using `Before` functions:

```javascript
Feature('gallery');

Before(({login}) => {
    login('admin');
});

Scenario('zoznam fotografii', ({I}) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    I.click("test");
    I.see("koala.jpg");
});
```

### Assert library

An extension is available [codeceptjs-chai](https://www.npmjs.com/package/codeceptjs-chai) for calling assert functions:

Basic use:

```javascript
I.assertEqual(1, 1);
I.assertEqual('foo', 'foo');
I.assertEqual('foo', 'foo', 'Both the values are not equal');

I.assertNotEqual('foobar', 'foo', 'Both the values are equal');

I.assertContain('foobar', 'foo', 'Target value does not contain given value');
I.assertNotContain('foo', 'bar', 'Target value contains given value');

I.assertStartsWith('foobar', 'foo', 'Target value does not start with given value');
I.assertNotStartsWith('foobar', 'bar', 'Target value starts with given value');

I.assertEndsWith('foobar', 'bar', 'Target value does not ends with given value');
I.assertNotEndsWith('foobar', 'bar', 'Target value ends with given value');

I.assertLengthOf('foo', 3, 'Target data does not match the length');
I.assertLengthAboveThan('foo', 2, 'Target length or size not above than given number');
I.assertLengthAboveThan('foo', 4, 'Target length or size not below than given number');

I.assertEmpty('', 'Target data is not empty');

I.assertTrue(true, 'Target data is not true');
I.assertFalse(false, 'Target data is not false');

I.assertAbove(2, 1, 'Target data not above the given value');
I.assertAbove(1, 2, 'Target data not below the given value');
```

If necessary, you can also use [assert](https://www.npmjs.com/package/assert) library. An example of use is in the test [gallery.js](../../../../src/test/webapp/tests/apps/gallery/gallery.js):

```javascript
const assert = require('assert');
...
assert.equal(+inputValueH, +area.h);
```

### Page objects

To create universal test scenarios, the component `Pages` into which Page objects are generated via the `npx codeceptjs gpo`, a page object is created using `Dependency Injection` (similar to Angular).

```javascript
const { I } = inject();

module.exports = {

  // insert your locators and methods here
}
```

In order to use it in tests, you need to register it in `codecept.conf.js`.

```javascript
exports.config = {
    include: {
        I: './steps_file.js',
        PageObject: './pages/PageObject.js'
    }
}
```

We can then insert it into our test scenario.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  PageObject.someMethod();
})
```

It is also possible to insert objects into tests dynamically via `injectDependencies({})`.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  I.fillField('Username', PageObject.username);
  I.pressKey('Enter');
}).injectDependencies({ PageObject: require('./PageObject.js') });
```

### Browser detection

If your tests behave differently in Firefox or Chromium, it is possible to use the browser validation features in the tests.

```javascript
if (Browser.isChromium()) {
  I.amOnPage("/admin/v9/apps/insert-script/");
  ...
}

if (Browser.isFirefox()) {
  I.say("Firefox, skipping test");
  return;
}

if (Browser.isFirefox()) {
    //ff ma nejak inak kurzor a je potrebne este 2x ist hore
    I.pressKey('ArrowUp');
    I.pressKey('ArrowUp');
}
```

## Revocation of the right

By calling the page address with the parameter `removePerm` it is possible to remove a specified right from a logged-in user on the fly (without saving changes in rights) if the user's login name starts with `tester`. It is possible to test the display of the page without the specified permission and verify the security of the REST service call.

The revocation of the right is implemented in the function `DT.checkPerms(perms, url)` v [DT.js](../../../../src/test/webapp/pages/DT.js). Requires you to specify the right and the address of the page on which the right is being tested. Testo verifies the display of the notification `Prístup k tejto stránke je zamietnutý`. Optional parameter `datatableId` represents the ID/name of the table in the page (it is necessary to specify if there are multiple datatables in the page).

Example of use:

```javascript
Scenario('zoznam stranok', ({ I, DT }) => {
    I.waitForText("Newsletter", 20);
    I.click("Newsletter", container);
    I.see("Testovaci newsletter");

    //over prava
    DT.checkPerms("menuWebpages", "/admin/v9/webpages/web-pages-list/");
});
```

To the parameter `removePerm` it is also possible to specify multiple rights separated by a comma.

For datatables, it is also possible to set the rights to [individual buttons](../datatables/README.md#buttons-by-rights) (add, edit, duplicate, delete). You can also test individually disabled rights. But to verify the rights on the backend, you need to test the REST service as well. By adding the expression `forceShowButton` to the parameter `removePerm` for a user with a login name starting with `tester` the buttons in the datatable will be displayed. It is thus possible to test the display of the error message from the REST service (that the record cannot be added/edited/deleted). An example is in `webpage-perms.js`:

```javascript
Scenario('stranky-overenie prav na tlacidla', ({ I, login, DT, DTE }) => {
    login("admin");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=addPage,pageSave,deletePage,pageSaveAs");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-create");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-edit");
    I.dontSeeElement("#datatableInit_wrapper button.btn-duplicate");
    I.dontSeeElement("#datatableInit_wrapper button.buttons-remove");

    //over v dialogu
    I.click("Jet portal 4 - testovacia stranka");
    DTE.waitForEditor();
    I.dontSeeElement("#datatableInit_modal div.DTE_Form_Buttons button.btn-primary");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?removePerm=addPage,pageSave,deletePage,pageSaveAs,forceShowButton&groupid=67");
    DT.waitForLoader();
    //skus pridat
    I.clickCss("#datatableInit_wrapper button.buttons-create");
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.fillField("Názov web stránky", auto_webPage);
    DTE.save();
    I.see("Pridať web stránku - nemáte právo na pridanie web stránky");
    DTE.cancel();

    //skus editovat
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("Nedá sa zmazať", "#datatableInit");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nedá sa zmazať");
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
    DTE.cancel();

    //skus zmazat
    DT.filter("title", "Nedá sa zmazať");
    I.click("table.datatableInit button.buttons-select-all");
    I.clickCss("#datatableInit_wrapper button.buttons-remove");
    DT.waitForEditor();
    I.see("Naozaj chcete zmazať položku?");
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
});
```

**Technical information:**

The revocation of the right is implemented in [ThymeleafAdminController.removePermissionFromCurrentUser](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java). When you enter the URL parameter `removePerm` the rights of the currently logged in user are modified, including the Spring context.

## Visual testing

The visual testing function should be used to verify displays that cannot be verified by text testing (e.g. correct position of the selection menu). It is used [plugin pixelMatchHelper](https://github.com/stracker-phil/codeceptjs-pixelmatchhelper) which can compare the reference screenshot with the current one and can also highlight changes.

To simplify use, we have prepared a function `Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)` which will ensure the necessary steps. It has parameters:
- `selector` - `selector` the element from which the snapshot is to be taken (it is not taken from the whole screen, but only from the specified element)
- `screenshotFileName` - the image file name, it will automatically be compared with the same image name in the directory `src/test/webapp/screenshots/base`. For filename use prefix `autotest-` for better tracking of the created image
- `width` (optional) - browser window width
- `height` (optional) - browser window height
- `tolerance` (optional) - the degree of tolerance of differences to the reference image (0-100)

Example of use:

```javascript
await Document.compareScreenshotElement("#insertScriptTable_wrapper", "autotest-insert-script-settings.png", 1280, 270);
```

There will probably not be a reference image the first time you run it. But the test will create the current image and save it in the directory `build/test` (so we recommend prefixing the image name with autotest- so that the image can be easily found among screenshots of bugs from testing). If you want to use the image as a reference, copy it to the `src/test/webapp/screenshots/base`. Then the next time you run it, the reference image will be compared with the web page.

Identified differences are generated into images in the directory `src/test/webapp/screenshots/diff` for easy error verification. The test will also declare an error like any other test scenario when differences are identified.

Example of display error (wrong position of selection menu) - reference base image:

![](autotest-insert-script-settings.png)

incorrect display in the page:

![](autotest-insert-script-settings-error.png)

the resulting comparison with the highlighting of the difference area (pink colour):

![](autotest-insert-script-settings-diff.png)

**Notes on implementation**

Image comparison is encapsulated in a function `Document.compareScreenshotElement` implemented in `Document.js`. When the window is resized, it performs the resizing and returns the window to the default size after creating a screenshot by calling the function `I.wjSetDefaultWindowSize()` (this is also called after each login for consistency).

## Best practices

To run the tests successfully and repeatedly, we recommend the following points:

### Nomenclature

- you start the scenario with the function `Feature('xxx');` where xxx is the name of the test file. If an error occurs you can easily find the corresponding test file.

### Test data

- prepare and delete your test data
- all created objects must contain text `autotest` for identifying objects created by an automated test
- we recommend to use the call `I.getRandomText()` to obtain a unique suffix, a usage seen e.g. in [group-internal.js](../../../../src/test/webapp/tests/webpages/group-internal.js) where variables are defined and are filled in `Before` function
- it is ideal if you create the test data in a separate scenario and also delete it in a separate scenario. So if a test fails, the data deletion is done anyway.

```javascript
var auto_name, auto_folder_internal, auto_folder_public, sub_folder_public;

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list/');

     if (typeof auto_name=="undefined") {
          var randomNumber = I.getRandomText();
          auto_name = 'name-autotest-' + randomNumber;
          auto_folder_internal = 'internal_folder-autotest-' + randomNumber;
          auto_folder_public = 'public_folder-autotest-' + randomNumber;
          sub_folder_public = 'sub_folder_public-autotest-' + randomNumber;
     }
});
```

### Selectors

It is important to use the correct selectors, text/element can appear multiple times in the page and then the test randomly crashes. Use ready-made features like `I.jstreeClick(name)` for tree structure and functions starting with DT./DTE. for datatable and editor e.g. `DTE.selectOption(name, text)` or `DT.filterSelect(name, value)`.

We recommend to try the selector in the JS console of the browser using jQuery, for example:

```javascript
//tlacidlo na pridanie zaznamu
$(".btn.btn-sm.buttons-create.btn-success.buttons-divider");

//v konzole bude vidno, ze vo web strankach je takych tlacidiel viacero:
//1xjstree a 2xdatatabulka z ktorej jedna je neviditelna pre adresare)
//je preto potrebne selektor zuzit tak, aby obsahoval vhodny parent kontajner

//zuzene len na column so stromovou strukturou - najde korektne len jedno tlacidlo
$("div.tree-col .btn.btn-sm.buttons-create.btn-success.buttons-divider")
```

### Timing

Timing of execution is very important, on a different computer or server the test may run at a different speed. It is necessary to wait correctly for asynchronous calls to the server to complete. Similarly, waiting for a dialog box to open, data to be saved, and so on can be a problem.

Do not use fixed time type `I.wait(1)` but use calls `I.waitFor...` or our `DT.waitFor...`. More in the section [Waiting for completion](#waiting-for-completion) a [WebJET additional features](#webjet-additional-features).

Typical examples:

```javascript
//kliknutie na tlacidlo Pridat a cakanie na otvorenie editora
I.click(add_button);
DTE.waitForEditor("groups-datatable");

//cakanie na loading datatabulky
DT.waitForLoader
//cakanie na otvorenie editora
DTE.waitForEditor
//cakanie na ulozenie editora
DTE.waitForLoader
```

**Every call**, `I.click('Uložiť');` has to wait for saving via `DTE.waitForLoader`.

### Scenario length

Try to keep individual scenarios short, don't combine unrelated parts into one scenario. However, you can prepare test data and reuse it in multiple scenarios (this will save time creating and deleting data between scenarios).

The script can also be run independently using `--grep` parameter see [Start testing](#start-of-testing).

### Debugging

You can run the test with the parameter `-p pauseOnFail` if an error occurs, the interactive console will automatically be displayed. In it you can check the browser's status and possibly try a correction command, which you can then translate into a test.

For this reason, do not use `After` function in the scenario, because it will be executed before the interactive console is invoked after the error and the browser window will no longer be in the same state.

## Deleting the database

The database grows with the use of tests, as both directories and web pages are moved to the trash after deletion. It is important to delete this data from the database once a quarter. You can use the following SQL statement:

```sql
DELETE FROM emails_campain WHERE subject LIKE '%-autotest%';
OPTIMIZE TABLE emails_campain;
DELETE FROM emails WHERE recipient_email LIKE '%autotest%' OR recipient_email LIKE '%emailtounsubscibe%';
OPTIMIZE TABLE emails;
DELETE FROM groups WHERE group_name LIKE '%sk-mirroring-subfolder%' OR group_name LIKE '%sk-mir-subfolder%' OR group_name LIKE '%autotest%' OR group_name LIKE '%test-adresar-2%' OR group_name='NewSubFolder' OR group_name LIKE 'section-2%';
OPTIMIZE TABLE groups;
DELETE FROM groups_scheduler WHERE group_name LIKE '%sk-mirroring-subfolder%' OR group_name LIKE '%sk-mir-subfolder%' OR group_name LIKE '%autotest%' OR group_name LIKE '%test-adresar-2%' OR group_name='NewSubFolder' OR group_name LIKE 'section-2%';
OPTIMIZE TABLE groups_scheduler;
DELETE FROM documents WHERE (doc_id NOT IN (7611, 18426, 2664, 27827, 29195, 29289, 64425, 50222, 60434)) AND (title LIKE '%sk-mirroring-subfolder%' OR title LIKE '%sk-mir-subfolder%' OR title LIKE '%-autotest%' OR title LIKE '%autotest_%' OR title LIKE '%_autotest%' OR title='autotest' OR title LIKE 'test-adresar-%' OR title='Nová web stránka' OR title LIKE 'page-%' OR title LIKE 'dobré ráno-%' OR title LIKE 'good morning-%' OR title LIKE 'test-mir-elfinderFile%');
OPTIMIZE TABLE documents;
DELETE FROM documents_history WHERE title LIKE 'Test_volnych_poli_sablony%' OR title LIKE '%sk-mirroring-subfolder%' OR title LIKE '%sk-mir-subfolder%' OR title LIKE '%-autotest%' OR title LIKE '%autotest_%' OR title LIKE '%_autotest%' OR title='autotest' OR title LIKE 'test-adresar-%' OR title='Nová web stránka' OR title LIKE 'page-%' OR title LIKE 'dobré ráno-%' OR title LIKE 'good morning-%' OR title LIKE 'test-mir-elfinderFile%' OR title LIKE 'Test_show_in%';
DELETE FROM documents_history WHERE doc_id=4 AND history_id>26 AND actual=0 AND publicable=0;
OPTIMIZE TABLE documents_history;
DELETE FROM _adminlog_ WHERE log_id>10 AND log_id NOT IN (58993, 58730, 103758, 103756);
OPTIMIZE TABLE _adminlog_;
DELETE FROM monitoring WHERE monitoring_id > 1219015;
OPTIMIZE TABLE monitoring;
DELETE FROM enumeration_data WHERE child_enumeration_type_id IS NOT NULL;
DELETE FROM enumeration_data WHERE parent_enumeration_data_id IS NOT NULL;
DELETE FROM enumeration_data WHERE string1 like '%testTest%';
DELETE FROM enumeration_data WHERE string1 like 'string1%';
OPTIMIZE TABLE enumeration_data;
UPDATE enumeration_type SET child_enumeration_type_id=NULL WHERE name like '%AutoTest%' AND enumeration_type_id>2283;
DELETE FROM enumeration_type WHERE name like '%AutoTest%' AND enumeration_type_id>2283;
OPTIMIZE TABLE enumeration_type;
DELETE FROM documents_history WHERE doc_id=22955 AND publicable=0;
UPDATE groups SET sort_priority=10 WHERE parent_group_id IN (15257, 80578);
DELETE FROM media WHERE media_fk_id NOT IN (259) AND (media_title_sk LIKE '%autotest%' OR media_title_sk LIKE 'image test%' OR media_title_sk LIKE '%onerror=alert%' OR media_title_sk LIKE 'media%');
OPTIMIZE TABLE media;
```

If you want to change the passwords in the test database across the board, use:

```sql
UPDATE users SET password='bcrypt:...', password_salt='bcrypt:...' WHERE user_id>1 AND login NOT IN ('user_sha512', 'user_bcrypt');
```

In the penetration test database, users need to disable the menu item for editing administrators, use the following SQL:

```sql
INSERT INTO `user_disabled_items` (`user_id`, `item_name`)
VALUES
	(18, 'users.edit_admins'),
    (18, 'conf.show_all_variables'),
    (1827, 'users.edit_admins'),
    (1827, 'conf.show_all_variables');
UPDATE _conf_ SET value='' WHERE name='adminEnableIPs';
```

For a clean database on which you want to run `baseTest` needs to be set:

```sql
INSERT INTO _conf_ VALUES ('templatesUseDomainLocalSystemFolder', 'true', NULL);
INSERT INTO _conf_ VALUES ('multiDomainEnabled', 'true', NULL);
INSERT INTO _conf_ VALUES ('enableStaticFilesExternalDir', 'true', NULL);
UPDATE _conf_ SET value='aceintegration' WHERE name='installName';

UPDATE groups SET group_name='Jet portal 4', navbar='Jet portal 4' WHERE group_id=1;
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(141, 'Jet portal 4 - testovacia stranka', '<p>Toto je testovacia stranka... 01</p>\n\n<p>Druhy odstavec</p>', '   toto je testovacia stranka... 01    \n\n   druhy odstavec    <h1>jet portal 4   testovacia stranka</h1>\n', '', 'Jet portal 4 - testovacia stranka', '2024-09-10 07:39:06', '2020-08-24 11:39:00', NULL, 1, 1, 4, 0, 0, 1, 1, 0, '/Jet portal 4', NULL, 10, -1, -1, -1, NULL, '', '', '', '/images/zo-sveta-financii/foto-3.jpg', '', 1, NULL, '/lta-href=39amp4739gtjet-portalltamp47agt/', 0, 0, 0, -1, '', '', '', '', '', '', '', '', '', '', 'ewteret', '', 0, 1, '', '', '', '', '', '', '', '', 0, 1, NULL, NULL, 9780, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, '', 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(264, 'Presmerovaná extrená linka', '<p>&nbsp;</p>', '   &nbsp;    <h1>presmerovana extrena linka</h1>', '/presmerovanie/', 'Presmerovaná extrená linka', '2020-12-12 16:57:10', '2020-12-12 16:56:00', NULL, 1, 67, 4, 0, 0, 1, 1, 0, '/Test stavov', NULL, 30, -1, -1, -1, NULL, '', NULL, '', '', NULL, 1, NULL, '/novy-adresar-01/presmerovana-extrena-linka.html', 0, 0, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', '', 0, 67, NULL, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, NULL, 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(60434, 'Autotest banner', '<p>Obrázkový banner:</p>\n\n<div class=\"banner-image\">\n!INCLUDE(/components/banner/banner.jsp, group=&quot;autotest-group&quot;, status=enabled, displayMode=2, refreshRate=0, bannerIndex=&quot;&quot;, showInIframe=false, iframeWidth=, iframeHeight=)!\n</div>', '   obrazkovy banner:    \n\n                          \n                                                                                                                                                                                                   \n      <h1>autotest banner</h1>\n', '', 'Autotest banner', '2023-05-11 10:03:52', '2023-05-11 09:54:09', NULL, 18, 18621, 4, 0, 0, 1, 1, 0, '/Aplikácie/Bannerový systém', NULL, 4140, -1, -1, -1, NULL, '', '', '', '', NULL, 1, NULL, '/apps/bannerovy-system/autotest-banner.html', 30608, 0, 0, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', '', 0, 15257, 18621, NULL, -1, -1, -1, -1, NULL, NULL, NULL, NULL, NULL, 0, 1, '', 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(21343, 'Bannerový systém', '<p>HTML&nbsp;Banner:</p>\n\n<div class=\"banner-html\">\n!INCLUDE(/components/banner/banner.jsp, group=&quot;banner-html&quot;, status=enabled, displayMode=2, refreshRate=0, bannerIndex=&quot;&quot;, showInIframe=false, iframeWidth=, iframeHeight=)!\n</div>\n\n<p>Obrázkový banner:</p>\n\n<div class=\"banner-image\">\n!INCLUDE(/components/banner/banner.jsp, group=&quot;banner-image&quot;, status=enabled, displayMode=2, refreshRate=0, bannerIndex=&quot;&quot;, showInIframe=false, iframeWidth=, iframeHeight=)!\n</div>\n\n<p>Obsahový banner:</p>\n\n<div class=\"banner-content\">\n!INCLUDE(/components/banner/banner.jsp, group=&quot;banner-obsahovy&quot;, status=enabled, displayMode=2, bannerIndex=0, videoWrapperClass=, jumbotronVideoClass=, showInIframe=false, refreshRate=0, iframeWidth=0, iframeHeight=0)!\n</div>', '   html      banner:    \n\n                         \n                                                                                                                                                                                                \n      \n\n   obrazkovy banner:    \n\n                          \n                                                                                                                                                                                                 \n      \n\n   obsahovy banner:    \n\n                            \n                                                                                                                                                                                                                                     \n      <h1>bannerovy system</h1>\n', '', 'Bannerový systém', '2025-05-09 11:43:07', '2021-11-23 19:22:00', NULL, 18, 18621, 4, 0, 0, 1, 1, 0, '/Aplikácie/Bannerový systém', NULL, 4120, -1, -1, -1, NULL, '', '', '', '', NULL, 1, NULL, '/apps/bannerovy-system/', 0, 0, 0, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', 'Bez nadpisu', 0, 15257, 18621, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, '', 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(18660, 'test_final', '<p>&nbsp;</p>', '   &nbsp;    <h1>test final</h1>\n', NULL, 'test_final', '2022-03-28 20:07:11', '2022-01-02 00:00:00', '2022-01-02 00:00:00', 18, 84, 1, 0, 0, 1, 1, 0, '/test', NULL, 120, 9780, 1, 9780, ',5,6', NULL, NULL, NULL, NULL, NULL, 1, '2022-01-02 00:00:00', '/tseer/test_final.html', 0, 0, 0, -1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Bez nadpisu', 0, 84, NULL, NULL, -2, 9780, 1, 3, 1, 1, 1, 1, 1, 0, 0, '', 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(274, 'Tento nie je interný', '<p>&nbsp;</p>', '   &nbsp;    <h1>tento nie je interny</h1>\n', '', 'Tento nie je interný', '2022-07-06 12:46:41', '2020-12-13 13:52:00', NULL, 1, 228, 4, 0, 0, 1, 1, 0, '/Test stavov/Tento nie je interný', NULL, 130, -1, -1, -1, NULL, '', '', '', '', ',,', 1, NULL, '/novy-adresar-01/interny/tento-nie-je-interny/', 0, 0, 0, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', '', 0, 67, 228, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, '', 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(183, 'Produktová stránka - B verzia', '<section class=\"prices\">\n<div class=\"container\">\n<div class=\"row\">\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$9,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$19,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$29,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n</div>\n</div>\n</section>\n\n<section class=\"break\" style=\"margin-top: -20px;\">\n<div class=\"container\">\n<div class=\"row justify-content-md-center\">\n<div class=\"col-12 col-md-8\">\n<p class=\"text-center\">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam nec purus quis lectus semper blandit et in leo.</p>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Amet</a></p>\n</div>\n</div>\n</div>\n</section>\n\n<section>\n<div class=\"container\">\n<div class=\"row\">\n<div class=\"col-12 col-md-4\"><img alt=\"demo image\" class=\"w-100\" src=\"/thumb/images/produktova-stranka/money.jpg?w=445&amp;h=360&amp;ip=5\" /></div>\n\n<div class=\"col-12 col-md-7 align-self-center offset-md-1\">\n<h2>Etiam sit amet</h2>\n\n<p>Ut efficitur venenatis erat a&nbsp;facilisis. Ut sit amet ante et eros mollis congue at at nisi. Nunc scelerisque bibendum mauris, et cursus ipsum euismod molestie. Phasellus massa dui, luctus eu accumsan auctor, tristique eu magna. Suspendisse ullamcorper luctus ligula, eu vehicula nisi volutpat id.</p>\n\n<p><a class=\"btn btn-primary\" href=\"#\">Nullam</a></p>\n</div>\n</div>\n</div>\n</section>\n\n<p>&nbsp;</p>', '                        \n                       \n                 \n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $9,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n\n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $19,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n\n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $29,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n      \n      \n          \n\n                                                  \n                       \n                                           \n                             \n                       lorem ipsum dolor sit amet, consectetur adipiscing elit. nullam nec purus quis lectus semper blandit et in leo.    \n\n                                                           amet        \n      \n      \n      \n          \n\n         \n                       \n                 \n                                                                                                                                                   \n\n                                                           \n    etiam sit amet     \n\n   ut efficitur venenatis erat a&nbsp;facilisis. ut sit amet ante et eros mollis congue at at nisi. nunc scelerisque bibendum mauris, et cursus ipsum euismod molestie. phasellus massa dui, luctus eu accumsan auctor, tristique eu magna. suspendisse ullamcorper luctus ligula, eu vehicula nisi volutpat id.    \n\n                                       nullam        \n      \n      \n      \n          \n\n   &nbsp;    <h1>produktova stranka   b verzia</h1>\n<div style=\'display:none\' class=\'fulltextPerex\'>b verzia<br>produktova stranka, ktora je ako samostatna<br>microsite v roznych farebnych variantoch.</div>\n', '', 'Produktová stránka - B verzia', '2025-12-01 23:02:15', '2018-11-19 12:49:00', NULL, 18, 27, 5, 0, 0, 1, 1, 0, '/Newsletter', NULL, 30, -1, -1, -1, '5', '', 'B verzia<br>Produktová stránka, ktorá je ako samostatná<br>microsite v rôznych farebných variantoch.', '', '', ',,', 0, NULL, '/newsletter/produktova-stranka-b-verzia.html', 0, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', 'undefined', 0, 27, NULL, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, NULL, 0);
INSERT INTO `documents` (`doc_id`, `title`, `data`, `data_asc`, `external_link`, `navbar`, `date_created`, `publish_start`, `publish_end`, `author_id`, `group_id`, `temp_id`, `views_total`, `views_month`, `searchable`, `available`, `cacheable`, `file_name`, `file_change`, `sort_priority`, `header_doc_id`, `menu_doc_id`, `footer_doc_id`, `password_protected`, `html_head`, `html_data`, `perex_place`, `perex_image`, `perex_group`, `show_in_menu`, `event_date`, `virtual_path`, `sync_id`, `sync_status`, `logon_page_doc_id`, `right_menu_doc_id`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `disable_after_end`, `forum_count`, `field_m`, `field_n`, `field_o`, `field_p`, `field_q`, `field_r`, `field_s`, `field_t`, `require_ssl`, `root_group_l1`, `root_group_l2`, `root_group_l3`, `temp_field_a_docid`, `temp_field_b_docid`, `temp_field_c_docid`, `temp_field_d_docid`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_menu`, `logged_show_in_navbar`, `logged_show_in_sitemap`, `url_inherit_group`, `generate_url_from_title`, `editor_virtual_path`, `publish_after_start`)
VALUES
	(259, 'Zobrazený v menu', '<p>&nbsp;</p>', '             <h1>zobrazeny v menu</h1>\n', '', 'Zobrazený v menu', '2025-12-02 01:36:51', '2020-12-12 16:42:00', NULL, 18, 221, 4, 0, 0, 1, 1, 0, '/Test stavov/Zobrazený v menu', NULL, 20, -1, -1, -1, NULL, '', '', '', '', ',,', 1, NULL, '/novy-adresar-01/zobrazeny-menu/', 0, 0, 0, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', '', 0, 67, 221, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, '', 0);

INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(15257, 'Aplikácie', 0, 0, 'Aplikácie', 17321, 4, 410, '', 1, 'apps', 30581, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', 'Hodnota 1', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(18621, 'Bannerový systém', 0, 15257, 'Bannerový systém', 21343, 4, 10, NULL, 1, 'bannerovy-system', 30605, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(84, 'test', 0, 0, '<a href=&#47;tseer&#47;>tetste<&#47;a>', 167, 4, 50, '3', 1, 'tseer', 0, 1, '', 171, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, 2, NULL, 2);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(67, 'Test stavov', 0, 0, 'Test stavov', 92, 4, 30, NULL, 2, 'test-stavov', 0, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(221, 'Zobrazený v menu', 0, 67, 'Zobrazený v menu', 111232, 4, 60, NULL, 1, 'zobrazeny-menu', 0, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(27, 'Newsletter', 0, 0, 'Newsletter', 319, 3, 10, '5', 1, 'newsletter', 0, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', 'Hodnota 1', '', '', -1, 0, '', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(83, 'test23', 0, 0, '<a href=\'&#47;teeeeestststst&#47;\'>teeest<&#47;a>', 114, 4, 0, '', 2, 'teeeeestststst', 0, 1, '', 0, 'test23.tau27.iway.sk', -1, '', '', '', '', '', 2, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(9811, 'Slovensky', 0, 0, 'Slovensky', 11036, 4, 10, '', 2, 'slovensky', 944, 1, '', 0, 'mirroring.tau27.iway.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(104, 'Podadresar 1', 0, 84, '<a href=\'&#47;podadresar-1&#47;\'>Podadresar 1<&#47;a>', 133, 4, 30, NULL, 1, 'podadresar-1', 0, 1, '', 0, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `groups` (`group_id`, `group_name`, `internal`, `parent_group_id`, `navbar`, `default_doc_id`, `temp_id`, `sort_priority`, `password_protected`, `menu_type`, `url_dir_name`, `sync_id`, `sync_status`, `html_head`, `logon_page_doc_id`, `domain_name`, `new_page_docid_template`, `install_name`, `field_a`, `field_b`, `field_c`, `field_d`, `logged_menu_type`, `link_group_id`, `lng`, `hidden_in_admin`, `force_group_template`, `show_in_navbar`, `show_in_sitemap`, `logged_show_in_navbar`, `logged_show_in_sitemap`)
VALUES
	(228, 'Tento nie je interný', 0, 67, 'Tento nie je interný', 274, 4, 40, '', 1, 'tento-nie-je-interny', 0, 1, '', -1, 'demo.webjetcms.sk', -1, '', '', '', '', '', -1, 0, 'sk', 0, 0, NULL, NULL, NULL, NULL);

UPDATE groups SET domain_name='demo.webjetcms.sk' WHERE domain_name IS NULL OR domain_name='';
UPDATE groups SET domain_name='demo.webjetcms.sk' WHERE domain_name != 'mirroring.tau27.iway.sk' AND domain_name != 'test23.tau27.iway.sk';

INSERT INTO `templates` (`temp_id`, `temp_name`, `forward`, `lng`, `header_doc_id`, `footer_doc_id`, `after_body_data`, `css`, `menu_doc_id`, `right_menu_doc_id`, `base_css_path`, `object_a_doc_id`, `object_b_doc_id`, `object_c_doc_id`, `object_d_doc_id`, `available_groups`, `template_install_name`, `disable_spam_protection`, `templates_group_id`, `inline_editing_mode`)
VALUES
	(4, 'Article', 'jet/article.jsp', 'sk', 1, 3, '', '', -1, -1, '/templates/aceintegration/jet/assets/css/ninja.min.css\n/templates/aceintegration/jet/assets/fontawesome/css/fontawesome.css\n/templates/aceintegration/jet/assets/fontawesome/css/solid.css', -1, -1, -1, -1, '', '', 0, 2, '');

INSERT INTO templates_group VALUES
	(2, 'Demo JET', 'jet', 'demojet', '');


INSERT INTO enumeration_type (enumeration_type_id, name, string1_name, string2_name, string3_name, string4_name, string5_name, string6_name, string7_name, string8_name, string9_name, string10_name, string11_name, string12_name, decimal1_name, decimal2_name, decimal3_name, decimal4_name, boolean1_name, boolean2_name, boolean3_name, boolean4_name, hidden, child_enumeration_type_id, DTYPE, allow_child_enumeration_type, date1_name, date2_name, date3_name, date4_name, allow_parent_enumeration_data)
VALUES
	(2, 'Okresne Mestá', 'mesto', 'krajina', '', '', '', '', '', '', '', '', '', '', 'latitude', 'longitude', '', '', '', '', '', '', 0, NULL, 'default', 0, '', '', '', '', 1);
INSERT INTO enumeration_data (enumeration_data_id, enumeration_type_id, child_enumeration_type_id, string1, string2, string3, string4, decimal1, decimal2, decimal3, decimal4, boolean1, boolean2, boolean3, boolean4, sort_priority, hidden, DTYPE, string5, string6, date1, date2, date3, date4, parent_enumeration_data_id, string7, string8, string9, string10, string11, string12)
VALUES
	(2, 2, NULL, 'Bánovce nad Bebravou', 'Slovensko', '', '', 48.7167, 18.2667, NULL, NULL, 0, 0, 0, 0, 9, 0, 'default', '', '', NULL, NULL, NULL, NULL, NULL, '', '', '', '', '', '');

INSERT INTO user_perm_groups VALUES
	(2, 'testovacia skupina - VSETKY APLIKacie', '', '', '');
INSERT INTO user_perm_groups VALUES
	(3, 'forTestA', '', '1', '');
INSERT INTO user_perm_groups VALUES
	(4, 'forTestB', '', '', '');

INSERT INTO media_groups VALUES
	(1, 'Media Skupina 1', NULL, NULL);
INSERT INTO media_groups VALUES
	(2, 'Iná skupina 2', '', NULL);

INSERT INTO `_properties_` (`id`, `prop_key`, `prop_value`, `lng`, `update_date`)
VALUES
	(21182, 'components.media.field_a.type', 'autocomplete:Autocomplete Možnosť 1|Autocomplete Iná možnosť|Autocomplete Pokus 3|Chacha funguje', 'sk', '2023-09-18 13:58:57');
INSERT INTO `_properties_` (`id`, `prop_key`, `prop_value`, `lng`, `update_date`)
VALUES
	(39186, 'components.perex.field_a.type', 'autocomplete:Autocomplete Možnosť 1|Autocomplete Iná možnosť|Autocomplete Pokus 3|Chacha funguje', 'sk', '2024-12-11 08:33:49');
INSERT INTO `_properties_` (`id`, `prop_key`, `prop_value`, `lng`, `update_date`)
VALUES
	(39192, 'components.perex.field_b.type', 'boolean', 'sk', '2024-12-11 08:34:35');

DELETE FROM `user_groups` WHERE user_group_id IN (2,3,4);
INSERT INTO `user_groups` (`user_group_id`, `user_group_name`, `user_group_type`, `user_group_comment`, `require_approve`, `email_doc_id`, `allow_user_edit`, `require_email_verification`, `price_discount`)
VALUES
	(2, 'Obchodní partneri', 0, NULL, 0, -1, 0, NULL, 0);
INSERT INTO `user_groups` (`user_group_id`, `user_group_name`, `user_group_type`, `user_group_comment`, `require_approve`, `email_doc_id`, `allow_user_edit`, `require_email_verification`, `price_discount`)
VALUES
	(3, 'Redaktori', 0, '', 0, 68580, 0, 0, 0);
INSERT INTO `user_groups` (`user_group_id`, `user_group_name`, `user_group_type`, `user_group_comment`, `require_approve`, `email_doc_id`, `allow_user_edit`, `require_email_verification`, `price_discount`)
VALUES
	(4, 'Bankári', 0, '', 1, -1, 1, 0, 0);
UPDATE user_groups SET user_group_name='VIP Klienti' WHERE user_group_id=1;
UPDATE user_groups SET user_group_name='Redaktori' WHERE user_group_id=3;

INSERT INTO `users` (`user_id`, `title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`, `mobile_device`, `api_key`)
VALUES
	(2, '', 'Obchodny', 'Partner', 'partner', 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTuzBPw.kBr4ocMbfzVwD3ffc2DVEH1QT.', 0, '2', 'Interway s.r.o.', 'Hattalova 12/a', '', 'partner@fexpost.com', '83103', 'Slovensko', '0903-945990', 1, NULL, '', '', NULL, NULL, NULL, '2024-10-30 10:24:49', '', 'Hodnota 1', '', '', '', NULL, 1, '', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', '', 0, 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTu', 1, NULL, 'bcrypt:$2a$12$6XjthBQuDhTodQUAup60uu|bcrypt:$2a$12$6XjthBQuDhTodQUAup60uuWLYvmTWZ7XZ0LV7Rgtl4htTXMV3zUx6');
INSERT INTO `users` (`user_id`, `title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`, `mobile_device`, `api_key`)
VALUES
	(3, '', 'VIP', 'Klient', 'vipklient', 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTuzBPw.kBr4ocMbfzVwD3ffc2DVEH1QT.', 0, '1,5', 'Interway s.r.o.', 'Hattalova 12/a', '', 'vipklient@fexpost.com', '83103', 'Slovensko', '0903-945990', 1, NULL, '', '', NULL, NULL, NULL, '2022-06-06 13:57:03', '', 'Hodnota 1', '', '', '', NULL, 1, '', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', '', 0, 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTu', 1, NULL, NULL);
INSERT INTO `users` (`user_id`, `title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`, `mobile_device`, `api_key`)
VALUES
	(4, NULL, 'Matej', 'Pavlík', 'arnoldschwarzenegger', 'bcrypt:$2a$12$pqnu2eJLMkU1d8jszOBsOes392DU.c.kmoBsO7GeotsGiNl0ctGUK', 0, '4', NULL, NULL, NULL, 'arnoldschwarzenegger@fexpost.com', NULL, NULL, NULL, 1, NULL, '', '', '2025-12-01 22:26:42', NULL, NULL, '2025-01-28 20:52:55', NULL, 'Hodnota 1', NULL, NULL, NULL, NULL, 1, '/images/gallery/user/arnoldschwarzenegger.jpg', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', 'Osobný bankár', 0, 'bcrypt:$2a$12$pqnu2eJLMkU1d8jszOBsOe', 1, NULL, NULL);
INSERT INTO `users` (`user_id`, `title`, `first_name`, `last_name`, `login`, `password`, `is_admin`, `user_groups`, `company`, `adress`, `city`, `email`, `PSC`, `country`, `phone`, `authorized`, `editable_groups`, `editable_pages`, `writable_folders`, `last_logon`, `module_perms`, `disabled_items`, `reg_date`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `date_of_birth`, `sex_male`, `photo`, `signature`, `forum_rank`, `rating_rank`, `allow_login_start`, `allow_login_end`, `authorize_hash`, `fax`, `delivery_first_name`, `delivery_last_name`, `delivery_company`, `delivery_adress`, `delivery_city`, `delivery_psc`, `delivery_country`, `delivery_phone`, `position`, `parent_id`, `password_salt`, `domain_id`, `mobile_device`, `api_key`)
VALUES
	(5, '', 'Filip', 'Lukáč', 'sylvesterstallone', 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTuzBPw.kBr4ocMbfzVwD3ffc2DVEH1QT.', 0, '4', '', '', '', 'sylvesterstallone@fexpost.com', '', '', '', 1, '1,31', '10,13', '', '2022-05-26 09:28:27', NULL, NULL, '2025-03-06 19:51:12', '', '', '', '', '', NULL, 1, '/images/gallery/user/sylvesterstallone.jpg', '', 0, 0, NULL, NULL, NULL, '', '', '', '', '', '', '', '', '', 'Osobný bankár', 0, 'bcrypt:$2a$12$c.Oqx1kz8x/t6sh3GP1CTu', 1, NULL, NULL);

INSERT INTO `banner_banners` (`banner_id`, `banner_type`, `banner_group`, `priority`, `active`, `banner_location`, `banner_redirect`, `width`, `height`, `html_code`, `date_from`, `date_to`, `max_views`, `max_clicks`, `stat_views`, `stat_clicks`, `stat_date`, `name`, `target`, `click_tag`, `frame_rate`, `client_id`, `domain_id`, `visitor_cookie_group`, `image_link`, `image_link_mobile`, `primary_header`, `secondary_header`, `description_text`, `primary_link_title`, `primary_link_url`, `primary_link_target`, `secondary_link_title`, `secondary_link_url`, `secondary_link_target`, `campaign_title`, `only_with_campaign`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`)
VALUES
	(123, 1, 'banner-image', 10, 1, '/images/bannery/banner-iwayday.png', '/investicie/', 0, 0, '', '1970-01-01 00:00:00', '2099-12-31 00:00:00', 0, 0, 13933, 37, '2025-12-01 19:57:15', 'Obrázkový banner', '_self', NULL, 0, -1, 1, NULL, '', '', '', '', '', '', '', '_self', '', '', '_self', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `banner_banners` (`banner_id`, `banner_type`, `banner_group`, `priority`, `active`, `banner_location`, `banner_redirect`, `width`, `height`, `html_code`, `date_from`, `date_to`, `max_views`, `max_clicks`, `stat_views`, `stat_clicks`, `stat_date`, `name`, `target`, `click_tag`, `frame_rate`, `client_id`, `domain_id`, `visitor_cookie_group`, `image_link`, `image_link_mobile`, `primary_header`, `secondary_header`, `description_text`, `primary_link_title`, `primary_link_url`, `primary_link_target`, `secondary_link_title`, `secondary_link_url`, `secondary_link_target`, `campaign_title`, `only_with_campaign`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`)
VALUES
	(124, 4, 'banner-obsahovy', 10, 1, '/apps/bannerovy-system/', '/investicie/', 0, 0, '', '1970-01-01 00:00:00', '2099-12-31 00:00:00', 0, 0, 7956, 0, '2025-12-01 19:55:44', 'Obsahový banner', '_self', NULL, 0, -1, 1, NULL, '/images/gallery/test-vela-foto/o_img04152.jpg', '/images/gallery/test-vela-foto/o_img04082.jpg', 'Primárny nadpis', 'Sekundárny nadpis', '<p>Naše výhody:</p><ul><li>Zrýchlenie 4,5s/100 km/h</li><li>Dojazd: 423 km</li></ul>', 'Primárny odkaz', '/', '_self', 'Sekundárny odkaz', '/sekundarny', '_self', '', 0, '', '', '', '', '', '');

INSERT INTO media VALUES
	(62, 259, 'documents', 'www.sme.sk', NULL, NULL, NULL, 'www.sme.sk', '/images/bannery/banner-iwayday.png', 'Media Skupina 1', NULL, NULL, NULL, NULL, 20, '2020-12-28 17:29:45', 1, '', '', '', '', 'false', '');

INSERT INTO `calendar_types` (`type_id`, `name`, `schvalovatel_id`, `domain_id`)
VALUES
	(434, 'DomainTest_Test23_type', -1, 83);

INSERT INTO `basket_delivery_methods` (`id`, `delivery_method_name`, `supported_countries`, `price`, `vat`, `sort_priority`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `domain_id`, `title`)
VALUES
	(4, 'sk.iway.iwcm.components.basket.delivery_methods.rest.ByMailService', '.sk', 5.00, 23, 0, '', '', '', '', '', '', '', '', '', '', '', '', 1, 'Štandardná pošta');
INSERT INTO `basket_delivery_methods` (`id`, `delivery_method_name`, `supported_countries`, `price`, `vat`, `sort_priority`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `domain_id`, `title`)
VALUES
	(5, 'sk.iway.iwcm.components.basket.delivery_methods.rest.InStoreService', '.sk+.cz+.pl', 0.00, 0, 0, '', '', '', '', '', '', '', '', '', '', '', '', 1, NULL);
INSERT INTO `basket_delivery_methods` (`id`, `delivery_method_name`, `supported_countries`, `price`, `vat`, `sort_priority`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `domain_id`, `title`)
VALUES
	(6, 'sk.iway.iwcm.components.basket.delivery_methods.rest.ByMailService', '.sk', 12.00, 23, 0, '', '', '', '', '', '', '', '', '', '', '', '', 1, 'Express 24h');
INSERT INTO `basket_delivery_methods` (`id`, `delivery_method_name`, `supported_countries`, `price`, `vat`, `sort_priority`, `field_a`, `field_b`, `field_c`, `field_d`, `field_e`, `field_f`, `field_g`, `field_h`, `field_i`, `field_j`, `field_k`, `field_l`, `domain_id`, `title`)
VALUES
	(7, 'sk.iway.iwcm.components.basket.delivery_methods.rest.ByMailService', '.sk', 12.00, 23, 0, '', '', '', '', '', '', '', '', '', '', '', '', 1, 'components.basket.order_form.delivery_courier');
```

## Testing REST services

CodeceptJS also supports [testing of REST services](https://codecept.io/helpers/REST/). The setting is in `codecept.conf.js`:

```javascript
exports.config = {
  helpers: {
    REST: {
      defaultHeaders: {
        'x-auth-token': 'dGVzd.....g8'
      },
    },
    JSONResponse: {},
  }
}
```

Example of calling REST services and testing the returned state, login and JSON object:

```javascript
Before(({ I }) => {
    I.amOnPage('/logoff.do?forward=/admin/');
});

Scenario("API volanie", ({ I }) => {
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25');
    I.seeResponseCodeIs(200);
    I.seeResponseContainsKeys(['numberOfElements', 'totalPages']);
    I.seeResponseContainsJson({
        content: [
            {
                "id":11,
                "groupId":25,
                "title":"Produktová stránka"
            }
        ]
    })
});

Scenario("API volanie zle heslo", ({ I }) => {
    I.sendGetRequest('/admin/rest/web-pages/all?groupId=25', {
        'x-auth-token': 'dGVzd7VyOmNrTzaIfXRid05bTEldfGx3OURUa2sqQ1pOVnJ+Njg8'
    });
    I.seeResponseCodeIs(401);
});
```
