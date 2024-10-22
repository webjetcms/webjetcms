# Třída "Cell Visibility Service

Třída `CellVisibilityService` rozšiřuje funkčnost `DataTables`

***

**Závislosti**

- [Nástroje](tools.md)
- [StorageHandler](storage-handler.md)

***

### Popis operace:

Po potvrzení nastavení se odešle pomocí metody [buildConfigDataFromObject()](#buildconfigdatafromobject) objektu aktuální tabulky dataTable a získá jeho ID a atribut `bVisible`, který je pak uložen v úložišti localStorage pod jedinečným klíčem, který identifikuje konkrétní tabulku.

Při každé změně pořadí tabulky se metoda [updateColumnConfig()](#updatecolumnconfig) do kterého posíláme ID tabulky a konfigurační objekt samotné tabulky. Na základě ID vybereme uložené nastavení z localStorage a aktualizujeme konfigurační objekt, který je pak nastaven jako výchozí.

***

## Vytvoření instance:

**WebJET** vytvoří instanci v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import {CellVisibilityService} from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

**Použití:**

**WebJET** používá třídu v souboru [.../webjetdatatables/index.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

Data se shromažďují a ukládají po stisknutí tlačítka uložit v konfiguraci sloupce.

```javascript
{
    action: function action(e, dataTable) {
        window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
    }
}
```

Data sloupců tabulky se aktualizují při vytváření tabulky.

```javascript
DataTable({
    columns: window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, DATA.columns),
});
```

## Seznam rozhraní API

**(Kliknutím zobrazíte detail funkce)**

| Metody |
| --------------------------------------------------------- |
| [buildConfigDataFromObject()](#buildconfigdatafromobject) |
| [updateColumnConfig()](#updatecolumnconfig)               |

### Podrobný popis funkcí

#### buildConfigDataFromObject()

Z objektu dataTable vybere seznam dostupných polí a vybere název pole (data) a atribut visible (bVisible).

```javascript
/**
 * @description Z objektu dataTable, vyberie zoznam dostupných fieldov a vyberie z nich názov fieldu (data) a atribút visible (bVisible).
 * @example
 *              Vo fielde chodia atribúty `visible` a `bVisible`.
 *
 *              `visible` - predošlá hodnota / tá nás nezaujíma /
 *              `bVisible` - aktuálna (nová) hodnota
 *
 * @param {Object} dataTable
 * @returns {void}
 * @public
 */
window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
```

***

#### updateColumnConfig()

Aktualizuje existující objekt sloupce pomocí uložených dat. Pokud data neexistují, metoda vrátí svůj vstup.

```javascript
/**
 * @description Aktualizuje existujúci column objekt uloženými dátami. Ak dáta neexistujú, metóda vráti jej vstup.
 * @param {string} dataTableID
 * @param {Object[]} configArray
 * @returns {Object[]}
 * @public
 */
window.dataTableCellVisibilityService.updateColumnConfig(dataTableID, configArray);
```

***
