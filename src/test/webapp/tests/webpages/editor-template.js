Feature('webpages.template');

Before(({ login }) => {
    login('admin');
});

async function checkColor(title, expectedColor, I, DTE) {
    I.click(title);
    DTE.waitForLoader();
    DTE.waitForCkeditor();

    const elementSelector = "section.prices div.card-header.text-white:nth-child(1)";

    I.switchTo("iframe.cke_wysiwyg_frame.cke_reset");
    let background = await I.grabCssPropertyFrom(elementSelector, 'background-color');
    I.switchTo();

    I.assertEqual(background, expectedColor);
    DTE.cancel();
}

Scenario('verify template CSS is loaded', async ({ I, DT, DTE }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=25");
    DT.waitForLoader();

    await checkColor("Produktová stránka - B verzia", "rgb(0, 163, 224)", I, DTE);
    await checkColor("Produktová stránka - yellow", "rgb(255, 170, 0)", I, DTE);
});