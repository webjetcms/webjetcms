Feature("video.260-58593-manazer-dokumentov-integracia-do-web-stranky");

const archiveFolder = "/files/archiv/marketing/webjet-cms/";
const archiveFolderName = "webjet-cms";
const cleanupFilter = "PR 260";
const productVirtualName = "PR 260 produktovy list";
const priceListVirtualName = "PR 260 cennik sluzieb";
const cleanupVirtualNames = [productVirtualName, priceListVirtualName];
const fileArchiveFrame = "#wjLinkFileArchiveIframeElement";
const fileArchiveTableId = "fileArchiveDataTable";
const fileArchiveTable = `#${fileArchiveTableId}`;
const dropzoneInput = "input.dz-hidden-input.dz-hidden-input-dt-upload";

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
Pri vkladaní odkazu na dokument už redaktor nemusí odchádzať z rozpracovanej webovej stránky, otvárať ďalšiu aplikáciu a ručne kopírovať adresu súboru.

WebJET CMS teraz prepája editor stránok priamo s Manažérom dokumentov. V dialógu Odkaz pribudla samostatná karta. Na jednom mieste ponúka strom priečinkov aj prehľad dokumentov vo vybranom priečinku.

Ak dokument ešte v archíve nie je, môžete ho sem presunúť priamo z počítača. Naraz je možné nahrať jeden alebo viac súborov. Panel priebehu ukáže stav každého súboru aj celého nahrávania a po dokončení sa zoznam automaticky obnoví.

Pri súbore s rovnakým názvom zostáva rozhodnutie vo vašich rukách. Môžete ho preskočiť, nahradiť aktuálny dokument alebo uložiť novú verziu. Pri novej verzii zostane pôvodný súbor zachovaný v histórii.

Potom stačí vybrať priečinok a kliknúť na názov dokumentu. WebJET CMS prenesie jeho adresu do poľa URL. Potvrdením sa odkaz vloží priamo do označeného textu.

Rovnaký strom a hromadné nahrávanie sú dostupné aj v samostatnom Manažéri dokumentov. Práca s väčším množstvom súborov je preto rýchlejšia, prehľadnejšia a bezpečnejšia.

Podrobný postup nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
0:00-0:05 - MANUAL: titulná karta „Manažér dokumentov priamo v editore“ s logom WebJET CMS.
0:05-0:16 - Otvoriť existujúcu stránku v editore, označiť text „Aktuálny produktový list“ a kliknúť na tlačidlo Odkaz.
0:16-0:28 - V dialógu Odkaz kliknúť na kartu Manažér dokumentov. Ukázať strom priečinkov a tabuľku, potom vybrať priečinok marketing/webjet-cms.
0:28-0:40 - MANUAL: vložiť krátky záber presunutia dvoch PDF súborov z plochy do dialógu. Nadviazať automatizovaným záberom panela s priebehom oboch nahrávaní.
0:40-0:55 - Nahrať súbor s rovnakým názvom, ukázať možnosti Preskočiť, Nahradiť a Nová verzia a kliknúť na Nová verzia.
0:55-1:08 - Zavrieť panel nahrávania, prefiltrovať dokumenty a kliknúť na „PR 260 produktovy list“. Podržať záber na automaticky vyplnenom poli URL.
1:08-1:19 - Potvrdiť dialóg a ukázať odkaz vložený do označeného textu bez uloženia webovej stránky.
1:19-1:25 - Celkový pohľad na editor s hotovým odkazom. Voliteľný titulok: „Menej prepínania. Rýchlejšia práca. Bezpečné verzie.“
1:25-1:30 - MANUAL: otvoriť https://docs.webjetcms.sk/latest/sk/redactor/webpages/working-in-editor/?id=odkazy-na-s%C3%BAbory-a-nahr%C3%A1vanie-s%C3%BAborov a zobraziť záverečný titulok „Podrobný postup nájdete v dokumentácii WebJET CMS.“
`);
});

Scenario("260-58593-manazer-dokumentov-integracia-do-web-stranky", async ({ I, DT, DTE, login }) => {
    const productFileName = "PR-260-produktovy-list.pdf";
    const priceListFileName = "PR-260-cennik-sluzieb.pdf";
    const productFile = createUploadFile("archive_file_test.pdf", productFileName);
    const priceListFile = createUploadFile("archive_file_test_fourth.pdf", priceListFileName);
    const productUpdate = createUploadFile("archive_file_test_second.pdf", productFileName);
    const expectedProductUrl = archiveFolder + productFileName.toLowerCase();

    login("admin");
    await deleteArchiveRowsByFilter(I, DT);
    removeLocalArchiveFiles([productFileName, priceListFileName]);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=100605");
    DTE.waitForEditor();
    I.waitForVisible("#trEditor", 20);

    // Shot 1: prepare a clear link label in the unsaved editor buffer.
    await DTE.fillCkeditor("<p>Aktuálny produktový list</p>");
    I.videoClick("#trEditor", 0.2);
    I.pressKey(["CommandOrControl", "A"]);
    I.wait(3);
    I.videoClick(".cke_button_icon.cke_button__link_icon", 0.35);
    I.waitForText("Informácie o odkaze", 10);
    const fileArchiveTab = locate(".cke_dialog_tab").withText("Manažér dokumentov");
    I.waitForElement(fileArchiveTab, 20);
    I.wait(3);

    // Shot 2: open the embedded file archive and choose the target folder.
    I.videoClick(fileArchiveTab, 0.45);
    I.waitForElement(fileArchiveFrame, 20);
    I.switchTo(fileArchiveFrame);
    I.waitForVisible(fileArchiveTable, 20);
    DT.waitForLoader(fileArchiveTableId);
    selectArchiveFolder(I, DT);
    I.wait(5);

    // Shot 3: upload two documents and show individual and total progress.
    uploadFilesToDropzone(I, [productFile, priceListFile], "success");
    I.wait(7);
    I.videoClick("#upload-wrapper-close", 0.25);
    I.waitForInvisible("#upload-wrapper", 10);
    reloadFileArchiveFrame(I, DT);

    // Shot 4: upload a duplicate and preserve the original as a historical version.
    uploadFilesToDropzone(I, [productUpdate], "exist");
    const keepBothButton = "#toast-container-upload div.toast[data-upload-status='exist'] .btn-toast-keepboth";
    I.waitForVisible(keepBothButton, 20);
    I.wait(5);
    I.videoClick(keepBothButton, 0.5);
    I.waitForElement(
        locate("#toast-container-upload div.toast[data-upload-status='success']").withText(productFileName),
        60
    );
    DT.waitForLoader(fileArchiveTableId);
    I.wait(4);

    // Shot 5: select the uploaded document and transfer its URL to the link dialog.
    I.videoClick("#upload-wrapper-close", 0.25);
    I.waitForInvisible("#upload-wrapper", 10);
    DT.filterContains("virtualFileName", cleanupFilter);
    const productLink = locate(`${fileArchiveTable} tbody .dt-row-edit a`).withText(productVirtualName);
    I.waitForVisible(productLink, 20);
    I.videoClick(productLink, 0.4);
    I.seeInField("#txtUrl", expectedProductUrl);
    I.wait(5);

    // Shot 6: confirm the link and keep the page itself unsaved.
    I.switchTo();
    I.videoClick(".cke_dialog_ui_button_ok", 0.3);
    I.waitForFunction((expectedUrl) => {
        return window.ckEditorInstance != null && window.ckEditorInstance.getData().includes(expectedUrl);
    }, [expectedProductUrl], 20);
    I.wait(8);

    // Remove isolated archive records in a disposable tab and return to the final editor state.
    await deleteArchiveRowsByFilter(I, DT);
    removeLocalArchiveFiles([productFileName, priceListFileName]);
    I.waitForVisible("#trEditor", 20);
    I.wait(8);
}).tag("@video");

function createUploadFile(sourceName, targetName) {
    const path = require("path");
    const sourcePath = path.join(__dirname, "../tests/apps/file-archive/docs", sourceName);

    return {
        fileName: targetName,
        filePath: sourcePath,
    };
}

function removeLocalArchiveFiles(fileNames) {
    if (process.argv.includes("dry-run")) return;

    const fs = require("fs");
    const path = require("path");
    const localArchiveFolder = path.resolve(__dirname, "../../../main/webapp/files/archiv/marketing/webjet-cms");
    for (const fileName of fileNames) {
        fs.rmSync(path.join(localArchiveFolder, fileName.toLowerCase()), { force: true });
    }
}

function selectArchiveFolder(I, DT) {
    I.waitForElement("#SomStromcek", 20);
    I.executeScript(() => {
        const folderWithoutSlash = "/files/archiv/marketing/webjet-cms";
        const parentFolder = folderWithoutSlash.substring(0, folderWithoutSlash.lastIndexOf("/"));
        const tree = window.$ && window.$("#SomStromcek").jstree(true);
        if (tree == null) return;

        tree.open_node(parentFolder, () => {
            tree.deselect_all(true);
            tree.select_node(folderWithoutSlash);
        });
    });

    const selectedFolder = locate("#SomStromcek a.jstree-anchor").withText(archiveFolderName);
    I.waitForElement(selectedFolder, 20);
    I.videoClick(selectedFolder, 0.3);
    I.waitForElement(locate("#SomStromcek a.jstree-clicked").withText(archiveFolderName), 20);
    DT.waitForLoader(fileArchiveTableId);
}

function uploadFilesToDropzone(I, files, expectedStatus) {
    I.waitForElement(dropzoneInput, 20);
    I.usePlaywrightTo("upload feature-video files to the file archive dropzone", async ({ page }) => {
        const fs = require("fs");
        const uploadPayloads = files.map(file => ({
            name: file.fileName,
            mimeType: "application/pdf",
            buffer: fs.readFileSync(file.filePath),
        }));
        await page.frameLocator(fileArchiveFrame).locator(dropzoneInput).setInputFiles(uploadPayloads);
    });

    I.waitForVisible("#upload-wrapper", 25);
    for (const file of files) {
        I.waitForElement(locate("#toast-container-upload div.toast").withText(file.fileName), 20);
        I.waitForElement(
            locate(`#toast-container-upload div.toast[data-upload-status='${expectedStatus}']`).withText(file.fileName),
            60
        );
    }
}

function reloadFileArchiveFrame(I, DT) {
    I.switchTo();
    I.executeScript(() => {
        document.querySelector("#wjLinkFileArchiveIframeElement").contentWindow.location.reload();
    });
    I.switchTo(fileArchiveFrame);
    I.waitForVisible(fileArchiveTable, 20);
    DT.waitForLoader(fileArchiveTableId);
    selectArchiveFolder(I, DT);
}

async function deleteArchiveRowsByFilter(I, DT) {
    I.switchTo();
    I.openNewTab();
    I.amOnPage("/apps/file-archive/admin/");
    I.waitForVisible(fileArchiveTable, 20);
    DT.waitForLoader(fileArchiveTableId);
    selectArchiveFolder(I, DT);
    DT.filterContainsForce("virtualFileName", cleanupFilter);

    if (!process.argv.includes("dry-run")) {
        const recordCount = await DT.getRecordCount(fileArchiveTableId);
        if (recordCount > 0) {
            const visibleVirtualNames = (await I.grabTextFromAll(`${fileArchiveTable} tbody .dt-row-edit a`))
                .map(name => name.trim());
            const unexpectedVirtualNames = visibleVirtualNames
                .filter(name => !cleanupVirtualNames.includes(name));
            if (visibleVirtualNames.length !== recordCount || unexpectedVirtualNames.length > 0) {
                throw new Error(`Refusing to delete unexpected PR 260 archive rows: ${unexpectedVirtualNames.join(", ")}`);
            }

            DT.deleteAll(fileArchiveTableId);
            DT.waitForLoader(fileArchiveTableId);
            I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 20, fileArchiveTable);
        }
    }

    I.closeCurrentTab();
    I.switchTo();
}
