const { restoreDefaultDashboard } = require('../../../helpers/dashboard-browser');

Feature('admin.dashboard.zzz-default').tag('@singlethread');

Before(({ login }) => {
    login('admin');
});

Scenario('Restore the default overview after dashboard tests', async ({ I }) => {
    await restoreDefaultDashboard(I);
});
