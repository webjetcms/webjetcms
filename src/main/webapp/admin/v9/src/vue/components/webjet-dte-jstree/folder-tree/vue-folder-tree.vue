<template>
    <section id="custom-modal-id" :class="['custom-modal', {'open-custom-modal': readyToOpen}]">
        <div class="custom-modal-wrapper">
            <a class="close-custom-modal" href="javascript:;" @click.prevent="hideCustomModal">
                <i class="ti ti-circle-x"></i>
            </a>
            <div class="jsTree-wrapper">
                <div id="jsTree" data-rest-url="/admin/rest/groups/tree?click=aaa" data-rest-param-name="id" :data-group-id="groupId" data-rest-root="-1"></div>
            </div>
        </div>
    </section>
</template>

<script lang="js">
    export default {
        name: 'vue-folder-tree',
        props: ['groupId', 'readyToOpen', 'click', 'attr'],
        data() {
            return {
                jsTree: null
            }
        },

        methods: {
            hideCustomModal() {
                this.$emit("close-custom-modal", false);
            }
        },

        mounted() {
            //console.log("mounted", $("#jsTree").attr("data-rest-url"), "attr=", this.$props.attr, "click=", this.click, "props=", this.$props);
            let restEndpoint = "/admin/rest/groups/tree";
            if (this.click.indexOf("dt-tree-dir")==0) restEndpoint = "/admin/rest/elfinder/tree";
            if (this.$props.attr != null && typeof this.$props.attr["data-dt-field-dt-url"] != "undefined") restEndpoint = this.$props.attr["data-dt-field-dt-url"];

            restEndpoint = restEndpoint + "?click=" + this.click;

            //
            if(this.$props.attr != null && typeof this.$props.attr["data-dt-field-skipFolders"] != "undefined") {
                restEndpoint = restEndpoint + "&skipFolders=" + this.$props.attr["data-dt-field-skipFolders"];
            }

            if(this.$props.attr != null && typeof this.$props.attr["data-dt-field-hideRootParents"] != "undefined") {
                restEndpoint = restEndpoint + "&hideRootParents=" + this.$props.attr["data-dt-field-hideRootParents"];
            }

            //set root if defined
            if (this.$props.attr != null && typeof this.$props.attr["data-dt-field-root"] != "undefined") {
                $("#jsTree").attr("data-rest-root", this.$props.attr["data-dt-field-root"]);

                //add root to url
                restEndpoint = restEndpoint + "&rootFolder=" + this.$props.attr["data-dt-field-root"];
            }

            //SET rest url
            $("#jsTree").attr("data-rest-url", restEndpoint);

            //console.log("mounted 2", $("#jsTree").attr("data-rest-url"));
            this.jsTree = new WebjetJsTree('jsTree');
        },
    }
</script>

<style lang="scss">
    .custom-modal {
        width: auto;
        overflow-y: scroll;
        height: 400px;
        margin: 0 auto;
        background-color: #fff;
        border: 1px solid #1f2022;
        position: relative;
        border-radius: 5px;

        .custom-modal-wrapper {
            max-width: 1000px;
            display: block;
            margin: 5% auto;
            position: relative;

            .close-custom-modal {
                position: absolute;
                right: -10px;
                top: 0px;
                font-size: 19px;
                color: #868EA5;

                &:hover {
                    i {
                        color:#1f2022;
                    }
                }
            }

            .jstree {
                background: #fff;
            }
        }
    }
    @media (max-width: 1000px) {
        .custom-modal .custom-modal-wrapper {
            max-width: 90%;
        }
    }
</style>