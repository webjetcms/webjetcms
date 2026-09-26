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
    const context = vm.createContext({ window, document: window.document, CustomEvent: window.CustomEvent, AbortController, fetch, console, crypto: require("node:crypto").webcrypto });
    for (const filename of ["registry.js", "model.js", "dashboard.js"]) {
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

test("Collapsing disposes active work and the session summary retains its management action", async t => {
    let cleanup = 0;
    let signal;
    const { controller, host, window } = fixture(t, {
        items: [item("sessions", "sessions", "2x3")],
        definitions: [{
            type: "sessions", titleKey: "Sessions", mandatory: true, sizes: ["2x3"],
            render: values => { signal = values.signal; return () => cleanup++; },
            renderCollapsed: ({ container }) => { const link = window.document.createElement("a"); link.href = "/admin/v9/users/self/"; link.textContent = "Manage sessions"; container.append(link); }
        }]
    });
    await controller.start();
    await tick();
    await controller.updateInstance("sessions", { collapsed: true });
    assert.equal(signal.aborted, true);
    assert.equal(cleanup, 1);
    assert.equal(host.querySelector(".md-dashboard__widget-body a").textContent, "Manage sessions");
    assert.equal(host.querySelector(".md-dashboard__widget-body").hidden, false);
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

test("Acknowledged news disappears without losing its instance and can be revealed from the catalogue", async t => {
    let cleanup = 0;
    const { controller, host, window, stored } = fixture(t, {
        items: [item("news", "news")],
        definitions: [{
            type: "news", titleKey: "News", isVisible: (instance, context) => context.settings.acknowledgedNewsVersion !== "2026.18",
            reveal: (instance, context) => context.dashboard.acknowledgeNews(null),
            render: ({ container }) => { container.textContent = "Release news"; return () => cleanup++; }
        }]
    });
    await controller.start();
    await tick();
    await controller.acknowledgeNews("2026.18");
    assert.equal(host.querySelector('[data-instance-id="news"]'), null);
    assert.equal(stored().items[0].id, "news", "Acknowledgement must preserve the user's selected widget");
    assert.equal(cleanup, 1);
    controller.showCatalogue();
    const reveal = window.document.querySelector('.md-dashboard__catalogue-item[data-widget-type="news"] button');
    assert.equal(reveal.textContent, "Show");
    reveal.click();
    await tick();
    assert.ok(host.querySelector('[data-instance-id="news"]'));
    assert.equal(stored().acknowledgedNewsVersion, null);
});
