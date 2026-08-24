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

Scenario("p43: p44: button contrast", async ({ I, DT, a11y }) => {
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
        transitionOverride.textContent = 'div.dt-buttons button { transition: none !important; }';
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
                        tabIndex: button.tabIndex,
                        tooltip: button.matches('[data-toggle*="tooltip"], [data-bs-toggle*="tooltip"]')
                    };
                });

                const focusables = [...toolbar.querySelectorAll(focusableSelector)]
                    .filter(element => isVisible(element) && !isDisabled(element) && element.tabIndex >= 0)
                    .map((element, focusIndex) => {
                        element.dataset.a11yFocusIndex = `${toolbarIndex}-${focusIndex}`;
                        return {
                            selector: `[data-a11y-focus-index="${toolbarIndex}-${focusIndex}"]`,
                            button: element.tagName === 'BUTTON',
                            tooltip: element.matches('[data-toggle*="tooltip"], [data-bs-toggle*="tooltip"]')
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
            I.assertTrue(button.tooltip, `${button.name} must provide a keyboard-accessible tooltip`);
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

                if (focusable.tooltip) {
                    I.waitForElement(`${focusable.selector}[aria-describedby]`, 5);
                    const tooltipId = await I.grabAttributeFrom(focusable.selector, 'aria-describedby');
                    I.assertTrue(tooltipId.length > 0, `${focusState.name} must reference its tooltip`);
                    I.waitForVisible(`#${tooltipId}`, 5);
                    I.seeElement(`#${tooltipId}[role="tooltip"]`);
                    const tooltipText = await I.grabTextFrom(`#${tooltipId}`);
                    I.assertTrue(tooltipText.trim().length > 0, `${focusState.name} tooltip must contain text`);

                    I.pressKey('Escape');
                    I.waitForInvisible(`#${tooltipId}`, 5);
                    I.waitForElement(`${focusable.selector}:focus`, 5);
                }
            }
        }

        await I.executeScript(() => document.querySelector('.a11y-toolbar-sentinel')?.remove());

        await I.executeScript(selector => {
            $(selector).find('[data-toggle*="tooltip"], [data-bs-toggle*="tooltip"]').each(function() {
                $(this).tooltip('hide').tooltip('disable');
            });
        }, toolbar.selector);

        for (const button of toolbar.buttons.filter(button => !button.disabled)) {
            I.moveCursorTo(button.selector);
            I.waitForElement(`${button.selector}:hover`, 5);
            const hoverState = await I.executeScript(selector => window.__wjA11yGetButtonState(selector), button.selector);
            assertButtonContrast(hoverState, `${button.name} hover state`);
        }
    }
});

Scenario("p47: modal window rowcount", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();

    const wrapper = "#datatableInit_wrapper";
    const pageLengthTrigger = `${wrapper} button.buttons-page-length`;
    I.clickCss(`${wrapper} button.buttons-settings`);
    I.waitForVisible(pageLengthTrigger, 5);
    I.executeScript(selector => document.querySelector(selector).focus(), pageLengthTrigger);
    I.pressKey("Enter");

    const dialog = `${wrapper} button.buttons-page-length[aria-expanded="true"] ~ div.dt-button-collection`;
    I.waitForVisible(dialog, 5);
    I.waitForElement(`${dialog} button.button-page-length:focus`, 5);
    I.seeElement(`${dialog}[role="dialog"][aria-modal="true"][aria-label]`);
    I.dontSeeElement(`${dialog} button > a, ${dialog} button > button`);
    I.seeElement(`${dialog} [role="listbox"][aria-label]`);

    const state = await I.executeScript(selector => {
        const dialogElement = document.querySelector(selector);
        const isVisible = element => element.getClientRects().length > 0 && getComputedStyle(element).visibility !== 'hidden';
        const getRect = element => {
            const rect = element.getBoundingClientRect();
            return {
                top: Math.round(rect.top),
                bottom: Math.round(rect.bottom),
                width: Math.round(rect.width)
            };
        };
        const options = [...dialogElement.querySelectorAll('button.button-page-length')].map(button => ({
            active: button.classList.contains('dt-button-active-a'),
            role: button.getAttribute('role'),
            selected: button.getAttribute('aria-selected'),
            rect: getRect(button),
            marker: getComputedStyle(button, '::before').content
        }));
        const buttons = [...dialogElement.querySelectorAll('button')].filter(isVisible).map((button, index) => {
            button.dataset.a11yRowcountIndex = index;
            return {
                name: button.innerText.trim(),
                selector: `[data-a11y-rowcount-index="${index}"]`
            };
        });
        const saveButton = dialogElement.querySelector('button.dt-close-modal.btn-primary');
        const cancelButton = dialogElement.querySelector('button.dt-close-modal.btn-outline-secondary');

        return {
            dialogWidth: Math.round(dialogElement.getBoundingClientRect().width),
            options,
            buttons,
            saveRect: getRect(saveButton),
            cancelRect: getRect(cancelButton)
        };
    }, dialog);

    I.assertAbove(state.options.length, 1, "The row count dialog must provide multiple options");
    I.assertAbove(state.dialogWidth, 750, "The row count dialog must preserve its wide modal layout");
    I.assertEqual(new Set(state.options.map(option => option.rect.top)).size, 3,
        "The row count options must be arranged in three rows");
    I.assertTrue(state.options.every(option => option.rect.width >= 175),
        "Every row count option must preserve its grid width");
    I.assertTrue(state.options.every(option => option.marker !== 'none' && option.marker !== 'normal'),
        "Every row count option must display its selection marker");
    const optionsBottom = Math.max(...state.options.map(option => option.rect.bottom));
    I.assertAbove(state.saveRect.top, optionsBottom, "The Save button must be placed below the row count options");
    I.assertEqual(state.saveRect.top, state.cancelRect.top, "The modal action buttons must be aligned in one row");
    I.assertEqual(state.options.filter(option => option.selected === 'true').length, 1,
        "Exactly one row count option must be selected");
    for (const option of state.options) {
        I.assertEqual(option.role, 'option', "Every row count choice must expose the option role");
        I.assertEqual(option.selected, option.active ? 'true' : 'false',
            "aria-selected must match the visually active row count option");
    }

    I.assertTrue(state.buttons.some(button => button.name.includes("Uložiť")), "The dialog must contain a Save button");
    I.assertTrue(state.buttons.some(button => button.name.includes("Zrušiť")), "The dialog must contain a Cancel button");
    I.waitForElement(`${state.buttons[0].selector}:focus`, 5);
    for (const button of state.buttons.slice(1)) {
        I.pressKey("Tab");
        I.waitForElement(`${button.selector}:focus`, 5);
    }
    I.pressKey("Tab");
    I.waitForElement(`${state.buttons[0].selector}:focus`, 5);

    await a11y.check(dialog);
});

Scenario("p48: DT dialog focus", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();

    const wrapper = "#datatableInit_wrapper";
    const modal = "#datatableInit_modal";
    I.forceClick(`${wrapper} .dt-scroll-body tbody tr:first-child td.dt-select-td`);
    I.waitForElement(`${wrapper} button[data-dtbtn="edit"]:not(.disabled)`, 5);

    const actions = [
        { name: "create", selector: `${wrapper} button[data-dtbtn="create"]`, expectsFieldFocus: true },
        { name: "edit", selector: `${wrapper} button[data-dtbtn="edit"]`, expectsFieldFocus: true },
        { name: "duplicate", selector: `${wrapper} button[data-dtbtn="duplicate"]`, expectsFieldFocus: true },
        { name: "remove", selector: `${wrapper} button[data-dtbtn="remove"]`, expectsFieldFocus: false }
    ];

    const assertFocusedHeaderButton = async (selector, label) => {
        I.waitForElement(`${selector}:focus`, 5);
        I.waitForElement(`${selector}[aria-describedby]`, 5);

        const tooltipId = await I.grabAttributeFrom(selector, "aria-describedby");
        I.assertTrue(tooltipId.length > 0, `${label} must reference its tooltip when focused`);
        I.waitForVisible(`#${tooltipId}`, 5);
        I.seeElement(`#${tooltipId}[role="tooltip"]`);
        const tooltipText = await I.grabTextFrom(`#${tooltipId}`);
        I.assertTrue(tooltipText.trim().length > 0, `${label} tooltip must contain text`);

        const iconContrast = await I.executeScript(buttonSelector => {
            const button = document.querySelector(buttonSelector);
            const icon = button.querySelector(".ti");
            const parseColor = value => {
                const channels = value.match(/[\d.]+/g).map(Number);
                return channels.slice(0, 3);
            };
            const luminance = color => {
                const channels = color.map(channel => {
                    const value = channel / 255;
                    return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
                });
                return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
            };
            const backgroundLuminance = luminance(parseColor(getComputedStyle(button).backgroundColor));
            const iconLuminance = luminance(parseColor(getComputedStyle(icon).color));
            return (Math.max(backgroundLuminance, iconLuminance) + 0.05) /
                (Math.min(backgroundLuminance, iconLuminance) + 0.05);
        }, selector);
        I.assertAbove(iconContrast, 2.99, `${label} focused icon contrast must be at least 3:1`);

        I.pressKey("Escape");
        I.waitForInvisible(`#${tooltipId}`, 5);
        I.waitForElement(`${selector}:focus`, 5);
    };

    for (const action of actions) {
        I.executeScript(selector => document.querySelector(selector).focus(), action.selector);
        I.pressKey("Enter");
        I.waitForVisible(`${modal}.show`, 5);
        I.waitForElement(`${modal} :focus`, 5);

        const state = await I.executeScript(selector => {
            const dialog = document.querySelector(selector);
            const activeElement = document.activeElement;
            const requiredFields = [...dialog.querySelectorAll('.DTE_Field.required')].map(field => {
                const controls = [...field.querySelectorAll('input:not([type="hidden"]), select, textarea, [contenteditable="true"]')]
                    .filter(control => !control.closest('.bs-searchbox, .dt-search, .dataTables_filter'));
                const selectpicker = field.querySelector('.bootstrap-select > button[role="combobox"]');
                return {
                    hasControl: controls.length > 0,
                    controlsRequired: controls.every(control => control.getAttribute('aria-required') === 'true'),
                    selectpickerRequired: selectpicker == null || selectpicker.getAttribute('aria-required') === 'true'
                };
            });

            return {
                activeInDialog: dialog.contains(activeElement),
                activeInBody: activeElement.closest('.DTE_Body') != null,
                dialogRole: dialog.getAttribute('role'),
                modal: dialog.getAttribute('aria-modal'),
                labelledBy: dialog.getAttribute('aria-labelledby'),
                help: {
                    label: dialog.querySelector('.DTE_Header button.show-help')?.getAttribute('aria-label'),
                    tabIndex: dialog.querySelector('.DTE_Header button.show-help')?.tabIndex,
                    type: dialog.querySelector('.DTE_Header button.show-help')?.type
                },
                maximize: {
                    label: dialog.querySelector('.DTE_Header button.maximize')?.getAttribute('aria-label'),
                    tabIndex: dialog.querySelector('.DTE_Header button.maximize')?.tabIndex,
                    type: dialog.querySelector('.DTE_Header button.maximize')?.type
                },
                requiredFields,
                searchLabels: [...dialog.querySelectorAll('.bs-searchbox input')].map(input => input.getAttribute('aria-label'))
            };
        }, modal);

        I.assertTrue(state.activeInDialog, `${action.name} dialog must receive focus when it opens`);
        if (action.expectsFieldFocus) {
            I.assertTrue(state.activeInBody, `${action.name} dialog must focus its first form control`);
        }
        I.assertEqual(state.dialogRole, "dialog", `${action.name} editor must expose the dialog role`);
        I.assertEqual(state.modal, "true", `${action.name} editor must be announced as modal`);
        I.assertTrue(state.labelledBy != null && state.labelledBy.length > 0,
            `${action.name} editor must be labelled by its heading`);

        if (action.name !== "remove") {
            I.assertEqual(state.help.label, "Pomocník", "The Help button must have a Slovak accessible name");
            I.assertEqual(state.help.tabIndex, 0, "The Help button must be keyboard focusable");
            I.assertEqual(state.help.type, "button", "The Help button must not submit the form");
            I.assertTrue(/^Maximal+izovať okno$/.test(state.maximize.label),
                "The Maximize button must have a Slovak accessible name");
            I.assertEqual(state.maximize.tabIndex, 0, "The Maximize button must be keyboard focusable");
            I.assertEqual(state.maximize.type, "button", "The Maximize button must not submit the form");
            I.assertTrue(state.requiredFields.length > 0, "The test editor must contain required fields");
            I.assertTrue(state.requiredFields.every(field => field.hasControl && field.controlsRequired && field.selectpickerRequired),
                "Every required field must expose aria-required=true");
            I.assertTrue(state.searchLabels.length > 0, "The editor must contain searchable select fields");
            I.assertTrue(state.searchLabels.every(label => label === "Hľadať"),
                "Search instructions must use the current interface language");
        }

        if (action.name === "create") {
            I.pressKey(['Shift', 'Tab']);
            I.waitForElement(`${modal} .DTE_Header button.btn-close-editor:focus`, 5);
            I.pressKey(['Shift', 'Tab']);
            await assertFocusedHeaderButton(`${modal} .DTE_Header button.maximize`, "Maximize button");
            I.pressKey("Enter");
            I.waitForVisible(`${modal} .DTE_Header button.minimize`, 5);
            await assertFocusedHeaderButton(`${modal} .DTE_Header button.minimize`, "Minimize button");
            I.pressKey("Enter");
            I.waitForElement(`${modal} .DTE_Header button.maximize:focus`, 5);
            I.pressKey(['Shift', 'Tab']);
            await assertFocusedHeaderButton(`${modal} .DTE_Header button.show-help`, "Help button");
            await a11y.check(modal);
        }

        I.executeScript(selector => document.querySelector(selector).focus(), `${modal} .DTE_Footer button.btn-close-editor`);
        I.pressKey("Enter");
        I.waitForInvisible(modal, 5);
        I.waitForElement(`${action.selector}:focus`, 5);
    }
});

Scenario("p49: datatable interactive rows", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/templates/temps-list/");
    DT.waitForLoader();

    const table = "#datatableInit";
    const wrapper = `${table}_wrapper`;
    const firstRow = `${table} tbody tr:first-child`;
    const editButton = `${wrapper} button[data-dtbtn="edit"]`;

    const rows = await I.executeScript(tableSelector => [...document.querySelectorAll(`${tableSelector} tbody tr`)]
        .filter(row => row.querySelector("td.dt-select-td"))
        .map(row => ({
            selected: row.getAttribute("aria-selected"),
            tabIndex: row.tabIndex
        })), table);

    I.assertAbove(rows.length, 1, "The test table must contain multiple interactive rows");
    I.assertTrue(rows.every(row => row.tabIndex === 0), "Every selectable row must be keyboard focusable");
    I.assertTrue(rows.every(row => row.selected === "false"), "Every unselected row must expose aria-selected=false");

    I.executeScript(selector => document.querySelector(selector).focus(), firstRow);
    I.waitForElement(`${firstRow}:focus`, 5);
    I.pressKey("Space");
    I.waitForElement(`${firstRow}.selected[aria-selected="true"]:focus`, 5);
    I.waitForElement(`${editButton}:not(:disabled)`, 5);

    I.pressKey("Space");
    I.waitForElement(`${firstRow}:not(.selected)[aria-selected="false"]:focus`, 5);
    I.waitForElement(`${editButton}:disabled`, 5);

    I.pressKey("Enter");
    I.waitForElement(`${firstRow}.selected[aria-selected="true"]:focus`, 5);
    await a11y.check(wrapper);
});

Scenario("p50: date picker", async ({ I, DT, DTE, a11y }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();
    I.clickCss("#redirectTable_wrapper button[data-dtbtn='create']");
    DTE.waitForEditor("redirectTable");

    const modal = "#redirectTable_modal";
    const input = "#DTE_Field_publishDate";
    I.executeScript(selector => document.querySelector(selector).focus(), input);
    I.waitForVisible("div.dt-datetime[role='dialog']", 5);
    I.waitForElement("div.dt-datetime .dt-datetime-calendar button[tabindex='0']:focus", 5);

    const state = await I.executeScript(inputSelector => {
        const dateInput = document.querySelector(inputSelector);
        const dialog = document.getElementById(dateInput.getAttribute("aria-controls"));
        const describedBy = dateInput.getAttribute("aria-describedby").split(/\s+/);
        const instructions = describedBy.map(id => document.getElementById(id)).find(element => element?.classList.contains("wj-datetime-instructions"));
        const days = [...dialog.querySelectorAll(".dt-datetime-calendar button[data-year]")];
        const timeButtons = [...dialog.querySelectorAll(".dt-datetime-time button[data-unit]")];

        return {
            dialogDescription: dialog.getAttribute("aria-describedby"),
            dialogLabel: dialog.getAttribute("aria-label"),
            dialogRole: dialog.getAttribute("role"),
            dayLabels: days.map(button => button.getAttribute("aria-label")),
            dayTabStops: days.filter(button => button.tabIndex === 0).length,
            expanded: dateInput.getAttribute("aria-expanded"),
            focusedDay: days.includes(document.activeElement),
            focusedDaySelectedOrCurrent: document.activeElement.closest("td")?.classList.contains("selected") ||
                document.activeElement.closest("td")?.classList.contains("now"),
            hasPopup: dateInput.getAttribute("aria-haspopup"),
            instructions: instructions?.textContent,
            monthLabel: dialog.querySelector(".dt-datetime-month")?.getAttribute("aria-label"),
            nextLabel: dialog.querySelector(".dt-datetime-iconRight button")?.getAttribute("aria-label"),
            previousLabel: dialog.querySelector(".dt-datetime-iconLeft button")?.getAttribute("aria-label"),
            selectedDays: dialog.querySelectorAll(".dt-datetime-calendar td[aria-selected='true']").length,
            timeLabels: timeButtons.map(button => button.getAttribute("aria-label")),
            timePressed: timeButtons.every(button => ["true", "false"].includes(button.getAttribute("aria-pressed"))),
            yearLabel: dialog.querySelector(".dt-datetime-year")?.getAttribute("aria-label")
        };
    }, input);

    I.assertEqual(state.hasPopup, "dialog", "The date input must announce that it opens a dialog");
    I.assertEqual(state.expanded, "true", "The date input must announce the open picker");
    I.assertTrue(state.focusedDay, "Opening the date picker must focus a calendar day");
    I.assertTrue(state.focusedDaySelectedOrCurrent, "Opening the date picker must focus the selected or current day");
    I.assertEqual(state.dialogRole, "dialog", "The date picker must expose the dialog role");
    I.assertTrue(state.dialogLabel.length > 0, "The date picker dialog must have an accessible name");
    I.assertTrue(state.dialogDescription.length > 0, "The date picker dialog must reference its instructions");
    I.assertTrue(state.instructions.includes("DD.MM.YYYY"), "The input instructions must explain the expected date format");
    I.assertTrue(state.instructions.toLowerCase().includes("šípk"), "The input instructions must explain keyboard navigation");
    I.assertTrue(state.previousLabel.length > 0 && state.nextLabel.length > 0,
        "Previous and next month buttons must have accessible names");
    I.assertTrue(state.monthLabel.length > 0 && state.yearLabel.length > 0,
        "Month and year controls must have accessible names");
    I.assertTrue(state.dayLabels.length > 27 && state.dayLabels.every(label => label.length > 0),
        "Every day button must announce its full date");
    I.assertEqual(state.dayTabStops, 1, "The calendar grid must use a single keyboard tab stop");
    I.assertTrue(state.selectedDays <= 1, "At most one calendar day may be marked as selected");
    I.assertTrue(state.timeLabels.length > 0 && state.timeLabels.every(label => label.length > 0) && state.timePressed,
        "Every time option must expose its name and selected state");

    I.pressKey("Tab");
    I.waitForElement("div.dt-datetime :focus", 5);
    I.seeElement("div.dt-datetime:has(:focus)");
    I.pressKey(["Shift", "Tab"]);
    I.waitForElement("div.dt-datetime .dt-datetime-calendar button:focus", 5);

    const firstDateLabel = await I.grabAttributeFrom("div.dt-datetime .dt-datetime-calendar button:focus", "aria-label");
    I.pressKey("ArrowRight");
    I.waitForElement("div.dt-datetime .dt-datetime-calendar button:focus", 5);
    const nextDateLabel = await I.grabAttributeFrom("div.dt-datetime .dt-datetime-calendar button:focus", "aria-label");
    I.assertNotEqual(nextDateLabel, firstDateLabel, "Arrow keys must move focus to another calendar day");

    I.pressKey("Enter");
    I.waitForElement("div.dt-datetime .dt-datetime-calendar td[aria-selected='true'] button:focus", 5);
    const value = await I.grabValueFrom(input);
    I.assertTrue(value.length > 0, "Enter must select the focused date and update the input");

    I.pressKey("ArrowRight");
    I.pressKey("Space");
    I.waitForElement("div.dt-datetime .dt-datetime-calendar td[aria-selected='true'] button:focus", 5);
    const nextValue = await I.grabValueFrom(input);
    I.assertNotEqual(nextValue, value, "Space must select the focused date and update the input");

    I.pressKey("Tab");
    I.waitForElement("div.dt-datetime :focus", 5);
    I.seeElement("div.dt-datetime:has(:focus)");

    await a11y.check(modal);

    I.executeScript(() => document.activeElement.dispatchEvent(new KeyboardEvent("keydown", {
        bubbles: true,
        cancelable: true,
        key: "Escape"
    })));
    I.waitForInvisible("div.dt-datetime[role='dialog']", 5);
    I.waitForElement(`${input}:focus[aria-expanded="false"]`, 5);
});

Scenario("p51: grid edit accessibility", async ({ I, DT, a11y }) => {
    I.amOnPage("/admin/v9/settings/redirect/");
    DT.waitForLoader();

    const wrapper = "#redirectTable_wrapper";
    const toggle = `${wrapper} [data-dtbtn="celledit"]`;
    const editableCell = "#redirectTable tbody tr:first-child td.dt-row-edit:not(.cell-not-editable)";

    const initialState = await I.executeScript(toggleSelector => {
        const control = document.querySelector(toggleSelector);
        const decoration = control.querySelector('.form-check-input');
        return {
            controls: control.getAttribute("aria-controls"),
            decorationHidden: decoration.getAttribute("aria-hidden"),
            label: control.getAttribute("aria-label"),
            nestedControls: control.querySelectorAll('input, button, select, textarea, a[href]').length,
            pressed: control.getAttribute("aria-pressed"),
            role: control.getAttribute("role"),
            tabIndex: control.tabIndex
        };
    }, toggle);

    I.assertEqual(initialState.role, "button", "The grid edit toggle must expose the button role");
    I.assertEqual(initialState.pressed, "false", "The grid edit toggle must announce its inactive state");
    I.assertEqual(initialState.controls, "redirectTable", "The grid edit toggle must reference its table");
    I.assertTrue(initialState.label.length > 0, "The grid edit toggle must have an accessible name");
    I.assertEqual(initialState.tabIndex, 0, "The grid edit toggle must be keyboard focusable");
    I.assertEqual(initialState.nestedControls, 0, "The grid edit toggle must not contain nested interactive controls");
    I.assertEqual(initialState.decorationHidden, "true", "The visual switch must be hidden from assistive technology");

    I.executeScript(toggleSelector => document.querySelector(toggleSelector).focus(), toggle);
    I.pressKey("Space");
    I.waitForElement(`${toggle}[aria-pressed="true"]:focus`, 5);
    I.seeElement("body.datatable-cell-editing");
    I.waitForElement(`${editableCell}[tabindex="0"]`, 5);

    const enabledState = await I.executeScript(toggleSelector => {
        const control = document.querySelector(toggleSelector);
        return {
            enabled: control.classList.contains("enabled"),
            pressed: control.getAttribute("aria-pressed")
        };
    }, toggle);
    I.assertEqual(enabledState.pressed, "true", "The grid edit toggle must announce its active state");
    I.assertTrue(enabledState.enabled, "The visual switch must match the active state");

    I.executeScript(cellSelector => document.querySelector(cellSelector).focus(), editableCell);
    I.waitForElement(`${editableCell}:focus`, 5);
    I.pressKey("Enter");
    const bubble = "div.DTE_Bubble.wj-cell-edit-dialog[role='dialog']";
    I.waitForVisible(`${bubble} .DTE_Bubble_Liner`, 5);
    I.waitForElement(`${bubble} .btn-ai`, 5);
    I.waitForElement(`${bubble} input:focus`, 5);

    const bubbleState = await I.executeScript(() => {
        const bubble = document.querySelector("div.DTE_Bubble.wj-cell-edit-dialog");
        const buttons = [...bubble.querySelectorAll("div.DTE_Form_Buttons button")];
        const input = bubble.querySelector("input:not([type='hidden']), textarea, select");
        const triangle = bubble.querySelector("div.DTE_Bubble_Triangle");
        return {
            dialogLabel: bubble.getAttribute("aria-label"),
            buttonLabels: buttons.map(button => button.getAttribute("aria-label")),
            buttonTypes: buttons.map(button => button.getAttribute("type")),
            buttonTops: buttons.map(button => button.getBoundingClientRect().top),
            inputTop: input.getBoundingClientRect().top,
            triangleDisplay: getComputedStyle(triangle).display
        };
    });
    I.assertTrue(bubbleState.dialogLabel.length > 0, "The cell editor dialog must have an accessible name");
    I.assertEqual(bubbleState.buttonLabels.length, 2, "The cell editor must expose confirm and cancel buttons");
    I.assertTrue(bubbleState.buttonLabels.every(label => label?.length > 0), "Every cell editor button must have an accessible name");
    I.assertTrue(bubbleState.buttonTypes.every(type => type === "button"), "Cell editor actions must use button semantics");
    I.assertTrue(bubbleState.buttonTops.every(top => Math.abs(top - bubbleState.inputTop) < 1), "Cell editor actions must align with the input");
    I.assertEqual(bubbleState.triangleDisplay, "none", "The obsolete cell editor pointer must be hidden");
    await a11y.check(bubble);

    I.clickCss(`${bubble} div.DTE_Form_Buttons button.btn-outline-secondary`);
    I.waitForInvisible(`${bubble} .DTE_Bubble_Liner`, 5);
    I.executeScript(toggleSelector => document.querySelector(toggleSelector).focus(), toggle);
    I.pressKey("Enter");
    I.waitForElement(`${toggle}[aria-pressed="false"]:focus`, 5);
    I.dontSeeElement("body.datatable-cell-editing");
    I.dontSeeElement(`${editableCell}[tabindex="0"]`);
});
