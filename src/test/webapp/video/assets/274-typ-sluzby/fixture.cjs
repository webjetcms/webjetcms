// Shared, page-scoped baseline for the complete recording, retakes and thumbnail.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const output = path.resolve(__dirname, "../../../../../../docs/feature-video/274-typ-sluzby");
const manifest = path.join(output, "fixture.json");
const origin = "http://iwcm.interway.sk";
const groupId = 67;
const domain = "demo.webjetcms.sk";
const title = "Školenie pre redaktorov";
const virtualPath = "/test-stavov/skolenie-pre-redaktorov-video-274.html";
const className = "sk.iway.iwcm.doc.DocDetails";
const tooltip = "Vyberte službu, ktorú táto stránka predstavuje.";
const docs = "/admin/rest/web-pages";
const types = "/admin/rest/enumeration/enumeration-type";
const items = "/admin/rest/enumeration/enumeration-data";
const rules = "/admin/rest/custom-fields";
const options = [
    ["Konzultácia", "konzultacia"], ["Školenie", "skolenie"],
    ["Technická podpora", "podpora"], ["Servisný zásah", "servis"]
];
const editorUrl = "/admin/v9/webpages/web-pages-list/";

async function fixtureFor(page) {
    assert.equal(new URL(page.url()).origin, origin);
    const api = (url, payload) => page.evaluate(async ({ url, payload, origin }) => {
        if (location.origin !== origin) throw Error("Wrong origin for the demo fixture");
        const res = await fetch(url, { method: payload ? "POST" : "GET",
            headers: { "X-CSRF-Token": window.csrfToken, "X-Requested-With": "XMLHttpRequest", "Content-Type": "application/json" },
            body: payload ? JSON.stringify(payload) : undefined });
        if (!res.ok) throw Error(url + " HTTP " + res.status);
        const body = await res.json();
        if (body.error || body.fieldErrors?.length) throw Error(JSON.stringify([body.error, body.fieldErrors]));
        return body;
    }, { url, payload, origin });
    const list = async (endpoint, query = "") => {
        const result = await api(endpoint + "/all?size=1000" + query);
        assert.equal(result.content.length, result.totalElements, "Expected an untruncated fixture lookup");
        return result.content;
    };
    const write = async (endpoint, action, record, query = "") => {
        const id = action === "create" ? 0 : record.id;
        assert.ok(action === "create" || id > 0);
        return (await api(endpoint + "/editor" + query, { action, data: { [id]: record } })).data?.[0];
    };
    fs.mkdirSync(output, { recursive: true });
    const state = fs.existsSync(manifest) ? JSON.parse(fs.readFileSync(manifest, "utf8")) : {};
    function remember(fields) {
        Object.assign(state, fields);
        fs.writeFileSync(manifest + ".tmp", JSON.stringify(state, null, 2));
        fs.renameSync(manifest + ".tmp", manifest);
    }

    assert.equal(await page.locator('select[id="header.currentDomain"] option:checked').textContent(), domain);
    const parent = await api("/admin/rest/groups/" + groupId);
    assert.equal(parent.domainName, domain);
    assert.equal(parent.groupName, "Test stavov");
    const settings = await list(rules); // Also verifies dataTablesConfig in this domain.
    const typeList = await list(types);
    let type = typeList.find(row => !row.hidden && row.typeName === "Typy služieb");
    if (!type) {
        const blank = await api(types + "/-1");
        type = await write(types, "create", { ...blank, typeName: "Typy služieb", string1Name: "Názov služby", string2Name: "Kód služby" });
        remember({ typeId: type.id });
    }
    assert.equal(type.id, state.typeId, "An existing enumeration with this name belongs to another fixture.");
    assert.equal(type.string1Name, "Názov služby");
    assert.equal(type.string2Name, "Kód služby");
    const query = "?enumerationTypeId=" + type.id;
    const rows = () => list(items, "&" + query.slice(1));
    let entries = await rows();
    for (const [name, code] of options.slice(0, 3)) {
        if (!entries.some(row => row.fieldB === code)) {
            const fresh = await api(items + "/-1" + query);
            await write(items, "create", { ...fresh, typeId: type.id, fieldA: name, fieldB: code }, query);
        }
    }
    entries = await rows();
    for (const row of entries) assert.ok(options.some(([name, code]) => name === row.fieldA && code === row.fieldB));
    for (const [name] of options.slice(0, 3)) assert.equal(entries.filter(row => row.fieldA === name).length, 1);

    let doc = (await list(docs, "&groupId=" + groupId)).find(row => row.virtualPath === virtualPath || row.title === title);
    if (!doc) {
        const fresh = await api(docs + "/-1?groupId=" + groupId);
        Object.assign(fresh, { title, navbar: title, groupId, available: true, searchable: false, showInMenu: false,
            data: "<h1>Školenie pre redaktorov</h1><p>Na praktickom školení si vyskúšate tvorbu a aktualizáciu webových stránok vo WebJET CMS.</p>",
            generateUrlFromTitle: false, urlInheritGroup: false, editorVirtualPath: virtualPath, virtualPath });
        fresh.editorFields.requestPublish = true;
        doc = await write(docs, "create", fresh, "?groupId=" + groupId);
        remember({ docId: doc.docId });
    }
    assert.equal(doc.docId, state.docId, "An existing service page belongs to someone else.");
    const readPage = async () => {
        const saved = await api(docs + "/" + state.docId);
        assert.equal(saved.groupId, groupId);
        assert.equal(saved.virtualPath, virtualPath);
        return saved;
    };
    doc = await readPage();
    const relevant = settings.filter(r => r.className === className && (!r.entityId || r.entityId === doc.docId) &&
        (!r.bonusClassName || (r.bonusClassName === "sk.iway.iwcm.doc.TemplateDetails" && r.bonusEntityId === doc.tempId)));
    if (!state.letter) {
        const free = doc.editorFields.fieldsDefinition.find(f => !f.disabled && !f.required && f.type === "text" &&
            f.label === "Pole " + f.key.toUpperCase() && !doc["field" + f.key.toUpperCase()] &&
            !relevant.some(r => r.alphabet === f.key.toUpperCase()));
        assert.ok(free, "No unused plain page field without a template override.");
        remember({ letter: free.key.toUpperCase() });
    }
    const letter = state.letter;
    const field = "field" + letter;
    const definition = { className, alphabet: letter, entityId: doc.docId, label: "Typ služby", type: "radio",
        tooltip, required: true, optionsSource: "enumeration", enumeration: "enumeration-options|" + type.id + "|string1|string2" };
    function owned(rule) {
        return rule.className === className && rule.alphabet === letter && rule.entityId === doc.docId &&
            !rule.bonusClassName && !rule.bonusEntityId && rule.label === definition.label && rule.tooltip === tooltip;
    }
    assert.ok(relevant.filter(r => r.alphabet === letter).every(owned), "The demo field has a competing configuration.");
    const rule = async () => (await list(rules)).find(owned);
    async function ensureRule() {
        if (!await rule()) await write(rules, "create", { ...await api(rules + "/-1"), ...definition });
        const actual = await api(rules + "/" + (await rule()).id);
        for (const [key,value] of Object.entries(definition)) assert.equal(actual[key], value, key);
    }
    async function value(code) {
        const saved = await readPage();
        assert.ok(["", null, "skolenie"].includes(saved[field]), "Unexpected value on the demo page");
        if ((saved[field] || "") === code) return;
        saved[field] = code;
        saved.editorFields.requestPublish = true;
        await write(docs, "edit", { ...saved, id: saved.docId }, "?groupId=" + groupId);
        assert.equal((await readPage())[field] || "", code);
    }
    async function restore() {
        if (new URL(page.url()).origin !== origin) await page.goto(origin + "/admin/v9/");
        const current = await rule();
        if (current) await write(rules, "remove", current);
        await value("");
        for (const row of await rows()) {
            if (row.typeId === type.id && row.fieldA === options[3][0] && row.fieldB === options[3][1]) await write(items, "remove", row, query);
        }
        assert.equal((await rows()).length, 3);
        assert.ok(!await rule());
    }
    return { docId: doc.docId, typeId: type.id, letter, field, readPage, rule, ensureRule, rows, restore, value };
}

module.exports = { fixtureFor, editorUrl, options, tooltip, title, className, output };
