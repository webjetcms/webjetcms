# Attributes, conditions, and cycles

## Basic text/attribute listing

The text cannot be printed directly, it must be placed in, for example, a ```span``` wrapper with the ```data-th-text``` attribute, which will replace the content of the ```span``` element (so that it can be prototyped).
Attributes are set similarly, e.g. ```data-th-href=...```

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

To insert **HTML code (without escaping characters)**, you need to use the ```data-th-utext``` attribute.

in JavaScript code, the value can be assigned as follows:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.title = "[[${docDetails.title}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.title = [(${docDetails.title})];
```

If you then need to [remove the entire tag](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#removing-template-fragments) you can use the ```data-th-remove="tag"``` attribute:

```html
<div data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag"></div>
```

If you need to list [simple attribute](webjet-objects.md) from an ```request``` object, you can use:

```html
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>
```

## Translation text

The translation text is written in the form ```#{prekladovy.kluc}```.

```html
<span data-th-text="#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[#{menu.top.help}]]
```

## Connecting chains

```html
<img
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
>
```

In addition, you can also use [Literal substitutions](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#literal-substitutions):

```html
<span data-th-text="|Welcome to our application, ${docDetails.title}!|">
```

!>**Warning:** if you get an error like: ```Could not parse as expression: "aitem--open md-large-menu"```, it's because of ```__```. That's a special tag for the [pre-processor](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#preprocessing)
and it is necessary to escape it as `\\\\_`, example:

```html
<div data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'">
```

## Cycle

For [cycle](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#iteration) the ```data-th-each``` tag is used:

```html
<select data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}">
    <option data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}"></option>
</select>
```

## Condition

The ```data-th-if``` attribute ensures that ```tagu``` is displayed only when the [specified condition](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#conditional-evaluation) is met.

```html
<i data-th-if="${!docDetails.available}" class="ti ti-chevron-down"></i>
```

## Includes

Using the ```data-th-replace``` attribute, you can easily [insert an include of another HTML](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#including-template-fragments) file into your HTML template:

```html
<header class="ly-header">
    <div data-th-replace="/templates/bare/includes/header.html"></div>
</header>
```

The following attributes can be used:

- ```data-th-insert``` - ​​inserts the specified file into the tag body
- ```data-th-replace``` - ​​replaces the entire tag with the specified file

## Calling a static method

If you need to call a static method you can use the `T()` function:

```html
<p>date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(demoComponent.date)}"></span></p>
<p class="currentDate">current date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(T(sk.iway.iwcm.Tools).getNow())}"></span></p>
```

## Inserting the current version

WebJET initializes the value in `CombineTag.getVersion()` to the current timestamp when it starts. This value will also change when all cache objects are deleted. It can be used in the `?v=` parameter to load new versions of files after a server restart. When using `data-iwcm-combine`, the `?v=` parameter is added automatically. However, you can also insert it manually:

```html
<script data-th-src="${'/components/aceintegration/admin/pagesupport-custom.js?v=' + version}"></script>
```