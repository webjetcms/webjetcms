Feature('admin.datatable-field-types');

Before(({ I, login }) => {
     login('admin');
});


/**
 * Test HTML view of quill editor, test UL and OL tags
 * Quill editor supports only UL and OL tags, so we need to test that
 * the HTML view is correct and that we can edit it.
 */
Scenario('type-quill-htmlview', async ({ I, DT, DTE }) => {

    let htmlCode = `<p>Specs:</p><ol><li>0-100 km/h in ~2.1 s</li><li>Up to 652 km range</li><li>Tri-motor all-wheel drive</li></ol><p>Features:</p><ul class="features"><li>Autopilot &amp; FSD Capability</li><li>17-inch Cinematic Touchscreen</li><li>Premium Interior</li></ul>`;

    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");

    I.click(locate("button").withChild(".ti-code"));

    I.waitForElement(".ql-html-textContainer", 5);
    I.fillField(".ql-html-textArea", htmlCode);
    I.clickCss("button.ql-html-buttonOk");

    DTE.save();

    //open again the editor and check the HTML view
    I.amOnPage("/apps/banner/admin/?id=7053");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");
    I.click(locate("button").withChild(".ti-code"));

    I.waitForElement(".ql-html-textContainer", 5);
    let htmlFromEditor = await I.grabValueFrom(".ql-html-textArea");

    //trim all spaces and new lines from the HTML code
    htmlFromEditor = htmlFromEditor.replace(/\s+/g, ' ').trim();
    htmlFromEditor = htmlFromEditor.replace(/> </g, '><');

    console.log("htmlEdit=", htmlFromEditor);
    console.log("htmlCode=", htmlCode);
    //check that the HTML view is correct
    I.assertEqual(htmlFromEditor, htmlCode, "HTML view of quill editor is not correct");

    //test it on webpage
    I.amOnPage("/apps/bannerovy-system/banner-system.html");
    I.seeInSource(htmlCode);
});