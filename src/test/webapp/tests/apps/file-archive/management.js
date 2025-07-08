Feature('apps.file-archive.management');

const SL = require("./SL.js");

Before(({ login, DT }) => {
    login('admin');
    DT.addContext("category","#categoryManagerDataTable_wrapper");
    DT.addContext("product","#productManagerDataTable_wrapper");
    DT.addContext("archive","#fileArchiveDataTable_wrapper");
});

Scenario('File Archive Product & Category Management Test', ({ I, DT, DTE }) => {
    const productCategoryTestFileName = 'archive_file_test.pdf';
    const productCategoryTestVirtualFileName = SL.randomName("productcategorytest");
    const productName = SL.randomName("product");
    const categoryName = SL.randomName("category");

    // 1. Pridanie nového súboru do archívu
    I.say("Phase 1 - Add new file to archive");
    I.amOnPage(SL.fileArchive);
    SL.uploadFile(productCategoryTestVirtualFileName, productCategoryTestFileName);
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    DTE.fillField("product", productName);
    DTE.fillField("category", categoryName);
    DTE.save('fileArchiveDataTable');
    I.wait(2);

    // 2. Manažer produktov - úprava
    I.say("Phase 2 - Product Manager - edit");
    I.amOnPage("/apps/file-archive/admin/product/");
    manageProductCategory(I, DT, DTE, 'product', productName, 'edit');

    // 3. Manažer kategórií - úprava
    I.say("Phase 3 - Category Manager - edit");
    I.amOnPage("/apps/file-archive/admin/category/");
    manageProductCategory(I, DT, DTE, 'category', categoryName, 'edit');

    // 4. Overiť, že sa kategória aj produkt zmenili
    I.say("Phase 4 - Verify that both the category and product have changed");
    I.openNewTab();
    I.closeOtherTabs();
    I.amOnPage(SL.fileArchive);
    DT.filterContains("virtualFileName", productCategoryTestVirtualFileName);
    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.archive_edit_button);
    I.waitForElement("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.seeInField("#DTE_Field_product", productName + "-chan.ge");
    I.seeInField("#DTE_Field_category", categoryName + "-chan.ge");

    // 5. Odstránenie produktu
    I.say("Phase 5 - Deleting the product");
    I.amOnPage("/apps/file-archive/admin/product/");
    manageProductCategory(I, DT, DTE, 'product', productName, 'delete');

    // 6. Odstránenie kategórie
    I.say("Phase 6 - Deleting the category");
    I.amOnPage("/apps/file-archive/admin/category/");
    manageProductCategory(I, DT, DTE, 'category', categoryName, 'delete');

    // 7. Overiť zmazanie
    I.say("Phase 7 - Verify that both the category and product have been removed");
    I.amOnPage(SL.fileArchive);
    DT.filterContains("virtualFileName", productCategoryTestVirtualFileName);
    I.clickCss("button.buttons-select-all");
    I.click(DT.btn.archive_edit_button);
    I.waitForElement("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.clickCss("#pills-dt-fileArchiveDataTable-advanced-tab");
    I.seeInField("#DTE_Field_product", "");
    I.seeInField("#DTE_Field_category", "");
});

Scenario('Delete archiv entity (and file using elfinder if neccesary)', async ({ I }) => {
    SL.deleteTestFiles();

    const fileSelector = ".elfinder-cwd-filename[title^='archive_file_test']";
    let wasRemovedByElfinder = await SL.removeFileByElfinder(fileSelector);
    if (wasRemovedByElfinder){
        I.assertTrue(false, "The file was not removed by archive and had to be removed by elfinder!");
    }
});

function manageProductCategory(I, DT, DTE, entityType, entityName, action){
    DT.filterContains("name", entityName);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    if (action === "edit") {
        I.clickCss("button.buttons-select-all");
        I.click(DT.btn[`${entityType}_edit_button`]);
        DTE.waitForEditor(`${entityType}ManagerDataTable`);
        DTE.fillField("newName", entityName + "-chan.ge");
        I.click({ css: `#${entityType}ManagerDataTable_modal` + ".DTED.show div.DTE_Footer.modal-footer button.btn.btn-primary" });
        DT.waitForLoader();
        //DTE.save(`${entityType}ManagerDataTable`);
        DT.checkTableCell(`${entityType}ManagerDataTable`, 1, 2, entityName + "-chan.ge");
    } else if (action === "delete") {
        DT.deleteAll(`${entityType}ManagerDataTable`);
    }
};