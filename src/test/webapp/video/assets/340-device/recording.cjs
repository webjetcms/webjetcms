const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const { setTimeout: delay } = require("node:timers/promises");
const { webkit } = require("playwright");
const { readDashboardBootstrap } = require("../../../helpers/dashboard-browser");
const { FEATURE_VIDEO_DIRECTORY } = require("../../../helpers/feature_video_paths");

const login = "publishNotification";
const address = "webjetcmsnotif@fexpost.com";
const dashboard = '.md-dashboard[data-loaded="true"]';
const dialog = ".md-dashboard-modal--device-security";
const guide = "https://docs.webjetcms.sk/latest/sk/redactor/admin/welcome?id=ak-prihl%C3%A1senie-nepozn%C3%A1te";
const mailAPI = "https://tempmail.plus/api/mails";
const alertSubject = "Nové prihlásenie do WebJET CMS";
const codeSubject = "Kód na potvrdenie prihlásenia do WebJET CMS";
const viewport = { width: 1360, height: 765 };

async function mailRequest(id = "") {
    const response = await fetch(mailAPI + id + "?email=" + encodeURIComponent(address) + "&limit=30", {
        signal: AbortSignal.timeout(10000)
    });
    assert.ok(response.ok, "The demo inbox must be reachable.");
    const result = await response.json();
    assert.ok(result.result, "The demo inbox must accept the request.");
    return result;
}

// Filter by arrival, subject and the exact event link before showing the message or using its code.
async function findMessage(id, subject, after) {
    const examined = new Set();
    const deadline = Date.now() + 75000;
    while (Date.now() < deadline) {
        const { mail_list: messages } = await mailRequest();
        for (const item of messages) {
            if (item.mail_id <= after || examined.has(item.mail_id) || !item.subject.startsWith(subject)) continue;
            examined.add(item.mail_id);
            const message = await mailRequest("/" + item.mail_id);
            if (new RegExp("securityEvent=" + id + "(?:&|[\"'])").test(message.html)) return message;
        }
        await delay(1200);
    }
    throw Error("No matching email arrived for demo event " + id);
}

async function createDemo(I, titleOnly = false) {
    const origin = new URL(process.env.CODECEPT_URL || "http://iwcm.interway.sk").origin;
    assert.equal(origin, "http://iwcm.interway.sk", "Use the local DEV instance with the dedicated notification fixture.");
    const password = process.env.CODECEPT_DEFAULT_PASSWORD;
    assert.ok(password, "The notification fixture needs CODECEPT_DEFAULT_PASSWORD.");
    const directory = path.join(FEATURE_VIDEO_DIRECTORY, "340-zariadenia", ".work");
    let output;
    if (!titleOnly) {
        await fs.mkdir(directory, { recursive: true });
        output = await fs.mkdtemp(path.join(directory, (process.env.VIDEO_SHOT?.trim() || "full") + "-"));
        console.log("Demo camera sources and cues:", output);
    }
    let owner;
    await I.usePlaywrightTo("use the fresh recording profile", async ({ page }) => { owner = page; });
    const pages = { owner };
    const started = { owner: Date.now() };
    const events = {};
    const extraPages = [];
    let alternate, report, mailbox, proof, confirmed = false, blocked = false, deviceCookie;
    const firstMail = Number((await mailRequest()).first_id || 0);
    const notice = () => '[data-notice-id="newDevice:' + events.owner.id + '"]';

    async function attach(key, page) {
        pages[key] = page;
        started[key] = Date.now();
        extraPages.push([key, page]);
        if (key !== "other") {
            const size = owner.viewportSize();
            await page.setViewportSize(size);
            const cdp = await page.context().newCDPSession(page);
            try { await cdp.send("Emulation.setVisibleSize", size); } finally { await cdp.detach(); }
        }
        return page;
    }
    async function cue(key, name, hold = 1) {
        if (titleOnly) return;
        const page = pages[key];
        const seconds = (Date.now() - started[key]) / 1000;
        await fs.appendFile(path.join(output, "cues.ndjson"), JSON.stringify({ key, name, seconds }) + "\n");
        await page.screenshot({ path: path.join(output, name + ".jpg"), quality: 78, type: "jpeg", timeout: 10000 });
        await delay(hold * 1000);
    }
    async function form(page) {
        await page.goto(origin + "/admin/logon/", { waitUntil: "domcontentloaded" });
        await page.locator("#username").waitFor();
        // The DEV server skips the stock E2E browser signatures. Opt this fresh profile into real detection.
        await page.setExtraHTTPHeaders({ "User-Agent": (await page.evaluate(() => navigator.userAgent)) + " WebJET-feature-video" });
        assert.equal(await page.locator("#password").getAttribute("type"), "password");
    }
    async function credentials(key, onCamera) {
        const page = pages[key];
        await page.locator("#username").fill(login);
        await page.locator("#password").fill(password);
        if (onCamera) await cue(key, key + (blocked ? "-retry" : "-credentials"), 1);
        await page.locator('[name="login-submit"],#login-submit').first().click();
    }
    async function state(page) {
        return page.evaluate(readDashboardBootstrap);
    }
    const sessions = data => data.currentSessions.userSessions.flatMap(cluster => cluster.userSessions);
    async function newLogin(key, onCamera) {
        const page = pages[key];
        await credentials(key, onCamera);
        await page.locator(dashboard).waitFor({ timeout: 30000 });
        assert.equal(await page.evaluate(() => window.currentUser.login), login);
        const data = await state(page);
        const own = sessions(data).find(s => s.sessionId === data.currentSessions.currentSessionId);
        const alert = data.notices.find(n => n.kind === "newDevice" && String(n.securityEvent.id) === String(own.deviceId));
        assert.ok(own.deviceId && !own.deviceConfirmed && alert, "Expected a fresh browser notice for device " + own.deviceId + ", confirmed=" + own.deviceConfirmed + ", new-device IDs=" + data.notices.filter(n => n.kind === "newDevice").map(n => n.securityEvent.id));
        events[key] = alert.securityEvent;
        if (key === "other") {
            assert.notEqual(events.owner.id, events.other.id);
            deviceCookie = (await page.context().cookies(origin)).find(c => c.name === "wjdevice")?.value;
            assert.ok(deviceCookie);
        }
    }
    async function prepareMailbox() {
        if (mailbox) return;
        mailbox = await attach("mail", await owner.context().newPage());
        await mailbox.goto("https://tempmail.plus/en/", { waitUntil: "domcontentloaded" });
        await mailbox.locator("#pre_button").fill(address.split("@")[0]);
        await mailbox.locator("#pre_button").press("Enter");
        await mailbox.locator("#domain").click();
        await mailbox.locator("button.dropdown-item").filter({ hasText: "fexpost.com" }).click();
    }
    async function showMail(key, kind) {
        const subject = kind === "code" ? codeSubject : alertSubject;
        const id = events[key].id;
        const message = await findMessage(id, subject, firstMail);
        await prepareMailbox();
        await mailbox.bringToFront();
        await mailbox.goto("https://tempmail.plus/en/#!", { waitUntil: "domcontentloaded" });
        const row = mailbox.locator(`div.inbox .mail[onclick="fex.go('mail/${message.mail_id}')"] .subj`);
        await row.waitFor({ timeout: 30000 });
        await row.evaluate(el => el.scrollIntoView({ block: "center" }));
        await row.hover();
        await cue("mail", key + "-" + kind + "-inbox", 1);
        await row.click();
        const content = mailbox.locator("#info > .overflow-auto");
        const link = content.locator('a[href*="securityEvent=' + id + '"]').last();
        await link.waitFor({ timeout: 30000 });
        assert.ok((await mailbox.locator("#info .subject").innerText()).includes(subject));
        if (kind === "code") {
            proof = (await content.innerText()).match(/(?:^|\n)\s*([0-9]{6})\s*(?:\n|$)/)?.[1];
            assert.ok(proof, "The displayed message must contain a six-digit code on its own line.");
        } else {
            for (const field of ["browserName", "operatingSystem", "ipAddress"]) {
                assert.ok((await content.innerText()).includes(events[key][field]));
            }
        }
        await mailbox.locator("#info .subject").evaluate(el => {
            const top = el.getBoundingClientRect().top + window.scrollY;
            window.scrollTo(0, top - 105);
        });
        await delay(600);
        await cue("mail", key + "-" + kind + "-mail", 4);
    }
    async function ensurePendingOwner() {
        if (events.owner) return;
        await form(owner);
        await newLogin("owner", false);
    }
    async function ensureConfirmedOwner() {
        if (confirmed) return;
        await ensurePendingOwner();
        const id = events.owner.id;
        const message = await findMessage(id, alertSubject, firstMail);
        const url = await owner.evaluate(html => {
            const doc = new DOMParser().parseFromString(html, "text/html");
            return doc.querySelector('a[href*="deviceConfirmation="]').getAttribute("href");
        }, message.html);
        const link = new URL(url);
        assert.equal(link.pathname, "/admin/v9/");
        assert.equal(link.searchParams.get("securityEvent"), String(id));
        await owner.goto(origin + link.pathname + link.search, { waitUntil: "domcontentloaded" });
        await owner.locator(".toast-success").waitFor();
        assert.ok((await owner.evaluate(readDashboardBootstrap, "?securityEvent=" + id)).requestedSecurityEvent.confirmedAt);
        confirmed = true;
    }
    async function otherLoginForm() {
        if (pages.other) return;
        alternate = await webkit.launch({ headless: true, timeout: 20000 });
        const context = await alternate.newContext({
            locale: "sk-SK", viewport,
            recordVideo: { dir: path.join(output, "second-profile"), size: viewport }
        });
        const other = await attach("other", await context.newPage());
        await form(other);
    }
    async function checkPair(otherActive) {
        const data = await state(owner);
        const mine = sessions(data);
        assert.ok(mine.some(s => s.sessionId === data.currentSessions.currentSessionId && String(s.deviceId) === String(events.owner.id) && s.deviceConfirmed));
        assert.equal(mine.some(s => String(s.deviceId) === String(events.other.id)), otherActive);
    }
    async function showDevices(key) {
        await owner.bringToFront();
        const modal = owner.locator(".md-dashboard-modal--sessions");
        if (await modal.isVisible()) await modal.locator(".btn-close").first().click();
        await I.videoClick("#dropdownMenuUser");
        await cue("owner", key + "-menu");
        await I.videoClick(".md-user-menu__sessions");
        await modal.getByRole("tab", { name: /Moje zariadenia/ }).waitFor();
        await modal.locator(".modal-dialog").evaluate(el => Promise.all(el.getAnimations().map(a => a.finished)));
        await cue("owner", key + "-sessions");
        await I.videoClick('.md-dashboard-sessions__tabs [id$="-devices"]');
        const row = modal.locator('[data-device-id="' + events[key].id + '"]');
        const badge = key === "owner" ? "Potvrdené" : "Zablokované";
        await row.getByText(badge, { exact: true }).waitFor();
        await row.scrollIntoViewIfNeeded();
        await cue("owner", key + "-devices", 3);
    }
    async function openReport() {
        const id = events.other.id;
        const link = mailbox.getByRole("link", { name: /Nebol som to ja/ });
        const url = new URL(await link.getAttribute("href"));
        assert.equal(url.origin, origin, "Only open the local DEV link from the email.");
        assert.equal(url.search, "?securityEvent=" + id);
        await link.hover();
        await cue("mail", "other-open-link");
        const opening = mailbox.waitForEvent("popup");
        await link.click();
        report = await attach("report", await opening);
        await report.locator(dialog + " .md-dashboard-device-security__report").waitFor();
        assert.equal(await report.evaluate(() => window.currentUser.login), login);
        await cue("report", "report-open", 3);
    }
    async function blockOther() {
        await checkPair(true);
        await report.bringToFront();
        const button = report.getByRole("button", { name: "Zablokovať zariadenie" });
        await button.hover();
        await cue("report", "report-before-click");
        await button.click();
        await report.locator(".md-dashboard-device-security__result").waitFor();
        await checkPair(false);
        const event = (await owner.evaluate(readDashboardBootstrap, "?securityEvent=" + events.other.id)).requestedSecurityEvent;
        assert.ok(event.reportedAt);
        blocked = true;
        await cue("report", "report-result", 3);
    }
    async function checkOtherSignedOut() {
        await pages.other.goto(origin + "/admin/v9/", { waitUntil: "domcontentloaded" });
        await pages.other.locator("#username").waitFor();
        assert.match(new URL(pages.other.url()).pathname, /^\/admin\/logon\//);
        await checkPair(false);
        await cue("other", "other-signed-out", 3);
    }
    return {
        loginForm: () => form(owner),
        signInOwner: async () => { await cue("owner", "owner-login"); await newLogin("owner", true); },
        showNotice: async () => {
            await owner.locator(notice()).getByText("Prihlásili ste sa z nového prehliadača").waitFor();
            await cue("owner", "owner-notice", 3);
        },
        showMail, ensurePendingOwner, ensureConfirmedOwner, otherLoginForm, showDevices, openReport, blockOther, checkOtherSignedOut,
        requestCode: async () => {
            await owner.bringToFront();
            await cue("owner", "owner-confirm-start");
            await I.videoClick(notice() + " .md-dashboard__notice-confirm");
            await owner.locator(notice() + " input[name=deviceConfirmationCode]").waitFor();
            await cue("owner", "owner-code-form", 2);
        },
        enterCode: async () => {
            await owner.bringToFront();
            await owner.locator(notice() + " input[name=deviceConfirmationCode]").pressSequentially(proof, { delay: 115 });
            await cue("owner", "owner-code-entered", 2);
            await I.videoClick(notice() + ' .md-dashboard-device-confirmation [type="submit"]');
            await owner.locator(notice()).waitFor({ state: "detached" });
            const result = await owner.evaluate(readDashboardBootstrap, "?securityEvent=" + events.owner.id);
            assert.ok(result.requestedSecurityEvent.confirmedAt);
            confirmed = true;
            proof = undefined;
            await cue("owner", "owner-approved", 3);
        },
        signInOther: async () => { await cue("other", "other-login"); await newLogin("other", true); await cue("other", "other-dashboard", 2); },
        checkOtherStillSignedIn: async () => {
            await pages.other.bringToFront();
            await pages.other.reload({ waitUntil: "domcontentloaded" });
            await pages.other.locator(dashboard).waitFor();
            await checkPair(true);
            await cue("other", "other-still-active", 3);
        },
        ensureBlockedOther: async () => {
            if (blocked) return;
            await ensureConfirmedOwner();
            await otherLoginForm();
            await newLogin("other", false);
            await checkPair(true);
            // A standalone retake starts from its own real blocked device and live cookie.
            const id = events.other.id;
            const saved = await owner.evaluate(async id => {
                const response = await fetch("/admin/rest/security/login-events/" + id + "/report", {
                    method: "POST", headers: { "X-CSRF-Token": window.csrfToken }
                });
                if (!response.ok) throw Error("Could not prepare the demo device block.");
                return response.json();
            }, id);
            assert.equal(String(saved.id), String(id));
            assert.ok(saved.reportedAt);
            blocked = true;
            await checkPair(false);
            report = await attach("report", await owner.context().newPage());
            await report.goto(origin + "/admin/v9/?securityEvent=" + id, { waitUntil: "domcontentloaded" });
            await pages.other.goto(origin + "/admin/v9/", { waitUntil: "domcontentloaded" });
            await pages.other.locator("#username").waitFor();
        },
        retryBlockedLogin: async () => {
            const cookie = (await pages.other.context().cookies(origin)).find(c => c.name === "wjdevice");
            assert.ok(cookie && cookie.value === deviceCookie, "The blocked second profile must retain its own device cookie.");
            await cue("other", "other-retry-form");
            await credentials("other", true);
            await pages.other.locator("#deviceCode").waitFor();
            assert.ok((await pages.other.locator("body").innerText()).includes("Overenie zablokovaného zariadenia"));
            assert.equal(new URL(pages.other.url()).pathname, "/admin/logon/device/");
            assert.equal(await pages.other.locator("#deviceCode").inputValue(), "");
            await checkPair(false);
            await cue("other", "other-challenge", 5);
        },
        showAdvice: async () => {
            await report.locator(".md-dashboard-device-security__result").waitFor();
            await cue("report", "owner-advice", 4);
        },
        showDocumentation: async () => {
            await owner.bringToFront();
            await owner.goto(guide, { waitUntil: "domcontentloaded" });
            const heading = owner.locator("article").getByRole("heading", { name: "Ak prihlásenie nepoznáte" });
            await heading.waitFor();
            await owner.evaluate(() => document.fonts.ready);
            await owner.waitForFunction(() => {
                for (let el = document.querySelector("article"); el; el = el.parentElement) {
                    if (Number(getComputedStyle(el).opacity) < 0.99) return false;
                }
                return true;
            });
            await heading.evaluate(el => el.scrollIntoView({ block: "start" }));
            await cue("owner", "guide", 4);
        },
        thumbnail: async () => {
            // This genuine read-only link opens the account-security dialog. Leave the device untouched.
            await owner.goto(origin + "/admin/v9/?securityEvent=" + events.owner.id, { waitUntil: "domcontentloaded" });
            await owner.locator(dialog + " .md-dashboard-device-security__report").waitFor();
            await delay(500);
        },
        close: async () => {
            try {
                for (const [key, page] of extraPages.reverse()) {
                    const recording = page.video();
                    if (key === "other" && !blocked && events.other) await page.goto(origin + "/logoff.do?forward=/admin/logon/");
                    await page.close();
                    if (recording && !titleOnly) await recording.saveAs(path.join(output, key + ".webm"));
                }
            } finally {
                if (alternate) await alternate.close();
                if (process.env.VIDEO_SHOT?.trim() === "upozornenie" && events.owner && !confirmed) await ensureConfirmedOwner();
                if (events.owner && !owner.isClosed()) await owner.goto(origin + "/logoff.do?forward=/admin/logon/", { waitUntil: "domcontentloaded" });
            }
        }
    };
}

module.exports = { createDemo };
