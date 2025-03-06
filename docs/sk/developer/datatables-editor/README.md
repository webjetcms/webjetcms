# Datatables EDITOR

[Editor pre datatables](http://editor.datatables.net) je rozšírenie pre datatables.net. Pre zobrazenie dokumentácie musíte byť prihlásený na stránke editor.datatables.net, inak neuvidíte kompletnú dokumentáciu.


<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Datatables EDITOR](#datatables-editor)
  - [Karty v editore](#karty-v-editore)
  - [Možnosti konfigurácie columns objektu](#možnosti-konfigurácie-columns-objektu)
  - [Udalosti](#udalosti)
  - [Štýlovanie](#štýlovanie)
  - [Špeciálne tlačidlá](#špeciálne-tlačidlá)
  - [Ukážky kódu](#ukážky-kódu)
    - [Dynamická zmena hodnôt vo výberovom poli](#dynamická-zmena-hodnôt-vo-výberovom-poli)
    - [Získanie JSON objektu pred editáciou](#získanie-json-objektu-pred-editáciou)
    - [Programové otvorenie editora](#programové-otvorenie-editora)
    - [Podmienené zobrazenie vstupného poľa](#podmienené-zobrazenie-vstupného-poľa)
    - [API funkcie](#api-funkcie)

<!-- /code_chunk_output -->


## Karty v editore

WebJET verzia Editora má implementovanú podporu kariet. Každá karta musí mať unikátne ``id``, na ktoré sa v columns definícii v časti editor nastavuje karta, do ktorej sa presunie dané pole. Ak hodnota ```tab``` v columns definícii nie je nastavená pole sa presunie do prvej karty.

Voliteľne môže ```tabs``` definícia obsahovať atribút ```content``` s HTML kódom ktorý sa vloží do karty. V takom prípade už nesmie obsahovať polia z ```columns``` definície. Zároveň sa takto definovanej karte zrušia odsadenia a ```before``` element so šedým pásom, obsah karty je na celú šírku a prípadné odsadenie je potrebné si riešiť v custom CSS.

Možné atribúty:
- ```selected``` definuje predvolene zobrazenú kartu, mal by byť nastavený len na jednej karte.
- ```className``` umožňuje doplniť CSS štýl k prepínaču kariet. Preddefinované sú CSS štýly ```hide-on-create``` pre schovanie karty pri vytváraní nového záznamu a ```hide-on-edit``` pre schovanie karty pri editácii už existujúceho záznamu, `hide-on-duplicate` pre schovanie karty pri duplikovaní.
- ```perms``` umožňuje nezobraziť kartu ak používateľ nemá zadané právo (pozor, znak ```.``` v práve sa nadrádza za znak ```_```). Kontrolujú sa práva v JavaScript objekte ```nopermsJavascript```.
- ```hideOnCreate``` nastavením na hodnotu ```true``` schová kartu pri vytváraní nového záznamu.
- ```hideOnEdit``` nastavením na hodnotu ```true``` schová kartu pri editácii existujúceho záznamu.

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

Na prepnutie tabu je možné aktivovať ```listener```:

```javascript
window.addEventListener('WJ.DTE.tabclick', function (e) {
    console.log("tabclick, e=", e);
    if ("photoeditor" == e.detail.id) initializeImageEditor(e);
}, false);
```

Ak potrebujete mať **kartu na plnú výšku okna** je možné karte nastaviť CSS štýl ```.dte-tab-autoheight```. V prípade potreby je možné cez data atribút ```data-dt-autoheight-offset``` nastaviť posun veľkosti (ak okno obsahuje už iný element). Ak potrebujete, aby okno bolo posuvné (môže obsahovať veľký objekt) nezabudnite nastaviť ```style="overflow:scroll;"```.

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

## Možnosti konfigurácie columns objektu

Ukážkové možnosti konfigurácie DT Editora (vrámci columns definície)

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

## Udalosti

Okrem [štandardných udalostí](https://editor.datatables.net/reference/event/) z Datatabuľky sú vo WebJETe pridané nasledovné udalosti:

- ```WJ.DTE.open``` - vyvolaný pri otvorení okna editora, v ```details``` objekte prenesie ```dte``` objekt editora.
- ```WJ.DTE.opened``` - vyvolaný po otvorení okna (okno je už zobrazené), v ```details``` objekte prenesie ```dte``` objekt editora, jeho ```id``` a typ akcie (```edit,create,remove```) v ```action```.
- ```WJ.DTE.close``` - vyvolaný pri zatvorení okna editora, v ```details``` objekte prenesie ```dte``` objekt editora.
- ```WJ.DTE.forceReload``` - je vyvolaný keď REST služba vráti požiadavku na obnovenie datatabuľky. Tá sa obnoví sama, tento event sa odošle pre možnosť aktualizácie napr. JS tree
- ```WJ.DTE.tabclick``` - je vyvolaný po kliknutí na tab v editore
- ```WJ.DTE.submitclick``` - je vyvolaný po kliknutí na tlačidlo uloženia editora
- ```WJ.DTE.closeclick``` - je vyvolaný po kliknutí na tlačidlo zatvorenia okna editora
- ```WJ.DTE.xhrfetch``` - je vyvolaný po načítaní dát do editora pri volaní cez fetchOnEdit, okno editora je stále v DOM strome a nefungujú štandardné eventy ```dt.opened```
- ```WJ.DTE.resize``` - je vyvolané pri zmene veľkosti okna (kliknutie na tlačidlo maximalizovať/minimalizovať)

## Štýlovanie

Pre špecifické prípady je potrebné nastaviť CSS štýl pre pole (anotácia className):

- ```todo``` - pole sa zobrazí oranžovou farbou naznačujúc, že je zatiaľ nefunkčné
- ```DTE_Field_Has_Checkbox``` - zmenší odsadenie medzi poľom a nasledujúcim zaškrtávacím poľom (nastavuje sa na pole, nie checkbox)

## Špeciálne tlačidlá

Ak potrebujete mať špeciálne tlačidlá v editore namiesto štandardného tlačidla Uložiť môžete pri konfigurácii Datatabuľky použiť možnosť ```editorButtons```:

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

Podobne pre pridanie nového záznamu je možné nastaviť vlastné tlačidlá pomocou `createButtons` s možnosťou nezatvorenia editora po uložení záznamu:

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

## Ukážky kódu

### Dynamická zmena hodnôt vo výberovom poli

Niekedy je potrebné dynamicky meniť hodnoty v select boxe na základe zmien v predchádzajúcich poliach. Príklad je v súbore [temps-list.pug](../../../../src/main/webapp/admin/v9/views/pages/templates/temps-list.pug) kde je riešenie na:

- zmena hodnôt select boxu pri načítaní editora
- zmena hodnôt pri zmene predchádzajúceho poľa

V ukážkovom prípade sa jedná o zmenu hodnôt pola ```forward``` na základe hodnoty vybranej v select boxe ```templatesGroupId```. Naviac sa zachováva aj aktuálne nastavená hodnota v poli ```forward```, aby sa nestalo, že vrátené možné hodnoty neobsahujú aktuálne nastavenú hodnotu.

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

REST služba je pridaná do existujúcej triedy [TemplatesController](../../../../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java):

```java
@RequestMapping("/forwards/")
public List<LabelValue> getForwards(@RequestParam(required = false) Integer templatesGroupId, @RequestParam(required = false) String currentForward) {
    List<LabelValue> forwards = templateDetailsService.getTemplateForwards(getRequest(), templatesGroupId, currentForward);
    return forwards;
}
```

### Získanie JSON objektu pred editáciou

Získanie JSON objektu pred editáciou (je možné modifikovať dáta pre editor)

```javascript
webpagesDatatable.EDITOR.on( 'initEdit', function( e, node, data, items, type ){
    console.log("editor initEdit, data=", data, " items=", items);
    editorCurrentJson = data;
    return ;
});
```

### Programové otvorenie editora

Pre korektné programové otvorenie editora je potrebné využiť WebJET API. To zabezpečí korektné zobrazenie listov a tlačidiel. Parameter ```row``` je **jQuery selektor** riadku, podobne ako v [originálnom API](https://editor.datatables.net/reference/api/edit()). Najjednoduchšie pomocou selektora podľa ID záznamu ```#ID```.

Funkcia ```wjEditFetch``` vykoná aj prípadné obnovenie dát riadku (ak je nastavená možnosť ```fetchOnEdit```) a vykonanie nastavenej ```onEdit``` funkcie. Volanie ```wjEdit``` otvorí editor s už načítanými dátami a nerobí žiadne špeciálne operácie. Ak máte upravenú funkciu ```onEdit``` je vždy potrebné použiť volanie ```wjEdit```, inak dôjde k zacykleniu.

```javascript
//API
TABLE.wjEditFetch = function(row) {...}
TABLE.wjEdit = function(row) {...}

//priklad pouzitia
webpagesDatatable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
```

### Podmienené zobrazenie vstupného poľa

Ak potrebujete podmienene zobraziť vstupné pole na základe inej hodnoty môžete využiť API volanie ```field("meno").hide()```. Ukážka schová/zobrazí pole pri otvorení editora na základe inej hodnoty v JSON dátach:

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

### API funkcie

Ďalšie API funkcie, ktoré môžete použiť:

- ```TABLE.wjUpdateOptions(url=null, callback=null)``` - znova vykoná volanie REST služby ```/all``` pre získanie aktuálnych ```json.options``` číselníkov a tieto aktualizuje vo filtroch datatabuľky aj v editore (a to aj keď je editor otvorený). Voliteľne je možné zadať URL adresu a prípadný ```callback``` zavolaný po aktualizácii číselníkov.