Feature('admin.datatable-field-types');

Before(({ I, login }) => {
     login('admin');
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
