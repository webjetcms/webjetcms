Feature('rag.embedding-chunks');

const WebjetDteJsTree = require("../../pages/WebjetDteJsTree");

Before(({ I, login }) =>{
    login('admin');
});

const rootDirId = "115851";
const rootDirPath = "/Aplikácie/Vyhľadávanie/semantic_parent";
const rootSubDirPath = "/Aplikácie/Vyhľadávanie/semantic_parent/semantic_child";
const valueA = "Všade na svete však podľa Boston Consulting Group Wealth Reportu 2018 platí";
const valueB = "Hoci reakcie na whisky sú vzhľadom na chute a kvalitu, pochopiteľne, rôzne";
const valueC = "Celková hodnota top 10 krajín presahuje 62 biliónov dolárov .";
const openAiProvider = "openai";
const openAiProviderLabel = "OpenAI";
const openAiModel = "text-embedding-3-small";
const geminiProvider = "gemini";
const geminiProviderLabel = "Gemini";
const geminiModel = "gemini-embedding-001";

Scenario('Allow rag semantic search', ({ I, DT, Document }) => {
    Document.setConfigValue("ragSemanticSearchEnabled", "true");

    // DELETE FROM rag_embedding_chunks;
    // /Aplikácie/Vyhľadávanie/semantic_parent - DO NOT check include subdirs
});

Scenario('Set OpenAI as the initial embedding provider', ({ I, DT, DTE }) => {
    setEmbeddingAssistant(I, DT, DTE, openAiProviderLabel, openAiModel);

    I.amOnPage("/admin/v9/settings/embedding-chunks/");
    checkCurrentEmbeddingConfiguration(I, openAiProvider, openAiModel);
});

Scenario('Chunks - base test', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/");

    checkCurrentEmbeddingConfiguration(I, openAiProvider, openAiModel);

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
    DT.waitForLoader();

    I.say("See subfolder value");
    DT.filterContains("chunkText", valueA);
    I.see(valueA, "#datatableInit tbody td");

    I.say("Select folder and try it again");
    selectTree(I, "#embeddingChunksDataTable_extfilter button.btn-webjet-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent"]);

    I.say("Check loaded values");
    DT.waitForLoader();
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterEquals("embeddingProvider", openAiProvider);
    DT.filterEquals("embeddingModel", openAiModel);
    I.see(openAiProvider, "#datatableInit tbody");
    I.see(openAiModel, "#datatableInit tbody");
});

Scenario('Stop indexing CronJob before preparing the delete test', ({ I, DT, DTE }) => {
    I.say("Turn off indexing before preparing a repeatable OpenAI baseline");
    setRagCronJob(I, DT, DTE, false, "*/1");
});

Scenario('Prepare OpenAI indexes for all test pages', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    checkCurrentEmbeddingConfiguration(I, openAiProvider, openAiModel);

    I.say("Queue the parent folder and its child folder for OpenAI indexing");
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#editorApprootDir input[value='" + rootDirPath + "']", 10);
    I.checkOption("#include_subfolders");

    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-primary");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#succ-msg-index", 10);
});

Scenario('Start indexing CronJob for the OpenAI baseline', ({ I, DT, DTE }) => {
    setRagCronJob(I, DT, DTE, true, "*/1");
});

Scenario('Wait for the complete OpenAI baseline', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    checkCurrentEmbeddingConfiguration(I, openAiProvider, openAiModel);

    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    DT.filterEquals("embeddingProvider", openAiProvider);
    DT.filterEquals("embeddingModel", openAiModel);
    startEmbeddingTableAutoRefresh(I);
    I.waitForText(valueC, 100, "#datatableInit tbody");
    stopEmbeddingTableAutoRefresh(I);
});

Scenario('Stop indexing CronJob before removing action', ({ I, DT, DTE }) => {
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
    DT.waitForLoader();

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
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Check ADD index dialog");
    I.uncheckOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeInField("#editorApprootDir input.form-control", rootDirPath);

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
    I.seeInField("#editorApprootDir input.form-control", rootDirPath);

    I.say("Folder contains 5 pages, 5 are indexed - and no awaiting REMOVE");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 7, 0);

    I.say("Switch to subfolder in the dialog");
    selectTree(I, ".rootDirDiv button.btn-webjet-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);

    I.say("Check changed path");
    I.seeInField("#editorApprootDir input.form-control", rootSubDirPath);

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
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    DT.waitForLoader();

    I.say("Wait until the child-page indexes are removed");
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    startEmbeddingTableAutoRefresh(I);
    I.waitForText("Nenašli sa žiadne vyhovujúce záznamy", 100, "#datatableInit_wrapper");
    stopEmbeddingTableAutoRefresh(I);

    I.say("Check that the parent-folder indexes were not removed");
    DT.filterContains("chunkText", valueA);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");
    DT.filterContains("chunkText", valueB);
    I.dontSee("Nenašli sa žiadne vyhovujúce záznamy");

    I.say("Open REMOVE dialog and check");
    I.uncheckOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    I.clickCss("button.btnRemoveIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#editorApprootDir input.form-control", 10);
    I.seeInField("#editorApprootDir input.form-control", rootDirPath);

    I.say("Parent folder should be unchanged");
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Switch to sub-folder and check");
    selectTree(I, ".rootDirDiv button.btn-webjet-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);

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

    I.uncheckOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();

    I.say("Open INDEX dialog and check");
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.seeInField("#editorApprootDir input.form-control", rootDirPath);
    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders, see 2 pages missing index");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 5, 0);

    I.say('Switch to child folder - 2 out of 2 need indexing');
    selectTree(I, ".rootDirDiv button.btn-webjet-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);
    I.seeInField("#editorApprootDir input.form-control", rootSubDirPath);
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
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");

    I.say("Wait until the child pages are indexed again");
    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    startEmbeddingTableAutoRefresh(I);
    I.waitForText(valueC, 100, "#datatableInit tbody");
    stopEmbeddingTableAutoRefresh(I);

    I.say("Check INDEX dialog");
    I.uncheckOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#editorApprootDir input.form-control", 10);
    I.seeInField("#editorApprootDir input.form-control", rootDirPath);

    checkIndexingStatusValues(I, 1, 5, 5, 0);

    I.say("Include sub-folders");
    I.checkOption("#include_subfolders");
    checkIndexingStatusValues(I, 2, 7, 7, 0);
});

Scenario('Stop indexing CronJob before switching embedding provider', ({ I, DT, DTE }) => {
    I.say("Turn off indexing before the Gemini reindex");
    setRagCronJob(I, DT, DTE, false, "*/1");
});

Scenario('Switch indexing assistant from OpenAI to Gemini', ({ I, DT, DTE }) => {
    setEmbeddingAssistant(I, DT, DTE, geminiProviderLabel, geminiModel);

    I.amOnPage("/admin/v9/settings/embedding-chunks/");
    checkCurrentEmbeddingConfiguration(I, geminiProvider, geminiModel);
});

Scenario('Queue the same pages for indexing with Gemini', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    checkCurrentEmbeddingConfiguration(I, geminiProvider, geminiModel);

    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#editorApprootDir input[value='" + rootDirPath + "']", 10);

    I.say("Switch to the child folder that currently has only OpenAI indexes");
    selectTree(I, ".rootDirDiv button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);
    I.seeElement("#editorApprootDir input[value='" + rootSubDirPath + "']");

    I.say("OpenAI indexes must not count as current Gemini indexes");
    checkIndexingStatusValues(I, 1, 2, 0, 0);

    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-primary");
    I.switchTo("#modalIframeIframeElement");
    I.waitForVisible("#succ-msg-index", 10);
    I.seeTextEquals("Webové stránky boli úspešne zaradené do fronty na indexovanie. (2)", "#succ-msg-index");
    checkIndexingStatusValues(I, 1, 2, 0, 2);
});

Scenario('Start indexing CronJob for Gemini indexes', ({ I, DT, DTE }) => {
    setRagCronJob(I, DT, DTE, true, "*/1");
});

Scenario('Gemini reindex preserves OpenAI indexes for the same page', ({ I, DT }) => {
    I.amOnPage("/admin/v9/settings/embedding-chunks/?rootDir=" + rootDirId + "#pills-document");
    checkCurrentEmbeddingConfiguration(I, geminiProvider, geminiModel);

    I.checkOption("#embeddingChunksDataTable_extfilter #includeSubfolders");
    DT.waitForLoader();
    DT.filterContains("chunkText", valueC);
    DT.filterEquals("embeddingProvider", geminiProvider);
    DT.filterEquals("embeddingModel", geminiModel);

    startEmbeddingTableAutoRefresh(I);
    I.waitForText(valueC, 100, "#datatableInit tbody");
    stopEmbeddingTableAutoRefresh(I);
    I.see(geminiProvider, "#datatableInit tbody");
    I.see(geminiModel, "#datatableInit tbody");

    I.say("The previous OpenAI index for the same page must still exist");
    DT.filterEquals("embeddingProvider", openAiProvider);
    DT.filterEquals("embeddingModel", openAiModel);
    I.see(valueC, "#datatableInit tbody");
    I.see(openAiProvider, "#datatableInit tbody");
    I.see(openAiModel, "#datatableInit tbody");

    I.say("The Gemini indexing preview must now recognize both child pages");
    DT.filterEquals("embeddingProvider", "");
    DT.filterEquals("embeddingModel", "");
    DT.filterContains("chunkText", "");
    I.clickCss("button.btnAddIndex");
    I.waitForVisible("#modalIframeIframeElement");
    I.switchTo("#modalIframeIframeElement");
    selectTree(I, ".rootDirDiv button.btn-vue-jstree-item-edit", ["Aplikácie", "Vyhľadávanie", "semantic_parent", "semantic_child"]);
    checkIndexingStatusValues(I, 1, 2, 2, 0);
});

Scenario('Restore OpenAI embedding provider', ({ I, DT, DTE }) => {
    I.switchTo();
    setEmbeddingAssistant(I, DT, DTE, openAiProviderLabel, openAiModel);

    I.amOnPage("/admin/v9/settings/embedding-chunks/");
    checkCurrentEmbeddingConfiguration(I, openAiProvider, openAiModel);
});

Scenario('Set CronJob to higher interval', ({ I, DT, DTE }) => {
    // Set to higher interval
    I.say("Turn ON indexing CronJob");
    setRagCronJob(I, DT, DTE, true, "*/30");
});

Scenario('Disable rag semantic search', ({ I, DT, Document }) => {
    Document.setConfigValue("ragSemanticSearchEnabled", "false");
});

function selectTree(I, buttonSelector, nodesArr) {
    I.clickCss(buttonSelector);
    I.waitForVisible(WebjetDteJsTree.tree, 10);

    for(let i = 0; i < nodesArr.length; i++) {
        if(i == nodesArr.length - 1) {
            I.click(locate(WebjetDteJsTree.anchors).withText(nodesArr[i]));
        } else {
            I.click(locate(WebjetDteJsTree.tree + ' .jstree-node.jstree-closed').withText(nodesArr[i]).find('.jstree-icon.jstree-ocl'));
        }
    }

    I.waitForInvisible(WebjetDteJsTree.tree, 10);
}

function checkIndexingStatusValues(I, allGroups, allDoc, indexedDoc, queuedDoc) {
    I.waitForElement("#allGroups", 10);
    I.waitForElement(locate("#allGroups").withText(allGroups + ""), 10);
    I.seeElement(locate("#allDoc").withText(allDoc + ""));
    I.seeElement(locate("#indexedDoc").withText(indexedDoc + ""));
    I.seeElement(locate("#queuedDoc").withText(queuedDoc + ""));
}

function checkPathInRootDir(I, dirPath) {
    I.seeInField("#embeddingChunksDataTable_extfilter #editorApprootDir input.form-control", dirPath);
}

function checkCurrentEmbeddingConfiguration(I, provider, model) {
    I.waitForElement("#toast-container-webjet > .toast-info", 10);
    I.see("Aktuálne nastavenie indexovania", "#toast-container-webjet");
    I.see("Poskytovateľ embeddingu", "#toast-container-webjet");
    I.see(provider, "#toast-container-webjet");
    I.see("Model embeddingu", "#toast-container-webjet");
    I.see(model, "#toast-container-webjet");
    I.toastrClose();
}

function setEmbeddingAssistant(I, DT, DTE, providerLabel, model) {
    I.amOnPage("/admin/v9/settings/embedding-chunks/");
    I.waitForElement("#toast-container-webjet", 10);
    I.toastrClose();

    I.amOnPage("/admin/v9/settings/ai-assistants/");
    DT.waitForLoader();
    DT.filterEquals("name", "RAG-EMB-INDEX");
    I.click("RAG-EMB-INDEX", "#datatableInit tbody");
    DTE.waitForEditor();

    I.clickCss("#pills-dt-datatableInit-provider-tab");
    DTE.selectOption("provider", providerLabel);
    I.waitForVisible("#DTE_Field_model", 10);
    I.fillField("#DTE_Field_model", model);
    DTE.save();
}

function startEmbeddingTableAutoRefresh(I) {
    I.executeScript(() => {
        window.embeddingIndexRefreshInterval = window.setInterval(() => {
            if(window.embeddingChunksDataTable != null) {
                window.embeddingChunksDataTable.ajax.reload(null, false);
            }
        }, 5000);
    });
}

function stopEmbeddingTableAutoRefresh(I) {
    I.executeScript(() => {
        window.clearInterval(window.embeddingIndexRefreshInterval);
        window.embeddingIndexRefreshInterval = null;
    });
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
