Feature('webpages.template');

Before(({ login }) => {
    login('admin');
});

async function checkColor(title, expectedColor, I, DTE) {
    I.click(title);
    DTE.waitForEditor();
    I.see(title, "#datatableInit_modal .modal-title");
    DTE.waitForLoader();
    DTE.waitForCkeditor();

    const elementSelector = "section.prices div.card-header.text-white:nth-child(1)";

    I.waitForFunction(([selector, color]) => {
        const frame = document.querySelector("iframe.cke_wysiwyg_frame.cke_reset");
        const element = frame?.contentDocument?.querySelector(selector);
        return element != null && frame.contentWindow.getComputedStyle(element).backgroundColor === color;
    }, [elementSelector, expectedColor], 10);
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
    await checkColor("Produktová stránka - B verzia", "rgb(0, 163, 224)", I, DTE);
});
