# Generovanie snímkov obrazovky dokumentácie

Snímky obrazovky (```screenshot```) pre manuál/dokumentáciu je potrebné generovať automatizovane, aby ich bolo možné kedykoľvek znova vygenerovať (napr. po zmene dizajnu). Pre ich generovanie je využitý CodeceptJS framework používaný pre automatizované testovanie. Skripty sú teda veľmi podobné tým pre testovanie.

Pre potreby dokumentácie existujú špeciálne funkcie:

- ```Document.screenshot(screenshotFilePath, width, height)``` - vytvorí snímku celej obrazovky, parametre ```width``` a ```height``` sú voliteľné.
- ```Document.screenshotElement(selector, screenshotFilePath, width, height)``` - vytvorí snímku elementu podľa zadaného ```selector``` parametra.
- ```Document.screenshotAppEditor(docId, path, callback, width, height)``` - vytvorí snímku nastavenia aplikácie v editore (napr. galérie).

!>**Upozornenie:** snímky obrazovky sú automaticky ukladané do ```/docs``` adresára. Pre zadanú cestu ```/redactor/webpages/domain-select.png``` vznikne súbor ```/docs/redactor/webpages/domain-select.png```.

[Príklad](../../../src/test/webapp/screenshots/generator/manual-redactor.js) scenára pre vytvorenie snímkov obrazovky:

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
    I.click("#pills-system-tab");
    DT.waitForLoader();
    Document.screenshotElement("div.tree-col", "/redactor/webpages/system-folder.png", 1280, 300);

    //screenshot nastavenia aplikacie galeria
    Document.screenshotAppEditor(45926, "/redactor/apps/gallery/editor-dialog.png", function(Document, I, DT, DTE) {
        //prepni sa na prvu kartu
        I.click("#tabLink1");
    }, 1280, 800);
});
```

Snímky obrazovky vygenerujete nasledovným príkazom:

```shell
cd src/test/webapp
#spustenie generovania vsetkych screenshotov
npm run scr
#spustenie generovania screenshotov z konkretneho suboru
npm run scr screenshots/generator/manual-sysadmin.js
#spustenie generovania screenshotov konkrétneho scenára s hodnotou ```@current``` v názve
npm run scr:current
```

## Úprava CSS štýlov

V niektorých prípadoch pri vytvorení snímky elementu je vhodné mať zobrazenú väčšiu časť (okraje), prípadne elementu nastaviť vhodnú CSS triedu (napr. na orámovanie elementu). Z automatizovaného testu je možné vykonať JavaScript kód v kontexte stránky. Príklad je v teste [manual-redactor.js](../../../src/test/webapp/screenshots/generator/manual-redactor.js):

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

kde pre pole A aj pole B v editore je nastavené odsadenie z hora a dola. Pre pole B je nastavené väčšie odsadenie z dola, keďže na snímku chceme mať zobrazené aj výberové pole a jeho hodnoty.