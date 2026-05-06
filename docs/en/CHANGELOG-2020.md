# Changelog version 2020

## 2020.53

> 2020.53 brings compatibility with use in client projects. The package is available on the ```artifactory/maven``` server as version ```2021.0-SNAPSHOT```. When first run, it adds database columns to some tables, but the change is backward compatible and it is still possible to go back to WebJET 8.8.

In client projects, just set the appropriate version in ```build.gradle```:

```gradle
ext {
    webjetVersion = "2021.0-SNAPSHOT";
}
```

We have experimentally verified the basic functionality on projects with MariaDB, Microsoft SQL and Oracle DB.

**Build Artifactory/Maven versions**

#52318 [Build file](../ant/build.xml) contains multiple ```task``` elements, the final one is ```deploy```, which has the dependencies set correctly, so just run that one. List ```taskov```:

- ```setup``` - ​​restores dependencies and generates ```WAR``` archive
- ```updatezip``` - ​​prepares a temporary structure in the ```build/updatezip``` directory. The structure contains the unpacked ```WAR``` archive, unpacked ```webjet-XXXX.jar``` files (i.e. the complete structure of the /admin, /components and /WEB-INF/classes directories)
- ```preparesrc``` - ​​downloads the ```SRC``` jar file and prepares the structure for the jar archive with source files (linked from the jar archive and the WebJET 2021 source code)
- ```define-artifact-properties``` - ​​defines properties for generating artifacts, here in ```artifact.version``` the version of the generated artifact is set
- ```makejars``` - ​​prepares jar class archives, /admin and /components directories and source files
- ```download``` - ​​helper task to download ```pom``` files and jar archives that are not modified (```struts, daisydiff, jtidy, swagger```)
- ```deploy``` - ​​the actual storage of artifacts on the ```artrifactory``` server, contains the definition of dependencies (generation of the ```POM``` file)

How to generate a new version:

```shell
cd ant
ant deploy
```

*Note*: an unpacked structure will be created in the ```build/updatezip``` directory, which can be zipped and used as an update package for WebJET in the old structure (not using jar archives).

**JPA and Spring initialization**

JPA and Spring initialization is moved to package ```sk.iway.webjet.v9```. For backward compatibility, we had to solve the problem with naming database tables ```_adminlog_``` and ```_properties_``` in Oracle DB. There, the names ```webjet_adminlog``` and ```webjet_properties``` are used. However, the table name is directly in the JPA entities. We used the option of using the interface ```SessionCustomizer```, which is implemented in the class [JpaSessionCustomizer](../src/main/java/sk/iway/webjet/v9/JpaSessionCustomizer.java). During JPA initialization, it goes through all found JPA entities and changes the annotation value for the specified database tables in the case of an Oracle installation.

We added the correct database type setting for JPA (the previous version only had the MariaDB option set).

**Home Screen**

#47428 Sliced ​​home screen design, filled with data. For now, the data is inserted directly into [ThymeleafAdminController](../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java), later after the properties are finalized it will be read via ajax.

The screen is cut using VUE components. New DTO objects for ```DocDetails, Audit a User beany``` are used.

JavaScript object ```window.currentUser``` is available. Added VUE filters for date and time formatting - ```formatDate,formatDateTime,formatDateTimeSeconds,formatTime```.

**Other changes**

- [Scripts] corrected audit record type from ```CLIENT_SPECIFIC``` to correct ```INSERT_SCRIPT```, added method ```toString()``` on doc and group beans for better marking of selected restriction on directories/pages in audit record (record ID and path to directory/page are written).
- [WebJET] modified initialization to search for SpringConfig in package ```sk.iway.webjet.v9```, automatic search for translation files and autoupdate also in ```text-webjetXX.properties``` and ```autoupdate-webjetXX.xml``` (where webjetXX is the value from configuration variable ```defaultSkin```).
- ```autoupdate.xml``` - ​​improved compatibility with WebJET 8, so that it is possible to run in a WebJET 2021 project and at the same time return to WebJET 8.8 without any impact. Columns ```id,update_date``` have been added to the table ```_properties_``` and column ```task_name``` to the table ```crontab```. The columns do not conflict with the old version and do not prevent the use of administration in version 8.8.

**Documentation**

- it is deployed at http://docs.webjetcms.sk/v2021/, the side menu has not been generated yet, solved by the gradle task ```rsyncDocs```
- added documentation for inserting [HTML code without escaping](developer/frameworks/thymeleaf.md#basic-text-extract-attribute) in Thymeleaf
- added documentation for [date and time formatting](developer/frameworks/vue.md#date-and-time-formatting) in Vue

![meme](_media/meme/2020-53.jpg ":no-zoom")

## 2020.51

**Server Monitoring - Recorded Values**

New version of the display of recorded server monitoring values. It displays both a table and graphs, and for graphs it uses data aggregation by day range:

- 0-5 = exact value without aggregation
- 5-10 = aggregation for 10 minute interval
- 10-14 = aggregation for 30 minute interval
- 14-30 = aggregation per hour
- 30-60 = aggregation for 4 hours
- 60+ = aggregation for 12 hours

If the aggregation were not performed, the browser would probably crash due to the amount of data in the graph. The implementation is in the [MonitoringAggregator](../src/main/java/sk/iway/iwcm/components/monitoring/MonitoringAggregator.java) class, the highest value in the given time interval is recorded.

- #50053 [Charts] - basic documentation for the [amcharts] library (developer/frameworks/amcharts.md) has been added.
- #50053 [Charts] - in ```app.js``` added asynchronous initialization of the amcharts library by calling ```window.initAmcharts()```.

![meme](_media/meme/2020-51.jpg ":no-zoom")

## 2020.50

> 2020.50 brings correct generation of administration menu items according to the availability of the new version of the admin page. This allows you to seamlessly switch between version 8 and 9 in the menu. If necessary, you can switch to the original version of the admin page.
>
> The page editor has also been improved, automatically adjusting its height to the window size. Directory and website status icons have also been added to the tree structure.

**Website**


  - Deleted the original sample code for editing the page and directory, only datatable views are now used.
  - Added status icons in the tree structure according to the original design for the directory (internal, not displayed in the menu, password protected) and the web page (display disabled, redirected, not searchable, password protected).
  - The editor dialog has an automatically calculated height based on the window size (but at least 300 points). The height is recalculated every 3 seconds.
  - Added a click on a web page in the tree structure. This is quite complicated, since the editor is integrated with the data table. First, it is checked whether the directory in which the page is located is displayed in the data table. If not, the value ```docId``` is stored in the variable ```docidAfterLoad```. This is checked after loading the list of pages in the directory, if it is set, editing of the given page is automatically invoked.

**Generating a menu**

- #51997 [Navigation] Modified generation of menu items. In both v8 and v9 versions, links are generated to already redesigned pages in v9, so you can transparently navigate between the old and new WebJET. When requesting to display the old v8 version, you can enter ```/admin/v8``` in the URL address, which will switch the menu generation to the old version and you can display the old version of the administration. You can get to v9 by calling the URL address ```/admin/v9```. There is also an icon at the top of the page to switch to v8.

**Documentation**

Added documentation on using [jsTree](developer/jstree/README.md).

## 2020.49

**Import/Export:**

Implemented system for importing and exporting data between data tables.

Export:

- Allows export to **Excel(xlsx), PDF and direct printing to printer**
- The file will be named according to the current ```title``` page and the current date and time will be automatically added.
- It is possible to set the data type for the export (**current page/all, filtered/all/selected rows, sorting, columns**).
- With server paging, a REST service call is first made, from which **a maximum of 50,000 records** are retrieved. If you need to export more records, use multiple exports using filtering.
- Server data processing takes place outside the data table (for performance reasons), ```datetime``` columns and ```radio button``` are redrawn (```date-time```, ```date```, ```*-select```, ```*-boolean``` are supported).
- When exporting, a list of columns is prepared in the first row, **the import is then not sensitive to the order of the columns**.
- For **select fields** (select/numbers), the **text value** is exported and reconstructed back to an ID when imported. This allows you to have **different IDs of linked records** between environments (e.g. template ID for a web page), if the name matches, the record is paired correctly. The export then also contains human-readable text instead of the ID.

Import:

- Allows **import data as new** (added to the database) or **match existing data according to the selected column** (e.g. name, URL address, etc.). When matching, it automatically searches for a record in the database first and then updates it. If it does not exist, it creates a new record.
- **Importing from xlsx format**.
- The import is performed **gradually in batches of 25 records** to avoid overloading the server.

**Datatables:**

- Modified filtering by dates - added time setting option to UI, no longer need to set end of day time on backend
- Added attributes (we need them to call the REST service for data export):
  - ```DATA.jsonOptions``` - ​​setting from the REST service response for rendering (needed for exporting codebook data)
  - ```DATA.urlLatest``` - ​​complete URL of the last REST call
  - ```DATA.urlLatestParams``` - ​​all parameters of the last REST call (current page, page size, filters)

## 2020.45

- #47293 [Deleting records in the database] Reworked version of deleting data from WJ8 to datatable. It uses several hacks, such as setting [external filter](developer/datatables/README.md#external-filter), disabling paging (option paging)

**Datatables:**

- Added option **not to show filter** in table header for column by setting attribute ```filter=false``` in annotation
- Added option to **disable pagination** using ```options paging: false```, [documentation](developer/datatables/README.md#configuration-options)

## 2020.44

> Version 2020.44 brings **new documentation** for [WebJET JavaScript functions](developer/frameworks/webjetjs.md), [Automated datatable testing](developer/testing/datatable.md), we add an example for [Dynamic change of values ​​in a selection field in the Datatables Editor](developer/datatables-editor/README.md#dynamic-change-of-values-in-a-selection-field) and other minor changes.
>
> We have ported the **Clearing Cache Objects** application to WebJET 2021 and completed the **Templates** application. It includes the selection of a JSP template file on the server with dynamic loading based on the selected template group and setting directories for displaying the template. We have also added [automated tests](../src/test/webapp/tests/components/templates.js).
>
> We have visually fine-tuned the **Server Monitoring/Current Values** application (indents, mobile display) and adjusted the live data in the graphs. They now scroll correctly in real time.
>
> We added a [confirmation of the performed action](developer/datatables/README.md#server-action-button) option to the datatable for special buttons in the datatable (such as delete all).

- [VS Code] modified [tasks.json](../.vscode/tasks.json) and [launch.json](../.vscode/launch.json) by adding debug configuration ```Debug```. This starts the application server and then connects in debug mode. Unfortunately, due to [bug in gretty](https://github.com/gretty-gradle-plugin/gretty/issues/166), shutdown is handled by ```killnutím``` process (not yet tested on windows). In VS Code, you can run debug configuration "Debug" directly in the Run tab.
- [Layout] Visually fixed the display of the domain selection in the upper right part
- #47293 [toastr] Refactored code using toastr directly for API calls [WJ.notify](developer/frameworks/webjetjs.md#notifications)

**Datatables:**

- #47293 [Datatable] Modified function ```TABLE.executeAction``` for the ability to confirm an action before execution
- #45718 [Datatable Editor] Disabled sending the editor after pressing Enter by setting the configuration ```onReturn: false```. This caused a problem in the select field filter, where after pressing Enter the value was set and the form was submitted.
- #47293 [Datatables] Modified code [DatatableRestControllerV2.action](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) to allow the action to be triggered even for an unspecified entity (null value). Used for actions of the "Delete All" type

**Deleting Cache Objects:**

- #47293 Redesigned version from WebJET 8 to datatable. Cache objects are not Spring DATA repository. A custom [service](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) is implemented, which provides API for accessing the Cache class and list of objects.
- #47293 In the [CacheObjectsService](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) class, ```deleteAllCacheBeans()``` methods are implemented for clearing the entire cache, including DocDB, GroupsDB and other objects. The ```deletePictureCache()``` method implements clearing the ```/thumb``` image cache.

**Templates:**

- #45718 [Templates] added [REST service /forward](../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java) for selecting JSP template from list of files in ```/templates``` directory
- #45718 [Templates] added option to set directories in which templates should be displayed

**Testing:**

- [Testing] Refactored the automatic test code [DataTables.js](../src/test/webapp/pages/DataTables.js) from a parametric constructor to an ```options``` object. Added the ability to add test steps for a new record, editing, searching and deleting records. Adjusted waiting from fixed ```I.wait(1)``` to ```DTE.waitForLoader()```, added ```I.say``` for better orientation in the test log. Search test added verification of found records.
- [Testing] Added template ```default``` for testing option of selecting JSP template files

**Documentation:**

- [Datatable] Added documentation for [server action button](developer/datatables/README.md#server-action-button) (e.g. image rotation, deleting all records)
- Added documentation for using [Gitlab in VS Code](developer/guildelines/gitlab.md#working-in-vs-code)
- Added documentation for [WebJET JavaScript functions](developer/frameworks/webjetjs.md) for displaying notifications, confirming actions, etc. Refactored their code for better usability.
- Added list of [special functions](developer/testing/README.md) for datatable testing
- Added description of usage and implementation of [Automated Datatable Testing](developer/testing/datatable.md)
- #45718 added instructions for [dynamically changing values ​​in select box](developer/datatables-editor/README.md#dynamically-changing-values-in-select-box) in Datatable Editor based on other fields

## 2020.43

- [Layout] Modified breadcrumb navigation generation - the original elements consisted of the name ```pills-#tab[1]```, the # character is now removed from the text, so it is now generated as ```pills-tab[0]```
- #47419 [Server Monitoring] - modified visual display (indents), redesigned way of displaying lines in the Free Disk Space graph (alignment between values ​​in GB and graph positions)
- #47419 [Server Monitoring] - increased time displayed on the graph of current values ​​to 10 minutes, added descriptions, moved interval setting to header, moved translations to ```properties``` file

## 2020.42

> Version 2020.42 brings in particular [automatic way of auditing JPA entities](developer/backend/auditing.md). With a simple annotation you can ensure the creation of audit records for each manipulation with the entity. The audit record also contains a list of changed attributes (in the form old value -> new value), or a list of all attributes when creating and deleting an entity.
>
> We have expanded and documented the JSON field capabilities for [adding directories and websites](developer/datatables-editor/field-json.md) in datatables.
>
> We added the ability to set a [default sorting method] for datatables (developer/datatables/README.md#sorting) and added the ability to display seconds for datetime fields.
>
> We added the ```oshi-core``` library to WebJET for obtaining CPU load and amcharts for displaying graphs in Server Monitoring.
>
> We have expanded the documentation with a section describing the [Spring framework](developer/frameworks/spring.md), added a [troubleshooting](developer/troubles/README.md) section, edited [examples](developer/frameworks/example.md) and supplemented the documentation for [testing](developer/testing/README.md).

- #46891 [Testing] - modified existing tests for new table properties and settings, in ```audit-search.js``` modified fixed waiting for dynamic using ```DT.waitForLoader```, in ```forms.js``` redesigned testing of search by dates
- #46891 [Datatable] - added button to reload (refresh) table data from the server
- #46891 [Audit] - added display of entity ID column
- #46891 [Translation keys] - fixed search by change date
- #46891 [Auditing] - JPA entity auditing enabled ```FormsEntity, GalleryDimension, GallleryEntity, InsertScriptBean, InsertScriptDocBean, InsertScriptGroupBean, TranslationkeyEntity, AuditNotifyEntity```
- #46891 [Auditing] - renamed table ```webjet_audit``` to ```webjet_adminlog``` according to the original name in Oracle
- #46891 [Testing] - added [codeceptjs-chai extension](developer/testing/README.md#assert-library) for easy writing of ```assert``` conditions
- #46891 [Documentation] - added documentation for [auditing](developer/backend/auditing.md), [troubleshooting](developer/troubles/README.md) and added information for [testing](developer/testing/README.md#webjet-additional-functions)
- adjusted settings in ```build.gradle```, ```options.encoding=utf-8```, corrected diacritics in ```PathFilter```
- #46891 [Testing] - added audit log testing to standard call ```DataTables.baseTest('domainRedirectTable');```
- #46891 [Datatable] - added option to set [arrangement method](developer/datatables/README.md#arrangement) when displaying the page, set arrangement for configuration (by change date),
- #46891 [Datatable] - added seconds to date and time formatting
- #46891 [Audit] - added datatable export auditing

## 2020.41

- [Documentation] documentation modified [Sample code](developer/frameworks/example.md) - PUG modified to use annotations, examples added
- [Documentation] added basic information about [EclipseLink JPA, Spring DATA and Spring REST](developer/frameworks/spring.md)

## 2020.40

- #47419 [Server Monitoring] module displays current server monitoring values, including CPU and memory graphs
- #47419 [Server Monitoring] added [oshi-core](https://github.com/oshi/oshi) library to get CPU load data
- #47419 [Server Monitoring] added [amcharts](https://www.amcharts.com/) library for displaying charts, commercial license used
- #46261 [Scripts] module added with the option to set a directory or web page for the script
- #46261 [DTED json field] refactored code of [JSON data field for DTED](developer/datatables-editor/field-json.md). Added option to convert JSON objects and button text, created documentation.
- #47293 [Datatable] added option to display custom button only when a row is selected using code ```$.fn.dataTable.Buttons.showIfRowSelected(this, dt);```, [documentation](developer/datatables/README.md)
- #47293 [Datatable] added support for ```range``` also for numeric values, prefix is ​​```range:```, works similarly to prefix ```daterange:```
- #47293 [Persistent cache] displaying records via datatable, Spring DATA

## 2020.39

> In this release, we added options to data tables to **refresh the table** after calling a REST service (e.g. in web pages when the page is moved to a different directory), the ```fetchOnEdit``` option to **refresh the edited record** before displaying the editor, and ```fetchOnCreate``` to **get data for a new record** (e.g. set a template, sort order for a web page).
>
> We have added the ability to **hide a column** in the table or editor using the ```hidden``` or ```hiddenEditor``` parameters to Java annotations.
>
> We also added **JSON object editing**, for now it is connected to directory editing and directory listing (e.g. for a web page as ```GroupDetails``` objects). It is implemented as a VUE component.

- #47341 - everything below applies to branch/ticket #47341
- [Web pages] Functional setting of directory and copy of page in directories. Solved using VUE component [vue-folder-tree.vue](../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue). JSTree opened in full screen dialog is used. In annotation ```@DatatableColumn``` it is written as ```DataTableColumnType.JSON``` where using attribute ```className = "dt-tree-group"``` it is set to select one directory or using ```className = "dt-tree-group-array"``` to select multiple directories. Objects are of type ```GroupDetails```.
- [Web pages] - solved moving a page to another directory with invoking a refresh of the data table and the tree structure of the pages
- [Websites] - added deletion of web pages
- [Datatables] Added option to [update datatables](developer/datatables/restcontroller.md#reload-datatables) after saving data (e.g. when the website is moved to another directory). Added method ```setForceReload(boolean)``` to REST controller and JavaScript event ```WJ.DTE.forceReload```.
- [Datatables] Added the option to not display a column in the datatable or in the editor using the ```hidden``` parameter (for not displaying in the datatable) or ```hiddenEditor``` parameter (for not displaying in the editor) of the [@DatatableColumn](developer/datatables-editor/datatable-columns.md#vlasnosti-datatablecolumn) annotation. Unlike the ```visible``` parameter, such columns cannot be displayed even through the datatable settings on the frontend.
- [Datatables] Added initialization parameter ```fetchOnEdit``` - ​​after setting to true, a REST call will be made before editing the record to obtain the current data of the edited record. When using a datatable, e.g. for a web page, the given record is updated from the server before opening the editor and the latest version is always opened in the editor. Implemented via JS function ```refreshRow``` and customer button ```$.fn.dataTable.ext.buttons.editRefresh``` which replaces the standard button ```edit```.
- [Datatables] Added initialization parameter ```fetchOnCreate``` - ​​after setting to ```true```, a REST call with value -1 will be made before creating a new record to get the data of the new object. This is useful for setting default data for a new record. For example, for a web page, the directory, sorting order, template, etc. will be set beforehand.
- [Datatables] Added automatic display of record ID in the first column, where previously there were only selection fields to mark the row
- [Datatables] Adjusted the display of the error message from the server so that it is not between the Cancel and Save buttons but to the left of them.

- [Testing] added function ```DT.waitForLoader```, which waits for displaying and then hiding the "Processing" information in the data table. Used as ```DT.waitForLoader("#forms-list_processing");```

## 2020.36

- #44968 [Translation keys] Modified saving of translation after language change - a copy of the translation key is created, added saving of the last change date, fixed tests
- #44968 [Datatables] Added option to not display a column in a datatable using the ```hidden=true``` annotation, such a column, unlike ```visible```, cannot be displayed in the datatable column settings. However, it can still be used in the editor if it has an annotation for the editor.

## 2020.34

- #47008 [Datatables] Added automatic generation of ```required``` attribute in ```columns``` definition if field has annotation ```@NotEmpty``` or ```@NotBlank```. Implemented in [DatatableColumnEditor.java](../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumnEditor.java).
- #47008 [Automated Testing] Added [automated test](../src/test/webapp/pages/DataTables.js) of datatables with reading definition directly from ```columns``` object. It implements standard CRUD test. Sample usage in [redirects.js](../src/test/webapp/tests/components/redirects.js):

```javascript
const requiredFields = ['oldUrl', 'newUrl'];
DataTables.baseTest('redirectTable', requiredFields);
```

## 2020.33

- #47341 [Datatables] - Added new JSON field type, currently displayed as ```textarea``` but when reading and writing data is sent correctly as JSON objects. Implemented as new field type json in index.js in code ```$.fn.dataTable.Editor.fieldTypes.json```
- #47425 [Datatables] - Fixed bulk row marking and delete call on server
- #44890 [Tests] - [added instructions](developer/testing/README.md#waiting-for-completion) to use ```I.waitFor*``` instead of the fixed ```I.wait```. Modified test [webpages.js](../src/test/webapp/tests/components/webpages.js) to use ```waitFor``` and ```within```, since the page contains two datatables.
- #44890 [Datatables Editor] - Added CSS class transfer from ```className``` annotation attribute to entire row in div.DTE_Field editor. See ```//prenesieme hodnotu className aj do DT editora do prislusneho riadku``` comment in [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js).
- #44890 [Datatables] - Fixed comparison of ```true/false``` values ​​for select field. Similar to checkbox field, non-string value ```true``` must be compared (see ```ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot``` comment in [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).

## 2020.32

- #44890 [Datatables] - Modified functionality ```BOOLEAN```, ```dt-format-boolean-true```, in JSON object the value is transmitted as ```true``` but in ```options``` object as ```"true"``` (string). So comparison and setting of value did not work. Modified code so that ```editor.options``` is changed from ```"true" na true``` value. Subsequently, comparison with object and correct setting of ```checkboxu``` now works. Also modified filtering in client paging, where label is not compared but value value (see comment ```//neplati pre column-type-boolean``` in [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).
- #44890 [Datatables] - Added transfer of data attributes to fields of type ```checkbox``` and ```radio``` (condition ```"div.DTE_Field_Type_"+data.editor.type```). They are set to the first element in the list. They allow you to set attributes like ```data-dt-field-hr``` or ```data-dt-field-headline```.
- #44890 [Date and Time] - Modified behavior of value 0 in date in primitive type format ```long```. For value 0, empty date and time will be displayed instead of 1.1.1970. Modified in ```renderDate``` in [datatables-config.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js).
- #44890 [Datatables] - Modified generation of ```Options``` fields for the editor using the [LabelValue](../src/main/java/sk/iway/iwcm/system/datatable/json/LabelValue.java) object.

## 2020.31

- #44890 [Datatables] - Added the option in the annotation to set the order of fields in the output JSON object using the ```sortAfter``` attribute in the ```@DatatableColumn``` annotation. Sorting is performed in the [DatatableColumnsFactory.java](../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) class in the ```sortColumns``` method.
- #44890 [Datatables] - Added annotation options ```DataTableColumnType.BOOLEAN``` for true/false values ​​and ```DataTableColumnType.CHECKBOX``` for standard checkboxes.
- #44890 [Datatables] - Added the option to annotate [nested objects](developer/datatables-editor/datatable-columns.md#nested-attributes) using ```@DatatableColumnNested```. The prefix attribute (default value is auto, which generates a prefix based on the name of the annotated field) allows you to specify the prefix of the attribute in the generated JSON object (e.g., by setting it to the value ```editorFields```, the field ```allowChangeUrl``` will be generated in JSON as ```editorFields.allowChangeUrl```). The prefix can also be empty, in which case only the variable name will be generated (if it has ```gettery/settery``` in the original object, such as [FieldsFromAtoE](../src/main/java/sk/iway/iwcm/doc/FieldsFromAtoE.java) in the [DocDetails](../src/main/java/sk/iway/iwcm/doc/DocDetails.java) object.)

## 2020.30

- #44890 [Datatables] - Fixed inserting multiple datatables into a web page. Each table is set to an HTML ID attribute even if it is not in ```options``` (default datatableInit). The ID attribute is then also inserted into other selectors for correct mapping of filters and buttons to the correct datatable. Modified functions ```$.fn.dataTable.Editor.display.bootstrap.init``` and ```$.fn.dataTable.Editor.display.bootstrap.open``` to support multiple DT editors in a page.

## 2020.29

- #346264 [Websites] Added ability to add/edit directories in the tree structure of web pages. Implemented using a hidden data table and DT editor.
- #45763 [Tree structure] Added API for [editing tree structure](developer/jstree/README.md), used in the website and photo gallery sections. It is now possible to move items in the website directory structure and move directories in the gallery.
- #44836 [Websites] Added fields to DT editor, backend is not processing them yet
- #47125 [Background Tasks] Converted to datatables, added a column with the task name to the database

## 2020.28

- #45532 [Forms] Added automated searches
- #45409 [Audit] Added filtering tests
- [Datatables] for checkbox automatically set attribute ```falseValue``` to no (if not specified)
- Fixed diacritic settings for JS files from the ```/admin/v9/dist/``` directory (outputted in UTF-8)

## 2020.27

- #46264 [standalone datatables editor] Added option to mark datatable as hidden ([hideTable attribute in options](developer/datatables/README.md)) for option ```standalone``` DT editor for jstree
- #45721 [Template groups] Modified display in datatable for using annotations, added test

## 2020.26

- #43588 [Datatables] Fixed repeated setting of select box in filter and editor (after the second setting, values ​​could not be selected)
- #45532 [Forms] Added basic search option in form data, limited to a maximum of 6 columns (due to Spring DATA API)
- #45718 [Templates] Version of template list via datatable, uses ```TemplatesDB``` API
- #45490 [Audit] View audit records and notifications. Both via Spring DATA, for audit added select boxes for filtering in the datatable (connected to the options editor), added [custom filtering] option (./datatables/restcontroller.md#special-search) in the REST controller.
- #44968 [Translation keys] View and edit translation keys, implemented reading of original values, mapping of ```.properties``` and DB changes.

## 2020.25

- #45532 [Forms] Displaying a list of forms and form data (dynamic data) via a datatable.
- #45679 [Datatables] Annotations for generating ```columns``` configuration ported to WebJET 8 for the ability to annotate existing beans
- #45382 [Datatables] Import and export data - data import dialog adjustment

## 2020.23

- #45685 [CI-CD] Configured CI-CD pipeline on Gitlab server, nightly build from master branch is available at http://demotest.webjetcms.sk/admin/
- #45382 [Datatables] Initial version of data import in the form of gradually sending data from the import excel to the datatables editor, possibility of setting the import mode (backend support is still missing)
- #45379 [Datatables] Refactored code for datatable customization from ```app.js``` to a separate file [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) with the option to also use it as an [NPM module](../src/main/webapp/admin/v9/npm_packages/README.md) (e.g. on the frontend in a template). Refactored code for the ability to use multiple datatables on one page.
- #45679 [Datatables] Added option to [generate columns configuration](developer/datatables-editor/datatable-columns.md) datatable using Java bean annotations

## 2020.22

- #44836 [Websites] Migrated editing code in ckeditor from WJ8. Image insertion, forms and other CK editor dialogs have been modified. Added hidden form containing ```docid, groupid, virtualpath``` for compatibility of application preview and dialogs from WJ8.
- #44836 [Websites] For the editor, the ```options``` template object is sent in ```original```, which is needed to get paths to CSS styles. When opening the DT editor, CSS starting with /admin (for the CK editor extension) is preserved, the rest are changed based on the assigned template.
- #44836 [Website] DT editor has a directly available [JSON object](./datatables-editor/README.md#code-samples) of the edited record (not all data). For ```options``` code lists, the option to send [original object](./datatables/restcontroller.md) has been added. The JSON object obtained from the server (containing complete data of all rows and options property) is available in DT as DATA.json.
- #44737 [Configuration] Added [autotest](../src/test/webapp/tests/components/configuration.js).
- #44293 [Domain Redirects] Added [autotest](../src/test/webapp/tests/components/domain-redirects.js).

## 2020.21

- [Testing] Added CSS classes ```dt-filter-MENO``` to filter fields in datatable for filtering options in automated test ```I.fillField("input.dt-filter-newUrl", randomNumber);```. Example in test [redirects.js](../src/test/webapp/tests/components/redirects.js)
- [Redirects] Added [mandatory field annotations](./datatables/restcontroller.md#valid%c3%a1cia--povinn%c3%a9-polia) to UrlRedirectBean.java (in WJ8)
- [Gallery] Added text translations for the image editor and area of ​​interest, added the option to set the area of ​​interest to input fields, added [auto test](../src/test/webapp/tests/components/gallery.js)
- [Documentation] Added [example code](developer/frameworks/example.md) for using frameworks
- [Testing] Added [report generation](./testing/README.md#generate-html-report) via mochawesome and CodeceptUI for [test control](./testing/README.md#codecept-ui) via browser
- [Datatable] Added the option to display data (columns and fields in the editor) [based on the rights](./datatables/README.md#display-data-based-on-the-rights) of the logged-in user

## 2020.20

- [Datatable] Added [search by date range](./datatables/restcontroller.md#vyh%c4%bead%c3%a1vanie-pod%c4%bea-rozsahu-d%c3%a1tumov) by specific processing of URL parameter with prefix ```daterange:od-do```.
- [Documentation] Completed manual for using the [assert](./testing/README.md#assert-library) library, using and writing new [translation keys](./frameworks/thymeleaf.md#translation-text)
- [Gallery] Adjusted the display of image language mutation columns to use the datatables API (so that inline editing also works correctly)
- [Translation keys] New version of viewing and editing [translation keys](../src/main/webapp/admin/v9/views/pages/settings/translation-keys.pug)
- [Datatable] Refactored code [DatatableRestControllerV2.java](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) with commented division into entity and rest methods
- [Configuration] New version of the [configuration] view (../src/main/webapp/admin/v9/views/pages/settings/configuration.pug) of WebJET
- [Documentation] Supplemented documentation for [SFC Vue](developer/frameworks/vue.md)
- [Photo Gallery] Added connection of [image editor](../src/main/webapp/admin/v9/src/js/image-editor.js) to ```chunked upload``` and toast notifications
- [Redirects] New version of the [redirects] view (../src/main/webapp/admin/v9/views/pages/settings/redirect.pug)
- [Photo Gallery] Changed JCrop plugin for marking area of ​​interest to ```vue-advanced-cropper```

## 2020.19

- [Testing] Basic concept of [automated testing](developer/testing/README.md)
- [Documentation] Extension [documentation](README.md)
- [Web pages] Initial version of [ckeditor integration into datatables](../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) and display of [web pages](../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug)
- [Datatable] Fixed issue with date format, automatic setting according to ```window.userLang``` on moment library - setting ```wireformat``` (default x) and attribute format in editor (default L HH:mm)
- [Gallery] Added option to add and [edit directory](../src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug), connected to Vue

## 2020.18

- [Domain Redirects] New version of the [domain redirects] view (../src/main/webapp/admin/v9/views/pages/settings/domain-redirect.pug)
- [Security] The security of calling all rest services is ensured by [automatically setting](../src/main/webapp/admin/v9/views/partials/head.pug) CSRF token for all ajax requests
- [Upload] Fixed call for correct file processing after uploading and API for file deletion (gallery cleaning, video previews, fulltext index)
- [Gallery] Added [image editor](../src/main/webapp/admin/v9/src/js/image-editor.js)
- [Websites] Basic implementation of displaying [tree structure](../src/main/java/sk/iway/iwcm/admin/layout/JsTreeItem.java) pages

## 2020.13

[#43588 - wj9 - basic layout functionality] - task #5 =>

- modified lombook configuration (in build.gradle - ```config['lombok.accessors.chain'] = 'true'```) so that ```settre``` returns ```this``` for chaining
- webpack set to not remove quotes in attributes (because TH can return a more verbose expression - ```removeAttributeQuotes: false```)
- MENU - added MenuBean and MenuService objects that generate the menu, translation of the original ModuleInfo from the ```Modules``` object (groups, icons) + edited texts via ```text-webjet9.properties```

## 2020.12

[#43588 - wj9 - basic layout functionality] - task #1 =>
I committed the first version, it works:

- click on the logo + ```tooltip``` with the version (TODO: I would like to write this out better, TODO: add logos for NET and LMS)
- domain switch (there are also illustrative data for ```iwcm.interway.sk```), TODO: it's broken, ```@MHO``` needs to be styled
- assistant
- name of the logged in user
- logout link
- TODO: remaining icons in the header

PLUS:

- lombok enabled on the project - https://projectlombok.org
- fixed base classes ```LayoutBean``` and ```HeaderBean``` with their insertion into the model
- supplemented ```webjet.js``` with JS functions WJ.xxx
- basic skeleton for text translation, added ```WJ.translate```, but it must be called as ```async```, which slows down the display.

I suggest in the future to pass translation keys from the HTML code as a parameter of a JS function (in the pug file, the translation is simply inserted as ```#{prekladovy.kluc}``` and it is translated on the server.