Feature('admin.archive');

Before(({ login }) => {
    login('admin');
});

Scenario('archive tests', async ({ I }) => {
    I.amOnPage("/admin/archive.jsp");
    I.see('Cesta k archívu:');
    I.see('Archivovať priečinky');
    const directories = ['apps','components','files','images','templates','WEB-INF','wjerrorpages'];

    directories.forEach((directory) => {
        I.see(directory);
    });

    //select wjerrorpages folder
    I.click("wjerrorpages");
    I.clickCss("#btnOk");

    //check response
    I.waitForText("Hotovo", 30);
    I.see("Názov archívu");

    //delete file
    I.amOnPage('/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL3Byb3RlY3RlZC9iYWNrdXA_E');
    I.wait(1);
    const fileSelector = ['.elfinder-cwd-filename[title*="aceintegration-"]'];
    for (let i = 0; i < fileSelector.length; i++) {
        let numVisible;
        do {
            numVisible = await I.grabNumberOfVisibleElements(fileSelector[i]);
            if (numVisible) {
                await I.clickCss(fileSelector[i]);
                await I.pressKey('Delete');
                var text = "html";
                if (fileSelector[i].includes("aceintegration-")) {
                    text = "aceintegration-";
                }
                I.waitForText(text, 5, "div.elfinder-rm-title");
                await I.clickCss('.elfinder-confirm-accept');
                I.wait(2);
            }
        } while (numVisible > 0);
    }
});

Scenario('check perms', async ({ I }) => {
    I.amOnPage("/admin/v9/?removePerm=make_zip_archive");
    I.amOnPage("/admin/archive.jsp");
    I.see("Na túto aplikáciu/funkciu nemáte prístupové práva");
});

Scenario('logout', ({ I }) => {
    I.logout();
});