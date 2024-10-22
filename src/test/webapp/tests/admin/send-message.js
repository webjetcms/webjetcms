Feature('admin.send-message');

Before(({ login }) => {
    login('admin');
});

Scenario('send message tests', ({ I }) => {
    I.amOnPage("/components/messages/send_message.jsp");
    I.see('Poslať správu');
    I.see('Zadajte text a príjemcu.');
    I.selectOption('select[name=group]', 'Administrátori');
    I.selectOption('select[name=komu]', 'Tester Playwright');
    I.fillField('textarea[name=text]', 'Ahoj, toto je testovacia správa');
    I.seeInField('textarea[name="text"]', 'Ahoj, toto je testovacia správa');
    I.clickCss('#btnOk');
    I.dontSeeInField('textarea[name="text"]', 'Ahoj, toto je testovacia správa');
    //wait until message popup is open and then close it
    I.wait(1);
    I.amOnPage("/admin/refresher.jsp");
    I.wait(2);
    I.closeOtherTabs();
    I.wait(1);
});