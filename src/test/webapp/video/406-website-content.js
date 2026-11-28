Feature("video.406-website-content");

// Homepage overview for PR 406. The PDF is the real fixture from the AI documentation.
// From src/test/webapp: npm run video:plan video/406-website-content.js
// Record with npm run video or npm run video:shots. Audio generation uses ElevenLabs credits.
// Full editor take: CODECEPT_VIDEO_ZOOM=1 VIDEO_SHOT=block-library npm run video video/406-website-content.js
const videoPlan = {
    language: "sk",
    notes: "16:9 homepage film. Frame the complete CMS viewport. Keep the insertion point, library choice, insertion and move in a single shot. Use short, modest punch-ins for text entry. Remove slates, loading, setup and cleanup. Append the standard WebJET outro once.",
    shots: [
        {
            id: "pdf-page", type: "auto", durationSeconds: 17, title: "From a salon brief to a page",
            "text-sk": "Máte ponuku služieb v PDF a chcete ju dostať na web? Vo WebJET CMS priložíte súbor do četu s asistentom Webový dizajnér. Umelá inteligencia z vašej ponuky vytvorí návrh stránky. Keď vám vyhovuje, vložíte ho do editora ako samostatné bloky.",
            notes: "Show the designer, the attached salon PDF, the proposal and the accepted salon page. Cut the generation wait.",
            prepare: async ({ I, openPage, frame }) => {
                await openPage(true);
                await I.switchTo(frame);
                await I.clickCss(".cke_button__aibutton_icon");
                await I.switchTo();
                await I.waitForVisible("button.btn-ai-action", 15);
            },
            shot: async ({ I, frame, host, state, saveSalon }) => {
                await I.videoClick(locate("button.btn-ai-action").withText("Webový dizajnér (PageBuilder)").withChild(locate("span.provider").withText("Gemini")));
                await I.waitForEnabled(host + " .btn-ai-chat-attach", 15);
                await I.attachFile(host + ' input[type=file]', "screenshots/generator/ai/salon-linia.pdf");
                await I.waitForText("salon-linia.pdf", 10, host + " .ai-chat-composer");
                await I.fillField(host + " .ai-chat-input", "Podľa PDF vytvor stránku salónu Línia. Priprav úvod, služby a otváracie hodiny ako tri samostatné sekcie. Kontakt a mapu zatiaľ vynechaj.");
                await I.videoClick(host + " .btn-ai-chat-send");
                const replace = host + ' button[data-ai-action="replace"]';
                await I.waitForVisible(replace, 240);
                await I.waitForVisible(host + " .ai-chat-assistant iframe", 20);
                await I.videoClick(replace);
                await I.videoClick(host + " .ai-chat-minimize");
                await I.switchTo(frame);
                await I.waitForText("Línia", 20, "#wjInline-docdata");
                await I.waitForElement("#wjInline-docdata > .pb-section ~ .pb-section", 20);
                await I.scrollPageToTop();
                await saveSalon();
                state.hasPage = true;
                await I.switchTo();
            }
        },
        {
            id: "block-library", type: "auto", durationSeconds: 15, title: "Insert and move a library block",
            "text-sk": "Chýba vám na stránke kontakt? V knižnici Page Buildera si vyberiete hotový blok a vložíte ho na stránku. Jedným posunutím ho dostanete vyššie, bližšie k ponuke služieb.",
            notes: "Keep the library thumbnails, the inserted section and its move upwards. Use the same salon page in a continuous take.",
            prepare: async ({ I, ensureSalon, frame }) => {
                await ensureSalon();
                await I.switchTo(frame);
                // Start at the insertion site with the existing last section in view.
                await I.executeScript(() => document.querySelector("#wjInline-docdata > .pb-section:last-of-type h2")?.scrollIntoView({ block: "center", behavior: "instant" }));
                await I.switchTo();
            },
            shot: async ({ I, frame, action }) => {
                await I.switchTo(frame);
                await I.videoClick(action("insert"));
                await I.waitForVisible(".pb-insert-point[data-type=section]", 15);
                // Identify the real insertion button following the last section, including on a retake.
                await I.executeScript(() => {
                    const end = window.pageBuilder.ui.insertPoints.find(p => p.type === "section" && p.previous && !p.next);
                    end.button.attr("data-homepage-insert", "end");
                });
                await I.videoClick('[data-homepage-insert="end"]');
                await I.videoClick(".pb-library .library-tab-link[data-library-type=library]");
                const group = '.pb-library [data-library-item-id="c2VjdGlvbi9Db250YWN0"]';
                await I.videoClick(group + " .library-group-toggle");
                await I.videoClick(locate(".pb-library .library-tab-item-button__toggler.active .library-full-width-item").first());
                await I.waitForInvisible(".pb-library", 15);
                // Mark the actual neighbors to verify their order after the UI move.
                await I.executeScript(() => {
                    const added = window.pageBuilder.ui.selected.closest(".pb-section");
                    // Personalize the unsaved contact block before filming its finished position.
                    added.querySelector("h2").textContent = "Kontakt";
                    added.querySelector("h2 + p").textContent = "Máte otázku alebo si chcete dohodnúť termín? Napíšte nám.";
                    added.dataset.homepageBlock = "contact";
                    added.previousElementSibling.dataset.homepageBlock = "before-contact";
                });
                await I.waitForElement('[data-homepage-block="before-contact"] + [data-homepage-block="contact"]', 10);
                await I.videoClick(action("more"));
                await I.videoClick(action("previous"));
                await I.waitForElement('[data-homepage-block="contact"] + [data-homepage-block="before-contact"]', 10);
                // Follow the moved section so the canvas and the structure show the same result.
                await I.executeScript(() => document.querySelector('[data-homepage-block="contact"] h2').scrollIntoView({ block: "center", behavior: "smooth" }));
                await I.videoClick(action("structure"));
                await I.waitForText("Otváracie hodiny", 10, ".pb-structure");
                await I.switchTo();
            }
        },
        {
            id: "salon-image", type: "auto", durationSeconds: 12, title: "Generate a photograph for the salon",
            "text-sk": "Aj ilustračný obrázok vytvoríte priamo v editore. Umelej inteligencii stačí opísať, čo má zachytiť. Pre salón si tak pripravíte fotografiu interiéru a vložíte ju k textu.",
            notes: "Show the actual image prompt, generated preview and the photograph in the page. Remove the provider wait and filename entry.",
            prepare: async ({ I, ensureSalon, frame }) => {
                await ensureSalon();
                await I.switchTo(frame);
                if (await I.grabNumberOfVisibleElements(".pb-structure")) await I.pressKey("Escape");
                const image = "#wjInline-docdata .pb-column img";
                if (await I.grabNumberOfVisibleElements(image)) {
                    await I.click(locate(image).first());
                } else {
                    await I.click(locate("#wjInline-docdata h1, #wjInline-docdata h2").first());
                    await I.pressKey("End");
                    await I.pressKey("Enter");
                    await I.clickCss(".cke_button__image");
                }
                await I.waitForVisible("#wjImageIframeElement", 15);
                await I.switchTo("#wjImageIframeElement");
                await I.click(locate("button.btn-ai").first());
            },
            shot: async ({ I, frame, state, cacheImage }) => {
                const assistant = locate("button.btn-ai-action").withText("Vytvoriť nový obrázok").withChild(locate("span.provider").withText("OpenAI"));
                await I.videoClick(assistant);
                await I.waitForVisible("#ai-user-prompt", 15);
                await I.fillField("#ai-user-prompt", "Svetlý interiér kaderníckeho salónu, dubové drevo, veľké zrkadlá a ranné svetlo. Realistická fotografia na šírku, bez ľudí a bez nápisov.");
                await I.videoClick(locate("#toast-container-ai-content button").withText("Generovať"));
                await I.waitForVisible(".ai-image-preview-div img", 300);
                await I.fillField("#generated-image-name", state.imageName);
                await I.videoClick("button.select-image");
                await I.waitForInvisible("button.select-image", 30);
                await I.seeInField("#txtUrl", state.imageName);
                state.imageUrl = await I.grabValueFrom("#txtUrl");
                await I.switchTo();
                await I.switchTo(frame);
                await I.videoClick(".cke_dialog_ui_button_ok");
                await I.waitForVisible('#wjInline-docdata img[src*="' + state.imageName + '"]', 20);
                await I.click(locate("#wjInline-docdata h1").first());
                await I.pressKey("Escape");
                cacheImage();
                await I.switchTo();
            }
        },
        {
            id: "publication-date", type: "auto", durationSeconds: 8, title: "Schedule the page in Perex",
            "text-sk": "Stránku môžete pripraviť vopred. Na karte Perex zadáte dátum aj čas a zapnete zverejnenie stránky po tomto termíne.",
            notes: "Hold the populated start date together with the checked scheduling option. Discard the unsaved settings.",
            prepare: async ({ I, openPage, showIllustration }) => {
                await openPage(false);
                await showIllustration(true);
                await I.executeScript(() => {
                    const editor = window.webpagesDatatable.EDITOR;
                    editor.field("title").val("Kadernícky salón Línia");
                    document.querySelector("#datatableInit_modal h5.modal-title").textContent = WJ.translate("button.edit") + ": " + editor.field("title").val();
                    editor.field("htmlData").val("Dámske a pánske strihy v salóne Línia.");
                });
                await I.usePlaywrightTo("move the pointer away from the maximize button", async ({ page }) => page.mouse.move(670, 82));
            },
            shot: async ({ I, date }) => {
                await I.videoClick("#pills-dt-datatableInit-perex-tab");
                await I.fillField("#DTE_Field_publishStartDate", date);
                await I.pressKey("Tab");
                await I.videoClick("#DTE_Field_editorFields-publishAfterStart_0");
                await I.seeCheckboxIsChecked("#DTE_Field_editorFields-publishAfterStart_0");
                await I.seeInField("#DTE_Field_publishStartDate", date);
            }
        },
        {
            id: "course-registration", type: "auto", durationSeconds: 12, title: "Set a field rule and show the registration",
            "text-sk": "Prihlášku na online kurz rozdelíte na kroky. Pri telefónnom čísle si vyberiete, v akom formáte ho môže návštevník zadať. Takto vyzerá prvý krok registrácie priamo na webe.",
            notes: "Show the two steps, select the phone validation in the field editor, then cut to the public first step. Leave the shared form untouched.",
            prepare: async ({ I }) => {
                await I.amOnPage("/apps/form/admin/form-steps/?formName=Registracia-na-online-kurz");
                await I.waitForText("Telefónne číslo", 20, "#formItemsDataTable");
            },
            shot: async ({ I, DTE }) => {
                await I.videoClick(locate("#formItemsDataTable td").withText("Telefónne číslo"));
                await I.videoClick("#formItemsDataTable_wrapper .buttons-edit");
                await DTE.waitForEditor("formItemsDataTable");
                const field = ".DTE_Field_Name_regexValidationArr";
                await I.videoClick(field + " button.dropdown-toggle");
                await I.fillField(".bs-container .bs-searchbox input", "phone -");
                await I.videoClick(locate(".bs-container .dropdown-item").withText("phone - telefónne číslo"));
                await I.pressKey("Escape");
                await I.see("phone - telefónne číslo", field + " button.dropdown-toggle");
                await DTE.cancel();
                await I.amOnPage("/showdoc.do?docid=156109");
                await I.waitForVisible("#f1-meno-1", 20);
                await I.scrollTo("#f1-meno-1", 0, -170);
                const phone = "#f1-telefon-1";
                await I.videoClick(phone);
                await I.fillField(phone, "+421901234567");
                await I.seeInField(phone, "+421901234567");
            }
        },
        {
            id: "document-release", type: "auto", durationSeconds: 15, title: "Choose when a document becomes available",
            "text-sk": "Rovnako si viete načasovať aj dokumenty na stiahnutie. V Manažéri dokumentov vyberiete možnosť Nahrať dokument neskôr a určíte, odkedy má byť súbor dostupný. Ďalšie možnosti nájdete v dokumentácii WebJET CMS.",
            notes: "Show the document list and the actual deferred-upload controls. Cut to the documentation address for the last sentence, then append the standard outro.",
            prepare: async ({ I, DT }) => {
                await I.amOnPage("/apps/file-archive/admin/");
                await DT.waitForLoader();
                await I.waitForVisible("#fileArchiveDataTable tbody tr", 20);
            },
            shot: async ({ I, DTE, date }) => {
                await I.videoClick("#fileArchiveDataTable_wrapper .buttons-create");
                await DTE.waitForEditor("fileArchiveDataTable");
                // Keep the demonstration free of the account's default notification address.
                await I.executeScript(() => {
                    fileArchiveDataTable.EDITOR.field("editorFields.emails").val("");
                    fileArchiveDataTable.EDITOR.field("virtualFileName").val("Cenník služieb");
                });
                await I.videoClick("#DTE_Field_editorFields-saveLater_0");
                const uploadDate = "#DTE_Field_editorFields-dateUploadLater";
                await I.waitForVisible(uploadDate, 10);
                await I.fillField(uploadDate, date);
                await I.pressKey("Tab");
                await I.seeInField(uploadDate, date);
                await I.clickCss("#fileArchiveDataTable_modal .modal-title");
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan");
    I.say(formatShotPlan(videoPlan));
});

/** Reuses the unsaved salon page for the walkthrough, independent retakes and the thumbnail. */
function homepageEditor({ I, DTE, DT }) {
    const frame = "#DTE_Field_data-pageBuilderIframe";
    const host = "#toast-container-ai";
    const state = { hasPage: false, imageName: "salon-linia-autotest-" + Date.now() + ".png" };
    const action = name => '.pb-workbench [data-pb-action="' + name + '"]';
    const nextMonth = new Date();
    nextMonth.setDate(1);
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    nextMonth.setHours(9, 0, 0, 0);
    const date = I.formatDateTime(nextMonth.getTime());

    const openPage = async (create) => {
        await I.switchTo();
        await I.amOnPage(create ? "/admin/v9/webpages/web-pages-list/?groupid=34495" : "/admin/v9/webpages/web-pages-list/?docid=134996");
        await DT.waitForLoader();
        if (create) await I.clickCss("#datatableInit_wrapper .buttons-create");
        await DTE.waitForEditor();
        await I.clickCss("#pills-dt-datatableInit-content-tab");
        await DTE.waitForCkeditor();
        if (await I.grabNumberOfVisibleElements("div.DTED.show button.maximize")) await I.clickCss("div.DTED.show button.maximize");
        if (!create) return;
        await I.waitForVisible(frame, 20);
        await I.switchTo(frame);
        await I.waitForVisible(".pb-workbench", 20);
        await I.switchTo();
    };
    const fs = require("node:fs");
    const path = require("node:path");
    const snapshot = path.resolve(__dirname, "../../../../docs/feature-video/406-salon.html");
    const fallback = path.resolve(__dirname, "assets/406-salon.html");
    const imageCache = path.resolve(path.dirname(snapshot), "406-salon-illustration.png");
    // Read the iframe and write the retake fixture in a real browser step; dry runs perform no I/O.
    const saveSalon = () => I.usePlaywrightTo("save the accepted salon page for retakes", async ({ page }) => {
        const html = await page.frameLocator(frame).locator("body").evaluate(() =>
            window.getSaveData().editable.find(field => field.wjAppField === "doc_data").data);
        fs.writeFileSync(snapshot, html);
    });
    // Restore real recorded material when available; the PDF-based seed also supports a first isolated retake.
    const restoreContent = (withPhoto, useActiveEditor = false) => I.usePlaywrightTo("restore unsaved salon content", async ({ page }) => {
        const html = fs.readFileSync(fs.existsSync(snapshot) ? snapshot : fallback, "utf8");
        const image = withPhoto && fs.existsSync(imageCache) ? "data:image/png;base64," + fs.readFileSync(imageCache).toString("base64") : null;
        await page.evaluate(async ({ html, image, useActiveEditor }) => {
            const content = document.createElement("div");
            content.innerHTML = html;
            const photo = content.querySelector("img");
            if (photo && image) photo.src = image;
            const editor = window.webpagesDatatable.EDITOR.field("data").s.opts.wjeditor;
            if (useActiveEditor && editor.editingMode !== "pageBuilder") {
                await new Promise(resolve => window.ckEditorInstance.setData(content.innerHTML, resolve));
            } else {
                await editor.pbInsertContent(content.innerHTML, "replace", true, false);
            }
        }, { html, image, useActiveEditor });
    });
    const ensureSalon = async () => {
        await I.switchTo();
        if (state.hasPage && await I.executeScript(selector => {
            const iframe = document.querySelector(selector);
            return iframe?.offsetWidth > 0 && iframe.contentDocument?.querySelector("#wjInline-docdata h1")?.textContent.includes("Línia");
        }, frame)) return;
        await openPage(true);
        await restoreContent(false);
        state.hasPage = true;
    };

    const webRoot = path.resolve(__dirname, "../../../main/webapp");
    const generatedPath = () => {
        if (typeof state.imageUrl !== "string") return null;
        const pathname = new URL(state.imageUrl, "http://localhost").pathname.replace(/^\/thumb\//, "/");
        const localFile = path.resolve(webRoot, "." + pathname);
        if (!localFile.startsWith(webRoot + path.sep + "images" + path.sep) || path.basename(localFile) !== state.imageName) return null;
        return localFile;
    };
    const cacheImage = () => {
        const file = generatedPath();
        if (file && fs.existsSync(file)) fs.copyFileSync(file, imageCache);
    };
    const discardImage = () => {
        const file = generatedPath();
        if (file && fs.existsSync(file)) fs.unlinkSync(file);
    };
    const showIllustration = useActiveEditor => restoreContent(true, useActiveEditor);
    return { frame, host, state, action, date, openPage, ensureSalon, saveSalon, cacheImage, discardImage, showIllustration };
}

Scenario("YouTube thumbnail", async ({ I, DTE, DT, login }) => {
    login("admin");
    const editor = homepageEditor({ I, DTE, DT });
    await editor.ensureSalon();
    await editor.showIllustration();
    await I.saveScreenshot("406-website-content-page.png");
    await I.videoTitle("Čo dokáže WebJET CMS?", 56, "glow");
    await DTE.cancel();
}).tag("@title");

Scenario("406-website-content", async ({ I, DTE, DT, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan");
    const editor = homepageEditor({ I, DTE, DT });
    try {
        await recordVideoPlan(I, {
            plan: videoPlan, context: editor,
            setup: async () => { login("admin"); },
            cleanup: async shot => {
                if (shot.id === "document-release" || shot.id === "publication-date") await DTE.cancel();
            }
        });
    } finally {
        editor.discardImage();
    }
}).tag("@video");
