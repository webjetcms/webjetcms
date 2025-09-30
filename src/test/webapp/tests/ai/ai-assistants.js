Feature('ai.ai-assistants');

Before(({ I, login }) => {
    login('admin');
});

//Need to have pre-set perexImage and you must set its name and location
let pageId = "128791";
let originalImageName = "turtle";
let originalImageLocation = "/images/gallery/test/editor/";

let openAiId = "OpenAI";
let geminiId = "Gemini";
let openRouterId = "OpenRouter";

let defaultValue = "TO CHANGE TEXT";
let containerAiContent = "#toast-container-ai-content";
let btnAiAction = "button.btn-ai-action";
let btnAiAssistants = "button.btn-ai[aria-label='AI asistent']";

Scenario('zakladne testy @baseTest', async ({I, DT, DTE, DataTables}) => {
    I.amOnPage("/admin/v9/settings/ai-assistants/");

    await DT.showColumn("Použiť pre Entitu");
    await DT.showColumn("Cieľové pole");

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

    I.relogin("admin");
    I.amOnPage("/admin/v9/settings/ai-assistants/");
    DT.resetTable();
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

        DTE.selectOption("provider", openAiId);
        I.seeElement("#DTE_Field_model");
        let optionsD = ["gpt-4.1", "gpt-4.1-nano", "gpt-4.1-mini"];
        checkAutocomplete(I, "DTE_Field_model", "gpt-4.1", null, optionsD);

        DTE.selectOption("provider", geminiId);
        I.seeElement("#DTE_Field_model");
        let optionsE = ["gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite"];
        checkAutocomplete(I, "DTE_Field_model", "gemini-2.5", null, optionsE);
});

Scenario('ai buttons usage', async ({I, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageId);
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-perex-tab");

    I.say("Check buttons");
    I.seeElement( locate(".DTE_Field_Name_htmlData").find( locate(btnAiAssistants) ) );
    I.seeElement( locate(".DTE_Field_Name_perexPlace").find( locate(btnAiAssistants) ) );
    I.seeElement( locate(".DTE_Field_Name_perexImage").find( locate(btnAiAssistants) ) );

    I.say("Test AI toast");
    I.click( locate(".DTE_Field_Name_htmlData").find( locate(btnAiAssistants) ) );
    I.waitForVisible("#toast-container-ai");

    within("div#toast-container-ai", () => {
        I.seeElement( locate(btnAiAction).withText("Vytvoriť zhrnutie") );
        I.seeElement( locate(btnAiAction).withText("SEO Meta popis") );
        I.seeElement( locate(btnAiAction).withText("Napísať nový text/článok") );

        I.dontSeeElement( locate(btnAiAction).withText("Vytvoriť nový obrázok") );
        I.dontSeeElement( locate(btnAiAction).withText("Odstrániť pozadie") );
    });
    I.click( locate("#toast-container-ai").find(".toast-close-button") );
    I.waitForInvisible("#toast-container-ai");

    I.click( locate(".DTE_Field_Name_perexImage").find( locate(btnAiAssistants) ) );
    I.waitForVisible("#toast-container-ai");
    within("div#toast-container-ai", () => {
        I.dontSeeElement( locate(btnAiAction).withText("Vytvoriť zhrnutie") );
        I.dontSeeElement( locate(btnAiAction).withText("SEO Meta popis") );
        I.dontSeeElement( locate(btnAiAction).withText("Napísať nový text/článok") );

        I.seeElement( locate(btnAiAction).withText("Vytvoriť nový obrázok") );
        I.seeElement( locate(btnAiAction).withText("Odstrániť pozadie") );
    });

    I.say("Test image values for generation");
    I.click( locate(btnAiAction).withText("Vytvoriť nový obrázok") );
    within(containerAiContent, () => {
        I.seeElement("textarea#ai-user-prompt");

        I.say("Test that openai bonus content is here");
        I.seeElement( locate("div.bonus-content").find("input#bonusContent-imageCount") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageSize") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageQuality") );
    });
});


/* !!! LOCAL AI sa sa nedá otestovať, nakoľko server beží na Linuxe !!! */


/* AI assistant with TEXT response */

Scenario('test OpenAI AI text answers', async ({I, DTE}) => {
    textAnswerTest(I, DTE, openAiId);
});

Scenario('test Gemini AI text answers', async ({I, DTE}) => {
    textAnswerTest(I, DTE, geminiId);
});

Scenario('test OpenRouter AI text answers', async ({I, DTE}) => {
    textAnswerTest(I, DTE, openRouterId);
});

/**
 * THE asistants MUST BE IDENTICAL - only provider change
 */
async function textAnswerTest(I, DTE, aiProviderId) {
    openPageAndPerexTab(I, DTE);

    I.say("TRY basic TEXT answer without stream");
        I.clickCss("#pills-dt-datatableInit-fields-tab");
        I.fillField("#DTE_Field_fieldS", defaultValue);

        runTextAnswer(I, "fieldS", "Vytvoriť zoznam kľúčových slov", aiProviderId, "ti.ti-tags");

        const valueA = await I.grabValueFrom('#DTE_Field_fieldS');
        I.assertNotContain(valueA, defaultValue);
        let parts = valueA.split("|");
        I.assertEqual(5, parts.length);

    I.say("Revert value and check");
        I.click( locate(containerAiContent + " > .ai-status-buttons-container > .text-end > button.btn-ai-undo").withText("Zrušiť zmenu") );
        I.waitForVisible( locate(btnAiAction).withText("Vytvoriť zoznam kľúčových slov") );
        const valueB = await I.grabValueFrom('#DTE_Field_fieldS');
        I.assertEqual(defaultValue, valueB);

    I.say("TRY the TEXT answer WITH stream");
        I.clickCss("#pills-dt-datatableInit-perex-tab");
        I.fillField("#DTE_Field_htmlData", defaultValue);

        runTextAnswer(I, "htmlData", "Vytvoriť zhrnutie", aiProviderId, "ti.ti-list-letters");

        const valueC = await I.grabValueFrom('#DTE_Field_htmlData');
        //Theer is no other way how to check value
        I.assertNotContain(valueC, defaultValue);
}


/* AI assistant with IMAGE response - WITHOUT user promp (user input) */

//For OpenAI is tets more extended, so we can cover more things. Rest of providers do just baisc test they works.
Scenario('test OpenAI AI image answers - no user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Odstrániť pozadie", openAiId);
    checkBaseWaitDialog(I, "Odstrániť pozadie", openAiId, "ti.ti-photo-x");
    waiToEndImage(I);
    checkImages(I, 1);
    checkImageInfo(I);

    I.say("Check that I see donwload button for image");
    I.seeElement( locate(containerAiContent).find( locate("button.select-image").withText("Uložiť obrázok") ) );
    I.seeElement( locate(containerAiContent).find("button.select-image > i.ti.ti-download") );

    I.click( locate(containerAiContent).find( locate("button.select-image").withText("Uložiť obrázok") ) );

    I.say("Check name selection");
    I.seeElement( locate("#toast-ai-name-selection").find( locate("button#rewrite").withText("Prepísať súbor") ) );
    I.seeElement( locate("#toast-ai-name-selection").find( locate("button#rename").withText("Premenovať súbor na:") ) );
    I.seeElement( locate("#toast-ai-name-selection").find( locate("button#back").withText("Zrušiť") ) );

    I.say("Go back and check that buttons are not visible");
    I.click(locate("button#back").withText("Zrušiť"));
    I.dontSeeElement("#toast-ai-name-selection");

    I.say("Now click back so you go back to assistants selection");
    I.click(locate(".toast-title > .header-back-button > button").withText("Späť"));

    I.seeElement( locate('button.btn-ai-action').withText("Odstrániť pozadie").withChild(locate('span.provider').withText(openAiId)));
});

Scenario('test Gemini AI image answers - no user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Odstrániť pozadie", geminiId);
    checkBaseWaitDialog(I, "Odstrániť pozadie", geminiId, "ti.ti-photo-x");
    waiToEndImage(I);
    checkImages(I, 1);
    checkImageInfo(I);
});

Scenario('test OpenRouter AI image answers - no user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Odstrániť pozadie", openRouterId);
    checkBaseWaitDialog(I, "Odstrániť pozadie", openRouterId, "ti.ti-photo-x");
    waiToEndImage(I);
    checkImages(I, 1);
    checkImageInfo(I);
});

/* AI assistant with IMAGE response - WITH user promp (user input) AND maybe miage settings like quality, if provider supports it */

Scenario('test OpenAI AI image answers - WITH user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Vytvoriť nový obrázok", openAiId);

    I.say("CHECK user input");
    I.seeElement( locate(containerAiContent).find("textarea#ai-user-prompt") );

    I.seeElement( locate(containerAiContent).find( locate(".bonus-content > div:nth-child(1) > label").withText("Počet obrázkov") ) );
    I.seeElement( locate(containerAiContent).find(".bonus-content > div:nth-child(1) > input#bonusContent-imageCount") );
    I.seeElement( locate(containerAiContent).find( locate(".bonus-content > div:nth-child(2) > label").withText("Rozmer") ) );
    I.seeElement( locate(containerAiContent).find(".bonus-content > div:nth-child(2) > select#bonusContent-imageSize") );
    I.seeElement( locate(containerAiContent).find( locate(".bonus-content > div:nth-child(3) > label").withText("Kvalita") ) );
    I.seeElement( locate(containerAiContent).find(".bonus-content > div:nth-child(3) > select#bonusContent-imageQuality") );

    I.say("Request 2 images of a car");
    I.fillField(locate(containerAiContent).find("textarea#ai-user-prompt"), "car black and white");
    I.fillField(locate(containerAiContent).find("input#bonusContent-imageCount"), 2);
    I.click( locate(containerAiContent).find(locate(".text-end > button").withText("Generovať")) )

    checkBaseWaitDialog(I, "Vytvoriť nový obrázok", openAiId, "ti.ti-photo-ai", true);
    waiToEndImage(I, true);
    checkImages(I, 2);
});

Scenario('test Gemini AI image answers - WITH user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Vytvoriť nový obrázok", geminiId);

    I.say("CHECK user input");
    I.seeElement( locate(containerAiContent).find("textarea#ai-user-prompt") );

    // GEMINI do not have image settings like OpenAI .. aka count, quality and size

    I.say("Request image of a car");
    I.fillField(locate(containerAiContent).find("textarea#ai-user-prompt"), "car black and white");
    I.click( locate(containerAiContent).find(locate(".text-end > button").withText("Generovať")) )

    checkBaseWaitDialog(I, "Vytvoriť nový obrázok", geminiId, "ti.ti-photo-ai", true);
    waiToEndImage(I, true);
    checkImages(I, 1);
});

Scenario('test OpenRouter AI image answers - WITH user input', async ({I, DTE}) => {
    openPageAndPerexTab(I, DTE);
    openPageAndPerexTab(I, DTE);
    startAssistant(I, "perexImage", "Vytvoriť nový obrázok", openRouterId);

    I.say("CHECK user input");
    I.seeElement( locate(containerAiContent).find("textarea#ai-user-prompt") );

    // OpenRouter have image settings like OpenAI ... BUT we do not implement them

    I.say("Request image of a car");
    I.fillField(locate(containerAiContent).find("textarea#ai-user-prompt"), "car black and white");
    I.click( locate(containerAiContent).find(locate(".text-end > button").withText("Generovať")) )

    checkBaseWaitDialog(I, "Vytvoriť nový obrázok", openRouterId, "ti.ti-photo-ai", true);
    waiToEndImage(I, true);
    checkImages(I, 1);
});

/* Support functions */

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

function openPageAndPerexTab(I, DTE) {
    I.say("openPageAndPerexTab");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + pageId);
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss("#pills-dt-datatableInit-perex-tab");
    I.waitForVisible("#DTE_Field_htmlData");
}

function startAssistant(I, field, assistantName, provider) {
    I.say("startAssistant");

    I.click( locate(".DTE_Field_Name_" + field).find( locate(btnAiAssistants) ) );
    I.waitForVisible("#toast-container-ai");
    I.click( locate('button.btn-ai-action').withText(assistantName).withChild(locate('span.provider').withText(provider)));
    I.waitForVisible("div.toast.toast-info");
}

function checkBaseWaitDialog(I, assistantName, provider, icon, hasContainer = false) {
    I.say("checkBaseWaitDialog");

    I.seeElement( locate(".toast.toast-info > .toast-title > .header-back-button > button").withText("Späť") );
    I.seeElement( locate(".toast.toast-info > .toast-title > .header-back-button > .ai-title > i." + icon) );
    I.seeElement( locate(".toast.toast-info > .toast-title > .header-back-button > .ai-title > span").withText(assistantName) );
    I.seeElement( locate(".toast.toast-info > .toast-title > .header-back-button > .ai-title > span.provider").withText("(" + provider + ")") );

    if(hasContainer === false) {
        I.seeElement( locate(containerAiContent + " > .current-status > span").withText("AI už na tom pracuje...") );
        I.seeElement( locate(containerAiContent + " > .current-status > span > i.ti.ti-exclamation-circle") );
    } else {
        I.seeElement( locate(containerAiContent + " > .user-prompt-container > .chat-error-container > .current-status > span").withText("AI už na tom pracuje...") );
        I.seeElement( locate(containerAiContent + " > .user-prompt-container > .chat-error-container > .current-status > span > i.ti.ti-exclamation-circle") );
    }
}

function waiToEndText(I) {
    I.say("waiToEndText");

    I.waitForInvisible( locate(containerAiContent + " > .current-status > span").withText("AI už na tom pracuje...") );
    I.seeElement( locate(containerAiContent + " > .current-status > span").withText("Hotovo! Bolo použitých") );
    I.seeElement( locate(containerAiContent + " > .current-status > span > i.ti.ti-circle-check") );
}

function waiToEndImage(I, hasContainer = false) {
    I.say("waiToEndImage");

    if(hasContainer === false) {
        I.waitForInvisible( locate(containerAiContent + " > .current-status > span").withText("AI už na tom pracuje..."), 180 );
        I.seeElement( locate(containerAiContent + " > .chat-error-container > div.current-status > span").withText("Hotovo! Bolo použitých") );
        I.seeElement( locate(containerAiContent + " > .chat-error-container > div.current-status > span > i.ti.ti-circle-check") );
    } else {
        I.waitForInvisible( locate(containerAiContent + " > .user-prompt-container > .chat-error-container > .current-status > span").withText("AI už na tom pracuje..."), 180 );
        I.seeElement( locate(containerAiContent + "  > .chat-error-container > div.current-status > span").withText("Hotovo! Bolo použitých") );
        I.seeElement( locate(containerAiContent + " > .chat-error-container > div.current-status > span > i.ti.ti-circle-check") );
    }

    I.seeElement( locate(containerAiContent + " > div").withText("Názov obrázku") );

    I.seeElement( locate(containerAiContent + " > .ai-image-preview-div") );
    I.seeElement( locate(containerAiContent + " > .image-info") );
    I.seeElement( locate(containerAiContent + " > .button-div.text-end") );
}

function runTextAnswer(I, field, assistantName, provider, icon) {
    startAssistant(I, field, assistantName, provider);

    checkBaseWaitDialog(I, assistantName, provider, icon);

    waiToEndText(I);

    I.waitForVisible( locate(containerAiContent + " > .ai-status-buttons-container > .text-end > button.btn-ai-undo").withText("Zrušiť zmenu"), 5);
    I.seeElement(containerAiContent + " > .ai-status-buttons-container > .text-end > button.btn-ai-undo > i.ti-arrow-back");
}

function checkImages(I, imagesCount) {
    I.say("checkImages");

    //Verify number of visible images
    I.seeNumberOfVisibleElements( locate(containerAiContent + " > .ai-image-preview-div > .image-preview"), imagesCount );

    //Check that every preview has image and button
    for(let i = 0; i < imagesCount; i++) {
        I.seeElement( locate(containerAiContent + " > .ai-image-preview-div > div:nth-child(" + (i+1) +  ") > img") );
        I.seeElement( locate(containerAiContent + " > .ai-image-preview-div > div:nth-child(" + (i+1) +  ") > a.zoom-in") );
    }
}

function checkImageInfo(I) {
    I.say("checkImageInfo");

    I.seeElement( locate(containerAiContent + " > .image-info > div:nth-child(1) > label").withText("Názov obrázku") );
    I.seeInField( locate(containerAiContent + " > .image-info > div:nth-child(1) > input"), originalImageName);

    I.seeElement( locate(containerAiContent + " > .image-info > div:nth-child(2) > label").withText("Umiestnenie") );
    I.seeInField( locate(containerAiContent + " > .image-info > div:nth-child(2)").find("input.form-control"), originalImageLocation);
}