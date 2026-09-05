Feature('webpages.pagebuilder');

const assert = require('assert');

Before(({ I, login }) => {
    login('admin');
});

Scenario('overenie zobrazenia podla sablony', async ({I, DTE, Document}) => {

    //reset PB settings
    Document.resetPageBuilderMode();

    //stranka s PB
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();

    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.waitForElement("div.exit-inline-editor", 10);
    I.waitForElement("#trEditor div.wysiwyg", 30);
    I.waitForInvisible("#trEditor div.wysiwyg_textarea", 30);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForElement("div.exit-inline-editor", 10);
    var value = await I.grabValueFrom({css: "div.exit-inline-editor select"});
    I.assertEqual(value, "pageBuilder", "Expected editor type selector to have value 'pageBuilder'");

    I.seeElement("div.cke_inner");
    I.see("Odstavec a zarovnanie");
    I.waitForElement("#wjInline-docdata.pb-wrapper", 10);

    //over moznost prepnutia editora
    I.selectOption({css: "div.exit-inline-editor select"}, "");
    I.wait(2);

    I.switchTo();

    I.waitForInvisible("#trEditor div.wysiwyg", 10);
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //otvor stranku kde nie je PB
    DTE.cancel();
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(5);

    I.waitForText("Štandardný", 10, "div.exit-inline-editor button .filter-option-inner-inner");
    I.dontSee("Page Builder", "div.exit-inline-editor button .filter-option-inner-inner");
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //
    I.say("reload page and check if PB is still NOT active");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(5);
    I.dontSeeElement("#trEditor div.wysiwyg");
    I.seeElement("#trEditor div.wysiwyg_textarea");

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - prepnutie editora', async ({I, DTE, Apps, Document}) => {
    //bug: nacitam do editora stranku, prepnem na Standardny editor, prepnem do HTML kodu, ulozim
    //otvorim inu stranku, prepnem editor na Standardny a vidim stary text

    //reset PB settings
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.selectOption({css: "div.exit-inline-editor select"}, "");
    I.switchTo();
    I.wait(2);

    I.waitForElement('.cke_wysiwyg_frame.cke_reset', 10);
    Apps.switchEditor('html');
    const html = await I.executeScript(() => document.querySelector('.CodeMirror').CodeMirror.getValue());
    assert.ok(html.includes('Suspendisse interdum dolor justo, ac venenatis massa'), 'The HTML editor must retain the complete page, including content outside its virtual viewport');

    I.wait(1);
    I.switchTo();
    DTE.cancel();

    //otvor druhu stranku a over zobrazeny kod
    I.click("Produktová stránka - multi");
    DTE.waitForEditor();
    I.wait(5);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.dontSee("This is old page");
    I.switchTo();

    Apps.switchEditor('standard');
    I.dontSee("Suspendisse interdum dolor justo, ac venenatis massa");

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - zobrazenie standardny po prepnuti a zatvoreni okna', async ({I, DTE, Document}) => {
    //bug: ked prepnem z PB na standardny, zatvorim okno, otvorim, tak sa prepinac nezobrazi
    //overit aj to, ze sa nezobrazi na stranke, kde nie je PB zapnute

    //reset PB settings
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();

    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.waitForElement("#DTE_Field_data-editorTypeSelector select", 10);
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForElement("div.exit-inline-editor", 10);
    I.selectOption({css: "div.exit-inline-editor select"}, "");
    I.switchTo();
    I.wait(2);

    DTE.cancel();

    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.waitForElement("div.exit-inline-editor", 10);

    DTE.cancel();

    //
    I.say("stranka bez pagebuildera");
    I.click("Produktová stránka - B verzia");
    DTE.waitForEditor();
    I.wait(2);
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);

    DTE.cancel();

    //
    I.say("otvor znova PB a over, ze mame selector aj s moznostou PageBuilder");
    I.click("Produktová stránka - PageBuilder");
    DTE.waitForEditor();
    I.waitForElement("div.exit-inline-editor", 10);
    I.clickCss("div.exit-inline-editor button");
    I.see("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);

    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - nova stranka sablona podla priecinka', async ({I, DT, DTE}) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=0");
    DT.waitForLoader();
    I.jstreeNavigate(["Test stavov", "Page Builder"]);
    I.click("Page Builder", "#datatableInit_wrapper");

    //tato stranka nema PB
    DTE.waitForEditor();
    I.wait(5);
    I.dontSeeElement("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement("div.exit-inline-editor", 10);
    I.clickCss("div.exit-inline-editor button");
    I.dontSee("Page Builder", "ul.dropdown-menu.inner.show li a span");
    I.pressKey(['Escape']);
    DTE.cancel();

    //
    I.say("skusim novu stranku, ta musi mat PB option v selecte");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss("#pills-dt-datatableInit-content-tab");
    I.waitForVisible("#DTE_Field_data-pageBuilderIframe", 5);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForElement("div.exit-inline-editor", 10);
    var value = await I.grabValueFrom({css: "div.exit-inline-editor select"});
    I.assertEqual(value, "pageBuilder", "Expected editor type selector to have value 'pageBuilder'");
    I.switchTo();

    DTE.cancel();
});

function openStyleModal(I, colSelector=".col-3") {
    I.waitForElement("#trEditor div.wysiwyg", 10);
    I.waitForInvisible("#trEditor > div.wysiwyg_textarea", 10);

    I.switchTo('#DTE_Field_data-pageBuilderIframe');

    I.waitForElement("div.cke_inner", 10);
    I.seeElement("div.cke_inner");
    I.waitForText("Odstavec a zarovnanie", 10);
    I.see("Odstavec a zarovnanie");

    //
    I.waitForElement("#wjInline-docdata.pb-wrapper", 10);
    I.say("Select a column using the shared toolbar");
    I.click({css: "section:nth-child(1) .container .row "+colSelector+":nth-child(1) :is(h1,h2,h3,h4,p):not(:has(img))"});
    I.click(locate(".pb-workbench-path button[data-type=column]").last());
    I.waitForVisible(".pb-outline[data-type=column]", 10);

    //
    I.say("Open style modal");
    I.click(".pb-workbench [data-pb-action=more]");
    I.click(".pb-workbench [data-pb-action=style]");
    I.waitForElement("#wjInline-docdata.pb-is-modal-open div.pb-modal", 10);
}

function closeStyleModal(I) {
    I.forceClick({css: "#wjInline-docdata.pb-is-modal-open div.pb-modal .pb-modal__footer .pb-modal__footer__button-close"});
    I.dontSeeElement("#wjInline-docdata div.pb-modal");
}

function saveStyleModal(I) {
    I.forceClick({css: "#wjInline-docdata.pb-is-modal-open div.pb-modal .pb-modal__footer .pb-modal__footer__button-save"});
    I.dontSeeElement("#wjInline-docdata div.pb-modal");
}

const pricingListsSelector = "section.prices .card-body ul.list-group";
const duplicableRowFixtureSelector = ".pb-row-duplicable-autotest";
const duplicableRowSelector = duplicableRowFixtureSelector+" > [data-pb-autotest='duplicable-row'].pb-duplicable-element";

function getDuplicableItem(listIndex, itemIndex) {
    return locate(pricingListsSelector)
        .at(listIndex)
        .find("li.pb-duplicable-element:nth-of-type("+itemIndex+")");
}

function openDuplicableToolbar(I, listIndex, itemIndex, action) {
    const item = getDuplicableItem(listIndex, itemIndex);
    I.click(item);
    I.click(".pb-workbench [data-pb-action=more]");
    I.waitForVisible(".pb-workbench [data-pb-action="+action+"]", 10);
}

async function getDuplicableItemTexts(I, listIndex) {
    return await I.executeScript((root, { selector, index }) => {
        const list = document.querySelectorAll(selector)[index];
        if (list == null) return [];

        return Array.from(list.children)
            .filter(item => item.matches("li.pb-duplicable-element"))
            .map(item => Array.from(item.childNodes)
                .filter(node => node.nodeType === Node.TEXT_NODE)
                .map(node => node.textContent.trim())
                .filter(Boolean)
                .join(" "));
    }, { selector: pricingListsSelector, index: listIndex - 1 });
}

async function armDuplicableToolbarMouseupProbe(I, listIndex, itemIndex, action) {
    const armed = await I.executeScript((root, args) => {
        const list = document.querySelectorAll(args.selector)[args.listIndex - 1];
        const item = list && list.querySelector(":scope > li.pb-duplicable-element:nth-of-type("+args.itemIndex+")");
        const button = document.querySelector(".pb-workbench [data-pb-action="+args.action+"]");
        const editable = item && item.closest('[contenteditable="true"]');

        if (button == null || editable == null) return false;

        const probe = {
            button: button,
            editable: editable,
            reached: false
        };
        probe.listener = function(event) {
            if (event.target === probe.button || probe.button.contains(event.target)) {
                probe.reached = true;
            }
        };

        window.pbDuplicableToolbarMouseupProbe = probe;
        editable.addEventListener("mouseup", probe.listener);
        return true;
    }, { selector: pricingListsSelector, listIndex, itemIndex, action });

    assert.strictEqual(armed, true, "The duplicable toolbar mouseup probe must be armed");
}

async function assertDuplicableToolbarMouseupStopped(I, action) {
    const reachedEditable = await I.executeScript(() => {
        const probe = window.pbDuplicableToolbarMouseupProbe;
        if (probe == null) return null;

        probe.editable.removeEventListener("mouseup", probe.listener);
        delete window.pbDuplicableToolbarMouseupProbe;
        return probe.reached;
    });

    assert.strictEqual(reachedEditable, false, action+" mouseup must not reach the CKEditor editable");
}

async function openDuplicableElementsPage(I, DTE, Document) {
    Document.resetPageBuilderMode();
    I.resizeWindow(1280, 960);
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement(pricingListsSelector, 10);

    const fixtureState = await I.executeScript((root, { selector }) => {
        const lists = Array.from(document.querySelectorAll(selector));
        lists.forEach(list => Array.from(list.children).forEach(item => item.classList.add("pb-duplicable")));
        window.pageBuilder.mark_duplicable_elements();

        return {
            listCount: lists.length,
            itemCounts: lists.map(list => list.querySelectorAll(":scope > li.pb-duplicable").length)
        };
    }, { selector: pricingListsSelector });

    assert.strictEqual(fixtureState.listCount, 3, "The pricing fixture must contain three lists");
    assert.deepStrictEqual(fixtureState.itemCounts, [3, 3, 3], "Each pricing list must contain three marked items");
    I.waitForElement(getDuplicableItem(1, 1).find("aside.pb-toolbar"), 10);
}

function getDuplicableRowController(rowIndex, controllerSelector) {
    return {css: duplicableRowSelector+":nth-of-type("+rowIndex+") > "+controllerSelector};
}

function openDuplicableRowToolbar(I, rowIndex, action) {
    I.click(getDuplicableRowController(rowIndex, ".pb-column .column-content p"));
    I.click(locate(".pb-workbench-path button[data-type=row]").last());
    I.click(".pb-workbench [data-pb-action=more]");
    I.waitForVisible(".pb-workbench [data-pb-action="+action+"]", 10);
}

async function getPageBuilderFrame(page) {
    const iframeElement = await page.locator("#DTE_Field_data-pageBuilderIframe").elementHandle();
    assert.ok(iframeElement, "The Page Builder iframe element must exist");

    const frame = await iframeElement.contentFrame();
    assert.ok(frame, "The Page Builder iframe must have a content frame");
    return frame;
}

async function waitForDuplicableRowEditors(I, expectedCount) {
    await I.usePlaywrightTo("wait for independent duplicable row CKEditor instances", async ({ page }) => {
        const frame = await getPageBuilderFrame(page);
        await frame.waitForFunction(args => {
            const rows = Array.from(document.querySelectorAll(args.selector));
            const editorNames = rows.map(row => {
                const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
                return editable && editable.getAttribute("data-ckeditor-instance");
            });
            return rows.length === args.count && editorNames.every((editorName, index) => {
                const editable = rows[index].querySelector(":scope > .pb-column > .pb-column__content");
                const editor = editorName && CKEDITOR.instances[editorName];
                return editor && editor.status === "ready" && editor.element.$ === editable;
            }) && new Set(editorNames).size === args.count;
        }, { selector: duplicableRowSelector, count: expectedCount }, { timeout: 20000 });
    });
}

async function waitForDuplicableRowCount(I, expectedCount) {
    await I.usePlaywrightTo("wait for the duplicable row count", async ({ page }) => {
        const frame = await getPageBuilderFrame(page);
        await frame.waitForFunction(args => document.querySelectorAll(args.selector).length === args.count,
            { selector: duplicableRowSelector, count: expectedCount }, { timeout: 10000 });
    });
}

async function setDuplicableRowEditorData(I, rowIndex, text) {
    await I.usePlaywrightTo("set and wait for duplicable row CKEditor data", async ({ page }) => {
        const frame = await getPageBuilderFrame(page);
        const args = { selector: duplicableRowSelector, rowIndex, text };

        await frame.evaluate(args => {
            const row = document.querySelectorAll(args.selector)[args.rowIndex - 1];
            const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
            const editor = CKEDITOR.instances[editable.getAttribute("data-ckeditor-instance")];
            editor.setData("<p>"+args.text+"</p>");
        }, args);
        await frame.waitForFunction(args => {
            const row = document.querySelectorAll(args.selector)[args.rowIndex - 1];
            const editable = row && row.querySelector(":scope > .pb-column > .pb-column__content");
            const editorName = editable && editable.getAttribute("data-ckeditor-instance");
            const editor = editorName && CKEDITOR.instances[editorName];
            return editor && editor.getData().includes(args.text) && editable.textContent.includes(args.text);
        }, args, { timeout: 10000 });
    });
}

Scenario('check toolbar elements', ({I, DTE, Document}) => {
    //reset PB settings
    Document.resetPageBuilderMode();

    //stranka s PB
    I.resizeWindow(1280, 960);
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);

    openStyleModal(I);
    closeStyleModal(I);

    //
    I.say("check styleCombo options");
    I.forceClick({css: "span.cke_combo__styles a.cke_combo_button"});
    I.waitForElement("iframe.cke_panel_frame", 5);
    I.switchTo("iframe.cke_panel_frame");
    I.see("Nadpis 1");
    I.see("baretest1");
    I.see("Bare TEST 02 bold");
    I.see("Bare TEST 03");
    I.see("Bare TEST 04");
    I.switchTo();
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.pressKey(['Escape']);
    I.dontSeeElement("iframe.cke_panel_frame");

    I.switchTo();
});

Scenario('duplicable elements toolbar, cleanup and operations', async ({I, DTE, Document}) => {
    await openDuplicableElementsPage(I, DTE, Document);

    const controllerState = await I.executeScript((root, { selector }) => {
        const item = document.querySelectorAll(selector)[0].querySelector(":scope > li.pb-duplicable-element");
        const toolbar = item.querySelector(":scope > aside.pb-toolbar");
        const columnToolbar = item.closest(".pb-column").querySelector(":scope > aside.pb-toolbar");

        return {
            buttonCount: toolbar.querySelectorAll("button.pb-toolbar-button").length,
            hasMove: toolbar.querySelector("button.pb-toolbar-button__move") != null,
            hasDuplicate: toolbar.querySelector("button.pb-toolbar-button__duplicate") != null,
            hasRemove: toolbar.querySelector("button.pb-toolbar-button__remove") != null,
            hasStyle: toolbar.querySelector("button.pb-toolbar-button__style") != null,
            hasResize: toolbar.querySelector("button.pb-toolbar-button__resize") != null,
            hasFavorite: toolbar.querySelector("button.pb-toolbar-button__add_to_favorites") != null,
            highlighterCount: item.querySelectorAll(":scope > aside.pb-highlighter").length,
            plusButtonCount: item.querySelectorAll(":scope > aside.pb-plus-button").length,
            dimmerCount: item.querySelectorAll(":scope > aside.pb-dimmer").length,
            highlighterColor: getComputedStyle(item.querySelector(":scope > aside.pb-highlighter__top")).backgroundColor,
            prependDisplay: getComputedStyle(item.querySelector(":scope > aside.pb-prepend")).display,
            appendDisplay: getComputedStyle(item.querySelector(":scope > aside.pb-append")).display,
            controllersAreNotEditable: Array.from(item.querySelectorAll(":scope > aside"))
                .every(controller => controller.getAttribute("contenteditable") === "false"),
            columnHasStyle: columnToolbar.querySelector("button.pb-toolbar-button__style") != null,
            columnHasFavorite: columnToolbar.querySelector("button.pb-toolbar-button__add_to_favorites") != null
        };
    }, { selector: pricingListsSelector });

    assert.strictEqual(controllerState.buttonCount, 3, "A duplicable toolbar must contain exactly three actions");
    assert.strictEqual(controllerState.hasMove, true, "The move action must be available");
    assert.strictEqual(controllerState.hasDuplicate, true, "The duplicate action must be available");
    assert.strictEqual(controllerState.hasRemove, true, "The remove action must be available");
    assert.strictEqual(controllerState.hasStyle, false, "The style action must not be available");
    assert.strictEqual(controllerState.hasResize, false, "The resize action must not be available");
    assert.strictEqual(controllerState.hasFavorite, false, "The favorite action must not be available");
    assert.strictEqual(controllerState.highlighterCount, 4, "A duplicable element must have four highlighter sides");
    assert.strictEqual(controllerState.plusButtonCount, 2, "A duplicable element must have prepend and append targets");
    assert.strictEqual(controllerState.dimmerCount, 1, "A duplicable element must have one dimmer");
    assert.strictEqual(controllerState.highlighterColor, "rgb(209, 122, 0)", "A duplicable element must use the orange highlighter");
    assert.strictEqual(controllerState.prependDisplay, "none", "The prepend target must stay hidden while idle");
    assert.strictEqual(controllerState.appendDisplay, "none", "The append target must stay hidden while idle");
    assert.strictEqual(controllerState.controllersAreNotEditable, true, "Page Builder controllers must not be editable by CKEditor");
    assert.strictEqual(controllerState.columnHasStyle, true, "The existing column toolbar must keep the style action");
    assert.strictEqual(controllerState.columnHasFavorite, true, "The existing column toolbar must keep the favorite action");

    const cleanState = await I.executeScript((root, { selector }) => {
        const pageBuilder = window.pageBuilder;
        const node = pageBuilder.getClearNode();
        pageBuilder.clearEditorAttributes(node);

        return {
            markerCount: node.find(selector+" > li.pb-duplicable").length,
            runtimeClassCount: node.find(selector+" > li.pb-duplicable-element, "+selector+" > li.pb-grid-element").length,
            controllerCount: node.find(selector+" > li > aside.pb-toolbar, "+selector+" > li > aside.pb-plus-button, "+selector+" > li > aside.pb-highlighter, "+selector+" > li > aside.pb-dimmer").length
        };
    }, { selector: pricingListsSelector });

    assert.strictEqual(cleanState.markerCount, 9, "Clean HTML must preserve all source marker classes");
    assert.strictEqual(cleanState.runtimeClassCount, 0, "Clean HTML must remove Page Builder runtime classes");
    assert.strictEqual(cleanState.controllerCount, 0, "Clean HTML must remove Page Builder controllers");

    openDuplicableToolbar(I, 1, 1, "duplicate");
    await armDuplicableToolbarMouseupProbe(I, 1, 1, "duplicate");
    I.click(".pb-workbench [data-pb-action=duplicate]");
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element.pb-is-duplicating", 10);
    await assertDuplicableToolbarMouseupStopped(I, "Duplicate");
    I.dontSeeElement(".cke_reset_all.cke_dialog_container");

    const duplicateTargets = await I.executeScript((root, { selector }) => Array.from(document.querySelectorAll(selector))
        .map(list => list.querySelectorAll(":scope > li.pb-is-duplicable-target").length), { selector: pricingListsSelector });
    assert.ok(duplicateTargets[0] > 0, "Compatible items in the same list must become duplicate targets");
    assert.deepStrictEqual(duplicateTargets.slice(1), [0, 0], "Items with another parent must not become duplicate targets");

    // A forced click verifies that JavaScript also rejects a visually hidden incompatible target.
    I.forceClick(getDuplicableItem(2, 1).find("aside.pb-append"));
    I.seeElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element.pb-is-duplicating");
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["Nunc sed purus", "rutrum varius sollicitudin", "vulputate purus"]);
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 2), ["Nunc sed purus", "rutrum varius sollicitudin", "vulputate purus"]);

    I.forceClick(getDuplicableItem(1, 2).find("aside.pb-append"));
    I.waitForElement(getDuplicableItem(1, 4), 10);
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["Nunc sed purus", "rutrum varius sollicitudin", "Nunc sed purus", "vulputate purus"]);
    I.dontSeeElement("#wjInline-docdata.pb-is-moving-child");
    I.dontSeeElement("li.pb-is-duplicable-target");

    openDuplicableToolbar(I, 1, 4, "move");
    I.click(".pb-workbench [data-pb-action=move]");
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element", 10);
    I.dontSeeElement("#wjInline-docdata.pb-is-duplicating");
    I.dontSeeElement(locate(pricingListsSelector).at(2).find("li.pb-is-duplicable-target"));
    I.forceClick(getDuplicableItem(1, 1).find("aside.pb-prepend"));
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["vulputate purus", "Nunc sed purus", "rutrum varius sollicitudin", "Nunc sed purus"]);

    openDuplicableToolbar(I, 1, 2, "move");
    I.click(".pb-workbench [data-pb-action=move]");
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element", 10);
    const iframeFocused = await I.executeScript((root) => {
        const cancelButton = root.querySelector("button.pb-notify__footer__button");
        cancelButton.focus();
        return root.ownerDocument.activeElement === cancelButton;
    });
    assert.strictEqual(iframeFocused, true, "The Page Builder iframe must receive the Escape key");
    I.pressKey("Escape");
    I.dontSeeElement("#wjInline-docdata.pb-is-moving-child");
    I.dontSeeElement("li.pb-is-moving, li.pb-is-duplicable-target");
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["vulputate purus", "Nunc sed purus", "rutrum varius sollicitudin", "Nunc sed purus"]);

    I.amAcceptingPopups();
    openDuplicableToolbar(I, 1, 4, "remove");
    await armDuplicableToolbarMouseupProbe(I, 1, 4, "remove");
    I.click(".pb-workbench [data-pb-action=remove]");
    I.acceptPopup();
    await assertDuplicableToolbarMouseupStopped(I, "Remove");
    I.dontSeeElement(".cke_reset_all.cke_dialog_container");
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["vulputate purus", "Nunc sed purus", "rutrum varius sollicitudin"]);

    // Close the editor without saving the DOM-only fixture changes.
    I.switchTo();
    DTE.cancel();
    I.acceptPopup();
});

Scenario('duplicable row toolbar, CKEditor lifecycle and cleanup', async ({I, DTE, Document}) => {
    Document.resetPageBuilderMode();
    I.resizeWindow(1280, 960);
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    I.waitForElement("#DTE_Field_data-pageBuilderIframe", 10);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement("#wjInline-docdata.pb-wrapper", 10);

    const fixtureInserted = await I.executeScript((root, { fixtureClass }) => {
        const section = document.querySelector("#wjInline-docdata > section.pb-section");
        if (section == null) return false;

        const fixture = document.createElement("div");
        fixture.className = "container "+fixtureClass;
        fixture.innerHTML = '<div class="row pb-duplicable" data-pb-autotest="duplicable-row">' +
            '<div class="col-12"><p>row-autotest-initial</p></div>' +
            '</div>';
        section.appendChild(fixture);
        window.markPbElements("doc_data");
        return true;
    }, { fixtureClass: duplicableRowFixtureSelector.substring(1) });

    assert.strictEqual(fixtureInserted, true, "The row fixture must be inserted into a Page Builder section");
    I.waitForElement(duplicableRowSelector, 10);
    await waitForDuplicableRowEditors(I, 1);

    const initialState = await I.executeScript((root, { selector }) => {
        const row = document.querySelector(selector);
        const toolbar = row.querySelector(":scope > aside.pb-toolbar");
        const columnToolbar = row.querySelector(":scope > .pb-column > aside.pb-toolbar");
        const leftHighlighter = row.querySelector(":scope > aside.pb-highlighter__left");
        const rightHighlighter = row.querySelector(":scope > aside.pb-highlighter__right");
        const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
        const rowToolbarRect = toolbar.getBoundingClientRect();
        const columnToolbarRect = columnToolbar.getBoundingClientRect();

        return {
            hasRowRuntimeClass: row.classList.contains("pb-row"),
            buttonClasses: Array.from(toolbar.querySelectorAll("button.pb-toolbar-button"))
                .map(button => button.className),
            sourceEditorName: editable.getAttribute("data-ckeditor-instance"),
            editorNames: Object.keys(CKEDITOR.instances),
            columnToolbarCount: row.querySelectorAll(":scope > .pb-column > aside.pb-toolbar").length,
            sideHighlighterWidths: [
                leftHighlighter.getBoundingClientRect().width,
                rightHighlighter.getBoundingClientRect().width
            ],
            controllersHaveNoHorizontalPadding: Array.from(row.querySelectorAll(":scope > aside")).every(controller => {
                const style = getComputedStyle(controller);
                return parseFloat(style.paddingLeft) === 0 && parseFloat(style.paddingRight) === 0;
            }),
            toolbarHandlesOverlap:
                rowToolbarRect.left < columnToolbarRect.right &&
                rowToolbarRect.right > columnToolbarRect.left &&
                rowToolbarRect.top < columnToolbarRect.bottom &&
                rowToolbarRect.bottom > columnToolbarRect.top,
            toolbarHorizontalGap: columnToolbarRect.left - rowToolbarRect.right
        };
    }, { selector: duplicableRowSelector });

    assert.strictEqual(initialState.hasRowRuntimeClass, true, "A duplicable row must retain its Page Builder row identity");
    assert.strictEqual(initialState.buttonClasses.length, 3, "A duplicable row toolbar must contain exactly three actions");
    assert.strictEqual(initialState.buttonClasses.some(classes => classes.includes("pb-toolbar-button__move")), true, "The row move action must be available");
    assert.strictEqual(initialState.buttonClasses.some(classes => classes.includes("pb-toolbar-button__duplicate")), true, "The row duplicate action must be available");
    assert.strictEqual(initialState.buttonClasses.some(classes => classes.includes("pb-toolbar-button__remove")), true, "The row remove action must be available");
    assert.strictEqual(initialState.columnToolbarCount, 1, "The column inside a duplicable row must keep its own toolbar");
    assert.strictEqual(initialState.sideHighlighterWidths.every(width => width === 0), true, "Legacy highlighters must be hidden behind the shared outline");
    assert.strictEqual(initialState.controllersHaveNoHorizontalPadding, true, "Page Builder controllers inside a Bootstrap row must not inherit row gutter padding");
    assert.strictEqual(initialState.toolbarHandlesOverlap, false, "The row and column toolbar handles must not overlap");
    assert.strictEqual(initialState.toolbarHorizontalGap, 0, "Legacy handles must not occupy canvas space");

    openDuplicableRowToolbar(I, 1, "duplicate");
    I.click(".pb-workbench [data-pb-action=duplicate]");
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element.pb-is-duplicating", 10);
    I.waitForVisible(getDuplicableRowController(1, "aside.pb-append"), 10);
    I.click(getDuplicableRowController(1, "aside.pb-append"));
    await waitForDuplicableRowEditors(I, 2);

    const duplicatedState = await I.executeScript((root, { selector }) => Array.from(document.querySelectorAll(selector)).map(row => {
        const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
        const editorName = editable.getAttribute("data-ckeditor-instance");
        const editor = CKEDITOR.instances[editorName];
        return {
            editorName: editorName,
            editorOwnsElement: editor.element.$ === editable
        };
    }), { selector: duplicableRowSelector });

    assert.strictEqual(duplicatedState.length, 2, "Duplicating a row must create one new row");
    assert.strictEqual(duplicatedState[0].editorName, initialState.sourceEditorName, "The source row must keep its CKEditor instance");
    assert.notStrictEqual(duplicatedState[1].editorName, initialState.sourceEditorName, "The duplicated row must get a distinct CKEditor instance");
    assert.strictEqual(initialState.editorNames.includes(duplicatedState[1].editorName), false, "The duplicated row CKEditor instance must be newly initialized");
    assert.strictEqual(duplicatedState.every(state => state.editorOwnsElement), true, "Each CKEditor instance must own its row content element");

    await setDuplicableRowEditorData(I, 1, "row-autotest-source-edited");
    let editorData = await I.executeScript((root, { selector }) => Array.from(document.querySelectorAll(selector)).map(row => {
        const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
        return CKEDITOR.instances[editable.getAttribute("data-ckeditor-instance")].getData();
    }), { selector: duplicableRowSelector });
    assert.ok(editorData[0].includes("row-autotest-source-edited"), "The source editor must contain its new content");
    assert.ok(editorData[1].includes("row-autotest-initial"), "Editing the source row must not change the duplicated row");

    await setDuplicableRowEditorData(I, 2, "row-autotest-clone-edited");
    editorData = await I.executeScript((root, { selector }) => Array.from(document.querySelectorAll(selector)).map(row => {
        const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
        return CKEDITOR.instances[editable.getAttribute("data-ckeditor-instance")].getData();
    }), { selector: duplicableRowSelector });
    assert.ok(editorData[0].includes("row-autotest-source-edited") && !editorData[0].includes("row-autotest-clone-edited"), "Editing the clone must not change the source row");
    assert.ok(editorData[1].includes("row-autotest-clone-edited") && !editorData[1].includes("row-autotest-source-edited"), "The clone editor must contain only its new content");

    openDuplicableRowToolbar(I, 2, "move");
    I.click(".pb-workbench [data-pb-action=move]");
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element", 10);
    I.dontSeeElement("#wjInline-docdata.pb-is-duplicating");
    I.waitForVisible(getDuplicableRowController(1, "aside.pb-prepend"), 10);
    I.click(getDuplicableRowController(1, "aside.pb-prepend"));
    await waitForDuplicableRowEditors(I, 2);

    const movedState = await I.executeScript((root, { selector, removedEditorName }) => {
        const rows = Array.from(document.querySelectorAll(selector));
        return {
            rows: rows.map(row => {
                const editable = row.querySelector(":scope > .pb-column > .pb-column__content");
                const editorName = editable.getAttribute("data-ckeditor-instance");
                const editor = CKEDITOR.instances[editorName];
                return {
                    editorName: editorName,
                    editorData: editor.getData(),
                    editorOwnsElement: editor.element.$ === editable
                };
            }),
            removedEditorExists: CKEDITOR.instances[removedEditorName] != null
        };
    }, { selector: duplicableRowSelector, removedEditorName: duplicatedState[1].editorName });

    assert.ok(movedState.rows[0].editorData.includes("row-autotest-clone-edited"), "The moved row must keep its own content");
    assert.ok(movedState.rows[1].editorData.includes("row-autotest-source-edited"), "The source row content must remain unchanged after moving its sibling");
    assert.notStrictEqual(movedState.rows[0].editorName, duplicatedState[1].editorName, "The moved row must get a new CKEditor instance");
    assert.strictEqual(movedState.rows[1].editorName, initialState.sourceEditorName, "Moving the clone must not replace the source CKEditor instance");
    assert.strictEqual(movedState.rows.every(state => state.editorOwnsElement), true, "Each CKEditor instance must remain bound to its own element after moving a row");
    assert.strictEqual(movedState.removedEditorExists, false, "Moving a row must destroy the CKEditor instance bound to the removed DOM element");

    const cleanState = await I.executeScript((root, { fixtureSelector }) => {
        const saveData = window.getSaveData();
        const docData = saveData.editable.find(item => item.wjAppField === "doc_data");
        const savedPage = document.createElement("div");
        savedPage.innerHTML = docData.data;
        const fixture = savedPage.querySelector(fixtureSelector);
        const rows = Array.from(fixture.querySelectorAll(":scope > .row.pb-duplicable"));

        const runtimeSelector = ".pb-container, .pb-row, .pb-column, .pb-duplicable-element, .pb-grid-element";
        return {
            rowTexts: rows.map(row => row.textContent.trim()),
            markerCount: rows.length,
            runtimeClassCount: (fixture.matches(runtimeSelector) ? 1 : 0) + fixture.querySelectorAll(runtimeSelector).length,
            controllerCount: fixture.querySelectorAll("aside[class^='pb-']").length,
            editorAttributeCount: fixture.querySelectorAll("[data-ckeditor-instance], .editableElement").length
        };
    }, { fixtureSelector: duplicableRowFixtureSelector });

    assert.deepStrictEqual(cleanState.rowTexts, ["row-autotest-clone-edited", "row-autotest-source-edited"], "Saved HTML must use data from each row's own CKEditor instance in the moved order");
    assert.strictEqual(cleanState.markerCount, 2, "Saved HTML must preserve the duplicable marker on both rows");
    assert.strictEqual(cleanState.runtimeClassCount, 0, "Saved HTML must remove Page Builder runtime classes");
    assert.strictEqual(cleanState.controllerCount, 0, "Saved HTML must remove Page Builder controllers");
    assert.strictEqual(cleanState.editorAttributeCount, 0, "Saved HTML must remove CKEditor runtime attributes and classes");

    I.amAcceptingPopups();
    openDuplicableRowToolbar(I, 2, "remove");
    I.click(".pb-workbench [data-pb-action=remove]");
    I.acceptPopup();
    await waitForDuplicableRowEditors(I, 1);
    let removedEditorsState = await I.executeScript((root, editorName) => CKEDITOR.instances[editorName] == null,
        movedState.rows[1].editorName);
    assert.strictEqual(removedEditorsState, true, "Deleting a row must destroy its CKEditor instance");

    openDuplicableRowToolbar(I, 1, "remove");
    I.click(".pb-workbench [data-pb-action=remove]");
    I.acceptPopup();
    await waitForDuplicableRowCount(I, 0);
    I.waitForElement(duplicableRowFixtureSelector+" > .row.pb-row > aside.pb-empty-placeholder", 10);
    I.dontSeeElement(duplicableRowFixtureSelector+" > .pb-duplicable-element");
    removedEditorsState = await I.executeScript((root, editorName) => CKEDITOR.instances[editorName] == null,
        movedState.rows[0].editorName);
    assert.strictEqual(removedEditorsState, true, "Deleting the last row must destroy its CKEditor instance");

    // Close the editor without saving the DOM-only fixture changes.
    I.switchTo();
    DTE.cancel();
    I.acceptPopup();
});

const workbenchFixture = 'section.pb-workbench-autotest';

/** Opens a DOM-only fixture so workbench regressions never publish changes to the demo page. */
async function openWorkbenchFixture(I, DTE, Document) {
    Document.resetPageBuilderMode();
    I.executeScript(() => localStorage.removeItem('webjet.pagebuilder.guides'));
    I.resizeWindow(1440, 1000);
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    DTE.waitForEditor();
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForVisible('.pb-workbench', 20);
    await I.executeScript(() => {
        const section = document.createElement('section');
        section.className = 'pb-workbench-autotest';
        section.innerHTML = '<div class="container"><div class="row">' +
            '<div class="col-12 col-md-6 col-xl-6"><div class="column-content" style="overflow:hidden">' +
            '<h2>Workbench autotest</h2><p class="pb-workbench-copy">Editable text for the workbench autotest. This paragraph must keep its exact wrapping when the structure panel is opened and when guides are hidden.</p>' +
            '<ul><li class="pb-duplicable">First autotest item</li><li class="pb-duplicable">Second autotest item</li></ul></div></div>' +
            '<div class="col-12 col-md-6 col-xl-6"><p>Second autotest column</p></div>' +
            '</div><p class="pb-editable">Standalone autotest text</p></div>';
        document.querySelector('#wjInline-docdata').prepend(section);
        window.markPbElements('doc_data');
    });
    await I.usePlaywrightTo('wait for the workbench fixture editors', async ({page}) => {
        const frame = await getPageBuilderFrame(page);
        await frame.waitForFunction(selector => Array.from(document.querySelectorAll(selector+' [data-ckeditor-instance]'))
            .length === 3 && Array.from(document.querySelectorAll(selector+' [data-ckeditor-instance]'))
            .every(element => CKEDITOR.instances[element.dataset.ckeditorInstance]?.status === 'ready'), workbenchFixture);
    });
    I.click(workbenchFixture+' .pb-workbench-copy');
    I.waitForVisible('.pb-outline[data-type=column]', 10);
}

/** Captures layout relative to the content root, independent of selection-induced scrolling. */
async function workbenchGeometry(I) {
    return I.executeScript((root, selector) => {
        const wrapper = document.querySelector('#wjInline-docdata').getBoundingClientRect();
        return Array.from(document.querySelectorAll(selector+' .col-12, '+selector+' p')).map(element => {
            const rect = element.getBoundingClientRect();
            const range = document.createRange();
            range.selectNodeContents(element);
            return { left: rect.left - wrapper.left, top: rect.top - wrapper.top, width: rect.width, height: rect.height, lines: range.getClientRects().length };
        });
    }, workbenchFixture);
}

Scenario('workbench selection, structure and unchanged canvas geometry', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    const before = await workbenchGeometry(I);
    const state = await I.executeScript(() => {
        const selected = window.pageBuilder.ui.selected;
        const block = selected.getBoundingClientRect();
        const frame = document.querySelector('.pb-outline').getBoundingClientRect();
        const content = selected.querySelector('.pb-workbench-copy');
        const text = content.getBoundingClientRect();
        return {
            outside: [block.left - frame.left, block.top - frame.top, frame.right - block.right, frame.bottom - block.bottom],
            pointerEvents: getComputedStyle(document.querySelector('.pb-outline')).pointerEvents,
            legacyVisible: Array.from(document.querySelectorAll('.pb-toolbar, .pb-highlighter')).filter(node => node.getClientRects().length).length,
            chromeInContent: selected.closest('#wjInline-docdata').querySelectorAll('.pb-workbench, .pb-outline-layer, .pb-structure').length,
            textReachable: content.contains(document.elementFromPoint(text.left + 2, text.top + 5))
        };
    });
    assert.deepStrictEqual(state.outside, [4, 4, 4, 4], 'The outline must remain four pixels outside the selected box');
    assert.strictEqual(state.pointerEvents, 'none', 'The outline must not capture content clicks');
    assert.strictEqual(state.legacyVisible, 0, 'Ancestor outlines and local palettes must remain hidden');
    assert.strictEqual(state.chromeInContent, 0, 'New chrome must live outside serialized content');
    assert.strictEqual(state.textReachable, true, 'The first text character must remain clickable');
    const cleanLayout = await I.executeScript((root, selector) => {
        const wrapper = document.querySelector('#wjInline-docdata');
        const original = document.querySelector(selector);
        const clone = wrapper.cloneNode(false);
        clone.append(original.cloneNode(true));
        window.pageBuilder.getClearNode($(clone));
        window.pageBuilder.clearEditorAttributes($(clone));
        Object.assign(clone.style, {position: 'fixed', left: '-20000px', top: '0', width: wrapper.getBoundingClientRect().width+'px'});
        document.body.append(clone);
        const geometry = section => Array.from(section.querySelectorAll('.col-12, p')).map(element => {
            const rect = element.getBoundingClientRect();
            return {left: rect.left - section.getBoundingClientRect().left, width: rect.width, height: rect.height};
        });
        const result = {editing: geometry(original), clean: geometry(clone.querySelector(selector))};
        clone.remove();
        return result;
    }, workbenchFixture);
    assert.deepStrictEqual(cleanLayout.editing, cleanLayout.clean, 'Editor chrome must preserve the same box sizes and text wrapping as cleaned HTML');

    I.click('.pb-workbench [data-pb-action=structure]');
    I.waitForVisible('.pb-structure [role=treeitem]', 10);
    I.saveScreenshot('pagebuilder-workbench-structure.png');
    assert.deepStrictEqual(await workbenchGeometry(I), before, 'Opening the tree must not shrink or reflow the canvas');
    await I.usePlaywrightTo('expand a selected branch through its label while preserving arrow toggling', async ({page}) => {
        const frame = await getPageBuilderFrame(page);
        const branch = frame.locator('.pb-structure > ul > li').first();
        const arrow = branch.locator(':scope > div > [data-pb-expand]');
        const label = branch.locator(':scope > div > span').last();
        await arrow.click();
        assert.equal(await branch.getAttribute('aria-expanded'), 'false');
        await label.click();
        assert.equal(await branch.getAttribute('aria-expanded'), 'true', 'Clicking the label must expand a collapsed branch');
        assert.equal(await branch.getAttribute('aria-selected'), 'true', 'Expanding through the label must also select the block');
        assert.ok(await branch.locator(':scope > ul').isVisible(), 'The subtree must become visible');
        await label.click();
        assert.equal(await branch.getAttribute('aria-expanded'), 'true', 'Clicking an expanded label must keep its subtree open');
        await arrow.click();
        assert.equal(await branch.getAttribute('aria-expanded'), 'false', 'The arrow must still collapse the branch');
        await branch.press('Enter');
        assert.equal(await branch.getAttribute('aria-expanded'), 'true', 'Keyboard activation must also expand the branch');
    });
    I.fillField('.pb-structure input[type=search]', 'Second autotest column');
    I.waitForText('Second autotest column', 10, '.pb-structure');
    I.click(locate('.pb-structure [role=treeitem] > div').withText('Second autotest column').last());
    const selectedText = await I.executeScript(() => window.pageBuilder.ui.selected.textContent);
    assert.ok(selectedText.includes('Second autotest column'), 'Selecting a search result must select its actual column');
    I.fillField('.pb-structure input[type=search]', 'no-such-autotest-block');
    I.waitForVisible('.pb-structure > p', 10);
    I.click('.pb-structure [data-pb-action=close-structure]');
    I.click('.pb-workbench [data-pb-action=guides]');
    I.waitForInvisible('.pb-outline', 10);
    assert.deepStrictEqual(await workbenchGeometry(I), before, 'Hiding guides must preserve the complete canvas geometry');
    I.click('.pb-workbench [data-pb-action=guides]');
    I.click('.pb-workbench [data-pb-action=guides]');
    I.waitForVisible('.pb-outline:not([hidden])', 10);
    I.saveScreenshot('pagebuilder-workbench.png');
    I.switchTo();
    I.amAcceptingPopups();
    DTE.cancel();
});

/** Focuses a real insertion button, allowing its keyboard handler to reveal off-screen destinations. */
async function chooseWorkbenchInsertion(I, type, parent, index) {
    await I.executeScript((root, args) => {
        document.querySelectorAll('[data-autotest-insert]').forEach(button => button.removeAttribute('data-autotest-insert'));
        const point = window.pageBuilder.ui.insertPoints.filter(point => point.type === args.type && point.parent.matches(args.parent))[args.index];
        point.button.attr('data-autotest-insert', 'true');
        point.button[0].focus();
    }, {type, parent, index});
    I.click('[data-autotest-insert]');
    I.waitForVisible('.pb-library--'+type, 10);
}

Scenario('workbench CSS tooltips on toolbar and insertion buttons', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    await I.usePlaywrightTo('verify styled tooltip placement and keyboard focus', async ({page}) => {
        const frame=await getPageBuilderFrame(page);
        const selector='.pb-workbench [data-pb-action=guides]';
        await frame.locator(selector).hover();
        await frame.waitForFunction(selector=>getComputedStyle(document.querySelector(selector),'::before').opacity==='1',selector);
        const tooltip=await frame.locator(selector).evaluate(button=>{
            const style=getComputedStyle(button,'::before');
            const rect=button.getBoundingClientRect();
            return {title:button.getAttribute('title'),text:button.dataset.title,content:style.content,color:style.color,background:style.backgroundColor,left:rect.right-parseFloat(style.width),right:rect.right,width:innerWidth};
        });
        assert.equal(tooltip.title,null,'A styled tooltip must not also show the browser title');
        assert.ok(tooltip.content.includes(tooltip.text),'The tooltip must show the current button label');
        assert.equal(tooltip.color,'rgb(255, 255, 255)');
        assert.notEqual(tooltip.background,'rgba(0, 0, 0, 0)');
        assert.ok(tooltip.left>=0 && tooltip.right<=tooltip.width,'The last toolbar tooltip must fit the viewport');
        await page.locator('#DTE_Field_data-pageBuilderIframe').screenshot({path:'../../../build/test/pagebuilder-tooltip-toolbar.png'});
        await frame.locator(selector).click();
        const changed=await frame.locator(selector).getAttribute('data-title');
        assert.notEqual(changed,tooltip.text,'Guide mode changes must update the styled tooltip');
        await page.keyboard.press('Tab');
        await page.keyboard.press('Shift+Tab');
        await frame.waitForFunction(selector=>document.activeElement.matches(selector) && getComputedStyle(document.activeElement,'::before').opacity==='1',selector);
        await frame.locator('.pb-workbench [data-pb-action=insert]').click();
        await frame.waitForFunction(()=>window.pageBuilder.ui.insertAnimations.every(animation=>animation.playState==='finished'));
        await frame.evaluate(()=>{
            const point=window.pageBuilder.ui.insertPoints.find(point=>point.type==='column' && !point.previous && point.parent.closest('section.pb-workbench-autotest'));
            point.button.attr('data-autotest-tooltip','true');
            point.button[0].focus();
        });
        await frame.locator('[data-autotest-tooltip]').hover();
        await frame.waitForFunction(()=>getComputedStyle(document.querySelector('[data-autotest-tooltip]'),'::before').opacity==='1');
        assert.equal(await frame.locator('[data-autotest-tooltip]').getAttribute('data-tooltip-align'),'left');
        await page.locator('#DTE_Field_data-pageBuilderIframe').screenshot({path:'../../../build/test/pagebuilder-tooltip-insert.png'});
        await page.keyboard.press('Escape');
        await frame.waitForFunction(()=>!window.pageBuilder.ui.inserting);
    });
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench insertion expands without scrolling away from the first section', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    for (const reducedMotion of ['no-preference', 'reduce']) {
        await I.usePlaywrightTo('set the motion preference', async ({page}) => page.emulateMedia({reducedMotion}));
        I.executeScript(() => {
            window.scrollTo(0,0);
            document.querySelector('.pb-workbench [data-pb-action=insert]').addEventListener('click', () => {
                window.pbInsertionSamples=[];
                function sample() {
                    const ui=window.pageBuilder.ui;
                    const point=ui.insertPoints.find(point=>point.type==='section');
                    window.pbInsertionSamples.push({scroll:window.scrollY,height:point.space[0].getBoundingClientRect().height});
                    if (ui.insertAnimations.some(animation=>animation.playState==='running')) requestAnimationFrame(sample);
                }
                requestAnimationFrame(sample);
            }, {once:true});
        });
        I.click('.pb-workbench [data-pb-action=insert]');
        await I.usePlaywrightTo('wait for insertion expansion', async ({page}) => {
            const frame=await getPageBuilderFrame(page);
            await frame.waitForFunction(() => window.pbInsertionSamples?.length>0 && window.pageBuilder.ui.insertAnimations.every(animation=>animation.playState==='finished'));
        });
        const state=await I.executeScript(() => {
            const ui=window.pageBuilder.ui, point=ui.insertPoints.find(point=>point.type==='section');
            return {samples:window.pbInsertionSamples, firstTop:point.button[0].getBoundingClientRect().top, toolbarBottom:ui.bar[0].getBoundingClientRect().bottom};
        });
        assert.ok(state.samples.every(sample=>sample.scroll<=1),'Opening insertion mode at the top must never scroll the first destination out of view');
        assert.ok(state.firstTop>=state.toolbarBottom,'The first section destination must be visible without scrolling back up');
        if (reducedMotion==='no-preference') assert.ok(state.samples.some(sample=>sample.height>0 && sample.height<48),'Visible gaps must expand gradually');
        else assert.ok(state.samples.every(sample=>sample.height===48),'Reduced motion must show the completed layout immediately');
        I.click('.pb-workbench [data-pb-action=insert]');
        I.dontSeeElement('.pb-insert-space');
    }
    await I.usePlaywrightTo('restore the motion preference', async ({page}) => page.emulateMedia({reducedMotion:'no-preference'}));
    const scrolled = await I.executeScript(() => {
        window.scrollTo({top:window.innerHeight,behavior:'instant'});
        const ui=window.pageBuilder.ui, top=ui.bar[0].getBoundingClientRect().bottom;
        window.pbScrollAnchor=window.pageBuilder.$wrapper.find('.pb-column:visible').get().find(element=>element.getBoundingClientRect().bottom>top);
        return window.pbScrollAnchor.getBoundingClientRect().top-top;
    });
    I.click('.pb-workbench [data-pb-action=insert]');
    await I.usePlaywrightTo('wait for expansion after scrolling without changing selection', async ({page}) => {
        const frame=await getPageBuilderFrame(page);
        await frame.waitForFunction(() => window.pageBuilder.ui.insertAnimations.every(animation=>animation.playState==='finished'));
    });
    const afterScroll=await I.executeScript(() => window.pbScrollAnchor.getBoundingClientRect().top-window.pageBuilder.ui.bar[0].getBoundingClientRect().bottom);
    assert.ok(Math.abs(afterScroll-scrolled)<2,'Offscreen gaps must preserve the visible content even when the selected column is elsewhere: '+scrolled+' -> '+afterScroll);
    I.pressKey('Escape');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench insertion cancellation collapses smoothly and restores focus', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    const before=await workbenchGeometry(I);
    for (const action of ['Escape','end-insert','insert']) {
        I.executeScript(() => window.scrollTo({top:0,behavior:'instant'}));
        I.click('.pb-workbench [data-pb-action=insert]');
        await I.usePlaywrightTo('sample the collapsing insertion gaps', async ({page}) => {
            const frame=await getPageBuilderFrame(page);
            await frame.waitForFunction(() => window.pageBuilder.ui.insertAnimations.every(animation=>animation.playState==='finished'));
            await frame.evaluate(() => {
                window.pbCollapseSamples=[];
                const point=window.pageBuilder.ui.insertPoints.find(point=>point.type==='section');
                function sample() {
                    const ui=window.pageBuilder.ui;
                    window.pbCollapseSamples.push({height:point.space[0].getBoundingClientRect().height,scroll:window.scrollY});
                    if (ui.inserting) requestAnimationFrame(sample);
                }
                requestAnimationFrame(sample);
            });
            if (action==='Escape') await page.keyboard.press('Escape');
            else await frame.locator('.pb-workbench [data-pb-action='+action+']').click();
            await frame.waitForFunction(() => !window.pageBuilder.ui.inserting);
        });
        const state=await I.executeScript(() => ({samples:window.pbCollapseSamples,focused:document.activeElement.matches('[data-pb-action=insert]'),animations:window.pageBuilder.ui.insertAnimations.length}));
        assert.ok(state.samples.some(sample=>sample.height>0 && sample.height<48),'Cancellation must animate the gap instead of removing it immediately');
        assert.ok(state.samples.every(sample=>sample.scroll<=1),'Collapsing at the top must not scroll the page');
        assert.equal(state.focused,true,'Focus must return to the rebuilt insertion toggle');
        assert.equal(state.animations,0,'Completed collapse animations must be cleaned up');
        I.dontSeeElement('.pb-insert-space');
        assert.deepStrictEqual(await workbenchGeometry(I),before,'Cancellation must restore the authored geometry');
    }
    await I.usePlaywrightTo('cancel while the insertion gaps are still opening', async ({page}) => {
        const frame=await getPageBuilderFrame(page);
        await frame.locator('.pb-workbench [data-pb-action=insert]').click();
        await page.keyboard.press('Escape');
        await page.keyboard.press('Escape');
        await frame.waitForFunction(() => !window.pageBuilder.ui.inserting && !document.querySelector('.pb-insert-space'));
    });
    assert.deepStrictEqual(await workbenchGeometry(I),before,'Interrupted expansion must also restore the authored geometry');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench insertion destinations, cancellation and clean geometry', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    const before = await workbenchGeometry(I);
    const html = await I.executeScript(() => window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data);
    I.click('.pb-workbench [data-pb-action=insert]');
    I.waitForVisible('.pb-insert-hint', 10);
    I.dontSeeElement('.pb-outline:not([hidden])');
    const state = await I.executeScript(() => {
        const pb = window.pageBuilder, points = pb.ui.insertPoints;
        return {
            sections: points.filter(point => point.type === 'section').length,
            expectedSections: pb.$wrapper.children('.pb-section:visible').length+1,
            containers: points.filter(point => point.type === 'container' && point.parent.matches('section.pb-workbench-autotest')).length,
            columns: points.filter(point => point.type === 'column' && point.parent.closest('section.pb-workbench-autotest')).length,
            editorSpacers: document.querySelectorAll('[data-ckeditor-instance] .pb-insert-space').length,
            html: window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data
        };
    });
    assert.equal(state.sections, state.expectedSections, 'Every section boundary must have exactly one destination');
    assert.equal(state.containers, 2, 'A single container must have two destinations');
    assert.equal(state.columns, 3, 'Two columns must have three destinations');
    assert.equal(state.editorSpacers, 0, 'Insertion helpers must stay outside CKEditor regions');
    assert.equal(state.html, html, 'Saving in insertion mode must not persist any helpers or labels');
    const during = await workbenchGeometry(I);
    assert.deepStrictEqual(during.map(item => [item.left,item.width,item.height,item.lines]), before.map(item => [item.left,item.width,item.height,item.lines]), 'Insertion mode must preserve column widths and text wrapping');
    await chooseWorkbenchInsertion(I, 'column', workbenchFixture+' .row', 1);
    I.seeElement('.pb-insert-context');
    I.pressKey('Escape');
    I.waitForInvisible('.pb-library', 10);
    I.seeElement('.pb-insert-layer:not([hidden])');
    const returned = await I.executeScript(() => document.activeElement.matches('[data-autotest-insert]'));
    assert.equal(returned, true, 'Cancelling the library must return focus to the chosen destination');
    I.pressKey('Escape');
    I.waitForInvisible('.pb-insert-layer', 10);
    I.dontSeeElement('.pb-insert-space');
    assert.deepStrictEqual(await workbenchGeometry(I), before, 'Leaving insertion mode must restore the original geometry');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench inserts sections containers and columns through the library', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    for (const target of [
        {type:'column', parent:workbenchFixture+' .row', index:1},
        {type:'container', parent:workbenchFixture, index:0},
        {type:'section', parent:'#wjInline-docdata', index:1}
    ]) {
        I.click('.pb-workbench [data-pb-action=insert]');
        await chooseWorkbenchInsertion(I, target.type, target.parent, target.index);
        const position = await I.executeScript(() => {
            const point = window.pageBuilder.ui.insertPending;
            window.pbAutotestInsertPoint = point;
            return Array.from(point.parent.children).filter(node=>node.matches('.pb-'+point.type)).length;
        });
        I.click('.pb-library .library-tab-link[data-library-type=basic]');
        I.click(locate('.pb-library .library-tab-item--basic .library-template-block--'+target.type+' .library-tab-item-button').first());
        I.waitForInvisible('.pb-library', 10);
        I.waitForInvisible('.pb-insert-layer', 10);
        await I.usePlaywrightTo('wait for the inserted block editor and shared toolbar', async ({page}) => {
            const frame = await getPageBuilderFrame(page);
            await frame.waitForFunction(() => {
                const selected=window.pageBuilder.ui.selected;
                const field=selected?.querySelector('[data-ckeditor-instance]');
                const editor=field && CKEDITOR.instances[field.dataset.ckeditorInstance];
                return editor?.status==='ready' && editor.focusManager.hasFocus && document.querySelector('#wjInlineCkEditorToolbarOffsetElement').getBoundingClientRect().height>0;
            });
        });
        const result = await I.executeScript(() => {
            const pb=window.pageBuilder, point=window.pbAutotestInsertPoint;
            const siblings=Array.from(point.parent.children).filter(node=>node.matches('.pb-'+point.type));
            const selected=pb.ui.selected;
            const at=siblings.indexOf(selected);
            return {count:siblings.length, correctPosition:point.next ? siblings[at+1]===point.next : siblings[at-1]===point.previous,
                html:window.getSaveData().editable.find(item=>item.wjAppField==='doc_data').data};
        });
        assert.equal(result.count, position+1, 'The library must insert exactly one block of the requested type');
        assert.equal(result.correctPosition, true, 'The new block must appear at the chosen sibling boundary');
        assert.ok(!/pb-insert-(space|point|context)|pb-is-inserting/.test(result.html), 'Inserted HTML must contain no transient UI');
    }
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench insertion in narrow gutters and wrapped columns', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    await I.executeScript(() => {
        document.querySelectorAll('section.pb-workbench-autotest .pb-column').forEach(column => column.style.padding='0');
    });
    I.click('.pb-workbench [data-pb-action=insert]');
    const gutter = await I.executeScript(() => {
        const point=window.pageBuilder.ui.insertPoints.find(point=>point.type==='column' && point.next && point.previous && point.parent.closest('section.pb-workbench-autotest'));
        return {hasLane:!!point.header, top:point.button[0].getBoundingClientRect().bottom, contentTop:point.next.getBoundingClientRect().top};
    });
    assert.equal(gutter.hasLane,true,'A narrow gutter must get an insertion lane above its content');
    assert.ok(gutter.top<=gutter.contentTop,'The insertion button must not overlap column text');
    I.executeScript(() => window.scrollTo(0,0));
    await I.usePlaywrightTo('capture desktop insertion destinations', async ({page}) => {
        const frame=await getPageBuilderFrame(page);
        await frame.waitForFunction(() => {
            const pb=window.pageBuilder;
            const first=pb.ui.insertPoints.find(point=>point.type==='section');
            return first.button[0].getBoundingClientRect().top>=pb.ui.bar[0].getBoundingClientRect().bottom;
        });
        await page.locator('#DTE_Field_data-pageBuilderIframe').screenshot({path:'../../../build/test/pagebuilder-insertion-desktop.png'});
    });
    const edges = await I.executeScript(() => window.pageBuilder.ui.insertPoints
        .filter(point=>point.type==='column' && point.parent.closest('section.pb-workbench-autotest') && (!point.next || !point.previous))
        .every(point=>point.next ? point.button[0].getBoundingClientRect().right<=point.next.getBoundingClientRect().left :
            point.button[0].getBoundingClientRect().left>=point.previous.getBoundingClientRect().right));
    assert.equal(edges,true,'Edge destinations must not cover text in columns without padding');
    I.switchTo();
    I.executeScript(() => window.pbSetWindowSize('phone'));
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    await I.usePlaywrightTo('wait for wrapped insertion destinations', async ({page}) => {
        const frame=await getPageBuilderFrame(page);
        await frame.waitForFunction(() => window.innerWidth<768 && window.pageBuilder.ui.insertPoints.some(point=>
            point.type==='column' && point.next && point.previous && point.parent.closest('section.pb-workbench-autotest') && point.space));
    });
    const wrapped = await I.executeScript(() => {
        const points=window.pageBuilder.ui.insertPoints.filter(point=>point.type==='column' && point.parent.closest('section.pb-workbench-autotest'));
        const middle=points.find(point=>point.next&&point.previous);
        const rect=middle.button[0].getBoundingClientRect();
        return {count:points.length, before:middle.previous.getBoundingClientRect().bottom, after:middle.next.getBoundingClientRect().top, top:rect.top,bottom:rect.bottom};
    });
    assert.equal(wrapped.count,3,'Wrapping must not duplicate sibling boundaries');
    assert.ok(wrapped.top>=wrapped.before && wrapped.bottom<=wrapped.after,'A wrapped boundary must sit in the gap between the columns');
    await I.usePlaywrightTo('capture mobile insertion destinations', async ({page}) => {
        await page.locator('#DTE_Field_data-pageBuilderIframe').screenshot({path:'../../../build/test/pagebuilder-insertion-mobile.png'});
    });
    await chooseWorkbenchInsertion(I,'column',workbenchFixture+' .row',1);
    I.click('.pb-library__footer__button');
    I.waitForInvisible('.pb-library',10);
    I.click(workbenchFixture+' .pb-workbench-copy');
    I.waitForInvisible('.pb-insert-layer',10);
    I.dontSeeElement('.pb-insert-space');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench outline modes, offsets and remembered preference', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    const button = '.pb-workbench [data-pb-action=guides]';
    const before = await workbenchGeometry(I);
    const initial = await I.executeScript(() => ({
        icon: document.querySelector('[data-pb-action=guides] path').getAttribute('d'),
        html: window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data
    }));
    I.seeElement(button+'[data-pb-guides=selected]');
    I.click('.pb-workbench [data-pb-action=structure]');
    I.click(button);
    I.seeElement(button+'[data-pb-guides=hidden]');
    I.waitForInvisible('.pb-outline:not([hidden])', 10);
    I.seeElement('.pb-structure');
    I.seeElement('.pb-workbench [data-pb-action=resize]');
    const hiddenIcon = await I.grabAttributeFrom(button+' path', 'd');
    assert.notStrictEqual(hiddenIcon, initial.icon, 'Hidden outlines must have their own crossed-out eye icon');
    I.click(button);
    I.seeElement(button+'[data-pb-guides=all]');
    I.waitForVisible('.pb-outline[data-type=section]:not([hidden])', 10);
    const all = await I.executeScript(() => ({
        icon: document.querySelector('[data-pb-action=guides] path').getAttribute('d'),
        stored: localStorage.getItem('webjet.pagebuilder.guides'),
        outlines: Array.from(document.querySelectorAll('.pb-outline:not([hidden])')).map(node => {
            const rect = node.getBoundingClientRect();
            return {type: node.dataset.type, left: rect.left, top: rect.top, right: rect.right, bottom: rect.bottom,
                color: getComputedStyle(node).borderColor, pointerEvents: getComputedStyle(node).pointerEvents};
        }),
        html: window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data
    }));
    assert.deepStrictEqual(all.outlines.map(outline => outline.type), ['column', 'row', 'container', 'section']);
    assert.strictEqual(new Set(all.outlines.map(outline => outline.color)).size, 4, 'Each structural level must keep its distinct color');
    assert.ok(all.outlines.every(outline => outline.pointerEvents === 'none'), 'All outlines must let clicks reach the content');
    for (let index = 1; index < all.outlines.length; index++) {
        const inner = all.outlines[index - 1], outer = all.outlines[index];
        assert.ok(outer.left <= inner.left - 4 && outer.top <= inner.top - 4 &&
            outer.right >= inner.right + 4 && outer.bottom >= inner.bottom + 4,
        'Ancestor borders must be separated even where the underlying block edges coincide');
    }
    assert.notStrictEqual(all.icon, initial.icon, 'The hierarchy mode must have a distinct layers icon');
    assert.notStrictEqual(all.icon, hiddenIcon);
    assert.strictEqual(all.stored, 'all');
    assert.strictEqual(all.html, initial.html, 'Outline preferences and helper elements must never enter saved HTML');
    assert.deepStrictEqual(await workbenchGeometry(I), before, 'All outline modes must preserve box sizes and text wrapping');
    I.click('.pb-structure [data-pb-action=close-structure]');
    I.saveScreenshot('pagebuilder-outline-hierarchy.png');
    I.click(workbenchFixture+' > .container > p.pb-editable');
    I.waitForVisible('.pb-outline[data-type=text]:not([hidden])', 10);
    I.seeNumberOfVisibleElements('.pb-outline', 3);
    I.click(button);
    I.seeElement(button+'[data-pb-guides=selected]');
    I.seeNumberOfVisibleElements('.pb-outline', 1);
    I.click(button);
    I.seeElement(button+'[data-pb-guides=hidden]');
    I.click(button);
    I.switchTo();
    I.amAcceptingPopups();
    DTE.cancel();
    I.amOnPage('/admin/v9/webpages/web-pages-list/?docid=57');
    DTE.waitForEditor();
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    I.waitForVisible(button+'[data-pb-guides=all]', 20);

    const fallback = await I.executeScript(() => {
        const builder = window.pageBuilder;
        localStorage.setItem('webjet.pagebuilder.guides', 'invalid-autotest');
        builder.destroy_workbench();
        builder.create_workbench();
        const invalid = builder.ui.guideMode;
        const descriptor = Object.getOwnPropertyDescriptor(window, 'localStorage');
        try {
            Object.defineProperty(window, 'localStorage', {configurable: true, get() { throw new Error('autotest blocked storage'); }});
            builder.destroy_workbench();
            builder.create_workbench();
            builder.workbench_action('guides');
            return {invalid, blocked: builder.ui.guideMode};
        } finally {
            Object.defineProperty(window, 'localStorage', descriptor);
            localStorage.removeItem('webjet.pagebuilder.guides');
        }
    });
    assert.deepStrictEqual(fallback, {invalid: 'selected', blocked: 'hidden'}, 'Invalid or unavailable storage must not prevent outline switching');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench quick actions, source markers and clean serialization', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    I.click(locate(workbenchFixture+' li.pb-duplicable-element').first());
    I.waitForVisible('.pb-outline[data-type=item]', 10);
    I.dontSeeElement('.pb-workbench [data-pb-action=style]');
    I.dontSeeElement('.pb-workbench [data-pb-action=resize]');
    I.click('.pb-workbench [data-pb-action=duplicate-adjacent]');
    I.waitForElement(workbenchFixture+' li.pb-duplicable-element:nth-child(3)', 10);
    let texts = await I.executeScript((root, selector) => Array.from(document.querySelectorAll(selector+' li')).map(element => Array.from(element.childNodes).filter(node => node.nodeType === Node.TEXT_NODE).map(node => node.textContent.trim()).join('')), workbenchFixture);
    assert.deepStrictEqual(texts, ['First autotest item', 'First autotest item', 'Second autotest item']);
    I.dontSeeElement('#wjInline-docdata.pb-is-moving-child');
    I.click('.pb-workbench [data-pb-action=more]');
    I.click('.pb-workbench [data-pb-action=next]');
    const moved = await I.executeScript((root, selector) => ({
        selectedLast: window.pageBuilder.ui.selected === document.querySelector(selector+' li:last-child'),
        html: window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data
    }), workbenchFixture);
    assert.strictEqual(moved.selectedLast, true, 'Quick move must select the moved clone');
    assert.ok(moved.html.includes('pb-duplicable'), 'Authored duplicable markers must survive serialization');
    assert.ok(moved.html.includes('overflow:hidden') || moved.html.includes('overflow: hidden'), 'Authored style must survive serialization');
    for (const token of ['pb-workbench-path', 'pb-structure', 'pb-outline-layer', 'pb-has-workbench', 'pb-hide-guides', 'data-pb-action', 'data-ckeditor-instance', 'pb-duplicable-element', 'pb-toolbar']) {
        assert.ok(!moved.html.includes(token), 'Serialized HTML must not contain runtime chrome: '+token);
    }
    I.click('.pb-workbench [data-pb-action=more]');
    I.seeElement('.pb-workbench [data-pb-action=next]:disabled');
    I.pressKey('Escape');
    I.click(workbenchFixture+' > .container > p.pb-editable');
    I.waitForVisible('.pb-outline[data-type=text]', 10);
    I.dontSeeElement('.pb-workbench [data-pb-action=duplicate-adjacent]');
    I.switchTo();
    I.amAcceptingPopups();
    DTE.cancel();
});

/** Waits for real editing focus, including asynchronously recreated inline editors. */
async function assertWorkbenchEditorFocused(I, selector) {
    await I.usePlaywrightTo('wait for CKEditor focus and its shared toolbar', async ({page}) => {
        const frame = await getPageBuilderFrame(page);
        await frame.waitForFunction(selector => {
            const element = document.querySelector(selector)?.closest('[data-ckeditor-instance]');
            const editor = element && CKEDITOR.instances[element.dataset.ckeditorInstance];
            return editor?.status === 'ready' && editor.focusManager.hasFocus &&
                element.contains(document.activeElement) &&
                document.querySelector('#wjInlineCkEditorToolbarElement').getBoundingClientRect().height > 50;
        }, selector, {timeout: 10000});
    });
}

Scenario('workbench keeps CKEditor toolbar after deleting the active column', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    I.executeScript((root, selector) => document.querySelector(selector).closest('[data-ckeditor-instance]').focus(), workbenchFixture+' .pb-workbench-copy');
    await assertWorkbenchEditorFocused(I, workbenchFixture+' .pb-workbench-copy');
    const initial = await I.executeScript((root, selector) => ({
        editor: document.querySelector(selector+' .pb-workbench-copy').closest('[data-ckeditor-instance]').dataset.ckeditorInstance,
        height: document.querySelector('#inlineEditorToolbarTop').getBoundingClientRect().height
    }), workbenchFixture);
    I.amAcceptingPopups();
    I.click('.pb-workbench [data-pb-action=more]');
    I.click('.pb-workbench [data-pb-action=remove]');
    I.acceptPopup();
    I.waitForDetached(workbenchFixture+' .pb-workbench-copy', 10);
    await assertWorkbenchEditorFocused(I, workbenchFixture+' .pb-column p');
    const state = await I.executeScript((root, initial) => ({
        destroyed: CKEDITOR.instances[initial.editor] === undefined,
        height: document.querySelector('#inlineEditorToolbarTop').getBoundingClientRect().height
    }), initial);
    assert.strictEqual(state.destroyed, true, 'Deleting a column must destroy its CKEditor instance');
    assert.strictEqual(state.height, initial.height, 'Deleting the active column must not collapse the toolbar header');
    I.type(' focused-autotest');
    I.see('focused-autotest', workbenchFixture+' .pb-column');

    // Delete a whole section at the end of the page to exercise the previous-editor fallback.
    const previousEditor = await I.executeScript((root, selector) => {
        const section = document.querySelector(selector);
        section.parentElement.append(section);
        return Array.from(document.querySelectorAll('#wjInline-docdata [data-ckeditor-instance]'))
            .filter(element => !section.contains(element)).pop().dataset.ckeditorInstance;
    }, workbenchFixture);
    I.click(workbenchFixture+' .pb-column p');
    I.click('.pb-workbench-path [data-type=section]');
    I.click('.pb-workbench [data-pb-action=more]');
    I.click('.pb-workbench [data-pb-action=remove]');
    I.acceptPopup();
    I.waitForDetached(workbenchFixture, 10);
    await assertWorkbenchEditorFocused(I, '[data-ckeditor-instance="'+previousEditor+'"]');
    I.switchTo();
    DTE.cancel();
});

Scenario('workbench keeps CKEditor toolbar when moving a column in both directions', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    const selector = workbenchFixture+' .pb-workbench-copy';
    I.executeScript((root, selector) => document.querySelector(selector).closest('[data-ckeditor-instance]').focus(), selector);
    await assertWorkbenchEditorFocused(I, selector);
    await I.executeScript(() => {
        const toolbar = document.querySelector('#wjInlineCkEditorToolbarElement');
        window.pbAutotestToolbarHeights = [];
        window.pbAutotestToolbarObserver = new ResizeObserver(() => {
            window.pbAutotestToolbarHeights.push(toolbar.getBoundingClientRect().height);
        });
        window.pbAutotestToolbarObserver.observe(toolbar);
    });
    for (const action of ['next', 'previous']) {
        const initial = await I.executeScript((root, selector) => ({
            editor: document.querySelector(selector).closest('[data-ckeditor-instance]').dataset.ckeditorInstance,
            height: document.querySelector('#inlineEditorToolbarTop').getBoundingClientRect().height
        }), selector);
        I.click('.pb-workbench [data-pb-action=more]');
        I.click('.pb-workbench [data-pb-action='+action+']');
        await assertWorkbenchEditorFocused(I, selector);
        const state = await I.executeScript((root, params) => ({
            destroyed: CKEDITOR.instances[params.initial.editor] === undefined,
            selected: window.pageBuilder.ui.selected === document.querySelector(params.selector).closest('.pb-column'),
            height: document.querySelector('#inlineEditorToolbarTop').getBoundingClientRect().height
        }), {selector, initial});
        assert.strictEqual(state.destroyed, true, 'Moving a column must dispose of the original CKEditor instance');
        assert.strictEqual(state.selected, true, 'Editor focus must retain the moved column selection');
        assert.strictEqual(state.height, initial.height, 'Moving a column must preserve the toolbar header height');
    }
    const heights = await I.executeScript(() => {
        window.pbAutotestToolbarObserver.disconnect();
        return window.pbAutotestToolbarHeights;
    });
    assert.ok(heights.length > 0 && heights.every(height => height > 50), 'The shared toolbar must remain visible while CKEditor instances are recreated');
    I.switchTo();
    I.amAcceptingPopups();
    DTE.cancel();
});

Scenario('workbench keyboard, responsive toolbar and lifecycle', async ({I, DTE, Document}) => {
    await openWorkbenchFixture(I, DTE, Document);
    I.click('.pb-workbench [data-pb-action=structure]');
    I.pressKey('Tab');
    I.pressKey('Home');
    I.pressKey('ArrowRight');
    I.pressKey('ArrowDown');
    I.pressKey('Enter');
    const treeFocus = await I.executeScript(() => ({ role: document.activeElement.getAttribute('role'), selected: document.activeElement.getAttribute('aria-selected') }));
    assert.deepStrictEqual(treeFocus, {role: 'treeitem', selected: 'true'}, 'Tree navigation must retain focus and select the requested block');
    I.pressKey('Escape');
    I.waitForInvisible('.pb-structure', 10);
    I.click(workbenchFixture+' .pb-workbench-copy');
    I.pressKey('End');
    I.type(' keyboard-autotest');
    I.waitForVisible('.pb-outline.is-quiet', 10);
    I.dontSeeElement('.pb-workbench [data-pb-action=style]');
    I.click('.pb-workbench [data-pb-action=more]');
    I.click('.pb-workbench [data-pb-action=style]');
    I.waitForVisible('.pb-modal', 10);
    I.pressKey('Escape');
    I.waitForInvisible('.pb-modal', 10);
    I.dontSeeElement('.pb-workbench-menu:not([hidden])');
    const editorText = await I.executeScript(() => window.getSaveData().editable.find(item => item.wjAppField === 'doc_data').data);
    assert.ok(editorText.includes('keyboard-autotest'), 'Opening and closing properties must preserve typed content');
    I.switchTo();
    I.executeScript(() => window.pbSetWindowSize('phone'));
    I.switchTo('#DTE_Field_data-pageBuilderIframe');
    await I.usePlaywrightTo('wait for responsive workbench sizing', async ({page}) => {
        const frame = await getPageBuilderFrame(page);
        await frame.waitForFunction(() => window.innerWidth <= 576);
    });
    I.click('.pb-workbench [data-pb-action=ancestors]');
    I.waitForVisible('.pb-workbench-path.is-expanded', 10);
    I.pressKey('Escape');
    I.dontSeeElement('.pb-workbench-path.is-expanded');
    const responsive = await I.executeScript(() => {
        const toolbar = document.querySelector('.pb-workbench').getBoundingClientRect();
        return {fits: toolbar.width <= window.innerWidth, controlsFit: Array.from(document.querySelectorAll('.pb-workbench-actions button')).every(button => button.getBoundingClientRect().right <= window.innerWidth)};
    });
    assert.deepStrictEqual(responsive, {fits: true, controlsFit: true}, 'The mobile toolbar must keep all primary actions reachable');
    const destroyed = await I.executeScript(() => {
        const builder = window.pageBuilder;
        builder.destroy_workbench();
        const remaining = document.querySelectorAll('.pb-workbench, .pb-outline-layer, .pb-structure').length;
        builder.create_workbench();
        return {remaining, count: document.querySelectorAll('.pb-workbench').length};
    });
    assert.deepStrictEqual(destroyed, {remaining: 0, count: 1}, 'Recreating chrome must not retain old UI or duplicate its toolbar');
    I.switchTo();
    I.amAcceptingPopups();
    DTE.cancel();
});

Scenario('reset PB settings', ({Document}) => {
    //reset PB settings
    Document.resetPageBuilderMode();
});

Scenario('bug - open window for file selection', ({ I, DTE ,Document}) => {
    Document.resetPageBuilderMode();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();

    const links = [
        { elementText: "Etiam orci", link: "/zo-sveta-financii/konsolidacia-napriec-trhmi.html" },
        { elementText: "archiv", link: "/zo-sveta-financii/trhy-su-nadalej-vydesene.html" }
    ];

    I.waitForElement("#DTE_Field_data-pageBuilderIframe");
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    links.forEach(({ elementText, link }) => {
         I.waitForElement(locate("h3").withText(elementText), 10);
         I.click(locate("h3").withText(elementText));
         I.pressKey("Enter");
         insertLink(I, link);
    });

    I.switchTo();
    I.clickCss('#datatableInit_modal button.btn.btn-warning.btn-preview');
    I.wait(2);
    I.switchToNextTab();
    links.forEach(({ link }) => {
         I.see(link);
         I.seeInSource(`href="${link}"`);
    });
    I.switchToPreviousTab();
    I.closeOtherTabs();
    I.switchTo();
});

Scenario('bug - /thumb prefix and parameters in image url', async ({ I, DTE, Document }) => {
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();

    var setData = { width: 300, height: 300 };
    await validateThumb(I, "Etiam orci", { width: 160, height: 160 }, setData);
    var setData2 = { width: 400, height: 300, ipMode: 3, bgColor: "ff0000" };
    await validateThumb(I, "Etiam orci", setData, setData2);
    var setData3 = { width: 500, height: 500, ipMode: 4, bgColor: "00ff00", noIp: true };
    await validateThumb(I, "Etiam orci", setData2, setData3);
    var setData4 = { width: 150, height: 0, ipMode: 1 };
    await validateThumb(I, "Etiam orci", setData3, setData4);
    await validateThumb(I, "Etiam orci", setData4, null);

    setData = { width: 200, height: 200 };
    await validateThumb(I, "intranetové riešenie", { width: 160, height: 160 }, setData);
    await validateThumb(I, "intranetové riešenie", setData, null);
});

async function validateThumb(I, elementText, checkData, setData = null) {
    I.say("---> validateThumb, element: "+elementText+", checkData: "+JSON.stringify(checkData)+", setData: "+JSON.stringify(setData));

    I.switchTo("#DTE_Field_data-pageBuilderIframe");

    var fixedSizeClass = "fixedSize-"+checkData.width+"-"+checkData.height;
    if (checkData.ipMode) fixedSizeClass += "-"+checkData.ipMode;
    else fixedSizeClass += "-5"; //default ip mode is 5
    if (checkData.bgColor) fixedSizeClass += "-"+checkData.bgColor;
    if (checkData.noIp) fixedSizeClass += "-true";

    var imgLocator = locate("div").withChild(locate("h3").withText(elementText)).find(locate("img."+fixedSizeClass));
    I.waitForElement(imgLocator, 10);
    I.click(imgLocator);
    I.switchTo(locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find("table.cke_dialog #wjImageIframeElement"));

    I.waitForElement('#txtUrl', 10);
    I.wait(3); //wait to populate URL between ckeditor / elfinder
    const url = await I.grabValueFrom("#txtUrl");

    //
    I.say('Checking if the URL contains "/thumb/" only once.');
    const regex = /^(?!.*\bthumb\b.*\bthumb\b).*thumb.*/;
    assert.match(url, regex, 'URL does not contain "/thumb/" only once');

    if (checkData.height === 0) checkData.height = ""; //if height is 0, it should not be present in URL, so we check empty value
    //
    I.say('Checking if the URL contains the correct parameters: w='+checkData.width+', h='+checkData.height+', ip=5.');
    assert.match(url, new RegExp('w=' + checkData.width + "&"));
    assert.match(url, new RegExp('h=' + checkData.height + "&"));
    assert.match(url, new RegExp('ip=' + (checkData.ipMode || 5)));

    //
    I.say('Checking if the parameters are not duplicated in the URL.');
    assert.doesNotMatch(url, new RegExp('w=' + checkData.width + '.*w=' + checkData.width), "Parameter 'w' is duplicated in the URL");
    assert.doesNotMatch(url, new RegExp('h=' + checkData.height + '.*h=' + checkData.height), "Parameter 'h' is duplicated in the URL");
    assert.doesNotMatch(url, new RegExp('ip=' + (checkData.ipMode || 5) + '.*ip=' + (checkData.ipMode || 5)), "Parameter 'ip' is duplicated in the URL");

    //
    I.switchTo();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");


    if (checkData.height === "") checkData.height = 0; //switch back to 0 for checking in field, if it was empty in URL

    //check values in thumbs dialog
    I.click(locate("a.cke_dialog_tab").withText("Miniatúra"));
    var widthLocator = locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find(".cke_dialog_ui_vbox.cke_dialog_page_contents").withText("Režim").find(".cke_dialog_ui_text").withText("Šírka").find("input.cke_dialog_ui_input_text");
    var heightLocator = locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find(".cke_dialog_ui_vbox.cke_dialog_page_contents").withText("Režim").find(".cke_dialog_ui_text").withText("Výška").find("input.cke_dialog_ui_input_text");
    var ipLocator = locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find(".cke_dialog_ui_vbox.cke_dialog_page_contents").withText("Režim").find(".cke_dialog_ui_select").withText("Režim").find("select.cke_dialog_ui_input_select");
    var colorLocator = locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find(".cke_dialog_ui_vbox.cke_dialog_page_contents").withText("Režim").find(".cke_dialog_ui_text").withText("Farba pozadia").find("input.cke_dialog_ui_input_text");
    var noIpLocator = locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find(".cke_dialog_ui_vbox.cke_dialog_page_contents").withText("Režim").find(".cke_dialog_ui_checkbox").withText("Vypnúť bod záujmu").find("input.cke_dialog_ui_checkbox_input");
    if (checkData.width) I.seeInField(widthLocator, checkData.width);
    if (checkData.height) I.seeInField(heightLocator, checkData.height);
    if (checkData.ipMode) I.seeInField(ipLocator, checkData.ipMode);
    if (checkData.bgColor) I.seeInField(colorLocator, checkData.bgColor);
    if (checkData.noIp === true) I.seeCheckboxIsChecked(noIpLocator);
    else  I.dontSeeCheckboxIsChecked(noIpLocator);

    if (setData !== null) {
        if (setData.width !== null) I.fillField(widthLocator, setData.width);
        if (setData.height !== null) I.fillField(heightLocator, setData.height);
        if (setData.ipMode) I.selectOption(ipLocator, setData.ipMode);
        if (setData.bgColor) I.fillField(colorLocator, setData.bgColor);
        if (setData.noIp === true) I.checkOption(noIpLocator);
        else I.uncheckOption(noIpLocator);
    }

    I.clickCss(".cke_dialog_ui_button_ok");

    await I.executeScript(() => {
        const element = document.querySelector('#cke_692_uiElement #wjImageIframeElement');
            if (element) {
              element.remove();
            }
    });

    I.switchTo();
}

function insertLink(I, link) {
    I.clickCss(".cke_button.cke_button__link.cke_button_off");
    I.switchTo(locate(".cke_dialog_container").withAttr({style: "display: flex; z-index: 10010;"}).find("table.cke_dialog #wjLinkIframe"));
    I.wait(1); //necessary static waiting
    I.waitForElement('#txtUrl', 10);
    I.fillField("#txtUrl", link);
    I.switchTo();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.clickCss('.cke_dialog_ui_button_ok');
    I.executeScript(() => {
    const element = document.querySelector('#cke_730_uiElement #wjLinkIframe');
        if (element) {
          element.remove();
        }
    });
}

Scenario('BUG: when you open PB doc and then empty NON PB it has PB content', ({I, DTE, Document}) => {

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=57");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.waitForElement("#DTE_Field_data-pageBuilderIframe");
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement(locate("h3").withText("Etiam orci"), 10);

    I.switchTo();
    DTE.cancel();

    I.clickCss("#datatableInit_wrapper .dt-buttons .buttons-create");
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);
    I.clickCss("#pills-dt-datatableInit-content-tab");

    I.dontSeeElement("#DTE_Field_data-pageBuilderIframe");
    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 10);

    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.dontSee("Etiam orci");
    I.switchTo();

    DTE.cancel();

});

function checkStyleModal(docId, colSelector, isCustom, I, DTE, Apps) {

    //to force codemirror render all items
    I.resizeWindow(1280, 1800);

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
    I.closeOtherTabs();
    DTE.waitForEditor();
    DTE.waitForCkeditor();

    openStyleModal(I, colSelector);

    var color = "0, 116, 217";
    var columnSelector = "column-content";
    //
    if (isCustom) {
        color = "255, 0, 0";
        columnSelector = "osk-content";
        I.say("Check CUSTOM color swatches");
        I.seeElement({css: '.pb-modal span.minicolors-swatch-color[style="background-color: rgb('+color+');"]'});
        I.dontSeeElement({css: '.pb-modal div.minicolors .minicolors-slider'});
        I.dontSeeElement({css: '.pb-modal div.minicolors .minicolors-grid'});
        I.clickCss('.pb-modal span.minicolors-swatch-color[style="background-color: rgb('+color+');"]');
    } else {
        I.say("Check DEFAULT color picker");
        I.seeElement({css: '.pb-modal div.minicolors .minicolors-slider'});
        I.seeElement({css: '.pb-modal div.minicolors .minicolors-grid'});
        I.clickCss('.pb-modal span.minicolors-swatch-color[style="background-color: rgb('+color+');"]');
        I.pressKey('Enter');
    }

    var textToCheck = "div."+columnSelector+"{background-color:rgba("+color+", 1);}";

    saveStyleModal(I);

    //
    I.say("Check HTML code for applied style");
    I.switchTo();
    I.clickCss('button.btn.btn-warning.btn-preview');
    I.switchToNextTab();
    I.seeInSource(textToCheck);
    I.switchToPreviousTab();
    I.closeOtherTabs();

    //
    I.say("Check mode switching");
    I.switchTo();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.selectOption({css: "div.exit-inline-editor select"}, "html");
    I.switchTo();

    I.seeElement(locate(".CodeMirror span.cm-qualifier").withText(columnSelector));
    I.seeElement(locate(".CodeMirror span.cm-property").withText("background-color"));

    //split colors and check values
    var colors = color.split(", ");
    colors.forEach(function(c, index){
        I.seeElement(locate(".CodeMirror span.cm-number").withText(c));
    });
}

Scenario("custom PB settings", ({I, DTE, Apps, Document}) => {
    Document.resetPageBuilderMode();

    checkStyleModal(150095, ".col-md-12", true, I, DTE, Apps);
    checkStyleModal(147174, ".col-md-3", false, I, DTE, Apps);
});

Scenario("filtering and tags", ({I, DTE, Apps, Document}) => {
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=152046");
    DTE.waitForEditor();
    I.wait(3);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.click(".pb-empty-placeholder-wrapper .pb-empty-placeholder__button");

    I.waitForElement(".library-template-block--section", 10);

    var baseHarmonika = locate(".library-tab-item-button__toggler").withText("Harmonika");;
    var baseKontakt = locate(".library-tab-item-button__toggler").withText("Kontakt");
    var subHarmonika = locate(".library-full-width-item").withText("Harmonika");;
    var subKontakt = locate(".library-full-width-item").withText("Kontaktný formulár");
    var subKontaktOSK = locate(".library-tab-item-button__toggler").withText("OSK-case3").find(".library-full-width-item").withText("form");

    //
    I.say("Check main items are present");
    I.seeElement(baseHarmonika);
    I.seeElement(baseKontakt);

    //
    I.say("Check main items are not opened");
    I.dontSeeElement(subHarmonika);
    I.dontSeeElement(subKontakt);

    //
    I.say("Open Harmonika");
    I.click(baseHarmonika);
    I.seeElement(subHarmonika);
    I.dontSeeElement(subKontakt);

    //
    I.say("Filter by tag 'form'");
    I.click(locate("label.library-tag-item-btn").withText("Formulár"));
    I.wait(1);
    I.dontSeeElement(baseHarmonika);
    I.seeElement(baseKontakt);
    I.seeElement(subKontakt);
    I.dontSeeElement(subHarmonika);

    //
    I.say("Clear filter");
    I.click(locate("label.library-tag-item-btn").withText("Formulár"));
    I.wait(1);
    I.seeElement(baseHarmonika);
    I.seeElement(baseKontakt);
    I.dontSeeElement(subHarmonika);
    I.dontSeeElement(subKontakt);

    //
    I.say("Search form");
    I.fillField(".library-filter-block input.library-filter-input", "form");
    I.dontSeeElement(baseHarmonika);
    I.seeElement(baseKontakt);
    I.seeElement(subKontakt);
    I.dontSeeElement(subHarmonika);

    //
    I.say("Search notfound something");
    I.fillField(".library-filter-block input.library-filter-input", "notfound");
    I.wait(1);
    I.dontSeeElement("div.library-tab-item-button__toggler");
    I.dontSeeElement("div.library-full-width-item");

    //
    I.say("Search form + tags");
    I.fillField(".library-filter-block input.library-filter-input", "form");
    I.click(locate("label.library-tag-item-btn").withText("Formulár"));
    I.wait(1);
    I.dontSeeElement(baseHarmonika);
    I.seeElement(baseKontakt);
    I.seeElement(subKontakt);
    I.seeElement(subKontaktOSK);
    I.dontSeeElement(subHarmonika);
    I.click(locate("label.library-tag-item-btn").withText("Kontakt"));
    I.dontSeeElement(baseHarmonika);
    I.dontSeeElement(baseKontakt);
    I.dontSeeElement(subKontakt);
    I.seeElement(subKontaktOSK);
    I.dontSeeElement(subHarmonika);

    //
    I.click(locate("label.library-tag-item-btn").withText("Kontakt")); //unclick kontakt tag
    I.fillField(".library-filter-block input.library-filter-input", "harmon");
    I.seeElement(baseHarmonika);
    I.dontSeeElement(baseKontakt);
    I.seeElement(subHarmonika);
    I.dontSeeElement(subKontakt);

    //
    I.say("Clear search");
    I.fillField(".library-filter-block input.library-filter-input", "");
    I.wait(1);
    I.seeElement(baseHarmonika);
    I.seeElement(baseKontakt);
    I.dontSeeElement(subHarmonika);
    I.dontSeeElement(subKontakt);

    //
    I.switchTo();
});

Scenario("insert blocks into page", ({I, DTE, Apps, Document}) => {
    Document.resetPageBuilderMode();

    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=152046");
    DTE.waitForEditor();
    I.wait(3);
    I.switchTo("#DTE_Field_data-pageBuilderIframe");

    I.click(".pb-empty-placeholder-wrapper .pb-empty-placeholder__button");
    I.waitForElement(".library-template-block--section", 10);

    //
    I.say("Inserting contact form block");
    I.click(locate("label.library-tag-item-btn").withText("Formulár"));
    I.click(locate(".library-full-width-item").withText("Kontaktný formulár"));

    I.waitForElement(locate("section.pb-section h2.text-center").withText("Contact us"), 10);
    //check data-pb-id attribute
    I.seeElement(locate('section[data-pb-id="c2VjdGlvbi9Db250YWN0L2NvbnRhY3RfMDY="]'));

    //
    I.say("Inserting standard section block");
    I.click(".pb-empty-placeholder-wrapper .pb-empty-placeholder__button");
    I.waitForElement(".library-template-block--section", 10);
    I.click(".pb-library--section .library-tab-link:nth-child(1)"); //first tab
    I.click('.library-template-block--section .library-tab-item-button[data-library-item-id="pb-basic-2.4"]');

    I.waitForElement(locate("section.pb-section div.col-2.pb-column").withText("Text"), 10);

    I.switchTo();
    I.resizeWindow(1280, 1200);

    I.switchTo("#DTE_Field_data-pageBuilderIframe");
    I.selectOption({css: "div.exit-inline-editor select"}, "html");
    I.switchTo();

    I.seeElement(locate(".CodeMirror-line").withText("!INCLUDE(/components/formsimple/form.jsp"));
    I.seeElement(locate(".CodeMirror-line").withText('Text'));
    I.seeElement(locate(".CodeMirror-line .cm-string").withText('col-2'));
    I.seeElement(locate(".CodeMirror-line .cm-string").withText('c2VjdGlvbi9Db250YWN0L2NvbnRhY3RfMDY'));

    I.wjSetDefaultWindowSize();
});

function checkNewPageTemplate(groupId, hasTemplate, I, DT, DTE) {
    I.switchTo();
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=" + groupId);
    DT.waitForLoader();

    I.click(DT.btn.add_button);
    DTE.waitForEditor();

    I.waitForElement("#pills-dt-datatableInit-basic-tab.active", 10);

    //verify title and navbar
    DTE.seeInField("navbar", "Nová web stránka");

    I.clickCss("#pills-dt-datatableInit-content-tab");
    DTE.waitForCkeditor();
    I.switchTo("#DTE_Field_data-pageBuilderIframe");

    if (hasTemplate === true) {
        I.waitForText("Toto je stlpec 1", 10, "div.column-content p");
        I.see("Toto je nadpis stránky", "div.column-content h1");
        I.dontSee("Nová web stránka", "div.column-content p");
    } else {
        I.waitForText("Nová web stránka", 10, "div.column-content p");
        I.dontSee("Toto je stlpec 1", "div.column-content");
        I.dontSee("Toto je nadpis stránky", "div.column-content");
    }
}

Scenario("NewPageDocIdTemplate is used for new page", ({I, DT, DTE}) => {

    //negative scenario - blank page
    checkNewPageTemplate(34495, false, I, DT, DTE);

    //positive scenario - template with 2 columns
    checkNewPageTemplate(112952, true, I, DT, DTE);

});
