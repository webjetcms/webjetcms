<template>
    <section>
        <table id="form-detail" class="datatableInit table"></table>
    </section>
</template>

<script lang="js">
    export default {
        name: 'form-detail',
        data() {
            return {
                formDetailTable: null,
            }
        },
        beforeDestroy() {
            if (this.formDetailTable != null) this.formDetailTable.destroy();
        },
        mounted() {
            var titleKey = 'forms.formsList.js';
            if (window.formRestUrl.indexOf("archive")!=-1) titleKey = 'forms.archiveList.js'
            WJ.breadcrumb({
                    id: "forms",
                    tabs: [
                        {
                            url: "#/detail/"+this.$route.params.name,
                            title: this.$route.params.name,
                            active: true
                        }
                    ],
                    backlink: {
                        url: "#/",
                        title: WJ.translate(titleKey),
                    }
                }
            );

            this.createDetailDataTable();
        },
        methods: {
            async getColumns(formColumns, orderable) {
                //console.log("formColumns=", formColumns);
                let that = this;
                const columns = new Array(
                    {
                        data: "id",
                        name: "id",
                        title: this.$id,
                        defaultContent: '',
                        className: 'dt-select-td cell-not-editable',
                        renderFormat: "dt-format-selector",
                        orderable: true,
                        editor: {
                            type : "hidden"
                        }
                    },
                    {
                        data: "id",
                        name: "tools",
                        title: this.$tools,
                        renderFormat: "dt-format-text",
                        orderable: false,
                        filter: false,
                        className: "not-export cell-not-editable",
                        editor: {
                            type: "hidden"
                        },
                        "render": function ( data, type, full ) {
                            //console.log("Render tools, data=", data);
                            return `<a class="form-view" id="form-view-${data}" href="javascript:openFormHtml(${data})" title="${that.$preview}" data-toggle="tooltip"><i class="ti ti-eye"></i></a>`;
                        }
                    },
                    {
                        data: "createDate",
                        name: "dateTime",
                        title: this.$createDate,
                        renderFormat: "dt-format-date-time",
                        orderable: true,
                        "className" : "dt-row-edit cell-not-editable",
                        "renderFormatLinkTemplate" : "javascript:;",
                        "renderFormatPrefix" : "<i class=\"ti ti-pencil\"></i> ",
                        editor: {
                            type: "datetime",
                            attr: {
                                disabled: true
                            },
                            tab: "metadata"
                        }
                    },
                    {
                        data: "lastExportDate",
                        name: "lastExportDate",
                        title: this.$lastExportDate,
                        renderFormat: "dt-format-date-time",
                        orderable: true,
                        className: "cell-not-editable",
                        editor: {
                            type: "datetime",
                            attr: {
                                disabled: true
                            },
                            tab: "metadata"
                        }
                    },
                    {
                        data: "note",
                        name: "note",
                        title: this.$note,
                        renderFormat: "dt-format-text",
                        className: "allow-html",
                        orderable: true,
                        editor: {
                            type: "textarea",
                            tab: "metadata"
                        }
                    },
                    {
                        data: "files",
                        name: "files",
                        title: this.$files,
                        renderFormat: "dt-format-text",
                        orderable: false,
                        className: "cell-not-editable",
                        editor: {
                            type: "text",
                            attr: {
                                disabled: true
                            },
                            tab: "metadata"
                        },
                        render: function ( data, type, full, meta ) {
                            let htmlCode = "";
                            if (data != null && data.length>0){
                                let fileNames = data.split(",");
                                for (const fileName of fileNames) {
                                    if (htmlCode != "") htmlCode += ", ";
                                    let fileNameText = fileName;
                                    let i = fileName.indexOf("_");
                                    if (i>0) fileNameText = fileName.substring(i+1);
                                    htmlCode += `<a href="/apps/forms/admin/attachment/?name=${fileName}" target="_blank">${fileNameText}</a>`;
                                }
                            }
                            return htmlCode;
                        }
                    }
                );

                //console.log("formColumns=", typeof formColumns.columns);
                if (typeof formColumns.columns === "undefined") {
                    WJ.notifyError(this.$permsDeniedTitle, this.$permsDeniedText);
                    return null;
                }

                //fields formulara
                for (let i=0; i<formColumns.columns.length; i++) {
                    let column = formColumns.columns[i];
                    //console.log("add column: ", column);
                    columns.push({
                        data: "col_"+column.value,
                        name: "col_"+column.value,
                        title: column.label,
                        renderFormat: 'dt-format-text',
                        orderable: orderable,
                        defaultContent: "",
                        className: "cell-not-editable allow-html",
                        editor: {
                            type: 'text',
                            attr: {
                                disabled: true
                            },
                            tab: "fields"
                        }
                    });
                }

                //prihlaseny pouzivatel
                columns.push({
                    data: "userDetails.userId",
                    name: "userDetails.userId",
                    title: this.$userId,
                    renderFormat: 'dt-format-number',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true,
                            type: "number"
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.firstName",
                    name: "userDetails.firstName",
                    title: this.$userFirstName,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.lastName",
                    name: "userDetails.lastName",
                    title: this.$userLastName,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.login",
                    name: "userDetails.login",
                    title: this.$userLogin,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.adress",
                    name: "userDetails.adress",
                    title: this.$userAdress,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.company",
                    name: "userDetails.company",
                    title: this.$userCompany,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.city",
                    name: "userDetails.city",
                    title: this.$userCity,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.zip",
                    name: "userDetails.zip",
                    title: this.$userZip,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.country",
                    name: "userDetails.country",
                    title: this.$userCountry,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.email",
                    name: "userDetails.email",
                    title: this.$userEmail,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.phone",
                    name: "userDetails.phone",
                    title: this.$userAddress,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.fieldA",
                    name: "userDetails.fieldA",
                    title: this.$userFieldA,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.fieldB",
                    name: "userDetails.fieldB",
                    title: this.$userFieldB,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.fieldC",
                    name: "userDetails.fieldC",
                    title: this.$userFieldC,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.fieldD",
                    name: "userDetails.fieldD",
                    title: this.$userFieldD,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });
                columns.push({
                    data: "userDetails.fieldE",
                    name: "userDetails.fieldE",
                    title: this.$userFieldE,
                    renderFormat: 'dt-format-text',
                    orderable: orderable,
                    filter: orderable,
                    defaultContent: "",
                    visible: false,
                    className: "cell-not-editable",
                    editor: {
                        type: 'text',
                        attr: {
                            disabled: true
                        },
                        tab: "user"
                    }
                });


                return columns;
            },
            async createDetailDataTable() {
                const url = `${window.formRestUrl}/columns/${this.$route.params.name}`;
                //ziskaj pocet zaznamov a zoznam stlpcov
                const formColumns = await $.get(url);

                let serverSide = true;
                let formCount = formColumns.count;
                //if (typeof formCount != "undefined" && formCount < window.formsDatatableServerSizeLimit) serverSide = false;

                //console.log("serverSide=", serverSide, "formCount=", formCount);

                let orderable = !serverSide;
                const columns = await this.getColumns(formColumns, orderable);

                if (columns == null) return;

                let tabs = [
                        { id: 'metadata', title: this.$tabMetadata, selected: true },
                        { id: 'fields', title: this.$tabFields },
                        { id: 'user', title: this.$tabUser }
                ]

                $("#form-detail").on( 'init.dt', function () {
                    if (serverSide) {
                        //console.log("on init");

                        //zmaz ine ako startswith moznosti selectpickerov
                        $("#form-detail_wrapper th form select.selectpickerbinded").each(function(index, element) {
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
                                    //console.log(this.options[0]);
                                    this.options[3] = null;
                                    this.options[2] = null;
                                    this.options[0] = null;
                                }
                                $this.selectpicker("refresh");
                            }
                        });
                    }
                } );

                //console.log("Creating datatable, serverSide=", serverSide, "columns=", columns);

                this.formDetailTable = WJ.DataTable({
                    url:  `${window.formRestUrl}/data/${this.$route.params.name}`,
                    columns: columns,
                    id: 'form-detail',
                    noAll: true,
                    tabs: tabs,
                    serverSide: serverSide,
                    order: [[2, 'desc']],
                    idAutoOpener: true,
                    onXhr: function(...args) {
                        const json = args[3];
                        json.forEach(j => {
                            for (let [key, value] of Object.entries(j.columnNamesAndValues)) {
                                if (value === "") {
                                    j["col_"+key] = 'prázdne';
                                } else {
                                    j["col_"+key] = value;
                                }
                            }
                        });
                        //console.log("Fixed json=", json);
                    },
                    lastExportColumnName: "lastExportDate",
                    byIdExportColumnName: "id"
                });

                this.formDetailTable.hideButton('import');
                this.formDetailTable.hideButton('duplicate');

                let that = this;
                let divider = ""
                if (window.formRestUrl.indexOf("archive")!=-1) divider = " buttons-divider"
                //preview page
                this.formDetailTable.button().add(4, {
                    text: '<i class="ti ti-eye"></i>',
                    action: function (e, dt, node) {
                        let selectedRows = dt.rows({ selected: true }).data();
                        for (let i=0; i<selectedRows.length; i++) {
                            let row = selectedRows[i];
                            if (selectedRows.length==1) {
                                openFormHtml(row.id);
                            } else {
                                window.open(window.formRestUrl+"/html/?id="+row.id);
                            }
                        }
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                    },
                    className: 'btn btn-outline-secondary buttons-history-preview'+divider,
                    attr: {
                        'title': that.$preview,
                        'data-toggle': 'tooltip'
                    }
                });
                if (window.formRestUrl.indexOf("archive")==-1) {
                    let that = this;
                    this.formDetailTable.button().add(5, {
                        extends: 'remove',
                        editor: that.formDetailTable.EDITOR,
                        text: '<i class="ti ti-archive"></i>',
                        action: function (e, dt, node) {
                            //console.log("Rotate, e=",e," dt=",dt," node=",node);
                            that.formDetailTable.executeAction("archiveFormDetail");
                        },
                        init: function ( dt, node, config ) {
                            $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                        },
                        className: 'btn btn-outline-secondary buttons-divider',
                        attr: {
                            'title': this.$archiveForm,
                            'data-toggle': 'tooltip'
                        }
                    });
                }
            }
        }
    }
</script>

<style lang="css"></style>