/**
 * Implementuje funkciu nahladu web stranky v editore
 * @link /developer/apps/webpages/README.md#náhľad-stránky
 */
export class EditorPreview {

    options = null;
	webpagesDatatable = null;

    isPreviewClicked = false;
    previewWindow = null;
    previewSubmitOptions = null;

    constructor(options) {
        //console.log("Constructor, options=", options);
        this.options = options;
		this.webpagesDatatable = options.datatable;
    }

    bindEvents() {

        var self = this;

        window.previewPage = function() {
            if (self.webpagesDatatable.EDITOR.s.processing) return;
            if (window.branchScreen?.enabled && window.branchScreen.branchId) {
                self.previewWindow = window.open('about:blank', 'webjetPreviewWindow');
                if (!self.previewWindow) {
                    WJ.notifyError(WJ.translate('editor.preview.error.js'));
                    return;
                }
                const openPreview = async (doc) => {
                    try {
                        const response = await fetch(`/admin/rest/branches/${window.branchScreen.branchId}/preview-link?docId=${doc.docId}`);
                        if (!response.ok) throw new Error(await response.text());
                        self.previewWindow.location = (await response.json()).url;
                    } catch (error) { self.previewWindow.close(); WJ.notifyError(error.message); }
                };
                if (self.webpagesDatatable.EDITOR.currentJson.editorFields?.branchReadOnlyReason) {
                    openPreview(self.webpagesDatatable.EDITOR.currentJson);
                    return;
                }
            }
            // Preview needs every field, even when Editor's changed-only submission would skip preSubmit.
            const options = self.webpagesDatatable.EDITOR.s.editOpts;
            self.previewSubmitOptions = { options, submit: options.submit };
            options.submit = 'all';
            self.isPreviewClicked = true;
            //vykonaj akoze submit, v preSubmit to odchytime a zastavime odoslanie
            self.webpagesDatatable.EDITOR.submit(null, null, null, false);
        }

        self.webpagesDatatable.EDITOR.on('processing', function(e, processing) {
            // Validation can cancel submission before the preview handler runs.
            if (processing === false) {
                if (self.isPreviewClicked && window.branchScreen?.branchId) self.previewWindow?.close();
                self.isPreviewClicked = false;
                if (self.previewSubmitOptions) {
                    self.previewSubmitOptions.options.submit = self.previewSubmitOptions.submit;
                    self.previewSubmitOptions = null;
                }
            }
        });

        self.webpagesDatatable.EDITOR.on('close', function() {
            self.isPreviewClicked = false;
        });

        self.webpagesDatatable.EDITOR.on( 'preSubmit', function ( e, data, action ) {
            if (self.isPreviewClicked) {
                self.isPreviewClicked = false;

                //console.log("preSubmit, e=", e, "data=", data, "action=", action);

                try {
                    var docId = self.webpagesDatatable.EDITOR.currentJson.docId;
                    //console.log("docId=", docId);
                    //DT ma pre novy zaznam index 0, nie -1 ako WJ
                    if (docId < 0) docId = 0;
                    var jsonData = data.data[docId] || Object.values(data.data)[0];
                    //console.log("jsonData=", jsonData);

                    var json = JSON.stringify(jsonData);
                    //fix na checkboxy
                    json = json.replace(/:\[true\]/gi, ":true");
                    json = json.replace(/:\[false\]/gi, ":false");
                    //console.log("json 2: ", json);

                    if (jsonData.id == null) jsonData.id = -1;

                    let url = "/admin/webpages/preview/?docid="+jsonData.id;
                    const branchPreview = window.branchScreen?.enabled && window.branchScreen.branchId;
                    if (branchPreview) url += '&branchId=' + window.branchScreen.branchId + '&branchDomain=' + window.branchScreen.domainId;

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
                                if (branchPreview) self.previewWindow.location = url;
                                else self.previewWindow = window.open(url, "webjetPreviewWindow");
                                window.previewWindow = self.previewWindow;
                            } else {
                                if (branchPreview) self.previewWindow?.close();
                                window.WJ.notifyError(WJ.translate("editor.preview.error.js"), response);
                            }
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            if (branchPreview) self.previewWindow?.close();
                            console.log("ERROR, textStatus=", textStatus, "errorThrown=", errorThrown);
                            window.WJ.notifyError(WJ.translate("editor.preview.error.js"), textStatus);
                        }
                    })
                } catch (ex) {
                    if (window.branchScreen?.branchId) self.previewWindow?.close();
                    WJ.notifyError(WJ.translate("editor.preview.error.js"));
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
