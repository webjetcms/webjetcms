import WJ from "../../src/js/webjet";

export function typeSelectEditable() {

    function openIframeModal(conf, EDITOR, editUrl, editTitle, id) {

        var width = Math.min(1170, $(window).innerWidth());
        var height = $(window).innerHeight()-32-71;
        if ($("html").hasClass("in-iframe")) {
            //height = $(window).innerHeight()-32;
            width = width - 20;
        }

        WJ.openIframeModal({
            url: WJ.urlAddParam(editUrl.replace("{id}", id), "showOnlyEditor", "true"),
            width: width,
            height: height,
            buttonTitleKey: "button.save",
            closeButtonPosition: "close-button-over",
            okclick: function() {
                //console.log("Dialog okclick");
                $('#modalIframeIframeElement').contents().find('div.modal.DTED.show div.DTE_Footer button.btn-primary').trigger("click");
                return false;
            },
            onload: function(detail) {
                let iframeWindow = detail.window;
                //console.log("iframeWindow=", iframeWindow.location.href);
                iframeWindow.addEventListener("WJ.DTE.close", function(event) {
                    //ulozenie nested modalu nema sposobit zatvorenie okna
                    if (true===event.detail.dte.TABLE.DATA.nestedModal) return;
                
                    const $select = conf._input;
                    const oldOptions = Array.from($select[0].options).map(opt => opt.value);
                
                    WJ.closeIframeModal();
                    //vyvolaj nacitanie JSON optionov
                    EDITOR.TABLE.wjUpdateOptions(null, () => {
                        const newOptions = Array.from($select[0].options).map(opt => opt.value);
                        const added = newOptions.filter(val => !oldOptions.includes(val));
                
                        if (added.length === 1) {
                            $select.val(added[0]).trigger("change");
                        }
                    });
                });
                iframeWindow.addEventListener("WJ.DTE.open", function(event) {
                    iframeWindow.$("#modalIframeLoader").css("display", "none");

                    let title = conf.label;
                    if (typeof editTitle == "string") title = WJ.translate(editTitle);
                    iframeWindow.$(".modal div.DTE_Header_Content h5.modal-title").html(title);
                });
            }
        });
    }

    function showHideIcons(htmlCode) {
        let value = htmlCode.find("select").val();
        let editButton = htmlCode.find("button.btn-edit");
        //console.log("value=", value);
        if (value < 0) {
            editButton.hide();
        } else {
            editButton.show();
        }
    }

    let originalSelectCreateFunction = $.fn.dataTable.Editor.fieldTypes.select.create;
    $.fn.dataTable.Editor.fieldTypes.select.create = function (conf) {
        if (conf.attr && conf.attr["data-dt-edit-url"] && typeof conf.attr["data-dt-edit-url"] == "string") {
            //console.log("typeSelectEditable - creating editable select, conf=", conf, "type=", typeof conf.attr["data-dt-edit-url"], "this=", this);

            const editUrl = conf.attr["data-dt-edit-url"];
            const editPerms = conf.attr["data-dt-edit-perms"];
            const editTitle = conf.attr["data-dt-edit-title"];
            const EDITOR = this;

            if (typeof editPerms == "string") {
                //console.log("Checking perms=", editPerms);
                if (WJ.hasPermission(editPerms)===false) {
                    //console.log("User doesn't have permission");
                    return originalSelectCreateFunction(conf);
                }
            }

            //return originalSelectCreateFunction(conf);
            let htmlCode = $(`
                <div class="input-group">
                    <select></select>
                    <button class="btn btn-outline-secondary btn-edit" type="button" data-toggle="tooltip" title="${WJ.translate('button.edit')}"><i class="ti ti-pencil"></i></button>
                    <button class="btn btn-outline-secondary btn-add" type="button" data-toggle="tooltip" title="${WJ.translate('button.createNew')}"><i class="ti ti-plus"></i></button>
                </div>
            `);

            conf._input = htmlCode.find("select")
                .attr($.extend({
                id: $.fn.dataTable.Editor.safeId(conf.id),
                multiple: conf.multiple === true
            }, conf.attr || {}))
                .on('change.dte', function (e, d) {
                // On change, get the user selected value and store it as the
                // last set, so `update` can reflect it. This way `_lastSet`
                // always gives the intended value, be it set via the API or by
                // the end user.
                if (!d || !d.editor) {
                    //console.log("conf._lastSet=", conf._lastSet);
                    conf._lastSet = $.fn.dataTable.Editor.fieldTypes.select.get(conf);
                    showHideIcons(htmlCode);
                }
            });
            $.fn.dataTable.Editor.fieldTypes.select._addOptions(conf, conf.options || conf.ipOpts);

            htmlCode.find("button.btn-edit").on("click", () => {
                //console.log("btn-edit click, input=", conf._input.val());
                openIframeModal(conf, EDITOR, editUrl, editTitle, conf._input.val());
            });
            htmlCode.find("button.btn-add").on("click", () => {
                //console.log("btn-add click");
                openIframeModal(conf, EDITOR, editUrl, editTitle, -1);
            });

            this.on('open', function (e, mode, action) {
                setTimeout(()=> {
                    showHideIcons(htmlCode);
                }, 500);
            });

            return htmlCode;
        }
        return originalSelectCreateFunction(conf);
    };

}