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
                        order: order
                    });
                    newsDataTable = wpdInstance.createDatatable();
                });

                $("#groupIdFilterSelect").on("change", function() {
                    var value = this.value;
                    var newUrl = WJ.urlAddParam(url, "groupIdList", this.value);
                    newsDataTable.ajax.url(newUrl);
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