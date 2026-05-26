# Amcharts

Knihovna [amcharts](http://amcharts.com) se používá k zobrazení grafů. Pro WebJET máme zakoupenou komerční OEM verzi.

## Amcharts inicializace

V souboru [app.js](../../../../../../src/main/webapp/admin/v9/src/js/app.js) je připravena asynchronní inicializace amcharts verze 5. Ta zajistí, že knihovna se načte a inicializuje až v případě potřeby.

Inicializace `Amchart5` se provádí voláním ```window.initAmcharts()```. Po inicializaci se vyvolá událost ```WJ.initAmcharts.success``` na ```window``` objektu. Na tuto událost lze poslouchat a následně už použít objekt ```window.am5```, přes který je knihovna dostupná. Lepší řešení je ale použít ```then``` funkci jako ```window.initAmcharts().then(module => { ... } );```.

Kromě objektu se samotnou knihovnou jsou následně dostupné i objekty ```window.am5xy```, ```window.am5percent``` a ```window.am5wc```, které jsou nezbytné k vytváření a práci s grafy amcharts.

Kromě nastavení licence se v tomto souboru nastavují také témata pro grafy. Tato témata ovlivní vzhled grafů a animace. Kromě použitých amcharts témat, využíváme také vlastní téma pro různé grafické úpravy prvků v grafech. Toto téma se nachází v souboru [amcharts.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js) jako třída `WebjetTheme`.

## Práce s grafy

Pro práci s grafy jsme vytvořili javascript soubor [chart-tools.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js), který je dostupný jako ```window.ChartTools``` objekt. Tento soubor obsahuje vlastní funkce a třídy, které poskytují zjednodušenou práci s grafy, vytvořenými pomocí knihovny ```Amchart5```. Cílem bylo vytvořit modulární kód, který bude umět vytvořit/nastavit/upravovat grafy podle určitých specifikací.

Tento kód podporuje tvorbu grafů typu:

- `LINE`
- `BAR_VERTICAL`
- `BAR_HORIZONTAL`
- `PIE_CLASSIC`
- `PIE_DONUT`
- `DOUBLE_PIE`
- `WORD_CLOUD`
- `TABLE`

a stará se o všechna logická i grafická nastavení grafů.

!>**Upozornění:** Grafy typu `TABLE` reálně nevyužívají `Amchart5` knihovnu, ale využívají se skrze stejný soubor aby byly pohromadě.

## Vytvoření nového grafu

Vytvoření grafu umíme rozdělit do několika kroků, kde prvním krokem je inicializace knihovny amcharts, voláním ```window.initAmcharts()```. Až je knihovna inicializována, můžeme pomocí ajax volání získat data pro graf. Využijeme data a jiné parametry k vytvoření **Formuláru** grafu (bude vysvětleno později), který uložíme do před-vytvořené proměnné. Posledním krokem je vytvoření grafu pomocí připravené ```createAmchart()``` funkce dostupné přes ```window.ChartTools```.

Příklad použití:

```javascript
//Vytvorenie premennej, v ktorej bude udržiavať inštanciu grafu
let pieChartVisit;

//Vyvolanie udalosti na inicializáciu knižnice
window.initAmcharts().then(module => {
    //Získanie dát pre graf pomocou ajax volania
    $.ajax({url: getGraphUrl(), success: function(result) {

        // Vytvorenie objektu konfigurácie grafu (obsahuje všetky potrebné parametre k vytvoreniu grafu)
        const pieConfig = {
            yAxeName: "visits",
            xAxeName: "name",
            chartTitle: '[[#{stat.top.pieChart}]]',
            chartDivId: "top-pieVisits",
            chartData: result['content']
        };

        //Vytvorenie inštancie FORMULÁRU grafu a jej uloženie do premennej
        pieChartVisits = new ChartTools.PieChartForm(pieConfig);

        //Vytvorenie grafu pomocou premennej s inštanciou FORMULÁRU grafu
        ChartTools.createAmchart(pieChartVisits);
    }});
});

//Div s id=graphsDiv môže obsahovať aj viacero grafov a je to kontajner na grafy s určitým nastaveným štýlom
//Div s id=top-pieVisits predstavuje element ku ktorému bude priradený graf
<div id="graphsDiv">
    <div id="top-pieVisits" class="amcharts"></div>
</div>
```

Toto byla ukázka jak může vypadat vytvoření/nastavení grafu. Důležitá je zde třída ```PieChartForm```, která reprezentuje graf typu ```PIE_DONUT```, jeho data a všechny parametry potřebné ke správnému vytvoření a nastavení grafu. Podpora pro různé typy grafů je reprezentována prostřednictvím samostatných tříd (nebo jak jsme zmínili formulářů) dostupných z ```window.ChartTools``` :

- třída `LineChartForm`, reprezentuje grafy typu `LINE`
- třída `BarChartForm`, reprezentuje grafy typu `BAR_VERTICAL` a `BAR_HORIZONTAL`
- třída `PieChartForm`, reprezentuje grafy typu `PIE_CLASSIC` a `PIE_DONUT`
- třída `DoublePieChartForm`, reprezentuje grafy typu `DOUBLE_PIE` (takže v podstatě dvojitý graf typu `PIE_DONUT`)
- třída `WordCloudForm`, reprezentuje grafy typu `WORD_CLOUD`

Bližší informace o tom, co dělají jednotlivé parametry těchto tříd, jaký mají formát a dopad na vygenerovaný graf jsou popsány v souboru [dokumentací](statjs.md).

!>**Upozornění:** historicky se využíval zápis bez konfiguračního objektu, kde se parametry zadávaly přímo do konstruktoru třídy formuláře grafu. Tento způsob je sice stále podporován, ale nedoporučujeme jej používat. Ukázka takového zápisu:

```javascript
    pieChartVisits = new ChartTools.PieChartForm("visits", "name", '[[#{stat.top.pieChart}]]', "top-pieVisits", result['content']);
```

### HTML element grafu

Jak jsme si v ukázce vytvoření grafu mohli všimnout, je zapotřebí přidání HTML div elementu, který bude obsahovat tento graf. Při zadávání id je třeba mít na paměti, že **každý prvek představující graf musí mít jedinečné ID napříč celým projektem**, jelikož am5 knihovna při vytváření grafu potřebuje id elementu a tyto id hodnoty si pamatuje na globální úrovni. V situaci, kdy se pokusíme vytvořit více grafů pro element se stejným id (i když v jiné aplikaci), am5 vyhodí chybu a graf se nevytvoří. Doporučujeme skládat `id` elementu grafu jako "název aplikace - název grafu", například. ```top-pieVisits```.

### Úpravy dat grafu

U některých grafů si můžeme všimnout úpravy získaných dat předtím, než jsou přiřazeny dále grafu. Tato úprava se týká formy dat, filtrování a podobně. Nejčastěji se s ní setkáváme při vytváření/úpravě grafu typu ```Line```. Pokud jsou data z BackEnd části včetně ve správném (zaužívaném) formátu, můžeme využít dostupnou ```convertDataForLineChart()``` funkci jak je to zobrazeno v následujícím příkladu. Bližší informace k tomu jak tato funkce funguje naleznete v souboru [chart-tools.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js).

```javascript
    //Získanie dát pre graf pomocou Ajax
    await $.ajax({url: getGraphUrl("lineVisits"), success: function(result) {
        //Úprava získaných dáta pre graf s použitím convertDataForLineChart() fn
        let convertedData = ChartTools.convertDataForLineChart(result);

        //
        const lineConfig = {
            yAxeNames: ChartTools.getLineChartYAxeNameObjs(["visits"], [undefined]),
            xAxeName: "dayDate",
            chartTitle: '[[#{stat.top.lineChart}]]',
            chartDivId: "top-lineVisits",
            chartData: convertedData,
            dateType: ChartTools.DateType.Days
        };

        //Použitie upravených dát pri vytváraní grafu typu LINE
        lineChartVisits = new ChartTools.LineChartForm(lineConfig);
        ChartTools.createAmchart(lineChartVisits);
    }});
```

Při vlastní úpravě dat je důležité rozumět, jaký formát dat je grafem podporován a proto doporučujeme si to prohlédnout v dokumentaci knihovny.

## Aktualizování stávajícího grafu

Jelikož součástí grafů je vždy externí filtr, jehož úloha je filtrování v datech grafu, je třeba umět grafy upravovat.

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

Jak jsme mohli vidět v ukázce výše, data grafu můžeme změnit kdykoli během běhu programu. Stačí, když ve vytvořené instanci formuláře grafu vyměníme stará data grafu za nová a pomocí dostupné funkce ```updateChart()``` spustíme aktualizaci grafu, která se o vše potřebné postará.

## Aktualizování datatabulky

Někdy při filtrování dat grafu pomocí externího filtru chceme filtrovat i data tabulky, ale to může být problém, jelikož externí filtr nemusí být propojen s tabulkou (nebo máme více tabulek a pouze jedna z nich může být propojena s externím filtrem). Proto potřebujeme způsob, jak tyto tabulky filtrovat.

Příklad použití:

```javascript
//Pridanie parametrov do url
topDataTable.setAjaxUrl(WJ.urlAddParam(url, "chartType", ChartType.Not_Chart));
topDataTable.setAjaxUrl(WJ.urlAddParam(topDataTable.getAjaxUrl(), "searchRootDir", $('#rootDir').val()));
topDataTable.setAjaxUrl(WJ.urlAddParam(topDataTable.getAjaxUrl(), "searchFilterBotsOut", $('#botFilterOut').is(':checked')));

//Refresh tabuľky
topDataTable.ajax.reload();
```

Z příkladu vidíme, že někdy nestačí tabulku pouze obnovit, ale potřebujeme si přidat parametry do url adresy, případně je pouze upravit. Dále to už je práce BackEnd, který tyto parametry z url dostane a vrátí patřičná data pro tabulku.