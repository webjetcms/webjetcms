Feature("video.309-58742-forms-add-step-back-button-fn");

const videoPlan = {
    language: "sk",
    shots: [
        {
            id: "intro",
            type: "auto",
            durationSeconds: 8,
            title: "Correct an answer without starting again",
            "text-sk": "Preklep vo formulári nemusí znamenať začínať odznova. Vo WebJET CMS sa návštevník jednoducho vráti k predchádzajúcemu kroku a opraví svoje údaje.",
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
            durationSeconds: 12,
            title: "Return to previously saved details",
            "text-sk": "Pozrime sa na registráciu na online kurz. Od druhého kroku je dostupné tlačidlo na návrat. Po kliknutí sa obnovia údaje uložené pri poslednom úspešnom pokračovaní dopredu.",
            notes: "Start on the second step with fictional contact details already saved in the session. Return to the first step and frame the restored values. Never submit the final step.",
            shot: async ({ I, back, firstName, surname, email }) => {
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
            "text-sk": "Návštevník tak nemusí znovu vypĺňať celý formulár. Opraví napríklad priezvisko a pokračuje ďalej. Pri ďalšom návrate už vidí opravenú hodnotu, ktorú uložil prechodom dopredu.",
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
                await I.videoClick(back);
                await I.waitForVisible(surname, 10);
                await I.seeInField(surname, "Nováková");
            }
        },
        {
            id: "unsaved-current-step",
            type: "auto",
            durationSeconds: 12,
            title: "Explain which changes Back does not save",
            "text-sk": "Pozor však na práve otvorený krok. Tlačidlo Späť jeho rozpracované zmeny neukladá ani nekontroluje. Ak tu návštevník zmení výber a hneď sa vráti, táto zmena sa nezachová.",
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
            "text-sk": "Text tlačidla si prispôsobíte v editore kroku. Na karte Pokročilé vyplníte pole Predchádzajúci krok, napríklad Späť na údaje. Prázdne pole ponechá predvolený text tlačidla.",
            notes: "Edit the second step of the existing screenshot fixture. Demonstrate the label field, then discard the change in cleanup.",
            prepare: async ({ I, DTE }) => {
                await I.click(locate("#formStepsDataTable tbody td").withText("Krok 2"));
                await I.waitForVisible(".stepPreview [data-multistep-back-step]", 10);
                await I.clickCss("#formStepsDataTable_wrapper button.buttons-edit");
                await DTE.waitForEditor("formStepsDataTable");
            },
            shot: async ({ I, typeText }) => {
                await I.videoClick("#pills-dt-formStepsDataTable-advanced-tab");
                await I.waitForVisible("#DTE_Field_backStepBtnLabel", 10);
                await I.videoClick("#DTE_Field_backStepBtnLabel");
                await I.clearField("#DTE_Field_backStepBtnLabel");
                await typeText("Späť na údaje");
                await I.seeInField("#DTE_Field_backStepBtnLabel", "Späť na údaje");
                await I.wait(3);
            }
        },
        {
            id: "preview-styles",
            type: "auto",
            durationSeconds: 11,
            title: "Compare form styles in the preview",
            "text-sk": "Novinkou sú aj pripravené vzhľady formulára. V administračnom náhľade prepnete CSS šablónu a hneď porovnáte výsledok. Toto skúšanie nemení vzhľad formulára, ktorý už máte na stránke.",
            shot: async ({ I }) => {
                for (const template of ["template-1.css", "template-2.css", "template-3.css"]) {
                    await I.videoClick("#previewCssTemplate");
                    await I.selectOption("#previewCssTemplate", template);
                    await I.pressKey("Escape");
                    await I.waitForElement(`.stepPreview[data-multistep-css-template='/apps/form/mvc/styles/${template}'] #formStepPreviewCssTemplate`, 10);
                    await I.waitForFunction(() => document.querySelector("#formStepPreviewCssTemplate")?.sheet != null, [], 10);
                    await I.wait(2);
                }
            }
        },
        {
            id: "page-style",
            type: "auto",
            durationSeconds: 11,
            title: "Select a style for a particular page instance",
            "text-sk": "Vybraný vzhľad nastavíte samostatne v aplikácii Formulár vloženej na stránke. Každá vložená inštancia môže mať vlastnú šablónu. Rovnaký formulár tak viete vizuálne prispôsobiť rôznym stránkam.",
            notes: "Open the existing form component on page 156109. Select a style without confirming the app dialog or saving the page; cleanup discards it.",
            prepare: async ({ I }) => {
                await I.waitForElement("#DTE_Field_cssTemplate option[value='/apps/form/mvc/styles/template-1.css']", 20);
            },
            shot: async ({ I }) => {
                await I.videoClick(".DTE_Field_Name_cssTemplate button.dropdown-toggle");
                const option = locate("div.dropdown-menu.show a.dropdown-item").withText("template-1.css");
                await I.waitForVisible(option, 10);
                await I.videoClick(option);
                await I.seeInField("#DTE_Field_cssTemplate", "/apps/form/mvc/styles/template-1.css");
                await I.wait(4);
            }
        },
        {
            id: "documentation",
            type: "auto",
            durationSeconds: 9,
            title: "Read the multistep form documentation",
            "text-sk": "Uľahčite návštevníkom opravy a vyberte formuláru vhodný vzhľad. Podrobný postup nájdete v dokumentácii WebJET CMS. Odkaz na návod je v popise videa.",
            notes: "Public article URL verified. Publish the branch documentation before releasing the film; the public article does not yet describe Back or CSS templates. Trim the scroll to the narration length.",
            shot: async ({ I }) => {
                await I.videoDocumentation("https://docs.webjetcms.sk/latest/sk/redactor/apps/multistep-form/");
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
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
                await Apps.openAppEditor(156109);
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
            if (shot.id === "intro") return;
            await I.clickCss(next);
            await I.waitForVisible(back, 10);
            await I.waitForVisible(newsletter, 10);
            await I.dontSeeCheckboxIsChecked(newsletter);
        },
        cleanup: async shot => {
            if (shot.id === "button-label") {
                await DTE.cancel("formStepsDataTable");
            } else if (shot.id === "page-style") {
                await I.switchTo();
                await I.clickCss("td.cke_dialog_footer .cke_dialog_ui_button_cancel");
                await I.waitForInvisible(".cke_dialog_ui_iframe", 10);
                await DTE.cancel();
            }
        }
    });
}).tag("@video");
