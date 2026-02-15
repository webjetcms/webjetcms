# CKEditor

When editing web pages is used [CKEditor with our own modifications](https://github.com/webjetcms/libs-ckeditor4/) and extensions.

## Configuration variables

The following configuration variables are supported:
- `editorAutomaticWordClean` - if set to `true`, the HTML code is automatically cleaned when the text is inserted. The user has the option to paste the text as plain text.
- `editorFontAwesomeCssPath` - setting the path to [FontAwesome](../webpages/fontawesome/README.md).
- `ckeditor_toolbar` - setup toolbar items for the website section, values are in JSON format.
- `ckeditor_toolbar-standalone` - setting toolbar items for inserting the editor into different data tables, values are in JSON format.
- `ckeditor_removeButtons` - list of buttons you want to hide (not show) in the editor, no need to edit the settings `toolbar`, just set a comma separated list here.

Settings for tables:
- `ckeditor_table_class` - Default CSS class for tables in CKEditor, by default `table table-sm tabulkaStandard`.
- `ckeditor_table_cols` - Default number of table columns in CKEditor, by default 5.
- `ckeditor_table_rows` - Default number of table rows in CKEditor, by default 2.
- `ckeditor_table_width` - Default table width in CKEditor, by default 100%.
- `ckeditor_table_height` - Default table height in CKEditor.
- `ckeditor_table_border` - Default table border value in CKEditor, by default 1.
- `ckeditor_table_cellpadding` - Default value `cellpadding` tables in CKEditor, by default 1.
- `ckeditor_table_cellspacing` - Default value `cellspacing` tables in CKEditor, by default 1.
- `ckeditor_table_wrapper_class` - CSS wrapper class for table in CKEditor, by default `table-responsive`. If empty, the table will not wrap to the responsive container.

## PICTURE element

If you need support in your project for `PICTURE` element, it is enough if the configuration `ckeditor_toolbar` add a value to the appropriate place `WebjetPicture`. The icon is automatically added even if you have set a custom value in the configuration variable `ckeditor_pictureDialogBreakpoints`.

![](picture-element.png)

Values `breakpoints` can be set via the configuration variable `ckeditor_pictureDialogBreakpoints`, Example:

```json
[
    {
        name: "Desktop",
        minWidth: 992,
        fallback: true
    },
    {
        name: "Tablet",
        minWidth: 768
    },
    {
        name: "Mobile",
        minWidth: 0
    },
    {
        name: "XL",
        minWidth: 1240
    },
    {
        name: "XXL",
        minWidth: 1380
    }
]
```

A value that has the attribute set `fallback: true` shall also be inserted in the backup `IMG` element for browsers that `PICTURE` they don't know.

## SVG icons

CKEditor supports the use of SVG icons instead of classic PNG icons. To enable this feature, you need to set the configuration variable `ckeditor_svgIcon_path` the path to the SVG sprite file that contains the icon definitions.

![](svgicon.png)

Optionally, you can also set other configuration variables to customize the icons:
- `ckeditor_svgIcon_icons` - JSON object defining icons for CKEditor, see example below. If empty it is retrieved from the SVG file by the value of the element ID, groups are not available in this case.
- `ckeditor_svgIcon_width` - SVG icon width in pixels.
- `ckeditor_svgIcon_height` - SVG icon height in points.
- `ckeditor_svgIcon_sizes` - Available icon sizes separated by a comma, e.g. `small,medium,large,xlarge,xxlarge,huge`.
- `ckeditor_svgIcon_colors` - Available icon colours separated by a comma, e.g. `info,success,warning,danger,orange`.

Example of setting `ckeditor_svgIcon_icons` including groups:

```json
{
    "4g": ["other"],
    "5g": ["other"],
    "airbox-auto": ["vehicles", "waves"],
    "alias-numbers": ["people"],
    "android-manage": ["other"],
}
```

The SVG file must contain icon definitions with the set ID attribute, example:

```xml
<?xml version="1.0" encoding="utf-8"?>
<svg
	xmlns="http://www.w3.org/2000/svg"
	xmlns:xlink="http://www.w3.org/1999/xlink">
	<symbol viewBox="0 0 80 80" id="4g"
		xmlns="http://www.w3.org/2000/svg">
		<path fill-rule="evenodd" clip-rule="evenodd" d="M22.56 29.6 12.32 43.36h10.4V29.6h-.16ZM6 50v-7.44l17.2-23.04h7.2v23.76h5.2v6.64h-5.2v9.2h-7.68V50H6Zm56.102 8.809A15.027 15.027 0 0 1 56.24 60c-3.092 0-5.837-.512-8.306-1.587-2.47-1.075-4.553-2.554-6.25-4.44-1.697-1.886-2.998-4.1-3.903-6.646-.905-2.545-1.357-5.288-1.357-8.23 0-3.016.453-5.815 1.357-8.399.905-2.582 2.206-4.835 3.903-6.758 1.697-1.924 3.78-3.432 6.25-4.525 2.47-1.093 5.25-1.64 8.343-1.64a19.63 19.63 0 0 1 6.024.933c1.9.6 3.68 1.529 5.26 2.743a15.043 15.043 0 0 1 3.874 4.468c1.018 1.773 1.64 3.81 1.867 6.109h-8.485c-.529-2.262-1.547-3.96-3.054-5.09-1.509-1.132-3.338-1.697-5.487-1.697-1.999 0-3.696.386-5.09 1.16a9.607 9.607 0 0 0-3.394 3.11c-.867 1.3-1.5 2.78-1.895 4.44a22.105 22.105 0 0 0-.593 5.147c-.004 1.668.196 3.33.593 4.949a13.352 13.352 0 0 0 1.895 4.327 9.696 9.696 0 0 0 3.394 3.082c1.394.773 3.091 1.16 5.09 1.16 2.941 0 5.213-.745 6.815-2.235 1.603-1.489 2.565-3.633 2.829-6.461h-8.96v-6.64h17.12L74 59.12h-5.76l-.878-4.581c-1.584 2.036-3.337 3.46-5.26 4.27Z"/>
	</symbol>
	<symbol viewBox="0 0 80 80" id="5g"
		xmlns="http://www.w3.org/2000/svg">
		<path fill-rule="evenodd" clip-rule="evenodd" d="m14.905 26.135-1.723 9.807.123.123a10.882 10.882 0 0 1 3.826-2.56 12.98 12.98 0 0 1 4.629-.77c2.138 0 4.03.389 5.673 1.17a12.52 12.52 0 0 1 4.167 3.146c1.127 1.316 1.993 2.873 2.59 4.658.603 1.84.908 3.767.894 5.71a14.909 14.909 0 0 1-1.295 6.196 15.805 15.805 0 0 1-3.487 4.967 15.41 15.41 0 0 1-5.091 3.236 15.878 15.878 0 0 1-6.167 1.08 21.46 21.46 0 0 1-6.015-.833 15.01 15.01 0 0 1-5.09-2.56 12.826 12.826 0 0 1-3.546-4.287c-.891-1.705-1.35-3.69-1.393-5.953h8.764c.203 1.975.945 3.55 2.218 4.72 1.273 1.171 2.898 1.757 4.873 1.757 1.152 0 2.189-.236 3.116-.71a7.394 7.394 0 0 0 2.345-1.85 8.329 8.329 0 0 0 1.48-2.655 9.891 9.891 0 0 0 .03-6.261 7.37 7.37 0 0 0-1.48-2.586A6.965 6.965 0 0 0 22 39.953a7.555 7.555 0 0 0-3.145-.618c-1.564 0-2.837.276-3.826.832-.985.557-1.913 1.43-2.774 2.618H4.356l4.255-23.869h24.123v7.219H14.905ZM64.2 61.753a16.235 16.235 0 0 1-6.353 1.327c-3.374 0-6.407-.585-9.102-1.76a19.504 19.504 0 0 1-6.818-4.84 21.426 21.426 0 0 1-4.254-7.247 26.61 26.61 0 0 1-1.48-8.978c0-3.288.49-6.342 1.48-9.16a22.019 22.019 0 0 1 4.254-7.371 19.441 19.441 0 0 1 6.819-4.935c2.689-1.193 5.726-1.789 9.1-1.789 2.229-.004 4.445.34 6.568 1.018a18.562 18.562 0 0 1 5.738 2.993 16.327 16.327 0 0 1 4.229 4.873c1.109 1.93 1.789 4.152 2.036 6.661h-9.254c-.582-2.469-1.688-4.32-3.335-5.552-1.644-1.237-3.636-1.851-5.982-1.851-2.181 0-4.032.422-5.553 1.265a10.457 10.457 0 0 0-3.701 3.393c-.946 1.418-1.637 3.033-2.07 4.844a24.096 24.096 0 0 0-.647 5.61c0 1.819.215 3.637.648 5.4a14.614 14.614 0 0 0 2.069 4.72 10.557 10.557 0 0 0 3.701 3.36c1.52.844 3.372 1.266 5.553 1.266 3.207 0 5.684-.815 7.433-2.436 1.745-1.626 2.764-3.978 3.055-7.066h-9.746v-7.21h18.506v23.81h-6.171l-.986-4.996c-1.727 2.218-3.64 3.767-5.738 4.65Z"/>
	</symbol>
</svg>
```

Sample `sprite.svg` you can get the file in [@orangesk/orange-design-system](https://www.npmjs.com/package/@orangesk/orange-design-system?activeTab=code) package in the way `build/sprite.svg`.

Clicking on an existing icon in the toolbar will open a dialog for setting it, where you can select the size, color and a specific icon from the available icons defined in the SVG file. Right-clicking on the icon will show the option to delete it with confirmation.

## Button

The button is often used as a `call to action` element on the website. WebJET supports element insertion `button` or `a` with class `btn` via a custom button in CKEditor. For `button` the following configuration variables can be set:
- `ckeditor_button_baseClass` - Basic CSS class for buttons in CKEditor, by default `btn`.
- `ckeditor_button_sizes` - Available button sizes separated by a comma, standard `btn-lg,btn-sm`.
- `ckeditor_button_types` - Available button colours/types separated by a comma, standard `btn-primary,btn-secondary,btn-success,btn-danger,btn-warning,btn-info,btn-light,btn-dark,btn-link,btn-outline-primary,btn-outline-secondary,btn-outline-success,btn-outline-danger,btn-outline-warning,btn-outline-info,btn-outline-light,btn-outline-dark`.
- `ckeditor_button_textHiddenClass` - CSS class for hiding button text - displaying only the icon, by default `visually-hidden`.
- `ckeditor_button_allowedClasses` - A comma-separated list of allowed CSS classes to open the button settings dialog. An empty value will enable all CSS classes. Example `btn-primary,btn-secondary,btn-lg`.
- `ckeditor_button_deniedClasses` - A comma-separated list of disabled CSS classes to open the button settings dialog. An empty value will not disable any CSS classes. Example `no-button,no-btn`.
- `ckeditor_button_attrs` - Comma-separated list of configurable button attributes, by default `data-bs-toggle,data-bs-target,aria-controls,aria-expanded,aria-label`.

SVG icons are also supported and can be embedded in the button, see section above.

If you set the button as disabled - `disabled`, it is not possible to click on it in the editor to open a dialog for editing the button properties. However, you can click the first button and select Button Properties from the context menu.

The insert button option is displayed in the form insert selection menu. You can add it to the toolbar by adding a value `WebjetFormButton` to the configuration variable `ckeditor_toolbar`. It will appear automatically if you have configured SVG icon embedding.

## Custom plugins

CKEditor supports adding your own plugins. After creating a custom plugin, you need to add it to the configuration variable `ckeditor_extraPlugins` as a comma-separated list. For more information on creating custom add-ins, see [CKEditor documentation](https://ckeditor.com/docs/ckeditor4/latest/guide/plugin_sdk_intro.html).

Place the resulting plugin in the directory `src/main/webapp/admin/skins/webjet8/ckeditor/dist/plugins/` your project.

Then add the button to the configuration variable `ckeditor_toolbar` to a suitable location.
