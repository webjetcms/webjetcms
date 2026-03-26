# Automatické testovanie DataTables

V administrácii je väčšina testov datatabuľky zhodná vo forme ```CRUD (Create, Read, Update, Delete)``` operácií. Aby sa nemuseli dookola opakovať štandardné postupy testu datatabuľky pripravili sme **automatizované testovanie**. Implementácia je v súbore [DataTables.js](../../../../src/test/webapp/pages/DataTables.js) a zahŕňa nasledovné kroky:

- pridanie nového záznamu
  - overenie povinných polí
- vyhľadanie pridaného záznamu
- editácia záznamu
- kontrola medzi-doménového oddelenia záznamov
- vyhľadanie editovaného záznamu
- zmazanie záznamu
- overenie zápisu auditných záznamov

## Použitie

Pre použitie je potrebné v pug súbore definovať premennú, cez ktorú je datatabuľka dostupná:

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

Následne v testovacom scenári môžete použiť základný test ako:

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable'
    });
});
```

do ```dataTable``` nastavujete **meno** premennej v stránke. Je možné použiť aj ďalšie parametre testu (príklad v súbore [templates.js](../../../../src/test/webapp/tests/components/templates.js)):

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

Pri uložení nového záznamu sa zároveň do objektu ```options.testingData``` uloží pole vyplnených údajov povinných polí. Tie môžete využiť napr. vo funkcii ```editSearchSteps``` ako:

```javascript
    I.see(`${options.testingData[0]}-change`, "div.dt-scroll-body");
```

Objekt ```options``` je vrátený z ```baseTest``` funkcie a je možné ho použiť v ďalších scenároch, príklad je v [translation-keys.js](../../../../src/test/webapp/tests/settings/translation-keys.js)

!>**Upozornenie:** automatizovaný test základných operácii nenahrádza komplexné testovanie. Má slúžiť ako základ testu, vašou úlohou je pridať testovacie scenáre špecifických vlastností. Ideálne ako **samostatné scenáre**, alebo doplnením krokov do funkcií ```createSteps, editSteps, editSearchSteps, beforeDeleteSteps```.

## Možnosti nastavenia

Cez ```options``` objekt povinné nastaviť:

- ```dataTable``` - meno premennej vo web stránke s datatabuľkou.

Voliteľné možnosti:

- ```requiredFields``` - zoznam povinných polí. Ak nie sú zadané načítajú sa automaticky z ```columns``` definície (majú atribút ```required: true```).
- ```testingData``` - zoznam hodnôt pre vyplnenie namiesto generovanej ```autotest-xxxx```. Je to potrebné, keď polia majú špecifický formát/dĺžku (napr. email).
- ```container``` - možnosť definovať CSS selektor kontajnera, v ktorej je datatabuľka vložená (potrebné definovať pre vnorenú datatabuľku).
- ```containerModal``` - možnosť definovať CSS selektor kontajnera dialógového okna datatable editora (potrebné definovať pre vnorenú datatabuľku).
- ```skipRefresh``` - ak je nastavené na ```true``` nevykoná sa obnovenie web stránky po pridaní záznamu.
- ```skipSwitchDomain``` - ak je nastavené na ```true``` nevykoná sa kontrola medzi-doménového oddelenia záznamov.
- ```switchDomainName``` - možnosť definovať inú doménu ako je predvolená ```mirroring.tau27.iway.sk``` pre kontrolu medzi-doménového oddelenia záznamov.
- ```skipDuplication``` - ak je nastavené na ```true``` nevykoná sa test duplikovania záznamov.
- ```createSteps``` - funkcia pridávajúca kroky testovania pri vytvorí nového záznamu.
- ```afterCreateSteps``` - funkcia je vykonaná po uložení nového záznamu. Ak tabuľka nemá žiadne povinné polia je možné nastavením ```requiredFields.push("string1");options.testingData[0] = string1;``` pole zadefinovať a nastaviť mu hodnotu.
- ```editSteps``` - funkcia pridávajúca kroky testovania pri editácii záznamu.
- ```editSearchSteps``` - funkcia pridávajúca kroky testovania pri vyhľadaní záznamu.
- ```beforeDeleteSteps``` - funkcia pridávajúca kroky testovania pred zmazaním záznamu.
- ```perms``` - meno práva (napr. ```editor_edit_media_all```) pre automatické overenie zabezpečenia REST služby.
- ```extraWait``` - ak je nastavené v niektorých prípadoch sa počká nastavený čas v sekundách, potrebné ak stránka načítava nejaké extra údaje.

Interne sa po inicializácii ešte nastavia nasledovné atribúty:

- ```id``` - ID objektu datatabuľky v DOM strome.
- ```url``` - URL adresa aktuálnej web stránky.
- ```testingData``` - obsahuje nastavené hodnoty povinných polí.

## Spôsob generovania povinných polí

Automatizovaný test vypĺňa len povinné polia, zároveň testuje zobrazenie chybového hlásenia, keď povinné pole nie je vyplnené. Na začiatku testu sa zadefinujú konštanty ```startDate``` a ```date``` a z nich sa generuje hodnota povinného poľa, ktorej sa pridá aj náhodné číslo. Hodnota vždy obsahuje aj text **autotest**, podľa ktorej sa dajú vyhľadať nezmazané záznamy (je bezpečné ich zmazať).

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

Pomocou konfigurácie ```testingData``` môžete vopred nastaviť hodnotu poľa pre jeho nastavenie pri vytvorení. V takom prípade sa vygenerovaná hodnota ```autotest-xxxx``` nepoužije.

Ak má pole v názve hodnotu email vygeneruje sa hodnota aj s doménovým menom na konci, aby malo pole platnú hodnotu.

## Testovanie auditných záznamov

Po zmazaní záznamov sa testuje audit. Kontroluje sa počet záznamov od spustenia testu (podľa konštanty ```startDate```) obsahujúcich v popise text prvého povinného poľa (```testingData[0]```).

Overuje sa počet nájdených záznamov, ktorých by malo byť minimálne 3 (create, edit, delete).

## Testovanie práv

Po zadaní parametra ```perms``` datatabuľka automaticky overí aj zadané právo. Zobrazí obsah datatabuľky, právo odstráni a následne overí zobrazenie chybového hlásenia ```Prístup k tejto stránke je zamietnutý```.

Príklad použitia:

```javascript
await DataTables.baseTest({
    dataTable: 'mediaGroupsTable',
    perms: 'editor_edit_media_group'
});
```

!>**Upozornenie:** zadanie tohto parametra je povinné, aby sa vždy pri zobrazení datatabuľky otestovali aj práva. Pri vnorenej datatabuľke ale tento test môže byť problematický z dôvodu odhlásenia, môžete zadať prázdnu hodnotu, alebo znak ```-```.

## Detaily implementácie

Test je implementovaný v súbore [DataTables.js](../../../../src/test/webapp/pages/DataTables.js). Základom je získanie columns definície:

```javascript
const columns = await I.getDataTableColumns(dataTable);
```

Z ktorej sa číta zoznam polí, ich nastavenie atď. Ak nie sú definované povinné polia, získajú sa z columns definície podľa atribútu ```required: true```.

Funkcia ```I.getDataTableColumns``` je definovaná v [custom_helper.js](../../../../src/test/webapp/custom_helper.js):

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

Všetky funkcie sú asynchrónne z dôvodu volania ```I.getDataTableColumns```.

!>**Upozornenie:** pri implementácii sa nám stalo, že druhé volanie ```this.helpers['Playwright'];``` počas behu testu odhlási prihláseného používateľa (resetne cookies). Chceli sme použiť toto volanie pri formátovaní dátumov, žiaľ aktuálne to nie je možné. Predpokladáme, že sa jedná o chybu v CodeceptJS.

Následne sú vykonávané jednotlivé kroky testu ako ich bežne poznáme.