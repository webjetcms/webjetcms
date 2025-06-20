Feature("admin.xlsimport");

Before(({ I, login }) => {
    login('admin');
});

Scenario("/admin/spec/import_xls.jsp", ({ I }) => {
    I.say("Testing old version of XLS import");

    I.amOnPage("/admin/spec/import_xls.jsp");
    I.selectOption("select[name='type']", "AtrExcelImport");
    I.attachFile("input[name='file']", "tests/admin/doc-atr-import.xls");
    I.click("form[name='xlsImportForm'] input[type='submit']");

    I.waitForText("Import dokončený", 10);
    I.see("Importujem: autotest-import AutoTest-imp1");
    I.see("Deleting sheet: autotest-import");
    I.see("Importujem: autotest-import AutoTest-imp2");
});
