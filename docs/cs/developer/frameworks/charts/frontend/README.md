# Amcharts

Knihovna [amcharts](http://amcharts.com) se používá k zobrazení grafů. Pro WebJET jsme zakoupili komerční OEM verzi.

## Inicializace Amcharts

V souboru [app.js](../../../../../src/main/webapp/admin/v9/src/js/app.js) asynchronní inicializace amcharts verze 5 je připravena. Tím se zajistí, že knihovna bude načtena a inicializována pouze v případě potřeby.

Iniciace `Amchart5` se provádí voláním `window.initAmcharts()`. Po inicializaci je událost vyvolána `WJ.initAmcharts.success` na adrese `window` zařízení. Tuto událost je možné vyslechnout a poté již použít objekt `window.am5`, prostřednictvím kterého je knihovna přístupná. Lepším řešením je však použití `then` funkci jako `window.initAmcharts().then(module => { ... } );`.

Kromě budovy se samotnou knihovnou jsou zde také objekty `window.am5xy` a `window.am5percent`, které jsou nepostradatelné pro vytváření a práci s amcharty.

Kromě nastavení licence tento soubor také nastavuje témata grafů. Tato témata ovlivní vzhled grafů, animace i použitou paletu barev. Kromě použitých témat amcharts používáme také vlastní téma pro nastavení barevné palety, kterou mají grafy používat. Toto téma naleznete v souboru [amcharts.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js) jako třída `WebjetTheme`.

# Práce s grafy

Pro práci s grafy jsme vytvořili javascriptový soubor [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js), který je k dispozici jako `window.ChartTools` objekt. Tento soubor obsahuje vlastní funkce a třídy, které umožňují zjednodušenou práci s grafy vytvořenými pomocí knihovny. `Amchart5`. Cílem bylo vytvořit modulární kód, který dokáže vytvářet/nastavovat/upravovat grafy podle určitých specifikací. Tento kód podporuje vytváření grafů typu `Line`, `Pie` a `Bar` a stará se o všechna logická i grafická nastavení grafů.

## Vytvoření nového grafu

Vytvoření grafu lze rozdělit do několika kroků, přičemž prvním krokem je inicializace knihovny amcharts voláním příkazu `window.initAmcharts()`. Jakmile je knihovna inicializována, můžeme pomocí volání ajaxu načíst data pro graf. Data a další parametry použijeme k vytvoření **Formulář** (bude vysvětleno později), který uložíme do předem vytvořené proměnné. Posledním krokem je vytvoření grafu pomocí připravené proměnné `createAmchart()` funkce dostupné prostřednictvím `window.ChartTools`.

Příklad použití:

```javascript
//Vytvorenie premennej, v ktorej bude udržiavať inštanciu grafu
let pieChartVisit;

//Vyvolanie udalosti na inicializáciu knižnice
window.initAmcharts().then(module => {
    //Získanie dát pre graf pomocou ajax volania
    $.ajax({url: getGraphUrl(), success: function(result) {

        //Vytvorenie inštancie FORMULÁRU grafu a jej uloženie do premennej
        pieChartVisits = new ChartTools.PieChartForm("visits", "name", '[[#{stat.top.pieChart}]]', "top-pieVisits", result['content']);

        //Vytvorenie grafu pomocou premennej s inštanciou FORMULÁRU grafu
        ChartTools.createAmchart(pieChartVisits);
    }});
});

//Div s id=graphsDiv môže obsahovať aj viacero grafov a je to kontainer na grafy s určitým nastaveným štýlom
//Div s id=top-pieVisits predstavuje elemnt ku ktorému bude priradený graf
<div id="graphsDiv">
    <div id="top-pieVisits" class="amcharts"></div>
</div>
```

Toto byl příklad toho, jak může vypadat vytvoření/nastavení grafu. Důležitá je zde třída `PieChartForm`, který představuje graf typu `PIE`, jeho data a všechny parametry potřebné k vytvoření a správnému nastavení grafu. Podpora pro 3 typy grafů je reprezentována 3 třídami (nebo, jak jsme se zmínili, formuláři) dostupnými ze stránek `window.ChartTools` :
- Třída `PieChartForm`, představuje grafy typu `Pie`
- Třída `BarChartForm`, představuje grafy typu `Bar`
- Třída `LineChartForm`, představuje grafy typu `Line`

Další informace o tom, co jednotlivé parametry těchto tříd dělají, jaký mají formát a vliv na generovaný graf, jsou popsány v souboru [Dokumentace](statjs.md).

### Prvek grafu HTML

Jak jsme si mohli všimnout v příkladu vytvoření grafu, musíme přidat element HTML div, který bude obsahovat tento graf. Při zadávání id mějte na paměti, že **každý prvek představující graf musí mít v rámci celého projektu jedinečné ID.** protože knihovna am5 potřebuje při vytváření grafu id prvku a pamatuje si tyto hodnoty id na globální úrovni. V situacích, kdy se pokusíme vytvořit více grafů pro prvek se stejným id (i když v jiné aplikaci), vyhodí am5 chybu a graf se nevytvoří. Doporučujeme sestavit `id` prvek grafu jako "název aplikace - název grafu", např. `top-pieVisits`.

### Úprava dat grafu

U některých grafů si můžeme všimnout úpravy extrahovaných dat před jejich dalším přiřazením ke grafu. Tato úprava souvisí s formou dat, filtrováním apod. Nejčastěji se s ní setkáme při vytváření/úpravě grafu typu `Line`. Pokud jsou data z části BackEnd vrácena ve správném (použitém) formátu, můžeme použít dostupnou funkci `convertDataForLineChart()` jak je uvedeno v následujícím příkladu. Další informace o fungování této funkce naleznete v souboru [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js).

```javascript
    //Získanie dát pre graf pomocou Ajax
    await $.ajax({url: getGraphUrl("lineVisits"), success: function(result) {
        //Úprava získaných dáta pre graf s použitím convertDataForLineChart() fn
        let convertedData = ChartTools.convertDataForLineChart(result);

        //Použitie upravených dát pri vytvárani grafu typu LINE
        lineChartVisits = new ChartTools.LineChartForm(ChartTools.getLineChartYAxeNameObjs(["visits"], [undefined]), "dayDate", '[[#{stat.top.lineChart}]]', "top-lineVisits", convertedData, ChartTools.DateType.Days);
        ChartTools.createAmchart(lineChartVisits);
    }});
```

Při úpravách vlastních dat je důležité pochopit, jaký formát dat je grafem podporován, proto doporučujeme ověřit si to v dokumentaci knihovny.

## Aktualizace existujícího grafu

Vzhledem k tomu, že grafy vždy obsahují externí filtr, jehož úkolem je filtrovat v datech grafu, je nutné vědět, jak grafy upravovat.

Příklad použití:

```javascript
    //Príklad funkcie na aktualizáciu dát grafu
    async function getDataAndUpdateAmcharts() {
        //Získanie nových dát pre graf
        await $.ajax({url: getGraphUrl("pieVisits"), success: function(result) {
            //Nastavenie nových dát grafu do formuláru
            pieChartVisits.chartData =  result['content'];

            //Vyvolanie aktualizácie grafu
            ChartTools.updateChart(pieChartVisits);
        }});
    }
```

Jak jsme mohli vidět v příkladu výše, můžeme data grafu měnit kdykoli během běhu programu. Stačí, když ve vytvořené instanci formuláře grafu nahradíme stará data grafu novými a použijeme dostupnou funkci `updateChart()` spustíme aktualizaci grafu, která se postará o vše, co je třeba udělat.

## Aktualizace tabulky

Někdy chceme při filtrování dat grafu pomocí externího filtru filtrovat také data tabulky, což může být problém, protože externí filtr nemusí být propojen s tabulkou (nebo máme více tabulek a pouze jedna z nich může být propojena s externím filtrem). Proto potřebujeme způsob, jak tyto tabulky filtrovat.

Příklad použití:

```javascript
//Pridanie parametrov do url
topDataTable.ajax.url(WJ.urlAddParam(url, "chartType", ChartType.Not_Chart));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchRootDir", $('#rootDir').val()));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchFilterBotsOut", $('#botFilterOut').is(':checked')));

//Refresh tabuľky
topDataTable.ajax.reload();
```

Z příkladu vidíme, že někdy nestačí jen obnovit tabulku, ale je třeba přidat parametry do url adresy nebo je jen upravit. Pak je úkolem BackEndu získat tyto parametry z url a vrátit příslušná data pro tabulku.
