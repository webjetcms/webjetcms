# 'CellVisibilityService' knižnica rozširujúca funkcionalitu DataTables
###### Dependencies (závislosti)
- [StorageHandler](../../storage-handler)


### Inicializácia
```javascript
import {CellVisibilityService} from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

#### Použitie:
Zozbieranie a uloženie dát prebieha, pri stlačení tlačítka save v konfigurácii stĺpcov.
```javascript
{
    action: function action(e, dataTable) {
        window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
    }
}
```

Aktualizácia column dát tabuľky prebieha pri jej vytváraní.
```javascript
DataTable({

    columns: window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, DATA.columns),

});
```

### Zoznam API:
###### (Kliknutím zobrazíš detail pre funkciu)
- [buildConfigDataFromObject()](#readme-buildConfigDataFromObject)
- [updateColumnConfig()](#readme-updateColumnConfig)

## Detailný popis funkcií

#### buildConfigDataFromObject() <a name="readme-buildConfigDataFromObject"></a>
Z objektu dataTable, vyberie zoznam dostupných fieldov a vyberie z nich názov fieldu (data) a atribút visible (bVisible).
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
---

#### updateColumnConfig() <a name="readme-updateColumnConfig"></a>
Aktualizuje existujúci column objekt uloženými dátami. Ak dáta neexistujú, metóda vráti jej vstup.
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
---

