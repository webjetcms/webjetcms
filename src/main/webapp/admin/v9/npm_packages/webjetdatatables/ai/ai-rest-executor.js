import {AiExecutionResult} from './ai-execution-result';
import { requestAiText } from '../../../src/js/ai/ai-rest-transport';
import { parseAiResponse } from '../../../src/js/ai/ai-response-parser';

/**
 * Class to wrap REST API for AI execution logic
 */
export class AiRestExecutor {

    EDITOR = null;
    editorAiInstance = null;

    curentAssistantId = null;
    curentTimestamp = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    async execute(aiCol, inputData, setFunction = null) {
        let self = this;
        let executionResult = new AiExecutionResult();

        inputData.assistantId = aiCol.assistantId;
        inputData.timestamp = Date.now();

        self.curentAssistantId = inputData.assistantId;
        self.curentTimestamp = inputData.timestamp;

        const streaming = aiCol.useStreaming === true;
        const setText = (text, final) => {
            const parsed = parseAiResponse(text);
            if (setFunction != null) setFunction(parsed.html, final);
            else self.EDITOR.set(aiCol.to, parsed.html);
            return parsed;
        };
        try {
            let wholeText = "";
            const result = await requestAiText(inputData, {
                streaming,
                onDelta: text => {
                    wholeText += text;
                    setText(wholeText, false);
                }
            });
            executionResult.totalTokens = result.totalTokens ?? 0;
            if (result.error) {
                executionResult.errorText = result.error;
            } else {
                const parsed = setText(streaming ? wholeText : result.response, true);
                executionResult.explanatoryText = parsed.explanatoryText;
                executionResult.success = true;
            }
        } catch (error) {
            executionResult.errorText = error.message || String(error);
        } finally {
            self.curentAssistantId = null;
            self.curentTimestamp = null;
        }

        return executionResult;
    }

    async executeImageAction(aiCol, inputData, button) {
        let self = this;
        let executionResult = new AiExecutionResult();

        inputData.assistantId = aiCol.assistantId;
        inputData.timestamp = Date.now();

        self.curentAssistantId = inputData.assistantId;
        self.curentTimestamp = inputData.timestamp;

        await $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/response-image/",
            data: JSON.stringify(inputData),
            contentType: "application/json; charset=utf-8",
            success: function(res)
            {
                //handle res.error
                if (res.error) {
                    executionResult.errorText = res.error;
                } else {
                    if (typeof res.tempFiles == "object" && res.tempFiles.length>0) self.editorAiInstance.aiUserInterface.renderImageSelection(button, aiCol, res.tempFiles, res.generatedFileName, aiCol.to, "components.ai_assistants.stat.totalTokens.js", res.totalTokens);
                    else executionResult.errorText = WJ.translate("components.ai_assistants.editor.image_generation.no_images.js");
                    executionResult.totalTokens = res.totalTokens;
                    //set to false, otherwise dialog will be rerendered with default success message
                    executionResult.success = false;
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {
                executionResult.errorText = thrownError;
            }
        });

        //Remove old run info
        self.curentAssistantId = null;
        self.curentTimestamp = null;

        return executionResult;
    }

    destroy() {
        let self = this;

        //console.log("ai-rest-execution self", self);

        if(self.curentAssistantId != null && self.curentTimestamp != null) {
            $.ajax({
                type: "GET",
                url: "/admin/rest/ai/assistant/stop-assistant/",
                data: {
                    assistantId : self.curentAssistantId,
                    timestamp : self.curentTimestamp
                },
                success: function(res)
                {
                    //console.log("SUCCESSS : ", res);
                }
            });
        }

        self.curentAssistantId = null;
        self.curentTimestamp = null;
    }

}
