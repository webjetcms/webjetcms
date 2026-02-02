const { I } = inject();
const DT = require("../../../pages/DT.js");
const DTE = require("../../../pages/DTE.js");
const Document = require("../../../pages/Document.js")

module.exports = {

    // Constants
    fileArchive: '/apps/file-archive/admin/',
    elfinder: '/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL2FyY2hpdg_E_E',
    elfinderLater: '/admin/v9/files/index/#elf_iwcm_2_L2ZpbGVzL2FyY2hpdi9maWxlcy9hcmNoaXZfaW5zZXJ0X2xhdGVyL2ZpbGVzL2FyY2hpdg_E_E',

    red: 'rgb(255, 75, 88)',
    black: 'rgb(19, 21, 27)',
    gray: 'rgb(243, 243, 246)',
    white: 'rgb(255, 255, 255)',

    randomName(testName) {return `autotest-${testName}-${I.getRandomTextShort()}`},

    getFutureTimestamp(secondsToAdd) {
        const now = new Date();
        now.setSeconds(now.getSeconds() + secondsToAdd);
        const day = String(now.getDate()).padStart(2, '0');
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const year = now.getFullYear();
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
    },

    async setCronjob(seconds, minutes){
        I.say(`Setting cronjob to seconds: ${seconds}, minutes ${minutes}`);
        I.amOnPage('/admin/v9/settings/cronjob/');
        DT.waitForLoader();
        DT.filterEquals('task', 'sk.iway.iwcm.components.file_archiv.FileArchivatorInsertLater');
        await this.selectAll();
        I.click(DT.btn.edit_button);
        DTE.waitForEditor();
        I.checkOption("#DTE_Field_enableTask_0");
        DTE.fillField('minutes', minutes);
        DTE.fillField('seconds', seconds);
        DTE.save();
        I.relogin('admin');
    },

    // == File archive functions ==

    checkStatus(row, col, expectedIcons = [], unexpectedIcons = []){
        const iconNames = [
            'star',
            'star-off',
            'map-pin',
            'map-pin-off',
            'texture',
            'calendar-time',
            'calendar-plus'
        ]
        expectedIcons.forEach(icon => {
            I.assertTrue(iconNames.includes(icon), "Icon name '" + icon + "' is not valid");
            I.waitForElement(locate("#fileArchiveDataTable tbody tr:nth-child("+row+") td:nth-child("+col+")").find(`.ti-${icon}`), 10);
        });
        unexpectedIcons.forEach(icon => {
            I.assertTrue(iconNames.includes(icon), "Icon name '" + icon + "' is not valid");
            I.dontSeeElement(locate("#fileArchiveDataTable tbody tr:nth-child("+row+") td:nth-child("+col+")").find(`.ti-${icon}`), 10);
        });
    },

    uploadFile(virtualFileName, fileName, validFrom, validTo, destFolder, dateUploadLater, emails){
        I.clickCss('#fileArchiveDataTable_wrapper .buttons-create');
        DTE.waitForEditor('fileArchiveDataTable');
        DTE.fillField('virtualFileName', virtualFileName);
        I.attachFile('input.dz-hidden-input', 'tests/apps/file-archive/docs/' + fileName);
        I.waitForElement("//button[normalize-space(text())='Pridať' and @disabled]", 10);
        I.waitForElement("//button[normalize-space(text())='Pridať' and not(@disabled)]", 10);

        if (validFrom) DTE.fillField("validFrom", validFrom);
        if (validTo) DTE.fillField("validTo", validTo);
        if (destFolder) I.fillField('#editorAppDTE_Field_editorFields-dir div input', destFolder);
        if (dateUploadLater){
            DTE.clickSwitch('editorFields-saveLater_0');
            DTE.fillField('editorFields-dateUploadLater', dateUploadLater);
        }
        if (emails) I.fillField("#DTE_Field_editorFields-emails", emails);
    },

    editFile(virtualFileName, newFileName, validFrom, validTo, uploadType, fileName){
        I.clickCss('#fileArchiveDataTable_wrapper .buttons-edit');
        DTE.waitForEditor('fileArchiveDataTable');
        if(virtualFileName) DTE.fillField('virtualFileName', virtualFileName);

        if (newFileName){
            DTE.clickSwitch('editorFields-renameFile_0');
            I.waitForElement(locate('label.warningLabel').withText('Upozornenie: Po tejto zmene je treba aktualizovať všetky statické odkazy.'), 10);
            DTE.fillField('editorFields-newFileName', newFileName);
        }

        if (validFrom){
            DTE.fillField("validFrom", validFrom);
            I.pressKey('Enter');
        }
        if (validTo) {
            DTE.fillField("validTo", validTo);
            I.pressKey('Enter');
        }
        if (uploadType) {
            I.selectOption('select#DTE_Field_editorFields-uploadType', uploadType);
            I.attachFile('input.dz-hidden-input', 'tests/apps/file-archive/docs/' + fileName);
        }
    },

    async getFontColor(row, col){
        const elementSelector = locate("#fileArchiveDataTable tbody tr:nth-child("+row+") td:nth-child("+col+")");
        return await I.grabCssPropertyFrom(elementSelector, 'color');
    },

    async getBackgroundColor(row){
        const elementSelector = locate("#fileArchiveDataTable tbody tr:nth-child("+row+") td:nth-child(1)");
        return await I.grabCssPropertyFrom(elementSelector, 'background-color');
    },

    async hasCheckbox(row) {
        const beforeContent = await I.executeScript((row) => {
          const elementSelector = document.querySelector(
            `#fileArchiveDataTable tbody tr:nth-child(${row}) td.dt-select-td`
          );
          if (!elementSelector) return null;
          return window.getComputedStyle(elementSelector, '::before').content;
        }, row);

        return beforeContent === '"\\eb2c"';
      },

    getVersionName(fileName, version) {
        return fileName.replace(/(\.)/g, `_v_${version}$1`);
    },

    deleteTestFiles(name = ""){
        I.amOnPage(this.fileArchive);
        DT.filterContains('virtualFileName', 'autotest-' + name);
        I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
        DT.deleteAll('fileArchiveDataTable');
        I.waitForText('Nenašli sa žiadne vyhovujúce záznamy', 10);
    },

    rollback(virtualFileName, shouldSucceed = true, text = "Rollback neúspešný"){
        DT.filterEquals('virtualFileName', virtualFileName);
        I.clickCss('button.buttons-select-all');
        I.clickCss('.buttons-rollback');
        I.waitForElement("div.toast-message", 10);
        I.see("Naozaj si prajete vykonať rollback ?", "div.toast-title");
        I.click('Potvrdiť', "div.toast-message" )
        if (shouldSucceed){
            I.waitForElement("div.toast-success", 10);
            I.see("Vrátenie poslednej zmeny spať" ,"div.toast-title");
            I.see("Rollback úspešný", "div.toast-message");
        } else {
            I.waitForElement("div.toast-error", 10);
            I.see("Vrátenie poslednej zmeny spať" ,"div.toast-title");
            I.see(text, "div.toast-message");
        }
        I.clickCss('button.toast-close-button');
    },

    async checkTreeStructure() {
        const anchors = await I.grabTextFromAll('.jstree-anchor');
        let disabledAnchors = await I.grabTextFromAll('.jstree-anchor.jstree-disabled');
        const limitIndex = anchors.indexOf('archiv');
        I.assertNotEqual(limitIndex, -1, '"Archiv" not found in the anchor list');
        await anchors.slice(0, limitIndex).forEach(anchor => {
            I.assertTrue(disabledAnchors.includes(anchor), `Anchor "${anchor}" should be disabled`);
            disabledAnchors = disabledAnchors.filter(item => item !== anchor);
        });
        anchors.slice(limitIndex).forEach(anchor => {
            I.assertFalse(disabledAnchors.includes(anchor), `Anchor "${anchor}" should NOT be disabled`);
        });
    },

    // == Elfinder functions ==

    async checkFileContent(fileName, base = 'archive_file_test.png', visualTest = true){
        I.say("Checking file content")
        I.seeElement(`.elfinder-cwd-filename[title='${fileName}']`)
        I.waitForElement('.elfinder-cwd', 10);

        if (visualTest){
            within('.elfinder-cwd', () => {
                I.rightClick(fileName);
            });

            I.waitForVisible('.elfinder-contextmenu', 10);
            I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-wjfileopen');
            await this.checkScreenshot(fileName, base);
            I.closeOtherTabs();
        }
    },

    async removeFileByElfinder(fileSelector, url = this.elfinder) {
        I.amOnPage(url);
        let wasRemovedByElfinder = false;
        while (await I.grabNumberOfVisibleElements(fileSelector) > 0) {
            within('.elfinder-cwd', () => {
                I.rightClick(fileSelector);
            });
            I.waitForVisible('.elfinder-contextmenu', 10);
            I.clickCss('.elfinder-contextmenu-item .elfinder-button-icon-rm');
            I.waitForVisible(".elfinder-dialog.elfinder-dialog-confirm", 10);
            I.clickCss(".elfinder-dialog.elfinder-dialog-confirm .elfinder-confirm-accept");
            wasRemovedByElfinder = true;
        }
        return wasRemovedByElfinder;
    },

    async selectAll() {
        const rowCount = await I.grabNumberOfVisibleElements('#fileArchiveDataTable > tbody > tr.selected');
        if (rowCount === 0) {
            I.click('button.buttons-select-all');
        }
    },

    async importFile(filePath) {
        I.openNewTab();
        I.amOnPage("/components/file_archiv/import_archiv.jsp");

        I.waitForFile(filePath, 30);
        I.attachFile("#xmlFile", filePath);
        I.clickCss("#saveFileForm");

        I.say("Uncheck all and chose file for replace");
        await I.executeScript(() => {
            $( 'tbody input[type="checkbox"]' ).removeAttr( 'checked');
        });

        I.clickCss("#btnOk");
        I.waitForText("Import ukončený", 10);
        I.clickCss("#btnCancel");
        I.switchToPreviousTab();
        I.closeOtherTabs();
    },

    checkOrderIds(values) {
        I.say("checkOrderIds, values=" + values);
        values.forEach((value, index) => {
            DT.checkTableCell("fileArchiveDataTable", index + 1, 13, value);
        });
    },

    async setAsc() {
        for (let i = 0; i < 3 && !await I.grabNumberOfVisibleElements('.dt-ordering-asc[aria-sort="ascending"]'); i++) {
            I.clickCss('.dt-th-id > .dt-column-order');
        }
    },

    async checkScreenshot(fileName, base) {
        I.wait(2);
        I.switchToNextTab();

        //chromium PDF viewer is not working in headless mode
        if ("false" !== process.env.CODECEPT_SHOW) {
            I.seeInCurrentUrl(fileName);

            I.resizeWindow(1280, 760);
            I.wait(2);
            await Document.compareScreenshotElement(null, base, 1280, 760, 9);
        }
    }
}