# Atributy, podmínky a cykly

## Základní výpis textu/atributů

Text nelze vypsat přímo, musí se vložit např. do. `span` obalů s atributem `data-th-text` který nahradí obsah `span` prvek (bude vytvořen prototyp). Podobně se nastavují atributy, např. `data-th-href=...`

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
```

Pro vložení **Kód HTML (bez escapování znaků)** atribut musí být použit `data-th-utext`.

v kódu JavaScriptu lze hodnotu přiřadit následujícím způsobem:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.title = "[[${docDetails.title}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.title = [(${docDetails.title})];
```

Pokud potřebujete následně [také odstranit celou značku](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#removing-template-fragments) můžete použít atribut `data-th-remove="tag"`:

```html
<div data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag"></div>
```

Pokud potřebujete vypsat [jednoduchý atribut](webjet-objects.md) z `request` můžete použít:

```html
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>
```

## Text překladu

Text překladu se píše ve tvaru `#{prekladovy.kluc}`.

```html
<span data-th-text="#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[#{menu.top.help}]]
```

## Propojení řetězců

```html
<img
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
>
```

kromě toho je možné použít také [Doslovné záměny](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#literal-substitutions):

```html
<span data-th-text="|Welcome to our application, ${docDetails.title}!|">
```

**Varování:** pokud vyhodí chybu jako: `Could not parse as expression: "aitem--open md-large-menu"`, je to kvůli `__`. Jedná se o speciální značku pro [předprocesor](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#preprocessing) a je třeba jej escapovat jako \\\\\_, příklad:

```html
<div data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'">
```

## Cyklus

Pro [cyklus](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#iteration) značka se používá `data-th-each`:

```html
<select data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}">
    <option data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}"></option>
</select>
```

## Stav

Atribut `data-th-if` zajistit zobrazení `tagu` pouze při setkání [uvedenou podmínku](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#conditional-evaluation).

```html
<i data-th-if="${!docDetails.available}" class="ti ti-chevron-down"></i>
```

## Zahrnout

Použití atributu `data-th-replace` můžete snadno přidat do šablon HTML [vložit include jiného HTML](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#including-template-fragments) soubor:

```html
<header class="ly-header">
    <div data-th-replace="/templates/bare/includes/header.html"></div>
</header>
```

Lze použít následující atributy:
- `data-th-insert` - vloží zadaný soubor do těla značky
- `data-th-replace` - nahradí celou značku zadaným souborem

## Volání statické metody

Pokud potřebujete zavolat statickou metodu, můžete použít příkaz `T()` funkce:

```html
<p>date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(demoComponent.date)}"></span></p>
<p class="currentDate">current date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(T(sk.iway.iwcm.Tools).getNow())}"></span></p>
```
