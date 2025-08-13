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

    async execute(aiCol) {

        let instructions = aiCol.instructions;

        let from = aiCol.from;
        if (from == null || from == "")  from = aiCol.to; //if from is not set, use to as from

        let text = this.EDITOR.get(from);
        let toElementId = "DTE_Field_"+aiCol.to;


        if (typeof instructions == "undefined" || instructions == null || instructions == "") {

        } else if (instructions.indexOf("translate")==0) {
            await this._translatorInitialize();
            if (this.translator) {
                await this.translate(text, toElementId);
            }
        } else if (instructions.indexOf("summarize")==0) {
            await this._summarizeInitialize();
            if (this.summarizer) {
                await this.summarize(text, toElementId);
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

    async translate(text, elementId) {
        await this._translatorInitialize();
        if (this.translator) {
            console.log("Translating text:", text, "translator=", this.translator);
            let element = $("#" + elementId);
            element.val(WJ.translate("components.ai_assistants.editor.loading.js"));

            const stream = this.translator.translateStreaming(text);
            element.val(''); // Clear the element before appending new text
            for await (const chunk of stream) {
                console.log(chunk);
                element.val(element.val() + chunk);
            }
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

    async summarize(text, elementId) {
        await this._summarizeInitialize();
        if (this.summarizer) {
            console.log("Summarizing text:", text, "summarizer=", this.summarizer);
            let element = $("#" + elementId);

            element.val(WJ.translate("components.ai_assistants.editor.loading.js"));

            const stream = this.summarizer.summarizeStreaming(text, {
                context: "Použi slovenský jazyk"
            });
            let firstItem = true;
            for await (const chunk of stream) {
                if (firstItem) {
                    element.val(chunk);
                } else {
                    element.val(element.val() + chunk);
                }
                firstItem = false;
                console.log(chunk);
            }
        }
    }
}