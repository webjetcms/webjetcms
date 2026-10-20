import { EditorPreview } from './preview'

/**
 * Implements various functions for web pages Datatable Editor - bindings, buttons, listeners etc.
 */
export class WebPagesDatatable {

    options = null;
    webpagesDatatable = null;
    #webpagesDatatableEventsBinded = false;
    #httpHeadersRequest = null;
    #mediaTable = null;
    #plannedRequest = null;

    constructor(options) {
        var defaultTitleKey = "editor.newDocumentName";

        this.options = window.$.extend({}, {
            serverSide: true,
            tabs: null,
            editorId: "docId",
            fetchOnCreate: true,
            fetchOnEdit: true,
            perms: {
                create: 'addPage',
                edit: 'pageSave',
                duplicate: 'pageSaveAs',
                remove: 'deletePage'
            },
            editorButtons: [
                {
                    text: '<i class="ti ti-check"></i> ' + WJ.translate('button.save'),
                    action: function() {
                        //console.log("SAVING, this=", this);
                        let close = true;
                        if ($("#webpagesSaveCheckbox").is(":checked") && "main"===this.s.mode) close = false;
                        //console.log("this=", this);

                        if (close) {
                            this.submit();
                        } else {
                            //action musime zachovat, lebo to editor pri ulozeni zmaze
                            let editorAction = this.s.action;
                            //console.log("editorAction=", editorAction);
                            let editor = this;
                            this.submit(
                                function() {
                                    editor.s.action = editorAction;
                                },
                                null,
                                null,
                                close
                            );
                        }
                    }
                }
            ],
            newPageTitleKey: defaultTitleKey,
            showPageTitleKey: "history.showPage",
            onClose: function(TABLE, EDITOR, e) {
                try {
                    var isDirty = EDITOR.field("data").isDirty();
                    //console.log("isDirty=", isDirty);
                    if (isDirty && window.confirm(WJ.translate('editor.confirmExitMessage'))==false) {
                        //do not close dialog
                        return false;
                    }
                } catch (err) {
                    console.log("Error checking CKEditor dirty state:", err);
                }
                return true;
            }
        }, options);
        //console.log("WebPagesDatatable.options=", this.options);

        if (this.options.newPageTitleKey != defaultTitleKey) {
            this.options.url = WJ.urlAddParam(this.options.url, "newPageTitleKey", this.options.newPageTitleKey);
        }
    }

    createDatatable() {
        if (this.options.tabs == null) this.options.tabs = WebPagesDatatable.getEditorTabs();
        this.#updateColumns(this.options.columns);
        this.webpagesDatatable = window.WJ.DataTable(this.options);
        this.#bindAll();

        //spravanie tlacidla nahlad
        let preview = new EditorPreview({
            datatable: this.webpagesDatatable
        });
        preview.bindEvents();

        //we need this as static function to click on toastr notify bubble
        var self = this;
        window.editFromHistory = function(docId, historyId) {
            self.#editFromHistory(docId, historyId);
        }

        return this.webpagesDatatable;
    }

    getDatatable() {
        return this.webpagesDatatable;
    }

    static getEditorTabs() {
        var tabs = [
            { id: 'content', title: WJ.translate('editor.tab.editor'), selected: true, content: '' },
            { id: 'basic', title: WJ.translate('editor.tab.basic_info') },
            { id: 'template', title: WJ.translate('editor.tab.template') },
            { id: 'menu', title: WJ.translate('editor.tab.navigation') },
            { id: 'access', title: WJ.translate('editor.tab.permissions') },
            { id: 'perex', title: WJ.translate('editor.tab.public') },
            { id: 'media', title: WJ.translate('editor.tab.media'), content: '<div class="mediaContentPlaceholder"><div>' },
            { id: 'fields', title: WJ.translate('editor.tab.fields') },
            { id: 'attributes', title: WJ.translate('editor.tab.atributes') },
            { id: 'history', title: WJ.translate('editor.tab.history'), content: '<div class="historyContentPlaceholder"><div>' }
        ];
        return tabs;
    }

    #bindAll() {
        this.#innerTableInitialized();
        this.#setButtons();
        this.#bindOpen();
    }

    #updateColumns(webpageColumns) {
        if (window.perexGroupsRenderAsSelect===true) {
            window.WJ.DataTable.mergeColumns(webpageColumns, {
                name: "perexGroups",
                renderFormat: "dt-format-text",
                "editor" : {
                    type : "select",
                    multiple: true,
                    attr: {
                        "data-multiple-separator": "\x0a\x0d"
                    }
                }
            });
        }
    }

    #innerTableInitialized() {
        //MEDIA - nastavenie handlera pre vyber linky
        let linkChangeHandlerSet = false;
        let instance = this;
        window.addEventListener("WJ.DTE.innerTableInitialized", function(event) {
            //console.log("DTE initialized, event=", event);
            if (event.detail.conf.id=="DTE_Field_editorFields.media") {
                let mediaTable = event.detail.conf.datatable;
                instance.#mediaTable = mediaTable;
                event.detail.conf.datatable.EDITOR.on( 'open', function( e, mode, action ) {
                    //console.log("DTED inner table opened");
                    //pri prvom nacitani nastav on change listener na reload dat
                    if (linkChangeHandlerSet == false) {
                        let input = $(mediaTable.EDITOR.node("mediaLink")).find("input.form-control")[0];
                        $(input).on("change", function() {
                            let val = $(mediaTable.EDITOR.node("mediaLink")).find("input.form-control").val();
                            //console.log("ZMENA, val=", val);
                            if (val == "") return;

                            //ziskaj z linky meno suboru a skus pouzit ako nazov
                            let i = val.lastIndexOf("/");
                            let j = val.lastIndexOf(".");
                            if (i>0 && j>i) {
                                val = val.substring(i+1, j);
                                val = val.replace(/[-_]/gi, " ");
                            }
                            if ($("#DTE_Field_mediaTitleSk").val()=="") {
                                //console.log("Setting val=", val);
                                $("#DTE_Field_mediaTitleSk").val(val);
                            }
                        });
                    }
                });
            }
            if (event.detail.conf.id=="DTE_Field_editorFields.history") {
                //console.log("Setting history table")
                let historyTable = event.detail.conf.datatable;
                historyTable.hideButton("create");
                historyTable.hideButton("edit");
                historyTable.hideButton("duplicate");
                historyTable.hideButton("remove");
                historyTable.hideButton("celledit");
                historyTable.hideButton("import");
                historyTable.hideButton("export");

                let buttonCounter = 1;
                //edit
                historyTable.button().add(buttonCounter++, {
                    text: '<i class="ti ti-pencil"></i>',
                    action: function (e, dt, node) {
                        //console.log("btn, e=",e,"dt=",dt,"node=",node);
                        //ziskaj data selectnuteho riadku
                        let selectedRows = dt.rows({ selected: true }).data();
                        //console.log("selectedRows=", selectedRows);
                        if (selectedRows.length>0) {
                            let row = selectedRows[0];
                            instance.#editFromHistory(row.docId, row.id);
                        }
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
                    },
                    className: 'btn btn-warning buttons-history-edit',
                    attr: {
                        'title': window.WJ.translate('history.editPage'),
                        'data-toggle': 'tooltip'
                    }
                });
                //delete
                historyTable.button().add(buttonCounter++, {
                    extend: "remove",
                    text: "<i class='ti ti-trash'></i>",
                    editor: historyTable.EDITOR,
                    formMessage: function (editor, dt) {
                        return $.fn.dataTable.ext.buttons.remove.formMessage(editor, dt)
                            + "<p>" + WJ.translate("editor.history.delete.confirm.js") + "</p>";
                    },
                    init: function ( dt, node, config ) {
                        var button = this;
                        dt.on('select.dt.DT deselect.dt.DT', function () {
                            let show = true;
                            let count = 0;
                            dt.rows({selected: true}).every( function ( rowIdx, tableLoop, rowLoop ) {
                                var data = this.data();
                                if (data.deleteAllowed !== true) show = false;
                                count ++;
                            });
                            //console.log("show=", show, "count=", count);
                            button.enable(count > 0 && show===true);
                        });
                        button.disable();
                    },
                    className: 'btn buttons-history-remove btn-danger buttons-divider',
                    attr: {
                        'title': window.WJ.translate('button.delete'),
                        'data-toggle': 'tooltip',
                        "data-dtbtn": "remove"
                    }
                });
                //preview
                historyTable.button().add(buttonCounter++, {
                    text: '<i class="ti ti-eye"></i>',
                    action: function (e, dt, node) {
                        //console.log("btn, e=",e,"dt=",dt,"node=",node);
                        //ziskaj data selectnuteho riadku
                        let selectedRows = dt.rows({ selected: true }).data();
                        //console.log("selectedRows=", selectedRows);
                        for (let i=0; i<selectedRows.length; i++) {
                            let row = selectedRows[i];
                            window.open("/showdoc.do?docid="+row.docId+"&historyid="+row.id);
                        }
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                    },
                    className: 'btn btn-outline-secondary buttons-history-preview',
                    attr: {
                        'title': window.WJ.translate(instance.options.showPageTitleKey),
                        'data-toggle': 'tooltip'
                    }
                });
                //compare
                historyTable.button().add(buttonCounter++, {
                    text: '<i class="ti ti-git-compare"></i>',
                    action: function (e, dt, node) {
                        //console.log("btn, e=",e,"dt=",dt,"node=",node);
                        //ziskaj data selectnuteho riadku
                        let selectedRows = dt.rows({ selected: true }).data();
                        //console.log("selectedRows=", selectedRows);
                        for (let i=0; i<selectedRows.length; i++) {
                            let row = selectedRows[i];
                            window.open("/admin/doc_compare.jsp?historyid="+row.id+"&docid="+row.docId);
                        }
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                    },
                    className: 'btn btn-outline-secondary buttons-divider buttons-history-compare',
                    attr: {
                        'title': window.WJ.translate('groupslist.compare'),
                        'data-toggle': 'tooltip'
                    }
                });

                historyTable.on( 'draw', function () {
                    historyTable.rows().every(function() {
                        var data = this.data();
                        if (data.publicable === false && data.publishStartStringExtra != null && data.publishStartStringExtra != "") {
                            //only if it is in future
                            if (Date.now() < data.publishStartStringExtra || data.syncStatus == 2) {
                                $(this.node()).addClass('not-publicable');
                            }
                        }
                    });
                });
            }
            if (event.detail.conf.id=="DTE_Field_editorFields.groupSchedulerPlannedChanges" ||
                    event.detail.conf.id=="DTE_Field_editorFields.groupSchedulerChangeHistory") {
                let historyTable = event.detail.conf.datatable;

                let buttonCounter = 1;
                //edit
                historyTable.button().add(buttonCounter++, {
                    text: '<i class="ti ti-pencil"></i>',
                    action: function (e, dt, node) {
                        //console.log("btn, e=",e,"dt=",dt,"node=",node);
                        //ziskaj data selectnuteho riadku
                        let selectedRows = dt.rows({ selected: true }).data();
                        //console.log("selectedRows=", selectedRows);
                        if (selectedRows.length>0) {
                            let row = selectedRows[0];
                            //HACK, potrebujeme ziskat dany zaznam ako JSON a podvrhnut ho do datatabulky
                            $.ajax({
                                url: "/admin/rest/groups/"+row.groupId+"?schedulerId="+row.id,
                                method: "GET",
                                success: function(json) {
                                    //console.log("Edit JSON", json);
                                    let oldJson = groupsDatatable.row("#"+row.groupId).data();
                                    groupsDatatable.row("#"+row.groupId).data(json);
                                    groupsDatatable.EDITOR.setJson(json);
                                    groupsDatatable.wjEdit(groupsDatatable.row("#"+row.groupId));
                                    setTimeout(function() {
                                         //console.log("returning oldJson=", oldJson);
                                         groupsDatatable.row("#"+row.groupId).data(oldJson);
                                     }, 100);
                                }
                            })
                        }
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfOneRowSelected(this, dt);
                    },
                    className: 'btn btn-warning buttons-history-edit buttons-divider',
                    attr: {
                        'title': window.WJ.translate('groupslist.edit_dir'),
                        'data-toggle': 'tooltip'
                    }
                });
            }
        });
    }

    #editFromHistory(docId, historyId) {
        var self = this;
        //potrebujeme ziskat dany zaznam ako JSON a podvrhnut ho do datatabulky
        $.ajax({
            url: "/admin/rest/web-pages/"+docId+"?historyId="+historyId,
            method: "GET",
            success: function(json) {
                //console.log("Edit JSON", json);

                //v bubble editacii neotvorme editor, len nastavme data
                if ("bubble"!==self.webpagesDatatable.EDITOR.s.mode) {
                    //console.log("Opening editor for history edit, data=", self.webpagesDatatable.row("#"+docId));
                    //this must be before set data to first set CSS styles in editor
                    webpagesDatatable.EDITOR.field("data").setJson(json);
                }

                // this will set ALL input fields from JSON value
                // it is different function as webpagesDatatable.EDITOR.field("data").setJson(json)
                self.webpagesDatatable.EDITOR.setJson(json);
                self.#updatePlannedMode(true);

                // Clear the visible tooltip and pending hover/focus timers before hiding History.
                const historyEditButton = $("#pills-dt-datatableInit-history .buttons-history-edit");
                historyEditButton.tooltip("dispose").removeAttr("aria-describedby");
                WJ.initTooltip(historyEditButton);

                //show content tab
                $("#pills-dt-datatableInit-content-tab").trigger("click");

            }
        })
    }

    #setButtons() {
        var self = this;
        let buttonCounter = 4;
        //preview page
        this.webpagesDatatable.button().add(buttonCounter++, {
            text: '<i class="ti ti-eye"></i>',
            action: function (e, dt, node) {
                let selectedRows = dt.rows({ selected: true }).data();
                for (let i=0; i<selectedRows.length; i++) {
                    let row = selectedRows[i];
                    if($("#pills-waiting-tab").hasClass("active")) {
                        //pre schvalovanie mame otocene docId a historyId
                        window.open("/showdoc.do?docid="+row.historyId+"&historyId="+row.docId);
                    } else {
                        window.open("/showdoc.do?docid="+row.docId);
                    }
                }
            },
            init: function ( dt, node, config ) {
                $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
            },
            className: 'btn btn-outline-secondary buttons-history-preview',
            attr: {
                'title': window.WJ.translate(self.options.showPageTitleKey),
                'data-toggle': 'tooltip'
            }
        });

        if (WJ.hasPermission("cmp_abtesting")) {
            //ab test
            this.webpagesDatatable.button().add(buttonCounter++, {
                text: '<i class="ti ti-a-b"></i>',
                action: function (e, dt, node) {
                    self.webpagesDatatable.executeAction("saveAsAbTest", true, window.WJ.translate("editor.save_as_abtest.confirm.title"), window.WJ.translate("editor.save_as_abtest.confirm.text"));
                },
                init: function ( dt, node, config ) {
                    $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                },
                className: 'btn btn-outline-secondary buttons-abtest',
                attr: {
                    'title': window.WJ.translate('editor.save_as_abtest'),
                    'data-toggle': 'tooltip',
                    'data-dtbtn': 'abtest'
                }
            });
        }

        if (WJ.hasPermission("cmp_stat")) {
            //show stat
            this.webpagesDatatable.button().add(buttonCounter++, {
                text: '<i class="ti ti-chart-line"></i>',
                action: function (e, dt, node) {
                    let selectedRows = dt.rows({ selected: true }).data();
                    for (let i=0; i<selectedRows.length; i++) {
                        let row = selectedRows[i];
                        if($("#pills-waiting-tab").hasClass("active")) {
                            //pre schvalovanie mame otocene docId a historyId
                            window.open("/apps/stat/admin/top-details/?docId="+row.historyId+"&dateRange=");
                        } else {
                            window.open("/apps/stat/admin/top-details/?docId="+row.docId+"&dateRange=");
                        }
                    }
                },
                init: function ( dt, node, config ) {
                    $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                },
                className: 'btn btn-outline-secondary buttons-stat',
                attr: {
                    'title': window.WJ.translate('stat_doc.pageStat'),
                    'data-toggle': 'tooltip',
                    'data-dtbtn': 'stat'
                }
            });
        }

        if (typeof window.webpagesDatatableGroupId !== "undefined") {
            //link check button
            this.webpagesDatatable.button().add(buttonCounter++, {
                text: '<i class="ti ti-external-link"></i>',
                action: function (e, dt, node) {
                    window.location.href="/admin/v9/webpages/linkcheck/?groupId="+webpagesDatatableGroupId;
                },
                init: function ( dt, node, config ) {
                    let button = this;
                    button.disable();
                    //zatial som to lepsie nevedel vymysliet
                    setInterval(function() {
                        //button je disabled ak nemame nastavene ziadne groupId
                        //console.log("webpagesDatatableGroupId=", webpagesDatatableGroupId);
                        if (window.webpagesDatatableGroupId > 0) button.enable();
                        else button.disable();
                    }, 500);
                },
                className: 'btn btn-outline-secondary buttons-linkcheck',
                attr: {
                    'title': window.WJ.translate('web_pages_list.link_check_button'),
                    'data-toggle': 'tooltip'
                }
            });
        }

        if (typeof window.recursive !== "undefined") {
            //prepinac zobrazenia stranok z podadresarov
            this.webpagesDatatable.button().add(buttonCounter++, {
                tag: "div",
                text: ` <input type="checkbox" class="form-check-input" id="dtRecursiveSwitch" value="true" aria-label="${window.WJ.translate('datatable.show_all_subfolders.js')}"></input>
                        <label class="form-check-label is-icon-arrows-v" for="dtRecursiveSwitch"></label>`,
                className: 'custom-control form-switch buttons-recursive buttons-divider-left',
                attr: {
                    'title': window.WJ.translate('datatable.show_all_subfolders.js'),
                    'data-toggle': 'tooltip',
                    'data-dtbtn': 'showsubfolders',
                    'role': 'group',
                },
                action: function (e, node, el) {
                    if ($(el).hasClass("enabled")) {
                        window.recursive = false;
                        $(el).removeClass("enabled");
                    } else {
                        window.recursive = true;
                        $(el).addClass("enabled");
                    }

                    self.webpagesDatatable.setAjaxUrl(WJ.urlAddParam(returnActualPathWithoutRecursionParam(), "recursive", window.recursive));
                    self.webpagesDatatable.ajax.reload();
                }
            });
        }

        this.webpagesDatatable.hideButton("import");
        this.webpagesDatatable.hideButton("export");
        if (typeof window.lastGroup != "undefined") {
            //povodny import/export stranok
            this.webpagesDatatable.button().add(null, {
                text: '<i class="ti ti-download"></i>',
                action: function (e, dt, node) {
                    let groupId = 0;
                    if (typeof window.lastGroup != "undefined" && window.lastGroup != null && typeof window.lastGroup.groupId != "undefined") {
                        groupId = window.lastGroup.groupId;
                    }
                    //this apend on load where lastGroup is not initialized
                    if (typeof window.lastGroup != "undefined" && window.lastGroup == null && window.selectedNode != null) {
                        if (typeof window.selectedNode.groupDetails != "undefined") groupId = window.selectedNode.groupDetails.groupId;
                    }
                    WJ.openPopupDialog("/components/import_web_pages/import_pages.jsp?groupId="+groupId);
                },
                init: function ( dt, node, config ) {
                    let button = this;
                    button.disable();
                    //zatial som to lepsie nevedel vymysliet
                    setInterval(function() {
                        //button je disabled ak nemame nastavene ziadne groupId
                        let groupId = 0;
                        if (typeof window.lastGroup != "undefined" && window.lastGroup != null && typeof window.lastGroup.groupId != "undefined") {
                            groupId = window.lastGroup.groupId;
                        }
                        //this apend on load where lastGroup is not initialized
                        if (typeof window.lastGroup != "undefined" && window.lastGroup == null && window.selectedNode != null) {
                            if (typeof window.selectedNode.groupDetails != "undefined") groupId = window.selectedNode.groupDetails.groupId;
                        }
                        if (groupId > 0) button.enable();
                        else button.disable();
                    }, 500);
                },
                className: 'btn btn-outline-secondary buttons-divider buttons-import-export buttons-right',
                attr: {
                    'title': window.WJ.translate('components.import_web_pages.menu'),
                    'data-toggle': 'tooltip',
                    'data-dtbtn': 'wjimportexport'
                }
            });
        }
    }

    /** Adds the header preview once and loads it only while the Basic tab is visible. */
    #bindHttpHeaders() {
        const modal = $("#" + this.webpagesDatatable.DATA.id + "_modal");
        const basic = modal.find("div[id$='-basic'].tab-pane");
        if (basic.find(".webpage-http-headers").length === 0) {
            const section = $(`<div class="DTE_Field form-group row webpage-http-headers">
                <div class="col-sm-4"></div>
                <div class="col-sm-7"><h5 class="mb-3"></h5><pre class="mb-0" aria-live="polite"></pre></div>
                <div class="col-sm-1 form-group-tooltip"><button type="button" class="btn btn-link" data-toggle="tooltip"><i class="ti ti-info-circle" aria-hidden="true"></i></button></div>
            </div>`);
            section.find("h5").text(WJ.translate("editor.http_headers.js"));
            section.find("button").attr({ title: WJ.translate("editor.http_headers.tooltip.js"), "aria-label": WJ.translate("editor.http_headers.js") });
            $(this.webpagesDatatable.EDITOR.field("cacheable").node()).after(section);
            WJ.initTooltip(section.find("button"));
        }
        basic.find(".webpage-http-headers pre").text(WJ.translate("editor.http_headers.loading.js"));
        modal.find("[id$='-basic-tab']").off("shown.bs.tab.httpHeaders").on("shown.bs.tab.httpHeaders", () => this.#updateHttpHeaders());
        basic.off("change.httpHeaders").on("change.httpHeaders", "input, select", () => this.#updateHttpHeaders());
        this.webpagesDatatable.EDITOR.off("close.httpHeaders").on("close.httpHeaders", () => this.#httpHeadersRequest?.abort());
        this.#updateHttpHeaders();
    }

    /** Sends current editor values to the shared backend header calculation. */
    #updateHttpHeaders() {
        const output = $("#" + this.webpagesDatatable.DATA.id + "_modal .webpage-http-headers pre");
        if (!output.is(":visible")) return;
        this.#httpHeadersRequest?.abort();
        output.attr("aria-busy", "true");
        const editor = this.webpagesDatatable.EDITOR;
        const values = editor.get(["title", "navbar", "tempId", "virtualPath", "editorVirtualPath", "generateUrlFromTitle", "urlInheritGroup", "searchable", "followLinks", "externalLink"]);
        values.groupId = editor.get("editorFields.groupDetails").groupId;
        values.docId = editor.currentJson?.docId ?? -1;
        const json = JSON.stringify(values, (key, value) => {
            if (Array.isArray(value) && value.length === 1 && [true, false, "true", "false"].includes(value[0])) return value[0] === true || value[0] === "true";
            return value;
        });
        this.#httpHeadersRequest = $.ajax({
            url: WJ.urlAddPath(this.webpagesDatatable.DATA.url, "/http-headers"), method: "POST", contentType: "application/json", data: json
        }).done(headers => {
            const names = ["X-Robots-Tag", "Referrer-Policy"];
            if (headers.Location != null) names.push("Location");
            output.text(names.map(name => name + ": " + (headers[name] ?? WJ.translate("editor.http_headers.not_sent.js"))).join("\n"));
        }).fail((xhr, status) => {
            if (status !== "abort") output.text(WJ.translate("editor.http_headers.error.js"));
        }).always(() => {
            output.removeAttr("aria-busy");
        });
    }

    #bindOpen() {
        var self = this;
        this.webpagesDatatable.EDITOR.on('opened', function (e, type, action) {
            //bindni eventy, DETD je aj tak stale otvoreny, takze sa tento event viac krat nevola
            if (self.#webpagesDatatableEventsBinded == false) {
                //console.log("Binding webpages DTED events");
                self.#webpagesDatatableEventsBinded = true;

                var newPageTitle = WJ.translate(self.options.newPageTitleKey);
                //navbar nastaveny podla title
                $("#DTE_Field_title").on("focus", function() {
                    if ($("#DTE_Field_title").val()==newPageTitle) {
                        $("#DTE_Field_title").val("");
                    }
                });
                $("#DTE_Field_title").on("blur", function() {
                    if ($("#DTE_Field_title").val()=="") {
                        $("#DTE_Field_title").val(newPageTitle);
                    }

                    if ($("#DTE_Field_navbar").val()=="" || $("#DTE_Field_navbar").val()==newPageTitle) {
                        if ($("#DTE_Field_navbar").val()==newPageTitle) {
                            //zresetuj aj URL, aby sa znova vygenerovalo
                            $("#DTE_Field_virtualPath").val("");
                        }
                        $("#DTE_Field_navbar").val($("#DTE_Field_title").val());
                    }
                });
                $("#DTE_Field_urlInheritGroup_0").on("change", function() { self.#showHideUrlAddress("urlInheritGroup"); });
                $("#DTE_Field_generateUrlFromTitle_0").on("change", function() { self.#showHideUrlAddress("generateUrlFromTitle"); });

                $("#DTE_Field_editorFields-publishAfterStart_0").on("change", function() { self.#publishAfterStartChanged(); self.#updatePlannedMode(true); });
            }

            self.#showHideUrlAddress();
            self.#publishAfterStartChanged();
            self.#bindHttpHeaders();
            self.#updatePlannedMode(true);
        });
        this.webpagesDatatable.EDITOR.on('open', function (e, mode, action) {
            //console.log("Open, e=", e, "mode=", mode, "action=", action);
            if ("main"===mode && (
                    ("create"===action && self.webpagesDatatable.hasPermission("create")) ||
                    ("edit"===action && self.webpagesDatatable.hasPermission("edit"))
                    )
                ) {
                //pridaj pole pre ulozenie stranky
                let checkboxHtml = `
                    <div class="form-check edit-append">
                        <input class="form-check-input" type="checkbox" value="" id="webpagesSaveCheckbox">
                        <label class="form-check-label" for="webpagesSaveCheckbox">
                            ${WJ.translate('editor.saveWorkCopy.js')}
                            <span role="img" data-toggle="tooltip" title="${WJ.translate('editor.saveWorkCopy.tooltip.js')}"><i class="ti ti-info-circle"></i></span>
                        </label>
                    </div>
                `;
                $('#'+self.webpagesDatatable.DATA.id+'_modal div.DTE_Form_Buttons button.btn-close-editor').after(checkboxHtml)
            }

            WJ.initTooltip($('#'+self.webpagesDatatable.DATA.id+'_modal div.DTE_Form_Buttons [data-toggle*="tooltip"]'));

            self.#showHideUrlAddress();
        });
        this.webpagesDatatable.EDITOR.on( 'initSubmit', function (e, action) {
            if (self.#plannedRequest !== null) return false;
            let isChecked = $("#webpagesSaveCheckbox").is(':checked');
            self.webpagesDatatable.EDITOR.field("editorFields.requestPublish").val(!isChecked);
            //console.log("initSubmit, checked=", isChecked, "editorFields.requestPublish=", webpagesDatatable.EDITOR.field("editorFields.requestPublish").val())
        });

        window.addEventListener("WJ.DTE.opened", function(e) {
            if (self.webpagesDatatable.DATA.id===e.detail.id) {
                if (self.webpagesDatatable.EDITOR.currentJson?.docId === -1) {
                    if ($("#pills-dt-"+self.webpagesDatatable.DATA.id+"-content-tab").hasClass("active")) {
                        //je to nova stranka, prepni sa na kartu Zakladne
                        setTimeout(()=> {
                            $("#pills-dt-"+self.webpagesDatatable.DATA.id+"-basic-tab").trigger("click");
                            setTimeout(()=> {
                                $("#"+self.webpagesDatatable.DATA.id+"_modal #DTE_Field_title").focus();
                            }, 200);
                        }, 700);
                    }
                }
                //pri duplikovani zmazme niektore hodnoty
                setTimeout(()=> {
                    if ($("#"+self.webpagesDatatable.DATA.id+"_modal").attr("data-action")=="duplicate") {
                        $("#pills-dt-"+self.webpagesDatatable.DATA.id+"-basic-tab").trigger("click");
                        $("#DTE_Field_navbar").val("");
                        $("#DTE_Field_virtualPath").val("");
                        $("#DTE_Field_sortPriority").val("-1");
                    }
                }, 700);

                //bug: edit - celledit - edit = virtualPath bad positioning, fix to flex
                var display = $("div.DTE_Field_Name_virtualPath").css("display");
                //console.log("display=", display);
                if (typeof display != undefined && "block"==display) {
                    $("div.DTE_Field_Name_virtualPath").css("display", "flex");
                }
            }
        });
    }

    #showHideUrlAddress(button) {
        var self = this;
        var urlInheritGroup = $("#DTE_Field_urlInheritGroup_0").is(":checked");
        var generateUrlFromTitle = $("#DTE_Field_generateUrlFromTitle_0").is(":checked");

        //console.log("showHideUrlAddress, urlInheritGroup=", urlInheritGroup, "generateUrlFromTitle=", generateUrlFromTitle);
        var labelElement = $("div.DTE_Field_Name_editorVirtualPath div.DTE_Field_InputControl span.baseurl");
        if (labelElement.length == 0) {
            $("div.DTE_Field_Name_editorVirtualPath div.DTE_Field_InputControl").prepend("<span class=\"baseurl\"></span>");
            labelElement = $("div.DTE_Field_Name_editorVirtualPath div.DTE_Field_InputControl span.baseurl");
        }
        labelElement.hide();
        $("#DTE_Field_virtualPath").attr("disabled", null)

        if (urlInheritGroup === false && generateUrlFromTitle === false) {
            this.webpagesDatatable.EDITOR.field("virtualPath").show();
            this.webpagesDatatable.EDITOR.field("editorVirtualPath").hide();
            return;
        }
        if (urlInheritGroup === true && generateUrlFromTitle === true) {
            //obe nesmu byt zapnute
            if (typeof button != "undefined" && "urlInheritGroup"===button) {
                $("#DTE_Field_generateUrlFromTitle_0").click();
                generateUrlFromTitle = false;
            } else {
                $("#DTE_Field_urlInheritGroup_0").click();
                urlInheritGroup = false;
            }
        }
        if (urlInheritGroup === true) {
            this.webpagesDatatable.EDITOR.field("virtualPath").hide();
            this.webpagesDatatable.EDITOR.field("editorVirtualPath").show();

            //ak je prazdne nastav
            if (""===this.webpagesDatatable.EDITOR.field("editorVirtualPath").val()) {
                var virtualPath = this.webpagesDatatable.EDITOR.field("virtualPath").val();
                if (""!==virtualPath) {
                    var virtualPathArr = virtualPath.split("/");
                    if (virtualPathArr.length>0) self.webpagesDatatable.EDITOR.field("editorVirtualPath").val(virtualPathArr[virtualPathArr.length-1]);
                }
            }

            //zobrazit label s prefixom virtualPath
            labelElement.show();
            var virtualPath = this.webpagesDatatable.EDITOR.field("virtualPath").val();
            if (""!==virtualPath) {
                var i = virtualPath.lastIndexOf("/");
                if (i > 0) {
                    labelElement.html(virtualPath.substring(0, i+1));
                }
            }
        } else if (generateUrlFromTitle === true) {
            this.webpagesDatatable.EDITOR.field("virtualPath").show();
            this.webpagesDatatable.EDITOR.field("editorVirtualPath").hide();
            $("#DTE_Field_virtualPath").attr("disabled", "disabled");
        }
    }

    #updatePlannedMode(initialize) {
        const editor = this.webpagesDatatable.EDITOR;
        const json = editor.currentJson;
        const modal = $("#" + this.webpagesDatatable.DATA.id + "_modal");
        modal.find(".planned-change-notice").remove();
        if (!json?.editorFields?.plannedPublishingEnabled) return;
        const selected = $("#DTE_Field_editorFields-publishAfterStart_0").is(":checked");
        if (json.docId < 1) {
            if (selected) {
                editor.field("editorFields.publishAfterStart").val(false);
                this.#publishAfterStartChanged();
                WJ.confirm({
                    title: WJ.translate("planned.saveFirst.js"),
                    message: WJ.translate("planned.saveFirst.message.js"),
                    btnOkText: WJ.translate("planned.saveFirst.button.js"),
                    success: () => {
                        if (editor.currentJson === json && editor.display()) this.#saveNewPageForPlanning();
                    }
                });
            }
            return;
        }
        const notice = $('<div class="alert alert-warning planned-change-notice mt-2 mb-0" role="status"></div>');
        const perex = $(editor.node("editorFields.publishAfterStart"));
        perex.find('[data-dte-e="input"]').append(notice);
        notice.append($("<p class='mb-2'></p>").text(WJ.translate("planned.warning.js")));
        const active = json.editorFields.plannedHistoryId;
        if (active > 0 && Number(active) !== Number(json.historyId)) {
            $("<button type='button' class='btn btn-outline-secondary btn-sm me-2'></button>")
                .text(WJ.translate("planned.open.js")).appendTo(notice)
                .on("click", () => this.#editFromHistory(json.docId, active));
        }
        $("<button type='button' class='btn btn-outline-danger btn-sm'></button>")
            .text(WJ.translate("planned.cancel.js")).appendTo(notice).on("click", () => {
                if (this.#plannedRequest !== null || json.docId < 1) return;
                this.#plannedRequest = $.ajax({url: `/admin/rest/web-pages/planned/${json.docId}/cancel`, method: "POST"})
                    .done(() => this.#editFromHistory(json.docId, -1))
                    .fail(() => WJ.notifyError(WJ.translate("planned.error.js")))
                    .always(() => { this.#plannedRequest = null; });
            });
        if (selected && initialize) {
            if (this.#plannedRequest !== null) return;
            this.#plannedRequest = $.ajax({url: `/admin/rest/web-pages/planned/${json.docId}/initialize`, method: "POST"})
                .done(result => {
                    if (result.historyId > 0 && Number(result.historyId) !== Number(json.historyId)) {
                        json.editorFields.plannedHistoryId = result.historyId;
                        editor.field("editorFields.publishAfterStart").val(false);
                        WJ.notifyError(WJ.translate("planned.openExisting.js"));
                        this.#updatePlannedMode(false);
                        return;
                    }
                    this.#setPlannedMedia(true);
                })
                .fail(() => {
                    editor.field("editorFields.publishAfterStart").val(false);
                    WJ.notifyError(WJ.translate("planned.error.js"));
                })
                .always(() => { this.#plannedRequest = null; this.#publishAfterStartChanged(); });
        } else {
            this.#setPlannedMedia(selected);
        }
        if (!selected && !active) notice.hide();
    }

    /** Saves a new unpublished page, then reopens the editor with its persistent ID before initializing the plan. */
    #saveNewPageForPlanning() {
        const table = this.webpagesDatatable;
        const editor = table.EDITOR;
        if (!editor.currentJson?.editorFields?.plannedPublishingEnabled || editor.currentJson.docId > 0 || editor.mode() !== "create") return;

        editor.submit(json => {
            const saved = json.data[0];
            const modal = $("#" + table.DATA.id + "_modal");
            const reopen = () => {
                // Reload first so the create response's table redraw cannot replace the row being edited.
                table.ajax.reload(() => {
                    saved.editorFields.publishAfterStart = true;
                    let row = table.row("#" + saved.docId);
                    if (row.any()) row.data(saved);
                    else row = table.row.add(saved);
                    modal.one("shown.bs.modal.plannedNewPage", () => {
                        $("#pills-dt-" + table.DATA.id + "-perex-tab").trigger("click");
                    });
                    table.wjEdit(row.index());
                }, false);
            };
            // The submit callback runs after closing has started; wait for both submission and the modal transition.
            editor.one("submitComplete.plannedReopen", () => {
                if (modal.attr("data-dte-close-state") === "closed") reopen();
                else modal.one("hidden.bs.modal.plannedNewPage", reopen);
            });
        }, null, data => {
            // Change only the submitted snapshot so validation failures leave the form intact.
            for (const page of Object.values(data.data)) {
                page.available = false;
                page.editorFields.requestPublish = false;
                page.editorFields.publishAfterStart = false;
            }
        });
    }

    #setPlannedMedia(planned) {
        const editor = this.webpagesDatatable.EDITOR;
        editor.field("editorFields.plannedMedia").val(planned);
        $("#webpagesSaveCheckbox").prop("disabled", planned).prop("checked", false);
        const tab = $("#pills-dt-" + this.webpagesDatatable.DATA.id + "-media-tab");
        tab.text(WJ.translate(planned ? "planned.media.js" : "editor.tab.media"));
        if (this.#mediaTable) {
            const json = editor.currentJson;
            const url = `/admin/rest/media?docId=${json.docId}&groupId=${json.groupId}&planned=${planned}`;
            this.#mediaTable.rows().deselect();
            this.#mediaTable.setAjaxUrl(url);
            this.#mediaTable.DATA.url = url;
            this.#mediaTable.EDITOR.ajax().url = WJ.urlAddPath(url, '/editor');
            this.#mediaTable.ajax.reload();
        }
    }

    #publishAfterStartChanged() {
        var publishAfterStart = $("#DTE_Field_editorFields-publishAfterStart_0").is(":checked");
        var primaryButton= $("#" + this.webpagesDatatable.DATA.id + "_modal .DTE_Footer .DTE_Form_Buttons button.btn-primary");
        //it's not loaded on second open, but it is reseted by DTE, so we need to check the button value
        if (primaryButton.length == 0) return;
        //set editor submit button to save or plan according to checkbox state
        var currentHtml = primaryButton.html();
        if (typeof currentHtml == "undefined") return;

        //only change when is is save or schedule, it also can be delete or duplicate
        if (currentHtml.indexOf(WJ.translate("button.schedule"))>=0 || currentHtml.indexOf(WJ.translate('button.save'))>=0) {
            if (publishAfterStart === true) {
                primaryButton.html('<i class="ti ti-calendar-check"></i> ' + WJ.translate("button.schedule"));
            } else {
                primaryButton.html('<i class="ti ti-check"></i> ' + WJ.translate('button.save'));
            }
        }
    }

}
