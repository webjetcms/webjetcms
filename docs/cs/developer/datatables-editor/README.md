# Datové tabulky EDITOR

[Editor pro datové tabulky](http://editor.datatables.net) je rozšíření pro datatables.net. Pro zobrazení dokumentace musíte být přihlášeni na editor.datatables.net, jinak se vám nezobrazí kompletní dokumentace.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Datové tabulky EDITOR](#editor-datových-tabulek)
  - [Karty v editoru](#karty-v-editoru)
  - [Možnosti konfigurace objektu sloupce](#možnosti-konfigurace-objektu-sloupců)
  - [Události](#události)
  - [Styling](#styling)
  - [Speciální tlačítka](#speciální-tlačítka)
  - [Ukázky kódu](#ukázky-kódu)
    - [Dynamická změna hodnot ve výběrovém poli](#dynamická-změna-hodnot-ve-výběrovém-poli)
    - [Získání objektu JSON před úpravou](#získání-objektu-json-před-úpravou)
    - [Otevření editoru programu](#otevření-editoru-programu)
    - [Podmíněné zobrazení vstupního pole](#podmíněné-zobrazení-vstupního-pole)
    - [Funkce API](#funkce-api)

<!-- /code_chunk_output -->

## Karty v editoru

Ve verzi editoru WebJET je implementována podpora karet. Každá karta musí mít jedinečný `id`, který se používá v definici sloupců v sekci editoru k nastavení karty, na kterou bude pole přesunuto. Pokud je hodnota `tab` v definici sloupců není nastaveno, pole se přesune na první kartu.

Volitelně může `tabs` definice, která obsahuje atribut `content` s kódem HTML, který je vložen do karty. V takovém případě již nesmí obsahovat pole ze seznamu `columns` Definice. Současně se z definované karty odstraní odsazení a. `before` s šedým pruhem, obsah tabulátoru je na celou šířku a případné odsazení je třeba řešit ve vlastním CSS.

Možné atributy:
- `selected` definuje výchozí zobrazenou kartu, měla by být nastavena pouze na jedné kartě.
- `className` umožňuje přidat styl CSS do přepínače karet. Styly CSS jsou předdefinované `hide-on-create` skrýt kartu při vytváření nového záznamu a `hide-on-edit` skrýt kartu při úpravě existujícího záznamu.
- `perms` umožňuje nezobrazovat tabulátor, pokud uživatel nemá zadané právo (pozn. znak `.` v zákoně je považován za znamení `_`). Práva v objektu JavaScript jsou kontrolována `nopermsJavascript`.
- `hideOnCreate` nastavením na hodnotu `true` skryje kartu při vytváření nového záznamu.
- `hideOnEdit` nastavením na hodnotu `true` skryje kartu při úpravě existujícího záznamu.

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

Chcete-li přepínat karty, můžete aktivovat `listener`:

```javascript
window.addEventListener('WJ.DTE.tabclick', function (e) {
    console.log("tabclick, e=", e);
    if ("photoeditor" == e.detail.id) initializeImageEditor(e);
}, false);
```

Pokud potřebujete mít **na celou výšku okna** je možné nastavit styl CSS `.dte-tab-autoheight`. V případě potřeby je to možné prostřednictvím atributu data `data-dt-autoheight-offset` nastavit posunutí velikosti (pokud okno již obsahuje jiný prvek). Pokud potřebujete, aby se okno dalo posouvat (může obsahovat velký objekt), nezapomeňte nastavit posunutí okna. `style="overflow:scroll;"`.

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

## Možnosti konfigurace objektu sloupce

Ukázkové možnosti konfigurace editoru DT (v rámci definice sloupců)

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

Kromě [standardní události](https://editor.datatables.net/reference/event/) z datové tabulky ve WebJETu jsou přidány následující události:
- `WJ.DTE.open` - volán při otevření okna editoru, v `details` objekt přenese `dte` objekt redakce.
- `WJ.DTE.opened` - zavolán po otevření okna (okno je již zobrazeno), v položce `details` objekt přenese `dte` objekt redakce, jeho `id` a typ akce (`edit,create,remove`) v `action`.
- `WJ.DTE.close` - vyvolán při zavření okna editoru, v položce `details` objekt přenese `dte` objekt redakce.
- `WJ.DTE.forceReload` - je vyvolán, když služba REST vrátí požadavek na obnovení datové tabulky. Obnoví se sama, tato událost se posílá pro možnost aktualizace např. JS stromu.
- `WJ.DTE.tabclick` - je vyvolán po kliknutí na záložku v editoru
- `WJ.DTE.submitclick` - se vyvolá po kliknutí na tlačítko uložit editor.
- `WJ.DTE.closeclick` - je vyvolán po kliknutí na tlačítko zavření okna editoru
- `WJ.DTE.xhrfetch` - je vyvolán po načtení dat do editoru při volání přes fetchOnEdit, okno editoru je stále ve stromu DOM a standardní události nefungují. `dt.opened`
- `WJ.DTE.resize` - se vyvolá při změně velikosti okna (kliknutím na tlačítko maximalizace/minimalizace).

## Styling

Ve specifických případech je třeba nastavit styl CSS pro pole (anotace className):
- `todo` - pole se zobrazí oranžově, což znamená, že ještě není funkční.
- `DTE_Field_Has_Checkbox` - snižuje posun mezi polem a dalším zaškrtávacím políčkem (nastavuje se pro pole, nikoli pro zaškrtávací políčko).

## Speciální tlačítka

Pokud potřebujete mít v editoru místo standardního tlačítka Uložit speciální tlačítka, můžete při konfiguraci datové tabulky použít možnost. `editorButtons`:

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

Podobně lze pro přidání nového záznamu nastavit vlastní tlačítka pomocí příkazu `createButtons` s možností nezavírat editor po uložení záznamu:

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

Někdy je nutné dynamicky měnit hodnoty v poli výběru na základě změn v předchozích polích. Příklad je v souboru [temps-list.pug](../../../../src/main/webapp/admin/v9/views/pages/templates/temps-list.pug) kde je řešení:
- změnit hodnoty políček při načítání editoru
- změna hodnot při změně předchozího pole

V ukázkovém případě se jedná o změnu hodnot polí. `forward` na základě hodnoty vybrané v poli pro výběr `templatesGroupId`. Kromě toho se zachová i aktuálně nastavená hodnota v poli. `forward` aby vrácené možné hodnoty neobsahovaly aktuálně nastavenou hodnotu.

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

Služba REST je přidána do existující třídy [TemplatesController](../../../../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java):

```java
@RequestMapping("/forwards/")
public List<LabelValue> getForwards(@RequestParam(required = false) Integer templatesGroupId, @RequestParam(required = false) String currentForward) {
    List<LabelValue> forwards = templateDetailsService.getTemplateForwards(getRequest(), templatesGroupId, currentForward);
    return forwards;
}
```

### Získání objektu JSON před úpravou

Získání objektu JSON před úpravou (je možné upravit data pro editor)

```javascript
webpagesDatatable.EDITOR.on( 'initEdit', function( e, node, data, items, type ){
    console.log("editor initEdit, data=", data, " items=", items);
    editorCurrentJson = data;
    return ;
});
```

### Otevření editoru programu

Pro správné programové otevření editoru je nutné použít rozhraní WebJET API. Tím se zajistí správné zobrazení písmen a tlačítek. Parametr `row` Je **selektor jQuery** podobně jako v případě [původní API](https://editor.datatables.net/reference/api/edit\(\)). Nejsnadněji pomocí voliče ID záznamu `#ID`.

Funkce `wjEditFetch` provede také případnou obnovu dat řádku (pokud je nastavena možnost `fetchOnEdit`) a provedení sady `onEdit` funkce. Volání `wjEdit` otevře editor s již načtenými daty a neprovádí žádné zvláštní operace. Pokud máte upravenou funkci `onEdit` je vždy nutné použít volání `wjEdit`, jinak dojde k zaseknutí.

```javascript
//API
TABLE.wjEditFetch = function(row) {...}
TABLE.wjEdit = function(row) {...}

//priklad pouzitia
webpagesDatatable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
```

### Podmíněné zobrazení vstupního pole

Pokud potřebujete podmíněně zobrazit vstupní pole na základě jiné hodnoty, můžete použít volání API. `field("meno").hide()`. Náhled skryje/zobrazí pole při otevření editoru na základě jiné hodnoty v datech JSON:

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

### Funkce API

Další funkce API, které můžete použít:
- `TABLE.wjUpdateOptions(url=null, callback=null)` - znovu provede volání služby REST `/all` získat nejnovější `json.options` a aktualizuje je ve filtrech datové tabulky a v editoru (i když je editor otevřen). Volitelně je možné zadat adresu URL a libovolné `callback` po aktualizaci číselníků.
