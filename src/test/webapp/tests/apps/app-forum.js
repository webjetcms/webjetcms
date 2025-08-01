Feature('apps.app-forum');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('Forum app - test INCLUDE building', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Diskusia', '#components-forum-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.say("Check tabs");
    I.seeElement("#pills-dt-component-datatable-basic-tab");
    I.seeElement("#pills-dt-component-datatable-componentIframeWindowTabList-tab");
    I.seeElement("#pills-dt-component-datatable-commonSettings-tab");
    I.dontSeeElement("#pills-dt-component-datatable-groups-tab");

    I.switchTo();
    I.clickCss('.cke_dialog .cke_dialog_footer_buttons a.cke_dialog_ui_button.cke_dialog_ui_button_ok');

    const defaultParams = {
        type: "normal",
        noPaging: "true",
        sortAscending : "true",
        notifyPageAuthor: "false"
    };

    await Apps.assertParams(defaultParams, "/components/forum/forum.jsp");

    Apps.openAppEditor();

    I.say("Test show/hide of fields");
        I.seeElement("#DTE_Field_forumType");
        I.seeElement("#DTE_Field_sortAscending");
        I.seeElement("#DTE_Field_type");
        I.seeElement("#DTE_Field_noPaging_0");
        I.seeElement("#DTE_Field_notifyPageAuthor_0");

        I.dontSeeElement("#DTE_Field_sortTopicsBy");
        I.dontSeeElement("#DTE_Field_pageSize");
        I.dontSeeElement("#DTE_Field_pageLinksNum");
        I.dontSeeElement("#DTE_Field_sortTopicsBy");
        I.dontSeeElement("#DTE_Field_delMinutes");

        //Change forum type
        DTE.selectOption("forumType", "viactémová diskusia (Message Board)");
        I.dontSeeElement("#DTE_Field_type");
        I.dontSeeElement("#DTE_Field_noPaging_0");
        I.seeElement("#DTE_Field_sortTopicsBy");
        I.seeElement("#DTE_Field_pageSize");
        I.seeElement("#DTE_Field_pageLinksNum");
        I.seeElement("#DTE_Field_sortTopicsBy");
        I.seeElement("#DTE_Field_delMinutes");

        I.say("Change params");
            I.fillField("#DTE_Field_pageSize", 15);
            DTE.selectOption("sortTopicsBy", "Podľa dátumu vytvorenia témy");

        //Test for auto making groups in separe test
        I.seeElement("#pills-dt-component-datatable-groups-tab");

        I.say("Check forum list tab");
        I.clickCss("#pills-dt-component-datatable-componentIframeWindowTabList-tab");
        I.waitForVisible("#DTE_Field_iframe", 5);

    I.seeElement("#pills-dt-component-datatable-basic-tab");
    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const changedParams = {
        type: "topics",
        sortAscending: "true",
        sortTopicsBy: "QuestionDate",
        pageSize: "15",
        pageLinksNum: "10",
        useDelTimeLimit: "true",
        delMinutes: "30",
        notifyPageAuthor: "false",
        rootGroup: "true"
    };

    await Apps.assertParams(changedParams, "/components/forum/forum_mb.jsp");
});