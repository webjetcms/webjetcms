Feature('webpages.webp');

var folderName;

Before(({ I, login }) => {
    login('admin');

    if (typeof folderName == "undefined") {
        var randomNumber = I.getRandomText();
        folderName = 'autotest-webp-' + randomNumber;
    }
});

Scenario('webp upload and thumbnail test', async ({ I, Document }) => {
    //navigate to files section
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2E_E'); //images/produktova-stranka
    I.waitForElement('.elfinder-navbar-wrapper', 10);
    I.waitForElement("#iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2EvbW9uZXkuanBn", 10); //money.jpg file
    I.click("#iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2EvbW9uZXkuanBn"); //focus

    I.say('Creating temporary folder');
    I.click('.elfinder-button-icon.elfinder-button-icon-mkdir');
    I.fillField({ css: '.elfinder-cwd-filename[title="Nový priečinok"] textarea' }, folderName);
    I.pressKey('Enter');
    I.waitForVisible('.elfinder-toast', 10);
    I.waitForText(folderName, 10, '.elfinder-cwd-wrapper');

    I.say('Opening created folder');
    I.doubleClick(locate('.elfinder-cwd-filename').withText(folderName));
    I.waitForElement(locate('.elfinder-navbar-dir.ui-state-active').withText(folderName), 10);

    I.say('Uploading webp image');
    I.click('.elfinder-button-icon.elfinder-button-icon-upload');
    I.attachFile('input[type=file]', 'tests/webpages/tree.webp');
    await I.clickIfVisible('.elfinder-confirm-accept');
    I.waitForText('tree.webp', 10, '.elfinder-cwd-wrapper');

    I.say('Verifying webp file is visible in elfinder');
    I.seeElement('.elfinder-cwd-filename[title="tree.webp"]');

    I.say('Displaying webp image via /thumb prefix and comparing screenshot');
    I.amOnPage('/thumb/images/produktova-stranka/' + folderName + '/tree.webp?w=400&h=400&ip=5');
    I.waitForElement('img', 10);
    await Document.compareScreenshotElement('img', 'thumb-servlet/webp-tree-thumb.png', null, null, 5);
});

Scenario('webp cleanup', async ({ I }) => {
    I.say('Deleting all autotest-webp- folders');
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2E_E'); //images/produktova-stranka
    I.waitForElement('.elfinder-navbar-wrapper', 10);
    I.waitForElement("#iwcm_2_L2ltYWdlcy9wcm9kdWt0b3ZhLXN0cmFua2EvbW9uZXkuanBn", 10); //money.jpg file

    //delete all folders starting with autotest-webp- (including leftovers from previous runs)
    let numVisible = await I.grabNumberOfVisibleElements('.elfinder-cwd-filename[title^="autotest-webp-"]');
    while (numVisible > 0) {
        I.click('.elfinder-cwd-filename[title^="autotest-webp-"]');
        I.rightClick('.elfinder-cwd-filename[title^="autotest-webp-"]');
        I.waitForVisible('.elfinder-contextmenu', 10);
        I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible('.elfinder-confirm-accept', 10);
        I.wait(1);
        numVisible = await I.grabNumberOfVisibleElements('.elfinder-cwd-filename[title^="autotest-webp-"]');
    }
});
