Feature('settings.custom-fields');

Before(({ I, login }) =>{
    login('admin');
});

const docId_1 = "163799";
const docId_2 = "163800";
const docClass = "sk.iway.iwcm.doc.DocDetails";

const templateId = "4319";
const templateClass = "sk.iway.iwcm.doc.TemplateDetails";
const tooltipText = "autotest-custom-fields-required";
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
const allTypesGroup = { groupId: 67, fullPath: "/Test stavov" };
const allTypesEnumeration = "enumeration-options|2|string1|string1";
const allTypesDefinitions = [
    { alphabet: "A", type: "text", label: "autotest-custom-type-text", settings: { textMaxLength: 14, textWarningLength: 8 } },
    { alphabet: "B", type: "textarea", label: "autotest-custom-type-textarea" },
    { alphabet: "C", type: "select", label: "autotest-custom-type-select", settings: { selectOptions: allTypesSelectOptions } },
    { alphabet: "D", type: "multiselect", label: "autotest-custom-type-multiselect", settings: { selectOptions: allTypesMultiOptions } },
    { alphabet: "E", type: "boolean", label: "autotest-custom-type-boolean" },
    { alphabet: "F", type: "number", label: "autotest-custom-type-number" },
    { alphabet: "G", type: "date", label: "autotest-custom-type-date" },
    { alphabet: "H", type: "none", label: "autotest-custom-type-none" },
    { alphabet: "J", type: "autocomplete", label: "autotest-custom-type-autocomplete" },
    { alphabet: "K", type: "image", label: "autotest-custom-type-image" },
    { alphabet: "L", type: "link", label: "autotest-custom-type-link" },
    { alphabet: "M", type: "json_group", label: "autotest-custom-type-json-group" },
    { alphabet: "N", type: "json_doc", label: "autotest-custom-type-json-doc" },
    { alphabet: "O", type: "dir", label: "autotest-custom-type-dir" },
    { alphabet: "P", type: "docsIn", label: "autotest-custom-type-docsin", settings: { docInGroup: allTypesGroup } },
    { alphabet: "Q", type: "enumeration", label: "autotest-custom-type-enumeration", settings: { enumeration: allTypesEnumeration } },
    { alphabet: "R", type: "uuid", label: "autotest-custom-type-uuid" },
    { alphabet: "S", type: "color", label: "autotest-custom-type-color" }
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
    addCustomFieldSetting(I, DTE, docClass, requiredOverrideAlphabet.toLowerCase(), null, false, "Kombinácia polí „Použiť pre entitu“, „Označenie poľa“ a „ID entity“ musí byť jedinečná. Zadajte inú hodnotu pre jedno z týchto polí.");

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

    Document.screenshotElement("div.DTE_Action_Create", "/frontend/webpages/customfields/custom-fields-settings-editor.png");

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
    checkDocCustomFieldText(I, DT, DTE, docId_2, overrideAlphabet, overrideGlobalLabel, 12, overrideSpecificLabel);
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

async function deleteCustomFieldSettingsByTooltip(I, DT, DTE) {
    I.amOnPage("/admin/v9/settings/custom-fields/");
    DT.filterEquals("tooltip", tooltipText);

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

function checkDocCustomFieldText(I, DT, DTE, docId, alphabet, label, maxLength, notExpectedLabel) {
    openDocFieldsTab(I, DT, DTE, docId);

    const fieldSelector = "#datatableInit_modal div.DTE_Field_Name_field" + alphabet;
    I.waitForElement(fieldSelector, 10);
    I.seeElement( locate(fieldSelector + " label").withText(label) );
    I.dontSee(notExpectedLabel, fieldSelector);
    I.seeElementInDOM(fieldSelector + " input#DTE_Field_field" + alphabet + "[type='text'][maxlength='" + maxLength + "']");

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

    DTE.cancel();
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
    } else if(fieldDefinition.type === "textarea") {
        I.seeElementInDOM(fieldSelector + " textarea#" + inputId);
    } else if(fieldDefinition.type === "select") {
        I.seeElementInDOM(fieldSelector + " select#" + inputId);
        await checkSelectOptions(I, fieldSelector, allTypesSelectOptions);
    } else if(fieldDefinition.type === "multiselect") {
        I.seeElementInDOM(fieldSelector + " select#" + inputId + "[multiple]");
        await checkSelectOptions(I, fieldSelector, allTypesMultiOptions);
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
        I.seeElementInDOM(fieldSelector + " div.vueComponent#" + inputId);
    } else if(fieldDefinition.type === "docsIn" || fieldDefinition.type === "enumeration") {
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

    if(fieldSettings.textMaxLength != null) {
        I.waitForVisible("div.DTE_Field_Name_textMaxLength", 10);
        I.fillField("#DTE_Field_textMaxLength", String(fieldSettings.textMaxLength));
    }

    if(fieldSettings.textWarningLength != null) {
        I.waitForVisible("div.DTE_Field_Name_textWarningLength", 10);
        I.fillField("#DTE_Field_textWarningLength", String(fieldSettings.textWarningLength));
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

    I.fillField("#DTE_Field_tooltip", tooltipText);

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
