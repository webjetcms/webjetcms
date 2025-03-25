# Datatables EDITOR

[Editor pro datatables](http://editor.datatables.net) je rozšíření pro datatables.net. Pro zobrazení dokumentace musíte být přihlášen na stránce editor.datatables.net, jinak neuvidíte kompletní dokumentaci.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Datatables EDITOR](#datatables-editor)
  - [Karty v editoru](#karty-v-editoru)
  - [Možnosti konfigurace columns objektu](#možnosti-konfigurace-columns-objektu)
  - [Události](#události)
  - [Stylování](#stylování)
  - [Speciální tlačítka](#speciální-tlačítka)
  - [Ukázky kódu](#ukázky-kódu)
    - [Dynamická změna hodnot ve výběrovém poli](#dynamická-změna-hodnot-ve-výběrovém-poli)
    - [Získání JSON objektu před editací](#získání-json-objektu-před-editací)
    - [Programové otevření editoru](#programové-otevření-editoru)
    - [Podmíněné zobrazení vstupního pole](#podmíněné-zobrazení-vstupního-pole)
    - [API funkce](#api-funkce)

<!-- /code_chunk_output -->

## Karty v editoru

WebJET verze Editoru má implementovanou podporu karet. Každá karta musí mít unikátní `id`, na které se v columns definici v části editor nastavuje karta, do které se přesune dané pole. Pokud hodnota `tab` v columns definici není nastavena pole se přesune do první karty.

Volitelně může `tabs` definice obsahovat atribut `content` s HTML kódem který se vloží do karty. V takovém případě již nesmí obsahovat pole z `columns` definice. Zároveň se takto definované kartě zruší odsazení a `before` element s šedým pasem, obsah karty je na celou šířku a případné odsazení je třeba si řešit v custom CSS.

Možné atributy:
- `selected` definuje výchozí zobrazenou kartu, měl by být nastaven pouze na jedné kartě.
- `className` umožňuje doplnit CSS styl k přepínači karet. Předdefinovány jsou CSS styly `hide-on-create` pro schování karty při pořizování nového záznamu a `hide-on-edit` pro schování karty při editaci již existujícího záznamu, `hide-on-duplicate` pro schování karty při duplikování.
- `perms` umožňuje nezobrazit kartu pokud uživatel nemá zadané právo (pozor, znak `.` v právu se nadrazuje za znak `_`). Kontrolují se práva v JavaScript objektu `nopermsJavascript`.
- `hideOnCreate` nastavením na hodnotu `true` schová kartu při vytváření nového záznamu.
- `hideOnEdit` nastavením na hodnotu `true` schová kartu při editaci stávajícího záznamu.

```javascript
let tabs = [
    //id: unikatne ID tabu, title: zobrazeny nazov tabu, selected: urcenie predvoleneho tabu
    { id: 'basic', title: 'Základné', selected: true },
    { id: 'description', title: 'Popis', selected: false },
    { id: 'metadata', title: 'Metadáta', selected: false, className: 'hide-on-create', perms: "users_edit_admins" },
    { id: 'photoeditor', title: 'Foto editor', selected: false, content: '<div id="photoEditorContainer" style="background-color: yellow; min-height: 450px; min-width: 800px;">TODO: sem inicializovat foto editor</div>' },
    { id: 'history', title: '[[\#{editor.tab.history}]]', hideOnCreate: true }
];

...

let columns = [
    {
        {
            data: "author",
            name: "author",
            title: "Autor",
            renderFormat: "dt-format-text",

            editor: {
                type:  "text",
                tab: "metadata" //definicia tabu do ktoreho sa pole presunie
            }
        }
    }
];

...

galleryTable = WJ.DataTable( {
    url: url,
    serverSide: false,
    columns: columns,
    tabs: tabs
});
```

Pro přepnutí tabu lze aktivovat `listener`:

```javascript
window.addEventListener('WJ.DTE.tabclick', function (e) {
    console.log("tabclick, e=", e);
    if ("photoeditor" == e.detail.id) initializeImageEditor(e);
}, false);
```

Pokud potřebujete mít **kartu na plnou výšku okna** lze kartě nastavit CSS styl `.dte-tab-autoheight`. V případě potřeby je možné přes data atribut `data-dt-autoheight-offset` nastavit posun velikosti (pokud okno obsahuje již jiný element). Pokud potřebujete, aby okno bylo posuvné (může obsahovat velký objekt) nezapomeňte nastavit `style="overflow:scroll;"`.

```javascript
var tabs = [
    { id: 'description', title: '[[\#{components.catalog.desc}]]', selected: true },
    { id: 'metadata', title: '[[\#{gallery.admin.metadata.title}]]', selected: false },
    {
        id: 'photoeditor',
        title: '[[\#{admin.editor.title}]]',
        selected: false,
        content: '<div id="photoEditorContainer" class="dte-tab-autoheight"></div>'
    },
    {
        id: 'areaOfInterest',
        title: '[[\#{components.gallery.areaOfInterest}]]',
        selected: false,
        content: '<div class="dte-tab-autoheight" style="overflow:scroll;"><div id="cropper-app"><webjet-cropper-component ref="vueCropper"></webjet-cropper-component></div></div>'
    }
];
```

## Možnosti konfigurace columns objektu

Ukázkové možnosti konfigurace DT Editoru (vrámci columns definice)

```javascript
let columns = [
    {
        data: "imageName",
        ...
        editor {
            //dokumentacia: https://editor.datatables.net/reference/type/field-options

            type:   "text"
                    "checkbox"
                    "textarea"
                    "datetime"
            label: "Spustiť úlohu po štarte servera?", //special label ak sa nepouzije title zo stlpca
            def: false || "*", //defaultna hodnota pre novy zaznam

            message:    "Toto je tooltip" //TOOLTIP
                        "data:label" //zoberie hodnotu tooltipu z label stlpca dat

            className:  'hide-on-create', //nezobrazit pole pri vytvarani noveho zaznamu
                        'disable-on-create', //pole nastavi ako disabled pri vytvarani noveho zaznamu
                        'hide-on-edit' //pole pri editacii existujuceho zaznamu schova
                        'disable-on-edit' //nepovoli editaciu daneho pola (nastavi ako disabled)

            attr: {
                "data-dt-field-hr": "after", //prida oddelovaci HR za tento riadok
                "data-dt-field-headline": "[[\#{temp.english}]]", //Nadpis
                "data-dt-field-full-headline": "Nadpis na celú šírku okna",
                disabled: "disabled", //pole sa nebude dat editovat
            }

            options: [
                { label: "áno", value: true } //moznosti pre true/false
                ||
                { label: "http", value: "http" }, //moznosti v select boxe
                { label: "https", value: "https" },
                { label: "alias", value: "alias" }
            ],
            unselectedValue: false //TODO ???

            format: 'L HH:mm', //format pre datum/cas
            wireFormat: "x", //format datetime z JSON subore, vid https://momentjs.com/docs/#/parsing/string-format/
            opts: {
                //zvysne moznosti https://editor.datatables.net/reference/field/datetime (je potrebne sa prihlasit pre zobrazenie moznosti)
            }
        }
    }
];
```

## Události

Kromě [standardních událostí](https://editor.datatables.net/reference/event/) z Datatabulky jsou ve WebJETu přidány následující události:
- `WJ.DTE.open` - vyvolaný při otevření okna editoru, v `details` objektu přenese `dte` objekt editoru.
- `WJ.DTE.opened` - vyvolaný po otevření okna (okno je již zobrazeno), v `details` objektu přenese `dte` objekt editoru, jeho `id` a typ akce (`edit,create,remove`) v `action`.
- `WJ.DTE.close` - vyvolaný při zavření okna editoru, v `details` objektu přenese `dte` objekt editoru.
- `WJ.DTE.forceReload` - je vyvolán když REST služba vrátí požadavek na obnovení datatabulky. Ta se obnoví sama, tento event se odešle pro možnost aktualizace nap. JS tree
- `WJ.DTE.tabclick` - je vyvolán po kliknutí na tab v editoru
- `WJ.DTE.submitclick` - je vyvolán po kliknutí na tlačítko uložení editoru
- `WJ.DTE.closeclick` - je vyvolán po kliknutí na tlačítko zavření okna editoru
- `WJ.DTE.xhrfetch` - je vyvolán po načtení dat do editoru při volání přes fetchOnEdit, okno editoru je stále v DOM stromu a nefungují standardní eventy `dt.opened`
- `WJ.DTE.resize` - je vyvoláno při změně velikosti okna (kliknutí na tlačítko maximalizovat/minimalizovat)

## Stylování

Pro specifické případy je třeba nastavit CSS styl pro pole (anotace className):
- `todo` - pole se zobrazí oranžovou barvou naznačující, že je zatím nefunkční
- `DTE_Field_Has_Checkbox` - zmenší odsazení mezi polem a následujícím zaškrtávacím polem (nastavuje se na pole, ne checkbox)

## Speciální tlačítka

Pokud potřebujete mít speciální tlačítka v editoru namísto standardního tlačítka Uložit můžete při konfiguraci Datatabulky použít možnost `editorButtons`:

```javascript
webpagesDatatable = WJ.DataTable({
    url: webpagesInitialUrl,
    initialData: webpagesInitialJson,
    serverSide: true,
    ...
    editorButtons: [
        {
            text: '<i class="fal fa-check"></i> ' + WJ.translate('button.save'),
            action: function() {
                //console.log("SAVING");
                let close = true;
                if ($("#webpagesSaveCheckbox").is(":checked")) close = false;
                //console.log("this=", this);

                if (close) {
                    this.submit();
                } else {
                    //action musime zachovat, lebo to editor pri ulozeni zmaze
                    let editorAction = this.s.action;
                    //console.log("editorAction=", editorAction);
                    let editor = this;
                    this.submit(
                        //success callback
                        function() {
                            editor.s.action = editorAction;
                        },
                        //error callback
                        null,
                        //formatData function
                        null,
                        //hide editor after save
                        close
                    );
                }
            }
        }
    ]
});
```

Podobně pro přidání nového záznamu lze nastavit vlastní tlačítka pomocí `createButtons` s možností nezavření editoru po uložení záznamu:

```javascript
filePropertiesTable = WJ.DataTable({
    url: ...,
    ...
    createButtons: [
        {
            text: '<i class="fal fa-check"></i> ' + WJ.translate('button.add'),
            action: function() {
                //console.log("SAVING NO CLOSE");
                //action musime zachovat, lebo to editor pri ulozeni zmaze
                let editorAction = this.s.action;
                //console.log("editorAction=", editorAction);
                let editor = this;
                this.submit(
                    //success callback
                    function() {
                        editor.s.action = editorAction;
                    },
                    //error callback
                    null,
                    //formatData function
                    null,
                    //hide editor after save
                    false
                );
            }
        }
    ]
});
```

## Ukázky kódu

### Dynamická změna hodnot ve výběrovém poli

Někdy je třeba dynamicky měnit hodnoty v select boxu na základě změn v předchozích polích. Příklad je v souboru [temps-list.pug](../../../../src/main/webapp/admin/v9/views/pages/templates/temps-list.pug) kde je řešení na:
- změna hodnot select boxu při načítání editoru
- změna hodnot při změně předchozího pole

V ukázkovém případě se jedná o změnu hodnot pole `forward` na základě hodnoty vybrané v select boxu `templatesGroupId`. Navíc se zachovává i aktuálně nastavená hodnota v poli `forward`, aby se nestalo, že vrácené možné hodnoty neobsahují aktuálně nastavenou hodnotu.

```javascript
let tabs = [
    {id: 'basicTab', title: '[[\#{templates.temps-list.basicTab}]]', selected: true},
    {id: 'templatesTab', title: '[[\#{templates.temps-list.templatesTab}]]', selected: false},
    {id: 'accessTab', title: '[[\#{templates.temps-list.accessTab}]]', selected: false}
];

let tempsTable;
window.domReady.add(function () {
    let columns = [(${layout.getDataTableColumns('sk.iway.iwcm.doc.TemplateDetails')})];

    tempsTable = WJ.DataTable({
        url: "/admin/rest/templates/temps-list",
        columns: columns,
        editorId: "tempId",
        tabs: tabs
    });

    //zavola REST sluzbu pre ziskanie moznosti selectu forward na zaklade pola templatesGroupId
    //do REST sluzby posiela aj aktualne nastavenu hodnotu, ktora sa vzdy vo vratenych datach bude nachadzat
    function reloadForwardsSelect() {
        let templatesGroupId = tempsTable.EDITOR.field("templatesGroupId").val();
        let currentForward = tempsTable.EDITOR.field("forward").val();
        let params = {
            templatesGroupId: templatesGroupId,
            currentForward: currentForward
        }
        $.get({
            url: "/admin/rest/templates/temps-list/forwards/",
            data: params,
            success: function(data) {
                tempsTable.EDITOR.field("forward").update(data);

                let select = $(tempsTable.EDITOR.node("forward")).find("select")[0];
                if (typeof $(select).data("selectpicker") != "undefined") {
                    //musime refreshnut selectpicker aby zmena bola viditelna
                    $(select).selectpicker('refresh');
                }
            }
        });
    }

    //tu evidujeme nastavenie on change event listeneru
    let templatesGroupChangeHandlerSet = false;
    tempsTable.EDITOR.on( 'open', function( e, mode, action ){

        //pri otvoreni potrebujeme refreshnut aktualne hodnoty na zaklade editovaneho zaznamu
        reloadForwardsSelect();

        //pri prvom nacitani nastav on change listener na reload dat
        if (templatesGroupChangeHandlerSet == false) {
            let select = $(tempsTable.EDITOR.node("templatesGroupId")).find("select")[0];
            $(select).on("change", reloadForwardsSelect);
            templatesGroupChangeHandlerSet = true;
        }

    });
});
```

REST služba je přidána do stávající třídy [TemplatesController](../../../../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java):

```java
@RequestMapping("/forwards/")
public List<LabelValue> getForwards(@RequestParam(required = false) Integer templatesGroupId, @RequestParam(required = false) String currentForward) {
    List<LabelValue> forwards = templateDetailsService.getTemplateForwards(getRequest(), templatesGroupId, currentForward);
    return forwards;
}
```

### Získání JSON objektu před editací

Získání JSON objektu před editací (lze modifikovat data pro editor)

```javascript
webpagesDatatable.EDITOR.on( 'initEdit', function( e, node, data, items, type ){
    console.log("editor initEdit, data=", data, " items=", items);
    editorCurrentJson = data;
    return ;
});
```

### Programové otevření editoru

Pro korektní programové otevření editoru je třeba využít WebJET API. To zajistí korektní zobrazení dopisů a tlačítek. Parametr `row` je **jQuery selektor** řádku, podobně jako v [originálním API](https://editor.datatables.net/reference/api/edit\(\)). Nejjednodušší pomocí selektoru podle ID záznamu `#ID`.

Funkce `wjEditFetch` provede i případné obnovení dat řádku (je-li nastavena možnost `fetchOnEdit`) a provedení nastavené `onEdit` funkce. Volání `wjEdit` otevře editor s již načtenými daty a neprovádí žádné speciální operace. Máte-li upravenou funkci `onEdit` je vždy třeba použít volání `wjEdit`, jinak dojde k zacyklení.

```javascript
//API
TABLE.wjEditFetch = function(row) {...}
TABLE.wjEdit = function(row) {...}

//priklad pouzitia
webpagesDatatable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
```

### Podmíněné zobrazení vstupního pole

Pokud potřebujete podmíněně zobrazit vstupní pole na základě jiné hodnoty můžete využít API volání `field("meno").hide()`. Ukázka schová/zobrazí pole při otevření editoru na základě jiné hodnoty v JSON datech:

```javascript
groupsDatatable.EDITOR.on('open', function (e, type) {
    //zobrazovat-pole-pre-zadanie-domeny-len-pre-korenove-priecinky
    let parentGroupId = e.target.currentJson.parentGroupId;
    if (parentGroupId > 0) {
        groupsDatatable.EDITOR.field("domainName").hide();
        groupsDatatable.EDITOR.field("editorFields.forceDomainNameChange").hide();
    } else {
        groupsDatatable.EDITOR.field("domainName").show();
        groupsDatatable.EDITOR.field("editorFields.forceDomainNameChange").show();
    }
});
```

### API funkce

Další API funkce, které můžete použít:
- `TABLE.wjUpdateOptions(url=null, callback=null)` - znovu provede volání REST služby `/all` pro získání aktuálních `json.options` číselníků a tyto aktualizuje ve filtrech datatabulky iv editoru (a to i když je editor otevřen). Volitelně lze zadat URL adresu a případný `callback` zavolán po aktualizaci číselníků.
