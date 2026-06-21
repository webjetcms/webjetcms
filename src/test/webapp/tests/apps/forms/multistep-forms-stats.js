Feature('apps.forms.multistep-forms-stats');

Before(({ I, DT, login }) => {
    login('admin');
});

Scenario('Base structure tests', ({ I }) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");

    I.say("Check header widgets");
    I.seeElement(".header-container");
    checkHeaderWidget(I, ".ti.ti-users", "Odpovedí");
    checkHeaderWidget(I, ".ti.ti-alarm", "Priemerné trvanie vyplnenia");
    checkHeaderWidget(I, ".ti.ti-calendar", "Dní od vytvorenia");
    checkHeaderWidget(I, ".ti.ti-calendar-pause", "Posledná odpoveď");

    // CHART section is generated using '/apps/_common/charts/stats-by-charts.js' soo we need to check structure throughly
    I.say("Check charts");
    checkChart(I, "meno-1", "Vaše meno");
    checkChart(I, "radiogroup-1", "Skupina výberových polí");
    checkChart(I, "checkboxgroup-1", "Skupina zaškrtávacích polí");
    checkChart(I, "pohlavie-false", "Pohlavie");
    checkChart(I, "select-1", "Select pole");
    checkChart(I, "radiogroup-2", "Ako je na tom Vaša organizácia s implementáciou podmienok kvality? Označte jednu možnosť, ktorá najlepšie zodpovedá Vašej súčasnej situácii.");
});

Scenario('Check setting button and showed dialog', ({ I, Document }) => {
    I.amOnPage("/apps/form/admin/form-stats/?formName=Multistepform_screens");

    checkChartSettings(I, Document, "priezvisko-1", "word_cloud");
    checkChartSettings(I, Document, "radiogroup-1", "pie_donut", true);
    checkChartSettings(I, Document, "select-1", "bar_horizontal");
    checkChartSettings(I, Document, "radiogroup-2", "bar_vertical", true);
    checkChartSettings(I, Document, "pohlavie-false", "pie_classic");
});

function checkChartSettings(I, Document, chartId, chartType, colorScheme = false) {
    I.say('Checking settings of chart : ' + chartId);
    let button = "#form-stats_" + chartId + "_container button.chart-more-btn";
    Document.scrollTo(button);
    I.clickCss(button);
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

async function setClEditorValue(I, wysiwyg) {
    await I.executeScript((text) => {
            const ta = document.querySelector('#wysiwyg-1');
            if (ta) ta.value = '<p>' + text + '</p>';
            const iframe = document.querySelector('.cleditorMain iframe');
            if (iframe && iframe.contentDocument && iframe.contentDocument.body) {
                iframe.contentDocument.body.innerHTML = '<p>' + text + '</p>';
            }
            if (typeof $ !== 'undefined') {
                try {
                    const ed = $('#wysiwyg-1').cleditor();
                    if (ed && ed.length > 0) ed[0].updateTextArea();
                } catch (e) { /* ignore if cleditor sync unavailable */ }
            }
        }, wysiwyg);
}

Scenario("Generate random data for nice charts", async ({ I }) => {
    I.logout();

    const firstNames = [
        "Jana", "Katarína", "Peter", "Zuzana", "Ján", "Mária", "Martin", "Tomáš", "Michal", "Lukáš", "Marek", "Rastislav",
        "Anna", "Eva", "Lucia", "Andrea"
    ];
    const lastNames = [
        "Novák", "Kováč", "Horváth", "Varga", "Tóth",
        "Procházka", "Blaho", "Mináč", "Hudák", "Šimko",
        "Rusnák", "Marko", "Polák", "Krajčí", "Baláž"
    ];
    const wysiwygs = [
        "Veľmi <strong>spokojný zákazník</strong>, odporúčam.",
        "<strong>Skvelý produkt</strong>, budem sa vracať.",
        "Výborná <strong>kvalita</strong> za rozumnú cenu.",
        "Splnilo moje očakávania.",
        "Profesionálny prístup a rýchle riešenie.",
        "Nadštandardné služby.",
        "Celkovo <strong>pozitívna</strong> skúsenosť.",
        "Rád odporúčam priateľom."
    ];
    const affiliate = [
        "google.com", "facebook.com", "interway.sk", "webjetcms.sk"
    ];

    // Weighted random selection: items at lower indices have higher probability.
    // Probability decreases linearly: P(i) ∝ (n - i), so first item is most likely.
    function weightedRandom(items) {
        const n = items.length;
        const total = (n * (n + 1)) / 2;
        let rand = Math.random() * total;
        for (let i = 0; i < n; i++) {
            rand -= (n - i);
            if (rand <= 0) return items[i];
        }
        return items[n - 1];
    }

    const iterations = 1;

    for (let iter = 0; iter < iterations; iter++) {
        var qs = "";
        if (Math.random() < 0.5) {
            qs = "?utm_source="+weightedRandom(affiliate);
        }

        var baseUrl = "/apps/multistep-formular/for-screenshots.html";
        if (Math.random() < 0.2) {
            baseUrl = "/apps/multistep-formular/en/for-screenshots.html";
        }

        I.amOnPage(baseUrl+qs);
        I.waitForElement("#meno-1", 10);

        const firstName = weightedRandom(firstNames);
        const lastName = weightedRandom(lastNames);

        //make error in 10% of cases to test error handling in stats
        if (Math.random() < 0.1) {
            I.fillField("#meno-1", "");
            I.click("Prejsť na ďalší krok");
        }
        if (Math.random() < 0.2) {
            I.fillField("#priezvisko-1", "");
            I.click("Prejsť na ďalší krok");
        }

        // Fill step 1 fields
        I.fillField("#meno-1", firstName);
        I.fillField("#priezvisko-1", lastName);

        //use firstname without diacritics for email to avoid potential encoding issues in stats processing
        const emailFirstName = firstName.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        I.fillField("#email-1", emailFirstName.toLowerCase() + "@onetimeusemail.com");

        var failStatus = null;
        if (Math.random() < 0.1) {
            I.fillField("#email-1", "relay.denied.email@onetimeusemail.com");
            failStatus = "emailNotSend";
        }

        // Checkboxes A, B, C with decreasing independent probability: A=65%, B=35%, C=15%
        if (Math.random() < 0.65) I.checkOption("#checkboxgroup-1-0");
        if (Math.random() < 0.35) I.checkOption("#checkboxgroup-1-1");
        if (Math.random() < 0.15) I.checkOption("#checkboxgroup-1-2");

        // Radio D/E/F with weighted selection: D most likely, F least likely
        I.checkOption(weightedRandom(["#radiogroup-1-0", "#radiogroup-1-1", "#radiogroup-1-2"]));

        // Potvrdit - optional field, selected with 75% probability
        if (Math.random() < 0.75) I.checkOption("#potvrdit-false-ano");

        // Gender with slight male bias (55% Muz, 45% Zena)
        I.checkOption(Math.random() < 0.55 ? "#pohlavie-false-muz" : "#pohlavie-false-zena");

        // Submit step 1
        I.click("Prejsť na ďalší krok");
        I.waitForElement("#select-1", 10);

        // Fill step 2 - select field with weighted probability: A most likely, D least likely
        I.selectOption("#select-1", weightedRandom(["Mačka", "Pes", "Škrečok", "Had"]));

        if (Math.random() < 0.1) {
            //empty wysiwyg
            await setClEditorValue(I, "");
            I.click("Odoslať formulár");
            I.wait(10);
        }

        // Fill step 2 - WYSIWYG is a required field rendered as cleditor (hidden textarea + iframe).
        // Inject the value via JS: set both the textarea and the iframe body, then trigger cleditor sync.
        let wysiwyg = weightedRandom(wysiwygs);

        if (Math.random() < 0.1) {
            wysiwyg += " <script>alert('XSS');</script>";
            failStatus = "probablySpamBot";
        }

        await setClEditorValue(I, wysiwyg);

        I.checkOption(weightedRandom(["#radiogroup-2-0", "#radiogroup-2-1", "#radiogroup-2-2", "#radiogroup-2-3"]));

        //SPAM PROTECTION: wait for random time between 30 and 34 seconds to simulate real user behavior
        const waitTime = 30000 + Math.random() * 4000;
        I.wait(waitTime / 1000);

        // Submit step 2
        I.click("Odoslať formulár");

        I.waitForElement(".alert.alert-success", 10);
        if (baseUrl.indexOf("/en/") >= 0) {
            if ("emailNotSend" === failStatus) {
                I.waitForText("Sending of form to email failed!", 10);
            } else if ("probablySpamBot" === failStatus) {
                I.waitForText("Detected spam - spambot", 10);
            } else {
                I.waitForText("The form was successfully submitted", 10);
            }
        } else {
            if ("emailNotSend" === failStatus) {
                I.waitForText("Formulár sa nepodarilo odoslať na email", 10);
            } else if ("probablySpamBot" === failStatus) {
                I.waitForText("Detekovany spam - spambot", 10);
            } else {
                I.waitForText("Formulár bol úspešne odoslaný", 10);
            }
        }
    }
});