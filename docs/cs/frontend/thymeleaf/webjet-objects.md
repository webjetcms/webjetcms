# Seznam atributů WebJET CMS

Seznam dostupných atributů při zobrazení stránky

## Web stránka

Údaje zobrazené web stránky

- **ID web stránky** - `<div data-iwcm-write="doc_id"/>` - Vloží docid web stránky, například: `148`
- **Název web stránky** - `<div data-iwcm-write="doc_title"/>` - Vloží název web stránky, například: Web JET CMS
- **Text navigační lišty** - `<div data-iwcm-write="doc_navbar"/>` - Text web stránky pro navigační lištu (obvykle stejný jako název web stránky - `doc_title`)
- **Text web stránky** - `<div data-iwcm-write="doc_data"/>` - Samotný text web stránky, jak byl zadán v editoru
- **Hlavička web stránky** - `<div data-iwcm-write="doc_header"/>` - HTML text s hlavičkou web stránky jak je definováno ve virtuální šabloně, kterou web stránka používá
- **Patička web stránky** - `<div data-iwcm-write="doc_footer"/>` - HTML text s patičkou web stránky jak je definováno ve virtuální šabloně, kterou web stránka používá
- **Menu web stránky** - `<div data-iwcm-write="doc_menu"/>` - HTML kód menu, který se zadává v admin části jednotlivým virtuálním šablonám
- **Pravé menu web stránky** - `<div data-iwcm-write="doc_right_menu"/>` - HTML kód pravého menu, který se zadává v admin části jednotlivým virtuálním šablonám
- **HTML kód ze šablony** - `<div data-iwcm-write="after_body"/>` - HTML text z virtuální šablony, kterou web stránka používá
- **HTML hlavička stránky** - `<div data-iwcm-write="html_head"/>` - HTML kód, který se zadává pro stránku v záložce HTML Vlastnosti jako HTML kód hlavičky. Může obsahovat kód, který se vkládá do šablony do sekce `<head>`
- **HTML kód stránky** - `<div data-iwcm-write="html_data"/>` - HTML kód, který se zadává pro stránku v záložce HTML Vlastnosti jako Dodatečný HTML kód.
- **Linka na hlavní kaskádový styl** - `<div data-iwcm-write="base_css_link"/>` - Linka na kaskádový styl, který se zadává v admin části virtuálním šablonám jako Hlavní CSS styl (např. `/css/page.css`).
- **Linka na hlavní kaskádový styl** - `<div data-iwcm-write="base_css_link_noext"/>` - Linka na kaskádový styl, který se zadává v admin části virtuálním šablonám jako Hlavní CSS styl bez přípony (např. `/css/page`). Lze použít pro vytváření podmíněných css stylů (např. `/css/page-dark.css`).
- **Linka na kaskádový styl** - `<div data-iwcm-write="css_link"/>` - Linka na kaskádový styl, který se zadává v admin části virtuálním šablonám jako CSS styl (např. `/css/custom.css`).
- **Linka na kaskádový styl** - `<div data-iwcm-write="css_link_noext"/>` - Linka na kaskádový styl, který se zadává v admin části virtuálním šablonám jako CSS styl bez přípony (např. /css/custom). Lze použít pro vytváření podmíněných css stylů (např. `/css/page-dark.css`).
- **Navigační lišta** - `<div data-iwcm-write="navbar"/>` - Kompletní navigační lišta, například. Interway > Produkty > Web JET. Názvy jsou klikatelné, linky jsou vytvářeny s použitím kaskádového stylu `navbar`.
- **Datum poslední změny web stránky** - `<div data-iwcm-write="doc_date_created"/>` - Datum, kdy byla web stránka naposledy změněna.
- **Čas poslední změny web stránky** - `<div data-iwcm-write="doc_time_created"/>` - Čas, kdy byla web stránka naposledy změněna.
- **Vlastní pole** - `<div data-iwcm-write="field_a"/>` až `<div data-iwcm-write="field_l"/>` - Vlastní pole stránky definované v Rozšířených vlastnostech, záložka Vlastní pole.
- **Volné objekty šablony** - `<div data-iwcm-write="template_object_a"/>` až `<div data-iwcm-write="template_object_d"/>` - Volně použitelné objekty šablony. Obsahují HTML kód přiřazené stránky. Používají se pokud nestačí použití hlavičky, menu a patičky.

## Publikování stránky

Údaje zadané při editování web stránky v záložce Rozšířené

- **Datum začátku publikování** - `<div data-iwcm-write="doc_publish_start"/>` - Datum zadané při editování web stránky v záložce Rozšířené jako Začátek.
- **Čas začátku publikování** - `<div data-iwcm-write="doc_publish_start_time"/>` - Čas zadaný při editování web stránky v záložce Rozšířené jako Začátek.
- **Datum konce publikování** - `<div data-iwcm-write="doc_publish_end"/>` - Datum zadané při editování web stránky v záložce Rozšířené jako Konec.
- **Čas konce publikování** - `<div data-iwcm-write="doc_publish_end_time"/>` - Čas zadaný při editování web stránky v záložce Rozšířené jako Konec.
- **Datum konání události** - `<div data-iwcm-write="doc_event_date"/>` - Datum konání události zadané při editování web stránky v záložce Rozšířené. Používá se například při akcích, kdy datum akce je později než požadované datum začátku publikování (chceme-li, aby se informace na stránce zobrazovala od 1 dne měsíce, ale akce o které stránka informuje je až 10 dne v měsíci).
- **Doba konání události** - `<div data-iwcm-write="doc_event_time"/>` - Čas konání události zadaný při editování web stránky v záložce Rozšířené.
- **Perex (anotace) web stránky** - `<div data-iwcm-write="perex_data"/>` - Vloží do HTML šablony anotaci stránky.
- **Formátovaný perex (anotace) web stránky** - `<div data-iwcm-write="perex_pre"/>` - Vloží do HTML šablony formátovanou anotaci stránky. Pokud je anotace zadána bez HTML znaků, jsou znaky nového řádku převedeny na značky `<br>`
- **Místo** - `<div data-iwcm-write="perex_place"/>` - Místo, kterého se text stránky týká.
- **Obrázek** - `<div data-iwcm-write="perex_image"/>` - Obrázek, který se týká anotace stránky.

## Autor stránky

Údaje o autorovi stránky (za autora stránky se považuje uživatel, který ji naposledy změnil):
- **ID autora web stránky** - `<div data-iwcm-write="author_id"/>` - ID autora web stránky (uživatele, který ji naposledy změnil).
- **Jméno autora web stránky** - `<div data-iwcm-write="author_name"/>` - Jméno autora web stránky (uživatele, který ji naposledy změnil).
- **Email autora web stránky** - `<div data-iwcm-write="author_email"/>` - Email autora web stránky (uživatele, který ji naposledy změnil).

## Složka

Údaje o složce, ve které se stránka nachází:
- **ID adresáře** - `<div data-iwcm-write="group_id"/>` - ID adresáře, ve kterém se nachází stránka
- **Název adresáře** - `<div data-iwcm-write="group_name"/>` - Název adresáře, ve kterém se nachází stránka
- **Text navigační lišty** - `<div data-iwcm-write="group_navbar"/>` - Text adresáře pro navigační lištu (obvykle stejný jako název adresáře)
- **HTML kód do hlavičky** - `<div data-iwcm-write="group_htmlhead"/>` - HTML kód do hlavičky adresáře
- **HTML kód do hlavičky - rekurzivně získaný** - `<div data-iwcm-write="group_htmlhead_recursive"/>` - HTML kód do hlavičky adresáře rekurzivně získaný - je-li obsah tohoto pole u aktuálního adresáře prázdný, získává se hodnota z jeho nadřazeného adresáře, není-li zadána ani tam, postupuje se dále přes podadresáře k hlavnímu adresáři
- **ID rodičů** - `<div data-iwcm-write="groupParentIds"/>` - Čárkou oddělené ID nadřazených adresářů - podle toho lze v HTML šabloně určovat jakou jazykovou mutaci nebo sekci právě zobrazujeme
- **Vlastní pole** - `<div data-iwcm-write="group_field_a"/>` až `<div data-iwcm-write="group_field_d"/>` - Vlastní pole adresáře.

## Šablona stránky

Údaje o šabloně stránky:
- **ID šablony** - `<div data-iwcm-write="doc_temp_id"/>` - ID šablony, kterou web stránka používá.
- **Název šablony** - `<div data-iwcm-write="doc_temp_name"/>` - Název šablony, kterou web stránka používá.
- **Hlavní CSS styl** - `${ninja.temp.baseCssLink}` - Název šablony, kterou web stránka používá.
- **Druhořadý CSS styl** - `${ninja.temp.cssLink}` - Název šablony, kterou web stránka používá.
- **Objekt šablony** - `${tempDetails}` - Objekt šablony, kterou web stránka používá.
- **Objekt skupiny šablony** - `${templatesGroupDetails}` - Objekt skupiny šablony, kterou web stránka používá.

## Ninja šablona

Příklady použití objektů z [Ninja šablony](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) (příklady jsou v [pug](../../developer/frameworks/pugjs.md) formátu). Typicky se Ninja objekt používá v hlavičce šablony:

```javascript
meta(http-equiv='X-UA-Compatible' content='IE=edge')
meta(charset="utf-8" data-th-charset='${ninja.temp.charset}')
meta(name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no')
meta(http-equiv="Content-type" content="text/html;charset=utf-8" data-th-content='|text/html;charset=${ninja.temp.charset}|')

title(data-th-text='|${docDetails.title} - ${ninja.temp.group.siteName}|') Page Title

meta(name='description' content="WebJET CMS" data-th-content='${ninja.page.seoDescription}')
meta(name='author', content="InterWay" data-th-content='${ninja.temp.group.author}')
meta(name='developer', content="InterWay" data-th-content='${ninja.temp.group.developer}')
meta(name='generator', content="WebJET CMS" data-th-content='${ninja.temp.group.generator}')
meta(name='copyright', content="InterWay" data-th-content='${ninja.temp.group.copyright}')
meta(name='robots', content="noindex" data-th-content='${ninja.page.robots}')

meta(property='og:title' content="WebJET CMS" data-th-content='${ninja.page.seoTitle}')
meta(property='og:description' content="WebJET CMS" data-th-content='${ninja.page.seoDescription}')
meta(property='og:url' content="http://webjetcms.sk" data-th-content='${ninja.page.url}')
meta(property='og:image' content="assets/images/og-webjet.png" data-th-content='|${ninja.page.urlDomain}${ninja.page.seoImage}|')
meta(property='og:site_name' content="WebJET CMS" data-th-content='${ninja.temp.group.siteName}')
meta(property='og:type' content='website')
meta(property='og:locale' content="sk-SK" data-th-content='${ninja.temp.lngIso}')

link(rel='icon' href="assets/images/favicon.ico" data-th-href='|${ninja.temp.basePathImg}favicon.ico|' sizes='any')
link(rel='icon' href="assets/images/icon.svg" data-th-href='|${ninja.temp.basePathImg}icon.svg|' type='image/svg+xml')

link(rel='canonical' data-th-href='${ninja.page.url}')
link(data-th-if="${docDetails.tempName == 'Blog template'}" rel='amphtml' data-th-href='|${ninja.page.url}?forceBrowserDetector=amp|')

touchicons(data-th-utext='${ninja.temp.insertTouchIconsHtml}' data-th-remove="tag")
    <link rel="apple-touch-icon-precomposed" href="assets/images/touch-icon.png" />

combine(
basePath='|${ninja.temp.basePath}dist/|',
data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}\n"+
"${ninja.webjet.pageFunctionsPath}"
)
    <link href="css/ninja.min.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="js/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
    <script src="js/global-functions.js" type="text/javascript"></script>
    <script src="js/ninja.min.js" type="text/javascript"></script>

div(data-iwcm-write="group_htmlhead_recursive")
div(data-iwcm-write="html_head")
div(data-iwcm-script="header")
```

## Dostupné objekty

Při zobrazení stránky WebJET vkládá následující objekty, ty můžete použít přímo pro výpis pomocí `${objekt.vlastnost}`. Název odkazuje na dokumentaci, kde je vidět seznam vlastností objektu:
- `request` - objekt `HttpServletRequest`
- `session` - objekt `HttpSession`
- [ninja](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - doplňkové atributy a funkce pro [Ninja šablonu](../ninja-starter-kit/README.md)
- [docDetails](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - objekt zobrazené web stránky
- [docDetailsOriginal](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - je-li zobrazena přihlašovací stránka obsahuje původní (zaheslovanou) stránku
- [pageGroupDetails](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html) - objekt adresáře aktuálně zobrazené web stránky
- [tempDetails](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html) - objekt šablony aktuálně zobrazené web stránky
- [templatesGroupDetails](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html) - objekt skupiny šablon zobrazené web stránky

Příklady použití:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.url}" />
```
