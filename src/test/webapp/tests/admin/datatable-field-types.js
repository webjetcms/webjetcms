Feature('admin.datatable-field-types');

Before(({ I, login }) => {
     login('admin');
});

/** Verifies that toolbar menus remain clickable beyond a short modal body and in a short viewport. */
Scenario('type-quill-pickers-in-short-modal', async ({ I, DT, DTE }) => {
    I.amOnPage('/apps/form/admin/form-steps/?formName=Multistepform_1');
    DT.waitForLoader('formStepsDataTable');
    I.waitForElement('#formStepsDataTable tbody tr.selected', 10);
    DT.waitForLoader('formItemsDataTable');

    const field = '#formItemsDataTable_modal #DTE_Field_label';
    const heading = field + ' .ql-header';
    for (const [width, height, toolbarOffset] of [[1440, 900, 0], [1024, 768, 0], [600, 650, 0], [600, 400, 40], [600, 400, 80]]) {
        I.resizeWindow(width, height);
        I.clickCss('#formItemsDataTable_wrapper button.buttons-create');
        DTE.waitForEditor('formItemsDataTable');
        DTE.selectOption('fieldType', 'Popiska (info text)');
        DTE.fillQuill('label', 'Quill picker autotest');

        // Place the toolbar where neither side has room for the full menu in a short viewport.
        I.executeScript(({ selector, offset }) => {
            document.querySelector(selector).closest('.DTE_Field').style.marginTop = offset + 'px';
        }, { selector: field, offset: toolbarOffset });

        for (const level of [1, 2, 3, 4, 5, 6]) {
            I.clickCss(heading + ' .ql-picker-label');
            // Hit testing also catches the picker label covering a vertically shifted menu.
            I.waitForFunction((selector) => {
                const items = [...document.querySelectorAll(selector + ' .ql-picker-item')];
                return items.length === 7 && items.every(item => {
                    const rect = item.getBoundingClientRect();
                    return item.contains(document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2));
                });
            }, [heading], 10);
            I.clickCss(heading + ' .ql-picker-item[data-value="' + level + '"]');
            I.see('Quill picker autotest', field + ' .ql-editor h' + level);
        }

        I.clickCss(heading + ' .ql-picker-label');
        I.pressKey('Escape');
        I.dontSeeElement(heading + ' .ql-picker-options');
        I.pressKey('Enter');
        I.waitForVisible(heading + ' .ql-picker-options', 5);
        I.clickCss(heading + ' .ql-picker-item:not([data-value])');
        I.see('Quill picker autotest', field + ' .ql-editor p');

        I.clickCss(field + ' .ql-color .ql-picker-label');
        I.clickCss(field + ' .ql-color .ql-picker-item:last-child');
        I.dontSeeElement(field + ' .ql-color .ql-picker-options');
        I.executeScript(selector => {
            document.querySelector(selector).closest('.DTE_Field').style.removeProperty('margin-top');
        }, field);
        DTE.cancel('formItemsDataTable');
    }
    I.wjSetDefaultWindowSize();
});


/**
 * Verify that the HTML source preserves ordered and unordered lists,
 * inline formatting, and intentional empty paragraphs after saving.
 */
Scenario('type-quill-htmlview', async ({ I, DTE }) => {

    const htmlCode = `<p><strong>Specs:</strong></p><p></p><ol><li>0-100 km/h in ~2.1 s</li><li>Up to <strong>652</strong> km range</li><li>Tri-motor all-wheel drive</li></ol><p><em>Features:</em></p><ul class="features"><li>Autopilot &amp; FSD Capability</li><li>17-inch <span style="color:rgb( 161 , 0 , 0 )">Cinematic</span> Touchscreen</li><li>Premium Interior</li></ul>`;

    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");

    I.click(locate("button").withChild(".ti-code"));

    I.waitForElement(".ql-html-textContainer", 5);
    I.fillField(".ql-html-textArea", htmlCode);
    I.clickCss("button.ql-html-buttonOk");

    DTE.save();

    // Reopen the saved content and check the HTML source.
    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");

    I.click(locate("button").withChild(".ti-code"));

    I.waitForElement(".ql-html-textContainer", 5);
    let htmlFromEditor = await I.grabValueFrom(".ql-html-textArea");

    // Normalize the whitespace added by the source formatter.
    htmlFromEditor = htmlFromEditor.replace(/\s+/g, ' ').trim();
    htmlFromEditor = htmlFromEditor.replace(/> </g, '><');

    I.assertEqual(htmlFromEditor, htmlCode, "Lists, inline formatting, and the internal empty paragraph must survive saving.");

    // Verify the persisted HTML on the public page.
    I.amOnPage("/apps/bannerovy-system/banner-system.html");
    I.seeInSource(htmlCode);
});

/**
 * Verify that nested and sibling DIV wrappers keep their classes across source
 * editing and saving, while only trailing empty paragraphs are removed.
 */
Scenario('type-quill-htmlview-div-wrappers', async ({ I, DTE }) => {
    const htmlCode = `
        <div class="autotest-wrapper">
            <div class="autotest-first">
                <p>autotest First</p>
                <p><br></p>
                <p><strong>autotest Second</strong></p>
            </div>
            <div class="autotest-second">
                autotest Direct <em>inline</em>
                <p><br></p>
            </div>
            <p><br></p>
        </div>
        <p><br></p>
    `;
    const expectedHtml = '<div class="autotest-wrapper"><div class="autotest-first"><p>autotest First</p><p></p><p><strong>autotest Second</strong></p></div><div class="autotest-second"><p>autotest Direct <em>inline</em></p></div></div>';

    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");
    I.click(locate("button").withChild(".ti-code"));
    I.waitForElement(".ql-html-textContainer", 5);
    I.fillField(".ql-html-textArea", htmlCode);
    I.clickCss("button.ql-html-buttonOk");
    DTE.save();

    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");
    I.click(locate("button").withChild(".ti-code"));
    I.waitForElement(".ql-html-textContainer", 5);
    let htmlFromEditor = await I.grabValueFrom(".ql-html-textArea");
    htmlFromEditor = htmlFromEditor.replace(/\s+/g, ' ').trim().replace(/> </g, '><');
    I.assertEqual(htmlFromEditor, expectedHtml, "DIV nesting, sibling classes, and the internal empty paragraph must survive saving without trailing empty paragraphs.");

    // Confirming the source again must preserve the same stored structure.
    I.clickCss("button.ql-html-buttonOk");
    DTE.save();
    I.amOnPage("/apps/bannerovy-system/banner-system.html");
    I.seeInSource(expectedHtml);
});
