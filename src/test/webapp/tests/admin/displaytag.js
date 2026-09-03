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
    I.click("Aplikácia", "#m thead th a");
    DT.checkTableRow("m", 1, ["Zrkadlenie", "webjet_cmp_mirroring"]);
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

Scenario('DisplayTag translation keys', ({ I, DT }) => {
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.see("Aplikácia", "table.sort_table thead tr th.sortable a");
    I.see("Kľúč", "table.sort_table thead tr th.sortable a");
    I.see("Popis", "table.sort_table thead tr th.sortable a");
    I.see("Nájdených", "div.displaytag-footer.displaytag-footer-bottom span.pagebanner");
    I.see("záznamov", "div.displaytag-footer.displaytag-footer-bottom span.pagebanner");

    I.relogin("admin", true, true, "cs");
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.see("Aplikace", "table.sort_table thead tr th.sortable a");
    I.see("Klíč", "table.sort_table thead tr th.sortable a");
    I.see("Popis", "table.sort_table thead tr th.sortable a");
    I.see("Nalezeno", "div.displaytag-footer.displaytag-footer-bottom span.pagebanner");
    I.see("záznamů", "div.displaytag-footer.displaytag-footer-bottom span.pagebanner");
});

Scenario('DisplayTag translation keys - logout', ({ I, DT }) => {
    I.logout();
});