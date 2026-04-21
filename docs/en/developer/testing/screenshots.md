# Generating documentation screenshots

Screenshots (```screenshot```) for the manual/documentation need to be generated automatically so that they can be regenerated at any time (e.g. after a design change). The CodeceptJS framework used for automated testing is used to generate them. The scripts are therefore very similar to those for testing.

There are special functions for documentation purposes:

- ```Document.screenshot(screenshotFilePath, width, height)``` - ​​takes a screenshot of the entire screen, the parameters ```width``` and ```height``` are optional.
- ```Document.screenshotElement(selector, screenshotFilePath, width, height)``` - ​​creates a snapshot of the element according to the specified ```selector``` parameter.
- ```Document.screenshotAppEditor(docId, path, callback, width, height)``` - ​​creates a snapshot of the application settings in the editor (e.g. gallery).

!>**Warning:** Screenshots are automatically saved to the ```/docs``` directory. The file ```/docs/redactor/webpages/domain-select.png``` will be created for the specified path ```/redactor/webpages/domain-select.png```.

[Example](../../../src/test/webapp/screenshots/generator/manual-redactor.js) script for creating screenshots:

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

```shell
cd src/test/webapp
#spustenie generovania vsetkych screenshotov
npm run scr
#spustenie generovania screenshotov z konkretneho suboru
npm run scr screenshots/generator/manual-sysadmin.js
#spustenie generovania screenshotov konkrétneho scenára s hodnotou ```@current``` v názve
npm run scr:current
```

## Editing CSS styles

In some cases, when creating a screenshot of an element, it is appropriate to have a larger part (the edges) displayed, or to set the appropriate CSS class to the element (e.g. to frame the element). From an automated test, it is possible to execute JavaScript code in the context of the page. An example is in the test [manual-redactor.js](../../../src/test/webapp/screenshots/generator/manual-redactor.js):

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

where the top and bottom indents are set for both field A and field B in the editor. A larger bottom indent is set for field B, since we want the selection field and its values ​​to be displayed on the screen.

## Image comparison

Often GIT marks an image as changed even though it looks the same. You can run the script [rm-same-images.sh](../../../../src/test/webapp/rm-same-images.sh), which uses ImageMagick to compare the new and original images. If the difference is less than 1%, it reverts the image to its original state. Images with changes highlighted are created in the `/build/images-diff` folder.