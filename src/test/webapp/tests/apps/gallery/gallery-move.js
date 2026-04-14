Feature('apps.gallery.gallery-move');

var randomNumber;
var autoName;

Before(({ I, DT, login }) => {
    login('admin');
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomTextShort();
        autoName = 'autotest-' + randomNumber;
    }
});


const testDirName = "moving-dir";
const dirFrom = "/images/gallery/test/move-from";
const dirTo = "/images/gallery/test/move-to";
Scenario('Gallery - Feature - select dir location during create', ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/apps/gallery/?dir=" + dirTo);
    DT.waitForLoader();
    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor("galleryDimensionDatatable");

    I.fillField("#DTE_Field_name", testDirName);

    I.say("Check location and change it");
    I.seeElement(".DTE_Field_Name_parent .vueComponent input.form-control[value='" + dirTo + "']");

    I.click("button.btn-vue-jstree-item-edit");
    I.waitForVisible("#jsTree");
    I.click(locate('.jsTree-wrapper .jstree-node.jstree-closed').withText('test').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jsTree-wrapper #jsTree a.jstree-anchor').withText('move-from'));
    I.waitForInvisible("#jsTree");

    I.say("Check value was change and we that we DONT see option updateInDoc");
    I.seeElement(".DTE_Field_Name_parent .vueComponent input.form-control[value='" + dirFrom + "']");
    I.dontSeeElement(".DTE_Field_Name_updateInDoc"); // dont see because its create not edit
    DTE.save();

    I.say('Test created dir');
    I.amOnPage("/admin/v9/apps/gallery/?dir=" + dirFrom + "/" + testDirName);
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("galleryDimensionDatatable");
    I.seeInField("#DTE_Field_name", testDirName);
    I.seeElement(".DTE_Field_Name_parent .vueComponent input.form-control[value='" + dirFrom + "']");
});

const movedImageId = "46";
const docBody_1 = `!INCLUDE(/components/gallery/gallery.jsp, style=photoSwipe, dir=&quot;/images/gallery/test/move-from/moving-dir&quot;, recursive=false, itemsOnPage=10, orderBy=title, orderDirection=asc, thumbsShortDescription=true, shortDescription=true, longDescription=true, author=true, perexGroup=, appHideFields=)! <p><img alt="" class="img-responsive img-fluid" src="/images/gallery/test/move-from/moving-dir/koala-sk.jpg?v=1775635997" title="koala sk | WebJET CMS" />&nbsp;</p>`;
Scenario('Gallery - Feature - allow relocated folder in editor AND handle doc update / url redirect - BEFORE', async ({ I, DT, DTE, Apps }) => {
    I.say("Duplicate image to our test folder");
    I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/test");

    I.waitForElement("button.btn-gallery-size-table", 5);
    I.wait(1);
    I.clickCss("button.btn-gallery-size-table");

    DT.filterId("id", movedImageId);
    I.clickCss("td.sorting_1");
    I.click(DT.btn.gallery_duplicate_button);
    DTE.waitForEditor("galleryTable");

    I.clickCss("#pills-dt-galleryTable-metadata-tab");

    I.click(".DTE_Field_Name_editorFields\\.imagePath button.btn-vue-jstree-item-edit");
    I.waitForVisible("#jsTree");
    I.click(locate('.jsTree-wrapper .jstree-node.jstree-closed').withText('test').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jsTree-wrapper .jstree-node.jstree-closed').withText('move-from').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jsTree-wrapper #jsTree a.jstree-anchor').withText('moving-dir'));
    I.waitForInvisible("#jsTree");
    DTE.save();

    I.say("Prepare page for test");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=160695");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    Apps.switchEditor("html");
    await DTE.fillCkeditor(docBody_1);
    DTE.save();

    checkPage(I);
});

Scenario('Gallery - Feature - allow relocated folder in editor AND handle doc update / url redirect', ({ I, DT, DTE }) => {
    I.say("Go change dir parent");
    I.amOnPage("/admin/v9/apps/gallery/?dir=" + dirFrom + "/" + testDirName);
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("galleryDimensionDatatable");
    DTE.seeInField("path", "/images/gallery/test/move-from/moving-dir");

    I.click("button.btn-vue-jstree-item-edit");
    I.waitForVisible("#jsTree");
    I.click(locate('.jsTree-wrapper .jstree-node.jstree-closed').withText('test').find('.jstree-icon.jstree-ocl'));
    I.click(locate('.jsTree-wrapper #jsTree a.jstree-anchor').withText('move-to'));
    I.waitForInvisible("#jsTree");

    I.say('Check');
    DTE.seeInField("path", "/images/gallery/test/move-to/moving-dir");
    I.seeElement(".DTE_Field_Name_parent .vueComponent input.form-control[value='" + dirTo + "']");
    I.seeElement(".DTE_Field_Name_updateInDoc");
    I.seeCheckboxIsChecked("#DTE_Field_updateInDoc_0");
    DTE.save();

    I.say('Check nitification')
    I.waitForVisible("#toast-container-webjet .toast-warning", 10);
    I.seeTextEquals("Presunutie priečinka", "#toast-container-webjet .toast-title")
    I.seeTextEquals("Táto akcia môže trvať dlhšie, pretože systém musí aktualizovať všetky odkazy na webových stránkach, ktoré smerujú na túto galériu.", "#toast-container-webjet .toast-message")
});

Scenario('Gallery - Feature - allow relocated folder in editor AND handle doc update / url redirect - AFTER', async ({ I, DT, DTE, Apps }) => {
    I.say("Check that redirect was added");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterEquals("newUrl", "/images/gallery/test/move-to/moving-dir/$1");
    I.see("/images/gallery/test/move-to/moving-dir/$1");
    I.see("301");
    I.see("demo.webjetcms.sk");

    I.say("Check updated page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=160695");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    Apps.switchEditor("html");
    I.see("dir=&quot;/images/gallery/test/move-to/moving-dir&quot;,");
    I.see('src="/images/gallery/test/move-to/moving-dir/koala-sk.jpg')

    I.say("Check taht page works");
    checkPage(I, true);

    I.say("Change page body, use BAD URL and check that redirect works");
    const docBody_2 = docBody_1.replaceAll("/images/gallery/test/move-from/moving-dir", "/images/gallery/test/move-to/moving-dir");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=160695");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    Apps.switchEditor("html");
    await DTE.fillCkeditor(docBody_2);
    DTE.save();

    checkPage(I, true);
});

Scenario('Gallery - Feature - to default again', ({ I, DT }) => {
    I.say("Remove redirect");
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.filterEquals("newUrl", "/images/gallery/test/move-to/moving-dir/$1");
    I.clickCss("button.buttons-select-all");
    I.clickCss("button.buttons-remove");
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Remove gallery folder");
    I.amOnPage("/admin/v9/apps/gallery/?dir=" + dirTo + "/" + testDirName);
    I.click(DT.btn.tree_delete_button);
    I.waitForElement("div.DTE_Action_Remove");
    I.click("Zmazať", "div.DTE_Action_Remove");
});


function checkPage(I, isMoved = false) {
    I.say("Check page");
    I.amOnPage("/apps/galeria/test-moved-gallery.html");

    // check title
    I.seeElement( locate("h1").withText("test-moved-gallery") );

    // check gallery content
    if(isMoved === true) {
        I.seeElement("#thumbs1 a[title='Koala sk'] img[src='/images/gallery/test/move-to/moving-dir/s_koala-sk.jpg']");
    } else {
        I.seeElement("#thumbs1 a[title='Koala sk'] img[src='/images/gallery/test/move-from/moving-dir/s_koala-sk.jpg']");
    }

    // Check image
    I.seeElement("p img.img-responsive[title='koala sk | WebJET CMS']");
}
