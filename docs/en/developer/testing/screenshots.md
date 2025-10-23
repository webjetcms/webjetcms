# Generate documentation screenshots

Screenshots (`screenshot`) for the manual/documentation should be generated automatically so that they can be re-generated at any time (e.g. after a design change). CodeceptJS framework used for automated testing is used for their generation. The scripts are thus very similar to those for testing.

There are special functions for documentation purposes:
- `Document.screenshot(screenshotFilePath, width, height)` - takes a snapshot of the entire screen, parameters `width` a `height` are optional.
- `Document.screenshotElement(selector, screenshotFilePath, width, height)` - creates a snapshot of the element according to the specified `selector` parameter.
- `Document.screenshotAppEditor(docId, path, callback, width, height)` - takes a snapshot of the application settings in the editor (e.g. gallery).

!>**Warning:** screenshots are automatically saved to `/docs` Directory. For the specified path `/redactor/webpages/domain-select.png` a file will be created `/docs/redactor/webpages/domain-select.png`.

[Example](../../../src/test/webapp/screenshots/generator/manual-redactor.js) scenarios to create screenshots:

```javascript
Feature('manual-redactor');

Before(({ I, login }) => {
    login('admin');
});

Scenario('web-pages-list', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");

    //domenovy selektor
    I.click("div.js-domain-toggler div.bootstrap-select button");
    Document.screenshot("/redactor/webpages/domain-select.png", 1280, 220);
    I.click("div.js-domain-toggler div.bootstrap-select button");

    //priecinky system/kod
    I.clickCss("#pills-system-tab");
    DT.waitForLoader();
    Document.screenshotElement("div.tree-col", "/redactor/webpages/system-folder.png", 1280, 300);

    //screenshot nastavenia aplikacie galeria
    Document.screenshotAppEditor(45926, "/redactor/apps/gallery/editor-dialog.png", function(Document, I, DT, DTE) {
        //prepni sa na prvu kartu
        I.clickCss("#tabLink1");
    }, 1280, 800);
});
```

To generate screenshots, use the following command:

````shell
cd src/test/webapp
#spustenie generovania vsetkych screenshotov
npm run scr
#spustenie generovania screenshotov z konkretneho suboru
npm run scr screenshots/generator/manual-sysadmin.js
#spustenie generovania screenshotov konkrétneho scenára s hodnotou ```@current``` v názve
npm run scr:current
````

## Modifying CSS styles

In some cases, when creating a snapshot of an element, it is useful to have a larger part (margins) displayed, or to set a suitable CSS class to the element (e.g. for framing the element). From the automated test, it is possible to execute JavaScript code in the context of the page. An example is in the test [manual-redactor.js](../../../src/test/webapp/screenshots/generator/manual-redactor.js):

```javascript
Scenario('custom-fields', async({ I, Document }) => {

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-bottom", "175px");
    });

    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldA", "/frontend/webpages/customfields/webpages-text.png");

    I.click("div.DTE_Action_Edit div.DTE_Field_Name_fieldB button.dropdown-toggle")
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldB", "/frontend/webpages/customfields/webpages-select.png");
    I.pressKey('Escape');
});
```

where both field A and field B in the editor are set to be offset from top and bottom. For field B, a larger bottom offset is set, as we want to have the selection field and its values displayed on the image.
