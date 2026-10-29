import {AiExecutionResult} from './ai-execution-result';
import { readAiStream } from '../../../src/js/ai/ai-stream';

/**
 * Class to wrap REST API for AI execution logic
 */
export class AiRestExecutor {

    EDITOR = null;
    editorAiInstance = null;

    curentAssistantId = null;
    curentTimestamp = null;
    conversation = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    async execute(aiCol, inputData, setFunction = null) {
        let self = this;
        let executionResult = new AiExecutionResult();

        inputData.assistantId = aiCol.assistantId;
        inputData.timestamp = Date.now();

        let conversation = null;
        if (aiCol.action === "live_chat") {
            if (self.conversation == null || self.conversation.assistantId !== aiCol.assistantId
                || self.conversation.to !== aiCol.to) {
                self.conversation = { assistantId: aiCol.assistantId, to: aiCol.to, id: null };
            }
            conversation = self.conversation;
        } else {
            self.resetConversation();
        }
        if (conversation?.id) inputData.conversationId = conversation.id;
        else delete inputData.conversationId;

        self.curentAssistantId = inputData.assistantId;
        self.curentTimestamp = inputData.timestamp;

        //debugger;

        if (aiCol.useStreaming===true) {
            //console.log("Using streaming for AI response:", aiCol.assistant);

            try {
                const response = await fetch("/admin/rest/ai/assistant/response-stream/", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json; charset=utf-8",
                        "Accept": "application/x-ndjson",
                        'X-CSRF-Token': window.csrfToken
                    },
                    body: JSON.stringify(inputData)
                });

                let wholeText = "";
                const result = await self._readStream(response, text => {
                    wholeText += text;
                    const explanatoryJson = self._handleExplanatoryResponse(wholeText);
                    if (setFunction != null) {
                        setFunction(explanatoryJson.html, false);
                    } else {
                        self.EDITOR.set(aiCol.to, explanatoryJson.html);
                    }
                });
                executionResult.totalTokens = result.totalTokens;

                if (result.error) {
                    executionResult.errorText = result.error;
                } else {
                    self._rememberConversation(conversation, result);
                    const explanatoryJson = self._handleExplanatoryResponse(wholeText);
                    if (setFunction != null) {
                        setFunction(explanatoryJson.html, true);
                    } else {
                        self.EDITOR.set(aiCol.to, explanatoryJson.html);
                    }
                    executionResult.explanatoryText = explanatoryJson.explanatoryText;
                    executionResult.success = true;
                }
            } catch (err) {
                executionResult.errorText = err.message || String(err);
                console.log("ERRR : ", err);
            }
        } else {
            await $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response/",
                data: JSON.stringify(inputData),
                contentType: "application/json; charset=utf-8",
                success: function(res)
                {
                    //console.log("AI response=", res, "to=", aiCol.to);

                    //handle res.error
                    if (res.error) {
                        executionResult.errorText = res.error;
                    } else {
                        self._rememberConversation(conversation, res);
                        executionResult.success = true;

                        //console.log("AI response received, response=", res);
                        let explanatoryJson = self._handleExplanatoryResponse(res.response);
                        if (setFunction != null) {
                            setFunction(explanatoryJson.html, true);
                        } else {
                            self.EDITOR.set(aiCol.to, explanatoryJson.html);
                        }
                        if (explanatoryJson.explanatoryText != null) {
                            executionResult.explanatoryText = explanatoryJson.explanatoryText;
                        }
                        executionResult.totalTokens = res.totalTokens;
                    }
                },
                error: function(xhr, ajaxOptions, thrownError) {
                    executionResult.errorText = thrownError;
                }
            });
        }

        //Remove old run info
        self.curentAssistantId = null;
        self.curentTimestamp = null;

        return executionResult;
    }

    resetConversation() {
        this.conversation = null;
    }

    /**
     * Saves the successful continuation without reviving a closed or replaced conversation.
     * @param {object|null} conversation Conversation associated with the outgoing request.
     * @param {object} response Completed assistant response containing the opaque continuation ID.
     */
    _rememberConversation(conversation, response) {
        if (conversation != null && this.conversation === conversation) {
            conversation.id = typeof response.conversationId === "string" && response.conversationId.length > 0
                ? response.conversationId : null;
        }
    }

    /**
     * Reads newline-delimited messages independently of HTTP chunk boundaries.
     * A terminal result is required before a streamed edit can be considered complete.
     * @param {Response} response HTTP response containing delta and result messages.
     * @param {function(string): void} onDelta Receives generated text as it arrives.
     * @returns {Promise<object>} Final assistant response, including tokens and any provider error.
     */
    async _readStream(response, onDelta) {
        return readAiStream(response, onDelta);
    }

    /**
     * Sometimes AI returns response as markdown, which contains HTML code block and explanatory text below
     *
     * ```html
     * HTML code
     * ```
     * Provided changes explanatory text
     *
     * we need to parse this and return it as JSON object with HTML part and explanatory part to handle it properly in UI
     * @param {*} wholeText
     */
    _handleExplanatoryResponse(wholeText) {
        const parsedJson = {
            wholeText: wholeText,
            html: null,
            explanatoryText: null
        }

        if (wholeText == null || wholeText.trim().length == 0) return parsedJson;

        let startBlock = wholeText.indexOf("```html");
        if (startBlock!=-1) {
            //if it starts with ```html markdown
            let endBlock = wholeText.indexOf("```", startBlock + 7);
            if (endBlock == -1) {
                //we still don't have an end block
                parsedJson.html = wholeText.substring(startBlock + 7).trim();
            } else {
                //we have an end block, extract HTML and explanatory block
                parsedJson.html = wholeText.substring(startBlock + 7, endBlock).trim();
                //if there is explanatory text use it
                if (wholeText.length > endBlock + 10) parsedJson.explanatoryText = wholeText.substring(endBlock + 3).trim();
            }
        } else {
            parsedJson.html = wholeText;
        }

        //if it has explanatory unwrap it from possible ```markdown container
        if (parsedJson.explanatoryText != null) {
            parsedJson.explanatoryText = parsedJson.explanatoryText.replace(/```markdown/g, "").replace(/```$/g, "").trim();
            //replace \" to "
            parsedJson.explanatoryText = parsedJson.explanatoryText.replace(/\\"/g, '"');
            //replace escaped comments &amp;lt;!-- to <!-- and --&amp;gt; to -->
            parsedJson.explanatoryText = parsedJson.explanatoryText.replace(/&amp;lt;!--/g, "&lt!--").replace(/--&amp;gt;/g, "--&gt;");
        }

        //if html contains body tags, extract only body content
        if (parsedJson.html != null) {
            let bodyStart = parsedJson.html.indexOf("<body");
            let bodyEnd = parsedJson.html.indexOf("</body>");
            if (bodyStart != -1 && bodyEnd != -1) {
                bodyStart = parsedJson.html.indexOf(">", bodyStart);
                parsedJson.html = parsedJson.html.substring(bodyStart+1, bodyEnd).trim();
            }
        }

        return parsedJson;
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
        self.resetConversation();

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
