Feature('webpages.group-approve-logic');

// Shared state variables initialized in Before hook
var randomNumber;
var testerEmail;
var tester2Email;
var emailDomain;

var newFolderName = "Group_approving_";
const baseFolderId = 114389; // ID of "Group_approving" folder under which the test will create a new folder for approval tests

const HISTORY_TABLE_ID = "datatableFieldDTE_Field_editorFields-groupSchedulerChangeHistory";

Before(({ I, TempMail }) => {
    // Initialize random data only once for entire test suite
    if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
        var randomShort = I.getRandomTextShort();
        testerEmail = "wjtester." + randomShort;
        tester2Email = "wjtester2." + randomShort;
        emailDomain = TempMail.getTempMailDomain();
        newFolderName += randomNumber;
    }
    I.switchTo();
    I.closeOtherTabs();
});

/**
 * Sets the email address for a given user in the user list.
 */
function setUserEmail(login, email, I, DT, DTE) {
    I.amOnPage("/admin/v9/users/user-list/");
    DT.filterContains("login", login);
    I.click(login);
    DTE.waitForEditor();
    DTE.fillField("email", email);
    DTE.save();
}

/**
 * Navigates to the test folder in the jstree and opens its editor.
 */
function openCreatedFolder(I, DT, DTE, folderName) {
    I.switchTo();
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeNavigate(["Test stavov", "Group_approving", folderName]);

    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");
}

/**
 * Opens the approval link from the latest email and switches to the new tab.
 */
async function openApprovalLinkFromEmail(I, Document, TempMail, emailAccount) {
    await TempMail.login(emailAccount);
    await TempMail.openLatestEmail();

    I.waitForElement("#info a", 5);
    I.clickCss("#info a");
    await Document.waitForTab();
    I.switchToNextTab();
    I.waitForVisible("#modalIframe", 10);
    I.wait(1);
    I.switchTo("#modalIframeIframeElement");
    I.wait(1);
}

/**
 * Verifies that folder fields (A-D) have the expected values in the fields tab.
 */
function checkFolderFields(I, fieldA, fieldB, fieldC, fieldD) {
    I.clickCss("#pills-dt-groups-datatable-fields-tab");
    I.seeInField("#DTE_Field_fieldA", fieldA);
    I.seeInField("#DTE_Field_fieldB", fieldB);
    I.seeInField("#DTE_Field_fieldC", fieldC);
    I.seeInField("#DTE_Field_fieldD", fieldD);
}

function fillFolderFields(I, fieldA, fieldB, fieldC, fieldD) {
    I.clickCss("#pills-dt-groups-datatable-fields-tab");
    I.fillField("#DTE_Field_fieldA", fieldA);
    I.fillField("#DTE_Field_fieldB", fieldB);
    I.fillField("#DTE_Field_fieldC", fieldC);
    I.fillField("#DTE_Field_fieldD", fieldD);
}

/**
 * Verifies that permission checkboxes have the expected state.
 */
function checkPermissions(I, checked) {
    I.clickCss("#pills-dt-groups-datatable-access-tab");
    if (checked) {
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_0");
        I.seeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
    } else {
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_0");
        I.dontSeeCheckboxIsChecked("#DTE_Field_editorFields-permisions_1");
    }
}

async function goOnEmailUrl(I, Document) {
    I.say("Open approval link from email");
    I.switchTo();
    I.waitForElement("#info a", 5);
    I.clickCss("#info a");
    await Document.waitForTab();
    I.switchToNextTab();
}

/**
 * Opens the folder history tab and waits for it to load.
 */
function openFolderHistoryTab(I, DT) {
    I.clickCss("#pills-dt-groups-datatable-history-tab");
    DT.waitForLoader();
}

/**
 * Verifies a row in the diff HTML table by title, old value and new value.
 */
function checkDiffTableRow(I, title, oldValue, newValue) {
    I.seeElement({xpath: `//table//tbody/tr[td[1][normalize-space()='${title}'] and td[2][normalize-space()='${oldValue}'] and td[3][normalize-space()='${newValue}']]`});
}

/**
 * Verifies the diff fields displayed on the approval page.
 */
function checkApprovalDiff(I, oldName, newName, fieldA, fieldB, fieldC, fieldD) {
    I.say("checkApprovalDiff()");

    checkDiffTableRow(I, "Názov priečinku", oldName, newName);
    checkDiffTableRow(I, "Pole A", "", fieldA);
    checkDiffTableRow(I, "Pole B", "", fieldB);
    checkDiffTableRow(I, "Pole C", "", fieldC);
    checkDiffTableRow(I, "Pole D", "", fieldD);
    checkDiffTableRow(I, "Prístupný iba pre", "", "Bankári, Blog (4,802)");
}

/**
 * Edits a folder as non-approver: changes name, enables permissions, fills custom fields, saves.
 */
function editFolderWithAllChanges(I, DT, DTE, Document, currentFolderName, newName, fieldA, fieldB, fieldC, fieldD) {
    I.say("editFolderWithAllChanges()");

    openCreatedFolder(I, DT, DTE, currentFolderName);

    I.fillField("#DTE_Field_groupName", newName);

    I.clickCss("#pills-dt-groups-datatable-access-tab");
    I.checkOption("#DTE_Field_editorFields-permisions_0");
    I.checkOption("#DTE_Field_editorFields-permisions_1");

    fillFolderFields(I, fieldA, fieldB, fieldC, fieldD);

    DTE.save();
    Document.notifyCheckAndClose("Žiadosť o schválenie priečinka dostal: Publish Notification, Tester Playwright");
}

/**
 * Requests folder deletion as non-approver and verifies the approval notification.
 */
function requestFolderDeletion(I, DT, DTE, Document, folderName) {
    I.say("requestFolderDeletion()");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeNavigate(["Test stavov", "Group_approving", folderName]);

    I.click(DT.btn.tree_delete_button);
    DTE.waitForEditor("groups-datatable");
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();

    Document.notifyCheckAndClose("Žiadosť o zmazanie priečinka dostal: Publish Notification, Tester Playwright");
}

/**
 * Closes current tab, reopens the approval link from email, and verifies the already-processed message.
 */
async function reopenEmailLinkAndCheckMessage(I, Document, expectedMessage) {
    I.say("reopenEmailLinkAndCheckMessage()");
    I.closeCurrentTab();
    await goOnEmailUrl(I, Document);
    I.waitForVisible("#modalIframe", 10);
    I.wait(1);
    I.switchTo("#modalIframeIframeElement");
    I.wait(1);
    I.waitForText(expectedMessage, 5);
    I.switchTo();
}

/**
 * Verifies the delete approval page heading is shown and no diff is displayed.
 */
function checkDeleteApprovalPage(I) {
    I.say("checkDeleteApprovalPage()");
    I.switchTo();
    I.seeElement( locate("h5").withText("Vymazanie priečinka") );
    I.switchTo("#modalIframeIframeElement");
    I.waitForElement("#note", 5);
    I.dontSeeElement("#diff");
}

Scenario('prepare users for approve logic tests @screenshot', ({I, DT, DTE}) => {
    I.say("Set temp email addresses for both test users");
    I.relogin("admin");
    setUserEmail("tester", testerEmail + emailDomain, I, DT, DTE);
    setUserEmail("tester2", tester2Email + emailDomain, I, DT, DTE);
});

// ------------------------
// CREATE LOGIC TESTS
// ------------------------

Scenario('Create new folder that requires approval @screenshot', async ({I, DT, DTE, Document}) => {
    I.say("Log as NON-approver and create new folder");
    I.relogin("tester2");

    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + baseFolderId);
    DT.waitForLoader();

    I.click(DT.btn.tree_add_button);
    DTE.waitForEditor("groups-datatable");

    I.fillField("#DTE_Field_groupName", newFolderName);
    DTE.save();

    I.waitForVisible("div.toast-container div.toast", 5);
    Document.screenshotElement("div.toast-container div.toast", "/redactor/webpages/approve/approve-notification.png");

    Document.notifyCheckAndClose("Žiadosť o schválenie pridania nového priečinka dostal: Publish Notification, Tester Playwright");

    I.say("Check created new folder - name should match, visibility should be internal");
    I.jstreeNavigate(["Test stavov", "Group_approving", newFolderName]);
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");

    I.seeInField("#DTE_Field_groupName", newFolderName);
    I.seeElement( locate(".DTE_Field_Name_internal div.filter-option-inner-inner").withText("Nedostupné/Neverejné (interné pre administrátorov)") );

    I.say("Check history table in tab");
    openFolderHistoryTab(I, DT);

    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName, "Tester2 Playwright2", "neschválené"]);

    Document.screenshotElement(".DTE.modal-content.DTE_Action_Edit", "/redactor/webpages/approve/group-history-tab.png");

    DTE.cancel();

    I.say("Check automatically created document in the new folder");
    DT.filterEquals("title", newFolderName);
    I.clickCss("#datatableInit_wrapper  td.sorting_1");
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.dontSeeCheckboxIsChecked("#DTE_Field_available_0");

    I.clickCss("#pills-dt-datatableInit-history-tab");
    DT.waitForLoader();
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-history", 1, [null, null, null, null, newFolderName, "Tester2 Playwright2", "neschvaľovalo sa"]);
});

Scenario('Check approve email and approve logic @screenshot', async ({I, Document, TempMail}) => {

    I.relogin("tester2");

    I.say("Verify approval request email was sent to approver");
    await TempMail.login(testerEmail);
    await TempMail.openLatestEmail();

    Document.screenshotElement("div.letter", "/redactor/webpages/approve/approve-group-email.png");

    I.see("Žiadosť o schválenie priečinka: " + newFolderName);
    I.see("Žiadam Vás o schválenie priečinka:");
    I.see("Polia priečinka:");

    await goOnEmailUrl(I, Document);

    I.say("Try to approve as non-approver - should be denied");
    I.waitForElement( locate("h5").withText("Schvaľovanie priečinka"), 10 );
    I.clickCss("#modalIframe .modal-footer-custom-buttons button.btn-success");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Na schválenie/zamietnutie tohto priečinka nemáte práva", 5);
    I.switchTo();

    I.say("Relogin as approver (admin) and approve the folder");
    I.relogin("admin");
    await TempMail.login(testerEmail);
    await TempMail.openLatestEmail();
    await goOnEmailUrl(I, Document);

    I.waitForElement( locate("h5").withText("Schvaľovanie priečinka") );

    I.switchTo("#modalIframeIframeElement");
    I.fillField("#note", "Good job, approving.");
    I.switchTo();

    Document.screenshot("/redactor/webpages/approve/approve-group-page.png");

    I.clickCss("#modalIframe .modal-footer-custom-buttons button.btn-success");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Schválenie/zamietnutie priečinka bolo úspešné", 5);
    I.switchTo();

    I.say("Re-open the same approval link - should show already approved");
    await reopenEmailLinkAndCheckMessage(I, Document, "Priečinok už bol schválený používateľom: Tester Playwright");
});

Scenario('Check approved structure ', async ({I, DT, DTE}) => {

    I.say("Verify folder name and visibility changed to public after approval");
    openCreatedFolder(I, DT, DTE, newFolderName);

    I.seeInField("#DTE_Field_groupName", newFolderName);
    I.seeElement( locate(".DTE_Field_Name_internal div.filter-option-inner-inner").withText("Dostupné/Verejné") );

    I.say("Check history table - should contain exactly one approved record");
    openFolderHistoryTab(I, DT);

    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright", ""]);

    // Verify only one history record exists at this point
    const folderHistoryCount = await I.grabNumberOfVisibleElements("#" + HISTORY_TABLE_ID + " tbody tr");
    I.assertEqual(folderHistoryCount, 1);

    DTE.cancel();

    I.say("Check document inside folder - should be available after approval");
    DT.filterEquals("title", newFolderName);
    I.clickCss("#datatableInit_wrapper td.sorting_1");
    I.click(DT.btn.edit_button);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab");
    I.seeCheckboxIsChecked("#DTE_Field_available_0");

    I.clickCss("#pills-dt-datatableInit-history-tab");
    DT.waitForLoader();
    DT.checkTableRow("datatableFieldDTE_Field_editorFields-history", 1, [null, null, null, null, newFolderName, "Tester2 Playwright2", "neschvaľovalo sa"]);
});

Scenario('Check email that confirms approval @screenshot', async ({I, TempMail, Document}) => {
    I.say("Verify that requester (tester2) received approval confirmation email");
    await TempMail.login(tester2Email);
    await TempMail.openLatestEmail();

    Document.screenshotElement("div.letter", "/redactor/webpages/approve/approve-group-notify-mail.png");

    I.see("Priečinok schválený: " + newFolderName);
    I.see("Priečinok je SCHVÁLENÝ:");
    I.see("Good job, approving.");
});

// ------------------------
// EDIT LOGIC TESTS
// ------------------------

Scenario('Edit folder that needs approval - as NON-approver ', async ({I, DT, DTE, Document}) => {
    I.relogin("tester2");

    I.say("Make multiple changes to folder as non-approver: name, permissions, custom fields");
    editFolderWithAllChanges(I, DT, DTE, Document, newFolderName, newFolderName + "_edited", "AAA", "BBB", "CCC", "DDD");

    I.say("Verify that no changes were applied yet - folder should have original values");
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");
    I.seeInField("#DTE_Field_groupName", newFolderName);

    checkPermissions(I, false);
    checkFolderFields(I, "", "", "", "");

    I.say("Verify pending change appears in history alongside the approved creation");
    openFolderHistoryTab(I, DT);
    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName + "_edited", "Tester2 Playwright2", "neschválené"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 2, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright"]);
});

Scenario('Go check email and reject request for edit ', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Verify approval request email contains all changed fields as diff");
    await TempMail.login(testerEmail);
    await TempMail.openLatestEmail();
    I.see("Žiadosť o schválenie priečinka: " + newFolderName);
    I.see("Žiadam Vás o schválenie priečinka:");
    I.see("Zmenené polia priečinka:");
    checkDiffTableRow(I, "Názov priečinku", newFolderName, newFolderName + "_edited");
    checkDiffTableRow(I, "Pole A", "", "AAA");
    checkDiffTableRow(I, "Pole B", "", "BBB");
    checkDiffTableRow(I, "Pole C", "", "CCC");
    checkDiffTableRow(I, "Pole D", "", "DDD");
    checkDiffTableRow(I, "Prístupný iba pre", "", "Bankári, Blog (4,802)");

    I.say("Try to reject as non-approver (tester2) - should be denied");
    await goOnEmailUrl(I, Document);

    I.seeElement( locate("h5").withText("Schvaľovanie priečinka") );
    I.clickCss("#modalIframe .modal-footer button.btn-danger");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Na schválenie/zamietnutie tohto priečinka nemáte práva", 5);
    I.switchTo();

    I.say("Relogin as approver (admin) and reject the change with a note");
    I.relogin("admin");
    await TempMail.login(testerEmail);
    await TempMail.openLatestEmail();
    await goOnEmailUrl(I, Document);

    I.say("Verify diff is displayed on the approval page");
    I.switchTo("#modalIframeIframeElement");
    checkApprovalDiff(I, newFolderName, newFolderName + "_edited", "AAA", "BBB", "CCC", "DDD");

    I.fillField("#note", "Its bad, NOT approving.");
    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-danger");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Schválenie/zamietnutie priečinka bolo úspešné", 5);
    I.switchTo();

    I.say("Re-open the same approval link - should show already rejected");
    await reopenEmailLinkAndCheckMessage(I, Document, "Priečinok už bol zamietnutý používateľom: Tester Playwright");
});

Scenario('Check group does NOT changed after rejection ', async ({I, DT, DTE, Document, TempMail}) => {

    I.relogin("admin");

    openCreatedFolder(I, DT, DTE, newFolderName);

    I.say("Verify folder remains unchanged after rejection - original values preserved");
    I.seeInField("#DTE_Field_groupName", newFolderName);

    checkPermissions(I, false);
    checkFolderFields(I, "", "", "", "");

    I.say("Verify history shows rejected record alongside the approved creation");
    openFolderHistoryTab(I, DT);
    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName + "_edited", "Tester2 Playwright2", "", "Tester Playwright"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 2, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright"]);
});

Scenario('Check email that confirms rejection ', async ({I, TempMail}) => {
    I.say("Verify that requester (tester2) received rejection notification email with note");
    await TempMail.login(tester2Email);
    await TempMail.openLatestEmail();

    I.see("Zmena priečinka zamietnutá: " + newFolderName + "_edited");
    I.see("Pripomienkovanie priečinka:");
    I.see("Its bad, NOT approving.");
});

Scenario('Edit folder again as NON-approver and APPROVE the change ', async ({I, DT, DTE, Document, TempMail}) => {

    I.say("Make changes to folder as non-approver - name, permissions, custom fields");
    I.relogin("tester2");

    editFolderWithAllChanges(I, DT, DTE, Document, newFolderName, newFolderName + "_edited_2", "AAA_2", "BBB_2", "CCC_2", "DDD_2");

    I.say("Open approval link from email as approver and verify diff");
    I.relogin("admin");
    await openApprovalLinkFromEmail(I, Document, TempMail, testerEmail);

    checkApprovalDiff(I, newFolderName, newFolderName + "_edited_2", "AAA_2", "BBB_2", "CCC_2", "DDD_2");

    I.fillField("#note", "Now its good, approving.");
    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-success");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Schválenie/zamietnutie priečinka bolo úspešné", 5);
    I.switchTo();

    I.say("Verify all changes were applied after approval");
    openCreatedFolder(I, DT, DTE, newFolderName);

    I.seeInField("#DTE_Field_groupName", newFolderName + "_edited_2");

    checkPermissions(I, true);
    checkFolderFields(I, "AAA_2", "BBB_2", "CCC_2", "DDD_2");

    I.say("Verify full history: approved edit, rejected edit, initial creation");
    openFolderHistoryTab(I, DT);
    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName + "_edited_2", "Tester2 Playwright2", "Tester Playwright", ""]);
    DT.checkTableRow(HISTORY_TABLE_ID, 2, [null, null, newFolderName + "_edited", "Tester2 Playwright2", "", "Tester Playwright"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 3, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright"]);
});

Scenario('Edit folder AS APPROVER - no approval needed ', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Edit folder directly as approver - changes should be applied immediately");
    I.relogin("admin");

    openCreatedFolder(I, DT, DTE, newFolderName);

    I.fillField("#DTE_Field_groupName", newFolderName + "_edited_3");

    DTE.save();
    I.jstreeWaitForLoader();
    DT.waitForLoader();

    I.say("Verify no approval notification toast was shown");
    I.dontSeeElement("div.toast-container div.toast");

    I.say("Re-open folder and verify changes were applied immediately");
    I.click(DT.btn.tree_edit_button);
    DTE.waitForEditor("groups-datatable");

    I.seeInField("#DTE_Field_groupName", newFolderName + "_edited_3");

    I.say("Verify full history: direct edit (no approval), approved edit, rejected edit, creation");
    openFolderHistoryTab(I, DT);
    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, newFolderName + "_edited_3", "Tester Playwright", "neschvaľovalo sa"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 2, [null, null, newFolderName + "_edited_2", "Tester2 Playwright2", "Tester Playwright", ""]);
    DT.checkTableRow(HISTORY_TABLE_ID, 3, [null, null, newFolderName + "_edited", "Tester2 Playwright2", "", "Tester Playwright"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 4, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright"]);
});

// ------------------------
// DELETE LOGIC TESTS
// ------------------------

Scenario('Delete folder as NON-Approver - requires approval ', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Try to delete folder as non-approver - should require approval");
    I.relogin("tester2");

    //delete old emails because of possible multiple email notifications from previous tests
    await TempMail.login(testerEmail);
    await TempMail.destroyInbox();

    requestFolderDeletion(I, DT, DTE, Document, newFolderName + "_edited_3");
});

Scenario('Stay logged as NON-Approver and try approve delete - must be refused ', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Open deletion approval email and reject the request");
    await TempMail.login(testerEmail);
    await TempMail.openLatestEmail();

    I.see("Žiadosť o vymazanie priečinka: " + newFolderName + "_edited_3");
    I.see("Žiadam vás o vymazanie priečinka:");

    await goOnEmailUrl(I, Document);

    I.waitForVisible("#modalIframe", 10);
    I.waitForElement( locate("h5").withText("Vymazanie priečinka"), 10 );
    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Na schválenie/zamietnutie tohto priečinka nemáte práva", 5);
    I.switchTo();
});

Scenario('Log as approver and reject deletion request - then check that folder was NOT deleted', async ({I, DT, DTE, Document, TempMail}) => {
    I.relogin("admin");

    await openApprovalLinkFromEmail(I, Document, TempMail, testerEmail);

    checkDeleteApprovalPage(I);
    I.fillField("#note", "I dont want to delete this, rejecting.");

    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-danger");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Schválenie/zamietnutie vymazania priečinka bolo úspešné", 5);
    I.switchTo();

    I.say("Check that link does not work anymore after rejection");
    await reopenEmailLinkAndCheckMessage(I, Document, "Zmazanie priečinka už bolo zamietnuté používateľom: Tester Playwright");

    I.say("Go back to folder list and verify folder is still present");
    openCreatedFolder(I, DT, DTE, newFolderName + "_edited_3");

    openFolderHistoryTab(I, DT);

    DT.checkTableRow(HISTORY_TABLE_ID, 1, [null, null, "[DELETE] " + newFolderName + "_edited_3", "Tester2 Playwright2", "", "Tester Playwright"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 2, [null, null, newFolderName + "_edited_3", "Tester Playwright", "neschvaľovalo sa"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 3, [null, null, newFolderName + "_edited_2", "Tester2 Playwright2", "Tester Playwright", ""]);
    DT.checkTableRow(HISTORY_TABLE_ID, 4, [null, null, newFolderName + "_edited", "Tester2 Playwright2", "", "Tester Playwright"]);
    DT.checkTableRow(HISTORY_TABLE_ID, 5, [null, null, newFolderName, "Tester2 Playwright2", "Tester Playwright"]);
});

Scenario('Check that requester obtained email about action reject ', async ({I, DT, DTE, Document, TempMail}) => {
    await TempMail.login(tester2Email);
    await TempMail.openLatestEmail();

    I.see("Zmena priečinka zamietnutá: " + newFolderName + "_edited_3");
    I.see("Vymazanie priečinka je zamietnuté:");
    I.see("I dont want to delete this, rejecting.");
});

Scenario('Again request for delete and this time approve it ', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Try to delete folder as non-approver - should require approval");
    I.relogin("tester2");

    requestFolderDeletion(I, DT, DTE, Document, newFolderName + "_edited_3");

    I.say("Relog as admin and approve delete request");
    I.relogin("admin");

    await openApprovalLinkFromEmail(I, Document, TempMail, testerEmail);

    checkDeleteApprovalPage(I);
    I.fillField("#note", "Approving delete.");

    I.switchTo();
    I.clickCss("#modalIframe .modal-footer button.btn-success");

    I.switchTo("#modalIframeIframeElement");
    I.waitForText("Schválenie/zamietnutie vymazania priečinka bolo úspešné", 5);
    I.switchTo();

    I.say("Verify that folder was deleted");
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.clickCss("#pills-trash-tab");
    DT.waitForLoader();

    I.fillField("#tree-folder-search-input", newFolderName + "_edited_3");
    I.clickCss("#tree-folder-search-button");
    DT.waitForLoader();
    I.seeElement( locate("a.jstree-anchor.jstree-search").withText(newFolderName + "_edited_3") );

    I.say('Check that requester obtained email about deletion approval');
    await TempMail.login(tester2Email);
    await TempMail.openLatestEmail();

    I.see("Žiadosť o vymazanie priečinka: " + newFolderName + "_edited_3");
    I.see("Vymazanie priečinka bolo schválené:");
    I.see("Approving delete.");
});

Scenario('Destroy both email inboxes', async ({I, DT, DTE, Document, TempMail}) => {
    I.say("Destroy temp email inboxes used for testing");
    await TempMail.login(testerEmail);
    await TempMail.destroyInbox();

    await TempMail.login(tester2Email);
    await TempMail.destroyInbox();
});

Scenario('revert users emails @screenshot', async ({I, DT, DTE, TempMail}) => {
    I.closeOtherTabs();
    I.switchTo();
    I.relogin("admin");
    setUserEmail("tester", "tester@balat.sk", I, DT, DTE);
    setUserEmail("tester2", "tester2@balat.sk", I, DT, DTE);

    await TempMail.login(testerEmail);
    await TempMail.destroyInbox();

    await TempMail.login(tester2Email);
    await TempMail.destroyInbox();
});