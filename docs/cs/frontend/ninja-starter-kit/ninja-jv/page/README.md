# Informace o webu

| Metoda | Typ | Popis
| ---------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| ${ninja.page.seoTitle}       | *Řetězec*     | Název |
| ${ninja.page.seoDescription} | *Řetězec*     | Popis |
| ${ninja.page.seoImage}       | *Řetězec*     | Odkaz na obrázek |
| ${ninja.page.url}            | *Řetězec*     | Url adresa |
| ${ninja.page.urlDomain}      | *Řetězec*     | Doména |
| ${ninja.page.urlPath}        | *Řetězec*     | Virtuální adresa |
| ${ninja.page.urlParameters}  | *Mapa*        | Parametry z adresy URL |
| ${ninja.page.robots}         | *Řetězec*     | Nastavení indexování |
| ${ninja.page.doc}            | *DocDetails* | Všechny vlastnosti |
| ${ninja.page.title}          | *Řetězec*     | Název stránky s mezerou za názvem `&nbsp;` entita za spojkou (`Peter a Miro aj Fero -> Peter a&nbsp;Miro aj&nbsp;Fero`) |
| ${ninja.page.perex}          | *Řetězec*     | Stránky Perex s mezerou za `&nbsp;` entita po spojení |
| ${ninja.page.perexPlace}     | *Řetězec*     | Perex namísto stránky s mezerou za názvem `&nbsp;` entita po spojení |
| ${ninja.abVariant}           | *Řetězec*     | Identifikátor reprezentující verzi stránky ve tvaru znaku a/b |

!>**Poznámka**: nahrazení mezery za spojovníkem slovy `&nbsp;` lze nastavit v konfigurační proměnné `ninjaNbspReplaceRegex`. Na prvním řádku je regexový výraz, na druhém je nahrazující text.

## Název *Řetězec*

Vyhledá text ve volitelném poli R :carousel\_horse: `getFieldR()` (SEO titulek), pokud je pole prázdné, použije se název webové stránky :carousel\_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

## Popis *Řetězec*

Hledá popis ve volitelném poli S :carousel\_horse: `getFieldS()` (SEO popis), pokud je pole prázdné, použije se standardní popis perex :carousel\_horse: `getPerexPre()`.

```java
${ninja.page.seoDescription}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Odkaz na obrázek *Řetězec*

Vyhledá obrázek ve volitelném poli T :carousel\_horse: `getFieldT()` (SEO obrázek), pokud je pole prázdné, použije se standardní obrázek :carousel\_horse perex: `getPerexImage()`.

```java
${ninja.page.seoImage}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

## URL adresa *Řetězec*

Url adresa webové stránky bez parametrů, která se skládá z `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.url}" />
```

## Doména *Řetězec*

Doména webových stránek bez nastavených parametrů z `Tools.getBaseHref()`.

```java
${ninja.page.urlDomain}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

Používá se v :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlDomain}</div></div>
```

## Virtuální adresa *Řetězec*

Virtuální adresa webové stránky bez domény a parametrů vygenerovaná z `PathFilter.getOrigPath()`.

```java
${ninja.page.urlPath}
```

Používá se v :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
```

## Parametry z adresy URL *Mapa*

Parametry z adresy URL webové stránky.

```java
${ninja.page.urlParameters}
```

Používá se v :ghost:<code>debug-info.jsp</code>

```java
<c:forEach items="${ninja.page.urlParameters}" var="parameter">
    //zobrazí sa zoznam parametrov
</c:forEach>
```

## Nastavení indexování *Řetězec*

Pokud má webová stránka `Prehľadávateľné` na kartě Rozšířená data vrátí hodnotu `index, follow` pokud tomu tak není `nofollow`. Kontrolní hodnota je vrácena metodou :carousel\_horse: `isSearchable()`.

```java
${ninja.page.robots}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## Všechny funkce *DocDetails*

Přístup k celému :carousel\_horse: docDetails - všechny vlastnosti webové stránky.

```java
${ninja.page.doc}
```

Používá se v :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```
