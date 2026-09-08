Feature('settings.custom-fields');

Before(({ I, login }) =>{
    login('admin');
});

const docId_1 = "163799";
const docId_2 = "163800";
const docClass = "sk.iway.iwcm.doc.DocDetails";

const templateId = "4319";
const templateClass = "sk.iway.iwcm.doc.TemplateDetails";
const tooltipText = "autotest-custom-fields-required-tooltip";
const jsonEditorMarker = "autotest-jsoneditor-settings";
let jsonEditorOriginal;
const requiredOverrideAlphabet = "K";
const overrideAlphabet = "T";
const overrideGlobalLabel = "autotest-custom-fields-global-text";
const overrideSpecificLabel = "autotest-custom-fields-specific-select";
const overrideOptionOne = { label: "autotest-specific-option-one", value: "autotest-option-one" };
const overrideOptionTwo = { label: "autotest-specific-option-two", value: "autotest-option-two" };
const allTypesDocId = docId_2;
const allTypesSelectOptions = [
    { label: "autotest-select-label-one", value: "autotest-select-value-one" },
    { label: "autotest-select-label-two", value: "autotest-select-value-two" }
];
const allTypesMultiOptions = [
    { label: "autotest-multiselect-label-one", value: "autotest-multiselect-value-one" },
    { label: "autotest-multiselect-label-two", value: "autotest-multiselect-value-two" }
];
const allTypesRadioOptions = [
    { label: "autotest-radio-label-one", value: "autotest-radio-value-one" },
    { label: "autotest-radio-label-two", value: "autotest-radio-value-two" }
];
const allTypesGroup = { groupId: 67, fullPath: "/Test stavov" };
const allTypesEnumeration = "enumeration-options|2|string1|string1";
const warningXssMarkerId = "autotest-warning-xss-payload";
const warningXssPayload = 'autotest-warning-xss"><img id="' + warningXssMarkerId + '" src="x-invalid" onerror="document.body.dataset.customFieldsWarningXss=1">';
const allTypesDefinitions = [
    { alphabet: "A", type: "text", label: "autotest-custom-type-text", settings: { textMaxLength: 14, textWarningLength: 8, warningText: warningXssPayload } },
    { alphabet: "B", type: "textarea", label: "autotest-custom-type-textarea" },
    { alphabet: "C", type: "select", label: "autotest-custom-type-select", settings: { selectOptions: allTypesSelectOptions } },
    { alphabet: "D", type: "multiselect", label: "autotest-custom-type-multiselect", settings: { selectOptions: allTypesMultiOptions } },
    { alphabet: "E", type: "boolean", label: "autotest-custom-type-boolean" },
    { alphabet: "F", type: "number", label: "autotest-custom-type-number" },
    { alphabet: "G", type: "date", label: "autotest-custom-type-date" },
    { alphabet: "H", type: "none", label: "autotest-custom-type-none" },
    { alphabet: "I", type: "radio", label: "autotest-custom-type-radio", settings: { selectOptions: allTypesRadioOptions } },
    { alphabet: "J", type: "autocomplete", label: "autotest-custom-type-autocomplete" },
    { alphabet: "K", type: "image", label: "autotest-custom-type-image" },
    { alphabet: "L", type: "link", label: "autotest-custom-type-link" },
    { alphabet: "M", type: "json_group", label: "autotest-custom-type-json-group" },
    { alphabet: "N", type: "json_doc", label: "autotest-custom-type-json-doc" },
    { alphabet: "O", type: "dir", label: "autotest-custom-type-dir" },
    { alphabet: "P", type: "docsIn", label: "autotest-custom-type-docsin", settings: { docInGroup: allTypesGroup } },
    { alphabet: "Q", type: "select", label: "autotest-custom-type-enumeration", settings: { optionsSource: "enumeration", enumeration: allTypesEnumeration } },
    { alphabet: "R", type: "uuid", label: "autotest-custom-type-uuid" },
    { alphabet: "S", type: "color", label: "autotest-custom-type-color" },
    { alphabet: "T", type: "checkbox", label: "autotest-custom-type-checkbox", settings: { optionsSource: "enumeration", enumeration: allTypesEnumeration } }
];

Scenario('Custom fields required logic test @screenshot', async ({ I, DT, DTE, Document }) =>{
    I.say("Do some basic checks");
    await deleteCustomFieldSettingsByTooltip(I, DT, DTE);
    I.amOnPage("/admin/v9/settings/custom-fields/");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");

    I.say("Test autocomplete for Entities");
    I.fillField("#DTE_Field_className", "doc");
    I.waitForVisible("ul.dt-autocomplete-select");
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText("sk.iway.iwcm.components.forum.jpa.DocForumEntity") );
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText(docClass) );
    I.seeElement( locate("ul.dt-autocomplete-select li.ui-menu-item div").withText("sk.iway.iwcm.doc.DocHistory") );

    I.say("Go test DOC entity - not fields required");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId_1);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");
    DTE.save();

    I.say("Same for second page");
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId_2);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-history-tab");
    DTE.save();

    I.say("Now set 3 global rules for DocDetails entity - fields required");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    // add small / big alphabet, it should proceed correctly
    addCustomFieldSetting(I, DTE, docClass, requiredOverrideAlphabet, null, true);
    addCustomFieldSetting(I, DTE, docClass, "h", null, true);
    addCustomFieldSetting(I, DTE, docClass, "I", null, true);

    checkDocCustomFields(I, DTE, docId_1, [requiredOverrideAlphabet, "H", "I"], ["F", "J"]);
    checkDocCustomFields(I, DTE, docId_2, [requiredOverrideAlphabet, "H", "I"], ["F", "J"]);

    I.say("Now test duplicity check");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    addCustomFieldSetting(I, DTE, docClass, requiredOverrideAlphabet.toLowerCase(), null, false, "Kombinácia polí „Použiť pre entitu“, „Voliteľné pole“, „ID entity“, „Závislé od entity“ a „ID závislej entity“ musí byť jedinečná. Zadajte inú hodnotu pre jedno z týchto polí.");

    I.say("Try save it but for specific entityId - should pass");
    I.fillField("#DTE_Field_entityId", docId_1);
    DTE.save();
    I.dontSeeElement(".DTE_Form_Error");

    I.say('Also for another entityId');
    addCustomFieldSetting(I, DTE, docClass, "I", docId_2, false);

    I.say("Now test how doc's fields changed");
    checkDocCustomFields(I, DTE, docId_1, ["H", "I"], ["F", requiredOverrideAlphabet, "J"]);
    checkDocCustomFields(I, DTE, docId_2, [requiredOverrideAlphabet, "H"], ["F", "J", "I"]);

    I.say("DocDetails are SPECIAL they custom fields logic can be ovewrite by TEMPLATES logic");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    addCustomFieldSetting(I, DTE, docClass, "f", null, true, null, templateClass, templateId, Document);
    addCustomFieldSetting(I, DTE, docClass, requiredOverrideAlphabet.toLowerCase(), null, true, null, templateClass, templateId);

    Document.screenshot("/frontend/webpages/customfields/custom-fields-settings-datatable.png");

    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");
    DTE.fillField("className", "sk.iway.iwcm.doc.DocDetails");
    DTE.selectOption("alphabet", "C");
    setCustomFieldType(I, "select");
    I.checkOption("#DTE_Field_required_0");
    I.fillField("input.options-value-1", "Mac OS");
    I.fillField("input.options-value-2", "macos");
    I.click("button.options-add-btn");

    Document.screenshotElement("div.DTE_Action_Create", "/frontend/webpages/customfields/custom-fields-settings-editor.png");

    DTE.cancel();

    I.say("GO check that only DOC with this TEMP is affected - bonus TEMP has highest priority");
    checkDocCustomFields(I, DTE, docId_1, ["F", requiredOverrideAlphabet, "H", "I"], ["J"]);
    checkDocCustomFields(I, DTE, docId_2, [requiredOverrideAlphabet, "H"], ["F", "J", "I"]);
});

Scenario('Custom fields entity override applies type label and options', async ({ I, DT, DTE }) => {
    I.say("Create global text custom field for DocDetails");
    I.amOnPage("/admin/v9/settings/custom-fields/");
    addCustomFieldSetting(I, DTE, docClass, overrideAlphabet, null, false, null, null, null, null, {
        type: "text",
        label: overrideGlobalLabel,
        textMaxLength: 12
    });

    I.say("Create entity-specific select custom field for the same DocDetails alphabet");
    addCustomFieldSetting(I, DTE, docClass, overrideAlphabet, docId_1, false, null, null, null, null, {
        type: "select",
        label: overrideSpecificLabel,
        selectOptions: [overrideOptionOne, overrideOptionTwo]
    });

    I.say("Specific entity should override global custom field settings");
    await checkDocCustomFieldSelect(I, DT, DTE, docId_1, overrideAlphabet, overrideSpecificLabel, [overrideOptionOne, overrideOptionTwo], overrideGlobalLabel);

    I.say("Another entity should keep global custom field settings");
    await checkDocCustomFieldText(I, DT, DTE, docId_2, overrideAlphabet, overrideGlobalLabel, 12, overrideSpecificLabel);
});

Scenario('Custom fields switch between static and enumeration options', ({ I, DTE }) => {
    I.amOnPage("/admin/v9/settings/custom-fields/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");

    ["select", "multiselect", "radio", "checkbox"].forEach(type => {
        I.say("Check option source switching for type " + type);
        setCustomFieldType(I, type);
        I.waitForVisible("div.DTE_Field_Name_optionsSource", 10);

        setCustomFieldOptionsSource(I, "static");
        I.seeCheckboxIsChecked("div.DTE_Field_Name_optionsSource input[value='static']");
        I.waitForVisible("div.DTE_Field_Name_selectOptions", 10);
        I.waitForInvisible("div.DTE_Field_Name_enumeration", 10);

        setCustomFieldOptionsSource(I, "enumeration");
        I.seeCheckboxIsChecked("div.DTE_Field_Name_optionsSource input[value='enumeration']");
        I.waitForVisible("div.DTE_Field_Name_enumeration", 10);
        I.waitForInvisible("div.DTE_Field_Name_selectOptions", 10);
    });

    I.say("Option source fields must be hidden for types without configurable options");
    setCustomFieldType(I, "text");
    I.waitForInvisible("div.DTE_Field_Name_optionsSource", 10);
    I.waitForInvisible("div.DTE_Field_Name_selectOptions", 10);
    I.waitForInvisible("div.DTE_Field_Name_enumeration", 10);

    DTE.cancel("customFieldsDataTable");
});

Scenario('Custom fields render all supported field types', async ({ I, DT, DTE }) => {
    I.say("Remove stale autotest custom field settings before all-types setup");
    await deleteCustomFieldSettingsByTooltip(I, DT, DTE);

    I.say("Create entity-specific custom field settings for every supported type");

    allTypesDefinitions.forEach(fieldDefinition => {
        addCustomFieldSetting(I, DTE, docClass, fieldDefinition.alphabet, allTypesDocId, false, null, null, null, null, {
            type: fieldDefinition.type,
            label: fieldDefinition.label,
            ...fieldDefinition.settings
        });
    });

    I.say("Verify every supported custom field type in the web page editor");
    openDocFieldsTab(I, DT, DTE, allTypesDocId);

    for (const fieldDefinition of allTypesDefinitions) {
        await checkRenderedCustomFieldType(I, fieldDefinition);
    }

    DTE.cancel();
});

Scenario('Custom fields required logic test - AFTER @screenshot', async ({ I, DT, DTE }) =>{
    I.say('Return it back to basic state - REMOVE added settings');

    await deleteCustomFieldSettingsByTooltip(I, DT, DTE);
});

Scenario('JSON editor validates objects and preserves source text', async ({ I, DT, DTE }) => {
    const fieldA = "#datatableInit_modal #DTE_Field_fieldA";
    const fieldB = "#datatableInit_modal #DTE_Field_fieldB";
    const escapedString = String.raw`"\u0061\n\""`;
    const source = '{"id":9007199254740993,"html":"</textarea>&quot;","nested":{"items":[true,null]},"decimal":1.00,"escapes":' + escapedString + '}';
    const formatted = '{\n  "id": 9007199254740993,\n  "html": "</textarea>&quot;",\n  "nested": {\n    "items": [\n      true,\n      null\n    ]\n  },\n  "decimal": 1.00,\n  "escapes": ' + escapedString + '\n}';
    const extended = "{title:'test', // title comment\ndata-toggle:'tooltip',action:{content:'{Question?}' // text of question\n}}";
    const extendedFormatted = "{\n  title: 'test', // title comment\n  data-toggle: 'tooltip',\n  action: {\n    content: '{Question?}' // text of question\n  }\n}";

    await deleteCustomFieldSettingsByTooltip(I, DT, DTE, jsonEditorMarker);
    openDocFieldsTab(I, DT, DTE, docId_2);
    jsonEditorOriginal = await getJsonEditorDocument(I, docId_2);
    DTE.cancel();

    I.amOnPage("/admin/v9/settings/custom-fields/");
    for (const alphabet of ["A", "B"]) {
        addCustomFieldSetting(I, DTE, docClass, alphabet, docId_2, alphabet === "A", null, null, null, null, {
            type: "jsoneditor",
            label: "autotest-jsoneditor-" + alphabet,
            tooltip: jsonEditorMarker
        });
    }

    openDocFieldsTab(I, DT, DTE, docId_2);
    I.waitForVisible(fieldA, 10);
    I.seeElementInDOM(fieldB);
    I.dontSeeElementInDOM(fieldA + "[maxlength='255']");
    I.fillField(fieldB, "");

    for (const invalid of [" ", "null", "[]", '"text"', '{"a":1,}', "{} {}"]) {
        I.fillField(fieldA, invalid);
        I.clickCss("#pills-dt-datatableInit-basic-tab");
        DTE.save();
        I.waitForVisible(fieldA + "[aria-invalid='true']", 10);
        I.seeElement("#datatableInit_modal .DTE_Field_Name_fieldA .form-text.text-danger");
    }

    I.fillField(fieldA, source);
    I.fillField(fieldB, "[]");
    DTE.save();
    I.waitForVisible(fieldB + "[aria-invalid='true']", 10);
    I.fillField(fieldB, "{title:'test' // closing brace is part of the comment }");
    DTE.save();
    I.waitForVisible(fieldB + "[aria-invalid='true']", 10);
    I.fillField(fieldB, "   ");
    I.clickCss("#datatableInit_modal .DTE_Field_Name_fieldA .md-jsoneditor-format");
    I.assertEqual(formatted, await I.grabValueFrom(fieldA), "Formatting must preserve numeric lexemes and string contents");
    I.dontSeeElement("#datatableInit_modal .DTE_Field_Name_fieldA script");
    const cursor = "#datatableInit_modal .DTE_Field_Name_fieldA .md-jsoneditor-position";
    const toolbarLayout = await I.executeScript(() => {
        const field = document.querySelector("#datatableInit_modal .DTE_Field_Name_fieldA");
        const toolbar = field.querySelector(".md-jsoneditor-toolbar").getBoundingClientRect();
        const textarea = field.querySelector("textarea").getBoundingClientRect();
        const button = field.querySelector(".md-jsoneditor-format").getBoundingClientRect();
        const position = field.querySelector(".md-jsoneditor-position").getBoundingClientRect();
        return { above: toolbar.bottom <= textarea.top, aligned: button.right < position.left,
            border: getComputedStyle(field.querySelector(".md-jsoneditor-format")).borderTopWidth,
            aiInToolbar: field.querySelectorAll(".md-jsoneditor-toolbar .btn-ai").length,
            sideButtons: field.querySelectorAll(".input-group > .btn-ai").length };
    });
    I.assertEqual("0px", toolbarLayout.border, "Toolbar actions must be borderless");
    I.assertEqual(1, toolbarLayout.aiInToolbar, "AI must be available in the JSON toolbar");
    I.assertEqual(0, toolbarLayout.sideButtons, "AI must not occupy a side column beside JSON");
    I.assertTrue(toolbarLayout.above, "The JSON toolbar must be above the textarea");
    I.assertTrue(toolbarLayout.aligned, "Cursor coordinates must be aligned to the right of the format button");
    I.executeScript(() => {
        const textarea = document.querySelector("#datatableInit_modal #DTE_Field_fieldA");
        textarea.focus();
        textarea.setSelectionRange(4, 4);
    });
    I.pressKey("ArrowRight");
    I.waitForText("Riadok 2, stĺpec 4", 5, cursor);
    I.pressKey("ArrowDown");
    I.waitForText("Riadok 3, stĺpec 4", 5, cursor);
    I.saveScreenshot("jsoneditor-desktop.png");

    const lineCount = await I.executeScript(() => document.querySelector("#datatableInit_modal .DTE_Field_Name_fieldA .md-textarea-editor__lines").textContent.split("\n").length);
    I.assertEqual(formatted.split("\n").length, lineCount, "The gutter must track every logical line");
    const typography = await I.executeScript(() => {
        const textarea = document.querySelector("#datatableInit_modal #DTE_Field_fieldA");
        const gutter = textarea.closest(".md-textarea-editor").querySelector(".md-textarea-editor__lines");
        return { input: getComputedStyle(textarea).fontFamily, lines: getComputedStyle(gutter).fontFamily,
            inputHeight: getComputedStyle(textarea).lineHeight, lineHeight: getComputedStyle(gutter).lineHeight };
    });
    I.assertContain(typography.input, "monospace", "JSON must use a monospace font even with AI controls");
    I.assertEqual(typography.input, typography.lines, "The gutter and textarea must share a font");
    I.assertEqual(typography.inputHeight, typography.lineHeight, "Line numbers must align with JSON lines");
    I.fillField(fieldA, '{\n  "items": [\n' + Array.from({ length: 40 }, (_, index) => "    " + (index === 0 ? JSON.stringify("x".repeat(180)) : index)).join(",\n") + '\n  ]\n}');
    const scrollState = await I.executeScript(() => {
        const textarea = document.querySelector("#datatableInit_modal #DTE_Field_fieldA");
        const gutter = textarea.closest(".md-textarea-editor").querySelector(".md-textarea-editor__lines");
        textarea.scrollTop = textarea.scrollHeight;
        textarea.dispatchEvent(new Event("scroll"));
        return { textarea: textarea.scrollTop, gutter: gutter.scrollTop, horizontal: textarea.scrollWidth > textarea.clientWidth };
    });
    I.assertAbove(scrollState.textarea, 0, "Long JSON must scroll inside the editor");
    I.assertEqual(scrollState.textarea, scrollState.gutter, "Line numbers must follow vertical scrolling");
    I.assertTrue(scrollState.horizontal, "Long JSON lines must scroll horizontally without wrapping");
    const errorIcon = await I.executeScript(() => {
        const textarea = document.querySelector("#datatableInit_modal #DTE_Field_fieldA");
        textarea.value += "}";
        textarea.dispatchEvent(new Event("input", { bubbles: true }));
        textarea.blur();
        textarea.scrollTop = 0;
        const style = getComputedStyle(textarea);
        return { invalid: textarea.getAttribute("aria-invalid"), position: style.backgroundPosition,
            padding: parseFloat(style.paddingRight), image: style.backgroundImage };
    });
    I.assertEqual("true", errorIcon.invalid, "Invalid long JSON must expose the error state");
    I.assertContain(errorIcon.position, "24px", "The error icon must leave space beside the scrollbar");
    I.assertAbove(errorIcon.padding, 24, "Invalid JSON must reserve text space for the error icon");
    I.assertNotEqual("none", errorIcon.image, "The error icon must remain visible");
    I.dontSeeElement(cursor);
    I.saveScreenshot("jsoneditor-error.png");
    I.executeScript(() => {
        const textarea = document.querySelector("#datatableInit_modal #DTE_Field_fieldA");
        textarea.value = textarea.value.slice(0, -1);
        textarea.dispatchEvent(new Event("input", { bubbles: true }));
        textarea.focus();
    });
    I.resizeWindow(720, 900);
    const fitsViewport = await I.executeScript(() => document.querySelector("#datatableInit_modal #DTE_Field_fieldA").getBoundingClientRect().right <= window.innerWidth);
    I.assertTrue(fitsViewport, "The JSON textarea must fit a narrow viewport");
    const toolbarFits = await I.executeScript(() => document.querySelector("#datatableInit_modal .DTE_Field_Name_fieldA .md-jsoneditor-position").getBoundingClientRect().right <= window.innerWidth);
    I.assertTrue(toolbarFits, "Cursor coordinates must fit the narrow viewport");
    I.saveScreenshot("jsoneditor-narrow.png");
    I.wjSetDefaultWindowSize();
    I.fillField(fieldA, formatted);
    I.clickCss(fieldA);
    I.pressKey("Tab");
    const tabLeftTextarea = await I.executeScript(() => document.activeElement.id !== "DTE_Field_fieldA");
    I.assertTrue(tabLeftTextarea, "Tab must move keyboard focus out of the textarea");
    I.dontSeeElement(cursor);
    I.pressKey(["Shift", "Tab"]);
    const shiftTabReturned = await I.executeScript(() => document.activeElement.id === "DTE_Field_fieldA");
    I.assertTrue(shiftTabReturned, "Shift+Tab must move keyboard focus back to the textarea");
    I.seeElement(cursor);
    I.fillField(fieldB, extended);
    I.clickCss("#datatableInit_modal .DTE_Field_Name_fieldB .md-jsoneditor-format");
    I.assertEqual(extendedFormatted, await I.grabValueFrom(fieldB), "Formatting must preserve supported extensions and comments");
    DTE.save();

    openDocFieldsTab(I, DT, DTE, docId_2);
    I.assertEqual(formatted, await I.grabValueFrom(fieldA), "Saving and reopening must preserve the exact JSON source");
    I.assertEqual(extendedFormatted, await I.grabValueFrom(fieldB), "Saving and reopening must preserve the extended object syntax");
    I.assertEqual(1, await I.grabNumberOfVisibleElements("#datatableInit_modal .DTE_Field_Name_fieldA .md-textarea-editor__lines"), "Reopening must not duplicate the gutter");
    I.assertEqual(1, await I.grabNumberOfVisibleElements("#datatableInit_modal .DTE_Field_Name_fieldA .md-jsoneditor-toolbar"), "Reopening must not duplicate the toolbar");
    DTE.cancel();

    const historyBefore = await getJsonEditorHistoryIds(I, docId_2);
    for (const operation of ["editor", "edit/" + docId_2, "import"]) {
        const rejected = await I.executeScript(async ({ docId, operation }) => {
            const entity = await fetch("/admin/rest/web-pages/" + docId, {
                headers: { "X-CSRF-Token": window.csrfToken }
            }).then(response => response.json());
            entity.fieldA = '{"invalid":}';
            // Client-provided definitions must never disable server-side validation.
            entity.editorFields.fieldsDefinition = [];
            entity.editorFields.fieldsDefinitionKeyPrefix = "autotest-untrusted-prefix";
            const endpoint = operation === "import" ? "editor" : operation;
            let body = endpoint === "editor" ? { action: "edit", data: { [docId]: entity } } : entity;
            if (operation === "import") {
                // Match the importer's row keys and defer validation until its target record is resolved.
                body = { action: "edit", data: { 0: entity }, dztotalchunkcount: 1, dzchunkindex: 0,
                    importMode: "update", updateByColumn: "id", skipWrongData: false,
                    importedColumns: ["id", "title", "tempId", "fieldA"], name: "autotest-jsoneditor.json" };
            }
            const response = await fetch("/admin/rest/web-pages/" + endpoint, {
                method: "POST",
                headers: { "Content-Type": "application/json", "X-CSRF-Token": window.csrfToken },
                body: JSON.stringify(body)
            });
            const result = await response.json();
            return { status: response.status, fields: result.fieldErrors || [], error: result.error };
        }, { docId: docId_2, operation });
        I.assertTrue(rejected.fields.some(field => field.name === "fieldA"), "REST validation must identify the JSON field: " + operation);
        I.assertEqual(formatted, (await getJsonEditorDocument(I, docId_2)).fieldA, "A rejected REST request must not change the page");
    }
    I.assertDeepEqual(historyBefore, await getJsonEditorHistoryIds(I, docId_2), "Rejected JSON must not create a history version");
});

Scenario('JSON editor cleanup', async ({ I, DT, DTE }) => {
    await deleteCustomFieldSettingsByTooltip(I, DT, DTE, jsonEditorMarker);
    if (jsonEditorOriginal == null) return;
    openDocFieldsTab(I, DT, DTE, docId_2);
    I.fillField("#datatableInit_modal #DTE_Field_fieldA", jsonEditorOriginal.fieldA || "");
    I.fillField("#datatableInit_modal #DTE_Field_fieldB", jsonEditorOriginal.fieldB || "");
    DTE.save();
});

async function getJsonEditorDocument(I, docId) {
    return I.executeScript(async docId => {
        const response = await fetch("/admin/rest/web-pages/" + docId, {
            headers: { "X-CSRF-Token": window.csrfToken }
        });
        if (!response.ok) throw new Error("Could not read the autotest document");
        const document = await response.json();
        return { fieldA: document.fieldA, fieldB: document.fieldB };
    }, docId);
}

async function getJsonEditorHistoryIds(I, docId) {
    return I.executeScript(async docId => {
        const response = await fetch("/admin/rest/web-pages/history/all?docId=" + docId, {
            headers: { "X-CSRF-Token": window.csrfToken }
        });
        if (!response.ok) throw new Error("Could not read the autotest document history");
        const history = await response.json();
        if (!Array.isArray(history.content)) throw new Error("Missing document history rows");
        return history.content.map(row => row.id).sort((a, b) => a - b);
    }, docId);
}

async function deleteCustomFieldSettingsByTooltip(I, DT, DTE, marker = tooltipText) {
    I.amOnPage("/admin/v9/settings/custom-fields/");
    DT.filterEquals("tooltip", marker);

    const initialRowsCount = await getCustomFieldSettingsFilteredRowsCount(I);

    for(let deleteAttempt = 0; deleteAttempt < Math.max(initialRowsCount, 1); deleteAttempt++) {
        const rowsCount = await getCustomFieldSettingsFilteredRowsCount(I);

        if(rowsCount < 1) return;

        await I.executeScript(() => {
            if(window.customFieldsDataTable != null && typeof window.customFieldsDataTable.page === "function") {
                window.customFieldsDataTable.page("first").draw(false);
            }
        });
        DT.waitForLoader("customFieldsDataTable");

        I.clickCss("#customFieldsDataTable_wrapper button.buttons-select-all");
        I.clickCss("#customFieldsDataTable_wrapper button.buttons-remove");
        I.waitForElement("div.DTE_Action_Remove", 10);
        I.click("Zmazať", "div.DTE_Action_Remove");
        DTE.waitForLoader();
        DT.waitForLoader("customFieldsDataTable");
    }

    I.assertEqual(0, await getCustomFieldSettingsFilteredRowsCount(I), "All autotest custom field settings must be removed");
}

async function getCustomFieldSettingsFilteredRowsCount(I) {
    return await I.executeScript(() => {
        if(window.customFieldsDataTable != null && typeof window.customFieldsDataTable.page === "function" && typeof window.customFieldsDataTable.page.info === "function") {
            return window.customFieldsDataTable.page.info().recordsDisplay;
        }

        const wrapper = document.querySelector("#customFieldsDataTable_wrapper");
        if(wrapper == null) return 0;
        if(wrapper.querySelector("tbody td.dt-empty, tbody td.dataTables_empty") != null) return 0;
        return wrapper.querySelectorAll("tbody tr").length;
    });
}

function checkDocCustomFields(I, DTE, docId, requiredFields, notRequiredFields) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-fields-tab");

    DTE.save();

    requiredFields.forEach(requiredField => {
        I.seeElement( locate("div.DTE_Field_Name_field" + requiredField + " div.form-text.text-danger").withText("Voliteľné pole je nastavené ako povinné.") );
    });

    notRequiredFields.forEach(notRequiredField => {
        I.dontSeeElement( locate("div.DTE_Field_Name_field" + notRequiredField + " div.form-text.text-danger").withText("Voliteľné pole je nastavené ako povinné.") );
    });
}

async function checkDocCustomFieldText(I, DT, DTE, docId, alphabet, label, maxLength, notExpectedLabel) {
    openDocFieldsTab(I, DT, DTE, docId);

    const fieldSelector = "#datatableInit_modal div.DTE_Field_Name_field" + alphabet;
    I.waitForElement(fieldSelector, 10);
    I.seeElement( locate(fieldSelector + " label").withText(label) );
    I.dontSee(notExpectedLabel, fieldSelector);
    I.seeElementInDOM(fieldSelector + " input#DTE_Field_field" + alphabet + "[type='text'][maxlength='" + maxLength + "']");
    await checkCustomFieldTooltip(I, fieldSelector, tooltipText);

    DTE.cancel();
}

async function checkDocCustomFieldSelect(I, DT, DTE, docId, alphabet, label, expectedOptions, notExpectedLabel) {
    openDocFieldsTab(I, DT, DTE, docId);

    const fieldSelector = "#datatableInit_modal div.DTE_Field_Name_field" + alphabet;
    I.waitForElement(fieldSelector, 10);
    I.seeElement( locate(fieldSelector + " label").withText(label) );
    I.dontSee(notExpectedLabel, fieldSelector);
    I.seeElementInDOM(fieldSelector + " select#DTE_Field_field" + alphabet);

    const optionsPresent = await I.executeScript(({ fieldSelector, expectedOptions }) => {
        const options = Array.from(document.querySelectorAll(fieldSelector + " select option")).map(option => ({
            label: option.textContent.trim(),
            value: option.value
        }));

        return expectedOptions.every(expectedOption => options.some(option =>
            option.label === expectedOption.label && option.value === expectedOption.value
        ));
    }, { fieldSelector, expectedOptions });

    I.assertTrue(optionsPresent, "Entity-specific select options must be applied to the custom field");
    await checkCustomFieldTooltip(I, fieldSelector, tooltipText);

    DTE.cancel();
}

async function checkCustomFieldTooltip(I, fieldSelector, expectedTooltip) {
    const actualTooltip = await I.executeScript((fieldSelector) => {
        const tooltip = document.querySelector(fieldSelector + " button.btn-tooltip");
        if(tooltip == null) return null;

        return tooltip.getAttribute("title") || tooltip.getAttribute("data-bs-original-title") || tooltip.getAttribute("data-original-title");
    }, fieldSelector);

    const sanitizedTooltip = actualTooltip?.replace(/(?:<br\s*\/?>\s*)+$/gi, "");
    I.assertEqual(expectedTooltip, sanitizedTooltip, "Custom field tooltip must use configured value");
}

function openDocFieldsTab(I, DT, DTE, docId) {
    I.amOnPage("/admin/v9/webpages/web-pages-list/?docid=" + docId);
    DTE.waitForEditor();
    I.clickCss("#pills-dt-datatableInit-fields-tab");
    DT.waitForLoader();
}

async function checkRenderedCustomFieldType(I, fieldDefinition) {
    const alphabet = fieldDefinition.alphabet;
    const fieldSelector = "#datatableInit_modal div.DTE_Field_Name_field" + alphabet;
    const inputId = "DTE_Field_field" + alphabet;

    if(fieldDefinition.type === "none") {
        const hiddenFieldState = await I.executeScript(({ fieldSelector }) => {
            const field = document.querySelector(fieldSelector);
            if(field == null) return null;

            const rect = field.getBoundingClientRect();
            const label = field.querySelector("label");

            return {
                label: label == null ? "" : label.textContent.trim(),
                hidden: rect.width === 0 && rect.height === 0
            };
        }, { fieldSelector });

        I.assertEqual(fieldDefinition.label, hiddenFieldState.label, "Hidden custom field label must still be configured");
        I.assertTrue(hiddenFieldState.hidden, "Custom field type none must be hidden");
        return;
    }

    I.waitForElement(fieldSelector, 10);
    I.seeElement( locate(fieldSelector + " label").withText(fieldDefinition.label) );

    if(fieldDefinition.type === "text") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text'][maxlength='14'][data-warningLength='8']");
        await checkWarningMessageIsSafe(I, fieldSelector, fieldDefinition.settings.warningText);
    } else if(fieldDefinition.type === "textarea") {
        I.seeElementInDOM(fieldSelector + " textarea#" + inputId);
    } else if(fieldDefinition.type === "select") {
        I.seeElementInDOM(fieldSelector + " select#" + inputId);
        if(fieldDefinition.settings?.optionsSource === "enumeration") {
            await checkSelectHasOptions(I, fieldSelector, "enumeration-backed select");
        } else {
            await checkSelectOptions(I, fieldSelector, allTypesSelectOptions);
        }
    } else if(fieldDefinition.type === "multiselect") {
        I.seeElementInDOM(fieldSelector + " select#" + inputId + "[multiple]");
        await checkSelectOptions(I, fieldSelector, allTypesMultiOptions);
    } else if(fieldDefinition.type === "radio" || fieldDefinition.type === "checkbox") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + ".custom-field-choice-value[type='hidden']");
        I.waitForElement(fieldSelector + " input.custom-field-choice-option[type='" + fieldDefinition.type + "']", 10);

        if(fieldDefinition.settings?.optionsSource === "enumeration") {
            await checkChoiceHasOptions(I, fieldSelector, fieldDefinition.type);
        } else {
            await checkChoiceOptions(I, fieldSelector, fieldDefinition.type, fieldDefinition.settings.selectOptions);
        }
    } else if(fieldDefinition.type === "boolean") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='checkbox']");
    } else if(fieldDefinition.type === "number") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='number']");
    } else if(fieldDefinition.type === "date") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text'][autocomplete='off']");
    } else if(fieldDefinition.type === "autocomplete") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + ".autocomplete[name='field" + alphabet + "']");
        I.seeElementInDOM(fieldSelector + " .input-group-text i.ti-search");
    } else if(fieldDefinition.type === "image") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text']");
        I.seeElementInDOM(fieldSelector + " .input-group-text i.ti-photo");
        I.seeElementInDOM(fieldSelector + " button i.ti-focus-2");
    } else if(fieldDefinition.type === "link") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text']");
        I.seeElementInDOM(fieldSelector + " button i.ti-focus-2");
    } else if(fieldDefinition.type === "json_group" || fieldDefinition.type === "json_doc" || fieldDefinition.type === "dir") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text']");
        I.seeElementInDOM(fieldSelector + " div.webjet-component#" + inputId);
    } else if(fieldDefinition.type === "docsIn") {
        I.seeElementInDOM(fieldSelector + " select#" + inputId);
        await checkSelectHasOptions(I, fieldSelector, fieldDefinition.type);
    } else if(fieldDefinition.type === "uuid") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + ".field-type-uuid[maxlength='255']");
    } else if(fieldDefinition.type === "color") {
        I.seeElementInDOM(fieldSelector + " input#" + inputId + "[type='text']");
        I.seeElementInDOM(fieldSelector + " .color-preview");
        I.seeElementInDOM(fieldSelector + " color-picker#" + inputId + "_picker");
    } else {
        I.assertTrue(false, "Unsupported custom field type in test: " + fieldDefinition.type);
    }
}

async function checkWarningMessageIsSafe(I, fieldSelector, expectedWarningMessage) {
    const expectedEscapedWarningMessage = expectedWarningMessage
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
    const getSecurityState = () => I.executeScript(({ fieldSelector, warningXssMarkerId }) => {
        const input = document.querySelector(fieldSelector + " input[data-warningmessage]");
        const warningToast = document.querySelector(".toast-warning .toast-title");

        return {
            warningMessage: input?.getAttribute("data-warningmessage"),
            injectedElementPresent: document.getElementById(warningXssMarkerId) != null,
            payloadExecuted: document.body.dataset.customFieldsWarningXss === "1",
            warningToastText: warningToast?.textContent
        };
    }, { fieldSelector, warningXssMarkerId });

    const editorSecurityState = await getSecurityState();
    I.assertEqual(expectedEscapedWarningMessage, editorSecurityState.warningMessage, "Warning message must remain an input data attribute");
    I.assertFalse(editorSecurityState.injectedElementPresent, "Warning message must not inject an HTML element into the editor");
    I.assertFalse(editorSecurityState.payloadExecuted, "Warning message event handler must not execute in the editor");

    I.fillField(fieldSelector + " input[data-warningmessage]", "autotest-warning-trigger");
    I.waitForElement(".toast-warning .toast-title", 10);

    const toastSecurityState = await getSecurityState();
    I.assertEqual(expectedWarningMessage, toastSecurityState.warningToastText, "Warning toast must display the configured warning as text");
    I.assertFalse(toastSecurityState.injectedElementPresent, "Warning message must not inject an HTML element into the toast");
    I.assertFalse(toastSecurityState.payloadExecuted, "Warning message event handler must not execute in the toast");
}

async function checkSelectOptions(I, fieldSelector, expectedOptions) {
    const optionsPresent = await I.executeScript(({ fieldSelector, expectedOptions }) => {
        const options = Array.from(document.querySelectorAll(fieldSelector + " select option")).map(option => ({
            label: option.textContent.trim(),
            value: option.value
        }));

        return expectedOptions.every(expectedOption => options.some(option =>
            option.label === expectedOption.label && option.value === expectedOption.value
        ));
    }, { fieldSelector, expectedOptions });

    I.assertTrue(optionsPresent, "Custom field select options must match configured values");
}

async function checkSelectHasOptions(I, fieldSelector, type) {
    const optionCount = await I.executeScript((fieldSelector) => {
        return Array.from(document.querySelectorAll(fieldSelector + " select option"))
            .filter(option => option.textContent.trim().length > 0 || option.value.length > 0)
            .length;
    }, fieldSelector);

    I.assertAbove(optionCount, 0, "Custom field type " + type + " must render available options");
}

async function checkChoiceOptions(I, fieldSelector, type, expectedOptions) {
    const optionsPresent = await I.executeScript(({ fieldSelector, type, expectedOptions }) => {
        const options = Array.from(document.querySelectorAll(fieldSelector + " input.custom-field-choice-option[type='" + type + "']")).map(input => ({
            label: input.closest(".form-check")?.querySelector("label")?.textContent.trim() || "",
            value: input.value
        }));

        return expectedOptions.every(expectedOption => options.some(option =>
            option.label === expectedOption.label && option.value === expectedOption.value
        ));
    }, { fieldSelector, type, expectedOptions });

    I.assertTrue(optionsPresent, "Custom field " + type + " options must match configured values");
}

async function checkChoiceHasOptions(I, fieldSelector, type) {
    const optionCount = await I.executeScript(({ fieldSelector, type }) => {
        return document.querySelectorAll(fieldSelector + " input.custom-field-choice-option[type='" + type + "']").length;
    }, { fieldSelector, type });

    I.assertAbove(optionCount, 0, "Enumeration-backed custom field type " + type + " must render available options");
}

function addCustomFieldSetting(I, DTE, className, alphabet, entityId, isRequired, seeError = null, bonusClassName = null, bonusEntityId = null, Document = null, fieldSettings = {}) {
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("customFieldsDataTable");
    I.fillField("#DTE_Field_className", className);
    DTE.selectOption("alphabet", alphabet.toUpperCase());

    if(fieldSettings.type != null) {
        setCustomFieldType(I, fieldSettings.type);
    }

    if(fieldSettings.label != null) {
        I.fillField("#DTE_Field_label", fieldSettings.label);
    }

    if(fieldSettings.optionsSource != null) {
        I.waitForVisible("div.DTE_Field_Name_optionsSource", 10);
        setCustomFieldOptionsSource(I, fieldSettings.optionsSource);
    }

    if(fieldSettings.textMaxLength != null) {
        I.waitForVisible("div.DTE_Field_Name_textMaxLength", 10);
        I.fillField("#DTE_Field_textMaxLength", String(fieldSettings.textMaxLength));
    }

    if(fieldSettings.textWarningLength != null) {
        I.waitForVisible("div.DTE_Field_Name_textWarningLength", 10);
        I.fillField("#DTE_Field_textWarningLength", String(fieldSettings.textWarningLength));
    }

    if(fieldSettings.warningText != null) {
        I.waitForVisible("div.DTE_Field_Name_warningText", 10);
        I.fillField("#DTE_Field_warningText", fieldSettings.warningText);
    }

    if(fieldSettings.selectOptions != null) {
        fillCustomFieldSelectOptions(I, fieldSettings.selectOptions);
    }

    if(fieldSettings.docInGroup != null) {
        I.waitForVisible("div.DTE_Field_Name_docInGroup", 10);
        setCustomFieldsEditorValue(I, "docInGroup", fieldSettings.docInGroup);
    }

    if(fieldSettings.enumeration != null) {
        I.waitForVisible("div.DTE_Field_Name_enumeration", 10);
        setCustomFieldsEditorValue(I, "enumeration", fieldSettings.enumeration);
    }

    if(entityId !== null) {
        I.fillField("#DTE_Field_entityId", entityId);
    }

    if(isRequired === true) {
        I.checkOption("#DTE_Field_required_0");
    } else {
        I.uncheckOption("#DTE_Field_required_0");
    }

    if (bonusClassName !== null) {
        I.clickCss("#pills-dt-customFieldsDataTable-bonus-tab");
        I.fillField("#DTE_Field_bonusClassName", bonusClassName);
        I.fillField("#DTE_Field_bonusEntityId", bonusEntityId);

        if (Document != null) Document.screenshot("/frontend/webpages/customfields/custom-fields-settings-editor-bonus.png");

        I.clickCss("#pills-dt-customFieldsDataTable-basic-tab");
    }

    I.fillField("#DTE_Field_tooltip", fieldSettings.tooltip || tooltipText);

    DTE.save();

    if(seeError === null) {
        I.dontSeeElement(".DTE_Form_Error");
    } else {
        I.see(seeError);
    }
}

function setCustomFieldType(I, type) {
    I.executeScript((type) => {
        const typeSelect = document.getElementById("DTE_Field_type");
        typeSelect.value = type;
        typeSelect.dispatchEvent(new Event("change", { bubbles: true }));

        if (window.$ && window.$(typeSelect).selectpicker) {
            window.$(typeSelect).selectpicker("refresh");
        }
    }, type);
}

function setCustomFieldOptionsSource(I, source) {
    I.checkOption("div.DTE_Field_Name_optionsSource input[value='" + source + "']");
}

function setCustomFieldsEditorValue(I, fieldName, value) {
    I.executeScript(({ fieldName, value }) => {
        window.customFieldsDataTable.EDITOR.field(fieldName).set(value);
    }, { fieldName, value });
}

function fillCustomFieldSelectOptions(I, options) {
    I.waitForVisible("div.DTE_Field_Name_selectOptions", 10);

    options.forEach((option, index) => {
        if(index > 0) {
            I.clickCss("div.DTE_Field_Name_selectOptions button.options-add-btn");
        }

        const rowSelector = "div.DTE_Field_Name_selectOptions .options-inputs .options-input-row:nth-child(" + (index + 1) + ")";
        I.fillField(rowSelector + " input.options-value-1", option.label);
        I.fillField(rowSelector + " input.options-value-2", option.value);
    });
}
