# Automatizované testování

Pro automatizované E2E testování je používán framework [CodeceptJS](https://codecept.io). Testy se píší v JavaScriptu a prakticky ovládají prohlížeč, ve kterém test probíhá. Více informací proč jsme zvolili tento framework je v sekci [Playwright + CodeceptJS](#playwright--codeceptjs).

## Instalace

```shell
cd src/test/webapp/
npm install
```

!>**Upozornění:** před spuštěním testování je třeba zkompilovat JS/CSS admin části WebJETu:

```shell
cd src/main/webapp/admin/v9/
npm install
npm run prod
```

a spustit aplikační server:

```shell
gradlew appRun
```

každý z uvedených příkazů vám doporučuji spustit v samostatném Terminálu (menu Terminal->New Terminal). Mezi spuštěnými terminály si můžete přepínat v okně `Terminal`.

## Spuštění testování

Testování spustíte pomocí následujících příkazů:

````shell
cd src/test/webapp/

#spustenie vsetkych testov
npm run all

#spustenie konkrétneho testu a zastavenie v prípade chyby
npm run pause tests/components/gallery_test.js

#Spustenie konkrétneho scenára s hodnotou ```@current``` v názve
npm run current
````

Pro spuštění v prohlížeči firefox použijte prefix `ff:` před názvem:

```shell
npm run ff:all
npm run ff:pause tests/components/gallery_test.js
npm run ff:current
```

Spuštění na jiné URL s vypnutým zobrazením prohlížeče a prohlížečem `firefox`:

```shell
CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npm run all
```

**Poznámka:** v prohlížeči Firefox jsme měli problémy s rychlostí testů. Proto se pro tento prohlížeč v souboru `codecept.conf.js` nastaví proměnná `autodeayEnabled` na hodnotu `true` a aktivuje se doplněk `autodelay`. Ten zpožďuje provedení funkcí `amOnPage,click,forceClick` o 200ms před a 300ms po zavolání příkazu. Také jsme identifikovali zvláštní chování prohlížeče, který pokud není na popředí tak testy z ničeho nic přestanou fungovat a zobrazuje nesmyslné chyby. Při jednorázovém spuštění testu se vždy test provedl korektně. Přisuzujeme to nějaké optimalizaci provádění JavaScript kódu v prohlížeči, když není aktivní. Při spuštění s nezobrazením prohlížeče je vše v pořádku, proto pro spuštění všech testů vždy používejte nastavení `CODECEPT_SHOW=false`.

### Codecept UI

Codecept nabízí v beta verzi UI pro zobrazení testování, spustíte jej příkazem:

```shell
npm run codeceptjs:ui
```

a následně v prohlížeči otevřete stránku `http://localhost:3001`.

### Generování HTML reportu

**Mochawesome**

V npm je nastaven [plugin pro generování HTML reportů](https://codecept.io/reports/#html). Vygenerujete jej spuštěním příkazu:

```shell
npm run codeceptjs --reporter mochawesome
```

a do adresáře /build/test/report se vygeneruje HTML report s výsledkem testu. Pro neúspěšné testy se vytvoří i fotka obrazovky. Nastavení je v [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) v sekci `mocha`.

**Allure**

Report lze generovat i přes [Allure](allure.md) spuštěním testu:

```shell
npm run codeceptjs --plugins allure
```

Po dokončení testu můžete zobrazit výsledky spuštěním `allure` serveru:

```shell
allure serve ../../../build/test
```

## Playwright + CodeceptJS

K testování se používá [Playwright](https://github.com/microsoft/playwright/tree/master/docs) a [CodeceptJS](https://codecept.io/basics/).

Proč Playwright?

- je to 3. generace testovacího frameworku (1. generace Selenium, 2. generace Puppeteer, 3. generace Playwright)
- Microsoft koupil autory `Puppeteer` frameworku a oni vyvíjejí Playwright, takže mají zkušenost
- podporuje `chromium, firefox, webkit` (2021 květnová aktualizace Windows přepne Edge na chromium jádro)
- umí emulovat rozlišení, user agenta, DPI

Proč CodeceptJS?

- Playwright stejně jako `Puppeteer` je komunikační (low level) protokol pro ovládání prohlížeče (jeho automatizaci)
- CodeceptJS je testovací framework, který mimo jiné umí používat Playwright
- testovací kód se píše v JavaScriptu
- kód testů je velmi [srozumitelný](https://codecept.io/playwright/#setup)
- má pokročilé možnosti [Lokátorů](https://codecept.io/locators/#css-and-xpath) - hledání elementů podle textu, css, xpath
- má [GUI](https://codecept.io/ui/) (dosud nevyzkoušeno) pro psaní a zobrazení výsledku testů

### Konfigurace

Základní konfigurace je v souboru `codecept.conf.js`. Důležité atributy:
- `url` (http://iwcm.interway.sk) - adresa (doména) serveru. Ten můžete změnit přes `--override` parametr a přepnout testování z DEV na TEST/PROD prostředí.
- `output` (../../../build/test) - adresář do kterého se vám vygeneruje fotka obrazovky v případě neúspěšného testu (výchozí na `build/test` v kořenovém adresáři)
- `browser` (chromium) - zvolený prohlížeč ke spuštění testů, může být `chromium, firefox, webkit`
- `emulate` (zakomentovaně) - [emulace](https://github.com/Microsoft/playwright/blob/master/src/deviceDescriptors.ts) zařízení
- `screenshotOnFail` - zapne/vypne vytváření screenshotů v případě neúspěšného testu

## Psaní testů

Testy se vytvářejí v pod adresářích tests, kde jsou děleny podle jednotlivých modulů/aplikací WebJETu. Jsou psané v jazyce JavaScript, takže je možné využívat všech možností, které vám JavaScript nabízí.

Příklad komplexnějšího testu na otestování přihlášení [src/test/webapp/tests/admin/login.js](../../../../src/test/webapp/tests/admin/login.js):

!>**Upozornění:** do `Feature` zápisu zadávejte hodnotu ve formátu `adresár.podadresár.meno-súboru` pro korektní zobrazení testů ve stromové struktuře a snadné dohledání souboru podle vypsaného `Feature` v log souboru.

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

Všimněte si, že v ukázce schválně používám různé lokátory/selektory (jméno fieldu, text/label, CSS selector). To je jedna z výhod CodeceptJS. Více o možnostech Lokátorů je v [dokumentaci](https://codecept.io/locators/#css-and-xpath).

### Lokátory

Lokátory (selectory), které vybírají element na stránce jsou dobře popsány v [oficiální dokumentaci](https://codecept.io/locators/).

```
{permalink: /'foo'} matches <div id="foo">
{name: 'foo'} matches <div name="foo">
{css: 'input[type=input][value=foo]'} matches <input type="input" value="foo">
{xpath: "//input[@type='submit'][contains(@value, 'foo')]"} matches <input type="submit" value="foobar">
{class: 'foo'} matches <div class="foo">
```

### Within

Pomocí zápisu `within` můžete omezit prvek, na který se použijí následující příkazy:

```javascript
within("div.breadcrumb-language-select", () => {
    I.click("Slovenský jazyk");
    I.click("Chorvátsky jazyk");
});
```

zároveň většina příkazů umožňuje zapsat i selektor do příkazu, výše uvedené lze zapsat i jako:

```javascript
  I.click("Slovenský jazyk", "div.breadcrumb-language-select");
  I.click("Chorvátsky jazyk", "div.breadcrumb-language-select");
```

### Playwright metody

V [oficiální dokumentaci](https://codecept.io/helpers/Playwright/) je seznam všech možností `I` objektu. Krátké odkazy:
- [pressKey](https://codecept.io/helpers/Playwright/#presskey)
- [click](https://codecept.io/helpers/Playwright/#click)
- [forceClick](https://codecept.io/helpers/Playwright/#forceclick) - vynucené kliknutí bez čekání na událost, je třeba použít na custom `checkboxy` (jinak se tam zacyklí)
- [see](https://codecept.io/helpers/Playwright/#see) / [dontSee](https://codecept.io/helpers/Playwright/#dontsee)
- [seeElement](https://codecept.io/helpers/TestCafe/#seeelement) / [dontSeeElement](https://codecept.io/helpers/Detox/#dontseeelement)
- [fillField](https://codecept.io/helpers/Playwright/#fillfield)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [wait](https://codecept.io/helpers/Playwright/#wait)
- [selectOption](https://codecept.io/helpers/Playwright/#selectoption)
- [executeScript](https://codecept.io/helpers/Playwright/#executescript)
- [saveScreenshot](https://codecept.io/helpers/Playwright/#savescreenshot)

### WebJET doplňkové funkce

Pro WebJET jsme doplnili několik užitečných funkcí:
- [I.formatDateTime(timestamp)](../../../../src/test/webapp/steps_file.js) - naformátuje timestamp na datum a čas s využitím moment knihovny
- [I.seeAndClick(selector)](../../../../src/test/webapp/steps_file.js) - počká na zobrazení elementu a následně na něj klikne
- [await I.clickIfVisible(selector)](../../../../src/test/webapp/custom_helper.js) - je-li daný element zobrazen klikne na něj, pokud zobrazený není přeskočí krok (nevyhodí chybu)
- [I.verifyDisabled(selector)](../../../../src/test/webapp/custom_helper.js) - ověří, zda dané pole je neaktivní
- [I.wjSetDefaultWindowSize()](../../../../src/test/webapp/steps_file.js) - nastaví výchozí velikost okna po jeho změně, je voláno automaticky i po přihlášení v přihlašovací sekvenci v [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js)
- [Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)](../../../../src/test/webapp/codecept.conf.js) - provede [vizuální srovnání](#vizuální-testování)
- `I.waitForTime(time)` - čekání do zadaného času (timestamp).
- `I.toastrClose()` - zavření okna `toastr` oznámení.
- `clickCss(name, parent=null)` - provede kliknutí stejně jako `I.click` ale `name` považuje za CSS selektor - provedení je rychlejší, není třeba použít obalení do `{css: name}`.
- `forceClickCss(name, parent=null)` - provede kliknutí stejně jako `I.forceClick` ale `name` považuje za CSS selektor - provedení je rychlejší, není třeba použít obalení do `{css: name}`.

Pro datatabulku máme připraveny speciální funkce. Jsou implementovány v [ZN.js](../../../../src/test/webapp/pages/DT.js):
- `DT.waitForLoader(name)` - čeká na zobrazení a následné schování informace "Zpracovávám" v datatabulce. Používá se jako `DT.waitForLoader("#forms-list_processing");`
- `DT.filter(name, value, type=null)` - nastaví hodnotu `value` do textového filtru sloupce `name` datatabulky. Je-li zadán i atribut `type` nastaví se typ hledání (např. Začíná na, Končí na, Rovná se).
- `DT.filterSelect(name, value)` - nastaví hodnotu `value` do výběrového pole (select) filtru sloupce `name` datatabulky. Používá se jako `DT.filterSelect('cookieClass', 'Neklasifikované');`
- `async I.getDataTableColumns(dataTableName)` - vrátí objekt DATA s definicí datatabulky, používá se v automatickém testování datatabulky
- `async getDataTableId(dataTableName)` - vrátí ID datatabulky, volá JS funkci `dataTable.DATA.id`
- [async I.getTotalRows()](../../../../src/test/webapp/custom_helper.js) - vrátí celkový počet záznamů v datatabulce
- `DT.deleteAll(name = "datatableInit")` - smaže aktuálně zobrazené záznamy, před použitím vždy použijte `DT.filter` pro filtrování potřebných údajů.

Pro Datatable Editor implementováno v [DTE.js](../../../../src/test/webapp/pages/DTE.js):
- `DTE.waitForLoader(name)` - čeká na schování `loadera` v editoru (uložení záznamu)
- `DTE.waitForEditor(name)` - čeká na zobrazení editoru, je-li definováno jméno, použije se datatabulka s daným jménem, ve výchozím nastavení `datatableInit`
- `DTE.selectOption.(name, text)` - vybere hodnotu v select boxu (korektním způsobem zobrazením možností a následným kliknutím na možnost)
- `DTE.save(name)` - klikne na tlačítko Uložit v editoru, je-li definováno jméno, použije se datatabulka s daným jménem, ve výchozím nastavení `datatableInit`
- `DTE.cancel(name)` - klikne na tlačítko zavření editoru, je-li definováno jméno, použije se datatabulka s daným jménem, ve výchozím nastavení `datatableInit`
- `DTE.fillField(name, value)` - vyplní standardní pole, na rozdíl od volání `I.fillField` je možné do `name` parametru zadat přímo jméno pole na backendu/json definici.
- `DTE.fillQuill(name, value)` - vyplní hodnotu do pole typy `QUILL`.
- `DTE.fillCkeditor(htmlCode)` - nastaví HTML kód do aktuálně zobrazeného CKEditoru.
- `DTE.fillCleditor(parentSelector, value)` - zadá text do WYSIWYG `cleditor`. Hodnota `parentSelector` - odkaz na element ve kterém se `cleditor` nachází (např. `#forum`), `value` - hodnota k vyplnění. **Upozornění:** neví to zatím diakritiku z důvodu použití `type` příkazu. Pro datatabulku lze provést i [automatizovaný test](datatable.md).
- `DTE.appendField(name, value)` - doplní text to pole v editoru, řeší problém s použitím `I.appendField`, které se v editoru neprovede vždy správně.

Pro JsTree (stromovou strukturu):
- `I.jstreeClick(name)` - klikne na zvolený text v jstree (důležité použít hlavně ve web stránkách kde je linka se stejným jménem jako adresář i v seznamu stránek)
- `I.createFolderStructure(randomNumber)` - připraví stromovou strukturu adresáře a dva pod adresářů k testování
- `I.deleteFolderStructure(randomNumber)` - smaže stromovou strukturu adresáře a dvou podadresářů připravených přes `I.createFolder`

```javascript
//povodne ZLE riesenie s I.wait
I.click("Pridať");
```
- `I.jstreeNavigate(pathArray)` - v poli `pathArray` je možné definovat jména jednotlivých uzlů ve stromové struktuře, na které funkce postupně klikne např. `I.jstreeNavigate( [ "English", "Contact" ] );`.

Pro ověření hodnot v tabulce můžete použít funkce:
- `DT.checkTableCell(name, row, col, value)` - ověří v zadané tabulce (ID tabulky) hodnotu `value` v zadaném řádku `row` a sloupci `col`. Řádky a sloupce začínají číslem 1.
- `DT.checkTableRow(name, row, values)` - ověří v zadané tabulce (ID tabulky) v zadaném řádku `row` hodnoty v poli `values`. Řádky začínají číslem 1. Např. `DT.checkTableRow("statsDataTable", 1, ["13", "2 022", "30", "533", "229", "1"]);`.

Funkce implementovány v `Document` objektu:
- `switchDomain(domain)` - přepne doménu na zadanou hodnotu.
- `setConfigValue(name, value)` - nastaví konfigurační proměnnou se zadaným názvem a hodnotou.
- `resetPageBuilderMode()` - smaže zapamatovaný režim editoru (standardní/PageBuilder).
- `notifyClose` - zavře `toastr` notifikaci.
- `notifyCheckAndClose(text)` - ověří text v `toastr` oznámení a zavře ji.
- `editorComponentOpen()` - otevře nastavení aplikace v editoru stránek (okno `editor_component.jsp`).
- `editorComponentOk()` - klikne na tlačítko OK pro uložení nastavení aplikace.
- `scrollTo(selector)` - posune obsah okna na zadaný element.

V `Document` objektu jsou také funkce pro vytváření [fotek obrazovky](screenshots.md).

Pro testování emailů pomocí [tempmail.plus](https://tempmail.plus) existuje objekt `TempMail`:
- `login(name, emailDomain = "fexpost.com")` - přihlášení a nastavení účtu
- `openLatestEmail()` - otevře nejnovější email
- `closeEmail()` - zavře otevřený email a vrátí se na seznam emailů
- `destroyInbox()` - smaže všechny emaily ve schránce

### Čekání na dokončení

Obecně se nedoporučuje používat `I.wait` s fixní dobou. Čas potřebný k čekání může být odlišný na lokálním počítači a v CI/CD pipeline. Navíc fixní doba může zbytečně prodlužovat dobu potřebnou k provedení testu.

Doporučujeme použít metody [waitFor\*](https://codecept.io/helpers/TestCafe/#waitforelement) a to hlavně `waitForElement`, `waitForText`, `waitForVisible` a `waitToHide`.

Výhodné je použití hlavně `waitForText` kde můžeme efektivně nahradit `I.wait` a následné `I.see` za jeden příkaz:

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

Pokud do kódu testu někde dáte příkaz `pause()`, tak se zastaví provádění testů a v Terminálu se vám zobrazí interaktivní konzole, ve které umíte spouštět příkazy. Takto dokážete připravit kroky testu a následně jednoduše příkazy zkopírovat do JS souboru s testem.

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

pomocí 2x stisku TAB klávesy se vám zobrazí nápověda (seznam možných příkazů). Ty můžete zadávat a sledovat, co se děje v prohlížeči. Stisknutím Enter klávesy se test posune na další příkaz. Zadáním `exit` se ukončí interaktivní terminál a test bude pokračovat automatizovaně dále.

### Přihlašování

V souboru [codecept.conf.js](../../../../src/test/webapp/codecept.conf.js) je definováno i přihlašování přes rozšíření [autologin](https://codecept.io/plugins/#autologin):

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

Je možné definovat více uživatelů (opakovat atribut admin). registrovaného uživatele, administrátora s omezenými právy a podobně.

Přihlášení lze do testů vkládat pomocí `Before` funkce:

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

### Assert knihovna

Dostupné je rozšíření [codeceptjs-chai](https://www.npmjs.com/package/codeceptjs-chai) pro volání assert funkcí:

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

Pokud je třeba, můžete využít i [assert](https://www.npmjs.com/package/assert) knihovnu. Příklad použití je v testu [gallery.js](../../../../src/test/webapp/tests/apps/gallery/gallery.js):

```javascript
const assert = require('assert');
...
assert.equal(+inputValueH, +area.h);
```

### Page objekty

Pro vytvoření univerzálních testovacích scénářů je složka `Pages` do které se generují Page objekty přes příkaz `npx codeceptjs gpo`, vytvoří se page objekt pomocí `Dependency Injection` (podobně jako v Angular).

```javascript
const { I } = inject();

module.exports = {

  // insert your locators and methods here
}
```

K tomu abychom ho uměli používat v testech je třeba ho zaregistrovat v `codecept.conf.js`.

```javascript
exports.config = {
    include: {
        I: './steps_file.js',
        PageObject: './pages/PageObject.js'
    }
}
```

Následně jej umíme vložit do našeho testovacího scénáře.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  PageObject.someMethod();
})
```

Je možné vložit objekty do testů i dynamicky přes `injectDependencies({})`.

```javascript
Scenario('test-scenario', ({I, PageObject}) => {
  I.fillField('Username', PageObject.username);
  I.pressKey('Enter');
}).injectDependencies({ PageObject: require('./PageObject.js') });
```

### Detekce prohlížeče

Pokud se vám testy chovají rozdílně v prohlížeči Firefox nebo Chromium je možné v testech použít funkce pro ověření použitého prohlížeče.

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

## Odebrání práva

Voláním adresy stránky s parametrem `removePerm` je možné za běhu odebrat zadané právo přihlášenému uživateli (bez uložení změn v právech), pokud přihlašovací jméno uživatele začíná na `tester`. Je tak možné testovat zobrazení stránky bez zadaného práva a ověřit tak bezpečnost volání REST služeb.

Odebrání práva je implementováno ve funkci `DT.checkPerms(perms, url)` v [ZN.js](../../../../src/test/webapp/pages/DT.js). Vyžaduje zadat právo a adresu stránky na které se právo testuje. Testo ověřuje zobrazení notifikace `Prístup k tejto stránke je zamietnutý`. Volitelný parametr `datatableId` reprezentuje ID/jméno tabulky ve stránce (je třeba zadat pokud je ve stránce více datatabulek).

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

Do parametru `removePerm` je možné zadat i více práv oddělených čárkou.

U datatabulek lze nastavovat i práva na [jednotlivá tlačítka](../datatables/README.md#tlačítka-podle-práv) (přidat, editovat, duplikovat, smazat). Testovat tak můžete i jednotlivě vypnutá práva. Pro ověření práv na backendu je ale třeba testovat i REST službu. Přidáním výrazu `forceShowButton` do parametru `removePerm` u uživatele s přihlašovacím jménem začínajícím na `tester` se tlačítka v datatabulce zobrazí. Je tak možné otestovat zobrazení chybového hlášení z REST služby (že skutečně záznam nelze přidat/editovat/smazat). Příklad je ve `webpage-perms.js`:

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

Odebrání práva je implementováno v [ThymeleafAdminController.removePermissionFromCurrentUser](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java). Při zadání URL parametru `removePerm` jsou upravena práva aktuálně přihlášeného uživatele včetně Spring kontextu.

## Vizuální testování

Funkci vizuálního testování je třeba použít k ověření zobrazení, které nelze ověřit testováním textu (např. korektní pozice výběrového menu). Využit je [plugin pixelMatchHelper](https://github.com/stracker-phil/codeceptjs-pixelmatchhelper), který umí porovnat referenční fotku obrazovky s aktuální a zároveň umí zvýraznit změny.

Pro zjednodušení použití jsme připravili funkci `Document.compareScreenshotElement(selector, screenshotFileName, width, height, tolerance)`, která zajistí potřebné kroky. Má parametry:
- `selector` - `selector` elementu, ze kterého se má pořídit snímek (nedělá se z celé obrazovky, ale jen ze zadaného elementu)
- `screenshotFileName` - jméno souboru snímku, automaticky se porovná se stejným jménem obrázku v adresáři `src/test/webapp/screenshots/base`. Pro jméno souboru použití prefix `autotest-` pro lepší dohledání pořízeného snímku
- `width` (volitelné) - šířka okna prohlížeče
- `height` (volitelné) - výška okna prohlížeče
- `tolerance` (volitelné) - míra tolerance rozdílů vůči referenčnímu obrázku (0-100)

Příklad použití:

```javascript
await Document.compareScreenshotElement("#insertScriptTable_wrapper", "autotest-insert-script-settings.png", 1280, 270);
```

Při prvním spuštění pravděpodobně nebude existovat referenční obrázek. Test ale pořídí aktuální snímek a uloží jej do adresáře `build/test` (proto doporučujeme jméno obrázku přefixovat textem autotest- aby se obrázek dal snadno najít mezi screenshoty chyb z testování). Chcete-li obrázek použít jako referenční zkopírujte jej do adresáře `src/test/webapp/screenshots/base`. Následně při dalším spuštění bude porovnán referenční obrázek s web stránkou.

Identifikované rozdíly jsou generovány do obrázků v adresáři `src/test/webapp/screenshots/diff` pro snadné ověření chyby. Test zároveň při identifikování rozdílů vyhlásí chybu jako jakýkoli jiný testovací scénář.

Příklad chyby zobrazení (špatná pozice výběrového menu) - referenční base obrázek:

![](autotest-insert-script-settings.png)

chybné zobrazení ve stránce:

![](autotest-insert-script-settings-error.png)

výsledné srovnání se zvýrazněním rozdílné oblasti (růžová barva):

![](autotest-insert-script-settings-diff.png)

**Poznámky k implementaci**

Porovnání obrázků je zapouzdřeno do funkce `Document.compareScreenshotElement` implementované v `Document.js`. Při zadání velikosti okna provede změnu velikosti a po vytvoření screenshotu vrátí okno do výchozí velikosti voláním funkce `I.wjSetDefaultWindowSize()` (tato je pro konzistenci volána i po každém přihlášení).

## Best practices

Pro úspěšné a opakované spouštění testů doporučujeme dodržet následující body:

### Názvosloví

- scénář začínáte funkcí `Feature('xxx');` kde jako xxx použijte jméno souboru s testem. Pokud nastane chyba snadno tak vyhledáte příslušný soubor s testem.

### Testovací data

- připravte a smažte si testovací data
- všechny vytvořené objekty musí obsahovat text `autotest` pro identifikování objektů vytvořených automatizovaným testem
- doporučujeme použít volání `I.getRandomText()` pro získání unikátního suffixu, použití vidět například. v [group-internal.js](../../../../src/test/webapp/tests/webpages/group-internal.js) kde jsou definovány proměnné a jsou naplněny v `Before` funkci
- je ideální, pokud testovací data vytvoříte v samostatném scénáři a také je smažete v samostatném scénáři. Pokud tedy padne některý test, tak smazání dat se provede stejně.

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

Je důležité používat korektní selektory, text/element se může ve stránce nacházet vícekrát a následně test náhodně padá. Používejte připravené funkce jako `I.jstreeClick(name)` pro stromovou strukturu a funkce začínající na DT./DTE. pro datatabulku a editor Např. `DTE.selectOption(name, text)` nebo `DT.filterSelect(name, value)`.

Doporučujeme si selektor vyzkoušet v JS konzoli prohlížeče s využitím jQuery, například:

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

Časování provádění je velmi důležité, na jiném počítači nebo na serveru může test běžet jinou rychlostí. Je třeba korektně čekat na dokončení asynchronních volání na server. Podobně může být problém s čekáním na otevření dialogového okna, uložení dat a podobně.

Nepoužívejte fixní čas typu `I.wait(1)` ale používejte volání `I.waitFor...` nebo naše `DT.waitFor...`. Více je v sekci [Čekání na dokončení](#čekání-na-dokončení) a [WebJET doplňkové funkce](#webjet-doplňkové-funkce).

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

**Každé volání**, `I.click('Uložiť');` musí čekat na uložení přes `DTE.waitForLoader`.

### Délka scénáře

Pokuste se mít jednotlivé scénáře krátké, nespojujte nesouvisející části do jednoho scénáře. Můžete si ale připravit testovací data a ta znovu použít ve více scénářích (ušetří se tak čas vytváření a mazání dat mezi scénáři).

Scénář lze spouštět i samostatně použitím `--grep` parametru viz [Spuštění testování](#spuštění-testování).

### Debugování

Test můžete spustit s parametrem `-p pauseOnFail`, pokud nastane chyba automaticky se zobrazí interaktivní konzole. V ní můžete ověřit stav prohlížeče a případně vyzkoušet opravný příkaz, který následně promítnete i do testu.

Z tohoto důvodu nepoužívejte `After` funkci ve scénáři, protože ta se provede před vyvoláním interaktivní konzole po chybě a okno prohlížeče již nebude ve stejném stavu.

## Smazání databáze

Databáze používáním testů roste, jelikož adresáře i web stránky se po smazání přesunou do koše. Je důležité tyto údaje z databáze jednou za kvartál smazat. Můžete použít následující SQL příkaz:

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

Chcete-li plošně změnit hesla v testovací databázi použijte:

```sql
UPDATE users SET password='bcrypt:...', password_salt='bcrypt:...' WHERE user_id>1 AND login NOT IN ('user_sha512', 'user_bcrypt');
```

## Testování REST služeb

CodeceptJS podporuje také [testování REST služeb](https://codecept.io/helpers/REST/). Nastavení je v `codecept.conf.js`:

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

Příklad volání REST služeb a testování vráceného stavu, přihlášení a JSON objektu:

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
