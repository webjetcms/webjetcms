Feature('apps.send-message');

Before(({ login }) => {
    login('admin');
});

Scenario('send message screenshots', ({ I, Document }) => {
    I.amOnPage("/components/messages/send_message.jsp");
    I.selectOption('select[name=komu]', 'User Slabeheslo');
    I.fillField('textarea[name=text]', 'Ahoj, toto je testovacia správa');
    Document.screenshot("/redactor/admin/send-message.png", 700, 550);
});