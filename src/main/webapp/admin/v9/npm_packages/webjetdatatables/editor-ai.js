import { AiBrowserExecutor } from "./ai/ai-browser-executor";
import { AiRestExecutor } from "./ai/ai-rest-executor";
import { AiUserInterface } from "./ai/ai-ui";

export class EditorAi {

    EDITOR = null;

    aiBrowserExecutor = null;
    aiRestExecutor = null;
    aiUserInterface = null;

    //field to remember value for undo process
    undoField = null;

    //response error code from executeSingleAction
    ERR_UNKNOWN = -1; //show unknown error message and close dialog
    ERR_CLOSE_DIALOG = -2; //close dialog, message must be written by _setCurrentStatus
    DO_NOT_CLOSE_DIALOG = -3; //do not close dialog, message must be written by _setCurrentStatus

    //constructor
    constructor(EDITOR) {
        this.EDITOR = EDITOR;
        //console.log("EditorAi instance created, editor=", this.EDITOR);
        this.aiBrowserExecutor = new AiBrowserExecutor(EDITOR, this);
        this.aiRestExecutor = new AiRestExecutor(EDITOR, this);

        EDITOR.editorAi = this;

        this.aiUserInterface = new AiUserInterface(EDITOR, this);

        this.bindEvents();
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
                    } else {
                        let inputField = $(field.dom.inputControl[0]).find(".form-control");

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
        });
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

    _getColumnType(editor, fieldName) {
        let className = editor?.field(fieldName)?.s?.opts?.className ?? "";
        let renderFormat = editor?.field(fieldName)?.s?.opts?.renderFormat ?? "";

        if(className.indexOf("image") != -1 || renderFormat.indexOf("dt-format-image") != -1) return "image";
        return "text";
    }

    setCurrentStatus(textKey, pulsate = false, ...params) {
        this.aiUserInterface.setCurrentStatus(textKey, pulsate, ...params);
    }

    setError(...params) {
        this.aiUserInterface.setError(...params);
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
            } else if (this.undoField.type === "field" && this.undoField.to != null) {
                this.EDITOR.set(this.undoField.to, this.undoField.value);
            }
            this._closeToast();
        }
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

        let inputData = {};
        if(inputValues !== null && typeof inputValues === 'object' && !Array.isArray(inputValues)) {
            inputData = inputValues;
        }
        //Add other values
        inputData.inputValueType = this._getColumnType(this.EDITOR, aiCol.to);

        //console.log(inputData);

        if (ckEditorRanges != null && ckEditorSelectionInstance != null) {

            self.setCurrentStatus("components.ai_assistants.editor.loading.js");

            // Get selected HTML
            var tempDiv = document.createElement('div');
            for (var i = 0; i < ckEditorRanges.length; i++) {
                var fragment = ckEditorRanges[i].cloneContents();
                tempDiv.appendChild(fragment.$);
            }
            //console.log("tempDiv=", tempDiv, "html=", tempDiv.innerHTML);

            inputData.inputValue = tempDiv.innerHTML;

            totalTokens += await this._executeSingleAction(button, column, aiCol, inputData, false, (response, finished) => {
                //console.log("response="+response, "setting to editor: ", ckEditorInstance, "inputData=", inputData);

                // Replace selection with AI response, do not stream, wait for finished
                if (finished === true) {
                    ckEditorRanges[0].deleteContents();
                    ckEditorSelectionInstance.insertHtml(response);
                }
            });

        } else if (isPageBuilder) {
            //console.log("Executing action for PageBuilder editor:", aiCol, "column", column);

            //get all PB editor instances and execute action on them separately
            let editors = this.EDITOR.field(aiCol.to).s.opts.wjeditor.getWysiwygEditors();
            let reuseApiInstance = false;

            self.undoField = {};
            self.undoField.type = "pageBuilder";
            self.undoField.editors = [];

            for (let i=0; i<editors.length; i++) {
                let editor = editors[i];
                if (i>0) reuseApiInstance = true;
                //console.log("Executing on editor: ", editor);

                self.setCurrentStatus("components.ai_assistants.editor.loading.js", false, (i+1)+"/"+editors.length);

                inputData.inputValue = editor.getData();
                self.undoField.editors.push({instance: editor, value: inputData.inputValue});

                totalTokens += await this._executeSingleAction(button, column, aiCol, inputData, reuseApiInstance, (response) => {
                    //console.log("response="+response, "setting to editor: ", editor);
                    editor.setData(response);
                });
            }

        } else {
            self.setCurrentStatus("components.ai_assistants.editor.loading.js");

            inputData.inputValue = self.EDITOR.get(from);
            try {
                if (self._getColumnType(self.EDITOR, aiCol.to) === "text") {
                    self.undoField = {};
                    self.undoField.type = "field";
                    self.undoField.value = self.EDITOR.get(aiCol.to);
                    self.undoField.to = aiCol.to;
                }
            } catch (error) {
                console.error("Error setting undoField:", error);
            }
            totalTokens = await this._executeSingleAction(button, column, aiCol, inputData);
        }

        if (totalTokens >= 0) {
            if (self.isUndo()==false) self._closeToast(3000);
            self._hideLoader(button);

            self.setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, totalTokens);
        } else if (totalTokens === this.DO_NOT_CLOSE_DIALOG) {
            //do nothing
            //content was rendered in execute method e.g. image selection dialog
        } else {
            //there seems to be an error
            self._closeToast(3000);
            self._hideLoader(button);

            //for -1 show default status, otherwise we expect status is allready set by other code
            if (totalTokens == this.ERR_UNKNOWN) self.setError();
        }
    }

    async _executeSingleAction(button, column, aiCol, inputData, reuseApiInstance = false, setFunction = null) {
        let self = this;
        let totalTokens = 0;

        if ("browser" === aiCol.provider) {
            if (this.aiBrowserExecutor.isAvailable(aiCol)) {
                totalTokens = await this.aiBrowserExecutor.execute(aiCol, inputData, reuseApiInstance, setFunction);
            } else {
                totalTokens = this.DO_NOT_CLOSE_DIALOG;
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

                totalTokens = await this.aiRestExecutor.executeImageAction(aiCol, inputData, button);
            } else {
                totalTokens = await this.aiRestExecutor.execute(aiCol, inputData, setFunction);
            }
        }
        return totalTokens;
    }

}