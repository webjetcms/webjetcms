# Datatables EDITOR

[Editor for datatables](http://editor.datatables.net) is an extension for datatables.net. You must be logged in to editor.datatables.net to view the documentation, otherwise you will not see the complete documentation.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Datatables EDITOR](#datatables-editor)
	- [Tabs in the editor](#karty-v-editore)
	- [Configuration options for the columns object](#možnosti-konfigurácie-columns-objektu)
	- [Events](#udalosti)
	- [Styling](#štýlovanie)
	- [Special buttons](#špeciálne-tlačidlá)
	- [Code samples](#ukážky-kódu)
		- [Dynamic change of values in the selection field](#dynamická-zmena-hodnôt-vo-výberovom-poli)
		- [Getting the JSON object before editing](#získanie-json-objektu-pred-editáciou)
		- [Program editor opening](#programové-otvorenie-editora)
		- [Conditional display of the input field](#podmienené-zobrazenie-vstupného-poľa)
		- [API functions](#api-funkcie)
<!-- /code_chunk_output -->

## Tabs in the editor

The WebJET version of the Editor has implemented card support. Each card must have a unique `id`, which is used in the columns definition in the editor section to set the tab to which the field will be moved. If the value `tab` in the columns definition is not set the field is moved to the first tab.

Optionally, it can `tabs` the definition to contain the attribute `content` with HTML code that is inserted into the card. In this case, it must no longer contain fields from `columns` Definitions. At the same time, the indentations are removed from the defined card and `before` element with grey band, the content of the tab is full width and any indentation needs to be solved in custom CSS.

Possible attributes:
- `selected` defines the default tab displayed, it should only be set on one tab.
- `className` allows you to add CSS style to the tab switcher. CSS styles are predefined `hide-on-create` to hide the card when creating a new record, and `hide-on-edit` to hide the tab when editing an existing record.
- `perms` allows to not display the tab if the user does not have the specified right (note, the character `.` in the law is taken as a sign of `_`). The rights in the JavaScript object are checked `nopermsJavascript`.
- `hideOnCreate` by setting it to `true` hides the tab when creating a new record.
- `hideOnEdit` by setting it to `true` hides the tab when editing an existing record.
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

To switch tabs, you can activate `listener`:

```javascript
window.addEventListener(
	"WJ.DTE.tabclick",
	function (e) {
		console.log("tabclick, e=", e);
		if ("photoeditor" == e.detail.id) initializeImageEditor(e);
	},
	false
);
```

If you need to have **tab to the full height of the window** it is possible to set the CSS style `.dte-tab-autoheight`. If necessary, it is possible via the data attribute `data-dt-autoheight-offset` set the size offset (if the window already contains another element). If you need the window to be scrollable (it may contain a large object) don't forget to set `style="overflow:scroll;"`.

```javascript
var tabs = [
	{ id: "description", title: "[[#{components.catalog.desc}]]", selected: true },
	{ id: "metadata", title: "[[#{gallery.admin.metadata.title}]]", selected: false },
	{
		id: "photoeditor",
		title: "[[#{admin.editor.title}]]",
		selected: false,
		content: '<div id="photoEditorContainer" class="dte-tab-autoheight"></div>',
	},
	{
		id: "areaOfInterest",
		title: "[[#{components.gallery.areaOfInterest}]]",
		selected: false,
		content: '<div class="dte-tab-autoheight" style="overflow:scroll;"><div id="cropper-app"><webjet-cropper-component ref="vueCropper"></webjet-cropper-component></div></div>',
	},
];
```

## Configuration options for the columns object

Sample DT Editor configuration options (within columns definition)

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

## Events

In addition to [standard events](https://editor.datatables.net/reference/event/) the following events are added from the Datatable in WebJET:
- `WJ.DTE.open` - called when the editor window is opened, in `details` object will transfer `dte` editor's object.
- `WJ.DTE.opened` - called after the window is opened (the window is already displayed), in `details` object will transfer `dte` the editor's object, his `id` and the type of action (`edit,create,remove`) v `action`.
- `WJ.DTE.close` - called when the editor window is closed, in `details` object will transfer `dte` editor's object.
- `WJ.DTE.forceReload` - is invoked when the REST service returns a request to restore the datatable. It will refresh itself, this event is sent for the possibility of updating e.g. JS tree
- `WJ.DTE.tabclick` - is called after clicking on tab in the editor
- `WJ.DTE.submitclick` - is invoked after clicking on the save editor button
- `WJ.DTE.closeclick` - is called after clicking on the close button of the editor window
- `WJ.DTE.xhrfetch` - is invoked after the data is loaded into the editor when called via fetchOnEdit, the editor window is still in the DOM tree and the standard events do not work `dt.opened`
- `WJ.DTE.resize` - is invoked when the window is resized (click on the maximize/minimize button)

## Styling

For specific cases, you need to set the CSS style for the field (className annotation):
- `todo` - the field will appear orange indicating that it is not yet functional
- `DTE_Field_Has_Checkbox` - reduces the offset between the field and the next checkbox (it is set per field, not per checkbox)

## Special buttons

If you need to have special buttons in the editor instead of the standard Save button you can use the option when configuring the Datatable `editorButtons`:

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

## Code samples

### Dynamic change of values in the selection field

Sometimes it is necessary to dynamically change the values in the select box based on changes in previous fields. An example is in the file [temps-list.pug](../../../src/main/webapp/admin/v9/views/pages/templates/temps-list.pug) where is the solution to:
- change select box values when loading the editor
- change of values when changing the previous field
In the sample case, this is a change of field values `forward` based on the value selected in the select box `templatesGroupId`. In addition, the currently set value in the field is also preserved. `forward`so that the returned possible values do not contain the currently set value.

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

REST service is added to an existing class [TemplatesController](../../../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java):

```java
@RequestMapping("/forwards/")
public List<LabelValue> getForwards(@RequestParam(required = false) Integer templatesGroupId, @RequestParam(required = false) String currentForward) {
    List<LabelValue> forwards = templateDetailsService.getTemplateForwards(getRequest(), templatesGroupId, currentForward);
    return forwards;
}
```

### Getting the JSON object before editing

Getting the JSON object before editing (it is possible to modify the data for the editor)

```javascript
webpagesDatatable.EDITOR.on("initEdit", function (e, node, data, items, type) {
	console.log("editor initEdit, data=", data, " items=", items);
	editorCurrentJson = data;
	return;
});
```

### Program editor opening

For correct programmatic opening of the editor it is necessary to use the WebJET API. This will ensure the correct display of letters and buttons. Parameter `row` Is **jQuery selector** line, similar to the [original API](https://editor.datatables.net/reference/api/edit\(\)). Easiest by using the record ID selector `#ID`.

Feature `wjEditFetch` also performs any row data refresh (if the option is set `fetchOnEdit`) and the execution of the set `onEdit` functions. Call `wjEdit` opens the editor with the already loaded data and does not do any special operations. If you have a modified function `onEdit` it is always necessary to use the call `wjEdit`, otherwise a jam will occur.

```javascript
//API
TABLE.wjEditFetch = function(row) {...}
TABLE.wjEdit = function(row) {...}

//priklad pouzitia
webpagesDatatable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
```

### Conditional display of the input field

If you need to conditionally display an input field based on a different value, you can use the API call `field("meno").hide()`. The preview hides/shows the field when the editor is opened based on a different value in the JSON data:

```javascript
groupsDatatable.EDITOR.on("open", function (e, type) {
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

### API functions

Other API functions you can use:
- `TABLE.wjUpdateOptions(url=null, callback=null)` - executes the REST service call again `/all` to get the latest `json.options` dials and updates them in the datatable filters and in the editor (even when the editor is open). Optionally, it is possible to specify the URL and any `callback` called after updating the dials.
