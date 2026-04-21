# 'Cell Visibility Service' class

Class `CellVisibilityService` extends the functionality of `DataTables`

---
**Dependencies**

- [Tools](tools.md)
- [StorageHandler](storage-handler.md)
---

### Description of operation:

When confirming the settings, the object of the current dataTable is sent using the [buildConfigDataFromObject()](#buildconfigdatafromobject) method.
and it extracts its ID and attribute `bVisible`, which it then stores in localStorage under a unique key that identifies the specific table.

With each subsequent table refresh, we use the [updateColumnConfig()](#updatecolumnconfig) method to which we send the table ID and the configuration object of the table itself.
We will retrieve the saved settings from localStorage based on the ID and update the configuration object, which will then be set as the default.

---
## Creating an instance:

**WebJET** creates an instance in the file [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import {CellVisibilityService} from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

**Usage:**

**WebJET** uses the class in the file [.../webjetdatatables/index.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)

Data collection and storage occurs when you press the save button in the column configuration.
```javascript
{
    action: function action(e, dataTable) {
        window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
    }
}
```

The table's column data is updated when it is created.
```javascript
DataTable({
    columns: window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, DATA.columns),
});
```

## API List

**(Click to view feature details)**

| Methods                                                    |
| -----------                                               |
| [buildConfigDataFromObject()](#buildconfigdatafromobject) |
| [updateColumnConfig()](#updatecolumnconfig)               |

### Detailed description of functions

#### buildConfigDataFromObject()

From the dataTable object, it selects a list of available fields and selects the field name (data) and the visible attribute (bVisible).
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

Updates an existing column object with the stored data. If the data does not exist, the method returns its input.
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
