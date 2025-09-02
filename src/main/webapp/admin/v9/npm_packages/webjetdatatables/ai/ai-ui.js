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
                closeHtml: '<i class="ti ti-x"></i>',
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
                    self._hideLoader(button);
                    if (self.aiBrowserExecutor != null) self.aiBrowserExecutor.destroy();
                }
            });
            window.lastToast = this.lastToast;
            contentContainer = $("#toast-container-ai-content");
            this.$progressElement = $("#toast-container-ai").find(".toast-progress");
        }

        //set default header
        $("#toast-container-ai").removeClass("has-back-button");
        $("#toast-container-ai .toast-title").html(WJ.translate("components.ai_assistants.editor.btn.tooltip.js"));

        // Clear previous content
        contentContainer.empty();

        // Use jQuery to build content
        let currentGroup = null;
        let lastGroup = null;
        for (let i = 0; i < column.ai.length; i++) {
            let aiCol = column.ai[i];
            //console.log("aiCol=", aiCol);

            currentGroup = aiCol.groupName;
            if (currentGroup !== null && currentGroup !== "" && currentGroup !== lastGroup) {
                contentContainer.append($('<div class="group-title"></div>').text(currentGroup));
                lastGroup = currentGroup;
            }

            // Create button element
            let chatPromptIcon = "";
            if (true === aiCol.userPromptEnabled) chatPromptIcon = "<i class='ti ti-blockquote has-user-prompt'></i>";
            const btn = $(`
                <button class="btn btn-light btn-ai-action" type="button">
                    <i class="ti ti-clipboard-text ti-${aiCol.icon}"></i>
                    ${aiCol.description}
                    ${chatPromptIcon}
                    <span class="provider">${aiCol.providerTitle}</span>
                </button>
            `);

            // Bind click to _executeAction
            btn.on('click', () => {
                //use original button clicked - which shows this popup

                if (true === aiCol.userPromptEnabled) {
                    //we must first show dialog with textarea and after that call _executeAction
                    this._renderUserPromptDialog(button, column, aiCol);
                } else {
                    this._executeAction(button, column, aiCol);
                }
            });

            // Append button to the container
            contentContainer.append(btn);
        }
    }

    setCurrentStatus(textKey, pulsate = false, ...params) {
        let contentContainer = $("#toast-container-ai-content");

        //reset contentContainer class
        contentContainer.attr("class", "");

        let chatErrorContainer = contentContainer.find(".chat-error-container");
        let html = '<div class="current-status';

        let statusClass = "";
        let icon = "";
        if ("components.ai_assistants.editor.loading.js" === textKey) {
            statusClass = "ai-status-working";
            icon = "exclamation-circle";
        } else if ("components.ai_assistants.stat.totalTokens.js" === textKey || "components.ai.editor.image_saved.js" === textKey) {
            statusClass = "ai-status-success";
            icon = "circle-check";
            //success replace whole content with success message
            chatErrorContainer = null;
        } else if ("components.ai_assistants.unknownError.js" === textKey) {
            statusClass = "ai-status-error";
            icon = "help-circle";
        }

        if (pulsate) {
            html += " pulsate-text";
        }
        if (statusClass != "") contentContainer.addClass(statusClass);

        html = html + '">';

        if (icon != "") html += '<i class="ti ti-' + icon + '"></i> ';

        let text = WJ.translate(textKey, params);
        text = WJ.parseMarkdown(text, { link: true });
        html += text + '</div>';

        if (chatErrorContainer != null && chatErrorContainer.length > 0) {
            chatErrorContainer.html(html);
        } else {
            contentContainer.html(html);
        }
    }

    setError(...params) {
        this.setCurrentStatus("components.ai_assistants.unknownError.js", false, ...params);
    }

    async _executeAction(button, column, aiCol, inputValues = null) {
        this.editorAiInstance._executeAction(button, column, aiCol, inputValues);
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

    setDownloadStatus(percent) {
        //console.log("Download progress:", percent);
        if (percent >= 99) {
            this.setCurrentStatus("components.ai_assistants.browser.optimizingModel.js", false);
            if (this.$progressElement != null) this.$progressElement.width('0%');
        } else {
            this.setCurrentStatus("components.ai_assistants.browser.downloadingModel.js", false, percent + "%");
            if (this.$progressElement != null) this.$progressElement.width(percent + '%');
        }
    }

    async _renderUserPromptDialog(button, column, aiCol) {
        //console.log("_renderUserPromptDialog, button=", button, "column=", column, "aiCol=", aiCol);

        let header = `
            <div class="header-back-button">
                <button class="btn btn-outline-secondary"><i class="ti ti-chevron-left"></i> ${WJ.translate("components.ai_assistants.user_prompt.back.js")}</button>
                <i class="ti ti-clipboard-text ti-${aiCol.icon}"></i>
                <span>
                    ${aiCol.description}
                </span>
            </div>
        `;

        $("#toast-container-ai").addClass("has-back-button");
        $("#toast-container-ai .toast-title").html(header);
        $("#toast-container-ai .toast-title .btn-outline-secondary").on('click', () => {
            this.generateAssistentOptions(button, column);
        });

        let bonusHtml = await this._getBonusHtml(aiCol.assistantId);
        let html = `
            <div class="mb-3">
                <textarea id="ai-user-prompt" class="form-control" rows="4" placeholder="${aiCol.userPromptLabel}"></textarea>
                ${bonusHtml}
            </div>
            <div class="chat-error-container"></div>
        `;

        let contentContainer = $("#toast-container-ai-content");
        contentContainer.html(html);

        const btn = $(`
            <button class="btn btn-primary" type="button">
                <i class="ti ti-sparkles"></i>
                ${WJ.translate("components.ai_assistants.user_prompt.generate.js")}
            </button>
        `);
        btn.on('click', () => {

            let inputValues = {
                "userPrompt" : $("#ai-user-prompt").val()
            };

            //Add bonus content
            const prefix = "bonusContent-";
            $(`input[id^='${prefix}'], select[id^='${prefix}']`).each(function () {
                let key = this.id.substring(prefix.length);
                inputValues[key] = $(this).val();
            });

            this._executeAction(button, column, aiCol, inputValues);
        });

        contentContainer.append(btn);
    }

    async getPathForNewImage(self) {
        try {
            //TODO: if current field has path /images/gallery/test-vela-foto/o_img04152.jpg preserve this path and file name (so replace existing image)

            const docId = self.EDITOR.get("id");
            const groupId = self.EDITOR.get("editorFields.groupDetails");
            const title = self.EDITOR.get("title");

            //console.log("docId=", docId, "groupId=", groupId.groupId, "title=", title);

            let url = "/admin/rest/ai/assistant/new-image-location/?";
            url += "docId=" + docId + "&groupId=" + groupId.groupId + "&title=" + title;

            const res = await $.ajax({
                type: "GET",
                url: url
            });

            if(res == undefined || res == null) return "/images/";

            return res;
        } catch(err) {
            //,aybe it's not DocDetails, we don't know docId/groupId
            //console.log(err);
            return "/images/";
        }
    }

    async renderImageSelection(button, images, toField, textKey, ...params) {
        let contentContainer = $("#toast-container-ai-content");

        let self = this;
        let imageUrl = null;
        let imageName = 'ai-image';

        let actualValue = self.EDITOR.get(toField);
        if(actualValue != undefined && actualValue != null && actualValue.length && /^\/.+\.(jpg|jpeg|png|gif|webp|svg)$/i.test(actualValue)) {
            //Use current location and name
            const lastIndex = actualValue.lastIndexOf("/");
            imageUrl = actualValue.substring(0, lastIndex + 1);
            imageName = actualValue.substring(lastIndex + 1);
            imageName = imageName.split(".")[0];
        } else {
            imageUrl = await this.getPathForNewImage(self);
        }

        this.setCurrentStatus(textKey, false, ...params);

        let currentStatus = contentContainer.html();

        let html = `
            <div class='ai-image-preview-div mb-3'></div>
            <div class='chat-error-container'>${currentStatus}</div>
            <div class='button-div' style='display: none;'> </div>

            <div class='image-info' style='display: none;'>
                <div>
                    <label for='generated-image-name' style='color: black;'>ImageName</label>
                    <input id='generated-image-name' type='text' class='form-control' value='${imageName}'>
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

        $("div.image-info").find("input.webjet-dte-jstree").each(async function() {
            var $element = $(this);

            //console.log("html=", $element[0].outerHTML, "val=", $element.val(), "text=", $element.data("text"));

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
                    virtualPath: imageUrl,
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

        const imageButton = $('<button class="btn btn-primary select-image"><i class="ti ti-download"></i> ' + WJ.translate("components.ai.editor.select_image.js") + '</button>');
        imageButton.on('click', () => {
            this._saveAndSetImage(button, toField);
        });
        buttonDiv.append(imageButton);
    }

    _selectImage(imageName) {
        //console.log("Selected image:", imageName);

        $("div.button-div").show();
        $("div.image-info").show();

        $("div.image-preview").find("img.selected").removeClass("selected");
        $("div.image-preview").find('img[alt="' + imageName + '"]').addClass("selected");
    }

    _saveAndSetImage(button, toField) {
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
                if (res && res.error) {
                    self.setError(res.error);
                    return;
                }

                self.EDITOR.set(toField, res.response);
                self.setCurrentStatus("components.ai.editor.image_saved.js");
                self._closeToast(3000);
                self._hideLoader(button);
            },
            error: function(xhr, ajaxOptions, thrownError) {
                self.setError(thrownError);
            }
        });
    }

    async _getBonusHtml(assistantId) {
        try {
            const res = await $.ajax({
                type: "GET",
                url: "/admin/rest/ai/assistant/bonus-content/?assistantId=" + assistantId,
                contentType: "application/x-www-form-urlencoded; charset=UTF-8"
            });

            if(res == undefined || res == null) return "";

            return res;
        } catch(err) {
            return "";
        }
    }
}