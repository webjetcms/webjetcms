Feature('settings.missing-keys');

// Prop excludes lowercase "autotest" keys from missing-key reporting.
const missingKeys = ['first', 'second'].map(name => `components.missing_keys.AUTOTEST.${name}-${Date.now()}`);

Before(async ({ I, login }) =>{
    login('admin');
    I.amOnPage('/admin/v9/');
    // Exercise the real translation lookup rather than relying on accidentally untranslated UI labels.
    const translations = await I.executeScript(async keys => {
        const results = [];
        for (const key of keys) {
            const response = await fetch('/admin/rest/ai/assistant-definition/translate-key?key=' + encodeURIComponent(key), {
                headers: { 'X-CSRF-Token': window.csrfToken }
            });
            results.push({ status: response.status, value: await response.text() });
        }
        return results;
    }, missingKeys);
    translations.forEach((translation, index) => {
        I.assertEqual(translation.status, 200);
        I.assertEqual(translation.value, missingKeys[index]);
    });
    I.amOnPage("/admin/v9/settings/missing-keys/");
});

Scenario('zakladne testy', async ({ I, DT }) =>{
    for (const key of missingKeys) {
        DT.filterEquals('key', key);
        I.see(key, '#datatableInit tbody');
    }
});
