Feature('apps.content_block');

Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario("Content block - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/content-block/");
    I.waitForElement(locate("h2").withText(("Najlepší elektromobil")), 10);
    I.seeElement({ css: 'div.blueBox div.container img[src="/images/gallery/test-vela-foto/dsc04089.jpeg"]' });
});

Scenario('testovanie app - Content Block', async ({ I, DTE, Apps }) => {
    Apps.insertApp('Content Block', '#components-content-block-title');

    const defaultParams = {
        type: '1',
        title: '',
        image1: '',
        image2: '',
        color: '',
        classes: '',
        device: '',
        cacheMinutes: ''
    };

    await Apps.assertParams(defaultParams);

    Apps.openAppEditor();

    const changedParams = {
        type: '1',
        title: 'Najlepší elektromobil - chan.ge',
        image1: '/images/gallery/test-vela-foto/dsc04089.jpeg',
        image2: '/images/gallery/test-vela-foto/dsc04159.jpeg',
        color: '#ff0000ff',
        classes: '',
        device: '',
        cacheMinutes: ''
    };
    DTE.fillField('title', changedParams.title);
    I.fillField('#panel-body-dt-component-datatable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_image1.image1 > div.col-sm-7 > div.DTE_Field_InputControl > div > input', changedParams.image1);
    I.fillField('#panel-body-dt-component-datatable-basic > div.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_image2.image2 > div.col-sm-7 > div.DTE_Field_InputControl > div > input', changedParams.image2);
    DTE.fillField('color', changedParams.color);

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.see(changedParams.title);
});

Scenario('testovanie app - Content Block - close tabs', ({ I }) => {
    I.closeOtherTabs();
});