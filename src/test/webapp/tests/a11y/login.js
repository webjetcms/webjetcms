Feature('a11y.login');

Scenario('logon form', async ({ I, a11y }) => {
    I.logout();
    I.amOnPage('/admin/');
    //show tooltip
    I.fillField("#password", "aaa");
    I.wait(1);
    await a11y.check();

    //click submit button to show error messages
    I.click("#login-submit");
    await a11y.check();
});


Scenario('send password', async ({ I, a11y }) => {
    I.amOnPage('/admin/');
    I.click("button.lost-password");
    //I.waitForElement
    await a11y.check();

    I.click("#register-submit-btn");
    await a11y.check();
});