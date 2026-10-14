Feature('webpages.pagebuilder-clipboard');

const clipboardKey = 'webjet.pagebuilder.clipboard';
const frameSelector = '#DTE_Field_data-pageBuilderIframe';
const toolbar = '.pb-workbench';
let savedTitle;

const source = `<section class="clipboard-section-autotest" id="clipboard-section-autotest" data-pb-user-style-id="clipboard-section-style-autotest">
    <div class="container clipboard-container-autotest"><div class="row">
        <div class="col-12 col-md-6 clipboard-column-autotest" data-pb-user-style-id="clipboard-column-style-autotest"><div class="column-content">
            <p id="clipboard-text-autotest">Clipboard original autotest</p><a href="#clipboard-text-autotest">Clipboard link autotest</a>
        </div></div>
    </div></div>
</section>
<style style-id="clipboard-section-style-autotest">[data-pb-user-style-id="clipboard-section-style-autotest"] { background-color: rgb(220, 230, 240); }</style>
<style style-id="clipboard-column-style-autotest">[data-pb-user-style-id="clipboard-column-style-autotest"] .column-content { color: rgb(20, 40, 60); } @media (min-width: 768px) { [data-pb-user-style-id="clipboard-column-style-autotest"] .column-content { padding: 13px; } }</style>`;
const target = '<section class="clipboard-target-autotest"><div class="container"><div class="row"><div class="col-12"><p>Clipboard target autotest</p></div></div></div></section>';

Before(({login}) => login('admin'));

/** Waits in the top document because CodeceptJS cannot poll a FrameLocator. */
function waitForEditors(I) {
    I.switchTo();
    I.waitForFunction(() => {
        const frame = document.querySelector('#DTE_Field_data-pageBuilderIframe')?.contentWindow;
        return frame?.pageBuilder?.ui && Array.from(frame.document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]')).every(element => {
            const editor = frame.CKEDITOR.instances[element.dataset.ckeditorInstance];
            return editor?.status === 'ready' && editor.element.$ === element;
        });
    }, 20);
    I.switchTo(frameSelector);
    I.waitForVisible(toolbar, 20);
}

/** Loads fixture HTML through the editor's supported mode switch. */
function loadSource(I, html) {
    I.selectOption('.exit-inline-editor select', 'html');
    I.switchTo();
    I.waitForVisible('.CodeMirror', 20);
    I.executeScript(html => {
        document.querySelector('.CodeMirror').CodeMirror.setValue(html);
        window.switchEditorType({value: 'pageBuilder'});
    }, html);
    waitForEditors(I);
}

/** Opens a separate unsaved page so tests never overwrite the product demo. */
async function openPage(I, DT, DTE, Document, html) {
    const title = 'pb-clipboard-autotest-' + I.getRandomText();
    Document.resetPageBuilderMode();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=34495');
    DT.waitForLoader();
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    DTE.fillField('title', title);
    DTE.fillField('navbar', title);
    I.clickCss('#pills-dt-datatableInit-content-tab');
    waitForEditors(I);
    loadSource(I, html);
    return title;
}

function copyBlock(I, type) {
    I.click('.clipboard-column-autotest p');
    I.click(toolbar+'-path button[data-type='+type+']');
    I.click(toolbar+' [data-pb-action=more]');
    I.click(toolbar+' [data-pb-action=copy]');
}

function startPaste(I) {
    I.click(toolbar+' [data-pb-action=more]');
    I.click(toolbar+' [data-pb-action=paste]');
    I.waitForVisible('.pb-insert-layer', 10);
}

/** Removes the fixture's only section through the UI, leaving a genuinely empty canvas. */
function removeOnlySection(I) {
    I.click(toolbar+'-path button[data-type=section]');
    I.click(toolbar+' [data-pb-action=more]');
    I.amAcceptingPopups();
    I.click(toolbar+' [data-pb-action=remove]');
    I.acceptPopup();
    I.waitForInvisible('#wjInline-docdata > section', 10);
}

Scenario('clipboard copies current content across pages and saves independent repeated copies', async ({I, DT, DTE, Document}) => {
    await openPage(I, DT, DTE, Document, source);
    I.executeScript((root, key) => localStorage.removeItem(key), clipboardKey);
    I.click('.clipboard-column-autotest p');
    I.click(toolbar+' [data-pb-action=more]');
    I.seeElement(toolbar+' [data-pb-action=paste]:disabled');
    I.pressKey('Escape');
    I.click('.clipboard-column-autotest p');
    I.pressKey(['CommandOrControl', 'End']);
    I.type(' changed autotest');
    copyBlock(I, 'section');
    I.switchTo();
    const copiedToast = locate('#toast-container-webjet .toast').withText('Blok bol skopírovaný.');
    I.waitForVisible(copiedToast, 5);
    I.waitForInvisible(copiedToast, 8);
    I.switchTo(frameSelector);
    const copied = await I.executeScript((root, key) => JSON.parse(localStorage.getItem(key)), clipboardKey);
    const remaining = await I.executeScript((root, expiresAt) => expiresAt - Date.now(), copied.expiresAt);
    I.assertTrue(remaining > 29 * 60 * 1000 && remaining <= 30 * 60 * 1000, 'A new copy must expire after 30 minutes');
    I.assertEqual(copied.type, 'section', 'The snapshot must remember the source type');
    I.assertContain(copied.html, 'changed autotest', 'Copy must read the current unsaved editor content');
    I.assertEqual(copied.styles.length, 2, 'The snapshot must include root and nested styles');
    I.assertFalse(/pb-grid-element|data-ckeditor-instance|pb-toolbar|contenteditable/.test(copied.html), 'The snapshot must not contain editor decorations');
    I.switchTo();
    DTE.cancel();

    const title = await openPage(I, DT, DTE, Document, target);
    I.click('.clipboard-target-autotest p');
    startPaste(I);
    I.see('Vyberte miesto, kam chcete vložiť skopírovanú sekciu.', '.pb-insert-hint');
    I.assertDeepEqual(await I.executeScript(() => [...new Set(window.pageBuilder.ui.insertPoints.map(point => point.type))]), ['section'], 'A selected column must not change the pasted type');
    I.pressKey('Escape');
    I.waitForInvisible('.pb-insert-layer', 10);
    I.dontSeeElement('.clipboard-section-autotest');
    startPaste(I);
    I.click(locate('.pb-insert-point[data-type=section] button').last());
    waitForEditors(I);
    I.waitForElement('.clipboard-target-autotest + .clipboard-section-autotest', 10);
    startPaste(I);
    I.click(locate('.pb-insert-point[data-type=section] button').last());
    waitForEditors(I);
    const pasted = await I.executeScript(() => {
        const sections = [...document.querySelectorAll('.clipboard-section-autotest')];
        return {
            count: sections.length,
            selected: window.pageBuilder.ui.selected === sections[1],
            rootStyles: sections.map(node => node.getAttribute('data-pb-user-style-id')),
            nestedStyles: sections.map(node => node.querySelector('.clipboard-column-autotest').getAttribute('data-pb-user-style-id')),
            colors: sections.map(node => getComputedStyle(node).backgroundColor),
            paddings: sections.map(node => getComputedStyle(node.querySelector('.column-content')).paddingTop),
            ids: sections.map(node => node.querySelector('p').id),
            links: sections.map(node => node.querySelector('a').getAttribute('href'))
        };
    });
    I.assertEqual(pasted.count, 2, 'Paste must retain the clipboard for repeated use');
    I.assertTrue(pasted.selected, 'The inserted block must be selected');
    I.assertEqual(new Set(pasted.rootStyles.concat(pasted.nestedStyles)).size, 4, 'Every pasted subtree must own independent styles');
    I.assertDeepEqual(pasted.colors, ['rgb(220, 230, 240)', 'rgb(220, 230, 240)'], 'Root styles must survive cross-page insertion');
    I.assertDeepEqual(pasted.paddings, ['13px', '13px'], 'Every responsive style selector must use the new style ID');
    I.assertNotEqual(pasted.ids[0], pasted.ids[1], 'Repeated paste must not create duplicate IDs');
    I.assertDeepEqual(pasted.links, pasted.ids.map(id => '#'+id), 'Local links must follow the copied IDs');
    I.assertEqual(await I.executeScript((root, key) => JSON.parse(localStorage.getItem(key)).expiresAt, clipboardKey), copied.expiresAt, 'Repeated pasting must not extend clipboard expiry');
    I.click(toolbar+' [data-pb-action=more]');
    I.saveScreenshot('pagebuilder-clipboard-menu.png');
    I.pressKey('Escape');
    I.switchTo();
    DTE.save();
    savedTitle = title;
    DT.filterEquals('title', title);
    I.click(locate('#datatableInit a').withText(title));
    DTE.waitForEditor();
    waitForEditors(I);
    I.assertEqual(await I.grabNumberOfVisibleElements('.clipboard-section-autotest'), 2, 'Both copies must survive save and reopen');
    I.see('changed autotest', '.clipboard-section-autotest');
    I.switchTo();
    DTE.cancel();
});

Scenario('clipboard restricts container and column targets and supports an empty page', async ({I, DT, DTE, Document}) => {
    await openPage(I, DT, DTE, Document, source);
    for (const type of ['container', 'column']) {
        copyBlock(I, type);
        startPaste(I);
        I.assertDeepEqual(await I.executeScript(() => [...new Set(window.pageBuilder.ui.insertPoints.map(point => point.type))]), [type], 'Only compatible destinations may be shown');
        I.click(locate('.pb-insert-point[data-type='+type+'] button').last());
        waitForEditors(I);
        I.assertEqual(await I.executeScript(() => window.pageBuilder.workbench_type(window.pageBuilder.ui.selected)), type, 'Paste must initialize the inserted structural type');
        loadSource(I, source);
    }
    copyBlock(I, 'section');
    removeOnlySection(I);
    I.click(toolbar+' [data-pb-action=paste]');
    I.waitForVisible('.pb-insert-layer', 10);
    I.click('.pb-insert-point[data-type=section] button');
    waitForEditors(I);
    I.seeElement('#wjInline-docdata > .clipboard-section-autotest');
    I.switchTo();
    DTE.cancel();
});

Scenario('clipboard shares blocks across tabs and keeps an active paste snapshot', async ({I, DT, DTE, Document}) => {
    await openPage(I, DT, DTE, Document, source);
    I.executeScript((root, key) => localStorage.removeItem(key), clipboardKey);
    I.switchTo();
    I.openNewTab();
    await openPage(I, DT, DTE, Document, target);
    I.click('.clipboard-target-autotest p');
    I.click(toolbar+' [data-pb-action=more]');
    I.seeElement(toolbar+' [data-pb-action=copy] i.ti-clipboard');
    I.seeElement(toolbar+' [data-pb-action=paste]:disabled i.ti-clipboard-plus');

    I.switchTo();
    I.switchToPreviousTab();
    waitForEditors(I);
    copyBlock(I, 'section');
    I.switchTo();
    I.switchToNextTab();
    waitForEditors(I);
    I.waitForVisible(toolbar+'-menu [data-pb-action=paste]:enabled', 10);
    I.seeAttributesOnElements(toolbar+' [data-pb-action=paste]', {'data-title': 'Vložiť — Sekcia'});
    I.saveScreenshot('pagebuilder-clipboard-tabs-menu.png');
    I.click(toolbar+' [data-pb-action=paste]');
    I.waitForVisible('.pb-insert-layer', 10);

    // A later copy in another tab must not replace the block already being positioned.
    I.switchTo();
    I.switchToPreviousTab();
    waitForEditors(I);
    copyBlock(I, 'column');
    I.switchTo();
    I.switchToNextTab();
    waitForEditors(I);
    I.assertEqual(await I.executeScript(() => window.pageBuilder.ui.pasting.type), 'section', 'An active paste must retain its original snapshot');
    I.click(locate('.pb-insert-point[data-type=section] button').last());
    waitForEditors(I);
    I.seeElement('.clipboard-target-autotest + .clipboard-section-autotest');
    startPaste(I);
    I.assertDeepEqual(await I.executeScript(() => [...new Set(window.pageBuilder.ui.insertPoints.map(point => point.type))]), ['column'], 'The next paste must use the latest copy from the other tab');
    I.click(locate('.pb-insert-point[data-type=column] button').last());
    waitForEditors(I);
    I.assertEqual(await I.grabNumberOfVisibleElements('.clipboard-column-autotest'), 2, 'Both block types must paste successfully across tabs');
    I.click(toolbar+' [data-pb-action=more]');

    I.switchTo();
    I.switchToPreviousTab();
    // Shorten the stored deadline to exercise real expiry timers in both tabs without waiting 30 minutes.
    I.executeScript(key => {
        const data = JSON.parse(localStorage.getItem(key));
        data.expiresAt = Date.now() + 1500;
        localStorage.setItem(key, JSON.stringify(data));
    }, clipboardKey);
    I.switchToNextTab();
    waitForEditors(I);
    I.waitForVisible(toolbar+'-menu [data-pb-action=paste]:disabled', 10);
    I.assertEqual(await I.executeScript((root, key) => localStorage.getItem(key), clipboardKey), null, 'Expiry must remove the stored block');

    I.switchTo();
    I.switchToPreviousTab();
    waitForEditors(I);
    copyBlock(I, 'section');
    I.switchTo();
    I.switchToNextTab();
    waitForEditors(I);
    I.waitForVisible(toolbar+'-menu [data-pb-action=paste]:enabled', 10);
    I.switchTo();
    DTE.cancel();
    I.closeCurrentTab();
    DTE.cancel();
});

Scenario('clipboard preserves applications and generated widget references', async ({I, DT, DTE, Document}) => {
    const include = '!INCLUDE(/components/app-htmlembed/embed.jsp, html="'+Buffer.from('<p>Clipboard application autotest</p>').toString('base64')+'")!';
    const widgets = `<section class="clipboard-widgets-autotest">
        <div class="container pb-not-container"><ul class="nav nav-tabs pb-autotabs"></ul><div class="tab-content">
            <div class="tab-pane pb-custom-container active show" title="Clipboard tab autotest"><div class="row"><div class="col-12"><p>Clipboard tab content autotest</p></div></div></div>
        </div></div>
        <div class="container pb-not-container pb-autoaccordion"><div class="card pb-custom-container">
            <div class="card-header"><a class="accordionLink pb-editable">Clipboard accordion autotest</a></div>
            <div class="collapse show"><div class="card-body"><div class="row"><div class="col-12"><p>Clipboard accordion content autotest</p></div></div></div></div>
        </div></div>
    </section><div class="pb-section clipboard-application-autotest">${include}</div>`;
    await openPage(I, DT, DTE, Document, widgets);
    I.click('.clipboard-widgets-autotest .tab-pane p');
    I.click(toolbar+'-path button[data-type=section]');
    I.click(toolbar+' [data-pb-action=more]');
    I.click(toolbar+' [data-pb-action=copy]');
    startPaste(I);
    I.click(locate('.pb-insert-point[data-type=section] button').last());
    waitForEditors(I);
    const widgetsState = await I.executeScript(() => {
        // Verify the periodic generators cannot undo the pasted ID mapping.
        pbAutotabs();
        pbAutoAcoordion();
        const sections = [...document.querySelectorAll('.clipboard-widgets-autotest')];
        return {
            tabs: sections.map(section => section.querySelector('.tab-pane').id),
            tabLinks: sections.map(section => section.querySelector('.pb-autotabs a').getAttribute('href')),
            collapses: sections.map(section => section.querySelector('.collapse').id),
            collapseLinks: sections.map(section => section.querySelector('.accordionLink').getAttribute('data-bs-target'))
        };
    });
    I.assertEqual(new Set(widgetsState.tabs).size, 2, 'Pasted tabs must keep unique IDs after regeneration');
    I.assertDeepEqual(widgetsState.tabLinks, widgetsState.tabs.map(id => '#'+id), 'Generated navigation must point to its own tab');
    I.assertEqual(new Set(widgetsState.collapses).size, 2, 'Pasted accordion panels must keep unique IDs');
    I.assertDeepEqual(widgetsState.collapseLinks, widgetsState.collapses.map(id => '#'+id), 'Accordion triggers must point to their own panels');
    // Application previews occupy the section; select it through the structure tree.
    I.click(toolbar+' [data-pb-action=structure]');
    I.click(locate('.pb-structure [role=tree] > [role=treeitem] > div').at(2));
    I.click(toolbar+' [data-pb-action=more]');
    I.click(toolbar+' [data-pb-action=copy]');
    const snapshot = await I.executeScript(() => JSON.parse(localStorage.getItem('webjet.pagebuilder.clipboard')));
    I.assertContain(snapshot.html.replace(/&quot;/g, '"'), include, 'Copy must store the application directive');
    I.assertFalse(/iframe|pb-temp-wrapper|data-ckeditor-instance/.test(snapshot.html), 'Application previews must not enter the clipboard');
    startPaste(I);
    I.click(locate('.pb-insert-point[data-type=section] button').last());
    waitForEditors(I);
    I.assertEqual(await I.grabNumberOfVisibleElements('.clipboard-application-autotest iframe.wj_component'), 2, 'Pasted applications must initialize a new editable preview');
    I.switchTo();
    DTE.cancel();
});

Scenario('clipboard rejects unavailable destinations and invalid storage without changing content', async ({I, DT, DTE, Document}) => {
    await openPage(I, DT, DTE, Document, source);
    copyBlock(I, 'column');
    removeOnlySection(I);
    I.click(toolbar+' [data-pb-action=paste]');
    I.dontSeeElement('.pb-insert-layer');
    I.dontSeeElement('#wjInline-docdata > section');
    I.switchTo();
    Document.notifyCheckAndClose('Na stránke nie je vhodné miesto pre skopírovaný blok.');
    I.switchTo(frameSelector);
    I.executeScript((root, key) => {
        const data = JSON.parse(localStorage.getItem(key));
        delete data.expiresAt;
        localStorage.setItem(key, JSON.stringify(data));
    }, clipboardKey);
    loadSource(I, target);
    I.click('.clipboard-target-autotest p');
    I.click(toolbar+' [data-pb-action=more]');
    I.seeElement(toolbar+' [data-pb-action=paste]:disabled');
    I.assertEqual(await I.executeScript((root, key) => localStorage.getItem(key), clipboardKey), null, 'Legacy snapshots without an expiry must be removed');
    I.pressKey('Escape');
    I.executeScript(() => localStorage.setItem('webjet.pagebuilder.clipboard', '{invalid JSON'));
    I.click(toolbar+' [data-pb-action=more]');
    I.seeElement(toolbar+' [data-pb-action=paste]:disabled');
    I.pressKey('Escape');
    I.click(toolbar+'-path button[data-type=row]');
    I.click(toolbar+' [data-pb-action=more]');
    I.dontSeeElement(toolbar+' [data-pb-action=copy]');
    I.switchTo();
    DTE.cancel();
});

Scenario('clipboard cleanup of the saved autotest page', ({I, DT}) => {
    I.amOnPage('/admin/v9/webpages/web-pages-list/?groupid=34495');
    DT.waitForLoader();
    I.executeScript(key => localStorage.removeItem(key), clipboardKey);
    if (!savedTitle) return;
    DT.filterEquals('title', savedTitle);
    DT.deleteAll();
    DT.waitForLoader();
});
