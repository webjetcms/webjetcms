import {AiExecutionResult} from './ai-execution-result';

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

        //debugger;

        if (aiCol.useStreaming===true) {
            //console.log("Using streaming for AI response:", aiCol.assistant);

            try {
                const response = await fetch("/admin/rest/ai/assistant/response-stream/", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json; charset=utf-8",
                        'X-CSRF-Token': window.csrfToken
                    },
                    body: JSON.stringify(inputData)
                });

                if (!response.ok) {
                    let bodyText = "";
                    try { bodyText = await response.text(); } catch {}
                    console.log(`ERR HTTP ${response.status} ${response.statusText}`);
                }

                const reader = response.body.getReader();
                const decoder = new TextDecoder("utf-8");
                let wholeText = "";
                let done = false;
                let totalTokens = 0;

                while (!done) {
                    const { value, done: streamDone } = await reader.read();
                    done = streamDone;
                    if (done) {
                        //console.log("Stream complete : ", value);
                        break;
                    }

                    const chunk = decoder.decode(value, { stream: true });
                    //console.log("Received:", chunk);

                    let isJson = false;
                    let parsed = null;
                    try {
                        parsed = JSON.parse(chunk);
                        isJson = typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed);
                    } catch (e) {
                        isJson = false;
                    }

                    if (isJson == false) {
                        wholeText += chunk;
                        //self.EDITOR.set(aiCol.to, wholeText);

                        //console.log("Setting partial text:", wholeText);

                        let explanatoryJson = self._handleExplanatoryResponse(wholeText);
                        if (setFunction != null) {
                            setFunction(explanatoryJson.html, false);
                        } else {
                            self.EDITOR.set(aiCol.to, explanatoryJson.html);
                        }
                        executionResult.success = true;
                    } else {
                        //handle parsed.error
                        totalTokens += parsed.totalTokens;
                        if (parsed.error) {
                            executionResult.errorText = parsed.error;
                        }
                    }
                }

                //console.log("Setting final text:", wholeText);
                executionResult.totalTokens = totalTokens;

                let explanatoryJson = self._handleExplanatoryResponse(wholeText);
                if (setFunction != null) {
                    setFunction(explanatoryJson.html, true);
                } else {
                    self.EDITOR.set(aiCol.to, explanatoryJson.html);
                }
                if (explanatoryJson.explanatoryText != null) {
                    executionResult.explanatoryText = explanatoryJson.explanatoryText;
                }
            } catch (err) {
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
        if (startBlock==0) {
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

        //console.log("Parsed JSON:", parsedJson);

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
                    self.editorAiInstance.aiUserInterface.renderImageSelection(button, aiCol, res.tempFiles, res.generatedFileName, aiCol.to, "components.ai_assistants.stat.totalTokens.js", res.totalTokens);
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