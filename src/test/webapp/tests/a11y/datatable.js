Feature('a11y.datatable');

Before(({ I, login }) => {
    login('admin');
});

Scenario('basic datatable', async ({ I, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    //mark first row as selected to reveal buttons
    I.forceClick(".dt-scroll-body tbody tr:nth-child(1) td.dt-select-td");
    I.waitForElement(".dt-buttons button.btn-danger:not(.disabled)", 5);

    await a11y.check();
    I.amOnPage("/apps/news/admin/");
    await a11y.check();
    I.amOnPage("/admin/v9/users/user-list/");
    await a11y.check();
    I.amOnPage("/apps/basket/admin/");
    await a11y.check();
});

Scenario('filter', async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.filterContains("tempName", "page");
    await a11y.check();
});

Scenario('editor - with error messages', async ({ I, DT, DTE, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.add_button);
    DTE.waitForEditor();
    //save empty form to trigger error messages
    DTE.save();
    I.pressKey("Escape");
    await a11y.check();
});

Scenario("toggle buttons", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    await a11y.check();
});

Scenario("export modal", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.export_button);
    I.waitForElement("#datatableExportModal", 5);
    await a11y.check();
});

Scenario("import modal", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    I.click(DT.btn.import_button);
    I.waitForElement("#datatableImportModal", 5);
    await a11y.check();
});

Scenario("p27: input with buttons", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/banner/admin/?id=2");
    DTE.waitForEditor("bannerDataTable");
    I.clickCss("#pills-dt-bannerDataTable-advanced-tab");
    await a11y.check();
});

Scenario("p27: input with buttons-top-filter", async ({ I, a11y }) => {
    I.amOnPage("/apps/contact/admin/");
    await a11y.check();

    I.amOnPage("/admin/v9/search/index/");
    I.wait(1); //wait for datatable to initialize
    await a11y.check();

    I.amOnPage("/apps/form/admin/detail/?formName=Form-with-redirection-to-Spring-App");
    await a11y.check();
});

Scenario("p28: select", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/enumeration/admin/enumeration-type/?id=2");
    DTE.waitForEditor("enumerationTypeDataTable");
    await a11y.check();
});

Scenario("p28: select-top-filter", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/news/admin/");
    await a11y.check();
});

Scenario("p29: checkbox-access, p30: fieldset/legend", async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    DTE.waitForCkeditor();
    I.clickCss("#pills-dt-datatableInit-access-tab")
    await a11y.check();
    //TODO: fieldset/legend
});

Scenario("p29: checkbox-main", async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=92");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-basic-tab")
    await a11y.check();
});

Scenario("p31: wysiwyg field", async ({ I, DTE, a11y }) => {
    I.amOnPage("/apps/inquiry/admin/?id=1");
    DTE.waitForEditor("inquiryDataTable");
    await a11y.check();
    //TODO: tab inside wysiwyg field - it's not possible to get out
});

Scenario("p34: help text", async ({ I, DTE, Apps, a11y }) => {
    Apps.openAppEditor(77667);
    //I.switchTo('#editorComponent');
    I.waitForText("Povoliť viacero odpovedí", 10, ".col-form-label");
    await a11y.check("iframe.cke_dialog_ui_iframe #editorComponent");
});

Scenario("p40: is-not-public contrast", async ({ I, DTE, Apps, a11y }) => {
    I.amOnPage("/apps/forum/admin/");
    await a11y.check();
});

Scenario("p41: column visibility settings", async ({ I, DTE, Apps, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");

    var tableId = "datatableInit";
    var container = "#"+tableId+"_wrapper";
    I.clickCss(container+" button.buttons-settings");
    I.clickCss(container+" button.buttons-colvis");
    I.waitForVisible("div.dt-button-collection ul[role=menu] div.dt-button-collection ul[role=menu]");

    await a11y.check("ul.dropdown-menu.show div.dt-button-collection ul.dropdown-menu.show");
});

Scenario('p42: Overenie zobrazenia tooltipov', async ({ I, DTE, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/?tempId=1");
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-advanced-tab");

    const titleTooltipButton = "div.DTE_Field_Name_moveStyleToHead button.btn-tooltip";
    const tooltipLabel = await I.grabAttributeFrom(titleTooltipButton, "aria-label");
    I.assertTrue(tooltipLabel.length > 0, "Tooltip button must have an accessible name");

    I.executeScript((selector) => document.querySelector(selector).focus(), titleTooltipButton);
    I.waitForVisible("div.tooltip.show", 5);
    const tooltipId = await I.grabAttributeFrom(titleTooltipButton, "aria-describedby");
    I.assertTrue(tooltipId.length > 0, "Tooltip button must reference the visible tooltip");

    await a11y.check("#datatableInit_modal");

    I.pressKey("Escape");
    I.waitForInvisible("div.tooltip.show", 5);
    I.seeElement("#datatableInit_modal");
});

Scenario("p43: contrast", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?groupid=269");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    await a11y.check("#SomStromcek");
});

Scenario("p43: p44: button contrast @current", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    DT.waitForLoader();
    I.jstreeWaitForLoader();
    I.click("#pills-trash-tab");
    I.waitForElement("#pills-trash-tab.active", 5);
    DT.waitForLoader();

    I.forceClick("#datatableInit tbody tr:first-child td.dt-select-td");
    I.waitForElement("#datatableInit_wrapper div.dt-buttons button.buttons-edit:not(:disabled)", 5);

    const toolbars = await I.executeScript(() => {
        const isVisible = element => {
            const style = getComputedStyle(element);
            return style.display !== 'none' && style.visibility !== 'hidden' && element.getClientRects().length > 0;
        };
        const isDisabled = element => element.matches(':disabled') || element.getAttribute('aria-disabled') === 'true';
        const focusableSelector = 'button, input, select, textarea, a[href], [tabindex]';

        const transitionOverride = document.createElement('style');
        transitionOverride.textContent = `
            div.dt-buttons button { transition: none !important; }
            .tooltip { pointer-events: none !important; }
        `;
        document.head.appendChild(transitionOverride);

        window.__wjA11yGetButtonState = selector => {
            const element = document.querySelector(selector);
            const style = getComputedStyle(element);

            const parseColor = value => {
                const channels = value.match(/[\d.]+/g)?.map(Number) || [0, 0, 0, 0];
                return {
                    red: channels[0],
                    green: channels[1],
                    blue: channels[2],
                    alpha: channels.length > 3 ? channels[3] : 1
                };
            };
            const composite = (foreground, background) => ({
                red: foreground.red * foreground.alpha + background.red * (1 - foreground.alpha),
                green: foreground.green * foreground.alpha + background.green * (1 - foreground.alpha),
                blue: foreground.blue * foreground.alpha + background.blue * (1 - foreground.alpha),
                alpha: 1
            });
            const luminance = color => {
                const channels = [color.red, color.green, color.blue].map(channel => {
                    const value = channel / 255;
                    return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
                });
                return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
            };
            const contrast = (first, second) => {
                const firstLuminance = luminance(first);
                const secondLuminance = luminance(second);
                return (Math.max(firstLuminance, secondLuminance) + 0.05) /
                    (Math.min(firstLuminance, secondLuminance) + 0.05);
            };

            let ancestor = element.parentElement;
            let surrounding = parseColor('rgba(0, 0, 0, 0)');
            while (ancestor && surrounding.alpha === 0) {
                surrounding = parseColor(getComputedStyle(ancestor).backgroundColor);
                ancestor = ancestor.parentElement;
            }
            if (surrounding.alpha === 0) surrounding = parseColor('rgb(255, 255, 255)');

            const rawBackground = parseColor(style.backgroundColor);
            const background = rawBackground.alpha < 1 ? composite(rawBackground, surrounding) : rawBackground;
            const foreground = parseColor(style.color);
            const border = parseColor(style.borderTopColor);
            const outline = parseColor(style.outlineColor);
            const outlineButton = element.classList.contains('btn-outline-secondary');

            return {
                name: element.getAttribute('aria-label') || element.getAttribute('title') || element.innerText.trim(),
                foregroundContrast: contrast(foreground, background),
                componentContrast: outlineButton ?
                    Math.max(contrast(border, surrounding), contrast(foreground, surrounding)) :
                    Math.max(contrast(background, surrounding), contrast(border, surrounding)),
                outlineContrast: contrast(outline, surrounding),
                outlineStyle: style.outlineStyle,
                outlineWidth: parseFloat(style.outlineWidth)
            };
        };

        return [...document.querySelectorAll('div.dt-buttons')]
            .filter(isVisible)
            .map((toolbar, toolbarIndex) => {
                toolbar.dataset.a11yToolbarIndex = toolbarIndex;

                const buttons = [...toolbar.querySelectorAll('button')].filter(isVisible).map((button, buttonIndex) => {
                    button.dataset.a11yButtonIndex = `${toolbarIndex}-${buttonIndex}`;
                    return {
                        selector: `[data-a11y-button-index="${toolbarIndex}-${buttonIndex}"]`,
                        name: button.getAttribute('aria-label') || button.getAttribute('title') || button.innerText.trim(),
                        disabled: isDisabled(button),
                        nativeDisabled: button.matches(':disabled'),
                        tabIndex: button.tabIndex
                    };
                });

                const focusables = [...toolbar.querySelectorAll(focusableSelector)]
                    .filter(element => isVisible(element) && !isDisabled(element) && element.tabIndex >= 0)
                    .map((element, focusIndex) => {
                        element.dataset.a11yFocusIndex = `${toolbarIndex}-${focusIndex}`;
                        return {
                            selector: `[data-a11y-focus-index="${toolbarIndex}-${focusIndex}"]`,
                            button: element.tagName === 'BUTTON'
                        };
                    });

                return {
                    selector: `[data-a11y-toolbar-index="${toolbarIndex}"]`,
                    buttons,
                    focusables
                };
            });
    });

    I.assertAbove(toolbars.length, 1, "The page must contain multiple visible DataTable button toolbars");

    const assertButtonContrast = (state, label) => {
        I.assertAbove(state.foregroundContrast, 2.99, `${label} foreground contrast must be at least 3:1`);
        //I.assertAbove(state.componentContrast, 2.99, `${label} component contrast must be at least 3:1`);
    };

    for (const toolbar of toolbars) {
        I.assertAbove(toolbar.buttons.length, 0, `${toolbar.selector} must contain visible buttons`);

        for (const button of toolbar.buttons) {
            I.assertTrue(button.name.length > 0, `${button.selector} must have an accessible name`);
            I.assertFalse(button.tabIndex > 0, `${button.name} must not use a positive tabindex`);
            if (button.disabled) {
                I.assertTrue(button.nativeDisabled || button.tabIndex === -1,
                    `${button.name} must be removed from keyboard navigation when disabled`);
                continue;
            }

            I.assertEqual(button.tabIndex, 0, `${button.name} must be reachable in the natural TAB order`);
            const normalState = await I.executeScript(selector => window.__wjA11yGetButtonState(selector), button.selector);
            assertButtonContrast(normalState, `${button.name} normal state`);
        }

        await I.executeScript(selector => {
            const toolbarElement = document.querySelector(selector);
            const sentinel = document.createElement('button');
            sentinel.className = 'a11y-toolbar-sentinel';
            toolbarElement.before(sentinel);
            sentinel.focus();
        }, toolbar.selector);

        for (const focusable of toolbar.focusables) {
            I.pressKey('Tab');
            I.waitForElement(`${focusable.selector}:focus`, 5);
            if (focusable.button) {
                const focusState = await I.executeScript(selector => window.__wjA11yGetButtonState(selector), focusable.selector);
                assertButtonContrast(focusState, `${focusState.name} focus state`);
                I.assertEqual(focusState.outlineStyle, 'solid', `${focusState.name} must have a solid focus indicator`);
                I.assertAbove(focusState.outlineWidth, 1.99, `${focusState.name} focus indicator must be at least 2px wide`);
                I.assertAbove(focusState.outlineContrast, 2.99, `${focusState.name} focus indicator contrast must be at least 3:1`);
            }
        }

        await I.executeScript(() => document.querySelector('.a11y-toolbar-sentinel')?.remove());

        for (const button of toolbar.buttons.filter(button => !button.disabled)) {
            I.moveCursorTo(button.selector);
            I.waitForElement(`${button.selector}:hover`, 5);
            const hoverState = await I.executeScript(selector => window.__wjA11yGetButtonState(selector), button.selector);
            assertButtonContrast(hoverState, `${button.name} hover state`);
        }
    }
});
