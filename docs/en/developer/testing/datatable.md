# Automatic DataTables testing

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Automatic DataTables testing](#automatické-testovanie-datatables)
	- [Use](#použitie)
	- [Setting options](#možnosti-nastavenia)
	- [Method of generating mandatory fields](#spôsob-generovania-povinných-polí)
	- [Testing of audit trails](#testovanie-auditných-záznamov)
	- [Rights testing](#testovanie-práv)
	- [Implementation details](#detaily-implementácie)
<!-- /code_chunk_output -->

In the administration, most of the datatable tests are identical in form `CRUD (Create, Read, Update, Delete)` operations. To avoid repeating the standard datatable test procedures over and over again, we have prepared **automated testing**. The implementation is in the file [DataTables.js](../../../src/test/webapp/pages/DataTables.js) and includes the following steps:
- adding a new record
	- verification of mandatory fields
- search for an added record
- record editing
- search for an edited record
- deleting an entry
- verification of the entry of audit records

## Use

To use it, it is necessary to define a variable in the pug file through which the datatable is accessible:

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

Then in the test scenario you can use the basic test as:

```javascript
Scenario("zakladne testy", async ({ I, DataTables }) => {
	await DataTables.baseTest({
		//meno objektu vo web stranke s datatabulkou
		dataTable: "tempsTable",
	});
});
```

to `dataTable` you are setting up **Name** variable in the page. It is also possible to use other test parameters (example in file [templates.js](../../../src/test/webapp/tests/components/templates.js)):

```javascript
Scenario("zakladne testy", async ({ I, DataTables }) => {
	await DataTables.baseTest({
		//meno objektu vo web stranke s datatabulkou
		dataTable: "tempsTable",
		//zoznam povinnych poli, ak nie su zadane nacitaju sa automaticky z columns definicie (maju atribut required: true)
		requiredFields: ["oldUrl", "newUrl"],
		//nepovinne, ak zadate, tak pre pole nazovPola sa vyplni test-hodnota namiesto generovanej autotest-xxxx
		//je to potrebne, ked polia maju specificky format/dlzku (napr. email)
		testingData: {
			nazovPola: "test-hodnota",
			email: "email@domena.sk",
		},
		//kroky, ktore sa vykonaju ako prve pri novom zazname
		createSteps: function (I, options, DT, DTE) {
			I.fillField("#xxx", "yyy");
		},
		//kroky, ktore sa vykonaju po ulozeni zaznamu
		afterCreateSteps: function (I, options, requiredFields, DT, DTE) {
			requiredFields.push("nazovPola");
			options.testingData[2] = "test-hodnota";
		},
		//ktoky, ktore sa vykonaju pri zmene existujuceho zaznamu
		editSteps: function (I, options, DT, DTE) {
			I.fillField("#xxx", "yyy-edited");
		},
		//kroky, ktore sa vykonaju pri vyhladani zmeneneho zaznamu
		editSearchSteps: function (I, options, DT, DTE) {
			I.fillField("input.dt-filter-availableGrooupsList", "News");
		},
		//kroky, ktore sa vykonaju pred zmazanim zaznamu
		beforeDeleteSteps: function (I, options, DT, DTE) {
			I.wait(20);
		},
	});
});
```

When a new record is saved, it is also saved to the object `options.testingData` saves an array of completed mandatory field data. You can use these e.g. in the function `editSearchSteps` Like:

```javascript
I.see(`${options.testingData[0]}-change`, "div.dataTables_scrollBody");
```

Object `options` is returned from `baseTest` function and can be used in other scenarios, an example is in [translation\_keys.js](../../../src/test/webapp/tests/components/translation_keys.js)

**ATTENTION**A: Automated testing of basic operations is not a substitute for comprehensive testing. It is meant to serve as the basis of the test, your job is to add test scenarios of specific characteristics. Ideal as **separate scenarios**, or by adding steps to functions `createSteps, editSteps, editSearchSteps, beforeDeleteSteps`.
## Setting options

Via `options` object mandatory to set:
- `dataTable` - name of the variable in the datatable web page.

Options:
- `requiredFields` - list of required fields. If they are not entered they are automatically loaded from `columns` definitions (having the attribute `required: true`).
- `testingData` - list of values to fill in instead of the generated `autotest-xxxx`. This is necessary when fields have a specific format/length (e.g. email).
- `container` - the possibility to define the CSS selector of the container in which the datatable is embedded (to be defined for nested datatable).
- `containerModal` - option to define CSS container selector of datatable editor dialog (to be defined for nested datatable).
- `skipRefresh` - if set to `true` the web page is not refreshed after the record is added.
- `createSteps` - a function that adds testing steps when a new record is created.
- `afterCreateSteps` - the function is executed after the new record is saved. If the table does not have any mandatory fields, it is possible by setting `requiredFields.push("string1");options.testingData[0] = string1;` define the field and set its value.
- `editSteps` - function adding testing steps when editing a record.
- `editSearchSteps` - a function adding testing steps when searching for a record.
- `beforeDeleteSteps` - a function that adds testing steps before deleting a record.
- `perms` - name rights (e.g. `editor_edit_media_all`) to automatically verify the security of the REST service.

Internally, the following attributes are still set after initialization:
- `id` - ID of the datatable object in the DOM tree.
- `url` - URL address of the current web page.
- `testingData` - contains the set values of mandatory fields.

## Method of generating mandatory fields

The automated test fills only the required fields, it also tests the display of an error message when a required field is not filled. At the beginning of the test, constants are defined `startDate` a `date` and the value of the mandatory field is generated from them, to which a random number is also added. The value always includes the text **self-test**, by which undeleted records can be found (it is safe to delete them).

```javascript
const randomText = I.getRandomText();
const randomTextShort = I.getRandomTextShort();

//definovanie textu povinneho pola
if (field.toLocaleLowerCase().indexOf("email") != -1) {
	testingData[index] = `${field}-autotest-${randomTextShort}@onetimeusemail.com`;
} else {
	testingData[index] = `${field}-autotest-${randomText}`;
}

//vyplnenie hodnoty
I.fillField(`#DTE_Field_${field}`, `${testingData[index]}`);
```

Using the configuration `testingData` you can pre-set the value of the field to set when it is created. In this case, the generated value `autotest-xxxx` will not apply.

If the field has a value of email in the name, the value is generated with the domain name at the end so that the field has a valid value.

## Testing of audit trails

After the records are deleted, the audit is tested. The number of records since the start of the test is checked (according to the constant `startDate`) containing in the description the text of the first mandatory field (`testingData[0]`).

The number of records found is checked, which should be at least 3 (create, edit, delete).

## Rights testing

After entering the parameter `perms` datatable will also automatically verify the specified right. It displays the contents of the datatable, deletes the right, and then verifies the display of the error message `Prístup k tejto stránke je zamietnutý`.

Example of use:

```javascript
await DataTables.baseTest({
	dataTable: "mediaGroupsTable",
	perms: "editor_edit_media_group",
});
```

NOTE: Specifying this parameter is mandatory so that the rights are always tested when the datatable is displayed. For a nested datatable, however, this test can be problematic due to logout, you can specify an empty value or a character `-`.

## Implementation details

The test is implemented in a file [DataTables.js](../../../src/test/webapp/pages/DataTables.js). The key is to get a columns definition:

```javascript
const columns = await I.getDataTableColumns(dataTable);
```

From which the list of fields, their settings, etc. are read. If no mandatory fields are defined, they are obtained from the columns definition by attribute `required: true`.

Feature `I.getDataTableColumns` is defined in [custom\_helper.js](../../../src/test/webapp/custom_helper.js):

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

All functions are asynchronous because of the call `I.getDataTableColumns`.

**ATTENTION**: during the implementation we happened to have a second call `this.helpers['Playwright'];` logs out the logged-in user (resets cookies) while the test is running. We wanted to use this call when formatting dates, unfortunately this is not currently possible. We assume that this is a bug in CodeceptJS.
Then, the individual steps of the test as we normally know them are carried out.
