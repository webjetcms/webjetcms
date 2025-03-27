# Generování snímků obrazovky dokumentace

Snímky obrazovky (`screenshot`) pro manuál/dokumentaci je třeba generovat automatizovaně, aby je bylo možné kdykoliv znovu vygenerovat (např. po změně designu). Pro jejich generování je využit CodeceptJS framework používaný pro automatizované testování. Skripty jsou tedy velmi podobné těm pro testování.

Pro potřeby dokumentace existují speciální funkce:
- `Document.screenshot(screenshotFilePath, width, height)` - pořídí snímek celé obrazovky, parametry `width` a `height` jsou volitelné.
- `Document.screenshotElement(selector, screenshotFilePath, width, height)` - pořídí snímek elementu podle zadaného `selector` parametru.
- `Document.screenshotAppEditor(docId, path, callback, width, height)` - pořídí snímek nastavení aplikace v editoru (např. galerie).

!>**Upozornění:** snímky obrazovky jsou automaticky ukládány do `/docs` adresáře. Pro zadanou cestu `/redactor/webpages/domain-select.png` vznikne soubor `/docs/redactor/webpages/domain-select.png`.

[Příklad](../../../src/test/webapp/screenshots/generator/manual-redactor.js) scénáře pro pořízení snímků obrazovky:

```javascript
Feature('manual-redactor');

Before(({ I, login }) => {
    login('admin');
});

Scenario('web-pages-list', ({ I, DT, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list");

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

Snímky obrazovky vygenerujete následujícím příkazem:

````shell
cd src/test/webapp
#spustenie generovania vsetkych screenshotov
npm run scr
#spustenie generovania screenshotov z konkretneho suboru
npm run scr screenshots/generator/manual-sysadmin.js
#spustenie generovania screenshotov konkrétneho scenára s hodnotou ```@current``` v názve
npm run scr:current
````

## Úprava CSS stylů

V některých případech při pořízení snímku elementu je vhodné mít zobrazenou větší část (okraje), případně elementu nastavit vhodnou CSS třídu (např. pro orámování elementu). Z automatizovaného testu lze provést JavaScript kód v kontextu stránky. Příklad je v testu [manual-redactor.js](../../../src/test/webapp/screenshots/generator/manual-redactor.js):

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

kde pro pole A i pole B v editoru je nastaveno odsazení shora a dolů. Pro pole B je nastaveno větší odsazení z dolů, jelikož na snímek chceme mít zobrazeno i výběrové pole a jeho hodnoty.
