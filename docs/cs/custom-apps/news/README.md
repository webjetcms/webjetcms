# Seznam webových stránek/novinek

Aplikace [Novinky](../../redactor/apps/news/README.md), vloží do stránky seznam webových stránek v zadané složce. Slouží k vložení seznamu novinek, tiskových zpráv, ale i dalších podobných výpisů (seznam kontaktních míst, osobních kontaktů, produktů apod.).

Tabulka poskytuje možnosti úprav podobné seznamu webových stránek.

![](../../redactor/apps/news/admin-dt.png)

Pokud má zákazník specifické požadavky na zobrazení sloupců (např. pro seznam produktů) nebo informací, můžete implementovat vlastní aplikaci, která použije kód pro seznam novinek.

Níže je uveden kompletní kód stránky s novinkami. Můžete využít funkci úpravy standardních sloupců pomocí příkazu `window.WJ.DataTable.mergeColumns`. Příklad upravuje nastavení atributu viditelnosti sloupce, ale můžete také přejmenovat název, jak je uvedeno v příkladu názvu stránky.

Pokud nepotřebujete měnit hodnotu ID složky, můžete samozřejmě volání AJAXu odstranit. `/admin/rest/news/news-list/convertIdsToNamePair`, který získá seznam pro pole výběru složky v záhlaví.

```html
<script data-th-inline="javascript">
    var webpageColumns = /*[(${layout.getDataTableColumns('sk.iway.iwcm.doc.DocDetails')})]*/ '';
</script>
<script type="text/javascript">
    var newsDataTable;
    var idsConstantName = "newsAdminGroupIds";

    window.domReady.add(function () {

        WJ.breadcrumb({
            id: "newsBreadcrumb",
            tabs: [
                {
                    url: '/apps/news/admin/',
                    title: '[[#{components.menu.news}]]',
                    active: true
                },
                {
                    url: '#newsGroup',
                    title: '{filter}',
                    active: false
                }
            ]
        });

        function setGroupIdFilterSelect(data) {
            //Get object, select
            let filterSelect = document.getElementById('groupIdFilterSelect');
            //Remove all options except the default one
            while(filterSelect.options.length > 1) {
                filterSelect.remove(1);
            }
            //Add new options
            for (const s of data) {
                filterSelect.add(new Option(s.label, s.value));
            }

            //set selected value
            var hash = window.location.hash;
            if (hash != "") $(filterSelect).val(hash.substr(1));

            //Refresh object
            $("#groupIdFilterSelect").selectpicker('refresh');
        }

        //move filter to top navbar
        $("#pills-newsGroup-tab").html("");
        $("div#groupId_extfilter").appendTo("#pills-newsGroup-tab");

        let urlGroupIdFilter = "/admin/rest/news/news-list/convertIdsToNamePair?ids=constant:"+idsConstantName;
        let data = {};
        var includeParameter = WJ.urlGetParam("include");
        if (includeParameter != null) {
            data.include = includeParameter
        }

        $.ajax({
            url: urlGroupIdFilter,
            data: data,
            method: "post",
            success: function (data) {

                setGroupIdFilterSelect(data);

                let url = "/admin/rest/web-pages";

                //rename title column
                window.WJ.DataTable.mergeColumns(webpageColumns, { name: "title", title: WJ.translate("apps.news.newsTitle.js") });
                //add column visibility
                window.WJ.DataTable.mergeColumns(webpageColumns, { name: "publishStartDate", visible: true });
                window.WJ.DataTable.mergeColumns(webpageColumns, { name: "publishEndDate", visible: true });
                window.WJ.DataTable.mergeColumns(webpageColumns, { name: "htmlData", visible: true });
                window.WJ.DataTable.mergeColumns(webpageColumns, { name: "perexImage", visible: true });

                window.importWebPagesDatatable().then(module => {

                    var order = [];
                    order.push([4, 'desc']);

                    let wpdInstance = new module.WebPagesDatatable({
                        url: WJ.urlAddParam(url, "groupIdList", $("#groupIdFilterSelect").val()),
                        columns: webpageColumns,
                        id: "newsDataTable",
                        order: order,
                        newPageTitleKey: "apps.news.newsTitle.js", //optional, title of new page
                        showPageTitleKey: "admin.search.showFile.js", //optional, title of Show Page (eye) button
                    });
                    newsDataTable = wpdInstance.createDatatable();
                });

                $("#groupIdFilterSelect").on("change", function() {
                    var value = this.value;
                    var newUrl = WJ.urlAddParam(url, "groupIdList", this.value);
                    newsDataTable.setAjaxUrl(newUrl);
                    newsDataTable.ajax.reload();
                });
            }

        });

    });
</script>

<style type="text/css">
    #pills-newsGroup-tab .bootstrap-select,
    #pills-newsGroup-tab .bootstrap-select button {
        min-width: 220px;
        width: auto;
    }
</style>

<div id="groupId_extfilter">
    <div class="row datatableInit">
        <div class="col-auto">
            <select id="groupIdFilterSelect">
            </select>
        </div>
    </div>
</div>

<table id="newsDataTable" class="datatableInit table"></table>
```

Pokud uživatel nemá přímý přístup k webovým stránkám, je třeba přidat právo aplikace do proměnné conf. `webpagesFunctionsPerms`, kde jsou uvedena práva pro práci s webovou stránkou. To zahrnuje funkce pro vložení obrázku a podobně.

## Backend

Pokud potřebujete konkrétní službu REST pro poskytování seznamu webových stránek/novinek, můžete použít třídu ready. [Webové stránkyDatovatelné](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) který můžete rozšířit a přepsat jeho metody podle svých potřeb.

```java
@Datatable
@RestController
@RequestMapping("/admin/rest/abtesting/list")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_abtesting')")
public class AbTestingRestController extends WebpagesDatatable {

    @Autowired
    public AbTestingRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {
        GetAllItemsDocOptions options = getDefaultOptions(pageable, true);
        return AbTestingService.getAllItems(options);
    }

    @Override
    public void beforeSave(DocDetails entity) {
        //In abtesting version user cant edit/insert/duplicate page's
        throwError(getProp().getText("admin.editPage.error"));
    }

    @Override
    public boolean deleteItem(DocDetails entity, long id) {
        //In abtesting version user cant delete page's
        throwError(getProp().getText("admin.editPage.error"));

        return false;
    }
}
```
