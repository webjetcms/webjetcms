# Site information

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.page.seoTitle} | *String* | Page name (value is taken from optional field R or if empty, from title) |
| ${ninja.page.seoDescription} | *String* | Page description (value is taken from optional field S or if empty, from perex) |
| ${ninja.page.seoImage} | *String* | Image link (value is taken from optional field T or if empty, from the image itself) |
| ${ninja.page.url} | *String* | URL address |
| ${ninja.page.urlDomain} | *String* | Domain |
| ${ninja.page.urlPath} | *String* | Virtual address |
| ${ninja.page.urlParameters} | *Map* | Parameters from the URL address |
| ${ninja.page.robots} | *String* | Indexing settings |
| ${ninja.page.doc} | *DocDetails* | All features |
| ${ninja.page.title} | *String* | Page title with space replaced by ``` ``` entity after the conjunction (```Peter and Miro and Fero -> Peter and Miro and Fero```) |
| ${ninja.page.perex} | *String* | Perex pages with space replaced by ``` ``` entity after the connector |
| ${ninja.page.perexPlace} | *String* | Perex page location with space replaced for ``` ``` entity after the connector |
| ${ninja.page.canonical} | *String* | Canonical URL of the page (value is taken from the optional Q field or, if empty, from the URL) |
| ${ninja.abVariant} | *String* | An identifier representing the page version in the form of a/b characters |

!>**Note**: the replacement of the space after the hyphen for the ```&nbsp;``` entity can be set in the configuration variable ```ninjaNbspReplaceRegex```. The first line is the regex expression, the second is the replacement text.

To set the optional fields R, S, T and Q, you need to set the values ​​as follows in the [translation keys] section (../../../../admin/settings/translation-keys/README.md):

```properties
editor.field_q=Kanonická URL adresa
editor.field_q.tooltip=Ak je zadaný, použije sa tento odkaz ako kanonická URL adresa stránky, ak je prázdny, použije sa URL adresa stránky.
editor.field_q.type=link
editor.field_r=SEO titulok
editor.field_r.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **titulku stránky**.\nMôžete tak optimalizovať zobrazený názov stránky na sociálnych sietiach.
editor.field_s=SEO opis (kľúčové slová)
editor.field_s.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **perex anotácie** stránky.\nMôžete tak optimalizovať zobrazený opis stránky na sociálnych sietiach.
editor.field_t=SEO obrázok
editor.field_t.type=image
editor.field_t.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný obrázok namiesto štandardného obrázka (zadaného ako **perex obrázok**).\nMôžete tak optimalizovať zobrazený obrázok na sociálnych sietiach.

```

## Name *String*

Searches for text in the optional field R :carousel_horse: `getFieldR()` (SEO title), if the field is empty, it uses the website name :carousel_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

When calling `seoTitle`, any HTML code from the page title is removed. If you need the title including the HTML code, you can use `${ninja.page.seoTitleHtml}`.

## Description *String*

It looks for a description in the optional field S :carousel_horse: `getFieldS()` (SEO description), if the field is empty, it uses the standard perex description :carousel_horse: `getPerexPre()`.

```java
${ninja.page.seoDescription}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Image link *String*

It looks for an image in the optional field T :carousel_horse: `getFieldT()` (SEO image), if the field is empty, it uses the standard perex image :carousel_horse: `getPerexImage()`.

```java
${ninja.page.seoImage}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

## URL address *String*

The URL of a website without parameters consists of `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.canonical}" />
```

## Domain *String*

Website domain without parameters set from `Tools.getBaseHref()`.

```java
${ninja.page.urlDomain}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

Used in :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlDomain}</div></div>
```

## Virtual address *String*

Virtual website address without domain and parameters generated from `PathFilter.getOrigPath()`.

```java
${ninja.page.urlPath}
```

Used in :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
```

## Parameters from URL address *Map*

Parameters from the website URL.

```java
${ninja.page.urlParameters}
```

Used in :ghost:<code>debug-info.jsp</code>

```java
<c:forEach items="${ninja.page.urlParameters}" var="parameter">
    //zobrazí sa zoznam parametrov
</c:forEach>
```

## Indexing settings *String*

If the website has `Prehľadávateľné` checked in the Advanced Data tab, it returns the value `index, follow`, otherwise `nofollow`. The checkmark value is returned by the :carousel_horse: `isSearchable()` method.

```java
${ninja.page.robots}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## All properties *DocDetails*

Makes the entire :carousel_horse: docDetails - all the properties of the website available.

```java
${ninja.page.doc}
```

Used in :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```