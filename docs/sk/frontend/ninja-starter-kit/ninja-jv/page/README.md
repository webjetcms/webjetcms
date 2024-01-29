# Informácie o stránke
| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.page.seoTitle} | *String* | Názov |
| ${ninja.page.seoDescription} | *String* | Popis |
| ${ninja.page.seoImage} | *String* | Odkaz na obrázok |
| ${ninja.page.url} | *String* | Url adresa |
| ${ninja.page.urlDomain} | *String* | Doména |
| ${ninja.page.urlPath} | *String* | Virtuálna adresa |
| ${ninja.page.urlParameters} | *Map* | Parametre z URL adresy |
| ${ninja.page.robots} | *String* | Nastavenie indexovania |
| ${ninja.page.doc} | *DocDetails* | Všetky vlastnosti |
| ${ninja.page.title} | *String* | Titulok stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke (```Peter a Miro aj Fero -> Peter a&nbsp;Miro aj&nbsp;Fero```) |
| ${ninja.page.perex} | *String* | Perex stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke |
| ${ninja.page.perexPlace} | *String* | Perex miesto stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke |

!>**Poznámka**: náhradu medzery po spojke za ```&nbsp;``` entitu je možné nastaviť v konfiguračnej premennej ```ninjaNbspReplaceRegex```. Na prvom riadku je regex výraz, na druhom je text náhrady.

## Názov *String*
Hľadá text vo voliteľnom poli R :carousel_horse: `getFieldR()` (SEO titulok), ak je pole prázdne, tak použije názov webovej stránky :carousel_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

## Popis *String*

Hľadá popis vo voliteľnom poli S :carousel_horse: `getFieldS()` (SEO popis), ak je pole prázdne, tak použije štandardný perex popis :carousel_horse: `getPerexPre()`.

```java
${ninja.page.seoDescription}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Odkaz na obrázok *String*
Hľadá obrázok vo voliteľnom poli T :carousel_horse: `getFieldT()` (SEO obrázok), ak je pole prázdne, tak použije štandardný perex obrázok :carousel_horse: `getPerexImage()`.

```java
${ninja.page.seoImage}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

## Url adresa *String*
Url adresa webovej stránky bez parametrov vyskladaná z `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.url}" />
```

## Doména *String*
Doména webovej stránky bez parametrov nastavená z `Tools.getBaseHref()`.

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

## Virtuálna adresa *String*
Virtuálna adresa webovej stránky bez domény a parametrov generovaná z `PathFilter.getOrigPath()`.

```java
${ninja.page.urlPath}
```

Použité v :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
```

## Parametre z URL adresy *Map*
Parametre z URL adresy webovej stránky.

```java
${ninja.page.urlParameters}
```

Použité v :ghost:<code>debug-info.jsp</code>

```java
<c:forEach items="${ninja.page.urlParameters}" var="parameter">
    //zobrazí sa zoznam parametrov
</c:forEach>
```

## Nastavenie indexovania *String*
Ak má webová stranka zaškrtnuté `Prehľadávateľné` v záložke Rozšírené údaje, tak vrati hodnotu `index, follow`, ak nie tak `nofollow`. Hodnotu zaškrtnutia vracia metóda :carousel_horse: `isSearchable()`.

```java
${ninja.page.robots}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## Všetky vlastnosti *DocDetails*
Sprístupní celý :carousel_horse: docDetails - všetky vlastnosti webovej stránky.

```java
${ninja.page.doc}
```

Použité v :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```