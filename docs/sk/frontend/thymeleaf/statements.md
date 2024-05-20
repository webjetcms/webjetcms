# Atribúty, podmienky a cykly

## Základný výpis textu / atribútu

Text nie je možné vypísať priamo, je nutné ho dať do napr. ```span``` obaľovača s atribútom ```data-th-text```, čo nahradí obsah ```span``` elementu (aby sa dalo prototypovať).
Podobne sa nastavujú aj atribúty, čiže napr. ```data-th-href=...```

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

Pre vloženie **HTML kódu (bez escapingu znakov)** je potrebné použiť atribút ```data-th-utext```.

v JavaScript kóde je možné hodnotu priradiť nasledovne:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.title = "[[${docDetails.title}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.title = [(${docDetails.title})];
```

Ak potrebujete následne [odstrániť aj celý tag](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#removing-template-fragments) môžete použiť atribút ```data-th-remove="tag"```:

```html
<div data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag"></div>
```

Ak potrebujete vypísať [jednoduchý atribút](webjet-objects.md) z ```request``` objektu, môžete použiť:

```html
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>
```

## Prekladový text

Prekladový text sa zapisuje vo forme ```#{prekladovy.kluc}```.

```html
<span data-th-text="#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[#{menu.top.help}]]
```

## Spájanie reťazcov

```html
<img
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
>
```
okrem toho je možné použiť aj [Literal substitutions](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#literal-substitutions):

```html
<span data-th-text="|Welcome to our application, ${docDetails.title}!|">
```

POZOR: ak vám vyhodí chybu typu: ```Could not parse as expression: "aitem--open md-large-menu"```, je to kvôli ```__```. To je špeciálna značka pre [pre-procesor](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#preprocessing)
a je potrebne to escapovať ako \\\\_, príklad:

```html
<div data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'">
```

## Cyklus

Pre [cyklus](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#iteration) sa používa značka ```data-th-each```:

```html
<select data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}">
    <option data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}"></option>
</select>
```

## Podmienka

Atribút ```data-th-if``` zabezpečí zobrazenie ```tagu``` jedine pri splnení [zadanej podmienky](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#conditional-evaluation).

```html
<i data-th-if="${!docDetails.available}" class="fas fa-chevron-down"></i>
```

## Include

Pomocou atribútu ```data-th-replace``` viete do HTML šablóny jednoducho [vložiť include iného HTML](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#including-template-fragments) súboru:

```html
<header class="ly-header">
    <div data-th-replace="/templates/bare/includes/header.html"></div>
</header>
```

Je možné použiť nasledovné atribúty:

- ```data-th-insert``` - do tela tag-u vloží zadaný súbor
- ```data-th-replace``` - celý tag nahradí zadaným súborom

## Volanie statickej metódy

Ak potrebujete volať statickú metódu môžete použiť `T()` funkciu:

```html
<p>date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(demoComponent.date)}"></span></p>
<p class="currentDate">current date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(T(sk.iway.iwcm.Tools).getNow())}"></span></p>
```
