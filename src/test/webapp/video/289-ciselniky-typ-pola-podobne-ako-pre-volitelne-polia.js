Feature("video.289-58641-ciselniky-typ-pola-podobne-ako-pre-volitelne-polia");

const enumerationTypeName = "CustomFieldsScreenshots";
const enumerationTypeTableId = "enumerationTypeDataTable";
const enumerationTypeTable = `#${enumerationTypeTableId}`;
const enumerationTypeWrapper = `${enumerationTypeTable}_wrapper`;
const stringFieldsTableId = "datatableFieldDTE_Field_editorFields-stringFieldTypes";
const stringFieldsTable = `#${stringFieldsTableId}`;
const stringFieldsWrapper = `${stringFieldsTable}_wrapper`;
const stringFieldsModal = `${stringFieldsTable}_modal`;
const enumerationDataTableId = "enumerationDataDataTable";
const enumerationDataWrapper = `#${enumerationDataTableId}_wrapper`;
const enumerationTypePicker = `${enumerationDataWrapper} .dt-buttons div.bootstrap-select button.dropdown-toggle`;
const enumerationDataCreateButton = `${enumerationDataWrapper} .dt-buttons button.buttons-create`;

// Move whole shot objects to reorder narration, slates and browser actions.
const videoPlan = {
    "language": "sk",
    "notes": "Durations estimate the edited narration. Cut setup, cleanup and slates; use the runner's transition holds when editing.",
    "shots": [
        {
            "id": "intro",
            "type": "manual",
            "durationSeconds": 6,
            "title": "Typed enumeration fields",
            "text-sk": "Pri práci s číselníkmi nemusí každé textové pole zostať obyčajným vstupom na voľné písanie.",
            "notes": "Create a title card: Číselníky - typy reťazcových polí, with the WebJET CMS logo."
        },
        {
            "id": "type-list",
            "type": "auto",
            "durationSeconds": 13,
            "title": "Find the existing enumeration type",
            "text-sk": "Takéto polia často vedú k nejednotným hodnotám a zbytočným chybám. WebJET CMS preto prináša nastavenie typov reťazcových polí podobne, ako ho poznáte z voliteľných polí.",
            "notes": "Filter the documentation fixture without changing saved configuration.",
            shot: async ({ I, DT }) => {
                DT.filterContains("typeName", enumerationTypeName);
                await I.waitForText(enumerationTypeName, 10, `${enumerationTypeTable} tbody`);
                await I.wait(5);
            }
        },
        {
            "id": "named-fields",
            "type": "auto",
            "durationSeconds": 12,
            "title": "Name the string fields",
            "text-sk": "Najskôr vytvoríte alebo upravíte typ číselníka a pomenujete potrebné reťazcové polia.",
            "notes": "Show the saved City and Street field names.",
            prepare: async ({ openType }) => {
                await openType();
            },
            shot: async ({ I }) => {
                await I.videoClick("#pills-dt-enumerationTypeDataTable-strings-tab");
                await I.waitForVisible("#DTE_Field_string1Name", 10);
                await I.seeInField("#DTE_Field_string1Name", "City");
                await I.seeInField("#DTE_Field_string2Name", "Street");
                await I.wait(7);
            }
        },
        {
            "id": "field-types",
            "type": "auto",
            "durationSeconds": 14,
            "title": "Inspect configured field types",
            "text-sk": "Po prvom uložení sa zobrazí nová karta Typy reťazcových polí. V prehľadnej tabuľke hneď vidíte, aké ovládanie používa každé pomenované pole.\n\nZ bežného textu môžete vytvoriť výberové pole, výber viacerých možností alebo automatické dopĺňanie. Pole môžete prepojiť s iným číselníkom. K dispozícii je aj výber obrázka, odkazu, priečinka alebo webovej stránky.",
            "notes": "Show the field-type table and image picker configuration.",
            prepare: async ({ openType }) => {
                await openType();
            },
            shot: async ({ I, DT }) => {
                await I.videoClick("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
                await I.waitForVisible(stringFieldsWrapper, 10);
                DT.waitForLoader(stringFieldsTableId);
                const cityFieldRow = `${stringFieldsTable} tbody tr:first-child`;
                const imageFieldRow = `${stringFieldsTable} tbody tr:nth-child(3)`;
                await I.waitForVisible(cityFieldRow, 10);
                await I.see("Reťazec 1", cityFieldRow);
                await I.see("City", cityFieldRow);
                await I.waitForVisible(imageFieldRow, 10);
                await I.see("Výber obrázka", imageFieldRow);
                await I.wait(8);
            }
        },
        {
            "id": "city-options",
            "type": "auto",
            "durationSeconds": 15,
            "title": "Inspect City options without saving",
            "text-sk": "Ku každému poľu nastavíte povinné vyplnenie, pomocný text a vlastnosti zodpovedajúce zvolenému typu. Pri výberových poliach určíte statické možnosti alebo použijete hodnoty z číselníka.",
            "notes": "Show required state, tooltip, option source and the list of supported types.",
            prepare: async ({ I, DT, openType }) => {
                await openType();
                await I.clickCss("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
                await I.waitForVisible(stringFieldsWrapper, 10);
                DT.waitForLoader(stringFieldsTableId);
                await I.waitForVisible(`${stringFieldsTable} tbody tr:first-child`, 10);
            },
            shot: async ({ I, DTE }) => {
                await I.videoClick(`${stringFieldsTable} tbody tr:first-child td:first-child`);
                await I.waitForElement(`${stringFieldsTable} tbody tr.selected`, 10);
                await I.videoClick(`${stringFieldsWrapper} button.buttons-edit`);
                DTE.waitForEditor(stringFieldsTableId);
                await I.waitForText("Upraviť: Reťazec 1", 10, `${stringFieldsModal} div.DTE_Header`);
                await I.waitForText("City", 10, `${stringFieldsModal} div.DTE_Header`);
                const fieldTypeSelect = `${stringFieldsModal} div.DTE_Field_Name_type button.dropdown-toggle`;
                await I.waitForVisible(fieldTypeSelect, 10);
                await I.waitForVisible(`${stringFieldsModal} div.DTE_Field_Name_optionsSource`, 10);
                await I.wait(6);
                await I.videoClick(fieldTypeSelect);
                await I.waitForVisible("div.dropdown-menu.show", 5);
                await I.wait(4);
                await I.pressKey("Escape");
                await I.wait(3);
            }
        },
        {
            "id": "choose-type",
            "type": "auto",
            "durationSeconds": 11,
            "title": "Choose the configured enumeration",
            "text-sk": "Keď redaktor pridáva údaje, WebJET CMS túto konfiguráciu automaticky prenesie do formulára.",
            "notes": "Select CustomFieldsScreenshots in the data view.",
            shot: async ({ chooseType }) => {
                await chooseType(true);
            }
        },
        {
            "id": "data-form",
            "type": "auto",
            "durationSeconds": 12,
            "title": "Show the generated data-entry form",
            "text-sk": "Namiesto voľného textu zobrazí správny ovládací prvok a dostupné možnosti. Nepomenované polia zostanú skryté. Pomenované pole bez osobitného nastavenia ostane bežným textovým poľom.\n\nVýsledkom je rýchlejšie zadávanie, jednotnejšie údaje a menej chýb bez potreby meniť formulár programovaním.",
            "notes": "Open a new unsaved record and inspect the generated fields.",
            prepare: async ({ chooseType }) => {
                await chooseType(false);
            },
            shot: async ({ I, DTE }) => {
                await I.videoClick(enumerationDataCreateButton);
                DTE.waitForEditor(enumerationDataTableId);
                await I.waitForElement(locate("label[for='DTE_Field_fieldA']").withText("City"), 10);
                await I.waitForElement("select#DTE_Field_fieldA", 10);
                await I.waitForElement(locate("label[for='DTE_Field_fieldB']").withText("Street"), 10);
                await I.waitForElement(locate("label[for='DTE_Field_fieldC']").withText("Image"), 10);
                await I.wait(12);
            }
        },
        {
            "id": "documentation",
            "type": "auto",
            "durationSeconds": 7,
            "title": "Enumeration documentation",
            "text-sk": "Viac informácií a prehľad podporovaných typov nájdete v dokumentácii WebJET CMS v kapitole Číselníky.",
            "notes": "Scroll the documentation; add the closing caption during editing.",
            shot: async ({ I }) => {
                await I.videoDocumentation("https://docs.webjetcms.sk/latest/sk/redactor/apps/enumeration/README");
                await I.wait(5);
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

Scenario("289-ciselniky-typ-pola-podobne-ako-pre-volitelne-polia", async ({ I, DT, DTE, login }) => {
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const openType = async () => {
        DT.filterContains("typeName", enumerationTypeName);
        await I.waitForText(enumerationTypeName, 10, `${enumerationTypeTable} tbody`);
        await I.click(locate(`${enumerationTypeTable} tbody td`).withText(enumerationTypeName));
        DTE.waitForEditor(enumerationTypeTableId);
    };
    const chooseType = async animated => {
        const click = async locator => animated ? I.videoClick(locator) : I.click(locator);
        await click(enumerationTypePicker);
        const enumerationSelect = "body > div.bs-container.dropdown.bootstrap-select.form-select";
        const enumerationSearch = `${enumerationSelect} div.bs-searchbox > input`;
        await I.waitForVisible(enumerationSearch, 10);
        await click(enumerationSearch);
        await I.fillField(enumerationSearch, enumerationTypeName);
        const option = locate(`${enumerationSelect} a[role='option'] > span`).withText(enumerationTypeName);
        await I.waitForVisible(option, 10);
        await click(option);
        DT.waitForLoader(enumerationDataTableId);
        await I.waitForText(enumerationTypeName, 10, enumerationTypePicker);
        await I.waitForVisible(enumerationDataCreateButton, 10);
        if (animated) await I.wait(6);
    };
    await recordVideoPlan(I, {
        plan: videoPlan,
        context: { DT, DTE, openType, chooseType },
        setup: async () => { login("admin"); },
        prepare: async shot => {
            if (shot.id === "documentation") return;
            const dataView = ["choose-type", "data-form"].includes(shot.id);
            await I.amOnPage(dataView ? "/apps/enumeration/admin/" : "/apps/enumeration/admin/enumeration-type/");
            await I.waitForVisible(`${dataView ? enumerationDataWrapper : enumerationTypeWrapper} table`, 20);
            DT.waitForLoader(dataView ? enumerationDataTableId : enumerationTypeTableId);
            if (dataView) await I.waitForVisible(enumerationTypePicker, 10);
        },
        cleanup: async shot => {
            if (shot.id === "city-options") {
                DTE.cancel(stringFieldsTableId, true);
                await I.waitForInvisible(`${stringFieldsModal}.show`, 10);
            }
            if (["named-fields", "field-types", "city-options"].includes(shot.id)) {
                DTE.cancel(enumerationTypeTableId, true);
                await I.waitForInvisible(`${enumerationTypeTable}_modal.show`, 10);
            }
            if (shot.id === "data-form") {
                DTE.cancel(enumerationDataTableId, true);
                await I.waitForInvisible(`#${enumerationDataTableId}_modal.show`, 10);
            }
        }
    });
}).tag("@video");
