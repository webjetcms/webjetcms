'use strict';

//jstree datovy typ pre datatable
//based on: https://gist.github.com/yajra/a383bf41ec7eb7e673cf9b7794020d51
export function typeJsTree() {
    return {
        create(conf) {
            //console.log("jstree.create, conf=", conf);

            conf._div = $('<div />');
            conf._searchForm = $('<div class="input-group" />');
            conf._div.append(conf._searchForm);

            conf._search = $('<input class="form-control"/>').appendTo(conf._searchForm);
            conf._searchForm.append(
                    '<button class="btn btn-sm btn-outline-secondary btn-search">' +
                        '<i class="fa fa-search"></i>' +
                    '</button>' +
                    '<button class="btn btn-sm btn-outline-secondary btn-clear" style="display: none;">' +
                        '<i class="fa fa-times"></i>' +
                    '</button>' +
                    '<button class="btn btn-sm btn-outline-secondary btn-select-all">' +
                        '<i class="far fa-check-square"></i>' +
                    '</button>' +
                    '<button class="btn btn-sm btn-outline-secondary btn-deselect-all">' +
                        '<i class="far fa-square"></i>' +
                    '</button>'
            );
            conf._searchForm.on('click', '.btn-search', () => {
                let v = conf._search.val();
                conf._tree.jstree('search', v);
                //console.log("clicked, clear=", conf._searchForm.find(".btn_clear"));
                conf._searchForm.find(".btn-clear").show();
            });
            conf._searchForm.on('click', '.btn-clear', () => {
                conf._search.val('');
                conf._tree.jstree('clear_search');
                conf._tree.jstree('open_all');
                conf._searchForm.find(".btn-clear").hide();
            });
            conf._searchForm.on('click', '.btn-select-all', () => {
                conf._search.val('');
                conf._tree.jstree('select_all');
            });
            conf._searchForm.on('click', '.btn-deselect-all', () => {
                conf._search.val('');
                conf._tree.jstree('deselect_all');
            });

            conf._tree = $('<div />')
                .attr($.extend({
                    id: $.fn.dataTable.Editor.safeId(conf.id)
                }, conf.attr || {}));

            conf._div.append(conf._tree);

            conf._to = false;
            conf._search.on('keyup', (evt) => {
                if (evt.key === 'Enter' || evt.keyCode === 13) {
                    let v = conf._search.val();
                    conf._tree.jstree('search', v);
                    conf._searchForm.find(".btn-clear").show();
                }
            });

            //v data atribute data-dt-field-jstree-name mame meno window objektu v ktorom su JSON data pre render jstree
            let objName = conf.attr["data-dt-field-jstree-name"];
            let jstreeJsonData = window[objName];

            let options = $.extend({
                core: {
                    data: jstreeJsonData
                },
                search: {
                    fuzzy: false,
                    show_only_matches: true,
                    search_leaves_only: false,
                    close_opened_onclear: true
                },
                plugins: ["checkbox", "search"]
            }, conf.opts);

            //console.log("jstree.options=", options);

            conf._tree.jstree(options);

            let EDITOR = this;
            window.addEventListener("WJ.DTE.opened", function(e) {
                //console.log("WJ.DTE.opened, e=", e);
                if (EDITOR.TABLE.DATA.id===e.detail.id) {

                    var val = conf._currentVal;

                    conf._tree.jstree('clear_search');
                    conf._tree.jstree('deselect_all');
                    conf._tree.jstree('open_all');
                    conf._searchForm.find(".btn-clear").hide();

                    if (Array.isArray(val)) {
                        val.forEach((v) => {
                            conf._tree.jstree('select_node', v);
                            //console.log("selecting, v=", v);
                        });
                    }
                }
            });

            return conf._div;
        },

        get(conf) {
            return conf._tree.jstree('get_selected');
        },

        set(conf, val) {
            //ve must set jstree items on WJ.DTE.opened event
            conf._currentVal = val;
        },

        enable(conf) {
            $(conf._tree).removeAttr('disabled');
        },

        disable(conf) {
            $(conf._tree).attr('disabled', 'disabled');
        },

        update(conf, data) {

        },

        focus(conf) {

        },

        owns(conf, node) {
            return true;
        },

        canReturnSubmit() {
            return false;
        }
    }
};