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
            I.clickCss("#pills-dt-datatableInit-instructions-tab");
            I.fillField("#DTE_Field_instructions", "Say hi to me.");

            I.clickCss("#pills-dt-datatableInit-basic-tab");
            DTE.selectOption("provider", "Prehliadač");
            I.dontSeeElement("#DTE_Field_model");
            DTE.selectOption("provider", "OpenAI");
            I.seeElement("#DTE_Field_model");
        },
        editSteps: function(I, options) {
            I.clickCss("#pills-dt-datatableInit-instructions-tab");
            I.seeInField("#DTE_Field_instructions", "Say hi to me.");

            I.clickCss("#pills-dt-datatableInit-basic-tab");
            I.seeInField("#DTE_Field_model", "gpt-3.5-turbo");
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('ai buttons usage', async ({I, DT, DTE}) => {
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
        I.seeElement( locate("button.btn-ai-action").withText("Preložiť do Angličtiny") );

        I.dontSeeElement( locate("button.btn-ai-action").withText("Vygeneruj obrázok") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Odstrániť pozadie") );
    });
    I.click( locate("#toast-container-ai").find("button.toast-close-button") );
    I.waitForInvisible("#toast-container-ai");

    I.click( locate(".DTE_Field_Name_perexImage").find( locate("button.btn-ai[aria-label='AI asistent']") ) );
    I.waitForVisible("#toast-container-ai");
    within("div#toast-container-ai", () => {
        I.dontSeeElement( locate("button.btn-ai-action").withText("Vytvoriť zhrnutie") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Vytvoriť zoznam kľúčových slov") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Opraviť gramatiku") );
        I.dontSeeElement( locate("button.btn-ai-action").withText("Preložiť do Angličtiny") );

        I.seeElement( locate("button.btn-ai-action").withText("Vygeneruj obrázok") );
        I.seeElement( locate("button.btn-ai-action").withText("Odstrániť pozadie") );
    });

    I.say("Test image values for generation");
    I.click( locate("button.btn-ai-action").withText("Vygeneruj obrázok") );
    within("#toast-container-ai-content", () => {
        I.seeElement("textarea#ai-user-prompt");

        I.say("Test that openai bonus content is here");
        I.seeElement( locate("div.bonus-content").find("input#bonusContent-imageCount") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageSize") );
        I.seeElement( locate("div.bonus-content").find("select#bonusContent-imageQuality") );
    });

    //TODO - chceme realne volat OpenAI ?? lebo kazdy test bude stat tokeny

});