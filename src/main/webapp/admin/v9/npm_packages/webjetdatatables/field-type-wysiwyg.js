export function typeWysiwyg() {
    return {
        create: function ( conf ) {

            //vlozenie ckeditor.js je opozdene kvoli performance CPU v prehliadaci
            setTimeout(()=> {
                //nechceme zatazit CPU hned na zaciatku, takze ckeditor.js nacitame cez timenout mierne neskor
                var head = document.getElementsByTagName('head')[0];
                var script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = "/admin/skins/webjet8/ckeditor/dist/ckeditor.js";

                head.appendChild(script);
            }, 2000);

            //console.log("Creating WYSIWYG field, conf=", conf, "datatable=", conf.datatable, "editor=", this);
            let EDITOR = this;
            var id = $.fn.dataTable.Editor.safeId( conf.id );
            conf._id = id;

            // Field HTML structure
            let htmlCode =
                `<div class="wysiwyg" id="trEditor">
                    <div class="wysiwyg_textarea"><textarea id="${id}" style="display: none;"></textarea>
                    <div id="editorFormDiv">
                        <form name="editorForm">
                            <textarea name="data" style="border: 0px"></textarea>
                            <input type="hidden" name="docId"/>
                            <input type="hidden" name="groupId"/>
                            <input type="hidden" name="virtualPath"/>
                            <input type="hidden" name="title"/>
                        </form>
                    </div>
                </div>
                <div class="wysiwyg" id="${id}-trPageBuilder">
                    <iframe id="${id}-pageBuilderIframe" class="md-pageBuilder" src="about:blank"></iframe>
                </div>
                <div class="exit-inline-editor" id="${id}-editorTypeSelector">
                    ${WJ.translate("editor.type_select.label.js")}<br/>
                    <select onchange="window.switchEditorType(this)">
                        <option value="">${WJ.translate("editor.type_select.standard.js")}</option>
                        <option value="pageBuilder" selected="selected">${WJ.translate("editor.type_select.page_builder.js")}</option>
                    </select>
                </div>
                `;
            let htmlCodeElement = $(htmlCode);

            conf._input = $(htmlCodeElement).find("textarea");

            //console.log("input=", conf._input);
            //nastav atributy
            if (typeof conf.attr != undefined && conf.attr != null) {
                $.each(conf.attr, function( key, value ) {
                    //console.log("Setting attr: key=", key, " value=", value);
                    $(conf._input).attr(key, value);
                });
            }

            conf.wjeditor = null;
            EDITOR.on( 'open', function ( e, type ) {
                //console.log("DT WYSIWYG Editor OPEN, e=", e, "type=", type);

                conf.datatableEditingType = type;

                if ("main"!==type) {
                    //console.log("not editor, returning")
                    return;
                }

                //console.log("data field: ", EDITOR.field( 'data' ).val());

                //TODO: FormDB.getAllRegularExpression();
                if (conf.wjeditor==null) {
                    window.createDatatablesCkEditor().then(module => {
                        const options = {
                            datatable: EDITOR.TABLE,
                            fieldid: id,
                            constants: {
                                pixabayEnabled: window.pixabayEnabled,
                                editorImageAutoTitle: window.editorImageAutoTitle
                            },
                            lang: {
                                projectName: WJ.translate("default.project.name")
                            },
                            onReady: function() {
                                //console.log("Setting json onReady, json=", EDITOR.currentJson);
                                conf.wjeditor.setJson(EDITOR.currentJson);
                                //instancia ckeditora, potrebuju to rozne pluginy a podobne, takze zatial takto kvoli spatnej kompatibilite
                                window.ckEditorInstance = conf.wjeditor.ckEditorInstance;

                                //nastav otvorene docid do inputu
                                if (typeof window.jsTreeDocumentOpener != "undefined" && typeof EDITOR.currentJson != undefined && EDITOR.currentJson != null) window.jsTreeDocumentOpener.setInputValue(EDITOR.currentJson.docId);
                            }
                        }
                        if (typeof window.CKEDITOR == "undefined") {
                            //este nedobehol load ckeditora
                            setTimeout(() => {
                                if (typeof window.CKEDITOR != "undefined") conf.wjeditor = new module.DatatablesCkEditor(options);
                            }, 2100);
                        } else {
                            conf.wjeditor = new module.DatatablesCkEditor(options);
                        }

                        //zrus spodny padding na tab-pane
                        $("#"+id).parents(".tab-pane").css("padding-bottom", "0px");

                        //console.log("editor constructed");
                    });
                    $("div.modal.DTED > div.modal-dialog").addClass("modal-xl");
                } else {
                    //console.log("Setting json, json=", EDITOR.currentJson);
                    conf.wjeditor.setJson(EDITOR.currentJson);

                    //nastav otvorene docid do inputu
                    if (typeof window.jsTreeDocumentOpener != "undefined" && typeof EDITOR.currentJson != "undefined") window.jsTreeDocumentOpener.setInputValue(EDITOR.currentJson.docId);
                }
            });

            EDITOR.on( 'close', function ( e, type ) {
                //console.log("EDITOR.onClose");
                $("#"+id+"-pageBuilderIframe").attr("src", "about:blank");
            });

            window.switchEditorType = function(select, e) {
                //console.log("Switch editor type to:", select.value, "conf=", conf.wjeditor, "e=", e);
                let editorType = select.value;
                conf.wjeditor.switchEditingMode(editorType);
                window.editorTypeForced = editorType;
                window.WJ.setAdminSetting("editorTypeForced", editorType);
            }

            return htmlCodeElement;
        },

        get: function ( conf ) {
            if (conf.wjeditor != null && "main"==conf.datatableEditingType) {
                var htmlCode = conf.wjeditor.getData();
                //console.log("WYSIWYG get, htmlCode=", htmlCode);
                conf._input.val(htmlCode);
            }
            //console.log("WYSIWYG get, conf=", conf, "returning=", conf._input.val());
            return conf._input.val();
        },

        set: function ( conf, val ) {
            //console.log("WYSIWYG set, val=", val, "conf=", conf);
            conf._input.val(val);
        },

        enable: function ( conf ) {
            conf._input.prop( 'disabled', false );
        },

        disable: function ( conf ) {
            conf._input.prop( 'disabled', true );
        },

        canReturnSubmit: function ( conf, node ) {
            return false;
        }
    }
}