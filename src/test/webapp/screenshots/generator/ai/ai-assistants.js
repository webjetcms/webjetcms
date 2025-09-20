Feature('ai.ai-assistants');

Before(({ login }) => {
    login('admin');
});

let pageIdText = "16";
let pageIdImage = "16";
let pageIdCK = "16";
let pageIdPageBuilder = "134996";

let openAI = "OpenAI";
let gemini = "Gemini";
let browser = "Prehliadač";

Scenario('ai-assistants table screenshots', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/settings/ai-assistants/");

    Document.screenshot("/redactor/ai/settings/datatable.png");

    DT.filterEquals("name", "DOC-SEO-01 Generate Keywords");
    I.clickCss("td.dt-select-td.sorting_1");
    I.clickCss("button.buttons-edit");
    DTE.waitForEditor();

    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-basic-tab.png");

    I.clickCss("#pills-dt-datatableInit-action-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-action-tab.png");

    I.clickCss("#pills-dt-datatableInit-provider-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-provider-tab.png");

    I.clickCss("#pills-dt-datatableInit-instructions-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-instructions-tab.png");

    I.clickCss("#pills-dt-datatableInit-advanced-tab");
    Document.screenshotElement("#datatableInit_modal > div > div.DTE_Action_Edit", "/redactor/ai/settings/datatable-advanced-tab.png");
});

Scenario('ai-assistants usage - TEXT', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdText);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.waitForElement("div.DTE_Field_Name_htmlData");
    //I.fillField("#DTE_Field_htmlData", "");
    I.moveCursorTo(locate("div.DTE_Field_Name_htmlData").find("button.btn-ai"));

    I.resizeWindow(1000, 500);
    I.scrollTo("div.DTE_Field_Name_perexImage");

    Document.screenshot("/redactor/ai/datatables/textarea-focus.png");

    I.say("TEXT Assistant without prompt");

    I.click(locate("div.DTE_Field_Name_htmlData").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");
    Document.screenshot("/redactor/ai/datatables/textarea-assistants.png");

    I.resizeWindow(1000, 400);

    I.click( locate('button.btn-ai-action').withText("Vytvoriť zhrnutie").withChild(locate('span.provider').withText(openAI)) );
    I.waitForInvisible( locate(".toast.toast-info > .toast-message > #toast-container-ai-content > .current-status > span").withText("AI už na tom pracuje...") );
    Document.screenshot("/redactor/ai/datatables/textarea-done.png");

    //AFTER DONE fake waiting because its to fast
    await I.executeScript(() => {
        $("div.DTE_Field_Name_htmlData").addClass("ai-loading");
        window.webpagesDatatable.EDITOR.editorAi.setCurrentStatus("components.ai_assistants.editor.loading.js");
    });
    Document.screenshot("/redactor/ai/datatables/textarea-waiting.png");
    await I.executeScript(() => {
        $("div.DTE_Field_Name_htmlData").removeClass("ai-loading");
    });

    I.click( locate("#toast-container-ai").find(".toast-close-button") );
    I.waitForInvisible("#toast-container-ai");

    I.fillField("#DTE_Field_htmlData", "");

    I.say("TEXT Assistant WITH prompt");

    I.resizeWindow(1000, 500);

    I.click(locate("div.DTE_Field_Name_htmlData").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");
    I.click( locate('button.btn-ai-action').withText("Napísať nový text/článok").withChild(locate('span.provider').withText(browser)) );
    Document.screenshot("/redactor/ai/datatables/textarea-prompt.png");

    //Fake loading
    await I.executeScript(() => {
        $("div.DTE_Field_Name_htmlData").addClass("ai-loading");
        window.webpagesDatatable.EDITOR.editorAi.setCurrentStatus("components.ai_assistants.editor.loading.js");
    });
    Document.screenshot("/redactor/ai/datatables/textarea-prompt-loading.png");
});

Scenario('ai-assistants usage - IMAGE - without prompt', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdImage);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.resizeWindow(1000, 500);
    I.scrollTo("div.DTE_Field_Name_perexGroups");

    I.waitForElement("div.DTE_Field_Name_perexImage");

    const originalValue = await I.grabValueFrom("div.DTE_Field_Name_perexImage input");

    I.moveCursorTo(locate("div.DTE_Field_Name_perexImage").find("button.btn-ai"));

    Document.screenshot("/redactor/ai/datatables/image-focus.png");

    I.click(locate("div.DTE_Field_Name_perexImage").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");
    Document.screenshot("/redactor/ai/datatables/image-assistants.png");

    //fake blank input value
    I.fillField("div.DTE_Field_Name_perexImage input", "");
    I.click( locate("#toast-container-ai-content").find(locate("button.btn-ai-action").withText("Odstrániť pozadie")) );
    I.waitForText("Nastala chyba pri volaní AI asistenta");
    Document.screenshot("/redactor/ai/datatables/image-error-1.png");

    I.click( locate("#toast-container-ai").find(".toast-close-button") );
    I.fillField("div.DTE_Field_Name_perexImage input", originalValue);
    I.click(locate("div.DTE_Field_Name_perexImage").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");
    I.click( locate("#toast-container-ai-content").find(locate("button.btn-ai-action").withText("Odstrániť pozadie")) );

    Document.screenshot("/redactor/ai/datatables/image-loading.png");

    I.resizeWindow(1000, 600);

    I.waitForInvisible( locate(".toast.toast-info > .toast-message > #toast-container-ai-content > .current-status > span").withText("AI už na tom pracuje...") );
    Document.screenshot("/redactor/ai/datatables/image-done.png");

    I.resizeWindow(1000, 500);

    I.clickCss("button.btn-primary.select-image");
    I.waitForVisible("#toast-ai-name-selection");
    Document.screenshot("/redactor/ai/datatables/image-name-select.png");
});

Scenario('ai-assistants usage - IMAGE - WITH prompt', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdImage);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.resizeWindow(1000, 500);
    I.scrollTo("div.DTE_Field_Name_perexGroups");

    I.waitForElement("div.DTE_Field_Name_perexImage");
    I.click(locate("div.DTE_Field_Name_perexImage").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");

    I.click( locate('button.btn-ai-action').withText("Vytvoriť nový obrázok").withChild(locate('span.provider').withText(gemini)) );
    I.waitForVisible("#ai-user-prompt");

    I.fillField("#ai-user-prompt", "Hokejista Hossa dáva víťazný gól");
    I.click( locate("#toast-container-ai-content").find(locate("button").withText("Generovať")) );
    I.waitForText("PROHIBITED_CONTENT", 60);
    Document.screenshot("/redactor/ai/datatables/image-error-2.png");

    I.click( locate("#toast-container-ai").find(".toast-close-button") );
    I.waitForInvisible("#toast-container-ai");

    I.click(locate("div.DTE_Field_Name_perexImage").find("button.btn-ai"));
    I.waitForVisible("#toast-container-ai > div.toast-info");

    I.click( locate('button.btn-ai-action').withText("Vytvoriť nový obrázok").withChild(locate('span.provider').withText(openAI)) );
    I.waitForVisible("#ai-user-prompt");
    Document.screenshot("/redactor/ai/datatables/image-prompt.png");

    I.resizeWindow(1400, 600);

    I.fillField("#bonusContent-imageCount", 3);
    I.fillField("#ai-user-prompt", "Izometrický pohľad na bratislavský hrad");
    I.click( locate("#toast-container-ai-content").find(locate("button").withText("Generovať")) );
    I.waitForElement( ".ai-image-preview-div", 100 );

    Document.screenshot("/redactor/ai/datatables/image-select.png");
});

Scenario('ai-assistants usage - TEXT - CKEditor', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdCK);
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    //fill short text to save credits
    DTE.fillCkeditor(`
        <h2>Americké akciové trhy sa minulý týždeň konsolidovali v&nbsp;blízkosti rekordných maxím po ďalšom kole zavedenia vzájomných dovozných ciel Spojených štátov a&nbsp;Číny, pričom investori boli naklonení podstupovať riziko, keď sa odrážali ceny aktív na&nbsp;rozvíjajúcich sa trhoch.</h2>
        <p>Hlavné svetové akciové trhy sa v&nbsp;minulom týždni viac-menej konsolidovali, keďže im chýbal výraznejší impulz k&nbsp;pohybu.&nbsp;<strong>Celkovo však bol sentiment naklonený podstupovaniu rizika, americké akcie sa držali v&nbsp;blízkosti historických maxím</strong>&nbsp;a&nbsp;zisk z&nbsp;ostatného obdobia si udržali aj emerging trhy. Súvisí to s&nbsp;tým, že ostatné kroky USA a&nbsp;Číny ohľadne protekcionizmu v&nbsp;medzinárodnom obchode neboli až také ostré, akých sa obávali obchodníci. Široký americký index S&nbsp;&amp; P 500&nbsp;klesol o&nbsp;0,5 percenta pri poklese paneurópskeho indexu STOXX Europe 600&nbsp;o&nbsp;0,3 percenta, pričom emerging akciový index MSCI Emerging World stratil rovnako len 0,3 percenta.&nbsp;<strong>Takmer štvorpercentné straty však zaknihovali talianske akcie</strong>&nbsp;potom, čo vláda koncom týždňa prekvapivo prijala&nbsp;<strong>expanzívny rozpočet na&nbsp;budúci rok so&nbsp;schodkom 2,4 percenta HDP</strong>, pričom trhy by boli spokojné so&nbsp;schodkom do&nbsp;dvoch percent výkonu ekonomiky.</p>
    `);

    I.resizeWindow(1280, 850);
    Document.screenshot("/redactor/ai/datatables/ckeditor.png", null, null, "#cke_58");

    I.clickCss("span.cke_button_icon.cke_button__aibutton_icon");
    I.waitForVisible("#toast-container-ai > div.toast-info");
    Document.screenshot("/redactor/ai/datatables/ckeditor-assistants.png");

    I.click( locate('button.btn-ai-action').withText("Vylepšiť text").withChild(locate('span.provider').withText(gemini)) );
    I.wait(2);
    Document.screenshot("/redactor/ai/datatables/ckeditor-loading.png");

    I.waitForInvisible( locate(".toast.toast-info > .toast-message > #toast-container-ai-content > .current-status > span").withText("AI už na tom pracuje..."), 100 );
    Document.screenshot("/redactor/ai/datatables/ckeditor-done.png");
});

Scenario('ai-assistants usage - TEXT - CKEditor - line', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdCK);
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.resizeWindow(1100, 750);

    I.switchTo(".cke_wysiwyg_frame.cke_reset");
    I.waitForElement("iframe.wj_component", 10);
    I.wait(1);

    I.clickCss("h2");
    I.pressKey(['Meta', 'ArrowUp']);
    I.pressKeyDown('Shift');
    I.pressKey(['Meta', 'ArrowRight']);
    I.pressKey(['ArrowDown']);
    I.pressKey(['ArrowDown']);
    I.pressKey(['ArrowDown']);
    I.pressKeyUp('Shift');

    I.switchTo();
    I.clickCss("span.cke_button_icon.cke_button__aibutton_icon");
    I.waitForVisible("#toast-container-ai > div.toast-info");
    Document.screenshot("/redactor/ai/datatables/ckeditor-text-selection.png");
});

Scenario('ai-assistants usage - TEXT - PageBuilder', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdPageBuilder);
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.resizeWindow(1100, 650);

    await I.executeScript(() => {
        $("#DTE_Field_data-pageBuilderIframe").contents().find("span#cke_57").css({"outline": "1px solid #FF4B58", "box-shadow": "0 0 12px #FF4B58"})
    });

    Document.screenshot("/redactor/ai/datatables/page_builder.png");

    await I.executeScript(() => {
        $("#DTE_Field_data-pageBuilderIframe").contents().find("span#cke_57").css({"outline": "none", "box-shadow": "none"})
    });

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.clickCss("span.cke_button_icon.cke_button__aibutton_icon");
    I.switchTo();
    I.waitForVisible("#toast-container-ai > div.toast-info");
    Document.screenshot("/redactor/ai/datatables/page_builder-assistants.png");

    I.click( locate('button.btn-ai-action').withText("Opraviť gramatiku").withChild(locate('span.provider').withText(gemini)) );
    I.wait(2);
    Document.screenshot("/redactor/ai/datatables/page_builder-loading.png");

    I.waitForInvisible( locate(".toast.toast-info > .toast-message > #toast-container-ai-content > .current-status > span").withText("AI už na tom pracuje..."), 100 );
    Document.screenshot("/redactor/ai/datatables/page_builder-done.png");
});

Scenario('ai-assistants usage - TEXT - PageBuilder - line', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageIdPageBuilder);
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.resizeWindow(1100, 650);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.click(locate("ul li:first-child"));

    I.pressKeyDown('Shift');
    I.pressKey(['ArrowDown']);
    I.pressKey(['ArrowDown']);
    I.pressKeyUp('Shift');

    I.clickCss("span.cke_button_icon.cke_button__aibutton_icon");
    I.switchTo();
    I.waitForVisible("#toast-container-ai > div.toast-info");

    Document.screenshot("/redactor/ai/datatables/page_builder-selection.png");
    I.switchTo();
});

Scenario('ai-assistants usage - page images', async ({ I, DTE, Document }) => {
    I.switchTo();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=11");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.clickCss("img");
    I.waitForElement("table.cke_dialog.cke_browser_webkit");
    I.waitForVisible("#wjImageIframeElement");
    I.switchTo("#wjImageIframeElement");
    I.moveCursorTo("button.btn-ai");

    Document.screenshot("/redactor/ai/datatables/page-image.png");

    I.clickCss("button.btn-ai");

    Document.screenshot("/redactor/ai/datatables/page-image-assistants.png");
});

Scenario('ai-assistants usage - other', async ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/apps/gallery/?id=164");
    DTE.waitForEditor("galleryTable");
    await I.executeScript(() => {
        $("#DTE_Field_descriptionLongSk").css({"margin": "15px"});
    });
    Document.screenshotElement("div.DTE_Field_Name_descriptionLongSk > div", "/redactor/ai/datatables/quill.png")
});

Scenario('ai-assistants empty', async ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/perex/");
    DT.addContext("perex","#perexDataTable_wrapper");
    I.click(DT.btn.perex_add_button);
    DTE.waitForEditor("perexDataTable");
    await I.executeScript(() => {
        //disable all userPromptEnabled to show error message on empty field
        let aiFields = perexDataTable.DATA.fields[1].ai;
        aiFields.forEach(field => {
            field.userPromptEnabled = false;
        });
    });
    I.click(locate("div.DTE_Field_Name_perexGroupName").find("button.btn-ai"));
    Document.screenshot("/redactor/ai/datatables/no-assistants-available.png", 1280, 420);
});