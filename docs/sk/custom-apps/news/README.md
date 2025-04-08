# Zoznam web stránok/noviniek

Aplikácia [Novinky](../../redactor/apps/news/README.md), vloží do stránky zoznam web stránok v zadanom priečinku. Používa sa na vkladanie zoznamu noviniek, tlačových správ, ale aj iných podobných výpisov (zoznam kontaktných miest, osobných kontaktov, produktov a podobne).

Tabuľka poskytuje editačné možnosti podobne ako zoznam web stránok.

![](../../redactor/apps/news/admin-dt.png)

Ak má zákazník špecifické požiadavky na zobrazenie stĺpcov (napr. pre zoznam produktov) alebo informácií môžete implementovať vlastnú aplikáciu, ktorá využíva kód pre zoznam noviniek.

Nižšie je uvedený kompletný kód stránky so zoznamom noviniek. Môžete využiť vlastnosť úpravy štandardných stĺpcov pomocou funkcie `window.WJ.DataTable.mergeColumns`. V príklade sa upravuje nastavenie atribútu viditeľnosti stĺpca, môžete ale aj premenovať názov ako je ukázané v prípade názvu stránky.

Ak nepotrebujete meniť hodnotu ID priečinka, môžete samozrejme odstrániť AJAX volanie `/admin/rest/news/news-list/convertIdsToNamePair`, ktorá získava zoznam pre výberové pole priečinka v hlavičke.

```html
<script data-th-inline="javascript">
    var webpageColumns = /*[(${layout.getDataTableColumns('sk.iway.iwcm.doc.DocDetails')})]*/ '';
</script>
<script type="text/javascript">
    var newsDataTable;
    var idsConstantName = "newsAdminGroupIds";

    window.domReady.add(function () {

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

                //console.log("mam data:", data);

                let url = "/admin/rest/news/news-list";

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

                    url = WJ.urlAddParam(url, "groupIdList", data[0].value);

                    let wpdInstance = new module.WebPagesDatatable({
                        url: url,
                        columns: webpageColumns,
                        id: "newsDataTable",
                        order: order,
                        newPageTitleKey: "apps.news.newsTitle.js",
                    });
                    newsDataTable = wpdInstance.createDatatable();

                    $(".dt-buttons").prepend('<div class="btn btn-sm buttons-select" id="groupSelect_wrapper"><label>[[#{editor.div_properties.section}]]</label><select class="form-select" id="groupIdFilterSelect"></select></div>');
                    setGroupIdFilterSelect(data);

                    $("#groupIdFilterSelect").on("change", function() {
                        var value = this.value;
                        newsDataTable.setAjaxUrl(WJ.urlUpdateParam( newsDataTable.getAjaxUrl() , "groupIdList", this.value));
                        newsDataTable.ajax.reload();
                    });
                });
            }

        });

    });
</script>

<table id="newsDataTable" class="datatableInit table"></table>
```

Ak používateľ nemá priamo prístup k web stránkam je potrebné pridať ešte vaše právo aplikácie do konf. premennej `webpagesFunctionsPerms`, ktorá obsahuje zoznam práv, ktoré získavajú právo na prácu s web stránkami. Jedná sa aj o funkcie pre vloženie obrázku a podobne.

## Backend

Ak potrebujete špecifickú REST službu pre poskytovanie zoznamu web stránok/noviniek môžete využiť pripravenú triedu [WebpagesDatatable](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) ktorú rozšírite a prepíšete metódy podľa vašich potrieb.

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