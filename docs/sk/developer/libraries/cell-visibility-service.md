# Trieda 'Cell Visibility Service'

Trieda `CellVisibilityService` rozširuje funkcionalitu `DataTables`

---
**Dependencies (závislosti)**
- [Tools](tools.md)
- [StorageHandler](storage-handler.md)
---

### Opis fungovania:

Pri potvrdení nastavení sa odošle pomocou metódy [buildConfigDataFromObject()](#buildconfigdatafromobject) objekt aktuálnej dataTabuľky
a ten z nej vyberie jej ID a atribút `bVisible`, ktorý následne uloží do localStorage pod unikátnym kľúčom, ktorý identifikuje konkrétnu tabuľku.

Pri každom ďalšom redere tabuľky sa pomocou metódy [updateColumnConfig()](#updatecolumnconfig) do ktorej posielame ID tabuľky a konfiguračný objekt samotnej tabuľky.
Vyberieme uložené nastavenia z localStorage na základe ID a aktualizujeme konfiguračný objekt, ktorý sa následne nastaví ako východiskový.
---
## Vytvorenie inštancie:
**WebJET** vytvára inštanciu v súbore [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import {CellVisibilityService} from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

**Použitie:**

**WebJET** používa triedu v súbore [.../webjetdatatables/index.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

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

## Zoznam API
**(Kliknutím zobrazíš detail pre funkciu)**

| Metódy                                                    |
| -----------                                               |
| [buildConfigDataFromObject()](#buildconfigdatafromobject) |
| [updateColumnConfig()](#updatecolumnconfig)               |


### Detailný popis funkcií

#### buildConfigDataFromObject()
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

#### updateColumnConfig()
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
