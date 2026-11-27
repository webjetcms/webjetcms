const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

/** Exercises folder selection and authentication ordering through the installed CodeceptJS runner. */
function runSuite(t, value, status = 200, pattern, command = 'run', storedValue = value) {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'autotest-s3-selection-'));
    t.after(() => fs.rmSync(directory, { recursive: true, force: true }));
    const trace = path.join(directory, 'trace.jsonl');
    const codecept = require.resolve('codeceptjs');
    const write = (name, content) => {
        const file = path.join(directory, name);
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, content);
    };
    write('helper.js', `
        const { Helper } = require(${JSON.stringify(codecept)});
        const fs = require('node:fs');
        const vm = require('node:vm');
        const record = value => fs.appendFileSync(${JSON.stringify(trace)}, JSON.stringify(value) + '\\n');
        module.exports = class extends Helper {
            _before() { this.ready = true; }
            login() {
                if (!this.ready) throw new Error('Browser has not been initialized');
                this.authenticated = true;
                record('login');
            }
            amOnPage() {
                if (!this.authenticated) throw new Error('Admin has not authenticated');
            }
            executeScript(fn) {
                return vm.runInNewContext('(' + fn.toString() + ')()', {
                    window: { csrfToken: 'autotest-csrf' },
                    fetch: async (url, options) => {
                        if (!url.endsWith('?name=s3Enabled') || options.headers['X-CSRF-Token'] !== 'autotest-csrf') {
                            throw new Error('Unexpected configuration request');
                        }
                        record('configuration');
                        return { ok: ${status === 200}, status: ${status}, json: async () => ({ name: 's3Enabled', value: ${JSON.stringify(storedValue)}, displayValue: ${JSON.stringify(value)} }) };
                    }
                }).catch(error => { throw new Error(error.message); });
            }
            record(value) { record(value); }
        };
    `);
    write('codecept.conf.js', `exports.config = {
        tests: './tests/**/*.js', output: './output',
        helpers: { Playwright: { require: './helper.js' } },
        plugins: {
            s3Tests: { enabled: true, require: ${JSON.stringify(path.join(__dirname, 's3_tests.js'))} },
            autoLogin: { enabled: true, inject: 'login', users: { admin: {
                login: I => I.login(), fetch: () => null
            } } }
        }
    };`);
    write('tests/admin/regular.js', `Feature('admin.regular'); Scenario('ordinary test', ({ I }) => I.record('regular'));`);
    for (const name of ['first', 'second']) {
        write('tests/s3/' + name + '.js', `
            Feature('s3.${name}').tag('@singlethread');
            Before(({ login }) => login('admin'));
            Scenario('setup', ({ I }) => I.record('${name}-setup'));
            Scenario('cleanup', ({ I }) => I.record('${name}-cleanup'));
        `);
    }
    const result = spawnSync(process.execPath, [require.resolve('codeceptjs/bin/codecept.js'), command, ...(pattern ? [pattern] : []), '-c', path.join(directory, 'codecept.conf.js')], {
        encoding: 'utf8', timeout: 30000
    });
    assert.ifError(result.error);
    const events = fs.existsSync(trace) ? fs.readFileSync(trace, 'utf8').trim().split('\n').filter(Boolean).map(line => JSON.parse(line)) : [];
    return { result, events, output: result.stdout + result.stderr };
}

test('Disabled S3 skips setup and cleanup in every S3 suite while ordinary tests run', t => {
    const { result, events, output } = runSuite(t, 'false');
    assert.equal(result.status, 0, output);
    assert.deepEqual(events, ['regular', 'login', 'configuration']);
    assert.match(output, /Skipping tests\/s3/);
});

test('Enabled S3 runs every S3 suite and reads Constants once after login', t => {
    const { result, events, output } = runSuite(t, 'true');
    assert.equal(result.status, 0, output);
    assert.equal(events.filter(event => event === 'configuration').length, 1);
    assert.ok(events.indexOf('login') < events.indexOf('configuration'));
    assert.deepEqual(events.filter(event => event !== 'login' && event !== 'configuration'), [
        'regular', 'first-setup', 'first-cleanup', 'second-setup', 'second-cleanup'
    ]);
});

test('Running an ordinary test file does not check S3 configuration', t => {
    const { result, events, output } = runSuite(t, 'false', 200, 'tests/admin/regular.js');
    assert.equal(result.status, 0, output);
    assert.deepEqual(events, ['regular']);
});

test('Running an S3 file directly still checks the server configuration', t => {
    const { result, events, output } = runSuite(t, 'false', 200, 'tests/s3/first.js');
    assert.equal(result.status, 0, output);
    assert.deepEqual(events, ['login', 'configuration']);
    assert.match(output, /Skipping tests\/s3/);
});

test('A dry run does not authenticate or read S3 configuration', t => {
    const { result, events, output } = runSuite(t, 'false', 200, undefined, 'dry-run');
    assert.equal(result.status, 0, output);
    assert.deepEqual(events, []);
});

for (const [current, stored] of [['true', 'false'], ['false', 'true']]) {
    test(`Current Constants s3Enabled=${current} takes precedence over stored ${stored}`, t => {
        const { result, events, output } = runSuite(t, current, 200, undefined, 'run', stored);
        assert.equal(result.status, 0, output);
        assert.equal(events.includes('first-setup'), current === 'true');
        assert.equal(events.includes('second-setup'), current === 'true');
    });
}

for (const [name, value, status] of [['HTTP error', 'true', 403], ['missing value', undefined, 200]]) {
    test(`An unreadable S3 configuration fails instead of executing S3 scenarios: ${name}`, t => {
        const { result, events, output } = runSuite(t, value, status);
        assert.equal(result.status, 1, output);
        assert.ok(!events.some(event => event.endsWith('-setup') || event.endsWith('-cleanup')));
        assert.match(output, /Cannot read/);
    });
}
