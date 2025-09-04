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
                        "Content-Type": "application/json",
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
                        console.log("Stream complete : ", value);
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

                        if (setFunction != null) {
                            setFunction(wholeText, false);
                        } else {
                            self.EDITOR.set(aiCol.to, wholeText);
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

                if (setFunction != null) {
                    setFunction(wholeText, true);
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
                        if (setFunction != null) {
                            setFunction(res.response, true);
                        } else {
                            self.EDITOR.set(aiCol.to, res.response);
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
        console.log("Setting error:", ...params);
        this.editorAiInstance.setError(...params);
    }

    async executeImageAction(aiCol, inputData, callback = null) {
        let self = this;
        let totalTokens = this.editorAiInstance.ERR_UNKNOWN;

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
                    totalTokens = self.editorAiInstance.ERR_CLOSE_DIALOG;
                    self._setError(res.error);
                }

                totalTokens = res.totalTokens;
                if (callback != null) {
                    callback(res);
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {
                totalTokens = self.editorAiInstance.ERR_CLOSE_DIALOG;
                self._setError(thrownError);
            }
        });

        return totalTokens;
    }

}