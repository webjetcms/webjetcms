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
        this.aiLocalExecutor = new AiLocalExecutor(EDITOR);
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

    _handleAiButtonClick(button, column) {
        //console.log("AI button clicked for column:", column);

        this._clearProgress();

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
                progressBar: true
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
                this._executeAction(button, column, aiCol);
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

        let contentContainer = $("#toast-container-ai-content");
        contentContainer.html('<div class="group-title pulsate-text">' + WJ.translate("components.ai_assistants.editor.loading.js") + '</div>');

        let from = aiCol.from;
        if (from == null || from == "")  from = aiCol.to; //if from is not set, use to as from


        //for PageBuilder if to===pageBuilder element we need to execute function for every editor in PB page to prevent HTML structure issues

        var isPageBuilder = false;
        if ("wysiwyg" === column.type && aiCol.to === column.name) {
            let wjeditor = this.EDITOR.field(aiCol.to).s.opts.wjeditor;
            if (wjeditor != null && "pageBuilder" === wjeditor.editingMode) isPageBuilder = true;
        }

        if (isPageBuilder) {
            console.log("Executing action for PageBuilder editor:", aiCol, "column", column);
        }

        if ("local" === aiCol.provider) {
            await this.aiLocalExecutor.execute(aiCol);
            self._hideLoader(button, column);
            contentContainer.html(WJ.translate("components.ai_assistants.stat.totalTokens.js", 0));
            self._closeToast(3000);
        } else {
            $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response/",
                data: {
                    "assistantName": aiCol.assistant,
                    "inputData": self.EDITOR.get(from)
                },
                success: function(res)
                {
                    //console.log("AI response=", res, "to=", aiCol.to);

                    self._hideLoader(button, column);

                    //handle res.error
                    if (res.error) {
                        contentContainer.html(res.error);
                    }

                    self.EDITOR.set(aiCol.to, res.response);
                    contentContainer.html(WJ.translate("components.ai_assistants.stat.totalTokens.js", res.totalTokens));
                    self._closeToast(3000);
                },
                error: function(xhr, ajaxOptions, thrownError) {

                    self._hideLoader(button);

                    contentContainer.html(WJ.translate("datatable.error.unknown"));
                }
            });
        }
    }

    _showLoader(button, column) {
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
}