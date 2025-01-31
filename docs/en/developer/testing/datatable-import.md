# Testing the import into the datatable

We have also added the ability to automatically test importing data into a datatable. The test performs the following operations:
- verifies that there is no data entered in the table
- imports data and verifies its display/filtering
- modifies required fields (adds an expression `-changed`) and verifies their display after saving
- reimports the data with matching according to the specified column, verifies that the data no longer contains `-changed` text

The implementation is similar to that for [Automatic DataTables testing](datatable.md). For preparation you need:
- create a test record in a spreadsheet (ideally with as much data as possible filled in)
- export the table to Excel, leaving only the header and the test record
- in Excel, edit the data as follows:
  - keep the column ID value, this will verify the overwrite of the original record (it must not be overwritten by the import)
  - edit the other columns accordingly, we recommend adding the expression `-import-test`
  - specify one unique column (e.g. name) that will be used to verify Update existing records

For the sake of order, save the prepared Excel file in the same directory as your test script and name it the same way (only with .xlsx extension, of course). Example `insert-script.js` a `insert-script.xlsx`.

![](test-import-excel.png)

Example test:

```javascript
Scenario('insert script-import', async ({ I, DataTables }) => {
     I.waitForText('Zoznam skriptov', 5);
     await DataTables.importTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'], //pre tuto tabulku mame fixne definovane, aby sa vyplnili len tieto atributy, pokuste sa nechat prazdne aby sa vyplnili vsetky
          file: 'tests/components/insert-script.xlsx',
          updateBy: 'Názov / popis skriptu - name',
          rows: [
               {
                    name: "Test import"
               }
          ]
     });
});
```

In addition to the standard parameters [automated datatable test](datatable.md) additional parameters are used:
- `file` - path to xlsx file with import test data
- `updateBy` - value used for testing Update existing records
- `rows` - an array containing the column name and the value that will be used for checking/filtering in the table after import
- `preserveColumns` - list of columns that are not in the Excel file. They will be set to a random value during the change, and then when updated by import, it will be verified that the value has not been overwritten/preserved. For example. `preserveColumns: [ 'title', 'deliveryFirstName','deliveryLastName' ]`.

The important parameter is `rows` in which you define a list of columns that will be used to filter the records after import. The value must match the value in the Excel file.
