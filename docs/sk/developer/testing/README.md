# Automatizované testovanie

Pre automatizované E2E testovanie je používaný framework [CodeceptJS](https://codecept.io). Testy sa píšu v JavaScript-e a prakticky ovládajú prehliadač v ktorom test prebieha. Viac informácií prečo sme zvolili tento framework je v sekcii [Playwright + CodeceptJS](#playwright--codeceptjs).


<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Automatizované testovanie](#automatizované-testovanie)
  - [Inštalácia](#inštalácia)
  - [Spustenie testovania](#spustenie-testovania)
    - [Codecept UI](#codecept-ui)
    - [Generovanie HTML reportu](#generovanie-html-reportu)
  - [Playwright + CodeceptJS](#playwright--codeceptjs)
    - [Konfigurácia](#konfigurácia)
  - [Písanie testov](#písanie-testov)
    - [Lokátory](#lokátory)
    - [Within](#within)
    - [Playwright metódy](#playwright-metódy)
    - [WebJET doplnkové funkcie](#webjet-doplnkové-funkcie)
    - [Čakanie na dokončenie](#čakanie-na-dokončenie)
    - [Pause](#pause)
    - [Prihlasovanie](#prihlasovanie)
    - [Assert knižnica](#assert-knižnica)
    - [Page objekty](#page-objekty)
    - [Detekcia prehliadača](#detekcia-prehliadača)
  - [Odobratie práva](#odobratie-práva)
  - [Vizuálne testovanie](#vizuálne-testovanie)
  - [Best practices](#best-practices)
    - [Názvoslovie](#názvoslovie)
    - [Testovacie dáta](#testovacie-dáta)
    - [Selektory](#selektory)
    - [Časovanie](#časovanie)
    - [Dĺžka scenára](#dĺžka-scenára)
    - [Debugovanie](#debugovanie)
  - [Zmazanie databázy](#zmazanie-databázy)
  - [Testovanie REST služieb](#testovanie-rest-služieb)

<!-- /code_chunk_output -->


## Inštalácia

```shell
cd src/test/webapp/
npm install
```

!>**Upozornenie:** pred spustením testovania je potrebné skompilovať JS/CSS admin časti WebJETu:

```shell
cd src/main/webapp/admin/v9/
npm install
npm run prod
```

a spustiť aplikačný server:

```shell
gradlew appRun
```

každý z uvedených príkazov vám odporúčam spustiť v samostatnom Termináli (menu Terminal->New Terminal). Medzi spustenými terminálmi si môžete prepínať v okne ```Terminal```.

## Spustenie testovania

Testovanie spustíte pomocou nasledovných príkazov:

```shell
cd src/test/webapp/

#spustenie vsetkych testov
npm run all

#spustenie konkrétneho testu a zastavenie v prípade chyby
npm run pause tests/components/gallery_test.js

#Spustenie konkrétneho scenára s hodnotou ```@current``` v názve
npm run current
```

Pre spustenie v prehliadači firefox použite prefix ```ff:``` pred názvom:

```shell
npm run ff:all
npm run ff:pause tests/components/gallery_test.js
npm run ff:current
```

Spustenie na inej URL s vypnutým zobrazením prehliadača a prehliadačom ```firefox```:

```shell
CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npm run all
```

**Poznámka:** v prehliadači Firefox sme mali problémy s rýchlosťou testov. Preto sa pre tento prehliadač v súbore ```codecept.conf.js``` nastaví premenná ```autodeayEnabled``` na hodnotu ```true``` a aktivuje sa doplnok ```autodelay```. Ten oneskoruje vykonanie funkcií ```amOnPage,click,forceClick``` o 200ms pred a 300ms po zavolaní príkazu. Tiež sme identifikovali zvláštne správanie prehliadača, ktorý pokiaľ nie je na popredí tak testy z ničoho nič prestanú fungovať a zobrazuje nezmyselné chyby. Pri jednorázovom spustení testu sa vždy test vykonal korektne. Prisudzujeme to nejakej optimalizácii vykonávania JavaScript kódu v prehliadači, keď nie je aktívny. Pri spustení s nezobrazením prehliadača je všetko v poriadku, preto pre spustenie všetkých testov vždy používajte nastavenie ```CODECEPT_SHOW=false```.

### Codecept UI

Codecept ponúka v beta verzii UI pre zobrazenie testovania, spustíte ho príkazom:

```shell
npm run codeceptjs:ui
```

a následne v prehliadači otvoríte stránku ```http://localhost:3001```.

### Generovanie HTML reportu

**Mochawesome**

V npm je nastavený [plugin pre generovanie HTML reportov](https://codecept.io/reports/#html). Vygenerujete ho spustením príkazu:

```shell
npm run codeceptjs --reporter mochawesome
```
a do adresára /build/test/report sa vygeneruje HTML report s výsledkom testu. Pre neúspešné testy sa vytvorí aj fotka obrazovky. Nastavenie je v [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) v sekcii ```mocha```.

**Allure**

Report je možné generovať aj cez [allure](allure.md) spustením testu:

```shell
npm run codeceptjs --plugins allure
```

Po dokončení testu môžete zobraziť výsledky spustením ```allure``` servera:

```shell
allure serve ../../../build/test
```

## Playwright + CodeceptJS

Na testovanie sa používa [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Prečo Playwright?

- je to 3. generácia testovacieho frameworku (1. generácia Selenium, 2. generácia Puppeteer, 3. generácia Playwright)
- Microsoft kúpil autorov ```Puppeteer``` frameworku a oni vyvíjajú Playwright, takže majú skúsenosť
- podporuje ```chromium, firefox, webkit``` (2021 májova aktualizácia Windows prepne Edge na chromium jadro)
- vie emulovať rozlíšenia, user agenta, DPI

Prečo CodeceptJS?

- Playwright rovnako ako ```Puppeteer``` je komunikačný (low level) protokol pre ovládanie prehliadača (jeho automatizáciu)
- CodeceptJS je testovací framework, ktorý okrem iného vie používať Playwright
- testovací kód sa píše v JavaScript-e
- kód testov je veľmi [zrozumiteľný](https://codecept.io/playwright/#setup)
- má pokročilé možnosti [Lokátorov](https://codecept.io/locators/#css-and-xpath) - hľadanie elementov podľa textu, css, xpath
- má [GUI](https://codecept.io/ui/) (zatiaľ nevyskúšané) na písanie a zobrazenie výsledku testov

### Konfigurácia

Základná konfigurácia je v súbore ```codecept.conf.js```. Dôležité atribúty:

- ```url``` (http://iwcm.interway.sk) - adresa (doména) servera. Ten môžete zmeniť cez ```--override``` parameter a prepnúť testovanie z DEV na TEST/PROD prostredie.
- ```output``` (../../../build/test) - adresár do ktorého sa vám vygeneruje fotka obrazovky v prípade neúspešného testu (predvolený na ```build/test``` v koreňovom adresári)
- ```browser``` (chromium) - zvolený prehliadač na spustenie testov, môže byť ```chromium, firefox, webkit```
- ```emulate``` (zakomentovane) - [emulácia](https://github.com/Microsoft/playwright/blob/master/src/deviceDescriptors.ts) zariadenia
- ```screenshotOnFail``` - zapne/vypne vytváranie screenshotov v prípade neúspešného testu

## Písanie testov

Testy sa vytvárajú v pod adresároch tests, kde sú delené podľa jednotlivých modulov/aplikácií WebJETu. Sú písane v jazyku JavaScript, takže je možné využívať všetky možnosti, ktoré vám JavaScript ponuka.

Príklad komplexnejšieho testu na otestovanie prihlásenia [src/test/webapp/tests/admin/login.js](../../../../src/test/webapp/tests/admin/login.js):

!>**Upozornenie:** do ```Feature``` zápisu zadávajte hodnotu vo formáte ```adresár.podadresár.meno-súboru``` pre korektné zobrazenie testov v stromovej štruktúre a ľahké dohľadanie súboru podľa vypísaného ```Feature``` v log súbore.

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

Všimnite si, že v ukážke schválne používam rôzne lokátory/selektory (meno fieldu, text/label, CSS selector). To je jedna z výhod CodeceptJS. Viac o možnostiach Lokátorov je v [dokumentácii](https://codecept.io/locators/#css-and-xpath).

### Lokátory

Lokátory (selectory), ktoré vyberajú element na stránke sú dobre popísané v [oficiálnej dokumentácii](https://codecept.io/locators/).

```
{permalink: /'foo'} matches <div id="foo">
{name: 'foo'} matches <div name="foo">
{css: 'input[type=input][value=foo]'} matches <input type="input" value="foo">
{xpath: "//input[@type='submit'][contains(@value, 'foo')]"} matches <input type="submit" value="foobar">
{class: 'foo'} matches <div class="foo">
```

### Within

Pomocou zápisu ```within``` môžete obmedziť element, na ktorý sa použijú nasledovné príkazy:

```javascript
within("div.breadcrumb-language-select", () => {
    I.click("Slovenský jazyk");
    I.click("Chorvátsky jazyk");
});
```

zároveň väčšina príkazov umožňuje zapísať aj selektor do príkazu, vyššie uvedené sa dá zapísať aj ako:

```javascript
  I.click("Slovenský jazyk", "div.breadcrumb-language-select");
  I.click("Chorvátsky jazyk", "div.breadcrumb-language-select");
```

### Playwright metódy

V [oficiálnej dokumentácii](https://codecept.io/helpers/Playwright/) je zoznam všetkých možností ```I``` objektu. Krátke odkazy:

- [pressKey](https://codecept.io/helpers/Playwright/#presskey)
- [click](https://codecept.io/helpers/Playwright/#click)
- [forceClick](https://codecept.io/helpers/Playwright/#forceclick) - vynútené kliknutie bez čakania na udalosť, je potrebné použiť na custom ```checkboxy``` (inak sa tam zacyklí)
- [see](https://codecept.io/helpers/Playwright/#see) / [dontSee](https://codecept.io/helpers/Playwright/#dontsee)
- [seeElement](https://codecept.io/helpers/TestCafe/#seeelement) / [dontSeeElement](https://codecept.io/helpers/Detox/#dontseeelement)
- [fillField](https://codecept.io/helpers/Playwright/#fillfield)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [wait](https://codecept.io/helpers/Playwright/#wait)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [executeScript](https://codecept.io/helpers/Playwright/#executescript)
- [saveScreenshot](https://codecept.io/helpers/Playwright/#savescreenshot)

### WebJET doplnkové funkcie

Pre WebJET sme doplnili niekoľko užitočných funkcií:

- [I.formatDateTime(timestamp)](../../../../src/test/webapp/steps_file.js) - naformátuje timestamp na dátum a čas s využitím moment knižnice
- [I.seeAndClick(selector)](../../../../src/test/webapp/steps_file.js) - počká na zobrazenie elementu a následne naň klikne
- [await I.clickIfVisible(selector)](../../../../src/test/webapp/custom_helper.js) - ak je daný element zobrazený klikne naň, ak zobrazený nie je preskočí krok (nevyhodí chybu)
- [I.verifyDisabled(selector)](../../../../src/test/webapp/custom_helper.js) - overí, či dané pole je neaktívne
- [I.wjSetDefaultWindowSize()](../../../../src/test/webapp/steps_file.js) - nastaví predvolenú veľkosť okna po jeho zmene, je volané automaticky aj po prihlásení v prihlasovacej sekvencii v [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js)
- [Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)](../../../../src/test/webapp/codecept.conf.js) - vykoná [vizuálne porovnanie](#vizuálne-testovanie)
- `I.waitForTime(time)` - čakanie do zadaného času (timestamp).
- `I.toastrClose()` - zatvorenie okna `toastr` notifikácie.
- `clickCss(name, parent=null)` - vykoná kliknutie rovnako ako `I.click` ale `name` považuje za CSS selektor - vykonanie je rýchlejšie, nie je potrebné použiť obalenie do `{css: name}`.
- `forceClickCss(name, parent=null)` - vykoná kliknutie rovnako ako `I.forceClick` ale `name` považuje za CSS selektor - vykonanie je rýchlejšie, nie je potrebné použiť obalenie do `{css: name}`.

Pre datatabuľku máme pripravené špeciálne funkcie. Sú implementované v [DT.js](../../../../src/test/webapp/pages/DT.js):

- ```DT.waitForLoader(name)``` - čaká na zobrazenie a následné schovanie informácie "Spracúvam" v datatabuľke. Používa sa ako ```DT.waitForLoader("#forms-list_processing");```
- ```DT.filter(name, value, type=null)``` - nastaví hodnotu ```value``` do textového filtra stĺpca ```name``` datatabuľky. Ak je zadaný aj atribút ```type``` nastaví sa typ hľadania (napr. Začína na, Končí na, Rovná sa).
- ```DT.filterSelect(name, value)``` - nastaví hodnotu ```value``` do výberového poľa (select) filtra stĺpca ```name``` datatabuľky. Používa sa ako ```DT.filterSelect('cookieClass', 'Neklasifikované');```
- ```async I.getDataTableColumns(dataTableName)``` - vráti objekt DATA s definíciou datatabuľky, používa sa v automatickom testovaní datatabuľky
- ```async getDataTableId(dataTableName)``` - vráti ID datatabuľky, volá JS funkciu ```dataTable.DATA.id```
- [async I.getTotalRows()](../../../../src/test/webapp/custom_helper.js) - vráti celkový počet záznamov v datatabuľke
- ```DT.deleteAll(name = "datatableInit")``` - zmaže aktuálne zobrazené záznamy, pred použitím vždy použite ```DT.filter``` pre filtrovanie potrebných údajov.

Pre Datatable Editor implementované v [DTE.js](../../../../src/test/webapp/pages/DTE.js):

- ```DTE.waitForLoader(name)``` - čaká na schovanie ```loadera``` v editore (uloženie záznamu)
- ```DTE.waitForEditor(name)``` - čaká na zobrazenie editora, ak je definované name, použije sa datatabuľka s daným menom, predvolene ```datatableInit```
- ```DTE.selectOption.(name, text)``` - vyberie hodnotu v select boxe (korektným spôsobom zobrazením možností a následným kliknutím na možnosť)
- ```DTE.save(name)``` - klikne na tlačidlo Uložiť v editore, ak je definované name, použije sa datatabuľka s daným menom, predvolene ```datatableInit```
- ```DTE.cancel(name)``` - klikne na tlačidlo zatvorenia editora, ak je definované name, použije sa datatabuľka s daným menom, predvolene ```datatableInit```
- ```DTE.fillField(name, value)``` - vyplní štandardné pole, na rozdiel od volania ```I.fillField``` je možné do ```name``` parametra zadať priamo meno poľa na backende/json definícii.
- ```DTE.fillQuill(name, value)``` - vyplní hodnotu do poľa typy ```QUILL```.
- ```DTE.fillCkeditor(htmlCode)``` - nastaví HTML kód do aktuálne zobrazeného CKEditor-a.
- ```DTE.fillCleditor(parentSelector, value)``` - zadá text do WYSIWYG ```cleditor```. Hodnota ```parentSelector``` - odkaz na element v ktorom sa ```cleditor``` nachádza (napr. ```#forum```), ```value``` - hodnota na vyplnenie. **Upozornenie:** nevie to zatiaľ diakritiku z dôvodu použitia ```type``` príkazu.
Pre datatabuľku je možné vykonať aj [automatizovaný test](datatable.md).
- `DTE.appendField(name, value)` - doplní text to poľa v editore, rieši problém s použitím `I.appendField`, ktoré sa v editore nevykoná vždy správne.

Pre JsTree (stromovú štruktúru):

- ```I.jstreeClick(name)``` - klikne na zvolený text v jstree (dôležité použiť hlavne vo web stránkach kde je linka s rovnakým menom ako adresár aj v zozname stránok)
- ```I.createFolderStructure(randomNumber)``` - pripraví stromovú štruktúru adresára a dvoch pod adresárov na testovanie
- ```I.deleteFolderStructure(randomNumber)``` - zmaže stromovú štruktúru adresára a dvoch podadresárov pripravených cez ```I.createFolder```

```javascript
//povodne ZLE riesenie s I.wait
I.click("Pridať");
```

- ```I.jstreeNavigate(pathArray)``` - v poli ```pathArray``` je možné definovať mená jednotlivých uzlov v stromovej štruktúre, na ktoré funkcia postupne klikne napr.  ```I.jstreeNavigate( [ "English", "Contact" ] );```.

Pre overenie hodnôt v tabuľke môžete použiť funkcie:

- ```DT.checkTableCell(name, row, col, value)``` - overí v zadanej tabuľke (ID tabuľky) hodnotu ```value``` v zadanom riadku ```row``` a stĺpci ```col```. Riadky a stĺpce začínajú číslom 1.
- ```DT.checkTableRow(name, row, values)``` - overí v zadanej tabuľke (ID tabuľky) v zadanom riadku ```row``` hodnoty v poli ```values```. Riadky začínajú číslom 1. Napr. ```DT.checkTableRow("statsDataTable", 1, ["13", "2 022", "30", "533", "229", "1"]);```.

Funkcie implementované v ```Document``` objekte:

- ```switchDomain(domain)``` - prepne doménu na zadanú hodnotu.
- ```setConfigValue(name, value)``` - nastaví konfiguračnú premennú so zadaným názvom a hodnotou.
- ```resetPageBuilderMode()``` - zmaže zapamätaný režim editora (štandardný/PageBuilder).
- `notifyClose` - zatvorí `toastr` notifikáciu.
- `notifyCheckAndClose(text)` - overí text v `toastr` notifikácii a zatvorí ju.
- `editorComponentOpen()` - otvorí nastavenie aplikácie v editore stránok (okno `editor_component.jsp`).
- `editorComponentOk()` - klikne na tlačidlo OK pre uloženie nastavenia aplikácie.
- `scrollTo(selector)` - posunie obsah okna na zadaný element.

V ```Document``` objekte sú aj funkcie pre vytváranie [fotiek obrazovky](screenshots.md).

Pre testovanie emailov pomocou [tempmail.plus](https://tempmail.plus) existuje objekt `TempMail`:

- `login(name, emailDomain = "fexpost.com")` - prihlásenie a nastavenie konta
- `openLatestEmail()` - otvorí najnovší email
- `closeEmail()` - zatvorí otvorený email a vráti sa na zoznam emailov
- `destroyInbox()` - zmaže všetky emaily v schránke

### Čakanie na dokončenie

Všeobecne sa neodporúča používať ```I.wait``` s fixnou dobou. Čas potrebný na čakanie môže byť odlišný na lokálnom počítači a v CI/CD pipeline. Naviac fixný čas môže zbytočne predlžovať dobu potrebnú na vykonanie testu.

Odporúčame použiť metódy [waitFor*](https://codecept.io/helpers/TestCafe/#waitforelement) a to hlavne ```waitForElement```, ```waitForText```, ```waitForVisible``` a ```waitToHide```.

Výhodné je použitie hlavne ```waitForText``` kde môžeme efektívne nahradiť ```I.wait``` a následné ```I.see``` za jeden príkaz:

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

Ak do kódu testu niekde dáte príkaz ```pause()```, tak sa zastaví vykonávanie testov a v Termináli sa vám zobrazí interaktívna konzola, v ktorej viete spúšťať príkazy. Takto dokážete pripraviť kroky testu a následne jednoducho príkazy skopírovať do JS súboru s testom.

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

pomocou 2x stlačenia TAB klávesy sa vám zobrazí nápoveda (zoznam možných príkazov). Tie môžete zadávať a sledovať, čo sa deje v prehliadači. Stlačením Enter klávesy sa test posunie na ďalší príkaz. Zadaním ```exit``` sa ukončí interaktívny terminál a test bude pokračovať automatizovane ďalej.

### Prihlasovanie

V súbore [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) je definované aj prihlasovanie cez rozšírenie [autologin](https://codecept.io/plugins/#autologin):

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

Je možné definovať viacero používateľov (opakovať atribút admin), napr. registrovaného používateľa, administrátora s obmedzenými právami a podobne.

Prihlásenie je možné do testov vkladať pomocou ```Before``` funkcie:

```javascript
Feature('gallery');

Before(({login}) => {
    login('admin');
});

Scenario('zoznam fotografii', ({I}) => {
    I.amOnPage("/admin/v9/apps/gallery");
    I.click("test");
    I.see("koala.jpg");
});
```

### Assert knižnica

Dostupné je rozšírenie [codeceptjs-chai](https://www.npmjs.com/package/codeceptjs-chai) pre volanie assert funkcií:

Základné použitie:

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

Ak je potrebné, môžete využiť aj [assert](https://www.npmjs.com/package/assert) knižnicu. Príklad použitia je v teste [gallery.js](../../../../src/test/webapp/tests/apps/gallery.js):

```javascript
const assert = require('assert');
...
assert.equal(+inputValueH, +area.h);
```

### Page objekty

Na vytvorenie univerzálnych testovacích scenárov je zložka `Pages` do ktorej sa generujú Page objekty
cez príkaz `npx codeceptjs gpo`, vytvorí sa page objekt pomocou ```Dependency Injection``` (podobne ako v Angular).

```javascript
const { I } = inject();

module.exports = {

  // insert your locators and methods here
}
```
Na to aby sme ho vedeli používať v testoch ho treba zaregistrovať v `codecept.conf.js`.

```javascript
exports.config = {
    include: {
        I: './steps_file.js',
        PageObject: './pages/PageObject.js'
    }
}
```
Následne ho vieme vložiť do nášho testovacieho scenára.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  PageObject.someMethod();
})
```
Je možné vložiť objekty do testov aj dynamicky cez `injectDependencies({})`.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  I.fillField('Username', PageObject.username);
  I.pressKey('Enter');
}).injectDependencies({ PageObject: require('./PageObject.js') });
```

### Detekcia prehliadača

Ak sa vám testy správajú rozdielne v prehliadači Firefox alebo Chromium je možné v testoch použiť funkcie na overenie použitého prehliadača.

```javascript
if (Browser.isChromium()) {
  I.amOnPage("/admin/v9/apps/insert-script#/");
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

## Odobratie práva

Volaním adresy stránky s parametrom ```removePerm``` je možné za behu odobrať zadané právo prihlásenému používateľovi (bez uloženia zmien v právach), ak prihlasovacie meno používateľa začína na ```tester```. Je tak možné testovať zobrazenie stránky bez zadaného práva a overiť tak bezpečnosť volania REST služieb.

Odobratie práva je implementované vo funkcii ```DT.checkPerms(perms, url)``` v [DT.js](../../../../src/test/webapp/pages/DT.js). Vyžaduje zadať právo a adresu stránky na ktorej sa právo testuje. Testo overuje zobrazenie notifikácie ```Prístup k tejto stránke je zamietnutý```.
 Voliteľný parameter ```datatableId``` reprezentuje ID/meno tabuľky v stránke (je potrebné zadať ak je v stránke viacero datatabuliek).

Príklad použitia:

```javascript
Scenario('zoznam stranok', ({ I, DT }) => {
    I.waitForText("Newsletter", 20);
    I.click("Newsletter", container);
    I.see("Testovaci newsletter");

    //over prava
    DT.checkPerms("menuWebpages", "/admin/v9/webpages/web-pages-list/");
});
```

Do parametra ```removePerm``` je možné zadať aj viacero práv oddelených čiarkou.

Pri datatabuľkách je možné nastavovať aj práva na [jednotlivé tlačidlá](../datatables/README.md#tlačidlá-podľa-práv) (pridať, editovať, duplikovať, zmazať). Testovať tak môžete aj jednotlivo vypnuté práva. Pre overenie práv na backende je ale potrebné testovať aj REST službu. Pridaním výrazu ```forceShowButton``` do parametra ```removePerm``` pri používateľovi s prihlasovacím menom začínajúcim na ```tester``` sa tlačidlá v datatabuľke zobrazia. Je tak možné otestovať zobrazenie chybového hlásenia z REST služby (že skutočne záznam nejde pridať/editovať/zmazať). Príklad je vo ```webpage-perms.js```:

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
    I.click("#datatableInit_wrapper button.buttons-create");
    I.click("#pills-dt-datatableInit-basic-tab");
    I.fillField("Názov web stránky", auto_webPage);
    DTE.save();
    I.see("Pridať web stránku - nemáte právo na pridanie web stránky");
    DTE.cancel();

    //skus editovat
    I.jstreeNavigate(["Test stavov", "Nedá sa zmazať"]);
    I.click("Nedá sa zmazať", "#datatableInit");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.seeInField("Názov web stránky", "Nedá sa zmazať");
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
    DTE.cancel();

    //skus zmazat
    DT.filter("title", "Nedá sa zmazať");
    I.click("table.datatableInit button.buttons-select-all");
    I.click("#datatableInit_wrapper button.buttons-remove");
    DT.waitForEditor();
    I.see("Naozaj chcete zmazať položku?");
    DTE.save();
    I.see("Nemáte právo na editáciu web stránky");
});
```

**Technické informácie:**

Odobratie práva je implementované v [ThymeleafAdminController.removePermissionFromCurrentUser](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java). Pri zadaní URL parametra ```removePerm``` sú upravené práva aktuálne prihláseného používateľa vrátane Spring kontextu.

## Vizuálne testovanie

Funkciu vizuálneho testovania je potrebné použiť na overenie zobrazenia, ktoré sa nedá overiť testovaním textu (napr. korektná pozícia výberového menu). Využitý je [plugin pixelMatchHelper](https://github.com/stracker-phil/codeceptjs-pixelmatchhelper), ktorý vie porovnať referenčnú fotku obrazovky s aktuálnou a zároveň vie zvýrazniť zmeny.

Pre zjednodušenie použitia sme pripravili funkciu ```Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)```, ktorá zabezpečí potrebné kroky. Má parametre:

- ```selector``` - ```selector``` elementu, z ktorého sa má spraviť snímka (nerobí sa z celej obrazovky, ale len zo zadaného elementu)
- ```screenshotFileName``` - meno súboru snímky, automaticky sa porovná s rovnakým menom obrázku v adresári ```src/test/webapp/screenshots/base```. Pre meno súboru použitie prefix ```autotest-``` pre lepšie dohľadanie vytvorenej snímky
- ```width``` (voliteľné) - šírka okna prehliadača
- ```height``` (voliteľné) - výška okna prehliadača
- ```tolerance``` (voliteľné) - miera tolerancie rozdielov voči referenčnému obrázku (0-100)

Príklad použitia:

```javascript
await Document.compareScreenshotElement("#insertScriptTable_wrapper", "autotest-insert-script-settings.png", 1280, 270);
```

Pri prvom spustení pravdepodobne nebude existovať referenčný obrázok. Test ale vytvorí aktuálnu snímku a uloží ho do adresára ```build/test``` (preto odporúčame meno obrázku prefixovať textom autotest- aby sa obrázok dal ľahko nájsť medzi screenshotmi chýb z testovania). Ak chcete obrázok použiť ako referenčný skopírujte ho do adresára ```src/test/webapp/screenshots/base```. Následne pri ďalšom spustení bude porovnaný referenčný obrázok s web stránkou.

Identifikované rozdiely sú generované do obrázkov v adresári ```src/test/webapp/screenshots/diff``` pre jednoduché overenie chyby. Test zároveň pri identifikovaní rozdielov vyhlási chybu ako akýkoľvek iný testovací scenár.

Príklad chyby zobrazenia (zlá pozícia výberového menu) - referenčný base obrázok:

![](autotest-insert-script-settings.png)

chybné zobrazenie v stránke:

![](autotest-insert-script-settings-error.png)

výsledné porovnanie so zvýraznením rozdielnej oblasti (ružová farba):

![](autotest-insert-script-settings-diff.png)

**Poznámky k implementácii**

Porovnanie obrázkov je zapúzdrené do funkcie ```Document.compareScreenshotElement``` implementovanej v ```Document.js```. Pri zadaní veľkosti okna vykoná zmenu veľkosti a po vytvorení screenshotu vráti okno do východzej veľkosti volaním funkcie ```I.wjSetDefaultWindowSize()``` (táto je pre konzistenciu volaná aj po každom prihlásení).

## Best practices

Pre úspešné a opakované spúšťanie testov odporúčame dodržať nasledovné body:

### Názvoslovie

- scenár začínate funkciou ```Feature('xxx');``` kde ako xxx použite meno súboru s testom. Ak nastane chyba ľahko tak vyhľadáte príslušný súbor s testom.

### Testovacie dáta

- pripravte a zmažte si testovacie dáta
- všetky vytvorené objekty musia obsahovať text ```autotest``` pre identifikovanie objektov vytvorených automatizovaným testom
- odporúčame použiť volanie ```I.getRandomText()``` pre získanie unikátneho suffixu, použitie vidno napr. v [group-internal.js](../../../../src/test/webapp/tests/webpages/group-internal.js) kde sú definované premenné a sú naplnené v ```Before``` funkcii
- je ideálne, ak testovacie dáta vytvoríte v samostatnom scenári a aj ich zmažete v samostatnom scenári. Ak teda padne niektorý test, tak zmazanie dát sa vykoná aj tak.

```javascript
var auto_name, auto_folder_internal, auto_folder_public, sub_folder_public;

Before(({ I, login }) => {
     login('admin');
     I.amOnPage('/admin/v9/webpages/web-pages-list#/');

     if (typeof auto_name=="undefined") {
          var randomNumber = I.getRandomText();
          auto_name = 'name-autotest-' + randomNumber;
          auto_folder_internal = 'internal_folder-autotest-' + randomNumber;
          auto_folder_public = 'public_folder-autotest-' + randomNumber;
          sub_folder_public = 'sub_folder_public-autotest-' + randomNumber;
     }
});
```

### Selektory

Je dôležité používať korektné selektory, text/element sa môže v stránke nachádzať viac krát a následne test náhodne padá. Používajte pripravené funkcie ako ```I.jstreeClick(name)``` pre stromovú štruktúru a funkcie začínajúce na DT./DTE. pre datatabuľku a editor napr. ```DTE.selectOption(name, text)``` alebo ```DT.filterSelect(name, value)```.

Odporúčame si selektor vyskúšať v JS konzole prehliadača s využitím jQuery, napríklad:

```javascript
//tlacidlo na pridanie zaznamu
$(".btn.btn-sm.buttons-create.btn-success.buttons-divider");

//v konzole bude vidno, ze vo web strankach je takych tlacidiel viacero:
//1xjstree a 2xdatatabulka z ktorej jedna je neviditelna pre adresare)
//je preto potrebne selektor zuzit tak, aby obsahoval vhodny parent kontajner

//zuzene len na column so stromovou strukturou - najde korektne len jedno tlacidlo
$("div.tree-col .btn.btn-sm.buttons-create.btn-success.buttons-divider")
```

### Časovanie

Časovanie vykonávania je veľmi dôležité, na inom počítači, alebo na serveri môže test bežať inou rýchlosťou. Je potrebné korektne čakať na dokončenie asynchrónnych volaní na server. Podobne môže byť problém s čakaním na otvorenie dialógového okna, uloženie dát a podobne.

Nepoužívajte fixný čas typu ```I.wait(1)``` ale používajte volania ```I.waitFor...``` alebo naše ```DT.waitFor...```. Viac je v sekcii [Čakanie na dokončenie](#čakanie-na-dokončenie) a [WebJET doplnkové funkcie](#webjet-doplnkové-funkcie).

Typické príklady:

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

**Každé volanie**, ```I.click('Uložiť');``` musí čakať na uloženie cez ```DTE.waitForLoader```.

### Dĺžka scenára

Pokúste sa mať jednotlivé scenáre krátke, nespájajte nesúvisiace časti do jedného scenára. Môžete si ale pripraviť testovacie dáta a tie znova použiť vo viacerých scenároch (ušetrí sa tak čas vytvárania a mazania dát medzi scenármi).

Scenár je možné spúšťať aj samostatne použitím ```--grep``` parametra viď [Spustenie testovania](#spustenie-testovania).

### Debugovanie

Test môžete spustiť s parametrom ```-p pauseOnFail```, ak nastane chyba automaticky sa zobrazí interaktívna konzola. V nej môžete overiť stav prehliadača a prípadne vyskúšať opravný príkaz, ktorý následne premietnete aj do testu.

Z tohto dôvodu nepoužívajte ```After``` funkciu v scenári, pretože tá sa vykoná pred vyvolaním interaktívnej konzoly po chybe a okno prehliadača už nebude v rovnakom stave.

## Zmazanie databázy

Databáza používaním testov rastie, keďže adresáre aj web stránky sa po zmazaní presunú do koša. Je dôležité tieto údaje z databázy raz za kvartál zmazať. Môžete použiť nasledovný SQL príkaz:

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

Pre čistú databázu na ktorej chcete spúšťať `baseTest` je potrebné nastaviť:

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
	(183, 'Produktová stránka - B verzia', '<section class=\"prices\">\n<div class=\"container\">\n<div class=\"row\">\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$9,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$19,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n\n<div class=\"col-12 col-md-4\">\n<div class=\"card border-0\">\n<div class=\"card-header text-white\">\n<h3 class=\"text-white text-center\">Vulputate</h3>\n\n<h4 class=\"text-white text-center\">$29,90</h4>\n</div>\n\n<div class=\"card-body bg-light\">\n<p class=\"text-center\"><i class=\"fa fa-sitemap fa-3x fa-3x\"></i></p>\n\n<p class=\"text-center\"><b>vulputate purus</b></p>\n\n<ul class=\"list-group bg-transparent text-center\">\n	<li class=\"list-group-item border-0 bg-transparent\">Nunc sed purus</li>\n	<li class=\"list-group-item border-0 bg-transparent\">rutrum varius sollicitudin</li>\n	<li class=\"list-group-item border-0 bg-transparent\">vulputate purus</li>\n</ul>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Sollicitudin</a></p>\n</div>\n</div>\n</div>\n</div>\n</div>\n</section>\n\n<section class=\"break\" style=\"margin-top: -20px;\">\n<div class=\"container\">\n<div class=\"row justify-content-md-center\">\n<div class=\"col-12 col-md-8\">\n<p class=\"text-center\">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam nec purus quis lectus semper blandit et in leo.</p>\n\n<p class=\"text-center\"><a class=\"btn btn-primary\" href=\"#\">Amet</a></p>\n</div>\n</div>\n</div>\n</section>\n\n<section>\n<div class=\"container\">\n<div class=\"row\">\n<div class=\"col-12 col-md-4\"><img alt=\"demo image\" class=\"w-100\" src=\"/thumb/images/produktova-stranka/money.jpg?w=445&amp;h=360&amp;ip=5\" /></div>\n\n<div class=\"col-12 col-md-7 align-self-center offset-md-1\">\n<h2>Etiam sit amet</h2>\n\n<p>Ut efficitur venenatis erat a&nbsp;facilisis. Ut sit amet ante et eros mollis congue at at nisi. Nunc scelerisque bibendum mauris, et cursus ipsum euismod molestie. Phasellus massa dui, luctus eu accumsan auctor, tristique eu magna. Suspendisse ullamcorper luctus ligula, eu vehicula nisi volutpat id.</p>\n\n<p><a class=\"btn btn-primary\" href=\"#\">Nullam</a></p>\n</div>\n</div>\n</div>\n</section>\n\n<p>&nbsp;</p>', '                        \n                       \n                 \n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $9,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n\n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $19,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n\n                             \n                           \n                                    \n                                   vulputate     \n\n                                   $29,90     \n      \n\n                                \n                                                                    \n\n                          vulputate purus        \n\n                                                  \n	                                                    nunc sed purus     \n	                                                    rutrum varius sollicitudin     \n	                                                    vulputate purus     \n     \n\n                                                           sollicitudin        \n      \n      \n      \n      \n      \n          \n\n                                                  \n                       \n                                           \n                             \n                       lorem ipsum dolor sit amet, consectetur adipiscing elit. nullam nec purus quis lectus semper blandit et in leo.    \n\n                                                           amet        \n      \n      \n      \n          \n\n         \n                       \n                 \n                                                                                                                                                   \n\n                                                           \n    etiam sit amet     \n\n   ut efficitur venenatis erat a&nbsp;facilisis. ut sit amet ante et eros mollis congue at at nisi. nunc scelerisque bibendum mauris, et cursus ipsum euismod molestie. phasellus massa dui, luctus eu accumsan auctor, tristique eu magna. suspendisse ullamcorper luctus ligula, eu vehicula nisi volutpat id.    \n\n                                       nullam        \n      \n      \n      \n          \n\n   &nbsp;    <h1>produktova stranka   b verzia</h1>\n<div style=\'display:none\' class=\'fulltextPerex\'>b verzia<br />produktova stranka, ktora je ako samostatna<br />microsite v roznych farebnych variantoch.</div>\n', '', 'Produktová stránka - B verzia', '2025-12-01 23:02:15', '2018-11-19 12:49:00', NULL, 18, 27, 5, 0, 0, 1, 1, 0, '/Newsletter', NULL, 30, -1, -1, -1, '5', '', 'B verzia<br />Produktová stránka, ktorá je ako samostatná<br />microsite v rôznych farebných variantoch.', '', '', ',,', 0, NULL, '/newsletter/produktova-stranka-b-verzia.html', 0, 1, -1, -1, '', '', '', '', '', '', '', '', '', '', '', '', 0, 0, '', '', '', '', '', '', '', 'undefined', 0, 27, NULL, NULL, -1, -1, -1, -1, 1, 1, 1, 1, 1, 0, 0, NULL, 0);
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
```

## Testovanie REST služieb

CodeceptJS podporuje aj [testovanie REST služieb](https://codecept.io/helpers/REST/). Nastavenie je v ```codecept.conf.js```:

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

Príklad volania REST služieb a testovania vráteného stavu, prihlásenia a JSON objektu:

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

