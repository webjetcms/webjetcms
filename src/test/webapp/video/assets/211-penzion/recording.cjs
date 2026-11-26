const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const demo = require("./demo");
const { output, adminClient } = require("./prepare.cjs");
const documents = "/admin/rest/web-pages";
const adminPath = "/admin/v9/webpages/web-pages-list/";
const docsURL = "https://docs.webjetcms.sk/latest/sk/redactor/apps/semantic-search/";

async function changeBaseline(api, fixture, enabled) {
    const id = fixture.documents[demo.search.slug];
    const doc = await api(documents + "/" + id);
    assert.equal(doc.groupId, fixture.groupId);
    assert.equal(doc.virtualPath, demo.url(demo.search));
    const include = doc.data.match(/!INCLUDE\(\/components\/search\/search\.jsp,[^!]*\)!/g) || [];
    assert.equal(include.length, 1);
    const param = (name, value) => new RegExp("\\b" + name + "=[\"]?" + value + "[\"]?(?=[, )])").test(include[0]);
    assert.ok(param("rootGroup", String(fixture.groupId)) && param("searchType", "semantic"));
    const answer = /\banswerAllowed=("?)(trueValue|falseValue)\1/;
    const wanted = enabled ? "trueValue" : "falseValue";
    const match = include[0].match(answer);
    assert.ok(match, "Expected an explicit answer setting");
    const wasEnabled = match[2] === "trueValue";
    if (wasEnabled === enabled) return wasEnabled;
    doc.data = doc.data.replace(include[0], include[0].replace(answer, "answerAllowed=" + wanted));
    doc.editorFields.requestPublish = true;
    const result = await api(documents + "/editor?groupId=" + fixture.groupId, { action: "edit", data: { [id]: doc } });
    assert.equal(result.data[0].docId, id);
    return wasEnabled;
}

function checkAnswer(text) {
    const answer = text.toLocaleLowerCase("sk").replace(/\s+/g, " ");
    assert.match(answer, /so psom|ps[aoí]|psík/);
    assert.match(answer, /dohod|vopred|predchádzajúc/);
    assert.match(answer, /(?:8|osem)\s*(?:eur|€)/);
    assert.match(answer, /noc/);
    assert.match(answer, /bezplatn|zadarmo|zdarma|bez poplatku/);
    assert.match(answer, /dvor/);
}

async function contextFor(I) {
    const fixture = JSON.parse(fs.readFileSync(path.join(output, "fixture.json"), "utf8"));
    assert.equal(fixture.baseURL, process.env.CODECEPT_URL || "http://iwcm.interway.sk");
    let admin, api, browserPage;
    await I.usePlaywrightTo("prepare a separate authenticated DEV session", async ({ page }) => {
        browserPage = page;
        admin = await page.context().browser().newContext();
        try { api = await adminClient(await admin.newPage()); }
        catch (error) { await admin.close(); throw error; }
    });
    const searchURL = fixture.baseURL + demo.url(demo.search);
    const visit = async () => {
        await I.amOnPage(searchURL);
        await I.waitForVisible("#searchWords", 20);
    };
    const ask = async () => {
        await I.videoClick("#searchWords");
        await browserPage.locator("#searchWords").pressSequentially(demo.question, { delay: 75 });
        await I.wait(1.5);
        await I.videoClick(".smallSearchSubmit");
        await I.waitForElement("h1.searchResultsH1", 75);
        const resultLinks = browserPage.locator(".search > p > a");
        const hrefs = await resultLinks.evaluateAll(links => links.map(a => new URL(a.href).pathname));
        for (const item of demo.pages.slice(1, 3)) assert.ok(hrefs.includes(demo.url(item)), item.title);
        assert.ok(hrefs.every(href => demo.pages.some(item => demo.url(item) === href)), "Unexpected result outside the demo");
        await I.waitForVisible("div.search dl dt", 10);
    };
    return {
        fixture, visit, ask, searchURL, docsURL, checkAnswer, demo,
        result: item => '.search > p > a[href="' + demo.url(item) + '"]',
        baseline: enabled => changeBaseline(api, fixture, enabled),
        asEditor: async () => browserPage.context().addCookies(await admin.cookies()),
        asVisitor: async () => browserPage.context().clearCookies(),
        editorListURL: fixture.baseURL + adminPath + "?groupid=" + fixture.groupId,
        close: () => admin.close()
    };
}
module.exports = { contextFor, checkAnswer, changeBaseline, docsURL };
