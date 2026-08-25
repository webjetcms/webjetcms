Feature('a11y.layout');

Before(({ I, login }) => {
    login('admin');
});

Scenario('p35: headings', async ({ I, a11y }) => {
    I.amOnPage('/apps/stat/admin/');
    I.wait(1);
    //TODO: div.header-title should be H1
    await a11y.check();
});

Scenario('p37: ly-content-wrapper section should be in main tag', async ({ I, a11y }) => {
    I.amOnPage('/apps/stat/admin/');

    const mainContent = "div.ly-content main#main-content.ly-container.container";
    const breadcrumb = `${mainContent} > div.md-breadcrumb`;

    I.waitForElement(mainContent, 5);
    I.waitForElement(breadcrumb, 5);
    I.assertEqual(await I.grabNumberOfVisibleElements("main#main-content"), 1,
        "The page must contain exactly one visible main landmark");
    I.assertAbove(await I.grabNumberOfVisibleElements(`${breadcrumb} .nav-item`), 0,
        "WJ.breadcrumb must render navigation inside the main landmark");
    await a11y.check();
});

Scenario('p39: contrast between default vs hover vs focus', async ({ I, a11y }) => {
    I.amOnPage('/admin/v9/settings/redirect/');
    //TODO: check contrast between default vs hover vs focus on buttons and links
    await a11y.check();
});

Scenario("p53: missing skip to content", async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    const skipLink = "a.skip-link";
    const mainContent = "main#main-content.ly-container.container";
    const mainContentHash = "#main-content";

    I.waitForElement(skipLink, 5);
    const initialState = await I.executeScript(({skipLink, mainContent}) => {
        const link = document.querySelector(skipLink);
        const target = document.querySelector(mainContent);
        const rect = link.getBoundingClientRect();
        return {
            activeTag: document.activeElement?.tagName,
            href: link.getAttribute("href"),
            linkText: link.textContent.trim(),
            linkBottom: rect.bottom,
            targetTabIndex: target.getAttribute("tabindex"),
            targetTag: target.tagName
        };
    }, {skipLink, mainContent});

    I.assertEqual(initialState.activeTag, "BODY", "Focus must start before the first interactive element");
    I.assertEqual(initialState.href, mainContentHash, "The skip link must reference the main content");
    I.assertTrue(initialState.linkText.length > 0, "The skip link must have an accessible name");
    I.assertTrue(initialState.linkBottom <= 0, "The skip link must be hidden above the viewport by default");
    I.assertEqual(initialState.targetTag, "MAIN", "The skip link target must be the main landmark");
    I.assertEqual(initialState.targetTabIndex, "-1", "The main content must accept programmatic focus");

    I.pressKey("Tab");
    I.waitForElement(`${skipLink}:focus`, 5);
    I.waitForFunction(() => document.querySelector("a.skip-link").getBoundingClientRect().top >= 0, 5);

    const focusedState = await I.executeScript(skipLink => {
        const link = document.querySelector(skipLink);
        const rect = link.getBoundingClientRect();
        return {
            hasFocus: document.activeElement === link,
            isInViewport: rect.top >= 0
                && rect.left >= 0
                && rect.bottom <= window.innerHeight
                && rect.right <= window.innerWidth,
            hasSize: rect.width > 0 && rect.height > 0
        };
    }, skipLink);

    I.assertTrue(focusedState.hasFocus, "The skip link must be the first keyboard focus target");
    I.assertTrue(focusedState.isInViewport, "The focused skip link must be fully visible in the viewport");
    I.assertTrue(focusedState.hasSize, "The focused skip link must have a visible hit area");
    await a11y.check();

    I.pressKey("Enter");
    I.waitForElement(`${mainContent}:focus`, 5);
    I.seeInCurrentUrl(mainContentHash);
});
