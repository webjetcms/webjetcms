const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { fixtureFor, editorUrl, options, tooltip, title, className, output } = require("./fixture.cjs");

function recording(I, DTE) {
    const { DT } = inject();
    let page, fixture, evidence;
    const box = () => "#datatableInit_modal .DTE_Field_Name_" + fixture.field;
    const demo = {
        get f() { return fixture; },
        box, className, tooltip,
        async setup() {
            await I.waitForVisible("button.js-profile-toggler", 20);
            await I.usePlaywrightTo("prepare the saved service page in the demo domain", async (browser) => {
                page = browser.page;
                fixture = await fixtureFor(page);
                await fixture.restore();
                if (page.video()) {
                    const work = path.join(output, ".work");
                    fs.mkdirSync(work, { recursive: true });
                    evidence = fs.mkdtempSync(path.join(work, (process.env.VIDEO_SHOT || "full") + "-"));
                    console.log("Editing cues:", evidence);
                }
            });
        },
        async close() {
            if (fixture) await fixture.restore();
        },
        async cue(id) {
            if (!evidence) return;
            const time = Date.now();
            await page.screenshot({ path: path.join(evidence, id + ".jpg"), type: "jpeg", quality: 83 });
            fs.appendFileSync(path.join(evidence, "cues.ndjson"), JSON.stringify({ id, time }) + "\n");
        },
        async frameThumbnail() {
            // Crop the real, painted tab and radio choices while the original document is still open.
            const crop = await page.locator(box()).evaluate(field => {
                const range = document.createRange();
                range.selectNodeContents(field.querySelector("label.col-form-label").firstChild);
                const name = range.getBoundingClientRect();
                const choices = [...field.querySelectorAll("label.form-check-label")].map(label => label.getBoundingClientRect());
                const x = Math.floor(name.left - 32);
                const y = Math.floor(Math.min(name.top, choices[0].top) - 12);
                return { x, y, width: Math.ceil(Math.max(...choices.map(rect => rect.right)) - x + 36),
                    height: Math.ceil(choices.at(-1).bottom - y + 14), viewport: window.innerWidth };
            });
            const pixels = await page.screenshot({ animations: "disabled" });
            // Chromium clip coordinates differ from CSS pixels under native recording zoom.
            const scale = pixels.readUInt32BE(16) / crop.viewport;
            const size = { width: Math.round(crop.width * scale), height: Math.round(crop.height * scale) };
            await page.goto("about:blank");
            await page.setViewportSize(size);
            await page.setContent("<style>*{margin:0}canvas{display:block;width:100vw;height:100vh}</style><canvas></canvas>");
            await page.evaluate(async ({ crop, size, scale, image }) => {
                const source = new Image();
                source.src = image;
                await source.decode();
                const canvas = document.querySelector("canvas");
                Object.assign(canvas, size);
                canvas.getContext("2d").drawImage(source, crop.x * scale, crop.y * scale, size.width, size.height,
                    0, 0, size.width, size.height);
            }, { crop, size, scale, image: "data:image/png;base64," + pixels.toString("base64") });
        },
        async listPage() {
            await I.amOnPage(editorUrl + "?groupid=67");
            await DT.waitForLoader("datatableInit");
            await DT.filterEquals("title", title, "#datatableInit_wrapper");
            await I.waitForText(title, 15, "#datatableInit tbody");
        },
        async openFromList() {
            await demo.cue("service-list");
            await I.videoClick(locate("#datatableInit tbody tr").withText(title).find("td.dt-select-td"));
            await I.videoClick("#datatableInit_wrapper button.buttons-edit");
            await DTE.waitForEditor("datatableInit");
            await demo.cue("editor-open");
        },
        async reopen() {
            await I.amOnPage(editorUrl + "?docid=" + fixture.docId);
            await DTE.waitForEditor("datatableInit");
            await I.waitForText(title, 10, "#datatableInit_modal .modal-title");
        },
        async fields() {
            await I.videoClick("#pills-dt-datatableInit-fields-tab");
            await I.waitForText("Typ služby", 10, box());
        },
        async fill(selector, value) {
            await I.videoClick(selector);
            await I.fillField(selector, value);
        },
        async pick(toggle, value) {
            await I.videoClick(toggle);
            const option = locate("div.dropdown-menu.show a.dropdown-item").withText(value).first();
            await I.waitForVisible(option, 10);
            await I.videoClick(option);
        },
        async save(table) {
            await I.videoClick("#" + table + "_modal .DTE_Footer button.btn-primary");
            await I.waitForInvisible("#" + table + "_modal", 20);
            await DT.waitForLoader(table);
        },
        async checkOptions(count, selected) {
            const radios = page.locator(box() + " input[type=radio]");
            const actual = await radios.evaluateAll(inputs => inputs.map(i => ({
                pair: [i.labels[0].textContent.trim(), i.value], required: i.required,
                checked: i.checked, aria: i.getAttribute("aria-required")
            })));
            assert.deepEqual(actual.map(r => r.pair), options.slice(0, count));
            assert.ok(actual.every(r => r.required && r.aria === "true"));
            assert.deepEqual(actual.filter(r => r.checked).map(r => r.pair[1]), selected ? [selected] : []);
        },
        async checkValue() {
            assert.equal((await fixture.readPage())[fixture.field], "skolenie");
        }
    };
    return demo;
}

module.exports = { recording };
