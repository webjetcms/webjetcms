Feature('webpages.webpage-preview');

var randomNumber;

Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
          randomNumber = I.getRandomText();
          I.say("randomNumber="+randomNumber);
     }
});

async function fillAndCheckPreview(I, DTE, text) {
     //await
     await DTE.fillCkeditor("<p>"+text+"</p>");

     I.click("button.btn-preview");

     I.wait(3);
     I.switchToNextTab();

     I.waitForText(text, 10);
     I.see(text);

     I.switchToPreviousTab();
}

Scenario('Otvorenie nahladu', async ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=259');
     DTE.waitForEditor();

     var text = "PREVIEW: "+randomNumber;

     await fillAndCheckPreview(I, DTE, text);

     await fillAndCheckPreview(I, DTE, "v2"+text);

     //over aktualizaci pri ulozeni pracovnej verzie
     text = "working copy "+text;
     I.wait(3);
     //await
     DTE.fillCkeditor("<p>"+text+"</p>");
     I.checkOption("#webpagesSaveCheckbox");
     I.wait(3);
     //DTE.save();
     I.forceClick({ css: "div.DTED.show div.DTE_Footer.modal-footer button.btn.btn-primary" });
     DTE.waitForLoader();
     I.wait(3);

     I.switchToNextTab();
     I.waitForText(text, 10);
     I.see(text);
     I.closeCurrentTab();

     DTE.cancel();
});

Scenario('Uloz cistu stranku', async ({ I, DTE }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=259');
     DTE.waitForEditor();

     I.wait(10);

     await DTE.fillCkeditor("<p>&nbsp;</p>");

     DTE.save();

     I.closeOtherTabs();
});


Scenario('Preview of new page', async ({ I, DTE, DT }) => {
     I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=0');

     I.click(DT.btn.add_button);
     DTE.waitForEditor();
     I.waitForElement("#pills-dt-datatableInit-basic-tab.active");
     I.clickCss("#pills-dt-datatableInit-content-tab");

     I.wait(10);
     //await
     await fillAndCheckPreview(I, DTE, "asdfghjkl");

     I.closeOtherTabs();
});
