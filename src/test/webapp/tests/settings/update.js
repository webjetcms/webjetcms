Feature('settings.update');

Before(({ I, login }) =>{
    login('admin');
    I.amOnPage("/admin/v9/settings/update/");
});

Scenario('update from server', async ({ I }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("2024.18");

    I.waitForText("v Štatistika-Prehliadače dôjde k drobným rozdielom, ale údaje o prehliadači anonymizovane zaznamenávame aj bez Cookies súhlasu.", 10);
    I.seeElement("#submitButton");
});

Scenario('release notes render external images and YouTube videos', async ({ I }) => {
    const versionsRoute = '**/admin/update/versions';
    const videoUrl = 'https://www.youtube.com/watch?v=q8xs3qDq-G4';
    const thumbnailUrl = 'https://img.youtube.com/vi/q8xs3qDq-G4/hqdefault.jpg';
    const imageUrl = 'https://example.invalid/autotest.png';
    const content = '#versions-tab-content .tab-pane.active';
    I.mockRoute(versionsRoute, route => route.fulfill({ json: [{
        version: '2026.40',
        note: `![External image](${imageUrl})\n\n![Documentation image](autotest.png)\n\n[![YouTube video](${thumbnailUrl})](${videoUrl})`
    }] }));
    I.refreshPage();

    const externalImage = `${content} img[alt="External image"]`;
    I.waitForElement(externalImage);
    I.assertEqual(await I.grabAttributeFrom(externalImage, 'src'), imageUrl);
    I.assertEndsWith(await I.grabAttributeFrom(`${content} img[alt="Documentation image"]`, 'src'), '/v2026/autotest.png');
    I.seeAttributesOnElements(`${content} iframe`, { src: 'https://www.youtube.com/embed/q8xs3qDq-G4' });
    I.see('YouTube video', `${content} a[href="${videoUrl}"]`);

    I.stopMockingRoute(versionsRoute);
});

Scenario('update from file', async ({ I }) => {
    I.waitForElement("#version-selector");
    I.wait(1);

    I.click("Aktualizovať zo súboru");
    I.dontSeeElement("#submitButton");
    I.seeElement("#UPLOAD > form");

    I.clickCss("#uploadSubmitButton");
    I.waitForText("Dokument nemôže byť prázdny");
    I.waitForElement(locate("div.alert-danger li").withText("Dokument nemôže byť prázdny"), 10);
    I.dontSeeElement("#submitButton");
    I.seeElement("#UPLOAD > form");

});

Scenario('test perms', async ({ I }) => {
    I.amOnPage("/admin/v9/settings/update/?removePerm=modUpdate");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
    I.logout();
});