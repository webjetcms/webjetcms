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

Scenario('bug - prepnutie editora', ({I, DTE, Apps, Document}) => {
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
    I.see("Suspendisse interdum dolor justo, ac venenatis massa");

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
    I.say("Click on col toolbar");
    I.seeElementInDOM("section:nth-child(1) aside.pb-toolbar");
    I.forceClick({css: "section:nth-child(1) .container .row "+colSelector+":nth-child(1) aside.pb-toolbar"});
    I.seeElement("section:nth-child(1) .container .row "+colSelector+":nth-child(1) aside.pb-highlighter__top");

    //
    I.say("Open style modal");
    I.forceClick({css: "aside.pb-is-toolbar-active button.pb-toolbar-button__style"});
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
    I.forceClick(item.find("aside.pb-toolbar"));
    I.waitForVisible(item.find("aside.pb-toolbar.pb-is-toolbar-active button.pb-toolbar-button__"+action), 10);
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
        const button = item && item.querySelector("button.pb-toolbar-button__"+args.action);
        const editable = button && button.closest('[contenteditable="true"]');

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
    I.executeScript(() => window.pageBuilder.set_toolbar_invisible());
    I.forceClick(getDuplicableRowController(rowIndex, "aside.pb-toolbar"));
    I.waitForVisible(getDuplicableRowController(rowIndex, "aside.pb-toolbar.pb-is-toolbar-active button.pb-toolbar-button__"+action), 10);
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
    I.click(getDuplicableItem(1, 1).find("button.pb-toolbar-button__duplicate"));
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

    I.forceClick(getDuplicableItem(1, 4).find("button.pb-toolbar-button__move"));
    I.waitForElement("#wjInline-docdata.pb-is-moving-child.pb-is-moving-duplicable-element", 10);
    I.dontSeeElement("#wjInline-docdata.pb-is-duplicating");
    I.dontSeeElement(locate(pricingListsSelector).at(2).find("li.pb-is-duplicable-target"));
    I.forceClick(getDuplicableItem(1, 1).find("aside.pb-prepend"));
    assert.deepStrictEqual(await getDuplicableItemTexts(I, 1), ["vulputate purus", "Nunc sed purus", "rutrum varius sollicitudin", "Nunc sed purus"]);

    I.forceClick(getDuplicableItem(1, 2).find("button.pb-toolbar-button__move"));
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
    I.click(getDuplicableItem(1, 4).find("button.pb-toolbar-button__remove"));
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
    assert.strictEqual(initialState.sideHighlighterWidths.every(width => width >= 1.9 && width <= 2.1), true, "A duplicable row must render two-pixel side highlighters");
    assert.strictEqual(initialState.controllersHaveNoHorizontalPadding, true, "Page Builder controllers inside a Bootstrap row must not inherit row gutter padding");
    assert.strictEqual(initialState.toolbarHandlesOverlap, false, "The row and column toolbar handles must not overlap");
    assert.ok(initialState.toolbarHorizontalGap >= 7.9, "The row and column toolbar handles must have a visible gap");

    openDuplicableRowToolbar(I, 1, "duplicate");
    I.click(getDuplicableRowController(1, "aside.pb-toolbar button.pb-toolbar-button__duplicate"));
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
    I.click(getDuplicableRowController(2, "aside.pb-toolbar button.pb-toolbar-button__move"));
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
    I.click(getDuplicableRowController(2, "aside.pb-toolbar button.pb-toolbar-button__remove"));
    I.acceptPopup();
    await waitForDuplicableRowEditors(I, 1);
    let removedEditorsState = await I.executeScript((root, editorName) => CKEDITOR.instances[editorName] == null,
        movedState.rows[1].editorName);
    assert.strictEqual(removedEditorsState, true, "Deleting a row must destroy its CKEditor instance");

    openDuplicableRowToolbar(I, 1, "remove");
    I.click(getDuplicableRowController(1, "aside.pb-toolbar button.pb-toolbar-button__remove"));
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
