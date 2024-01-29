Feature('webpages.html-editor');

Before(({ I, login }) => {
    login('admin');
});

function testHtmlEditor(I) {
    I.say("Testing HTML editor");
    I.waitForElement("div.CodeMirror-code", 10);
    I.waitForText("<!DOCTYPE html>", 10, "div.CodeMirror-code pre.CodeMirror-line span span");
    I.see("head", "div.CodeMirror-code pre.CodeMirror-line span span.cm-tag");
    I.see("title", "div.CodeMirror-code pre.CodeMirror-line span span.cm-tag");
    I.see("body", "div.CodeMirror-code pre.CodeMirror-line span span.cm-tag");
    I.see("Welcome to Your Website", "div.CodeMirror-code pre.CodeMirror-line span");
    I.see("</", "div.CodeMirror-code pre.CodeMirror-line span span.cm-tag.cm-bracket");
}

Scenario('Edit webpage in html mode', ({ I, DT, DTE }) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=79020');
    DTE.waitForEditor();
    testHtmlEditor(I);

    I.say("Saving editor and realoading");
    DTE.save();
    DT.waitForLoader();
    I.click("Testovaci newsletter - HTML");
    DTE.waitForEditor();
    testHtmlEditor(I);
});