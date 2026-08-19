# Informace o stránce

| Metoda | Typ | Popis |
| --- | --- | --- |
| ${ninja.page.seoTitle} | *String* | Název stránky (hodnota se bere z volitelného pole R nebo je-li prázdné, tak z titulku) |
| ${ninja.page.seoDescription} | *String* | Popis stránky (hodnota se bere z volitelného pole S, perexu nebo výchozího popisu skupiny šablon) |
| ${ninja.page.seoImage} | *String* | Odkaz na obrázek (hodnota se bere z volitelného pole T, perex obrázku, skupiny šablon nebo konfigurace šablony) |
| ${ninja.page.seoImageAlt} | *String* | Alternativní text SEO obrázku (hodnota se bere z volitelného pole P nebo skupiny šablon) |
| ${ninja.page.seoImageWidth} | *int* | Šířka SEO obrázku v pixelech |
| ${ninja.page.seoImageHeight} | *int* | Výška SEO obrázku v pixelech |
| ${ninja.page.url} | *String* | Url adresa |
| ${ninja.page.urlDomain} | *String* | Doména |
| ${ninja.page.urlPath} | *String* | Virtuální adresa |
| ${ninja.page.urlParameters} | *Map* | Parametry z URL adresy |
| ${ninja.page.robots} | *String* | Nastavení indexování a sledování odkazů vyhledávači |
| ${ninja.page.doc} | *DocDetails* | Všechny vlastnosti |
| ${ninja.page.title} | *String* | Titulek stránky s nahrazenou mezerou za `````` entitu po spojce (```Peter a Miro i Fero -> Peter a Miro i Fero```) |
| ${ninja.page.perex} | *String* | Perex stránky s nahrazenou mezerou za `````` entitu po spojce |
| ${ninja.page.perexPlace} | *String* | Perex místo stránky s nahrazenou mezerou za `````` entitu po spojce |
| ${ninja.page.canonical} | *String* | Kanonická URL adresa stránky (hodnota se bere z volitelného pole Q nebo pokud je prázdné, tak z URL adresy). Přidá do URL parametr `page`, pokud existuje, pro zobrazení správné strany v seznamu novinek. Jako doménové jméno vždy použije doménu nastavenou ve složce stránky bez ohledu na zobrazení přes administrační/CMS server. |
| ${ninja.abVariant} | *String* | Identifikátor reprezentující verzi stránka ve formě znaku a/b |

!>**Poznámka**: náhradu mezery po spojce za ```&nbsp;``` entitu lze nastavit v konfigurační proměnné ```ninjaNbspReplaceRegex```. Na prvním řádku je regex výraz, na druhém je text náhrady.

Pro nastavení volitelných polí P, Q, R, S a T je třeba v sekci [překladové klíče](../../../../admin/settings/translation-keys/README.md) nastavit hodnoty následovně:

```properties
editor.field_p=Alternatívny text SEO obrázka (og:image:alt)
editor.field_p.tooltip=Ak je zadaný, použije sa pre SEO a sociálne siete ako opis obsahu SEO obrázka namiesto predvolenej hodnoty zo skupiny šablón.
editor.field_q=Kanonická URL adresa
editor.field_q.tooltip=Ak je zadaný, použije sa tento odkaz ako kanonická URL adresa stránky, ak je prázdny, použije sa URL adresa stránky.
editor.field_q.type=link
editor.field_r=SEO titulok (og:title)
editor.field_r.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **titulku stránky**.\nMôžete tak optimalizovať zobrazený názov stránky na sociálnych sietiach.
editor.field_s=SEO popis (og:description)
editor.field_s.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný text namiesto **perex anotácie** stránky.\nMôžete tak optimalizovať zobrazený opis stránky na sociálnych sietiach.
editor.field_t=SEO obrázok (og:image)
editor.field_t.type=image
editor.field_t.tooltip=Ak je zadaný, použije sa pre SEO/Sociálne siete/Facebook zadaný obrázok namiesto štandardného obrázka (zadaného ako **perex obrázok**).\nMôžete tak optimalizovať zobrazený obrázok na sociálnych sietiach.
```

!> Pole P je obecné volitelné pole a stávající projekt jej již může používat pro jiný účel, například pro varianty produktů. Před použitím `${ninja.page.seoImageAlt}` proto zkontrolujte jeho význam v daném projektu.

## Název *String*

Hledá text ve volitelném poli R :carousel_horse: `getFieldR()` (SEO titulek), pokud je pole prázdné, tak použije název webové stránky :carousel_horse: `getTitle()`.

```java
${ninja.page.seoTitle}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:title" content="${ninja.page.seoTitle}" />
```

Při volání `seoTitle` je odstraněn případný HTML kód z titulku stránky, pokud potřebujete titulek včetně HTML kódu můžete použít `${ninja.page.seoTitleHtml}`.

## Popis *String*

Hledá popis ve volitelném poli S :carousel_horse: `getFieldS()` (SEO popis). Pokud je pole prázdné, použije standardní perex popis :carousel_horse: `getPerexPre()` a následně výchozí popis skupiny šablon pro jazyk zobrazené stránky.

```java
${ninja.page.seoDescription}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="description" content="${ninja.page.seoDescription}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
```

## Odkaz na obrázek *String*

Hledá obrázek ve volitelném poli T :carousel_horse: `getFieldT()` (SEO obrázek). Pokud pole neobsahuje platnou cestu k obrázku, použije standardní perex obrázek :carousel_horse: `getPerexImage()`, výchozí SEO obrázek skupiny šablon a nakonec původní hodnotu `defaultSeoImage` z konfiguračního souboru šablony.

```java
${ninja.page.seoImage}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
<meta property="og:image:width" content="${ninja.page.seoImageWidth}" />
<meta property="og:image:height" content="${ninja.page.seoImageHeight}" />
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
```

## Alternativní text SEO obrázku *String*

Hledá alternativní text SEO obrázku ve volitelném poli P :carousel_horse: `getFieldP()`. Pokud je pole prázdné, použije výchozí alternativní text skupiny šablon pro jazyk zobrazené stránky. Výsledek je převeden na čistý text bez HTML značek a uvozovek.

```java
${ninja.page.seoImageAlt}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
```

## Url adresa *String*

Url adresa webové stránky bez parametrů skládaná z `${ninja.page.urlDomain}` + `${ninja.page.urlPath}`.

```java
${ninja.page.url}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.canonical}" />
```

## Kanonická URL adresa *String*

Kanonická Url adresa webové stránky včetně domény. Možné je použít volitelné pole Q pro nastavení vlastní kanonické adresy stránky. Pokud v URL existuje parametr `page`, napø. v seznamu novinek, je k URL adrese přidán.

```java
${ninja.page.canonical}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:url" content="${ninja.page.url}" />
<link rel="canonical" href="${ninja.page.canonical}" />
```

## Doména *String*

Doména webové stránky bez parametrů nastavena z `Tools.getBaseHref()`.

```java
${ninja.page.urlDomain}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
<meta property="og:image:width" content="${ninja.page.seoImageWidth}" />
<meta property="og:image:height" content="${ninja.page.seoImageHeight}" />
<meta property="og:image:alt" content="${ninja.page.seoImageAlt}" />
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

Vrátí hodnotu pro SEO direktivy podle polí **Procházet** a **Následování odkazů vyhledávači** v editoru webové stránky. Stejná hodnota se použije i v HTTP hlavičce `X-Robots-Tag`, pokud je její generování pro webové stránky povoleno konfigurací.

Pole **Následujení odkazů vyhledávači** podporuje možnosti:

- **Podle nastavení Procházet** - při povoleném prohledávání povolí následování odkazů, při zakázaném prohledávání ho zakáže.
- **Povolit následování odkazů** - nastaví `follow` nezávisle na poli **Procházet**.
- **Zakázat následování odkazů** - nastaví `nofollow` nezávisle na poli **Procházet**.

Výchozí hodnota je **Podle nastavení Procházet**, která zachovává společné řízení indexování a sledování odkazů pro stávající stránky.

Výsledná hodnota obsahuje `all`, pokud indexování ani následování odkazů není omezeno. V ostatních případech obsahuje pouze omezující direktivy `noindex` a/nebo `nofollow`:

| Procházet | Následování odkazů vyhledávači | `${ninja.page.robots}` / `X-Robots-Tag` |
| --- | --- | --- |
| Ano | Podle nastavení Procházet | `all` |
| Ano | Povolit následování odkazů | `all` |
| Ano | Zakázat následování odkazů | `nofollow` |
| Ne | Podle nastavení Procházet | `noindex, nofollow` |
| Ne | Povolit následování odkazů | `noindex` |
| Ne | Zakázat následování odkazů | `noindex, nofollow` |

```java
${ninja.page.robots}
```

Použité v :ghost:<code>head.jsp</code>

```html
<meta name="robots" content="${ninja.page.robots}" />
```

## Všechny vlastnosti *DocDetails*

Zpřístupní celý: carousel_horse: docDetails - všechny vlastnosti webové stránky.

```java
${ninja.page.doc}
```

Použité v :ghost:<code>head.jsp</code>

```html
<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>
```
