Feature('rag.semantic-search');

Before(({ I, login }) =>{
    login('admin');
});

Scenario('Set semantic search as wanted search', ({ I, DT, Document }) => {
    Document.setConfigValue("searchType", "semantic");
    Document.setConfigValue("searchType", "semantic");
    Document.setConfigValue("ragSemanticSearchEnabled", "true");
});

const searchData = [
    {
        pageName: "semantic_parent",
        pageUrl: "/apps/vyhladavanie/semantic_parent/",
        questions: [
            "Prečo sa McGregorova whisky volá Proper No. Twelve",
            "Akú whisky vlastní Conor McGregor",
            "Ktoré celebrity majú vlastné značky alkoholu",
            "Ako McGregor využil zápas s Khabibom na propagáciu"
        ]
    },
    {
        pageName: "Konsolidácia naprieč trhmi",
        pageUrl: "/apps/vyhladavanie/semantic_parent/konsolidacia-napriec-trhmi.html",
        questions: [
            "Ako reagovali americké akcie na nové clá medzi USA a Čínou",
            "Prečo sa americké akcie držali blízko historických maxím",
            "Ako odstránenie slova „uvoľnená“ z hodnotenia menovej politiky Fedu ovplyvnilo trhy"
        ]
    },
    {
        pageName: "Trhy sú naďalej vydesené",
        pageUrl: "/apps/vyhladavanie/semantic_parent/trhy-su-nadalej-vydesene.html",
        questions: [
            "Prečo boli finančné trhy také volatilné",
            "Ako inflácia, vyššie úroky a obchodná vojna ovplyvnili americké akcie",
            "Na akú úroveň klesol 10-ročný americký výnos"
        ]
    },
    {
        pageName: "Čím je človek bohatší, tým má menej hotovosti",
        pageUrl: "/apps/vyhladavanie/semantic_parent/cim-je-clovek-bohatsi-tym-ma-menej-hotovosti.html",
        questions: [
            "Kde majú ľudia najväčší podiel rizikových aktív",
            "Prečo bohatší ľudia držia menej hotovosti",
            "Ako bohatstvo ovplyvňuje ochotu investovať",
            "Prečo bohatší ľudia viac podstupujú investičné riziko"
        ]
    },
    {
        pageName: "Zo sveta financií",
        pageUrl: "/apps/vyhladavanie/semantic_parent/zo-sveta-financii.html",
        questions: [
            "Aké témy patria do sveta financií"
        ]
    },
    {
        pageName: "Graf týždňa: Svetové akciové indexy majú vyšší podiel klesajúcich aktív",
        pageUrl: "/apps/vyhladavanie/semantic_parent/semantic_child/graf-tyzdna-svetove-akciove-indexy-maju-vyssi-podiel-klesajucich-aktiv.html",
        questions: [
            "Koľko bola hodná národná značka Číny",
            "Akú hodnotu mala značka Nemecka",
            "Akú hodnotu mala značka Slovakia",
            "Ktorá krajina V4 mala najhodnotnejšiu národnú značku"
        ]
    }
];

Scenario('Try semantic search', ({ I, DT, Document }) => {
    I.amOnPage("/apps/vyhladavanie/semanticke-vyhladavanie.html?");
    I.waitForVisible("#searchWords", 5);

    searchData.forEach(({ pageName, pageUrl, questions }) => {
        questions.forEach(question => {
            I.fillField('#searchWords', question);
            I.click(".smallSearchSubmit");
            I.waitForElement("h1.searchResultsH1", 20);
            checkTopFind(I, pageUrl, pageName);
            I.wait(1);
        });
    });
});

Scenario('Remove search preference', ({ I, DT, Document }) => {
    Document.setConfigValue("spamProtectionTimeout-search", "10");
    Document.setConfigValue("searchType", "db");
    Document.setConfigValue("ragSemanticSearchEnabled", "false");
});

function checkTopFind(I, pageUrl, pageName) {
    I.seeElement( "div.search p:nth-child(2) a[href='" + pageUrl + "']" )
    I.seeElement( locate("div.search p:nth-child(2) strong").withText(pageName) )
}