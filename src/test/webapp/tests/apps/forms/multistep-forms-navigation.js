const fs = require('node:fs');
const path = require('node:path');

Feature('apps.forms.multistep-forms-navigation');

const formName = 'autotest-navigation';
const fixturePath = '/apps/multistep-formular/autotest-navigation.html';
const modulePath = '/apps/form/mvc/multistep-form.js';
const cssPath = '/apps/form/mvc/default.css';
const webRoot = path.resolve(__dirname, '../../../../../main/webapp');

/**
 * Runs the production browser module against controlled step responses without saving test submissions.
 */
async function openForm(I, terminalError = false) {
    await I.usePlaywrightTo('prepare multistep navigation responses', async ({ page }) => {
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
