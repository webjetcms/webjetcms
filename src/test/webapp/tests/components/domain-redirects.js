Feature('components.domain_redirects');

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/settings/domain-redirect");
});

Scenario('domain_redirects-zakladne testy', async ({ I, DataTables }) => {
    I.see("Aktívne");
    await DataTables.baseTest({
        dataTable: 'domainRedirectTable',
        requiredFields: ['redirectFrom', 'redirectTo'],
        perms: 'cmp_redirects'
    });
});

//over, ze zmeny su zapisane aj v audite