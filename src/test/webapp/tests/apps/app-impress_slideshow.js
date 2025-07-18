Feature('apps.app-impress_slideshow');

Before(({ I, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Pôsobivá prezentácia - test zobrazovania",({ I }) => {
    I.amOnPage("/apps/posobiva-prezentacia/");
    I.waitForElement(".jms-slideshow");
    I.seeElement(locate("h1").withText("Pôsobivá prezentácia"));
    I.seeElement('.jms-dots-current');

    I.say("overenie rotácie a vykreslenia");
    I.waitForElement('.step.jmstep1.active');
    within({ css: '.step.jmstep1.active' }, async () => {
        I.seeElement(locate('h3').withText('Koleso'));
        const imgUrl = await I.grabAttributeFrom('img', 'src');
        I.assertContain(imgUrl,'/thumb//images/gallery/test-vela-foto/dsc04082.jpeg?w=400&h=300&ip=5', 'The image was not loaded correctly. The source differs from what was expected.');
    });
    I.waitForElement('.step.jmstep2.active');
    within({ css: '.step.jmstep2.active' }, async () => {
        I.seeElement(locate('h3').withText('Svetlo'));
        const imgUrl = await I.grabAttributeFrom('img', 'src');
        I.assertContain(imgUrl,'/thumb//images/gallery/test-vela-foto/dsc04089.jpeg?w=400&h=300&ip=5', 'The image was not loaded correctly. The source differs from what was expected.');
    });
    I.waitForElement('.step.jmstep3.active');
    within({ css: '.step.jmstep3.active' }, async () => {
        I.seeElement(locate('h3').withText('Volant'));
        const imgUrl = await I.grabAttributeFrom('img', 'src');
        I.assertContain(imgUrl,'/thumb//images/gallery/test-vela-foto/dsc04126.jpeg?w=400&h=300&ip=5', 'The image was not loaded correctly. The source differs from what was expected.');
    });

    I.say("overenie prepínania pomocou jms-dots");
    for (let i = 3; i > 0; i--){
        I.clickCss(`#jms-slideshow > nav.jms-dots > span:nth-child(${i})`);
        I.seeElement(`.step.jmstep${i}.active`);
        I.seeElement(locate('h3').withText('Volant'));
        I.seeElement(`#jms-slideshow > nav.jms-dots > span:nth-child(${i})[class="jms-dots-current"]`);
    }

    I.say("overenie prepínania pomocou šípok");
    for (let i = 1; i < 5; i++){
        I.clickCss('#jms-slideshow > nav.jms-arrows > span.jms-arrows-next');
        I.seeElement(`.step.jmstep${i%3+1}.active`);
        I.seeElement(`#jms-slideshow > nav.jms-dots > span:nth-child(${i%3+1})[class="jms-dots-current"]`);
    }

    for (let i = 2; i < 5 ; i++){
        I.clickCss('#jms-slideshow > nav.jms-arrows > span.jms-arrows-prev');
        I.seeElement(`.step.jmstep${4-(i%3+1)}.active`);
        I.seeElement(`#jms-slideshow > nav.jms-dots > span:nth-child(${4-(i%3+1)})[class="jms-dots-current"]`);
    }

    I.say("Check stop rotating action after selecting slide");
    I.clickCss(`#jms-slideshow > nav.jms-dots > span:nth-child(1)`);
    I.waitForVisible(locate('h3').withText('Volant'));
    for(let i = 0;  i < 5; i++) {
        I.wait(1);
        I.seeElement(locate('h3').withText('Volant'));
    }
});


Scenario('testovanie aplikácie - Posobiva prezentacii', async ({ I, DT, DTE, Apps, Document }) => {
    Apps.insertApp('Pôsobivá prezentácia', '#components-app-impress_slideshow-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.say("Check tabs");
    I.seeElement("#pills-dt-component-datatable-style-tab");
    I.seeElement("#pills-dt-component-datatable-tabLink2-tab");
    I.seeElement("#pills-dt-component-datatable-commonSettings-tab");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        nivoSliderHeight: "400",
        imageWidth: "400",
        imageHeight: "300",
        editorData: "JTVCJTVE"
    };
    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement("#jms-slideshow");

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();
    I.clickCss("#pills-dt-component-datatable-style-tab");

    const changedParams = {
        nivoSliderHeight: "300",
        imageWidth: "300",
        imageHeight: "200",
        editorData: "JTVCJTdCJTIyaWQlMjI6MiwlMjJpbWFnZSUyMjolMjIvaW1hZ2VzL2dhbGxlcnkvdGVzdC12ZWxhLWZvdG8vZHNjMDQwODIuanBlZyUyMiwlMjJ0aXRsZSUyMjolMjIlM0NwJTNFVG90byUyMGplJTIwbmFkcGlzJTIwb2JyYXprYSUzQ2JyJTNFJTNDL3AlM0UlMjIsJTIyc3VidGl0bGUlMjI6JTIyJTNDcCUzRVRvdG8lMjBqZSUyMHBvZG5hZHBpcyUyMG9icmF6a2ElM0NiciUzRSUzQy9wJTNFJTIyLCUyMnJlZGlyZWN0VXJsJTIyOiUyMiUyMiwlMjJoZWFkaW5nQ29sb3IlMjI6JTIyI2VhMTAxMGZmJTIyLCUyMnN1YmhlYWRpbmdDb2xvciUyMjolMjIjMjdlYzJlZmYlMjIsJTIyYmFja2dyb3VuZENvbG9yJTIyOiUyMiUyMiwlMjJjdXN0b21TdHlsZUhlYWRpbmclMjI6JTIyJTIyLCUyMmN1c3RvbVN0eWxlU3ViSGVhZGluZyUyMjolMjIlMjIlN0QlNUQ="
    };

    I.fillField("#DTE_Field_nivoSliderHeight", changedParams.nivoSliderHeight);
    I.fillField("#DTE_Field_imageWidth", changedParams.imageWidth);
    I.fillField("#DTE_Field_imageHeight", changedParams.imageHeight);

    I.say("Test ITEMS inner table in tab");
        I.clickCss("#pills-dt-component-datatable-tabLink2-tab");
        I.waitForVisible("#DTE_Field_iframe", 5);
        I.switchTo("#DTE_Field_iframe");
        I.seeElement("#impressSlideshowDataTable_wrapper");

        I.say("Add item");
        I.clickCss("button.buttons-create");
        DTE.waitForEditor("impressSlideshowDataTable");

        // Item title and subtitle
        DTE.fillQuill("title", "Toto je nadpis obrazka");
        DTE.fillQuill("subtitle", "Toto je podnadpis obrazka");

        // colors
        I.fillField("#DTE_Field_headingColor", "#ea1010ff");
        I.fillField("#DTE_Field_subheadingColor", "#27ec2eff");

        // Select item aka image
        I.click( locate(".DTE_Field_Type_elfinder").find("button") );
        I.waitForVisible("#modalIframeIframeElement", 5);
        I.switchTo("#modalIframeIframeElement", 5);
        I.fillField("#file" , "/images/gallery/test-vela-foto/dsc04082.jpeg");

        // Back to iframe
        I.switchTo();
        I.switchTo('.cke_dialog_ui_iframe');
        I.wait(1);
        I.switchTo('#editorComponent');
        I.wait(1);
        I.switchTo("#DTE_Field_iframe");

        // Confirm image selection
        I.click( locate("#modalIframe").find("button.btn-primary") );

        // Save new item
        DTE.save('impressSlideshowDataTable');

        // Check item in DT
        DT.checkTableRow("impressSlideshowDataTable", 1, ["", "/images/gallery/test-vela-foto/dsc04082.jpeg", "Toto je nadpis obrazka", "Toto je podnadpis obrazka", "", "#ea1010ff", "#27ec2eff"]);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement("#jms-slideshow");

    within("#jms-slideshow", () => {
        I.seeElement("img[src='/thumb//images/gallery/test-vela-foto/dsc04082.jpeg?w=300&h=200&ip=5']");
        I.seeElement( locate("h3[style='color:#ea1010ff!important; ']").withChild( locate("p").withText("Toto je nadpis obrazka") ) );
        I.seeElement( locate("div[style='color:#27ec2eff!important; ']").withChild( locate("p").withText("Toto je podnadpis obrazka") ) );
    });
});