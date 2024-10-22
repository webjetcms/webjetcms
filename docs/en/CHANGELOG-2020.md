# Changelog version 2020

## 2020.53

> 2020.53 brings compatibility for use in client projects. The package is available on `artifactory/maven` server as version `2021.0-SNAPSHOT`. When you first run it, it adds database columns to some tables, but the change is backward compatible and you can still revert to WebJET 8.8.

In client projects, just set the appropriate version in `build.gradle`:

```gradle
ext {
    webjetVersion = "2021.0-SNAPSHOT";
}
```

we have experimentally verified the basic functionality on projects with MariaDB, Microsoft SQL and Oracle DB.

**Build Artifactory/Maven versions**
#52318 [Build file](../ant/build.xml) contains several `task` elements, the final one is `deploy`that has the correct dependencies set, so just run that one. List `taskov`:
- `setup` - recovers dependencies and generates `WAR` archive
- `updatezip` - prepare the temporary structure in `build/updatezip` directory. The structure contains an unpacked `WAR` archive, unpacked `webjet-XXXX.jar` files (i.e. the complete structure of the /admin, /components and /WEB-INF/classes directories)
- `preparesrc` - withdraws `SRC` jar file and prepares the structure for the jar archive with source files (linked from the jar archive and WebJET 2021 source code)
- `define-artifact-properties` - defines properties for generating artifacts, here in `artifact.version` sets the version of the generated artifact
- `makejars` - prepares jar archives of classes, /admin and /components directories and source files
- `download` - auxiliary task download `pom` files and jar archives that are not modified (`struts, daisydiff, jtidy, swagger`)
- `deploy` - the actual deposition of artifacts on the `artrifactory` server, contains the dependency definition (generating `POM` file)

The procedure for generating a new version:

```shell
cd ant
ant deploy
```

*Note*: in directory `build/updatezip` an unzipped structure is created, which can be zipped and used as an update package for WebJET in the old structure (not using spring archives).

**JPA and Spring initialization**

JPA and Spring initialization is moved to package `sk.iway.webjet.v9`. For backward compatibility reasons we had to solve the problem with naming of database tables `_adminlog_` a `_properties_` in Oracle DB. There is used the name `webjet_adminlog` a `webjet_properties`. But the table name is directly in the JPA entities. We have taken the opportunity to use the interface `SessionCustomizer`, which is implemented in the class [JpaSessionCustomizer](../src/main/java/sk/iway/webjet/v9/JpaSessionCustomizer.java). It traverses all found JPA entities during JPA initialization and changes the annotation value for the specified database tables in case of Oracle installation.

For JPA, we have added the correct database type setting (the previous version only had the MariaDB option set).

**Home screen**
#47428 Sliced splash screen design, filled with data. So far, data is entered directly in [ThymeleafAdminController](../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java), it will be read by ajax later after finalizing the properties.

The screen is cut using VUE components. New DTO objects are used for `DocDetails, Audit a User beany`.

JavaScript object is available `window.currentUser`. Added VUE filters for date and time formatting - `formatDate,formatDateTime,formatDateTimeSeconds,formatTime`.

**Other changes**

- [Scripts] corrected audit record type from `CLIENT_SPECIFIC` to correct `INSERT_SCRIPT`, added method `toString()` on doc and group beans to better record the selected restriction on directories/pages in the audit record (record ID and directory/page path are written).
- [WebJET] modified initialization to find SpringConfig in package `sk.iway.webjet.v9`, automatic search of translation files and autoupdate also in `text-webjetXX.properties` a `autoupdate-webjetXX.xml` (where webjetXX is the value from the configuration variable `defaultSkin`).
- `autoupdate.xml` - Improved compatibility with WebJET 8 to be able to run in the WebJET 2021 project while reverting back to WebJET 8.8 without impact. columns added `id,update_date` to the table `_properties_` and column `task_name` to the table `crontab`. The columns do not clash with the old version and do not prevent the use of the administration in version 8.8.

**Documentation**

- is deployed on http://docs.webjetcms.sk/v2021/, no side menu is generated yet, solved by gradle task `rsyncDocs`
- documentation for insertion completed [HTML code without escaping](developer/frameworks/thymeleaf.md#základny-výpis-textu--atribútu) in Thymeleaf
- supplemented documentation for [date and time formatting](developer/frameworks/vue.md#formátovanie-dátumu-a-času) in Vue

![meme](_media/meme/2020-53.jpg)

## 2020.51

**Server monitoring - recorded values**

New version of the server monitoring logged values display. Displays both table and graphs, using data aggregation by day range for the graphs:
- 0-5 = exact value without aggregation
- 5-10 = aggregation to 10 minute interval
- 10-14 = aggregation to 30 minute interval
- 14-30 = aggregation per hour
- 30-60 = aggregation for 4 hours
- 60+ = aggregation for 12 hours
If aggregation was not performed, the browser would likely fall on the amount of data in the graph. The implementation is in the class [MonitoringAggregator](../src/main/java/sk/iway/iwcm/components/monitoring/MonitoringAggregator.java), the highest value in a given time interval is recorded.

- #50053 [Charts] - added basic documentation for the library [amcharts](developer/frameworks/amcharts.md).
- #50053 [Charts] - v `app.js` added asynchronous initialization of the amcharts library by calling `window.initAmcharts()`.

![meme](_media/meme/2020-51.jpg)

## 2020.50

> 2020.50 introduces correct generation of menu items of administration according to the availability of the new version of the admin page. In the menu you can switch seamlessly between version 8 and 9. If necessary, you can switch to the original version of the admin page.
>
> The page editor has also been improved, it automatically adjusts its height according to the size of the window. Icons for directory and web page statuses in a tree structure have also been added.

**Website**

- Deleted the original sample page and directory editing code, only datatable views are used anymore.
- Added status icons in the tree structure according to the original design for the directory (internal, not displayed in the menu, passworded) and the web page (display disabled, redirected, unsearchable, passworded).
- The editor dialog has an automatic calculated height according to the size of the window (but at least 300 pixels). The height is recalculated every 3 seconds.
- Added clickable web page in tree structure. This is quite complicated since the editor is integrated with the datatable. So first it is checked if the datatable shows the directory in which the page is located. If not, the value is discarded `docId` into the variable `docidAfterLoad`. This is checked after the list of pages in the directory is loaded, if it is set, editing of the page is automatically triggered.

**Generating menus**

- #51997 [Navigation] Modified menu item generation. In both v8 and v9, links to pages already redesigned in v9 are generated, so you can transparently navigate between the old and new WebJET. When requesting to view the old v8 version, it is possible to enter the following in the URL `/admin/v8` which switches the menu generation to the old version and it is possible to view the old version of the administration. You can get to v9 by calling the URL `/admin/v9`. There is also an icon at the top of the page to switch to v8.

**Documentation**

Supplemented documentation for use [jsTree](developer/jstree/README.md).

## 2020.49

**Import/Export:**
Implemented a system for importing and exporting data between datatables.

Export:
- Allows export to **Excel(xlsx), PDF and direct print to printer**
- The file is named according to the current `title` page and the current date and time will be automatically added.
- The export can be set to the data type (**current page/all, filtered/all/marked rows, sorting, columns**).
- In server paging, a call is first made to the REST service from which the **maximum 50 000 records**. If you need to export multiple records, use multiple export using filtering.
- Server data is processed outside the datatable (for performance reasons), it is re-rendered `datetime` columns and `radio button` (supported are `date-time`, `date`, `*-select`, `*-boolean`).
- When exporting, a list of columns is prepared in the first row, **the import is consequently not sensitive to the order of the columns**.
- For **selection fields** (select/digits) with **export text value** and reconstructs back to the ID on import. This allows to have **different IDs of bound records** between environments (e.g. template ID for a web page), if the name matches, the record is correctly paired. Consequently, the export also includes human readable text in place of the ID.

Import:
- Enables **import data as new** (to be added to the database) or **match existing data according to the selected column** (e.g. name, URL, etc.). When matching, it will automatically look up the record in the database first and then update it. If it does not exist, it creates a new record.
- **Imported from xlsx format**.
- Import is carried out **successively in batches of 25 records**so as not to overload the server.
**Datatables:**
- Modified filtering by dates - added option to set the time to the UI, no need to set the end of day time on the backend anymore

- Added attributes (we need them for calling the REST service to export data):
	- `DATA.jsonOptions` - settings from the REST service response for rendering (we need for export of codebook data)
	- `DATA.urlLatest` - complete URL of the last REST call
	- `DATA.urlLatestParams` - all parameters of the last REST call (current page, page size, filters)
## 2020.45

- #47293 [Deleting records in database] Remastered version of deleting data from WJ8 to datatable. Uses a few hacks, like setting [external filter](developer/datatables/README.md#externý-filter), disable paging (paging option)
**Datatables:**
- Added option **do not show filter** in the table header for the column by setting the attribute `filter=false` in the annotation
- Added option **disable paging** via `options paging: false`, [Documentation](developer/datatables/README.md#možnosti-konfigurácie)

## 2020.44

> Version 2020.44 brings **new documentation** k [WebJET JavaScript functions](developer/frameworks/webjetjs.md), [Automated testing of the datatable](developer/testing/datatable.md), we add an example for [Dynamic change of values in the selection field in Datatables Editor](developer/datatables-editor/README.md#dynamická-zmena-hodnôt-vo-výberovom-poli) and other minor changes.
>
> We have ported the application to WebJET 2021 **Deleting cache objects** and we have completed the application **Templates**. It adds JSP template file selection on the server with dynamic loading based on the selected template group and directory settings for template display. We also added [automated tests](../src/test/webapp/tests/components/templates.js).
>
> Application **Server monitoring/actual values** we have visually fine-tuned (indentations, mobile version display) and edited the live data in the charts. They now scroll correctly in real time.
>
> We have added an option to the datatable [confirmation of the action taken](developer/datatables/README.md#tlačidlo-pre-vykonanie-serverovej-akcie) for special buttons in the datatable (such as delete all).

- [VS Code] modified [tasks.json](../.vscode/tasks.json) a [launch.json](../.vscode/launch.json) adding debug configuration `Debug`. It starts the application server and then connects in debug mode. Unfortunately due to [error in gretty](https://github.com/gretty-gradle-plugin/gretty/issues/166) the shutdown is solved `killnutím` process (not yet tested on windows). So in VS Code you can run the "Debug" configuration directly in the Run tab.
- [Layout] Visually corrected display of domain selection in the upper right part
- #47293 [toastr] Refactored code using toastr directly for API calls [WJ.notify](developer/frameworks/webjetjs.md#notifikácie)
**Datatables:**
- #47293 [Datatable] Modified function `TABLE.executeAction` for the possibility to confirm the action before execution
- #45718 [Datatable Editor] Disabled sending editor after pressing Enter by configuration setting `onReturn: false`. It was causing a problem in the selection field filter, where pressing Enter set the value and also submitted the form.
- #47293 [Datatables] Modified code [DatatableRestControllerV2.action](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) for the possibility of triggering the action even for an unentered entity (null value). Used for actions of type "Delete All"
**Deleting Cache Objects:**
- #47293 Converted version from WebJET 8 to datatable. Cache objects but not Spring DATA repository. Implemented is a custom [service](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java), which provides an API for accessing the Cache class and object list.
- #47293 In class [CacheObjectsService](../src/main/java/sk/iway/iwcm/components/memory_cleanup/cache_objects/CacheObjectsService.java) methods are implemented `deleteAllCacheBeans()` to delete the entire cache including DocDB, GroupsDB and other objects. Method `deletePictureCache()` implements cache clearing `/thumb` Images.
**Templates:**
- #45718 [Templates] added [REST service /forward](../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java) to select a JSP template from the list of files in `/templates` directory
- #45718 [Templates] added option to set directories where templates should be displayed
**Testing:**
- [Testing] Refactored auto test code [DataTables.js](../src/test/webapp/pages/DataTables.js) from parametric constructor to `options` object. Added ability to add test steps for new record, edit, search and delete records. Modified wait from fixed `I.wait(1)` at `DTE.waitForLoader()`, supplemented `I.say` for better orientation in the test logo. Search test supplemented with verification of found records.
- [Testing] Added template `default` for the ability to test a selection of JSP template files
**Documentation:**
- [Datatable] Supplemented documentation for [button to perform a server action](developer/datatables/README.md#tlačidlo-pre-vykonanie-serverovej-akcie) (e.g. rotate image, delete all records)
- Supplemented documentation for use [Gitlabu in VS Code](developer/guildelines/gitlab.md#práca-vo-vs-code)
- Added documentation [WebJET JavaScript functions](developer/frameworks/webjetjs.md) to view notifications, action confirmations and more. Refactored their code for better use.
- Supplemented list [special features](developer/testing/README.md) for testing datatables
- Added description of use and implementation [Automated datatable testing](developer/testing/datatable.md)
- #45718 added instructions for [dynamic change of values in select box](developer/datatables-editor/README.md#dynamická-zmena-hodnôt-vo-výberovom-poli) in Datatable Editor by other fields

## 2020.43

- [Layout] Modified generation of breadcrumb navigation - original elements consisted of title `pills-#tab[1]`, the # character is now removed from the text, so it is now generated as `pills-tab[0]`
- #47419 [Server Monitoring] - modified visual display (offsets), redesigned way of displaying lines in the Free Disk Space graph (alignment between GB values and graph positions)
- #47419 [Server Monitoring] - increased time displayed on graph of current values to 10 minutes, added descriptions, moved interval setting to header, moved translations to `properties` file

## 2020.42

> Version 2020.42 introduces in particular [automatic auditing of JPA entities](developer/backend/auditing.md). A simple annotation ensures that audit trails are created for each entity manipulation. The audit record also contains a list of changed attributes (in the form old value -> new value), or a list of all attributes when the entity is created and deleted.
>
> We have extended and documented the JSON array capabilities for [adding directories and web pages](developer/datatables-editor/field-json.md) in datatables.
>
> We have added the ability to set the data tables [the default arrangement method](developer/datatables/README.md#usporiadanie) and date-time fields, we also added the display of seconds.
>
> We have added a library to WebJET `oshi-core` to get the CPU load and amcharts to display graphs in Server Monitoring.
>
> We have expanded the documentation to include a section describing [Spring framework](developer/frameworks/spring.md), we have added a section [problem solving](developer/troubles/README.md), edited [Examples](developer/frameworks/example.md) and completed the documentation for [testing](developer/testing/README.md).

- #46891 [Testing] - modified existing tests to new properties and table settings, in `audit-search.js` modified fixed waiting to dynamic using `DT.waitForLoader`, v `forms.js` redesigned date search testing
- #46891 [Datatable] - added button to reload (restore) table data from server
- #46891 [Audit] - added entity ID column display
- #46891 [Translation keys] - corrected search by change date
- #46891 [Auditing] - JPA entity auditing enabled `FormsEntity, GalleryDimension, GallleryEntity, InsertScriptBean, InsertScriptDocBean, InsertScriptGroupBean, TranslationkeyEntity, AuditNotifyEntity`
- #46891 [Auditing] - renamed table `webjet_audit` at `webjet_adminlog` by its original name in Oracle
- #46891 [Testing] - added [extension codeceptjs-chai](developer/testing/README.md#assert-knižnica) for easy writing `assert` conditions
- #46891 [Documentation] - added documentation for [auditing](developer/backend/auditing.md), [problem solving](developer/troubles/README.md) and supplemented by information on [testing](developer/testing/README.md#webjet-doplnkové-funkcie)
- adjusted settings in `build.gradle`, `options.encoding=utf-8`, corrected diacritics in `PathFilter`
- #46891 [Testing] - added audit trail testing to standard call `DataTables.baseTest('domainRedirectTable');`
- #46891 [Datatable] - added setting option [method of arrangement](developer/datatables/README.md#usporiadanie) when displaying the page, set the order for configuration (by the date of change),
- #46891 [Datatable] - added seconds to date and time formatting
- #46891 [Audit] - added auditing of datatable export

## 2020.41

- [Documentation] modified documentation [Sample code](developer/frameworks/example.md) - PUG adapted to use annotations, examples added
- [Documentation] supplemented with background information on [EclipseLink JPA, Spring DATA and Spring REST](developer/frameworks/spring.md)

## 2020.40

- #47419 [Server Monitoring] module displays current server monitoring values, including CPU and memory graphs
- #47419 [Server Monitoring] library added [oshi-core](https://github.com/oshi/oshi) to obtain CPU load data
- #47419 [Server Monitoring] library added [amcharts](https://www.amcharts.com/) for displaying graphs, commercial license is used
- #46261 [Scripts] module added the possibility to set a directory or web page for the script
- #46261 [DTED json field] refactored code [JSON data field for DTED](developer/datatables-editor/field-json.md). Added JSON object and button text conversion option, created documentation.
- #47293 [Datatable] added option to show custom button only when a row is selected using code `$.fn.dataTable.Buttons.showIfRowSelected(this, dt);`, [Documentation](developer/datatables/README.md)
- #47293 [Datatable] added support `range` even for numeric values, the prefix is `range:`, works similarly to a prefix `daterange:`
- #47293 [Persistent cache] view records via datatable, Spring DATA

## 2020.39

> In this release, we have added options to the datatables **table refreshes** after calling the REST service (e.g. in web pages when the page is moved to another directory), the option `fetchOnEdit` For **restoring an edited record** before displaying the editor and `fetchOnCreate` For **getting data for a new record** (e.g. for a web page set template, order of layout).
>
> We have added an option to Java annotations **hiding columns** in the table or in the editor using the parameters `hidden` or `hiddenEditor`.
>
> We have also added **editing JSON objects**, meanwhile, is linked to the directory editing and directory listing (e.g. for a web page like `GroupDetails` objects). It is implemented as a VUE component.

- #47341 - everything below is related to Branch/Tag #47341
- [Web pages] Functional setting of directory and page copy in directories. Solved by VUE component [vue-folder-tree.vue](../src/main/webapp/admin/v9/src/vue/components/webjet-dte-jstree/folder-tree/vue-folder-tree.vue). JSTree is used and opened into a full-screen dialog. In the annotation `@DatatableColumn` shall be entered as `DataTableColumnType.JSON` where using the attribute `className = "dt-tree-group"` is set to select a single directory or by `className = "dt-tree-group-array"` selecting multiple directories. Objects are of type `GroupDetails`.
- [Web pages] - resolved moving the page to another directory with restoring the datatable and page tree structure
- [Web pages] - deletion of web pages added
- [Datatables] Added option [datatable updates](developer/datatables/restcontroller.md#reload-datatabuľky) after the data has been saved (e.g. when a web page is moved to another directory). Added method `setForceReload(boolean)` to REST controller and JavaScript events `WJ.DTE.forceReload`.
- [Datatables] Added option not to display column in datatable or in editor by parameter `hidden` (for not displaying in the datatable) or `hiddenEditor` (not displayed in the editor) annotations [@DatatableColumn](developer/datatables-editor/datatable-columns.md#vlasnosti-datatablecolumn). In contrast to the parameter `visible` it is not possible to display such columns even via datatable settings on the frontend.
- [Datatables] Added initialization parameter `fetchOnEdit` - when set to true, a REST call will be made before editing the record to retrieve the current data of the record being edited. When using a datatable, for example for a web page, the record is updated from the server before the editor is opened, so the latest version is always opened in the editor. Implemented via JS function `refreshRow` and customer button `$.fn.dataTable.ext.buttons.editRefresh` to replace the standard button `edit`.
- [Datatables] Added initialization parameter `fetchOnCreate` - after setting to `true` a REST call with value -1 will be made to get the data of the new object before the new record is created. This is convenient for setting the default data for the new record. For example, for a web page, the directory, layout order, template, and so on will be set before the record.
- [Datatables] Added automatic display of the record ID in the first column, where previously there were only selection fields to mark the row
- [Datatables] Modified the display of the error message from the server so that it is not between the Cancel and Save buttons but to the left of them.
- [Testing] function added `DT.waitForLoader`which is waiting for the "Processing" information to be displayed and then hidden in the datatable. It is used as `DT.waitForLoader("#forms-list_processing");`

## 2020.36

- #44968 [Translation keys] Modified saving of translation after language change - copy of translation key is created, added saving of last change date, fixed tests
- #44968 [Datatables] Added option not to display column in datatable using annotation `hidden=true`, such a column as opposed to `visible` cannot be displayed even in the datatable column settings. However, it can still be used in the editor as long as it has an annotation for the editor.

## 2020.34

- #47008 [Datatables] Automatic generation added `required` of the attribute in `columns` definition if the field has an annotation `@NotEmpty` or `@NotBlank`. Implemented in [DatatableColumnEditor.java](../src/main/java/sk/iway/iwcm/system/datatable/json/DataTableColumnEditor.java).

- #47008 [Automated Testing] Added [automated test](../src/test/webapp/pages/DataTables.js) datatables with reading the definition directly from the `columns` object. It performs a standard CRUD test. Sample use in [redirects.js](../src/test/webapp/tests/components/redirects.js):
```javascript
const requiredFields = ["oldUrl", "newUrl"];
DataTables.baseTest("redirectTable", requiredFields);
```

## 2020.33

- #47341 [Datatables] - Added new JSON field type, so far it is displayed as `textarea` but when reading and writing, the data is sent correctly as JSON objects. Implemented as a new field type json in index.js in the code `$.fn.dataTable.Editor.fieldTypes.json`
- #47425 [Datatables] - Fixed bulk row marking and delete call on server
- #44890 [Tests] - [added instructions](developer/testing/README.md#čakanie-na-dokončenie) for use `I.waitFor*` instead of fixed `I.wait`. Modified test [webpages.js](../src/test/webapp/tests/components/webpages.js) for use `waitFor` a `within`, as the page contains two datatables.
- #44890 [Datatables Editor] - Added CSS class transfer from attribute `className` annotations on the whole line in the div.DTE\_Field editor. See comment `//prenesieme hodnotu className aj do DT editora do prislusneho riadku` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js).
- #44890 [Datatables] - Modified comparison `true/false` values for the selection field. Similarly to the checkbox, the non-string value must be compared `true` (see comment `ak mame true/false select box, aj tomu musime zmenit value na true/false namiesto String hodnot` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).

## 2020.32

- #44890 [Datatables] - Modified functionality `BOOLEAN`, `dt-format-boolean-true`, in the JSON object the value is transmitted as `true` but in `options` object as `"true"` (string). So the comparison and setting of the value did not work. Modified the code so that it `editor.options` will change from `"true" na true` Value. Then the comparison with the object and the correct setting works `checkboxu`. Modified also filtering in client paging, where value value is not compared to label (see comment `//neplati pre column-type-boolean` v [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js)).
- #44890 [Datatables] - Added transfer of data attributes also to fields of type `checkbox` a `radio` (condition `"div.DTE_Field_Type_"+data.editor.type`). They are set to the first element in the list. They allow you to set attributes such as `data-dt-field-hr` or `data-dt-field-headline`.
- #44890 [Date and Time] - Modified behaviour of the value 0 in the date format of the primitive type `long`. For a value of 0, a blank date and time will be displayed instead of 1.1.1970. Modified in `renderDate` v [datatables-config.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-config.js).
- #44890 [Datatables] - Modified generation `Options` fields for the editor using [LabelValue](../src/main/java/sk/iway/iwcm/system/datatable/json/LabelValue.java) object.

## 2020.31

- #44890 [Datatables] - Added option in annotation to set the arrangement of fields in the output JSON object using the attribute `sortAfter` in the annotation `@DatatableColumn`. Sorting is done in the class [DatatableColumnsFactory.java](../src/main/java/sk/iway/iwcm/system/datatable/DataTableColumnsFactory.java) in the method `sortColumns`.
- #44890 [Datatables] - Added annotation options `DataTableColumnType.BOOLEAN` for true/false values and `DataTableColumnType.CHECKBOX` for standard checkboxes.
- #44890 [Datatables] - Added annotation option [nested objects](developer/datatables-editor/datatable-columns.md#vnorené-atribúty) via `@DatatableColumnNested`. The prefix attribute (the default value is auto, which generates the prefix according to the name of the annotated field) can be used to specify the prefix of an attribute in the generated JSON object (e.g. by setting it to `editorFields` an array is generated in JSON `allowChangeUrl` Like `editorFields.allowChangeUrl`). The prefix can also be empty, then only the variable name is generated (if it has e.g. `gettery/settery` in the original object such as. [FieldsFromAtoE](../src/main/java/sk/iway/iwcm/doc/FieldsFromAtoE.java) v [DocDetails](../src/main/java/sk/iway/iwcm/doc/DocDetails.java) facility.)

## 2020.30

- #44890 [Datatables] - Resolved embedding multiple datatables into a web page. Each datatable will have its HTML ID attribute set even if it is not in `options` (by default datatableInit). The ID attribute is then also inserted into other selectors to correctly map filters and buttons to the correct datatable. Modified functions `$.fn.dataTable.Editor.display.bootstrap.init` a `$.fn.dataTable.Editor.display.bootstrap.open` to support multiple DT editors in the page.

## 2020.29

- #346264 [Web pages] Added ability to add/edit directories in the tree structure of web pages. Implemented using hidden datatable and DT editor.
- #45763 [Tree structure] Added API to [editing the tree structure](developer/jstree/README.md), used in the website section and photo gallery. It is already possible to move items in the directory structure of web pages and move directories in the gallery.
- #44836 [Website] Added fields to DT editor, backend doesn't process them yet
- #47125 [Background tasks] Remade into datatables, added column with task name to database

## 2020.28

- #45532 [Forms] Added automated search
- #45409 [Audit] Added filtering tests
- [Datatables] for the checkbox attribute automatically set `falseValue` to no (if not specified)
- Fixed accent setting for JS files from `/admin/v9/dist/` directories (output sent in UTF-8)

## 2020.27

- #46264 [standalone datatables editor] Added option to mark a datatable as hidden ([hideTable attribute in options](developer/datatables/README.md)) for the option `standalone` DT editor for jstree
- #45721 [Template Groups] Modified datatable view to use annotation, added test

## 2020.26

- #43588 [Datatables] Fixed repeated setting of select box in filter and in editor (after second setting values could not be selected)
- #45532 [Forms] Added basic search option in form data, limited to maximum 6 columns (due to Spring DATA API)
- #45718 [Templates] Template list version via datatable, uses `TemplatesDB` API
- #45490 [Audit] View audit logs and notifications. Both via Spring DATA, for audit added select boxes for filtering in datatable (linked to options editor), added option [custom filtering](./datatables/restcontroller.md#špeciálne-vyhľadávanie) in the REST controller.
- #44968 [Translation keys] Display and edit translation keys, implemented reading of original values, mapping `.properties` and DB changes.

## 2020.25

- #45532 [Forms] Display list of forms and form data (dynamic data) via datatable.
- #45679 [Datatables] Annotations for generation `columns` configurations transferred to WebJET 8 for annotation of existing beans
- #45382 [Datatables] Data import and export - modify data import dialog

## 2020.23

- #45685 \[CI-CD] Configured CI-CD pipeline on Gitlab server, nightly build from master branch available at http://demotest.webjetcms.sk/admin/
- #45382 [Datatables] Initial version of data import by sending data from import excel to datatables editor, possibility to set import mode (no support on backend yet)
- #45379 [Datatables] Refactored datatable customization code from `app.js` into a separate file [index.js](../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) with the possibility of use as a [NPM module](../src/main/webapp/admin/v9/npm_packages/README.md) (e.g. on the frontend in the template). Refactored code for the possibility of using multiple datatables on one page.
- #45679 [Datatables] Added option [generate columns configuration](developer/datatables-editor/datatable-columns.md) datatables using Java bean annotation

## 2020.22

- #44836 [Website] Migrated ckeditor editing code from WJ8. Modified image embedding, forms, and other CK editor dialogs. Added hidden form containing `docid, groupid, virtualpath` for compatibility of application preview and dialogs from WJ8.
- #44836 [Website] For the editor, the `options` sends `original` template object that is needed to get paths to CSS styles. When the DT editor is opened, the CSS starting with /admin is preserved (for the CK extension editor), the rest is changed based on the assigned template.
- #44836 [Website] The DT editor is available directly [JSON object](./datatables-editor/README.md#ukážky-kódu) of the edited record (not all data). For `options` dials added sending option [original object](./datatables/restcontroller.md). The JSON object retrieved from the server (containing the complete data of all rows and the options property) is available in the DT as DATA.json.
- #44737 [Configuration] Updated [self-test](../src/test/webapp/tests/components/configuration.js).
- #44293 [Domain redirects] Updated [self-test](../src/test/webapp/tests/components/domain-redirects.js).

## 2020.21

- [Testing] Added CSS classes `dt-filter-MENO` on the filter fields in the datatable for the ability to set filtering in the automated test `I.fillField("input.dt-filter-newUrl", randomNumber);`. Example in the test [redirects.js](../src/test/webapp/tests/components/redirects.js)
- [Redirections] Added [annotation of mandatory fields](./datatables/restcontroller.md#valid%c3%a1cia--povinn%c3%a9-polia) to UrlRedirectBean.java (in WJ8)
- [Gallery] Added text translations for image editor and area of interest, added option to set area of interest in input fields, added [auto test](../src/test/webapp/tests/components/gallery.js)
- [Documentation] Added [sample code](developer/frameworks/example.md) use of frameworks
- [Testing] Added [report generation](./testing/README.md#generovanie-html-reportu) via mochawesome and CodeceptUI for [test control](./testing/README.md#codecept-ui) via browser
- [Datatable] Added option to display data (columns and fields in the editor) [on the basis of the rights](./datatables/README.md#zobrazenie-dát-na-základe-práv) logged in user

## 2020.20

- [Datatable] Added [search by date range](./datatables/restcontroller.md#vyh%c4%bead%c3%a1vanie-pod%c4%bea-rozsahu-d%c3%a1tumov) specific processing of URL parameter with prefix `daterange:od-do`.
- [Documentation] Supplemented User Manual [assert](./testing/README.md#assert-kni%c5%benica) libraries, the use and writing of new [translation keys](./frameworks/thymeleaf.md#prekladov%c3%bd-text)
- [Gallery] Modified display of image language mutation columns to use API datatables (to allow inline editing to work correctly)
- [Translation keys] New version of display and editing [translation keys](../src/main/webapp/admin/v9/views/pages/settings/translation-keys.pug)
- [Datatable] Refactored code [DatatableRestControllerV2.java](../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java) with an annotated division into entity and rest methods
- [Configuration] New view version [configurations](../src/main/webapp/admin/v9/views/pages/settings/configuration.pug) WebJET
- [Documentation] Supplementary documentation for [SFC Vue](developer/frameworks/vue.md)
- [Photo Gallery] Additional connection [image editor](../src/main/webapp/admin/v9/src/js/image-editor.js) at `chunked upload` and toastr notifications
- [Redirects] New view version [redirected by](../src/main/webapp/admin/v9/views/pages/settings/redirect.pug)
- [Photo Gallery] Changed JCrop add-on to indicate the area of interest on `vue-advanced-cropper`

## 2020.19

- [Testing] Basic concept [automated testing](developer/testing/README.md)
- [Documentation] Extensions [Documentation](README.md)
- [Web site] Initial version of the integration [ckeditor to datatable](../src/main/webapp/admin/v9/src/js/datatables-ckeditor.js) and display [web pages](../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug)
- [Datatable] Fixed problem with date format, automatic setting by `window.userLang` at the moment library - settings `wireformat` (default x) and the format of attributes in the editor(default L HH:mm)
- [Gallery] Added option to add and [edit directory](../src/main/webapp/admin/v9/views/partials/folder-tree-galleria.pug), connected to Vue

## 2020.18

- [Domain redirects] New view version [domain redirects](../src/main/webapp/admin/v9/views/pages/settings/domain-redirect.pug)
- [Security] Call security of all rest services is ensured [automatic setting](../src/main/webapp/admin/v9/views/partials/head.pug) CSRF token for all ajax requests
- [Upload] Resolved call for correct processing of files after upload and API for file deletion (cleanup of gallery, video thumbnails, fulltext index)
- [Gallery] Added [image editor](../src/main/webapp/admin/v9/src/js/image-editor.js)
- [Web pages] Basic display implementation [tree structures](../src/main/java/sk/iway/iwcm/admin/layout/JsTreeItem.java) Site

## 2020.13

\[#43588 - wj9 - basic layout functionalization] - task #5 =\&gt;

- modified lombook configuration (in build.gradle - `config['lombok.accessors.chain'] = 'true'`) in order to `settre` Returned `this` for chaining
- webpack set to not remove quotes in attributes (because TH can return a multi-word expression - `removeAttributeQuotes: false`)
- MENU - added MenuBean and MenuService objects that generate menus, translation of the original ModuleInfo from `Modules` object (groups, icons) + modified texts via `text-webjet9.properties`

## 2020.12

\[#43588 - wj9 - basic layout functionality] - task #1 =\&gt; I committed the first version, made it functional:
- click on the logo + `tooltip` with version (TODO: it would be better to write it out somehow, TODO: add logos for NET and LMS)
- switching domains (they are there for `iwcm.interway.sk` also illustration data), TODO: it's broken, `@MHO` need to be upset
- Help
- the name of the logged-in user
- logout link
- TODO: remaining icons in the header

PLUS:
- lombook enabled on the project - https://projectlombok.org
- basic classes made `LayoutBean` a `HeaderBean` with their insertion into the model
- Supplemented by `webjet.js` with JS functions WJ.xxx
- basic skeleton for translation of texts, added `WJ.translate`, but it must be called as `async`which slows down the display.
I suggest in the future to pass translation keys from HTML code as a JS function parameter (in the pug file the translation is simply inserted as `#{prekladovy.kluc}` and the server is translated.
