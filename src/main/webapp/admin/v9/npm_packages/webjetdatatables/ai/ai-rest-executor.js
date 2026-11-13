import {AiExecutionResult} from './ai-execution-result';
import { requestAiText } from '../../../src/js/ai/ai-rest-transport';
import { parseAiResponse } from '../../../src/js/ai/ai-response-parser';
import { toolReply } from '../../../src/js/ai/ai-tool-transport';

/**
 * Class to wrap REST API for AI execution logic
 */
export class AiRestExecutor {

    EDITOR = null;
    editorAiInstance = null;

    curentAssistantId = null;
    curentTimestamp = null;
    controller = null;

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
        const controller = new AbortController();
        self.controller = controller;

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
                signal: controller.signal,
                toolsEnabled: aiCol.toolsEnabled,
                streaming,
                onDelta: text => {
                    wholeText += text;
                    if (!aiCol.toolsWrite) setText(wholeText, false);
                }
            });
            executionResult.totalTokens = result.totalTokens ?? 0;
            if (result.error) {
                executionResult.errorText = result.error;
                if (aiCol.toolsWrite) executionResult.explanatoryText = toolReply(result, true);
            } else if (aiCol.toolsWrite) {
                executionResult.explanatoryText = toolReply(result, true);
                if (result.toolProblem || !result.toolStatus) executionResult.statusKey = 'components.ai.tools.unverified.js';
                executionResult.success = true;
            } else {
                const parsed = setText(result.response ?? wholeText, true);
                executionResult.explanatoryText = parsed.explanatoryText;
                executionResult.success = true;
            }
        } catch (error) {
            executionResult.errorText = error.message || String(error);
            executionResult.explanatoryText = error.toolStatus;
        } finally {
            if (self.controller === controller) {
                self.curentAssistantId = null;
                self.curentTimestamp = null;
                self.controller = null;
            }
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

        try {
            const res = await $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response-image/",
                data: JSON.stringify(inputData),
                contentType: "application/json; charset=utf-8"
            });
            if (res.error) {
                executionResult.errorText = res.error;
            } else {
                if (typeof res.tempFiles == "object" && res.tempFiles.length > 0) {
                    self.editorAiInstance.aiUserInterface.renderImageSelection(button, aiCol, res.tempFiles, res.generatedFileName, aiCol.to, "components.ai_assistants.stat.totalTokens.js", res.totalTokens);
                } else {
                    executionResult.errorText = WJ.translate("components.ai_assistants.editor.image_generation.no_images.js");
                }
                executionResult.totalTokens = res.totalTokens;
                // Image selection handles the successful response without the generic success dialog.
                executionResult.success = false;
            }
        } catch (error) {
            executionResult.errorText = error.responseJSON?.error || error.statusText || error.message ||
                (error.status != null ? "HTTP " + error.status : String(error));
        } finally {
            self.curentAssistantId = null;
            self.curentTimestamp = null;
        }

        return executionResult;
    }

    destroy() {
        let self = this;
        self.controller?.abort();

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
