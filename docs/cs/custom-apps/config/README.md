# Konfigurace

Chcete-li zobrazit pouze konkrétní konfigurační proměnné na základě jejich prefixu, můžete použít konfiguraci prefixu. Jedná se o zjednodušenou verzi oddílu [Konfigurace](../../admin/setup/configuration/README.md), kde se na základě zadané předpony zobrazí konfigurační proměnné.

!>**Varování:** transformace konfigurace s předponou se zobrazují také ty, které se neliší od přednastavených hodnot, což je hlavní rozdíl oproti standardní sekci Konfigurace.

Implementace této zjednodušené verze konfigurace je ideální, pokud chcete zobrazit pouze ty hodnoty konfigurace, které přímo ovlivňují určitou sekci nebo aplikaci. Příklad lze vidět v části [AB testování](../../redactor/apps/abtesting/abtesting.md) Sekce konfigurace. V této části jsou zobrazeny všechny konfigurační proměnné, které mají vliv na testování AB, tj. začínají předponou `ABTesting`.

## Implementace backendu

Celá logika backendu je umístěna v abstraktním řadiči. `AbstractConfigurationController`. Tento abstraktní řadič již dědí z abstraktní třídy `DatatableRestControllerV2` a automatický je přednastaven na práci s třídou `ConfPrefixDto`.

Příklad implementace z `AbTestingRestController`:

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

Jak je ukázáno v příkladu výše, k jeho použití stačí mít kontrolér, který bude z abstraktního souboru `AbstractConfigurationController` dědit. Tato nadřazená třída má již implementovanou logiku, která se stará o vše ostatní (získání všech dat, získání jednoho záznamu, změna záznamu).

!>**Varování:** je třeba zavolat nadřazený konstruktor s následujícími parametry (prefix konfiguračních proměnných, instance `ConfDetailsMapper`).

Povolena je pouze akce editace záznamu, všechny ostatní akce, jako je přidání/odstranění/duplikace, jsou zakázány.

## Předsunutá implementace

Tato implementace je stejně jednoduchá jako vytvoření jakékoli jiné datové tabulky. Stačí vytvořit instanci v souboru `WJ.DataTable`.

Příklad implementace z `apps/abtesting/admin/config.html`:

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

Nastavení:
- url musí směřovat na kontrolér, který dědí od `AbTestingRestController`
- musíte vždy použít entitu `sk.iway.iwcm.components.configuration.model.ConfPrefixDto`, jako zdroj datových sloupců

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>
```

Protože jedinou povolenou akcí je editace, doporučujeme skrýt tlačítka nepovolených akcí a zakázat změnu názvu konfigurační proměnné v editoru.
