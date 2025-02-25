# Automatizované testování

Pro automatizované testování E2E se používá framework. [CodeceptJS](https://codecept.io). Testy jsou napsány v jazyce JavaScript a prakticky řídí prohlížeč, ve kterém je test spuštěn. Více informací o tom, proč jsme zvolili tento framework, najdete v části [Playwright + CodeceptJS](#dramaturg--codeceptjs).

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Automatizované testování](#automatizované-testování)
  - [Instalace](#instalace)
  - [Zahájení testování](#zahájení-testování)
    - [Uživatelské rozhraní Codecept](#kódové-rozhraní-ui)
    - [Generování sestav HTML](#generování-html-sestav)
  - [Playwright + CodeceptJS](#dramaturg--codeceptjs)
    - [Konfigurace](#Konfigurace)
  - [Psaní testů](#psaní-testů)
    - [Lokátory](#lokátory)
    - [V rámci](#v-rámci)
    - [Playwright metody](#dramaturgické-metody)
    - [Další funkce WebJET](#další-funkce-webjetu)
    - [Čekání na dokončení](#čekání-na-dokončení)
    - [Pause](#pauza)
    - [Přihlášení](#přihlášení)
    - [Knihovna Assert](#knihovna-assert)
    - [Stránka objekty](#objekty-na-stránce)
    - [Detekce prohlížeče](#detekce-prohlížeče)
  - [Odnětí práva](#odnětí-práva)
  - [Vizuální testování](#vizuální-testování)
  - [Best practices](#osvědčené-postupy)
    - [Nomenklatura](#nomenklatura)
    - [Testovací data](#testovací-data)
    - [Selektory](#selektory)
    - [Časování](#časování)
    - [Délka scénáře](#délka-scénáře)
    - [Ladění](#ladění)
  - [Odstranění databáze](#odstranění-databáze)
  - [Testování služeb REST](#testovací-služby)

<!-- /code_chunk_output -->

## Instalace

```shell
cd src/test/webapp/
npm install
```

!>**Varování:** Před spuštěním testování je třeba zkompilovat JS/CSS administrátorskou část WebJETu:

```shell
cd src/main/webapp/admin/v9/
npm install
npm run prod
```

a spustit aplikační server:

```shell
gradlew appRun
```

Doporučuji spustit každý z výše uvedených příkazů v samostatném terminálu (nabídka Terminál->Nový terminál). Mezi spuštěnými terminály můžete přepínat v nabídce `Terminal`.

## Zahájení testování

Chcete-li zahájit testování, použijte následující příkazy:

````shell
cd src/test/webapp/

#spustenie vsetkych testov
npm run all

#spustenie konkrétneho testu a zastavenie v prípade chyby
npm run pause tests/components/gallery_test.js

#Spustenie konkrétneho scenára s hodnotou ```@current``` v názve
npm run current
````

Pro spuštění ve Firefoxu použijte předponu `ff:` před názvem:

```shell
npm run ff:all
npm run ff:pause tests/components/gallery_test.js
npm run ff:current
```

Spuštění na jiné adrese URL s vypnutým zobrazením a prohlížečem `firefox`:

```shell
CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npm run all
```

**Poznámka:** ve Firefoxu jsme měli problémy s rychlostí testů. Proto jsme pro tento prohlížeč v souboru `codecept.conf.js` nastaví proměnnou `autodeayEnabled` na hodnotu `true` a doplněk je aktivován `autodelay`. Ten zpožďuje provádění funkcí `amOnPage,click,forceClick` přibližně 200 ms před a 300 ms po vyvolání příkazu. Zjistili jsme také podivné chování prohlížeče, který pokud není na popředí, testy z ničeho nic přestanou fungovat a zobrazují nesmyslné chyby. Při jednorázovém spuštění testu se test vždy provedl správně. Přičítáme to určité optimalizaci provádění kódu JavaScriptu v prohlížeči, pokud není aktivní. Při spuštění s nezobrazeným prohlížečem je vše v pořádku, proto vždy použijte nastavení pro spuštění všech testů `CODECEPT_SHOW=false`.

### Uživatelské rozhraní Codecept

Codecept nabízí v beta verzi uživatelské rozhraní pro zobrazení testování, které spustíte pomocí příkazu:

```shell
npm run codeceptjs:ui
```

a poté otevřete stránku v prohlížeči `http://localhost:3001`.

### Generování sestav HTML

**Mochawesome**

V npm je nastavena [zásuvný modul pro generování sestav HTML](https://codecept.io/reports/#html). Vytvoříte ji spuštěním příkazu:

```shell
npm run codeceptjs --reporter mochawesome
```

a do adresáře /build/test/report se vygeneruje HTML report s výsledkem testu. V případě neúspěšných testů se také vytvoří snímek obrazovky. Nastavení se nachází v [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) v sekci `mocha`.

**Allure**

Zprávu lze vygenerovat také prostřednictvím [allure](allure.md) provedením testu:

```shell
npm run codeceptjs --plugins allure
```

Po dokončení testu můžete zobrazit výsledky spuštěním příkazu `allure` serveru:

```shell
allure serve ../../../build/test
```

## Playwright + CodeceptJS

Pro testování se používá [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Proč Playwright?

- jedná se o 3. generaci testovacího frameworku (1. generace Selenium, 2. generace Puppeteer, 3. generace Playwright).
- Microsoft koupil autory `Puppeteer` a vyvíjejí program Playwright, takže mají zkušenosti s tím.
- Podporováno `chromium, firefox, webkit` (2021 Květnová aktualizace systému Windows přepne prohlížeč Edge na jádro Chromium)
- dokáže emulovat rozlišení, uživatelského agenta, DPI.

Proč CodeceptJS?

- Playwright stejne jako`Puppeteer` je komunikační (nízkoúrovňový) protokol pro ovládání prohlížeče (jeho automatizace).
- CodeceptJS je testovací framework, který mimo jiné umí používat Playwright.
- testovací kód je napsán v jazyce JavaScript
- kód testů je velmi [srozumitelné](https://codecept.io/playwright/#setup)
- má pokročilé možnosti [Lokátory](https://codecept.io/locators/#css-and-xpath) - vyhledávání prvků podle textu, css, xpath
- má adresu [GRAFICKÉ UŽIVATELSKÉ ROZHRANÍ](https://codecept.io/ui/) (zatím netestováno) pro zápis a zobrazení výsledků testů

### Konfigurace

Základní konfigurace je v souboru `codecept.conf.js`. Důležité vlastnosti:
- `url` (http://iwcm.interway.sk) - adresa (doména) serveru. Tuto hodnotu můžete změnit pomocí `--override` parametr a přepnout testování z prostředí DEV do prostředí TEST/PROD.
- `output` (../../../build/test) - adresář, do kterého se vygeneruje snímek obrazovky v případě neúspěšného testu (výchozí hodnota je `build/test` v kořenovém adresáři)
- `browser` (chromium) - zvolený prohlížeč pro spuštění testů, může být `chromium, firefox, webkit`
- `emulate` (komentář) - [Emulace](https://github.com/Microsoft/playwright/blob/master/src/deviceDescriptors.ts) zařízení
- `screenshotOnFail` - povolí/zakáže vytváření snímků obrazovky v případě neúspěšného testu.

## Psaní testů

Testy se vytvářejí v podadresářích tests, kde jsou rozděleny podle jednotlivých modulů/aplikací WebJETu. Jsou napsány v jazyce JavaScript, takže můžete využít všech možností, které JavaScript nabízí.

Příklad složitějšího testu pro testování přihlášení [src/test/webapp/tests/admin/login.js](../../../../src/test/webapp/tests/admin/login.js):

!>**Varování:** na `Feature` zadejte hodnotu ve formátu `adresár.podadresár.meno-súboru` pro správné zobrazení testů ve stromové struktuře a snadné dohledání souboru podle `Feature` v souboru protokolu.

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

Všimněte si, že v ukázce záměrně používám různé lokátory/selektory (název pole, text/nápis, selektor CSS). To je jedna z výhod CodeceptJS. Více o možnostech lokátorů se dozvíte v [Dokumentace](https://codecept.io/locators/#css-and-xpath).

### Lokátory

Selektory, které vybírají prvek na stránce, jsou dobře popsány v části [oficiální dokumentace](https://codecept.io/locators/).

```
{permalink: /'foo'} matches <div id="foo">
{name: 'foo'} matches <div name="foo">
{css: 'input[type=input][value=foo]'} matches <input type="input" value="foo">
{xpath: "//input[@type='submit'][contains(@value, 'foo')]"} matches <input type="submit" value="foobar">
{class: 'foo'} matches <div class="foo">
```

### V rámci

Použití zápisu `within` můžete omezit prvek, na který se použijí následující příkazy:

```javascript
within("div.breadcrumb-language-select", () => {
    I.click("Slovenský jazyk");
    I.click("Chorvátsky jazyk");
});
```

zároveň většina příkazů umožňuje zapsat k příkazu selektor, výše uvedené lze zapsat také jako:

```javascript
  I.click("Slovenský jazyk", "div.breadcrumb-language-select");
  I.click("Chorvátsky jazyk", "div.breadcrumb-language-select");
```

### Playwright metody

V [oficiální dokumentace](https://codecept.io/helpers/Playwright/) je seznam všech možností `I` objekt. Krátké odkazy:
- [pressKey](https://codecept.io/helpers/Playwright/#presskey)
- [klikněte na](https://codecept.io/helpers/Playwright/#click)
- [forceClick](https://codecept.io/helpers/Playwright/#forceclick) - nucené kliknutí bez čekání na událost, musí být použito na vlastní `checkboxy` (jinak se tam zasekne)
- [viz](https://codecept.io/helpers/Playwright/#see) / [dontSee](https://codecept.io/helpers/Playwright/#dontsee)
- [vizElement](https://codecept.io/helpers/TestCafe/#seeelement) / [dontSeeElement](https://codecept.io/helpers/Detox/#dontseeelement)
- [fillField](https://codecept.io/helpers/Playwright/#fillfield)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [čekat](https://codecept.io/helpers/Playwright/#wait)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [executeScript](https://codecept.io/helpers/Playwright/#executescript)
- [saveScreenshot](https://codecept.io/helpers/Playwright/#savescreenshot)

### Další funkce WebJET

Přidali jsme několik užitečných funkcí pro WebJET:
- [I.formatDateTime(časové razítko)](../../../../src/test/webapp/steps_file.js) - zformátuje časovou značku na datum a čas pomocí knihovny moment
- [I.seeAndClick(selektor)](../../../../src/test/webapp/steps_file.js) - počká, až se prvek zobrazí, a pak na něj klikne.
- [await I.clickIfVisible(selektor)](../../../../src/test/webapp/custom_helper.js) - pokud je prvek zobrazen, klikněte na něj, pokud není zobrazen, krok přeskočte (nevyhazujte chybu).
- [I.verifyDisabled(selektor)](../../../../src/test/webapp/custom_helper.js) - ověří, zda je pole neaktivní
- [I.wjSetDefaultWindowSize()](../../../../src/test/webapp/steps_file.js) - nastaví výchozí velikost okna po jeho změně, je volán automaticky i po přihlášení v přihlašovací sekvenci v sekvenci [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js)
- [Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)](../../../../src/test/webapp/codecept.conf.js) - provede [vizuální srovnání](#vizuální-testování)
- `I.waitForTime(time)` - čekání do zadaného času (časové razítko).
- `I.toastrClose()` - zavření okna `toastr` oznámení.
- `clickCss(name, parent=null)` - provede kliknutí stejně jako `I.click` Ale `name` považován za selektor CSS - provedení je rychlejší, není třeba používat obtékání, aby se `{css: name}`.
- `forceClickCss(name, parent=null)` - provede kliknutí stejně jako `I.forceClick` Ale `name` považován za selektor CSS - provedení je rychlejší, není třeba používat obtékání, aby se `{css: name}`.

Pro tabulku dat jsme připravili speciální funkce. Jsou implementovány v [DT.js](../../../../src/test/webapp/pages/DT.js):
- `DT.waitForLoader(name)` - čeká na zobrazení a následné skrytí informace "Zpracování" v datové tabulce. Používá se jako `DT.waitForLoader("#forms-list_processing");`
- `DT.filter(name, value, type=null)` - nastaví hodnotu `value` ve sloupci textového filtru `name` DATATabulky. Pokud je atribut zadán také `type` nastaví typ vyhledávání (např. Začíná v, Končí v, Rovná se).
- `DT.filterSelect(name, value)` - nastaví hodnotu `value` do výběrového pole sloupcového filtru `name` DATATabulky. Používá se jako `DT.filterSelect('cookieClass', 'Neklasifikované');`
- `async I.getDataTableColumns(dataTableName)` - vrací objekt DATA s definicí datové tabulky, který se používá při automatickém testování datových tabulek.
- `async getDataTableId(dataTableName)` - vrací ID datového souboru, volá funkci JS `dataTable.DATA.id`
- [async I.getTotalRows()](../../../../src/test/webapp/custom_helper.js) - vrací celkový počet záznamů v datové tabulce
- `DT.deleteAll(name = "datatableInit")` - vymaže aktuálně zobrazené záznamy, vždy použijte příkaz `DT.filter` pro filtrování potřebných údajů.

Pro Datatable Editor implementovaný v [DTE.js](../../../../src/test/webapp/pages/DTE.js):
- `DTE.waitForLoader(name)` - čekající na skrytí `loadera` v editoru (uložit záznam)
- `DTE.waitForEditor(name)` - čeká na zobrazení editoru, pokud je definován název, použije se datová tabulka s daným názvem, ve výchozím nastavení `datatableInit`
- `DTE.selectOption.(name, text)` - vybere hodnotu ve výběrovém poli (správným způsobem zobrazením možností a následným kliknutím na možnost).
- `DTE.save(name)` - klikne na tlačítko Uložit v editoru, pokud je definován název, použije se ve výchozím nastavení datová tabulka s daným názvem. `datatableInit`
- `DTE.cancel(name)` - klikne na tlačítko zavřít editor, pokud je definován název, použije se datová tabulka s daným názvem, ve výchozím nastavení `datatableInit`
- `DTE.fillField(name, value)` - vyplní standardní pole, na rozdíl od volání `I.fillField` je možné `name` parametr pro přímé zadání názvu pole v definici backend/json.
- `DTE.fillQuill(name, value)` - vyplní hodnotu v poli types `QUILL`.
- `DTE.fillCkeditor(htmlCode)` - nastaví kód HTML na aktuálně zobrazený CKEditor.
- `DTE.fillCleditor(parentSelector, value)` - zadává text do WYSIWYG `cleditor`. Hodnota `parentSelector` - odkaz na prvek, ve kterém je `cleditor` se nachází (např. `#forum`), `value` - hodnota k vyplnění. **Varování:** zatím nezná diakritiku, protože se používá `type` příkaz. Pro datovou tabulku je také možné provést následující příkaz [automatizovaný test](datatable.md).
- `DTE.appendField(name, value)` - přidá text do pole v editoru, řeší problém s použitím `I.appendField` což není v editoru vždy správně provedeno.

Pro JsTree (stromová struktura):
- `I.jstreeClick(name)` - klikne na vybraný text v jstree (důležité použít zejména na webových stránkách, kde je odkaz se stejným názvem jako adresář také v seznamu stránek).
- `I.createFolderStructure(randomNumber)` - připraví stromovou strukturu adresářů a dva podadresáře pro testování.
- `I.deleteFolderStructure(randomNumber)` - odstraní stromovou strukturu adresáře a dvou podadresářů připravených pomocí funkce `I.createFolder`

```javascript
//povodne ZLE riesenie s I.wait
I.click("Pridať");
```
- `I.jstreeNavigate(pathArray)` - v terénu `pathArray` je možné definovat názvy jednotlivých uzlů ve stromové struktuře, na které funkce postupně kliká, např. `I.jstreeNavigate( [ "English", "Contact" ] );`.

K ověření hodnot v tabulce můžete použít funkce:
- `DT.checkTableCell(name, row, col, value)` - ověří hodnotu v zadané tabulce (ID tabulky). `value` v zadaném řádku `row` a sloupec `col`. Řádky a sloupce začínají číslem 1.
- `DT.checkTableRow(name, row, values)` - ověřuje v zadané tabulce (ID tabulky) v zadaném řádku `row` hodnoty v poli `values`. Řádky začínají číslem 1. Například. `DT.checkTableRow("statsDataTable", 1, ["13", "2 022", "30", "533", "229", "1"]);`.

Funkce implementované v `Document` zařízení:
- `switchDomain(domain)` - přepne doménu na zadanou hodnotu.
- `setConfigValue(name, value)` - nastaví konfigurační proměnnou se zadaným názvem a hodnotou.
- `resetPageBuilderMode()` - odstraní zapamatovaný režim editoru (standardní/PageBuilder).
- `notifyClose` - se uzavře `toastr` oznámení.
- `notifyCheckAndClose(text)` - ověřuje text v `toastr` a zavře jej.
- `editorComponentOpen()` - otevře nastavení aplikace v editoru stránek (okno `editor_component.jsp`).
- `editorComponentOk()` - kliknutím na tlačítko OK uložíte nastavení aplikace.
- `scrollTo(selector)` - posune obsah okna na zadaný prvek.

V `Document` objekt obsahuje také funkce pro vytváření [snímky obrazovky](screenshots.md).

Testování e-mailů pomocí [tempmail.plus](https://tempmail.plus) existuje objekt `TempMail`:
- `login(name, emailDomain = "fexpost.com")` - přihlášení a nastavení účtu
- `openLatestEmail()` - otevře nejnovější e-mail
- `closeEmail()` - zavře otevřený e-mail a vrátí se do seznamu e-mailů.
- `destroyInbox()` - odstraní všechny e-maily ve složce Doručená pošta

### Čekání na dokončení

Obecně se nedoporučuje používat `I.wait` s pevně stanovenou dobou. Čekací doba se může lišit na místním počítači a v potrubí CI/CD. Pevná doba navíc může zbytečně prodloužit dobu potřebnou k provedení testu.

Doporučujeme používat metody [waitFor\*](https://codecept.io/helpers/TestCafe/#waitforelement) zejména `waitForElement`, `waitForText`, `waitForVisible` a `waitToHide`.

Je vhodnější používat hlavně `waitForText` kde můžeme účinně nahradit `I.wait` a následné `I.see` pro jeden příkaz:

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

Pokud někde v testovacím kódu vložíte příkaz `pause()`, provádění testů se zastaví a zobrazí se interaktivní konzola v Terminálu, kde můžete spouštět příkazy. Tímto způsobem si můžete připravit kroky testu a poté jednoduše zkopírovat příkazy do souboru JS testu.

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

Dvojím stisknutím klávesy TAB se zobrazí nápověda (seznam možných příkazů). Ty můžete zadat a sledovat, co se v prohlížeči stane. Stisknutím klávesy Enter se test posune na další příkaz. Zadání adresy `exit` bude interaktivní terminál ukončen a test bude pokračovat automatizovaně dále.

### Přihlášení

V souboru [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) je definováno také přihlášení pomocí rozšíření [autologin](https://codecept.io/plugins/#autologin):

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

Je možné definovat více uživatelů (opakováním atributu admin), např. registrovaného uživatele, správce s omezenými právy atd.

Přihlašovací údaje lze do testů vložit pomocí `Before` funkce:

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

### Knihovna Assert

K dispozici je rozšíření [codeceptjs-chai](https://www.npmjs.com/package/codeceptjs-chai) pro volání funkcí assert:

Základní použití:

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

V případě potřeby můžete také použít [assert](https://www.npmjs.com/package/assert) knihovna. Příkladem použití je test [gallery.js](../../../../src/test/webapp/tests/apps/gallery.js):

```javascript
const assert = require('assert');
...
assert.equal(+inputValueH, +area.h);
```

### Stránka objekty

K vytvoření univerzálních testovacích scénářů slouží komponenta `Pages` do kterého jsou objekty stránky generovány prostřednictvím `npx codeceptjs gpo`, objekt stránky je vytvořen pomocí `Dependency Injection` (podobně jako Angular).

```javascript
const { I } = inject();

module.exports = {

  // insert your locators and methods here
}
```

Abyste ji mohli používat v testech, musíte ji zaregistrovat v položce `codecept.conf.js`.

```javascript
exports.config = {
    include: {
        I: './steps_file.js',
        PageObject: './pages/PageObject.js'
    }
}
```

Poté ji můžeme vložit do našeho testovacího scénáře.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  PageObject.someMethod();
})
```

Objekty je možné do testů vkládat také dynamicky prostřednictvím `injectDependencies({})`.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  I.fillField('Username', PageObject.username);
  I.pressKey('Enter');
}).injectDependencies({ PageObject: require('./PageObject.js') });
```

### Detekce prohlížeče

Pokud se vaše testy chovají ve Firefoxu nebo Chromu jinak, je možné v testech použít funkce validace prohlížeče.

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

## Odnětí práva

Voláním adresy stránky s parametrem `removePerm` je možné přihlášenému uživateli odebrat zadané právo za běhu (bez uložení změn v právech), pokud přihlašovací jméno uživatele začíná písmeny `tester`. Je možné otestovat zobrazení stránky bez zadaného oprávnění a ověřit zabezpečení volání služby REST.

Odvolání práva se provádí pomocí funkce `DT.checkPerms(perms, url)` v [DT.js](../../../../src/test/webapp/pages/DT.js). Vyžaduje zadání práva a adresy stránky, na které se právo testuje. Testo ověří zobrazení oznámení `Prístup k tejto stránke je zamietnutý`. Nepovinný parametr `datatableId` představuje ID/název tabulky na stránce (je nutné zadat, pokud je na stránce více datových tabulek).

Příklad použití:

```javascript
Scenario('zoznam stranok', ({ I, DT }) => {
    I.waitForText("Newsletter", 20);
    I.click("Newsletter", container);
    I.see("Testovaci newsletter");

    //over prava
    DT.checkPerms("menuWebpages", "/admin/v9/webpages/web-pages-list/");
});
```

K parametru `removePerm` je také možné zadat více práv oddělených čárkou.

U datových tabulek je také možné nastavit práva na [jednotlivá tlačítka](../datatables/README.md#tlačítka-podle-práv) (přidat, upravit, duplikovat, odstranit). Můžete také otestovat jednotlivě zakázaná práva. Chcete-li však ověřit práva na backendu, musíte otestovat také službu REST. Přidáním výrazu `forceShowButton` k parametru `removePerm` pro uživatele s přihlašovacím jménem začínajícím na `tester` se zobrazí tlačítka v datové tabulce. Je tedy možné otestovat zobrazení chybového hlášení ze služby REST (že záznam nelze přidat/upravit/smazat). Příklad je v `webpage-perms.js`:

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

**Technické informace:**

Odnětí práva se provádí ve formě [ThymeleafAdminController.removePermissionFromCurrentUser](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java). Po zadání parametru URL `removePerm` jsou upravena práva aktuálně přihlášeného uživatele, včetně kontextu Spring.

## Vizuální testování

Funkce vizuálního testování by měla být použita k ověření zobrazení, která nelze ověřit textovým testováním (např. správná poloha nabídky výběru). Používá se [plugin pixelMatchHelper](https://github.com/stracker-phil/codeceptjs-pixelmatchhelper) který dokáže porovnat referenční snímek obrazovky s aktuálním a také zvýraznit změny.

Pro zjednodušení použití jsme připravili funkci `Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)` která zajistí, aby byly podniknuty nezbytné kroky. Má parametry:
- `selector` - `selector` prvek, ze kterého má být snímek pořízen (není pořízen z celé obrazovky, ale pouze ze zadaného prvku).
- `screenshotFileName` - název souboru obrázku, bude automaticky porovnán se stejným názvem obrázku v adresáři. `src/test/webapp/screenshots/base`. Pro název souboru použijte předponu `autotest-` pro lepší sledování vytvořeného obrazu
- `width` (nepovinné) - šířka okna prohlížeče
- `height` (nepovinné) - výška okna prohlížeče
- `tolerance` (nepovinné) - míra tolerance rozdílů oproti referenčnímu obrazu (0-100)

Příklad použití:

```javascript
await Document.compareScreenshotElement("#insertScriptTable_wrapper", "autotest-insert-script-settings.png", 1280, 270);
```

Při prvním spuštění pravděpodobně nebude k dispozici referenční obrázek. Test však vytvoří aktuální obraz a uloží jej do adresáře `build/test` (proto doporučujeme název obrázku předřadit před autotest-, aby bylo možné obrázek snadno najít mezi snímky obrazovky s chybami z testování). Pokud chcete obrázek použít jako referenci, zkopírujte jej do složky `src/test/webapp/screenshots/base`. Při dalším spuštění se pak referenční obrázek porovná s webovou stránkou.

Identifikované rozdíly jsou generovány do obrázků v adresáři `src/test/webapp/screenshots/diff` pro snadné ověření chyb. Test také vyhlásí chybu jako každý jiný testovací scénář, pokud jsou zjištěny rozdíly.

Příklad chyby zobrazení (špatná pozice výběrové nabídky) - referenční základní obrázek:

![](autotest-insert-script-settings.png)

nesprávné zobrazení na stránce:

![](autotest-insert-script-settings-error.png)

výsledné srovnání se zvýrazněním rozdílové oblasti (růžová barva):

![](autotest-insert-script-settings-diff.png)

**Poznámky k implementaci**

Porovnávání obrázků je obsaženo ve funkci `Document.compareScreenshotElement` zavedené v `Document.js`. Při změně velikosti okna provede změnu velikosti a po vytvoření snímku obrazovky vrátí okno na výchozí velikost voláním funkce `I.wjSetDefaultWindowSize()` (tento příkaz se také volá po každém přihlášení kvůli konzistenci).

## Best practices

Pro úspěšné a opakované provádění testů doporučujeme následující body:

### Nomenklatura

- začínáte scénář funkcí `Feature('xxx');` kde xxx je název testovacího souboru. Pokud dojde k chybě, můžete snadno najít příslušný testovací soubor.

### Testovací data

- připravit a odstranit testovací data
- všechny vytvořené objekty musí obsahovat text `autotest` pro identifikaci objektů vytvořených automatizovaným testem
- doporučujeme použít volání `I.getRandomText()` k získání jedinečné přípony, což je použití, které se objevuje např. ve slovech [group-internal.js](../../../../src/test/webapp/tests/webpages/group-internal.js) kde jsou definovány a vyplněny proměnné `Before` funkce
- je ideální, pokud testovací data vytvoříte v samostatném scénáři a v samostatném scénáři je také odstraníte. Pokud tedy test selže, dojde k odstranění dat i tak.

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

Je důležité používat správné selektory, text/element se může na stránce objevit vícekrát a test pak náhodně spadne. Používejte připravené funkce, jako např. `I.jstreeClick(name)` pro stromovou strukturu a funkce začínající na DT./DTE. pro datovou tabulku a editor, např. `DTE.selectOption(name, text)` nebo `DT.filterSelect(name, value)`.

Doporučujeme vyzkoušet selektor v JS konzole prohlížeče například pomocí jQuery:

```javascript
//tlacidlo na pridanie zaznamu
$(".btn.btn-sm.buttons-create.btn-success.buttons-divider");

//v konzole bude vidno, ze vo web strankach je takych tlacidiel viacero:
//1xjstree a 2xdatatabulka z ktorej jedna je neviditelna pre adresare)
//je preto potrebne selektor zuzit tak, aby obsahoval vhodny parent kontajner

//zuzene len na column so stromovou strukturou - najde korektne len jedno tlacidlo
$("div.tree-col .btn.btn-sm.buttons-create.btn-success.buttons-divider")
```

### Časování

Načasování provedení je velmi důležité, na jiném počítači nebo serveru může test probíhat jinou rychlostí. Je nutné správně počkat na dokončení asynchronních volání na server. Stejně tak může být problémem čekání na otevření dialogového okna, uložení dat apod.

Nepoužívejte pevný typ času `I.wait(1)` ale použijte volání `I.waitFor...` nebo naše `DT.waitFor...`. Více v sekci [Čekání na dokončení](#čekání-na-dokončení) a [Další funkce WebJET](#další-funkce-webjetu).

Typické příklady:

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

**Každé volání**, `I.click('Uložiť');` musí čekat na uložení prostřednictvím `DTE.waitForLoader`.

### Délka scénáře

Snažte se jednotlivé scénáře zkrátit, nespojujte nesouvisející části do jednoho scénáře. Můžete si však připravit testovací data a znovu je použít ve více scénářích (ušetříte tak čas při vytváření a mazání dat mezi scénáři).

Skript lze spustit také samostatně pomocí `--grep` parametr viz [Zahájení testování](#zahájení-testování).

### Ladění

Test můžete spustit s parametrem `-p pauseOnFail` pokud dojde k chybě, automaticky se zobrazí interaktivní konzola. V ní můžete zkontrolovat stav prohlížeče a případně vyzkoušet opravný příkaz, který pak můžete převést do testu.

Z tohoto důvodu nepoužívejte `After` ve scénáři, protože bude provedena před vyvoláním interaktivní konzoly po chybě a okno prohlížeče již nebude ve stejném stavu.

## Odstranění databáze

Databáze se s používáním testů rozrůstá, protože adresáře i webové stránky jsou po smazání přesunuty do koše. Je důležité tato data z databáze jednou za čtvrt roku vymazat. Můžete použít následující příkaz SQL:

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

## Testování služeb REST

CodeceptJS také podporuje [testování služeb REST](https://codecept.io/helpers/REST/). Nastavení je v `codecept.conf.js`:

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

Příklad volání služby REST a testování vráceného stavu, přihlášení a objektu JSON:

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
