Feature('webpages.webpage-content');

var folder_name, randomNumber;

Before(({ I, DTE, login }) => {
     login('admin');
     I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=153671");
     DTE.waitForEditor();
});

function checkField(I, label, value) {
    I.switchTo();
    I.waitForElement(".cke_reset_all.cke_dialog_container", 10);
    I.seeInField(locate(".cke_reset_all.cke_dialog_container .cke_dialog_ui_text").withText(label).find("input.cke_dialog_ui_input_text"), value);
    I.click(".cke_reset_all.cke_dialog_container .cke_dialog_footer a.cke_dialog_ui_button_cancel");
    I.waitForInvisible(".cke_reset_all.cke_dialog_container", 10)
}

function ckeditorCheckField(I, value, isButton = false) {

    I.switchTo(".cke_wysiwyg_frame.cke_reset");

    if (isButton) I.dispatchMouseEvent("button.btn", value, { x: 10, y: 10 });
    else I.click(locate("a").withText(value));

    var label = "Text";
    if (isButton) label = "Text tlačidla";

    checkField(I, label, value);

    //
    if (isButton) {
        I.say("Right click and verify context menu");
        I.switchTo(".cke_wysiwyg_frame.cke_reset");
        I.wait(1);

        if ("NON clicking button" === value) {
            I.dispatchMouseEvent("button.btn", value, { x: 40, y: 20 }, "right");
        } else {
            I.rightClick(locate("button.btn").withText(value));
        }

        I.switchTo();
        I.switchTo(".cke_panel_frame");
        I.waitForElement(".cke_menu", 10);
        I.waitForText("Tlačidlo", 5, ".cke_menuitem");

        I.click(locate(".cke_menuitem a").withText("Tlačidlo"));

        checkField(I, label, value);
    }
}

Scenario('test buttons in webpage content', ({ I, DT }) => {

    ckeditorCheckField(I, "A button");
    ckeditorCheckField(I, "A block button");

    ckeditorCheckField(I, "normal button", true);
    ckeditorCheckField(I, "NON clicking button", true);

});