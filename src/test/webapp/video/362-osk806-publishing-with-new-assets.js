Feature("video.362-osk806-publishing-with-new-assets");

// Source: https://github.com/webjetcms/webjetcms/pull/362
// Documentation: https://docs.webjetcms.sk/latest/sk/redactor/webpages/planned-publishing
// Run from src/test/webapp: npm run video video/362-osk806-publishing-with-new-assets.js
const videoPlan = {
    language: "sk",
    notes: "Slovak editorial manual, 16:9. Three continuous workflows. Remove slates, setup and publication polling; append the complete standard WebJET outro. Product content is an isolated Roadster demonstration with illustrative specifications requested by the user.",
    shots: [
        {
            id: "prepare-assets",
            type: "auto",
            durationSeconds: 77,
            title: "Prepare the page and protected product assets",
            "text-sk": "Predstavujete nový produkt a jeho fotografie nesmú byť dostupné skôr než v deň uvedenia? Vo WebJET CMS môžete naplánovať text stránky spolu s obrázkami a dokumentmi. Pôvodná stránka zostane dovtedy nezmenená. V karte Perex nastavte dátum začiatku a zaškrtnite naplánovanie zmeny stránky po tomto dátume. Potom v obsahu otvorte vlastnosti obrázka. V časti Médiá tejto stránky vyberte Obrázky – plánované a nahrajte pripravenú fotografiu. Pri aktualizácii existujúceho obrázka použite rovnaký názov a zodpovedajúci priečinok. Verejný súbor sa prepíše až pri publikovaní. Nový obrázok nahráte rovnako, iba s novým názvom. Vyberte nahratý obrázok. V jeho adrese vidíte cestu do chráneného priečinka. Bez prihlásenia tieto podklady nie sú dostupné. Potvrďte výber a upravte aj text stránky. Pripojené dokumenty pripravíte obdobne v karte Plánované médiá. Súbory nahrávajte do priečinka Súbory – plánované. Nakoniec v Perexe skontrolujte termín a kliknite na Naplánovať. V ukážke je termín o chvíľu, aby sme si výsledok mohli overiť.",
            notes: "Show the existing product, enable scheduling, open its image dialog, upload a same-name replacement into the planned page folder, reselect it, show its protected URL, confirm, edit the visible paragraph, show planned media, check the near-future date and save. Anonymous denial is asserted off-screen. Keep each result visible for its sentence.",
            prepare: async ({ openFixture }) => { await openFixture(); },
            shot: async ({ I, DTE, state, cue, enablePlanning, openImage, uploadPlannedImage, editCopy, savePlan }) => {
                // 1. The existing product supports the opening promise.
                await cue("existing-product");
                await I.wait(9);
                // 2. Enable planning before selecting the protected assets.
                await I.videoClick("#pills-dt-datatableInit-perex-tab");
                await enablePlanning(true);
                await cue("planning-enabled");
                await I.wait(5);
                await I.videoClick("#pills-dt-datatableInit-content-tab");
                await openImage(true);
                await cue("image-dialog");
                await uploadPlannedImage(true);
                await cue("protected-image-url");
                await I.wait(13);
                await I.switchTo();
                await I.videoClick(".cke_dialog_ui_button_ok");
                await I.waitForInvisible("#wjImageIframeElement", 10);
                await cue("image-applied");
                await I.wait(5);
                // 3. Edit the copy on camera, then explain the related media tab.
                await editCopy();
                await cue("text-updated");
                await I.wait(5);
                await I.videoClick("#pills-dt-datatableInit-media-tab");
                await I.waitForText("Plánované médiá", 10, "#pills-dt-datatableInit-media-tab");
                await cue("planned-media");
                await I.wait(10);
                await I.videoClick("#pills-dt-datatableInit-perex-tab");
                await savePlan(true);
                await cue("plan-saved");
            }
        },
        {
            id: "published-result",
            type: "auto",
            durationSeconds: 15,
            title: "Verify the actual timed publication",
            "text-sk": "Na verejnej stránke je zatiaľ pôvodný text aj obrázok. Čakanie na termín teraz preskočíme. Po publikovaní stránku obnovte. Vidíte nový text aj novú fotografiu. Zverejnili sa spolu, v naplánovanom čase.",
            notes: "Keep the public page with old content visible, poll anonymously without refreshing the visible page, then reload once the real publisher has completed. Cut only the polling interval between wait-start and publication-ready cues. Verify content and the published image bytes.",
            prepare: async ({ I, state, ensurePlan }) => {
                await ensurePlan();
                await I.amOnPage(state.publicUrl);
                await I.waitForVisible("#video-product", 20);
                await I.waitForText("Červený Roadster · 4 miesta · dojazd 600 míľ", 10, "#video-copy");
            },
            shot: async ({ I, state, cue, waitForPublication, publicFetch }) => {
                await cue("public-before");
                await I.wait(6);
                await cue("wait-start");
                await waitForPublication();
                await cue("publication-ready");
                await I.refreshPage();
                await I.waitForText(state.newCopy, 20, "#video-copy");
                await I.waitForFunction(() => [...document.querySelectorAll("#video-product img")].every(image => image.complete && image.naturalWidth > 0), 20);
                const imageUrl = await I.grabAttributeFrom("#video-product img", "src");
                const response = await publicFetch(imageUrl);
                const result = { url: imageUrl, status: response.status, bytes: (await response.arrayBuffer()).byteLength };
                await I.assertFalse(result.url.includes("/protected/"));
                await I.assertEqual(result.status, 200);
                await I.assertEqual(result.bytes, state.newImageBytes);
                await cue("public-after");
                await I.saveScreenshot("362-published-result.png");
                await I.wait(9);
            }
        },
        {
            id: "new-page",
            type: "auto",
            durationSeconds: 41,
            title: "Save a new unpublished page before planning its assets",
            "text-sk": "Pri úplne novej stránke je navyše jeden krok. Po vyplnení obsahu nastavte v Perexe termín a zapnite plánovanie. Zobrazí sa výzva na prvé uloženie. Kliknite na Uložiť a pokračovať. Stránka sa uloží ako nezverejnená a editor sa znova otvorí. WebJET tak získa identifikátor stránky, ktorý potrebuje pre jej chránený priečinok. Až teraz pripravte obrázky a súbory v plánovaných priečinkoch. Samotné prvé uloženie ešte termín nepotvrdí. Keď máte všetko hotové, kliknite na Naplánovať. Podrobný postup nájdete v dokumentácii WebJET CMS.",
            notes: "Start with a filled unsaved page. Set the date, enable scheduling, hold the real save-first prompt, accept it, show the reopened Perex and the planned image/file folders, then confirm the plan. Verify the new page remains unavailable anonymously. End on the scheduled page before the standard brand animation.",
            prepare: async ({ prepareNewPage }) => { await prepareNewPage(); },
            shot: async ({ I, DTE, state, cue, publicFetch }) => {
                await cue("new-unsaved");
                await I.wait(4);
                await I.videoClick("#pills-dt-datatableInit-perex-tab");
                await I.fillField("#DTE_Field_publishStartDate", I.formatDateTime(Date.now() + 86400000));
                await I.pressKey("Tab");
                await I.videoClick("#DTE_Field_editorFields-publishAfterStart_0");
                const prompt = '#toast-container-webjet .toast[role="dialog"]';
                await I.waitForText("Najskôr uložte novú stránku", 10, prompt);
                await cue("save-first-prompt");
                await I.wait(6);
                await I.videoClick(locate(`${prompt} button.btn-primary`).withText("Uložiť a pokračovať"));
                await I.waitForInvisible(prompt, 15);
                await I.waitForFunction(() => window.webpagesDatatable.EDITOR.mode() === "edit" && window.webpagesDatatable.EDITOR.currentJson.docId > 0, 20);
                await DTE.waitForEditor();
                state.newDocId = await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.docId);
                await I.waitForText("Plánované médiá", 15, "#pills-dt-datatableInit-media-tab");
                await I.seeCheckboxIsChecked("#DTE_Field_editorFields-publishAfterStart_0");
                await cue("new-page-reopened");
                await I.wait(6);
                await I.videoClick(locate(".DTE_Field_Name_perexImage").find("button.btn-outline-secondary").first());
                await I.waitForElement("#modalIframeIframeElement", 15);
                await I.switchTo("#modalIframeIframeElement");
                await I.waitForElement("#nav-iwcm_fs_ap_volume_", 20);
                if (await I.grabNumberOfVisibleElements(locate(".elfinder-navbar-dir").withText("Obrázky - plánované")) === 0) {
                    await I.clickCss("#nav-iwcm_fs_ap_volume_");
                }
                await I.waitForText("Obrázky - plánované", 15, ".elfinder-navbar-wrapper");
                await I.videoClick(locate(".elfinder-navbar-dir").withText("Obrázky - plánované"));
                await I.waitForElement(locate(".elfinder-navbar-dir.ui-state-active").withText("Obrázky - plánované"), 10);
                await cue("new-page-folders");
                await I.wait(6);
                await I.switchTo();
                await I.videoClick("#modalIframe button.btn-close");
                await I.waitForInvisible("#modalIframe", 10);
                await I.videoClick("#datatableInit_modal .DTE_Form_Buttons button.btn-primary");
                await DTE.waitForLoader();
                await I.waitForInvisible("#datatableInit_modal", 15);
                await cue("new-page-scheduled");
                const hidden = await I.executeScript(async id => {
                    const page = await window.$.get(`/admin/rest/web-pages/${id}`);
                    return !page.available && page.editorFields.plannedHistoryId > 0;
                }, state.newDocId);
                await I.assertTrue(hidden);
                const anonymous = await publicFetch(`/showdoc.do?docid=${state.newDocId}`);
                await I.assertFalse((await anonymous.text()).includes("Pripravujeme novú kolekciu."));
                await I.wait(5);
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => { I.generateAudio(videoPlan); }).tag("@audio");
Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

/** Builds narrowly scoped persistent demo data and records semantic editing cues. */
function publishingVideoContext({ I, DT, DTE }) {
    const fs = require("node:fs");
    const path = require("node:path");
    const webRoot = path.resolve(__dirname, "../../../main/webapp");
    const output = path.resolve(__dirname, "../../../../docs/feature-video");
    fs.mkdirSync(output, { recursive: true });
    const key = `autotest-video-362-${String(Date.now()).slice(-5)}`;
    const state = { key, newCopy: "Raketový pohon · 2 miesta · dojazd 300 míľ", cueLog: [] };
    const source = path.join(__dirname, "assets/362-roadster");
    const uploads = path.join(output, key);
    fs.mkdirSync(uploads);
    fs.copyFileSync(path.join(source, "roadster-new.jpg"), path.join(uploads, "produkt.jpg"));
    state.newImageBytes = fs.statSync(path.join(uploads, "produkt.jpg")).size;
    // A fresh Node request has no browser cookies or the admin CSRF interceptor.
    const publicFetch = url => fetch(new URL(url, process.env.CODECEPT_URL || "http://iwcm.interway.sk"));
    const waitForPublication = async () => {
        const deadline = Date.now() + 150000;
        while (Date.now() < deadline) {
            const response = await publicFetch(state.publicUrl);
            const published = (await response.text())
                .replace(/&nbsp;|&#160;|\u00a0/g, " ")
                .replace(/&middot;|&#183;/g, "·");
            if (published.includes(state.newCopy)) return;
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        throw new Error("The real publisher did not publish the demonstration within 150 seconds.");
    };
    const cue = async name => {
        // The final edit uses encoded frames; wall-clock cues only help find those frames.
        state.cueLog.push({ name, time: Date.now() });
        fs.writeFileSync(path.join(output, `${key}-cues.json`), JSON.stringify(state, null, 2));
        await I.say(`VIDEO CUE: ${name}`);
        await I.saveScreenshot(`362-${name}.png`);
    };
    const html = image => `<style>#video-product img {height:300px!important;width:auto!important;max-width:100%;object-fit:contain}</style><div id="video-product" style="max-width:900px;margin:24px auto;padding:28px;font-family:Arial,sans-serif;background:#fff;color:#17243b"><h1 style="font-size:34px;margin-bottom:10px">Nový Tesla Roadster</h1><p id="video-copy" style="font-size:23px;margin-bottom:24px">Červený Roadster · 4 miesta · dojazd 600 míľ</p><p><img id="video-photo" src="${image}" alt="Produktová fotografia Roadstera" style="height:300px;width:auto;max-width:100%;object-fit:contain" /></p><p style="color:#64748b;font-size:15px">Fotografie: Editor928111 (CC BY-SA 4.0) · Courtesy of Tesla, Inc.</p></div>`;
    const openFixture = async () => {
        await I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${state.docId}`);
        await DTE.waitForEditor();
        await I.waitForElement(".cke_wysiwyg_frame", 20);
    };
    const setup = async () => {
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
        await DT.waitForLoader();
        state.originalEditorMode = await I.executeScript(() => window.WJ.getAdminSetting("editorTypeForced") || "pageBuilder");
        await I.executeScript(() => window.WJ.setAdminSetting("editorTypeForced", "standard"));
        await I.click(DT.btn.add_button);
        await DTE.waitForEditor();
        await DTE.fillField("title", `Roadster – ukážka ${key}`);
        await I.executeScript(key => {
            const editor = window.webpagesDatatable.EDITOR;
            editor.field("generateUrlFromTitle").val(false);
            editor.field("virtualPath").val(`/test-stavov/${key}.html`);
            editor.field("editorVirtualPath").val(`${key}.html`);
        }, key);
        const enabled = await I.executeScript(() => window.webpagesDatatable.EDITOR.currentJson.editorFields.plannedPublishingEnabled);
        await I.assertTrue(enabled, "Enable plannedPublishingEnabled on the recording instance first.");
        const folder = path.join(webRoot, "images/test-stavov", `roadster-ukazka-${key}`);
        fs.mkdirSync(folder, { recursive: true });
        fs.copyFileSync(path.join(source, "roadster-old.jpg"), path.join(folder, "produkt.jpg"));
        state.imageFolder = folder;
        state.liveImage = `/images/test-stavov/roadster-ukazka-${key}/produkt.jpg`;
        await I.clickCss("#pills-dt-datatableInit-content-tab");
        await DTE.fillCkeditor(html(state.liveImage));
        await I.executeScript(() => window.webpagesDatatable.EDITOR.one("submitSuccess", (event, json) => { window.videoCreatedDocId = json.data[0].docId; }));
        await DTE.save();
        state.docId = await I.executeScript(() => window.videoCreatedDocId);
        await I.assertTrue(state.docId > 0);
        state.publicUrl = `/showdoc.do?docid=${state.docId}`;
        await cue("fixture-created");
    };
    const enablePlanning = async animated => {
        await I.fillField("#DTE_Field_publishStartDate", I.formatDateTime(Date.now() + 10 * 60000));
        await I.pressKey("Tab");
        if (animated) await I.videoClick("#DTE_Field_editorFields-publishAfterStart_0");
        else await I.clickCss("#DTE_Field_editorFields-publishAfterStart_0");
        await I.waitForText("Súbory a obrázky nahrávajte do plánovaných priečinkov.", 15, ".planned-change-notice");
        await I.waitForText("Plánované médiá", 10, "#pills-dt-datatableInit-media-tab");
    };
    const openImage = async animated => {
        await I.switchTo(".cke_wysiwyg_frame");
        if (animated) await I.videoClick("#video-photo");
        else await I.clickCss("#video-photo");
        await I.doubleClick("#video-photo");
        await I.switchTo();
        await I.waitForElement("#wjImageIframeElement", 15);
        await I.switchTo("#wjImageIframeElement");
        await I.waitForElement("#nav-iwcm_fs_ap_volume_", 20);
        await I.clickCss("#nav-iwcm_fs_ap_volume_");
        await I.waitForText("Obrázky - plánované", 20, ".elfinder-navbar-wrapper");
        await I.waitForInvisible(".elfinder-notify", 15);
    };
    const uploadPlannedImage = async animated => {
        const folder = locate(".elfinder-navbar-dir").withText("Obrázky - plánované");
        if (animated) await I.videoClick(folder); else await I.click(folder);
        await I.waitForElement(locate(".elfinder-navbar-dir.ui-state-active").withText("Obrázky - plánované"), 10);
        await I.wait(3);
        if (animated) await I.videoClick(".elfinder-button-icon-upload"); else await I.clickCss(".elfinder-button-icon-upload");
        await I.attachFile("input[type=file]", path.relative(path.resolve(__dirname, ".."), path.join(uploads, "produkt.jpg")));
        await I.usePlaywrightTo("wait for the protected upload URL", async ({ page }) => {
            const frame = await (await page.$("#wjImageIframeElement")).contentFrame();
            await frame.waitForFunction(() => document.querySelector("#txtUrl").value.includes("/planned-changes/"), null, { timeout: 20000 });
        });
        await I.waitForInvisible(".elfinder-notify", 20);
        // Re-enter the folder after the upload callback has selected the protected URL.
        // Cut this readiness refresh from the final film; the upload and selection remain visible.
        await cue("upload-refresh-start");
        await I.clickCss('.elfinder-navbar-dir[title="Médiá tejto stránky/Obrázky"]');
        await I.waitForElement('.elfinder-navbar-dir.ui-state-active[title="Médiá tejto stránky/Obrázky"]', 10);
        await I.click(folder);
        await I.waitForElement(locate(".elfinder-navbar-dir.ui-state-active").withText("Obrázky - plánované"), 10);
        await I.waitForElement('.elfinder-cwd-filename[title="produkt.jpg"]', 20);
        if (animated) await I.videoClick('.elfinder-cwd-filename[title="produkt.jpg"]'); else await I.clickCss('.elfinder-cwd-filename[title="produkt.jpg"]');
        state.privateImage = await I.grabValueFrom("#txtUrl");
        await I.assertContain(state.privateImage, `/files/protected/planned-changes/${state.docId}/`);
        await I.assertContain(state.privateImage, `/files/protected/planned-changes/${state.docId}${state.liveImage}`);
        const denial = (await publicFetch(state.privateImage)).status;
        await I.assertEqual(denial, 404);
    };
    const editCopy = async () => {
        await I.switchTo(".cke_wysiwyg_frame");
        await I.videoClick("#video-copy");
        // Select only this paragraph; platform-specific Home/End shortcuts can include adjacent images.
        await I.usePlaywrightTo("replace the selected product paragraph", async ({ page }) => {
            const frame = await (await page.$(".cke_wysiwyg_frame")).contentFrame();
            await frame.evaluate(() => {
                const range = document.createRange();
                range.selectNodeContents(document.querySelector("#video-copy"));
                const selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
            });
            await page.keyboard.type(state.newCopy, { delay: 50 });
        });
        await I.waitForText(state.newCopy, 10, "#video-copy");
        await I.seeElement("#video-photo");
        await I.switchTo();
    };
    const savePlan = async animated => {
        state.publishAt = Date.now() + 35000;
        await I.fillField("#DTE_Field_publishStartDate", I.formatDateTime(state.publishAt));
        await I.pressKey("Tab");
        await I.wait(4);
        if (animated) await I.videoClick("#datatableInit_modal .DTE_Form_Buttons button.btn-primary");
        else await I.clickCss("#datatableInit_modal .DTE_Form_Buttons button.btn-primary");
        await DTE.waitForLoader();
        await I.waitForInvisible("#datatableInit_modal", 20);
        state.planned = true;
    };
    const ensurePlan = async () => {
        if (state.planned) return;
        // Isolated result retakes prepare the same real workflow before their shot slate.
        await openFixture();
        await I.clickCss("#pills-dt-datatableInit-perex-tab");
        await enablePlanning(false);
        await I.clickCss("#pills-dt-datatableInit-content-tab");
        await openImage(false);
        await uploadPlannedImage(false);
        await I.switchTo();
        await I.clickCss(".cke_dialog_ui_button_ok");
        await I.waitForInvisible("#wjImageIframeElement", 10);
        await editCopy();
        await I.clickCss("#pills-dt-datatableInit-perex-tab");
        await savePlan(false);
    };
    const prepareNewPage = async () => {
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
        await DT.waitForLoader();
        await I.click(DT.btn.add_button);
        await DTE.waitForEditor();
        await DTE.fillField("title", `Nová kolekcia ${key}`);
        await I.clickCss("#pills-dt-datatableInit-content-tab");
        await DTE.fillCkeditor("<h1>Nová kolekcia Roadster</h1><p>Pripravujeme novú kolekciu.</p>");
    };
    const cleanup = async () => {
        await I.switchTo();
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
        await DT.waitForLoader();
        await I.executeScript(mode => window.WJ.setAdminSetting("editorTypeForced", mode), state.originalEditorMode);
        await DT.filterContains("title", key);
        await DT.deleteAll();
        for (const id of [state.docId, state.newDocId].filter(Boolean)) {
            fs.rmSync(path.join(webRoot, "files/protected/planned-changes", String(id)), { recursive: true, force: true });
        }
        fs.rmSync(state.imageFolder, { recursive: true, force: true });
        fs.rmSync(uploads, { recursive: true, force: true });
    };
    return { state, cue, publicFetch, waitForPublication, setup, cleanup, openFixture, enablePlanning, openImage, uploadPlannedImage, editCopy, savePlan, ensurePlan, prepareNewPage };
}

Scenario("YouTube thumbnail", async ({ I, DT, DTE, login }) => {
    if (process.argv.includes("dry-run")) return;
    const context = publishingVideoContext({ I, DT, DTE });
    login("admin");
    await context.setup();
    await context.openFixture();
    await I.clickCss("#pills-dt-datatableInit-perex-tab");
    await context.enablePlanning(false);
    await I.clickCss("#pills-dt-datatableInit-content-tab");
    await context.openImage(false);
    await context.uploadPlannedImage(false);
    await I.switchTo();
    await I.videoTitle("Text aj obrázky\nv správnom čase", 50, "glow");
    await context.cleanup();
}).tag("@title");

Scenario("362-osk806-publishing-with-new-assets", async ({ I, DT, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const context = publishingVideoContext({ I, DT, DTE });
    await recordVideoPlan(I, { plan: videoPlan, context, setup: async () => { login("admin"); await context.setup(); } });
    await context.cleanup();
}).tag("@video");
