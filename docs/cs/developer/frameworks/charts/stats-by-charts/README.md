# Statistiky pomocí grafů

Třída `StatsByCharts` v souboru [stats-by-charts.js](../../../../../../src/main/webapp/apps/_common/charts/stats-by-charts.js) je pomocná obálka (wrapper) nad [chart-tools.js](../../../../../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js). Jejím cílem je zjednodušit tvorbu více grafů najednou ze strukturovaných dat přicházejících z backendu (REST API).

## Struktura dat

Data musí být ve formátu pole obsahujícího objekty – jeden objekt pro každý graf.

Každý objekt **musí obsahovat**:

- `id` – jedinečný identifikátor grafu
- `type` – hodnotu enumerace [ChartType](../backend/README.md#typ-grafu), která specifikuje o jaký typ grafu máme zájem
- `values` – pole hodnot pro graf

Objekty **mohou navíc obsahovat**:

- `title` – nadpis, který se použije pro vygenerovaný graf
- `chart_colorScheme` – hodnota [barevného schématu](../backend/README.md#barevná-schéma-grafů), která se má použít pro **tento specifický graf**
- `xAxeName` – název pole v objektu `values`, které reprezentuje osu X grafu
- `yAxeName` – název pole v objektu `values`, které reprezentuje osu Y grafu

Výchozí hodnoty `xAxeName` / `yAxeName` se liší podle typu grafu:

| Typ grafu | `xAxeName` | `yAxeName` |
| --- | --- | --- |
| `PIE_CLASSIC` / `PIE_DONUT` | `"name"` | `"count"` |
| `BAR_VERTICAL` / `BAR_HORIZONTAL` | `"count"` | `"name"` |
| `WORD_CLOUD` | `"name"` | `"count"` |

Grafy `DOUBLE_PIE` a `TABLE` používají místo `yAxeName` specifické vlastnosti:

- pro typ `DOUBLE_PIE`: `yAxeName_inner` (výchozí `"count"`) a `yAxeName_outer` (výchozí `"count"`); osa X zůstává `xAxeName` (výchozí `"name"`)
- pro typ `TABLE`: `paramsNames` – pole názvů polí zobrazených v tabulce (výchozí `["name", "count"]`)

## Co třída dělá

- Načte amcharts knihovnu (`window.initAmcharts()`) automaticky při prvním vytvoření grafů.
- Dynamicky vytváří DOM elementy (kontejner, `<div>` pro každý graf, tlačítko nastavení).
- Podle hodnoty `type` v datech rozhodne, jaký typ grafu se má vykreslit. Rozpoznává rozdíly mezi jednotlivými variantami (např. `pie_donut` vs. `pie_classic`) a patřičně upraví nastavení bez dalšího zásahu. Podporuje typy:
  - `pie_donut` / `pie_classic` -> koláčový graf (`PieChartForm`)
  - `bar_vertical` / `bar_horizontal` -> sloupcový graf (`BarChartForm`)
  - `table` -> tabulka (`TableChartForm`)
  - `word_cloud` -> slovní oblak (`WordCloudChartForm`)
- Udržuje mapu instancí grafů (`chartsInstances`) pro pozdější aktualizaci.
- Umožňuje aktualizaci jednotlivého grafu bez obnovy celé stránky – stará instance se zničí a nová se vytvoří na stejném místě.
- Ke každému grafu automaticky přidá tlačítko nastavení s volitelnou callback funkcí.

!> **Upozornění:** Grafy typu `LINE` třídou `StatsByCharts` **nejsou** podporovány, protože vyžadují specifické nastavení ze strany programátora na frontendové straně.

## Výhody oproti přímému použití `chart-tools.js`

| | `chart-tools.js` přímo | `StatsByCharts` |
| --- | --- | --- |
| Inicializace amcharts | manuálně `window.initAmcharts().then(...)` | automaticky |
| Vytvoření DOM elementů | manuálně (`<div id="..."> `) | automaticky |
| Více grafů | každý zvlášť | iterace přes pole dat |
| Určení typu grafu | manuálně (volíte správnou třídu) | podle `type` v datech |
| Aktualizace grafu | manuálně zrušit + znovu vytvořit | `updateChart()` |
| Tlačítko nastavení | manuálně | automaticky, s callback podporou |

`chart-tools.js` zůstává vhodnou volbou, pokud potřebujete plnou kontrolu nad individuálním grafem (např. jeden graf s vlastní logikou, typ `LINE`). `StatsByCharts` je vhodná tam, kde backend vrací pole grafů s jejich konfigurací.

## API

### Konstruktor

```javascript
new StatsByCharts(options)
```

| Parametr | Typ | Popis |
| --- | --- | --- |
| `options.targetSelector` | `string` | CSS selektor elementu, do kterého se vloží grafy (povinný) |
| `options.id` | `string` | Prefix pro unikátní ID grafů (výchozí ``stats-by-charts'`) |
| `options.chartSettingBtnFn` | `function` | Callback volaný po kliknutí na tlačítko nastavení grafu; dostane objekt `chartDef` jako argument |

### Metody

#### `createCharts(chartsDefinitions)`

Vytvoří všechny grafy najednou. Volejte po načtení dat z REST API.

- `chartsDefinitions` – pole objektů s definicí grafů (struktura popsaná níže).

#### `updateChart(newChartsDefinitions)`

Aktualizuje jeden nebo více stávajících grafů.

- Zničí starou instanci grafu, odstraní hlavičku a vykreslí nový graf s aktualizovanými daty.

## Použití – příklad

Následující příklad pochází ze stránky statistik formulářů [form-stats.html](../../../../../../src/main/webapp/apps/form/admin/form-stats.html).

### 1. Import třídy

```javascript
import { StatsByCharts } from '/apps/_common/charts/stats-by-charts.js';
```

### 2. Vytvoření instance a grafů

```javascript
fetch("/rest/multistep-form-stat/get-stat-data?form-name=" + urlFormName)
    .then(response => response.json())
    .then(data => {
        let instance = new StatsByCharts({
            targetSelector: "#chartContainer",
            id: "form-stats",
            chartSettingBtnFn: (chartDef) => {
                // Otvorenie modálneho okna pre nastavenie grafu
                WJ.openIframeModalDatatable({
                    url: "/apps/form/admin/form-stats-table/?id=-1&formName=" + urlFormName + "&itemFormId=" + chartDef.id + "&showOnlyEditor=true",
                    width: 850,
                    height: 500,
                    buttonTitleKey: "button.save"
                });
            }
        });

        // Vykreslenie všetkých grafov naraz
        instance.createCharts(data.chartData);
    });
```

### 3. HTML kontejner

```html
<div id="chartContainer"></div>
```

Třída sama vytvoří vnitřní strukturu:

```html
<div id="chartContainer">
    <div id="form-stats">
        <div id="form-stats_{chartId}_container" class="stat-chart-wrapper">
            <button class="btn btn-sm btn-outline-secondary chart-more-btn">...</button>
            <div id="form-stats_{chartId}" class="amcharts"></div>
        </div>
        <!-- ďalší graf... -->
    </div>
</div>
```

### 4. Aktualizace grafu po změně nastavení

Graf lze aktualizovat bez obnovy celé stránky voláním metody `updateChart(data)`, kde `data` je pole jednoho nebo více objektů grafů ve stejné struktuře jako při [vytváření](#struktura-dat). Třída podle `id` automaticky najde existující instanci, zničí ji a vykreslí nově s novými daty.

Příklad aktualizace grafu/grafů:

```javascript
fetch("/get_new_chart_data")
    .then(response => response.json())
    .then(data => {
        instance.updateChart(data);
    })
    .catch(error => {
        console.error("Error fetching chart new data:", error);
    });
```

Ukázka vygenerované statistiky z příkladu výše:

![](example-1.png)