Feature("webpages.dirty");

Before(({ I, login }) => {
    login('admin');
});

function checkDirty(docId, expectedDirtyState, isPagebuilder, I, DTE) {
    I.say("checkDirty, docId=" + docId + ", expectedDirtyState=" + expectedDirtyState, ", isPagebuilder=" + isPagebuilder);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
    DTE.waitForCkeditor();
    I.wait(10);

    I.amCancellingPopups();

    //cant't use DTE.cancel() because it waits for modal close which does not happen when dirty
    let cancelLocator = locate("div.DTED.show div.DTE_Footer.modal-footer button.btn-close-editor");

    if (expectedDirtyState) {
        if (isPagebuilder) {
            I.waitForElement("#DTE_Field_data-pageBuilderIframe");
            I.switchTo("#DTE_Field_data-pageBuilderIframe");
            I.waitForElement(locate("h3").withText("Etiam orci"), 10);
            I.click(locate("h3").withText("archiv"), null, {position: {x:130, y:10}});
        } else {
            I.waitForElement('#trEditor', 10);
            I.clickCss('#trEditor');
            I.wait(2);
            I.clickCss('#trEditor');
        }
        I.pressKey('x');
        I.pressKey('y');
        I.pressKey('z');
        I.switchTo();

        //
        I.say("Checking that dirty state is detected - editor should stay open on cancel");
        I.click(cancelLocator);
        I.wait(3);
        DTE.waitForEditor();

        //
        I.say("Checking for dirty - accpeting popup - editor should close");
        I.amAcceptingPopups();
        I.click(cancelLocator);
        DTE.waitForModalClose();

    } else {
        I.click(cancelLocator);
        DTE.waitForModalClose();
    }

}

Scenario("check dirty - normalpage", ({ I, DTE }) => {
    checkDirty(25, false, false, I, DTE);
    checkDirty(25, true, false, I, DTE);
});

Scenario("check dirty - pagebuilder", ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();
    checkDirty(57, false, true, I, DTE);
    checkDirty(57, true, true, I, DTE);
});

Scenario("reset", ({ I }) => {
    I.amAcceptingPopups();
});