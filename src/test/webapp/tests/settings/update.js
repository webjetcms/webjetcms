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

Data([
    { policy: 'strict-origin-when-cross-origin', embed: true },
    { policy: 'same-origin', embed: false }
]).Scenario('release notes respect the referrer policy', async ({ I, current }) => {
    const pageRoute = '**/admin/v9/settings/update/';
    const versionsRoute = '**/admin/update/versions';
    const youtubeRoute = 'https://*.youtube.com/**';
    const videoUrl = 'https://www.youtube.com/watch?v=q8xs3qDq-G4';
    const thumbnailUrl = 'https://img.youtube.com/vi/q8xs3qDq-G4/hqdefault.jpg';
    const imageUrl = 'https://example.invalid/autotest.png';
    const content = '#versions-tab-content .tab-pane.active';
    let youtubeRequests = 0;
    await I.mockRoute(pageRoute, async route => {
        const response = await route.fetch();
        const body = (await response.text()).replace(/data-referrer-policy="[^"]*"/, `data-referrer-policy="${current.policy}"`);
        const headers = { ...response.headers(), 'referrer-policy': current.policy };
        delete headers['content-encoding'];
        delete headers['content-length'];
        return route.fulfill({ response, body, headers });
    });
    await I.mockRoute(youtubeRoute, route => {
        youtubeRequests++;
        return route.fulfill({ contentType: 'text/html', body: '' });
    });
    await I.mockRoute(versionsRoute, route => route.fulfill({ json: [{
        version: '2026.40',
        note: `![External image](${imageUrl})\n\n![Documentation image](autotest.png)\n\n[![YouTube video](${thumbnailUrl})](${videoUrl})`
    }] }));
    try {
        await I.refreshPage();
        const externalImage = `${content} img[alt="External image"]`;
        await I.waitForElement(externalImage);
        await I.assertEqual(await I.grabAttributeFrom(externalImage, 'src'), imageUrl);
        await I.assertEndsWith(await I.grabAttributeFrom(`${content} img[alt="Documentation image"]`, 'src'), '/v2026/autotest.png');
        await I.see('YouTube video', `${content} a[href="${videoUrl}"][rel~="noreferrer"]`);
        await I.dontSeeElementInDOM(`${content} iframe[referrerpolicy], ${content} img[alt="YouTube video"]`);
        if (current.embed) await I.seeAttributesOnElements(`${content} iframe`, { src: 'https://www.youtube.com/embed/q8xs3qDq-G4' });
        else {
            await I.dontSeeElementInDOM(`${content} iframe`);
            await I.assertEqual(youtubeRequests, 0);
        }
    } finally {
        await I.stopMockingRoute(versionsRoute);
        await I.stopMockingRoute(youtubeRoute);
        await I.stopMockingRoute(pageRoute);
    }
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
