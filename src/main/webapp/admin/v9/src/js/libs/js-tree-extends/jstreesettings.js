/**
 * Spravuje nastavenie zobrazenia web stranky.
 * - otvori a ulozi dialogove okno s nastaveniami
 * - exportuje moznosti do window.jstreeSettings.isIdShow() pre nastavenie zobrazenia v JS kode
 *
 * Zobrazenie jstree je upravene volanim window.jstreeCustomizeData co je inicializovane v app-init.js
 */
export class JstreeSettings {

    options = null;
    jstree = null;
    settingsModal = null;
    STORAGE_KEY = "jstreeSettings_web-pages-list";

    constructor(options) {
        //console.log("JstreeSettings constructor, options=", options);
        this.options = options;
        this.STORAGE_KEY = options.storageKey ? options.storageKey : "jstreeSettings_web-pages-list";
        window.jstreeCustomizeData = this.jstreeCustomizeData;
    }

    bindEvents() {
        var self = this;
        var somStromcek = $("#SomStromcek");
        self.jstree = somStromcek.jstree(true);
        if (self.isPagesShow()) {
            somStromcek.data("rest-url", somStromcek.data("rest-url").replace(/dt-tree-group/gi, "dt-tree"));
        }
        if (self.isShowFoldersDt()) $("body").addClass("showfolders-dt");
        else $("body").removeClass("showfolders-dt");

        var width = self.getTreeWidth();
        if (width != 4) {
            this.setTreeColWidth(width);
        }

        $("button.buttons-jstree-settings").on("click", function() {
            if (self.settingsModal == null) {
                self.settingsModal = new bootstrap.Modal(document.getElementById('jstreeSettingsModal'), {
                    keyboard: false,
                    backdrop: 'static'
                });
            }

            $("#jstree-settings-showid").prop("checked", self.isIdShow());
            $("#jstree-settings-showorder").prop("checked", self.isPriotityShow());
            $("#jstree-settings-showpages").prop("checked", self.isPagesShow());
            $("#jstree-settings-showfolders-dt").prop("checked", self.isShowFoldersDt());
            $("#jstree-settings-treeWidth").selectpicker("val", "" + self.getTreeWidth());
            $("#jstree-settings-treeSortType").selectpicker("val", self.getTreeSortType());
            $("#jstree-settings-treeSortOrderAsc").prop("checked", self.isTreeSortOrderAsc());
            self.settingsModal.show();
        });

        $("#jstree-settings-submit").on("click", function() {
            //console.log("SAVE JSTREE SETTINGS");
            let settings = self.getSettings();
            settings.showId = $("#jstree-settings-showid").is(":checked");
            settings.showPriority = $("#jstree-settings-showorder").is(":checked");
            settings.showPages = $("#jstree-settings-showpages").is(":checked");
            settings.showFoldersDt = $("#jstree-settings-showfolders-dt").is(":checked");
            var treeWidth = parseInt($("#jstree-settings-treeWidth").val());
            settings.treeWidth = treeWidth;

            let newTreeSortType = $("#jstree-settings-treeSortType").val();
            let newTreeSortOrderAsc = $("#jstree-settings-treeSortOrderAsc").is(":checked");
            let treeSortChange = false;
            if(newTreeSortType !== self.getTreeSortType() || newTreeSortOrderAsc !== self.isTreeSortOrderAsc()) {
                treeSortChange = true;
                settings.treeSortType = newTreeSortType;
                settings.treeSortOrderAsc = newTreeSortOrderAsc;
            }

            self.saveSettings(settings);
            self.settingsModal.hide();

            self.jstreeReload();
            self.jstree.refresh();

            if (self.isShowFoldersDt()) $("body").addClass("showfolders-dt")
            else $("body").removeClass("showfolders-dt")

            self.setTreeColWidth(treeWidth);
        });
    }

    getSettings() {
        let storeItem = window.WJ.getAdminSetting(this.STORAGE_KEY);
        if (typeof storeItem != "undefined") {
            return JSON.parse(storeItem);
        }
        return {}
    }

    saveSettings(settings) {
        //localStorage.setItem(this.STORAGE_KEY, JSON.stringify(settings));
        window.WJ.setAdminSetting(this.STORAGE_KEY, JSON.stringify(settings));
    }

    isIdShow() {
        let show = (true === this.getSettings().showId);
        //console.log("isIdShow=", show);
        return show;
    }

    isPriotityShow() {
        let show = (true === this.getSettings().showPriority);
        //console.log("isIdShow=", show);
        return show;
    }

    isPagesShow() {
        let show = (true === this.getSettings().showPages);
        //console.log("isPagesShow=", show);
        return show;
    }

    isShowFoldersDt() {
        let show = (true === this.getSettings().showFoldersDt);
        //console.log("showFoldersDt=", show);
        return show;
    }

    getTreeWidth() {
        //console.log("treeWidth=", this.getSettings().treeWidth);
        let width = this.getSettings().treeWidth;
        if (typeof width != "undefined") return parseInt(width);
        return 4;
    }

    getTreeSortType() {
        //console.log("treeSortType=", this.getSettings().treeSortType);
        let sortType = this.getSettings().treeSortType;
        if (typeof sortType != "undefined") return sortType;
        return "priority";
    }

    isTreeSortOrderAsc() {
        let order = (true === this.getSettings().treeSortOrderAsc);
        //console.log("treeSortOrderAsc=", order);
        return order;
    }

    jstreeReload() {
        $("#pills-folders-tab").trigger("click");
        $("#pills-folders-tab").tab("show");
    }

    jstreeCustomizeData(data) {
        //console.log("Customize data: ", data);

        if (data==null || typeof data == "undefined" || data.length < 1 ) return;

        const idShow = window.jstreeSettings.isIdShow();
        const priorityShow = window.jstreeSettings.isPriotityShow();
        data.forEach(function(item) {
            //console.log(item);
            if (typeof item.groupDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.groupDetails.groupId}</span> ${item.text}`;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.groupDetails.sortPriority})</span>`;
            } else if (typeof item.docDetails != "undefined") {
                if (idShow) item.text = `<span class="id">#${item.docDetails.docId}</span> ${item.text}`;
                if (priorityShow) item.text = `${item.text} <span class="sortPriority">(${item.docDetails.sortPriority})</span>`;
            }
        });
    }

    setTreeColWidth(width) {
        var dtWidth = 12 - width;
        //console.log("Menim sirku stlpcov, width=", width, "dtWidth=", dtWidth);
        var treeCol = $(".tree-col");
        treeCol.attr("class", "tree-col col-md-"+width);
        var datatableCol = $(".datatable-col");
        datatableCol.attr("class", "datatable-col col-md-"+dtWidth);
    }
}
