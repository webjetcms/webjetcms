const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { JSDOM } = require("jsdom");

const moduleDirectory = path.resolve(__dirname, "../../../main/webapp/admin/v9/src/js/dashboard");
const copy = value => JSON.parse(JSON.stringify(value));
const tick = () => new Promise(resolve => setImmediate(resolve));
const item = (id, type = "test", size = "2x2", options = {}) => ({ id, type, size, options, collapsed: false });

/** Runs production browser modules against a DOM and a stateful settings server. */
function fixture(t, { items = [], configured = true, definitions = [], defaults = [], failSave = false, failLoad = false, failReset = false, deferModalShown = false } = {}) {
    const dom = new JSDOM("<!doctype html><html><body><div id='alerts'>System warning</div><div id='dashboard'></div></body></html>", { url: "http://localhost/admin/v9/" });
    const { window } = dom;
    window.WJ = { translate: key => key };
    window.csrfToken = "test-csrf-token";
    window.bootstrap = { Modal: class {
        constructor(element) {
            assert.ok(element.querySelector(".modal-dialog"), "Bootstrap reads the dialog element during construction");
            this.element = element;
        }
        show() {
            const shown = () => {
                this.element.focus();
                this.element.dispatchEvent(new window.Event("shown.bs.modal"));
            };
            if (deferModalShown) setImmediate(shown);
            else shown();
        }
        hide() { this.element.dispatchEvent(new window.Event("hidden.bs.modal")); }
        dispose() {}
    } };
    const requests = [];
    let stored = { version: 1, configured, items: copy(items), domainOptions: {}, acknowledgedNewsVersion: null };
    const fetch = async (url, options = {}) => {
        requests.push({ url, ...options });
        if (options.method === "PUT") {
            if (failSave) return { ok: false, status: 500 };
            stored = { ...JSON.parse(options.body), configured: true };
        } else if (options.method === "DELETE") {
            if (failReset) return { ok: false, status: 503 };
            stored = { version: 1, configured: false, items: [], domainOptions: {}, acknowledgedNewsVersion: null };
        } else if (failLoad) return { ok: false, status: 503 };
        return { ok: true, json: async () => copy(stored) };
    };
    const context = vm.createContext({ window, document: window.document, CustomEvent: window.CustomEvent, URL: window.URL, AbortController, fetch, console, crypto: require("node:crypto").webcrypto });
    for (const filename of ["registry.js", "model.js", "widget-utils.js", "dashboard.js"]) {
        const source = fs.readFileSync(path.join(moduleDirectory, filename), "utf8").replace(/^import .+;\r?$/gm, "").replace(/^export /gm, "");
        vm.runInContext(source, context, { filename });
    }
    vm.runInContext("this.Controller = DashboardController", context);
    context.registerWidget({ type: "test", titleKey: "Test", sizes: ["1x1", "2x2", "2x3", "3x2", "3x3", "fullauto"], multiple: true, render: ({ container }) => { container.textContent = "Widget content"; } });
    definitions.forEach(definition => context.registerWidget(definition));
    const host = window.document.querySelector("#dashboard");
    const controller = new context.Controller(host, { config: { dashboardDefaults: defaults } });
    t.after(() => { controller.destroy(); window.close(); });
    return { window, host, controller, context, requests, stored: () => copy(stored), setSaveFailure: value => { failSave = value; }, setLoadFailure: value => { failLoad = value; } };
}

test("Reordering variable-height widgets preserves DOM order and existing widget content", async t => {
    let renders = 0;
    const { controller, host, window } = fixture(t, {
        items: [item("large", "counted", "3x3"), item("small", "test", "1x1"), item("full", "test", "fullauto"), item("last")],
        definitions: [{ type: "counted", titleKey: "Counted", sizes: ["3x3"], render: ({ container }) => { renders++; container.append(window.document.createElement("input")); } }]
    });
    await controller.start();
    const originalCard = host.querySelector('[data-instance-id="large"]');
    const originalInput = originalCard.querySelector("input");
    const alerts = window.document.querySelector("#alerts");
    originalInput.value = "Work in progress";
    assert.equal(host.querySelectorAll(".md-dashboard__grid").length, 2);
    assert.equal(host.querySelectorAll(".md-dashboard__full").length, 1);
    await controller.moveBefore("large", "last");
    assert.deepEqual([...host.querySelectorAll("[data-instance-id]")].map(card => card.dataset.instanceId), ["small", "full", "large", "last"]);
    assert.equal(host.querySelector('[data-instance-id="large"]'), originalCard);
    assert.equal(originalInput.value, "Work in progress");
    assert.equal(renders, 1, "Moving a card must not reload its data or reset its content");
    assert.equal(window.document.querySelector("#alerts"), alerts, "Widget edits must preserve system alerts");
});

test("Widget translations forward interpolation parameters to the shared translator", t => {
    const { controller } = fixture(t);
    controller.context.translate = (key, count) => `${key}:${count}`;
    assert.equal(controller._widgetContext().translate('admin.dashboard.trafficSessions.js', 30), 'admin.dashboard.trafficSessions.js:30');
});

test("Defaults are used only for an unconfigured profile and mandatory widgets survive an empty profile", async t => {
    const empty = fixture(t, { items: [], configured: true, defaults: [{ type: "test" }] });
    await empty.controller.start();
    assert.equal(empty.controller.settings.items.length, 0, "An intentionally empty profile must not restore optional defaults");
    const initial = fixture(t, {
        configured: false, defaults: [{ type: "test", options: { name: "Default" } }],
        definitions: [{ type: "sessions", titleKey: "Sessions", sizes: ["2x3"], mandatory: true, render() {} }]
    });
    await initial.controller.start();
    assert.deepEqual(copy(initial.controller.settings.items.map(value => value.type)), ["test", "sessions"]);
    assert.equal(await initial.controller.remove(initial.controller.settings.items[1].id), false);
    assert.equal(initial.requests.length, 1, "Mandatory removal must not reach the server");
});

test("Header navigation survives loading, title changes, collapse and edit controls", async t => {
    let finish;
    const { controller, host, window } = fixture(t, {
        items: [item("traffic", "linked-title", "2x2", { title: "Traffic" }), item("pages", "linked-action")],
        definitions: [
            { type: "linked-title", titleKey: "Traffic", headerLink: { href: "/apps/stat/admin/" },
                getTitle: instance => instance.options.title, render: () => new Promise(resolve => { finish = resolve; }) },
            { type: "linked-action", titleKey: "Recent pages", headerLink: { href: "/admin/v9/webpages/web-pages-list/", labelKey: "admin.dashboard.allShort.js" }, render() {} }
        ]
    });
    window.WJ.translate = key => key === "admin.dashboard.allShort.js" ? "All" : key;
    await controller.start();
    const titleLink = host.querySelector('[data-instance-id="traffic"] .md-dashboard__title-link');
    const actionLink = host.querySelector('[data-instance-id="pages"] .md-dashboard__header-link');
    assert.equal(titleLink.getAttribute("href"), "/apps/stat/admin/");
    assert.equal(titleLink.textContent, "Traffic");
    assert.equal(actionLink.getAttribute("href"), "/admin/v9/webpages/web-pages-list/");
    assert.equal(actionLink.textContent, "All");
    assert.equal(titleLink.closest("section").querySelector('[aria-busy="true"]') !== null, true, "Navigation is available before widget data arrives");
    assert.equal(titleLink.querySelector(".ti-arrow-up-right").getAttribute("aria-hidden"), "true");
    finish();
    await tick();
    titleLink.focus();
    await controller.saveOptions("traffic", { options: { title: "<strong>Page views</strong>" } });
    assert.equal(host.querySelector('[data-instance-id="traffic"] .md-dashboard__title-link'), titleLink);
    assert.equal(window.document.activeElement, titleLink, "Updating the card preserves link focus");
    assert.equal(titleLink.textContent, "<strong>Page views</strong>");
    assert.equal(titleLink.querySelector("strong"), null, "Dynamic titles remain text");
    assert.ok(titleLink.querySelector(".ti-arrow-up-right"), "Updating title text must not erase the navigation icon");
    finish();
    await controller.updateInstance("traffic", { collapsed: true });
    await controller.updateInstance("pages", { collapsed: true });
    assert.equal(titleLink.closest("section").querySelector(".md-dashboard__widget-body").hidden, true);
    assert.equal(host.querySelector('[data-instance-id="pages"] .md-dashboard__header-link'), actionLink);
    controller.setEditing(true);
    assert.equal(actionLink.closest(".md-dashboard__widget-header").querySelector(".md-dashboard__widget-controls").hidden, false);
    assert.equal(actionLink.closest(".md-dashboard__edit-control"), null, "Module navigation stays independent of arrangement controls");
});

test("Header navigation rejects unsafe and external destinations through the shared link helper", async t => {
    const { controller, host } = fixture(t, {
        items: [item("unsafe", "unsafe-link"), item("external", "external-link")],
        definitions: [
            { type: "unsafe-link", titleKey: "Unsafe", headerLink: { href: "javascript:alert(1)" }, render() {} },
            { type: "external-link", titleKey: "External", headerLink: { href: "https://example.org/", labelKey: "All" }, render() {} }
        ]
    });
    await controller.start();
    assert.equal(host.querySelectorAll(".md-dashboard__widget-header a").length, 0);
    assert.equal(host.querySelector(".md-dashboard__title-link").textContent, "Unsafe");
    assert.equal(host.querySelector(".md-dashboard__header-link").textContent, "All");
});

test("Reset clears personal preferences and disposes replaced widgets only after server confirmation", async t => {
    let disposed = 0;
    let signal;
    const { controller, host, window, requests, stored } = fixture(t, {
        items: [item("custom", "resource"), item("removable")], defaults: [{ type: "test", size: "3x3" }],
        definitions: [
            { type: "resource", titleKey: "Resource", render: values => { signal = values.signal; return () => disposed++; } },
            { type: "sessions", titleKey: "Sessions", sizes: ["2x3"], mandatory: true, render() {} }
        ]
    });
    await controller.start();
    await tick();
    await controller.saveOptions("custom", { domainOptions: { formName: "Custom form" } });
    await controller.acknowledgeNews("2026.18");
    await controller.remove("removable");
    const previousDisposals = disposed;
    const alerts = window.document.querySelector("#alerts");
    assert.equal(await controller.reset(), true);
    assert.equal(requests.at(-1).method, "DELETE");
    assert.equal(requests.at(-1).headers["X-CSRF-Token"], "test-csrf-token");
    assert.equal(requests.at(-1).body, undefined, "Account and domain must never be supplied by the client");
    assert.equal(stored().configured, false);
    assert.equal(controller.settings.acknowledgedNewsVersion, null);
    assert.equal(controller.settings.domainOptions.custom, undefined);
    assert.equal(controller.removed, null);
    assert.equal(signal.aborted, true);
    assert.equal(disposed, previousDisposals + 1);
    assert.equal(window.document.querySelector("#alerts"), alerts);
    assert.deepEqual(copy(controller.settings.items.map(value => [value.type, value.size])), [["test", "3x3"], ["sessions", "2x3"]]);
    assert.equal(host.querySelector('[data-instance-id="custom"]'), null);
    await controller.start();
    assert.deepEqual(copy(controller.settings.items.map(value => value.type)), ["test", "sessions"], "A reload reapplies defaults until the first personal edit");
    await controller.updateInstance(controller.settings.items[0].id, { collapsed: true });
    assert.equal(stored().configured, true);
    await controller.start();
    assert.equal(controller.settings.items[0].collapsed, true);
});

test("A failed reset preserves the layout, filters, acknowledged news and removal undo", async t => {
    const { controller, host } = fixture(t, { items: [item("kept"), item("removed")], defaults: [{ type: "test" }], failReset: true });
    await controller.start();
    await controller.saveOptions("kept", { domainOptions: { formName: "Contact" } });
    await controller.acknowledgeNews("2026.18");
    await controller.remove("removed");
    const settings = copy(controller.settings);
    const card = host.querySelector('[data-instance-id="kept"]');
    assert.equal(await controller.reset(), false);
    assert.deepEqual(copy(controller.settings), settings);
    assert.equal(host.querySelector('[data-instance-id="kept"]'), card);
    assert.equal(controller.removed.instance.id, "removed");
    assert.match(host.querySelector('[role="status"]').textContent, /previous settings/);
});

test("Catalogue reset explains its scope and waits for explicit confirmation", async t => {
    const { controller, window, requests } = fixture(t, { items: [item("custom")], defaults: [{ type: "test" }] });
    await controller.start();
    controller.showCatalogue();
    const dialog = window.document.querySelector('[role="dialog"]');
    dialog.querySelector(".md-dashboard__reset").click();
    assert.equal(requests.some(request => request.method === "DELETE"), false);
    const confirm = dialog.querySelector("button[aria-describedby]");
    assert.match(window.document.getElementById(confirm.getAttribute("aria-describedby")).textContent, /all domains/);
    assert.equal(window.document.activeElement, confirm);
    confirm.click();
    await tick();
    assert.equal(requests.at(-1).method, "DELETE");
    assert.equal(window.document.querySelector('[role="dialog"]'), null);
});

test("Shared options and domain filters refresh only the changed widget", async t => {
    const renders = { a: 0, b: 0 };
    const { controller, requests } = fixture(t, {
        items: [item("a", "counted"), item("b", "counted")],
        definitions: [{ type: "counted", titleKey: "Counted", multiple: true, render: ({ instance }) => { renders[instance.id]++; } }]
    });
    await controller.start();
    await controller.saveOptions("a", { options: { period: 7 }, domainOptions: { formName: "Contact" } });
    assert.deepEqual(renders, { a: 2, b: 1 });
    const payload = JSON.parse(requests.at(-1).body);
    assert.deepEqual(payload.items[0].options, { period: 7 });
    assert.deepEqual(payload.domainOptions.a, { formName: "Contact" });
    assert.equal(payload.userId, undefined);
    assert.equal(payload.domain, undefined);
    assert.equal(requests.at(-1).headers["X-CSRF-Token"], "test-csrf-token");
});

test("Removing and undoing restores the original instance, options, domain filters and position", async t => {
    const { controller, host } = fixture(t, { items: [item("a"), item("b", "test", "3x3", { title: "Custom" }), item("c")] });
    await controller.start();
    await controller.saveOptions("b", { domainOptions: { groupId: 123 } });
    await controller.remove("b");
    assert.equal(host.querySelector('[data-instance-id="b"]'), null);
    await controller.undoRemove();
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["a", "b", "c"]);
    assert.equal(controller.settings.items[1].size, "3x3");
    assert.equal(controller.settings.items[1].options.title, "Custom");
    assert.equal(controller.settings.domainOptions.b.groupId, 123);
});

test("A rejected save leaves the confirmed layout and content untouched", async t => {
    const { controller, host } = fixture(t, { items: [item("a"), item("b")], failSave: true });
    await controller.start();
    const card = host.querySelector('[data-instance-id="a"]');
    assert.equal(await controller.remove("a"), false);
    assert.equal(host.querySelector('[data-instance-id="a"]'), card);
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["a", "b"]);
    assert.match(host.querySelector('[role="status"]').textContent, /previous settings/);
});

test("Removal undo survives a failed save but expires after the next successful mutation", async t => {
    const { controller, host, setSaveFailure } = fixture(t, { items: [item("a"), item("b")] });
    await controller.start();
    await controller.remove("b");
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, false);
    setSaveFailure(true);
    assert.equal(await controller.updateInstance("a", { collapsed: true }), false);
    assert.equal(controller.removed.instance.id, "b");
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, false);
    setSaveFailure(false);
    assert.equal(await controller.updateInstance("a", { collapsed: true }), true);
    assert.equal(controller.removed, null);
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, true);
    assert.equal(await controller.undoRemove(), false);
});

test("Failed loading cannot overwrite a user's stored layout with defaults", async t => {
    const { controller, host, requests } = fixture(t, { configured: true, items: [item("stored")], defaults: [{ type: "test" }], failLoad: true });
    await controller.start();
    assert.equal(host.querySelector(".md-dashboard__toolbar button").disabled, true);
    assert.equal(controller.settings.items.length, 0);
    assert.equal(await controller.acknowledgeNews("2026.18"), false, "A release action must not overwrite unread preferences after a failed load");
    assert.equal(requests.some(request => request.method === "PUT"), false);
});

test("Unavailable types remain persisted while authorized instances render", async t => {
    const { controller, host, stored } = fixture(t, {
        items: [item("visible"), item("restricted", "restricted")],
        definitions: [{ type: "restricted", titleKey: "Restricted", isAvailable: () => false, render() { throw new Error("Restricted content must not render"); } }]
    });
    await controller.start();
    assert.equal(host.querySelectorAll("[data-instance-id]").length, 1);
    await controller.updateInstance("visible", { collapsed: true });
    assert.equal(stored().items.length, 2, "A changed permission must not erase the user's preferences");
});

test("A failed domain-context reload does not display the previous domain's widget data", async t => {
    const { controller, host, setLoadFailure } = fixture(t, {
        items: [item("domain", "domain")],
        definitions: [{ type: "domain", titleKey: "Domain", render: ({ container, context }) => { container.textContent = context.data?.name || "Original domain"; } }]
    });
    await controller.start();
    setLoadFailure(true);
    await controller.setContext({ data: { name: "New domain" }, config: {} });
    assert.equal(host.querySelector(".md-dashboard__layout").hidden, true);
    assert.equal(host.dataset.loaded, "false");
    setLoadFailure(false);
    await controller.start();
    assert.equal(host.querySelector(".md-dashboard__layout").hidden, false);
    assert.equal(host.querySelector(".md-dashboard__widget-content").textContent, "New domain");
});

test("Sessions remain expanded in the permanent header while stored preferences stay intact", async t => {
    let renders = 0;
    const session = { ...item("sessions", "sessions", "2x3"), collapsed: true };
    const { controller, host, requests, stored } = fixture(t, {
        items: [session, item("ordinary")],
        definitions: [{
            type: "sessions", titleKey: "Sessions", mandatory: true, sizes: ["2x3"],
            render: ({ container, instance }) => { renders++; assert.equal(instance.collapsed, false); container.textContent = "Active session details"; },
            renderCollapsed: () => assert.fail("Security details must not collapse")
        }]
    });
    await controller.start();
    assert.match(host.querySelector(".md-dashboard__sessions").textContent, /Active session details/);
    assert.equal(host.querySelector(".md-dashboard__layout [data-widget-type='sessions']"), null);
    assert.equal(host.querySelector(".md-dashboard__sessions .md-dashboard__widget-controls"), null);
    const count = requests.length;
    assert.equal(await controller.updateInstance("sessions", { collapsed: true }), false);
    assert.equal(await controller.remove("sessions"), false);
    assert.equal(await controller.moveBefore("sessions", "ordinary"), false);
    assert.equal(requests.length, count);
    await controller.updateInstance("ordinary", { collapsed: true });
    assert.equal(stored().items[0].collapsed, true, "The visual move must not silently rewrite old preferences");
    assert.equal(renders, 1, "Unrelated edits must not reload security data");
});

test("Late asynchronous rendering is cleaned up after removal even if it ignores abort", async t => {
    let finish;
    let destroyed = 0;
    let signal;
    const { controller } = fixture(t, {
        items: [item("late", "late")],
        definitions: [{ type: "late", titleKey: "Late", render: values => { signal = values.signal; return new Promise(resolve => { finish = resolve; }); } }]
    });
    await controller.start();
    await controller.remove("late");
    assert.equal(signal.aborted, true);
    finish({ destroy() { destroyed++; } });
    await tick();
    assert.equal(destroyed, 1);
});

test("A renderer failure stays inside its own card and exposes a retry", async t => {
    const { controller, host } = fixture(t, {
        items: [item("good"), item("bad", "bad")],
        definitions: [{ type: "bad", titleKey: "Bad", render() { throw new Error("Provider unavailable"); } }]
    });
    await controller.start();
    await tick();
    assert.equal(host.querySelector('[data-instance-id="good"] .md-dashboard__widget-content').textContent, "Widget content");
    assert.match(host.querySelector('[data-instance-id="bad"] .md-dashboard__widget-content').textContent, /could not be loaded/);
    assert.equal(host.querySelector('[data-instance-id="bad"] .md-dashboard__widget-content button').textContent, "Try again");
});

test("Multiple instances use different ids, singleton duplication is refused and the limit is enforced", async t => {
    const { controller, requests } = fixture(t, { definitions: [{ type: "single", titleKey: "Single", render() {} }] });
    await controller.start();
    assert.equal(await controller.add("single"), true);
    assert.equal(await controller.add("single"), false);
    await controller.add("test");
    await controller.add("test");
    const ids = controller.settings.items.map(value => value.id);
    assert.equal(new Set(ids).size, 3);
    controller.settings.items = Array.from({ length: 32 }, (_, index) => item(`item-${index}`));
    const before = requests.length;
    assert.equal(await controller.add("test"), false);
    assert.equal(requests.length, before);
});

test("Keyboard movement dialog offers a target and the end without coordinates", async t => {
    const { controller, host, window } = fixture(t, { items: [item("a"), item("b"), item("c")] });
    await controller.start();
    controller.setEditing(true);
    const move = host.querySelector('[data-instance-id="a"] .md-dashboard__drag');
    move.focus();
    move.click();
    const dialog = window.document.querySelector('[role="dialog"]');
    assert.ok(dialog);
    dialog.querySelector("select").value = "";
    dialog.querySelector(".modal-footer button").click();
    await tick();
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["b", "c", "a"]);
    assert.equal(window.document.querySelector('[role="dialog"]'), null);
    assert.equal(window.document.activeElement, move);
});

test("A menu action closes its dropdown before disabling controls for persistence", async t => {
    const { controller, host, window } = fixture(t, { items: [item("a")] });
    await controller.start();
    let hidden = false;
    window.bootstrap.Dropdown = { getInstance: () => ({ hide() {
        assert.equal(controller.saving, false, "Bootstrap must hide the menu before the toggle is disabled");
        hidden = true;
    }, dispose() {} }) };
    host.querySelector('[data-dashboard-action="collapse"]').click();
    assert.equal(hidden, true);
    await tick();
    assert.equal(controller.settings.items[0].collapsed, true);
});

test("A settings dialog disposed during async initialization releases the late result", async t => {
    let finish;
    let cleanup = 0;
    const { controller, window } = fixture(t, {
        items: [item("configured", "configured")],
        definitions: [{ type: "configured", titleKey: "Configured", render() {}, configure: () => new Promise(resolve => { finish = resolve; }) }]
    });
    await controller.start();
    const opening = controller.showSettings("configured");
    window.document.querySelector(".modal-header button").click();
    finish({ read: () => ({}), destroy: () => cleanup++ });
    await opening;
    assert.equal(cleanup, 1);
    assert.equal(window.document.querySelector('[role="dialog"]'), null);
});

test("Closing a dialog during its opening transition completes after it is shown", async t => {
    const { controller, window } = fixture(t, { deferModalShown: true });
    const dialog = controller.showDialog("Opening autotest dialog");
    dialog.close();
    await tick();
    assert.equal(window.document.querySelector('[role="dialog"]'), null);
    assert.equal(dialog.signal.aborted, true);
});

test("Finishing the opening transition preserves focus in an already edited field", async t => {
    const { controller, window } = fixture(t, { deferModalShown: true });
    const dialog = controller.showDialog("Focus autotest dialog");
    const input = window.document.createElement("input");
    dialog.body.append(input);
    input.focus();
    input.value = "autotest title";
    await tick();
    assert.equal(window.document.activeElement, input, "Bootstrap focus activation must not interrupt typing");
    assert.equal(input.value, "autotest title");
});

test("Acknowledged news stays in the header and refreshes only its own content", async t => {
    let newsRenders = 0;
    let otherRenders = 0;
    const { controller, host, window, stored } = fixture(t, {
        items: [item("news", "news"), item("ordinary", "counted")],
        definitions: [{
            type: "news", titleKey: "News",
            render: ({ container, context }) => { newsRenders++; container.textContent = context.settings.acknowledgedNewsVersion ? "Release summary" : "Release news"; }
        }, { type: "counted", titleKey: "Counted", render: () => { otherRenders++; } }]
    });
    await controller.start();
    const card = host.querySelector('[data-instance-id="news"]');
    await controller.acknowledgeNews("2026.18");
    assert.equal(host.querySelector('.md-dashboard__news [data-instance-id="news"]'), card);
    assert.equal(stored().items[0].id, "news");
    assert.match(card.textContent, /Release summary/);
    assert.equal(newsRenders, 2);
    assert.equal(otherRenders, 1);
    controller.showCatalogue();
    assert.equal(window.document.querySelector('.md-dashboard__catalogue-item[data-widget-type="news"]'), null);
    await controller.acknowledgeNews(null);
    assert.match(card.textContent, /Release news/);
});

test("Edit mode reveals arrangement controls without a settings mutation", async t => {
    const { controller, host, requests } = fixture(t, { items: [item("ordinary")] });
    await controller.start();
    const controls = [...host.querySelectorAll('.md-dashboard__edit-control')];
    assert.ok(controls.length >= 2);
    assert.ok(controls.every(control => control.hidden));
    controller.editButton.click();
    assert.equal(controller.editButton.getAttribute('aria-pressed'), 'true');
    assert.ok(controls.every(control => !control.hidden));
    controller.editButton.click();
    assert.ok(controls.every(control => control.hidden));
    assert.equal(requests.length, 1);
});

test("Shortcuts configure before saving and render in the permanent shortcut strip", async t => {
    const { controller, host, window, requests, stored } = fixture(t, {
        definitions: [{ type: "shortcut", titleKey: "Shortcut", sizes: ["1x1"], multiple: true,
            configure: ({ container }) => {
                const input = window.document.createElement('input'); container.append(input);
                return { read: () => ({ options: { href: '/admin/v9/users/', title: input.value } }) };
            }, render: ({ container, options }) => { container.textContent = options.title; }
        }]
    });
    await controller.start();
    await controller.showAddWidget('shortcut');
    window.document.querySelector('.modal-header button').click();
    assert.equal(requests.length, 1, 'Cancelling must not save an empty shortcut');
    await controller.showAddWidget('shortcut');
    window.document.querySelector('.md-dashboard__settings input').value = 'My users';
    window.document.querySelector('.modal-footer button').click();
    await tick();
    assert.equal(stored().items.length, 1);
    assert.equal(stored().items[0].options.title, 'My users');
    assert.match(host.querySelector('.md-dashboard__shortcut-list').textContent, /My users/);
    assert.equal(host.querySelector('.md-dashboard__layout [data-widget-type="shortcut"]'), null);
});

test("Security remains available when loading personal preferences fails", async t => {
    const { controller, host } = fixture(t, { failLoad: true,
        definitions: [{ type: 'sessions', titleKey: 'Sessions', mandatory: true, render: ({ container }) => { container.textContent = 'Current browser session'; } }]
    });
    await controller.start();
    assert.match(host.querySelector('.md-dashboard__sessions').textContent, /Current browser session/);
    assert.equal(controller.editButton.disabled, true);
});

/** Loads the overview component against the existing DOM without starting the application shell. */
function overviewFixture(t) {
    const environment = fixture(t);
    const { context, window, controller } = environment;
    Object.assign(context, { HTMLElement: window.HTMLElement, customElements: window.customElements, WJ: window.WJ });
    const source = fs.readFileSync(path.join(moduleDirectory, '../web-components/webjet-overview-dashboard.js'), 'utf8')
        .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context, { filename: 'webjet-overview-dashboard.js' });
    const overview = window.document.createElement('webjet-overview-dashboard');
    overview.dashboardController = controller;
    return { ...environment, overview };
}

test('System notices remain independent accordions and invoke their own authorized actions', async t => {
    const { context, window, controller, overview } = overviewFixture(t);
    const actions = [];
    window.WJ.openPopupDialog = url => actions.push(['popup', url]);
    window.WJ.showHelpWindow = url => actions.push(['help', url]);
    const requests = [];
    context.fetch = async (url, options) => {
        requests.push({ url, options });
        return { ok: true, json: async () => [
            { id: 'two-factor', severity: 'warning', icon: 'ti-shield', title: 'Enable verification', bodyHtml: '<p>Protect your account.</p>', action: { type: 'popup', url: '/admin/2factorauth.jsp', label: 'Configure' } },
            { id: 'ses', severity: 'warning', icon: 'ti-mail', title: 'Configure email', bodyHtml: '<p>Set up sending.</p>', action: { type: 'help', url: '/install/config/README', label: 'Read guide' } }
        ] };
    };
    const customNotice = window.document.createElement('div');
    customNotice.textContent = 'External application warning';
    controller.notices.append(customNotice);
    await overview._loadNotices();
    assert.equal(customNotice.parentElement, controller.notices, 'Async system notices must preserve externally supplied notifications');
    assert.equal(requests[0].url, '/admin/rest/dashboard/notices');
    assert.equal(requests[0].options.headers['X-CSRF-Token'], 'test-csrf-token');
    const details = [...controller.notices.querySelectorAll('details')];
    assert.equal(details.length, 2);
    assert.deepEqual(details.map(notice => notice.querySelector('summary').textContent), ['Enable verification', 'Configure email']);
    details[0].open = true;
    assert.equal(details[1].open, false);
    details[0].querySelector('button').click();
    details[1].querySelector('button').click();
    assert.deepEqual(actions, [['popup', '/admin/2factorauth.jsp'], ['help', '/install/config/README']]);
    await controller.start();
    assert.equal(controller.notices.querySelectorAll('details').length, 2, 'Rendering personal layout must preserve system warnings');
});

test('Failed notice checks stay visible and offer an independent retry', async t => {
    const { context, controller, overview } = overviewFixture(t);
    context.fetch = async () => ({ ok: false, status: 503 });
    await overview._loadNotices();
    assert.ok(controller.notices.querySelector('[role="alert"]'));
    assert.equal(controller.notices.querySelector('.md-dashboard__notice-list').getAttribute('aria-busy'), 'false');
    context.fetch = async () => ({ ok: true, json: async () => [] });
    controller.notices.querySelector('button').click();
    await tick();
    assert.equal(controller.notices.querySelector('.md-dashboard__notice-list').children.length, 0);
});

test('Release note persistence restores the replacement toggle without stealing focus from another control', async t => {
    const { context, window, host } = fixture(t);
    Object.assign(context, { DOMParser: window.DOMParser });
    const source = fs.readFileSync(path.join(moduleDirectory, 'utility-widgets.js'), 'utf8')
        .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context, { filename: 'utility-widgets.js' });
    context.registerUtilityWidgets();
    const news = context.getWidget('news');
    const region = window.document.createElement('section');
    region.dataset.widgetType = 'news';
    const otherControl = window.document.createElement('input');
    host.append(region, otherControl);
    let finishSave;
    const widgetContext = {
        translate: key => key,
        labels: { changelog: '<p>WebJET CMS 2026.18</p><p>Release highlights</p>' },
        config: { releaseVersion: '2026.18' }, settings: { acknowledgedNewsVersion: null },
        dashboard: { acknowledgeNews: () => new Promise(resolve => { finishSave = resolve; }) }
    };
    const render = () => {
        const container = window.document.createElement('div');
        region.replaceChildren(container);
        news.render({ container, context: widgetContext });
        return region.querySelector('.md-dashboard-widget__news-toggle');
    };
    for (const moveFocusElsewhere of [false, true]) {
        const toggle = render();
        toggle.focus();
        toggle.click();
        assert.equal(toggle.disabled, true);
        toggle.blur(); // Browsers blur a focused button when persistence disables it.
        if (moveFocusElsewhere) otherControl.focus();
        const replacement = render();
        finishSave(true);
        await tick();
        assert.equal(window.document.activeElement, moveFocusElsewhere ? otherControl : replacement,
            'Saving must restore lost toggle focus while respecting a deliberate focus change.');
    }
});
