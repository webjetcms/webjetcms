<template>
    <section>
        <div v-if="click.indexOf('dt-tree-group-array-scheduler')!=-1" class="dt-tree-container">
            <div class="form-group"
            v-for="(item, index) in data" v-bind:grp="item" v-bind:index="index" v-bind:key="item.scheduleId">
                <Item v-bind:grp="item" :data-table-name="dataTableName" :data-table="dataTable" v-bind:index="index" :click="click" :id-key="idKey" :data="data" @remove-item="onRemoveItem"/>
            </div>
        </div>
        <div v-if="click.indexOf('dt-tree-page')!=-1" class="dt-tree-container">
            <div class="form-group"
            v-for="(item, index) in data" v-bind:grp="item" v-bind:index="index" v-bind:key="item.docId">
                <Item v-bind:grp="item" :data-table-name="dataTableName" :data-table="dataTable" v-bind:index="index" :click="click" :id-key="idKey" :data="data" @remove-item="onRemoveItem"/>
            </div>
        </div>
        <div v-if="click.indexOf('dt-tree-group-array-scheduler')==-1 && click.indexOf('dt-tree-page')==-1" class="dt-tree-container">
            <div class="form-group"
            v-for="(item, index) in data" v-bind:grp="item" v-bind:index="index" v-bind:key="item.groupId">
                <Item v-bind:grp="item" :data-table-name="dataTableName" :data-table="dataTable" v-bind:index="index" :click="click" :id-key="idKey" :data="data" @remove-item="onRemoveItem"/>
            </div>
        </div>
        <div v-if="click.indexOf('dt-tree-group-array')==0 || click.indexOf('dt-tree-page-array')==0 || click.indexOf('dt-tree-dir-array')==0 || click.indexOf('dt-tree-universal-array')==0" v-bind:class="{'dt-tree-container-no-margin-top': data.length<1}" class="form-group row">
            <div class="col-12">
                <button @mouseup="toggleModals" class="btn btn-outline-secondary btn-vue-jstree-add"><i class="far fa-plus"></i> <span v-text="addButtonText"/></button>
            </div>
        </div>
        <transition name="fade">
            <folder-tree-modal v-if="readyToOpen" :groupId="1" :click="click" :readyToOpen="readyToOpen" :attr="attr" @close-custom-modal="closeModal"></folder-tree-modal>
        </transition>
    </section>
</template>

<script lang="js">
    import FolderTree from './folder-tree/vue-folder-tree.vue';
    import Item from './webjet-dte-jstree-item.vue';
    import { default as EventBus } from './event-bus.js';

    export default {
        name: 'webjet-dte-jstree',
        //idKey == ID input elementu
        props: ['data', 'idKey', 'dataTableName', 'dataTable', 'click', 'attr'],
        emits: ["remove-item"],
        components: {
            'folder-tree-modal': FolderTree,
            Item
        },

        data() {
            return {
                jsonData: null,
                readyToOpen: false,
                toggleIdButton: null,
                group: null,
                addButtonText: WJ.translate("button.add")
            }
        },
        watch: {
            readyToOpen(oldVal, newVal) {
                !newVal ? $(`#${this.toggleIdButton}`).html('Zatvoriť okno') : $(`#${this.toggleIdButton}`).html('Upraviť');
            }
        },
        created() {
            //console.log("created, idKey=", this.$props.idKey, " click=", this.$props.click, " attrs=", this.$props.attr);

            this.jsonData = this.$props.data;

            //zachyt kliknutie v jstree a sprocesuj ho
            EventBus.$on('change-jstree', (data) => {
                if (this.readyToOpen == false) return;
                this.processTreeItem(this, data);
            });

            if (this.$props.attr != null) {
                if (typeof(this.$props.attr["data-dt-json-addbutton"])!=undefined) this.addButtonText=this.$props.attr["data-dt-json-addbutton"];
            }
            if (this.click.indexOf("dt-tree-dir-simple")!=-1) {
                //remove diabled element on input
                var that = this;
                setTimeout(function() {
                    window.$("#"+that.idKey).parent().find("div.dt-tree-container .input-group input").removeAttr("disabled");
                }, 100);
            }
        },
        methods: {
            onRemoveItem(id) {
                //console.log("Volane onRemoveItem na jstree, id=", id);
                this.$emit("remove-item", id);
            },
            toggleModals() {
                //console.log("toggleModals");
                this.readyToOpen = !this.readyToOpen;
            },
            onDeleteGroup() {
                $(`#${this.idKey}`).val('');
            },
            closeModal() {
                this.readyToOpen = false;
            },
            processTreeItem(that, data) {
                //tu sa procesuje kliknutie na objekt, that je instancia, bud root komponenty alebo item komponenty
                //tento kod sa vola aj z item komponenty pri zmene objektu

                // console.log("som processTreeItem, that=", that, " datatable=", that.$props.dataTable);
                // console.log("change-jstree, data=", data, " click=", that.click, " index=", that.index);

                //validuj, ze bol vybraty adresar alebo stranka (podla toho co ocakavame)
                if (this._validateGroupPageClick(that, data)==false) return;

                //validuj, ci v zozname nemame uz rovnaky objekt
                if (that.click.indexOf("-array")!=-1) {
                    if (this._validateDuplicity(that, data)==false) return;
                }

                //ziskaj bud custom item podla konstruktora DT alebo standardny GroupDetails/DocDetails
                var item = null;
                if (that.$props.dataTable.DATA.jsonField != null && "function"==(typeof that.$props.dataTable.DATA.jsonField.getItem)) {
                    item = that.$props.dataTable.DATA.jsonField.getItem(that.$props, data);
                } else {
                    if (that.click.indexOf("dt-tree-group")==0) item = data.node.original.groupDetails;
                    else if (that.click.indexOf("dt-tree-page")==0) {
                        item = data.node.original.docDetails;
                        //kopirujeme virtualPath z node do DocDetails objektu, kedze ta hodnota sa potom pouziva, nemusime na backende modifikovat DocDetails objekt
                        if (that.click.indexOf("alldomains")!=-1) item.fullPath = data.node.original.virtualPath;
                    }
                    else item = data.node.original;
                }

                //console.log("ITEM=", item, "click=", that.click);

                if (that.click.indexOf("-array")!=-1) {
                    if (typeof(item) != "undefined" && item != null) {
                        if (typeof(that.index) != "undefined" && that.index>=0) {
                            //ak sa jedna o nahradu existujuceho objektu mame setnute that.index
                            that.data.splice(that.index, 1, item);
                        } else {
                            that.data.push(item);
                        }

                        that.readyToOpen = false;
                    }
                } else {
                    //vymenit existujuci objekt je potrebne cez pop+push
                    that.data.pop();
                    that.data.push(item);

                    that.readyToOpen = false;
                }

                //There must by allso prefix of that.$props.dataTableName, because table can be nested in another table with same columns
                //And first-child because it's text input (value that is saved in DB) and second child is VUE component
                var textInputId;
                if (that.$props.dataTableName!=null) textInputId = "#" + that.$props.dataTableName + "_modal" + " #"+that.$props.idKey;
                else textInputId = " #"+that.$props.idKey;

                //zmen hodnotu v textarea, aby sme to videli a firni event, aby sa to dalo pocuvat
                if (that.click.indexOf("dt-tree-dir-simple")!=-1) {
                        window.$(textInputId).val(that.$root.data[0].virtualPath).change();
                } else if (that.click.indexOf("dt-tree-groupid-root")!=-1 || that.click.indexOf("dt-tree-groupid")!=-1) {
                    setTimeout(function() {
                        //console.log("input=", textInputId);
                        //console.log("data=", that.$root.data);
                        let $element = window.$(textInputId);
                        let groupId = that.$root.data[0].groupId;
                        let text = that.$root.data[0].fullPath;

                        if (groupId < 1) {
                            groupId = -1;
                            let textEmpty = $element.attr("data-text-empty");
                            if (typeof textEmpty != "undefined" && textEmpty != null) {
                                text = textEmpty;
                                that.$root.data[0].fullPath = text;
                            }
                        }

                        $element.attr("data-text", text);
                        $element.val(groupId);
                        $element.trigger("change");
                    }, 100);
                } else {
                    window.$(textInputId).val(JSON.stringify(that.$root.data, undefined, 4)).change();
                }
            },
            _validateGroupPageClick(that, data) {
                //If group is marked with this icon (no permitted group) after click on it, nothing happens - sekjurity
                if(data.node.icon !== undefined && "fas fa-folder-times" == data.node.icon) {
                    return false;
                }

                //kontrola, ci sa vybral adresar ked sa mal
                if (that.click.indexOf("dt-tree-group")==0 && typeof(data.node.original.groupDetails)=="undefined") {
                    WJ.notifyError("Vyberte adresár", null, 5000);
                    return false;
                }
                //alebo ci sa vybrala stranka ked sa mala
                if (that.click.indexOf("dt-tree-page")==0 && typeof(data.node.original.docDetails)=="undefined") {
                    WJ.notifyError("Vyberte web stránku", null, 5000);
                    return false;
                }
                return true;
            },
            _validateDuplicity(that, data) {
                //kontrola duplicity vybrateho objektu
                var i;
                var newKey;
                if (that.click.indexOf("dt-tree-group")==0) newKey = this._getKey(that, data.node.original.groupDetails);
                else if (that.click.indexOf("dt-tree-page")==0) newKey = this._getKey(that, data.node.original.docDetails);
                else newKey = this._getKey(that, data.node);

                //console.log("new key=", newKey);
                for (i=0; i<that.data.length; i++) {
                    var itemKey = this._getKey(that, that.data[i]);
                    //console.log(newKey, "vs", itemKey);

                    if (newKey == itemKey) {
                        WJ.notifyError("Zvolená položka sa už v zozname nachádza", null, 5000);
                        return false;
                    }
                }
                return true;
            },
            _getKey(that, data) {
                //ziska jednoznacne ID objektu pre kontrolu duplicity
                //console.log("getKey, data=", data, "that=", that);
                var key;
                if (that.$props.dataTable.DATA.jsonField != null && "function"==(typeof that.$props.dataTable.DATA.jsonField.getKey)) {
                    key = that.$props.dataTable.DATA.jsonField.getKey(that.$props, data);
                } else {
                    if (that.click.indexOf("dt-tree-group")==0) key = data.groupId;
                    else if (that.click.indexOf("dt-tree-page")==0) key = data.docId;
                    else key = data.id;
                }
                //console.log("getKey=", key, "click=", that.click);
                return key;
            }
        }
    }
</script>

<style lang="scss">
    .group-id {
        width: 100%;
        display: flex;
        justify-content: space-between;
    }
    .fade-enter-active, .fade-leave-active {
        transition: opacity .5s;
    }
    .fade-enter, .fade-leave-to {
        opacity: 0;
    }
</style>