import { default as EventBus } from '../vue/components/webjet-dte-jstree/event-bus';

export class WebjetJsTree {
    constructor(id) {
        this.id = $(`#${id}`);
        this.jstree = null;

        this.jsTreeUrl = this.id.data("rest-url");
        this.jsTreeMoveUrl = this.id.data("rest-move-url");
        this.jsTreeParamName = this.id.data("rest-param-name");
        this.jsTreeRoot = this.id.data("rest-root") || "-1";
        this.createJsTree();
        this.events();
    }

    createJsTree() {
        this.jstree = this.id.jstree({
            'core': {
                'check_callback': (operation, node, parent) => {
                    return !(operation === 'copy_node' || operation === 'move_node' && parent.id === '#');
                },
                'data': (obj, callback) => {
                    let jsTreeParamValue = this.jsTreeRoot;

                    if (this.jsTreeParamName === 'url') {
                        jsTreeParamValue = '/images';
                        if (obj.id !== '#' && typeof obj.original !== 'undefined') {
                            jsTreeParamValue = obj.original.virtualPath;
                        }
                    } else {
                        if (obj.id !== '#') {
                            jsTreeParamValue = obj.id;
                        }
                    }

                    const data = {
                        [this.jsTreeParamName]: jsTreeParamValue,
                    };

                    let url = this.updateUrl(this.jsTreeUrl, '/tree');

                    $.ajax({
                        url: url,
                        method: 'post',
                        contentType: 'application/json',
                        data: JSON.stringify(data),
                        success: data => {
                            if (!data.result) {
                                WJ.notifyError(data.error);
                                return;
                            }

                            //If its dt-tree-dir-simple, open disabled parent folders in way, that it will not call open event
                            if(url.indexOf("click=dt-tree-dir-simple") > 0) {
                                //Get root folder from url
                                let paramString = url.split('?')[1];
                                let queryString = new URLSearchParams(paramString);
                                let rootFolder = queryString.get("rootFolder");

                                data.items.forEach((item) => {
                                    if(item.state['disabled'] == true || item.icon == "ti ti-folder-x") {
                                        item.children = [];
                                        item.state['opened']  = true;
                                    }

                                    //We must ENABLE rootFolder, can be disabled
                                    if(rootFolder == item.id) {
                                        item.icon = 'ti ti-folder';
                                        item.state['disabled'] = false;
                                    }
                                });
                            }

                            callback(data.items);
                        }
                    });
                }
            },
            "plugins": [
                "dnd"
            ],
            "types": {
                "#": {
                    "max_children": 1,
                    "max_depth": 4,
                    "valid_children": ["root"]
                },
                "root": {
                    "icon": "/static/3.3.8/assets/images/tree_icon.png",
                    "valid_children": ["default"]
                },
                "default": {
                    "valid_children": ["default", "file"]
                },
                "file": {
                    "icon": "glyphicon glyphicon-file",
                    "valid_children": []
                }
            }
        });

    }

    events() {
        this.jstree.on("move_node.jstree", (e, data) => {
            //console.log("AAAA");
            delete data.node.children;
            const json = {
                node: data.node,
                parent: data.parent,
                position: data.position,
                old_parent: data.old_parent,
                old_position: data.old_position,
            };

            const url = this.updateUrl(this.jsTreeUrl, '/move');
            $("div.dt-processing").show();
            $.ajax({
                url: url,
                method: 'post',
                dataType: 'json',
                contentType: 'application/json',
                data: JSON.stringify(json),
                success: response => {
                    $("div.dt-processing").hide();
                    if (!response.result) {
                        WJ.notifyError(response.error);
                        return;
                    }

                    // LPA: bez refreshu parenta jstree hadzalo internu chybu
                    this.jstree.jstree(true).refresh(data.parent);
                },
                error: () => {
                    $("div.dt-processing").hide();
                }
            })
        });

        this.jstree.on("select_node.jstree", (e, data) => {
            const instance = data.instance;
            const parent = instance.get_parent(data.node);
            data.parentNode = instance.get_node(parent);

            EventBus.$emit('change-jstree', data);
        });
    }

    /**
     * @param {string} url
     * @param {string} [updateUri]
     * @returns {string}
     * @private
     */
    updateUrl(url, updateUri = '') {
        /** @type {string[]} */
        const urlTempArray = url.split('?');
        url = urlTempArray.shift() + updateUri;
        if (urlTempArray.length) {
            url += '?' + urlTempArray.join('?');
        }
        return url;
    }
}