Feature('webpages.branch-p04');

if (process.env.WEBJET_P04_BROWSER === 'true') {
    const fs = require('fs');
    const read = path => Object.fromEntries(fs.readFileSync(path, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('=')).map(line => { const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)]; }));
    const p03 = read('/tmp/webjet-p03-manual.properties');
    const credentials = read('/tmp/webjet-p03-login.properties');
    let fixture = fs.existsSync('/tmp/webjet-p04-manual.properties') ? read('/tmp/webjet-p04-manual.properties') : {};
    const saveFixture = () => fs.writeFileSync('/tmp/webjet-p04-manual.properties', Object.entries(fixture).map(([key,value]) => `${key}=${value}`).join('\n')+'\n');

    function openComponent(I, DTE, index = 1) {
        DTE.waitForCkeditor();
        I.switchTo('.cke_wysiwyg_frame.cke_reset');
        I.switchTo(locate('iframe.wj_component').at(index));
        I.waitForElement('.inlineComponentEdit .inlineComponentButton.cke_button', 30);
        // CKEditor opens a parent dialog and replaces the originating frame; do not await child navigation.
        I.forceClick('.inlineComponentEdit .inlineComponentButton.cke_button');
        I.switchTo();
        I.waitForVisible('.cke_dialog_ui_iframe', 20);
        I.switchTo('.cke_dialog_ui_iframe');
        I.switchTo('#editorComponent');
        I.waitForVisible('#component-datatable_modal', 20);
    }


    Before(({ I }) => {
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForElement('#branch-selection', 30);
        I.waitForEnabled('#branch-selection', 20);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${p03.groupId}`);
        I.waitForEnabled('#branch-selection', 20);
    });

    Scenario('P04 real editor creates branch folder and available categorized news', async ({ I, DT, DTE }) => {
        if (!fixture.branchId) {
            I.selectOption('#branch-selection', '+ Nová pracovná verzia');
            I.waitForVisible('dialog[open]', 10);
            I.fillField('dialog input[name=name]', 'autotest P04 redakcia');
            I.fillField('dialog textarea[name=description]', 'P04 News and Menu editorial walkthrough');
            I.click('Vytvoriť', 'dialog');
            I.waitForText('autotest P04 redakcia', 20, 'dialog .branch-result');
            I.click('Upravovať web', 'dialog .branch-result');
            I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
            fixture.branchId = await I.executeScript(() => window.branchScreen.branchId);
            saveFixture();
        } else {
            I.selectOption('#branch-selection', String(fixture.branchId));
            I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        }
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${p03.groupId}`);
        DT.waitForLoader();
        if (!fixture.groupId) {
            I.click(DT.btn.tree_add_button);
            DTE.waitForEditor('groups-datatable');
            I.fillField('#DTE_Field_groupName', 'autotest P04 pracovný priečinok');
            I.fillField('#DTE_Field_navbarName', 'autotest P04 pracovný priečinok');
            I.fillField('#DTE_Field_urlDirName', 'autotest-p04');
            DTE.save('groups-datatable');
            I.waitForText('autotest P04 pracovný priečinok', 20);
            fixture.groupId = await I.executeScript(async () => {
                const changes = await fetch('/admin/rest/branch-content').then(r => r.json());
                return changes.find(item => item.type === 'GROUP' && item.title === 'autotest P04 pracovný priečinok').id;
            });
            saveFixture();
        }
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        DT.waitForLoader();
        if (!fixture.newsId) {
            I.click(DT.btn.add_button);
            I.waitForVisible("#datatableInit_modal.DTED.show[data-dte-focus-state=ready]", 30);
            DTE.waitForEditor();
            I.fillField('#DTE_Field_title', 'autotest P04 dostupná novinka');
            I.clickCss('#pills-dt-datatableInit-content-tab');
            await DTE.fillCkeditor('<p>autotest P04 obsah novej novinky</p>');
            I.clickCss('#pills-dt-datatableInit-perex-tab');
            I.fillField('#DTE_Field_htmlData', 'autotest P04 perex novinky');
            I.checkOption('.DTE_Field_Name_perexGroups input[value="1930083"]');
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.checkOption('#DTE_Field_available_0');
            DTE.save();
            I.waitForText('autotest P04 dostupná novinka', 20, '#datatableInit');
            fixture.newsId = await I.executeScript(() => webpagesDatatable.rows().data().toArray().find(row => row.title === 'autotest P04 dostupná novinka').docId);
            saveFixture();
        }
        I.saveScreenshot('branch-p04-created-news.png');
    });

    Scenario('P04 filtered News preview, nested administration and MenuApp selector', async ({ I, DT, DTE, Apps }) => {
        I.selectOption('#branch-selection', String(fixture.branchId));
        I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.homeId}`);
        DTE.waitForEditor();
        const menu = `!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=${p03.groupId}, openAllItems=true, classes=full)!`;
        const news = `!INCLUDE(/components/news/news-velocity.jsp, groupIds=${fixture.groupId}, template=p03-manual, publishType=all, perexGroup=1930083, perexNotRequired=false, order=priority, ascending=true, pageSize=10, paging=true, includeActualDoc=false)!`;
        I.clickCss('#pills-dt-datatableInit-content-tab');
        await DTE.fillCkeditor(`<h1>autotest P04 pracovný rozcestník</h1><p>${menu}</p><p>${news}</p>`);
        DTE.save();
        DTE.waitForModalClose();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 pracovný rozcestník', 20);
        I.see('autotest P04 dostupná novinka', 'ol.news');
        I.see('autotest P04 perex novinky', 'ol.news');
        const detail = await I.grabAttributeFrom('ol.news a', 'href');
        I.amOnPage(detail);
        I.waitForText('autotest P04 obsah novej novinky', 20);
        I.seeInCurrentUrl(`/admin/webpages/branch-preview/${fixture.branchId}/`);
        fixture.newsUrl = new URL(detail, 'http://iwcm.interway.sk:8086').pathname;
        saveFixture();
        I.amOnPage('/p03-manual/');
        I.waitForText('P03 LIVE home', 20);
        I.dontSee('autotest P04 pracovný rozcestník');
        I.dontSee('autotest P04 dostupná novinka');
        I.assertEqual(await I.executeScript(async path => (await fetch(path)).status, fixture.newsUrl.replace(`/admin/webpages/branch-preview/${fixture.branchId}`, '')), 404);

        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.homeId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        openComponent(I, DTE);
        I.seeInField('#DTE_Field_rootGroupId', String(p03.groupId));
        I.clickCss('.btn-webjet-jstree-item-edit');
        I.click(`[id="${p03.groupId}"] > .jstree-ocl`);
        I.waitForText('autotest P04 pracovný priečinok', 20, '.jstree-anchor');
        I.click(locate('.jstree-anchor').withText('autotest P04 pracovný priečinok'));
        I.seeInField('#DTE_Field_rootGroupId', String(fixture.groupId));
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');
        DTE.save();
        DTE.waitForModalClose();
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.homeId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        openComponent(I, DTE);
        I.seeInField('#DTE_Field_rootGroupId', String(fixture.groupId));
        I.switchTo();
        const menuContext = await I.executeScript(() => { const child = document.querySelector('.cke_dialog_ui_iframe').contentDocument.querySelector('#editorComponent').contentWindow; return {branch:child.branchScreen,value:child.document.querySelector('#DTE_Field_rootGroupId').value}; });
        I.assertEqual(menuContext.branch.branchId, Number(fixture.branchId));
        I.assertEqual(menuContext.branch.domainName, 'iwcm.interway.sk');
        I.saveScreenshot('branch-p04-menu-selector.png');
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_ok');
        DTE.cancel();

        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.homeId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        openComponent(I, DTE, 2);
        I.waitForVisible('#pills-dt-component-datatable-news-tab', 20);
        I.clickCss('#pills-dt-component-datatable-news-tab');
        I.switchTo('#newsListIframe');
        I.waitForText('autotest P04 dostupná novinka', 30, '#newsDataTable');
        I.click('autotest P04 dostupná novinka', '#newsDataTable');
        DTE.waitForEditor('newsDataTable');
        I.clickCss('#pills-dt-newsDataTable-perex-tab');
        I.seeCheckboxIsChecked('.DTE_Field_Name_perexGroups input[value="1930083"]');
        I.fillField('#DTE_Field_htmlData', 'autotest P04 perex novinky upravený cez NewsApp');
        DTE.save('newsDataTable', true);
        I.switchTo();
        const newsContext = await I.executeScript(() => { const child = document.querySelector('.cke_dialog_ui_iframe').contentDocument.querySelector('#editorComponent').contentDocument.querySelector('#newsListIframe').contentWindow; return {branch:child.branchScreen,url:child.location.href}; });
        I.assertEqual(newsContext.branch.branchId, Number(fixture.branchId));
        I.assertEqual(newsContext.branch.domainName, 'iwcm.interway.sk');
        I.assertContain(newsContext.url, `branchId=${fixture.branchId}`);
        I.assertContain(newsContext.url, 'branchDomain=iwcm.interway.sk');
        I.saveScreenshot('branch-p04-news-iframe.png');
        I.openNewTab();
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        I.waitForEnabled('#branch-selection', 20);
        I.selectOption('#branch-selection', p03.branchB);
        I.waitForText('P03 manual B', 20, '#branch-active-label');
        I.switchToPreviousTab();
        const staleIframe = await I.executeScript(async groupId => {
            const child = document.querySelector('.cke_dialog_ui_iframe').contentDocument.querySelector('#editorComponent').contentDocument.querySelector('#newsListIframe').contentWindow;
            return { rest: (await child.fetch(`/admin/rest/news/news-list/all?groupId=${groupId}`)).status, bootstrap: (await fetch(child.location.href)).status };
        }, fixture.groupId);
        I.assertEqual(staleIframe.rest, 409);
        I.assertEqual(staleIframe.bootstrap, 409);
        I.switchToNextTab();
        I.selectOption('#branch-selection', String(fixture.branchId));
        I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        I.switchToPreviousTab();
        I.closeOtherTabs();
        I.switchTo();
        I.clickCss('.cke_dialog_ui_button_cancel');
        DTE.cancel();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 dostupná novinka', 20, 'ol.news');
        I.see('autotest P04 perex novinky upravený cez NewsApp', 'ol.news');
        I.saveScreenshot('branch-p04-preview.png');
    });


    Scenario('P04 menu follows editor addition, move, rename and deletion', async ({ I, DT, DTE }) => {
        I.selectOption('#branch-selection', String(fixture.branchId));
        I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        DT.waitForLoader();
        if (!fixture.moveGroupId) {
            I.click(DT.btn.tree_add_button);
            DTE.waitForEditor('groups-datatable');
            I.fillField('#DTE_Field_groupName', 'autotest P04 cieľ presunu');
            I.fillField('#DTE_Field_navbarName', 'autotest P04 cieľ presunu');
            I.fillField('#DTE_Field_urlDirName', 'presuny');
            DTE.save('groups-datatable', true);
            fixture.moveGroupId = await I.executeScript(async () => (await fetch('/admin/rest/branch-content').then(r => r.json())).find(item => item.type === 'GROUP' && item.title === 'autotest P04 cieľ presunu').id);
            saveFixture();
        }
        if (!fixture.moveDocId) {
            I.click(DT.btn.add_button);
            DTE.waitForEditor();
            I.fillField('#DTE_Field_title', 'autotest P04 menu pridanie');
            I.clickCss('#pills-dt-datatableInit-content-tab');
            await DTE.fillCkeditor('<p>autotest P04 presúvaný obsah</p>');
            I.clickCss('#pills-dt-datatableInit-basic-tab');
            I.fillField('#DTE_Field_navbar', 'autotest P04 menu pridanie');
            I.checkOption('#DTE_Field_available_0');
            I.checkOption('#DTE_Field_urlInheritGroup_0');
            DTE.save(undefined, true);
            fixture.moveDocId = await I.executeScript(() => webpagesDatatable.rows().data().toArray().find(row => row.title === 'autotest P04 menu pridanie').docId);
            saveFixture();
        }
        // A failed browser run can leave the item moved. Reset only these identified WORK fixtures through UI.
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor('groups-datatable');
        I.fillField('#DTE_Field_groupName', 'autotest P04 cieľ presunu');
        I.fillField('#DTE_Field_navbarName', 'autotest P04 cieľ presunu');
        I.fillField('#DTE_Field_urlDirName', 'presuny');
        I.click('#editorAppDTE_Field_editorFields-parentGroupDetails .btn-webjet-jstree-item-edit');
        I.click(`webjet-dte-jstree .open-custom-modal [id="${p03.groupId}"] > .jstree-ocl`);
        I.click(`webjet-dte-jstree .open-custom-modal [id="${fixture.groupId}"] > .jstree-anchor`);
        DTE.save('groups-datatable', true);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.moveDocId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.click('#editorAppDTE_Field_editorFields-groupDetails .btn-webjet-jstree-item-edit');
        I.click(`webjet-dte-jstree .open-custom-modal [id="${p03.groupId}"] > .jstree-ocl`);
        I.click(`webjet-dte-jstree .open-custom-modal [id="${fixture.groupId}"] > .jstree-anchor`);
        DTE.save(undefined, true);
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 menu pridanie', 20, 'ul');
        I.dontSee('autotest P04 menu pridanie', 'ol.news');
        const oldLink = await I.grabAttributeFrom(locate('ul a').withText('autotest P04 menu pridanie'), 'href');
        I.saveScreenshot('branch-p04-menu-added.png');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.moveDocId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.click('#editorAppDTE_Field_editorFields-groupDetails .btn-webjet-jstree-item-edit');
        I.click(`webjet-dte-jstree .open-custom-modal [id="${p03.groupId}"] > .jstree-ocl`);
        I.click(`webjet-dte-jstree .open-custom-modal [id="${fixture.groupId}"] > .jstree-ocl`);
        I.waitForText('autotest P04 cieľ presunu', 20, 'webjet-dte-jstree .open-custom-modal');
        I.click(locate('webjet-dte-jstree .open-custom-modal .jstree-anchor').withText('autotest P04 cieľ presunu'));
        DTE.save(undefined, true);
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 cieľ presunu', 20, 'ul');
        const movedLink = await I.grabAttributeFrom(locate('ul a').withText('autotest P04 cieľ presunu'), 'href');
        I.assertContain(movedLink, '/presuny/');
        I.amOnPage(movedLink);
        I.waitForText('autotest P04 presúvaný obsah', 20);
        I.assertEqual(await I.executeScript(async url => (await fetch(url)).status, oldLink), 404);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor('groups-datatable');
        I.fillField('#DTE_Field_groupName', 'autotest P04 premenovaný priečinok');
        I.fillField('#DTE_Field_navbarName', 'autotest P04 premenovaný priečinok');
        I.fillField('#DTE_Field_urlDirName', 'premenovany');
        DTE.save('groups-datatable', true);
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 premenovaný priečinok', 20, 'ul');
        const renamedLink = await I.grabAttributeFrom(locate('ul a').withText('autotest P04 premenovaný priečinok'), 'href');
        I.assertContain(renamedLink, '/premenovany/');
        I.amOnPage(renamedLink);
        I.waitForText('autotest P04 presúvaný obsah', 20);
        I.saveScreenshot('branch-p04-menu-moved.png');
        // Move the whole folder out of and back into the MenuApp root through its ordinary parent selector.
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor('groups-datatable');
        I.click('#editorAppDTE_Field_editorFields-parentGroupDetails .btn-webjet-jstree-item-edit');
        I.click(`webjet-dte-jstree .open-custom-modal [id="${p03.groupId}"] > .jstree-anchor`);
        DTE.save('groups-datatable', true);
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 pracovný rozcestník', 20);
        I.dontSee('autotest P04 premenovaný priečinok');
        I.assertEqual(await I.executeScript(async url => (await fetch(url)).status, renamedLink), 404);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(DT.btn.tree_edit_button);
        DTE.waitForEditor('groups-datatable');
        I.click('#editorAppDTE_Field_editorFields-parentGroupDetails .btn-webjet-jstree-item-edit');
        I.click(`webjet-dte-jstree .open-custom-modal [id="${p03.groupId}"] > .jstree-ocl`);
        I.click(`webjet-dte-jstree .open-custom-modal [id="${fixture.groupId}"] > .jstree-anchor`);
        DTE.save('groups-datatable', true);
        I.amOnPage(renamedLink);
        I.waitForText('autotest P04 presúvaný obsah', 20);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(`#datatableInit tbody tr[id="${fixture.moveDocId}"] td.dt-select-td`);
        I.click(DT.btn.delete_button);
        I.waitForVisible('#datatableInit_modal.DTED.show', 10);
        DTE.save(undefined, true);
        await I.executeScript(() => true);
        delete fixture.moveDocId; saveFixture();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 pracovný rozcestník', 20);
        I.dontSee('autotest P04 menu pridanie', 'ul');
        I.assertEqual(await I.grabAttributeFrom(locate('ul a').withText('autotest P04 premenovaný priečinok'), 'href'), '#nopage');
        I.assertEqual(await I.executeScript(async url => (await fetch(url)).status, renamedLink), 404);
        I.saveScreenshot('branch-p04-menu-deleted.png');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.moveGroupId}`);
        DT.waitForLoader();
        I.click(DT.btn.tree_delete_button);
        I.waitForVisible('#groups-datatable_modal.DTED.show', 10);
        DTE.save('groups-datatable', true);
        await I.executeScript(() => true);
        delete fixture.moveGroupId; saveFixture();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchId}/p03-manual/`);
        I.waitForText('autotest P04 pracovný rozcestník', 20);
        I.dontSee('autotest P04 premenovaný priečinok');
    });

    Scenario('P04 revert working edit and reject an old form after branch switch', async ({ I, DT, DTE }) => {
        I.selectOption('#branch-selection', String(fixture.branchId));
        I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.detailId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.fillField('#DTE_Field_title', 'autotest P04 návrat zmeny');
        DTE.save(undefined, true);
        I.click('Pracovné zmeny');
        I.waitForText('autotest P04 návrat zmeny', 20, 'dialog');
        I.click('Vrátiť zmenu', locate('dialog p').withText('autotest P04 návrat zmeny'));
        I.waitForInvisible('dialog[open]', 20);
        I.waitForEnabled('#branch-selection', 20);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${p03.detailId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.seeInField('#DTE_Field_title', 'P03 LIVE detail');
        DTE.cancel();
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.newsId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-basic-tab');
        I.fillField('#DTE_Field_title', 'autotest P04 zachovaný starý formulár');
        I.executeScript(() => { window.p04LastStatus = null; $(document).ajaxError((event, xhr) => { window.p04LastStatus = xhr.status; }); });
        I.openNewTab();
        I.amOnPage('/admin/v9/webpages/web-pages-list/');
        I.waitForEnabled('#branch-selection', 20);
        I.selectOption('#branch-selection', '0');
        I.waitForText('Produkcia', 20, '#branch-active-label');
        I.switchToPreviousTab();
        I.switchTo();
        I.waitForVisible('#datatableInit_modal', 10);
        I.click('#datatableInit_modal button.btn-primary');
        DTE.waitForLoader();
        DT.waitForLoader();
        I.assertEqual(await I.executeScript(() => window.p04LastStatus), 409);
        I.seeInField('#DTE_Field_title', 'autotest P04 zachovaný starý formulár');
        I.saveScreenshot('branch-p04-stale-form.png');
    });

    Scenario('P04 server rejects unsupported writes and multiple component includes', async ({ I }) => {
        I.selectOption('#branch-selection', String(fixture.branchId));
        I.waitForText('autotest P04 redakcia', 20, '#branch-active-label');
        const statuses = await I.executeScript(async ({ groupId, docId }) => {
            const json = async (path, body) => {
                const response=await fetch(path, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body)});
                return {status:response.status,error:(await response.json()).error};
            };
            const preview = '/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/component_preview.jsp';
            const menu = `!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=${groupId}, openAllItems=true)!`;
            // Keep the preview-origin client stable; its normal auto-submit is exercised by the app UI scenario.
            const frame=document.createElement('iframe'); frame.hidden=true; frame.setAttribute('sandbox','allow-same-origin'); frame.src=preview;
            await new Promise(resolve=>{frame.onload=resolve;document.body.append(frame);});
            const render = async componentCode => {
                const response=await frame.contentWindow.fetch(preview, {method:'POST', referrerPolicy:'unsafe-url', headers:{'X-CSRF-Token':window.csrfToken,'X-WebJET-Branch':String(window.branchScreen.branchId),'X-WebJET-Branch-Domain':window.branchScreen.domainName}, body:new URLSearchParams({docid:docId,componentCode})});
                const text=await response.text();
                return {status:response.status,form:text.includes('document.componentForm'),blocked:text.includes('This component preview is unavailable')};
            };
            const roots=await fetch('/admin/rest/news/news-list/convertIdsToNamePair', {method:'POST',body:new URLSearchParams({ids:''})}).then(r=>r.json());
            const result={
                scan:roots.some(root=>root.value===String(groupId)),
                import:await json('/admin/rest/web-pages/editor',{action:'import',data:{}}),
                bulk:await json('/admin/rest/groups/editor',{action:'editall',data:{}}),
                tree:await fetch('/admin/rest/groups/tree/move',{method:'POST'}).then(r=>r.status),
                menu:await render(menu),
                multiple:await render(menu+'!INCLUDE(/autotest-p04-not-supported.jsp)!'),
                app:await render('!INCLUDE(/components/gallery/gallery.jsp)!')
            };
            frame.remove();
            return result;
        }, {groupId:fixture.groupId,docId:p03.homeId});
        I.assertEqual(statuses.menu.status, 200);
        I.assertFalse(statuses.menu.form, 'The supported preview must render, not return the bootstrap form');
        I.assertTrue(statuses.scan, 'News discovers the new WORK root from an INCLUDE stored only in WORK');
        for (const key of ['import','bulk']) {
            I.assertEqual(statuses[key].status, 200, key);
            I.assertContain(statuses[key].error, 'Imports and bulk operations are unavailable', key);
        }
        I.assertEqual(statuses.tree,409);
        for (const key of ['multiple','app']) I.assertEqual(statuses[key].status, 409, key);
    });

}
