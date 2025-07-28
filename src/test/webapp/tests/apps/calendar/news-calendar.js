Feature('apps.calendar.news-calendar');

Before(({ I, login, DT }) => {
    login('admin');
    DT.addContext("calendar", "#calendarEventsDataTable_wrapper");
});

Scenario('testovanie app - Kalendar news', async ({ I, DT, DTE, Document, Apps }) => {
    I.say('Retriving perex id for test');
    I.amOnPage('/admin/v9/webpages/perex/');
    DT.filterEquals('perexGroupName', 'Import perex');
    const perexId = await I.grabTextFrom('td.dt-select-td');

    Apps.insertApp('Kalendár noviniek', '#components-calendarnews-title');

    const defaultParams = {
        groupIds: '',
        expandGroupIds: 'true',
        perexGroup: ''
    };

    await Apps.assertParams(defaultParams);

    I.say('Default parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    await Document.waitForTab();
    I.switchToNextTab();

    I.waitForElement("div.kalendar", 10);

    I.switchToPreviousTab();
    I.closeOtherTabs();

    Apps.openAppEditor();

    const changedParams = {
        groupIds: '24',
        expandGroupIds: 'true',
        perexGroup: perexId
    };

    I.clickCss("#editorAppDTE_Field_groupIds button");
    I.waitForElement("#custom-modal-id", 10);
    I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText("Jet portal 4").find('.jstree-icon.jstree-ocl'));
    I.click(locate(".jstree-anchor").withText("Zo sveta financií"));
    DTE.clickSwitch("perexGroup_3");

    I.switchTo();
    I.clickCss('.cke_dialog_ui_button_ok')

    await Apps.assertParams(changedParams);

    I.say('Changed parameters visual testing');
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();

    I.waitForElement("div.kalendar", 10);
});

Scenario('Test - News Calendar and News component connection', ({ I }) => {
    const newsData = [
        { title: "McGregorov obchodný údera", date: "29.10.2018" },
        { title: "Trhy sú naďalej vydesené", date: "01.11.2018" },
        { title: "Konsolidácia naprieč trhmi", date: "05.11.2018" },
        { title: "Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív", date: "03.11.2018" },
        //{ title: "Čím je človek bohatší, tým má menej hotovosti", date: "03.05.2020" }
    ];

    newsData.forEach(({ title, date }) => {
        checkNewsForDate(I, title, date);
    });
});

function checkNewsForDate(I, title, date) {
    I.amOnPage(`/apps/kalendar-noviniek/?datum=${date}`);
    I.seeElement(locate("div.news-calendar-container h3 a").withText(title));

    const otherTitles = [
        "Konsolidácia naprieč trhmi",
        "Trhy sú naďalej vydesené",
        "McGregorov obchodný údera",
        "Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív"
    ];

    otherTitles.forEach((otherTitle) => {
        I.seeElement(locate("div.news-container h3 a").withText(otherTitle));
    });
}
