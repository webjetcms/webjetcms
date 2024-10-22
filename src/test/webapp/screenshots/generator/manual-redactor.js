Feature('manual-redactor');

Before(({ I, login }) => {
    login('admin');
});

//TODO: nahradit docs/frontend/setup/header-footer.png generovanym obrazkom

Scenario('web-pages-list', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    let confLng = I.getConfLng();

    I.say("domenovy selektor");
    I.click("div.js-domain-toggler div.bootstrap-select button");
    Document.screenshot("/redactor/webpages/domain-select.png", 1360, 220);
    I.click("div.js-domain-toggler div.bootstrap-select button");

    I.say("priecinky system/kos");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.click("#pills-system-tab");
    DT.waitForLoader();
    Document.screenshotElement("div.tree-col", "/redactor/webpages/system-folder.png", 1360, 300);

    I.say("listy naposledy upravene a cakajuce na schvalenie");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.click("#pills-waiting-tab");
    DT.waitForLoader();
    Document.screenshot("/_media/changelog/2021q1/2021-13-awaiting-approve.png", 1360, 500);

    I.say("zobrazenie stranok aj z podadresarov");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    I.resizeWindow(1650, 860);
    var container="#datatableInit_wrapper";

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    switch (confLng) {
        case 'sk':
            I.click("Nadradený priečinok");
            break;
        case 'en':
            I.click("Parent folder");
            break;
        case 'cs':
            I.click("Nadřazený adresář");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.click("button.btn.btn-primary.dt-close-modal");
    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.forceClick({ css: '#dtRecursiveSwitch' });
    I.moveCursorTo('#dtRecursiveSwitch');
    DT.waitForLoader();
    Document.screenshot("/redactor/webpages/recursive-list.png");
    I.wjSetDefaultWindowSize();

    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    Document.screenshot("/redactor/datatables/dt-colvis.png");
    switch (confLng) {
        case 'sk':
            I.click("Obnoviť");
            break;
        case 'en':
            I.click("Restore");
            break;
        case 'cs':
            I.click("Obnovit");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    I.waitForInvisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    I.say("zobrazenie poctu zaznamov");
    I.click(container+" button.buttons-settings");
    I.click(container+" button.buttons-page-length");
    I.waitForVisible("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu");

    Document.screenshot("/redactor/datatables/dt-pagelength.png");

    I.click("div.dt-button-collection div.dropdown-menu.dt-dropdown-menu div.dt-button-collection div.dropdown-menu.dt-dropdown-menu button.dt-close-modal")

    I.say("specialne ikony");
    I.click("button.buttons-select-all");
    Document.screenshotElement("button.buttons-history-preview", "/redactor/webpages/icon-preview.png");
    Document.screenshotElement("div.buttons-recursive", "/redactor/webpages/icon-recursive.png");

    I.say("specialne ikony");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=263");
    I.resizeWindow(1360, 740);
    DTE.waitForEditor();
    I.wait(5);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.click("#WebJETEditorBody img");
    I.wait(3);
    Document.screenshot("/redactor/webpages/editor-floating-tools.png");
    I.switchTo();
    I.wjSetDefaultWindowSize();

    I.say("ulozit pracovnu verziu");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    I.resizeWindow(1280, 550);
    DTE.waitForEditor();
    I.wait(5);
    I.click("#webpagesSaveCheckbox");
    I.moveCursorTo("div.DTE_Form_Buttons div.form-check label span");
    Document.screenshot("/redactor/webpages/save-work-version.png");
    I.wjSetDefaultWindowSize();

    I.say("stavove ikony aj s vyhladavanim");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    I.click({ css: "div.dataTables_scrollHeadInner div.dt-filter-editorFields\\.statusIcons button.btn-outline-secondary" });
    Document.screenshot("/redactor/webpages/status-icons.png");
});

Scenario('media', ({ I, DT, DTE, Document }) => {
    //media
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Jet portal 4", "Úvodná stránka"]);
    I.click("Úvodná stránka", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-media-tab");
    DT.waitForLoader();
    Document.screenshot("/redactor/webpages/media.png");

    //vsetky media
    I.amOnPage("/admin/v9/webpages/media/");
    DT.waitForLoader();
    Document.screenshot("/redactor/webpages/media-all.png", 1280, 500);

    //editor
    I.click("button.buttons-create");
    DTE.waitForEditor('mediaTable');
    Document.screenshotElement("div.DTE_Action_Create", "/redactor/webpages/media-all-editor.png");
    I.click("div.DTE_Header button.btn-close-editor");

    //bubble editacia
    I.click({css: "div.buttons-select-cel"});
    I.wait(1);
    I.click("#mediaTable tbody tr td:nth-child(4)");
    Document.screenshot("/_media/changelog/2021q2/2021-17-media-bubble.png", 1280, 400);

    //media skupiny
    I.amOnPage("/admin/v9/webpages/media-groups/");
    DT.waitForLoader();
    I.click("English a Newsletter skupina");
    DTE.waitForEditor("mediaGroupsTable");
    Document.screenshot("/redactor/webpages/media-groups.png", 1280, 490);
});

Scenario('historia', ({ I, DT, DTE, Document }) => {
    //media
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=22956");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-history-tab");
    DT.waitForLoader();
    //zmen usporiadanie od najstarsieho, tam mame fake data
    //I.click("#datatableFieldDTE_Field_editorFields-history_wrapper th.dt-format-date-time");
    //DT.waitForLoader();

    I.waitForInvisible("#datatableFieldDTE_Field_editorFields-history_processing", 120);

    Document.screenshotElement(null, "/redactor/webpages/history.png");

    I.click("#datatableFieldDTE_Field_editorFields-history tr:nth-child(1) td.dt-select-td");

    Document.screenshotElement("#panel-body-dt-datatableInit-history button.buttons-history-edit", "/redactor/webpages/history-btn-edit.png");
    Document.screenshotElement("#panel-body-dt-datatableInit-history button.buttons-history-preview", "/redactor/webpages/history-btn-preview.png");
    Document.screenshotElement("#panel-body-dt-datatableInit-history button.buttons-history-compare", "/redactor/webpages/history-btn-compare.png");
    Document.screenshotElement("#panel-body-dt-datatableInit-history button.buttons-history-remove", "/redactor/webpages/history-btn-remove.png");

});

Scenario('konfiguracia', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/settings/configuration/");
    //aby sa schovala notifikacia o zozname zmenenych
    I.wait(10);

    const historyName = "aaatest";
    const datatableName = "configurationDatatable";
    I.fillField("input.dt-filter-value", historyName);
    I.pressKey('Enter', "input.dt-filter-name");

    I.click(historyName);
    DTE.waitForEditor(datatableName);

    I.click("#pills-dt-configurationDatatable-advanced-tab");

    Document.screenshot("/_media/changelog/2021q1/2021-13-configuration-history.png", 1280, 600);
});

Scenario('perex-groups', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/perex/");

    Document.screenshot("/redactor/webpages/perex-groups.png", 1280, 500);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Jet portal 4", "Kontakt"]);
    I.click("Kontakt", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-perex-tab");
    I.wait(1);
    I.scrollTo("#datatableInit_modal .DTE_Field_Name_perexGroups", 5, 5);
    Document.screenshot("/redactor/webpages/webpage-perex-groups.png");
});

Scenario('custom-fields', async({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov", "Voliteľné polia"]);
    I.click("Voliteľné polia", "#datatableInit_wrapper");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-fields-tab");
    DT.waitForLoader();
    Document.screenshot("/frontend/webpages/customfields/webpages.png");

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-bottom", "175px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldD').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldD').css("padding-bottom", "90px");

        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldE').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldE').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldF').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldF').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldG').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldG').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldH').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldH').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldI').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldI').css("padding-bottom", "10px");

        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldJ').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldJ').css("padding-bottom", "205px");

        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldK').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldK').css("padding-bottom", "10px");

        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldL').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldL').css("padding-bottom", "10px");

        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldM').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldM').css("padding-bottom", "10px");

    });
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldA", "/frontend/webpages/customfields/webpages-text.png");

    I.click("div.DTE_Action_Edit div.DTE_Field_Name_fieldB button.dropdown-toggle")
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldB", "/frontend/webpages/customfields/webpages-select.png");
    I.pressKey('Escape');

    I.fillField("div.DTE_Action_Edit div.DTE_Field_Name_fieldD input", "auto");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldD", "/frontend/webpages/customfields/webpages-autocomplete.png");
    I.pressKey('Escape');

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldE");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldE", "/frontend/webpages/customfields/webpages-image.png");

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldF");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldF", "/frontend/webpages/customfields/webpages-link.png");

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldG");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldG", "/frontend/webpages/customfields/webpages-docsin.png");

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldH");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldH", "/frontend/webpages/customfields/webpages-enumeration.png");

    I.resizeWindow(1280, 400);
    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldI");
    I.fillField("div.DTE_Action_Edit div.DTE_Field_Name_fieldI input", "123456");
    I.moveCursorTo('#toast-container');
    Document.screenshot("/frontend/webpages/customfields/webpages-length.png");
    I.wjSetDefaultWindowSize();

    I.click("div.DTE_Action_Edit div.DTE_Field_Name_fieldJ button.dropdown-toggle")
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldJ", "/frontend/webpages/customfields/webpages-select-multi.png");
    I.pressKey('Escape');

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldK");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldK", "/frontend/webpages/customfields/webpages-dir.png");

    I.scrollTo("div.DTE_Action_Edit div.DTE_Field_Name_fieldM");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldM", "/frontend/webpages/customfields/webpages-uuid.png");

    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.waitForLoader();
    I.click("button.buttons-settings");
    I.click("button.buttons-page-length");
    I.click("50", "div.dropdown-menu.dt-dropdown-menu");
    I.click("div.dropdown-menu.dt-dropdown-menu button.dt-close-modal");
    DT.filter("key", "temp-3");
    Document.screenshot("/frontend/webpages/customfields/translations.png");


    I.amOnPage("/admin/v9/settings/translation-keys/");
    DT.filter("key", "button.cancel");
    I.click("button.cancel");
    DTE.waitForEditor();

    I.executeScript(function() {
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldA').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_originalValueA').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_originalValueA').css("padding-bottom", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-top", "10px");
        $('div.DTE_Action_Edit div.DTE_Field_Name_fieldB').css("padding-bottom", "175px");
    });
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_fieldA", "/frontend/webpages/customfields/webpages-textarea.png");
    Document.screenshotElement("div.DTE_Action_Edit div.DTE_Field_Name_originalValueA", "/frontend/webpages/customfields/webpages-label.png");

});

Scenario('datatable-duplicate', ({ I, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Test stavov"]);
    I.forceClick("table.datatableInit tbody tr:nth-child(2) td.dt-select-td");
    I.forceClick("table.datatableInit tbody tr:nth-child(3) td.dt-select-td")
    I.moveCursorTo("#datatableInit_wrapper button.btn-duplicate");
    Document.screenshot("/_media/changelog/2021q2/2021-24-duplicate-button.png", 1280, 500);
});

Scenario('apps-qa', ({ I, DT, DTE, Document }) => {

    switch (I.getConfLng()) {
        case "sk":
            I.amOnPage("/apps/otazky-odpovede/");
            Document.screenshotElement("article div.container", "/redactor/apps/qa/webform.png", 1000, 1000);
            I.amOnPage("/apps/qa/admin/");
            Document.screenshot("/redactor/apps/qa/admin.png");
            DT.filter("question", "Koľko nôh ma pavúk ?");
            I.click("Koľko nôh ma pavúk ?");
            DTE.waitForEditor("qaDataTable");
            I.click("#pills-dt-qaDataTable-answer-tab");
            Document.screenshot("/redactor/apps/qa/admin-edit.png");
            break;
        
        case "en":
            I.amOnPage("/apps/otazky-odpovede/questions-answers.html?language=en");
            Document.screenshotElement("article div.container", "/redactor/apps/qa/webform.png", 1000, 1000);
            I.amOnPage("/apps/qa/admin/");
            Document.screenshot("/redactor/apps/qa/admin.png");
            DT.filter("question", "How many legs does a spider have ?");
            I.click("How many legs does a spider have ?");
            DTE.waitForEditor("qaDataTable");
            I.click("#pills-dt-qaDataTable-answer-tab");
            Document.screenshot("/redactor/apps/qa/admin-edit.png");
            break;
    
        case "cs":
            I.amOnPage("/apps/otazky-odpovedi/otazky-odpovedi-2.html");
            Document.screenshotElement("article div.container", "/redactor/apps/qa/webform.png", 1000, 1000);
            I.amOnPage("/apps/qa/admin/");
            Document.screenshot("/redactor/apps/qa/admin.png");
            DT.filter("question", "Kolik nohou má pavouk");
            I.click("Kolik nohou má pavouk ?");
            DTE.waitForEditor("qaDataTable");
            I.click("#pills-dt-qaDataTable-answer-tab");
            Document.screenshot("/redactor/apps/qa/admin-edit.png");
            break;
    
        default:
            throw new Error("Unknown language: " + I.getConfLng());
    }    
});

Scenario('logon', ({ I, Document }) => {
    I.amOnPage('/logoff.do?forward=/admin/');

    let confLng = I.getConfLng();
    //Select language if not default
    if ("sk" !== confLng) {
        switch (confLng) {
            case 'en':
                I.selectOption("language", "English");
                break;
            case 'cs':
                I.selectOption("language", "Česky");
                break;
            default:
                throw new Error(`Unsupported language code: ${confLng}`);
        }
    }

    I.fillField("#password", "12345");
    Document.screenshot("/redactor/admin/logon.png", 1080, 685);

    I.fillField("#username", "testerslabeheslo");
    I.fillField("#password", "tentousermavelmislabeheslo");
    I.click("#login-submit");
    I.wait(2);
    Document.screenshot("/redactor/admin/logon-weak-password.png", 1080, 685);
});

Scenario('layout-menu', ({ I, Document }) => {
    I.amOnPage('/admin/v9/');

    Document.screenshot("/redactor/admin/welcome.png", 1360, 685);
    Document.screenshotElement("div.ly-header", "/redactor/admin/header.png");

    Document.screenshotElement("a.js-logout-toggler", "/redactor/admin/icon-logoff.png");

    I.moveCursorTo("a.js-logout-toggler");
    Document.screenshotElement("div.ly-header", "/redactor/admin/header-logoff.png");

    Document.screenshotElement("div.md-large-menu", "/redactor/admin/menu-main-sections.png");
    Document.screenshotElement("div.menu-wrapper", "/redactor/admin/menu-items.png");

    I.resizeWindow(1199, 685);
    Document.screenshotElement(".js-sidebar-toggler", "/redactor/admin/icon-hamburger.png");

    Document.screenshot("/redactor/admin/welcome-tablet.png");

    I.click(".js-sidebar-toggler");
    Document.screenshot("/redactor/admin/welcome-tablet-showmenu.png");
    Document.screenshotElement(".js-sidebar-toggler", "/redactor/admin/icon-hamburger-show.png");

    I.wjSetDefaultWindowSize();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=12");
    I.resizeWindow(768, 1024);

    I.wait(2); //Need to wait for included app thumbnails to load
    Document.screenshot("/redactor/admin/editor-tablet.png");

    I.click("#pills-dt-datatableInit-basic-tab");
    I.resizeWindow(390, 844);
    Document.screenshot("/redactor/admin/editor-phone.png");

    I.wjSetDefaultWindowSize();
});

Scenario('editor-virtualPath', ({ I, DTE, Document }) => {

    //podla title
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=25861");
    I.wait(5);
    I.click("#pills-dt-datatableInit-basic-tab");
    I.moveCursorTo("div.DTE_Field_Name_generateUrlFromTitle button.btn-tooltip");

    Document.screenshot("/redactor/webpages/virtual-path-title.png")
    DTE.cancel();

    //podla urlDirName
    I.click("Podľa urlDirName");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-basic-tab");
    I.moveCursorTo("div.DTE_Field_Name_urlInheritGroup button.btn-tooltip");

    Document.screenshot("/redactor/webpages/virtual-path-inherit.png")

});

Scenario('pagebuilder', ({ I, DTE, Document }) => {

    //POZOR: tento scnreenshot neviem automatizovat, je tam schvalne cakanie 5 sekund
    //je treba kurzorom focusnut text, aby sa zobrazili PB zvyraznenia pre screenshot

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");

    DTE.waitForEditor();
    I.wait(10);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.say("Klikni kurzorom do textu a drz focus");
    I.say("Klikni kurzorom do textu a drz focus");
    I.say("Klikni kurzorom do textu a drz focus");
    I.say("Klikni kurzorom do textu a drz focus");
    I.say("Klikni kurzorom do textu a drz focus");
    I.wait(5);

    //I.moveCursorTo("p.text-center:nth-child(2)");

    Document.screenshot("/redactor/webpages/pagebuilder.png");

    I.switchTo();
    Document.screenshotElement("#trEditor > #DTE_Field_data-editorTypeSelector", "/redactor/webpages/pagebuilder-switcher.png");
});

Scenario('welcome', ({ I, Document }) => {
    I.amOnPage("/admin/v9/");

    Document.screenshotElement("div.overview-logged.feedback", "/redactor/admin/feedback.png", 1360, 900);
    I.clickCss(".ti.ti-writing");
    I.waitForElement("#feedback_modal");
    Document.screenshot("/redactor/admin/feedback-modal.png");

    I.amOnPage("/admin/v9/");
    I.executeScript(function() {
        $('div.overview-logged.bookmark').css("padding-top", "40px");
    });
    I.moveCursorTo("div.overview-logged.bookmark .overview-logged__head__icon");
    Document.screenshotElement("div.overview-logged.bookmark", "/redactor/admin/bookmarks.png");

    I.amOnPage("/admin/v9/");
    I.click(".ti.ti-plus");
    I.waitForElement("#bookmark_modal");
    I.wait(2);
    Document.screenshot("/redactor/admin/bookmarks-modal.png", 1280, 580);

});

Scenario('webpages-temp-edit-btn', ({ I, DTE, Document }) => {
    //screenshot ikon editacie Hlavicky/Paticky vo web stranke
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=7611");
    DTE.waitForEditor();
    I.click("#pills-dt-datatableInit-template-tab");
    DTE.selectOption("headerDocId", "Hlavičky/Default header");
    I.wait(3);
    Document.screenshot("/redactor/webpages/editor-templates-tab.png");
    I.wait(3);
    Document.screenshotElement("div.DTE_Field_Name_headerDocId", "/redactor/webpages/header-doc-edit.png");
});

Scenario('jstree-settings', ({ I, DTE, Document }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.click("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal");
    I.checkOption("#jstree-settings-showid");
    I.checkOption("#jstree-settings-showorder");
    I.checkOption("#jstree-settings-showpages");
    I.click("#jstree-settings-submit");

    I.jstreeClick("Test stavov");
    I.jstreeClick("Zobrazený v menu");

    I.click("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal");

    Document.screenshot("/redactor/webpages/jstree-settings.png");

    DTE.cancel();


    I.click("button.buttons-jstree-settings");
    I.waitForElement("#jstreeSettingsModal");
    I.uncheckOption("#jstree-settings-showid");
    I.uncheckOption("#jstree-settings-showorder");
    I.uncheckOption("#jstree-settings-showpages");
});

Scenario('formsimple', ({ I, DTE, Document }) => {
    let confLng = I.getConfLng();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=26180");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');

    I.click("iframe.wj_component");

    Document.screenshot("/redactor/apps/formsimple/editor-dialog-basic.png");

    I.switchTo();
    I.wait(2);
    I.switchTo(".cke_dialog_ui_iframe");
    I.wait(2);
    I.switchTo("#editorComponent");
    I.wait(2);
    I.click("#tabLink2");
    Document.screenshot("/redactor/apps/formsimple/editor-dialog-advanced.png");

    I.click("#tabLink3");
    Document.screenshot("/redactor/apps/formsimple/editor-dialog-items.png");

    I.switchTo();

    //aby na screenshote nebolo meno usera

    I.amOnPage("/logoff.do?forward=/admin/");
    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/formular-lahko/");
            break;
        case 'en':
            I.amOnPage("/apps/formular-lahko/?language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/formular-lahko/?language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    I.wait(5);
    Document.screenshot("/redactor/apps/formsimple/formsimple.png");

    I.say("skupiny poli");
    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/formular-lahko/zaskrtavacie-vyberove-polia.html");
            break;
        case 'en':
            I.amOnPage("/apps/formular-lahko/check-radio-fields.html?language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/formular-lahko/check-radio-fields.html?language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    I.wait(5);
    Document.screenshotElement("article.ly-content div.container", "/redactor/apps/formsimple/formsimple-radiogroup.png");

    I.say("riadkove zobrazenie");
    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/formular-lahko/riadkove-zobrazenie.html");
            break;
        case 'en':
            I.amOnPage("/apps/formular-lahko/line-view.html");
            break;
        case 'cs':
            I.amOnPage("/apps/formular-lahko/line-view.html?language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    I.wait(5);
    Document.screenshotElement("article.ly-content div.container", "/redactor/apps/formsimple/formsimple-rowview.png");

    I.say("wysiwyg version");
    switch (confLng) {
        case 'sk':
            I.amOnPage("/apps/formular-lahko/formular-lahko-wysiwyg.html?NO_WJTOOLBAR=true");
            break;
        case 'en':
            I.amOnPage("/apps/formular-lahko/formular-lahko-wysiwyg.html?NO_WJTOOLBAR=true&language=en");
            break;
        case 'cs':
            I.amOnPage("/apps/formular-lahko/formular-lahko-wysiwyg.html?NO_WJTOOLBAR=true&language=cs");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }
    I.wait(5);
    I.pressKey("ArrowDown");
    I.pressKey("ArrowDown");

    switch (confLng) {
        case 'sk':
            I.fillField("#meno-a-priezvisko", "Form Tester");
            break;
        case 'en':
            I.fillField("#name-and-surname", "Form Tester");
            break;
        case 'cs':
            I.fillField("#jmeno-a-prijmeni", "Form Tester");
            break;
        default:
            throw new Error(`Unsupported language code: ${confLng}`);
    }

    DTE.fillCleditor("form.formsimple > div.form-group", "Lorem ipsum dolor sit amet!");
    I.pressKey(['CommandOrControl', 'A']);
    I.wait(0.3);
    I.pressKey(['CommandOrControl', 'B'])
    I.wait(0.3);
    I.pressKey('ArrowRight');
    I.wait(0.3);
    I.pressKey('Enter');
    I.pressKey(['CommandOrControl', 'B'])
    I.type("Quis autem vel eum iure reprehenderit?", 50);
    I.pressKey('Enter');
    I.pressKey('Enter');
    I.type("Lorem ipsum", 50);
    I.click("div.cleditorButton[title=Numbering]");

    Document.screenshot("/redactor/apps/formsimple/formsimple-wysiwyg.png");
});

Scenario('template list', ({ I, DTE, Document }) => {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    DTE.waitForLoader("tempsTable");
    I.clickCss("#pills-dt-datatableInit-templatesTab-tab");
    I.click( locate("div.DTE_Field_Name_footerDocId").find("button.dropdown-toggle") );
    Document.screenshotElement(locate("#datatableInit_modal").find("div.DTE_Action_Edit") ,"/frontend/setup/header-footer.png");
});