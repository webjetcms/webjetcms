# Page Builder

## Editor initialization

When editing a page, the editor is initialized to individual editable blocks, meaning **multiple editors are initialized on the page at the same time**. The Page Builder is used to edit the page structure using its tools for working with blocks. The CK editor is used to edit the texts themselves.

The editor is automatically initialized to:

- column elements (```class="col-*"```)
- elements marked with CSS class ```pb-editable``` (```class="pb-editable"```)

**Setup and configuration:**

To run Page Builder correctly, set:

- in the template group properties, set Page Editor Type to Page Builder.
- if you want the Page Builder to be available even when the page is displayed on the frontend, set the configuration variable ```inlineEditingEnabled``` to ```true```
- in case of non-standard ```bootstrap gridu```, configuration variable ```bootstrapColumns```, which determines the number of columns ```gridu```

Other config variables that can be edited:

- `pagebuilderFilterAutoOpenItems` - ​​number of items that will automatically open when filtering in the block list, default 10.
- `pagebuilderLibraryImageWidth` - ​​width of preview images in the block library, default 310.
- `inlineEditingDisabledUrls` - ​​list of URLs for which the inline editor will not be available
- `pageBuilderPrefix` - ​​prefix used for Page Builder CSS classes (pb by default), can only be changed if you also change the prefixes in Page Builder CSS classes

**Exceptions**

The mode is set for a group of templates. If you need to disable the mode in a **specific template**, you can set a different option in the `Typ editora stránok` field in the template on the Basic tab than the one used by the group of templates.

**Headers/Footers**

The Page Builder in the editor is initialized to the entire page, it is loaded including CSS styles and template into an iframe element. In this mode, however, we do not want to display other elements of the web page (header, footer...) so these elements are set to an empty value. In addition, when the page is displayed, the following elements are hidden via the CSS attribute ```display: none```:

```css
 body div.header, body header, body div.footer, body footer { display: none; }
```

## CSS class convention with support for Ninja page builder

The correct HTML code structure that Page Builder can initialize on is as follows:

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

Styling is recommended only for ```section```, ```container``` and ```column content```, because only these elements can be edited by the user using Page Builder.

!>**Warning:** in ```column``` elements it is not allowed to use text directly, at least a P element must be used.
Additionally, due to the ability to set indents (```margin/padding```) within the ```column``` element, the content of the element is wrapped in a DIV element with the CSS class ```column-content``` when opened in Page Builder.

So from the code:

```html
<div class="col-4">
   Some content
</div>
```

The following code is created after the Page Builder initialization:

```html
<div class="col-4">
    <div class="column-content">
        <p>Some content</p>
    </div>
</div>
```

## Styling elements

### `SECTION` (blue color)

Initialization when using element: ```<section>```.

Styling using a class, with prefix: ```pb-style-section-```

```html
<section class="pb-style-section-team-26"></section>
```

By setting the CSS class ```pb-not-section```, the element **will not be considered a section* element.

### `CONTAINER` (red color)

Initialization when using CSS class: ```container``` or ```pb-custom-container```. By setting CSS class ```pb-not-container```, the element **will not be considered a container** even if it has CSS class ```container```.

Styling using a class, with prefix: ```pb-style-container-```

```html
<div class="container pb-style-container-group-26"></div>
```

### `ROW`

```<div class="row">``` sa momentálne nedá editovať pomocou Page Builder, je použitý z dôvodu bootstrap kompatibility.

### `COLUMN` (zelená farba)

Inicializácia pri použití triedy: ```col-``` ALEBO ```pb-col-``` (ak DIV element nie je štandardný bootstrap ```col-```). Akceptované sú aj hodnoty ```pb-col``` a ```pb-col-auto``` - ak element obsahuje tieto CSS štýly, tak v nástrojovej lište sa nezobrazí ikona nastavenia šírky stĺpca.

Ak má column CSS triedu ```pb-not-editable``` tak sa **nebude považovať za column** (nebude v ňom automaticky aj editovateľný text). Ak pre ne-editovateľný element potrebujete mať možnosť nastavovania šírky/kopírovania/presúvania atď. nastavením CSS triedy ```pb-always-mark``` sa element označí a bude sa zobrazovať zelený rámik aj s jeho možnosťami.

Štýlovanie pomocou triedy, s prefixom: ```pb-style-column-```.

```html
<div class="col-12 pb-style-content-person-26"></div>
```

By setting the CSS class ```pb-not-column```, the element **will not be considered columns* even if it has the CSS class ```col-```.

## Editing exceptions

### Editable element

If editing an element that is not among those mentioned above is required, an element with the class ```pb-editable``` can be used.

This element will not be styled using Page Builder, but will be editable using the CK editor.

```html
<div class="pb-editable"></div>
```

### Non-editable element

If an element is required that you do not want to be editable, the ```pb-not-editable``` class can be used.

This will disable the initialization of Page Builder and CK editor on the given element and all elements within it.

```html
<section class="pb-not-editable"></section>
```

## Unique ID

If you need a unique ID to be used in blocks, you can use the value ```__ID__```, which will be replaced with a random value (```timestamp```) in the HTML code after insertion into the page.

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

PageBuilder raises several events on the ```windows``` object while it is working. You can listen to them as follows:

```javascript
window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
    //
});
```

The following events are currently supported:

- ```WJ.PageBuilder.loaded``` - ​​after loading the page in the editor
- ```WJ.PageBuilder.gridChanged``` - ​​change in ```gride```
- ```WJ.PageBuilder.styleChange``` - ​​change in block properties (styling)
- ```WJ.PageBuilder.newElementAdded``` - ​​new element added
- ```WJ.PageBuilder.elementDuplicated``` - ​​duplicate element
- ```WJ.PageBuilder.elementMoved``` - ​​moved element

## Showing unnecessary blocks

The Page Builder also displays blocks such as header, footer, menu, etc., which may not be desirable in some cases. A simple solution is to hide these blocks via CSS style. In editor mode, the `body` element has a CSS class `is-edit-mode` that allows you to hide unnecessary elements.

If you just need to disable links, you can set the CSS style `pointer-events: none`:

```css
.is-edit-mode .header a {
    pointer-events: none;
}
```