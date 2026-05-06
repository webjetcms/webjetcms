# Automatic testing of DataTables

In the administration, most datatable tests are identical in the form of ```CRUD (Create, Read, Update, Delete)``` operations. In order to avoid repeating the standard datatable test procedures over and over again, we have prepared **automated testing**. The implementation is in the file [DataTables.js](../../../../src/test/webapp/pages/DataTables.js) and includes the following steps:

- adding a new record
  - verification of required fields
- search for added record
- record editing
- checking cross-domain record separation
- search for edited record
- delete record
- verification of the recording of audit records

## Use

To use it, you need to define a variable in the pug file through which the data table is accessible:

```javascript
//POZOR: je potrebne definovat cez var, nie cez let
var tempsTable;
window.domReady.add(function () {
    ...
    tempsTable = WJ.DataTable({
        ...
    });
});
```

Then, in a test scenario, you can use a basic test like:

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable'
    });
});
```

in ```dataTable``` you set the **name** of the variable in the page. It is also possible to use other test parameters (example in the file [templates.js](../../../../src/test/webapp/tests/components/templates.js)):

```javascript
Scenario('zakladne testy', async ({I, DataTables}) => {
    await DataTables.baseTest({
        //meno objektu vo web stranke s datatabulkou
        dataTable: 'tempsTable',
        //zoznam povinnych poli, ak nie su zadane nacitaju sa automaticky z columns definicie (maju atribut required: true)
        requiredFields: ['oldUrl', 'newUrl'],
        //nepovinne, ak zadate, tak pre pole nazovPola sa vyplni test-hodnota namiesto generovanej autotest-xxxx
        //je to potrebne, ked polia maju specificky format/dlzku (napr. email)
        testingData: {
          nazovPola: "test-hodnota",
          email: "email@domena.sk"
        },
        //kroky, ktore sa vykonaju ako prve pri novom zazname
        createSteps: function(I, options, DT, DTE) {
            I.fillField("#xxx", "yyy");
        },
        //kroky, ktore sa vykonaju po ulozeni zaznamu
        afterCreateSteps: function(I, options, requiredFields, DT, DTE) {
            requiredFields.push("nazovPola");
            options.testingData[2] = "test-hodnota";
        },
        //ktoky, ktore sa vykonaju pri zmene existujuceho zaznamu
        editSteps: function(I, options, DT, DTE) {
            I.fillField("#xxx", "yyy-edited");
        },
        //kroky, ktore sa vykonaju pri vyhladani zmeneneho zaznamu
        editSearchSteps: function(I, options, DT, DTE) {
            I.fillField("input.dt-filter-availableGrooupsList", "News");
        },
        //kroky, ktore sa vykonaju pred zmazanim zaznamu
        beforeDeleteSteps: function(I, options, DT, DTE) {
            I.wait(20);
        },
    });
});
```

When saving a new record, an array of filled-in data for mandatory fields is also saved to the ```options.testingData``` object. You can use these, for example, in the ```editSearchSteps``` function as:

```javascript
    I.see(`${options.testingData[0]}-change`, "div.dt-scroll-body");
```

The ```options``` object is returned from the ```baseTest``` function and can be used in other scenarios, an example is in [translation-keys.js](../../../../src/test/webapp/tests/settings/translation-keys.js)

!>**Warning:** Automated testing of basic operations does not replace comprehensive testing. It is intended to serve as a basis for testing, your task is to add test scenarios for specific features. Ideally as **standalone scenarios**, or by adding steps to ```createSteps, editSteps, editSearchSteps, beforeDeleteSteps``` functions.

## Setting options

Via ```options``` object it is mandatory to set:

- ```dataTable``` - ​​variable name in the web page with the data table.

Optional options:

- ```requiredFields``` - ​​list of mandatory fields. If they are not specified, they will be automatically loaded from the ```columns``` definition (they have the ```required: true``` attribute).
- ```testingData``` - ​​list of values ​​to fill in instead of the generated ```autotest-xxxx```. This is needed when fields have a specific format/length (e.g. email).
- ```container``` - ​​option to define the CSS selector of the container in which the data table is inserted (necessary to define for a nested data table).
- ```containerModal``` - ​​option to define CSS selector of datatable editor dialog container (necessary to define for nested datatable).
- ```skipRefresh``` - ​​if set to ```true```, the web page will not be refreshed after adding a record.
- ```skipSwitchDomain``` - ​​if set to ```true```, cross-domain record separation checking will not be performed.
- ```switchDomainName``` - ​​option to define a different domain than the default ```mirroring.tau27.iway.sk``` to control cross-domain record separation.
- ```skipDuplication``` - ​​if set to ```true```, the record duplication test will not be performed.
- ```createSteps``` - ​​function adding testing steps when creating a new record.
- ```afterCreateSteps``` - ​​the function is executed after saving a new record. If the table does not have any mandatory fields, you can define the field and set its value by setting ```requiredFields.push("string1");options.testingData[0] = string1;```.
- ```editSteps``` - ​​function adding testing steps when editing a record.
- ```editSearchSteps``` - ​​function adding testing steps when searching for a record.
- ```beforeDeleteSteps``` - ​​function adding testing steps before deleting a record.
- ```perms``` - ​​name of the right (e.g. ```editor_edit_media_all```) for automatic security verification of the REST service.
- ```extraWait``` - ​​if set, in some cases the set time in seconds will be waited, necessary if the page loads some extra data.

Internally, the following attributes are set after initialization:

- ```id``` - ​​ID of the datatable object in the DOM tree.
- ```url``` - ​​URL address of the current web page.
- ```testingData``` - ​​contains the set values ​​of mandatory fields.

## How to generate required fields

The automated test fills in only the required fields, and also tests the display of an error message when a required field is not filled in. At the beginning of the test, the constants ```startDate``` and ```date``` are defined, and the value of the required field is generated from them, to which a random number is added. The value always contains the text **autotest**, which can be used to search for undeleted records (it is safe to delete them).

```javascript
const randomText = I.getRandomText();
const randomTextShort = I.getRandomTextShort();

//definovanie textu povinneho pola
if (field.toLocaleLowerCase().indexOf("email")!=-1) {
    testingData[index] = `${field}-autotest-${randomTextShort}@onetimeusemail.com`;
} else {
    testingData[index] = `${field}-autotest-${randomText}`;
}

//vyplnenie hodnoty
I.fillField(`#DTE_Field_${field}`, `${testingData[index]}`);
```

You can use the ```testingData``` configuration to pre-set the value of a field to set it on creation. In that case, the generated value ```autotest-xxxx``` will not be used.

If the field name has the value email, the value will also be generated with the domain name at the end so that the field has a valid value.

## Audit trail testing

After deleting the records, the audit is tested. The number of records since the test was started (according to the constant ```startDate```) containing the text of the first mandatory field (```testingData[0]```) in the description is checked.

The number of records found is verified, which should be at least 3 (create, edit, delete).

## Rights testing

After entering the parameter ```perms```, the data table automatically verifies the entered right. It displays the contents of the data table, removes the right, and then verifies the display of the error message ```Prístup k tejto stránke je zamietnutý```.

Example of use:

```javascript
await DataTables.baseTest({
    dataTable: 'mediaGroupsTable',
    perms: 'editor_edit_media_group'
});
```

!>**Warning:** entering this parameter is mandatory so that the rights are also tested every time the data table is displayed. However, with a nested data table, this test may be problematic due to logout, you can enter an empty value or the character ```-```.

## Implementation details

The test is implemented in the file [DataTables.js](../../../../src/test/webapp/pages/DataTables.js). The basis is to obtain the columns definition:

```javascript
const columns = await I.getDataTableColumns(dataTable);
```

From which the list of fields, their settings, etc. are read. If mandatory fields are not defined, they are obtained from the columns definition according to the ```required: true``` attribute.

The function ```I.getDataTableColumns``` is defined in [custom_helper.js](../../../../src/test/webapp/custom_helper.js):

```javascript
  /**
   * Metoda vrati stlpce vo formate .json
   * @param {string} dataTable objekt pre datatabulku
   */
  async getDataTableColumns(dataTable) {
    const { page } = this.helpers['Playwright'];
    const window = await page.evaluateHandle(() => window);
    const table = await window.getProperty(dataTable);
    const DATA = await table.getProperty('DATA');
    const columns = await DATA.getProperty('columns');
    const columnsToJson = await columns.jsonValue();
    try {
      if (columnsToJson) return columnsToJson;
    } catch(err) {
      console.log(err);
    }
  }
```

All functions are asynchronous due to the call to ```I.getDataTableColumns```.

!>**Warning:** During the implementation, we found that the second call ```this.helpers['Playwright'];``` logs out the logged-in user (resets cookies) during the test run. We wanted to use this call when formatting dates, but unfortunately this is currently not possible. We assume that this is a bug in CodeceptJS.

Subsequently, the individual steps of the test are performed as we commonly know them.