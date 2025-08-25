import { AiBrowserExecutor } from "./ai/ai-browser-executor";
import { AiRestExecutor } from "./ai/ai-rest-executor";
import { AiUserInterface } from "./ai/ai-ui";

export class EditorAi {

    EDITOR = null;

    aiBrowserExecutor = null;
    aiRestExecutor = null;
    aiUserInterface = null;

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

    _showLoader(button) {
        this.aiUserInterface._showLoader(button);
    }

    _hideLoader(button) {
        this.aiUserInterface._hideLoader(button);
    }

    _closeToast(timeOut) {
        this.aiUserInterface._closeToast(timeOut);
    }

    async _executeAction(button, column, aiCol, userPrompt = null) {
        // Implement AI button click handling logic here
        //console.log("Executing action for AI column:", aiCol);

        let self = this;
        self._showLoader(button);

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
                            }
                        }
                    }
                }
            } catch (e) {
                console.error("Error checking PageBuilder editor:", e);
            }
        }

        let totalTokens = 0;

        let inputData = {
            type: this._getColumnType(this.EDITOR, aiCol.to),
            userPrompt: userPrompt
        }

        if (ckEditorRanges != null && ckEditorSelectionInstance != null) {

            self.setCurrentStatus("components.ai_assistants.editor.loading.js");

            // Get selected HTML
            var tempDiv = document.createElement('div');
            for (var i = 0; i < ckEditorRanges.length; i++) {
                var fragment = ckEditorRanges[i].cloneContents();
                tempDiv.appendChild(fragment.$);
            }
            //console.log("tempDiv=", tempDiv, "html=", tempDiv.innerHTML);

            inputData.value = tempDiv.innerHTML;

            totalTokens += await this._executeSingleAction(button, column, aiCol, inputData, (response) => {
                //console.log("response="+response, "setting to editor: ", ckEditorInstance, "inputData=", inputData);

                // Replace selection with AI response
                ckEditorRanges[0].deleteContents();
                ckEditorSelectionInstance.insertHtml(response);
            });

        } else if (isPageBuilder) {
            //console.log("Executing action for PageBuilder editor:", aiCol, "column", column);

            //get all PB editor instances and execute action on them separately
            let editors = this.EDITOR.field(aiCol.to).s.opts.wjeditor.getWysiwygEditors();
            for (let i=0; i<editors.length; i++) {
                let editor = editors[i];
                //this._executeSingleAction(button, column, aiCol, from, editor);
                //console.log("Executing on editor: ", editor);

                self.setCurrentStatus("components.ai_assistants.editor.loading.js", false, (i+1)+"/"+editors.length);

                inputData.value = editor.getData();
                totalTokens += await this._executeSingleAction(button, column, aiCol, inputData, (response) => {
                    //console.log("response="+response, "setting to editor: ", editor);
                    editor.setData(response);
                });
            };

        } else {
            self.setCurrentStatus("components.ai_assistants.editor.loading.js");

            inputData.value = self.EDITOR.get(from);
            totalTokens = await this._executeSingleAction(button, column, aiCol, inputData);
        }

        if (totalTokens >= 0) {
            self._closeToast(3000);
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

    async _executeSingleAction(button, column, aiCol, inputData, setFunction = null) {
        let self = this;
        let totalTokens = 0;

        if ("browser" === aiCol.provider) {
            if (this.aiBrowserExecutor.isAvailable(aiCol)) {
                totalTokens = await this.aiBrowserExecutor.execute(aiCol, inputData, setFunction);
            } else {
                totalTokens = this.ERR_CLOSE_DIALOG;
            }
        } else {
            // IS IMAGE
            if ("image"===inputData.type) {
                //inputData.value = "/images/zo-sveta-financii/konsolidacia-napriec-trhmi/oil-pump.jpg";
                inputData.type = "text";
                await this.aiRestExecutor.executeImageAction(aiCol, inputData, (result) => {
                    console.log("Image action result:", result);
                    if (result.error != null) {
                        totalTokens = this.ERR_CLOSE_DIALOG;
                        self.setError(result.error);
                        return totalTokens;
                    }
                    self.aiUserInterface.renderImageSelection(button, result.tempFiles, aiCol.to, "components.ai_assistants.stat.totalTokens.js", result.totalTokens);
                    //dialog is rewriten, do not close it
                    totalTokens = this.DO_NOT_CLOSE_DIALOG;
                });
            } else {
                totalTokens = await this.aiRestExecutor.execute(aiCol, inputData, setFunction);
            }
        }
        return totalTokens;
    }

}