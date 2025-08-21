/**
 * User Interface for AI interactions
 */

export class AiUserInterface {

    EDITOR = null;
    lastToast = null;
    progressBar = {
        intervalId: null,
        hideEta: null,
        maxHideTime: null
    };
    $progressElement = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    generateAssistentOptions(button, column) {
        //console.log("AI button clicked for column:", column);

        this._clearProgress();
        let self = this;

        let contentContainer = $("#toast-container-ai-content");
        if (contentContainer.length === 0) {
            this.lastToast = window.toastr.info("<div id='toast-container-ai-content'></div>", WJ.translate("components.ai_assistants.editor.btn.tooltip.js"), {
                closeButton: true,
                timeOut: 0,
                tapToDismiss: false,
                extendedTimeOut: 0,
                progressBar: false,
                containerId: 'toast-container-ai',
                positionClass: 'toast-container toast-top-center',
                preventDuplicates: true,
                progressBar: true,
                onCloseClick: () => {
                    self._clearProgress();
                    self._hideLoader(button, column);
                    if (self.aiBrowserExecutor != null) self.aiBrowserExecutor.destroy();
                }
            });
            window.lastToast = this.lastToast;
            contentContainer = $("#toast-container-ai-content");
            this.$progressElement = $("#toast-container-ai").find(".toast-progress");
        }

        // Clear previous content
        contentContainer.empty();

        // Use jQuery to build content
        let currentGroup = null;
        for (let i = 0; i < column.ai.length; i++) {
            let aiCol = column.ai[i];
            //console.log("aiCol=", aiCol);

            // Add group titles at specific indexes
            if (i === 0) {
                currentGroup = $('<div class="group-title">Generovanie</div>');
                contentContainer.append(currentGroup);
            }
            if (i === 2) {
                currentGroup = $('<div class="group-title">Gramatika</div>');
                contentContainer.append(currentGroup);
            }

            // Create button element
            let buttonText = aiCol.description;
            if (typeof buttonText === 'undefined' || buttonText === null || buttonText === "") buttonText = aiCol.assistant;
            const btn = $(`
                <button class="btn btn-light btn-ai-action" type="button">
                    <i class="ti ti-clipboard-text ti-${aiCol.icon}"></i>
                    ${buttonText}
                    <span class="provider">${aiCol.providerTitle}</span>
                </button>
            `);

            // Bind click to _executeAction
            btn.on('click', () => {
                //use original button clicked - which shows this popup

                if ("PROMPT"===aiCol.from) {

                } else {
                    this._processAiActionButtonClick(button, column, aiCol);
                }
            });

            // Append button to the container
            contentContainer.append(btn);
        }
    }

    setCurrentStatus(textKey, pulsate = false, ...params) {
        let contentContainer = $("#toast-container-ai-content");
        let html = '<div class="group-title"';

        if (pulsate) {
            html += " pulsate-text";
        }

        html = html + '">' + WJ.translate(textKey, params) + '</div>';
        contentContainer.html(html);
    }

    async _processAiActionButtonClick(button, column, aiCol) {
        this.editorAiInstance._executeAction(button, column, aiCol);
    }

    _showLoader(button) {
        button.parents(".DTE_Field").addClass("ai-loading");
        let input = button.parents(".DTE_Field").find(".form-control");
        if (input.val()=="") input.val(WJ.translate("components.ai_assistants.editor.loading.js"));
    }

    _hideLoader(button) {
        button.parents(".DTE_Field").removeClass("ai-loading");
        let input = button.parents(".DTE_Field").find(".form-control");
        if (input.val()==WJ.translate("components.ai_assistants.editor.loading.js")) input.val("");
    }

    _closeToast(timeOut) {
        let progressBar = this.progressBar;

        //console.log("progressBar=", progressBar, "this.progressBar=", this.progressBar);

        progressBar.maxHideTime = parseFloat(timeOut);
        progressBar.hideEta = new Date().getTime() + timeOut;
        progressBar.intervalId = setInterval(this._updateProgress, 50, this);
    }

    _updateProgress(self) {
        let progressBar = self.progressBar;

        //console.log("progressBar=", progressBar, "this.progressBar=", self.progressBar, "self.$progressElement", self.$progressElement);

        var percentage = ((progressBar.hideEta - (new Date().getTime())) / progressBar.maxHideTime) * 100;
        self.$progressElement.width(percentage + '%');

        if (percentage <= 1) {
            clearInterval(progressBar.intervalId);
            toastr.clear(self.lastToast);
        }
    }

    _clearProgress() {
        let progressBar = this.progressBar;

        if (progressBar.intervalId != null) clearInterval(progressBar.intervalId);
        if (this.$progressElement != null) this.$progressElement.width('0%');
    }

    _setDownloadStatus(percent) {
        console.log("Download progress:", percent);
        this.setCurrentStatus("components.ai_assistants.browser.downloadingModel.js", false, percent + "%");
    }

    _setImageStatus(images, toField, textKey, ...params) {
        let contentContainer = $("#toast-container-ai-content");

        let html = "<div>" + WJ.translate(textKey, params) + "</div>";
        html += "<div class='ai-image-preview-div'></div>";
        html += "<div class='button-div' style='display: none;'> </div>";

        html += `
            <div class='image-info' style='display: none;'>
                <div>
                    <label for='generated-image-name' style='color: black;'>ImageName</label>
                    <input id='generated-image-name' type='text' class='form-control'>
                </div>

                <div>
                    <label for='editorAppimageLocation' style='color: black;'>ImageLocation</label>
                    <div class="col-auto" data-toggle="tooltip">
                        <input type="text" class="webjet-dte-jstree" id="imageLocation"/>
                    </div>
                </div>


            </div>
        `;

        contentContainer.html(html);

        $("div.image-info").find("input.webjet-dte-jstree").each(async function(index) {
                var $element = $(this);

                console.log("html=", $element[0].outerHTML, "val=", $element.val(), "text=", $element.data("text"));

                var id = $element.attr("id");
                var htmlCode = $('<div class="vueComponent" id="editorApp'+id+'"><webjet-dte-jstree :data-table-name="dataTableName" :data-table="dataTable" :click="click" :id-key="idKey" :data="data" :attr="attr" @remove-item="onRemoveItem"></webjet-dte-jstree></div>');
                htmlCode.insertAfter($element);

                function fixNullData(data, click) {
                    //console.log("fixNullData, data=", data, "click=", click);
                    //ak to je pole neriesime, ponechame bezo zmeny
                    if (click.indexOf("-array")!=-1) return data;
                    //ak to nie je pole, musime nafejkovat jeden objekt aby sa pole aspon zobrazilo (a dala sa zmenit hodnota)
                    if (data.length==0) {
                        let emptyItem = {
                            fullPath: ""
                        }
                        emptyItem.id = -1;

                        return [emptyItem];
                    }
                    return data;
                }

                let conf = {
                    jsonData: [{
                        virtualPath: "",
                        id: "",
                        type: "DIR"
                    }],
                    className: "dt-tree-dir-simple",
                    _id: id
                };
                //console.log("conf=", conf);
                const app = window.VueTools.createApp({
                    components: {},
                    data() {
                        return {
                            data: null,
                            idKey: null,
                            dataTable: null,
                            dataTableName: null,
                            click: null,
                            attr: null
                        }
                    },
                    created() {
                        this.data = fixNullData(conf.jsonData, conf.className);
                        //console.log("JS created, data=", this.data, " conf=", conf, " val=", conf._input.val());
                        this.idKey = conf._id;
                        //co sa ma stat po kliknuti prenasame z atributu className datatabulky (pre jednoduchost zapisu), je to hodnota obsahujuca dt-tree-
                        //priklad: className: "dt-row-edit dt-style-json dt-tree-group", click=dt-tree-group
                        const confClassNameArr = conf.className.split(" ");
                        for (var i=0; i<confClassNameArr.length; i++) {
                            let className = confClassNameArr[i];
                            this.click = className;
                        }
                        //console.log("click=", this.click);
                        if (typeof(conf.attr)!="undefined") this.attr = conf.attr;

                        this.dataTable = {
                            DATA: {}
                        }
                    },
                    methods: {
                        onRemoveItem(id){
                        }
                    }
                });
                VueTools.setDefaultObjects(app);
                app.component('webjet-dte-jstree', window.VueTools.getComponent('webjet-dte-jstree'));
                const vm = app.mount($element.parent().find("div.vueComponent")[0]);
                //console.log("Setting vm, input=", element, "vm=", vm);
                $element.data("vm", vm);
                //console.log("set vm=", $element.data("vm"));
                $element.hide();
            });


        let previewDiv = contentContainer.find('.ai-image-preview-div');

        for (let i = 0; i < images.length; i++) {
            const imageName = images[i];
            const imgDiv = $(`
                <div class="image-preview">
                    <img src="/admin/rest/ai/assistant/file/binary/?fileName=${imageName}" alt="${imageName}" />
                </div>
            `);
            imgDiv.find('img').on('click', () => {
                this._selectImage(imageName);
            });
            previewDiv.append(imgDiv);
        }

        let buttonDiv = contentContainer.find('.button-div');

        const imageButton = $('<button class="btn btn-outline-secondary select-image">' + WJ.translate("components.ai.editor.select_image.js") + '</button>');
        imageButton.on('click', () => {
            this._saveAndSetImage(toField);
        });
        buttonDiv.append(imageButton);
    }

    _selectImage(imageName) {
        console.log("Selected image:", imageName);

        $("div.button-div").show();
        $("div.image-info").show();

        $("div.image-preview").find("img.selected").removeClass("selected");
        $("div.image-preview").find('img[alt="' + imageName + '"]').addClass("selected");
    }

    _saveAndSetImage(toField) {
        let self = this;
        let tempFileName = $(".image-preview").find("img.selected").attr("alt");
        let imageName = $("#generated-image-name").val();
        let imageLocation = $("#editorAppimageLocation").find("input").val();

        $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/save-temp-file/",
            data: {
                "tempFileName": tempFileName,
                "imageName": imageName,
                "imageLocation": imageLocation
            },
            success: function(res)
            {
                self.EDITOR.set(toField, res);
                self.setCurrentStatus("components.ai.editor.image_saved.js");
                self._closeToast(3000);
            },
            error: function(xhr, ajaxOptions, thrownError) {

            }
        });
    }

}