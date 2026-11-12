Feature('webpages.branch-management');

if (process.env.WEBJET_P05_BROWSER === 'true') {
    const fs = require('fs');
    const branchPicker = require('../../helpers/branch-picker');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('#')).map(line => {
        const i = line.indexOf('=');
        return [line.slice(0, i), line.slice(i + 1)];
    }));
    const credentials = read('/tmp/webjet-p05-login.properties');
    const fixture = read('/tmp/webjet-p05-manual.properties');
    const name = 'autotest UI – jesenná kampaň ' + Date.now();
    const modal = '#branchesDataTable_modal';

    Before(({ I }) => {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForEnabled('#branch-switcher', 30);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForEnabled('#branch-switcher', 30);
    });

    Scenario('Working version management creates, edits, compares and opens a draft', async ({ I, DT, DTE }) => {
        branchPicker.open(I);
        I.dontSeeElement('#branch-picker [data-branch-action="create"]');
        I.click('#branch-picker [data-branch-action="manage"]');
        DT.waitForLoader();
        I.seeInCurrentUrl('/admin/v9/webpages/branches/');
        I.seeElement('a[href="/admin/v9/webpages/branches/"]');
        I.seeElement('.buttons-branch-open:disabled');
        I.seeElement('.buttons-branch-compare:disabled');
        I.seeElement('.buttons-remove:disabled');
        I.dontSeeElement('#branchesDataTable_wrapper .buttons-export');
        I.dontSeeElement('#branchesDataTable_wrapper .buttons-import');
        I.seeAttributesOnElements('.buttons-branch-compare', {'aria-label':'Skontrolovať zmeny', 'data-toggle':'tooltip'});
        I.seeAttributesOnElements('.buttons-branch-open', {'aria-label':'Upravovať web', 'data-toggle':'tooltip'});
        I.clickCss('#branchesDataTable_wrapper .buttons-create');
        DTE.waitForEditor('branchesDataTable');
        DTE.save('branchesDataTable');
        I.seeElement(modal+' .DTE_Field_Name_name .text-danger');
        I.fillField(modal+' #DTE_Field_name', name);
        I.fillField(modal+' #DTE_Field_description', 'Campaign management regression');
        I.checkOption(modal+' input[name="color"][value="lavender"]');
        I.seeAttributesOnElements(modal+' .md-branch-sample', {'data-branch-color':'lavender'});
        DTE.save('branchesDataTable', true);
        DT.filterContains('name', name);
        I.click(name, '#branchesDataTable');
        DTE.waitForEditor('branchesDataTable');
        I.seeInField(modal+' #DTE_Field_description', 'Campaign management regression');
        I.seeCheckboxIsChecked(modal+' input[name="color"][value="lavender"]');
        I.fillField(modal+' #DTE_Field_name', name+' upravená');
        I.checkOption(modal+' input[name="color"][value="mint"]');
        DTE.save('branchesDataTable', true);
        I.refreshPage();
        DT.waitForLoader();
        DT.filterContains('name', name+' upravená');
        I.see('Zelená', '#branchesDataTable');
        I.clickCss('#branchesDataTable tbody tr td.dt-select-td');
        I.waitForEnabled('.buttons-branch-open', 10);
        I.clickCss('.buttons-branch-compare');
        I.waitForVisible('#branch-merge-dialog', 20);
        I.see(name+' upravená', '#branch-merge-title');
        I.pressKey('Escape');
        I.waitForInvisible('#branch-merge-dialog', 10);
        I.clickCss('.buttons-branch-open');
        I.waitForText(name+' upravená', 20, '#branch-active-label');
        I.seeInCurrentUrl('/admin/v9/webpages/web-pages-list/');
        I.seeAttributesOnElements('#branch-band', {'data-branch-color':'mint'});
        I.amOnPage('/admin/v9/webpages/branches/');
        DT.waitForLoader();
        DT.filterContains('name', name+' upravená');
        DT.deleteAll('branchesDataTable');
        I.waitForElement('#branch-switcher[aria-label*="Hlavná verzia"]', 20);
        DT.waitForLoader();
        DT.filterContains('name', name+' upravená');
        I.dontSee(name+' upravená', '#branchesDataTable');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.amOnPage('/admin/v9/webpages/branches/');
        DT.waitForLoader();
        // A published version can be reviewed but cannot be opened for content editing.
        DT.filterSelect('state', 'Publikovaná');
        I.waitForElement('#branchesDataTable tbody td.dt-select-td', 10);
        I.clickCss('#branchesDataTable tbody tr:first-child td.dt-select-td');
        I.seeElement('.buttons-branch-open:disabled');
        I.seeElement('.buttons-branch-compare:not(:disabled)');
        I.seeElement('.buttons-remove:not(:disabled)');
        DT.checkPerms('menuBranches', '/admin/v9/webpages/branches/');
    });
}
