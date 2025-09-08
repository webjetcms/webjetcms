/**
 * Class to wrap REST API for AI execution logic
 */
export class AiRestExecutor {

    EDITOR = null;
    editorAiInstance = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    async execute(aiCol, inputData, setFunction = null) {
        let self = this;
        let totalTokens = this.editorAiInstance.ERR_UNKNOWN;

        inputData.assistantId = aiCol.assistantId;

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
                    } else {
                        //handle parsed.error
                        totalTokens += parsed.totalTokens;
                        if (parsed.error) {
                            self._setError(parsed.error);
                            return self.editorAiInstance.DO_NOT_CLOSE_DIALOG;
                        }
                    }
                }

                //console.log("Setting final text:", wholeText);

                let explanatoryJson = self._handleExplanatoryResponse(wholeText);
                if (setFunction != null) {
                    setFunction(explanatoryJson.html, true);
                } else {
                    self.EDITOR.set(aiCol.to, explanatoryJson.html);
                }
                if (explanatoryJson.explanatory!=null) {
                    self.editorAiInstance.setExplanatoryText(explanatoryJson.explanatory);
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
                        totalTokens = self.editorAiInstance.DO_NOT_CLOSE_DIALOG;
                        self._setError(res.error);
                    } else {

                        //console.log("AI response received, response=", res);
                        let explanatoryJson = self._handleExplanatoryResponse(res.response);
                        if (setFunction != null) {
                            setFunction(explanatoryJson.html, true);
                        } else {
                            self.EDITOR.set(aiCol.to, explanatoryJson.html);
                        }
                        if (explanatoryJson.explanatory!=null) {
                            self.editorAiInstance.setExplanatoryText(explanatoryJson.explanatory);
                        }
                    }

                    totalTokens = res.totalTokens;
                },
                error: function(xhr, ajaxOptions, thrownError) {
                    totalTokens = self.editorAiInstance.DO_NOT_CLOSE_DIALOG;
                    self._setError(thrownError);
                }
            });
        }
        return totalTokens;
    }

    _setError(...params) {
        //console.log("Setting error:", ...params);
        this.editorAiInstance.setError(...params);
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
            explanatory: null
        }

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
                if (wholeText.length > endBlock + 10) parsedJson.explanatory = wholeText.substring(endBlock + 3).trim();
            }
        } else {
            parsedJson.html = wholeText;
        }

        //if it has explanatory unwrap it from possible ```markdown container
        if (parsedJson.explanatory != null) {
            parsedJson.explanatory = parsedJson.explanatory.replace(/```markdown/g, "").replace(/```$/g, "").trim();
            //replace \" to "
            parsedJson.explanatory = parsedJson.explanatory.replace(/\\"/g, '"');
        }

        return parsedJson;
    }

    async executeImageAction(aiCol, inputData, button) {
        let self = this;

        inputData.assistantId = aiCol.assistantId;

        await $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/response-image/",
            data: JSON.stringify(inputData),
            contentType: "application/json; charset=utf-8",
            success: function(res)
            {
                //handle res.error
                if (res.error) {
                    self._setError(res.error);
                } else {
                    self.editorAiInstance.aiUserInterface.renderImageSelection(button, res.tempFiles, aiCol.to, "components.ai_assistants.stat.totalTokens.js", res.totalTokens);
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {
                self._setError(thrownError);
            }
        });

        return self.editorAiInstance.DO_NOT_CLOSE_DIALOG;
    }

}