Feature("video.309-58742-forms-add-step-back-button-fn");

const videoPlan = {
    language: "sk",
    shots: [
        {
            id: "intro",
            type: "auto",
            durationSeconds: 10,
            title: "Correct an answer without starting again",
            "text-sk": "Preklep vo formulári nemusí znamenať začínať odznova. Do viac krokových formulárov sme doplnili funkciu návratu na predchádzajúci krok.",
            notes: "Show the real registration page with fictional contact details filled in. Focus the surname and hold the form during the opening narration; the following shots demonstrate navigation and correction.",
            shot: async ({ I, surname, email }) => {
                await I.videoClick(surname);
                await I.seeInField(surname, "Novakova");
                await I.seeInField(email, "jana@example.com");
                await I.wait(7);
            }
        },
        {
            id: "return-to-details",
            type: "auto",
            durationSeconds: 11,
            title: "Return to previously saved details",
            "text-sk": "Pozrime sa na registráciu na online kurz. Od druhého kroku je dostupné tlačidlo na návrat. Po kliknutí sa vrátite na prvý krok, ktorý obsahuje vami vyplnené údaje.",
            notes: "Start on the second step with fictional contact details already saved in the session. Return to the first step and frame the restored values. Never submit the final step.",
            shot: async ({ I, back, next, newsletter, firstName, surname, email }) => {
                await I.wait(1);
                await I.videoClick(next);
                await I.waitForVisible(back, 10);
                await I.waitForVisible(newsletter, 10);
                await I.dontSeeCheckboxIsChecked(newsletter);
                await I.wait(2);
                await I.videoClick(back);
                await I.waitForVisible(firstName, 10);
                await I.seeInField(firstName, "Jana");
                await I.seeInField(surname, "Novakova");
                await I.seeInField(email, "jana@example.com");
                await I.dontSeeElement(back);
                await I.wait(4);
            }
        },
        {
            id: "correct-details",
            type: "auto",
            durationSeconds: 11,
            title: "Save a correction by continuing forward",
            "text-sk": "Pri chybe nemusíte teda začínať odznova, opravíte napríklad priezvisko a pokračujete ďalej vo vypĺňaní formuláru.",
            prepare: async ({ I, back, firstName }) => {
                await I.clickCss(back);
                await I.waitForVisible(firstName, 10);
            },
            shot: async ({ I, surname, next, back, typeText }) => {
                await I.videoClick(surname);
                await I.clearField(surname);
                await typeText("Nováková");
                await I.videoClick(next);
                await I.waitForVisible(back, 10);
                await I.wait(4);
            }
        },
        {
            id: "unsaved-current-step",
            type: "auto",
            durationSeconds: 10,
            title: "Explain which changes Back does not save",
            "text-sk": "Ak by ste už mali niečo v druhom kroku vyplnené a vrátite sa nazad, tak tieto zmeny nebudú uložené. Zmeny v prvom kroku môžu ovplyvniť druhý krok, napríklad zobrazené možnosti alebo povinné polia.",
            notes: "Check the newsletter option, return without submitting this step, then advance from the first step. Show that the newsletter option is unchecked again.",
            shot: async ({ I, newsletter, back, firstName, next }) => {
                await I.dontSeeCheckboxIsChecked(newsletter);
                await I.videoClick(newsletter);
                await I.seeCheckboxIsChecked(newsletter);
                await I.videoClick(back);
                await I.waitForVisible(firstName, 10);
                await I.videoClick(next);
                await I.waitForVisible(newsletter, 10);
                await I.dontSeeCheckboxIsChecked(newsletter);
            }
        },
        {
            id: "button-label",
            type: "auto",
            durationSeconds: 11,
            title: "Customize the previous-step button label",
            "text-sk": "Nastavenie je jednoduché, v administrácii v editore druhého kroku otvorte kartu Pokročilé. Pole Predchádzajúci krok určuje text tlačidla na návrat. Zadajte Späť na údaje, aby bolo jasné, kam tlačidlo vedie.",
            notes: "Edit the second step of the existing screenshot fixture. Demonstrate the label field, then discard the change in cleanup.",
            shot: async ({ I, DTE, typeText }) => {
                await I.click(locate("#formStepsDataTable tbody td").withText("Krok 2"));
                await I.waitForVisible(".stepPreview [data-multistep-back-step]", 10);
                await I.clickCss("#formStepsDataTable_wrapper button.buttons-edit");
                await DTE.waitForEditor("formStepsDataTable");

                await I.videoClick("#pills-dt-formStepsDataTable-advanced-tab");
                await I.waitForVisible("#DTE_Field_backStepBtnLabel", 10);
                await I.videoClick("#DTE_Field_backStepBtnLabel");
                await I.clearField("#DTE_Field_backStepBtnLabel");
                await typeText("Späť na údaje");
                await I.seeInField("#DTE_Field_backStepBtnLabel", "Späť na údaje");
                await I.wait(3);
                await DTE.save("formStepsDataTable");
            }
        },
        {
            id: "preview-styles",
            type: "auto",
            durationSeconds: 11,
            title: "Compare form styles in the preview",
            "text-sk": "Pridali sme aj možnosť prispôsobiť štýl formulára. Pri jeho zmene okamžite vidíte náhľad výsledku.",
            shot: async ({ I }) => {
                for (const template of ["template-1.css", "template-2.css", "template-3.css"]) {
                    await I.videoClick("#previewCssTemplate");
                    await I.selectOption("#previewCssTemplate", template);
                    await I.pressKey("Escape");
                    await I.waitForElement(`.stepPreview[data-multistep-css-template='/apps/form/mvc/styles/${template}'] #formStepPreviewCssTemplate`, 10);
                    await I.waitForFunction(() => document.querySelector("#formStepPreviewCssTemplate")?.sheet != null, [], 10);
                    // Larger template headings can push the fields below the preview viewport.
                    await I.scrollTo(".stepPreview #f1-meno-1");
                    await I.wait(2);
                }
            }
        },
        {
            id: "page-style",
            type: "auto",
            durationSeconds: 11,
            title: "Select a style for a particular page instance",
            "text-sk": "Teraz sme v aplikácii Formulár vloženej na stránke. Otvoríme výber CSS šablóny a zvolíme vzhľad. Vybraná šablóna sa použije pri zobrazení formuláru. Môžete mať tak jeden formulár v rôznych dizajnoch podľa potrieb, napríklad zobrazenie vo web stránke, alebo v dialógovom okne.",
            notes: "Open the existing form component on page 156109. Select a style without confirming the app dialog or saving the page; cleanup discards it.",
            shot: async ({ I, Apps }) => {
                await Apps.openAppEditor(156109);
                await I.waitForElement("#DTE_Field_cssTemplate option[value='/apps/form/mvc/styles/template-1.css']", 20);
                await I.videoClick(".DTE_Field_Name_cssTemplate button.dropdown-toggle");
                const option = locate("div.dropdown-menu.show a.dropdown-item").withText("template-3.css");
                await I.waitForVisible(option, 10);
                await I.videoClick(option);
                await I.seeInField("#DTE_Field_cssTemplate", "/apps/form/mvc/styles/template-3.css");
                await I.wait(1);
                await Apps.confirm();
                await I.wait(1);
                await I.videoClick("#datatableInit_modal button.btn-preview");
                await I.usePlaywrightTo("wait for the preview tab", async ({ page }) => {
                    await page.waitForFunction(() => window.previewWindow != null && !window.previewWindow.closed);
                });
                await I.switchToNextTab();
                const previewUrl = await I.grabCurrentUrl();
                await I.wait(1);
                await I.closeCurrentTab();
                // Playwright records each tab separately; retain this view in the main recording.
                await I.amOnPage(previewUrl);
                await I.wait(1);
                await I.videoScroll();
                await I.wait(5);
            }
        },
        {
            id: "documentation",
            type: "auto",
            durationSeconds: 10,
            title: "Read the multistep form documentation",
            "text-sk": "Ďalšie možnosti viackrokových formulárov nájdete v dokumentácii WebJET CMS. Otvorená časť Položky formuláru vysvetľuje prácu s krokmi a poľami. Odkaz nájdete v popise videa.",
            notes: "Hold the article heading and introduction during the documentation invitation. Include this public URL in the video description. The current article does not yet document Back or CSS templates, so do not promise those instructions.",
            prepare: async ({ I }) => {
                await I.amOnPage("https://docs.webjetcms.sk/latest/sk/redactor/apps/multistep-form/");
                await I.waitForVisible("article h1", 20);
            },
            shot: async ({ I }) => {
                await I.see("Položky formuláru", "article h1");
                await I.wait(8);
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

Scenario("309-58742-forms-add-step-back-button-fn", async ({ I, DT, DTE, Document, Apps, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const firstName = "#f1-meno-1";
    const surname = "#f1-priezvisko-1";
    const email = "#f1-email-1";
    const next = ".multistep-form-app button[type='submit']";
    const back = ".multistep-form-app [data-multistep-back-step]";
    const newsletter = "#f1-checkbox-1";
    const typeText = text => I.usePlaywrightTo("type the Slovak demonstration text", async ({ page }) => {
        await page.keyboard.type(text, { delay: 70 });
    });

    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { firstName, surname, email, next, back, newsletter, typeText },
        setup: async () => {
            login("admin");
            await Document.resetPageBuilderMode();
        },
        prepare: async shot => {
            await I.switchTo();
            if (shot.id === "documentation") return;
            if (shot.id === "page-style") {
                return;
            }
            if (shot.id === "button-label" || shot.id === "preview-styles") {
                await I.amOnPage("/apps/form/admin/form-steps/?formName=Registracia-na-online-kurz");
                await DT.waitForLoader("formStepsDataTable");
                await I.waitForVisible(".stepPreview #f1-meno-1", 20);
                await I.waitForElement("#previewCssTemplate option[value='/apps/form/mvc/styles/template-3.css']", 10);
                await I.selectOption("#previewCssTemplate", "Bez šablóny");
                return;
            }

            // Reload the existing screenshot fixture to start an independent form session per shot.
            // Only the first step is submitted; the final step never creates a registration or sends mail.
            await I.amOnPage("/showdoc.do?docid=156109");
            await I.waitForVisible(firstName, 20);
            await I.fillField(firstName, "Jana");
            await I.fillField(surname, "Novakova");
            await I.fillField(email, "jana@example.com");
            await I.fillField("#f1-telefon-1", "");
            if (shot.id === "intro" || shot.id === "return-to-details") return;
            await I.clickCss(next);
            await I.waitForVisible(back, 10);
            await I.waitForVisible(newsletter, 10);
            await I.dontSeeCheckboxIsChecked(newsletter);
        },
        cleanup: async shot => {

        }
    });
}).tag("@video");
