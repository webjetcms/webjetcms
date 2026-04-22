# Field Type - elfinder/file selection

The elfinder data field integrates file link selection using the elfinder/files application. The field is displayed as a text field, with a pencil icon at the end. Clicking on the icon opens the elfinder/file selection dialog.

For a field containing an image link, a preview of the image is displayed at the beginning of the field.

The field value is displayed with a gray background, but the field is not of type ```disabled```, if necessary, the value can be edited or pasted from the clipboard. The gray color is chosen to guide the user to click on the link selection icon.

![](field-type-elfinder.png)

## Using annotation

The annotation is used as ```DataTableColumnType.ELFINDER```, and the following attributes can be set:

- ```className``` - ​​if it contains the value ```image```, a preview of the selected image will be displayed at the beginning of the field (or an empty image icon if the image is not yet selected)

Complete example of annotation:

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

## Setting

Using the `className` attribute, it is possible to set additional filtering of displayed files:

- `image` - ​​only files whose `mime-type` starts with `image/` will be displayed.
- `video` - ​​only files whose `mime-type` starts with `video/` will be displayed.
- `multimedia` - ​​only files whose `mime-type` starts with `image/` or `video/` will be displayed.

The `mime-type` setting is read from the [mime.types](../../../../src/main/webapp/WEB-INF/mime.types) file.

## Implementation notes

The implementation is in the file [field-type-elfinder.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-elfinder.js). According to ```className```, it generates the appropriate HTML code for the input field. In the variable ```conf._prepend```, it stores a reference to the prepend element (```div.input-group-prepend .input-group-text```) with the preview image. The function ```setValue(conf, val)``` is used to set the field value and also to set the preview image (if it is of type ```.jpg``` or ```.png```).

The preview image is set as ```background-image```, while the ```prepend``` element has the CSS class ```has-image``` set.

Opening the elfinder window is ensured by calling the function [WJ.openElFinder](../frameworks/webjetjs.md#iframe-dialog) with ```callback``` set to the function ```setValue```.