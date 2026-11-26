// Seed only the film's folder on the local DEV installation. Run with Node before a recording.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const demo = require("./demo");
const baseURL = "http://iwcm.interway.sk";
const output = path.resolve(__dirname, "../../../../../../docs/feature-video/211-penzion");
const groupEndpoint = "/admin/rest/groups";
const docEndpoint = "/admin/rest/web-pages";

async function adminClient(page) {
    await page.goto(baseURL + "/admin/logon/");
    await page.locator("[name=username]").fill("tester");
    await page.locator("[name=password]").fill(process.env.CODECEPT_DEFAULT_PASSWORD);
    await page.locator("#login-submit").click();
    await page.locator("button.js-profile-toggler").waitFor();
    return async (url, data) => page.evaluate(async ({ url, data }) => {
        const res = await fetch(url, { method: data ? "POST" : "GET", headers: {
            "X-CSRF-Token": window.csrfToken, "X-Requested-With": "XMLHttpRequest", "Content-Type": "application/json"
        }, body: data ? JSON.stringify(data) : undefined });
        if (!res.ok) throw Error(url + " " + res.status);
        const json = await res.json();
        if (json.error || json.fieldErrors?.length) throw Error(JSON.stringify([json.error, json.fieldErrors]));
        return json;
    }, { url, data });
}

async function seed(api) {
    const parentId = 67;
    const parent = await api(groupEndpoint + "/" + parentId);
    assert.equal(parent.groupName, "Test stavov");
    const children = await api(groupEndpoint + "/all?groupId=" + parentId);
    let group = children.content.find(item => item.groupName === demo.groupName);
    if (!group) {
        const fresh = await api(groupEndpoint + "/-1?groupId=" + parentId);
        assert.equal(fresh.parentGroupId, parentId);
        Object.assign(fresh, { groupName: demo.groupName, navbarName: demo.groupName, htmlHead: demo.head,
            urlDirName: "penzion-pod-lipou", showInNavbar: 0, showInSitemap: 0, menuType: 0 });
        group = (await api(groupEndpoint + "/editor", { action: "create", data: { 0: fresh } })).data[0];
    }
    group = await api(groupEndpoint + "/" + group.groupId);
    assert.equal(group.parentGroupId, parentId);
    assert.ok(group.htmlHead.includes(demo.marker));
    const groupId = group.groupId;
    const templates = "/admin/rest/templates/temps-list";
    const name = "Penzión Pod lipou · 211";
    let template = (await api(templates + "/all")).content.find(item => item.tempName === name);
    if (!template) {
        const blank = await api(templates + "/8");
        assert.equal(blank.forward, "jet/blank.jsp");
        Object.assign(blank, { tempId: -1, id: -1, tempName: name, availableGroups: String(groupId), availableGrooupsList: [group] });
        template = (await api(templates + "/editor", { action: "create", data: { 0: blank } })).data[0];
    }
    assert.deepEqual(template.availableGrooupsList.map(group => group.groupId), [groupId]);
    const tempId = template.tempId;
    if (group.tempId !== tempId) {
        group.tempId = tempId;
        await api(groupEndpoint + "/editor", { action: "edit", data: { [groupId]: group } });
    }
    const list = await api(docEndpoint + "/all?groupId=" + groupId + "&size=100");
    const fixture = { baseURL, groupId, documents: {} };
    for (const item of [...demo.pages, demo.search]) {
        let doc = list.content.find(row => row.title === item.title);
        if (!doc) {
            const fresh = await api(docEndpoint + "/-1?groupId=" + groupId);
            const url = demo.url(item);
            Object.assign(fresh, { groupId, tempId, title: item.title, navbar: item.title, data: demo.body(item, groupId),
                available: true, searchable: item !== demo.search, showInMenu: false, cacheable: false,
                generateUrlFromTitle: false, urlInheritGroup: false, editorVirtualPath: url, virtualPath: url });
            fresh.editorFields.requestPublish = true;
            doc = (await api(docEndpoint + "/editor?groupId=" + groupId, { action: "create", data: { 0: fresh } })).data[0];
        }
        doc = await api(docEndpoint + "/" + doc.docId);
        assert.equal(doc.groupId, groupId);
        assert.equal(doc.virtualPath, demo.url(item));
        assert.ok(doc.available && (item === demo.search ? doc.data.includes("rootGroup=" + groupId) : doc.data.replaceAll("&nbsp;", " ").replaceAll("\u00a0", " ").includes(item.text)));
        if (doc.tempId !== tempId) {
            doc.tempId = tempId;
            doc.editorFields.requestPublish = true;
            await api(docEndpoint + "/editor?groupId=" + groupId, { action: "edit", data: { [doc.docId]: doc } });
        }
        fixture.documents[item.slug] = doc.docId;
    }
    fs.mkdirSync(output, { recursive: true });
    fs.writeFileSync(path.join(output, "fixture.json"), JSON.stringify(fixture, null, 2));
    console.log(JSON.stringify(fixture));
}
if (require.main === module) (async () => {
    const browser = await chromium.launch();
    try { await seed(await adminClient(await browser.newPage())); }
    finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });

module.exports = { adminClient, output };
