/**
 * Class to wrap Local Browser AI execution logic
 */
export class AiBrowserExecutor {

    //TODO: cache API created with _apiInitialize
    localApiCache = {};
    lastApiInstance = null;
    languageDetector = null;

    EDITOR = null;
    editorAiInstance = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    isAvailable(aiCol = null) {
        let apiName = "Translator";
        if (aiCol != null) {
            let instructions = aiCol.instructions;
            apiName = this._getApiName(instructions);
        }

        //check for browser AI instances support
        if (apiName in self) {
            return true;
        }

        this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.api_not_available.js", false, apiName);
        return false;
    }

    async execute(aiCol, inputData, setFunction = null) {

        let instructions = aiCol.instructions;
        let totalTokens = this.editorAiInstance.ERR_UNKNOWN;

        //console.log("execute, inputData=", inputData);
        try {
            let apiName = this._getApiName(instructions);
            if (apiName == null) {
                this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.unknownApi.js", false, apiName);
            } else {
                let config = this._getConfig(instructions);
                let apiInstance = await this._apiInitialize(apiName, config);
                let useStreaming = aiCol.useStreaming || false;
                this.lastApiInstance = apiInstance; //store last instance for destroy
                if (apiInstance) {
                    totalTokens = await this._apiExecute(apiName, apiInstance, config, aiCol.to, inputData, useStreaming, setFunction);
                }
            }
        } catch (e) {
            console.log(e);
            this.editorAiInstance.setError(e.message);
            return this.editorAiInstance.DO_NOT_CLOSE_DIALOG;
        }

        return totalTokens;
    }

    /**
     * Call this to destroy current instance and stop streaming
     */
    destroy() {
        //console.log("Destroying instance=", this.lastApiInstance);
        if (this.lastApiInstance) {
            this.lastApiInstance.destroy();
            this.lastApiInstance = null;
        }
    }

    _getApiName(instructions) {
        if (typeof instructions == "undefined" || instructions == null || instructions == "") return null;

        //before first : there is API name in string
        let i = instructions.indexOf(":");
        if (i !== -1) {
            let apiString = instructions.substring(0, i).trim();
            return apiString;
        }
        return null;
    }

    _getConfig(instructions) {
        //after first : there is JSON config in string
        let i = instructions.indexOf(":");
        if (i !== -1) {
            let configString = instructions.substring(i + 1).trim();
            //console.log("configString:", configString);
            try {
                let config = JSON.parse(configString);
                return config;
            } catch (e) {
                console.error("Failed to parse config JSON:", e, "configString:", configString);
            }
        }
        return {};
    }

    async _apiInitialize(apiName, config, skipIsActive = false) {
        let instance = this;
        let apiInstance = null;

        config.monitor = (m) => {
            m.addEventListener('downloadprogress', (e) => {
                instance._setDownloadStatus(e);
            });
        };

        if (apiName in self) {
            if (navigator.userActivation.isActive || skipIsActive === true) {
                apiInstance = await self[apiName].create(config);
            } else {
                throw new Error(WJ.translate("components.ai_assistants.browser.userInteractionError.js"));
            }
        }
        //console.log("apiInstance:", apiInstance, "apiName=", apiName, "isInSelf=", apiName in self, "config=", config);
        return apiInstance;
    }

    async _apiExecute(apiName, apiInstance, config, fieldName, inputData, useStreaming, setFunction = null) {
        if (apiInstance) {
            let text = inputData.inputValue;
            if (inputData.userPrompt != null) {
                text = inputData.userPrompt;
            }

            //console.log("_apiExecute, apiName=", apiName, "apiInstance=", apiInstance, "inputData=", inputData, " text:", text, "config=", config, "setFunction=", setFunction);

            let content = null;
            if (useStreaming) {
                let stream = null;
                if ("Writer" === apiName) stream = apiInstance.writeStreaming(text, config);
                else if ("Rewriter" === apiName) stream = apiInstance.rewriteStreaming(text, config);
                else if ("Translator" === apiName) stream = apiInstance.translateStreaming(text, config);
                else if ("LanguageDetector" === apiName) stream = apiInstance.detectLanguageStreaming(text, config);
                else if ("Summarizer" === apiName) stream = apiInstance.summarizeStreaming(text, config);
                else if ("LanguageModel" === apiName) stream = apiInstance.promptStreaming(text, config);

                if (stream!=null) content = await this._setField(fieldName, stream, setFunction);
            } else {
                if ("Writer" === apiName) content = await apiInstance.write(text, config);
                else if ("Rewriter" === apiName) content = await apiInstance.rewrite(text, config);
                else if ("Translator" === apiName) content = await apiInstance.translate(text, config);
                else if ("LanguageDetector" === apiName) content = await apiInstance.detectLanguage(text, config);
                else if ("Summarizer" === apiName) content = await apiInstance.summarize(text, config);
                else if ("LanguageModel" === apiName) content = await apiInstance.prompt(text, config);

                if (content != null) {
                    if (setFunction != null) {
                        setFunction(content);
                    } else {
                        this.EDITOR.set(fieldName, content);
                    }
                }
            }

            //AI APIs can't return content in requested language (e.g. slovak), so we need to translate it afterwards
            if (content != null && content.length > 0) {
                let translateOutputLanguage = config.translateOutputLanguage || null;
                if (typeof translateOutputLanguage != "undefined" && translateOutputLanguage != null) {

                    this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.translating.js", false);

                    let translateFromLanguage = await this._detectLanguage(content);
                    let translateToLanguage = window.userLng;

                    if ("preserve" === translateOutputLanguage || true === translateOutputLanguage) {
                        //detect source language
                        translateToLanguage = await this._detectLanguage(text);
                    }

                    //console.log("Translating from:", translateFromLanguage, "to:", translateToLanguage);

                    if (translateFromLanguage != translateToLanguage) {
                        this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.translatingFromTo.js", false, translateFromLanguage, translateToLanguage);

                        let config = {
                            sourceLanguage: translateFromLanguage,
                            targetLanguage: translateToLanguage
                        }
                        const translator = await this._apiInitialize("Translator", config, true);
                        //console.log("Mam translator: ", translator);
                        let translatedContent = await translator.translate(content);
                        //console.log("Translated content:", translatedContent, "content=", content);
                        if (setFunction != null) {
                            setFunction(translatedContent);
                        } else {
                            this.EDITOR.set(fieldName, translatedContent);
                        }
                    }
                }
            }

            //local browser uses zero tokens
            return 0;
        }
        return this.EDITOR.ERR_UNKNOWN;
    }

    async _detectLanguage(text) {
        if (this.languageDetector == null) {
            this.languageDetector = await LanguageDetector.create({});
        }
        const results = await this.languageDetector.detect(text);
        //console.log("Detected languages:", results);
        if (results.length > 0) {
            return results[0].detectedLanguage;
        }
        return null;
    }

    async _setField(fieldName, stream, setFunction = null) {
        let firstItem = true;
        let content = "";
        //console.log("Setting field:", fieldName, "stream=", stream);
        for await (const chunk of stream) {
            if (firstItem) {
                content = chunk;
            } else {
                content += chunk;
            }
            //console.log('\r'+content);

            if (setFunction != null) {
                setFunction(content);
            } else {
                this.EDITOR.set(fieldName, content);
            }
            firstItem = false;
        }
        return content;
    }

    _setDownloadStatus(progress) {
        //console.log("Progress: ", progress);

        let percent = Math.round(progress.loaded * 100);
        //skip this values, they are not useful
        if (percent == 0 || percent == 100) return;

        if (this.editorAiInstance) {
            this.editorAiInstance.setDownloadStatus(percent);
        } else {
            console.log("Download progress:", percent+"%");
        }
    }
}