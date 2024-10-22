# Konfigurácia

Pre zobrazenie iba špecifických konfiguračných premenných, na základe ich prefixu, môžete využiť prefix konfiguráciu. Ide o zjednodušenú verziu sekcie [Konfigurácia](../../admin/setup/configuration/README.md), kde sa zobrazujú konfiguračné premenné na základe zadaného prefixu.

!>**Upozornenie:** prefixom filtrované konfiguračné premeny zobrazujú aj tie, ktoré sa neodlišujú od prednastavených hodnôt, čo je hlavný rozdiel oproti štandardnej sekcií Konfigurácia.

Implementácia tejto zjednodušenej verzie konfigurácie je ideálna v prípade, že chcete zobraziť iba tie konfiguračné hodnoty, ktoré priamo ovplyvnia konkrétnu sekciu či aplikáciu. Príklad môžete vidieť v sekcií [AB testovanie](../../redactor/apps/abtesting/abtesting.md) časť Konfigurácia. Táto časť zobrazuje všetky konfiguračné premenné, ktoré majú dopad práve na AB testovanie, čiže začínajú prefixom ``ABTesting``.

## Backend implementácia

Celá backend logika sa nachádza v abstraktnom kontroléri ```AbstractConfigurationController```.  Tento abstraktný kontrolér už dedí z abstraktnej triedy ```DatatableRestControllerV2``` a automatický je prednastavená k práci s triedou ```ConfPrefixDto```.

Príklad implementácie z ``AbTestingRestController``:
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

Ako je zobrazené na ukážke vyššie, pre využitie stačí mať kontrolér, ktorý bude od abstraktného ```AbstractConfigurationController``` dediť. Táto rodičovská trieda má už implementovanú logiku, ktorá sa o všetko ostatné postará (získanie všetkých dát, získanie jedného záznamu, úprava záznamu).

!>**Upozornenie:** pre fungovanie je potrebné zavolať konštruktor rodiča s týmito parametrami (prefix konfiguračných premenných, inštancia ``ConfDetailsMapper``).

Povolená je iba akcia úpravy záznamu, všetky ostatné akcie ako pridanie/vymazanie/duplikovanie sú zakázané.

## Fronted implementácia

Táto implementácia je rovnako jednoduchá ako pri vytvorení každej inej datatabuľky. Stačí v súbore vytvoriť inštanciu ``WJ.DataTable``.

Príklad implementácie z ``apps/abtesting/admin/config.html``:

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

Nastavenie:

- url musí smerovať na kontrolér, ktorý dedí od ``AbTestingRestController``
- vždy musíte využiť entitu ``sk.iway.iwcm.components.configuration.model.ConfPrefixDto``, ako zdroj pre stĺpce datatabuľky

```javascript
    <script data-th-inline="javascript">
        let url = "/admin/rest/abtesting";
        let columns = /*[(${layout.getDataTableColumns("sk.iway.iwcm.components.configuration.model.ConfPrefixDto")})]*/ '';
    </script>
```

Nakoľko jediná povolená akcia je úprava, odporúčame skryť tlačidlá nepovolených akcií ako aj v editore vypnúť zmenu názvu konfiguračné premennej.