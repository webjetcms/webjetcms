import { AiBrowserExecutor } from "./ai/ai-browser-executor";
import { AiRestExecutor } from "./ai/ai-rest-executor";
import { AiUserInterface } from "./ai/ai-ui";
import { AiExecutionResult } from "./ai/ai-execution-result";
import { AiChat } from "../../src/js/ai/ai-chat";
import { AiChatHost } from "../../src/js/ai/ai-chat-host";
import { parseAiResponse } from "../../src/js/ai/ai-response-parser";

/**
 * Main class for AI integration in Datatables Editor - binds buttons, handles events, shows UI, executes actions
 */
export class EditorAi {

    EDITOR = null;

    aiBrowserExecutor = null;
    aiRestExecutor = null;
    aiUserInterface = null;
    chatConversations = new Map();
    chatApplyingFields = new Map();

    //field to remember value for undo process
    undoField = null;
    stopped = false;

    //constructor
    constructor(EDITOR, bindEvents = true) {
        this.EDITOR = EDITOR;
        //console.log("EditorAi instance created, editor=", this.EDITOR);
        this.aiBrowserExecutor = new AiBrowserExecutor(EDITOR, this);
        this.aiRestExecutor = new AiRestExecutor(EDITOR, this);

        EDITOR.editorAi = this;

        this.aiUserInterface = new AiUserInterface(EDITOR, this);

        this._chatPageHide = () => this.disposeChats();
        window.addEventListener("pagehide", this._chatPageHide);
        if (typeof EDITOR.on === "function") {
            EDITOR.on("close.aiChat", () => this.disposeChats());
            EDITOR.on("destroy.aiChat", () => {
                this.disposeChats();
                window.removeEventListener("pagehide", this._chatPageHide);
                $(document).off("hidden.bs.modal.aiChat", this._chatModalHidden);
            });
            this._chatModalHidden = event => {
                if (event.target.id === this.EDITOR.TABLE?.DATA?.id + "_modal") this.disposeChats();
            };
            $(document).on("hidden.bs.modal.aiChat", this._chatModalHidden);
        }

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
        if (WJ.hasPermission("cmp_ai_button") === false) {
            //user does not have permission to see AI button
            return;
        }

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
                    } else if ("options"===column.type || "enumeration"===column.type || "optionsSimple"===column.type) {
                        //skip OPTIONS, ENUMERATION, OPTIONS_SIMPLE field type - AI button is not supported
                    } else {
                        let inputField = $(field.dom.inputControl[0]).find(".form-control");
                        const focusedInput = inputField.filter(':focus')[0];

                        //console.log("inputField:", inputField, "parents=", inputField.parents(".bootstrap-select").length);

                        if (inputField.closest(".custom-field-ai-disabled").length > 0) {
                            // This custom field type does not support AI actions.
                        } else if (inputField.closest(".md-jsoneditor-control").length > 0) {
                            const toolbar = inputField.closest(".md-jsoneditor-control").find(".md-jsoneditor-toolbar");
                            if (toolbar.find(".ti-sparkles").length === 0) {
                                const button = this._getEditorButton(column, "btn-sm");
                                toolbar.find(".md-jsoneditor-position").before(button);
                            }
                        } else if (inputField.parents(".bootstrap-select").length > 0) {
                            //it is probably custom field set as selectpicker, skip it
                            //we should probably better handle custom fields in future
                        } else {
                            //if it doesnt have input-group, wrap it
                            if (inputField.parents(".input-group").length === 0) {
                                const textareaEditor = inputField.closest(".md-textarea-editor");
                                (textareaEditor.length > 0 ? textareaEditor : inputField).wrap('<div class="input-group"></div>');
                            }

                            //if it doesnt have ti-sparkles button add it
                            if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                                const button = this._getEditorButton(column, null);
                                inputField.parents(".input-group").append(button);
                            }

                            if (focusedInput != null && document.activeElement !== focusedInput) {
                                focusedInput.focus({preventScroll: true});
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
            if (WJ.hasPermission("cmp_ai_button") === false) {
                //user does not have permission to see AI button
                inputField.removeClass("ai-field");
            } else {
                //if it doesnt have input-group, wrap it
                if (inputField.parents(".input-group").length === 0) {
                    const textareaEditor = inputField.closest(".md-textarea-editor");
                    (textareaEditor.length > 0 ? textareaEditor : inputField).wrap('<div class="input-group"></div>');
                }

                //if it doesnt have ti-sparkles button add it
                if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                    const button = this._getOtherButton(inputField);
                    inputField.parents(".input-group").append(button);
                }
            }
        });
    }

    stopExecution() {
        this.stopped = true;
        this.aiUserInterface.saveUserPrompt();
        this.aiUserInterface.setCurrentStatus("components.ai_assistants.stopping.js", false);
        if (this.aiBrowserExecutor != null) this.aiBrowserExecutor.destroy();
        if (this.aiRestExecutor != null) this.aiRestExecutor.destroy();
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
        //console.log("Determining column type for field:", fieldName, "column=", column);
        try {
            let className = column.className ?? "";
            let renderFormat = column.renderFormat ?? "";

            if(className.indexOf("image") != -1 || renderFormat.indexOf("dt-format-image") != -1) return "image";

            if (fieldName != null && fieldName != "") {
                let inputField = $(".form-control[name='"+fieldName+"']");
                if (inputField.length===0) inputField = $("#"+fieldName);
                renderFormat = $(inputField).attr("data-ai-render-format") ?? "";
            }

            if(renderFormat.indexOf("dt-format-image") != -1) return "image";
        } catch (error) {
            console.error("Error determining column type:", error);
        }

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

    /** Returns a reusable chat with editor-specific insertion callbacks for this field. */
    getChatConversation(button, column, aiCol) {
        const context = this._getChatContext(aiCol.to || column.name);
        // A saved field shares one host across assistants; an unsaved field keeps a separate transient chat per assistant.
        const key = JSON.stringify([context, aiCol.to || column.name, context ? null : aiCol.assistantId]);
        let conversation = this.chatConversations.get(key);
        if (conversation == null) {
            conversation = { key, button, column, aiCol, selection: null, undo: null, applying: false, revision: 0 };
            const toolbarActions = () => conversation.undo == null ? [] : [{
                id: "undo", label: WJ.translate("components.ai_assistants.chat.undo.js"), icon: "arrow-back",
                disabled: conversation.applying, run: () => this.undoChatInsertion(conversation)
            }];
            const chatOptions = assistant => ({
                assistant,
                showAttachment: true,
                allowAttachments: ['openai', 'gemini'].includes(assistant.provider)
                    && this._getColumnType(column, assistant.from) === 'text',
                getInputData: () => ({
                    inputValue: this._isChatSelectionValid(conversation.selection)
                        ? conversation.selection.inputValue : this.EDITOR.get(assistant.from || assistant.to || column.name),
                    inputValueType: this._getColumnType(conversation.column, assistant.from),
                    outputValueType: "text"
                }),
                getAnswerActions: message => this._getChatAnswerActions(conversation, message),
                getToolbarActions: toolbarActions,
                onChange: () => this._updateChatLoader(conversation),
                onReset: () => { conversation.revision++; conversation.undo = null; }
            });
            if (context != null) {
                const assistants = (column.ai || [aiCol]).filter(item => item.action === 'live_chat'
                    && (item.to || column.name) === context.fieldName).map(item => ({ ...item,
                        persistentChat: item.persistentChat === true && this._getColumnType(column, item.from) === 'text' }));
                conversation.host = new AiChatHost({
                    context, assistants, assistantId: aiCol.assistantId, historyOpen: true,
                    getChatOptions: chatOptions,
                    onShowChat: chat => {
                        if (conversation.chat !== chat) {
                            conversation.revision++;
                            conversation.undo = null;
                        }
                        conversation.chat = chat;
                        conversation.aiCol = { ...aiCol, ...chat.assistant, to: context.fieldName };
                        if (this.aiUserInterface.currentChat === conversation) {
                            this.aiUserInterface._renderHeader(conversation.button, conversation.column, conversation.aiCol);
                        }
                    }
                });
            } else {
                conversation.chat = new AiChat(chatOptions(aiCol));
            }
            conversation.view = conversation.host || conversation.chat;
            this.chatConversations.set(key, conversation);
        }
        conversation.button = button;
        conversation.column = column;
        conversation.aiCol = aiCol;
        if (conversation.host) {
            conversation.host.unmount();
            conversation.host.selectAssistant(String(aiCol.assistantId));
        } else {
            conversation.chat.assistant = aiCol;
            conversation.chat.options.allowAttachments = ['openai', 'gemini'].includes(aiCol.provider)
                && this._getColumnType(column, aiCol.from) === 'text';
        }
        conversation.selection = this._captureChatSelection(column, aiCol);
        return conversation;
    }

    /** Uses the REST resource and saved record ID so identical fields in different editors cannot share history. */
    _getChatContext(fieldName) {
        const data = this.EDITOR.TABLE?.DATA;
        const ids = this.EDITOR.ids?.();
        if (!data?.url || this.EDITOR.mode?.() !== 'edit' || ids?.length !== 1) return null;
        const id = ids[0];
        if (id == null || String(id).trim() === '' || Number(id) <= 0) return null;
        return { entityType: new URL(data.url, window.location.href).pathname.replace(/\/$/, ''),
            entityId: String(id), fieldName };
    }

    _getChatAnswerActions(conversation, message) {
        if (!message.text?.trim()) return [];
        return [{
            id: "add", label: WJ.translate("components.ai_assistants.chat.add.js"), icon: "plus", iconOnly: true,
            disabled: conversation.applying, run: () => this.insertChatAnswer(conversation, message, "append")
        }, {
            id: "replace", icon: "replace", iconOnly: true, disabled: conversation.applying,
            label: WJ.translate(conversation.selection != null
                ? "components.ai_assistants.chat.replaceSelection.js" : "components.ai_assistants.chat.replace.js"),
            run: () => this.insertChatAnswer(conversation, message, "replace")
        }];
    }

    /** Captures a rich-text selection without inserting bookmarks into the document. */
    _captureChatSelection(column, aiCol) {
        if (column.type !== "wysiwyg" || aiCol.to !== column.name) return null;
        const wjeditor = this.EDITOR.field(aiCol.to)?.s?.opts?.wjeditor;
        const editors = wjeditor?.editingMode === "pageBuilder" ? wjeditor.getWysiwygEditors() : [wjeditor?.ckEditorInstance];
        for (const editor of editors) {
            if (editor == null || editor.status !== "ready") continue;
            const selection = editor.getSelection();
            if (!selection?.getSelectedText()) continue;
            const ranges = selection.getRanges();
            if (ranges.length === 0) continue;
            const container = document.createElement("div");
            for (const range of ranges) container.appendChild(range.cloneContents().$);
            return { editor, ranges: ranges.map(range => range.clone()), value: editor.getData(), inputValue: container.innerHTML };
        }
        return null;
    }

    _isChatSelectionValid(selection) {
        if (selection == null || selection.editor.status !== "ready" || selection.editor.getData() !== selection.value) return false;
        const root = selection.editor.editable()?.$;
        return root?.isConnected && selection.ranges.every(range =>
            root.contains(range.startContainer.$) && root.contains(range.endContainer.$));
    }

    _updateChatLoader(conversation) {
        const working = [...this.chatConversations.values()].some(item =>
            item.aiCol.to === conversation.aiCol.to && item.view.isPending());
        if (working) this._showLoader(conversation.button);
        else this._hideLoader(conversation.button);
    }

    /** Clears all chat state only after the enclosing editor has really closed. */
    disposeChats() {
        for (const conversation of this.chatConversations.values()) conversation.view.destroy();
        this.chatConversations.clear();
        this.chatApplyingFields.clear();
        if (this.aiUserInterface.isActive()) this._closeToast();
        this.aiUserInterface.detachChat();
    }

    /** Applies a completed answer explicitly, capturing the current document for undo. */
    async insertChatAnswer(conversation, message, mode) {
        if (conversation.chat.state.disposed || message.status !== "complete" || !["append", "replace"].includes(mode)) return;
        const target = conversation.aiCol.to;
        if (this.chatApplyingFields.has(target)) return;
        const revision = conversation.revision;
        const operation = {};
        this.chatApplyingFields.set(target, operation);
        conversation.applying = true;
        conversation.chat.refresh();
        const selection = mode === "replace" ? conversation.selection : null;
        try {
            const fieldOptions = typeof this.EDITOR.field === "function" ? this.EDITOR.field(target)?.s?.opts : null;
            await fieldOptions?.wjeditorReadyPromise;
            // Editor initialization is asynchronous: reject an insertion if the user switched chats or closed the editor.
            if (conversation.chat.state.disposed || conversation.revision !== revision) return;
            if (selection != null && !this._isChatSelectionValid(selection)) {
                conversation.chat.state.notice = WJ.translate("components.ai_assistants.chat.selectionStale.js");
                return;
            }
            const content = parseAiResponse(message.text).html;
            if (typeof content !== "string" || !content.trim()) return;
            const wjeditor = fieldOptions?.wjeditor;
            // Capture undo at insertion time, then use the selected editor, PageBuilder or ordinary field write path.
            const undo = { target, value: this.EDITOR.get(target), wjeditor: wjeditor?.editingMode === "pageBuilder" ? wjeditor : null };
            if (wjeditor != null && typeof undo.value !== "string") throw new Error("Editor content is unavailable.");
            if (selection != null) {
                undo.selectionEditor = selection.editor;
                undo.selectionValue = selection.editor.getData();
                selection.editor.fire("saveSnapshot");
                selection.editor.getSelection().selectRanges(selection.ranges);
                selection.editor.insertHtml(content);
                selection.editor.fire("saveSnapshot");
                conversation.selection = null;
            } else if (undo.wjeditor != null) {
                undo.value = undo.wjeditor.getData();
                undo.wjeditor.pbInsertContent(content, mode, true);
            } else {
                this.EDITOR.set(target, mode === "append" ? (undo.value ?? "") + content : content);
                await fieldOptions?.wjeditorReadyPromise;
            }
            if (conversation.chat.state.disposed || conversation.revision !== revision) return;
            undo.appliedValue = this.EDITOR.get(target);
            // An older field snapshot must never overwrite a later insertion from another chat.
            for (const other of this.chatConversations.values()) {
                if (other.aiCol.to === target) other.undo = null;
            }
            conversation.undo = undo;
            conversation.chat.state.notice = null;
        } catch (error) {
            conversation.chat.state.notice = WJ.translate("components.ai_assistants.chat.insertFailed.js");
            console.error("Could not insert chat answer:", error);
        } finally {
            conversation.applying = false;
            if (this.chatApplyingFields.get(target) === operation) this.chatApplyingFields.delete(target);
            conversation.chat.refresh();
        }
    }

    async undoChatInsertion(conversation) {
        const undo = conversation.undo;
        if (undo == null || conversation.chat.state.disposed || this.chatApplyingFields.has(undo.target)) return;
        const revision = conversation.revision;
        const operation = {};
        this.chatApplyingFields.set(undo.target, operation);
        conversation.applying = true;
        conversation.chat.refresh();
        try {
            const fieldOptions = typeof this.EDITOR.field === "function" ? this.EDITOR.field(undo.target)?.s?.opts : null;
            await fieldOptions?.wjeditorReadyPromise;
            if (conversation.chat.state.disposed || conversation.revision !== revision) return;
            if (this.EDITOR.get(undo.target) !== undo.appliedValue) {
                conversation.undo = null;
                conversation.chat.state.notice = WJ.translate("components.ai_assistants.chat.undoStale.js");
                return;
            }
            if (undo.selectionEditor?.status === "ready") {
                await new Promise((resolve, reject) => {
                    const editor = undo.selectionEditor;
                    const finish = () => {
                        editor.removeListener("destroy", finish);
                        resolve();
                    };
                    // Destroying CKEditor removes dataReady callbacks, so closure must also settle the write.
                    editor.on("destroy", finish);
                    try {
                        editor.setData(undo.selectionValue, finish);
                    } catch (error) {
                        editor.removeListener("destroy", finish);
                        reject(error);
                    }
                });
            } else if (undo.wjeditor != null) undo.wjeditor.pbInsertContent(undo.value, "replace", true);
            else {
                this.EDITOR.set(undo.target, undo.value);
                await fieldOptions?.wjeditorReadyPromise;
            }
            conversation.undo = null;
            conversation.selection = null;
            conversation.chat.state.notice = null;
        } catch (error) {
            conversation.chat.state.notice = WJ.translate("components.ai_assistants.chat.insertFailed.js");
        } finally {
            conversation.applying = false;
            if (this.chatApplyingFields.get(undo.target) === operation) this.chatApplyingFields.delete(undo.target);
            conversation.chat.refresh();
        }
    }

    async _executeAction(button, column, aiCol, inputValues = null) {
        if (aiCol.action === "live_chat") {
            this.aiUserInterface.openChat(this.getChatConversation(button, column, aiCol));
            return;
        }
        // Implement AI button click handling logic here
        //console.log("Executing action for AI column:", aiCol);

        let self = this;
        self._showLoader(button);
        self.undoField = null;
        self.stopped = false;

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
        inputData.inputValueType = this._getColumnType(column, aiCol.from);
        inputData.outputValueType = this._getColumnType(column, aiCol.to);

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
                if (self.stopped === true) {
                    executionResult.stopped = true;
                    break;
                }

                let editor = editors[i];
                if (i>0) reuseApiInstance = true;
                //console.log("Executing on editor: ", editor);

                self.setCurrentStatus("components.ai_assistants.editor.loading.js", false, (i+1)+"/"+editors.length);

                inputData.inputValue = editor.getData();
                self.undoField.editors.push({instance: editor, value: inputData.inputValue});

                let editorExecutionResult = await this._executeSingleAction(button, column, aiCol, inputData, reuseApiInstance, (response) => {
                    //console.log("response="+response, "setting to editor: ", editor);

                    //if response contains section wrapped extract only inner part, it is often wrapped by AI into section element, but we are allready inside outer section
                    let startSection = response.indexOf("<section");
                    let endSection = response.lastIndexOf("</section>");
                    if (startSection != -1 && endSection != -1) {
                        //move to end of <section tag
                        startSection = response.indexOf(">", startSection);
                        response = response.substring(startSection+1, endSection+10);
                    }

                    if (self.stopped === false) editor.setData(response);
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
                }
            }

            if (successCount === editors.length) executionResult.success = true;
            executionResult.explanatoryText = explanatoryText;

        } else {

            inputData.inputValue = self.EDITOR.get(from);
            try {
                self.undoField = {};
                self.undoField.type = "field";
                self.undoField.value = self.EDITOR.get(aiCol.to);
                self.undoField.to = aiCol.to;
            } catch (error) {
                console.error("Error setting undoField:", error);
            }
            executionResult = await this._executeSingleAction(button, column, aiCol, inputData);

            if (executionResult!=null) totalTokens += executionResult.totalTokens;
        }

        //console.log("executionResult=", executionResult, "totalTokens=", totalTokens, "stopped=", self.stopped);

        if (executionResult == null) {
            self._hideLoader(button);
            self.setError();
        } else {
            if (executionResult.errorText != null) {
                self._hideLoader(button);
                self.setError(executionResult.errorText);
            } else if (executionResult.statusKey != null) {
                self.setCurrentStatus(executionResult.statusKey, false, ...(executionResult.statusKeyParams ?? []));
            } else if (executionResult.success === true) {
                if (self.isUndo()==false) self._closeToast(3000);
                self._hideLoader(button);

                self.setCurrentStatus("components.ai_assistants.stat.totalTokens.js", false, totalTokens);
            }

            if (executionResult.stopped === true || self.stopped === true) {
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
        } else {
            //return to list of AI assistants as failsafe
            $("#toast-container-ai .header-back-button .btn-outline-secondary").trigger("click");
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

            if ("image"===inputData.outputValueType) {
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
