# Amcharts

Knižnica [amcharts](http://amcharts.com) sa používa na zobrazenie grafov. Pre WebJET máme zakúpenú komerčnú OEM verziu.

## Amcharts inicializácia

V súbore [app.js](../../../../../src/main/webapp/admin/v9/src/js/app.js) je pripravená asynchrónna inicializácia amcharts verzie 5. Tá zabezpečí, že knižnica sa načíta a inicializuje až v prípade potreby.

Inicializácia `Amchart5` sa vykonáva volaním ```window.initAmcharts()```. Po inicializácii sa vyvolá udalosť ```WJ.initAmcharts.success``` na ```window``` objekte. Na túto udalosť je možné počúvať a následne už použiť objekt ```window.am5```, cez ktorý je knižnica dostupná. Lepšie riešenie je ale použiť `then` funkciu ako `window.initAmcharts().then(module => { ... } );`.

Okrem objektu so samotnou knižnicou sú následne dostupné aj objekty ```window.am5xy``` a ```window.am5percent```, ktoré sú nevyhnuté k vytváraniu a práci s grafmi amcharts.

Okrem nastavenia licencie sa v tomto súbore nastavujú aj témy pre grafy. Tieto témy ovplyvnia vzhľad grafov, animácie ako aj použitú paletu farieb. Okrem použitých amcharts tém, využívame aj vlastnú tému pre nastavenie palety farieb, ktoré majú grafy využívať. Táto téma sa nachádza v súbore [amcharts.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/amcharts.js) ako trieda `WebjetTheme`.

# Práca s grafmi

Pre prácu s grafmi sme vytvorili javascript súbor [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js), ktorý je dostupný ako ```window.ChartTools``` objekt. Tento súbor obsahuje vlastné funckie a triedy, ktoré poskytujú zjednodušenú prácu s grafmi, vytvorenými pomocou knižnice ```Amchart5```. Cieľom bolo vytvoriť modulárny kód, ktorý bude vedieť vytvoriť/nastaviť/upravovať grafy podľa určitých špecifikácií. Tento kód podporuje tvorbu grafov typu ```Line```, ```Pie``` a ```Bar``` a stará sa o všetky logické ako aj grafické nastavenia grafov.

## Vytvorenie nového grafu

Vytvorenie grafu vieme rozdeliť do niekoľkých krokov, kde prvým krokom je inicializácia knižnice amcharts, volaním ```window.initAmcharts()```. Až je knižnica inicializovaná, možeme pomocou ajax volania získať dáta pre graf. Využijeme dáta a iné parametre k vytvoreniu **Formuláru** grafu (bude vysvetlené neskôr), ktorý uložíme do pred-vytvorenej premennej. Posledným krokom je vytvorenie grafu pomocou pripravenej ```createAmchart()``` funkcie dostupnej cez ```window.ChartTools```.

Príklad použitia:

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

Toto bola ukážka ako môže vyzerať vytvorenie/nastavenie grafu. Dôležitá je tu trieda ```PieChartForm```, ktorá reprezentuje graf typu ```PIE```, jeho dáta a všetký parametre potrebné k správnemu vytvoreniu a nastaveniu grafu. Podpora pre 3 typy grafov je reprezentovaná prostredníctvom 3 tried (alebo ako sme spomenuli formulárov) dostupných z ```window.ChartTools``` :

-   trieda ```PieChartForm```, reprezentuje grafy typu ```Pie```
-   trieda ```BarChartForm```, reprezentuje grafy typu ```Bar```
-   trieda ```LineChartForm```, reprezentuje grafy typu ```Line```


Bližšie informácie o tom čo robia jednotlivé parametre týchto tried, aký majú formát a dopad na vygenerovaný graf sú opísané v súbore [dokumentácií](statjs.md).

### HTML element grafu

Ako sme si v ukážke vytvorenia  grafu mohli všimnúť, je potrebné pridanie HTML div elementu, ktorý bude obsahovať tento graf. Pri zadávaní id treba mať na pamäti, že **každý prvok predstavujúci graf musí mať jedinečné ID naprieč celým projektom**, nakoľko am5 knižnica pri vytváraní grafu potrebuje id elementu a tieto id hodnoty si pamätá na globálnej úrovni. V situácií keď sa pokúsime vytvoriť viac grafov pre element s tým istým id (aj keď v inej aplikácií), am5 vyhodí chybu a graf sa nevytvorí. Odporúčame skladať `id` elementu grafu ako "názov aplikácie - názov grafu", napr. ```top-pieVisits```.

### Úpravy dát grafu

Pri niektorých grafoch si môžeme všimnúť úpravu získaných dát predtým, než sú priradené ďalej grafu. Táto úprava sa týka formy dát, filtrovania a podobne. Najčastejšie sa s ňou stretávame pri vytváraní/úprave grafu typu ```Line```. Ak su dáta z BackEnd časti vrátene v správnom (zaužívanom) formáte, môžeme využiť dostupnú ```convertDataForLineChart()``` funkciu ako je to zobrazené v nasledujúcom príklade. Bližšie informácie k tomu ako táto funkcia funguje nájdete v súbore [chart-tools.js](../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js).

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

Pri vlastnej úprave dát je dôležité rozumieť, aký formát dát je grafom podporovaný a preto odporúčame si to pozrieť v dokumentácií knižnice.

## Aktualizovanie existujúceho grafu

Keďže súčasťou grafov je vždy externý filter, ktorého úloha je filtrovanie v dátach grafu, je potrebné vedieť grafy upravovať.

Príklad použitia:

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

Ako sme mohli vidieť v ukážke vyššie, dáta grafu môžeme zmeniť kedykoľvek počas behu programu. Stačí ak vo vytvorenej inštancií formulára grafu vymeníme staré dáta grafu za nové a pomocou dostupnej funkcie ```updateChart()``` spustíme aktualizáciu grafu, ktorá sa o všetko potrebné postará.

## Aktualizovanie tabuľky

Niekedy pri filtrovaní dát grafu pomocou externého filtra chceme filtrovať aj dáta tabuľky, ale to môže byť problém, keďže externý filter nemusí byť prepojený s tabuľkou (alebo máme viac tabuliek a iba jedná z nich môže byť prepojená s externým filtrom). Preto potrebujeme spôsob ako tieto tabuľky filtrovať.

Príklad použitia:

```javascript
//Pridanie parametrov do url
topDataTable.ajax.url(WJ.urlAddParam(url, "chartType", ChartType.Not_Chart));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchRootDir", $('#rootDir').val()));
topDataTable.ajax.url(WJ.urlAddParam(topDataTable.ajax.url(), "searchFilterBotsOut", $('#botFilterOut').is(':checked')));

//Refresh tabuľky
topDataTable.ajax.reload();
```

Z príkladu vidíme že niekedy nestačí tabuľku iba obnoviť, ale potrebujeme si pridať parametre do url adresy, prípadne ich iba upraviť. Ďalej to už je práca BackEnd-u ktorý tieto parametre z url dostane a vráti patričné dáta pre tabuľku.