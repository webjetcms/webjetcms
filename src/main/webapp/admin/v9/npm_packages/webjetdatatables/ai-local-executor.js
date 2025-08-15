export class AiLocalExecutor {

    translator = null;
    EDITOR = null;

    constructor(EDITOR) {
        this.EDITOR = EDITOR;
    }

    isAvailable() {
        if ('Translator' in self) {
            return true;
        }
        return false;
    }

    async execute(aiCol, inputData, setFunction = null) {

        let instructions = aiCol.instructions;

        //console.log("execute, inputData=", inputData);

        if (typeof instructions == "undefined" || instructions == null || instructions == "") {

        } else if (instructions.indexOf("translate")==0) {
            await this._translatorInitialize();
            if (this.translator) {
                await this.translate(inputData, aiCol.to, setFunction);
            }
        } else if (instructions.indexOf("summarize")==0) {
            await this._summarizeInitialize();
            if (this.summarizer) {
                await this.summarize(inputData, aiCol.to, setFunction);
            }
        }
    }

    async _translatorInitialize() {
        if ('Translator' in self) {
            this.translator = await Translator.create({
                sourceLanguage: 'sk',
                targetLanguage: 'en',
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        console.log(`Downloaded ${e.loaded * 100}%`);
                    });
                },
            });
        }
    }

    async translate(text, fieldName, setFunction = null) {
        await this._translatorInitialize();
        if (this.translator) {
            //console.log("Translating text:", text, "translator=", this.translator);
            this.EDITOR.set(fieldName, WJ.translate("components.ai_assistants.editor.loading.js"));

            const stream = this.translator.translateStreaming(text);

            await this._setField(fieldName, stream, setFunction);
        }
    }

    async _summarizeInitialize() {
        if ('Summarizer' in self) {
            this.summarizer = await Summarizer.create({
                type: "tldr",
                format: "plain-text",
                length: "short",
                monitor(m) {
                    m.addEventListener('downloadprogress', (e) => {
                        console.log(`Downloaded ${e.loaded * 100}%`);
                    });
                },
            });
        }
    }

    async summarize(text, fieldName, setFunction = null) {
        await this._summarizeInitialize();
        if (this.summarizer) {
            //console.log("Summarizing text:", text, "summarizer=", this.summarizer);

            this.EDITOR.set(fieldName, WJ.translate("components.ai_assistants.editor.loading.js"));

            const stream = this.summarizer.summarizeStreaming(text, {
                context: "Použi slovenský jazyk"
            });

            await this._setField(fieldName, stream, setFunction);
        }
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
}