Feature('apps.rating');

Before(({ login }) => {
    login('admin');
});

Scenario('rating screen', ({ I , DT, DTE, Document }) => {
    let confLng = I.getConfLng();
    let postfix = "";
    let postfix_lng = "";

    if(confLng === "sk") {
        I.amOnPage("/apps/rating/");
    } else if(confLng === "en") {
        I.amOnPage("/apps/rating/rating-en.html?language=en");
        postfix = "-en";
        postfix_lng = "?language=en";
    }
    else{
        I.amOnPage("/apps/rating/rating-cs.html?language=cs");
        postfix="-cs";
        postfix_lng="?language=cs";
    }

    DT.waitForLoader();
    Document.screenshot("/redactor/apps/rating/rating-form.png");

    I.amOnPage("/apps/rating/rating-page" + postfix + ".html" + postfix_lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/rating/rating-page.png");

    I.amOnPage("/apps/rating/rating-top-users" + postfix + ".html" + postfix_lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/rating/rating-top-users.png");

    I.amOnPage("/apps/rating/rating-top-pages" + postfix + ".html" + postfix_lng);
    DT.waitForLoader();
    Document.screenshot("/redactor/apps/rating/rating-top-pages.png");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=70839");
    DTE.waitForEditor();

    Document.editorComponentOpen();
    Document.screenshot("/redactor/apps/rating/rating-form_app.png");

    DTE.selectOption("ratingType", "Rating stránky");
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-page_app.png");

    DTE.selectOption("ratingType", "Top používatelia");
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-top-users_app.png");

    DTE.selectOption("ratingType", "Top stránky");
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-top-pages_app.png");
});