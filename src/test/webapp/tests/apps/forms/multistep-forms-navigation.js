const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

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
    await I.limitTime(30).usePlaywrightTo('prepare multistep navigation responses', async ({ page }) => {
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
    });
    I.amOnPage(fixturePath);
    I.waitForVisible('#f1-subscribe');
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
    await I.limitTime(30).usePlaywrightTo('hold the Back response while more navigation is attempted', async ({ page }) => {
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
    await I.limitTime(30).usePlaywrightTo('delay each phase of submission', async ({ page }) => {
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

for (const action of ['Back', 'Submit']) {
    Scenario(`Allow navigation after a failed ${action} request`, async ({ I }) => {
        await openForm(I);
        I.click('Next');
        I.waitForVisible('[data-multistep-back-step]');
        await I.limitTime(30).usePlaywrightTo('fail one request and retry navigation', async ({ page }) => {
            await page.locator('#f1-details').fill('autotest details');
            await page.locator('form').evaluate(form => form.insertAdjacentHTML('beforeend', '<button type="submit" disabled>Disabled autotest action</button>'));
            const endpoint = action === 'Back' ? 'get-step' : 'save-form';
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
    await I.limitTime(30).usePlaywrightTo('navigate two independent instances', async ({ page }) => {
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
