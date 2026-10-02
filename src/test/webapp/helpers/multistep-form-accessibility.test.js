const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { test, before, after } = require("node:test");
const { chromium } = require("playwright");

const source = fs.readFileSync(path.resolve(__dirname, "../../../main/webapp/apps/form/mvc/multistep-form.js"), "utf8")
    .replace(/^export /gm, "");
const stylesheet = fs.readFileSync(path.resolve(__dirname, "../../../main/webapp/apps/form/mvc/default.css"), "utf8");
let browser;

before(async () => { browser = await chromium.launch({ headless: true }); });
after(async () => { await browser?.close(); });

/** Creates real form controllers with isolated HTTP fixtures and no jQuery dependency. */
async function createPage(t, additionalFields = () => "") {
    const page = await browser.newPage();
    t.after(() => page.close());
    await page.route("**/*", async route => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname === "/") {
            return route.fulfill({ contentType: "text/html", body: `<style>${stylesheet}</style><div id="multistep-form-wrapper-first"></div><div id="multistep-form-wrapper-second"></div>` });
        }
        if (url.pathname.endsWith("/get-step")) {
            const prefix = request.headers()["x-csrf-token"] === "first" ? "f1-" : "f2-";
            return route.fulfill({ json: {
                domIdPrefix: prefix,
                validateOnBlur: true,
                html: `<form action="/rest/multistep-form/save-form">
                    <div class="form-group">
                        <label for="${prefix}email">Email</label>
                        <input id="${prefix}email" name="${prefix}email" aria-describedby="${prefix}help">
                        <span id="${prefix}help">Use your work email.</span>
                        <div class="cs-error cs-error-${prefix}email"></div>
                    </div>
                    <div class="form-group">
                        <label for="${prefix}message">Message</label>
                        <textarea id="${prefix}message" name="${prefix}message"></textarea>
                        <div class="cs-error cs-error-${prefix}message"></div>
                    </div>
                    ${additionalFields(prefix)}
                    <input id="${prefix}external" aria-label="Externally validated field" aria-invalid="true">
                </form>`
            } });
        }
        if (url.pathname.endsWith("/validate-field")) {
            const values = request.postDataJSON();
            const fieldErrors = values.email && !values.email.includes("@") ? { email: "Email is invalid." } : {};
            return route.fulfill({ json: { fieldErrors } });
        }
        return route.abort();
    });
    await page.goto("https://multistep-form.test/");
    await page.addScriptTag({ content: source });
    await page.evaluate(async () => {
        window.firstForm = new MultistepForm({ csrf: "first", formName: "contact", stepId: 1 });
        window.secondForm = new MultistepForm({ csrf: "second", formName: "contact", stepId: 1 });
        await firstForm.loadStep("contact", 1);
        await secondForm.loadStep("contact", 1);
    });
    return page;
}

test("blur errors have a live region, preserve help text and clear invalid state without moving focus", async t => {
    const page = await createPage(t);
    const error = page.locator(".cs-error-f1-email");
    const email = page.locator("#f1-email");
    assert.equal(await error.getAttribute("aria-live"), "polite");
    assert.equal(await error.getAttribute("aria-atomic"), "true");
    assert.equal(await error.textContent(), "");
    const errorId = await error.getAttribute("id");
    assert.ok(errorId);

    await email.fill("invalid");
    await email.press("Tab");
    await page.waitForFunction(() => document.querySelector(".cs-error-f1-email").textContent.includes("Email is invalid."));
    assert.equal(await email.getAttribute("aria-invalid"), "true");
    assert.equal(await email.getAttribute("aria-describedby"), `f1-help ${errorId}`);
    assert.equal(await page.evaluate(() => document.activeElement.id), "f1-message");
    assert.equal(await page.locator("#f2-email").getAttribute("aria-invalid"), null);

    await email.fill("visitor@example.com");
    await email.press("Tab");
    await page.waitForFunction(() => document.querySelector("#f1-email").getAttribute("aria-invalid") === "false");
    assert.equal(await error.textContent(), "");
    assert.equal(await email.getAttribute("aria-describedby"), `f1-help ${errorId}`);
    assert.equal(await page.evaluate(() => document.activeElement.id), "f1-message");
});

test("blur validation keeps prefix-like logical field IDs separate from ordinary IDs", async t => {
    const page = await createPage(t, prefix => `
        <div class="form-group">
            <label for="${prefix}team-1">Team</label>
            <input id="${prefix}team-1" name="${prefix}team-1">
            <div class="cs-error cs-error-${prefix}team-1"></div>
        </div>
        <div class="form-group">
            <label for="${prefix}f1-team-1">F1 team</label>
            <input id="${prefix}f1-team-1" name="${prefix}f1-team-1">
            <div class="cs-error cs-error-${prefix}f1-team-1"></div>
        </div>`);
    const firstWrapper = page.locator("#multistep-form-wrapper-first");
    const ordinary = firstWrapper.locator("#f1-team-1");
    const prefixed = firstWrapper.locator("#f1-f1-team-1");
    const requests = [];
    await page.route("**/validate-field?*", route => {
        const request = route.request();
        const fieldId = new URL(request.url()).searchParams.get("field-id");
        const values = request.postDataJSON();
        requests.push({ fieldId, values });
        return route.fulfill({ json: { fieldErrors: { [fieldId]: "F1 team is invalid." } } });
    });

    await prefixed.fill("invalid");
    const response = page.waitForResponse("**/validate-field?*");
    await prefixed.press("Tab");
    await response;
    await page.waitForFunction(() => firstForm._fieldValidationRequests.size === 0);

    assert.deepEqual(requests, [{ fieldId: "f1-team-1", values: { "f1-team-1": "invalid" } }]);
    assert.equal(await ordinary.getAttribute("aria-invalid"), null, "A prefix-like logical ID must not target an ordinary field");
    assert.equal(await firstWrapper.locator(".cs-error-f1-team-1").textContent(), "");
    assert.equal(await prefixed.getAttribute("aria-invalid"), "true");
    assert.equal(await firstWrapper.locator(".cs-error-f1-f1-team-1").textContent(), "F1 team is invalid.");
});

test("submission errors reset independently across form instances and after a step reload", async t => {
    const page = await createPage(t);
    await page.evaluate(async () => {
        const response = { fieldErrors: { email: "Email is invalid.", message: "Message is required." } };
        await firstForm.postSaveAction(response);
        await firstForm.postSaveAction(response);
        await secondForm.postSaveAction(response);
    });
    const errorIds = await page.locator(".cs-error").evaluateAll(elements => elements.map(element => element.id));
    assert.equal(new Set(errorIds).size, 4, "Error IDs must be unique across form instances");
    const descriptions = (await page.locator("#f1-email").getAttribute("aria-describedby")).split(" ");
    assert.equal(descriptions.length, 2, "Repeated validation must not duplicate error references");

    await page.evaluate(() => firstForm.hideErrors());
    for (const field of ["email", "message"]) {
        assert.equal(await page.locator(`#f1-${field}`).getAttribute("aria-invalid"), "false");
        assert.equal(await page.locator(`.cs-error-f1-${field}`).textContent(), "");
        assert.equal(await page.locator(`#f2-${field}`).getAttribute("aria-invalid"), "true");
    }
    assert.equal(await page.locator("#f1-external").getAttribute("aria-invalid"), "true", "Clearing form errors must preserve unrelated validation state");

    await page.evaluate(async () => {
        await firstForm.loadStep("contact", 2);
        await firstForm.postSaveAction({ fieldErrors: { email: "Email is invalid." } });
    });
    const emailErrorId = await page.locator(".cs-error-f1-email").getAttribute("id");
    assert.equal(await page.locator("#f1-email").getAttribute("aria-describedby"), `f1-help ${emailErrorId}`);
    assert.equal(await page.locator("#f1-email").getAttribute("aria-invalid"), "true");
});

test("file errors display escaped filenames as text without creating HTML or running event handlers", async t => {
    const page = await createPage(t, prefix => `
        <div class="form-group">
            <input id="${prefix}upload" name="${prefix}upload" aria-label="Upload">
            <div class="cs-error cs-error-${prefix}upload"></div>
        </div>`);
    const messages = [
        {
            encoded: "File Draft B · 08 · System notifications &lt;b&gt;B11a–c.png contains a forbidden character or string in its name: &gt;.",
            text: "File Draft B · 08 · System notifications <b>B11a–c.png contains a forbidden character or string in its name: >."
        },
        {
            encoded: "File report&lt;img src=x onerror=window.injected=true&gt;.pdf contains a forbidden character or string in its name: &gt;.",
            text: "File report<img src=x onerror=window.injected=true>.pdf contains a forbidden character or string in its name: >."
        }
    ];
    const error = page.locator(".cs-error-f1-upload");

    for (const message of messages) {
        await page.evaluate(encoded => firstForm.postSaveAction({ fieldErrors: { upload: encoded } }), message.encoded);
        assert.equal(await error.textContent(), message.text);
        assert.equal(await error.locator("b, img").count(), 0);
        assert.equal(await page.evaluate(() => window.injected), undefined);
    }
});

for (const [buttonName, instance, width, motion] of [
    ["Next", "first", 600, "no-preference"],
    ["Submit", "second", 375, "reduce"]
]) {
    test(`${buttonName} reveals the first visible field error on every failed attempt in a small viewport`, async t => {
        const page = await createPage(t, prefix => `
            <div hidden class="form-group"><input id="${prefix}hidden"><div class="cs-error cs-error-${prefix}hidden"></div></div>
            <div class="submission-spacer"></div>
            <button type="submit">${buttonName}</button>`);
        await page.setViewportSize({ width, height: 400 });
        await page.emulateMedia({ reducedMotion: motion });
        await page.addStyleTag({ content: `
            body { padding-top: 250px; }
            body::before { content: "Fixed header"; position: fixed; top: 0; left: 0; width: 100%; height: 80px; background: white; z-index: 1; }
            .submission-spacer { height: 1200px; }` });
        const wrapper = page.locator(`#multistep-form-wrapper-${instance}`);
        const error = wrapper.locator(`.cs-error-${instance === "first" ? "f1" : "f2"}-email`);
        if (instance === "second") {
            await page.evaluate(() => firstForm.postSaveAction({ fieldErrors: { email: "Other form error." } }));
        }
        let submissions = 0;
        await page.route("**/save-form?*", route => {
            submissions++;
            return route.fulfill({ json: { fieldErrors: {
                hidden: "Hidden field error.", message: "Message is required.", email: "Email is invalid."
            } } });
        });

        for (let attempt = 1; attempt <= 2; attempt++) {
            await wrapper.getByRole("button", { name: buttonName }).click();
            await page.waitForFunction(name => !window[`${name}Form`]._isNavigating, instance);
            assert.equal(submissions, attempt);
            await page.waitForFunction(selector => {
                const rect = document.querySelector(selector).getBoundingClientRect();
                return rect.top >= 80 && rect.bottom <= innerHeight;
            }, `#multistep-form-wrapper-${instance} .cs-error-${instance === "first" ? "f1" : "f2"}-email`, { timeout: 1500 });
            assert.equal(await error.evaluate(element => element === document.activeElement), true);
        }
    });
}

test("global errors are focused and revealed again after scrolling away", async t => {
    const page = await createPage(t, () => '<div style="height: 1600px"></div>');
    await page.setViewportSize({ width: 600, height: 400 });
    await page.emulateMedia({ reducedMotion: "reduce" });
    await page.addStyleTag({ content: "body { padding-top: 250px; }" });
    const alert = page.locator("#multistep-form-wrapper-first .alert-danger");

    for (let attempt = 0; attempt < 2; attempt++) {
        await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
        await page.evaluate(() => firstForm.showGlobalErr({ err_msg: "The upload failed." }));
        const rect = await alert.boundingBox();
        assert.ok(rect.y >= 0 && rect.y + rect.height <= 400);
        assert.equal(await alert.evaluate(element => element === document.activeElement), true);
    }
});
