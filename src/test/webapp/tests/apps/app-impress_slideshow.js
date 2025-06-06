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


Scenario('testovanie aplikácie - Posobiva prezentacii', async ({ I, Apps, Document }) => {
    Apps.insertApp('Pôsobivá prezentácia', '#components-app-impress_slideshow-title', null, false);
    I.switchTo('.cke_dialog_ui_iframe');
    I.switchTo('#editorComponent');

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok');

    const defaultParams = {
        nivoSliderHeight: "400",
        imageWidth: "400",
        imageHeight: "300"
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
        imageHeight: "200"
    };

    I.fillField("#DTE_Field_nivoSliderHeight", changedParams.nivoSliderHeight);
    I.fillField("#DTE_Field_imageWidth", changedParams.imageWidth);
    I.fillField("#DTE_Field_imageHeight", changedParams.imageHeight);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement("#jms-slideshow");
});