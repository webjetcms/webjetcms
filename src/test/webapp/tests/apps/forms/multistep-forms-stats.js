Feature('apps.forms.multistep-forms-stats');

Before(({ I, DT, login }) => {
    login('admin');
});

Scenario('Base structure tests', ({ I }) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");

    I.say("Check header widgets");
    I.seeElement(".header-container");
    checkHeaderWidget(I, ".ti.ti-users", "Počet odpovedí");
    checkHeaderWidget(I, ".ti.ti-alarm", "Priemerný čas vyplnenia");
    checkHeaderWidget(I, ".ti.ti-calendar", "Počet dní od vytvorenia");
    checkHeaderWidget(I, ".ti.ti-calendar-pause", "Dátum poslednej odpovede");

    // CHART section is generated using '/apps/_common/charts/stats-by-charts.js' soo we need to check structure throughly
    I.say("Check charts");
    checkChart(I, "meno-1", "Meno");
    checkChart(I, "radiogroup-1", "Skupina výberových polí");
    checkChart(I, "checkboxgroup-1", "Skupina zaškrtávacích polí");
    checkChart(I, "suhlaspodmienky-1", "Súhlas s podmienkami");
    checkChart(I, "select-1", "Výberový zoznam - select");
    checkChart(I, "select-2", "Ako je na tom Vaša organizácia s implementáciou podmienok kvality? (označte jednu možnosť, ktorá najlepšie zodpovedá Vašej súčasnej situácii)");
});

Scenario('Check setting button and showed dialog', ({ I }) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=stattestform");

    checkChartSettings(I, "meno-1", "word_cloud");
    checkChartSettings(I, "radiogroup-1", "pie_donut");
    checkChartSettings(I, "checkboxgroup-1", "bar_horizontal");
    checkChartSettings(I, "suhlaspodmienky-1", "pie_classic", true);
});

function checkChartSettings(I, chartId, chartType, colorScheme = false) {
    I.say('Checking settings of chart : ' + chartId);
    I.clickCss("#form-stats_" + chartId + "_container button.chart-more-btn");
    I.waitForVisible("iframe#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");

    within("#formItemsDataTable_modal #pills-dt-formItemsDataTable-stat", () => {
        I.seeCheckboxIsChecked("#DTE_Field_showStat_0");
        I.seeCheckboxIsChecked( locate(".DTE_Field_Name_chartType input[value='" + chartType + "']") );

        if(colorScheme === true) {
            I.seeCheckboxIsChecked("#DTE_Field_useColorScheme_0");
        } else {
            I.dontSeeCheckboxIsChecked("#DTE_Field_useColorScheme_0");
        }
    });

    I.switchTo();
    I.clickCss("#modalIframe button.btn-close");
    I.waitForInvisible("iframe#modalIframeIframeElement");
}

function checkHeaderWidget(I, icon, text) {
    I.say("Checking header widget");
    I.seeElement(".header-container > div.header-wrapper-outher > div.stat-card > div.stat-card__icon > i" + icon);
    I.seeElement( locate(".header-container > div.header-wrapper-outher > div.stat-card > div.stat-card__content > span.stat-card__title").withText(text) );
}

function checkChart(I, chartId, chartTitle) {
    I.say("Checking chart : " + chartId);
    I.seeElement("div#chartContainer > #form-stats > #form-stats_" + chartId + "_container");
    I.seeElement("div#chartContainer > #form-stats > #form-stats_" + chartId + "_container > button.chart-more-btn > span > i.ti.ti-settings");
    I.seeElement( locate("div#chartContainer > #form-stats > #form-stats_" + chartId + "_container > h6.amchart-header").withText(chartTitle) );
    I.seeElement("div#chartContainer > #form-stats > #form-stats_" + chartId + "_container > div#form-stats_" + chartId + ".amcharts");
}
