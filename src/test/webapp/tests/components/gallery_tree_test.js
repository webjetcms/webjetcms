Feature('components.gallery_tree_test');

var randomNumber = (new Date()).getTime();
var dragdrop = "dragdrop-" + randomNumber;
var dragdropSubfolder1 = "dragdrop-subfolder1-" + randomNumber;
var dragdropSubfolder2 = "dragdrop-subfolder2-" + randomNumber;

Before(( { login } ) => {
    login('admin');
});

/*
Scenario('zoznam fotografii', (I) => {
    I.amOnPage("/admin/v9/apps/gallery");
    I.click("test");
    I.see("koala.jpg");
});
 */

Scenario('Vytvorenie, presun a zmazanie adresaru', ({ I, DTE }) => {
    // vytvorenie adresaru
    I.amOnPage("/admin/v9/apps/gallery");
    I.wait(3);
    I.click(".tree-col .buttons-create");
    I.waitForVisible('#galleryDimensionDatatable_modal');

    I.wait(1);

    I.fillField("#DTE_Field_name", dragdrop);
    DTE.save();

    I.click(dragdrop);

    // vytvorenie podadresaru
    I.click(".tree-col .buttons-create");
    I.waitForVisible('#galleryDimensionDatatable_modal');
    I.wait(2);
    I.fillField("#DTE_Field_name", dragdropSubfolder1);
    DTE.save();

    // vytvorenie dalsieho podadresaru
    I.click(".tree-col .buttons-create");
    I.waitForVisible('#galleryDimensionDatatable_modal');
    I.wait(1);
    I.fillField("#DTE_Field_name", dragdropSubfolder2);
    DTE.save();

    //povol drag & drop
    I.forceClick("#treeAllowDragDrop");

    // overis ze dragdrop-timestamp nejde dropnut do dragdrop-subfolder1-timestamp
    //I.click(locate("li").withAttr({ id: '/images/gallery/' + dragdrop }).find("i"));
    //I.wait(1);

    I.dragAndDrop(
        { id: '\\/images\\/gallery\\/' + dragdrop + '_anchor'},
        { id: '\\/images\\/gallery\\/' + dragdrop + '\\/' + dragdropSubfolder1 + '_anchor'},
        { force: true }
    );

    I.seeElement({ id: '\\/images\\/gallery\\/' + dragdrop });

    I.wait(2);

    // dokazes dropnut dragdrop-subfolder2-timestamp do dragdrop-subfolder1-timestamp
    I.dragAndDrop(
        { id: '\\/images\\/gallery\\/' + dragdrop + '\\/' + dragdropSubfolder2 + '_anchor'},
        { id: '\\/images\\/gallery\\/' + dragdrop + '\\/' + dragdropSubfolder1 + '_anchor'},
        { force: true }
    );

    I.wait(2);

    I.click({ id: '\\/images\\/gallery\\/' + dragdrop + '\\/' + dragdropSubfolder1});

    I.wait(2);

    I.seeElement({ id: '\\/images\\/gallery\\/' + dragdrop + '\\/' + dragdropSubfolder1 + "\\/" + dragdropSubfolder2 });

    // zmazanie
    I.click(dragdrop);
    I.click("button.buttons-remove");
    I.waitForVisible('#galleryDimensionDatatable_modal');
    DTE.save();
});