Feature('manual-sysadmin');

Before(({ I, login }) => {
    login('admin');
});

Scenario('monitoring', ({ I, Document }) => {
    I.amOnPage("/apps/server_monitoring/admin/");
    I.wait(120);
    Document.screenshot("/sysadmin/monitoring/actual.png", 1360, 1000);

    I.amOnPage("/apps/server_monitoring/admin/records/");
    Document.screenshot("/sysadmin/monitoring/historical.png", 1360, 1000);
});

Scenario('monitoring - node based', ({ I, Document }) => {
    I.amOnPage("/apps/server_monitoring/admin/sql/");

    //
    Document.screenshot("/sysadmin/monitoring/sql.png", 700, 500);
    switch (I.getConfLng()) {
        case "en":
            I.click('//button[contains(@title, "Current node")]');
            break;
        case "sk":
            I.click('//button[contains(@title, "Aktuálny uzol")]');
            break;
        case "cs":
            I.click('//button[contains(@title, "Aktuální uzel")]');
            break;
        default:
            throw new Error("Unknown language: " + I.getConfLng());
    }

    //Select options
    Document.screenshotElement("div.dropdown-menu.show" ,"/sysadmin/monitoring/select-options.png");

    //Change selected node
    I.click(locate("a.dropdown-item").withText("webjet4b"));
    I.wait(1);

    //Call node refresh data (other then actual node)
    I.click({css: "button.buttons-refresh"});
    I.wait(1);

    //Screen of loader and notification
    Document.screenshot("/sysadmin/monitoring/updating-data.png");
});
