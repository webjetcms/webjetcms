Feature('rag.embedding-chunks');

Before(({ I, login }) =>{
    login('admin');
});

const rootDirId = "64071";

Scenario('Allow rag semantic search', ({ I, DT, DTE, Document }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + rootDirId);
    DT.waitForLoader();

    Document.screenshotElement(".tree-col button.buttons-index", "/redactor/apps/semantic-search/redirect-button.png");

    I.clickCss(".tree-col button.buttons-index");
    DT.waitForLoader();

    I.resizeWindow(1920, 800);
    Document.screenshot("/redactor/apps/semantic-search/datatable.png");

    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");

    Document.screenshotElement("#modalIframe div.modal-content", "/redactor/apps/semantic-search/index-dialog.png");

    I.clickCss("#modalIframe .modal-header button.btn-close");
    I.waitForInvisible("#modalIframeIframeElement");

    I.clickCss("button.btnRemoveIndex");
    I.waitForVisible("#modalIframeIframeElement");

    Document.screenshotElement("#modalIframe div.modal-content", "/redactor/apps/semantic-search/remove-index-dialog.png");
});