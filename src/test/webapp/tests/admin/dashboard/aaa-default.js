const { restoreDefaultDashboard } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.aaa-default').tag('@singlethread');

Before(({ login }) => {
    login('admin');
});

Scenario('Restore the default overview before dashboard tests', async ({ I }) => {
    await restoreDefaultDashboard(I);
});
