# Informácie o stránke

| Metóda | Typ | Popis |
| --- | --- | --- |
| ${ninja.page.seoTitle} | *String* | Názov stránky (hodnota sa berie z voliteľného poľa R alebo ak je prázdne, tak z titulku) |
| ${ninja.page.seoDescription} | *String* | Popis stránky (hodnota sa berie z voliteľného poľa S alebo ak je prázdne, tak z perexu) |
| ${ninja.page.seoImage} | *String* | Odkaz na obrázok (hodnota sa berie z voliteľného poľa T alebo ak je prázdne, tak z perex obrázku) |
| ${ninja.page.url} | *String* | Url adresa |
| ${ninja.page.urlDomain} | *String* | Doména |
| ${ninja.page.urlPath} | *String* | Virtuálna adresa |
| ${ninja.page.urlParameters} | *Map* | Parametre z URL adresy |
| ${ninja.page.robots} | *String* | Nastavenie indexovania |
| ${ninja.page.doc} | *DocDetails* | Všetky vlastnosti |
| ${ninja.page.title} | *String* | Titulok stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke (```Peter a Miro aj Fero -> Peter a&nbsp;Miro aj&nbsp;Fero```) |
| ${ninja.page.perex} | *String* | Perex stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke |
| ${ninja.page.perexPlace} | *String* | Perex miesto stránky s nahradenou medzerou za ```&nbsp;``` entitu po spojke |
| ${ninja.page.canonical} | *String* | Kanonická URL adresa stránky (hodnota sa berie z voliteľného poľa Q alebo ak je prázdne, tak z URL adresy) |
| ${ninja.abVariant} | *String* | Identifikátor reprezentujúci verziu stránka vo forme znaku a/b |

!>**Poznámka**: náhradu medzery po spojke za ```&nbsp;``` entitu je možné nastaviť v konfiguračnej premennej ```ninjaNbspReplaceRegex```. Na prvom riadku je regex výraz, na druhom je text náhrady.

Pre nastavenie voliteľných polí R, S, T a Q je potrebné v sekcii [prekladové kľúče](../../../../admin/settings/translation-keys/README.md) nastaviť hodnoty nasledovne:

```properties
editor.field_q=Kanonická URL adresa
editor.field_q.tooltip=Ak je zadaný, použije se tento odkaz ako kanonická URL adresa stránky, ak je prázdny, použije se URL adresa stránky.
editor.field_q.type=link
editor.field_r=SEO titulok
editor.field_r.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **titulku stránky**.\nMôžete tak optimalizovať zobrazený názov stránky na sociálnych sietiach.
editor.field_s=SEO opis (kľúčové slová)
editor.field_s.tooltip=Ak je zadaný, použije pre SEO/Sociálne siete/Facebook zadaný text namiesto **perex anotácie** stránky.\nMôžete tak optimalizovať zobrazený opis stránky na sociálnych sietiach.
editor.field_t=SEO obrázok
editor.field_t.type=image
editor.field_t.tooltip=Ak je zadaný, použije pre SEO/Sociálne siete/Facebook zadaný obrázok namiesto štandardného obrázka (zadaného ako **perex obrázok**).\nMôžete tak optimalizovať zobrazený obrázok na sociálnych sietiach.

```

## Názov *String*

Hľadá text vo voliteľnom poli R :carousel_horse: `getFieldR()` (SEO titulok), ak je pole prázdne, tak použije názov webovej stránky :carousel_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

Pri volaní `seoTitle` je odstránený prípadný HTML kód z titulku stránky, ak potrebujete titulok vrátane HTML kódu môžete použiť `${ninja.page.seoTitleHtml}`.

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
Url adresa webovej stránky bez parametrov skladaná z `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

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
Ak má webová stránka zaškrtnuté `Prehľadávateľné` v záložke Rozšírené údaje, tak vráti hodnotu `index, follow`, ak nie tak `nofollow`. Hodnotu zaškrtnutia vracia metóda :carousel_horse: `isSearchable()`.

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