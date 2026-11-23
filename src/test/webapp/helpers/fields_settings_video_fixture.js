/** Owns only the four temporary settings used by the PR 366 recording. */
module.exports = function createFieldsSettingsFixture(I, DT, DTE) {
    const { store } = require("codeceptjs");
    const entity = "sk.iway.iwcm.doc.DocDetails";
    const fields = ["fieldR", "fieldS", "fieldT"];
    const urls = {
        fields: "/admin/v9/settings/fields-settings/",
        tabs: "/admin/v9/settings/tabs-settings/",
        page: "/admin/v9/webpages/web-pages-list/?docid=57"
    };
    const tables = { fields: "fieldsSettingsDataTable", tabs: "tabsSettingsDataTable" };
    const owned = [];

    // Use the normal Editor endpoint, including validation, domain scope and CSRF.
    async function request(kind, suffix, body) {
        return I.executeScript(async ({ kind, suffix, body }) => {
            const response = await fetch(`/admin/rest/settings/${kind}-settings/${suffix}`, {
                method: body ? "POST" : "GET",
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken },
                body: body ? JSON.stringify(body) : undefined
            });
            if (!response.ok) throw new Error(`Video fixture request failed: ${response.status}`);
            const result = await response.json();
            if (result.error || result.fieldErrors?.length) throw new Error(JSON.stringify(result));
            return result;
        }, { kind, suffix, body });
    }

    async function find(kind, key) {
        if (store.dryRun) return [];
        const property = kind === "tabs" ? "TabId" : "FieldName";
        const query = `search/findByColumns?size=100&searchClassName=${encodeURIComponent("^" + entity + "$")}&search${property}=${encodeURIComponent("^" + key + "$")}`;
        const result = await request(kind, query);
        return result.content.filter(row => row.className === entity && row[kind === "tabs" ? "tabId" : "fieldName"] === key);
    }

    async function open(kind) {
        const currentUrl = await I.grabCurrentUrl();
        if (typeof currentUrl !== "string" || !currentUrl.endsWith(urls[kind])) await I.amOnPage(urls[kind]);
        await DT.waitForLoader(tables[kind]);
        await I.waitForVisible(`#${tables[kind]}_wrapper button.buttons-create`, 20);
    }

    async function checkBaseline() {
        await open("fields");
        // The requested identifiers cannot carry an autotest prefix. Refuse collisions
        // and retain the returned IDs, rather than deleting matching user settings.
        for (const [kind, key] of [["tabs", "seo"], ...fields.map(field => ["fields", field])]) {
            if ((await find(kind, key)).length) throw new Error(`Existing ${kind} setting ${key}: preserve it and use a clean demo baseline.`);
        }
    }

    async function remember(kind, key) {
        if (store.dryRun) return;
        const rows = await find(kind, key);
        if (rows.length !== 1) throw new Error(`Expected exactly one saved ${kind} setting: ${key}`);
        if (!owned.some(item => item.kind === kind && item.row.id === rows[0].id)) owned.push({ kind, row: rows[0] });
    }

    async function seed(kind, key) {
        if (store.dryRun) return;
        if (owned.some(item => item.kind === kind && item.row[kind === "tabs" ? "tabId" : "fieldName"] === key)) return;
        if ((await find(kind, key)).length) throw new Error(`Refusing to overwrite ${kind} setting ${key}`);
        const row = kind === "tabs"
            ? { className: entity, tabId: "seo", title: "SEO", sortAfter: "basic" }
            : { className: entity, fieldName: key, tab: "seo" };
        const result = await request(kind, "editor", { action: "create", data: { "-1": row } });
        if (!result.data?.[0]?.id) throw new Error("Video fixture creation did not return its ID.");
        owned.push({ kind, row: result.data[0] });
    }

    async function ensureTab() { await seed("tabs", "seo"); }
    async function ensureFields() {
        await ensureTab();
        for (const field of fields) await seed("fields", field);
    }

    async function cleanup() {
        if (store.dryRun) return;
        await I.switchTo();
        if (await I.grabNumberOfVisibleElements("#datatableInit_modal.DTED.show") > 0) await DTE.cancel();
        for (const item of [...owned].reverse()) {
            await request(item.kind, "editor", { action: "remove", data: { [item.row.id]: item.row } });
        }
        owned.length = 0;
    }

    return { entity, fields, urls, tables, open, checkBaseline, remember, ensureTab, ensureFields, cleanup };
};
