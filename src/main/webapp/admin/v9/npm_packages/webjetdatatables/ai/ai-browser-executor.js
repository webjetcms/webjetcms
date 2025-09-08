import {AiExecutionResult} from './ai-execution-result';

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
        let apiName = this.getApiName(aiCol);

        //check for browser AI instances support
        if (apiName in self) {
            return true;
        }
        return false;
    }

    getApiName(aiCol = null) {
        let apiName = "Translator";
        if (aiCol != null) {
            let instructions = aiCol.instructions;
            apiName = this._getApiName(instructions);
        }
        return apiName;
    }

    /**
     *
     * @param {*} aiCol
     * @param {*} inputData
     * @param {*} reuseApiInstance - set to true to reuse latest instance, EG for PageBuilder you need to reuse it for every editor
     * @param {*} setFunction
     * @returns {AiExecutionResult}
     */
    async execute(aiCol, inputData, reuseApiInstance = false, setFunction = null) {

        let instructions = aiCol.instructions;
        let executionResult = new AiExecutionResult();

        //console.log("execute, inputData=", inputData);
        try {
            let apiName = this._getApiName(instructions);
            if (apiName == null) {
                executionResult.statusKey = "components.ai_assistants.browser.unknownApi.js";
                executionResult.statusKeyParams = ["NULL"];
            } else {
                let config = this._getConfig(instructions);
                let apiInstance = null;

                if (reuseApiInstance === true && this.lastApiInstance != null) {
                    apiInstance = this.lastApiInstance;
                } else {
                    apiInstance = await this._apiInitialize(apiName, config, inputData);
                    this.lastApiInstance = apiInstance; //store last instance for destroy
                }

                let useStreaming = aiCol.useStreaming || false;
                if (apiInstance) {
                    executionResult = await this._apiExecute(apiName, apiInstance, config, aiCol.to, inputData, useStreaming, setFunction);
                }
            }
        } catch (e) {
            console.log(e);
            executionResult.error = e.message;
        }

        //console.log("executionResult:", executionResult);

        return executionResult;
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

    async _apiInitialize(apiName, config, inputData, skipIsActive = false) {
        let instance = this;
        let apiInstance = null;

        let text = inputData.inputValue;
        if (inputData.userPrompt != null) {
            text = inputData.userPrompt;
        }

        config.monitor = (m) => {
            m.addEventListener('downloadprogress', (e) => {
                instance._setDownloadStatus(e);
            });
        };

        if (apiName in self) {
            if ("Translator" === apiName) {
                if ("autodetect"===config.sourceLanguage) {
                    config.sourceLanguage = await this._detectLanguage(text);
                } else if ("userLng" === config.sourceLanguage) {
                    config.sourceLanguage = window.userLng;
                }
                if ("userLng" === config.targetLanguage) {
                    config.targetLanguage = window.userLng;
                }
            }

            if ("Translator" === apiName && navigator.userActivation.isActive===false) {
                //verify translation pair availability
                let status = await Translator.availability(config);
                if ("available" !== status) {
                    //we need to wait for user to click to some button, create dialog and await for user click
                    await new Promise((resolve) => {
                        // Create a dialog or some UI element to prompt the user
                        let dialog = document.createElement('div');
                        dialog.innerText = WJ.translate("components.ai_assistants.userActivationPrompt.js", apiName);

                        let button = document.createElement('button');
                        button.innerText = WJ.translate("components.ai_assistants.userActivationPrompt.button.js");
                        button.classList.add('btn');
                        button.classList.add('btn-primary');
                        dialog.appendChild(button);

                        const container = document.getElementById('toast-container-ai-content');
                        container.innerHTML = "";
                        container.appendChild(dialog);

                        button.addEventListener('click', () => {
                            instance.editorAiInstance.setCurrentStatus("components.ai_assistants.editor.loading.js");
                            resolve();
                        });
                    });
                }
            }

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

        let executionResult = new AiExecutionResult();

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
                        setFunction(content, true);
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
                    let translateToLanguage = translateOutputLanguage;

                    if ("autodetect" === translateOutputLanguage || true === translateOutputLanguage || "true" === translateOutputLanguage) {
                        //detect source language
                        translateToLanguage = await this._detectLanguage(text);
                    } else if ("userLng" === translateOutputLanguage) {
                        translateToLanguage = window.userLng;
                    }

                    //console.log("Translating from:", translateFromLanguage, "to:", translateToLanguage);

                    if (translateFromLanguage != translateToLanguage) {
                        this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.translatingFromTo.js", false, translateFromLanguage, translateToLanguage);

                        let config = {
                            sourceLanguage: translateFromLanguage,
                            targetLanguage: translateToLanguage
                        }
                        const translator = await this._apiInitialize("Translator", config, inputData, true);
                        //console.log("Mam translator: ", translator);
                        let translatedContent = await translator.translate(content);
                        //console.log("Translated content:", translatedContent, "content=", content);
                        if (setFunction != null) {
                            setFunction(translatedContent, true);
                        } else {
                            this.EDITOR.set(fieldName, translatedContent);
                        }
                    }
                }
            }

            //local browser uses zero tokens
            executionResult.totalTokens = 0;
            executionResult.success = true;
        } else {
            executionResult.statusKey = "components.ai_assistants.browser.api_not_available.js";
        }

        return executionResult;
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
                setFunction(content, false);
            } else {
                this.EDITOR.set(fieldName, content);
            }
            firstItem = false;
        }
        if (setFunction != null) {
            setFunction(content, true);
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