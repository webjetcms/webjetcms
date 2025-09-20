/**
 * User Interface for AI interactions. It uses toastr to show popup with options and status.
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
            this.lastToast = window.toastr.info("<div id='toast-container-ai-content'></div>", "<strong>"+WJ.translate("components.ai_assistants.editor.btn.tooltip.js")+"</strong>", {
                closeButton: true,
                closeHtml: '<button class="toast-close-button" data-toggle="tooltip" title="'+WJ.translate('datatables.modal.close.js')+'"><i class="ti ti-x"></i></button>',
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
                    //get tootltip element and hide tooltip
                    $('#toast-container-ai').find('.toast-close-button').tooltip('disable');
                    $('#toast-container-ai').find('.toast-close-button').tooltip('hide');
                }
            });
            window.lastToast = this.lastToast;
            contentContainer = $("#toast-container-ai-content");
            this.$progressElement = $("#toast-container-ai").find(".toast-progress");

            //container with saved userPromptDialog (to preserve it for undo)
            const $userPromptDialog = $(`<div id="toast-ai-user-prompt-saved" style="display:none"></div>`);
            $("#toast-container-ai .toast-message").append($userPromptDialog);

            // Container with action buttons for handling existing file name conflicts
            const $nameSelection = $(`
                <div id="toast-ai-name-selection" class="ai-name-selection" style="display:none">
                    <button class="btn btn-primary" id="rewrite">
                        ${WJ.translate("components.ai_assistants.saveFile.rewrite.js")}
                    </button>
                    <button class="btn btn-primary" id="rename"></button>
                    <button class="btn btn-outline-secondary" id="back">
                        ${WJ.translate("button.cancel")}
                    </button>
                </div>
            `);

            $("#toast-container-ai .toast-message").append($nameSelection);

            //add tooltip to close button
            $('#toast-container-ai').find('.toast-close-button').tooltip({ trigger: 'hover', customClass: 'tooltip-ai' });
        }

        //set default header
        $("#toast-container-ai").removeClass("has-back-button");
        $("#toast-container-ai .toast-title").html("<strong>"+WJ.translate("components.ai_assistants.editor.btn.tooltip.js")+"</strong>");
        $("#toast-ai-user-prompt-saved").empty();

        $("#toast-container-ai").on("click", (e) => {
            if (e.target.id === "toast-container-ai") {
                //animate zoom in on .toast.toast-info element
                const $toast = $("#toast-container-ai .toast.toast-info");
                $toast.removeClass("ai-toast-zoom"); // Remove if already present
                void $toast[0].offsetWidth; // Force reflow to restart animation
                $toast.addClass("ai-toast-zoom");
            }
        });

        // Clear previous content
        contentContainer.empty();

        // Use jQuery to build content
        let currentGroup = null;
        let lastGroup = null;
        let buttonCounter = 0;
        for (let i = 0; i < column.ai.length; i++) {
            let aiCol = column.ai[i];
            //console.log("aiCol=", aiCol);

            //check value in input, if empty remove if there is no userPromptEnabled
            let hasInput = false;
            if (true === aiCol.userPromptEnabled && "edit_image" !== aiCol.action) hasInput = true;
            else {
                let inputFieldName = aiCol.from;
                if (inputFieldName == null || inputFieldName === "") inputFieldName = aiCol.to;
                let inputValue = this.EDITOR.get(inputFieldName);
                if (inputValue != null && inputValue !== "") hasInput = true;
                else {
                    console.log("inputFieldName=", inputFieldName, "inputValue=", inputValue, "asistent=", aiCol.description, "aiCol=", aiCol);
                }
            }
            if (hasInput === false) continue;

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
                    <i class="ti ti-${aiCol.icon}"></i>
                    ${aiCol.description}
                    ${chatPromptIcon}
                    <span class="provider">${aiCol.providerTitle}</span>
                </button>
            `);

            // Bind click to _executeAction
            btn.on('click', () => {
                //use original button clicked - which shows this popup
                this._renderHeader(button, column, aiCol);

                if (true === aiCol.userPromptEnabled) {
                    //we must first show dialog with textarea and after that call _executeAction
                    this._renderUserPromptDialog(button, column, aiCol);
                } else {
                    this._executeAction(button, column, aiCol);
                }
            });

            // Append button to the container
            contentContainer.append(btn);
            buttonCounter++;
        }
        if (buttonCounter === 0) {
            contentContainer.append($('<div class="no-ai-options"></div>').html(WJ.translate("components.ai_assistants.noOptionsAvailable.js")));
        }
    }

    async generateOtherAssistentOptions(button) {
        let datatableColumn = null;
        await $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/other-button-column/",
            data: {
                "fieldName": "txtUrl", //button.parent().find(".form-control").attr("name"),
                "javaClassName": button.data("ai-java-class"),
                "renderFormat": button.data("ai-render-format")
            },
            success: function(res)
            {
                //console.log("Other button AI assistants:", res);
                if (res == null || res.ai == null || res.ai.length === 0) {
                    console.warn("No AI assistants found for button:", button);
                    return;
                }
                datatableColumn = res;

            },
            error: function(xhr, ajaxOptions, thrownError) {
                console.error("Error loading AI assistants:", thrownError);
            }
        });

        if (datatableColumn != null) {
            this.generateAssistentOptions(button, datatableColumn);
        } else {
            this.setError(WJ.translate("components.ai_assistants.unknownError.js"));
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
        } else if ("components.ai_assistants.stat.totalTokens.js" === textKey || "components.ai_assistants.editor.image_saved.js" === textKey) {
            statusClass = "ai-status-success";
            icon = "circle-check";
            //success replace whole content with success message
            chatErrorContainer = null;
        } else if ("components.ai_assistants.unknownError.js" === textKey) {
            statusClass = "ai-status-error";
            icon = "help-circle";
            if (typeof params != "undefined" && params != null && params.length == 1 && params[0].indexOf("!")==0) {
                //if params starts with ! use it as main text
                textKey = params[0].substring(1);
            }
        } else if ("components.ai_assistants.saveFile.unknownError.js" === textKey) {
            statusClass = "ai-status-error";
            icon = "help-circle";
        }

        if ("components.ai_assistants.stat.totalTokens.js" === textKey && typeof params != "undefined" && params != null && params.length == 1 && params[0] == 0) {
            textKey = "components.ai_assistants.stat.totalTokens-0.js";
        }

        if (pulsate) {
            html += " pulsate-text";
        }
        if (statusClass != "") contentContainer.addClass(statusClass);

        html = html + '"><span>';

        if (icon != "") html += '<i class="ti ti-' + icon + '"></i> ';

        let text = WJ.translate(textKey, params);
        text = WJ.parseMarkdown(text, { link: true, removeLastBr: true });
        html += text + '</span></div><div class="explanatory-text-container" style="display: none"></div>';

        if (statusClass === "ai-status-success" && chatErrorContainer == null) {
            //preserve userPromptDialog if exists
            let userPromptSaved = $("#toast-ai-user-prompt-saved");
            userPromptSaved.empty();
            let userPromptDialog = $("#toast-container-ai-content").find(".user-prompt-container");
            if (userPromptDialog != null && userPromptDialog.length > 0) {
                userPromptSaved.append(userPromptDialog);
            }
        }

        if (chatErrorContainer != null && chatErrorContainer.length > 0) {
            chatErrorContainer.html(html);
        } else {
            contentContainer.html(html);


            if (statusClass === "ai-status-success" && this.editorAiInstance.isUndo()) {
                //generate undo button, on click call undo on editorAiInstance
                let undoButton = $('<div class="text-end"><button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i> ' + WJ.translate("components.ai_assistants.editor.undo.js")+'</button></div>');
                undoButton.find("button").on('click', () => {
                    this.editorAiInstance.undo();
                });
                //generate OK button, on click close toast
                let okButton = $('<div class="text-end"><button class="btn btn-primary btn-ai-ok" type="button"><i class="ti ti-check"></i> ' + WJ.translate("button.ok")+'</button></div>');
                okButton.find("button").on('click', () => {
                    this._closeToast();
                });
                let buttonContainer = $('<div class="ai-status-buttons-container d-flex justify-content-end gap-3 mt-1"></div>');
                buttonContainer.append(undoButton);
                buttonContainer.append(okButton);
                contentContainer.append(buttonContainer);
            }
        }

        //for safety hide previous explanatory text
        if (statusClass == "ai-status-working") this.hideExplanatoryText();
    }

    setError(...params) {
        this.setCurrentStatus("components.ai_assistants.unknownError.js", false, ...params);
    }

    setExplanatoryText(text) {
        if (typeof text != "undefined" && text != null) {
            let explanatoryTextContainer = $("#toast-container-ai").find(".explanatory-text-container");
            explanatoryTextContainer.html(WJ.parseMarkdown(text));
            explanatoryTextContainer.show();
        } else {
            this.hideExplanatoryText();
        }
    }

    hideExplanatoryText() {
        let explanatoryTextContainer = $("#toast-container-ai").find(".explanatory-text-container");
        explanatoryTextContainer.html("");
        explanatoryTextContainer.hide();
    }

    async _executeAction(button, column, aiCol, inputValues = null) {
        this.editorAiInstance._executeAction(button, column, aiCol, inputValues);
    }

    _showLoader(button) {
        button.parents(".DTE_Field").addClass("ai-loading");
    }

    _hideLoader(button) {
        button.parents(".DTE_Field").removeClass("ai-loading");
    }

    _closeToast(timeOut) {
        if (typeof timeOut === "undefined" || timeOut == null || timeOut < 1) {
            $("#toast-container-ai").find(".toast-close-button").trigger("click");
        }

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
            //toastr.clear(self.lastToast);
            $("#toast-container-ai").find(".toast-close-button").trigger("click");
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
            var value = percent + "%";
            if (percent == null || percent == "") value = "";
            this.setCurrentStatus("components.ai_assistants.browser.downloadingModel.js", false, value);
            if (this.$progressElement != null) this.$progressElement.width(percent + '%');
        }
    }

    _renderHeader(button, column, aiCol) {
        let header = `
            <div class="header-back-button">
                <button class="btn btn-outline-secondary"><i class="ti ti-chevron-left"></i> ${WJ.translate("components.ai_assistants.user_prompt.back.js")}</button>
                <div class="ai-title">
                    <i class="ti ti-${aiCol.icon}"></i>
                    <span>
                        ${aiCol.description}
                    </span>
                    <span class="provider">
                    (${aiCol.providerTitle})
                    </span>
                </div>
            </div>
        `;

        $("#toast-container-ai").addClass("has-back-button");
        $("#toast-container-ai .toast-title").html(header);

        $("#toast-container-ai .toast-title .btn-outline-secondary").on('click', () => {
            this.generateAssistentOptions(button, column);
        });
    }

    async _renderUserPromptDialog(button, column, aiCol) {
        //console.log("_renderUserPromptDialog, button=", button, "column=", column, "aiCol=", aiCol);

        let bonusHtml = await this._getBonusHtml(aiCol.assistantId);
        let html = `
            <div class="user-prompt-container">
                <div class="chat-error-container"></div>
                <div class="mb-3">
                    <textarea id="ai-user-prompt" class="form-control" rows="4" placeholder="${aiCol.userPromptLabel}"></textarea>
                    ${bonusHtml}
                </div>
            </div>
        `;

        let contentContainer = $("#toast-container-ai-content");
        contentContainer.html(html);

        const btn = $(`
            <div class="text-end">
                <button class="btn btn-primary" type="button">
                    <i class="ti ti-sparkles"></i>
                    ${WJ.translate("components.ai_assistants.user_prompt.generate.js")}
                </button>
            </div>
        `);
        btn.find("button").on('click', () => {

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

        contentContainer.find(".user-prompt-container").append(btn);
    }

    async getPathForNewImage() {
        try {
            let docId = this.EDITOR.get("id");
            let groupDetails = this.EDITOR.get("editorFields.groupDetails");
            let title = this.EDITOR.get("title");
            let groupId = null;
            if (groupDetails != null) groupId = groupDetails.groupId;

            if (typeof docId === "undefined" || docId == null) {
                docId = window.parent.getCkEditorInstance().element.$.form.docId.value
            }
            if (typeof title === "undefined" || title == null) {
                if (typeof getPageNavbar === "function") title = getPageNavbar();
                else title = "";
            }
            if (typeof groupId === "undefined" || groupId == null) {
                groupId = window.parent.getCkEditorInstance().element.$.form.groupId.value
            }

            //console.log("docId=", docId, "groupDetails=", groupDetails, "title=", title);

            if (typeof docId !== "undefined" && typeof groupId !== "undefined" && typeof title !== "undefined" && docId != null && groupId != null && title != null) {
                let url = "/admin/rest/ai/assistant/new-image-location/";
                const res = await $.ajax({
                    type: "POST",
                    url: url,
                    data: {
                        docId: docId,
                        groupId: groupId,
                        title: title
                    }
                });

                if(res == undefined || res == null) return "/images/";

                return res;
            }
        } catch(err) {
            //,aybe it's not DocDetails, we don't know docId/groupId
            console.log(err);
        }
        return "/images/";
    }

    async renderImageSelection(button, aiCol, images, generatedImageName, toField, textKey, ...params) {
        let contentContainer = $("#toast-container-ai-content");

        let self = this;
        let imageUrl = null;
        let imageName = 'ai-image';

        let actualValue = self.EDITOR.get(toField);

        //remove /thumb prefix if exists
        if (typeof actualValue != "undefined" && actualValue != null) {
            if (actualValue.startsWith("/thumb")) actualValue = actualValue.substring("/thumb".length);
            //remove any parameters
            actualValue = actualValue.split("?")[0];
        }

        if(actualValue != undefined && actualValue != null && actualValue.length && /^\/.+\.(jpg|jpeg|png|gif|webp|svg)$/i.test(actualValue)) {
            //Use current location and name
            const lastIndex = actualValue.lastIndexOf("/");
            imageUrl = actualValue.substring(0, lastIndex + 1);
            imageName = actualValue.substring(lastIndex + 1);
        } else {
            imageUrl = await this.getPathForNewImage();
            if(generatedImageName != null && generatedImageName != undefined && generatedImageName.length) {
                imageName = generatedImageName;
            }
        }

        this.setCurrentStatus(textKey, false, ...params);

        let currentStatus = contentContainer.html();

        let html = `
            <div class='chat-error-container'>${currentStatus}</div>
            <div>${WJ.translate("components.ai_assistants.temp_file_storage.select_image.js")}</div>
            <div class='ai-image-preview-div mb-3'></div>

            <div class='image-info' style='display: none;'>
                <div class='mb-3'>
                    <label for='generated-image-name' style='color: black;'>${WJ.translate("components.ai_assistants.temp_file_storage.file_name.js")}</label>
                    <input id='generated-image-name' type='text' class='form-control' value='${imageName}'>
                </div>
                <div class='mb-3'>
                    <label for='editorAppimageLocation' style='color: black;'>${WJ.translate("components.ai_assistants.temp_file_storage.file_dir.js")}</label>
                    <div class="col-auto" data-toggle="tooltip">
                        <input type="text" class="webjet-dte-jstree" id="imageLocation"/>
                    </div>
                </div>
            </div>
            <div class='button-div text-end' style='display: none;'> </div>
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
                    <a href="/admin/rest/ai/assistant/file/binary/?fileName=${imageName}" target="_blank" class="zoom-in"><i class="ti ti-zoom-in"></i></a>
                </div>
            `);
            imgDiv.find('img').on('click', () => {
                this._selectImage(imageName);
            });
            previewDiv.append(imgDiv);
        }

        let buttonDiv = contentContainer.find('.button-div');

        if (aiCol.userPromptEnabled===true) {
            const editInstructionsButton = $('<button class="btn btn-outline-secondary me-2 edit-instructions"><i class="ti ti-pencil"></i> ' + WJ.translate("components.ai_assistants.editor.edit_prompt.js") + '</button>');
            editInstructionsButton.on('click', () => {
                //this.generateAssistentOptions(button, column);
                self.editorAiInstance.revertUserPrompt();
            });
            buttonDiv.append(editInstructionsButton);
        }

        const imageButton = $('<button class="btn btn-primary select-image"><i class="ti ti-download"></i> ' + WJ.translate("components.ai_assistants.editor.select_image.js") + '</button>');
        imageButton.on('click', () => {
            this._chooseNameAndSaveImage(generatedImageName, button, toField);
        });
        buttonDiv.append(imageButton);

        if (images.length>0) this._selectImage(images[0]);
    }

    _chooseNameAndSaveImage(generatedImageName, button, toField) {
        let self = this;
        let imageName = $("#generated-image-name").val();
        let imageLocation = $("#editorAppimageLocation").find("input").val();

        //Hide part to select image
        $("#toast-container-ai-content").hide();

        $.ajax({
            type: "GET",
            url: "/admin/rest/ai/assistant/check-name-location/",
            data: {
                "fileLocation": imageLocation,
                "currentName": imageName,
                "generatedName": generatedImageName
            },
            success: function(res)
            {
                if (res && res.error) {
                    self.setCurrentStatus("components.ai_assistants.saveFile.unknownError.js", false, res.error);
                    $("#toast-container-ai-content").show();
                    return;
                }

                let currentNameExist = false;
                let generatedNameExist = false;
                let arr = JSON.parse(res.response);

                for(let i = 0; i < arr.length; i++) {
                    if(imageName === arr[i].name) {
                        currentNameExist = arr[i].doExist;
                    }

                    if(generatedImageName === arr[i].name) {
                        generatedNameExist = arr[i].doExist;
                    }
                }

                if(currentNameExist === true) {
                    //Current name exist, we must show options too user
                    $("#toast-ai-name-selection").css({ display: "grid" });

                    let buttonRewrite = $("#toast-ai-name-selection").find("#rewrite");
                    let buttonBack = $("#toast-ai-name-selection").find("#back");

                    buttonRewrite.on("click", () => {
                        self._saveAndSetImage(button, toField, imageName, imageLocation);
                        $("#toast-ai-name-selection").hide();
                        $("#toast-container-ai-content").show();
                        return;
                    });

                    buttonBack.on("click", () => {
                        $("#toast-ai-name-selection").hide();
                        $("#toast-container-ai-content").show();
                        return;
                    });

                    let buttonRename = $("#toast-ai-name-selection").find("#rename");
                    if(generatedNameExist === false) {
                        buttonRename.show();
                        buttonRename.text(WJ.translate("components.ai_assistants.saveFile.rename.js", generatedImageName));

                        // Back button action: hide the name selection panel and show original content again
                        buttonRename.on("click", () => {
                            self._saveAndSetImage(button, toField, generatedImageName, imageLocation);
                            $("#toast-ai-name-selection").hide();
                            $("#toast-container-ai-content").show();
                            return;
                        });
                    } else {
                        buttonRename.hide();
                    }
                } else {
                    self._saveAndSetImage(button, toField, imageName, imageLocation);
                    $("#toast-ai-name-selection").hide();
                    $("#toast-container-ai-content").show();
                    return;
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {
                self.setCurrentStatus("components.ai_assistants.saveFile.unknownError.js", false, res.error);
                $("#toast-container-ai-content").show();
            }
        });
    }

    _selectImage(imageName) {
        $("div.button-div").show();
        $("div.image-info").show();

        $("div.image-preview").find("img.selected").removeClass("selected");
        $("div.image-preview").find('img[alt="' + imageName + '"]').addClass("selected");
    }

    _saveAndSetImage(button, toField, imageName, imageLocation) {
        let self = this;
        let tempFileName = $(".image-preview").find("img.selected").attr("alt");

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
                self.setCurrentStatus("components.ai_assistants.editor.image_saved.js");
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