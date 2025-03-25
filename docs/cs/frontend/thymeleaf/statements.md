# Atributy, podmínky a cykly

## Základní výpis textu / atributu

Text nelze vypsat přímo, je nutné jej dát do např. `span` obalovače s atributem `data-th-text`, co nahradí obsah `span` elementu (aby se dalo prototypovat). Podobně se nastavují i atributy, tzn. `data-th-href=...`

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

Pro vložení **HTML kódu (bez escapingu znaků)** je třeba použít atribut `data-th-utext`.

v JavaScript kódu lze hodnotu přiřadit následovně:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.title = "[[${docDetails.title}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.title = [(${docDetails.title})];
```

Potřebujete-li následně [odstranit i celý tag](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#removing-template-fragments) můžete použít atribut `data-th-remove="tag"`:

```html
<div data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag"></div>
```

Pokud potřebujete vypsat [jednoduchý atribut](webjet-objects.md) z `request` objektu, můžete použít:

```html
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>
```

## Překladový text

Překladový text se zapisuje ve formě `#{prekladovy.kluc}`.

```html
<span data-th-text="#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[#{menu.top.help}]]
```

## Spojování řetězců

```html
<img
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
>
```

kromě toho lze použít i [Literal substitutions](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#literal-substitutions):

```html
<span data-th-text="|Welcome to our application, ${docDetails.title}!|">
```

!>**Upozornění:** pokud vám vyhodí chybu typu: `Could not parse as expression: "aitem--open md-large-menu"`, je to kvůli `__`. To je speciální značka pro [pre-procesor](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#preprocessing) a je třeba to escapovat jako \\\\\_, příklad:

```html
<div data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'">
```

## Cyklus

Pro [cyklus](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#iteration) se používá značka `data-th-each`:

```html
<select data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}">
    <option data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}"></option>
</select>
```

## Podmínka

Atribut `data-th-if` zajistí zobrazení `tagu` jedině při splnění [zadané podmínky](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#conditional-evaluation).

```html
<i data-th-if="${!docDetails.available}" class="ti ti-chevron-down"></i>
```

## Include

Pomocí atributu `data-th-replace` víte do HTML šablony jednoduše [vložit include jiného HTML](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#including-template-fragments) souboru:

```html
<header class="ly-header">
    <div data-th-replace="/templates/bare/includes/header.html"></div>
</header>
```

Lze použít následující atributy:
- `data-th-insert` - do těla tagu vloží zadaný soubor
- `data-th-replace` - celý tag nahradí zadaným souborem

## Volání statické metody

Pokud potřebujete volat statickou metodu můžete použít `T()` funkci:

```html
<p>date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(demoComponent.date)}"></span></p>
<p class="currentDate">current date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(T(sk.iway.iwcm.Tools).getNow())}"></span></p>
```
