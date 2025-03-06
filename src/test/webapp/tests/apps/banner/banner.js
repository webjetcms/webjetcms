Feature('apps.banner');

Before(({ I, login }) => {
    login('admin');
});

Scenario('banner zakladne testy @baseTest', async ({I, DataTables, DT, DTE}) => {
    I.amOnPage("/apps/banner/admin/");

    //delete FAILED autotest-group banners
    DT.filterContains("bannerGroup", "autotest-group");
    DT.filterSelect("editorFields.viewable", "Áno");

    I.dontSee("Exspirovaný dátum", "#bannerDataTable");

    const rows = await I.getTotalRows();
    I.say("Rows: "+rows);
    if (rows > 0) {
        I.clickCss(".buttons-select-all");
        I.clickCss(".buttons-remove");
        DTE.waitForEditor("bannerDataTable");
        DTE.save();
        DTE.waitForModalClose("bannerDataTable_modal");
    }

    await DataTables.baseTest({
        dataTable: 'bannerDataTable',
        perms: 'menuBanner',
        requiredFields: ['name'],

        createSteps: function(I, options) {
            //I.fillField({css: 'input[name=bannerLocation]'}, '/images/gallery/chrysanthemum.jpg');
            I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
            I.fillField("div.DTE_Field_Name_bannerLocation input", '/images/gallery/chrysanthemum.jpg');
            I.fillField("div.DTE_Field_Name_bannerRedirect input", "/apps/bannerovy-system/");
            I.click({css: "#pills-dt-bannerDataTable-main-tab"});
            //FF autocomplete fix
            DTE.appendField("bannerGroup", "autotest-group");
        },
        editSteps: function(I, options) {
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});

            I.say("Selecting page");
            I.clickCss("div.DTE_Field_Name_docIds button.btn-vue-jstree-add");
            I.waitForVisible('#jsTree');
            I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
            I.click(locate(".jstree-anchor").withText("Jet portal 4 - testovacia stranka"));
            I.seeInField("div.DTE_Field_Name_docIds input.form-control", "/Jet portal 4/Jet portal 4 - testovacia stranka");

            I.say("Selecting group");

            I.clickCss("div.DTE_Field_Name_groupIds button.btn-vue-jstree-add");
            I.waitForVisible('#jsTree');
            I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Aplikácie").find('.jstree-icon.jstree-ocl'));
            I.click(locate(".jstree-anchor").withText("Bannerový systém"));
            I.seeInField("div.DTE_Field_Name_groupIds input.form-control", "/Aplikácie/Bannerový systém");

            I.click({css: "#pills-dt-bannerDataTable-main-tab"});
        },
        beforeDeleteSteps: function(I, options) {
            //
            I.say("check zero statistics");
            I.see("0", "#bannerDataTable tr td:nth-child(7)");
            I.see("0", "#bannerDataTable tr td:nth-child(8)");

            //
            I.say("check doc/group mapping");
            I.click({css: "button.buttons-refresh"});
            DT.waitForLoader();
            I.click(locate("#bannerDataTable td.dt-row-edit div.datatable-column-width a").withText(options.testingData[0]));
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.seeInField("div.DTE_Field_Name_docIds input.form-control", "/Jet portal 4/Jet portal 4 - testovacia stranka");
            I.seeInField("div.DTE_Field_Name_groupIds input.form-control", "/Aplikácie/Bannerový systém");
            DTE.cancel();

            I.click({css: "button.buttons-refresh"});
            DT.waitForLoader();
            I.click(locate("#bannerDataTable td.dt-row-edit div.datatable-column-width a").withText(options.testingData[0]));
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.clickCss("div.DTE_Field_Name_docIds button.btn-vue-jstree-item-remove");
            I.dontSeeElement("div.DTE_Field_Name_docIds input.form-control");
            DTE.save();

            I.click({css: "button.buttons-refresh"});
            DT.waitForLoader();
            I.click(locate("#bannerDataTable td.dt-row-edit div.datatable-column-width a").withText(options.testingData[0]));
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.dontSeeElement("div.DTE_Field_Name_docIds input.form-control");
            I.seeInField("div.DTE_Field_Name_groupIds input.form-control", "/Aplikácie/Bannerový systém");
            DTE.cancel();


            //
            I.say("show banner and click on it");
            I.amOnPage("/apps/bannerovy-system/autotest-banner.html");
            I.clickCss("div.banner-image a");
            I.see("Obsahový banner");
            I.see("Sekundárny nadpis");

            //
            I.say("execute job to write statistics");
            I.amOnPage("/admin/v9/settings/cronjob/");
            DT.filterContains("task", "sk.iway.iwcm.stat.StatWriteBuffer");
            I.clickCss("td.dt-select-td");
            I.clickCss("button.button-execute-task");
            I.waitForElement("#toast-container-webjet");
            I.clickCss("div.toastr-buttons button.btn.btn-primary");
            DT.waitForLoader();
            I.see("Úloha sk.iway.iwcm.stat.StatWriteBuffer bola spustená");

            //wait for DB write
            I.wait(30);

            //
            I.say("reload page");
            I.amOnPage("/apps/banner/admin/");
            DT.filterContains("name", options.testingData[0]);

            I.see("1", "#bannerDataTable tr td:nth-child(7)");
            I.see("1", "#bannerDataTable tr td:nth-child(8)");
        }
    });
});

Scenario('kontrola hide/show content banner stlpcov', ({I, DTE}) => {
    I.amOnPage("/apps/banner/admin/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor('bannerDataTable');

    // Go to advance tab and check if content banner fields are hiden
    I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.dontSee("Odkaz na obrázok");
    I.dontSee("Sekundárny nadpis");
    I.dontSee("Spôsob otvorenia odkazu");

    // Go to main tab and select contentBanner option into bannerType select
    I.click({css: "#pills-dt-bannerDataTable-main-tab"});
    DTE.selectOption("bannerType", "Obsahový banner");

    // Go to advance tab and check if content banner fields are showed
    I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.see("Odkaz na obrázok");
    I.see("Sekundárny nadpis");
    I.see("Spôsob otvorenia odkazu");

    // Go to main tab and select other option than contentBanner into bannerType select
    I.click({css: "#pills-dt-bannerDataTable-main-tab"});
    DTE.selectOption("bannerType", "HTML kód");

     // Go to advance tab and check if content banner fields are hiden
     I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.dontSee("Odkaz na obrázok");
    I.dontSee("Sekundárny nadpis");
    I.dontSee("Spôsob otvorenia odkazu");
});

Scenario('zobrazenie banneru na web stranke', ({I}) => {
    I.amOnPage("/apps/bannerovy-system/");
    //html banner
    I.see("Toto je HTML banner.");

    //obrazkovy banner
    I.seeElement("img[src='/images/bannery/banner-iwayday.png']");

    //obsahovy banner
    I.see("PRIMÁRNY NADPIS");
    I.see("Sekundárny nadpis");
});

Scenario('prepinanie domen', ({I, Document}) => {
    I.amOnPage("/apps/banner/admin/");

    I.see("Obsahový banner");
    I.see("Obrázkový banner");
    I.see("HTML Banner");

    //domenovy selektor
    Document.switchDomain("test23.tau27.iway.sk");

    //toto pada I.dontSee("Obsahový banner");
    I.dontSee("Obrázkový banner");
    I.dontSee("HTML Banner");
});

Scenario('odhlasenie', ({I}) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});

Scenario('BUG filtrovanie aktivny', ({I, DT}) => {
    I.amOnPage("/apps/banner/admin/");

    I.dtWaitForLoader();

    I.see("Test banner vypnuty");
    I.see("Investičný vklad");
    I.see("Exspirovaný dátum");

    //
    I.say("Filter podla zobrazitelny");
    I.dtFilterSelect("editorFields.viewable", "Nie");
    I.see("Test banner vypnuty");
    I.dontSee("Investičný vklad");
    I.see("Exspirovaný dátum");

    I.dtFilterSelect("editorFields.viewable", "Áno");
    I.dontSee("Test banner vypnuty");
    I.see("Investičný vklad");
    I.dontSee("Exspirovaný dátum");

    //
    I.say("Filter podla active");
    I.amOnPage("/apps/banner/admin/");
    I.dtResetTable("bannerDataTable");
    DT.showColumn("Aktívny", "bannerDataTable");

    I.dtFilterSelect("active", "Nie");
    I.see("Test banner vypnuty");
    I.dontSee("Investičný vklad");
    I.dontSee("Exspirovaný dátum");

    I.dtFilterSelect("active", "Áno");
    I.dontSee("Test banner vypnuty");
    I.see("Investičný vklad");
    I.see("Exspirovaný dátum");
});

Scenario('reset', ({I}) => {
    //lebo sme pred tym v teste zmenili stlpce
    I.amOnPage("/apps/banner/admin/");
    I.dtResetTable("bannerDataTable");
});

Scenario('kampanovy banner', ({I, DT, DTE}) => {
    //banner sa smie zobrazit len pri zadanom URL parametri
    for (var i=0; i<3; i++) {
        //pre istotu viac krat reloadneme
        I.amOnPage("/apps/bannerovy-system/");
        I.see("Sekundárny nadpis");
        I.dontSee("Sekundárny nadpis-kampaň");
        I.wait(1);
    }
    I.amOnPage("/apps/bannerovy-system/?utm_campaign=kampan");
    I.see("Sekundárny nadpis-kampaň");

    //html banner must be shown also
    I.see("Toto je HTML banner.");

    //obrazkovy banner must be shown also
    I.seeElement("img[src='/images/bannery/banner-iwayday.png']");

    //overenie zobrazenia URL v admin sekcii
    I.amOnPage("/apps/banner/admin/?id=124");
    DTE.waitForEditor("bannerDataTable");
    I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.dontSee("://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign");
    I.fillField("#DTE_Field_campaignTitle", "test");
    I.pressKey('Tab');
    I.waitForElement("#campaignTitleUrlShowcase", 10);
    I.waitForText("://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign=test", 10, "#campaignTitleUrlShowcase");

    I.amOnPage("/apps/banner/admin/?id=3602");
    DT.waitForLoader();
    DTE.waitForEditor("bannerDataTable");
    I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.waitForElement("#campaignTitleUrlShowcase", 10);
    I.waitForText("://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign", 10, "#campaignTitleUrlShowcase");
});

Scenario('Kontrola prava cmp_banner_seeall', ({I, DT}) => {
    I.amOnPage("/apps/banner/admin/");
    DT.waitForLoader();
    I.see("Terminovaný vklad 2");
    I.see("Test banner vypnuty");
    I.see("Investičný vklad");

    I.amOnPage("/apps/banner/admin/?removePerm=cmp_banner_seeall");
    DT.waitForLoader();
    I.see("Terminovaný vklad 2");
    I.dontSee("Test banner vypnuty");
    I.dontSee("Investičný vklad");
    I.dontSee("Exspirovaný dátum");
});

Scenario('odhlasenie2', ({I}) => {
    //lebo sme pred tym v teste zmenili domenu
    I.logout();
});

Scenario('Full path pre adresare a stranky', async ({ I, DTE, Document }) => {
    I.amOnPage("/apps/banner/admin/");

    I.fillField("input.dt-filter-name", "WebJET CMS");
    I.pressKey('Enter', "input.dt-filter-name");
    DTE.waitForLoader();

    I.click("WebJET CMS");
    DTE.waitForEditor("bannerDataTable");
    I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});

    I.see("/Aplikácie/Bannerový systém");
    DTE.cancel();

    I.fillField("input.dt-filter-name", "WebJET DMS");
    I.pressKey('Enter', "input.dt-filter-name");
    DTE.waitForLoader();

    I.click("WebJET DMS");
    DTE.waitForEditor("bannerDataTable");
    I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});

    I.see("/Aplikácie/Bannerový systém/Podpriečinok banner/Child_banner_test_2");
});

Scenario('Adresarove obmedzenie', async ({ I, DTE, Document }) => {

    var cms = "Toto je banner pre WebJET CMS";
    var net = "Toto je banner pre WebJET NET";
    var cloud = "Toto je banner pre WebJET Cloud";
    var dms = "Toto je banner pre WebJET DMS";

    //parent_banner_test can see only CMS and NET, the rest cant be seen
    await testShowedBanners(I, "/apps/bannerovy-system/parent_banner_test.html", [cms, net]);

    //child_banner_test_1 can see only CMS, NET and CLOUD, the rest cant be seen
    await testShowedBanners(I, "/apps/bannerovy-system/podpriecinok-banner/child_banner_test_1.html", [cms, net, cloud]);

    //child_banner_test_2 can see CMS, NET, CLOUD and DMS
    await testShowedBanners(I, "/apps/bannerovy-system/podpriecinok-banner/child_banner_test_2.html", [cms, net, cloud, dms]);
});

//
async function testShowedBanners(I, url, arrayOfBanners) {
    if(arrayOfBanners.length > 0) {

        var foundValues = [];

        I.say((arrayOfBanners.length * 2));

        for(var i = 0; i < (arrayOfBanners.length * 2); i++) {
            I.amOnPage(url);

            let value = await I.grabTextFrom('#wjInline-docdata');

            var supported = false;
            //Check taht value is supported
            for(var j = 0; j < arrayOfBanners.length; j++) {
                if(value.includes(arrayOfBanners[j])) {
                    supported= true;
                    break;
                }
            }

            //If value isnt supported throw error
            if(!supported) I.assertEqual("Throw", "Error, banner not supported in this page");

            //Value is good
            foundValues[foundValues.length] = value;
        }

        //Now verify that all banners was showed
        for(var i = 0; i < arrayOfBanners.length; i++) {
            var found = false;
            for(var j = 0; j < foundValues.length; j++) {
                if(foundValues[j].includes(arrayOfBanners[i])) {
                    found = true;
                    break;
                }
            }

            //Banner wasnt found, but shoud be seen at least once
            if(!found) I.assertEqual("Throw", "Error, banner not found in this page");
        }
    }
}

Scenario('test docid/groupid in request bean', ({ I, DTE }) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    I.jstreeNavigate(["Aplikácie", "Bannerový systém"]);
    I.click("Parent_banner_test");
    DTE.waitForEditor();
    I.wait(5);
    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 10);

    //over zobrazenie v nahlade
    I.switchTo();
    I.wait(2);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(2);
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.wait(2);
    I.see("Toto je banner pre WebJET");
    I.switchTo();

});

Scenario('Test video in banner', ({I}) => {
    //
    I.say("Video banner MP4");
        I.amOnPage("/apps/bannerovy-system/klasicky_video_banner_mp4.html");
        I.seeElement("#wjInline-docdata > a > div.embed-responsive > video");

    //
    I.say("Video bannner YouTube");
        I.amOnPage("/apps/bannerovy-system/klasicky_video_banner_yt.html");
        //Check iframe video is there
        I.seeElement("#wjInline-docdata > a > div.embed-responsive > iframe.video")

    //
    I.say("Obsahovy video banner MP4");
        I.amOnPage("/apps/bannerovy-system/obsahovy_video_banner_mp4.html");
        I.seeElement("#wjInline-docdata > div.jumbotron > video");

     //
     I.say("Obsahovy video banner YouTube");
        I.amOnPage("/apps/bannerovy-system/obsahovy_video_banner_yt.html");
        I.seeElement("#wjInline-docdata > div.jumbotron > iframe.video");
});

Scenario('Device type specific banner', ({I, DTE}) => {
    let phoneId = "Phone";
    let tabletId = "Tablet";
    let pcId = "Pc";

   selectDevices(I, DTE, [pcId]);

    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=pc");
        I.seeElement("#wjInline-docdata > a > img");
    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=phone");
        I.dontSeeElement("#wjInline-docdata > a > img");
    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=tablet");
        I.dontSeeElement("#wjInline-docdata > a > img");

    selectDevices(I, DTE, [phoneId, tabletId]);

    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=pc");
        I.dontSeeElement("#wjInline-docdata > a > img");
    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=phone");
        I.seeElement("#wjInline-docdata > a > img");
    I.amOnPage("/test-stavov/device_specific_baner/?forceBrowserDetector=tablet");
        I.seeElement("#wjInline-docdata > a > img");
});

function selectDevices(I, DTE, selectDevices) {
    let allDevices = {
        "Phone" : '#DTE_Field_device_0',
        "Tablet" : '#DTE_Field_device_1',
        "Pc" : '#DTE_Field_device_2'};

    //Open page
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=73822");
    DTE.waitForEditor();

    //Open banner setting
    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 30);
    I.switchTo("iframe.cke_reset");
    I.wait(1);
    I.waitForElement("iframe.wj_component", 10);
    I.switchTo("iframe.wj_component");
    I.wait(1);
        I.forceClick("div.inlineComponentButtons > a:nth-child(1)");
    I.switchTo();

    I.waitForElement("table.cke_dialog");

    I.switchTo("iframe.cke_dialog_ui_iframe") //iframe
    I.switchTo("#editorComponent") //iframe

    I.waitForElement("#pills-dt-component-datatable-commonSettings-tab");
    I.clickCss("#pills-dt-component-datatable-commonSettings-tab");

    //Check wanted, unchecke rest
    Object.keys(allDevices).forEach((device) => {
        if(selectDevices.includes(device)) {
            I.checkOption(allDevices[device]);
        } else {
            I.uncheckOption(allDevices[device]);
        }
    });

    I.switchTo("");

    //OK the changes
    I.clickCss("a.cke_dialog_ui_button.cke_dialog_ui_button_ok");

    I.switchTo("");

    DTE.save();
}