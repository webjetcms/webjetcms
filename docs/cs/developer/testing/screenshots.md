# Generování snímků dokumentace

Snímky obrazovky (`screenshot`) pro příručku/dokumentaci by měly být generovány automaticky, aby je bylo možné kdykoli (např. po změně návrhu) znovu vygenerovat. K jejich generování se používá framework CodeceptJS používaný pro automatizované testování. Skripty jsou tedy velmi podobné těm pro testování.

Pro účely dokumentace jsou k dispozici speciální funkce:
- `Document.screenshot(screenshotFilePath, width, height)` - pořídí snímek celé obrazovky, parametry `width` a `height` jsou nepovinné.
- `Document.screenshotElement(selector, screenshotFilePath, width, height)` - vytvoří snímek prvku podle zadaného parametru `selector` parametr.
- `Document.screenshotAppEditor(docId, path, callback, width, height)` - pořídí snímek nastavení aplikace v editoru (např. galerie).

!>**Varování:** snímky obrazovky se automaticky ukládají do `/docs` Adresář. Pro zadanou cestu `/redactor/webpages/domain-select.png` bude vytvořen soubor `/docs/redactor/webpages/domain-select.png`.

[Příklad](../../../src/test/webapp/screenshots/generator/manual-redactor.js) scénáře pro vytváření snímků obrazovky:

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

Chcete-li vygenerovat snímky obrazovky, použijte následující příkaz:

````shell
cd src/test/webapp
#spustenie generovania vsetkych screenshotov
npm run scr
#spustenie generovania screenshotov z konkretneho suboru
npm run scr screenshots/generator/manual-sysadmin.js
#spustenie generovania screenshotov konkrétneho scenára s hodnotou ```@current``` v názve
npm run scr:current
````

## Úprava stylů CSS

V některých případech je při vytváření snímku prvku užitečné nechat zobrazit jeho větší část (okraje) nebo nastavit prvku vhodnou třídu CSS (např. pro orámování prvku). Z automatizovaného testu je možné v kontextu stránky spustit kód JavaScriptu. Příkladem je test [manual-redactor.js](../../../src/test/webapp/screenshots/generator/manual-redactor.js):

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

kde jsou pole A i pole B v editoru nastavena tak, aby byla odsazena shora a zdola. Pro pole B je nastaveno větší odsazení odspodu, protože chceme, aby se pole výběru a jeho hodnoty zobrazovaly na obrázku.
