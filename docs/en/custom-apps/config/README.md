# Configuration

To display only specific configuration variables based on their prefix, you can use prefix configuration. This is a simplified version of the [Configuration](../../admin/setup/configuration/README.md) section, which displays configuration variables based on the specified prefix.

!>**Warning:** Prefix-filtered configuration changes also display those that do not differ from the default values, which is the main difference from the standard Configuration section.

Implementing this simplified version of the configuration is ideal if you want to display only those configuration values ​​that directly affect a specific section or application. You can see an example in the [AB testing](../../redactor/apps/abtesting/abtesting.md) section Configuration. This section displays all configuration variables that have an impact on AB testing, i.e. they start with the prefix ``ABTesting``.

## Backend implementation

All backend logic is located in the abstract controller ```AbstractConfigurationController```. This abstract controller already inherits from the abstract class ```DatatableRestControllerV2``` and is automatically preset to work with the class ```ConfPrefixDto```.

Example implementation from ``AbTestingRestController``:
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

As shown in the example above, to use it, you just need to have a controller that will inherit from the abstract ```AbstractConfigurationController```. This parent class already has the logic implemented that will take care of everything else (getting all data, getting one record, editing a record).

!>**Warning:** for this to work, the parent constructor must be called with these parameters (configuration variable prefix, instance ``ConfDetailsMapper``).

Only the action of editing a record is allowed, all other actions such as adding/deleting/duplicating are prohibited.

## Front-end implementation

This implementation is as simple as creating any other datatable. Just create an instance of ``WJ.DataTable`` in the file.

Example implementation from ``apps/abtesting/admin/config.html``:

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

- url must point to a controller that inherits from ``AbTestingRestController``
- you must always use the ``sk.iway.iwcm.components.configuration.model.ConfPrefixDto`` entity as the source for the datatable columns

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>
```

Since the only allowed action is editing, we recommend hiding the buttons for disallowed actions and disabling changing the name of the configuration variable in the editor.