# Autocomplete

In the editor it is easy to add **"whisperer" for text fields** for a more user-friendly retrieval of the written value:

![](autocomplete.png)

## Configuration

The autocomplete field is activated by setting the editor attributes using the annotation `@DataTableColumnEditorAttr`:

```java
@DataTableColumn(
    inputType = DataTableColumnType.TEXT,
    editor = {
        @DataTableColumnEditor(attr = {
            @DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/v9/settings/configuration/autocomplete")
        })
    }
)
```

The following attributes are supported, only mandatory `data-ac-url`:
- `data-ac-url` - URL of the REST service that returns the field/list `String` values.
- `data-ac-click` - the name of the function that will be called when the option in the autocomplete is clicked (to set additional fields). Setting to the value `fireEnter` a key press event is triggered when the value is selected `Enter`.
- `data-ac-name` - the name of the URL parameter in which the written value is sent to the REST service (by default term).
- `data-ac-min-length` - the minimum number of characters to call the REST service (default 1).
- `data-ac-max-rows` - maximum number of rows displayed (default 30).
- `data-ac-params` - a list of field selectors whose values are added to the URL of the REST service call, e.g. `#DTE_Field_templateInstallName,#DTE_Field_templatesGroupId`.
- `data-ac-select` - when set to `true` in autocomplete behaves similarly to a selection field - when you click the mouse in the input field, all options are loaded and displayed.
- `data-ac-collision` - the location of the loaded options relative to the input field. Default `flipfit` for automatic placement, for the possibility `select` is preset to `none` for strict placement under the input field.

An example of a REST service returning data is in [ConfigurationController.getAutocomplete](../../../src/main/java/sk/iway/iwcm/components/configuration/ConfigurationController.java), the implementation is simple - based on the specified `term` parameter returns a list of `List<String>` matching records:

```java
@GetMapping("/autocomplete")
public List<String> getAutocomplete(@RequestParam String term) {
    List<String> ac = new ArrayList<>();
    //na zaklade termu prehladaj zaznamy a do listu dopln len vyhovujuce
    if (...contains(term)) ac.add(...);
    return ac;
}
```

Since LIKE searches are typically used on the backend, it is possible to enter a character in the search `%` to see all results. However, this is untypical for users, so when entering a space or a character `*` the value is substituted into the search for the character `%` to view all records.

## Use outside the datatable

`Autocompleter` can also be used outside the datatable simply by setting `data-ac` attributes and CSS classes `autocomplete`. Initialization is automatically activated in [app-init.js](../../../src/main/webapp/admin/v9/src/js/app-init.js) to all `input` elements with CSS class `autocomplete`. Example:

```html
<div id="docIdInputWrapper" class="col-auto col-pk-input">
	<label for="tree-doc-id">Doc ID: </label>
	<input type="text" autocomplete="off" class="js-tree-doc-id__input autocomplete" id="tree-doc-id" data-ac-name="docid" data-ac-url="/admin/skins/webjet6/_doc_autocomplete.jsp" data-ac-click="fireEnter" />
</div>
```

## Notes on implementation

Autocomplete uses [jQuery-ui-autocomplete](https://api.jqueryui.com/autocomplete/) functions. Internally it is encapsulated in a JavaScript class [AutoCompleter](../../../src/main/webapp/admin/v9/src/js/autocompleter.js). This is modified from the original version in WebJET 8, it should be backwards compatible (it is also possible to use the URLs of the original autocomplete services in WebJET 8).

The function is added `autobind()`, which takes the settings from the data attributes of the specified input element. The autocomplete initialization is implemented in index.js in the code:

```javascript
//nastav autocomplete
$("#" + DATA.id + "_modal input.form-control[data-ac-url]").each(function () {
	var autocompleter = new AutoCompleter("#" + $(this).attr("id")).autobind();
	$(this).closest("div.DTE_Field").addClass("dt-autocomplete");
});
```

whereby as can be seen `div.DTE_Field` element is also set CSS class `dt-autocomplete` for future styling of the element.

Function set via `click` parameter is called with a delay of 100ms to first set a value in the array that can then be retrieved and used.
