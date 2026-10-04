const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { JSDOM } = require("jsdom");

const moduleDirectory = path.resolve(__dirname, "../../../main/webapp/admin/v9/src/js/dashboard");
const copy = value => JSON.parse(JSON.stringify(value));
const tick = () => new Promise(resolve => setImmediate(resolve));
const item = (id, type = "test", size = "2x2", options = {}) => ({ id, type, size, options });

test('Dialog headings retain their button and action when the dashboard reconciles views', async t => {
    let opened = 0;
    const { controller, host } = fixture(t, { items: [item('sessions-autotest', 'dialog-test')], definitions: [{ type: 'dialog-test',
        headerAction: () => opened++, render() {} }] });
    await controller.start();
    const heading = host.querySelector('.md-dashboard__title-action');
    assert.equal(heading.tagName, 'BUTTON');
    heading.click();
    controller._render();
    assert.equal(host.querySelector('.md-dashboard__title-action'), heading);
    heading.click();
    assert.equal(opened, 2);
});

/** Runs production browser modules against a DOM and a stateful settings server. */
function fixture(t, { items = [], configured = true, shortcutsConfigured = configured, legacyBookmarksHandled = false, definitions = [], defaults = [], config = {}, failSave = false, failReset = false, deferModalShown = false, realBootstrap = false, withTooltip = false, overview, IntersectionObserver } = {}) {
    const dom = new JSDOM("<!doctype html><html><body><div id='alerts'>System warning</div><div id='dashboard'></div></body></html>", { url: "http://localhost/admin/v9/", runScripts: realBootstrap ? "outside-only" : undefined });
    const { window } = dom;
    window.IntersectionObserver = IntersectionObserver;
    const notifications = [];
    const confirmations = [];
    window.WJ = {
        translate: key => key,
        notifySuccess: (...args) => notifications.push(args),
        notify: (type, title, message, timeout, buttons, append, id) => {
            const toast = window.document.createElement('div');
            toast.className = 'toast';
            toast.textContent = title;
            toast.dataset.timeout = timeout;
            window.document.getElementById(id).append(toast);
        },
        confirm: options => confirmations.push({ options, trigger: window.document.activeElement }),
        focusWithoutTooltip: element => element.focus({ preventScroll: true })
    };
    const closeConfirmation = () => {
        const confirmation = confirmations.at(-1);
        confirmation.options.onHidden?.();
        window.WJ.focusWithoutTooltip(confirmation.trigger);
    };
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
    if (realBootstrap) window.eval(fs.readFileSync(path.resolve(moduleDirectory, "../../../node_modules/bootstrap/dist/js/bootstrap.bundle.js"), "utf8"));
    const tooltipCalls = [];
    if (withTooltip) {
        const instances = new Map();
        window.jQuery = element => ({ 0: element, off: namespace => tooltipCalls.push(["off", namespace]) });
        window.jQuery.fn = {};
        window.WJ.initTooltip = wrapped => {
            tooltipCalls.push(["init", wrapped[0]]);
            instances.set(wrapped[0], {
                enabled: true, visible: false,
                enable() { this.enabled = true; tooltipCalls.push(["enable"]); },
                disable() { this.enabled = false; tooltipCalls.push(["disable"]); },
                hide() { this.visible = false; tooltipCalls.push(["hide"]); },
                show() { if (this.enabled) this.visible = true; },
                dispose() { tooltipCalls.push(["dispose"]); instances.delete(wrapped[0]); }
            });
        };
        window.bootstrap.Tooltip = { getInstance: element => instances.get(element) };
        window.WJ.focusWithoutTooltip = element => {
            tooltipCalls.push(["focusWithoutTooltip", element]);
            const tooltip = instances.get(element);
            tooltip?.disable();
            tooltip?.hide();
            element.focus({ preventScroll: true });
        };
    }
    const requests = [];
    let stored = { version: 1, configured, shortcutsConfigured, legacyBookmarksHandled, items: copy(items), domainOptions: {}, acknowledgedNewsVersion: null };
    const fetch = async (url, options = {}) => {
        requests.push({ url, ...options });
        if (url.endsWith("/settings/reset")) {
            if (failReset) return { ok: false, status: 503 };
            const layout = JSON.parse(options.body);
            stored = { ...layout, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: stored.legacyBookmarksHandled,
                items: [...stored.items.filter(item => item.type === "shortcut"), ...layout.items.filter(item => item.type !== "shortcut")] };
        } else if (options.method === "PUT") {
            if (failSave) return { ok: false, status: 500 };
            stored = { ...JSON.parse(options.body), configured: true, shortcutsConfigured: true };
        } else if (options.method === "DELETE") {
            if (failReset) return { ok: false, status: 503 };
            stored = { version: 1, configured: false, shortcutsConfigured: stored.shortcutsConfigured, legacyBookmarksHandled: stored.legacyBookmarksHandled, items: stored.items.filter(item => item.type === "shortcut"), domainOptions: {}, acknowledgedNewsVersion: null };
        }
        return { ok: true, json: async () => copy(stored) };
    };
    const context = vm.createContext({ window, document: window.document, CustomEvent: window.CustomEvent, URL: window.URL, AbortController, fetch, console, crypto: require("node:crypto").webcrypto });
    for (const filename of ["registry.js", "model.js", "widget-utils.js", "widget-colors.js", "editor.js", "dashboard.js"]) {
        const source = fs.readFileSync(path.join(moduleDirectory, filename), "utf8").replace(/^import .+;\r?$/gm, "").replace(/^export /gm, "");
        vm.runInContext(source, context, { filename });
    }
    vm.runInContext("this.Controller = DashboardController", context);
    context.registerWidget({ type: "test", titleKey: "Test", sizes: ["1x1", "2x2", "2x3", "3x2", "3x3", "fullauto"], multiple: true, render: ({ container }) => { container.textContent = "Widget content"; } });
    definitions.forEach(definition => context.registerWidget(definition));
    const host = window.document.querySelector("#dashboard");
    const controller = new context.Controller(host, { data: { settings: copy(stored) }, config: { dashboardDefaults: defaults, ...config }, overview });
    t.after(() => { controller.destroy(); window.close(); });
    return { window, host, controller, context, requests, tooltipCalls, notifications, confirmations, closeConfirmation, setResetFailure: value => { failReset = value; }, stored: () => copy(stored), setSaveFailure: value => { failSave = value; } };
}

/** Controls viewport entry independently of jsdom's missing layout engine. */
function observedFixture(t, options) {
    const targets = new Set();
    let callback, disconnected = false;
    const environment = fixture(t, { ...options, IntersectionObserver: class {
        constructor(handler) { callback = handler; }
        observe(card) { targets.add(card); }
        unobserve(card) { targets.delete(card); }
        disconnect() { targets.clear(); disconnected = true; }
    } });
    return { ...environment, targets, disconnected: () => disconnected,
        intersect: (card, isIntersecting = true) => callback([{ target: card, isIntersecting }]) };
}

test('Embedded settings render immediately and context changes use supplied preferences', async t => {
    const { controller, host, requests, stored } = fixture(t, { items: [item('embedded')] });
    const ready = controller.start();
    assert.equal(host.dataset.loaded, 'true');
    assert.ok(controller.views.has('embedded'));
    assert.equal(requests.length, 0);
    await ready;
    await controller.saveOptions('embedded', { options: { title: 'Saved autotest' } });
    assert.equal(requests[0].method, 'PUT');
    await controller.setContext({ data: { settings: stored() }, config: { domain: 'another.example' } });
    assert.equal(requests.length, 1);
    assert.equal(controller.settings.items[0].options.title, 'Saved autotest');
});

test('Embedded unconfigured settings apply defaults without reading REST preferences', async t => {
    const { controller, requests } = fixture(t, { configured: false, defaults: [{ type: 'test', size: '2x2' }] });
    await controller.start();
    assert.equal(controller.settings.items.length, 1);
    assert.equal(controller.settings.items[0].type, 'test');
    assert.equal(requests.length, 0);
});

test('Header image configuration binds local and HTTP URLs without accepting executable CSS or URL schemes', t => {
    const fallback = fixture(t);
    assert.equal(fallback.controller.hero.style.getPropertyValue('--wj-dashboard-hero-image'), '', 'An omitted setting must preserve the stylesheet image default');
    for (const [value, expected] of [
        ['/images/company/header.jpg', 'url("/images/company/header.jpg")'],
        ['/images/company/header wide.jpg', 'url("/images/company/header%20wide.jpg")'],
        ['https://cdn.example.test/header.jpg?theme=blue', 'url("https://cdn.example.test/header.jpg?theme=blue")'],
        ['/images/header");color:red;/*', 'url("/images/header%22);color:red;/*")']
    ]) {
        const { controller } = fixture(t, { config: { heroBackgroundImage: value } });
        assert.equal(controller.hero.style.getPropertyValue('--wj-dashboard-hero-image'), expected);
        assert.equal(controller.hero.style.color, '', 'A configured image must not inject a separate CSS declaration');
    }
    for (const value of ['', '   ', null, 'javascript:alert(1)', 'data:image/svg+xml,<svg/>', '//example.test/header.jpg', 'https://user:secret@example.test/header.jpg', '/images/hero\\image.jpg', '/images/hero\nimage.jpg']) {
        const { controller } = fixture(t, { config: { heroBackgroundImage: value } });
        assert.equal(controller.hero.style.getPropertyValue('--wj-dashboard-hero-image'), 'none', 'Empty or invalid configuration must disable the image');
    }
});

test("Grid widgets wait for viewport entry while fixed utilities render immediately", async t => {
    const renders = [];
    const { controller, targets, intersect } = observedFixture(t, {
        items: [item("first", "lazy"), item("second", "lazy")],
        definitions: [
            { type: "lazy", titleKey: "Lazy", sizes: ["2x2"], multiple: true, render: ({ instance }) => renders.push(instance.id) },
            { type: "sessions", titleKey: "Sessions", sizes: ["2x3"], render: () => renders.push("sessions") }
        ]
    });
    await controller.start();
    assert.deepEqual(renders, ["sessions"]);
    assert.equal(targets.size, 2);
    const first = controller.views.get("first");
    intersect(first.card, false);
    await tick();
    assert.deepEqual(renders, ["sessions"], "Offscreen cards must not invoke their data renderers");
    intersect(first.card);
    await tick();
    assert.deepEqual(renders, ["sessions", "first"]);
    assert.equal(first.body.getAttribute("aria-busy"), "false");
    assert.equal(targets.has(first.card), false);
    intersect(first.card, false);
    intersect(first.card);
    await controller.moveBefore("first", null);
    assert.deepEqual(renders, ["sessions", "first"], "Scrolling or moving loaded cards must not reload their data");
    const refresh = controller.refresh("first");
    intersect(first.card);
    await refresh;
    assert.deepEqual(renders, ["sessions", "first", "first"], "Explicit refresh still loads once visible");
    assert.equal(targets.size, 1, "The other card must remain deferred");
});

test("Deferred renders use the latest configuration when the card enters the viewport", async t => {
    const periods = [];
    const { controller, targets, intersect } = observedFixture(t, {
        items: [item("lazy", "lazy", "2x2", { days: 7 })],
        definitions: [{ type: "lazy", titleKey: "Lazy", sizes: ["2x2"], render: ({ options }) => periods.push(options.days) }]
    });
    await controller.start();
    const view = controller.views.get("lazy");
    const originalSignal = view.abort.signal;
    await controller.saveOptions("lazy", { options: { days: 30 } });
    assert.equal(originalSignal.aborted, true);
    assert.deepEqual(periods, []);
    assert.equal(targets.size, 1);
    assert.equal(view.body.hidden, false);
    assert.equal(view.body.getAttribute("aria-busy"), "true");
    intersect(view.card);
    await tick();
    assert.deepEqual(periods, [30]);
});

test("Domain changes, removal and destruction cancel pending viewport work", async t => {
    const domains = [];
    const { controller, targets, intersect, disconnected } = observedFixture(t, {
        items: [item("lazy", "lazy")],
        definitions: [{ type: "lazy", titleKey: "Lazy", sizes: ["2x2"], render: ({ context }) => domains.push(context.config.domain) }]
    });
    await controller.start();
    const view = controller.views.get("lazy");
    const originalSignal = view.abort.signal;
    await controller.setContext({ data: controller.context.data, config: { domain: "new-domain" } });
    assert.equal(originalSignal.aborted, true);
    assert.deepEqual(domains, []);
    intersect(view.card);
    await tick();
    assert.deepEqual(domains, ["new-domain"]);
    const pending = controller.refresh("lazy");
    await controller.remove("lazy");
    await pending;
    intersect(view.card);
    await tick();
    assert.deepEqual(domains, ["new-domain"], "Stale observer entries must not revive removed widgets");
    assert.equal(targets.size, 0);
    await controller.undoRemove();
    const restored = controller.views.get("lazy");
    const pendingDestroy = controller.refresh("lazy");
    controller.destroy();
    await pendingDestroy;
    intersect(restored.card);
    assert.equal(restored.abort.signal.aborted, true);
    assert.equal(disconnected(), true);
    assert.deepEqual(domains, ["new-domain"]);
});

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
    assert.equal(initial.requests.length, 0, "Mandatory removal must not reach the server");
});

test("Legacy minimized widgets render their full content and retain size, order and filters", async t => {
    const legacy = [
        { ...item("first", "test", "3x3", { days: 30 }), collapsed: true },
        { ...item("second", "test", "2x2"), collapsed: false }
    ];
    const { controller, host, requests, stored } = fixture(t, { items: legacy });
    await controller.start();
    assert.deepEqual(copy(controller.settings.items), legacy.map(({ collapsed, ...instance }) => instance));
    assert.equal(stored().items[0].collapsed, true, "Loading must not write to the stored profile");
    assert.equal(requests.length, 0);
    assert.equal(host.querySelector('[data-dashboard-action="collapse"], .is-collapsed'), null);
    for (const body of host.querySelectorAll('.md-dashboard__widget-body')) {
        assert.equal(body.hidden, false);
        assert.equal(body.textContent, "Widget content");
    }
    await controller.saveOptions("first", { domainOptions: { formName: "Contact" } });
    assert.deepEqual(stored().items, legacy.map(({ collapsed, ...instance }) => instance));
    assert.deepEqual(stored().domainOptions.first, { formName: "Contact" });
    assert.equal(legacy[0].collapsed, true, "Normalization must not mutate its input");
});

test("Header navigation survives loading, title changes and edit controls", async t => {
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
    assert.equal(host.querySelector('[data-instance-id="pages"] .md-dashboard__header-link'), actionLink);
    controller.setEditing(true);
    assert.equal(actionLink.closest(".md-dashboard__widget-header").querySelector(".md-dashboard__widget-controls").hidden, false);
    assert.equal(actionLink.closest(".md-dashboard__edit-control"), null, "Module navigation stays independent of arrangement controls");
});

test("Header destinations follow domain settings and validate every update", async t => {
    const { controller, host, window } = fixture(t, {
        items: [item("forms", "linked-form")],
        definitions: [{ type: "linked-form", titleKey: "Form", headerLink: {
            href: (instance, context) => context.settings.domainOptions[instance.id]?.href || "/apps/form/admin/"
        }, render() {} }]
    });
    await controller.start();
    const titleLink = host.querySelector('.md-dashboard__title-link');
    titleLink.focus();
    await controller.saveOptions("forms", { domainOptions: { href: "/apps/form/admin/detail/?formName=Contact" } });
    assert.equal(titleLink.getAttribute("href"), "/apps/form/admin/detail/?formName=Contact");
    assert.equal(window.document.activeElement, titleLink);
    await controller.saveOptions("forms", { domainOptions: { href: "javascript:alert(1)" } });
    assert.equal(host.querySelector('.md-dashboard__title-link').tagName, "SPAN");
    await controller.saveOptions("forms", { domainOptions: { href: "/apps/form/admin/" } });
    assert.equal(host.querySelector('.md-dashboard__title-link').tagName, "A");
    assert.equal(host.querySelector('.md-dashboard__title-link').textContent, "Form");
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
    await controller.saveOptions(controller.settings.items[0].id, { options: { days: 30 } });
    assert.equal(stored().configured, true);
    await controller.start();
    assert.equal(controller.settings.items[0].options.days, 30);
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
    assert.match(controller.status.textContent, /previous settings/);
});

test("The editing toolbar delegates the full reset scope to standard confirmation", async t => {
    const { controller, host, window, requests, confirmations, closeConfirmation } = fixture(t, { items: [item("custom")], defaults: [{ type: "test" }] });
    await controller.start();
    const reset = host.querySelector('.md-dashboard__toolbar-actions .md-dashboard__reset');
    assert.equal(reset.hidden, true);
    assert.equal(reset.nextElementSibling, controller.addButton);
    assert.equal(controller.addButton.nextElementSibling, controller.cancelButton);
    assert.equal(controller.cancelButton.nextElementSibling, controller.editButton);
    assert.match(reset.title, /standard widgets, sizes and order/);
    controller.setEditing(true);
    assert.equal(reset.hidden, false);
    reset.click();
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
    assert.equal(confirmations.length, 1);
    const { options, trigger } = confirmations[0];
    assert.equal(trigger, reset);
    assert.equal(options.title, 'Restore defaults');
    assert.equal(options.btnOkText, 'Restore defaults');
    assert.match(options.message, /all domains/);
    assert.match(options.message, /read news/);
    assert.equal(requests.some(request => request.method === "DELETE"), false);
    assert.equal(await options.success(), true);
    closeConfirmation();
    assert.equal(requests.length, 0, "Confirmed reset remains provisional until Save");
    assert.equal(window.document.activeElement, reset);
    await controller.saveEditing();
    assert.equal(requests.at(-1).url, "/admin/rest/dashboard/settings/reset");
});

test("Shift reset confirms and persists every authorized size, supports removal and undo, and rolls back failures", async t => {
    const { controller, window, host, confirmations, requests, stored, setResetFailure, closeConfirmation } = fixture(t, {
        items: [item("old"), item("link", "shortcut", "1x1", { href: "/custom/", title: "Custom" })],
        legacyBookmarksHandled: true, failReset: true,
        definitions: [
            { type: "sessions", sizes: ["2x3"], mandatory: true, render() {} },
            { type: "shortcut", sizes: ["1x1"], multiple: true, render() {} },
            { type: "denied", sizes: ["1x1", "3x3"], isAvailable: () => false, render() {} }
        ]
    });
    await controller.start();
    await controller.saveOptions("old", { domainOptions: { formName: "Contact" } });
    const original = copy(controller.settings);
    controller.setEditing(true);
    controller.resetButton.dispatchEvent(new window.MouseEvent("click", { shiftKey: true, bubbles: true }));
    const confirmation = confirmations.at(-1).options;
    assert.equal(confirmation.title, "Show all");
    assert.match(confirmation.message, /every supported size/);
    assert.deepEqual(copy(controller.settings), original, "Opening the confirmation must not modify preferences");
    assert.equal(requests.some(request => request.url.endsWith("/settings/reset")), false);
    assert.equal(await confirmation.success(), true);
    closeConfirmation();
    const draft = copy(controller.settings);
    assert.equal(await controller.saveEditing(), false);
    assert.deepEqual(copy(controller.settings), draft, "Failed save retains the complete provisional reset");
    assert.equal(controller.editing, true);
    setResetFailure(false);
    assert.equal(await controller.saveEditing(), true);
    assert.equal(requests.at(-1).method, "PUT");
    assert.equal(requests.at(-1).url, "/admin/rest/dashboard/settings/reset");
    assert.deepEqual(copy(controller.settings.items.filter(value => value.type === "test").map(value => value.size)), ["1x1", "2x2", "2x3", "3x2", "3x3", "fullauto"]);
    assert.equal(controller.settings.items.filter(value => value.type === "sessions").length, 1);
    assert.equal(controller.settings.items.some(value => value.type === "denied"), false);
    assert.deepEqual(copy(controller.settings.items.find(value => value.id === "link")), original.items.find(value => value.id === "link"));
    assert.equal(controller.settings.domainOptions.old, undefined);
    assert.equal(controller.settings.legacyBookmarksHandled, true);
    assert.equal(controller.settings.configured, true);
    assert.equal(new Set(controller.settings.items.map(value => value.id)).size, controller.settings.items.length);
    const generated = stored();
    await controller.start();
    assert.deepEqual(copy(controller.settings), generated);
    const variant = controller.settings.items.find(value => value.type === "test");
    assert.equal(await controller.remove(variant.id), true);
    assert.equal(await controller.undoRemove(), true);
    assert.ok(host.querySelector(`[data-instance-id="${variant.id}"]`));
});

test("An oversized variant reset is rejected before sending any mutation", async t => {
    const { controller, requests } = fixture(t, { items: Array.from({ length: 48 }, (_, i) => item(`link-${i}`, "shortcut")) });
    await controller.start();
    const before = requests.length;
    assert.equal(await controller.reset(true), false);
    assert.equal(requests.length, before);
    assert.equal(controller.settings.items.length, 48);
});

test("Dismissing standard reset confirmation preserves preferences and catalogue actions", async t => {
    const { controller, window, requests, closeConfirmation } = fixture(t, { items: [item("custom")] });
    await controller.start();
    controller.setEditing(true);
    controller.resetButton.click();
    closeConfirmation();
    assert.equal(requests.some(request => request.method === "DELETE"), false);
    assert.equal(window.document.activeElement, controller.resetButton);
    controller.showCatalogue();
    assert.equal(window.document.querySelector('.md-dashboard-modal .md-dashboard__reset'), null);
});

test("Catalogue combines category and title or description search using only authorized widgets", async t => {
    const { controller, window } = fixture(t, { definitions: [
        { type: "pages", titleKey: "Recent pages", descriptionKey: "Continue editing in selected folders", category: "content", multiple: true, render() {} },
        { type: "visits", titleKey: "Visits", descriptionKey: "Visitor totals", category: "traffic", multiple: true, render() {} },
        { type: "denied", titleKey: "Denied", category: "content", isAvailable: () => false, render() {} }
    ] });
    await controller.start();
    controller.showCatalogue();
    const modal = window.document.querySelector('.md-dashboard-modal--catalogue');
    assert.ok(modal.querySelector('.modal-lg.modal-dialog-centered.modal-dialog-scrollable'));
    assert.equal(modal.querySelector('[data-widget-type="denied"]'), null);
    assert.equal(modal.querySelector('[data-category="all"] .md-dashboard__catalogue-count').textContent, "3");
    const content = modal.querySelector('button[data-category="content"]');
    content.click();
    assert.equal(content.getAttribute("aria-pressed"), "true");
    assert.equal(modal.querySelector('[data-widget-type="visits"]').hidden, true);
    const search = modal.querySelector('input[type="search"]');
    search.value = " FOLDERS ";
    search.dispatchEvent(new window.Event("input"));
    assert.equal(modal.querySelector('[data-widget-type="pages"]').hidden, false);
    assert.equal(modal.querySelector('.md-dashboard__catalogue-empty').hidden, true);
    search.value = "visitor";
    search.dispatchEvent(new window.Event("input"));
    assert.equal(modal.querySelector('.md-dashboard__catalogue-empty').hidden, false);
    modal.querySelector('button[data-category="all"]').click();
    assert.equal(modal.querySelector('[data-widget-type="visits"]').hidden, false);
    search.value = "recent";
    search.dispatchEvent(new window.Event("input"));
    assert.equal(modal.querySelector('[data-widget-type="pages"]').hidden, false);
});

test("Catalogue opens settings and adds a configured widget only after confirmation", async t => {
    const { controller, window, requests, stored } = fixture(t, { items: [item("original")], definitions: [
        { type: "pages", titleKey: "Recent pages", category: "content", multiple: true, sizes: ["1x1", "3x2"], defaultSize: "3x2", render() {} }
    ] });
    await controller.start();
    controller.setEditing(true);
    controller.addButton.focus();
    controller.showCatalogue();
    const catalogue = window.document.querySelector('.md-dashboard-modal--catalogue');
    const card = catalogue.querySelector('[data-widget-type="pages"]');
    assert.equal(card.querySelector('.md-dashboard__catalogue-meta').textContent, "Content · Default size 3×2");
    card.querySelector('button').click();
    await tick();
    assert.equal(catalogue.isConnected, false);
    assert.equal(controller.settings.items.length, 1, "Opening settings must not insert an instance");
    const settings = window.document.querySelector('.md-dashboard-modal--settings');
    assert.equal(settings.querySelector('.btn-primary').textContent, "Add widget");
    settings.querySelector('.md-dashboard__size-choice input[value="1x1"]').click();
    settings.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller.settings.items.at(-1).type, "pages");
    assert.equal(controller.settings.items.at(-1).size, "1x1");
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
    assert.equal(window.document.activeElement, controller.addButton);
    assert.equal(requests.length, 0);
    assert.equal(stored().items.length, 1);
    assert.equal(await controller.saveEditing(), true);
    assert.equal(stored().items.length, 2);
});

test("Cancelling catalogue settings adds nothing and another widget requires reopening the catalogue", async t => {
    const { controller, window, requests } = fixture(t, { items: [item("original")] });
    await controller.start();
    controller.setEditing(true);
    for (let attempt = 0; attempt < 2; attempt++) {
        controller.addButton.focus();
        controller.addButton.click();
        const catalogue = window.document.querySelector('.md-dashboard-modal--catalogue');
        assert.equal(catalogue.querySelector('.md-dashboard__catalogue-item button').textContent, "Add another");
        catalogue.querySelector('.md-dashboard__catalogue-item button').click();
        await tick();
        const settings = window.document.querySelector('.md-dashboard-modal--settings');
        assert.equal(catalogue.isConnected, false);
        settings.querySelector('.modal-footer .btn-outline-secondary').click();
        assert.equal(controller.settings.items.length, 1);
        assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
        assert.equal(window.document.activeElement, controller.addButton);
    }
    assert.equal(requests.length, 0);
});

test("Catalogue disables additions at capacity after confirming the final available slot", async t => {
    const { controller, window, requests } = fixture(t, { items: Array.from({ length: 47 }, (_, index) => item(`existing-${index}`)) });
    await controller.start();
    controller.setEditing(true);
    controller.showCatalogue();
    window.document.querySelector('.md-dashboard__catalogue-item button').click();
    await tick();
    assert.equal(controller.settings.items.length, 47);
    window.document.querySelector('.md-dashboard-modal--settings .btn-primary').click();
    await tick();
    assert.equal(controller.settings.items.length, 48);
    controller.showCatalogue();
    const catalogue = window.document.querySelector('.md-dashboard-modal--catalogue');
    assert.equal(catalogue.querySelector('.md-dashboard__catalogue-item button').disabled, true);
    assert.match(catalogue.querySelector('[role="alert"]').textContent, /48 widgets/);
    assert.equal(requests.length, 0);
});

test("A confirmed reset can be cancelled and a failed save retains the draft for retry", async t => {
    const { controller, window, confirmations, closeConfirmation, setResetFailure, notifications, requests, stored } = fixture(t, { items: [item("custom")], failReset: true, defaults: [{ type: "test" }] });
    await controller.start();
    controller.setEditing(true);
    const original = stored();
    controller.resetButton.click();
    assert.equal(await confirmations.at(-1).options.success(), true);
    closeConfirmation();
    assert.equal(requests.length, 0);
    const draft = copy(controller.settings);
    assert.equal(await controller.saveEditing(), false);
    assert.match(controller.status.textContent, /changes are still available/);
    assert.deepEqual(copy(controller.settings), draft);
    assert.deepEqual(stored(), original);
    assert.equal(notifications.length, 0);
    controller.cancelEditing();
    window.document.querySelector('.md-dashboard-modal--confirm .btn-danger').click();
    assert.deepEqual(copy(controller.settings), original);
    controller.setEditing(true);
    controller.resetButton.click();
    await confirmations.at(-1).options.success();
    closeConfirmation();
    setResetFailure(false);
    assert.equal(await controller.saveEditing(), true);
    assert.equal(controller.editing, false);
    assert.deepEqual(notifications, [['The overview has been saved.', '', 10000]]);
});

test("Reset suppresses its tooltip through standard confirmation and releases listeners on removal", async t => {
    const { controller, window, tooltipCalls, closeConfirmation } = fixture(t, { withTooltip: true });
    await controller.start();
    assert.equal(tooltipCalls[0][0], 'init');
    assert.equal(tooltipCalls[0][1], controller.resetButton);
    controller.setEditing(true);
    const tooltip = window.bootstrap.Tooltip.getInstance(controller.resetButton);
    tooltip.show();
    assert.equal(tooltip.visible, true);
    for (let attempt = 0; attempt < 2; attempt++) {
        controller.resetButton.click();
        assert.deepEqual(tooltipCalls.slice(-3), [['off', '.wjFocusWithoutTooltip'], ['disable'], ['hide']]);
        tooltip.show();
        assert.equal(tooltip.visible, false, 'A delayed hover must not display the tooltip over confirmation');
        closeConfirmation();
        assert.equal(tooltipCalls.at(-4)[0], 'enable', 'Confirmation cleanup restores the tooltip before the shared focus helper takes over');
        assert.equal(tooltipCalls.at(-3)[0], 'focusWithoutTooltip');
        assert.equal(window.document.activeElement, controller.resetButton);
        tooltip.show();
        assert.equal(tooltip.visible, false, 'Restoring focus must not reopen a tooltip over the adjacent Done button');
    }
    controller.setEditing(false);
    assert.equal(tooltipCalls.at(-1)[0], 'hide');
    controller.destroy();
    assert.equal(tooltipCalls.filter(call => call[0] === 'dispose').length, 2);
    assert.deepEqual(tooltipCalls.at(-1), ['off', '.wjTooltipA11y .wjFocusWithoutTooltip']);
});

test("Successful preferences use a ten-second standard notification and clear inline status", async t => {
    const { controller, host, notifications, setSaveFailure } = fixture(t, { items: [item("custom")] });
    await controller.start();
    assert.equal(await controller.saveOptions('custom', { options: { days: 30 } }), true);
    assert.deepEqual(notifications, [['Saved.', '', 10000]]);
    assert.equal(controller.status.textContent, '');
    setSaveFailure(true);
    assert.equal(await controller.saveOptions('custom', { options: { days: 90 } }), false);
    assert.equal(notifications.length, 1, 'Failed persistence must not show a success notification');
    assert.match(controller.status.textContent, /previous settings/);
});

test("Widget controls hide and dispose tooltips and retain the grip's keyboard description", async t => {
    const { controller, host, window } = fixture(t, { items: [item('tooltip-widget')], withTooltip: true });
    await controller.start();
    controller.setEditing(true);
    const grip = host.querySelector('.md-dashboard__layout .md-dashboard__drag');
    const menu = host.querySelector('.md-dashboard__layout [data-bs-toggle="dropdown"]');
    const gripTooltip = window.bootstrap.Tooltip.getInstance(grip);
    const menuTooltip = window.bootstrap.Tooltip.getInstance(menu.parentElement);
    assert.equal(grip.title, grip.getAttribute('aria-label'));
    assert.equal(menu.parentElement.title, menu.getAttribute('aria-label'));
    grip.setAttribute('aria-describedby', 'temporary-tooltip');
    grip.dispatchEvent(new window.Event('hidden.bs.tooltip'));
    assert.equal(grip.getAttribute('aria-describedby'), controller.widgetMoveHint.id);
    menuTooltip.show();
    menu.dispatchEvent(new window.Event('show.bs.dropdown'));
    assert.equal(menuTooltip.visible, false);
    gripTooltip.show();
    controller.editor.beginMove('tooltip-widget');
    assert.equal(gripTooltip.visible, false);
    const showing = new window.Event('show.bs.tooltip', { cancelable: true });
    grip.dispatchEvent(showing);
    assert.equal(showing.defaultPrevented, true, 'Movement must prevent delayed tooltip display');
    controller.editor.finishMove(false);
    gripTooltip.show();
    controller.setEditing(false);
    assert.equal(gripTooltip.visible, false, 'Leaving edit mode hides the tooltip before its control disappears');
    controller.setEditing(true);
    await controller.remove('tooltip-widget');
    assert.equal(window.bootstrap.Tooltip.getInstance(grip), undefined);
    assert.equal(window.bootstrap.Tooltip.getInstance(menu.parentElement), undefined);
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
    assert.match(controller.status.textContent, /previous settings/);
});

test("Removal undo survives a failed save but expires after the next successful mutation", async t => {
    const { controller, host, setSaveFailure } = fixture(t, { items: [item("a"), item("b")] });
    await controller.start();
    await controller.remove("b");
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, false);
    setSaveFailure(true);
    assert.equal(await controller.saveOptions("a", { options: { days: 30 } }), false);
    assert.equal(controller.removed.instance.id, "b");
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, false);
    setSaveFailure(false);
    assert.equal(await controller.saveOptions("a", { options: { days: 30 } }), true);
    assert.equal(controller.removed, null);
    assert.equal(host.querySelector(".md-dashboard__undo").hidden, true);
    assert.equal(await controller.undoRemove(), false);
});

test("Unavailable types remain persisted while authorized instances render", async t => {
    const { controller, host, stored } = fixture(t, {
        items: [item("visible"), item("restricted", "restricted")],
        definitions: [{ type: "restricted", titleKey: "Restricted", isAvailable: () => false, render() { throw new Error("Restricted content must not render"); } }]
    });
    await controller.start();
    assert.equal(host.querySelectorAll("[data-instance-id]").length, 1);
    await controller.saveOptions("visible", { options: { days: 30 } });
    assert.equal(stored().items.length, 2, "A changed permission must not erase the user's preferences");
});

test("Sessions remain in the permanent header without arrangement controls", async t => {
    let renders = 0;
    const session = item("sessions", "sessions", "2x3");
    const { controller, host, requests, stored } = fixture(t, {
        items: [session, item("ordinary")],
        definitions: [{
            type: "sessions", titleKey: "Sessions", mandatory: true, sizes: ["2x3"],
            render: ({ container }) => { renders++; container.textContent = "Active session details"; }
        }]
    });
    await controller.start();
    assert.match(host.querySelector(".md-dashboard__sessions").textContent, /Active session details/);
    assert.equal(host.querySelector(".md-dashboard__layout [data-widget-type='sessions']"), null);
    assert.equal(host.querySelector(".md-dashboard__sessions .md-dashboard__widget-controls"), null);
    const count = requests.length;
    assert.equal(await controller.remove("sessions"), false);
    assert.equal(await controller.moveBefore("sessions", "ordinary"), false);
    assert.equal(requests.length, count);
    await controller.saveOptions("ordinary", { options: { days: 30 } });
    assert.deepEqual(stored().items[0], session, "Editing another card must preserve session preferences");
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
    controller.settings.items = Array.from({ length: 48 }, (_, index) => item(`item-${index}`));
    const before = requests.length;
    assert.equal(await controller.add("test"), false);
    assert.equal(requests.length, before);
});

test("Widget grips lift, move, drop and cancel with the keyboard without persisting a draft", async t => {
    const { controller, window, requests, stored } = fixture(t, { items: [item("a"), item("b"), item("c")] });
    await controller.start();
    controller.setEditing(true);
    const canvas = window.document.createElement('canvas');
    controller.views.get("a").body.append(canvas);
    let chartCopied = false;
    window.HTMLCanvasElement.prototype.getContext = () => ({ drawImage(source) { chartCopied = source === canvas; } });
    const grip = controller.views.get("a").card.querySelector('.md-dashboard__drag');
    const key = value => grip.dispatchEvent(new window.KeyboardEvent('keydown', { key: value, bubbles: true, cancelable: true }));
    key(' ');
    assert.equal(grip.getAttribute('aria-pressed'), 'true');
    assert.ok(window.document.querySelector('.md-dashboard__widget-drag-helper'));
    assert.equal(chartCopied, true, 'The raised helper must retain the chart pixels');
    key('ArrowRight');
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["b", "a", "c"]);
    assert.match(controller.widgetMoveStatus.textContent, /position 2 of 3/);
    assert.equal(controller.overviewStatus.textContent, '', 'Move announcements must not add a row above the grid');
    key('Escape');
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["a", "b", "c"]);
    assert.equal(window.document.querySelector('.md-dashboard__widget-drag-helper'), null);
    grip.click();
    key('ArrowRight');
    key('ArrowRight');
    key('Enter');
    assert.deepEqual(copy(controller.settings.items.map(value => value.id)), ["b", "c", "a"]);
    assert.equal(requests.length, 0);
    assert.deepEqual(stored().items.map(value => value.id), ["a", "b", "c"]);
    assert.equal(window.document.activeElement, grip);
    assert.equal(await controller.saveEditing(), true);
    assert.deepEqual(stored().items.map(value => value.id), ["b", "c", "a"]);
});

test("A menu action closes its dropdown before disabling controls for persistence", async t => {
    const { controller, host, window } = fixture(t, { items: [item("a")] });
    await controller.start();
    let hidden = false;
    window.bootstrap.Dropdown = { getInstance: () => ({ hide() {
        assert.equal(controller.saving, false, "Bootstrap must hide the menu before the toggle is disabled");
        hidden = true;
    }, dispose() {} }) };
    host.querySelector('[data-dashboard-action="remove"]').click();
    assert.equal(hidden, true);
    await tick();
    assert.equal(controller.settings.items.length, 0);
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
    window.document.querySelector(".modal-header button.btn-close").click();
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
    assert.equal(requests.length, 0);
});

test("The feedback toolbar action precedes widget controls and opens the existing form without saving preferences", async t => {
    let opened = 0;
    const { controller, host, requests } = fixture(t, { overview: { showFeedbackModal: () => { opened++; } } });
    await controller.start();
    const feedback = host.querySelector('.md-dashboard__feedback');
    const addWidget = feedback.nextElementSibling;
    assert.ok(addWidget.classList.contains('md-dashboard__edit-control'));
    assert.equal(addWidget.hidden, true);
    assert.equal(addWidget, controller.resetButton);
    assert.equal(controller.resetButton.nextElementSibling, controller.addButton);
    assert.equal(controller.addButton.nextElementSibling, controller.cancelButton);
    assert.equal(controller.cancelButton.nextElementSibling, controller.editButton);
    feedback.click();
    assert.equal(opened, 1);
    assert.equal(controller.editing, false);
    controller.editButton.click();
    assert.equal(addWidget.hidden, false);
    assert.equal(feedback.nextElementSibling, addWidget);
    assert.equal(addWidget, controller.resetButton);
    assert.equal(controller.resetButton.nextElementSibling, controller.addButton);
    assert.equal(controller.addButton.nextElementSibling, controller.cancelButton);
    assert.equal(controller.cancelButton.nextElementSibling, controller.editButton);
    assert.equal(requests.length, 0, 'Opening feedback must not save or reset dashboard preferences');
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
    assert.equal(requests.length, 0, 'Cancelling must not save an empty shortcut');
    await controller.showAddWidget('shortcut');
    window.document.querySelector('.md-dashboard__settings input').value = 'My users';
    window.document.querySelector('.modal-footer .btn-primary').click();
    await tick();
    assert.equal(stored().items.length, 1);
    assert.equal(stored().items[0].options.title, 'My users');
    assert.match(host.querySelector('.md-dashboard__shortcut-list').textContent, /My users/);
    assert.equal(host.querySelector('.md-dashboard__layout [data-widget-type="shortcut"]'), null);
});

/** Loads the overview component against the existing DOM without starting the application shell. */
function overviewFixture(t) {
    const environment = fixture(t);
    const { context, window, controller } = environment;
    Object.assign(context, { HTMLElement: window.HTMLElement, customElements: window.customElements, WJ: window.WJ });
    const noticesSource = fs.readFileSync(path.join(moduleDirectory, 'notices.js'), 'utf8').replace(/^export /gm, '');
    vm.runInContext(`{ ${noticesSource}; this.DashboardNotices = DashboardNotices; }`, context);
    const source = fs.readFileSync(path.join(moduleDirectory, '../web-components/webjet-overview-dashboard.js'), 'utf8')
        .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context, { filename: 'webjet-overview-dashboard.js' });
    const overview = window.document.createElement('webjet-overview-dashboard');
    overview.dashboardController = controller;
    t.after(() => overview.noticeController?.destroy());
    return { ...environment, overview };
}

test('Rebuilding the overview after a save never reapplies the embedded settings snapshot', async t => {
    const { context, overview, requests } = overviewFixture(t);
    context.registerDashboardWidgets = () => {};
    context.getDashboardDefaults = () => [];
    overview.configure({ data: { notices: [], settings: { configured: true, items: [item('embedded')] } } });
    overview.render();
    t.after(() => overview.disconnectedCallback());
    await overview.dashboardReady;
    assert.equal(requests.length, 0);
    await overview.dashboardController.saveOptions('embedded', { options: { title: 'Updated autotest' } });
    overview.render();
    await overview.dashboardReady;
    assert.equal(requests.length, 1);
    assert.equal(requests[0].method, 'PUT');
    assert.equal(overview.dashboardController.settings.items[0].options.title, 'Updated autotest');
});

test('System notice rows invoke authorized actions and survive personal layout rendering', async t => {
    const { window, controller, overview } = overviewFixture(t);
    const actions = [];
    window.WJ.openPopupDialog = url => actions.push(['popup', url]);
    window.WJ.showHelpWindow = url => actions.push(['help', url]);
    overview.data.notices = [
        { id: 'two-factor', severity: 'warning', icon: 'ti-shield', title: 'Enable verification', bodyHtml: '<p>Protect your account.</p>', action: { type: 'popup', url: '/admin/2factorauth.jsp', label: 'Configure' } },
        { id: 'ses', severity: 'error', icon: 'ti-mail', title: 'Configure email', bodyHtml: '<p>Set up sending.</p>', action: { type: 'help', url: '/install/config/README', label: 'Read guide' } }
    ];
    const customNotice = window.document.createElement('div');
    customNotice.textContent = 'External application warning';
    controller.notices.append(customNotice);
    overview._renderNotices();
    assert.equal(customNotice.parentElement, controller.notices, 'System notices must preserve externally supplied notifications');
    controller.setEditing(true);
    assert.equal(controller.notices.hasAttribute('inert'), false, 'System notices must stay interactive during overview editing');
    controller.notices.querySelector('[data-notice-id="two-factor"] .md-dashboard__notice-action').click();
    controller.notices.querySelector('[data-notice-id="ses"] .md-dashboard__notice-action').click();
    assert.deepEqual(actions, [['popup', '/admin/2factorauth.jsp'], ['help', '/install/config/README']]);
    await controller.start();
    assert.equal(controller.notices.querySelectorAll('.md-dashboard__notice').length, 2, 'Rendering personal layout must preserve system warnings');
    assert.equal(customNotice.parentElement, controller.notices, 'Rendering personal layout must preserve external notifications');
});

test('Embedded notices, including an empty list, render without a REST request', async t => {
    const { controller, overview, requests } = overviewFixture(t);
    overview.data.notices = [{ id: 'autotest-notice', severity: 'warning', title: 'Embedded warning', bodyHtml: '<p>Autotest details</p>' }];
    overview._renderNotices();
    assert.equal(controller.notices.querySelector('.md-dashboard__notice-title').textContent, 'Embedded warning');
    overview.data.notices = [];
    overview._renderNotices();
    assert.equal(controller.notices.querySelectorAll('.md-dashboard__notice').length, 0);
    assert.equal(requests.length, 0);
});

test('Release note persistence restores the replacement toggle without stealing focus from another control', async t => {
    const { context, window, host } = fixture(t);
    Object.assign(context, { DOMParser: window.DOMParser });
    const source = fs.readFileSync(path.join(moduleDirectory, 'utility-widgets.js'), 'utf8')
        .replace(/^import .+;\r?$/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context, { filename: 'utility-widgets.js' });
    context.registerSessionWidgets = () => {};
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


const shortcutDefinition = { type: "shortcut", titleKey: "Shortcut", sizes: ["1x1"], multiple: true,
    render: ({ container, options }) => { const link = container.ownerDocument.createElement("a"); link.href = options.href; link.textContent = options.title || options.href; container.append(link); } };

test("Welcome shortcuts and overview have independent edit controls and catalogue entries", async t => {
    const { controller, host, window } = fixture(t, { items: [item("grid"), item("link", "shortcut", "1x1", { href: "/apps/banner/admin/" })], definitions: [shortcutDefinition], withTooltip: true });
    await controller.start();
    assert.ok(host.querySelector('.md-dashboard__welcome .md-dashboard__shortcuts a[href="/apps/banner/admin/"]'));
    assert.equal(controller.editShortcutsButton.parentElement, controller.shortcutList.lastElementChild);
    assert.equal(controller.shortcutActions.previousElementSibling.dataset.instanceId, 'link');
    assert.equal(controller.addShortcutButton.hidden, false);
    assert.equal(controller.editShortcutsButton.textContent, 'Edit shortcuts');
    assert.ok(controller.editShortcutsButton.querySelector('.ti-pencil'));
    const tooltip = window.bootstrap.Tooltip.getInstance(controller.editShortcutsButton);
    controller.editShortcutsButton.focus();
    tooltip.show();
    assert.equal(tooltip.visible, true);
    const controls = id => host.querySelector(`[data-instance-id="${id}"] .md-dashboard__edit-control`);
    controller.setEditingShortcuts(true);
    assert.equal(controls("link").hidden, false);
    assert.equal(controls("grid").hidden, true);
    assert.equal(controller.addShortcutButton.hidden, false);
    assert.equal(controller.shortcutActions.children.length, 1);
    assert.ok(controller.editShortcutsButton.querySelector('.ti-check'));
    assert.equal(controller.editShortcutsButton.parentElement, controller.welcomeHeading);
    assert.equal(controller.editShortcutsButton.textContent, 'Done');
    assert.equal(controller.editShortcutsButton.querySelector('.visually-hidden'), null);
    assert.equal(window.document.activeElement, controller.editShortcutsButton);
    assert.equal(tooltip.visible, false);
    assert.equal(tooltip.enabled, false);
    controller.setEditing(true);
    assert.equal(controller.editingShortcuts, false);
    assert.equal(controller.editShortcutsButton.parentElement, controller.shortcutActions);
    assert.ok(controller.editShortcutsButton.querySelector('.ti-pencil'));
    assert.equal(controller.addShortcutButton.hidden, false);
    assert.equal(controls("link").hidden, true);
    assert.equal(controls("grid").hidden, false);
    controller.showCatalogue();
    assert.equal(host.ownerDocument.querySelector('.md-dashboard__catalogue [data-widget-type="shortcut"]'), null);
});

test("Widget reset preserves shortcuts and intentionally empty shortcuts survive reload", async t => {
    const { controller, stored } = fixture(t, { items: [item("grid"), item("link", "shortcut", "1x1", { href: "/custom/?a=1#anchor", title: "Custom" })], definitions: [shortcutDefinition], defaults: [{ type: "test" }, { type: "shortcut", options: { href: "/default/" } }], legacyBookmarksHandled: true });
    await controller.start();
    await controller.saveOptions("grid", { domainOptions: { formName: "Contact" } });
    await controller.acknowledgeNews("2026.18");
    assert.deepEqual(stored().domainOptions.grid, { formName: "Contact" });
    assert.equal(stored().acknowledgedNewsVersion, "2026.18");
    assert.equal(stored().items[0].id, "grid");
    const linkId = stored().items.find(item => item.type === "shortcut").id;
    assert.equal(await controller.reset(), true);
    assert.equal(controller.settings.items.find(item => item.type === "shortcut").id, linkId);
    assert.equal(controller.settings.legacyBookmarksHandled, true);
    await controller.remove(linkId);
    await controller.start();
    assert.equal(controller.settings.items.some(item => item.type === "shortcut"), false);
    await controller.reset();
    await controller.start();
    assert.equal(controller.settings.items.some(item => item.type === "shortcut"), false);
    assert.equal(stored().items.filter(item => item.type === "shortcut").length, 0);
});

test("Legacy bookmarks automatically replace shortcuts in original order and clear only the migrated source", async t => {
    const { controller, window, stored, requests, host } = fixture(t, { items: [item("grid"), item("existing", "shortcut", "1x1", { href: "/apps/form/admin/", title: "New label" }), item("extra", "shortcut", "1x1", { href: "/extra/", title: "My extra" })], definitions: [shortcutDefinition] });
    await controller.start();
    await controller.saveOptions("grid", { domainOptions: { formName: "Contact" } });
    await controller.acknowledgeNews("2026.18");
    await controller.saveOptions("existing", { domainOptions: {} });
    const previous = stored();
    window.localStorage.setItem("unrelated", "preserve");
    window.localStorage.setItem("bookmarks", JSON.stringify([
        { name: "Banner autotest", path: "/apps/banner/admin/?id=23#detail" },
        { name: "Forms autotest", path: "http://localhost/apps/form/admin/" },
        { name: "Duplicate", path: "/apps/form/admin/" },
        { name: "<img src=x>", path: "https://example.com/docs" }
    ]));
    await controller.start();
    const links = stored().items.filter(item => item.type === "shortcut");
    assert.deepEqual(links.map(item => item.options), [
        { source: "url", href: "/apps/banner/admin/?id=23#detail", title: "Banner autotest" },
        { source: "url", href: "/apps/form/admin/", title: "Forms autotest" },
        { source: "url", href: "https://example.com/docs", title: "<img src=x>" }
    ]);
    assert.deepEqual(stored().items.filter(item => item.type !== "shortcut"), previous.items.filter(item => item.type !== "shortcut"));
    assert.deepEqual(stored().domainOptions.grid, previous.domainOptions.grid);
    assert.equal(Object.hasOwn(stored().domainOptions, "existing"), false);
    assert.equal(stored().acknowledgedNewsVersion, previous.acknowledgedNewsVersion);
    assert.equal(stored().legacyBookmarksHandled, true);
    assert.equal(window.localStorage.getItem("bookmarks"), null);
    assert.equal(window.localStorage.getItem("unrelated"), "preserve");
    assert.equal(host.querySelector("img, .md-dashboard__legacy-shortcuts, .md-dashboard-modal"), null);
    const count = requests.filter(request => request.method === "PUT").length;
    await controller.start();
    assert.equal(requests.filter(request => request.method === "PUT").length, count, "Reloading must not import twice");
});

test("Failed and oversized automatic imports keep all preferences and retry on a later load", async t => {
    const { controller, window, stored, setSaveFailure } = fixture(t, { items: [item("grid"), item("previous", "shortcut", "1x1", { href: "/previous/" })], definitions: [shortcutDefinition], failSave: true });
    const original = stored();
    const legacy = JSON.stringify([{ name: "Autotest", path: "/apps/banner/admin/" }]);
    window.localStorage.setItem("bookmarks", legacy);
    await controller.start();
    assert.deepEqual(stored(), original);
    assert.equal(controller.settings.legacyBookmarksHandled, false);
    assert.equal(window.localStorage.getItem("bookmarks"), legacy);
    assert.match(controller.shortcutStatus.textContent, /could not be saved/);
    setSaveFailure(false);
    const oversized = JSON.stringify(Array.from({ length: 48 }, (_, i) => ({ name: `Autotest ${i}`, path: `/apps/banner/admin/?id=${i}` })));
    window.localStorage.setItem("bookmarks", oversized);
    await controller.start();
    assert.deepEqual(stored(), original, "Do not silently truncate an oversized import");
    assert.equal(window.localStorage.getItem("bookmarks"), oversized);
    assert.match(controller.shortcutStatus.textContent, /at most 48/);
    window.localStorage.setItem("bookmarks", legacy);
    await controller.start();
    assert.equal(stored().items.length, 2);
    assert.equal(stored().legacyBookmarksHandled, true);
    assert.equal(window.localStorage.getItem("bookmarks"), null);
    assert.equal(controller.shortcutStatus.textContent, "");
});

test("Invalid legacy records remain intact without a partial import", async t => {
    const { controller, window, stored, requests } = fixture(t, { items: [item("grid")], definitions: [shortcutDefinition] });
    const original = stored();
    for (const legacy of ["invalid JSON", "{}", JSON.stringify([{ name: "Valid", path: "/apps/form/admin/" }, { name: "Unsafe", path: "javascript:alert(1)" }])]) {
        window.localStorage.setItem("bookmarks", legacy);
        await controller.start();
        assert.deepEqual(stored(), original);
        assert.equal(window.localStorage.getItem("bookmarks"), legacy);
        assert.match(controller.shortcutStatus.textContent, /invalid name or URL/);
    }
    assert.equal(requests.some(request => request.method === "PUT"), false);
});

test("Missing, empty or unavailable browser storage retains default shortcuts without a migration save", async t => {
    const { controller, window, requests } = fixture(t, { configured: false, definitions: [shortcutDefinition], defaults: [{ type: "shortcut", options: { href: "/apps/form/admin/" } }] });
    await controller.start();
    window.localStorage.setItem("bookmarks", "[]");
    await controller.start();
    Object.defineProperty(window, "localStorage", { get() { throw new window.DOMException("Storage disabled", "SecurityError"); } });
    await controller.start();
    assert.equal(controller.settings.items.filter(item => item.type === "shortcut").length, 1);
    assert.equal(requests.some(request => request.method === "PUT"), false);
});

test("Import keeps the source until server confirmation and preserves browser changes made during the save", async t => {
    const { controller, context, window } = fixture(t, { definitions: [shortcutDefinition] });
    const fetch = context.fetch;
    let release;
    const pending = new Promise(resolve => { release = resolve; });
    context.fetch = async (url, options) => {
        if (options?.method === "PUT") await pending;
        return fetch(url, options);
    };
    const legacy = JSON.stringify([{ name: "Autotest", path: "/apps/form/admin/" }]);
    window.localStorage.setItem("bookmarks", legacy);
    const loading = controller.start();
    await tick();
    assert.equal(controller.saving, true);
    assert.equal(window.localStorage.getItem("bookmarks"), legacy);
    const updated = JSON.stringify([{ name: "Changed in another tab", path: "/apps/banner/admin/" }]);
    window.localStorage.setItem("bookmarks", updated);
    release();
    await loading;
    assert.equal(controller.settings.legacyBookmarksHandled, true);
    assert.equal(window.localStorage.getItem("bookmarks"), updated);
    await controller.start();
    assert.equal(controller.settings.items[0].options.href, "/apps/form/admin/", "The account marker prevents a second overwrite");
});

test("A storage cleanup failure does not repeat an already persisted import", async t => {
    const { controller, window, requests } = fixture(t, { definitions: [shortcutDefinition] });
    window.localStorage.setItem("bookmarks", JSON.stringify([{ name: "Autotest", path: "/apps/form/admin/" }]));
    window.Storage.prototype.removeItem = () => { throw new window.DOMException("Storage disabled", "SecurityError"); };
    await controller.start();
    assert.equal(controller.settings.legacyBookmarksHandled, true);
    assert.notEqual(window.localStorage.getItem("bookmarks"), null);
    await controller.start();
    assert.equal(requests.filter(request => request.method === "PUT").length, 1);
});

test('Shortcut editing replaces navigation and menu actions with a leading grip and direct removal', async t => {
    const { controller, host, window } = fixture(t, { items: [item('link', 'shortcut', '1x1', { href: '/target/' })], definitions: [shortcutDefinition] });
    await controller.start();
    const card = controller.views.get('link').card;
    const target = card.querySelector('a');
    assert.equal(target.getAttribute('href'), '/target/');
    assert.equal(card.querySelector('.dropdown'), null);
    controller.setEditingShortcuts(true);
    assert.equal(card.children[1].classList.contains('md-dashboard__drag'), true);
    assert.equal(card.lastElementChild.classList.contains('md-dashboard__shortcut-remove'), true);
    assert.equal(target.hasAttribute('href'), false);
    assert.equal(target.getAttribute('role'), 'button');
    const click = new window.MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true });
    target.dispatchEvent(click);
    assert.equal(click.defaultPrevented, true);
    assert.match(window.document.querySelector('.modal-title').textContent, /Edit shortcut/);
    window.document.querySelector('.btn-close').click();
    controller.setEditingShortcuts(false);
    assert.equal(target.getAttribute('href'), '/target/');
    assert.equal(target.hasAttribute('role'), false);
    assert.equal(host.querySelectorAll('.md-dashboard__shortcut-actions button').length, 2);
});

test('Shortcut keyboard moves remain provisional until Enter and roll back on cancellation or save failure', async t => {
    const { controller, window, requests, stored, setSaveFailure } = fixture(t, { items: ['first', 'second', 'third'].map(id => item(id, 'shortcut', '1x1', { href: `/${id}/` })), definitions: [shortcutDefinition] });
    await controller.start();
    controller.setEditingShortcuts(true);
    const grip = controller.views.get('first').card.querySelector('.md-dashboard__drag');
    const press = key => grip.dispatchEvent(new window.KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    press(' ');
    press('ArrowRight');
    assert.equal(controller._shortcutMove.beforeId, 'third');
    assert.equal(requests.length, 0);
    assert.ok(window.document.querySelector('.md-dashboard__shortcut-drag-helper'));
    press('Escape');
    assert.equal(controller._shortcutMove, null);
    assert.equal(window.document.querySelector('.md-dashboard__shortcut-drag-helper'), null);
    assert.deepEqual(stored().items.map(item => item.id), ['first', 'second', 'third']);
    press(' ');
    press('ArrowRight');
    press('Enter');
    await tick();
    assert.deepEqual(stored().items.map(item => item.id), ['second', 'first', 'third']);
    assert.equal(window.document.activeElement, grip);
    setSaveFailure(true);
    press(' ');
    press('ArrowRight');
    press('Enter');
    await tick();
    assert.deepEqual(stored().items.map(item => item.id), ['second', 'first', 'third']);
    assert.equal(window.document.querySelector('.is-shortcut-placeholder'), null);
});

test('Shortcut removal has an eight-second undo, preserves the last shortcut on failure and can retry undo', async t => {
    const { controller, window, stored, confirmations, setSaveFailure } = fixture(t, { items: [item('grid'), item('last', 'shortcut', '1x1', { href: '/last/', color: 'blue' })], definitions: [shortcutDefinition] });
    const timers = [];
    window.setTimeout = (callback, delay) => { timers.push({ callback, delay }); return timers.length; };
    window.clearTimeout = () => {};
    await controller.start();
    setSaveFailure(true);
    assert.equal(await controller.remove('last'), false);
    assert.equal(stored().items.length, 2);
    assert.equal(controller._shortcutUndo, undefined);
    setSaveFailure(false);
    assert.equal(await controller.remove('last'), true);
    assert.equal(confirmations.length, 0);
    assert.equal(stored().items.length, 1);
    assert.ok(controller.shortcutList.querySelector('.md-dashboard__shortcuts-empty'));
    assert.equal(timers.at(-1).delay, 8000);
    assert.equal(window.document.querySelector('.md-dashboard__shortcut-toast .toast').dataset.timeout, '8000');
    setSaveFailure(true);
    assert.equal(await controller._undoShortcutChange(), false);
    assert.ok(window.document.querySelector('[data-dashboard-shortcut-undo]'));
    setSaveFailure(false);
    assert.equal(await controller._undoShortcutChange(), true);
    assert.equal(stored().items[1].options.color, 'blue');
    await controller.remove('last');
    timers.at(-1).callback();
    assert.equal(window.document.querySelector('[data-dashboard-shortcut-undo]'), null);
    assert.equal(await controller._undoShortcutChange(), false);
});

test('Shortcut settings save immediately, support undo and include direct removal without confirmation', async t => {
    const { controller, window, stored, confirmations } = fixture(t, { items: [item('grid'), item('link', 'shortcut', '1x1', { href: '/old/', title: 'Original' })], definitions: [{ ...shortcutDefinition,
        configure: () => ({ read: () => ({ options: { href: '/new/', title: 'Changed', color: 'red' } }) })
    }] });
    await controller.start();
    controller.setEditingShortcuts(true);
    await controller.showSettings('link');
    assert.match(window.document.querySelector('.modal-footer .btn-primary').textContent, /Save changes/);
    window.document.querySelector('.modal-footer .btn-primary').click();
    await tick();
    assert.equal(stored().items[1].options.title, 'Changed');
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
    assert.equal(await controller._undoShortcutChange(), true);
    assert.equal(stored().items[1].options.title, 'Original');
    assert.equal(stored().items[0].id, 'grid');
    await controller.showSettings('link');
    window.document.querySelector('[data-dashboard-action="removeShortcut"]').click();
    await tick();
    assert.equal(stored().items.length, 1);
    assert.equal(confirmations.length, 0);
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
    assert.equal(await controller._undoShortcutChange(), true);
    assert.equal(stored().items[1].options.title, 'Original');
});

test('Widget settings apply to a draft in a centered modal and Cancel keeps the confirmed layout', async t => {
    const { controller, window, requests, stored } = fixture(t, { items: [item('draft')] });
    await controller.start();
    controller.setEditing(true);
    assert.equal(controller.notices.hasAttribute('inert'), false);
    await controller.showSettings('draft');
    const dialog = window.document.querySelector('.md-dashboard-modal--settings');
    assert.ok(dialog.querySelector('.modal-dialog-centered'));
    assert.equal(dialog.getAttribute('aria-modal'), 'true');
    assert.equal(dialog.querySelectorAll('.md-dashboard__size-choice input').length, 6);
    assert.equal(dialog.contains(window.document.activeElement), true);
    dialog.querySelector('.md-dashboard__size-choice input[value="3x3"]').click();
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller._instance('draft').size, '3x3');
    assert.equal(requests.length, 0);
    assert.equal(stored().items[0].size, '2x2');
    controller.cancelEditing();
    const confirm = window.document.querySelector('.md-dashboard-modal--confirm');
    assert.equal(window.document.activeElement.textContent, 'Continue editing');
    confirm.querySelector('.btn-outline-secondary:not(.btn-close)').click();
    assert.equal(controller.editing, true);
    assert.equal(controller._instance('draft').size, '3x3');
    await controller.showSettings('draft');
    controller.cancelEditing();
    window.document.querySelector('.md-dashboard-modal--confirm .btn-danger').click();
    assert.equal(controller.editing, false);
    assert.equal(controller._instance('draft').size, '2x2');
    assert.equal(window.document.querySelector('.md-dashboard-modal--settings'), null, 'Exiting editing must dispose open draft settings');
    assert.equal(controller.notices.hasAttribute('inert'), false);
    assert.equal(requests.length, 0);
});

for (const switchToShortcuts of [false, true]) {
    test(`Discard waits for Bootstrap cleanup before ${switchToShortcuts ? 'entering shortcut editing' : 'leaving overview editing'}`, { timeout: 2000 }, async t => {
        const { controller, window, requests, stored } = fixture(t, { items: [item('draft')], realBootstrap: true });
        const errors = [];
        window.addEventListener('error', event => { errors.push(event.error); event.preventDefault(); });
        await controller.start();
        const original = stored();
        controller.setEditing(true);
        await controller.saveOptions('draft', { options: { days: 30 } });
        let afterCalls = 0;
        controller.cancelEditing(() => {
            afterCalls++;
            assert.equal(window.document.body.classList.contains('modal-open'), false);
            assert.equal(window.document.body.style.overflow, '');
            if (switchToShortcuts) controller.setEditingShortcuts(true);
        });
        const dialog = window.document.querySelector('.md-dashboard-modal--confirm');
        await new Promise(resolve => dialog.addEventListener('shown.bs.modal', resolve, { once: true }));
        assert.equal(window.document.body.classList.contains('modal-open'), true);
        assert.equal(window.document.body.style.overflow, 'hidden');
        const hidden = new Promise(resolve => dialog.addEventListener('hidden.bs.modal', resolve, { once: true }));
        const discard = dialog.querySelector('.btn-danger');
        discard.click();
        discard.click();
        assert.equal(controller.editing, true, 'The editor must remain alive until the hide transition finishes');
        assert.equal(discard.disabled, true);
        assert.equal(afterCalls, 0);
        await hidden;
        assert.deepEqual(errors, []);
        assert.equal(afterCalls, 1);
        assert.equal(controller.editing, false);
        assert.equal(controller.editingShortcuts, switchToShortcuts);
        assert.deepEqual(copy(controller.settings), original);
        assert.equal(window.document.querySelector('.modal-backdrop, .md-dashboard-modal'), null);
        assert.equal(controller._dialogs.size, 0);
        assert.equal(window.document.activeElement, controller.editButton);
        assert.equal(requests.length, 0);
    });
}

test('Continue editing closes Bootstrap cleanly and cancelling a clean draft needs no modal', { timeout: 2000 }, async t => {
    const { controller, window, requests } = fixture(t, { items: [item('draft')], realBootstrap: true });
    await controller.start();
    controller.setEditing(true);
    controller.cancelEditing();
    assert.equal(controller.editing, false);
    assert.equal(controller._dialogs.size, 0);
    controller.setEditing(true);
    await controller.saveOptions('draft', { options: { days: 30 } });
    let afterCalls = 0;
    controller.cancelEditing(() => afterCalls++);
    const dialog = window.document.querySelector('.md-dashboard-modal--confirm');
    await new Promise(resolve => dialog.addEventListener('shown.bs.modal', resolve, { once: true }));
    const hidden = new Promise(resolve => dialog.addEventListener('hidden.bs.modal', resolve, { once: true }));
    dialog.querySelector('.modal-footer .btn-outline-secondary').click();
    await hidden;
    assert.equal(controller.editing, true);
    assert.equal(controller._instance('draft').options.days, 30);
    assert.equal(afterCalls, 0);
    assert.equal(window.document.body.classList.contains('modal-open'), false);
    assert.equal(window.document.body.style.overflow, '');
    assert.equal(window.document.querySelector('.modal-backdrop, .md-dashboard-modal'), null);
    assert.equal(requests.length, 0);
});

test('Discard completes immediately without Bootstrap and invokes its callback once', async t => {
    const { controller, window, requests, stored } = fixture(t, { items: [item('draft')] });
    const errors = [];
    window.addEventListener('error', event => { errors.push(event.error); event.preventDefault(); });
    delete window.bootstrap;
    await controller.start();
    const original = stored();
    controller.setEditing(true);
    await controller.saveOptions('draft', { options: { days: 30 } });
    let afterCalls = 0;
    controller.cancelEditing(() => afterCalls++);
    const discard = window.document.querySelector('.md-dashboard-modal--confirm .btn-danger');
    discard.click();
    discard.click();
    assert.deepEqual(errors, []);
    assert.equal(afterCalls, 1);
    assert.equal(controller.editing, false);
    assert.deepEqual(copy(controller.settings), original);
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
    assert.equal(window.document.activeElement, controller.editButton);
    assert.equal(requests.length, 0);
});

test('Live previews follow size, domain selection and color without mutating the dashboard', async t => {
    const renders = [];
    const { controller, window, requests } = fixture(t, {
        items: [item('preview-autotest', 'previewable', '1x1', { days: 7 })],
        definitions: [{ type: 'previewable', titleKey: 'Preview autotest', sizes: ['1x1', '3x3'], multiple: true,
            defaultDomainOptions: { formName: 'initial-autotest' },
            headerLink: { href: (instance, context) => `/apps/form/admin/detail/?formName=${context.settings.domainOptions[instance.id]?.formName || ''}` },
            render: ({ container, instance, options, domainOptions, signal }) => {
                renders.push({ id: instance.id, size: instance.size, options: copy(options), domainOptions: copy(domainOptions), signal });
                container.textContent = `${instance.size} / ${options.days} / ${domainOptions.formName}`;
            },
            configure: ({ container }) => {
                const select = window.document.createElement('select');
                select.innerHTML = '<option value="initial-autotest">Initial autotest</option><option value="changed-autotest">Changed autotest</option>';
                container.append(select);
                return { read: () => ({ options: { days: 30 }, domainOptions: { formName: select.value } }) };
            } }]
    });
    await controller.start();
    controller.setEditing(true);
    const original = copy(controller.settings);
    await controller.showSettings('preview-autotest');
    await tick();
    const dialog = window.document.querySelector('.md-dashboard-modal--settings');
    const preview = dialog.querySelector('.md-dashboard__widget-preview .md-dashboard__widget');
    assert.equal(preview.textContent, 'Preview autotest1x1 / 30 / initial-autotest');
    assert.equal(dialog.querySelectorAll('.md-dashboard__widget').length, 1, 'The live card replaces the separate background sample');
    assert.equal(preview.querySelector('.md-dashboard__drag, .dropdown'), null);
    const firstRender = renders.at(-1);
    dialog.querySelector('.md-dashboard__size-choice input[value="3x3"]').click();
    await tick();
    assert.equal(firstRender.signal.aborted, true, 'Changing size must abort the previous preview');
    assert.equal(preview.dataset.size, '3x3');
    const form = dialog.querySelector('select');
    form.value = 'changed-autotest';
    form.dispatchEvent(new window.Event('change', { bubbles: true }));
    await tick();
    assert.match(preview.textContent, /3x3 \/ 30 \/ changed-autotest/);
    assert.equal(preview.querySelector('a').getAttribute('href'), '/apps/form/admin/detail/?formName=changed-autotest');
    const count = renders.length;
    dialog.querySelector('.md-dashboard__widget-colors input[value="custom"]').click();
    dialog.querySelector('color-picker').dispatchEvent(new window.CustomEvent('update-color', { detail: { hex: '#fff1ec' } }));
    assert.equal(preview.style.backgroundColor, 'rgb(255, 241, 236)');
    assert.equal(renders.length, count, 'Changing color must not refetch widget data');
    assert.deepEqual(copy(controller.settings), original);
    const lastRender = renders.at(-1);
    dialog.querySelector('.modal-footer .btn-outline-secondary').click();
    assert.equal(lastRender.signal.aborted, true);
    assert.deepEqual(copy(controller.settings), original);
    assert.equal(requests.length, 0);
});

test('Replaced and closed previews dispose late renderer results without restoring stale content', async t => {
    const pending = [];
    const disposed = [];
    const { controller, window } = fixture(t, {
        items: [item('late-autotest', 'late-preview', '1x1')],
        definitions: [{ type: 'late-preview', titleKey: 'Late preview', sizes: ['1x1', '3x3'],
            render: ({ instance, signal }) => instance.id.startsWith('preview-') ? new Promise(resolve => pending.push({ signal, resolve })) : undefined }]
    });
    await controller.start();
    controller.setEditing(true);
    await controller.showSettings('late-autotest');
    await tick();
    const dialog = window.document.querySelector('.md-dashboard-modal--settings');
    dialog.querySelector('.md-dashboard__size-choice input[value="3x3"]').click();
    await tick();
    assert.equal(pending.length, 2);
    assert.equal(pending[0].signal.aborted, true);
    pending[0].resolve(() => disposed.push('replaced'));
    await tick();
    assert.deepEqual(disposed, ['replaced']);
    dialog.querySelector('.modal-footer .btn-outline-secondary').click();
    assert.equal(pending[1].signal.aborted, true);
    pending[1].resolve({ destroy: () => disposed.push('closed') });
    await tick();
    assert.deepEqual(disposed, ['replaced', 'closed']);
    assert.equal(window.document.querySelector('.md-dashboard-modal'), null);
});

test('Background settings preserve defaults, stage palette colors and survive save and reload', async t => {
    const { controller, window, context, requests, stored } = fixture(t, {
        items: [item('colored', 'one-size', '1x1', { days: 7 })],
        definitions: [{ type: 'one-size', titleKey: 'One size', sizes: ['1x1'], multiple: true,
            render: ({ container }) => { container.textContent = 'Widget autotest'; },
            configure: () => ({ read: () => ({ options: { days: 30 } }) }) }]
    });
    await controller.start();
    const card = controller.views.get('colored').card;
    assert.equal(card.style.backgroundColor, '', 'Unconfigured backgrounds must keep the existing stylesheet');
    assert.ok(card.querySelector('[data-dashboard-action="settings"]'), 'A single-size widget must expose background settings');
    controller.setEditing(true);
    await controller.showSettings('colored');
    let dialog = window.document.querySelector('.md-dashboard-modal--settings');
    assert.equal(dialog.querySelector('.md-dashboard__widget-colors input:checked').value, 'default');
    assert.equal(dialog.querySelectorAll('.md-dashboard__shortcut-swatch').length, 12, 'The palette must include the six restored shades and the four Figma alternatives');
    dialog.querySelector('input[value="figma-lavender"]').click();
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller._instance('colored').options.backgroundColor, 'figma-lavender');
    assert.equal(controller._instance('colored').options.days, 30);
    assert.equal(requests.length, 0, 'Applying a color must remain provisional until overview Save');
    await controller.saveEditing();
    assert.equal(stored().items[0].options.backgroundColor, 'figma-lavender');
    await controller.setContext({ data: { settings: stored() } });
    assert.equal(context.widgetBackground(controller._instance('colored').options.backgroundColor), 'var(--wj-dashboard-widget-lavender)');
    controller.setEditing(true);
    await controller.showSettings('colored');
    dialog = window.document.querySelector('.md-dashboard-modal--settings');
    dialog.querySelector('input[value="default"]').click();
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller._instance('colored').options.backgroundColor, undefined);
    assert.equal(controller.views.get('colored').card.style.backgroundColor, '', 'Default must remove the custom surface');
    for (const invalid of ['constructor', '__proto__', 'url(https://example.test)', '#fff', '#ffffff; color:red', ['mint'], {}, null]) {
        assert.equal(context.widgetBackground(invalid), '', 'Invalid stored values must never become CSS');
    }
    await controller.saveOptions('colored', { options: { backgroundColor: 'mint' } });
    await controller.showSettings('colored');
    dialog = window.document.querySelector('.md-dashboard-modal--settings');
    assert.equal(dialog.querySelector('.md-dashboard__widget-colors input:checked').value, 'figma-mint', 'Previously selected shades must resolve to their merged Figma alternative');
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller._instance('colored').options.backgroundColor, 'figma-mint');
});

test('Custom backgrounds preview immediately, reject unreadable shades and cancel without changing the draft', async t => {
    const { controller, window, requests } = fixture(t, { items: [item('custom')] });
    await controller.start();
    controller.setEditing(true);
    await controller.showSettings('custom');
    const dialog = window.document.querySelector('.md-dashboard-modal--settings');
    const picker = dialog.querySelector('color-picker');
    dialog.querySelector('input[value="custom"]').click();
    picker.dispatchEvent(new window.CustomEvent('update-color', { detail: { hex: '#112233' } }));
    assert.equal(dialog.querySelector('.md-dashboard__widget-preview .md-dashboard__widget').style.backgroundColor, 'rgb(17, 34, 51)');
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.ok(dialog.querySelector('[role="alert"]').textContent);
    assert.equal(controller._instance('custom').options.backgroundColor, undefined);
    picker.dispatchEvent(new window.CustomEvent('update-color', { detail: { hex: '#FFF1EC' } }));
    dialog.querySelector('.btn-primary').click();
    await tick();
    assert.equal(controller._instance('custom').options.backgroundColor, '#fff1ec');
    assert.equal(requests.length, 0);
    await controller.showSettings('custom');
    const reopened = window.document.querySelector('.md-dashboard-modal--settings');
    assert.equal(reopened.querySelector('.md-dashboard__widget-colors input:checked').value, 'custom');
    reopened.querySelector('input[value="figma-mint"]').click();
    reopened.querySelector('.btn-outline-secondary').click();
    assert.equal(controller._instance('custom').options.backgroundColor, '#fff1ec', 'Closing settings must discard unconfirmed colors');
});

test('A failed overview save keeps all staged changes for retry and shows no success toast', async t => {
    const { controller, notifications, requests, stored, setSaveFailure } = fixture(t, { items: [item('kept'), item('removed')], failSave: true });
    await controller.start();
    controller.setEditing(true);
    await controller.remove('removed');
    await controller.saveOptions('kept', { options: { days: 30 }, domainOptions: { formName: 'autotest' } });
    assert.equal(requests.length, 0);
    assert.equal(await controller.saveEditing(), false);
    assert.equal(controller.editing, true);
    assert.equal(controller._instance('kept').options.days, 30);
    assert.equal(stored().items.length, 2);
    assert.deepEqual(notifications, []);
    setSaveFailure(false);
    assert.equal(await controller.saveEditing(), true);
    assert.equal(controller.editing, false);
    assert.equal(stored().items.length, 1);
    assert.equal(stored().domainOptions.kept.formName, 'autotest');
    assert.deepEqual(notifications, [['The overview has been saved.', '', 10000]]);
});

test('Removing a widget offers pausable undo and Ctrl Z restores draft changes until editing closes', async t => {
    const { controller, window, requests } = fixture(t, { items: [item('first'), item('second', 'test', '3x3', { title: 'autotest' })] });
    await controller.start();
    controller.settings.domainOptions.second = { folder: 123 };
    controller.setEditing(true);
    await controller.remove('second');
    const toast = window.document.querySelector('.md-dashboard__widget-toast');
    assert.ok(toast);
    assert.ok(controller.editor.toastTimer);
    toast.dispatchEvent(new window.Event('mouseenter'));
    assert.equal(controller.editor.toastTimer, null);
    toast.dispatchEvent(new window.Event('mouseleave'));
    assert.ok(controller.editor.toastTimer);
    const undo = toast.querySelector('[data-dashboard-widget-undo]');
    undo.focus();
    assert.equal(controller.editor.toastTimer, null);
    undo.click();
    assert.equal(controller._instance('second').options.title, 'autotest');
    assert.equal(controller.settings.domainOptions.second.folder, 123);
    await controller.remove('second');
    controller.editor.clearToast();
    controller.cancelButton.focus();
    controller.cancelButton.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }));
    assert.ok(controller._instance('second'), 'Keyboard undo remains available after the toast expires');
    assert.equal(requests.length, 0);
});

test('Browser departure warns only for a dirty draft and edit-session cleanup removes the warning', async t => {
    const { controller, window } = fixture(t, { items: [item('draft')] });
    await controller.start();
    controller.setEditing(true);
    const leave = () => {
        const event = new window.Event('beforeunload', { cancelable: true });
        window.dispatchEvent(event);
        return event.defaultPrevented;
    };
    assert.equal(leave(), false);
    await controller.saveOptions('draft', { options: { days: 30 } });
    assert.equal(leave(), true);
    controller.editor.undo();
    assert.equal(leave(), false);
    await controller.saveOptions('draft', { options: { days: 90 } });
    controller.destroy();
    assert.equal(leave(), false);
});

test('Independent release preferences never persist widget drafts or disappear when the draft is cancelled', async t => {
    const { controller, window, stored } = fixture(t, { items: [item('draft')] });
    await controller.start();
    controller.setEditing(true);
    await controller.saveOptions('draft', { options: { days: 30 } });
    await controller.acknowledgeNews('autotest-news');
    assert.deepEqual(stored().items[0].options, {});
    assert.equal(stored().acknowledgedNewsVersion, 'autotest-news');
    assert.equal(controller._instance('draft').options.days, 30);
    controller.cancelEditing();
    window.document.querySelector('.md-dashboard-modal--confirm .btn-danger').click();
    assert.equal(controller.settings.acknowledgedNewsVersion, 'autotest-news');
    assert.deepEqual(copy(controller._instance('draft').options), {});
});

test('Entering shortcut editing confirms discarding dirty widgets, then shortcut edits still save immediately', async t => {
    const { controller, window, stored } = fixture(t, { items: [item('draft'), item('link', 'shortcut', '1x1')], definitions: [shortcutDefinition] });
    await controller.start();
    controller.setEditing(true);
    await controller.saveOptions('draft', { options: { days: 30 } });
    controller.editShortcutsButton.click();
    assert.equal(controller.editingShortcuts, false);
    window.document.querySelector('.md-dashboard-modal--confirm .btn-danger').click();
    assert.equal(controller.editing, false);
    assert.equal(controller.editingShortcuts, true);
    await controller.saveOptions('link', { options: { source: 'url', href: '/admin/v9/', title: 'autotest shortcut' } });
    assert.equal(stored().items.find(value => value.id === 'link').options.title, 'autotest shortcut');
    assert.deepEqual(stored().items.find(value => value.id === 'draft').options, {});
});

test('The edit toolbar counteracts smooth scrolling and releases its scroll listener on exit', async t => {
    const { controller, window } = fixture(t);
    await controller.start();
    const listeners = new Set();
    window.scrollbarMain = { addListener: listener => listeners.add(listener), removeListener: listener => listeners.delete(listener) };
    controller.toolbar.getBoundingClientRect = () => ({ top: -200 + (controller.editor?.toolbarShift || 0), height: 60 });
    controller.host.getBoundingClientRect = () => ({ bottom: 1000 });
    controller.setEditing(true);
    assert.equal(controller.toolbar.style.transform, 'translateY(248px)');
    assert.equal(listeners.size, 1);
    controller.setEditing(false);
    assert.equal(listeners.size, 0);
    assert.equal(controller.toolbar.style.transform, '');
});
