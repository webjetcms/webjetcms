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

// Move whole shot objects to reorder narration, slates and browser actions.
const videoPlan = {
    "language": "sk",
    "notes": "Durations estimate the edited narration. Cut setup, cleanup and slates; use the runner's transition holds when editing.",
    "shots": [
        {
            "id": "intro",
            "type": "manual",
            "durationSeconds": 5,
            "title": "Document Manager in the page editor",
            "text-sk": "",
            "notes": "Create a title card: Manažér dokumentov priamo v editore, with the WebJET CMS logo."
        },
        {
            "id": "open-link",
            "type": "auto",
            "durationSeconds": 11,
            "title": "Open a link from the unsaved editor",
            "text-sk": "Pri vkladaní odkazu na dokument už redaktor nemusí odchádzať z rozpracovanej webovej stránky, otvárať ďalšiu aplikáciu a ručne kopírovať adresu súboru.",
            "notes": "Select the prepared label and open the link dialog.",
            prepare: async ({ prepareEditor }) => {
                await prepareEditor();
            },
            shot: async ({ I }) => {
                await I.videoClick("#trEditor", 0.2);
                await I.pressKey(["CommandOrControl", "A"]);
                await I.wait(3);
                await I.videoClick(".cke_button_icon.cke_button__link_icon", 0.35);
                await I.waitForText("Informácie o odkaze", 10);
                await I.waitForElement(locate(".cke_dialog_tab").withText("Manažér dokumentov"), 20);
                await I.wait(3);
            }
        },
        {
            "id": "archive-tree",
            "type": "auto",
            "durationSeconds": 12,
            "title": "Browse the embedded Document Manager",
            "text-sk": "WebJET CMS teraz prepája editor stránok priamo s Manažérom dokumentov. V dialógu Odkaz pribudla samostatná karta. Na jednom mieste ponúka strom priečinkov aj prehľad dokumentov vo vybranom priečinku.",
            "notes": "Open the Document Manager tab and select marketing/webjet-cms.",
            prepare: async ({ prepareLinkDialog }) => {
                await prepareLinkDialog();
            },
            shot: async ({ I, DT }) => {
                await I.videoClick(locate(".cke_dialog_tab").withText("Manažér dokumentov"), 0.45);
                await I.waitForElement(fileArchiveFrame, 20);
                await I.switchTo(fileArchiveFrame);
                await I.waitForVisible(fileArchiveTable, 20);
                DT.waitForLoader(fileArchiveTableId);
                await selectArchiveFolderForVideo(I, DT);
                await I.wait(5);
            }
        },
        {
            "id": "desktop-drag",
            "type": "manual",
            "durationSeconds": 3,
            "title": "Drag local PDF files into the dialog",
            "text-sk": "Ak dokument ešte v archíve nie je, môžete ho sem presunúť priamo z počítača.",
            "notes": "Film dragging PR-260-produktovy-list.pdf and PR-260-cennik-sluzieb.pdf from the desktop into the embedded Document Manager. Cut to the automated upload-progress shot."
        },
        {
            "id": "upload-progress",
            "type": "auto",
            "durationSeconds": 9,
            "title": "Upload two files and show progress",
            "text-sk": "Naraz je možné nahrať jeden alebo viac súborov. Panel priebehu ukáže stav každého súboru aj celého nahrávania a po dokončení sa zoznam automaticky obnoví.",
            "notes": "Upload the two isolated demo PDFs and show individual and total progress.",
            prepare: async ({ prepareArchive }) => {
                await prepareArchive();
            },
            shot: async ({ I, productFile, priceListFile }) => {
                await uploadFilesToDropzone(I, [productFile, priceListFile], "success");
                await I.wait(7);
                await I.videoClick("#upload-wrapper-close", 0.25);
                await I.waitForInvisible("#upload-wrapper", 10);
            }
        },
        {
            "id": "duplicate-version",
            "type": "auto",
            "durationSeconds": 15,
            "title": "Keep the original as a historical version",
            "text-sk": "Pri súbore s rovnakým názvom zostáva rozhodnutie vo vašich rukách. Môžete ho preskočiť, nahradiť aktuálny dokument alebo uložiť novú verziu. Pri novej verzii zostane pôvodný súbor zachovaný v histórii.",
            "notes": "Seed the two demo files before the slate, then show duplicate resolution and New version.",
            prepare: async ({ prepareFiles }) => {
                await prepareFiles();
            },
            shot: async ({ I, DT, productUpdate }) => {
                await uploadFilesToDropzone(I, [productUpdate], "exist");
                const keepBothButton = "#toast-container-upload div.toast[data-upload-status='exist'] .btn-toast-keepboth";
                await I.waitForVisible(keepBothButton, 20);
                await I.wait(5);
                await I.videoClick(keepBothButton, 0.5);
                await I.waitForElement(locate("#toast-container-upload div.toast[data-upload-status='success']").withText(productUpdate.fileName), 60);
                DT.waitForLoader(fileArchiveTableId);
                await I.wait(4);
            }
        },
        {
            "id": "select-document",
            "type": "auto",
            "durationSeconds": 13,
            "title": "Transfer the document URL to the link dialog",
            "text-sk": "Potom stačí vybrať priečinok a kliknúť na názov dokumentu. WebJET CMS prenesie jeho adresu do poľa URL.",
            "notes": "Filter the isolated files, select the product sheet and show its URL.",
            prepare: async ({ prepareFiles }) => {
                await prepareFiles();
            },
            shot: async ({ I, DT, expectedProductUrl }) => {
                DT.filterContains("virtualFileName", cleanupFilter);
                const productLink = locate(`${fileArchiveTable} tbody .dt-row-edit a`).withText(productVirtualName);
                await I.waitForVisible(productLink, 20);
                await I.videoClick(productLink, 0.4);
                await I.seeInField("#txtUrl", expectedProductUrl);
                await I.wait(5);
            }
        },
        {
            "id": "confirm-link",
            "type": "auto",
            "durationSeconds": 11,
            "title": "Insert the link without saving the page",
            "text-sk": "Potvrdením sa odkaz vloží priamo do označeného textu.",
            "notes": "Prepare a selected document, then confirm the link and hold the unsaved result.",
            prepare: async ({ I, DT, prepareFiles, expectedProductUrl }) => {
                await prepareFiles();
                DT.filterContains("virtualFileName", cleanupFilter);
                const productLink = locate(`${fileArchiveTable} tbody .dt-row-edit a`).withText(productVirtualName);
                await I.waitForVisible(productLink, 20);
                await I.click(productLink);
                await I.seeInField("#txtUrl", expectedProductUrl);
                await I.switchTo();
            },
            shot: async ({ I, expectedProductUrl }) => {
                await I.videoClick(".cke_dialog_ui_button_ok", 0.3);
                await I.waitForFunction(expectedUrl => window.ckEditorInstance != null && window.ckEditorInstance.getData().includes(expectedUrl), [expectedProductUrl], 20);
                await I.wait(8);
            }
        },
        {
            "id": "standalone-archive",
            "type": "auto",
            "durationSeconds": 6,
            "title": "Use the standalone Document Manager",
            "text-sk": "Rovnaký strom a hromadné nahrávanie sú dostupné aj v samostatnom Manažéri dokumentov. Práca s väčším množstvom súborov je preto rýchlejšia, prehľadnejšia a bezpečnejšia.",
            "notes": "Show the same folder and files in the standalone application.",
            prepare: async ({ I, DT, prepareFiles, closeEditor }) => {
                await prepareFiles();
                await closeEditor();
                await I.amOnPage("/apps/file-archive/admin/");
                await I.waitForVisible(fileArchiveTable, 20);
                DT.waitForLoader(fileArchiveTableId);
                await selectArchiveFolder(I, DT);
                await I.waitForText(productVirtualName, 20, fileArchiveTable);
            },
            shot: async ({ I }) => {
                await I.wait(8);
            }
        },
        {
            "id": "documentation",
            "type": "auto",
            "durationSeconds": 5,
            "title": "Document Manager documentation",
            "text-sk": "Podrobný postup nájdete v dokumentácii WebJET CMS. Odkaz je v popise videa.",
            "notes": "Scroll the documentation in the recording tab and show the final link in the video description.",
            shot: async ({ I }) => {
                await I.videoDocumentation("https://docs.webjetcms.sk/latest/sk/redactor/files/file-archive/README");
                await I.wait(8);
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

Scenario("260-58593-manazer-dokumentov-integracia-do-web-stranky", async ({ I, DT, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const productFileName = "PR-260-produktovy-list.pdf";
    const priceListFileName = "PR-260-cennik-sluzieb.pdf";
    const productFile = createUploadFile("archive_file_test.pdf", productFileName);
    const priceListFile = createUploadFile("archive_file_test_fourth.pdf", priceListFileName);
    const productUpdate = createUploadFile("archive_file_test_second.pdf", productFileName);
    const expectedProductUrl = archiveFolder + productFileName.toLowerCase();
    const fileShots = ["upload-progress", "duplicate-version", "select-document", "confirm-link", "standalone-archive"];
    const prepareEditor = async () => {
        await I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=100605");
        DTE.waitForEditor();
        await I.waitForVisible("#trEditor", 20);
        await DTE.fillCkeditor("<p>Aktuálny produktový list</p>");
    };
    const prepareLinkDialog = async () => {
        await prepareEditor();
        await I.clickCss("#trEditor");
        await I.pressKey(["CommandOrControl", "A"]);
        await I.clickCss(".cke_button_icon.cke_button__link_icon");
        await I.waitForText("Informácie o odkaze", 10);
        await I.waitForElement(locate(".cke_dialog_tab").withText("Manažér dokumentov"), 20);
    };
    const prepareArchive = async () => {
        await prepareLinkDialog();
        await I.click(locate(".cke_dialog_tab").withText("Manažér dokumentov"));
        await I.waitForElement(fileArchiveFrame, 20);
        await I.switchTo(fileArchiveFrame);
        await I.waitForVisible(fileArchiveTable, 20);
        DT.waitForLoader(fileArchiveTableId);
        await selectArchiveFolder(I, DT);
    };
    const prepareFiles = async () => {
        await prepareArchive();
        await uploadFilesToDropzone(I, [productFile, priceListFile], "success");
        await I.clickCss("#upload-wrapper-close");
        await I.waitForInvisible("#upload-wrapper", 10);
        await reloadFileArchiveFrame(I, DT);
    };
    const closeEditor = async () => {
        await I.switchTo();
        if (process.argv.includes("dry-run")) return;
        if (await I.grabNumberOfVisibleElements(".cke_dialog") > 0) {
            await I.clickCss(".cke_dialog_ui_button_cancel");
            await I.waitForInvisible(".cke_dialog", 10);
        }
        if (await I.grabNumberOfVisibleElements("div.DTED.show") > 0) {
            DTE.cancel();
            await I.waitForInvisible("div.DTED.show", 10);
        }
    };
    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { DT, productFile, priceListFile, productUpdate, expectedProductUrl, prepareEditor, prepareLinkDialog, prepareArchive, prepareFiles, closeEditor },
        setup: async () => {
            login("admin");
            await deleteArchiveRowsByFilter(I, DT);
            removeLocalArchiveFiles([productFileName, priceListFileName]);
        },
        cleanup: async shot => {
            if (shot.id === "documentation") return;
            await closeEditor();
            if (fileShots.includes(shot.id)) {
                await deleteArchiveRowsByFilter(I, DT);
                removeLocalArchiveFiles([productFileName, priceListFileName]);
            }
        }
    });
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

async function selectArchiveFolderForVideo(I, DT) {
    const marketingFolderToggle =
        "#SomStromcek li[id='/files/archiv/marketing'].jstree-closed > i.jstree-ocl";
    const archiveFolderNode = "#SomStromcek li[id='/files/archiv/marketing/webjet-cms']";
    const archiveFolderAnchor = `${archiveFolderNode} > a.jstree-anchor`;

    await I.waitForElement("#SomStromcek", 20);
    await I.jstreeWaitForLoader();
    await I.waitForVisible(marketingFolderToggle, 20);
    await I.videoClick(marketingFolderToggle, 0.2);
    await I.jstreeWaitForLoader();
    await I.waitForVisible(archiveFolderAnchor, 20);
    await I.videoClick(archiveFolderAnchor, 0.3);
    await I.waitForElement(`${archiveFolderNode} > a.jstree-clicked[aria-selected='true']`, 20);
    DT.waitForLoader(fileArchiveTableId);
}

async function selectArchiveFolder(I, DT) {
    await I.waitForElement("#SomStromcek", 20);
    await I.executeScript(() => {
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
    await I.waitForElement(selectedFolder, 20);
    await I.click(selectedFolder);
    await I.waitForElement(locate("#SomStromcek a.jstree-clicked").withText(archiveFolderName), 20);
    DT.waitForLoader(fileArchiveTableId);
}

async function uploadFilesToDropzone(I, files, expectedStatus) {
    await I.waitForElement(dropzoneInput, 20);
    await I.usePlaywrightTo("upload feature-video files to the file archive dropzone", async ({ page }) => {
        const fs = require("fs");
        const uploadPayloads = files.map(file => ({
            name: file.fileName,
            mimeType: "application/pdf",
            buffer: fs.readFileSync(file.filePath),
        }));
        await page.frameLocator(fileArchiveFrame).locator(dropzoneInput).setInputFiles(uploadPayloads);
    });

    await I.waitForVisible("#upload-wrapper", 25);
    for (const file of files) {
        await I.waitForElement(locate("#toast-container-upload div.toast").withText(file.fileName), 20);
        await I.waitForElement(
            locate(`#toast-container-upload div.toast[data-upload-status='${expectedStatus}']`).withText(file.fileName),
            60
        );
    }
}

async function reloadFileArchiveFrame(I, DT) {
    await I.switchTo();
    await I.executeScript(() => {
        document.querySelector("#wjLinkFileArchiveIframeElement").contentWindow.location.reload();
    });
    await I.switchTo(fileArchiveFrame);
    await I.waitForVisible(fileArchiveTable, 20);
    DT.waitForLoader(fileArchiveTableId);
    await selectArchiveFolder(I, DT);
}

async function deleteArchiveRowsByFilter(I, DT) {
    await I.switchTo();
    await I.openNewTab();
    await I.amOnPage("/apps/file-archive/admin/");
    await I.waitForVisible(fileArchiveTable, 20);
    DT.waitForLoader(fileArchiveTableId);
    await selectArchiveFolder(I, DT);
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
            await I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 20, fileArchiveTable);
        }
    }

    await I.closeCurrentTab();
    await I.switchTo();
}
