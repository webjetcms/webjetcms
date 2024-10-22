Feature('webpages.editor');

Before(({ I, login }) => {
    login('admin');
});

Scenario('editor', ({I, DTE, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    Document.screenshot("/redactor/webpages/editor/tab-content.png");

    I.clickCss("a.cke_button__pastefromword");
    Document.screenshot("/redactor/webpages/editor/paste-word.png");
    I.clickCss("div.cke_dialog_container a.cke_dialog_ui_button.cke_dialog_ui_button_cancel");

    I.clickCss("#pills-dt-datatableInit-basic-tab");
    Document.screenshot("/redactor/webpages/editor/tab-basic.png");

    I.clickCss("#pills-dt-datatableInit-menu-tab");
    Document.screenshot("/redactor/webpages/editor/tab-menu.png");

    I.clickCss("#pills-dt-datatableInit-access-tab");
    Document.screenshot("/redactor/webpages/editor/tab-access.png");

    I.clickCss("#pills-dt-datatableInit-perex-tab");
    Document.screenshot("/redactor/webpages/editor/tab-perex.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=79020");
    DTE.waitForEditor();
    I.waitForElement("div.CodeMirror-code", 10);
    Document.screenshot("/redactor/webpages/editor/html-mode.png");
});

Scenario('working-in-editor', ({ I, Document, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=100605");
    DTE.waitForEditor();

    let settingDialogA = ".cke.cke_reset_all.cke_1.cke_panel.cke_panel.cke_menu_panel.cke_ltr";

    Document.screenshotElement('.DTE.DTE_Action_Edit.modal-content','/redactor/webpages/working-in-editor/editor_preview.png')
    I.clickCss('#cke_11');
    Document.screenshotElement('#cke_117','/redactor/webpages/working-in-editor/roletka.png');
    I.clickCss('#cke_11');

    const selectorsAndPaths = [
        ['#cke_13', '/redactor/webpages/working-in-editor/paste.png'],
        ['#cke_14', '/redactor/webpages/working-in-editor/cut.png'],
        ['#cke_15', '/redactor/webpages/working-in-editor/copy.png'],
        ['#cke_16', '/redactor/webpages/working-in-editor/paste_text.png'],
        ['#cke_17', '/redactor/webpages/working-in-editor/paste_from_word.png'],
        ['#cke_19', '/redactor/webpages/working-in-editor/remove_format.png'],
        ['#cke_20', '/redactor/webpages/working-in-editor/bold.png'],
        ['#cke_21', '/redactor/webpages/working-in-editor/italic.png'],
        ['#cke_22', '/redactor/webpages/working-in-editor/underline.png'],
        ['#cke_23', '/redactor/webpages/working-in-editor/strike.png'],
        ['#cke_24', '/redactor/webpages/working-in-editor/subscript.png'],
        ['#cke_25', '/redactor/webpages/working-in-editor/superscript.png'],
        ['#cke_26', '/redactor/webpages/working-in-editor/text_color.png'],
        ['#cke_27', '/redactor/webpages/working-in-editor/bcg_color.png'],
        ['#cke_29', '/redactor/webpages/working-in-editor/bulleted_list.png'],
        ['#cke_30', '/redactor/webpages/working-in-editor/numbered_list.png'],
        ['#cke_31', '/redactor/webpages/working-in-editor/outdent.png'],
        ['#cke_32', '/redactor/webpages/working-in-editor/indent.png'],
        ['#cke_38', '/redactor/webpages/working-in-editor/table.png'],
        ['#cke_40', '/redactor/webpages/working-in-editor/image.png'],
        ['#cke_41', '/redactor/webpages/working-in-editor/unlink.png'],
        ['#cke_42', '/redactor/webpages/working-in-editor/specialchar.png'],
        ['#cke_43', '/redactor/webpages/working-in-editor/tooltip.png'],
        ['#cke_44', '/redactor/webpages/working-in-editor/link.png'],
        ['#cke_45', '/redactor/webpages/working-in-editor/wjform.png'],
        ['#cke_46', '/redactor/webpages/working-in-editor/components.png'],
        ['#cke_47', '/redactor/apps/htmlbox/htmlbox_icon.png']
    ];

    I.say("DO screens of buttons in TOOLBAR");
    selectorsAndPaths.forEach(item => {
        const [selector, path] = item;
        Document.screenshotElement(selector, path);
    });

    I.say("Add link screens");
        I.clickCss('#cke_44');
        I.waitForVisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe"), 15);
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe") );
        I.waitForVisible("table.urlFormTable");
        Document.screenshotElement("div.elfinder-button.div-create", '/redactor/webpages/working-in-editor/elfinder_addFolder.png');
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/link_dialog.png');

        //link to page doalog
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe") );
        I.click("#nav-iwcm_doc_group_volume_");
        I.waitForElement("#nav-iwcm_doc_group_volume_L2dyb3VwOjE_E", 5); //Jet portal 4
        I.click("#nav-iwcm_doc_group_volume_L2dyb3VwOjE_E");
        I.waitForElement("#nav-iwcm_doc_group_volume_L2dyb3VwOjI0", 5); //zo sveta financii
        I.click("#nav-iwcm_doc_group_volume_L2dyb3VwOjI0");
        I.waitForElement("#iwcm_doc_group_volume_L2RvYzoxNg_E_E", 5); //konzolidacia napriec trhmi
        I.click("#iwcm_doc_group_volume_L2RvYzoxNg_E_E");
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/link_dialog-webpage.png');

        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe"), 5);

    I.say("Add image screens");
        I.clickCss('#cke_40');
        I.waitForVisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement"), 15);
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement") );
        I.waitForVisible("table.urlFormTable");
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog.png');
        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement"), 5);

        I.say("Image - right click");
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        I.rightClick( locate("#WebJETEditorBody").find("img") );
        I.switchTo();
        I.waitForVisible(locate(settingDialogA).last(), 10);
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        Document.screenshotElement("#WebJETEditorBody", '/redactor/webpages/working-in-editor/image_right_click.png');
        I.switchTo();

    I.say("list - screens");
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        I.waitForVisible("#WebJETEditorBody > ul > li:nth-child(1)", 10);
        I.rightClick("#WebJETEditorBody > ul > li:nth-child(1)");
        I.switchTo();
        I.waitForVisible(locate(settingDialogA).last(), 10);
        Document.screenshotElement(locate(settingDialogA).last(), '/redactor/webpages/working-in-editor/list_right_click.png');

        I.say("List - right click");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").last());
        I.clickCss( "a.cke_menubutton__bulletedlist" );
        I.switchTo();
        I.waitForVisible(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page", 5);
        Document.screenshotElement(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page"), '/redactor/webpages/working-in-editor/list_settings.png');
        I.click(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page").find("a.cke_dialog_close_button"));
        I.waitForInvisible(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page", 5);

    I.say("DO table screens");
        I.say("Table dialog");
        I.clickCss('#cke_38');
        Document.screenshotElement(locate('.cke.cke_reset_all.cke_1.cke_panel.cke_panel.cke_ltr').last(),'/redactor/webpages/working-in-editor/table_preview.png');
        I.switchTo(locate('.cke.cke_reset_all.cke_1.cke_panel.cke_panel.cke_ltr').last());
        I.switchTo('iframe');
        I.clickCss('.cke_colormore');
        I.switchTo();
        Document.screenshotElement(locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(),'/redactor/webpages/working-in-editor/table_dialog.png');
        I.clickCss(".cke_dialog_ui_button_ok");

        I.say("Table - setting");
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        I.rightClick('.table-responsive');
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last(), '/redactor/webpages/working-in-editor/table_edit.png');

        I.say("Table - setting -> RIADOK");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").last());
        I.click("span.cke_menuitem > a.cke_menubutton[title=Riadok]");
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_row.png');

        I.say("Table - setting -> STLPEC");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").first());
        I.click("span.cke_menuitem > a.cke_menubutton[title=Stĺpec]");
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_column.png');

        I.say("Table - setting -> BUNKA");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").first());
        I.click("span.cke_menuitem > a.cke_menubutton[title=Bunka]");
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_cell.png');

        I.say("Table - setting -> BUNKA -> VLASTNOSTI");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").last());
        I.click("Vlastnosti bunky");
        I.switchTo();
        I.waitForVisible(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page").last(), 5);
        Document.screenshotElement(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page").last(), "/redactor/webpages/working-in-editor/table_edit_cell_edit.png");
        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page"), 5);

    I.clickCss('#cke_42');
    I.waitForVisible(locate('.cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page').last(), 5);
    Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page').last(),'/redactor/webpages/working-in-editor/specialchar_dialog.png');
    I.clickCss(".cke_dialog_ui_button_cancel");

    I.clickCss('#cke_47');
    Document.screenshot('/redactor/apps/htmlbox/htmlbox_dialog.png');
    I.clickCss(".cke_dialog_ui_button_cancel");
});

Scenario('appstore', ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=17321");
    DTE.waitForEditor();
    I.wait(6);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.forceClick("#WebJETEditorBody p");
    I.switchTo();

    I.clickCss("a.cke_button__components");
    I.waitForElement("div.cke_editor_data_dialog");
    I.wait(6);
    Document.screenshot("/redactor/webpages/working-in-editor/appstore.png");

    I.switchTo("iframe.cke_dialog_ui_iframe");
    I.switchTo("#editorComponent")

    I.waitForVisible("input#search");
    I.fillField("input#search", "mapa");
    I.waitForVisible("#components-map-title", 10);
    I.wait(2);
    Document.screenshot("/redactor/webpages/working-in-editor/appstore-search.png");

    //klikni na demo komponentu
    I.clickCss("#components-map-title");
    I.wait(3);

    Document.screenshot("/redactor/webpages/working-in-editor/appstore-insert.png");

    I.click("div.app-buy > a.buy");

    Document.screenshot("/redactor/webpages/working-in-editor/appstore-editorcomponent.png");

    I.switchTo();
    I.clickCss(".cke_dialog_ui_button_cancel");
    DTE.cancel();
});