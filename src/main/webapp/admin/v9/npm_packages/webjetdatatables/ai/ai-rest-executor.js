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
        let totalTokens = -1;
        if (aiCol.useStreaming===true) {
            //console.log("Using streaming for AI response:", aiCol.assistant);

            const response = await fetch("/admin/rest/ai/assistant/response-stream/", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    'X-CSRF-Token': window.csrfToken
                },
                body: JSON.stringify({
                    assistantName: aiCol.assistant,
                    inputData: JSON.stringify(inputData)
                })
            });

            const reader = response.body.getReader();
            const decoder = new TextDecoder("utf-8");
            let wholeText = "";
            let done = false;

            while (!done) {
                const { value, done: streamDone } = await reader.read();
                done = streamDone;
                if (done) {
                    //console.log("Stream complete");
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
                        setFunction(wholeText);
                    } else {
                        self.EDITOR.set(aiCol.to, wholeText);
                    }
                } else {
                    //handle parsed.error
                    if (parsed.error) {
                        totalTokens = -1;
                        self._setCurrentStatus(parsed.error);
                        break;
                    }

                    totalTokens += parsed.totalTokens;
                }
            }
        } else {
            await $.ajax({
                type: "POST",
                url: "/admin/rest/ai/assistant/response/",
                data: {
                    "assistantName": aiCol.assistant,
                    "inputData": JSON.stringify(inputData)
                },
                success: function(res)
                {
                    //console.log("AI response=", res, "to=", aiCol.to);

                    //handle res.error
                    if (res.error) {
                        totalTokens = -1;
                        self._setCurrentStatus("datatable.error.unknown");
                    } else {
                        if (setFunction != null) {
                            setFunction(res.response);
                        } else {
                            self.EDITOR.set(aiCol.to, res.response);
                        }
                    }

                    totalTokens = res.totalTokens;
                },
                error: function(xhr, ajaxOptions, thrownError) {
                    totalTokens = -1;
                    self._setCurrentStatus("datatable.error.unknown");
                }
            });
        }
        return totalTokens;
    }

    _setCurrentStatus(textKey, pulsate = false, ...params) {
        this.editorAiInstance._setCurrentStatus(textKey, pulsate, ...params);
    }

    async executeImageAction(aiCol, inputData, callback = null) {
        let self = this;
        let totalTokens = -1;

        await $.ajax({
            type: "POST",
            url: "/admin/rest/ai/assistant/response-image/",
            data: {
                "assistantName": aiCol.assistant,
                "inputData": JSON.stringify(inputData)
            },
            success: function(res)
            {
                //handle res.error
                if (res.error) {
                    totalTokens = -1;
                    self._setCurrentStatus("datatable.error.unknown", false, res.error);
                }

                totalTokens = res.totalTokens;
                if (callback != null) {
                    callback(res);
                }
            },
            error: function(xhr, ajaxOptions, thrownError) {

                self._setCurrentStatus("datatable.error.unknown");
            }
        });

        return totalTokens;
    }

}