# Field Type - elfinder/file selection

The elfinder data field integrates the selection of a file reference using the elfinder/files application. The field appears as a text field, with a pencil icon at the end. Clicking on the icon opens the elfinder/file selection dialog.

For a field containing a link to an image, a preview of the image is displayed at the beginning of the field.

The field value is displayed with a grey background, but the field is not of type `disabled`, the value can be modified if necessary, or inserted from the clipboard. The gray color is chosen to guide the user to click on the link selection icon.

![](field-type-elfinder.png)

## Use of annotation

Annotation is used as `DataTableColumnType.ELFINDER`, and the following attributes can be set:
- `className` - if it contains the value `image` a preview of the selected image is displayed at the beginning of the field (or a blank image icon if the image is not yet selected)

Full annotation example:

```java
//vyber obrazka = className="image"
@DataTableColumn(inputType = DataTableColumnType.ELFINDER, title="editor.perex.image",
    tab = "perex", sortAfter = "perexPlace",
    className = "image",
)
String perexImage = "";

//vyber odkazu na subor/inu web stranku = className je prazdne/nenastavene
@DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "[[#{editor.external_link}]]",
        tab = "basic"
)
private String externalLink;
```

## Notes on implementation

The implementation is in the file [field-type-elfinder.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-elfinder.js). According to `className` generates the appropriate HTML code for the input field. Into the variable `conf._prepend` stores a reference to the prepend element (`div.input-group-prepend .input-group-text`) with a preview image. Feature `setValue(conf, val)` is used to set the value of the field and also to set the preview image (if it is of type `.jpg` or `.png`).

The preview image is set as `background-image`, at the same time is `prepend` element set CSS class `has-image`.

Opening the elfinder window is provided by calling the function [WJ.openElFinder](../frameworks/webjetjs.md#iframe-dialog) with set `callback` to function `setValue`.
