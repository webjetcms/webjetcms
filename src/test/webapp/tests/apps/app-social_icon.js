Feature('apps.app-social_icon');

Before(({ I, login }) => {
    login('admin');
    I.closeOtherTabs();
    I.amOnPage("/apps/odkazy-socialne-siete/");
    I.switchTo();
    I.waitForElement(locate("h1").withText("Odkazy na sociálne siete"), 10);
});

Scenario("Odkazy sociálne siete - facebook", async ({ I,Document }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 10);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Facebook");
    I.clickCss('img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Prihlásiť sa", 11);
    await I.clickIfVisible('[aria-label="Povoliť všetky cookies"]');
    I.wait(1);
    await I.clickIfVisible('[aria-label="Zavrieť"]');
    I.waitForText('WebJET CMS', 12);
    I.closeOtherTabs();
});

Scenario("Odkazy sociálne siete - youtube", async ({ I, Document }) => {
    I.waitForElement({ css: 'img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]'}, 10);
    I.seeElement({ css: 'img[src="/components/app-social_icon/facebook.png"][title="Facebook"][alt="Facebook"]'});

    I.say("Overenie odkazu na Youtube");
    I.clickCss('img[src="/components/app-social_icon/youtube.png"][title="Youtube"][alt="Youtube"]');
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForText("Prihlásiť sa", 12);
    await I.clickIfVisible('[aria-label="Prijať všetko"]');
    I.waitForElement(locate('h1 > span').withText("WebJET od InterWay"), 30);
    I.closeOtherTabs();
});

Scenario("cleanup", ({ I }) => {
    I.closeOtherTabs();
});

Scenario('testovanie aplikácie - Odkazy na sociálne siete', async ({ I, Apps, Document, DTE }) => {
    Apps.insertApp('Odkazy na sociálne siete', '#components-app-social_icon-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    DTE.fillField("facebook_url", "https://www.facebook.com/interway.sk");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        facebook_url: "aHR0cHM6Ly93d3cuZmFjZWJvb2suY29tL2ludGVyd2F5LnNr",
        instagram_url: "",
        linkedin_url: "",
        youtube_url: "",
        twitter_url: "",
        mail_url: "",
        blog_url: "",
        flickr_url: "",
        rss_url: "",
        style: "01",
        socialIconAlign: "left"
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.seeElement('p[style*="text-align: left;"] a[href="https://www.facebook.com/interway.sk"]');
    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor(undefined, undefined, true);

    DTE.fillField("youtube_url", "https://www.youtube.com/@webjet-od-interway");
    I.clickCss("#pills-dt-component-datatable-style-tab");
    I.clickCss("label[for=DTE_Field_style_1]");
    I.clickCss("label[for=DTE_Field_socialIconAlign_1]");

    const changedParams = {
        facebook_url: "aHR0cHM6Ly93d3cuZmFjZWJvb2suY29tL2ludGVyd2F5LnNr",
        instagram_url: "",
        linkedin_url: "",
        youtube_url: "aHR0cHM6Ly93d3cueW91dHViZS5jb20vQHdlYmpldC1vZC1pbnRlcndheQ==",
        twitter_url: "",
        mail_url: "",
        blog_url: "",
        flickr_url: "",
        rss_url: "",
        style: "bootstrap01",
        socialIconAlign: "center"
    };

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement(".iconFacebook", 10);
});
