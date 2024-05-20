Feature('apps.rating');

var doc_add_button = (locate("#datatableInit_wrapper > div.dt-header-row.clearfix.wp-header > div > div.col-auto > div > button.btn.btn-sm.buttons-create.btn-success"));

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

    I.selectOption('select[name=ratingType]', 'rating_page');
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-page_app.png");

    I.selectOption('select[name=ratingType]', 'rating_top_users');
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-top-users_app.png");

    I.selectOption('select[name=ratingType]', 'rating_top_pages_new');
    I.wait(1);
    Document.screenshot("/redactor/apps/rating/rating-top-pages_app.png");
});