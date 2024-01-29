Feature('components.cookie-manager');

var randomNumber, entityName;

Before(({ I, login }) => {
    login('admin');
});

Scenario('cookie-manager-zakladne testy', async ({I, DataTables}) => {
    I.amOnPage("/apps/gdpr/admin/");
    await DataTables.baseTest({
        dataTable: 'cookiesDataTable',
        perms: 'menuGDPR',
        createSteps: function(I, options) {
        },
        editSteps: function(I, options) {
        },
        editSearchSteps: function(I, options) {
        },
        beforeDeleteSteps: function(I, options) {
            //I.wait(20);
        },
    });
});

Scenario('cookie-manager testy-domeny-a-jazyka', async ({I, DT, DTE}) => {
    I.amOnPage("/apps/gdpr/admin/");

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        entityName = "name-autotest-" + randomNumber;
    }

    //Vytvor novu entitu, na vyplnenie field-u "cookieName" použi hodnotu entityName
    I.click("button.buttons-create");
    DT.waitForLoader();
    I.fillField("#DTE_Field_cookieName", entityName);
    //Prepni sa do založky "advanced" a vyplň field "purpouse" hodnotou "SK"
    I.click('#pills-dt-cookiesDataTable-advanced-tab');
    DT.waitForLoader();
    I.fillField("#DTE_Field_purpouse", "SK");
    DTE.save();

    //Zmena jazyka na cesky
    I.click({css: "div.breadcrumb-language-select"});
    I.click(locate('.dropdown-item').withText("Český jazyk"));

    //Vyfiltruj a over že entita bola pridaná
    I.fillField("input.dt-filter-cookieName", randomNumber);
    I.pressKey('Enter', "input.dt-filter-cookieName");
    DT.waitForLoader();
    I.see(entityName);

    //Pre cesky jazyk nastav inu hodnotu field-u
    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-edit");
    DT.waitForLoader();
    //Prepni sa do založky "advanced" a vyplň field "purpouse" hodnotou "CZ"
    I.click('#pills-dt-cookiesDataTable-advanced-tab');
    DT.waitForLoader();
    I.fillField("#DTE_Field_purpouse", "CZ");
    DTE.save();
    I.see("CZ");

    //Zmena jazyka spat na slovensky
    I.click({css: "div.breadcrumb-language-select"});
    I.click(locate('.dropdown-item').withText("Slovenský jazyk"));

    //Zisti ci sa hodnota z ceskeho jazyka nepreniesla do field-u purpouse v slovenskom jazyku
    I.click("button.buttons-edit"); //zaznam uz je vyfiltrovany a oznaceny
    DT.waitForLoader();
    //Prepni sa do založky "advanced", over ze tu nieje hodnota "CZ" a je hodnota "SK"
    I.click('#pills-dt-cookiesDataTable-advanced-tab');
    DT.waitForLoader();
    I.dontSee("CZ");
    I.see("SK");
    DTE.save();

    //Zmena domény
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("domena1.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    //Vyfiltruj a over že entita neexistuje v inej doméne ako tej, kde bola pridaná
    I.fillField("input.dt-filter-cookieName", randomNumber);
    I.pressKey('Enter', "input.dt-filter-cookieName");
    DT.waitForLoader();
    I.dontSee(entityName);

    //zmaz z povodnej domeny
    I.click("div.js-domain-toggler div.bootstrap-select button");
    I.wait(1);
    I.click(locate('.dropdown-item').withText("webjet9.tau27.iway.sk"));
    I.waitForElement("#toast-container-webjet", 10);
    I.click(".toastr-buttons button.btn-primary");

    I.fillField("input.dt-filter-cookieName", randomNumber);
    I.pressKey('Enter', "input.dt-filter-cookieName");
    DT.waitForLoader();
    I.see(entityName);

    I.click("td.dt-select-td.sorting_1");
    I.click("button.buttons-remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DT.waitForLoader();
    I.dontSee(entityName);
});