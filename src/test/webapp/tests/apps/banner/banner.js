Feature('apps.banner');

Before(({ I, login }) => {
    login('admin');
});

Scenario('banner zakladne testy', async ({I, DataTables, DT, DTE}) => {
    I.amOnPage("/apps/banner/admin/");
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
            I.fillField("#DTE_Field_bannerGroup", "autotest-group");
        },
        editSteps: function(I, options) {
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});

            I.say("Selecting page");
            I.click("div.DTE_Field_Name_docIds button.btn-vue-jstree-add");
            I.waitForVisible('#jsTree');
            I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
            I.click(locate(".jstree-anchor").withText("Jet portal 4 - testovacia stranka"));
            I.seeInField("div.DTE_Field_Name_docIds input.form-control", "/Jet portal 4/Jet portal 4 - testovacia stranka");

            I.say("Selecting group");

            I.click("div.DTE_Field_Name_groupIds button.btn-vue-jstree-add");
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
            I.click(options.testingData[0]);
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.seeInField("div.DTE_Field_Name_docIds input.form-control", "/Jet portal 4/Jet portal 4 - testovacia stranka");
            I.seeInField("div.DTE_Field_Name_groupIds input.form-control", "/Aplikácie/Bannerový systém");
            DTE.cancel();

            I.click({css: "button.buttons-refresh"});
            DT.waitForLoader();
            I.click(options.testingData[0]);
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.click("div.DTE_Field_Name_docIds button.btn-vue-jstree-item-remove");
            I.dontSeeElement("div.DTE_Field_Name_docIds input.form-control");
            DTE.save();

            I.click({css: "button.buttons-refresh"});
            DT.waitForLoader();
            I.click(options.testingData[0]);
            DTE.waitForEditor("bannerDataTable");
            I.click({css: "#pills-dt-bannerDataTable-restrictions-tab"});
            I.dontSeeElement("div.DTE_Field_Name_docIds input.form-control");
            I.seeInField("div.DTE_Field_Name_groupIds input.form-control", "/Aplikácie/Bannerový systém");
            DTE.cancel();


            //
            I.say("show banner and click on it");
            I.amOnPage("/apps/bannerovy-system/autotest-banner.html");
            I.click("div.banner-image a");
            I.see("Obsahový banner");
            I.see("Sekundárny nadpis");

            //
            I.say("execute job to write statistics");
            I.amOnPage("/admin/v9/settings/cronjob/");
            DT.filter("task", "sk.iway.iwcm.stat.StatWriteBuffer");
            I.click("td.dt-select-td");
            I.click("button.button-execute-task");
            I.waitForElement("#toast-container-webjet");
            I.click("div.toastr-buttons button.btn.btn-primary");
            DT.waitForLoader();
            I.see("Úloha sk.iway.iwcm.stat.StatWriteBuffer bola spustená!");

            //wait for DB write
            I.wait(30);

            //
            I.say("reload page");
            I.amOnPage("/apps/banner/admin/");
            DT.filter("name", options.testingData[0]);

            I.see("1", "#bannerDataTable tr td:nth-child(7)");
            I.see("1", "#bannerDataTable tr td:nth-child(8)");
        }
    });
});

Scenario('kontrola hide/show content banner stlpcov', ({I, DTE}) => {
    I.amOnPage("/apps/banner/admin/");

    I.click("button.buttons-create");
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

Scenario('prepinanie domen', ({I}) => {
    I.amOnPage("/apps/banner/admin/");

    I.see("Obsahový banner");
    I.see("Obrázkový banner");
    I.see("HTML Banner");

    //domenovy selektor
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    //I.click(locate('.dropdown-item').withText("test23.tau27.iway.sk"));
    I.click({css: "#bs-select-1-1"});
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");
    I.wait(1);

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
    I.see("Expirovaný dátum");

    //
    I.say("Filter podla zobrazitelny");
    I.dtFilterSelect("editorFields.viewable", "Nie");
    I.see("Test banner vypnuty");
    I.dontSee("Investičný vklad");
    I.see("Expirovaný dátum");

    I.dtFilterSelect("editorFields.viewable", "Áno");
    I.dontSee("Test banner vypnuty");
    I.see("Investičný vklad");
    I.dontSee("Expirovaný dátum");

    //
    I.say("Filter podla active");
    I.amOnPage("/apps/banner/admin/");
    I.dtResetTable("bannerDataTable");
    DT.showColumn("Aktívny", "bannerDataTable");

    I.dtFilterSelect("active", "Nie");
    I.see("Test banner vypnuty");
    I.dontSee("Investičný vklad");
    I.dontSee("Expirovaný dátum");

    I.dtFilterSelect("active", "Áno");
    I.dontSee("Test banner vypnuty");
    I.see("Investičný vklad");
    I.see("Expirovaný dátum");
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
    I.dontSee("http://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign");
    I.fillField("#DTE_Field_campaignTitle", "test");
    I.see("http://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign=test");

    I.amOnPage("/apps/banner/admin/?id=3602");
    DT.waitForLoader();
    DTE.waitForEditor("bannerDataTable");
    I.click({css: "#pills-dt-bannerDataTable-advanced-tab"});
    I.see("http://demotest.webjetcms.sk/apps/bannerovy-system/?utm_campaign");
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
    I.dontSee("Expirovaný dátum");
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