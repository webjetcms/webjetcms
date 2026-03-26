# Automatické testování DataTables

V administraci je většina testů datatabulky shodná ve formě `CRUD (Create, Read, Update, Delete)` operací. Aby se nemuseli dokola opakovat standardní postupy testu datatabulky připravili jsme **automatizované testování**. Implementace je v souboru [DataTables.js](../../../../src/test/webapp/pages/DataTables.js) a zahrnuje následující kroky:
- přidání nového záznamu
  - ověření povinných polí
- vyhledání přidaného záznamu
- editace záznamu
- kontrola mezi-doménového oddělení záznamů
- vyhledání editovaného záznamu
- smazání záznamu
- ověření zápisu auditních záznamů

## Použití

Pro použití je třeba v pug souboru definovat proměnnou, přes kterou je datatabulka dostupná:

```javascript
//POZOR: je potrebne definovat cez var, nie cez let
var tempsTable;
window.domReady.add(function () {
    ...
    tempsTable = WJ.DataTable({
        ...
    });
});
```

Následně v testovacím scénáři můžete použít základní test jako:

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable'
    });
});
```

do `dataTable` nastavujete **jméno** proměnné ve stránce. Je možné použít i další parametry testu (příklad v souboru [templates.js](../../../../src/test/webapp/tests/components/templates.js)):

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable',
        //zoznam povinnych poli, ak nie su zadane nacitaju sa automaticky z columns definicie (maju atribut required: true)
        requiredFields: ['oldUrl', 'newUrl'],
        //nepovinne, ak zadate, tak pre pole nazovPola sa vyplni test-hodnota namiesto generovanej autotest-xxxx
        //je to potrebne, ked polia maju specificky format/dlzku (napr. email)
        testingData: {
          nazovPola: "test-hodnota",
          email: "email@domena.sk"
        },
        //kroky, ktore sa vykonaju ako prve pri novom zazname
        createSteps: function(I, options, DT, DTE) {
            I.fillField("#xxx", "yyy");
        },
        //kroky, ktore sa vykonaju po ulozeni zaznamu
        afterCreateSteps: function(I, options, requiredFields, DT, DTE) {
            requiredFields.push("nazovPola");
            options.testingData[2] = "test-hodnota";
        },
        //ktoky, ktore sa vykonaju pri zmene existujuceho zaznamu
        editSteps: function(I, options, DT, DTE) {
            I.fillField("#xxx", "yyy-edited");
        },
        //kroky, ktore sa vykonaju pri vyhladani zmeneneho zaznamu
        editSearchSteps: function(I, options, DT, DTE) {
            I.fillField("input.dt-filter-availableGrooupsList", "News");
        },
        //kroky, ktore sa vykonaju pred zmazanim zaznamu
        beforeDeleteSteps: function(I, options, DT, DTE) {
            I.wait(20);
        },
    });
});
```

Při uložení nového záznamu se zároveň do objektu `options.testingData` uloží pole vyplněných údajů povinných polí. Ty můžete využít například. ve funkci `editSearchSteps` jako:

```javascript
    I.see(`${options.testingData[0]}-change`, "div.dt-scroll-body");
```

Objekt `options` je vrácen z `baseTest` funkce a lze jej použít v dalších scénářích, příklad je v [translation-keys.js](../../../../src/test/webapp/tests/settings/translation-keys.js)

!>**Upozornění:** automatizovaný test základních operací nenahrazuje komplexní testování. Má sloužit jako základ testu, vaším úkolem je přidat testovací scénáře specifických vlastností. Ideální jako **samostatné scénáře**, nebo doplněním kroků do funkcí `createSteps, editSteps, editSearchSteps, beforeDeleteSteps`.

## Možnosti nastavení

Přes `options` objekt povinné nastavit:
- `dataTable` - jméno proměnné ve web stránce s datatabulkou.

Volitelné možnosti:
- `requiredFields` - seznam povinných polí. Pokud nejsou zadané načtou se automaticky z `columns` definice (mají atribut `required: true`).
- `testingData` - seznam hodnot pro vyplnění místo generované `autotest-xxxx`. Je to nutné, když pole mají specifický formát/délku (např. email).
- `container` - možnost definovat CSS selektor kontejneru, ve které je datatabulka vložena (nutno definovat pro vnořenou datatabulku).
- `containerModal` - možnost definovat CSS selektor kontejneru dialogového okna datatable editoru (nutno definovat pro vnořenou datatabulku).
- `skipRefresh` - pokud je nastaveno na `true` neprovede se obnovení web stránky po přidání záznamu.
- `skipSwitchDomain` - pokud je nastaveno na `true` neprovede se kontrola mezidoménového oddělení záznamů.
- `switchDomainName` - možnost definovat jinou doménu než je výchozí `mirroring.tau27.iway.sk` pro kontrolu mezi-doménového oddělení záznamů.
- `skipDuplication` - pokud je nastaveno na `true` neprovede se test duplikování záznamů.
- `createSteps` - funkce přidávající kroky testování při vytvoří nového záznamu.
- `afterCreateSteps` - funkce je provedena po uložení nového záznamu. Pokud tabulka nemá žádná povinná pole je možné nastavením `requiredFields.push("string1");options.testingData[0] = string1;` pole zadefinovat a nastavit mu hodnotu.
- `editSteps` - funkce přidávající kroky testování při editaci záznamu.
- `editSearchSteps` - funkce přidávající kroky testování při vyhledání záznamu.
- `beforeDeleteSteps` - funkce přidávající kroky testování před smazáním záznamu.
- `perms` - jméno práva (např. `editor_edit_media_all`) pro automatické ověření zabezpečení REST služby.
- `extraWait` - pokud je nastaveno v některých případech se počká nastavený čas v sekundách, potřebné pokud stránka načítá nějaké extra údaje.

Interně se po inicializaci ještě nastaví následující atributy:
- `id` - ID objektu datatabulky v DOM stromu.
- `url` - URL adresa aktuální web stránky.
- `testingData` - obsahuje nastavené hodnoty povinných polí.

## Způsob generování povinných polí

Automatizovaný test vyplňuje pouze povinná pole, zároveň testuje zobrazení chybového hlášení, když povinné pole není vyplněno. Na začátku testu se definují konstanty `startDate` a `date` az nich se generuje hodnota povinného pole, které se přidá i náhodné číslo. Hodnota vždy obsahuje i text **autotest**, podle které lze vyhledat nesmazané záznamy (je bezpečné je smazat).

```javascript
const randomText = I.getRandomText();
const randomTextShort = I.getRandomTextShort();

//definovanie textu povinneho pola
if (field.toLocaleLowerCase().indexOf("email")!=-1) {
    testingData[index] = `${field}-autotest-${randomTextShort}@onetimeusemail.com`;
} else {
    testingData[index] = `${field}-autotest-${randomText}`;
}

//vyplnenie hodnoty
I.fillField(`#DTE_Field_${field}`, `${testingData[index]}`);
```

Pomocí konfigurace `testingData` můžete předem nastavit hodnotu pole pro jeho nastavení při vytvoření. V takovém případě se vygenerovaná hodnota `autotest-xxxx` nepoužije.

Pokud má pole v názvu hodnotu email vygeneruje se hodnota is doménovým jménem na konci, aby mělo pole platnou hodnotu.

## Testování auditních záznamů

Po smazání záznamů se testuje audit. Kontroluje se počet záznamů od spuštění testu (dle konstanty `startDate`) obsahujících v popisu text prvního povinného pole (`testingData[0]`).

Ověřuje se počet nalezených záznamů, kterých by mělo být minimálně 3 (create, edit, delete).

## Testování práv

Po zadání parametru `perms` datatabulka automaticky ověří i zadané právo. Zobrazí obsah datatabulky, právo odstraní a následně ověří zobrazení chybového hlášení `Prístup k tejto stránke je zamietnutý`.

Příklad použití:

```javascript
await DataTables.baseTest({
    dataTable: 'mediaGroupsTable',
    perms: 'editor_edit_media_group'
});
```

!>**Upozornění:** zadání tohoto parametru je povinné, aby se vždy při zobrazení datatabulky otestovala i práva. Při vnořené datatabulce ale tento test může být problematický z důvodu odhlášení, můžete zadat prázdnou hodnotu nebo znak `-`.

## Detaily implementace

Test je implementován v souboru [DataTables.js](../../../../src/test/webapp/pages/DataTables.js). Základem je získání columns definice:

```javascript
const columns = await I.getDataTableColumns(dataTable);
```

Ze které se čte seznam polí, jejich nastavení atp. Pokud nejsou definována povinná pole, získají se z columns definice podle atributu `required: true`.

Funkce `I.getDataTableColumns` je definována v [custom\_helper.js](../../../../src/test/webapp/custom_helper.js):

```javascript
  /**
   * Metoda vrati stlpce vo formate .json
   * @param {string} dataTable objekt pre datatabulku
   */
  async getDataTableColumns(dataTable) {
    const { page } = this.helpers['Playwright'];
    const window = await page.evaluateHandle(() => window);
    const table = await window.getProperty(dataTable);
    const DATA = await table.getProperty('DATA');
    const columns = await DATA.getProperty('columns');
    const columnsToJson = await columns.jsonValue();
    try {
      if (columnsToJson) return columnsToJson;
    } catch(err) {
      console.log(err);
    }
  }
```

Všechny funkce jsou asynchronní z důvodu volání `I.getDataTableColumns`.

!>**Upozornění:** při implementaci se nám stalo, že druhé volání `this.helpers['Playwright'];` během běhu testu odhlásí přihlášeného uživatele (resetne cookies). Chtěli jsme použít toto volání při formátování dat, bohužel aktuálně to není možné. Předpokládáme, že se jedná o chybu v CodeceptJS.

Následně jsou prováděny jednotlivé kroky testu jak je běžně známe.
