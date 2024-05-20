# Information about the site

| Method | Type | Description | | | ---------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------- | | ${ninja.page.seoTitle}       | *String*     | | | | ${ninja.page.seoDescription} | *String*     | Description | | ${ninja.page.seoImage}       | *String*     | Image link | | ${ninja.page.url}            | *String*     | Url address | ${ninja.page.urlDomain}      | *String*     | Domain | | ${ninja.page.urlPath}        | *String*     | Virtual address | ${ninja.page.urlParameters}  | *Map*        | | | ${ninja.page.robots}         | *String*     | Indexing settings | ${ninja.page.doc}            | *DocDetails* | All properties | | | ${ninja.page.title}          | *String*     | Page title with space after `&nbsp;` entity after the conjunction (`Peter a Miro aj Fero -> Peter a&nbsp;Miro aj&nbsp;Fero`) | | ${ninja.page.perex}          | *String*     | Perex pages with space after `&nbsp;` entity after the conjunction | | ${ninja.page.perexPlace}     | *String*     | Perex instead of a page with a space after `&nbsp;` entity after the coupling |

!\&gt;**Note**: replacing the space after the conjunction with `&nbsp;` entity can be set in the configuration variable `ninjaNbspReplaceRegex`. On the first line is the regex expression, on the second is the replacement text.
## Name *String*

Searches for text in the optional R :carousel\_horse field: `getFieldR()` (SEO headline), if the field is empty, it will use the website name :carousel\_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

## Description *String*

Looks for a description in the optional field S :carousel\_horse: `getFieldS()` (SEO description), if the field is empty, it will use the standard perex description :carousel\_horse: `getPerexPre()`.

```java
${ninja.page.seoDescription}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" /> <meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Link to the picture *String*

Searches for an image in the optional T :carousel\_horse field: `getFieldT()` (SEO image), if the field is empty, it will use the standard :carousel\_horse perex image: `getPerexImage()`.

```java
${ninja.page.seoImage}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
```

## Url address *String*

Url address of a web page without parameters composed of `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" /> <link rel="canonical" href="${ninja.page.url}" />
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

Virtual address of a web page without domain and parameters generated from `PathFilter.getOrigPath()`.

```java
${ninja.page.urlPath}
```

Used in :ghost:<code>debug-info.jsp</code>

```html
<div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
```

## Parameters from URL *Map*

Parameters from the URL of the web page.

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

If the website has checked `Prehľadávateľné` in the Advanced Data tab, then return the value `index, follow`if not so `nofollow`. The check value is returned by the :carousel\_horse method: `isSearchable()`.

```java
${ninja.page.robots}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## *DocDetails*



```java
${ninja.page.doc}
```

Used in :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```
