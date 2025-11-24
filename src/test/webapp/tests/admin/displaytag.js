Feature('admin.displaytag');

let randomNumber;
Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('DisplayTag sorting', ({ I, DT }) => {
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.see("A/B testovanie", "table.sort_table");
    DT.checkTableRow("m", 1, ["A/B testovanie", "webjet_cmp_abtesting"]);
    I.click("Modul", "#m thead th a");
    DT.checkTableRow("m", 1, ["Zrkadlenie štruktúry", "webjet_cmp_structuremirroring"]);
});

function exportFile(name, type, I) {
    I.handleDownloads("modules-"+randomNumber+"."+type);
    I.click(name, ".exportlinks");
    I.waitForFile("../../../build/test/modules-"+randomNumber+"."+type, 30);
}

Scenario('DisplayTag export', ({ I, DT }) => {
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.see("A/B testovanie", "table.sort_table");

    exportFile("Excel", "xls", I);
    exportFile("CSV", "csv", I);
    exportFile("XML", "xml", I);
});