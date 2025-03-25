# Třída 'Cell Visibility Service'

Třída `CellVisibilityService` rozšiřuje funkcionalitu `DataTables`

***

**Dependencies (závislosti)**

- [Tools](tools.md)
- [StorageHandler](storage-handler.md)

***

### Popis fungování:

Při potvrzení nastavení se odešle pomocí metody [buildConfigDataFromObject()](#buildconfigdatafromobject) objekt aktuální dataTabulky a ten z ní vybere její ID a atribut `bVisible`, který následně uloží do localStorage pod unikátním klíčem, který identifikuje konkrétní tabulku.

Při každém dalším rederu tabulky se pomocí metody [updateColumnConfig()](#updatecolumnconfig) do které posíláme ID tabulky a konfigurační objekt samotné tabulky. Vybereme uložená nastavení z localStorage na základě ID a aktualizujeme konfigurační objekt, který se následně nastaví jako výchozí.

***

## Vytvoření instance:

**WebJET** vytváří instanci v souboru [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)

```javascript
import {CellVisibilityService} from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

**Použití:**

**WebJET** používá třídu v souboru [.../webjetdatatables/index.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

Sesbírání a uložení dat probíhá, při stisku tlačítka save v konfiguraci sloupců.

```javascript
{
    action: function action(e, dataTable) {
        window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
    }
}
```

Aktualizace column dat tabulky probíhá při jejím vytváření.

```javascript
DataTable({
    columns: window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, DATA.columns),
});
```

## Seznam API

**(Kliknutím zobrazíš detail pro funkci)**

| Metody |
| ------------------------------------------------------------- |
| [buildConfigDataFromObject()](#buildconfigdatafromobject) |
| [updateColumnConfig()](#updatecolumnconfig)               |

### Detailní popis funkcí

#### buildConfigDataFromObject()

Z objektu dataTable vybere seznam dostupných fieldů a vybere z nich název fieldu (data) a atribut visible (bVisible).

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

Aktualizuje existující column objekt uloženými daty. Pokud data neexistují, metoda vrátí její vstup.

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
