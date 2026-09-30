Feature("video.332-58806-new-welcome-page");

const videoPlan = {
    language: "sk",
    notes: "Five continuous topic shots, in the requested order. Each narration line drives one matching on-camera cue; notes are editing guidance only. Keep the clicks and results within each topic together. Durations and cue holds are estimates, not measured speech synchronization. No paid audio has been generated. Dashboard preferences are isolated in memory; statistics and search results are real. Do not send the feedback form. Documentation: https://docs.webjetcms.sk/latest/sk/redactor/admin/welcome.",
    shots: [
        {
            id: "welcome-overview",
            type: "auto",
            durationSeconds: 22,
            title: "Useful information immediately after login",
            "text-sk": `Nová úvodná stránka WebJET CMS prináša informácie, ktoré potrebujete hneď po prihlásení.
Hore máte svoje skratky, novinky aj prehľad aktívnych prihlásení.
Pod nimi vidíte návštevnosť, odoslané formuláre a obsah čakajúci na schválenie.
K rozpracovaným stránkam sa ľahko vrátite cez kartu Pokračujte v práci.`,
            notes: "Lines 1-2: hold the welcome panel. Line 3: smoothly reveal the overview and its real metrics. Line 4: show the recent-page card. End at the overview toolbar so widget editing continues from the same screen.",
            shot: async ({ I, shot, cue, scrollTo }) => {
                await cue(shot, 1, async () => {
                    await I.see("Vitajte späť", ".md-dashboard__welcome");
                });
                await cue(shot, 2, async () => {
                    await I.seeElement(".md-dashboard__shortcuts");
                    await I.seeElement(".md-dashboard__sessions");
                });
                await cue(shot, 3, async () => {
                    await scrollTo(".md-dashboard__toolbar");
                    await I.seeElement('[data-instance-id="video-traffic"] .md-dashboard-widget__metric');
                    await I.see("Formuláre", '[data-widget-type="forms"]');
                    await I.see("Na schválenie", '[data-widget-type="approvals"]');
                });
                await cue(shot, 4, async () => {
                    await I.see("Pokračujte v práci", '[data-widget-type="recent-pages"]');
                });
            }
        },
        {
            id: "customize-widget",
            type: "auto",
            durationSeconds: 48,
            title: "Add, configure, resize and drag one widget",
            "text-sk": `Prehľad si môžete prispôsobiť tomu, čo pri práci sledujete najčastejšie.
Kliknite na Upraviť prehľad a cez Pridať widget vyhľadajte Návštevnosť.
Pridáte si ďalšiu kartu a v jej menu otvoríte Nastavenia widgetu.
Zvoľte menšiu veľkosť jeden krát jeden.
Obdobie nastavte na posledných tridsať ukončených dní.
Nastavenia uložte. Nová karta teraz ukazuje návštevnosť za dlhšie obdobie a zaberá menej miesta.
Uchopte ju za rukoväť a potiahnite pred pôvodný graf.
Kliknutím na Hotovo ukončíte úpravy. Dôležité údaje tak máte na začiatku prehľadu.`,
            notes: "One uninterrupted example. Lines 1-3: editing, catalogue, add a second traffic card and open its settings. Lines 4-5: visibly choose 1 × 1, then 30 days, each with its own spoken cue. Line 6: save and read the compact result. Line 7: real pointer drag before the original traffic card; never substitute the move dialog. Line 8: finish editing. Preserve the resulting layout for subsequent shots.",
            prepare: async ({ scrollTo }) => { await scrollTo(".md-dashboard__toolbar", 0); },
            shot: async ({ I, shot, cue, scrollTo, typeText, choose, save, dragBefore, editWidgets, modal }) => {
                let added;
                await cue(shot, 1, async () => { await I.seeElement(editWidgets); });
                await cue(shot, 2, async () => {
                    await I.videoClick(editWidgets);
                    await I.waitForElement(".md-dashboard.is-editing", 10);
                    await I.videoClick(".md-dashboard__toolbar .btn-primary");
                    await I.waitForVisible(`${modal} input[type="search"]`, 10);
                    await typeText(`${modal} input[type="search"]`, "Návštevnosť");
                    await I.waitForVisible(`${modal} [data-widget-type="traffic"] button`, 10);
                });
                await cue(shot, 3, async () => {
                    await I.videoClick(`${modal} [data-widget-type="traffic"] button`);
                    await I.waitForDetached(modal, 10);
                    added = '.md-dashboard__layout [data-widget-type="traffic"]:not([data-instance-id="video-traffic"])';
                    await scrollTo(added);
                    await I.waitForElement(`${added} .md-dashboard__widget-body[aria-busy="false"]`, 30);
                    await I.videoClick(`${added} [data-bs-toggle="dropdown"]`);
                    await I.waitForVisible(`${added} [data-dashboard-action="settings"]`, 10);
                    await I.videoClick(`${added} [data-dashboard-action="settings"]`);
                    await I.waitForVisible(`${modal}.show`, 10);
                });
                await cue(shot, 4, async () => {
                    await choose(`${modal} select[id^="dashboard-size-"]`, "1 × 1");
                });
                await cue(shot, 5, async () => {
                    await choose(`${modal} .md-dashboard__settings select:has(option[value="30"])`, "Posledných 30 ukončených dní");
                });
                await cue(shot, 6, async () => {
                    await save();
                    await scrollTo(".md-dashboard__toolbar");
                    await I.waitForElement(`${added}[data-size="1x1"] .md-dashboard__widget-body[aria-busy="false"]`, 30);
                    await I.see("30", added);
                });
                await cue(shot, 7, async () => { await dragBefore(added, '[data-instance-id="video-traffic"]'); });
                await cue(shot, 8, async () => {
                    await I.videoClick(editWidgets);
                    await I.waitForElement(".md-dashboard:not(.is-editing)", 10);
                });
            }
        },
        {
            id: "search-and-help",
            type: "auto",
            durationSeconds: 44,
            title: "Find a page with previews and search the documentation",
            "text-sk": `Vyhľadávanie máte poruke priamo na úvodnej stránke.
V administrácii stačí začať písať názov stránky, napríklad gregor.
Ponuka zobrazí zodpovedajúce stránky aj s náhľadovým obrázkom a priečinkom, takže ich ľahšie rozlíšite.
Kliknutím na slovenskú stránku otvoríte priamo jej editor.
Ak potrebujete návod, do rovnakého poľa zadáte napríklad formuláre a kliknete na tlačidlo V dokumentácii.
Otvorí sa dokumentácia s vyhľadaným výrazom, rýchlejšie sa tak dostanete k potrebným informáciám.`,
            notes: "Lines 1-4: reveal search, type gregor, hold the loaded thumbnails and paths, then click the Slovak result. Close the editor and return home as an unvoiced transition. Line 5: type formuláre and click documentation scope. Line 6: show the real documentation search. The scope click submits the existing query; do not submit gregor to documentation. Reopen the actual popup URL in the original recording tab, as each tab has a separate WebM.",
            shot: async ({ I, DTE, shot, cue, scrollTo, typeText, home, openDocumentationSearch, searchInput }) => {
                const results = ".md-dashboard-widget__search-results";
                await cue(shot, 1, async () => { await scrollTo('[data-widget-type="search"]'); });
                await cue(shot, 2, async () => {
                    await typeText(searchInput, "gregor");
                    await I.waitForVisible(`${results} li`, 10);
                });
                await cue(shot, 3, async () => {
                    await I.waitForFunction(([selector]) => [...document.querySelectorAll(`${selector} .md-dashboard-widget__page-image img`)]
                        .some(image => image.complete && image.naturalWidth > 0), [results], 15);
                    await I.see("/Jet portal 4/Zo sveta financií", results);
                });
                await cue(shot, 4, async () => {
                    await I.videoClick(locate(`${results} .md-dashboard-widget__page`).withText("McGregorov"));
                    await DTE.waitForEditor();
                    await I.seeInField("#DTE_Field_title", "McGregorov obchodný údera");
                    await I.waitForVisible(".cke_wysiwyg_frame.cke_reset", 20);
                    await I.waitForFunction(() => {
                        const content = document.querySelector(".cke_wysiwyg_frame.cke_reset")?.contentDocument;
                        return Boolean(content?.body?.innerText.trim()) && [...content.images].every(image => image.complete);
                    }, 20);
                    await I.wait(2);
                });
                // Return to search between the editor and documentation narration.
                await I.videoClick("#datatableInit_modal .btn-close-editor");
                await I.waitForInvisible("#datatableInit_modal", 10);
                await home();
                await scrollTo('[data-widget-type="search"]');
                await cue(shot, 5, async () => {
                    await typeText(searchInput, "formuláre");
                    await openDocumentationSearch();
                });
                await cue(shot, 6, async () => {
                    await I.seeInField('input.gsc-input', "formuláre");
                    await I.waitForVisible(".gsc-results", 30);
                });
            }
        },
        {
            id: "personal-shortcuts",
            type: "auto",
            durationSeconds: 68,
            title: "Create shortcuts to a nested module and a specific folder",
            "text-sk": `Skratky v hornej časti vás dostanú priamo na miesta, ktoré často používate. Ušetríte tak klikanie v menu - sekciu, ktorú často navštevujete, máte dostupnú na jeden klik.
Kliknite na Upraviť skratky a potom na Pridať skratku.
V hlavnej časti vyberte Aplikácie.
V sekcii zvoľte Bannerový systém.
Nakoniec vyberte kartu Štatistika bannerov.
Skratku uložte, ukončite úpravy a otvorte ňou rovno štatistiku bannerov.
Vráťte sa na úvod a pridajte druhú skratku.
Tentoraz ako cieľ zvoľte Vlastná URL adresa.
Vložte adresu zoznamu webových stránok aj s parametrom pre priečinok číslo dvadsaťštyri.
Skratku pomenujte Zo sveta financií a uložte ju.
Po ukončení úprav na ňu kliknite. Otvorí sa priamo sekcia Zo sveta financií so zoznamom článkov, bez rozbaľovania stromu priečinkov.`,
            notes: "Lines 1-6: introduce the benefit, open shortcut editing; select Aplikácie, Bannerový systém and Štatistika bannerov in three separate spoken cues; save, finish editing and visit /apps/banner/admin/banner-stat/. Lines 7-10: return through the logo, add a custom URL, visibly type /admin/v9/webpages/web-pages-list/?groupid=24 and the title, then save. Line 11: finish editing and click the new link; assert the selected folder and loaded article list. Return home on camera as an unvoiced transition to feedback. Do not replace these steps with pre-created shortcuts.",
            shot: async ({ I, DT, shot, cue, choose, typeText, save, home, editShortcuts, addShortcut, modal }) => {
                await cue(shot, 1, async () => { await I.seeElement(".md-dashboard__shortcuts"); });
                await cue(shot, 2, async () => {
                    await I.videoClick(editShortcuts);
                    await I.videoClick(addShortcut);
                    await I.waitForVisible(`${modal}.show`, 10);
                });
                await cue(shot, 3, async () => {
                    await choose('[name="dashboardShortcutGroup"]', "Aplikácie");
                });
                await cue(shot, 4, async () => {
                    await choose('[name="dashboardShortcutSection"]', "Bannerový systém");
                });
                await cue(shot, 5, async () => {
                    await choose('[name="dashboardShortcutMenu"]', "Štatistika bannerov");
                });
                await cue(shot, 6, async () => {
                    await save();
                    await I.videoClick(editShortcuts);
                    await I.videoClick('.md-dashboard__shortcuts a[href="/apps/banner/admin/banner-stat/"]');
                    await I.seeInCurrentUrl("/apps/banner/admin/banner-stat/");
                    await DT.waitForLoader();
                    await I.see("Štatistika bannerov", ".ly-submenu");
                });
                await cue(shot, 7, async () => {
                    await home();
                    await I.videoClick(editShortcuts);
                    await I.videoClick(addShortcut);
                    await I.waitForVisible(`${modal}.show`, 10);
                });
                await cue(shot, 8, async () => {
                    await choose('[name="dashboardShortcutSource"]', "Vlastná URL adresa");
                });
                await cue(shot, 9, async () => {
                    await typeText('[name="dashboardShortcutUrl"]', "/admin/v9/webpages/web-pages-list/?groupid=24");
                    await I.seeInField('[name="dashboardShortcutUrl"]', "/admin/v9/webpages/web-pages-list/?groupid=24");
                });
                await cue(shot, 10, async () => {
                    await typeText('[name="dashboardShortcutTitle"]', "Zo sveta financií");
                    await save();
                });
                await cue(shot, 11, async () => {
                    await I.videoClick(editShortcuts);
                    await I.videoClick('.md-dashboard__shortcuts a[href="/admin/v9/webpages/web-pages-list/?groupid=24"]');
                    await I.seeInCurrentUrl("groupid=24");
                    await DT.waitForLoader();
                    await I.waitForText("Zo sveta financií", 20, "#SomStromcek .jstree-clicked");
                    await I.waitForVisible("#datatableInit tbody tr td.dt-row-edit", 20);
                });
                await home();
            }
        },
        {
            id: "send-feedback",
            type: "auto",
            durationSeconds: 27,
            title: "Invite customers to share feedback",
            "text-sk": `Na úvodnej stránke nájdete aj tlačidlo Zaslať spätnú väzbu.
Do otvoreného dialógu môžete napísať svoje postrehy, návrhy na zlepšenie práce aj chyby, ktoré nájdete.
Pri probléme môžete priložiť snímku obrazovky, aby sme mu lepšie porozumeli.
Budeme radi, keď nám napíšete. Vaše skúsenosti z každodennej práce sú najlepší spôsob, ako spoločne zlepšovať WebJET CMS.`,
            notes: "Line 1: reveal the toolbar and open the actual feedback dialog. Line 2: type a short example suggestion. Line 3: leave the attachment area readable; do not upload a file. Line 4: hold the filled dialog and invitation. Do not press Poslať. Cancel only after the runner's final tail, outside the edited film. No separate outro shot.",
            shot: async ({ I, shot, cue, scrollTo, typeText }) => {
                await cue(shot, 1, async () => {
                    await scrollTo(".md-dashboard__toolbar");
                    await I.videoClick(".md-dashboard__feedback");
                    await I.waitForVisible("#feedback_modal.show", 10);
                });
                await cue(shot, 2, async () => {
                    await typeText("#feedback-group-text", "Pomohlo by mi vyhľadávanie v zozname mojich skratiek.");
                });
                await cue(shot, 3, async () => { await I.seeElement("#feedback-upload"); });
                await cue(shot, 4, async () => { await I.seeInField("#feedback-group-text", "Pomohlo by mi vyhľadávanie v zozname mojich skratiek."); });
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan, {
        modelId: "eleven_multilingual_v2"
    });
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

/** Keeps demonstration preferences in this browser run while reading real application data. */
function dashboardVideoContext(I) {
    const { mockDashboardBootstrap, dashboardPageRoute } = require("../helpers/dashboard-browser.js");
    const modal = ".md-dashboard-modal";
    const editWidgets = ".md-dashboard__toolbar-actions > button[aria-pressed]";
    const editShortcuts = ".md-dashboard__shortcut-actions > button[aria-pressed]";
    const addShortcut = ".md-dashboard__shortcut-actions > button.btn-primary";
    const searchInput = '[data-widget-type="search"] input[type="search"]';
    const settingsRoute = "**/admin/rest/dashboard/settings";
    let settings = {
        version: 1, configured: true, shortcutsConfigured: true, legacyBookmarksHandled: true,
        domainOptions: {},
        items: [
            { id: "video-search", type: "search", size: "fullauto", options: { scope: "admin" } },
            { id: "video-sessions", type: "sessions", size: "2x3", options: {} },
            { id: "video-news", type: "news", size: "3x2", options: {} },
            { id: "video-pages", type: "shortcut", size: "1x1", options: { href: "/admin/v9/webpages/web-pages-list/" } },
            { id: "video-forms-link", type: "shortcut", size: "1x1", options: { href: "/apps/form/admin/" } },
            { id: "video-traffic", type: "traffic", size: "3x3", options: { days: 7, metric: "sessions" } },
            { id: "video-forms", type: "forms", size: "1x1", options: {} },
            { id: "video-approvals", type: "approvals", size: "1x1", options: {} },
            { id: "video-errors", type: "errors", size: "1x1", options: { days: 7 } },
            { id: "video-recent", type: "recent-pages", size: "3x2", options: {} }
        ]
    };
    const ready = async () => {
        await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
        await I.waitForFunction(() => document.querySelector("webjet-overview-dashboard")?.dashboardController?.saving === false, 20);
    };
    const ensureDashboard = async () => {
        const url = await I.grabCurrentUrl();
        if (typeof url === "string" && new URL(url).pathname !== "/admin/v9/") await I.amOnPage("/admin/v9/");
        await ready();
    };
    const scrollTo = (selector, duration = 900) => I.executeScript(async ({ selector, duration }) => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const start = scrollbar.offset.y;
        const top = document.querySelector(selector).getBoundingClientRect().top;
        const end = Math.max(0, Math.min(scrollbar.limit.y, start + top - 90));
        await new Promise(resolve => scrollbar.scrollTo(0, end, duration, { callback: resolve }));
    }, { selector, duration });
    const typeText = async (selector, value) => {
        await I.videoClick(selector);
        await I.usePlaywrightTo("type the visible demonstration value", async ({ page }) => {
            await page.locator(selector).fill("");
            await page.locator(selector).pressSequentially(value, { delay: 45 });
        });
    };
    const choose = async (selector, label) => {
        await I.videoClick(`${selector} + button`);
        await I.waitForVisible(`${modal} .dropdown-menu.show .dropdown-item`, 10);
        await I.videoClick(locate(`${modal} .dropdown-menu.show .dropdown-item`).withText(label));
        await I.waitForInvisible(`${modal} .bs-container > .dropdown-menu.show`, 10);
    };
    const save = async () => {
        await I.waitForEnabled(`${modal} .modal-footer .btn-primary`, 10);
        await I.videoClick(`${modal} .modal-footer .btn-primary`);
        await I.waitForDetached(modal, 10);
        await ready();
    };
    const home = async () => {
        await I.videoClick('.ly-sidebar .navbar-brand a[href="/admin/v9/"] img');
        await ready();
        await I.usePlaywrightTo("leave the logo tooltip before the next narration cue", async ({ page }) => {
            const welcome = await page.locator(".md-dashboard__welcome").boundingBox();
            if (!welcome) throw new Error("The welcome panel must be visible after returning home.");
            await page.mouse.move(welcome.x + 30, welcome.y + 40, { steps: 18 });
        });
    };
    const cue = async (shot, line, action) => {
        const narration = shot.narration.split("\n").filter(Boolean)[line - 1];
        if (!narration) throw new Error(`Missing narration line ${line} in ${shot.id}`);
        await I.say(`Narration cue ${shot.id}/${line}: ${narration}`);
        const started = Date.now();
        await action();
        // Approximate reading room only; replace these estimates with measured voice timing in editing.
        const remaining = narration.split(/\s+/u).length / 2.15 - (Date.now() - started) / 1000;
        if (remaining > 0) await I.wait(remaining);
    };
    const dragBefore = async (sourceSelector, targetSelector) => {
        await scrollTo(".md-dashboard__toolbar");
        const movedId = await I.grabAttributeFrom(sourceSelector, "data-instance-id");
        await I.usePlaywrightTo("drag the compact traffic card before the original chart", async ({ page }) => {
            const source = await page.locator(`${sourceSelector} .md-dashboard__drag`).boundingBox();
            const target = await page.locator(`${targetSelector} .md-dashboard__widget-header`).boundingBox();
            if (!source || !target) throw new Error("Both drag handles must be visible.");
            const from = { x: source.x + source.width / 2, y: source.y + source.height / 2 };
            const to = { x: target.x + target.width / 2, y: target.y + target.height / 2 };
            await page.mouse.move(from.x, from.y, { steps: 18 });
            await page.mouse.down();
            await page.mouse.move(from.x - 12, from.y, { steps: 3 });
            for (let step = 1; step <= 45; step++) {
                const progress = step / 45;
                const eased = progress * progress * (3 - 2 * progress);
                await page.mouse.move(from.x + (to.x - from.x) * eased, from.y + (to.y - from.y) * eased);
                await page.waitForTimeout(25);
            }
            await page.mouse.up();
        });
        await ready();
        await I.waitForFunction(([id]) => document.querySelector(".md-dashboard__layout [data-instance-id]")?.dataset.instanceId === id, [movedId], 10);
    };
    const openDocumentationSearch = async () => {
        let popupPromise;
        await I.usePlaywrightTo("observe the documentation tab", async ({ page }) => {
            popupPromise = page.context().waitForEvent("page");
        });
        await I.videoClick('[data-widget-type="search"] label:has(input[value="docs"])');
        let url;
        await I.usePlaywrightTo("retain the real documentation destination in the recording", async ({ page }) => {
            const popup = await popupPromise;
            await popup.waitForLoadState("domcontentloaded");
            url = popup.url();
            if (new URL(url).origin !== "https://docs.webjetcms.sk" || new URL(url).searchParams.get("q") !== "formuláre") {
                throw new Error("The documentation search must receive the narrated query.");
            }
            await popup.close();
            await page.bringToFront();
        });
        await I.amOnPage(url);
        await I.waitForVisible("article h1", 30);
        await I.waitForVisible("input.gsc-input", 30);
    };
    const setup = async () => {
        await I.mockRoute(settingsRoute, route => {
            if (route.request().method() === "PUT") settings = { ...route.request().postDataJSON(), configured: true };
            return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(settings) });
        });
        await mockDashboardBootstrap(I, () => ({ settings }));
        await I.amOnPage("/admin/v9/");
        await ready();
        await I.clickCss('.md-dashboard-widget__news-toggle[aria-expanded="true"]');
        await ready();
        for (const type of ["traffic", "forms", "approvals", "errors", "recent-pages"]) {
            await scrollTo(`[data-widget-type="${type}"]`, 0);
            await I.waitForElement(`[data-widget-type="${type}"] .md-dashboard__widget-body[aria-busy="false"]`, 30);
        }
        await scrollTo(".md-dashboard__welcome", 0);
        await I.waitForInvisible(".toast-container .toast", 15);
    };
    const dispose = async () => {
        await I.clickIfVisible("#feedback_modal.show .btn-close-editor");
        await I.stopMockingRoute(settingsRoute);
        await I.stopMockingRoute(dashboardPageRoute);
    };
    return { modal, editWidgets, editShortcuts, addShortcut, searchInput, cue, scrollTo, typeText, choose, save,
        home, dragBefore, openDocumentationSearch, setup, ensureDashboard, dispose };
}

Scenario("YouTube thumbnail", async ({ I, login }) => {
    login("admin");
    const context = dashboardVideoContext(I);
    try {
        await context.setup();
        await I.videoTitle("Váš WebJET CMS\nVáš prehľad", 50, "glow");
    } finally {
        await context.dispose();
    }
}).tag("@title");

Scenario("332-58806-new-welcome-page", async ({ I, login, DTE, DT }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const context = dashboardVideoContext(I);
    try {
        await recordVideoPlan(I, {
            plan: videoPlan,
            context: { ...context, DTE, DT },
            setup: async () => {
                login("admin");
                await context.setup();
            },
            // Reuse the current dashboard between topics; retakes also get a complete one-time baseline.
            prepare: async () => { await context.ensureDashboard(); }
        });
    } finally {
        await context.dispose();
    }
}).tag("@video");
