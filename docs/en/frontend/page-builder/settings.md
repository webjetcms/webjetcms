# Page Builder

## Editor initialization

When editing a page, the editor is initialized to individual editable blocks, i.e. **multiple editors are initialized in the page at the same time**. Page Builder is used to modify the structure of a page using its block tools. The CK editor is used to edit the text itself.

The editor is automatically initialized to:
- column elements (`class="col-*"`)
- elements marked with CSS class `pb-editable` (`class="pb-editable"`)

**Setup and configuration:**

To run Page Builder correctly, set:
- in the properties of the template group Page editor type to Page Builder
- if you want Page Builder to be available also when the page is displayed on the frontend, set the configuration variable `inlineEditingEnabled` at `true`
- in case of non-standard `bootstrap gridu`, configuration variable `bootstrapColumns` which specifies the number of columns `gridu`

Other conf. variables that can be modified:
- `pagebuilderFilterAutoOpenItems` - the number of items that are automatically opened when filtering in the block list, by default 10.
- `pagebuilderLibraryImageWidth` - width of preview images in the block library, 310 by default.
- `inlineEditingDisabledUrls` - list of URLs for which the inline editor will not be available
- `pageBuilderPrefix` - prefix used for CSS Page Builder classes (pb by default), can only be changed if you also change prefixes in CSS Page Builder classes

**Exceptions**

The mode is set for a group of templates if you need to **specific template mode to turn off** you can set another option in the template in the Basic tab in the field `Typ editora stránok` as used by the template group.

**Headers/heels**

Page Builder in the editor is initialized to the full page, it is loaded including CSS styles and template into the iframe element. But in this mode we don't want to display other elements of the web page (header, footer...) so these elements are set to empty value. Additionally, when the page is displayed, the CSS attribute `display: none` the following elements are hidden:

```css
 body div.header, body header, body div.footer, body footer { display: none; }
```

## CSS class convention with support for Ninja page builder

The correct html code structure over which Page Builder can initialize is as follows:

```html
<section>
    <div class="container">
        <div class="pb-editable">
            <h1>Some editable heading</h1>
        </div>
        <p>
            Some NOT editable text
        </p>
        <p class="pb-editable">
            Editable paragraph
        </p>
        <div class="row">
            <div class="col-4">
               <p>Some editable paragraph</p>
            </div>
            <div class="col-4 pb-not-editable">
               <p>Some not editable content</p>
            </div>
            <div class="col-4">
               <p>Some editable content</p>
            </div>
        </div>
    </div>
</section>
```

Styling is only recommended on `section`, `container` a `column content` because only these elements can be customized by the user using Page Builder.

!>**Warning:** v `column` elements are not allowed to use text directly, it is necessary to use at least P element. Additionally, because of the possibility of setting indentations (`margin/padding`) in the framework of `column` element content is wrapped in a DIV element with CSS class after opening in Page Builder `column-content`.

That is, from code:

```html
<div class="col-4">
   Some content
</div>
```

will be created after initializing the Page Builder code:

```html
<div class="col-4">
    <div class="column-content">
        <p>Some content</p>
    </div>
</div>
```

## Styling of elements

### `SECTION` (blue colour)

Initialization when using the element: `<section>`.

Styling by class, with prefix: `pb-style-section-`

```html
<section class="pb-style-section-team-26"></section>
```

By setting the CSS class `pb-not-section` the element \**will not be considered as a section* element.

### `CONTAINER` (red)

Initialization when using CSS classes: `container` or `pb-custom-container`. By setting the CSS class `pb-not-container` with the element **will not be considered as a container** even if it has a CSS class `container`.

Styling by class, with prefix: `pb-style-container-`

```html
<div class="container pb-style-container-group-26"></div>
```

### `ROW`

`<div class="row">` is currently not editable with Page Builder, it is used for bootstrap compatibility reasons.

### `COLUMN` (green colour)

Initialization when using the class: `col-` OR `pb-col-` (if the DIV element is not a standard bootstrap `col-`). Values are also accepted `pb-col` a `pb-col-auto` - if the element contains these CSS styles, the column width setting icon will not appear in the toolbar.

If the column has a CSS class `pb-not-editable` so with **will not be considered as a column** (it will not automatically include editable text). If for a non-editable element you need to be able to set the width/copy/move etc. by setting the CSS class `pb-always-mark` the element will be selected and a green box with its options will be displayed.

Styling by class, with prefix: `pb-style-column-`.

```html
<div class="col-12 pb-style-content-person-26"></div>
```

By setting the CSS class `pb-not-column` the element \**will not be considered as columns* even if it has a CSS class `col-`.

## Editing exceptions

### Editable element

If editing is required for an element that is not one of the above, it is possible to use an element with the class `pb-editable`.

This element will not be stylable with Page Builder, but will be editable with the CK editor.

```html
<div class="pb-editable"></div>
```

### Non-editable element

If an element is required that you do not want to be editable, it is possible to use the class `pb-not-editable`.

This disables initialization of Page Builder and CK editor on the element and all elements in it.

```html
<section class="pb-not-editable"></section>
```

## Unique ID

If you need a unique ID to be used in the blocks you can use the value `__ID__`, which is replaced by a random value (`timestamp`).

```html
<div class="carousel slide" id="carouselControls__ID__" data-bs-ride="carousel">
    <div class="carousel-inner row">
        ...
    </div>
    <button class="carousel-control-prev" data-bs-slide="prev" data-bs-target="#carouselControls__ID__" type="button"></button>
    <button class="carousel-control-next" data-bs-slide="next" data-bs-target="#carouselControls__ID__" type="button"></button>
</div>
```

## Events

PageBuilder will call the `windows` several events. You can listen to them as follows:

```javascript
window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
    //
});
```

The following events are currently supported:
- `WJ.PageBuilder.loaded` - after uploading the page in the editor
- `WJ.PageBuilder.gridChanged` - change in `gride`
- `WJ.PageBuilder.styleChange` - change in block properties (styling)
- `WJ.PageBuilder.newElementAdded` - new element added
- `WJ.PageBuilder.elementDuplicated` - duplicated element
- `WJ.PageBuilder.elementMoved` - moved element
