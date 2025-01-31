# Configuration

To display only specific configuration variables, based on their prefix, you can use the prefix configuration. This is a simplified version of the section [Configuration](../../admin/setup/configuration/README.md), where the configuration variables are displayed based on the prefix entered.

!>**Warning:** prefix-filtered configuration transformations also show those that do not differ from the preset values, which is the main difference from the standard Configuration section.

Implementing this simplified version of the configuration is ideal if you want to display only those configuration values that directly affect a particular section or application. An example can be seen in the section [AB testing](../../redactor/apps/abtesting/abtesting.md) Configuration section. This section displays all configuration variables that have an impact on AB testing, i.e. they start with the prefix `ABTesting`.

## Backend implementation

The entire backend logic is located in the abstract controller `AbstractConfigurationController`. This abstract controller already inherits from the abstract class `DatatableRestControllerV2` and automatic is preset to work with the class `ConfPrefixDto`.

Example implementation from `AbTestingRestController`:

```java
    @RestController
    @Datatable
    @RequestMapping("/admin/rest/abtesting")
    @PreAuthorize("@WebjetSecurityService.hasPermission('cmp_abtesting')")
    public class AbTestingRestController extends AbstractConfigurationController {

        @Autowired
        public AbTestingRestController(ConfDetailsMapper confDetailsMapper) {
            super("ABTesting", confDetailsMapper);
        }
    }
```

As shown in the example above, to use it, you just need to have a controller that will be from the abstract `AbstractConfigurationController` inherit. This parent class has already implemented logic that takes care of everything else (getting all the data, getting one record, modifying a record).

!>**Warning:** to work, you need to call the parent constructor with the following parameters (prefix of configuration variables, instance `ConfDetailsMapper`).

Only the record edit action is enabled, all other actions such as add/delete/duplicate are disabled.

## Fronted implementation

This implementation is as simple as creating any other datatable. Just create an instance in the file `WJ.DataTable`.

Example implementation from `apps/abtesting/admin/config.html`:

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>

    <script type="text/javascript">
        var abtestingConfDataTable;

        WJ.breadcrumb({
            id: "configValues",
            tabs: [
                {
                    url: '/apps/abtesting/admin/',
                    title: '[[#{components.abtesting.dialog_title}]]',
                    active: false
                },
                {
                    url: '/apps/abtesting/admin/config/',
                    title: '[[#{webjet.left_conf.konfiguracia}]]',
                    active: true
                }
            ]
        });

        window.domReady.add(function () {
            abtestingConfDataTable = WJ.DataTable({
                url: url,
                serverSide: false,
                columns: columns,
                id: "abtestingConfDataTable"
            });

            //HIde buttons that can't be used
            abtestingConfDataTable.hideButton("create");
            abtestingConfDataTable.hideButton("remove");
            abtestingConfDataTable.hideButton("celledit");
            abtestingConfDataTable.hideButton("import");
            abtestingConfDataTable.hideButton("duplicate");

            //Disable field that can't be changed
            abtestingConfDataTable.EDITOR.on('open', function (e, mode, action) {
                $("#DTE_Field_name").prop('disabled', true);
            });
        });
    </script>

    <table id="abtestingConfDataTable" class="datatableInit table"></table>
```

Setting:
- url must be directed to a controller that inherits from `AbTestingRestController`
- you must always use the entity `sk.iway.iwcm.components.configuration.model.ConfPrefixDto`, as a source for datatable columns

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>
```

Since the only allowed action is editing, it is recommended to hide the buttons of not allowed actions as well as to disable changing the name of the configuration variable in the editor.
