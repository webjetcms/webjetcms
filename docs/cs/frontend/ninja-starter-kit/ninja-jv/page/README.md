# Informace o stránce

| Metoda | Typ | Popis
| ---------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| ${ninja.page.seoTitle}       | *String*     | Název |
| ${ninja.page.seoDescription} | *String*     | Popis |
| ${ninja.page.seoImage}       | *String*     | Odkaz na obrázek |
| ${ninja.page.url}            | *String*     | Url adresa |
| ${ninja.page.urlDomain}      | *String*     | Doména |
| ${ninja.page.urlPath}        | *String*     | Virtuální adresa |
| ${ninja.page.urlParameters}  | *Map*        | Parametry z adresy URL |
| ${ninja.page.robots}         | *String*     | Nastavení indexování |
| ${ninja.page.doc}            | *DocDetails* | Všechny vlastnosti |
| ${ninja.page.title}          | *String*     | Název stránky s mezerou za názvem `&nbsp;` entita za spojkou (`Peter a Miro aj Fero -> Peter a&nbsp;Miro aj&nbsp;Fero`) |
| ${ninja.page.perex}          | *String*     | Stránky Perex s mezerou za `&nbsp;` entita po spojení |
| ${ninja.page.perexPlace}     | *String*     | Perex namísto stránky s mezerou za názvem `&nbsp;` entita po spojení |
| ${ninja.abVariant}           | *String*     | Identifikátor reprezentující verzi stránky ve tvaru znaku a/b |

!>**Poznámka**: náhradu mezery po spojce za `&nbsp;` entitu lze nastavit v konfigurační proměnné `ninjaNbspReplaceRegex`. Na prvním řádku je regex výraz, na druhém je text náhrady.

## Název *String*

Hledá text ve volitelném poli R :carousel\_horse: `getFieldR()` (SEO titulek), pokud je pole prázdné, tak použije název webové stránky :carousel\_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

## Popis *String*

Hledá popis ve volitelném poli S :carousel\_horse: `getFieldS()` (SEO popis), pokud je pole prázdné, tak použije standardní perex popis :carousel\_horse: `getPerexPre()`.

```java
${ninja.page.seoDescription}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Odkaz na obrázek *String*

Hledá obrázek ve volitelném poli T :carousel\_horse: `getFieldT()` (SEO obrázek), pokud je pole prázdné, tak použije standardní perex obrázek :carousel\_horse: `getPerexImage()`.

```java
${ninja.page.seoImage}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

## Url adresa *String*

Url adresa webové stránky bez parametrů skládaná z `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.url}" />
```

## Doména *String*

Doména webové stránky bez parametrů nastavena z `Tools.getBaseHref()`.

```java
${ninja.page.urlDomain}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

Použité v :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlDomain}</div></div>
```

## Virtuální adresa *String*

Virtuální adresa webové stránky bez domény a parametrů generovaná z `PathFilter.getOrigPath()`.

```java
${ninja.page.urlPath}
```

Použité v :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
```

## Parametry z URL adresy *Map*

Parametry z URL adresy webové stránky.

```java
${ninja.page.urlParameters}
```

Použité v :ghost:<code>debug-info.jsp</code>

```java
<c:forEach items="${ninja.page.urlParameters}" var="parameter">
    //zobrazí sa zoznam parametrov
</c:forEach>
```

## Nastavení indexování *String*

Pokud má webová stránka zaškrtnuto `Prehľadávateľné` v záložce Rozšířené údaje, tak vrátí hodnotu `index, follow`, ne-li tak `nofollow`. Hodnotu zaškrtnutí vrací metoda :carousel\_horse: `isSearchable()`.

```java
${ninja.page.robots}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## Všechny vlastnosti *DocDetails*

Zpřístupní celý: carousel\_horse: docDetails - všechny vlastnosti webové stránky.

```java
${ninja.page.doc}
```

Použité v :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```
