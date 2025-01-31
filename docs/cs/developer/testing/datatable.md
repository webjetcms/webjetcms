# Automatické testování DataTables

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Automatické testování DataTables](#automatické-testování-datových-tabulek)
  - [Použití](#použít)
  - [Možnosti nastavení](#možnosti-nastavení)
  - [Způsob generování povinných polí](#metoda-generování-povinných-polí)
  - [Testování auditních záznamů](#testování-auditních-záznamů)
  - [Testování práv](#testování-práv)
  - [Podrobnosti o provádění](#podrobnosti-o-provádění)

<!-- /code_chunk_output -->

Většina datových testů má v administraci stejnou podobu. `CRUD (Create, Read, Update, Delete)` operace. Abychom nemuseli stále dokola opakovat standardní postupy testování datových souborů, připravili jsme **automatizované testování**. Implementace je v souboru [DataTables.js](../../../../src/test/webapp/pages/DataTables.js) a zahrnuje následující kroky:
- přidání nového záznamu
  - ověření povinných polí
- vyhledat přidaný záznam
- editace záznamů
- kontrola oddělení záznamů mezi doménami
- vyhledat upravený záznam
- odstranění záznamu
- ověřování záznamů o auditu

## Použití

Pro jeho použití je nutné v souboru pug definovat proměnnou, přes kterou je datová tabulka přístupná:

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

V testovacím scénáři pak můžete použít základní test jako:

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable'
    });
});
```

na `dataTable` nastavujete **Název** proměnné na stránce. Je také možné použít jiné parametry testu (příklad v souboru [templates.js](../../../../src/test/webapp/tests/components/templates.js)):

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

Při ukládání nového záznamu se tento záznam uloží také do objektu `options.testingData` uloží pole vyplněných povinných polí. Můžete je použít např. ve funkci `editSearchSteps` Stejně jako:

```javascript
    I.see(`${options.testingData[0]}-change`, "div.dt-scroll-body");
```

Objekt `options` je vrácena z `baseTest` a lze je použít i v jiných scénářích, příkladem je funkce [translation-keys.js](../../../../src/test/webapp/tests/settings/translation-keys.js)

!>**Varování:** Automatizované testování základních operací nenahrazuje komplexní testování. Má sloužit jako základ testu, vaším úkolem je doplnit testovací scénáře o specifické vlastnosti. Ideální jako **samostatné scénáře** nebo přidáním kroků do funkcí `createSteps, editSteps, editSearchSteps, beforeDeleteSteps`.

## Možnosti nastavení

Prostřednictvím `options` objekt povinně nastavit:
- `dataTable` - název proměnné na webové stránce s datovou tabulkou.

Možnosti:
- `requiredFields` - seznam povinných polí. Pokud nejsou zadána, jsou automaticky načtena z `columns` definice (s atributem `required: true`).
- `testingData` - seznam hodnot, které se vyplní místo vygenerovaného `autotest-xxxx`. To je nutné, pokud mají pole určitý formát/délku (např. e-mail).
- `container` - možnost definovat selektor CSS kontejneru, do kterého je datový soubor vložen (definuje se pro vnořené datové soubory).
- `containerModal` - možnost definovat selektor kontejneru CSS dialogového okna editoru datového souboru (definuje se pro vnořený datový soubor).
- `skipRefresh` - pokud je nastavena na `true` webová stránka se po přidání záznamu neobnoví.
- `skipSwitchDomain` - pokud je nastavena na `true` Kontrola oddělení záznamů mezi doménami se neprovádí.
- `switchDomainName` - možnost definovat jinou doménu než výchozí `mirroring.tau27.iway.sk` k řízení oddělení záznamů mezi doménami.
- `skipDuplication` - pokud je nastavena na `true` neprovádí se test duplicity záznamů.
- `createSteps` - funkce, která přidává kroky testování při vytváření nového záznamu.
- `afterCreateSteps` - funkce se provede po uložení nového záznamu. Pokud tabulka neobsahuje žádná povinná pole, je možné nastavením parametru `requiredFields.push("string1");options.testingData[0] = string1;` definovat pole a nastavit jeho hodnotu.
- `editSteps` - funkce přidávající kroky testování při úpravě záznamu.
- `editSearchSteps` - funkce přidávající testovací kroky při vyhledávání záznamu.
- `beforeDeleteSteps` - funkce, která přidává testovací kroky před odstraněním záznamu.
- `perms` - práva na jméno (např. `editor_edit_media_all`) k automatickému ověření zabezpečení služby REST.

Interně jsou po inicializaci nastaveny následující atributy:
- `id` - ID datového objektu ve stromu DOM.
- `url` - Adresa URL aktuální webové stránky.
- `testingData` - obsahuje nastavené hodnoty povinných polí.

## Způsob generování povinných polí

Automatizovaný test pouze vyplní požadovaná pole, ale také otestuje zobrazení chybové zprávy, pokud požadované pole není vyplněno. Na začátku testu jsou definovány konstanty `startDate` a `date` a z nich se vygeneruje hodnota povinného pole, ke které se přidá náhodné číslo. Hodnota vždy obsahuje text **autotest**, podle kterého lze najít nesmazané záznamy (je bezpečné je smazat).

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

Použití konfigurace `testingData` můžete předem nastavit hodnotu pole, která se nastaví při jeho vytvoření. V tomto případě bude vygenerovaná hodnota `autotest-xxxx` se neuplatní.

Pokud má pole v názvu hodnotu e-mail, vygeneruje se hodnota s názvem domény na konci, aby pole mělo platnou hodnotu.

## Testování auditních záznamů

Po vymazání záznamů se audit otestuje. Zkontroluje se počet záznamů od začátku testu (podle konstanty `startDate`) obsahující v popisu text prvního povinného pole (`testingData[0]`).

Kontroluje se počet nalezených záznamů, který by měl být alespoň 3 (vytvořit, upravit, smazat).

## Testování práv

Po zadání parametru `perms` datatable také automaticky ověří zadané právo. Zobrazí obsah datové tabulky, odstraní právo a poté ověří zobrazení chybové zprávy. `Prístup k tejto stránke je zamietnutý`.

Příklad použití:

```javascript
await DataTables.baseTest({
    dataTable: 'mediaGroupsTable',
    perms: 'editor_edit_media_group'
});
```

!>**Varování:** Zadání tohoto parametru je povinné, aby se při zobrazení datové tabulky vždy testovala práva. U vnořené datové tabulky však může být tento test problematický kvůli odhlášení, můžete zadat prázdnou hodnotu nebo znak `-`.

## Podrobnosti o provádění

Test je implementován v souboru [DataTables.js](../../../../src/test/webapp/pages/DataTables.js). Klíčové je získat definici sloupců:

```javascript
const columns = await I.getDataTableColumns(dataTable);
```

Z něj se načte seznam polí, jejich nastavení atd.. Pokud nejsou definována žádná povinná pole, získají se z definice sloupců pomocí atributu `required: true`.

Funkce `I.getDataTableColumns` je definován v [custom\_helper.js](../../../../src/test/webapp/custom_helper.js):

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

Všechny funkce jsou asynchronní kvůli volání `I.getDataTableColumns`.

!>**Varování:** během implementace se nám stalo, že jsme měli druhé volání `this.helpers['Playwright'];` odhlásí přihlášeného uživatele (resetuje soubory cookie), zatímco probíhá test. Toto volání jsme chtěli použít při formátování dat, bohužel to v současné době není možné. Předpokládáme, že se jedná o chybu v CodeceptJS.

Poté se provedou jednotlivé kroky testu, jak je běžně známe.
