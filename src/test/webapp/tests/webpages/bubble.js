Feature('webpages.bubble');
//test uprava bunky v datatabulke

Before(({ I, login }) => {
    login('admin');
});

Scenario('editacia kde nie su listy - najskor editor', async ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filter("mediaLink", "/files/cennik.pdf");

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("mediaTable");
    I.seeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.seeElement("#DTE_Field_mediaSortOrder")
    DTE.cancel();

    I.wait(1);
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#mediaTable tbody tr td:nth-child(3)");
    I.dontSeeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.dontSeeElement("#DTE_Field_mediaSortOrder")

    I.click("div.DTE_Bubble div.DTE_Form_Buttons button.btn-outline-secondary");
    I.wait(1);

    //znova skus editor, ci je vsetko v poriadku
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("mediaTable");
    I.seeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.seeElement("#DTE_Field_mediaSortOrder")
    DTE.cancel();
});

Scenario('editacia kde nie su listy - najskor bubble', async ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/webpages/media/");
    DT.filter("mediaLink", "/files/cennik.pdf");

    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#mediaTable tbody tr td:nth-child(3)");
    I.dontSeeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.dontSeeElement("#DTE_Field_mediaSortOrder")

    I.click("div.DTE_Bubble div.DTE_Form_Buttons button.btn-outline-secondary");
    I.wait(1);

    //over zobrazenie normalneho editora
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("mediaTable");
    I.seeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.seeElement("#DTE_Field_mediaSortOrder")
    DTE.cancel();

    //skus znova bubble
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#mediaTable tbody tr td:nth-child(3)");
    I.dontSeeElement("div.DTE_Field_Name_mediaLink input.form-control");
    I.seeElement("#DTE_Field_mediaTitleSk");
    I.dontSeeElement("#DTE_Field_mediaSortOrder")

});

Scenario('editacia kde su listy - najskor editor', async ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/apps/insert-script/");
    DT.filter("name", "Pokus 02");

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("insertScriptTable");
    I.seeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.click("#pills-dt-insertScriptTable-scriptPerms-tab");
    I.seeElement("#DTE_Field_validTo")
    DTE.cancel();

    I.wait(1);
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    //stlpec: umiestnenie
    I.click("#insertScriptTable tbody tr td:nth-child(3)");
    I.dontSeeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.dontSeeElement("#DTE_Field_validTo")

    I.click("div.DTE_Bubble div.DTE_Form_Buttons button.btn-outline-secondary");
    I.wait(1);

    //znova skus editor, ci je vsetko v poriadku
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("insertScriptTable");
    I.seeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.click("#pills-dt-insertScriptTable-scriptPerms-tab");
    I.seeElement("#DTE_Field_validTo")
    DTE.cancel();
});

Scenario('editacia kde su listy - najskor bubble', async ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/apps/insert-script/");
    DT.filter("name", "Pokus 02");

    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#insertScriptTable tbody tr td:nth-child(3)");
    I.dontSeeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.dontSeeElement("#DTE_Field_validTo");

    I.click("div.DTE_Bubble div.DTE_Form_Buttons button.btn-outline-secondary");
    I.wait(1);

    //over zobrazenie normalneho editora
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("td.dt-row-edit a");
    DTE.waitForEditor("insertScriptTable");
    I.seeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.click("#pills-dt-insertScriptTable-scriptPerms-tab");
    I.seeElement("#DTE_Field_validTo")
    DTE.cancel();

    //skus znova bubble
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#insertScriptTable tbody tr td:nth-child(3)");
    I.dontSeeElement("#DTE_Field_name");
    I.seeElement("#DTE_Field_position");
    I.dontSeeElement("#DTE_Field_validTo");
});

Scenario('BUG editacia bunky pouzije staru hodnotu v CKEditore', async ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=48191");

    var subpageName = "Test subpage";

    I.say("nastav default text do Bubble");
    I.click("Bubble", "#datatableInit_wrapper");
    DTE.waitForEditor();
    await DTE.fillCkeditor("<p>Test bubble, kedy sa obsah tejto stranky prenesie to stranky ktoru editujem cez bubble editaciu.</p>");
    DTE.save();

    //
    I.say("nastav default text do "+subpageName);
    I.click(subpageName, "#datatableInit_wrapper");
    DTE.waitForEditor();
    await DTE.fillCkeditor("<p>Bubble "+subpageName+"</p>");
    DTE.save();

    //tu bol bug, ze sa pre celledit pouzil potom tento posledny text z ckeditora
    I.say("nastav pracovnu verziu do "+subpageName);
    I.click(subpageName, "#datatableInit_wrapper");
    DTE.waitForEditor();
    await DTE.fillCkeditor("<p>Bubble "+subpageName+" - pracovna verzia</p>");
    I.click("#webpagesSaveCheckbox");
    DTE.save();
    DTE.cancel();
    I.click(".toast-close-button");

    //
    I.say("Zapni celledit a edituj bunku");
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click(locate("td.dt-row-edit").withText(subpageName));
    I.waitForElement("div.DTE.DTE_Bubble");
    I.click(".toast-close-button");
    I.seeInField({css: "div.DTE.DTE_Bubble div.DTE_Bubble_Table div.DTE_Field_Name_title input"}, subpageName);
    DTE.saveBubble();

    //
    I.say("Over ze sa nezobrazilo okno standardneho editora");
    I.dontSeeElement("div.DTED.show");

    I.click(locate("td.dt-row-edit").withText("Bubble"));
    I.waitForElement("div.DTE.DTE_Bubble");
    I.seeInField({css: "div.DTE.DTE_Bubble div.DTE_Bubble_Table div.DTE_Field_Name_title input"}, "Bubble");
    DTE.saveBubble();

    //
    I.say("Over ze sa nezobrazilo okno standardneho editora");
    I.dontSeeElement("div.DTED.show");

    //
    I.say("Over ze sa nezmenil text stranky");
    I.amOnPage("/test-stavov/bubble/");
    I.dontSee("Bubble "+subpageName);
    I.dontSee("pracovna verzia");
    I.see("Test bubble, kedy sa obsah tejto stranky prenesie to stranky ktoru editujem cez bubble editaciu.");

    I.amOnPage("/test-stavov/bubble/bubble-test-page.html")
    I.see("Bubble "+subpageName);
    I.dontSee("Test bubble, kedy sa obsah tejto stranky prenesie to stranky ktoru editujem cez bubble editaciu.");
    I.dontSee("pracovna verzia");
});

Scenario('BUG okno mimo dosah', ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.click("button.btn-gallery-size-table");
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);

    I.click("#galleryTable tbody tr td:nth-child(6)");
    //bubble element must be visible with below CSS class
    I.waitForElement("div.DTE.DTE_Bubble.below", 10);
});

Scenario('BUG okno mimo dosah-reset settings', ({I, DTE, DT}) => {
    I.amOnPage("/admin/v9/apps/gallery/");
    DT.waitForLoader();
    I.click("button.btn-gallery-size-s");
});