# Attributes, conditions and cycles

## Basic text/attribute listing

The text cannot be typed out directly, it must be put into e.g. `span` wrappers with attribute `data-th-text`which will replace the content `span` element (to be prototyped). Similarly, attributes are set, e.g. `data-th-href=...`

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

For insertion **HTML code (without escaping characters)** the attribute must be used `data-th-utext`.

in JavaScript code, the value can be assigned as follows:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.title = "[[${docDetails.title}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.title = [(${docDetails.title})];
```

If you need to subsequently [also remove the entire tag](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#removing-template-fragments) you can use the attribute `data-th-remove="tag"`:

```html
<div data-th-utext="${ninja.temp.insertTouchIconsHtml}" data-th-remove="tag"></div>
```

If you need to write out [simple attribute](webjet-objects.md) z `request` object, you can use:

```html
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css" />
```

## Translation text

The translation text shall be written in the form `#{prekladovy.kluc}`.

```html
<span data-th-text="#{menu.top.help}">Pomocník</span> alebo priamo ako text: [[#{menu.top.help}]]
```

## Linking strings

```html

<img data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}" data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}" />

```

in addition, it is also possible to use [Literal substitutions](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#literal-substitutions):

```html
<span data-th-text="|Welcome to our application, ${docDetails.title}!|"></span>
```

WARNING: if it throws an error like: `Could not parse as expression: "aitem--open md-large-menu"`, it's because of `__`. This is a special brand for [pre-processor](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#preprocessing) and it needs to be escaped as \\\\\_, example:

```html
<div data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'"></div>
```

## Cycle

For [cycle](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#iteration) the mark is used `data-th-each`:

```html
<select data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}">
	<option data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}"></option>
</select>
```

## Condition

Attribute `data-th-if` ensure the display of `tagu` only when meeting [the specified condition](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#conditional-evaluation).

```html
<i data-th-if="${!docDetails.available}" class="fas fa-chevron-down"></i>
```

## Include

Using the attribute `data-th-replace` you can easily add to HTML templates [insert include of other HTML](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#including-template-fragments) file:

```html
<header class="ly-header">
	<div data-th-replace="/templates/bare/includes/header.html"></div>
</header>
```

The following attributes can be used:
- `data-th-insert` - inserts the specified file into the body of the tag
- `data-th-replace` - replaces the entire tag with the specified file
