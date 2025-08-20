import { AiLocalExecutor } from "./ai-local-executor";

export class EditorAi {

    EDITOR = null;
    lastToast = null;
    progressBar = {
        intervalId: null,
        hideEta: null,
        maxHideTime: null
    };
    $progressElement = null;
    aiLocalExecutor = null;

    //constructor
    constructor(EDITOR) {
        this.EDITOR = EDITOR;
        //console.log("EditorAi instance created, editor=", this.EDITOR);
        this.aiLocalExecutor = new AiLocalExecutor(EDITOR, this);
        EDITOR.editorAi = this;

        this.bindEvents();
    }

    bindEvents() {
        window.addEventListener("WJ.DTE.opened", (event) => {
            //console.log("WJ.DTE.opened event received, detail=", event.detail, "this.id=", this.EDITOR.TABLE.DATA.id);
            if (this.EDITOR.TABLE.DATA.id===event.detail.id) {
                //console.log("WJ.DTE.open event triggered");
                this.bindButtons();
            }
        });
    }

    /**
     * Binds AI-related buttons to the provided EDITOR instance.
     */
    bindButtons() {
        //console.log("Binding editor buttons, editor=", this.EDITOR);

        //iterate over EDITOR.TABLE.DATA.columns[].ai fields and generate AI button
        this.EDITOR.TABLE.DATA.fields.forEach(column => {
            if("object" == typeof column.ai && column.ai != null && column.ai.length > 0) {
                let field = this.EDITOR.field(column.name);
                if (field) {
                    //console.log("Field found for column:", column.name, "field=", field.dom);
                    //append button into field
                    //field.dom.inputControl[0].appendChild(button);
                    //wrap inputControl into group using jquery wrap function
                    if ("wysiwyg"===column.type) {
                        //console.log("SOM DATA: column=", column, "field=", field);
                        let exitInlineEditorContainer = $(field.dom.inputControl[0]).find(".exit-inline-editor");
                        //console.log("exitInlineEditorContainer=", exitInlineEditorContainer);

                        if (exitInlineEditorContainer.find(".ti-sparkles").length === 0) {
                            const button = $('<button class="btn btn-outline-secondary btn-ai btn-ai-wysiwyg" type="button" data-toggle="tooltip" title="'+WJ.translate('components.ai_assistants.editor.btn.tooltip.js')+'"><i class="ti ti-sparkles"></i><i class="ti ti-loader"></i></button>');
                            button.on('click', () => {
                                this._handleAiButtonClick(button, column);
                            });
                            exitInlineEditorContainer.prepend(button);
                        }
                    } else {
                        let inputField = $(field.dom.inputControl[0]).find(".form-control");

                        //if it doesnt have input-group, wrap it
                        if (inputField.parents(".input-group").length === 0) {
                            inputField.wrap('<div class="input-group"></div>');
                        }

                        //if it doesnt have ti-sparkles button add it
                        if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                            const button = $('<button class="btn btn-outline-secondary btn-ai" type="button" data-toggle="tooltip" title="'+WJ.translate('components.ai_assistants.editor.btn.tooltip.js')+'"><i class="ti ti-sparkles"></i><i class="ti ti-loader"></i></button>');
                            button.on('click', () => {
                                this._handleAiButtonClick(button, column);
                            });
                            inputField.parents(".input-group").append(button);
                        }
                    }
                }
            }
        });
    }

    getColumnType(editor, fieldName) {
        let className = editor?.field(fieldName)?.s?.opts?.className ?? "";
        let renderFormat = editor?.field(fieldName)?.s?.opts?.renderFormat ?? "";

        if(className.indexOf("image") != -1 || renderFormat.indexOf("dt-format-image") != -1) return "image";
        return "text";
    }

    _setCurrentStatus(textKey, pulsate = false, ...params) {
        let contentContainer = $("#toast-container-ai-content");
        let html = '<div class="group-title"';

        if (pulsate) {
            html += " pulsate-text";
        }

        html = html + '">' + WJ.translate(textKey, params) + '</div>';
        contentContainer.html(html);
    }

    _handleAiButtonClick(button, column) {
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
                    console.log("Closing toast");
                    self._clearProgress();
                    self._hideLoader(button, column);
                    if (self.aiLocalExecutor != null) self.aiLocalExecutor.destroy();
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
                </button>
            `);

            // Bind click to _executeAction
            btn.on('click', () => {
                //use original button clicked - which shows this popup

                if(this.getColumnType(this.EDITOR, column?.name) === "image") {
                    // IS IMAGE
                    this._executeImageAction(button, aiCol, column)
                        .then(result => {
                            console.log("AJAX success:", result);

                            this._setImageStatus(result.tempFiles, aiCol.to, "components.ai.editor.generated_images.js", result.totalTokens);

                        })
                        .catch(err => {
                            console.error("AJAX failed:", err);
                        });
                }
                else if(aiCol.useStreaming === true) {
                    this._executeStreamAction(button, aiCol);
                }
                else {
                    this._executeAction(button, column, aiCol);
                }

            });

            // Append button to the container
            contentContainer.append(btn);
        }
    }

    async _executeAction(button, column, aiCol) {
        // Implement AI button click handling logic here
        //console.log("Executing action for AI column:", aiCol);

        let self = this;
        self._showLoader(button, column);

        let from = aiCol.from;
        if (from == null || from == "")  from = aiCol.to; //if from is not set, use to as from

        var isPageBuilder = false;
        if ("wysiwyg" === column.type && aiCol.to === column.name) {
            try {
                let wjeditor = this.EDITOR.field(aiCol.to).s.opts.wjeditor;
                //for PageBuilder if to===pageBuilder element we need to execute function for every editor in PB page to prevent HTML structure issues
                if (wjeditor != null && "pageBuilder" === wjeditor.editingMode) isPageBuilder = true;
            } catch (e) {
                console.error("Error checking PageBuilder editor:", e);
            }
        }

        let totalTokens = 0;

        if (isPageBuilder) {
            //console.log("Executing action for PageBuilder editor:", aiCol, "column", column);

            //get all PB editor instances and execute action on them separately
            let editors = this.EDITOR.field(aiCol.to).s.opts.wjeditor.getWysiwygEditors();
            for (let i=0; i<editors.length; i++) {
                let editor = editors[i];
                //this._executeSingleAction(button, column, aiCol, from, editor);
                //console.log("Executing on editor: ", editor);

                self._setCurrentStatus("components.ai_assistants.editor.loading.js", false, (i+1)+"/"+editors.length);

                let inputData = {
                    type: "text",
                    value: editor.getData()
                }
                totalTokens += await this._executeSingleAction(button, column, aiCol, JSON.stringify(inputData), (response) => {
                    //console.log("response="+response, "setting to editor: ", editor);
                    editor.setData(response);
                });
            };

        } else {
            self._setCurrentStatus("components.ai_assistants.editor.loading.js");

            let inputData = {
                type: this.getColumnType(this.EDITOR, from),
                value: self.EDITOR.get(from)
            }
            totalTokens = await this._executeSingleAction(button, column, aiCol, JSON.stringify(inputData));
        }

        if (totalTokens >= 0) {
            self._closeToast(3000);
            self._hideLoader(button, column);

            self._setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, totalTokens);
        } else {
            //there seems to be an error
            self._closeToast(3000);
            self._hideLoader(button, column);

            self._setCurrentStatus("components.ai_assistants.unknownError.js", false);
        }
    }

    async _executeSingleAction(button, column, aiCol, inputData, setFunction = null) {
        let self = this;
        let totalTokens = 0;

        if ("local" === aiCol.provider) {
            totalTokens = await this.aiLocalExecutor.execute(aiCol, inputData, setFunction);
        } else {
            await $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response/",
                data: {
                    "assistantName": aiCol.assistant,
                    "inputData": inputData
                },
                success: function(res)
                {
                    //console.log("AI response=", res, "to=", aiCol.to);

                    //handle res.error
                    if (res.error) {
                        contentContainer.html(res.error);
                    } else {
                        if (setFunction != null) {
                            setFunction(res.content);
                        } else {
                            self.EDITOR.set(aiCol.to, res.response);
                        }
                    }

                    totalTokens = res.totalTokens;
                },
                error: function(xhr, ajaxOptions, thrownError) {
                    self._setCurrentStatus("datatable.error.unknown");
                    totalTokens = -1;
                }
            });
            return totalTokens;
        }
    }

    _executeImageAction(button, aiCol, column) {
        return new Promise((resolve, reject) => {
            let self = this;
            self._showLoader(button);
            self._setCurrentStatus("components.ai_assistants.editor.loading.js");

            let from = aiCol.from;
            if (from == null || from == "")  from = aiCol.to;

            let inputData = {
                type: this.getColumnType(this.EDITOR, from),
                value: self.EDITOR.get(from)
            }

            $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response-image/",
                data: {
                    "assistantName": aiCol.assistant,
                    "inputData": JSON.stringify(inputData)
                },
                success: function(res)
                {
                    //self._closeToast(3000);
                    self._hideLoader(button, column);

                    //handle res.error
                    if (res.error) {
                        contentContainer.html(res.error);
                    }

                    self._setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, res.totalTokens);

                    resolve(res);
                },
                error: function(xhr, ajaxOptions, thrownError) {

                    self._closeToast(3000);
                    self._hideLoader(button);

                    contentContainer.html(WJ.translate("datatable.error.unknown"));

                    reject("kks");
                }
            });
        });
    }

    _selectImage(imageName, toField) {
        console.log("Selected image:", imageName);

        $("div.button-div").show();
        $("div.image-status").find("img.selected").removeClass("selected");
        $("div.image-status").find('img[alt="' + imageName + '"]').addClass("selected");

        //For now, only action gonna be SAVE of image and after save SET to field

    }

    _saveAndSetImage(imageName, toField) {
        return new Promise((resolve, reject) => {
            // Implement the save and set logic here
            $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response-image/",
                data: {
                    "assistantName": aiCol.assistant,
                    "inputData": JSON.stringify(inputData)
                },
                success: function(res)
                {
                    resolve(res);
                },
                error: function(xhr, ajaxOptions, thrownError) {
                    reject("kks");
                }
            });
        });
    }

    _setImageStatus(images, toField, textKey, ...params) {
        let contentContainer = $("#toast-container-ai-content");

        let html = "<div>" + WJ.translate(textKey, params) + "</div>";
        html += "<div class='ai-image-preview-div'></div>";
        html += "<div class='button-div' style='display: none;'> </div>";

        contentContainer.html(html);

        let previewDiv = contentContainer.find('.ai-image-preview-div');

        for (let i = 0; i < images.length; i++) {
            const imageName = images[i];
            const imgDiv = $(`
                <div class="image-status">
                    <img src="/admin/rest/ai/assistant/file/binary/?fileName=${imageName}" alt="${imageName}" />
                </div>
            `);
            imgDiv.find('img').on('click', () => {
                this._selectImage(imageName, toField);
            });
            previewDiv.append(imgDiv);
        }

        let buttonDiv = contentContainer.find('.button-div');

        const imageButton = $('<button class="btn btn-outline-secondary select-image">' + WJ.translate("components.ai.editor.select_image.js") + '</button>');
        buttonDiv.append(imageButton);
    }

    async _executeStreamAction(button, aiCol) {
        // Implement AI button click handling logic here
        //console.log("Executing action for AI column:", aiCol);

        let self = this;
        self._showLoader(button);

        self._setCurrentStatus("components.ai_assistants.editor.loading.js");

        let from = aiCol.from;
        if (from == null || from == "")  from = aiCol.to; //if from is not set, use to as from

        if ("local" === aiCol.provider) {
            await this.aiLocalExecutor.execute(aiCol);
            self._hideLoader(button);
            self._setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, 0);
        } else {

            let inputData = {
                type: this.getColumnType(this.EDITOR, from),
                value: self.EDITOR.get(from)
            }

            fetch("/admin/rest/ai/assistant/response-stream/", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    'X-CSRF-Token': window.csrfToken
                },
                body: JSON.stringify({
                    assistantName: aiCol.assistant,
                    inputData: JSON.stringify(inputData)
                })
            })
            .then(response => {
                const reader = response.body.getReader();
                const decoder = new TextDecoder("utf-8");

                let wholeText = "";

                function read() {
                    reader.read().then(({ done, value }) => {
                        if (done) {
                            console.log("Stream complete");
                            self._closeToast(3000);
                            self._hideLoader(button);

                            self._setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, value.totalTokens);

                            return;
                        }

                        const chunk = decoder.decode(value, { stream: true });
                        console.log("Received:", chunk);

                        let isJson = false;
                        try {
                            const parsed = JSON.parse(chunk);
                            isJson = typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed);
                        } catch (e) {
                            isJson = false;
                        }

                        if(isJson == false) {
                            wholeText += chunk;
                            self.EDITOR.set(aiCol.to, wholeText);
                        } else {
                            const parsed = JSON.parse(chunk);

                            //handle parsed.error
                            if (parsed.error) {
                                contentContainer.html(parsed.error);
                            }

                            self._setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, parsed.totalTokens);
                        }

                        read();
                    });
                }

                read();
            });
        }
    }

    // getTempFile(fileName) {
    //     return new Promise((resolve, reject) => {
    //         $.ajax({
    //             type: "GET",
    //             url: "/admin/rest/ai/assistant/file/binary/",
    //             data: {
    //                 "fileName": fileName,
    //             },
    //             xhrFields: { responseType: "arraybuffer" },
    //             success: function(res)
    //             {
    //                 resolve(res);
    //             },
    //             error: function(xhr, ajaxOptions, thrownError) {
    //                 reject("KUWA");
    //             }
    //         });
    //     });
    // }

    _showLoader(button) {
        button.parents(".DTE_Field").addClass("ai-loading");
        let input = button.parents(".DTE_Field").find(".form-control");
        if (input.val()=="") input.val(WJ.translate("components.ai_assistants.editor.loading.js"));
    }

    _hideLoader(button, column) {
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
        this._setCurrentStatus("components.ai_assistants.local.downloadingModel.js", false, percent + "%");
    }
}