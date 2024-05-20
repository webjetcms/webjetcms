# Class 'Cell Visibility Service'

Class `CellVisibilityService` extends functionality `DataTables`

***

**Dependencies**

- [Tools](tools.md)
- [StorageHandler](storage-handler.md)
***

### Description of operation:

When the settings are confirmed, it is sent using the method [buildConfigDataFromObject()](#buildconfigdatafromobject) object of the current dataTable and it extracts its ID and attribute `bVisible`, which is then stored in localStorage under a unique key that identifies the specific table.

Each time the table is reordered, the method [updateColumnConfig()](#updatecolumnconfig) to which we send the table ID and the configuration object of the table itself. We select the saved settings from localStorage based on the ID and update the configuration object, which is then set as the default.

***

## Creating an instance:

**WebJET** creates an instance in the file [app.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/src/js/app.js)
```javascript
import { CellVisibilityService } from "./libs/data-tables-extends/";

window.dataTableCellVisibilityService = new CellVisibilityService();
```

**Application:**

**WebJET** uses a class in the file [.../webjetdatatables/index.js](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)
The data is collected and saved when the save button is pressed in the column configuration.

```javascript
{
	action: function action(e, dataTable) {
		window.dataTableCellVisibilityService.buildConfigDataFromObject(dataTable);
	}
}
```

The column data of the table is updated when the table is created.

```javascript
DataTable({
	columns: window.dataTableCellVisibilityService.updateColumnConfig(DATA.id, DATA.columns),
});
```

## List of APIs

**(Click to see the detail for the function)**
| Methods | | --------------------------------------------------------- | | | [buildConfigDataFromObject()](#buildconfigdatafromobject) | | [updateColumnConfig()](#updatecolumnconfig)               |

### Detailed description of functions

#### buildConfigDataFromObject()

From the dataTable object, selects the list of available fields and selects the field name (data) and the visible attribute (bVisible).

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

***
