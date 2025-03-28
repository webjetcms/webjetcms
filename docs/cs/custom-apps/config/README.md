# Konfigurace

Pro zobrazení pouze specifických konfiguračních proměnných, na základě jejich prefixu, můžete využít prefix konfiguraci. Jedná se o zjednodušenou verzi sekce [Konfigurace](../../admin/setup/configuration/README.md), kde se zobrazují konfigurační proměnné na základě zadaného prefixu.

!>**Upozornění:** prefixem filtrované konfigurační přeměny zobrazují i ty, které se neodlišují od přednastavených hodnot, což je hlavní rozdíl oproti standardní sekci Konfigurace.

Implementace této zjednodušené verze konfigurace je ideální v případě, že chcete zobrazit jen ty konfigurační hodnoty, které přímo ovlivní konkrétní sekci či aplikaci. Příklad můžete vidět v sekci [AB testování](../../redactor/apps/abtesting/abtesting.md) část Konfigurace. Tato část zobrazuje všechny konfigurační proměnné, které mají dopad právě na AB testování, tedy začínají prefixem `ABTesting`.

## Backend implementace

Celá backend logika se nachází v abstraktním kontroléru `AbstractConfigurationController`. Tento abstraktní kontrolér již dědí z abstraktní třídy `DatatableRestControllerV2` a automatický je přednastavena k práci s třídou `ConfPrefixDto`.

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

Jak je zobrazeno na ukázce výše, pro využití stačí mít kontrolér, který bude od abstraktního `AbstractConfigurationController` dědit. Tato rodičovská třída má již implementovanou logiku, která se o vše ostatní postará (získání všech dat, získání jednoho záznamu, úprava záznamu).

!>**Upozornění:** pro fungování je třeba zavolat konstruktor rodiče s těmito parametry (prefix konfiguračních proměnných, instance `ConfDetailsMapper`).

Povolena je pouze akce úpravy záznamu, všechny ostatní akce jako přidání/vymazání/duplikování jsou zakázány.

## Fronted implementace

Tato implementace je stejně jednoduchá jako při vytvoření každé jiné datatabulky. Stačí v souboru vytvořit instanci `WJ.DataTable`.

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
- vždy musíte využít entitu `sk.iway.iwcm.components.configuration.model.ConfPrefixDto`, jako zdroj pro sloupce datatabulky

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>
```

Jelikož jediná povolená akce je úprava, doporučujeme skrýt tlačítka nepovolených akcí i v editoru vypnout změnu názvu konfigurační proměnné.
