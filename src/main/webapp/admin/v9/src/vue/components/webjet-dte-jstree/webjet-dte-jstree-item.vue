<template>
    <div>
        <div class="input-group">
            <input type="text" class="form-control" v-bind:value="[grp.fullPath ? ((grp.domainName && grp.domainName != '' && click.indexOf('alldomains')!=-1 ? grp.domainName+':' : '')+grp?.fullPath?.replaceAll('&'+'#'+'47;', '/')) : grp?.virtualPath?.replaceAll('&'+'#'+'47;', '/')]" disabled="disabled" />

            <button @click="toggleModals" class="btn btn-outline-secondary  btn-vue-jstree-item-edit" type="button" v-tooltip:top="$WJ.translate('button.select')"><i class="ti ti-focus-2"></i></button>
            <button @click="removeFolder" v-if="click.indexOf('dt-tree-group-array')==0 || click.indexOf('dt-tree-group-null')==0" class="btn btn-outline-secondary btn-vue-jstree-item-remove" type="button" v-tooltip:top="$WJ.translate('button.delete')"><i class="ti ti-trash"></i></button>
            <button @click="removePage" v-if="click.indexOf('dt-tree-page-array')==0 || click.indexOf('dt-tree-page-null')==0" class="btn btn-outline-secondary btn-vue-jstree-item-remove" type="button" v-tooltip:top="$WJ.translate('button.delete')"><i class="ti ti-trash"></i></button>
            <button @click="removeById" v-if="click.indexOf('dt-tree-dir-array')==0 || click.indexOf('dt-tree-universal-array')==0" class="btn btn-outline-secondary btn-vue-jstree-item-remove" type="button" v-tooltip:top="$WJ.translate('button.delete')"><i class="ti ti-trash"></i></button>

        </div>
        <transition name="fade">
            <folder-tree-modal v-if="readyToOpen" :groupId="grp.groupId" :click="click" :readyToOpen="readyToOpen" :attr="attr" @close-custom-modal="closeModal"></folder-tree-modal>
        </transition>
    </div>
</template>

<script lang="js">
    import FolderTree from './folder-tree/vue-folder-tree.vue';
    import { default as EventBus } from './event-bus.js';

    export default {
        name: 'webjet-dte-jstree-item',
        props: ['data', 'idKey', 'dataTableName', 'dataTable', 'click', 'grp', 'index', 'attr'],
        emits: ["remove-item"],
        components: {
            'folder-tree-modal': FolderTree
        },

        data() {
            return {
                jsonData: null,
                readyToOpen: false,
                group: null
            }
        },
        watch: {
            readyToOpen(oldVal, newVal) {

            }
        },
        created() {
            //console.log("vue ITEM, created, data=", this.$props.data, " length=", this.$props.data.length, " dataTable=", this.$props.dataTable, "attr=", this.$props.attr);

            this.jsonData = this.$props.data;

            EventBus.$on('change-jstree', (data) => {
                if (this.readyToOpen == false) return;
                this.$parent.processTreeItem(this, data);
            });
        },
        methods: {
            toggleModals() {
                //EventBus.$emit('close-modal-by-key', this.componentKey);
                this.readyToOpen = !this.readyToOpen;
            },
            onDeleteGroup() {
                //this.groupIdNames = null;
                $(`#${this.idKey}`).val('');
            },
            removeFolder() {
                //console.log("REMOVE folder, this=", this, "click=", this.$props.click); //= handluje to priamo field-type-json.js.remove method
                if (this.$props.click == "dt-tree-group-null") {
                    this.grp.groupId=-1;
                    this.grp.fullPath = "";
                    this.grp.virtualPath = "";
                }
                else {
                    //console.log("Emiting");
                    this.$emit("remove-item", this.grp.groupId);
                }
            },
            removePage() {
                //console.log("REMOVE PAGE, this.grp=", this.grp); //= handluje to priamo field-type-json.js.remove method
                if (this.$props.click == "dt-tree-page-null") {
                    this.grp.docId=-1;
                    this.grp.fullPath = "";
                    this.grp.virtualPath = "";
                }
                else {
                    this.$emit("remove-item", this.grp.docId);
                }
            },
            removeById() {
                //console.log("REMOVE by id, this.grp=", this.grp); // = handluje to priamo field-type-json.js.remove method
                this.$emit("remove-item", this.grp.id);
            },
            closeModal() {
                this.readyToOpen = false;
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
    .custom-modal {
        position: fixed;
        z-index: 99999999;
        top: 0px;
        left: 0px;
        right: 0px;
        bottom: 0px;
        //padding: 100px;
        border: 0;
        background: rgba(0,0,0,.7);
        height: 100%;
        width: 100%;

        .jstree {
            border-radius: 6px;
            //margin-left: 50px;
            //margin-right: 50px;
            padding-bottom: 15px;
        }
    }
    .fade-enter-active, .fade-leave-active {
        transition: opacity .5s;
    }
    .fade-enter, .fade-leave-to {
        opacity: 0;
    }
</style>