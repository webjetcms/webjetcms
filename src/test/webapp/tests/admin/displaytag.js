Feature('admin.displaytag');

let randomNumber;
Before(({ I, login }) => {
     login('admin');
     if (typeof randomNumber == "undefined") {
        randomNumber = I.getRandomText();
    }
});

Scenario('DisplayTag sorting', async ({ I }) => {
    I.amOnPage("/admin/modules_allinfo.jsp");
    const titleCells = "#m tbody tr td:first-child";
    const titles = await I.grabTextFromAll(titleCells);
    I.assertAbove(new Set(titles).size, 1, "Sorting requires at least two different application names.");
    I.assertBelow(titles[0].localeCompare(titles[titles.length - 1], "sk"), 0, "Application names must initially be ascending.");
    I.click("Aplikácia", "#m thead th a");
    I.assertDeepEqual(await I.grabTextFromAll(titleCells), titles.reverse(), "Clicking the header must reverse the application order.");
});

function exportFile(name, type, I) {
    I.handleDownloads("modules-"+randomNumber+"."+type);
    I.click(name, ".exportlinks");
    I.waitForFile("../../../build/test/modules-"+randomNumber+"."+type, 30);
}

Scenario('DisplayTag export', ({ I }) => {
    I.amOnPage("/admin/modules_allinfo.jsp");
    I.see("webjet_cmp_abtesting", "#m tbody");

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
