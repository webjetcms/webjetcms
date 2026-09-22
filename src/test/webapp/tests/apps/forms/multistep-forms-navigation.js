const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

Feature('apps.forms.multistep-forms-navigation');

const formName = 'autotest-navigation';
const fixturePath = '/apps/multistep-formular/autotest-navigation.html';
const modulePath = '/apps/form/mvc/multistep-form.js';
const cssPath = '/apps/form/mvc/default.css';
const webRoot = path.resolve(__dirname, '../../../../../main/webapp');

/**
 * Runs the production browser module against controlled step responses without saving test submissions.
 */
async function openForm(I, terminalError = false, threeSteps = false) {
    const submissions = [];
    await I.usePlaywrightTo('prepare multistep navigation responses', async ({ page }) => {
        const instances = new Map();
        const asArray = value => value == null ? [] : (Array.isArray(value) ? value : [value]);
        const firstStep = `<form action="/rest/multistep-form/save-form?step-id=1">
            <div class="form-group"><label><input id="f1-subscribe" name="f1-subscribe" type="checkbox" value="yes">Subscribe</label></div>
            <div class="form-group">
                <label><input id="f1-departments-0" name="f1-departments" type="checkbox" value="Sales, Europe">Sales, Europe</label>
                <label><input id="f1-departments-1" name="f1-departments" type="checkbox" value="Support">Support</label>
            </div>
            <button type="submit">Next</button>
        </form>`;
        const secondStep = `<form action="/rest/multistep-form/save-form?step-id=2">
            <div class="form-group"><label for="f1-details">Details</label><input id="f1-details" name="f1-details"><div class="cs-error-f1-details"></div></div>
            <button type="button" data-multistep-back-step="1">Back</button>
            ${threeSteps ? `<div class="form-group"><input id="f1-clear" value="autotest default"></div>
            <div class="form-group">
                <input type="checkbox" id="f1-options-0" name="f1-options" value="Sales, Europe">
                <input type="checkbox" id="f1-options-1" name="f1-options" value="Support">
            </div>
            <div class="form-group"><input type="radio" id="f1-radio-0" name="f1-radio" value="one"><input type="radio" id="f1-radio-1" name="f1-radio" value="two"></div>` : ''}
            <button type="submit">${threeSteps ? 'Next' : 'Submit'}</button>
        </form>`;
        const thirdStep = `<form action="/rest/multistep-form/save-form?step-id=3">
            <div class="form-group"><input id="f1-email"><div class="cs-error-f1-email"></div></div>
            <div class="form-group"><textarea id="f1-comment"></textarea></div>
            <button type="button" data-multistep-back-step="2">Back</button>
            <button type="submit">Submit</button>
        </form>`;

        await page.route('**/*', async route => {
            const url = new URL(route.request().url());
            if (url.pathname === fixturePath) {
                return route.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><link rel="stylesheet" href="${cssPath}"></head><body>
                    <div id="multistep-form-wrapper-autotest"></div>
                    <script type="module">
                        import { MultistepForm } from '${modulePath}';
                        new MultistepForm({formName: '${formName}', stepId: '1', csrf: 'autotest', language: 'en', successMessage: 'Saved autotest form'}).start();
                    </script></body></html>` });
            }
            if (url.pathname === modulePath || url.pathname === cssPath) {
                return route.fulfill({
                    contentType: url.pathname === modulePath ? 'text/javascript' : 'text/css',
                    body: fs.readFileSync(path.join(webRoot, url.pathname), 'utf8')
                });
            }
            const token = route.request().headers()['x-csrf-token'];
            if (!instances.has(token)) instances.set(token, { saved: {}, drafts: {} });
            const { saved, drafts } = instances.get(token);
            const stepId = url.searchParams.get('step-id');
            if (url.pathname === '/rest/multistep-form/save-draft') {
                drafts[stepId] = route.request().postDataJSON();
                return route.fulfill({ json: { success: true } });
            }
            if (url.pathname === '/rest/multistep-form/get-step') {
                const isFirstStep = url.searchParams.get('step-id') === '1';
                const subscribed = (saved[1]?.subscribe || []).length > 0;
                const conditions = [{ fieldId: 'subscribe', operator: 'empty', value: '' }];
                return route.fulfill({ json: {
                    html: isFirstStep ? firstStep : (stepId === '3' ? thirdStep : secondStep),
                    domIdPrefix: 'f1-',
                    savedValues: saved[stepId] || {},
                    draftValues: drafts[stepId] || {},
                    visibilityConditions: stepId !== '2' ? {} : { details: { conditions, hidden: subscribed } },
                    requirementConditions: stepId !== '2' ? {} : { details: { conditions, required: !subscribed } }
                } });
            }
            if (url.pathname === '/rest/multistep-form/save-form') {
                const values = route.request().postDataJSON();
                submissions.push({ stepId, values });
                if (stepId === '1') {
                    saved[1] = { subscribe: asArray(values.subscribe), departments: asArray(values.departments) };
                    return route.fulfill({ json: { 'form-name': formName, 'step-id': 2 } });
                }
                if (terminalError) {
                    return route.fulfill({ status: 400, json: { end_try: true, err_msg: 'Verification attempts exhausted' } });
                }
                if (stepId === '2' && (saved[1]?.subscribe || []).length === 0 && !values.details) {
                    return route.fulfill({ json: { fieldErrors: { details: 'Details are required' } } });
                }
                if (stepId === '3' && !values.email?.includes('@')) {
                    return route.fulfill({ json: { fieldErrors: { email: 'Email is invalid' } } });
                }
                saved[stepId] = stepId === '2' ? { details: '', clear: '', options: [], radio: [], ...values } : values;
                if (drafts[stepId]) {
                    const confirmed = { ...saved[stepId] };
                    if (stepId === '2' && (saved[1]?.subscribe || []).length > 0) delete confirmed.details;
                    Object.assign(drafts[stepId], confirmed);
                }
                return route.fulfill({ json: { 'form-name': formName, 'step-id': threeSteps && stepId === '2' ? 3 : -1 } });
            }
            return route.abort();
        });
    });
    I.amOnPage(fixturePath);
    I.waitForVisible('#f1-subscribe');
    return submissions;
}

Scenario('Re-evaluate later conditions after clearing a previous checkbox', async ({ I }) => {
    await openForm(I);
    I.checkOption('#f1-subscribe');
    I.click('Next');
    I.waitForVisible('[data-multistep-back-step]');
    I.waitForInvisible('#f1-details');
    I.click('Back');
    I.waitForVisible('#f1-subscribe');
    I.seeCheckboxIsChecked('#f1-subscribe');
    I.uncheckOption('#f1-subscribe');
    I.click('Next');
    I.waitForVisible('#f1-details');
    I.seeAttributesOnElements('#f1-details', { 'data-requirement-required': 'true' });
    I.fillField('#f1-details', 'autotest details');
    I.click('Submit');
    I.waitForText('Saved autotest form');
});

Scenario('Restore multiple selections containing commas', async ({ I }) => {
    await openForm(I);
    I.checkOption('#f1-departments-0');
    I.checkOption('#f1-departments-1');
    I.click('Next');
    I.waitForVisible('[data-multistep-back-step]');
    I.click('Back');
    I.waitForVisible('#f1-departments-0');
    I.seeCheckboxIsChecked('#f1-departments-0');
    I.seeCheckboxIsChecked('#f1-departments-1');
});

Scenario('Skip draft saving for Next and final Submit', async ({ I }) => {
    await openForm(I);
    await I.usePlaywrightTo('submit normally without requesting draft storage', async ({ page }) => {
        let draftRequests = 0;
        await page.route('**/rest/multistep-form/save-draft?*', route => {
            draftRequests++;
            return route.abort();
        });
        await page.getByRole('button', { name: 'Next', exact: true }).click();
        await page.locator('#f1-details').waitFor({ state: 'visible' });
        await page.locator('#f1-details').fill('autotest normal submission');
        await page.getByRole('button', { name: 'Submit', exact: true }).click();
        await page.getByText('Saved autotest form').waitFor({ state: 'visible' });
        assert.equal(draftRequests, 0);
    });
});

Scenario('Remove step submission after a terminal verification error', async ({ I }) => {
    await openForm(I, true);
    I.checkOption('#f1-subscribe');
    I.click('Next');
    I.waitForVisible('[data-multistep-back-step]');
    I.click('Submit');
    I.waitForText('Verification attempts exhausted');
    I.dontSeeElement('form');
});

/** Hold fetch calls until explicitly resumed, so concurrent actions can be tested without timed delays. */
async function pauseRequests(page) {
    await page.evaluate(() => {
        const fetch = window.fetch.bind(window);
        window.autotestRequests = [];
        window.fetch = (url, options) => new Promise(resolve => {
            window.autotestRequests.push({ url, resume: () => resolve(fetch(url, options)) });
        });
    });
}

Scenario('Ignore repeated Back and submission while a previous step is loading', async ({ I }) => {
    await openForm(I);
    I.click('Next');
    I.waitForVisible('[data-multistep-back-step]');
    await I.usePlaywrightTo('hold the Back response while more navigation is attempted', async ({ page }) => {
        await page.locator('#f1-details').fill('autotest details');
        await pauseRequests(page);
        await page.locator('[data-multistep-back-step]').dblclick();
        await page.locator('#f1-details').press('Enter');
        await page.locator('form').dispatchEvent('submit');
        await page.locator('[data-multistep-back-step]').dispatchEvent('click');
        assert.equal(await page.evaluate(() => window.autotestRequests.length), 1);
        assert.equal(await page.locator('[data-multistep-back-step]').isDisabled(), true);
        assert.equal(await page.locator('button[type="submit"]').isDisabled(), true);
        assert.equal(await page.locator('#f1-details').isEnabled(), true);
        await page.evaluate(() => window.autotestRequests[0].resume());
        await page.waitForFunction(() => window.autotestRequests.length === 2);
        await page.evaluate(() => window.autotestRequests[1].resume());
        await page.locator('#f1-subscribe').waitFor({ state: 'visible' });
        await page.locator('#f1-subscribe').check();
        assert.equal(await page.locator('#f1-subscribe').isChecked(), true);
        assert.equal(await page.locator('button[type="submit"]').isEnabled(), true);
    });
});

Scenario('Keep navigation blocked through CAPTCHA, submission and the next step request', async ({ I }) => {
    await openForm(I);
    I.click('Next');
    I.waitForVisible('[data-multistep-back-step]');
    await I.usePlaywrightTo('delay each phase of submission', async ({ page }) => {
        await page.locator('#f1-details').fill('autotest details');
        await page.route('**/rest/multistep-form/save-form?*', route => route.fulfill({ json: { 'form-name': formName, 'step-id': 1 } }));
        await pauseRequests(page);
        await page.evaluate(() => {
            document.querySelector('form').insertAdjacentHTML('beforeend', '<input type="hidden" name="g-recaptcha-response" data-type="V3">');
            window.grecaptcha = {};
            window.wjFormSubmit = (form, resolve) => { window.autotestResolveCaptcha = resolve; };
        });
        await page.locator('button[type="submit"]').dblclick();
        await page.locator('form').dispatchEvent('submit');
        await page.locator('[data-multistep-back-step]').dispatchEvent('click');
        assert.equal(await page.locator('[data-multistep-back-step]').isDisabled(), true);
        assert.equal(await page.locator('button[type="submit"]').isDisabled(), true);
        assert.equal(await page.evaluate(() => window.autotestRequests.length), 0);

        await page.evaluate(() => window.autotestResolveCaptcha());
        await page.waitForFunction(() => window.autotestRequests.length === 1);
        await page.locator('form').dispatchEvent('submit');
        await page.locator('[data-multistep-back-step]').dispatchEvent('click');
        assert.equal(await page.evaluate(() => window.autotestRequests.length), 1);
        await page.evaluate(() => window.autotestRequests[0].resume());
        await page.waitForFunction(() => window.autotestRequests.length === 2);
        await page.locator('form').dispatchEvent('submit');
        await page.locator('[data-multistep-back-step]').dispatchEvent('click');
        assert.equal(await page.evaluate(() => window.autotestRequests.length), 2);
        await page.evaluate(() => window.autotestRequests[1].resume());
        await page.locator('#f1-subscribe').waitFor({ state: 'visible' });
        assert.equal(await page.locator('button[type="submit"]').isEnabled(), true);
    });
});

for (const [action, endpoint] of [['Back', 'get-step'], ['Submit', 'save-form'], ['Back', 'save-draft']]) {
    Scenario(`Allow navigation after a failed ${action} ${endpoint} request`, async ({ I }) => {
        await openForm(I);
        I.click('Next');
        I.waitForVisible('[data-multistep-back-step]');
        await I.usePlaywrightTo('fail one request and retry navigation', async ({ page }) => {
            await page.locator('#f1-details').fill('autotest details');
            await page.locator('form').evaluate(form => form.insertAdjacentHTML('beforeend', '<button type="submit" disabled>Disabled autotest action</button>'));
            await page.route(`**/rest/multistep-form/${endpoint}?*`, route => route.fulfill({ status: 500, json: { err_msg: 'autotest temporary error' } }), { times: 1 });
            await page.getByRole('button', { name: action, exact: true }).click();
            await page.getByText('autotest temporary error').waitFor({ state: 'visible' });
            assert.equal(await page.getByRole('button', { name: 'Back', exact: true }).isEnabled(), true);
            assert.equal(await page.getByRole('button', { name: 'Submit', exact: true }).isEnabled(), true);
            assert.equal(await page.getByRole('button', { name: 'Disabled autotest action' }).isDisabled(), true);
            await page.getByRole('button', { name: 'Back', exact: true }).click();
            await page.locator('#f1-subscribe').waitFor({ state: 'visible' });
        });
    });
}

Scenario('Allow another form to navigate while the first form is loading', async ({ I }) => {
    await openForm(I);
    await I.usePlaywrightTo('navigate two independent instances', async ({ page }) => {
        await pauseRequests(page);
        await page.getByRole('button', { name: 'Next' }).click();
        await page.evaluate(async ({ formName, modulePath }) => {
            document.body.insertAdjacentHTML('beforeend', '<div id="multistep-form-wrapper-autotest-second"></div>');
            const { MultistepForm } = await import(modulePath);
            new MultistepForm({ formName, stepId: '1', csrf: 'autotest-second', language: 'en' }).start();
        }, { formName, modulePath });
        await page.evaluate(() => window.autotestRequests[1].resume());
        const second = page.locator('#multistep-form-wrapper-autotest-second');
        await second.locator('#f1-subscribe').waitFor({ state: 'visible' });
        await second.getByRole('button', { name: 'Next' }).click();
        assert.equal(await page.evaluate(() => window.autotestRequests.length), 3);
        await page.evaluate(() => window.autotestRequests[2].resume());
        await page.waitForFunction(() => window.autotestRequests.length === 4);
        await page.evaluate(() => window.autotestRequests[3].resume());
        await second.locator('[data-multistep-back-step]').waitFor({ state: 'visible' });
        assert.equal(await page.locator('#multistep-form-wrapper-autotest button[type="submit"]').isDisabled(), true);
    });
});

for (const changeSecondStep of [false, true]) {
    Scenario(`Preserve three-step drafts with second-step changes: ${changeSecondStep}`, async ({ I }) => {
        const submissions = await openForm(I, false, true);
        I.click('Next');
        I.waitForVisible('#f1-details');
        // Back must work even when the required detail field is empty.
        I.click('Back');
        I.waitForVisible('#f1-subscribe');
        I.click('Next');
        I.waitForVisible('#f1-details');
        I.fillField('#f1-details', 'autotest remembered details');
        I.fillField('#f1-clear', '');
        I.checkOption('#f1-options-0');
        I.checkOption('#f1-options-1');
        I.checkOption('#f1-radio-1');
        I.click('Next');
        I.waitForVisible('#f1-email');
        I.fillField('#f1-email', 'autotest invalid email');
        I.fillField('#f1-comment', 'autotest unfinished third step');
        I.click('Back');
        I.waitForVisible('#f1-details');
        I.seeInField('#f1-details', 'autotest remembered details');
        I.seeInField('#f1-clear', '');
        I.seeCheckboxIsChecked('#f1-options-0');
        I.seeCheckboxIsChecked('#f1-options-1');
        I.seeCheckboxIsChecked('#f1-radio-1');
        I.click('Back');
        I.waitForVisible('#f1-subscribe');
        I.checkOption('#f1-subscribe');
        I.click('Next');
        I.waitForVisible('#f1-clear');
        I.waitForInvisible('#f1-details');
        if (changeSecondStep) {
            I.fillField('#f1-clear', 'autotest changed second step');
            I.uncheckOption('#f1-options-0');
            I.uncheckOption('#f1-options-1');
        }
        I.click('Next');
        I.waitForVisible('#f1-email');
        I.seeInField('#f1-email', 'autotest invalid email');
        I.seeInField('#f1-comment', 'autotest unfinished third step');
        I.click('Submit');
        await I.waitForText('Email is invalid');
        const hiddenSubmission = submissions.filter(item => item.stepId === '2').at(-1);
        assert.equal(Object.hasOwn(hiddenSubmission.values, 'details'), false);

        I.click('Back');
        I.waitForVisible('#f1-clear');
        I.seeInField('#f1-clear', changeSecondStep ? 'autotest changed second step' : '');
        if (changeSecondStep) {
            I.dontSeeCheckboxIsChecked('#f1-options-0');
            I.dontSeeCheckboxIsChecked('#f1-options-1');
        }
        I.click('Back');
        I.waitForVisible('#f1-subscribe');
        I.uncheckOption('#f1-subscribe');
        I.click('Next');
        I.waitForVisible('#f1-details');
        I.seeInField('#f1-details', 'autotest remembered details');
        I.click('Next');
        I.waitForVisible('#f1-email');
        I.seeInField('#f1-comment', 'autotest unfinished third step');
        I.fillField('#f1-email', 'autotest@example.com');
        I.click('Submit');
        await I.waitForText('Saved autotest form');
        assert.equal(submissions.filter(item => item.stepId === '2').at(-1).values.details, 'autotest remembered details');
    });
}

Scenario('Keep input and allow retry after a draft network failure', async ({ I }) => {
    await openForm(I);
    I.click('Next');
    I.waitForVisible('#f1-details');
    await I.usePlaywrightTo('fail draft transport without discarding the current form', async ({ page }) => {
        await page.locator('#f1-details').fill('autotest network retry');
        await page.route('**/rest/multistep-form/save-draft?*', route => route.abort(), { times: 1 });
        await page.getByRole('button', { name: 'Back', exact: true }).click();
        await page.locator('.alert-danger').waitFor({ state: 'visible' });
        assert.equal(await page.locator('#f1-details').inputValue(), 'autotest network retry');
        assert.equal(await page.getByRole('button', { name: 'Back', exact: true }).isEnabled(), true);
        await page.getByRole('button', { name: 'Back', exact: true }).click();
        await page.locator('#f1-subscribe').waitFor({ state: 'visible' });
        await page.getByRole('button', { name: 'Next', exact: true }).click();
        await page.locator('#f1-details').waitFor({ state: 'visible' });
        assert.equal(await page.locator('#f1-details').inputValue(), 'autotest network retry');
    });
});
