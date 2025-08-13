export class EditorAi {

    EDITOR = null;

    //constructor
    constructor(EDITOR) {
        this.EDITOR = EDITOR;
        console.log("EditorAi instance created, editor=", this.EDITOR);
        EDITOR.editorAi = this;

        this.bindEvents();
    }

    bindEvents() {
        window.addEventListener("WJ.DTE.opened", (event) => {
            console.log("WJ.DTE.opened event received, detail=", event.detail, "this.id=", this.EDITOR.TABLE.DATA.id);
            if (this.EDITOR.TABLE.DATA.id===event.detail.id) {
                console.log("WJ.DTE.open event triggered");
                this.bindButtons();
            }
        });
    }

    /**
     * Binds AI-related buttons to the provided EDITOR instance.
     */
    bindButtons() {
        console.log("Binding editor buttons, editor=", this.EDITOR);

        //iterate over EDITOR.TABLE.DATA.columns[].ai fields and generate AI button
        this.EDITOR.TABLE.DATA.columns.forEach(column => {
            if("object" == typeof column.ai && column.ai != null && column.ai.length > 0) {
                let field = this.EDITOR.field(column.name);
                if (field) {
                    console.log("Field found for column:", column.name, "field=", field.dom);
                    //append button into field
                    //field.dom.inputControl[0].appendChild(button);
                    //wrap inputControl into group using jquery wrap function
                    let inputField = $(field.dom.inputControl[0]).find(".form-control");

                    //if it doesnt have input-group, wrap it
                    if (inputField.parents(".input-group").length === 0) {
                        inputField.wrap('<div class="input-group"></div>');
                    }

                    //if it doesnt have ti-sparkles button add it
                    if (inputField.parents(".input-group").find(".ti-sparkles").length === 0) {
                        const button = $('<button class="btn btn-outline-secondary btn-ai" type="button" data-toggle="tooltip" title="'+WJ.translate('components.ai_assistants.editor.btn.tooltip.js')+'"><i class="ti ti-sparkles"></i><i class="ti ti-loader"></i></button>');
                        button.on('click', () => {
                            this.handleAiButtonClick(button, column);
                        });
                        inputField.parents(".input-group").append(button);
                    }
                }
            }
        });
    }

    handleAiButtonClick(button, column) {
        console.log("AI button clicked for column:", column);
        // Implement AI button click handling logic here

        let self = this;
        self._showLoader(button);

        $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/response/",
            data: {
                "assistantName": column.ai[0].assistant,
                "inputData": self.EDITOR.get(column.ai[0].from)
            },
            success: function(res)
            {
                console.log("AI response=", res);

                self._hideLoader(button);

                //handle res.error
                if (res.error) {
                    WJ.notifyError("AI", res.error);
                }

                self.EDITOR.set(column.ai[0].to, res.response);
                WJ.notifyInfo(WJ.translate("components.ai_assistants.editor.btn.tooltip.js"), WJ.translate("components.ai_assistants.stat.totalTokens.js", res.totalTokens), 3000);
            },
            error: function(xhr, ajaxOptions, thrownError) {

                self._hideLoader(button);

                WJ.notifyError("AI", WJ.translate("datatable.error.unknown"));
            }
        });
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
}