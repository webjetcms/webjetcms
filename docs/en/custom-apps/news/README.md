# List of websites/news

The [News] application (../../redactor/apps/news/README.md) inserts a list of websites in the specified folder into the page. It is used to insert a list of news, press releases, but also other similar listings (list of contact points, personal contacts, products, etc.).

The table provides editing options similar to a list of web pages.

![](../../redactor/apps/news/admin-dt.png)

If a customer has specific requirements for displaying columns (e.g. for a product list) or information, you can implement your own application that uses the code for the news list.

Below is the complete code for the news list page. You can use the feature of editing standard columns using the `window.WJ.DataTable.mergeColumns` function. In the example, the visibility attribute setting of the column is modified, but you can also rename the name as shown in the case of the page name.

The tree uses the REST service `/admin/rest/news/news-list/tree`, which returns folders by `newsAdminGroupIds` or parameter `include`, their subfolders, parent branches, and search results. Parent folders outside the news scope have an icon `ti ti-folders`, are disabled for selection, and do not have `groupIdList` ; adding them does not make other sibling folders available. The value `groupIdList` in the news node specifies the table filter, including the `*` character, if any; the numeric `id` specifies the folder for the new page. The original service `/admin/rest/news/news-list/convertIdsToNamePair` remains available for existing integrations.

If you need a table for a fixed folder without a tree, just initialize `WebPagesDatatable` with the URL `/admin/rest/news/news-list?groupId=23&groupIdList=23`.

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

If the user does not have direct access to the web pages, it is necessary to add your application rights to the config variable `webpagesFunctionsPerms`, which contains a list of rights that acquire the right to work with the web pages. These include functions for inserting images and the like.

## Backend

If you need a specific REST service for providing a list of web pages/news, you can use the ready-made class [WebpagesDatatable](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java) which you can extend and override the methods according to your needs.

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
