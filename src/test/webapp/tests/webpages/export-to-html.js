Feature('webpages.export-to-html');

Before(({ login }) => {
    login('admin');
});

async function deleteExportedFiles(I) {
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
}

Scenario('export to html tests', async ({ I }) => {
    await deleteExportedFiles(I);

    I.amOnPage("/admin/offline.jsp");
    I.fillField('#groupId1', '23');
    I.clickCss('.button50');
    I.switchToNextTab();
    I.see('Adresáre');
    I.switchToPreviousTab();
    I.closeOtherTabs();
    I.dontSee('Vyberte priečinky');

    I.checkOption('#makeZipArchiveId');
    I.see('Vyberte priečinky');

    const directories = ['apps','components','files','images',
                        'static-files','templates','WEB-INF','wjerrorpages'];
    directories.forEach((directory) => {
        I.see(directory);
    });
    I.checkOption('#makeZipArchiveId');
    I.uncheckOption('input[value="images"]');
    I.uncheckOption('input[value="files"]');
    I.click("wjerrorpages");
    I.clickCss('#btnOk');
    I.waitForText('Generovanie offline verzie bolo úspešne ukončené!', 120);

    //verify that files were created
    I.relogin('admin');
    I.amOnPage("/admin/v9/files/index/#elf_iwcm_2_L2h0bWw_E");
    I.waitForElement("#iwcm_2_L2h0bWwvYmxhbmsuaHRtbA_E_E", 10); //blank.html
    I.waitForElement('.elfinder-cwd-filename[title="blank.html"]', 10);
    I.seeElement(".elfinder-cwd-filename[title='showdoc.dodocid=25100.html']");

    I.click( locate("span#nav-iwcm_2_"), null, { position: { x: 20, y: 5 } }); //vsetky subory
    I.waitForElement('.elfinder-cwd-filename[title*="offline-aceintegration-"]', 10);
});

Scenario('delete exported files', async ({ I }) => {
    await deleteExportedFiles(I);
});

Scenario('check perms', async ({ I }) => {
    I.amOnPage("/admin/v9/?removePerm=export_offline");
    I.amOnPage("/admin/offline.jsp");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
    I.logout();
});

