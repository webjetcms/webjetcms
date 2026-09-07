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

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
Pri práci s číselníkmi nemusí každé textové pole zostať obyčajným vstupom na voľné písanie. Takéto polia často vedú k nejednotným hodnotám a zbytočným chybám. WebJET CMS preto prináša nastavenie typov reťazcových polí podobne, ako ho poznáte z voliteľných polí.

Najskôr vytvoríte alebo upravíte typ číselníka a pomenujete potrebné reťazcové polia. Po prvom uložení sa zobrazí nová karta Typy reťazcových polí. V prehľadnej tabuľke hneď vidíte, aké ovládanie používa každé pomenované pole.

Z bežného textu môžete vytvoriť výberové pole, výber viacerých možností alebo automatické dopĺňanie. Pole môžete prepojiť s iným číselníkom. K dispozícii je aj výber obrázka, odkazu, priečinka alebo webovej stránky.

Ku každému poľu nastavíte povinné vyplnenie, pomocný text a vlastnosti zodpovedajúce zvolenému typu. Pri výberových poliach určíte statické možnosti alebo použijete hodnoty z číselníka.

Keď redaktor pridáva údaje, WebJET CMS túto konfiguráciu automaticky prenesie do formulára. Namiesto voľného textu zobrazí správny ovládací prvok a dostupné možnosti. Nepomenované polia zostanú skryté. Pomenované pole bez osobitného nastavenia ostane bežným textovým poľom.

Výsledkom je rýchlejšie zadávanie, jednotnejšie údaje a menej chýb bez potreby meniť formulár programovaním. Viac informácií a prehľad podporovaných typov nájdete v dokumentácii WebJET CMS v kapitole Číselníky.
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
0:00-0:06 - MANUAL: titulná karta „Číselníky - typy reťazcových polí“ s logom WebJET CMS.
0:06-0:19 - Zoznam typov číselníkov: vyhľadať a ukázať typ CustomFieldsScreenshots s pomenovanými stĺpcami.
0:19-0:31 - V editore typu otvoriť kartu Reťazce a ukázať názvy City, Street, Image a Something.
0:31-0:45 - Otvoriť kartu Typy reťazcových polí a ukázať prehľad nakonfigurovaných typov.
0:45-1:00 - Otvoriť konfiguráciu poľa City a ukázať typ poľa, povinnosť, tooltip a zdroj možností z číselníka.
1:00-1:11 - V zozname dát číselníkov vybrať typ CustomFieldsScreenshots.
1:11-1:23 - Otvoriť pridanie záznamu a ukázať vygenerované ovládacie prvky City, Street, Image a Something.
1:23-1:30 - MANUAL: záverečná karta s odkazom na kapitolu Číselníky v dokumentácii WebJET CMS.
`);
});

Scenario("289-ciselniky-typ-pola-podobne-ako-pre-volitelne-polia", ({ I, DT, DTE, login }) => {
    login("admin");
    I.amOnPage("/apps/enumeration/admin/enumeration-type/");
    I.waitForVisible(`${enumerationTypeWrapper} table`, 20);
    DT.waitForLoader(enumerationTypeTableId);

    // Shot 1: start from the existing enumeration type used by documentation screenshots.
    DT.filterContains("typeName", enumerationTypeName);
    I.waitForText(enumerationTypeName, 10, `${enumerationTypeTable} tbody`);
    I.wait(5);

    // Shot 2: show the named string fields that define the data columns.
    I.videoClick(locate(`${enumerationTypeTable} tbody td`).withText(enumerationTypeName));
    DTE.waitForEditor(enumerationTypeTableId);
    I.videoClick("#pills-dt-enumerationTypeDataTable-strings-tab");
    I.waitForVisible("#DTE_Field_string1Name", 10);
    I.seeInField("#DTE_Field_string1Name", "City");
    I.seeInField("#DTE_Field_string2Name", "Street");
    I.wait(7);

    // Shot 3: reveal the field-type configurations for the named strings.
    I.videoClick("#pills-dt-enumerationTypeDataTable-stringFieldTypes-tab");
    I.waitForVisible(stringFieldsWrapper, 10);
    DT.waitForLoader(stringFieldsTableId);
    const cityFieldRow = `${stringFieldsTable} tbody tr:first-child`;
    const imageFieldRow = `${stringFieldsTable} tbody tr:nth-child(3)`;
    I.waitForVisible(cityFieldRow, 10);
    I.see("Reťazec 1", cityFieldRow);
    I.see("City", cityFieldRow);
    I.waitForVisible(imageFieldRow, 10);
    I.see("Výber obrázka", imageFieldRow);
    I.wait(8);

    // Shot 4: inspect one configuration without saving any changes.
    I.videoClick(`${stringFieldsTable} tbody tr:first-child td:first-child`);
    I.waitForElement(`${stringFieldsTable} tbody tr.selected`, 10);
    I.videoClick(`${stringFieldsWrapper} button.buttons-edit`);
    DTE.waitForEditor(stringFieldsTableId);
    I.waitForText("Upraviť: Reťazec 1", 10, `${stringFieldsModal} div.DTE_Header`);
    I.waitForText("City", 10, `${stringFieldsModal} div.DTE_Header`);
    const fieldTypeSelect = `${stringFieldsModal} div.DTE_Field_Name_type button.dropdown-toggle`;
    I.waitForVisible(fieldTypeSelect, 10);
    I.waitForVisible(`${stringFieldsModal} div.DTE_Field_Name_optionsSource`, 10);
    I.wait(6);

    I.videoClick(fieldTypeSelect);
    I.waitForVisible("div.dropdown-menu.show", 5);
    I.wait(4);
    I.pressKey("Escape");
    I.wait(3);

    DTE.cancel(stringFieldsTableId, true);
    DTE.cancel(enumerationTypeTableId, true);

    // Shot 5: choose the configured enumeration in the data view.
    I.amOnPage("/apps/enumeration/admin/");
    I.waitForVisible(`${enumerationDataWrapper} table`, 20);
    DT.waitForLoader(enumerationDataTableId);
    I.waitForVisible(enumerationTypePicker, 10);
    I.videoClick(enumerationTypePicker);
    const enumerationSelect = "body > div.bs-container.dropdown.bootstrap-select.form-select";
    const enumerationSearch = `${enumerationSelect} div.bs-searchbox > input`;
    I.waitForVisible(enumerationSearch, 10);
    I.videoClick(enumerationSearch);
    I.fillField(enumerationSearch, enumerationTypeName);
    const enumerationOption = locate(`${enumerationSelect} a[role='option'] > span`).withText(enumerationTypeName);
    I.waitForVisible(enumerationOption, 10);
    I.videoClick(enumerationOption);
    DT.waitForLoader(enumerationDataTableId);
    I.waitForText(enumerationTypeName, 10, enumerationTypePicker);
    I.wait(6);

    // Shot 6: show how the saved configuration becomes the data-entry form.
    I.waitForVisible(enumerationDataCreateButton, 10);
    I.videoClick(enumerationDataCreateButton);
    DTE.waitForEditor(enumerationDataTableId);
    I.waitForElement(locate("label[for='DTE_Field_fieldA']").withText("City"), 10);
    I.waitForElement("select#DTE_Field_fieldA", 10);
    I.waitForElement(locate("label[for='DTE_Field_fieldB']").withText("Street"), 10);
    I.waitForElement(locate("label[for='DTE_Field_fieldC']").withText("Image"), 10);
    I.wait(12);
}).tag("@video");
