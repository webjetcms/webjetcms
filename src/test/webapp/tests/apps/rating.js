Feature('apps.rating');

var randomNumber;

var ratingForm = "!INCLUDE(/components/rating/rating_form.jsp, checkLogon=false, ratingDocId=REPLACE_THIS, range=10)!";
// var ratingPage = "!INCLUDE(/components/rating/rating_page.jsp, ratingDocId=REPLACE_THIS, range=10)!";
// var ratingTopUsers = "!INCLUDE(/components/rating/rating_top_users.jsp, usersLength=10)!";
// var ratingTopPages = "!INCLUDE(/components/rating/rating_top_pages.jsp, range=10, docsLength=10, period=7)!";


Before(({ I, login }) => {
    login('admin');

    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

async function fillDocBody(I, DTE, body) {
    I.waitForElement("iframe.cke_wysiwyg_frame");
    await DTE.fillCkeditor(body);
    DTE.save();
}

Scenario("Rating action testing", async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=57593");
    var docName = "Rating_autotest-" + randomNumber;

    //Crate new page
        I.click(DT.btn.add_button);
        DTE.waitForEditor();
        I.waitForElement("#DTE_Field_title");
        I.clickCss("#DTE_Field_title");
        I.fillField("#DTE_Field_title", docName);
        DTE.save();

    //Get created doc ID
        DT.filterContains("title", docName);
        I.see(docName);
        const docId = await I.grabTextFrom( "#datatableInit > tbody tr:nth-child(1) > td:nth-child(1) > div" );

        I.say(docId);

    //RATING FORM
        I.click(docName);
        DTE.waitForEditor();
        ratingForm = ratingForm.replace("REPLACE_THIS", docId);
        await fillDocBody(I, DTE, ratingForm);

        I.amOnPage("/apps/rating/" + docName.toLowerCase() + ".html");

        I.see("Hodnotenie: 0.0/10");
        I.see("Hlasovalo: 0");
        I.see("Hlasovať:");

        //should be 10
        const generatedStars = await I.grabNumberOfVisibleElements('div.star.star_live');
        I.assertEqual(generatedStars, 10);

        I.click( locate("div.star").withChild('a[title="6"]') );

        I.see("Hodnotenie: 6.0/10");
        I.see("Hlasovalo: 1")

        //SShould be 6
        const selectedStars = await I.grabNumberOfVisibleElements('div.star.star_live.star_on');
        I.assertEqual(selectedStars, 6);


    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=57593");
    DT.filterContains("title", docName);
    I.see(docName);
    I.clickCss("#datatableInit_wrapper .buttons-select-all");
    I.clickCss("#datatableInit_wrapper button.buttons-remove");
    DTE.waitForEditor();
    I.click("Zmazať", "div.DTE_Action_Remove");

    I.dontSee(docName);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario("Rating stat check testing", async ({ I}) => {
    //
    I.amOnPage("/apps/rating/");
    I.see("Hodnotenie stránky - Formulár");
    I.see("Hodnotenie: 6.5/10");
    I.see("Hlasovalo: 2");
    I.see("Vaše hlasovanie z 07.11.2023");

    //
    I.amOnPage("/apps/rating/rating-page.html");
    I.see("Hodnotenie stránky - Stav");
    I.see("Hodnotenie: 6.5/10");
    I.see("Hlasovalo: 2");
});