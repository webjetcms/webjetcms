/**
 * Implementuje funkciu nahladu web stranky v editore
 * @link /developer/apps/webpages/README.md#náhľad-stránky
 */
export class EditorPreview {

    options = null;
	webpagesDatatable = null;

    isPreviewClicked = false;
    previewWindow = null;

    constructor(options) {
        //console.log("Constructor, options=", options);
        this.options = options;
		this.webpagesDatatable = options.datatable;
    }

    bindEvents() {

        var self = this;

        window.previewPage = function() {
            self.isPreviewClicked = true;
            //vykonaj akoze submit, v preSubmit to odchytime a zastavime odoslanie
            self.webpagesDatatable.EDITOR.submit();
        }

        self.webpagesDatatable.EDITOR.on( 'preSubmit', function ( e, data, action ) {
            if (self.isPreviewClicked) {
                self.isPreviewClicked = false;

                //console.log("preSubmit, e=", e, "data=", data, "action=", action);

                try {
                    var docId = self.webpagesDatatable.EDITOR.currentJson.docId;
                    //console.log("docId=", docId);
                    //DT ma pre novy zaznam index 0, nie -1 ako WJ
                    if (docId < 0) docId = 0;
                    var jsonData = data.data[docId];
                    //console.log("jsonData=", jsonData);

                    var json = JSON.stringify(jsonData);
                    //fix na checkboxy
                    json = json.replace(/:\[true\]/gi, ":true");
                    json = json.replace(/:\[false\]/gi, ":false");
                    //console.log("json 2: ", json);

                    if (jsonData.id == null) jsonData.id = -1;

                    let url = "/admin/webpages/preview/?docid="+jsonData.id;

                    //uloz entitu volanim REST sluzby
                    $.ajax({
                        url: "/admin/rest/web-pages/preview/",
                        data: json,
                        contentType: 'application/json',
                        method: "post",
                        success: function(response) {
                            //console.log("SUCCESS, response=", response, "previewWindow=", self.previewWindow);
                            if (response.ok===true) {
                                //otvor okno s nahladom, otvarame vzdy takto, aby sa dostalo do popredia kliknutim na tlacidlo
                                self.previewWindow = window.open(url, "webjetPreviewWindow");
                                window.previewWindow = self.previewWindow;
                            } else {
                                window.WJ.notifyError(WJ.translate("editor.preview.error.js"), response);
                            }
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            console.log("ERROR, textStatus=", textStatus, "errorThrown=", errorThrown);
                            window.WJ.notifyError(WJ.translate("editor.preview.error.js"), textStatus);
                        }
                    })
                } catch (ex) {
                    console.log(ex);
                }

                return false;
            }
            return true;
        });

        self.webpagesDatatable.EDITOR.on('postSubmit', function (e, json, data) {
            //ak je okno stale otvorene ma nastaveny name atribut
            if (self.previewWindow != null && self.previewWindow.name != "") {
                //console.log("PREVIEW OKNO EXISTUJE, reloading");
                self.previewWindow.location.reload();
            }
        });

        self.webpagesDatatable.EDITOR.on('open', function (e, mode, action) {
            //console.log("Open, e=", e, "mode=", mode, "action=", action);
            if ("main"===mode && (
                    ("create"===action && self.webpagesDatatable.hasPermission("create")) ||
                    ("edit"===action && self.webpagesDatatable.hasPermission("edit"))
                    )
                ) {

                //pridaj btn Nahlad
                let previewButton = '<button class="btn btn-warning btn-preview" onclick="previewPage();" style="margin-right: 16px; margin-left: 0px;"><i class="ti ti-eye"></i> '+window.WJ.translate("editor.preview")+'</button>';
                $('#'+self.webpagesDatatable.DATA.id+'_modal div.DTE_Form_Buttons span.buttons-footer-left').prepend(previewButton);

            }
        });
    }
}