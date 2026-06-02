Feature('rag.embedding-chunks');

Before(({ I, login }) =>{
    login('admin');
});

const rootDirId = "115851";
const rootDirPath = "/Aplikácie/Vyhľadávanie/semantic_parent";
const rootSubDirPath = "/Aplikácie/Vyhľadávanie/semantic_parent/semantic_child";
const valueA = "Všade na svete však podľa Boston Consulting Group Wealth Reportu 2018 platí";
const valueB = "Hoci reakcie na whisky sú vzhľadom na chute a kvalitu, pochopiteľne, rôzne";
const valueC = "Celková hodnota top 10 krajín presahuje 62 biliónov dolárov .";

Scenario('Allow rag semantic search', ({ I, DT, Document }) => {
    Document.setConfigValue("ragSemanticSearchEnabled", "true");
});

Scenario('Chunks - base test', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/");

    const navLink = [
        {
            id: "pills-document-tab",
            title: "Webové stránky"
        }
        // future expansion here
    ]
    I.say("Check nav links");
    navLink.forEach(link => {
        I.seeElement( locate("#pills-embedding-chunks #" + link.id).withText(link.title) );
    });

    I.say("Check default dir");
    checkPathInRootDir(I, "Všetky priečinky");
    I.uncheckOption("#includeSubfolders");

    I.say("See subfolder value");
    DT.filterContains("chunkText", valueA);
    I.see(valueA, "#datatableInit tbody td");

    I.say("Select folder and try it again");
    selectTree(I, "#embeddingChunksDataTable_extfilter button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent"]);

    I.say("Check loaded values");
    DT.waitForLoader();
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.checkTableRow("datatableInit_wrapper", 1, [null, null, "0", null, "text-embedding-3-small", "sk", "COMPLETED"]);
});

Scenario('Stop indexing CronJob before removing action', ({ I, DT, DTE }) => {
    // Turn it off for check
    I.say("Turn off indexing");
    setRagCronJob(I, DT, DTE, false, "*/1");
});

Scenario('Chunks test + run deleting index action', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId);
    DT.waitForLoader();

    DT.waitForLoader();
    I.say("Check active tab");
    I.seeElement( locate("#pills-embedding-chunks a#pills-document-tab.active") );

    I.say("Check pre-selected root dir");
    checkPathInRootDir(I, rootDirPath);
    I.uncheckOption("#includeSubfolders");

    I.say("Check that index action buttons are there");
    I.seeElement("button.btnAddIndex");
    I.seeElement("button.btnRemoveIndex");

    I.say("Check I see value from selected dir");
    DT.filterContains("chunkText", valueA);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterContains("chunkText", valueB);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Can't see sub-folder value");

    DT.filterContains("chunkText", valueC);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Allow sub-folders and see value");
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders")
    DT.waitForLoader();
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check ADD index dialog");
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeElement("#editorApprootDir input[value='" + rootDirPath + "']");

    I.say("Folder contains 5 pages, 5 are indexed - and no awaiting ADDING");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 7, 0);

    I.say("Close dialog");
    I.switchTo();
    I.clickCss("#modalIframe .modal-header button.btn-close");
    I.waitForInvisible("#modalIframeIframeElement");

    I.say("Test now REMOVE dialog");
    I.clickCss("button.btnRemoveIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeElement("#editorApprootDir input[value='" + rootDirPath + "']");

    I.say("Folder contains 5 pages, 5 are indexed - and no awaiting REMOVE");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 7, 0);

    I.say("Switch to subfolder in the dialog");
    selectTree(I, ".rootDirDiv button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);

    I.say("Check changed path");
    I.seeElement("#editorApprootDir input[value='" + rootSubDirPath + "']");

    I.say("Check changed stats");
    checkIndexingStatusValues(I, 1, 2, 2, 0);

    I.say("Run REMOVE indexing action");
    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-primary");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#succ-msg-delete", 10);
    I.seeTextEquals("Webové stránky boli úspešne zaradené do fronty na odstránenie indexovania. (2)", "#succ-msg-delete");

    I.say("Check changed stats");
    checkIndexingStatusValues(I, 1, 2, 2, 2);
});

Scenario('Start indexing CronJob after removing action', ({ I, DT, DTE }) => {
    I.say("Turn ON indexing CronJob");
    setRagCronJob(I, DT, DTE, true, "*/1");
});

Scenario('After removing action - checks', ({ I, DT }) => {
    I.say("Wait and test if selected pages lost their indexes");
    I.wait(70); // 70 seconds just in case so change will be applied

    I.say("First test that parent folder did NOT lose their indexing");
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    DT.waitForLoader();

    DT.filterContains("chunkText", valueA);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterContains("chunkText", valueB);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Include subfolders and test that we can't find indexes");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Open REMOVE dialog and check");
    I.clickCss("button.btnRemoveIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeElement("#editorApprootDir input[value='" + rootDirPath + "']");

    I.say("Parent folder should be unchanged");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Switch to sub-folder and check");
    selectTree(I, ".rootDirDiv button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);

    I.say("Check that pages are really not indexed and they are no more awaiting index removing");
    checkIndexingStatusValues(I, 1, 2, 0, 0);
});

Scenario('Stop indexing CronJob before adding action', ({ I, DT, DTE }) => {
    // Turn it off for check
    I.say("Turn off indexing");
    setRagCronJob(I, DT, DTE, false, "*/1");
});

Scenario('Run adding index action', ({ I, DT }) => {
    I.say("Go and add pages back to queue for indexing");
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");

    I.say("Open INDEX dialog and check");
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeElement("#editorApprootDir input[value='" + rootDirPath + "']");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders, see 2 pages missing index");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 5, 0);

    I.say('Switch to child folder - 2 out of 2 need indexing');
    selectTree(I, ".rootDirDiv button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);
    I.seeElement("#editorApprootDir input[value='" + rootSubDirPath + "']");
    checkIndexingStatusValues(I, 1, 2, 0, 0);

    I.say("Run indexing action");
    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-primary");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#succ-msg-index", 10);
    I.seeTextEquals("Webové stránky boli úspešne zaradené do fronty na indexovanie. (2)", "#succ-msg-index");
    checkIndexingStatusValues(I, 1, 2, 0, 2);

    I.say("Close dialog");
    I.switchTo();
    I.clickCss("#modalIframe .modal-header button.btn-close");
    I.waitForInvisible("#modalIframeIframeElement");

    I.say("Test that pages are still not indexed");
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    I.see("Nenašli sa žiadne vyhovujúce záznamy");
});

Scenario('Start indexing CronJob after adding action', ({ I, DT, DTE }) => {
    // Turn it on after adding action
    I.say("Turn ON indexing CronJob");
    setRagCronJob(I, DT, DTE, true, "*/1");
});

Scenario('After adding action - checks', ({ I, DT }) => {
    I.say("Wait and test if selected pages gained back their indexes");
    I.wait(70); // 70 seconds just in case so change will be applied

    I.say("Go and check that pages are again indexed");
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");

    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check INDEX dialog");
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeElement("#editorApprootDir input[value='" + rootDirPath + "']");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 7, 0);
});

Scenario('Set CronJob to higher interval', ({ I, DT, DTE }) => {
    // Set to higher interval
    I.say("Turn ON indexing CronJob");
    setRagCronJob(I, DT, DTE, true, "*/30");
});

function selectTree(I, buttonSelector, nodesArr) {
    I.clickCss(buttonSelector);
    I.waitForVisible("#jsTree", 10);

    for(let i = 0; i < nodesArr.length; i++) {
        if(i == nodesArr.length - 1) {
            I.click(locate("#jsTree a.jstree-anchor").withText(nodesArr[i]));
        } else {
            I.click(locate('#jsTree .jstree-node.jstree-closed').withText(nodesArr[i]).find('.jstree-icon.jstree-ocl'));
        }
    }

    I.waitForInvisible("#jsTree", 10);
}

function checkIndexingStatusValues(I, allGroups, allDoc, indexedDoc, queuedDoc) {
    I.seeElement(locate("#allGroups").withText(allGroups + ""));
    I.seeElement(locate("#allDoc").withText(allDoc + ""));
    I.seeElement(locate("#indexedDoc").withText(indexedDoc + ""));
    I.seeElement(locate("#queuedDoc").withText(queuedDoc + ""));
}

function checkPathInRootDir(I, dirPath) {
    I.seeElement( locate("#embeddingChunksDataTable_extfilter #editorApprootDir input[value='" + dirPath + "']")  );
}

function setRagCronJob(I, DT, DTE, turnOn, minuteValue) {
    I.amOnPage("/admin/v9/settings/cronjob/");
    DT.waitForLoader();
    DT.filterEquals("taskName", "RAG - run indexing");
    I.click("RAG - run indexing");
    DTE.waitForEditor();
    I.fillField("#DTE_Field_minutes", minuteValue);

    if(turnOn === true) {
        I.checkOption('#DTE_Field_enableTask_0');
    } else {
        I.uncheckOption('#DTE_Field_enableTask_0');
    }

    DTE.save();
}