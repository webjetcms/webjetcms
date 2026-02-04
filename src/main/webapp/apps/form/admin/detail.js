(function() {
    if (!window.FormDetail) {
        window.FormDetail = {};
    }

    /**
     * Initialize Form Detail view (normal or archived)
     * @param {Object} config
     * @param {Array} config.columns - Base columns from server (Thymeleaf injected)
     * @param {boolean} config.isArchived - Whether this is the archived detail view
     */
    window.FormDetail.init = function(config) {
        const isArchived = !!(config && config.isArchived);
        const baseColumns = (config && config.columns) ? config.columns : [];

        const columnsToKeep = ["id", "preview", "createDate", "lastExportDate", "note", "files", "formName"];
        const removeUserFields = ["admin", "apiKey", "password"];

        const queryString = window.location.search;
        const urlParams = new URLSearchParams(queryString);
        const formName = urlParams.get('formName');

        const baseUrl = isArchived ? "/admin/rest/forms/archive-list/" : "/admin/rest/forms-list/";
        let url = baseUrl;
        url = WJ.urlUpdateParam(url, "detail", true);
        url = WJ.urlUpdateParam(url, "formName", formName);

        // Title
        WJ.setTitle(WJ.translate("menu.forms"));

        // Header tabs
        WJ.headerTabs({
            id: 'tabsFilter',
            tabs: [
                { url: '/apps/form/admin/', title: WJ.translate('forms.formsList.js'), active: !isArchived },
                { url: '/apps/form/admin/form-steps/?formName=' + formName, title: WJ.translate('components.form_items.navbar_title.js'), active: false },
                { url: '/apps/form/admin/archived/', title: WJ.translate('forms.archiveList.js'), active: isArchived },
                { url: '/apps/form/admin/regexps/', title: WJ.translate('components.form.reg_exp.js'), active: false }
            ]
        });

        // Breadcrumb
        WJ.breadcrumb({
            id: isArchived ? "form-details-archived" : "form-details",
            tabs: [
                {
                    url: (isArchived ? "/apps/form/admin/archived/detail/?formName=" : "/apps/form/admin/detail/?formName=") + formName,
                    title: formName,
                    active: true
                }
            ],
            backlink: {
                url: isArchived ? "/apps/form/admin/archived/" : "/apps/form/admin/",
                title: WJ.translate("forms.formsList.js"),
            }
        });

        // Tabs in row editor
        const tabs = [
            { id: 'basic', title: WJ.translate('datatable.tab.basic'), selected: true },
            { id: 'content', title: WJ.translate('components.form_items.navbar_title.js'), selected: false },
            { id: 'personalInfo', title: WJ.translate('components.form.personal_info.js'), selected: false },
            { id: 'contactTab', title: WJ.translate('components.form.contact.js'), selected: false },
            { id: 'freeItems', title: WJ.translate('editor.tab.fields'), selected: false },
        ];

        // Provide HTML preview modal opener
        window.openFormHtml = function (id) {
            // add print button
            if (document.getElementById('modalPrintButton') == null) {
                const printButton = document.createElement('button');
                printButton.id = 'modalPrintButton';
                printButton.className = 'btn btn-outline-secondary';
                printButton.innerHTML = WJ.translate('button.print');
                printButton.onclick = function() {
                    const iframe = document.getElementById('modalIframeIframeElement');
                    if (iframe && iframe.contentWindow) iframe.contentWindow.print();
                };
                const footer = document.querySelector('#modalIframe .modal-footer');
                if (footer) footer.prepend(printButton);
            }

            WJ.openIframeModal({
                url: baseUrl + "html/?id=" + id,
                width: 800,
                height: 600,
                closeButtonPosition: 'close-button-over empty-header',
                buttonTitleKey: 'button.confirm',
                okclick: function() {
                    WJ.closeIframeModal();
                }
            });
        };

        let filteredColumns = [];
        const columnsFetchPromise = fetch(baseUrl + "columns/" + encodeURIComponent(formName), {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'X-CSRF-Token': window.csrfToken
            }
        })
        .then(resp => {
            if (!resp.ok) throw new Error('Failed to load form columns: ' + resp.status);
            return resp.json();
        })
        .then(data => {
            if (!data) return;

            // First filter on base columns
            filteredColumns = baseColumns.filter(col =>
                columnsToKeep.includes(col.data) ||
                (col.data && col.data.startsWith('userDetails.') && !removeUserFields.includes(col.data.split('.')[1]))
            );
            // Remove all userDetails.editorFields columns
            filteredColumns = filteredColumns.filter(col => !(col.data && col.data.startsWith('userDetails.editorFields.')));

            window.WJ.DataTable.mergeColumns(filteredColumns, {
                name: "formName",
                hidden: true
            });

            // Set createDate as new row editor column
            window.WJ.DataTable.mergeColumns(filteredColumns, {
                name: "createDate",
                className: "dt-row-edit cell-not-editable",
                renderFormatLinkTemplate: "javascript:",
                renderFormatPrefix: '<i class="ti ti-pencil"></i> '
            });

            // Set userId column as base text column
            window.WJ.DataTable.mergeColumns(filteredColumns, {
                name: "userDetails.id",
                renderFormat: "dt-format-text",
                searchable: false,
                filter: false,
                className: "cell-not-editable"
            });

            // Files renderer
            window.WJ.DataTable.mergeColumns(filteredColumns, {
                name: "files",
                render: function ( data ) {
                    let htmlCode = "";
                    if (data != null && data.length>0){
                        let fileNames = data.split(",");
                        for (const fileName of fileNames) {
                            if (htmlCode !== "") htmlCode += ", ";
                            let fileNameText = fileName;
                            let i = fileName.indexOf("_");
                            if (i>0) fileNameText = fileName.substring(i+1);
                            htmlCode += `<a href="/apps/forms/admin/attachment/?name=${fileName}" target="_blank">${fileNameText}</a>`;
                        }
                    }
                    return htmlCode;
                }
            });

            window.WJ.DataTable.mergeColumns(filteredColumns, {
                name: "note",
                renderFormat: "dt-format-text",
                className: "allow-html td-note-column",
            });

            // Double Opt-In extra column
            if (data.doubleOptIn === true) {
                filteredColumns.splice(6, 0, {
                    data: "doubleOptinConfirmationDate",
                    name: "doubleOptinConfirmationDate",
                    title: WJ.translate('formslist.doubleOptInDate.js'),
                    renderFormat: "dt-format-date-time",
                    orderable: true,
                    className: "cell-not-editable",
                    editor: {
                        type: "datetime",
                        attr: { disabled: true },
                        tab: "basic"
                    }
                });
            }

            // Add form fields as columns
            if (Array.isArray(data.columns)) {
                data.columns.forEach(col => {
                    filteredColumns.push({
                        data: "col_" + col.value,
                        name: "col_" + col.value,
                        title: col.label,
                        renderFormat: "dt-format-text",
                        orderable: false,
                        className: "cell-not-editable allow-html",
                        editor: { tab: "content" }
                    });
                });
            }

            // Disable columns (editor readonly)
            filteredColumns.forEach(col => {
                if (col.data === "userDetails.login") {
                    delete col.renderFormatLinkTemplate;
                    delete col.renderFormatPrefix;
                    col.hidden = false;
                    col.hiddenEditor = false;
                }
                if (col.data === "userDetails.id") {
                    col.editor.tab = "personalInfo";
                }
                if (col.data.indexOf("userDetails.") === 0) {
                    col.visible = false;

                    //export only columns which is possible to see in datatable
                    if (typeof col.hidden === "boolean" && col.hidden === true) {
                        col.className = (col.className ? col.className + " " : "") + " not-export";
                    }
                }
                if (col.data === "note") return;

                window.WJ.DataTable.mergeColumns(filteredColumns, {
                    name: col.data,
                    editor: { attr: { disabled: true } },
                    ai: null
                });
            });

            // Sort fields by editor.tab value
            filteredColumns.sort((a, b) => {
                const tabOrder = {
                    'basic': 1,
                    'content': 2,
                    'personalInfo': 3,
                    'contactTab': 4,
                    'freeItems': 5
                };
                const tabA = (a.editor && a.editor.tab) ? a.editor.tab : 'personalInfo';
                const tabB = (b.editor && b.editor.tab) ? b.editor.tab : 'personalInfo';
                return (tabOrder[tabA] || 99) - (tabOrder[tabB] || 99);
            });
        })
        .catch(err => { console.error(err); });

        function initFormDetailDataTable() {
            const order = [];
            order.push([2, 'desc']);

            const formDetailDataTable = WJ.DataTable({
                url: url,
                columns: filteredColumns,
                id: 'formDetailDataTable',
                tabs: tabs,
                order: order,
                serverSide: true,
                idAutoOpener: true,
                onXhr: function( TABLE, e, settings, json ) {
                    if (!json || !Array.isArray(json.data)) return;
                    json.data.forEach(j => {
                        if (!j || j.columnNamesAndValues == null) return;
                        for (let [key, value] of Object.entries(j.columnNamesAndValues)) {
                            j["col_"+key] = value;
                        }

                        // set class for not confirmed double opt-in forms
                        if (typeof j.doubleOptinHash !== "undefined" && j.doubleOptinHash) {
                            if (typeof j.doubleOptinConfirmationDate !== "undefined" && j.doubleOptinConfirmationDate == null) {
                                j.editorFields = { rowClass: "is-disabled" };
                            }
                        }
                    });
                },
                lastExportColumnName: 'lastExportDate',
                byIdExportColumnName: 'id'
            });

            formDetailDataTable.hideButton('create');
            formDetailDataTable.hideButton('duplicate');
            formDetailDataTable.hideButton('celledit');
            formDetailDataTable.hideButton('import');

            // Add archive button only in non-archived detail view
            if (!isArchived && formDetailDataTable.button && typeof formDetailDataTable.button().add === 'function') {
                formDetailDataTable.button().add(4, {
                    extends: 'remove',
                    text: '<i class="ti ti-archive"></i>',
                    action: function () {
                        formDetailDataTable.executeAction("archiveFormDetail");
                    },
                    init: function ( dt ) {
                        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                    },
                    className: 'btn btn-outline-secondary',
                    attr: {
                        'title': WJ.translate('components.form.admin_forms.archivuj_formular'),
                        'data-toggle': 'tooltip'
                    }
                });
            }

            //
            formDetailDataTable.on( 'init.dt', function () {
                setTimeout(() => {
                    //zmaz ine ako startswith moznosti selectpickerov
                    $("#formDetailDataTable_wrapper th form select.selectpickerbinded").each(function(index, element) {
                        let $this = $(this);
                        //console.log("SELECT: ", this);
                        let inputClass = $this.parents("div.input-group").find("input.form-control").attr("class");
                        if (inputClass.indexOf("dt-filter-note")!=-1 || inputClass.indexOf("dt-filter-files")!=-1) {
                            //tieto su povolene a mozu mat plny filter
                        }
                        else {
                            //nastav moznost startswith do filtrov
                            $this.selectpicker("val", "startwith");
                            if (this.options.length==4 && this.options[0].value=="contains") {
                                //consolelog(this.options[0]);
                                this.options[3] = null;
                                this.options[2] = null;
                                this.options[0] = null;
                            }
                            $this.selectpicker("refresh");
                        }
                    });
                }, 1000);
            });
        }

        // Initialize after potential dynamic column fetch and when DOM is ready
        if (window.domReady && typeof window.domReady.add === 'function') {
            window.domReady.add(function () {
                Promise.resolve(columnsFetchPromise).finally(() => initFormDetailDataTable());
            });
        } else {
            // Fallback if domReady is not available
            Promise.resolve(columnsFetchPromise).finally(() => initFormDetailDataTable());
        }
    };
})();
