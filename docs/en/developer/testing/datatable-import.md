# Testing import into datasheet

We have also prepared the option to automatically test data import into a data table. The test will perform the following operations:

- verifies that there is no data entered in the table
- imports data and verifies its display/filtering
- edits required fields (adds the expression ```-changed```) and verifies their display after saving
- re-imports data with matching according to the specified column, verifies that the data no longer contains ```-changed``` text

The implementation is similar to [Automatic testing of DataTables](datatable.md). For preparation you need:

- create a test record in the table (ideally, with as much data as possible filled in)
- export the table to Excel, leaving only the header and test record in it
- in Excel, edit the data as follows:
  - leave the value of the ID column, this will verify the overwriting of the original record (it must not be overwritten by import)
  - adjust the other columns accordingly, we recommend adding the expression ```-import-test```
  - define one unique column (e.g. name) that will be used for validation Update existing records

For order, save the Excel file prepared in this way in the same directory as your test script and name it the same (of course with the .xlsx extension). An example is ```insert-script.js``` and ```insert-script.xlsx```.

![](test-import-excel.png)

Test example:

```javascript
Scenario('insert script-import', async ({ I, DataTables }) => {
     I.waitForText('Skripty', 5);
     await DataTables.importTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'], //For this table we have fixedly defined that only these attributes are filled in, try leaving them empty so that all are filled in
          file: 'tests/components/insert-script.xlsx',
          updateBy: 'Názov / popis skriptu - name',
          rows: [
               {
                    name: "Test import"
               }
          ],
          editSteps: function (row, counter, I, options, DT, DTE) {
               I.seeInField('#DTE_Field_sortPriority', '10');
          }
     });
});
```

In addition to the standard parameters of the [automated datatable test](datatable.md), additional parameters are used:

- ```file``` - ​​path to xlsx file with test import data
- ```updateBy``` - ​​value used for testing Update existing records
- ```rows``` - ​​field containing the column name and value that will be used for checking/filtering in the table after import
- `preserveColumns` - ​​list of columns that are not in the Excel file. They will be set to a random value during the change and then when updating by import, it will be verified that the value has not been overwritten/preserved. E.g. `preserveColumns: [ 'title', 'deliveryFirstName','deliveryLastName' ]`.
- `editSteps` - ​​optional callback function `function(row, counter, I, options, DT, DTE)`, which is called when editing the imported record before saving. Allows you to perform additional validations or changes in the editor (e.g. check default values ​​of fields that are not part of the required fields). Parameters `row` and `counter` contain the current row and its sequence number.

The important parameter is ```rows``` where you define the list of columns that will be used to filter records after import. The value must match the value in the Excel file.