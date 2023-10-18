Feature('webpages.linkcheck');

Before(({ I, login }) => {
    login('admin');
});

Scenario('basic-test stranky', async ({I, DT}) => {

    I.amOnPage("/admin/v9/webpages/linkcheck?groupId=");

    //Check you see all 3 tabs
    I.see("Nefunkčné odkazy");
    I.see("Stránky so zakázaným zobrazovaním");
    I.see("Prázdne stránky");

    //Check hidden buttons
    I.dontSeeElement("button.buttons-create");
    I.dontSeeElement("button.buttons-edit");
    I.dontSeeElement("button.btn-duplicate");
    I.dontSeeElement("button.buttons-remove");
    I.dontSeeElement("button.buttons-import");

    //Try redirect from web-page-list using button
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=67");
    DT.waitForLoader();
    I.click("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.btn-outline-secondary.buttons-linkcheck");

    DT.waitForLoader();
    I.see("/Test stavov/Test linkcheck");
    I.see("Odkaz neexistuje");
    I.see("Súbor neexistuje");
    I.see("Obrázok neexistuje");
    I.see("/ja-uz-neexistujem/");
    I.see("/files/neexistujuci-subor.pdf");
    I.see("/images/neexistujuci-obrazok.png");

    I.click("#pills-hiddenPages-tab");
    DT.waitForLoader();
    I.see("/Test stavov/Test linkcheck - vypnute zobrazenie");
    I.see("Stránka má zakázané zobrazovanie");
    I.see("/novy-adresar-01/stranka-vypnutym-zobrazenim.html");

    I.click("#pills-emptyPages-tab");
    DT.waitForLoader();
    I.see("/Test stavov/Zobrazený v menu/Zobrazený v menu");
    I.see("Stránka je pravdepodobne prázdna: 13 znakov");
    I.see("/novy-adresar-01/zobrazeny-menu/");
});