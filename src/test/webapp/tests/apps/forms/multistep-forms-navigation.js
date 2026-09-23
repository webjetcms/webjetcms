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
async function openForm(I, terminalError = false) {
    const saved = {};
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
        if (url.pathname === '/rest/multistep-form/get-step') {
            const isFirstStep = url.searchParams.get('step-id') === '1';
            const subscribed = (saved.subscribe || []).length > 0;
            const conditions = [{ fieldId: 'subscribe', operator: 'empty', value: '' }];
            return route.fulfill({ json: {
                html: isFirstStep ? firstStep : secondStep,
                domIdPrefix: 'f1-',
                savedValues: isFirstStep ? saved : {},
                visibilityConditions: isFirstStep ? {} : { details: { conditions, hidden: subscribed } },
                requirementConditions: isFirstStep ? {} : { details: { conditions, required: !subscribed } }
            } });
        }
        if (url.pathname === '/rest/multistep-form/save-form') {
            if (url.searchParams.get('step-id') === '1') {
                const values = route.request().postDataJSON();
                saved.subscribe = asArray(values.subscribe);
                saved.departments = asArray(values.departments);
                return route.fulfill({ json: { 'form-name': formName, 'step-id': 2 } });
            }
            if (terminalError) {
                return route.fulfill({ status: 400, json: { end_try: true, err_msg: 'Verification attempts exhausted' } });
            }
            const values = route.request().postDataJSON();
            if ((saved.subscribe || []).length === 0 && !values.details) {
                return route.fulfill({ json: { fieldErrors: { details: 'Details are required' } } });
            }
            return route.fulfill({ json: { 'form-name': formName, 'step-id': -1 } });
        }
        return route.abort();
    });
    await I.amOnPage(fixturePath);
    await I.waitForVisible('#f1-subscribe');
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

for (const action of ['Back', 'Submit']) {
    Scenario(`Allow navigation after a failed ${action} request`, async ({ I }) => {
        await openForm(I);
        await I.click('Next');
        await I.waitForVisible('[data-multistep-back-step]');
        await I.fillField('#f1-details', 'autotest details');
        await I.executeScript(() => document.querySelector('form').insertAdjacentHTML('beforeend', '<button type="submit" disabled>Disabled autotest action</button>'));
        const endpoint = action === 'Back' ? 'get-step' : 'save-form';
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
