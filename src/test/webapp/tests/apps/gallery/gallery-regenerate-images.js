Feature('apps.gallery.gallery-regenerate-images');

const DT = require("../../../pages/DT");
const DTE = require("../../../pages/DTE");


Before(({ login }) => {
     login('admin');
});

// otvorenie foldra v galerii
function modalWindow(I, DT, folder) {
     I.jstreeClick(folder);
     DT.waitForLoader();
     I.click(DT.btn.tree_edit_button);
     DTE.waitForEditor("galleryDimensionDatatable");
     I.seeInField('#DTE_Field_name', folder);
     I.clickCss('#pills-dt-galleryDimensionDatatable-sizes-tab'); // tab rozmery
     I.waitForVisible('#panel-body-dt-galleryDimensionDatatable-sizes', 10);
}

// Rozmery - sposob zmeny velkosti, zaskrtnutie checkboxov: pregenerovat obrazky, aplikovat na vsetky podpriecinky
function checkboxPicture(I, action) {
     I.selectOption('#DTE_Field_resizeMode', action); // napr. orezat na mieru
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-regenerateImages_0').find('.form-check-label')); // pregenerovat obrazky
     I.wait(0.5);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-regenerateImages_0');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceResizeModeToSubgroups_0').find('.form-check-label')); // aplikovat na vsetky podpriecinky
     I.wait(0.5);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-forceResizeModeToSubgroups_0');
}

// Vodotlac - zaskrtnutie checkboxov: pregenerovat obrazky, aplikovat na vsetky podpriecinky
function checkboxWatermark(I) {
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-regenerateWatermark_0').find('.form-check-label')); // pregenerovat obrazky
     I.wait(0.5);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-regenerateWatermark_0');
     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-forceWatermarkToSubgroups_0').find('.form-check-label')); // aplikovat na vsetky podpriecinky
     I.wait(0.5);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-regenerateWatermark_0');
}

// Vodotlac - poloha tlace a sytost tlace
function watermarkAction(I, action, percentage) {
     DTE.selectOption('watermarkPlacement', action); // poloha vodotlace
     DTE.selectOption('watermarkSaturation', percentage); // sytost tlace
}

// pregeneruj rozmery obrazka
async function regenerateDimensions(I, dimension) {
     if (dimension === 'up') {
          // premenne - velkosti maleho aj velkeho obrazka
          const widthSmallImg = await I.grabValueFrom('#DTE_Field_imageWidth');
          const heightSmallImg = await I.grabValueFrom('#DTE_Field_imageHeight');
          const widthBigImg = await I.grabValueFrom('#DTE_Field_normalWidth');
          const heightBigImg = await I.grabValueFrom('#DTE_Field_normalHeight');

          checkboxPicture(I, 'Orezať na mieru');

          // Zvacsi velkost obrazka o 1
          I.say('Zvacsujem velkost o 1');
          I.fillField('#DTE_Field_imageWidth', Number(widthSmallImg) + 1);
          I.fillField('#DTE_Field_imageHeight', Number(heightSmallImg) + 1);
          I.fillField('#DTE_Field_normalWidth', Number(widthBigImg) + 1);
          I.fillField('#DTE_Field_normalHeight', Number(heightBigImg) + 1);

     } else if (dimension === 'down') {
          checkboxPicture(I, 'Zobrazenie na mieru');
     }
}

// pregeneruj vodotlac
async function regenerateWatermark(I, watermark) {
     var waterkarkFile = "/images/watermark.svg";
     I.clickCss('#pills-dt-galleryDimensionDatatable-watermark-tab');
     I.waitForVisible('#panel-body-dt-galleryDimensionDatatable-watermark', 15);

     if (watermark === 'change') {
          // zmen vodotlac - vodotlac, poloha a sytost, pregenerovat obrazky, aplikovat na vsetky podpriecinky
          I.say('Zmen vodotlac');
          I.fillField(locate('.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_watermark').find('input.form-control'), waterkarkFile);
          watermarkAction(I, 'Dole', '80%') // nastav vodotlac
          checkboxWatermark(I); // potvrd checkboxy

     } else if (watermark === 'original') {
          // nastav vodotlac na povodne nastavenia
          I.say('Nastav vodotlac na povodne nastavenia');
          I.clearField(locate('.DTE_Field.form-group.row.DTE_Field_Type_elfinder.DTE_Field_Name_watermark').find('input.form-control'));

          watermarkAction(I, 'Stred', '70%'); // nastav vodotlac

          checkboxWatermark(I); // potvrd checkboxy

     } else if (watermark === 'check') {
          // overenie ulozenia zmien
          I.say('Overenie ulozenia zmien');

          // skontroluj zmenu nastaveni
          I.seeInField('.DTE_Field_Name_watermark input.form-control', waterkarkFile); // over, ze v ceste pre vodotlac je vybrany obrazok
          I.seeElement(locate('.filter-option-inner-inner').withText('Dole'));
          I.seeElement(locate('.filter-option-inner-inner').withText('80%'));
          I.dontSeeCheckboxIsChecked('Pregenerovať obrázky');
          I.dontSeeCheckboxIsChecked('Aplikovať na všetky podpriečinky');
     }
     DTE.save();
     I.waitForVisible('#SomStromcek', 10);
}

Scenario('Galeria - pregenerovanie obrazkov a vodotlac', async ({ I, DT }) => {
     I.amOnPage("/admin/v9/apps/gallery/");
     DT.waitForLoader();

     // pregeneruj obrazky v priecinku watermark
     modalWindow(I, DT, 'watermark');

     // premenne - velkosti maleho aj velkeho obrazka
     const widthSmallImg = await I.grabValueFrom('#DTE_Field_imageWidth');
     const heightSmallImg = await I.grabValueFrom('#DTE_Field_imageHeight');
     const widthBigImg = await I.grabValueFrom('#DTE_Field_normalWidth');
     const heightBigImg = await I.grabValueFrom('#DTE_Field_normalHeight');

     await regenerateDimensions(I, 'up');

     // nastav vodotlac v hlavnom foldri - pregenerovat obrazky + aplikovat na vsetky podpriecinky
     await regenerateWatermark(I, 'change');

     // over zmeny v hlavnom priecinku (rozery a vodotlac)
     I.say('Over zmeny v hlavnom priecinku');
     modalWindow(I, DT, 'watermark');

     I.waitForElement(locate('.filter-option-inner-inner').withText('Orezať na mieru'));
     I.seeInField('#DTE_Field_imageWidth', Number(widthSmallImg) + 1);
     I.seeInField('#DTE_Field_imageHeight', Number(heightSmallImg) + 1);
     I.seeInField('#DTE_Field_normalWidth', Number(widthBigImg) + 1);
     I.seeInField('#DTE_Field_normalHeight', Number(heightBigImg) + 1);

     await regenerateWatermark(I, 'check');

     // over nastavenie 1. podpriecinka (rozery a vodotlac)
     I.say('Over zmeny v 1. podpriecinku');
     modalWindow(I, DT, 'subfolder1');

     I.waitForElement(locate('.filter-option-inner-inner').withText('Orezať na mieru'));
     I.seeInField('#DTE_Field_imageWidth', Number(widthSmallImg) + 1);
     I.seeInField('#DTE_Field_imageHeight', Number(heightSmallImg) + 1);
     I.seeInField('#DTE_Field_normalWidth', Number(widthBigImg) + 1);
     I.seeInField('#DTE_Field_normalHeight', Number(heightBigImg) + 1);

     await regenerateWatermark(I, 'check');

     // over nastavenie 2. podpriecinka (rozery a vodotlac)
     I.say('Over zmeny v 2. podpriecinku');
     modalWindow(I, DT, 'subfolder2');

     I.waitForElement(locate('.filter-option-inner-inner').withText('Orezať na mieru'));
     I.seeInField('#DTE_Field_imageWidth', Number(widthSmallImg) + 1);
     I.seeInField('#DTE_Field_imageHeight', Number(heightSmallImg) + 1);
     I.seeInField('#DTE_Field_normalWidth', Number(widthBigImg) + 1);
     I.seeInField('#DTE_Field_normalHeight', Number(heightBigImg) + 1);

     await regenerateWatermark(I, 'check');

     // vrat na povodne nastavenia cez hlavny priecinok a daj aplikovat na vsetky podpriecinky
     modalWindow(I, DT, 'watermark');

     await regenerateDimensions(I, 'down');

     I.say('Upravujem velkost na povodne nastavenia');
     I.fillField('#DTE_Field_imageWidth', widthSmallImg);
     I.fillField('#DTE_Field_imageHeight', heightSmallImg);
     I.fillField('#DTE_Field_normalWidth', widthBigImg);
     I.fillField('#DTE_Field_normalHeight', heightBigImg);

     await regenerateWatermark(I, 'original');
});

async function setResizeMode(resizeMode, resizeModeLabel, I, DT, DTE, Document, width=400, height=200) {
     I.amOnPage("/admin/v9/apps/gallery/?dir=/images/gallery/watermark/subfolder2");
     DT.waitForLoader();
     I.jstreeWaitForLoader();
     I.jstreeWaitForLoader();

     I.clickCss(".tree-col .dt-buttons button.buttons-edit");
     DTE.waitForEditor("galleryDimensionDatatable");
     I.clickCss('#pills-dt-galleryDimensionDatatable-sizes-tab');

     DTE.selectOption("resizeMode", resizeModeLabel); // napr. orezat na mieru

     I.click(locate('.custom-control.form-switch').withChild('#DTE_Field_editorFields-regenerateImages_0').find('.form-check-label')); // pregenerovat obrazky
     I.wait(0.5);
     I.seeCheckboxIsChecked('#DTE_Field_editorFields-regenerateImages_0');

     if (resizeMode !== null) {
          DTE.fillField("normalWidth", width);
          DTE.fillField("normalHeight", height);
     } else {
          DTE.fillField("normalWidth", "750");
          DTE.fillField("normalHeight", "560");
     }

     DTE.save();

     if (resizeMode !== null) {
          let sizeAppend = "-" + width + "x" + height;

          I.amOnPage('/images/gallery/watermark/subfolder2/puppy-2785074.jpg');
          await Document.compareScreenshotElement('img', 'resize-mode/puppy-'+resizeMode+sizeAppend+'.png', null, null, 6);
     }
}

Scenario("resize mode - A @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode('A', "Presný rozmer", I, DT, DTE, Document);
});
Scenario("resize mode - C @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode('C', "Orezať na mieru", I, DT, DTE, Document);
     await setResizeMode('C', "Orezať na mieru", I, DT, DTE, Document, 400, 400);
     await setResizeMode('C', "Orezať na mieru", I, DT, DTE, Document, 200, 400);
});
Scenario("resize mode - H @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode('H', "Presná výška", I, DT, DTE, Document);
});
Scenario("resize mode - N @current", async ({ I, DT, DTE, Document }) => {
     //DO NOT use N mode, it will change the original image and break other tests, so we will not automate it for now
});
Scenario("resize mode - S @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode('S', "Zobrazenie na mieru", I, DT, DTE, Document);
});
Scenario("resize mode - W @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode('W', "Presná šírka", I, DT, DTE, Document);
});
Scenario("resize mode reset @current", async ({ I, DT, DTE, Document }) => {
     await setResizeMode(null, "Zobrazenie na mieru", I, DT, DTE, Document);
});