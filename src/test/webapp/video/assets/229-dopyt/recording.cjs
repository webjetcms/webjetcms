const assert = require("node:assert/strict");
const path = require("node:path");
const fs = require("node:fs");
const { prepareFixture, output } = require("./fixture.cjs");
const guide = "https://docs.webjetcms.sk/latest/sk/redactor/apps/multistep-form/";
const parent = "formItemsDataTable";

function recording(I, DT, DTE) {
    let page, fixture, evidence, adminCookies;
    async function resumeAdmin() {
        if (adminCookies) {
            await page.context().clearCookies();
            await page.context().addCookies(adminCookies);
            adminCookies = undefined;
        }
        if (!page.url().includes("/apps/form/admin/")) {
            await I.amOnPage(fixture.adminUrl);
            await I.waitForText("Typ zákazníka", 20, "#formItemsDataTable");
        }
    }
    const demo = {
        async start() {
            await I.waitForVisible("button.js-profile-toggler", 20);
            await I.usePlaywrightTo("prepare the dedicated service enquiry", async (browser) => {
                page = browser.page;
                fixture = await prepareFixture(page);
                if (page.video()) {
                    const work = path.join(output, ".work");
                    fs.mkdirSync(work, { recursive: true });
                    evidence = fs.mkdtempSync(path.join(work, (process.env.VIDEO_SHOT || "full") + "-"));
                }
            });
            await I.wjSetDefaultWindowSize();
        },
        async finish() {
            if (!fixture) return;
            try { await resumeAdmin(); await fixture.baseline(3); } finally { await fixture.release(); }
        },
        async cue(id) {
            if (!evidence) return;
            const time = Date.now();
            await page.screenshot({ path: path.join(evidence, id + ".jpg"), type: "jpeg", quality: 85 });
            fs.appendFileSync(path.join(evidence, "cues.ndjson"), JSON.stringify({ id, time }) + "\n");
        },
        async admin(count) {
            await resumeAdmin();
            await fixture.baseline(count);
            if (new URL(page.url()).pathname !== "/apps/form/admin/form-steps/") await I.amOnPage(fixture.adminUrl);
            await I.waitForText("Typ zákazníka", 20, "#formItemsDataTable");
            await DT.waitForLoader(parent);
        },
        async open(label) {
            const row = "#formItemsDataTable tbody tr[id='" + fixture.items[label].id + "']";
            if (!await page.locator(row + ".selected").count()) await I.videoClick(row + " td:last-child");
            await I.videoClick("#formItemsDataTable_wrapper button.buttons-edit");
            await DTE.waitForEditor(parent);
            await I.seeInField("#DTE_Field_itemFormId", fixture.items[label].itemFormId);
        },
        async tab(name) {
            const sel = "#pills-dt-formItemsDataTable-" + name + "-tab";
            await I.videoClick(sel);
            await I.waitForVisible(sel + '[aria-selected="true"]', 10);
        },
        async save(table) {
            await I.videoClick("#" + table + "_modal .DTE_Footer button.btn-primary");
            await I.waitForInvisible("#" + table + "_modal", 15);
            await DT.waitForLoader(table);
        },
        async pick(modal, key, value, cue) {
            const select = modal + " select#DTE_Field_" + key;
            const option = page.locator(select).locator("option").filter({ hasText: value }).first();
            const text = (await option.textContent()).trim();
            const toggle = modal + " .DTE_Field_Name_" + key + " button.dropdown-toggle";
            await I.videoClick(toggle);
            const entry = locate("div.dropdown-menu.show a.dropdown-item").withChild(locate("span.text").withTextEquals(text));
            await I.waitForVisible(entry, 10);
            await demo.cue(cue + "-menu");
            await I.videoClick(entry);
            assert.equal((await page.locator(select + " option:checked").textContent()).trim(), text);
            await demo.cue(cue);
        },
        async rule(kind, prefix) {
            const tab = kind + "Conditions";
            const table = "datatableFieldDTE_Field_" + tab;
            const modal = "#" + table + "_modal";
            await demo.tab(tab);
            await DT.waitForLoader(table);
            await demo.cue(prefix + "-tab");
            await I.videoClick("#" + table + "_wrapper button.buttons-create");
            await DTE.waitForEditor(table);
            await demo.cue(prefix + "-add");
            await demo.pick(modal, "itemFormId", "Typ zákazníka", prefix + "-source");
            await demo.pick(modal, "operator", "rovná sa", prefix + "-equals");
            await demo.pick(modal, "value", "Firma", prefix + "-company");
            assert.equal(await page.locator(modal + " select#DTE_Field_value").inputValue(), "company");
            await demo.save(table);
            await I.waitForText("Typ zákazníka", 10, "#" + table + " tbody");
            await demo.cue(prefix + "-row");
            await I.wait(1.2);
            await demo.save(parent);
            await demo.cue(prefix + "-saved");
        },
        input(label) { return "#f1-" + fixture.items[label].itemFormId; },
        async publicPage() {
            await fixture.baseline(3);
            adminCookies = await page.context().cookies();
            await page.context().clearCookies();
            // Use the supported reduced-motion path: during an animated reveal the current
            // CMS can miss the name field’s required marker even though the server requires it.
            await page.emulateMedia({ reducedMotion: "reduce" });
            await I.amOnPage(fixture.publicUrl);
            await I.waitForVisible('.multistep-form input[value="person"]', 20);
            await page.locator(".multistep-form").evaluate(el => window.scrollTo({ top: scrollY + el.getBoundingClientRect().top - 86, behavior: "instant" }));
        },
        async customer(value) {
            const input = '.multistep-form input[type="radio"][value="' + value + '"]';
            const id = await page.locator(input).getAttribute("id");
            await I.videoClick('label[for="' + id + '"]');
            await I.seeCheckboxIsChecked(input);
            const company = demo.input("Názov firmy");
            const phone = demo.input("Telefón");
            await I.waitForVisible(phone, 10);
            if (value === "person") {
                await I.waitForInvisible(company, 10);
                assert.equal(await page.locator(phone).getAttribute("data-requirement-required"), null);
            } else {
                await I.waitForVisible(company, 10);
                for (const sel of [company, phone]) {
                    assert.equal(await page.locator(sel).getAttribute("data-requirement-required"), "true");
                    await I.seeElement('label[for="' + sel.slice(1) + '"] .requirement-mark');
                }
            }
        },
        async fill(label, value) {
            const field = demo.input(label);
            await I.videoClick(field);
            await page.locator(field).pressSequentially(value, { delay: 45 });
        },
        async errors() {
            for (const label of ["Názov firmy", "Telefón"]) {
                const field = demo.input(label);
                await I.waitForText("povinné pole", 10, ".cs-error-" + field.slice(1));
                await I.seeElement(field + '[aria-invalid="true"]');
            }
            assert.equal(await page.locator(".multistep-form .cs-error li:visible").count(), 2);
        },
        async relations() {
            await demo.tab("relations");
            const names = id => page.locator("#formItem" + id + "Usage .form-item-relations-name").allTextContents();
            await I.waitForText("Telefón", 10, "#formItemRequirementUsage");
            assert.deepEqual((await names("Visibility")).map(s => s.trim()), ["Názov firmy"]);
            assert.deepEqual((await names("Requirement")).map(s => s.trim()).sort(), ["Názov firmy", "Telefón"]);
        },
        async phoneRule() {
            await I.videoClick(locate("#formItemRequirementUsage button").withTextEquals("Telefón"));
            await I.waitForFunction(([id]) => document.querySelector("#formItemsDataTable_modal #DTE_Field_itemFormId")?.value === id, [fixture.items["Telefón"].itemFormId], 15);
            await demo.cue("linked-phone");
            await demo.tab("requirementConditions");
            const id = "datatableFieldDTE_Field_requirementConditions";
            await I.waitForText("Typ zákazníka", 10, "#" + id + " tbody");
            await demo.cue("linked-row");
            await I.videoClick("#" + id + " tbody td.sorting_1");
            await I.videoClick("#" + id + "_wrapper button.buttons-edit");
            await DTE.waitForEditor(id);
            const select = "#" + id + "_modal select#DTE_Field_value";
            assert.equal(await page.locator(select).inputValue(), "company");
            assert.equal(await page.locator(select + " option:checked").textContent(), "Firma");
        },
        async docs(section) {
            const heading = page.locator("article h3").filter({ hasText: section }).first();
            if (new URL(page.url()).origin !== new URL(guide).origin) await I.amOnPage(guide);
            await heading.waitFor({ timeout: 20000 });
            await heading.evaluate(async el => {
                await document.fonts.ready;
                // Keep the original Docsify section, images and navigation in a short reading
                // view. Chromium can otherwise record blank tiles deep inside this long article.
                let preceding = true;
                for (const child of el.parentElement.children) {
                    if (child === el) preceding = false;
                    if (preceding) child.style.display = "none";
                    else child.style.removeProperty("display");
                }
                el.parentElement.style.paddingTop = "78px";
                window.scrollTo({ top: 0, behavior: "instant" });
                const picture = el.nextElementSibling?.parentElement.querySelector('img[src*="tab-relations"]');
                if (picture) await picture.decode();
                await new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done)));
            });
            const box = await heading.boundingBox();
            assert.ok(box && box.y >= 0 && box.y < 270, "Documentation heading outside the reading pane: " + JSON.stringify(box));
        }
    };
    return demo;
}
module.exports = { recording };
