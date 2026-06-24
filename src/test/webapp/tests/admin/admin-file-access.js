Feature('admin.admin-file-access');

Scenario("Admin File Access", async ({ I, DT }) => {
    I.relogin("tadminfileaccess");
    I.amOnPage("/admin/update/test_adminfilesaccess.jsp?act=fix&userid=true&search=all");
    I.waitForText("Found 6 admin files", 20, "h3");
});

Scenario("logout", async ({ I, DT }) => {
    I.logout();
});
