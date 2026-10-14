Feature('ai.token-limits').tag('@singlethread');

const assistantsUrl = '/admin/v9/settings/ai-assistants/';
const tokenLimitsTab = '#pills-dt-datatableInit-tokenLimits-tab';
const resetSelected = '#datatableInit_wrapper button.buttons-reset-token-usage';
const resetAll = '#datatableInit_wrapper button.buttons-reset-all-token-usage';
const confirmation = '#toast-container-webjet';
const aiContent = '#toast-container-ai-content';
let assistantName;

Before(({ I, login }) => {
    login('admin');
    if (!assistantName) assistantName = 'token-limit-autotest-' + I.getRandomText();
});

/**
 * Verifies that users without the AI tools permission cannot access assistant settings.
 */
Scenario('token limits require AI tools permission', ({ DT }) => {
    DT.checkPerms('cmp_ai_tools', assistantsUrl);
});

/**
 * Creates a temporary gpt-4.1-nano assistant and validates its one-token limit.
 * Verifies that a successful call records usage in the counter and 14-day chart,
 * then blocks another call even with the assistant definition cached on the page.
 * Checks reset button states and both confirmation dialogs, cancels the domain-wide
 * reset, and resets the selected assistant. Confirms that another call succeeds
 * after the reset while historical chart data remains intact.
 */
Scenario('limit blocks further calls and selected reset restores usage', async ({ I, DT, DTE }) => {
    I.amOnPage(assistantsUrl);
    DT.waitForLoader();
    I.clickCss('#datatableInit_wrapper button.buttons-create');
    DTE.waitForEditor();
    I.fillField('#DTE_Field_name', assistantName);
    I.fillField('#DTE_Field_description', assistantName);

    I.clickCss('#pills-dt-datatableInit-action-tab');
    DTE.selectOption('action', 'Vygenerovať text');
    I.fillField('#DTE_Field_className', 'sk.iway.iwcm.doc.DocDetails');
    I.fillField('#DTE_Field_fieldFrom', 'fieldS');
    I.fillField('#DTE_Field_fieldTo', 'fieldS');

    I.clickCss('#pills-dt-datatableInit-provider-tab');
    DTE.selectOption('provider', 'OpenAI');
    I.waitForVisible('#DTE_Field_model', 10);
    I.fillField('#DTE_Field_model', 'gpt-4.1-nano');

    I.clickCss('#pills-dt-datatableInit-instructions-tab');
    I.fillField('#DTE_Field_instructions', 'Reply with exactly the word OK.');
    I.clickCss('#pills-dt-datatableInit-advanced-tab');
    I.uncheckOption('#DTE_Field_useStreaming_0');
    I.uncheckOption('#DTE_Field_userPromptEnabled_0');

    I.clickCss(tokenLimitsTab);
    I.dontSeeCheckboxIsChecked('#DTE_Field_useTokenLimits_0');
    I.dontSeeElement('#DTE_Field_tokenLimit');
    I.dontSeeElement('#DTE_Field_usedTokens');
    I.checkOption('#DTE_Field_useTokenLimits_0');
    I.seeElement('#DTE_Field_usedTokens:disabled');
    I.seeElement('#DTE_Field_tokenLimit[required]');
    I.fillField('#DTE_Field_tokenLimit', '0');
    DTE.save();
    I.waitForText('Zadajte limit tokenov väčší ako 0.', 10, '#datatableInit_modal');
    I.fillField('#DTE_Field_tokenLimit', '1');
    DTE.save();
    DTE.waitForModalClose();

    openAssistant(I, DT, DTE);
    I.seeCheckboxIsChecked('#DTE_Field_useTokenLimits_0');
    I.seeInField('#DTE_Field_tokenLimit', '1');
    await checkUsageChart(I, 0);
    DTE.cancel();

    openWebPage(I, DTE);
    const spentTokens = await runAssistant(I);
    I.assertAbove(spentTokens, 1, 'The first provider call must exceed the one-token limit.');
    closeAssistantToast(I);

    // Reuse the page's cached assistant definition to verify the database limit check.
    startAssistant(I);
    I.waitForText('Asistent dosiahol limit tokenov a nie je možné ho ďalej používať.', 30, aiContent);
    I.dontSeeElement(aiContent + ' button.btn-ai-undo');
    closeAssistantToast(I);
    DTE.cancel();

    openAssistant(I, DT, DTE);
    I.seeInField('#DTE_Field_usedTokens', String(spentTokens));
    I.seeElement('#DTE_Field_usedTokens:disabled');
    await checkUsageChart(I, spentTokens);
    const assistantId = await I.executeScript(() => aiAssistantsDataTable.EDITOR.currentJson.id);
    DTE.cancel();

    I.amOnPage(assistantsUrl);
    DT.waitForLoader();
    DT.filterEquals('name', assistantName);
    I.seeElement('#datatableInit tbody tr.out-of-tokens');
    I.seeElement(resetSelected + ':disabled');
    I.seeElement(resetAll + ':enabled');

    // Check the domain-wide confirmation without changing other assistants' usage.
    I.clickCss(resetAll);
    I.waitForText('Vynulovanie tokenov', 10, confirmation + ' .toast-title');
    I.see('Naozaj chcete vynulovať použité tokeny všetkých asistentov v aktuálnej doméne?', confirmation);
    I.clickCss(confirmation + ' button[id^="confirmationNo"]');
    I.waitForInvisible(confirmation + ' .toastr-buttons', 10);

    I.clickCss('#datatableInit tbody td.dt-select-td');
    I.seeElement(resetSelected + ':enabled');
    I.seeElement(resetAll + ':disabled');
    I.clickCss(resetSelected);
    I.waitForText('Vynulovanie tokenov', 10, confirmation + ' .toast-title');
    I.see('Naozaj chcete vynulovať použité tokeny vybraných asistentov?', confirmation);
    I.clickCss(confirmation + ' button[id^="confirmationYes"]');
    I.waitForText('Použité tokeny asistenta s ID ' + assistantId + ' boli vynulované.', 15, confirmation);
    DT.waitForLoader();
    I.waitForInvisible('#datatableInit tbody tr.out-of-tokens', 10);
    I.clickCss(confirmation + ' .toast-close-button');

    openAssistant(I, DT, DTE);
    I.seeInField('#DTE_Field_usedTokens', '0');
    I.seeInField('#DTE_Field_tokenLimit', '1');
    I.seeCheckboxIsChecked('#DTE_Field_useTokenLimits_0');
    // Resetting the counter must preserve historical statistics.
    await checkUsageChart(I, spentTokens);
    DTE.cancel();

    openWebPage(I, DTE);
    const tokensAfterReset = await runAssistant(I);
    I.assertAbove(tokensAfterReset, 1, 'The assistant must work again after resetting usage.');
    closeAssistantToast(I);
    DTE.cancel();

    openAssistant(I, DT, DTE);
    I.seeInField('#DTE_Field_usedTokens', String(tokensAfterReset));
    await checkUsageChart(I, spentTokens + tokensAfterReset);
    DTE.cancel();
});

/**
 * Deletes the temporary assistant if it exists and verifies its removal.
 * Runs separately so cleanup is attempted even if the functional scenario fails.
 */
Scenario('cleanup token-limit assistant', async ({ I, DT }) => {
    I.amOnPage(assistantsUrl);
    DT.waitForLoader();
    DT.filterEquals('name', assistantName);
    if (await I.grabNumberOfVisibleElements(locate('#datatableInit tbody td.dt-row-edit').withText(assistantName)) > 0) {
        DT.deleteAll();
        DT.waitForLoader();
    }
    I.dontSee(assistantName, '#datatableInit tbody');
});

function openAssistant(I, DT, DTE) {
    I.amOnPage(assistantsUrl);
    DT.waitForLoader();
    DT.filterEquals('name', assistantName);
    I.click(assistantName, '#datatableInit tbody');
    DTE.waitForEditor();
    I.clickCss(tokenLimitsTab);
}

/**
 * Opens the existing AI test page and prepares fieldS as the assistant input.
 * The caller must cancel the editor to discard changes to the shared page.
 *
 * @param {CodeceptJS.I} I - CodeceptJS actor.
 * @param {typeof import('../../pages/DTE.js')} DTE - DataTable editor helpers.
 */
function openWebPage(I, DTE) {
    // Use the existing AI test page and discard all field changes with DTE.cancel().
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=128791');
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss('#pills-dt-datatableInit-fields-tab');
    I.fillField('#DTE_Field_fieldS', 'autotest');
}

function startAssistant(I) {
    I.clickCss('.DTE_Field_Name_fieldS button.btn-ai');
    I.waitForVisible('#toast-container-ai', 10);
    I.click(locate('#toast-container-ai button.btn-ai-action').withText(assistantName));
}

/**
 * Runs the temporary assistant from fieldS and reads its reported token usage.
 * Requires the webpage editor's Fields tab to be open; the response replaces fieldS.
 *
 * @param {CodeceptJS.I} I - CodeceptJS actor.
 * @returns {Promise<number>} Token total parsed from the successful response message.
 */
async function runAssistant(I) {
    startAssistant(I);
    const status = aiContent + ' > .current-status > span';
    I.waitForText('Hotovo! Bolo použitých', 90, status);
    I.waitForVisible(aiContent + ' button.btn-ai-undo', 10);
    const message = await I.grabTextFrom(status);
    return Number(message.replace(/\D/g, ''));
}

function closeAssistantToast(I) {
    I.clickCss('#toast-container-ai .toast-close-button');
    I.waitForInvisible('#toast-container-ai', 10);
}

/**
 * Verifies the visible usage chart contains 14 daily entries with the expected total.
 * Requires the assistant editor's Token limits tab and usage panel to be open.
 *
 * @param {CodeceptJS.I} I - CodeceptJS actor.
 * @param {number} expectedTokens - Expected sum of historical usage, including usage before a reset.
 * @returns {Promise<void>} Resolves after reading the chart data and queuing its assertions.
 */
async function checkUsageChart(I, expectedTokens) {
    I.waitForVisible('#tokenUsagePanel #graphsDiv', 30);
    I.waitForVisible('#ai-assistant-token-usage canvas', 30);
    I.see('Denná spotreba tokenov za posledných 14 dní (vrátane dneška)', '#tokenUsagePanel');
    const data = await I.executeScript(() => {
        const root = am5.registry.rootElements.find(root => root.dom.id === 'ai-assistant-token-usage');
        const chart = root.container.children.values.find(child => child.series);
        return chart.series.getIndex(0).data.values;
    });
    I.assertLengthOf(data, 14, 'The chart must include all 14 days, including days without usage.');
    I.assertEqual(data.reduce((sum, day) => sum + day.tokens, 0), expectedTokens, 'The chart must show the recorded token totals.');
}
