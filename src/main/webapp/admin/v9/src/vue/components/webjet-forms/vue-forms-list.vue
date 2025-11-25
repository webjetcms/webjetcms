<template>
    <section>
        <table id="forms-list" class="datatableInit table"></table>
    </section>
</template>

<script lang="js">
    export default {
        name: 'forms-list',
        data() {
            return {
                formsListTable: null,
                columns: [
                    {
                        data: "id",
                        name: "id",
                        title: this.$id,
                        defaultContent: '',
                        className: 'dt-select-td',
                        renderFormat: "dt-format-selector"
                    },
                    {
                        data: "formName",
                        name: "formName",
                        title: this.$formName,
                        className: "dt-row-edit",
                        renderFormat : "dt-format-text",
                        renderFormatLinkTemplate : "javascript:;",
                        renderFormatPrefix : "<i class=\"ti ti-eye\"></i> ",

                        editor: {
                            type: "textarea"
                        }
                    },
                    {
                        data: "count",
                        name: "count",
                        title: this.$count,
                        renderFormat: "dt-format-text",

                        editor: {
                            type: "text"
                        }
                    },
                    {
                        data: "createDate",
                        name: "createDate",
                        title: this.$lastSendDate,
                        renderFormat: "dt-format-date-time",

                        editor: {
                            type: "datetime"
                        }
                    }
                ]
            }
        },
        beforeDestroy() {
            this.formsListTable.destroy();
        },
        mounted() {
            var titleKey = 'forms.formsList.js';
            if (window.formRestUrl.indexOf("archive")!=-1) titleKey = 'forms.archiveList.js'
            WJ.breadcrumb({
                    id: "forms",
                    tabs: [
                        {
                            url: "#/",
                            title: WJ.translate(titleKey),
                            active: true
                        }
                    ]
                }
            );
            this.formsListTable = WJ.DataTable({
                id: 'forms-list',
                url: window.formRestUrl,
                columns: this.columns,
                order: [[3, 'desc']],
                onEdit: function(TABLE, row, dataAfterFetch, dataBeforeFetch) {
                    //console.log("onEdit, row=", row, "dataAfterFetch=", dataAfterFetch, "dataBeforeFetch=", dataBeforeFetch);
                    window.VueTools.getRouter().push({path: `/detail/${dataAfterFetch.formName}`, params: {name: dataAfterFetch.formName}});
                }
            });
            // this.formsListTable.hideButton("create");
            // this.formsListTable.hideButton("edit");
            this.formsListTable.hideButton("celledit");
            this.formsListTable.hideButton("import");
            this.formsListTable.hideButton("export");
            this.formsListTable.hideButton("duplicate");
            if (window.formRestUrl.indexOf("archive")==-1) {
                let that = this;
                this.formsListTable.button().add(3, {
                    extends: 'remove',
                    editor: that.formsListTable.EDITOR,
                    text: '<i class="ti ti-archive"></i>',
                    action: function (e, dt, node) {
                        //console.log("Rotate, e=",e," dt=",dt," node=",node);
                        that.formsListTable.executeAction("archiveForm");
                    },
                    init: function ( dt, node, config ) {
                        $.fn.dataTable.Buttons.showIfRowSelected(this, dt);
                    },
                    className: 'btn btn-outline-secondary',
                    attr: {
                        'title': this.$archiveForm,
                        'data-toggle': 'tooltip'
                    }
                });
            }
        },
        methods: {
            setHeader(name) {
               document.getElementById('pills-form-detail-tab').innerHTML = name;
            }
        }
    }
</script>

<style lang="css"></style>