Feature('apps.gallery.gallery-access');

const DT = require("../../../pages/DT");
const DTE = require("../../../pages/DTE");

var randomNumber,
     mainFolder,
     subfolder1,
     subfolder2,
     allowFolder,
     testFolder,
     addFolder = (locate('.col-md-4.tree-col').find('button.btn.btn-sm.buttons-create.btn-success.buttons-divider')),
     deleteBtn = (locate('.col-md-4.tree-col').find('button.btn.btn-sm.buttons-selected.buttons-remove.btn-danger.buttons-divider')),
     userName = "Tester_L2 Playwright",
     userLogin = "tester3",
     testImageName = "puppy-2785074.jpg";

Before(({ I }) => {
     // prihlas sa ako iny pouzivatel (aby v pripade padov testov nepadali vsetky testy pod uzivatelom Tester Playwright)
     I.relogin(userLogin);
     if (typeof randomNumber === 'undefined') {
          randomNumber = I.getRandomText();
          mainFolder = 'autotest-' + randomNumber;
          subfolder1 = 'subfolder1-' + randomNumber;
          subfolder2 = 'subfolder2-' + randomNumber;
          allowFolder = 'allowfolder-' + randomNumber;
          testFolder = 'autotest-testfolder-' + randomNumber;
     }
});

// pridanie prav uzivatelovi - zaklad
function openUserSettingsDialog(I) {
     I.click(locate('.btn.btn-sm.btn-outline-secondary.js-profile-toggler').withText(userName));
     I.click(locate("a.dropdown-item").withText('Profil'));
     I.switchTo("#modalIframeIframeElement");
     I.waitForText(userLogin, 60);
     I.waitForElement("#pills-dt-datatableInit-rightsTab-tab", 10);
     I.click('#pills-dt-datatableInit-rightsTab-tab');
}

// skontroluj ci mam prava na galeriu
async function setGalleryAccess(I) {
     openUserSettingsDialog(I);

     const redGallery = locate('#perms_menuGallery a.jstree-clicked');
     const numVisible = await I.grabNumberOfVisibleElements(redGallery);
     I.say("numVisible: " + numVisible);

     if (numVisible === 0) {
          I.click(locate('#perms_menuGallery a'));
          confirmWindow(I);
     } else {
          confirmWindow(I);
     }
}

// skontroluj ci mam vsetky prava na vsetky adresare v galerii
async function allowAllFolders(I) {
     openUserSettingsDialog(I);
     let numVisible = await I.grabNumberOfVisibleElements("#editorAppDTE_Field_editorFields-writableFolders input.form-control");
     let failsafe = 0;
     while (numVisible > 0 && failsafe++ < 10) {
          I.say("numVisible: " + numVisible);
          I.click(locate('#editorAppDTE_Field_editorFields-writableFolders button.btn-vue-jstree-item-remove'));
          I.wait(0.5);
          numVisible = await I.grabNumberOfVisibleElements("#editorAppDTE_Field_editorFields-writableFolders input.form-control");
     }
     confirmWindow(I);
}

// vytvorenie hlavneho priecinka - autotestFolder
function createMainFolder(I, folderName) {
     I.click(addFolder);
     I.waitForVisible('#galleryDimensionDatatable_modal', 15);
     I.fillField('#DTE_Field_name', folderName);
     DTE.save();
     I.seeElement(locate('a.jstree-anchor').withText(folderName));
}

// vytvorenie autotest_subfolder1 a autotest_subfolder2
function createSubfolders(I, createsubfolder) {
     I.click(mainFolder);
     I.click(addFolder);
     I.waitForVisible('#galleryDimensionDatatable_modal', 15);
     I.fillField('#DTE_Field_name', createsubfolder);
     DTE.save();
     I.seeElement(locate('a.jstree-anchor').withText(createsubfolder));
}

function openJstree(I, folder) {
     I.waitForElement(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(folder).find('.jstree-icon.jstree-ocl'), 10);
     I.click(locate('.jstree-node.jstree-closed').withDescendant('a.jstree-anchor').withText(folder).find('.jstree-icon.jstree-ocl'));
}

// vyber priecinok, na ktory mi budu pridelene prava
function limitFolderAccess(I, allowedFolder) {
     I.clickCss("#editorAppDTE_Field_editorFields-writableFolders button.btn-vue-jstree-add"); // nahravanie suborov do adresarov
     I.waitForElement("#custom-modal-id", 10);
     I.waitForVisible('#jsTree', 10);
     openJstree(I, "images");
     openJstree(I, "gallery");
     openJstree(I, mainFolder);

     I.waitForElement(locate('.jstree-node a.jstree-anchor').withText(allowedFolder), 10);
     I.click(locate('.jstree-node a.jstree-anchor').withText(allowedFolder));

     //I.seeInField("#editorAppDTE_Field_editorFields-writableFolders input.form-control", allowedFolder);
}

// test nedovolene vytvorenie priecinka
function checkFolderAddDenied(I) {
     I.click(addFolder);
     I.waitForVisible('#galleryDimensionDatatable_modal', 15);
     I.fillField("#DTE_Field_name", mainFolder);
     DTE.save();
     I.seeElement('#galleryDimensionDatatable_modal');
     I.see('Tento priečinok nie je možné upravovať.'); // TODO chyba 'o' v slove "pristupve"
     DTE.cancel();
}

function checkFolderIconsDenied(I) {
     I.waitForElement(".tree-col div.dt-buttons button.buttons-create.disabled", 3);
     I.seeElement(".tree-col div.dt-buttons button.buttons-edit.disabled");
}

// povoleny priecinok
function allowedFolder(I) {
     I.click(addFolder);
     I.waitForVisible('#galleryDimensionDatatable_modal', 15);
     I.fillField('#DTE_Field_name', allowFolder);
     DTE.save();
     I.dontSee('Tento priečinok nie je možné upravovať.');
     I.click(addFolder);
     I.waitForVisible('#galleryDimensionDatatable_modal', 15);
     I.fillField('#DTE_Field_name', allowFolder);
     DTE.save();
     I.see('Zadaný názov galérie už existuje');
     DTE.cancel();
}

// vymazanie vytvorenych priecinkov
function deleteFolder(I, folderName) {
     I.say("Deleting folder: " + folderName);
     if (folderName === subfolder1 || folderName === subfolder2) {
          I.jstreeClick(mainFolder);
     }

     I.jstreeClick(folderName);
     I.waitForVisible(locate('.jstree-anchor.jstree-clicked').withText(folderName), 10);
     I.click(deleteBtn);
     DTE.waitForEditor("galleryDimensionDatatable");
     DTE.save();
     DTE.waitForModalClose("galleryDimensionDatatable_modal");
     DT.waitForLoader();

     if (folderName === subfolder1 || folderName === subfolder2) {
          I.jstreeClick(mainFolder);
     }

     I.dontSeeElement(locate('a').withText(folderName));
}

// potvrdenie modalneho okna, kde sa nastavuju prava
function confirmWindow(I) {
     I.switchTo();
     I.click({ css: "#modalIframe div.modal-footer button.btn.btn-primary" });
     I.waitForInvisible('#modalIframe', 30);
}

function uploadImage(I, folder, image) {
     I.say('Nahravam obrazok ' + image + ' do priecinka ' + folder);
     if (folder === subfolder1 || folder === subfolder2) {
          I.click(mainFolder);
          I.waitForElement(locate('a.jstree-anchor').withText(folder), 10);
     }

     I.click(folder);
     I.waitForElement(locate('a.jstree-anchor.jstree-clicked').withText(folder), 10);
     I.attachFile('input.dz-hidden-input', 'tests/apps/gallery/' + image);
     I.waitForVisible('#upload-wrapper', 25);
     I.waitForElement(locate('.toast-message').withText(image), 10);

     if (folder === subfolder2 || folder === testFolder) {
          // over, ze do podpriecinka autotest-subfolder2 sa da pridat obrazok
          I.waitForVisible('.ti.ti-circle-check.float-end', 25);
          I.dontSeeElement(locate('.toast-error-message').withText('Do tohto adresára nemôžete nahrávať súbory'), 10);
          I.click('#upload-wrapper-close');
          I.waitForInvisible('#upload-wrapper', 15);
          I.seeElement(locate('#galleryTable td.dt-row-edit a').withText(image));
     } else {
          // over, ze do ostatnych priecinkov nemozno pridat obrazok
          I.waitForVisible('.ti.ti-alert-circle.float-end', 25);
          I.waitForElement(locate('.toast-error-message').withText('Do tohto adresára nemôžete nahrávať súbory'), 10);
          I.click('#upload-wrapper-close');
          I.waitForInvisible('#upload-wrapper', 15);
          I.dontSeeElement(locate('#galleryTable td.dt-row-edit a').withText(image));
     }
}

async function resetGalleryAccess(I) {
     I.say("RESETING: allow gallery access if previous test failed");
     I.amOnPage('/admin/v9/');
     await setGalleryAccess(I);
     await allowAllFolders(I);
}

Scenario('Zakazane prava na galeriu', async ({ I }) => {
     // over ci mam prava na galeriu
     I.say('Over ci mam prava na galeriu');
     await setGalleryAccess(I);

     I.amOnPage('/admin/v9/apps/gallery/');
     I.refreshPage();

     // zakaz prava na Galeriu
     I.say('Zmen uzivatelovi prava - zakaz prava na galeriu');
     openUserSettingsDialog(I);
     I.click(locate('#perms_menuGallery a.jstree-clicked'));
     confirmWindow(I);

     // over ze nemam povoleny pristup do galerie
     I.say('Over ze nemam povoleny pristup do galerie');
     I.refreshPage();
     I.see('nemáte prístupové práva');

     // vrat prava uzivatelovi spat
     I.say('Vrat prava uzivatelovi spat');
     I.amOnPage('/admin/v9/');
     await setGalleryAccess(I);

     // skontroluj ci je galeria opat dostupna
     I.say('Skontroluj ci je galeria opat dostupna');
     I.amOnPage('/admin/v9/apps/gallery/');
     I.waitForVisible('#SomStromcek', 5);
     I.dontSeeElement('#j1_loading');
     I.dontSeeElement('#datatableInit_processing');
     I.seeElement(locate('a').withText('gallery'));
});

Scenario('Reset gallery access', async ({ I }) => {
     await resetGalleryAccess(I);
});

Scenario('Zakazany priecinok v galerii', async ({ I }) => {
     // skontroluj ci mam prava na vsetky priecinky v galerii
     I.say('Skontroluj ci mam prava na vsetky priecinky v galerii');
     await allowAllFolders(I);

     I.amOnPage('/admin/v9/apps/gallery/');

     // vytvor hlavny priecinok autotestFolder, podpriecinky autotest_subfolder1 a autotest_subfolder2
     I.say('Vytvaram podpriecinky do priecinka autotestfolder');
     createMainFolder(I, mainFolder);
     createSubfolders(I, subfolder1);
     createSubfolders(I, subfolder2);

     // zmen uzivatelovi prava - prava ma len na priecinok autotest_subfolder2
     I.say('Zmen uzivatelovi prava - zakazane prava na priecinok autotestFolder v galerii');

     openUserSettingsDialog(I);
     limitFolderAccess(I, subfolder2);
     confirmWindow(I);

     //
     I.say("check backend - we can't refresh page in this test otherwise icons will be denied");
     checkFolderAddDenied(I);

     //
     I.say("Check upload image without refresh page");
     // do autotest_subfolder2 sa da pridat obrazok
     uploadImage(I, subfolder2, testImageName);

     // do ostatnych adresarov sa neda pridat obrazok
     uploadImage(I, subfolder1, testImageName);
     uploadImage(I, mainFolder, testImageName);

     // ------------------------------------------ PRIDANIE NOVEHO FOLDRA ----------------------------------------------
     // over ze do foldra autotestfolder nemozno pridavat novy folder autotest-allowfolder
     I.say('Over ze do foldra autotest nemozno pridavat novy folder');
     I.refreshPage();
     DT.waitForLoader();
     I.jstreeClick(mainFolder);
     checkFolderIconsDenied(I);

     // over ze do foldra autotest_subfolder nemozno pridavat novy folder autotest-allowfolder
     I.say('Over ze do foldra autotest_subfolder1 nemozno pridavat novy folder');
     I.jstreeClick(subfolder1);
     checkFolderIconsDenied(I);

     // over ze do foldra autotest_subfolder2 je mozne pridavat novy folder autotest-allowfolder
     I.say('Over ze do foldra autotest_subfolder2 je mozne pridavat novy folder autotest-allowfolder');
     I.jstreeClick(subfolder2);
     allowedFolder(I, allowFolder);

     // vrat prava spat t.j. prava na vsetky foldre
     I.say('Vrat uzivatelovi spat prava');
     await allowAllFolders(I);
});

Scenario("Delete folders 1", async ({ I }) => {
     await resetGalleryAccess(I)
     I.amOnPage('/admin/v9/apps/gallery/');

     // vymaz subfolder2 + hlavny priecinok autotestfolder
     I.say('Vymaz subfolder2 + hlavny priecinok autotestfolder');
     deleteFolder(I, subfolder2);
     deleteFolder(I, mainFolder);
});

Scenario('Duplikovanie suboru v galerii - drag&drop', async ({ I }) => {
     // skontroluj ci mam prava na vsetky priecinky v galerii
     I.say('Skontroluj ci mam prava na vsetky priecinky v galerii');
     await allowAllFolders(I);

     I.amOnPage('/admin/v9/apps/gallery/');
     I.refreshPage();

     // vytvor hlavny priecinok + podpriecinky autotest_subfolder1 a autotest_subfolder2
     I.say('Vytvaram hlavny priecinok + podpriecinky do priecinka galeria');
     createMainFolder(I, mainFolder);
     createSubfolders(I, subfolder2);

     // vytvor dalsi priecinok test-folder
     I.say('Vytvor dalsi priecinok test-folder');
     I.refreshPage();
     createMainFolder(I, testFolder);
     uploadImage(I, testFolder, testImageName);

     // zmen uzivatelovi prava - povolene prava na priecinok autotest_subfolder2 v galerii
     I.say('Zmen uzivatelovi prava - povolene prava na priecinok autotest/autotest_subfolder2 v galerii');
     openUserSettingsDialog(I);
     limitFolderAccess(I, subfolder2);
     limitFolderAccess(I, testFolder);
     confirmWindow(I);

     // ---------------------------- DRAG AND DROP SUBORU ------------------------------------------
     I.say('Drag and drop suboru z test priecinka do hlavneho autotestfolder priecinka');
     I.refreshPage();
     I.forceClick("#treeAllowDragDrop"); // povol presuvanie suborov
     I.wait(0.5);
     I.seeCheckboxIsChecked('#treeAllowDragDrop');

     // priecinok test-folder sa da premiestnit do priecinka autotest_subfolder2
     I.say('Priecinok ' + testFolder + ' sa da premiestnit do priecinka ' + subfolder2);
     I.click(mainFolder);
     I.waitForElement(locate('a').withAttr({ id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '_anchor' }));
     const selector = locate('//li[contains(@class, "jstree-leaf")]')
          .withChild(locate('a').withAttr({
               id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '_anchor'
          }));

     let maxIterations = 3;
     let failsafe = 0;
     while (await I.grabNumberOfVisibleElements(selector) > 0 && failsafe++ < maxIterations) {
         I.dragAndDrop(
             locate('a').withAttr({ id: '/images/gallery/' + testFolder + '_anchor' }),
             locate('a').withAttr({ id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '_anchor' })
         );
         I.wait(4);
     }
     I.dontSee("Tento priečinok nie je možné upravovať.");
     I.dontSeeElement(locate('li').withAttr({ id: '/images/gallery/' + testFolder}));
     I.click(subfolder2);
     I.wait(2);
     I.see("Záznamy 0 až 0 z 0", "#galleryTable_info");
     I.click(testFolder);
     I.waitForElement(locate('#galleryTable td.dt-row-edit a').withText(testImageName), 10);

     I.click(subfolder2);
     I.waitForElement(locate('li').withAttr({ id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '/' + testFolder }), 15);

     // subor by sa nemal dat premiestnit z priecinka autotest_subfolder2 do priecinka test-folder - zakazali sme prava na zaciatku testu
     I.say('Subor by sa nemal dat premiestnit z priecinka ' + mainFolder + '/' + subfolder2 + '/' + testFolder + ' do priecinka ' + mainFolder);
     I.dragAndDrop(
          locate('a').withAttr({ id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '/' + testFolder + '_anchor' }),
          locate('a').withAttr({ id: '/images/gallery/' + mainFolder + '_anchor' }),
          { force: true });

     I.wait(4);

     I.waitForElement("#toast-container-webjet", 10);
     I.see("Tento priečinok nie je možné upravovať.", "#toast-container-webjet");
     I.seeElement(locate('li').withAttr({ id: '/images/gallery/' + mainFolder + '/' + subfolder2 + '/' + testFolder }));
     I.dontSeeElement(locate('li').withAttr({ id: '/images/gallery/' + mainFolder + '/' + testFolder }));
});

Scenario("Delete folders 2", async ({ I }) => {
     await resetGalleryAccess(I);
     I.relogin("admin");
     I.amOnPage('/admin/v9/apps/gallery/');
     deleteFolder(I, mainFolder);
});
