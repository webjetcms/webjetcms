import { AiBrowserExecutor } from "./ai/ai-browser-executor";
import { AiRestExecutor } from "./ai/ai-rest-executor";
import { AiUserInterface } from "./ai/ai-ui";
import { AiExecutionResult } from "./ai/ai-execution-result";

/**
 * Main class for AI integration in Datatables Editor - binds buttons, handles events, shows UI, executes actions
 */
export class EditorAi {

    EDITOR = null;

    aiBrowserExecutor = null;
    aiRestExecutor = null;
    aiUserInterface = null;

    //field to remember value for undo process
    undoField = null;

    //constructor
    constructor(EDITOR, bindEvents = true) {
        this.EDITOR = EDITOR;
        //console.log("EditorAi instance created, editor=", this.EDITOR);
        this.aiBrowserExecutor = new AiBrowserExecutor(EDITOR, this);
        this.aiRestExecutor = new AiRestExecutor(EDITOR, this);

        EDITOR.editorAi = this;

        this.aiUserInterface = new AiUserInterface(EDITOR, this);

        if (bindEvents) {
            this.bindEvents();
        }
    }

    bindEvents() {
        //it must be WJ.DTE.open because of ckeditor button initialization
        window.addEventListener("WJ.DTE.open", (event) => {
            //console.log("WJ.DTE.open event received, detail=", event.detail, "this.id=", this.EDITOR.TABLE.DATA.id);
            if (this.EDITOR.TABLE.DATA.id===event.detail.id) {
                //console.log("WJ.DTE.open event triggered");
                this.bindEditorButtons();
            }
        });
        //also opened for custom fields initialized async
        window.addEventListener("WJ.DTE.opened", (event) => {
            //console.log("WJ.DTE.opened event received, detail=", event.detail, "this.id=", this.EDITOR.TABLE.DATA.id);
            if (this.EDITOR.TABLE.DATA.id===event.detail.id) {
                //console.log("WJ.DTE.opened event triggered");
                this.bindEditorButtons();
            }
        });
    }

    /**
     * Binds AI-related buttons to the provided EDITOR instance.
     */
    bindEditorButtons() {
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
                            const button = this._getEditorButton(column, "btn-ai-wysiwyg");
                            exitInlineEditorContainer.prepend(button);
                        }
                    } else if ("quill"===column.type) {
                        let inputField = $(field.dom.inputControl[0]).find(".quill-wrapper");
                        //console.log("inputField:", inputField);

                        if (inputField.parent().find(".ti-sparkles").length === 0) {
                            const button = this._getEditorButton(column, null);
                            inputField.find(".ql-toolbar").append($("<span class='ql-formats ql-formats-ai'></span>"));
                            inputField.find(".ql-formats-ai").append(button);
                        }
                    } else {
                        let inputField = $(field.dom.inputControl[0]).find(".form-control");

                        //console.log("inputField:", inputField, "parents=", inputField.parents(".bootstrap-select").length);

                        if (inputField.parents(".bootstrap-select").length > 0) {
                            //it is probably custom field set as selectpicker, skip it
                            //we should probably better handle custom fields in future
                        } else {
                            //if it doesnt have input-group, wrap it
                            if (inputField.parents(".input-group").length === 0) {
                                inputField.wrap('<div class="input-group"></div>');
                            }

                            //if it doesnt have ti-sparkles button add it
                            if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                                const button = this._getEditorButton(column, null);
                                inputField.parents(".input-group").append(button);
                            }
                        }
                    }
                }
            }
        });
    }

    //bind buttons by class/data attributes outside of Datatables Editor
    bindOtherButtons() {
        let inputFields = $(".ai-field");
        inputFields.each((index, element) => {
            let inputField = $(element);

            //if it doesnt have input-group, wrap it
            if (inputField.parents(".input-group").length === 0) {
                inputField.wrap('<div class="input-group"></div>');
            }

            //if it doesnt have ti-sparkles button add it
            if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                const button = this._getOtherButton(inputField);
                inputField.parents(".input-group").append(button);
            }
        });
    }

    stopExecution() {
        this.aiUserInterface.saveUserPrompt();
        this.aiUserInterface.setCurrentStatus("components.ai_assistants.stopping.js", false);
        if (this.aiBrowserExecutor != null) this.aiBrowserExecutor.destroy();
        //if (this.aiRestExecutor != null) this.aiRestExecutor.destroy();

    }

    _getEditorButton(column, appendClass) {
        let buttonHTML = '<button class="btn btn-outline-secondary btn-ai'
        if (appendClass != null && appendClass != "") buttonHTML += " " + appendClass;
        buttonHTML += '" type="button" data-toggle="tooltip" title="'+WJ.translate('components.ai_assistants.editor.btn.tooltip.js')+'"><i class="ti ti-sparkles"></i><i class="ti ti-loader"></i></button>';
        const button = $(buttonHTML);
        button.on('click', () => {
            this.aiUserInterface.generateAssistentOptions(button, column);
        });
        return button;
    }

    _getOtherButton(inputField, appendClass=null) {
        let buttonHTML = '<button class="btn btn-outline-secondary btn-ai'
        if (appendClass != null && appendClass != "") buttonHTML += " " + appendClass;
        buttonHTML += '" type="button" data-toggle="tooltip" title="'+WJ.translate('components.ai_assistants.editor.btn.tooltip.js')+'"><i class="ti ti-sparkles"></i><i class="ti ti-loader"></i></button>';
        const button = $(buttonHTML);
        button.on('click', () => {
            this.aiUserInterface.generateOtherAssistentOptions(inputField);
            this.aiUserInterface.getPathForNewImage();
        });
        return button;
    }

    _getColumnType(column, fieldName) {
        let className = column.className ?? "";
        let renderFormat = column.renderFormat ?? "";

        if(className.indexOf("image") != -1 || renderFormat.indexOf("dt-format-image") != -1) return "image";
        return "text";
    }

    setCurrentStatus(textKey, pulsate = false, ...params) {
        this.aiUserInterface.setCurrentStatus(textKey, pulsate, ...params);
    }

    setError(...params) {
        this.aiUserInterface.setError(...params);
    }

    setExplanatoryText(text) {
        setTimeout(() => {
            //because status will be changed to success after response
            this.aiUserInterface.setExplanatoryText(text);
        }, 800);
    }

    isUndo() {
        return this.undoField !== null;
    }

    undo() {
        if (this.isUndo()) {
            if (this.undoField.type === "range" && this.undoField.ckEditorSelectionInstance != null) {
                this.undoField.ckEditorSelectionInstance.setData(this.undoField.value);
            } else if (this.undoField.type === "pageBuilder" && this.undoField.editors.length > 0) {
                this.undoField.editors.forEach(editor => {
                    editor.instance.setData(editor.value);
                });
            } else if (this.undoField.type === "pageBuilder-full") {
                let options = this.EDITOR.field(this.undoField.to).s.opts;
                options.wjeditor.pbInsertContent(this.undoField.value, "replace", true);
            } else if (this.undoField.type === "field" && this.undoField.to != null) {
                this.EDITOR.set(this.undoField.to, this.undoField.value);
            }
            //this._closeToast();
            if (this.revertUserPrompt()==false) {
                $("#toast-container-ai .header-back-button .btn-outline-secondary").trigger("click");
            }
        }
    }

    revertUserPrompt() {
        let userPromptSaved = $("#toast-ai-user-prompt-saved");
        let savedContent = userPromptSaved.find(".user-prompt-container");
        //console.log("revertUserPrompt: savedContent=", savedContent);
        if (savedContent != null && savedContent.length > 0) {
            $("#toast-container-ai-content").empty();
            savedContent.find(".chat-error-container").empty();
            $("#toast-container-ai-content").append(savedContent);
            userPromptSaved.empty();
            return true;
        }
        return false;
    }

    setDownloadStatus(percent) {
        this.aiUserInterface.setDownloadStatus(percent);
    }

    _showLoader(button) {
        this.aiUserInterface._showLoader(button);
    }

    _hideLoader(button) {
        this.aiUserInterface._hideLoader(button);
    }

    _closeToast(timeOut) {
        this.aiUserInterface._closeToast(timeOut);
    }

    async _executeAction(button, column, aiCol, inputValues = null) {
        // Implement AI button click handling logic here
        //console.log("Executing action for AI column:", aiCol);

        let self = this;
        self._showLoader(button);
        self.undoField = null;

        let from = aiCol.from;
        if (from == null || from == "")  from = aiCol.to; //if from is not set, use to as from

        let isPageBuilder = false;
        let ckEditorRanges = null;
        let ckEditorSelectionInstance = null;
        if ("wysiwyg" === column.type && aiCol.to === column.name) {
            try {
                let wjeditor = this.EDITOR.field(aiCol.to).s.opts.wjeditor;
                //for PageBuilder if to===pageBuilder element we need to execute function for every editor in PB page to prevent HTML structure issues
                if (wjeditor != null) {
                    if ("pageBuilder" === wjeditor.editingMode) {
                        isPageBuilder = true;

                        //iterate over editors and find selection (if any)
                        let editors = this.EDITOR.field(aiCol.to).s.opts.wjeditor.getWysiwygEditors();
                        for (let i=0; i<editors.length; i++) {
                            let editor = editors[i];
                            let selection = editor.getSelection();
                            if (selection != null && selection.getSelectedText().length > 0) {
                                let ranges = selection.getRanges();
                                if (ranges.length > 0) {
                                    ckEditorSelectionInstance = editor;
                                }
                            }
                        }
                    } else {
                        //check selection in ckeditor
                        ckEditorSelectionInstance = ckEditorInstance;
                    }

                    if (ckEditorSelectionInstance != null) {
                        let selection = ckEditorSelectionInstance.getSelection();
                        if (selection != null && selection.getSelectedText().length > 0) {
                            let ranges = selection.getRanges();
                            if (ranges.length > 0) {
                                ckEditorRanges = ranges;

                                self.undoField = {};
                                self.undoField.type = "range";
                                self.undoField.value = ckEditorSelectionInstance.getData();
                                self.undoField.ckEditorSelectionInstance = ckEditorSelectionInstance;
                            }
                        }
                    }
                }
            } catch (e) {
                console.error("Error checking PageBuilder editor:", e);
            }
        }

        let totalTokens = 0;
        let executionResult = null;

        let inputData = {};
        if(inputValues !== null && typeof inputValues === 'object' && !Array.isArray(inputValues)) {
            inputData = inputValues;
        }
        //Add other values
        inputData.inputValueType = this._getColumnType(column, aiCol.to);

        //console.log(inputData);

        self.setCurrentStatus("components.ai_assistants.editor.loading.js");

        if (ckEditorRanges != null && ckEditorSelectionInstance != null) {

            // Get selected HTML
            var tempDiv = document.createElement('div');
            for (var i = 0; i < ckEditorRanges.length; i++) {
                var fragment = ckEditorRanges[i].cloneContents();
                tempDiv.appendChild(fragment.$);
            }
            //console.log("tempDiv=", tempDiv, "html=", tempDiv.innerHTML);

            inputData.inputValue = tempDiv.innerHTML;

            executionResult = await this._executeSingleAction(button, column, aiCol, inputData, false, (response, finished) => {
                //console.log("response="+response, "setting to editor: ", ckEditorInstance, "inputData=", inputData);

                // Replace selection with AI response, do not stream, wait for finished
                if (finished === true) {
                    ckEditorRanges[0].deleteContents();
                    ckEditorSelectionInstance.insertHtml(response);
                }
            });
            if (executionResult!=null) totalTokens += executionResult.totalTokens;

        } else if (isPageBuilder) {
            //console.log("Executing action for PageBuilder editor:", aiCol, "column", column);

            let mode = "";

            if ("append"===mode || "replace"===mode) {
                //append new content into page
                let reuseApiInstance = false;
                let self = this;
                let options = self.EDITOR.field(aiCol.to).s.opts;
                let lastLength = 0;

                self.undoField = {};
                self.undoField.type = "pageBuilder-full";
                self.undoField.value = options.wjeditor.getData();
                self.undoField.to = aiCol.to;

                executionResult = await this._executeSingleAction(button, column, aiCol, inputData, reuseApiInstance, (response, final) => {
                    let insertData = final;
                    if (final === false && lastLength+300 < response.length && response.lastIndexOf("</section>") > response.length-60) {
                        insertData = true;
                        //insert only valid HTML code with complete sections
                        response = response.substring(0, response.lastIndexOf("</section>")+10);
                        lastLength = response.length;
                    }

                    if (insertData) options.wjeditor.pbInsertContent(response, mode, final);
                });

                /*/wait for 10 seconds
                await new Promise(resolve => setTimeout(resolve, 10000));

                let response = `<section class="py-5 aaaa-apend text-center text-lg-start">
<div class="container">
<div class="row align-items-center g-5">
<div class="col-12 col-lg-6">
<h1 class="display-5 fw-bold">AAAA Spoľahlivý Opravár Vody, Plynu a&nbsp;Elektriny v&nbsp;Bratislave</h1>

<p class="lead my-4">Profesionálne služby v&nbsp;oblasti vodovodných a&nbsp;plynových prípojok, ako aj odborné revízie elektriny. Rýchlo, kvalitne a&nbsp;za férovú cenu.</p>

<p><a class="btn btn-primary btn-lg" href="#kontakt">Kontaktujte ma</a></p>
</div>

<div class="col-12 col-lg-6"><img alt="Ilustračný obrázok opravy vodovodného potrubia" class="img-fluid rounded-3 shadow-sm" src="/components/news/admin_imgplaceholder.png" /></div>
</div>
</div>
</section>`;
                options.wjeditor.pbInsertContent(response, mode, true);
                executionResult = new AiExecutionResult();
                executionResult.totalTokens = 100;
                console.log("executionResult=", executionResult);

                */
                totalTokens = executionResult.totalTokens;
            } else {

                //get all PB editor instances and execute action on them separately
                let editors = this.EDITOR.field(aiCol.to).s.opts.wjeditor.getWysiwygEditors();
                let reuseApiInstance = false;

                self.undoField = {};
                self.undoField.type = "pageBuilder";
                self.undoField.editors = [];
                let successCount = 0;
                let explanatoryText = null;
                executionResult = new AiExecutionResult();

                for (let i=0; i<editors.length; i++) {
                    let editor = editors[i];
                    if (i>0) reuseApiInstance = true;
                    //console.log("Executing on editor: ", editor);

                    self.setCurrentStatus("components.ai_assistants.editor.loading.js", false, (i+1)+"/"+editors.length);

                    inputData.inputValue = editor.getData();
                    self.undoField.editors.push({instance: editor, value: inputData.inputValue});

                    let editorExecutionResult = await this._executeSingleAction(button, column, aiCol, inputData, reuseApiInstance, (response) => {
                        //console.log("response="+response, "setting to editor: ", editor);
                        editor.setData(response);
                    });
                    //console.log("editorExecutionResult=", editorExecutionResult);
                    if (editorExecutionResult!=null) {
                        totalTokens += editorExecutionResult.totalTokens;

                        if (editorExecutionResult.stopped === true) {
                            this._stoppedSignalReceived(button);
                            return;
                        }

                        //preserve status to executionResults
                        if (editorExecutionResult.statusKey != null) executionResult.statusKey = editorExecutionResult.statusKey;
                        if (editorExecutionResult.statusKeyParams != null) executionResult.statusKeyParams = editorExecutionResult.statusKeyParams;
                        if (editorExecutionResult.errorText != null) executionResult.errorText = editorExecutionResult.errorText;
                        if (editorExecutionResult.success === true) successCount++;

                        if (editorExecutionResult.explanatoryText != null) {
                            if (explanatoryText == null || explanatoryText.trim() === "") explanatoryText = editorExecutionResult.explanatoryText;
                            else explanatoryText += "\n---\n" + editorExecutionResult.explanatoryText;
                        }
                        totalTokens += editorExecutionResult.totalTokens;
                    }
                }

                if (successCount === editors.length) executionResult.success = true;
                executionResult.explanatoryText = explanatoryText;
            }

        } else {

            inputData.inputValue = self.EDITOR.get(from);
            try {
                if (self._getColumnType(column, aiCol.to) === "text") {
                    self.undoField = {};
                    self.undoField.type = "field";
                    self.undoField.value = self.EDITOR.get(aiCol.to);
                    self.undoField.to = aiCol.to;
                }
            } catch (error) {
                console.error("Error setting undoField:", error);
            }
            executionResult = await this._executeSingleAction(button, column, aiCol, inputData);
            if (executionResult!=null) totalTokens += executionResult.totalTokens;
        }

        //console.log("executionResult=", executionResult, "totalTokens=", totalTokens);

        if (executionResult == null) {
            self.setError();
        } else {
            if (executionResult.errorText != null) {
                self.setError(executionResult.errorText);
            } else if (executionResult.statusKey != null) {
                self.setCurrentStatus(executionResult.statusKey, false, ...(executionResult.statusKeyParams ?? []));
            } else if (executionResult.success === true) {
                if (self.isUndo()==false) self._closeToast(3000);
                self._hideLoader(button);

                self.setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, totalTokens);
            }

            if (executionResult.stopped === true) {
                this._stoppedSignalReceived(button);
                return;
            }

            if (executionResult.explanatoryText != null) {
                self.aiUserInterface.setExplanatoryText(executionResult.explanatoryText);
            }
        }
    }

    _stoppedSignalReceived(button) {
        this._hideLoader(button);
        $("#toast-container-ai").removeClass("ai-status-working");

        if (this.isUndo()) {
            this.undo();
        }
    }


    async _executeSingleAction(button, column, aiCol, inputData, reuseApiInstance = false, setFunction = null) {
        let self = this;
        let executionResult = null;

        if ("browser" === aiCol.provider) {
            if (this.aiBrowserExecutor.isAvailable(aiCol)) {
                executionResult = await this.aiBrowserExecutor.execute(aiCol, inputData, reuseApiInstance, setFunction);
            } else {
                executionResult = new AiExecutionResult();
                executionResult.statusKey = "components.ai_assistants.browser.api_not_available.js"
                let apiName = this.aiBrowserExecutor.getApiName(aiCol);
                executionResult.statusKeyParams = [apiName];
            }
        } else {
            // IS IMAGE

            if ("image"===inputData.inputValueType) {
                //inputData.value = "/images/zo-sveta-financii/konsolidacia-napriec-trhmi/oil-pump.jpg";

                //We want IMAGE AI response, because SRC field is IMAGE
                if(aiCol.action === "generate_image") {
                    // We want image BUT we want generated new image from prompt (TEXT) so we set inputValueType as TEXT (we do NOT send image path od old image)
                    inputData.inputValueType = "text"
                }

                executionResult = await this.aiRestExecutor.executeImageAction(aiCol, inputData, button);
            } else {
                executionResult = await this.aiRestExecutor.execute(aiCol, inputData, setFunction);
            }
        }
        return executionResult;
    }

}