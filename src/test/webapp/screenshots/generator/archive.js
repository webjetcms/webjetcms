Feature('admin.archive');

Before(({ login }) => {
    login('admin');
});

Scenario('archive screenshots', ({ I, Document }) => {
    I.amOnPage("/admin/archive.jsp");
    I.wait(2);
    Document.screenshot("/sysadmin/files/backup/backup.png", 704, 450);
});

