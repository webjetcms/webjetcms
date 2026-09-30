const fs = require('node:fs');
const path = require('node:path');

// Isolate request interception from stale workers and dispose routes after every scenario.
Feature('apps.forms.multistep-forms-navigation', { timeout: 180 })
    .config('Playwright', { restart: 'context' });

AfterSuite(({ I }) => {
    I.limitTime(30).usePlaywrightTo('release the closed context before restoring session mode', async helper => {
        // CodeceptJS 3 retains the closed context, which session mode would otherwise reuse.
        helper.browserContext = null;
    });
});

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

    await I.mockRoute('**/*', async route => {
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
    await I.amOnPage(fixturePath);
    await I.waitForVisible('#f1-subscribe');
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
    let draftRequests = 0;
    await I.mockRoute('**/rest/multistep-form/save-draft?*', route => {
        draftRequests++;
        return route.abort();
    });
    await I.click('Next');
    await I.waitForVisible('#f1-details');
    await I.fillField('#f1-details', 'autotest normal submission');
    await I.click('Submit');
    await I.waitForText('Saved autotest form');
    await I.assertEqual(draftRequests, 0);
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
async function pauseRequests(I) {
    await I.executeScript(() => {
        const fetch = window.fetch.bind(window);
        window.autotestRequests = [];
        window.fetch = (url, options) => new Promise(resolve => {
            window.autotestRequests.push({ url, resume: () => resolve(fetch(url, options)) });
        });
    });
}

Scenario('Ignore repeated Back and submission while a previous step is loading', async ({ I }) => {
    await openForm(I);
    await I.click('Next');
    await I.waitForVisible('[data-multistep-back-step]');
    await I.fillField('#f1-details', 'autotest details');
    await pauseRequests(I);
    await I.doubleClick('[data-multistep-back-step]');
    await I.click('#f1-details');
    await I.pressKey('Enter');
    await I.executeScript(() => document.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true, composed: true })));
    await I.forceClick('[data-multistep-back-step]');
    await I.assertEqual(await I.executeScript(() => window.autotestRequests.length), 1);
    await I.seeElement('[data-multistep-back-step]:disabled');
    await I.seeElement('button[type="submit"]:disabled');
    await I.seeElement('#f1-details:enabled');
    await I.executeScript(() => window.autotestRequests[0].resume());
    await I.waitForFunction(() => window.autotestRequests.length === 2);
    await I.executeScript(() => window.autotestRequests[1].resume());
    await I.waitForVisible('#f1-subscribe');
    await I.checkOption('#f1-subscribe');
    await I.seeCheckboxIsChecked('#f1-subscribe');
    await I.seeElement('button[type="submit"]:enabled');
});

Scenario('Keep navigation blocked through CAPTCHA, submission and the next step request', async ({ I }) => {
    await openForm(I);
    await I.click('Next');
    await I.waitForVisible('[data-multistep-back-step]');
    await I.fillField('#f1-details', 'autotest details');
    await I.mockRoute('**/rest/multistep-form/save-form?*', route => route.fulfill({ json: { 'form-name': formName, 'step-id': 1 } }));
    await pauseRequests(I);
    await I.executeScript(() => {
        document.querySelector('form').insertAdjacentHTML('beforeend', '<input type="hidden" name="g-recaptcha-response" data-type="V3">');
        window.grecaptcha = {};
        window.wjFormSubmit = (form, resolve) => { window.autotestResolveCaptcha = resolve; };
    });
    await I.doubleClick('button[type="submit"]');
    await I.executeScript(() => document.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true, composed: true })));
    await I.forceClick('[data-multistep-back-step]');
    await I.seeElement('[data-multistep-back-step]:disabled');
    await I.seeElement('button[type="submit"]:disabled');
    await I.assertEqual(await I.executeScript(() => window.autotestRequests.length), 0);

    await I.executeScript(() => window.autotestResolveCaptcha());
    await I.waitForFunction(() => window.autotestRequests.length === 1);
    await I.executeScript(() => document.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true, composed: true })));
    await I.forceClick('[data-multistep-back-step]');
    await I.assertEqual(await I.executeScript(() => window.autotestRequests.length), 1);
    await I.executeScript(() => window.autotestRequests[0].resume());
    await I.waitForFunction(() => window.autotestRequests.length === 2);
    await I.executeScript(() => document.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true, composed: true })));
    await I.forceClick('[data-multistep-back-step]');
    await I.assertEqual(await I.executeScript(() => window.autotestRequests.length), 2);
    await I.executeScript(() => window.autotestRequests[1].resume());
    await I.waitForVisible('#f1-subscribe');
    await I.seeElement('button[type="submit"]:enabled');
});

for (const [action, endpoint] of [['Back', 'get-step'], ['Submit', 'save-form'], ['Back', 'save-draft']]) {
    Scenario(`Allow navigation after a failed ${action} ${endpoint} request`, async ({ I }) => {
        await openForm(I);
        await I.click('Next');
        await I.waitForVisible('[data-multistep-back-step]');
        await I.fillField('#f1-details', 'autotest details');
        await I.executeScript(() => document.querySelector('form').insertAdjacentHTML('beforeend', '<button type="submit" disabled>Disabled autotest action</button>'));
        let requestFailed = false;
        await I.mockRoute(`**/rest/multistep-form/${endpoint}?*`, route => {
            if (requestFailed) return route.fallback();
            requestFailed = true;
            return route.fulfill({ status: 500, json: { err_msg: 'autotest temporary error' } });
        });
        await I.click(action);
        await I.waitForText('autotest temporary error');
        await I.seeElement('[data-multistep-back-step]:enabled');
        await I.seeElement(locate('button:not([disabled])').withText('Submit'));
        await I.seeElement(locate('button[disabled]').withText('Disabled autotest action'));
        await I.click('Back');
        await I.waitForVisible('#f1-subscribe');
    });
}

Scenario('Allow another form to navigate while the first form is loading', async ({ I }) => {
    await openForm(I);
    await pauseRequests(I);
    await I.click('Next');
    await I.executeScript(async ({ formName, modulePath }) => {
        document.body.insertAdjacentHTML('beforeend', '<div id="multistep-form-wrapper-autotest-second"></div>');
        const { MultistepForm } = await import(modulePath);
        new MultistepForm({ formName, stepId: '1', csrf: 'autotest-second', language: 'en' }).start();
    }, { formName, modulePath });
    await I.executeScript(() => window.autotestRequests[1].resume());
    const second = '#multistep-form-wrapper-autotest-second';
    await I.waitForVisible(`${second} #f1-subscribe`);
    await I.click('Next', second);
    await I.assertEqual(await I.executeScript(() => window.autotestRequests.length), 3);
    await I.executeScript(() => window.autotestRequests[2].resume());
    await I.waitForFunction(() => window.autotestRequests.length === 4);
    await I.executeScript(() => window.autotestRequests[3].resume());
    await I.waitForVisible(`${second} [data-multistep-back-step]`);
    await I.seeElement('#multistep-form-wrapper-autotest button[type="submit"]:disabled');
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
        I.assertFalse(Object.hasOwn(hiddenSubmission.values, 'details'));

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
        I.assertEqual(submissions.filter(item => item.stepId === '2').at(-1).values.details, 'autotest remembered details');
    });
}

Scenario('Keep input and allow retry after a draft network failure', async ({ I }) => {
    await openForm(I);
    await I.click('Next');
    await I.waitForVisible('#f1-details');
    await I.fillField('#f1-details', 'autotest network retry');
    let requestFailed = false;
    await I.mockRoute('**/rest/multistep-form/save-draft?*', route => {
        if (requestFailed) return route.fallback();
        requestFailed = true;
        return route.abort();
    });
    await I.click('Back');
    await I.waitForVisible('.alert-danger');
    await I.seeInField('#f1-details', 'autotest network retry');
    await I.seeElement('[data-multistep-back-step]:enabled');
    await I.click('Back');
    await I.waitForVisible('#f1-subscribe');
    await I.click('Next');
    await I.waitForVisible('#f1-details');
    await I.seeInField('#f1-details', 'autotest network retry');
});
