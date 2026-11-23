import { AiExecutionResult } from './ai-execution-result';

const browserMethods = {
    Writer: "write",
    Rewriter: "rewrite",
    Translator: "translate",
    LanguageDetector: "detectLanguage",
    Summarizer: "summarize",
    LanguageModel: "prompt"
};

/** Runs a browser AI model for an editor action or chat turn. */
export class AiBrowserExecutor {

    lastApiInstance = null;
    lastConfigKey = null;
    languageDetector = null;
    controller = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    isAvailable(aiCol = null) {
        const name = this.getApiName(aiCol);
        return Object.hasOwn(browserMethods, name) && name in self;
    }

    getApiName(aiCol = null) {
        if (!aiCol) return "Translator";
        const instructions = aiCol.instructions || "";
        const separator = instructions.indexOf(":");
        return separator < 0 ? null : instructions.slice(0, separator).trim();
    }

    /** Reuses the configured model across PageBuilder blocks when requested. */
    async execute(aiCol, inputData, reuseApiInstance = false, setFunction = null) {
        const result = new AiExecutionResult();
        if (!this.isAvailable(aiCol)) {
            result.statusKey = "components.ai_assistants.browser.api_not_available.js";
            result.statusKeyParams = [this.getApiName(aiCol)];
            return result;
        }

        this.controller?.abort();
        this.controller = new AbortController();
        const { signal } = this.controller;
        const apiName = this.getApiName(aiCol);
        const text = inputData.userPrompt ?? inputData.inputValue;
        const update = setFunction ?? (value => this.EDITOR.set(aiCol.to, value));
        try {
            const config = this._getConfig(aiCol.instructions);
            const key = JSON.stringify([apiName, config]);
            if (!reuseApiInstance || !this.lastApiInstance || this.lastConfigKey !== key) {
                this.lastApiInstance?.destroy();
                this.lastApiInstance = null;
                const model = await this._createModel(apiName, config, text, signal);
                if (signal.aborted) {
                    model.destroy();
                    signal.throwIfAborted();
                }
                this.lastApiInstance = model;
                this.lastConfigKey = key;
            }
            signal.throwIfAborted();
            const model = this.lastApiInstance;
            const method = browserMethods[apiName];
            let content;
            if (aiCol.useStreaming) {
                content = "";
                const stream = model[method + "Streaming"](text, config);
                for await (const chunk of stream) {
                    signal.throwIfAborted();
                    content += chunk;
                    update(content, false);
                }
                signal.throwIfAborted();
                if (setFunction) update(content, true);
            } else {
                content = await model[method](text, config);
                signal.throwIfAborted();
                if (content != null) update(content, true);
            }
            if (content?.length && config.translateOutputLanguage) {
                await this._translateOutput(content, text, config.translateOutputLanguage, update, signal);
            }
            result.success = true;
        } catch (error) {
            if (signal.aborted || error.message === "signal is aborted without reason") result.stopped = true;
            else result.errorText = error.message;
        }
        return result;
    }

    destroy() {
        this.controller?.abort();
        this.lastApiInstance?.destroy();
        this.lastApiInstance = null;
        this.languageDetector?.destroy();
        this.languageDetector = null;
    }

    _getConfig(instructions) {
        const json = instructions.slice(instructions.indexOf(":") + 1).trim();
        try {
            return JSON.parse(json);
        } catch (error) {
            console.error("Failed to parse browser AI config:", error, json);
            return {};
        }
    }

    async _createModel(apiName, config, text, signal, allowInactive = false) {
        const api = self[apiName];
        config.monitor = monitor => monitor.addEventListener('downloadprogress', progress => {
            if (!signal.aborted) this._setDownloadStatus(progress);
        });
        if (apiName === "Translator") {
            if (config.sourceLanguage === "autodetect") config.sourceLanguage = await this._detectLanguage(text, signal);
            else if (config.sourceLanguage === "userLng") config.sourceLanguage = window.userLng;
            if (config.targetLanguage === "userLng") config.targetLanguage = window.userLng;
        }
        signal.throwIfAborted();
        let availability = await api.availability(config);
        signal.throwIfAborted();
        if (apiName === "Translator" && !navigator.userActivation.isActive && availability !== "available") {
            await this._confirm(apiName, false, signal);
            availability = await api.availability(config);
            signal.throwIfAborted();
        }
        if (availability === "unavailable") {
            throw new Error("!" + WJ.translate("components.ai_assistants.browser.model_unavailable.js", apiName));
        }
        if (availability === "downloadable") await this._confirm(apiName, true, signal);
        if (availability !== "available" && !navigator.userActivation.isActive && !allowInactive) {
            throw new Error(WJ.translate("components.ai_assistants.browser.userInteractionError.js"));
        }
        return api.create(config);
    }

    async _confirm(apiName, download, signal) {
        const message = WJ.translate(download ? "components.ai_assistants.browser.download_model.confirm.js"
            : "components.ai_assistants.userActivationPrompt.js", apiName);
        const buttonLabel = WJ.translate(download ? "components.ai_assistants.browser.download_model.download.js"
            : "components.ai_assistants.userActivationPrompt.button.js");
        const confirmed = this.editorAiInstance?.requestBrowserConfirmation
            ? await this.editorAiInstance.requestBrowserConfirmation(message, buttonLabel)
            : await this._confirmInEditor(message, buttonLabel, download, signal);
        signal.throwIfAborted();
        if (confirmed !== true) throw new Error("!" + WJ.translate("components.ai_assistants.browser.download_model.canceled.js"));
    }

    _confirmInEditor(message, buttonLabel, download, signal) {
        const $container = $("#toast-container-ai-content").empty();
        const $message = $("<div>").appendTo($container);
        if (download) $message.html(WJ.parseMarkdown(message, { link: true }));
        else $message.text(message);
        const $actions = download ? $('<div class="mt-2 text-end">').appendTo($container) : $message;
        return new Promise(resolve => {
            const abort = () => resolve(false);
            const finish = accepted => {
                signal.removeEventListener("abort", abort);
                resolve(accepted);
            };
            signal.addEventListener("abort", abort, { once: true });
            if (download) {
                $('<button type="button" class="btn btn-outline-secondary me-2">').text(WJ.translate("button.cancel"))
                    .appendTo($actions).one("click", () => finish(false));
            }
            $('<button type="button" class="btn btn-primary">').text(buttonLabel).appendTo($actions).one("click", () => {
                if (signal.aborted) return;
                if (download) this.editorAiInstance.setDownloadStatus("");
                else this.editorAiInstance.setCurrentStatus("components.ai_assistants.editor.loading.js");
                finish(true);
            });
        });
    }

    async _translateOutput(content, text, target, update, signal) {
        this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.translating.js", false);
        const sourceLanguage = await this._detectLanguage(content, signal);
        let targetLanguage = target;
        if (["autodetect", true, "true"].includes(target)) targetLanguage = await this._detectLanguage(text, signal);
        else if (target === "userLng") targetLanguage = window.userLng;
        signal.throwIfAborted();
        if (sourceLanguage === targetLanguage) return;
        this.editorAiInstance.setCurrentStatus("components.ai_assistants.browser.translatingFromTo.js", false, sourceLanguage, targetLanguage);
        const translator = await this._createModel("Translator", { sourceLanguage, targetLanguage }, text, signal, true);
        try {
            signal.throwIfAborted();
            const translated = await translator.translate(content);
            signal.throwIfAborted();
            update(translated, true);
        } finally {
            translator.destroy();
        }
    }

    async _detectLanguage(text, signal) {
        signal.throwIfAborted();
        if (!this.languageDetector) {
            const detector = await LanguageDetector.create({});
            if (signal.aborted) {
                detector.destroy();
                signal.throwIfAborted();
            }
            this.languageDetector = detector;
        }
        const results = await this.languageDetector.detect(text);
        signal.throwIfAborted();
        console.info("Detected languages:", results);
        return results[0]?.detectedLanguage ?? null;
    }

    _setDownloadStatus(progress) {
        const percent = Math.round(progress.loaded * 100);
        if (percent === 0) return;
        if (this.editorAiInstance) this.editorAiInstance.setDownloadStatus(percent);
        else console.log("Download progress:", percent + "%");
    }
}
