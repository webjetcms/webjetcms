const path = require('node:path');
const { container, event, output, recorder, store } = require('codeceptjs');

/** Runs suites in tests/s3 only when the target WebJET has S3 enabled. */
module.exports = function () {
    let s3Enabled;
    const directory = path.join(global.codecept_dir, 'tests', 's3') + path.sep;

    // Register after helper hooks so the browser is ready before authentication.
    event.dispatcher.once(event.all.before, () => event.dispatcher.on(event.test.before, test => {
        if (store.dryRun || !test?.file?.startsWith(directory)) return;
        if (s3Enabled !== undefined) {
            if (!s3Enabled) test.parent.pending = true;
            return;
        }

        container.support('login')('admin');
        recorder.add('Check WebJET s3Enabled before running S3 tests', async () => {
            const browser = container.helpers('Playwright');
            await browser.amOnPage('/admin/v9/');
            s3Enabled = await browser.executeScript(async () => {
                const response = await fetch('/admin/rest/settings/configuration/autocomplete/detail?name=s3Enabled', {
                    headers: { 'X-CSRF-Token': window.csrfToken }
                });
                if (!response.ok) throw new Error('Cannot read s3Enabled: HTTP ' + response.status);
                const configuration = await response.json();
                // displayValue reflects Constants even when a temporary value differs from the database.
                const value = configuration.displayValue ?? configuration.value;
                if (configuration.name !== 's3Enabled' || !['true', 'false'].includes(value)) {
                    throw new Error('Cannot read the runtime s3Enabled configuration');
                }
                return value === 'true';
            });
            if (!s3Enabled) {
                test.parent.pending = true;
                output.print('Skipping tests/s3: WebJET s3Enabled is false.');
            }
        });
    }));
};
