Feature('ai.ai-stat');

Before(({ I, login }) => {
    login('admin');
});

Scenario('base tests', async ({I, DTE, DataTables}) => {
    I.amOnPage("/admin/v9/settings/ai-stats/");

    I.say("Check elements");

    I.seeElement("#aiStatDataTable_extfilter");
    within("#aiStatDataTable_extfilter", () => {
        I.seeElement("div.dt-extfilter-created");
        I.seeElement("div.dt-extfilter-assistantAction");
        I.seeElement("div.dt-extfilter-assistantProvider");
        I.seeElement("div.dt-extfilter-assistantGroupName");
    });

    within("#graphsDiv > div:nth-child(1) > div:nth-child(1)", () => {
        I.seeElement( locate("h6.amchart-header").withText("Najčastejšie používaný AI asistenti") );
        I.seeElement( locate("#ai-pieChartMostUsed > div").withChild("div.am5-html-container") );
    });

    within("#graphsDiv > div:nth-child(1) > div:nth-child(2)", () => {
        I.seeElement( locate("h6.amchart-header").withText("AI asistenti s najvyšou spotrebou tokenov") );
        I.seeElement( locate("#ai-pieChartMostTokens > div").withChild("div.am5-html-container") );
    });

    within("#graphsDiv > div:nth-child(2)", () => {
        I.seeElement( locate("h6.amchart-header").withText("Použitie AI asistentov za jednotlivé dni") );
        I.seeElement( locate("#ai-lineChartDaysUsage > div").withChild("div.am5-html-container") );
    });

    within("#graphsDiv > div:nth-child(3)", () => {
        I.seeElement( locate("h6.amchart-header").withText("Počet použitých tokenov AI asistentami za jednotlivé dni") );
        I.seeElement( locate("#ai-lineChartDaysTokens > div").withChild("div.am5-html-container") );
    });

    I.seeElement("table#aiStatDataTable");

    I.say("Check filtered values");
    //TODO - after it gonna be merged all together
});