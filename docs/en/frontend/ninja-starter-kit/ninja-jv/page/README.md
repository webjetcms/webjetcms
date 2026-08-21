# Site information

| Method | Type | Description |
| --- | --- | --- |
| ${ninja.page.seoTitle} | *String* | Page name (value is taken from optional field R or if empty, from title) |
| ${ninja.page.seoDescription} | *String* | Page description (value is taken from optional field S, perex or default template group description) |
| ${ninja.page.seoImage} | *String* | Image reference (value is taken from optional field T, perex image, template group or template configuration) |
| ${ninja.page.seoImageAlt} | *String* | SEO image alt text (value is taken from optional P field or template group) |
| ${ninja.page.seoImageWidth} | *int* | SEO image width in pixels |
| ${ninja.page.seoImageHeight} | *int* | SEO image height in pixels |
| ${ninja.page.url} | *String* | URL address |
| ${ninja.page.urlDomain} | *String* | Domain |
| ${ninja.page.urlPath} | *String* | Virtual address |
| ${ninja.page.urlParameters} | *Map* | Parameters from the URL address |
| ${ninja.page.robots} | *String* | Setting up indexing and link tracking by search engines |
| ${ninja.page.doc} | *DocDetails* | All features |
| ${ninja.page.title} | *String* | Page title with space replaced by ``` ``` entity after the conjunction (```Peter and Miro and Fero -> Peter and Miro and Fero```) |
| ${ninja.page.perex} | *String* | Perex pages with space replaced by ``` ``` entity after the connector |
| ${ninja.page.perexPlace} | *String* | Perex page location with space replaced for ``` ``` entity after the connector |
| ${ninja.page.canonical} | *String* | Canonical URL of the page (value is taken from optional field Q or if empty, from URL). Appends `page` parameter to URL, if present, to display correct page in news list. Always uses domain set in page folder as domain name regardless of display via admin/CMS server. |
| ${ninja.abVariant} | *String* | An identifier representing the page version in the form of a/b characters |

!>**Note**: the replacement of the space after the hyphen for the ```&nbsp;``` entity can be set in the configuration variable ```ninjaNbspReplaceRegex```. The first line is the regex expression, the second is the replacement text.

To set the optional fields P, Q, R, S and T, you need to set the values ​​as follows in the [translation keys] section (../../../../admin/settings/translation-keys/README.md):

```properties
editor.field_p=Alternatívny text SEO obrázka (og:image:alt)
editor.field_p.tooltip=Ak je zadaný, použije sa pre SEO a sociálne siete ako opis obsahu SEO obrázka namiesto predvolenej hodnoty zo skupiny šablón.
editor.field_q=Kanonická URL adresa
editor.field_q.tooltip=Ak je zadaný, použije sa tento odkaz ako kanonická URL adresa stránky, ak je prázdny, použije sa URL adresa stránky.
editor.field_q.type=link
editor.field_r=SEO titulok (og:title)
editor.field_r.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **titulku stránky**.\nMôžete tak optimalizovať zobrazený názov stránky na sociálnych sietiach.
editor.field_s=SEO opis (og:description)
editor.field_s.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **perex anotácie** stránky.\nMôžete tak optimalizovať zobrazený opis stránky na sociálnych sietiach.
editor.field_t=SEO obrázok (og:image)
editor.field_t.type=image
editor.field_t.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný obrázok namiesto štandardného obrázka (zadaného ako **perex obrázok**).\nMôžete tak optimalizovať zobrazený obrázok na sociálnych sietiach.
```

!> The P field is a general optional field and an existing project may already be using it for another purpose, such as product variants. Therefore, check its meaning in your project before using `${ninja.page.seoImageAlt}`.

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

It looks for a description in the optional field S :carousel_horse: `getFieldS()` (SEO description). If the field is empty, it uses the standard perex description :carousel_horse: `getPerexPre()` and then the default template group description for the language of the displayed page.

```java
${ninja.page.seoDescription}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Image link *String*

It looks for an image in the optional field T :carousel_horse: `getFieldT()` (SEO image). If the field does not contain a valid image path, it uses the default perex image :carousel_horse: `getPerexImage()`, the default SEO image of the template group, and finally the original value `defaultSeoImage` from the template configuration file.

```java
${ninja.page.seoImage}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
<meta property="og:image:width" content="${ninja.page.seoImageWidth}" />
<meta property="og:image:height" content="${ninja.page.seoImageHeight}" />
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
```

## SEO image alternative text *String*

Searches for the SEO alt text of the image in the optional field P :carousel_horse: `getFieldP()`. If the field is empty, it uses the default alt text of the template group for the language of the displayed page. The result is converted to plain text without HTML tags and quotes.

```java
${ninja.page.seoImageAlt}
```

Used in :ghost:<code>head.jsp</code>

```html
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
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

## Canonical URL *String*

Canonical URL of the website, including the domain. You can use the optional Q field to set your own canonical URL. If the parameter `page` exists in the URL, e.g. in a news list, it is added to the URL.

```java
${ninja.page.canonical}
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
<meta property="og:image:width" content="${ninja.page.seoImageWidth}" />
<meta property="og:image:height" content="${ninja.page.seoImageHeight}" />
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
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

Returns the value for SEO directives according to the **Crawl** and **Search Engine Follow** fields in the website editor. The same value will also be used in the HTTP header `X-Robots-Tag` if its generation is enabled for websites by configuration.

The **Search engine following** field supports the following options:

- **According to the Browse setting** - when browsing is enabled, it allows following links, when browsing is disabled, it disables it.
- **Enable following links** - sets `follow` independently of the **Browse** field.
- **Disable following links** - sets `nofollow` independently of the **Browse** field.

The default value is **By Crawl Settings**, which maintains common control over indexing and link tracking for existing pages.

The resulting value contains `all` if neither indexing nor following links is restricted. Otherwise, it contains only the restrictive directives `noindex` and/or `nofollow`:

| Browse | Following links by search engines | `${ninja.page.robots}` / `X-Robots-Tag` |
| --- | --- | --- |
| Yes | By setting Browse | `all` |
| Yes | Allow following links | `all` |
| Yes | Disable following links | `nofollow` |
| No | By setting Browse | `noindex, nofollow` |
| No | Allow following links | `noindex` |
| No | Disable following links | `noindex, nofollow` |

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
