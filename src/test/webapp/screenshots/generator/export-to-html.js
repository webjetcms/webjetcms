Feature('webpages.export-to-html');

Before(({ login }) => {
    login('admin');
});

Scenario('export to html screenshots', ({ I, Document }) => {
    I.amOnPage("/admin/offline.jsp");
    I.fillField('#groupId1', '23');
    I.checkOption('#makeZipArchiveId');
    Document.screenshot("/redactor/webpages/export-to-html/export-to-html.png", 704, 623);
    I.uncheckOption('#dir_0Id');
    I.clickCss('#btnOk');
    I.waitForText('Done!', 300);
    I.executeScript(() => window.scrollTo(0, 0));
    Document.screenshot("/redactor/webpages/export-to-html/report.png", 704, 623);

    I.amOnPage("/admin/v9/files/");
    I.waitForElement("#nav-iwcm_2_");
    I.click( locate("span#nav-iwcm_2_"), null, { position: { x: 20, y: 5 } }); //vsetky subory
    I.waitForElement("#nav-iwcm_2_L2h0bWw_E"); //html
    I.clickCss("#iwcm_2_L2h0bWw_E div.elfinder-cwd-filename");
    var modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
    I.click('.elfinder-cwd-filename[title*="offline-aceintegration-"]', null, {modifiers: [modifier]});
    Document.screenshot("/redactor/webpages/export-to-html/exported-files.png");
});

Scenario('delete exported files', async ({ I }) => {
    I.amOnPage('/admin/v9/files/index/');
    I.waitForElement("#nav-iwcm_2_");
    I.wait(1);
    I.click( locate("span#nav-iwcm_2_"), null, { position: { x: 20, y: 5 } }); //vsetky subory
    I.waitForElement("#iwcm_2_L2ZpbGVz", 10); //files
    I.wait(1);
    const fileSelector = ['.elfinder-cwd-filename[title="html"]', '.elfinder-cwd-filename[title*="offline-aceintegration-"]'];
    for (let i = 0; i < fileSelector.length; i++) {
        let numVisible;
        do {
            numVisible = await I.grabNumberOfVisibleElements(fileSelector[i]);
            if (numVisible) {
                await I.clickCss(fileSelector[i]);
                await I.pressKey('Delete');
                var text = "html";
                if (fileSelector[i].includes("offline-aceintegration-")) {
                    text = "offline-aceintegration-";
                }
                I.waitForText(text, 5, "div.elfinder-rm-title");
                await I.clickCss('.elfinder-confirm-accept');
                I.wait(2);
            }
        } while (numVisible > 0);
    }
});