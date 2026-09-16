const assert = require('node:assert/strict');
const {execFile} = require('node:child_process');
const {readdirSync} = require('node:fs');
const path = require('node:path');
const {stripVTControlCharacters} = require('node:util');
const {container} = require('codeceptjs');

Feature('helpers.node-tests');

const webappDirectory = path.resolve(__dirname, '../..');
const testFiles = readdirSync(path.join(webappDirectory, 'helpers')).filter(file => file.endsWith('.test.js')).sort();

// Give each helper file its own Allure result and run it in an isolated Node.js process.
for (const file of testFiles) {
    Scenario(file, {timeout: 150}, async () => {
        const result = await new Promise(resolve => {
            execFile(process.execPath, ['--test', '--test-reporter=spec', path.join('helpers', file)], {
                cwd: webappDirectory,
                encoding: 'utf8',
                timeout: 120000,
                maxBuffer: 10 * 1024 * 1024
            }, (error, stdout, stderr) => resolve({error, stdout, stderr}));
        });

        const output = stripVTControlCharacters(result.stdout + result.stderr + (result.error ? '\n' + result.error.message : ''));
        console.log(output);
        const allure = container.plugins('allure');
        if (allure) allure.addAttachment(file + ' output', output, 'text/plain');

        assert.ifError(result.error);
    });
}
