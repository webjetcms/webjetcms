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
                <div class="wysiwyg wysiwyg-pageBuilder" id="${id}-trPageBuilder">
                    <iframe id="${id}-pageBuilderIframe" class="md-pageBuilder" src="about:blank"></iframe>
                </div>
                <div class="exit-inline-editor" id="${id}-editorTypeSelector">
                    ${WJ.translate("editor.type_select.label.js")}<br/>
                    <select onchange="window.switchEditorType(this)" data-hide-disabled="true">
                        <option value="">${WJ.translate("editor.type_select.standard.js")}</option>
                        <option value="html">${WJ.translate("editor.type_select.html.js")}</option>
                        <option value="pageBuilder">${WJ.translate("editor.type_select.page_builder.js")}</option>
                    </select>
                    <br/>
                    ${WJ.translate("pagebuilder.modal.tab.size")}:
                    <a href="javascript:pbSetWindowSize('phone')" title="${WJ.translate('pagebuilder.modal.visibility.sm')}" data-toggle="tooltip"><span class="ti ti-device-mobile"></span></a>
                    <a href="javascript:pbSetWindowSize('tablet')" title="${WJ.translate('pagebuilder.modal.visibility.md')}" data-toggle="tooltip"><span class="ti ti-device-tablet"></a>
                    <a href="javascript:pbSetWindowSize('desktop')" title="${WJ.translate('pagebuilder.modal.visibility.xl')}" data-toggle="tooltip"><span class="ti ti-device-desktop"></a>
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
                        //allow to use module for Page Builder
                        window.datatablesCkEditorModule = module;

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

                        WJ.initTooltip($('div.exit-inline-editor a[data-toggle*="tooltip"]'));

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
                conf.wjeditor.switchEditingMode(editorType, true);
                window.editorTypeForced = editorType;
                if ("html"!=editorType) window.WJ.setAdminSetting("editorTypeForced", editorType);
            }

            window.pbSetWindowSize = function(size) {
                var iframeElement = null;

                if ("pageBuilder"==window.editorTypeForced) iframeElement = $("iframe.md-pageBuilder");
                else iframeElement = $("iframe.cke_wysiwyg_frame.cke_reset");

                //console.log("iframeElement=", iframeElement, "size=", size);
                var maxWidth = "";
                if ('tablet'==size) {
                    maxWidth = "991px";
                } else if ('phone'==size) {
                    maxWidth = "576px";
                }
                //console.log("Setting width: ", maxWidth);
                iframeElement.css("max-width", maxWidth);
            }

            return htmlCodeElement;
        },

        get: function ( conf ) {
            if (conf.wjeditor != null && "main"==conf.datatableEditingType) {
                var htmlCode = conf.wjeditor.getData();
                //console.log("WYSIWYG get, htmlCode=", htmlCode);
                //set htmlCode to input element, because it can be PageBuilder instance
                conf._input.val(htmlCode);
            }
            try {
                //fix for acunetix long texts which loads too long/ha too many JS errors, and acunetix will timeout scanning
                if (window.currentUser.login.indexOf("tester")==0) {
                    let val = conf._input.val();
                    if (val != null && val.length > 32000) {
                        if (val.indexOf("<ScRiPt")!==-1 || val.indexOf("<zzz")!==-1|| val.indexOf("DBMS_PIPE")!==-1) {
                            val = val.substring(0, 16000);
                            console.log("shrinking val to length=", val.length);
                            conf._input.val(val);
                        }
                    }
                }
            } catch (e) {
                console.error(e);
            }
            //console.log("WYSIWYG get, conf=", conf, "returning=", conf._input.val());
            return conf._input.val();
        },

        set: function ( conf, val ) {
            //console.log("WYSIWYG set, val=", val, "conf=", conf, "wjeditor=", conf.wjeditor);
            if (conf.wjeditor != null && "main"==conf.datatableEditingType) {
                conf.wjeditor.setData(val);
            }
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