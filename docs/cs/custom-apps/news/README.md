# Seznam web stránek/novinek

Aplikace [Novinky](../../redactor/apps/news/README.md), vloží do stránky seznam web stránek v zadané složce. Používá se pro vkládání seznamu novinek, tiskových zpráv, ale i jiných podobných výpisů (seznam kontaktních míst, osobních kontaktů, produktů a podobně).

Tabulka poskytuje editační možnosti podobně jako seznam web stránek.

![](../../redactor/apps/news/admin-dt.png)

Pokud má zákazník specifické požadavky na zobrazení sloupců (např. pro seznam produktů) nebo informací můžete implementovat vlastní aplikaci, která využívá kód pro seznam novinek.

Níže je uveden kompletní kód stránky se seznamem novinek. Můžete využít vlastnost úpravy standardních sloupců pomocí funkce `window.WJ.DataTable.mergeColumns`. V příkladu se upravuje nastavení atributu viditelnosti sloupce, můžete ale také přejmenovat název jak je ukázáno v případě názvu stránky.

Strom využívá REST službu `/admin/rest/news/news-list/tree`, která vrací složky podle `newsAdminGroupIds` nebo parametru `include`, jejich podsložky, nadřazené větve a výsledky vyhledávání. Nadřazené složky mimo rozsah novinek mají ikonu `ti ti-folders`, vypnutý výběr a nemají `groupIdList` ; jejich přidáním se nezpřístupní další sourozenecké složky. Hodnota `groupIdList` v uzlu novinek určuje filtr tabulky včetně případného znaku `*` ; číselné `id` určuje složku pro novou stránku. Původní služba `/admin/rest/news/news-list/convertIdsToNamePair` zůstává dostupná pro stávající integrace.

Pokud potřebujete tabulku pro pevnou složku bez stromu, stačí inicializovat `WebPagesDatatable` s URL `/admin/rest/news/news-list?groupId=23&groupIdList=23`.

```html
<script data-th-inline="javascript">
    var webpageColumns = /*[(${layout.getDataTableColumns('sk.iway.iwcm.doc.DocDetails')})]*/ '';
</script>
<script type="text/javascript">
    var newsDataTable;
    const newsInclude = new URLSearchParams(window.location.search).get("include");

    window.getJstreeUrl = function() {
        let url = $("#SomStromcek").data("rest-url");
        if (newsInclude != null) url = WJ.urlUpdateParam(url, "include", newsInclude);
        const hash = window.location.hash.substring(1);
        if (/^\d+\*?$/.test(hash)) url = WJ.urlUpdateParam(url, "selectedId", parseInt(hash, 10));
        return url;
    };

    window.domReady.add(function () {
        const treeElement = $("#SomStromcek");
        let selectedFolder = null;
        let tablePromise = null;
        let currentFilter = null;

        window.WJ.DataTable.mergeColumns(webpageColumns, { name: "title", title: WJ.translate("apps.news.newsTitle.js") });
        for (const name of ["publishStartDate", "publishEndDate", "htmlData", "perexImage"]) {
            window.WJ.DataTable.mergeColumns(webpageColumns, { name: name, visible: true });
        }

        function getTableUrl() {
            let url = newsDataTable ? newsDataTable.getAjaxUrl() : "/admin/rest/news/news-list";
            url = WJ.urlUpdateParam(url, "groupIdList", selectedFolder ? selectedFolder.groupIdList : "-1");
            return WJ.urlUpdateParam(url, "groupId", selectedFolder ? selectedFolder.id : "-1");
        }

        /** Synchronizes the article list and creation defaults with the selected News folder. */
        function selectFolder(node) {
            selectedFolder = node ? node.original : null;
            window.selectedNode = selectedFolder;
            $("#news-folders-empty").toggleClass("d-none", selectedFolder != null);
            if (selectedFolder && (!window.location.hash || /^#\d+\*?$/.test(window.location.hash))) {
                window.history.replaceState(null, "", "#" + selectedFolder.groupIdList);
            }

            if (tablePromise == null) {
                tablePromise = window.importWebPagesDatatable().then(module => {
                    const instance = new module.WebPagesDatatable({
                        url: getTableUrl(),
                        columns: webpageColumns,
                        id: "newsDataTable",
                        order: [ [4, "desc"] ],
                        newPageTitleKey: "apps.news.newsTitle.js"
                    });
                    currentFilter = selectedFolder ? selectedFolder.groupIdList : "-1";
                    newsDataTable = instance.createDatatable();
                    // Include the empty-state message in the header measured by automatic table sizing.
                    $("#news-folders-empty").prependTo("#newsDataTable_wrapper .dt-header-row");
                    newsDataTable.on("draw.dt", function() {
                        newsDataTable.button(".buttons-create").enable(selectedFolder != null);
                    });
                });
            }
            tablePromise.then(() => {
                const filter = selectedFolder ? selectedFolder.groupIdList : "-1";
                if (currentFilter !== filter) {
                    currentFilter = filter;
                    newsDataTable.setAjaxUrl(getTableUrl());
                    newsDataTable.ajax.reload();
                }
                newsDataTable.button(".buttons-create").enable(selectedFolder != null);
            });
        }

        treeElement.on("select_node.jstree", function(e, data) {
            if (!data.node.state.disabled) selectFolder(data.node);
        });

        treeElement.on("ready.jstree refresh.jstree", function() {
            // Searching must not replace the article list when its folder is outside the results.
            if ($("#tree-folder-search-input").val()) return;
            const tree = treeElement.jstree(true);
            const selected = tree.get_selected(true).find(node => !node.state.disabled);
            if (selected) selectFolder(selected);
            else {
                const first = tree.get_json("#", { flat: true }).find(node => !node.state.disabled);
                if (first) tree.select_node(first.id);
                else selectFolder(null);
            }
        });

        window.addEventListener("hashchange", function() {
            if (/^#\d+\*?$/.test(window.location.hash)) treeElement.jstree(true).refresh(false, true);
        });
    });
</script>

<div class="row">
    <div class="col-md-4 tree-col">
        <div class="dt-header-row clearfix">
            <div class="row">
                <div class="col-auto">
                    <div class="dt-buttons">
                        <button class="btn btn-sm btn-outline-secondary buttons-refresh" type="button"
                            data-toggle="tooltip" data-th-title="#{datatables.button.reload.js}"
                            data-th-aria-label="#{datatables.button.reload.js}">
                            <span><i class="ti ti-refresh" aria-hidden="true"></i></span>
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <div id="SomStromcek" class="hide-while-loading" data-rest-url="/admin/rest/news/news-list"
            data-rest-param-name="id" data-single-select="true" data-search-restore-state="false"></div>
    </div>
    <div class="col-md-8 datatable-col">
        <div id="news-folders-empty" class="alert alert-info d-none" role="status" data-th-text="#{apps.news.noFolders}"></div>
        <table id="newsDataTable" class="datatableInit table"></table>
    </div>
</div>
```

Pokud uživatel nemá přímo přístup k web stránkám je třeba přidat ještě vaše právo aplikace do konf. proměnné `webpagesFunctionsPerms`, která obsahuje seznam práv, která získávají právo na práci s web stránkami. Jedná se také o funkce pro vložení obrázku a podobně.

## Backend

Pokud potřebujete specifickou REST službu pro poskytování seznamu web stránek/novinek můžete využít připravenou třídu [WebpagesDatatable](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) kterou rozšíříte a přepíšete metody podle vašich potřeb.

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
