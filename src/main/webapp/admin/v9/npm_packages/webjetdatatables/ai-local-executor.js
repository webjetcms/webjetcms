export class AiLocalExecutor {

    translator = null;
    summarizer = null;
    writer = null;
    rewriter = null;
    EDITOR = null;
    editorAiInstance = null;

    constructor(EDITOR, editorAiInstance) {
        this.EDITOR = EDITOR;
        this.editorAiInstance = editorAiInstance;
    }

    isAvailable() {
        if ('Translator' in self) {
            return true;
        }
        return false;
    }

    async execute(aiCol, inputData, setFunction = null) {

        let instructions = aiCol.instructions;
        let totalTokens = -1;

        //console.log("execute, inputData=", inputData);

        if (typeof instructions == "undefined" || instructions == null || instructions == "") {

        } else if (instructions.indexOf("translate")==0) {
            await this._translatorInitialize();
            if (this.translator) {
                totalTokens = await this.translate(this._getConfig(instructions), inputData, aiCol.to, setFunction);
            }
        } else if (instructions.indexOf("summarize")==0) {
            await this._summarizeInitialize();
            if (this.summarizer) {
                totalTokens = await this.summarize(this._getConfig(instructions), inputData, aiCol.to, setFunction);
            }
        } else if (instructions.indexOf("write")==0) {
            await this._writerInitialize();
            if (this.writer) {
                totalTokens = await this.write(this._getConfig(instructions), inputData, aiCol.to, setFunction);
            }
        } else if (instructions.indexOf("rewrite")==0) {
            await this._rewriterInitialize();
            if (this.rewriter) {
                totalTokens = await this.rewrite(this._getConfig(instructions), inputData, aiCol.to, setFunction);
            }
        }

        return totalTokens;
    }

    async _getConfig(instructions) {
        //after first : there is JSON config in string
        let i = instructions.indexOf(":");
        if (i !== -1) {
            let configString = instructions.substring(i + 1).trim();
            try {
                let config = JSON.parse(configString);
                console.log(config);
                return config;
            } catch (e) {
                console.error("Failed to parse config JSON:", e);
            }
        }
        return {};
    }

    async _translatorInitialize() {
        let instance = this;
        if ('Translator' in self) {
            this.translator = await Translator.create({
                sourceLanguage: 'sk',
                targetLanguage: 'en',
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        instance._setDownloadStatus(e);
                    });
                },
            });
        }
    }

    async translate(config, text, fieldName, setFunction = null) {
        await this._translatorInitialize();
        if (this.translator) {
            //console.log("Translating text:", text, "translator=", this.translator);

            const stream = this.translator.translateStreaming(text, config);

            await this._setField(fieldName, stream, setFunction);
            return 0;
        }
        return -1;
    }

    async _summarizeInitialize() {
        let instance = this;
        if ('Summarizer' in self) {
            this.summarizer = await Summarizer.create({
                type: "tldr",
                format: "plain-text",
                length: "short",
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        instance._setDownloadStatus(e);
                    });
                },
            });
        }
    }

    async summarize(config, text, fieldName, setFunction = null) {
        await this._summarizeInitialize();
        if (this.summarizer) {
            //console.log("Summarizing text:", text, "summarizer=", this.summarizer);

            const stream = this.summarizer.summarizeStreaming(text, config);

            await this._setField(fieldName, stream, setFunction);
            return 0;
        }
        return -1;
    }

    async _writerInitialize() {
        let instance = this;
        if ('Writer' in self) {
            this.writer = await Writer.create({
                tone: "neutral",
                format: "plain-text",
                length: "medium",
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        instance._setDownloadStatus(e);
                    });
                },
            });
        }
    }

    async write(config, text, fieldName, setFunction = null) {
        await this._writerInitialize();
        if (this.writer) {
            //console.log("Writing text:", text, "writer=", this.writer);

            const stream = this.writer.writeStreaming(text, config);

            await this._setField(fieldName, stream, setFunction);
            return 0;
        }
        return -1;
    }

    async _rewriterInitialize() {
        let instance = this;
        if ('Rewriter' in self) {
            this.rewriter = await Rewriter.create({
                tone: "neutral",
                format: "plain-text",
                length: "medium",
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        instance._setDownloadStatus(e);
                    });
                },
            });
        }
    }

    async rewrite(config, text, fieldName, setFunction = null) {
        await this._rewriterInitialize();
        if (this.rewriter) {
            const stream = this.rewriter.rewriteStreaming(text, config);

            await this._setField(fieldName, stream, setFunction);
            return 0;
        }
        return -1;
    }

    async _setField(fieldName, stream, setFunction = null) {
        let firstItem = true;
        let content = "";
        //console.log("Setting field:", fieldName);
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
    }

    _setDownloadStatus(progress) {
        console.log("Progress: ", progress);
        let percent = progress.loaded * 100;
        //skip this values, they are not useful
        if (percent == 0 || percent == 100) return;

        if (this.editorAiInstance) {
            this.editorAiInstance._setDownloadStatus(percent);
        } else {
            console.log("Download progress:", percent+"%");
        }
    }
}