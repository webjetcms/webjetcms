Feature('webpages.editor');

const fs = require("fs");
const path = require("path");
const pixabayImagePath = path.resolve(__dirname, "../../../../../main/webapp/images/test-stavov/manualtestpage/car.jpg");

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

Scenario('file-archive-read-only-icon', ({ I, DTE, Document, i18n }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=16');
    DTE.waitForEditor();
    I.clickCss('#trEditor');
    I.clickCss('.cke_button_icon.cke_button__link_icon');
    I.waitForElement('#wjLinkIframe', 10);
    I.wait(1);
    I.switchTo('#wjLinkIframe');
    I.waitForVisible(locate('.elfinder-navbar-root').withText(i18n.get("Document manager")), 20);
    I.click(locate('.elfinder-navbar-root').withText(i18n.get("Document manager")));
    I.switchTo();
    Document.screenshotElement(locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/link_dialog-read-only-archive.png');
});

Scenario('working-in-editor', ({ I, Document, DTE, i18n }) => {
    // Remove the Pixabay image left by a previous run, including another language.
    fs.rmSync(pixabayImagePath, { force: true });

    Document.setConfigValue("ckeditor_svgIcon_path", "");
    Document.setConfigValue('editorFontAwesomeCssPath', "");

    // Show the overflow tools in the mobile Page Builder preview.
    Document.resetPageBuilderMode();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.waitForVisible(".wj-cke-toolbar", 20);
    I.waitForText("Editor:", 10, ".exit-inline-editor");
    I.click("a[href*=\"pbSetWindowSize('phone')\"]");
    I.waitForVisible(".wj-cke-toolbar__more:visible", 10);
    I.click(".wj-cke-toolbar__more:visible");
    I.waitForVisible(".wj-cke-overflow", 10);
    I.moveCursorTo(".exit-inline-editor");
    I.switchTo();
    Document.screenshotElement("#DTE_Field_data-pageBuilderIframe", "/redactor/webpages/working-in-editor/toolbar-overflow.png");
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.pressKey("Escape");
    I.click("a[href*=\"pbSetWindowSize('desktop')\"]");
    I.switchTo();
    DTE.cancel();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=100605");
    DTE.waitForEditor();

    const toolbar = "#cke_data .cke_toolbox";
    const settingDialogA = ".cke_panel.cke_menu_panel";

    Document.screenshotElement('.DTE.DTE_Action_Edit.modal-content','/redactor/webpages/working-in-editor/editor_preview.png')

    const selectorsAndPaths = [
        ['a.cke_button__paste', '/redactor/webpages/working-in-editor/paste.png'],
        ['a.cke_button__cut', '/redactor/webpages/working-in-editor/cut.png'],
        ['a.cke_button__copy', '/redactor/webpages/working-in-editor/copy.png'],
        ['a.cke_button__pastetext', '/redactor/webpages/working-in-editor/paste_text.png'],
        ['a.cke_button__pastefromword', '/redactor/webpages/working-in-editor/paste_from_word.png'],
        ['a.cke_button__removeformat', '/redactor/webpages/working-in-editor/remove_format.png'],
        ['a.cke_button__bold', '/redactor/webpages/working-in-editor/bold.png'],
        ['a.cke_button__italic', '/redactor/webpages/working-in-editor/italic.png'],
        ['a.cke_button__underline', '/redactor/webpages/working-in-editor/underline.png'],
        ['a.cke_button__strike', '/redactor/webpages/working-in-editor/strike.png'],
        ['a.cke_button__subscript', '/redactor/webpages/working-in-editor/subscript.png'],
        ['a.cke_button__superscript', '/redactor/webpages/working-in-editor/superscript.png'],
        ['a.cke_button__textcolor', '/redactor/webpages/working-in-editor/text_color.png'],
        ['a.cke_button__bgcolor', '/redactor/webpages/working-in-editor/bcg_color.png'],
        ['a.cke_button__bulletedlist', '/redactor/webpages/working-in-editor/bulleted_list.png'],
        ['a.cke_button__numberedlist', '/redactor/webpages/working-in-editor/numbered_list.png'],
        ['a.cke_button__outdent', '/redactor/webpages/working-in-editor/outdent.png'],
        ['a.cke_button__indent', '/redactor/webpages/working-in-editor/indent.png'],
        ['a.cke_button__table', '/redactor/webpages/working-in-editor/table.png'],
        ['a.cke_button__image', '/redactor/webpages/working-in-editor/image.png'],
        ['a.cke_button__unlink', '/redactor/webpages/working-in-editor/unlink.png'],
        ['a.cke_button__specialchar', '/redactor/webpages/working-in-editor/specialchar.png'],
        ['a.cke_button__tooltip', '/redactor/webpages/working-in-editor/tooltip.png'],
        ['a.cke_button__link', '/redactor/webpages/working-in-editor/link.png'],
        ['a.cke_button__wjforms', '/redactor/webpages/working-in-editor/wjform.png'],
        ['a.cke_button__components', '/redactor/webpages/working-in-editor/components.png'],
        ['a.cke_button__htmlbox', '/redactor/apps/htmlbox/htmlbox_icon.png']
    ];

    I.say("DO screens of buttons in TOOLBAR");
    selectorsAndPaths.forEach(item => {
        const [selector, path] = item;
        Document.screenshotElement(`${toolbar} ${selector}`, path);
    });

    I.say("Add link screens");
        I.clickCss(`${toolbar} a.cke_button__link`);
        I.waitForVisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe"), 15);
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe") );
        I.waitForVisible("table.urlFormTable");
        Document.screenshotElement("div.elfinder-button.div-create", '/redactor/webpages/working-in-editor/elfinder_addFolder.png');
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/link_dialog.png');

        I.click(locate(".cke_dialog_tabs a.cke_dialog_tab:nth-child(5)"));
        Document.screenshot('/redactor/webpages/working-in-editor/link_dialog-file-archive.png');
        I.click(locate(".cke_dialog_tabs a.cke_dialog_tab:nth-child(1)"));

        //link to page doalog
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe") );
        I.clickCss("#nav-iwcm_doc_group_volume_");
        I.waitForElement("#nav-iwcm_doc_group_volume_L2dyb3VwOjE_E", 5); //Jet portal 4
        I.clickCss("#nav-iwcm_doc_group_volume_L2dyb3VwOjE_E");
        I.waitForElement("#nav-iwcm_doc_group_volume_L2dyb3VwOjI0", 5); //zo sveta financii
        I.wait(1);
        I.click(locate("#nav-iwcm_doc_group_volume_L2dyb3VwOjI0"), null, { position: { x: 20, y: 5 } });
        I.waitForElement("#iwcm_doc_group_volume_L2RvYzoxNg_E_E", 5); //konsolidacia napriec trhmi
        I.click("#iwcm_doc_group_volume_L2RvYzoxNg_E_E");
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/link_dialog-webpage.png');

        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjLinkIframe"), 5);

    I.say("Add image screens");
        I.clickCss(`${toolbar} a.cke_button__image`);
        I.waitForVisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement"), 15);
        I.switchTo( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement") );
        I.waitForVisible("table.urlFormTable");
        I.switchTo();
        Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog.png');

        I.say("Image - pixabay");
            I.click( locate( "div.cke_dialog_tabs" ).find( locate("a").withText(i18n.get("Photobank") ) ) );
            I.waitForVisible("#wjImagePixabayIframeElement");
            I.switchTo("#wjImagePixabayIframeElement");
            I.fillField("#search", "car");
            I.clickCss('button[type="submit"]');

            I.waitForElement('img[src*="https://cdn.pixabay.com/photo/"]', 10); //wait for images to load
            I.wait(2);

            I.switchTo();
            Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog-pixabay.png');
            I.switchTo("#wjImagePixabayIframeElement");

            I.clickCss('img[src*="https://cdn.pixabay.com/photo/"]');
            I.waitForVisible("#imageModal > div.modal-dialog");
            I.switchTo();
            Document.screenshotElement(locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog-pixabay-add.png');
            I.switchTo("#wjImagePixabayIframeElement");
            I.fillField(locate("#imageModal").find("#imageWidth"), 800);
            I.switchTo();
            Document.screenshotElement(locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog-pixabay-add2.png');

            I.switchTo("#wjImagePixabayIframeElement");
            I.clickCss('button.btn.btn-primary.saveImage');

            I.waitForInvisible("#imageModal", 10);
            I.switchTo();
            I.wait(1);
            Document.screenshotElement(locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), '/redactor/webpages/working-in-editor/image_dialog-pixabay-save.png');

        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible( locate(".cke_dialog.cke_browser_webkit.cke_ltr").find("iframe#wjImageIframeElement"), 5);

        I.say("Image - right click");
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        I.rightClick( locate("#WebJETEditorBody").find("img") );
        I.switchTo();
        I.waitForVisible(locate(settingDialogA).last(), 10);
        // Capture the viewport without scrolling or resizing the iframe and disturbing the context menu.
        Document.screenshot('/redactor/webpages/working-in-editor/image_right_click.png');

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
        I.clickCss(`${toolbar} a.cke_button__table`);
        const tablePanel = locate(".cke_panel:not(.cke_menu_panel)").last();
        I.waitForVisible(tablePanel, 10);
        Document.screenshotElement(tablePanel, '/redactor/webpages/working-in-editor/table_preview.png');
        I.switchTo(tablePanel.find("iframe.cke_panel_frame"));
        I.waitForElement('.cke_colormore', 10);
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
        I.click(`span.cke_menuitem > a.cke_menubutton[title=${i18n.get('Row')}]`);
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_row.png');

        I.say("Table - setting -> STLPEC");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").first());
        I.click(`span.cke_menuitem > a.cke_menubutton[title=${i18n.get('Column')}]`);
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_column.png');

        I.say("Table - setting -> BUNKA");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").first());
        I.click(`span.cke_menuitem > a.cke_menubutton[title=${i18n.get('Cell')}]`);
        I.switchTo();
        Document.screenshotElement(locate(settingDialogA).last() , '/redactor/webpages/working-in-editor/table_edit_cell.png');

        I.say("Table - setting -> BUNKA -> VLASTNOSTI");
        I.switchTo(locate(settingDialogA + " > iframe.cke_panel_frame").last());
        i18n.click("Cell Properties");
        I.switchTo();
        I.waitForVisible(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page").last(), 5);
        Document.screenshotElement(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page").last(), "/redactor/webpages/working-in-editor/table_edit_cell_edit.png");
        I.clickCss(".cke_dialog_ui_button_cancel");
        I.waitForInvisible(locate(".cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page"), 5);

    I.clickCss(`${toolbar} a.cke_button__specialchar`);
    I.waitForVisible(locate('.cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page').last(), 5);
    Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr.cke_single_page').last(),'/redactor/webpages/working-in-editor/specialchar_dialog.png');
    I.clickCss(".cke_dialog_ui_button_cancel");

    //I.clickCss(`${toolbar} a.cke_button__htmlbox`);
    //Document.screenshot('/redactor/apps/htmlbox/htmlbox_dialog.png');
    //I.clickCss(".cke_dialog_ui_button_cancel");
});

Scenario('editor - magicline', async ({ I, DTE, Document, Apps, Browser }) => {

    Document.setEditorMode("standard");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=150095");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.clickCss('#trEditor');
    if (Browser.isFirefox()) I.wait(2);
    I.clickCss('#trEditor');
    I.pressKey('ArrowDown');

    I.switchTo(".cke_wysiwyg_frame.cke_reset");

        I.executeScript(function() {
            document.body.dispatchEvent(new MouseEvent('mousemove', {
                bubbles: true,
                cancelable: true,
                view: window,
                clientX: 589,
                clientY: 248
            }));

            setTimeout(function() {
                document.getElementById("wjmagiclinePlus").dispatchEvent(new MouseEvent('mouseup', {
                    bubbles: true,
                    cancelable: true,
                    view: window
                }));
            }, 500);
        });

        I.wait(2);
        Document.screenshot('/redactor/webpages/working-in-editor/wjmagicline.png');

        I.pressKey("ArrowUp");

        I.executeScript(function() {
            document.elementFromPoint(507, 117).parentElement.dispatchEvent(new MouseEvent('mousemove', {
                bubbles: true,
                cancelable: true,
                view: window,
                clientX: 507,
                clientY: 117
            }));
        });

        I.wait(2);
        Document.screenshot('/redactor/webpages/working-in-editor/wjmagicline-append.png');

    I.switchTo();

    Document.resetPageBuilderMode();
});

Scenario('remove Pixabay image', ({ I, Document, i18n }) => {
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_1_L2ltYWdlcy90ZXN0LXN0YXZvdi9tYW51YWx0ZXN0cGFnZQ_E_E");

    I.waitForElement(`.elfinder-cwd-filename`);
    I.rightClick(`.elfinder-cwd-filename`);
    I.waitForVisible('.elfinder-contextmenu', 10);

    I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjeditswitch');
    I.switchToNextTab();
    I.waitForVisible("#galleryTable_modal");
    I.clickCss("#pills-dt-galleryTable-metadata-tab");

    Document.screenshotElement("#galleryTable_modal", '/redactor/webpages/working-in-editor/image_dialog-pixabay-edit.png');
    I.closeCurrentTab();

    I.waitForElement(`.elfinder-cwd-filename`);
    I.rightClick(`.elfinder-cwd-filename`);
    I.waitForVisible('.elfinder-contextmenu', 10);
    I.clickCss("span.elfinder-button-icon.elfinder-button-icon-rm");
    I.waitForElement(".elfinder-dialog-confirm");
    I.click( locate("button.ui-button > span.ui-button-text").withText(i18n.get("Delete")) );
    I.waitForInvisible(`.elfinder-cwd-filename`);
});

// Keep cleanup after the metadata screenshot; @current also includes it when running only the editor scenario.
Scenario('working-in-editor cleanup @current', () => {
    fs.rmSync(pixabayImagePath, { force: true });
});

Scenario('appstore', ({ I, DTE, Document, i18n }) => {

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
    I.fillField("input#search", i18n.get("map"));
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

Scenario('fontawesome', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=21");
    DTE.waitForEditor();
    I.wait(4);
    Document.screenshotElement("a[title='Insert Font Awesome']", "/frontend/webpages/fontawesome/editor-toolbar-icon.png");
    I.clickCss("a[title='Insert Font Awesome']");
    I.wait(3);
    Document.screenshot("/frontend/webpages/fontawesome/editor.png");
});

Scenario('editor-btn-dialog', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25");
    DTE.waitForEditor();
    I.wait(4);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.waitForVisible("#WebJETEditorBody .btn.btn-primary", 10);
    I.click(locate("#WebJETEditorBody .btn.btn-primary").first());

    Document.screenshot('/redactor/webpages/working-in-editor/link_dialog_button.png');

    I.switchTo();

    I.click(locate("div.cke_dialog_container a.cke_dialog_tab").withText("Rozšírené"));
    Document.screenshot('/redactor/webpages/working-in-editor/link_dialog_button_advanced.png');

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=153671");
    DTE.waitForEditor();
    I.switchTo(".cke_wysiwyg_frame.cke_reset");
    I.click(locate("button.btn").withText("normal button"));
    Document.screenshot('/redactor/webpages/working-in-editor/wjformbutton.png');
});

Scenario('webjet-toolbar', ({ I, Document }) => {
    I.amOnPage("/investicie/?language=" + I.getConfLng() + "&NO_WJTOOLBAR=false");
    Document.screenshot('/redactor/webpages/webjet-toolbar.png');
});

function makeDialogScreenshot(imagePath, I, Document, DTE, i18n, custom = false) {
    var elementText = "Etiam orci";

    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement(locate("div").withChild(locate("h3").withText(elementText)).find(locate(".fixedSize-160-160-5")), 10);
    I.click(locate("div").withChild(locate("h3").withText(elementText)).find(locate(".fixedSize-160-160-5")));

    I.waitForElement(locate("a.cke_dialog_tab").withText(i18n.get("Thumbnail")), 10);
    I.click(locate("a.cke_dialog_tab").withText(i18n.get("Thumbnail")));

    if (custom) I.executeScript(() => CKEDITOR.dialog.getCurrent().getContentElement('thumb', 'thumbAllowedSize')?.setValue('custom'));

    Document.screenshotElement( locate('.cke_dialog.cke_browser_webkit.cke_ltr').last(), imagePath);

    I.switchTo();
}

Scenario('thumb-servlet', ({ I, Document, DTE, i18n }) => {
    Document.setConfigValue('thumbServletAllowedSizeMode', 'learn');
    makeDialogScreenshot('/redactor/webpages/working-in-editor/image_dialog-thumb.png', I, Document, DTE, i18n, true);
    Document.setConfigValue('thumbServletAllowedSizeMode', 'strict');
    makeDialogScreenshot('/redactor/webpages/working-in-editor/image_dialog-thumb-strict.png', I, Document, DTE, i18n);
    Document.setConfigValue('thumbServletAllowedSizeMode', 'learn');
});
