Feature('ai.ai-assistants');

Before(({ I, login }) => {
    login('admin');
});

Scenario('zakladne testy @baseTest', async ({I, DTE, DataTables}) => {
    I.amOnPage("/admin/v9/settings/ai-assistants/");
    await DataTables.baseTest({
        dataTable: 'aiAssistantsDataTable',
        perms: 'cmp_ai_tools',
        createSteps: function(I, options) {
            I.clickCss("#pills-dt-datatableInit-action-tab");
            I.fillField("#DTE_Field_className", "sk.iway.iwcm.doc.DocDetails");
            I.fillField("#DTE_Field_fieldTo", "fieldA");

            I.clickCss("#pills-dt-datatableInit-instructions-tab");
            I.fillField("#DTE_Field_instructions", "Say hi to me.");

            I.clickCss("#pills-dt-datatableInit-provider-tab");
            DTE.selectOption("provider", "Prehliadač");

            I.clickCss("#pills-dt-datatableInit-basic-tab");
        },
        editSteps: function(I, options) {
            I.clickCss("#pills-dt-datatableInit-instructions-tab");
            I.seeInField("#DTE_Field_instructions", "Say hi to me.");
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('ai_assistants editor logic', async ({I, DTE}) => {
    I.amOnPage("/admin/v9/settings/ai-assistants/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor();

    I.say("Check action tab - if entity is recommended and after selection it shows good fields");
        I.clickCss("#pills-dt-datatableInit-action-tab");

        let optionsA = ["sk.iway.iwcm.components.users.userdetail.UserDetailsEntity", "sk.iway.iwcm.components.users.userdetail.UserDetailsSelfEntity", "sk.iway.iwcm.doc.DocDetails"];
        checkAutocomplete(I, "DTE_Field_className", "deta", optionsA[2], optionsA);

        let optionsB = ["perexGroups", "perexPlace", "perexImage"];
        checkAutocomplete(I, "DTE_Field_fieldFrom", "perex", null, optionsB);

        let optionsC = ["menuDocId", "rightMenuDocId", "showInMenu", "loggedShowInMenu"];
        checkAutocomplete(I, "DTE_Field_fieldTo", "menu", null, optionsC);


    I.say("Check provider tab - and model autocomplete for provider");
        I.clickCss("#pills-dt-datatableInit-provider-tab");
        DTE.selectOption("provider", "Prehliadač");
        I.dontSeeElement("#DTE_Field_model");

        DTE.selectOption("provider", "OpenAI");
        I.seeElement("#DTE_Field_model");
        let optionsD = ["gpt-4.1", "gpt-4.1-nano", "gpt-4.1-mini"];
        checkAutocomplete(I, "DTE_Field_model", "gpt-4.1", null, optionsD);

        DTE.selectOption("provider", "Gemini");
        I.seeElement("#DTE_Field_model");
        let optionsE = ["gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite"];
        checkAutocomplete(I, "DTE_Field_model", "gemini-2.5", null, optionsE);
});

function checkAutocomplete(I, id, value, valueToSelect, options) {
    I.fillField("#" + id, value);
    I.waitForVisible('ul.dt-autocomplete-select:not([style*="display: none"])');
    within('ul.dt-autocomplete-select:not([style*="display: none"])', () => {
        for(let i = 0; i < options.length; i++) {
            I.seeElement( locate("li.ui-menu-item").withText(options[i]) );
        }
    });

    if(valueToSelect != null) {
        I.click( locate("li.ui-menu-item").withText("sk.iway.iwcm.doc.DocDetails") );
    }
}

Scenario('ai buttons usage', async ({I, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=128791");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.say("Check buttons");
    I.seeElement( locate(".DTE_Field_Name_htmlData").find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.seeElement( locate(".DTE_Field_Name_perexPlace").find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.seeElement( locate(".DTE_Field_Name_perexImage").find( locate("button.btn-ai[aria-label='AI asistent']") ) );

    I.say("Test AI toast");
    I.click( locate(".DTE_Field_Name_htmlData").find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.waitForVisible("#toast-container-ai");

    within("div#toast-container-ai", () => {
        I.seeElement( locate("button.btn-ai-action").withText("Vytvoriť zhrnutie") );
        I.seeElement( locate("button.btn-ai-action").withText("Vytvoriť zoznam kľúčových slov") );
        I.seeElement( locate("button.btn-ai-action").withText("Opraviť gramatiku") );
        I.seeElement( locate("button.btn-ai-action").withText("Preložiť do angličtiny") );

        I.dontSeeElement( locate("button.btn-ai-action").withText("Vytvoriť nový obrázok") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Odstrániť pozadie") );
    });
    I.click( locate("#toast-container-ai").find("i.toast-close-button") );
    I.waitForInvisible("#toast-container-ai");

    I.click( locate(".DTE_Field_Name_perexImage").find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.waitForVisible("#toast-container-ai");
    within("div#toast-container-ai", () => {
        I.dontSeeElement( locate("button.btn-ai-action").withText("Vytvoriť zhrnutie") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Vytvoriť zoznam kľúčových slov") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Opraviť gramatiku") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Preložiť do angličtiny") );

        I.seeElement( locate("button.btn-ai-action").withText("Vytvoriť nový obrázok") );
        I.seeElement( locate("button.btn-ai-action").withText("Odstrániť pozadie") );
    });

    I.say("Test image values for generation");
    I.click( locate("button.btn-ai-action").withText("Vytvoriť nový obrázok") );
    within("#toast-container-ai-content", () => {
        I.seeElement("textarea#ai-user-prompt");

        I.say("Test that openai bonus content is here");
        I.seeElement( locate("div.bonus-content").find("input#bonusContent-imageCount") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageSize") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageQuality") );
    });
});

/* LOCAL AI sa sa nedá otestovať, nakoľko server beží na Linuxe */

let defaultValue = "TO CHANGE TEXT";
Scenario('test OpenAI AI text answers', async ({I, DT, DTE}) => {
    openPageAndPerexTab(I, DTE);

    I.fillField("#DTE_Field_htmlData", defaultValue);

    I.say("TRY basic TEXT answer without stream");

        kokosText(I, "htmlData", "Vytvoriť zoznam kľúčových slov", "OpenAI", "ti.ti-tags");

        const valueA = await I.grabValueFrom('#DTE_Field_htmlData');
        I.assertNotContain(valueA, defaultValue);
        let parts = valueA.split("|");
        I.assertEqual(5, parts.length);

    I.say("Rever value ancd check all");
        I.click( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > div.text-end > button.btn-ai-undo").withText("Zrušiť zmenu") );
        I.waitForInvisible("div.toast.toast-info");
        const valueB = await I.grabValueFrom('#DTE_Field_htmlData');
        I.assertEqual(defaultValue, valueB);

    I.say("TRY the TEXT answer WITH stream");
        kokosText(I, "htmlData", "Vytvoriť zhrnutie", "OpenAI", "ti.ti-list-letters");
        const valueC = await I.grabValueFrom('#DTE_Field_htmlData');
        //Theer is no other way how to check value
        I.assertNotContain(valueC, defaultValue);
});

Scenario(' test Gemini AI text answers', async ({I, DT, DTE}) => {
    openPageAndPerexTab(I, DTE);

    I.fillField("#DTE_Field_htmlData", defaultValue);

    I.say("TRY basic TEXT answer without stream");

        kokosText(I, "htmlData", "Vytvoriť zoznam kľúčových slov", "Gemini", "ti.ti-tags");

        const valueA = await I.grabValueFrom('#DTE_Field_htmlData');
        I.assertNotContain(valueA, defaultValue);
        let parts = valueA.split("|");
        I.assertEqual(5, parts.length);

    I.say("Rever value ancd check all");
        I.click( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > div.text-end > button.btn-ai-undo").withText("Zrušiť zmenu") );
        I.waitForInvisible("div.toast.toast-info");
        const valueB = await I.grabValueFrom('#DTE_Field_htmlData');
        I.assertEqual(defaultValue, valueB);

    I.say("TRY the TEXT answer WITH stream");
        kokosText(I, "htmlData", "Vytvoriť zhrnutie", "Gemini", "ti.ti-list-letters");
        const valueC = await I.grabValueFrom('#DTE_Field_htmlData');
        //Theer is no other way how to check value
        I.assertNotContain(valueC, defaultValue);
});

Scenario('@current test OpenAI AI image answers', async ({I, DT, DTE}) => {
    openPageAndPerexTab(I, DTE);

    kokosImage(I, "perexImage", "Odstrániť pozadie", "OpenAI", "ti.ti-photo-x");

    pause();
});

function openPageAndPerexTab(I, DTE) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=128791");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible("#DTE_Field_htmlData");
}

function startAssistant(I, field, assistantName, provider) {
    I.click( locate(".DTE_Field_Name_" + field).find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.waitForVisible("#toast-container-ai");
    I.click( locate('button.btn-ai-action').withText(assistantName).withChild(locate('span.provider').withText(provider)));
    I.waitForVisible("div.toast.toast-info");
}

function checkBaseWaitDialog(I, assistantName, provider, icon) {
    I.seeElement( locate("div.toast.toast-info > div.toast-title > div.header-back-button > button").withText("Späť") );
    I.seeElement( locate("div.toast.toast-info > div.toast-title > div.header-back-button > i." + icon) );
    I.seeElement( locate("div.toast.toast-info > div.toast-title > div.header-back-button > span").withText(assistantName) );
    I.seeElement( locate("div.toast.toast-info > div.toast-title > div.header-back-button > span.provider").withText("(" + provider + ")") );

    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span").withText("AI už na tom pracuje...") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span > i.ti.ti-exclamation-circle") );
}

function waiToEndText(I) {
    I.waitForInvisible( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span").withText("AI už na tom pracuje...") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span").withText("Hotovo! Bolo použitých") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span > i.ti.ti-circle-check") );
}

function waiToEndImage(I) {
    I.waitForInvisible( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > span").withText("AI už na tom pracuje..."), 60 );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.chat-error-container > div.current-status > span").withText("Hotovo! Bolo použitých") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.chat-error-container > div.current-status > span > i.ti.ti-circle-check") );

    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div").withText("Názov obrázku") );


    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.ai-image-preview-div") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.image-info") );
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.button-div.text-end") );
}

function kokosText(I, field, assistantName, provider, icon) {
    startAssistant(I, field, assistantName, provider);

    checkBaseWaitDialog(I, assistantName, provider, icon);

    waiToEndText(I);
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.current-status > div.text-end > button.btn-ai-undo").withText("Zrušiť zmenu") );
}

function checkImages(I, imagesCount) {
    //Verify number of visible images
    I.seeNumberOfVisibleElements( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.ai-image-preview-div > div.image-preview"), imagesCount );

    //Check that every preview has image and button
    for(let i = 0; i < imagesCount; i++) {
        I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.ai-image-preview-div > div:nth-child(" + (i+1) +  ") > img") );
        I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.ai-image-preview-div > div:nth-child(" + (i+1) +  ") > a.zoom-in") );
    }
}

function checkImageInfo(I) {
    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.image-info > div:nth-child(1) > label").withText("Názov obrázku") );
    I.seeInField( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.image-info > div:nth-child(1) > input"), "turtle" );

    I.seeElement( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.image-info > div:nth-child(2) > label").withText("Umiestnenie") );
    I.seeInField( locate("div.toast.toast-info > div.toast-message > div#toast-container-ai-content > div.image-info > div:nth-child(2)").find("inout.form-control"), "/images/gallery/test/editor/" );
}

function kokosImage(I, field, assistantName, provider, icon, imageLocation, imageName) {
    startAssistant(I, field, assistantName, provider);

    checkBaseWaitDialog(I, assistantName, provider, icon);

    waiToEndImage(I);

    checkImages(I, 1);

    checkImageInfo(I);
}