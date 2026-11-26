const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const lockfile = require("proper-lockfile");

const output = path.resolve(__dirname, "../../../../../../docs/feature-video/229-dopyt");
const origin = "http://iwcm.interway.sk";
const title = "Dopyt na službu";
const marker = "autotest-video-229-377-391";
const virtualPath = "/test-stavov/dopyt-na-sluzbu-video-229.html";
const groupId = 67;
const fields = [
    ["Typ zákazníka", "radiogroup", true], ["Meno", "meno", true], ["E-mail", "email", true],
    ["Názov firmy", "meno", false], ["Telefón", "telefon", false], ["Text dopytu", "poznamka", true]
];

// Send fixture requests only from the authenticated local administration.
async function prepareFixture(page) {
    assert.equal(new URL(page.url()).origin, origin);
    assert.equal(await page.locator('select[id="header.currentDomain"] option:checked').textContent(), "demo.webjetcms.sk");
    const api = (route, body) => page.evaluate(async ({ route, body, origin }) => {
        if (location.origin !== origin) throw Error("The fixture is outside its DEV origin");
        const res = await fetch("/admin/rest/" + route, {
            method: body ? "POST" : "GET", headers: {
                "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken, "X-Requested-With": "XMLHttpRequest"
            }, body: body ? JSON.stringify(body) : undefined
        });
        if (!res.ok) throw Error(route + " HTTP " + res.status);
        const json = await res.json();
        if (json.error || json.fieldErrors?.length) throw Error(route + " " + JSON.stringify([json.error, json.fieldErrors]));
        return json;
    }, { route, body, origin });
    const save = async (table, query, entity, action = "create") => {
        const id = action === "create" ? -1 : entity.id;
        const result = await api(table + "/editor" + query, { action, data: { [id]: entity } });
        return result.data?.[0];
    };
    const list = async (table, query) => (await api(table + "/all" + query)).content;
    fs.mkdirSync(output, { recursive: true });
    const release = await lockfile.lock(output);
    try {
        const group = await api("groups/" + groupId);
        assert.equal(group.domainName, "demo.webjetcms.sk");
        assert.equal(group.groupName, "Test stavov");
        const manifest = path.join(output, "fixture.json");
        let state = fs.existsSync(manifest) ? JSON.parse(fs.readFileSync(manifest, "utf8")) : {};
        function remember(extra) {
            state = { ...state, ...extra };
            fs.writeFileSync(manifest + ".tmp", JSON.stringify(state));
            fs.renameSync(manifest + ".tmp", manifest);
        }
        const ownedForm = async name => (await api("form-settings/search/findByColumns?searchFormName=" +
            encodeURIComponent("^" + name + "$"))).content;
        const settings = await ownedForm(state.formName || "dopyt-na-sluzbu");
        if (settings.length) {
            assert.equal(settings.length, 1);
            assert.equal(settings[0].fieldA, marker, "The existing form must belong to this production");
            if (!state.formName) remember({ formName: settings[0].formName });
        } else {
            const blank = await api("forms-list/-1");
            Object.assign(blank, { formName: title, formType: "multistep" });
            Object.assign(blank.formSettings, { fieldA: marker, subject: title, recipients: "" });
            const created = await save("forms-list", "", blank);
            remember({ formName: created.formName, formId: created.id });
            assert.equal((await ownedForm(state.formName))[0].fieldA, marker);
        }
        const q = "?formName=" + encodeURIComponent(state.formName);
        const steps = (await list("form-steps", q)).sort((a,b) => a.currentPosition - b.currentPosition);
        assert.ok(steps.length === 1 || steps.length === 2);
        if (!steps[0].header || !steps[0].header.includes("Údaje zákazníka")) {
            await save("form-steps", q, { ...steps[0], header: "<p>Údaje zákazníka</p>", nextStepBtnLabel: "Pokračovať" }, "edit");
        }
        if (steps.length === 1) {
            const blank = await api("form-steps/-1" + q);
            steps.push(await save("form-steps", q, { ...blank, sortPriority: 20, header: "<p>Váš dopyt</p>" }));
        }
        const items = {};
        for (const [index, step] of steps.entries()) {
            const iq = q + "&stepId=" + step.id;
            const rows = await list("form-items", iq);
            for (const [n, [label, fieldType, required]] of (index ? fields.slice(5) : fields.slice(0, 5)).entries()) {
                let item = rows.find(row => row.label === label);
                if (!item) {
                    const blank = await api("form-items/-1" + iq);
                    item = await save("form-items", iq, { ...blank, fieldType, label, sortPriority: 10 * (n + 1),
                        required, value: "", valueAsOptions: fieldType === "radiogroup" ? "Súkromná osoba:person|Firma:company" : "",
                        trimValue: false, tooltip: "", placeholder: "", compareInsensitive: false, showUnanswered: false });
                }
                assert.equal(item.stepId, step.id);
                items[label] = item;
            }
        }
        const dq = "?groupId=" + groupId;
        const include = "!INCLUDE(sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp, formName=" +
            state.formName + ", cssTemplate=/apps/form/mvc/styles/template-1.css)!";
        const content = `<style>@media(min-width:800px){
.wj-enquiry-demo .form-step{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:2rem}
.wj-enquiry-demo .form-step > :not(.form-group),.wj-enquiry-demo .form-group:has(input[type=radio]),.wj-enquiry-demo .form-group:has(textarea){grid-column:1/-1}
.wj-enquiry-demo .form-group:has(input[type=radio]) .form-check{display:inline-block;margin-right:1.25rem}
}</style><div class="wj-enquiry-demo">${include}</div>`;
        if (!state.docId) {
            const pages = (await api("web-pages/all" + dq + "&size=1000"));
            assert.equal(pages.content.length, pages.totalElements);
            const existing = pages.content.find(d => d.virtualPath === virtualPath);
            if (existing) {
                const saved = await api("web-pages/" + existing.docId);
                assert.equal(saved.title, title);
                assert.ok(saved.data.includes("wj-enquiry-demo") && saved.data.includes("formName=" + state.formName),
                    "The URL already belongs to a different page");
                remember({ docId: saved.docId });
            } else {
                const blank = await api("web-pages/-1" + dq);
                Object.assign(blank, { title, navbar: title, groupId, available: true, searchable: false, showInMenu: false,
                    generateUrlFromTitle: false, urlInheritGroup: false, editorVirtualPath: virtualPath, virtualPath, data: content });
                blank.editorFields.requestPublish = true;
                const doc = await save("web-pages", dq, blank);
                remember({ docId: doc.docId });
            }
        }
        const doc = await api("web-pages/" + state.docId);
        assert.equal(doc.virtualPath, virtualPath);
        assert.ok(doc.data.includes("formName=" + state.formName));
        assert.equal(doc.groupId, groupId);
        assert.ok(doc.data.includes("wj-enquiry-demo"));
        // Only these three clauses on the dedicated form may change during a retake.
        const clauses = [["Názov firmy", "VISIBILITY"], ["Názov firmy", "REQUIREMENT"], ["Telefón", "REQUIREMENT"]];
        async function baseline(count) {
            for (const [index, [label, type]] of clauses.entries()) {
                const item = items[label];
                const cq = q + "&stepId=" + item.stepId + "&itemId=" + item.id + "&conditionType=" + type;
                const rows = await list("form-items-conditions", cq);
                assert.ok(rows.length <= 1);
                if (rows.length) {
                    const row = rows[0];
                    assert.equal(row.formItemId, item.id);
                    assert.equal(row.itemFormId, items["Typ zákazníka"].itemFormId);
                    assert.equal(row.value, "company");
                    assert.equal(row.operator, "EQUALS");
                    if (index >= count) await save("form-items-conditions", cq, row, "remove");
                } else if (index < count) {
                    await save("form-items-conditions", cq, { itemFormId: items["Typ zákazníka"].itemFormId, value: "company",
                        operator: "EQUALS", joinOperatorType: "AND", caseInsensitive: false, sortPriority: 10 });
                }
            }
        }
        return { items, baseline, release, adminUrl: "/apps/form/admin/form-steps/" + q,
            publicUrl: "/showdoc.do?docid=" + state.docId, formName: state.formName };
    } catch (error) {
        await release();
        throw error;
    }
}
module.exports = { prepareFixture, output };
