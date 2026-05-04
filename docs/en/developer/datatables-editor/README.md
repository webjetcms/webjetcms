# Datatables EDITOR

[Editor for datatables](http://editor.datatables.net) is an extension for datatables.net. You must be logged in to editor.datatables.net to view the documentation, otherwise you will not see the complete documentation.

## Tabs in the editor

The WebJET version of the Editor has implemented tab support. Each tab must have a unique ``id``, which is set in the columns definition in the editor section to the tab to which the field will be moved. If the value ```tab``` in the columns definition is not set, the field will be moved to the first tab.

Optionally, the ```tabs``` definition can contain an attribute ```content``` with HTML code that is inserted into the card. In this case, it must no longer contain fields from the ```columns``` definition. At the same time, the indentation and the ```before``` element with a gray band are removed from the card defined in this way, the card content is full-width and any indentation needs to be addressed in custom CSS.

Possible attributes:
- ```selected``` defines the default tab displayed, it should only be set on one tab.
- ```className``` allows you to add CSS style to the tab switcher. The predefined CSS styles are ```hide-on-create``` for hiding the tab when creating a new record and ```hide-on-edit``` for hiding the tab when editing an existing record, `hide-on-duplicate` for hiding the tab when duplicating.
- ```perms``` allows not to display the card if the user does not have the specified right (note, the character ```.``` in the right is superseded by the character ```_```). The rights in the JavaScript object ```nopermsJavascript``` are checked.
- ```hideOnCreate``` setting to ```true``` hides the tab when creating a new record.
- ```hideOnEdit``` setting to the value ```true``` hides the tab when editing an existing record.

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

To toggle the taboo, you can activate ```listener```:

```javascript
window.addEventListener('WJ.DTE.tabclick', function (e) {
    console.log("tabclick, e=", e);
    if ("photoeditor" == e.detail.id) initializeImageEditor(e);
}, false);
```

If you need to have a **tab that fills the window**, you can set the CSS style ```.dte-tab-autoheight``` to the tab. If necessary, you can set the size offset via the data attribute ```data-dt-autoheight-offset``` (if the window already contains another element). If you need the window to be scrollable (it can contain a large object), don't forget to set ```style="overflow:scroll;"```.

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

## Columns object configuration options

Sample DT Editor configuration options (within the columns definition)

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

In addition to the [standard events](https://editor.datatables.net/reference/event/) from Datatable, the following events have been added to WebJET:

- ```WJ.DTE.open``` - ​​called when opening the editor window, in the ```details``` object, the ```dte``` editor object is transferred.
- ```WJ.DTE.opened``` - ​​called after opening the window (the window is already displayed), in the ```details``` object, ```dte``` the editor object is transferred, its ```id``` and the action type (```edit,create,remove```) in the ```action```.
- ```WJ.DTE.close``` - ​​called when closing the editor window, in the ```details``` object, the ```dte``` editor object is transferred.
- ```WJ.DTE.forceReload``` - ​​is called when the REST service returns a request to refresh the data table. It refreshes itself, this event is sent for the possibility of updating e.g. JS tree
- ```WJ.DTE.tabclick``` - ​​is called after clicking on a tab in the editor
- ```WJ.DTE.submitclick``` - ​​is called after clicking the editor save button
- ```WJ.DTE.closeclick``` - ​​is called after clicking the close button of the editor window
- ```WJ.DTE.xhrfetch``` - ​​is called after loading data into the editor when called via fetchOnEdit, the editor window is still in the DOM tree and standard events ```dt.opened``` do not work
- ```WJ.DTE.resize``` - ​​is called when the window is resized (clicking the maximize/minimize button)

## Styling

For specific cases, it is necessary to set a CSS style for the field (className annotation):

- ```todo``` - ​​the field will be displayed in orange indicating that it is not yet functional
- ```DTE_Field_Has_Checkbox``` - ​​reduces the indentation between the field and the next checkbox (set on the field, not the checkbox)

## Special buttons

If you need to have special buttons in the editor instead of the standard Save button, you can use the ```editorButtons``` option when configuring the Datatable:

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

Similarly, for adding a new record, you can set custom buttons using `createButtons` with the option of not closing the editor after saving the record:

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

## Code samples

### Dynamically changing values ​​in a selection field

Sometimes it is necessary to dynamically change the values ​​in the select box based on changes in previous fields. An example is in the file [temps-list.pug](../../../../src/main/webapp/admin/v9/views/pages/templates/temps-list.pug) where the solution is to:

- change select box values ​​when loading the editor
- change values ​​when changing the previous field

In the example case, the values ​​of the field ```forward``` are changed based on the value selected in the select box ```templatesGroupId```. In addition, the currently set value in the field ```forward``` is also preserved to prevent the possible values ​​returned from not containing the currently set value.

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

The REST service is added to the existing class [TemplatesController](../../../../src/main/java/sk/iway/iwcm/components/templates/TemplatesController.java):

```java
@RequestMapping("/forwards/")
public List<LabelValue> getForwards(@RequestParam(required = false) Integer templatesGroupId, @RequestParam(required = false) String currentForward) {
    List<LabelValue> forwards = templateDetailsService.getTemplateForwards(getRequest(), templatesGroupId, currentForward);
    return forwards;
}
```

### Getting a JSON object before editing

Getting a JSON object before editing (it is possible to modify the data for the editor)

```javascript
webpagesDatatable.EDITOR.on( 'initEdit', function( e, node, data, items, type ){
    console.log("editor initEdit, data=", data, " items=", items);
    editorCurrentJson = data;
    return ;
});
```

### Opening the editor programmatically

To open the editor correctly programmatically, you need to use the WebJET API. This will ensure that the sheets and buttons are displayed correctly. The parameter ```row``` is a **jQuery selector** of the row, similar to the [original API](https://editor.datatables.net/reference/api/edit()). The easiest way is to use the selector by record ID ```#ID```.

The ```wjEditFetch``` function also performs any possible row data refresh (if the ```fetchOnEdit``` option is set) and executes the set ```onEdit``` function. The ```wjEdit``` call opens the editor with already loaded data and does not perform any special operations. If you have modified the ```onEdit``` function, it is always necessary to use the ```wjEdit``` call, otherwise a loop will occur.

```javascript
//API
TABLE.wjEditFetch = function(row) {...}
TABLE.wjEdit = function(row) {...}

//priklad pouzitia
webpagesDatatable.wjEditFetch($('.datatableInit tr[id=' + docId + ']'));
```

### Conditional display of an input field

If you need to conditionally display an input field based on a different value, you can use the API call ```field("meno").hide()```. The example hides/shows the field when the editor is opened based on a different value in the JSON data:

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

### API functions

Other API functions you can use:

- ```TABLE.wjUpdateOptions(url=null, callback=null)``` - ​​re-executes the REST service call ```/all``` to get the current ```json.options``` code lists and updates them in the data table filters and in the editor (even if the editor is open). Optionally, you can specify a URL address and a possible ```callback``` called after updating the code lists.