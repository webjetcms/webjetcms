# Seznam atributů systému WebJET CMS

Seznam dostupných atributů při zobrazení stránky

## Webová stránka

Zobrazená data webové stránky

- **ID webové stránky** - `<div data-iwcm-write="doc_id"/>` - Vloží například docid webové stránky: `148`
- **Název webové stránky** - `<div data-iwcm-write="doc_title"/>` - Vloží název webové stránky, například Web JET CMS.
- **Text navigačního panelu** - `<div data-iwcm-write="doc_navbar"/>` - Text webové stránky pro navigační panel (obvykle stejný jako název webové stránky) - `doc_title`)
- **Text webové stránky** - `<div data-iwcm-write="doc_data"/>` - Skutečný text webové stránky zadaný v editoru
- **Záhlaví webové stránky** - `<div data-iwcm-write="doc_header"/>` - Text HTML s hlavičkou webové stránky definovanou ve virtuální šabloně použité webovou stránkou.
- **Zápatí webové stránky** - `<div data-iwcm-write="doc_footer"/>` - Text HTML se zápatím webové stránky definovaný ve virtuální šabloně použité webovou stránkou.
- **Nabídka webových stránek** - `<div data-iwcm-write="doc_menu"/>` - HTML kód menu, který se zadává v sekci správce každé virtuální šablony.
- **Pravé menu webové stránky** - `<div data-iwcm-write="doc_right_menu"/>` - HTML kód pravého menu, který se zadává v sekci správce každé virtuální šablony.
- **Kód HTML ze šablony** - `<div data-iwcm-write="after_body"/>` - Text HTML z virtuální šablony použité webovou stránkou
- **Záhlaví stránky HTML** - `<div data-iwcm-write="html_head"/>` - Kód HTML, který je pro stránku zadán na kartě Vlastnosti HTML jako kód záhlaví HTML. Může obsahovat kód, který je vložen do šablony v záložce `<head>`
- **Kód HTML stránky** - `<div data-iwcm-write="html_data"/>` - Kód HTML, který je pro stránku zadán na kartě Vlastnosti HTML jako Další kód HTML.
- **Řádek k hlavnímu kaskádovému stylu** - `<div data-iwcm-write="base_css_link"/>` - Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako Hlavní styl CSS (např. `/css/page.css`).
- **Řádek k hlavnímu kaskádovému stylu** - `<div data-iwcm-write="base_css_link_noext"/>` - Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako Hlavní styl CSS bez přípony (např. `/css/page`). Lze jej použít k vytváření podmíněných stylů css (např. `/css/page-dark.css`).
- **Linka ve stylu kaskády** - `<div data-iwcm-write="css_link"/>` - Odkaz na kaskádový styl, který je zadán v části pro správu virtuálních šablon jako styl CSS (např. `/css/custom.css`).
- **Linka ve stylu kaskády** - `<div data-iwcm-write="css_link_noext"/>` - Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako styl CSS bez přípony (např. /css/custom). Lze jej použít k vytvoření podmíněných stylů css (např. `/css/page-dark.css`).
- **Navigační panel** - `<div data-iwcm-write="navbar"/>` - Kompletní navigační panel, např. Interway > Produkty > Web JET. Nadpisy jsou klikací, odkazy jsou vytvořeny pomocí kaskádového stylu. `navbar`.
- **Datum poslední změny webových stránek** - `<div data-iwcm-write="doc_date_created"/>` - Datum poslední změny webové stránky.
- **Čas poslední změny webové stránky** - `<div data-iwcm-write="doc_time_created"/>` - Čas poslední změny webové stránky.
- **Vlastní pole** - `<div data-iwcm-write="field_a"/>` na `<div data-iwcm-write="field_l"/>` - Vlastní pole stránky definovaná v části Upřesnit vlastnosti, karta Vlastní pole.
- **Bezplatné objekty šablon** - `<div data-iwcm-write="template_object_a"/>` na `<div data-iwcm-write="template_object_d"/>` - Volně použitelné objekty šablon. Obsahují kód HTML přidružené stránky. Používají se, pokud nestačí použití záhlaví, menu a zápatí.

## Zveřejnění stránky

Údaje zadané při úpravě webové stránky na kartě Upřesnit

- **Datum zahájení zveřejňování** - `<div data-iwcm-write="doc_publish_start"/>` - Datum zadané při úpravě webové stránky na kartě Upřesnit jako Začátek.
- **Čas zahájení zveřejnění** - `<div data-iwcm-write="doc_publish_start_time"/>` - Čas zadaný při úpravě webové stránky na kartě Upřesnit jako Start.
- **Datum ukončení zveřejnění** - `<div data-iwcm-write="doc_publish_end"/>` - Datum zadané při úpravě webové stránky na kartě Upřesnit jako Konec.
- **Čas ukončení zveřejnění** - `<div data-iwcm-write="doc_publish_end_time"/>` - Čas zadaný při úpravě webové stránky na kartě Upřesnit jako Konec.
- **Datum konání akce** - `<div data-iwcm-write="doc_event_date"/>` - Datum události zadané při úpravě webové stránky na kartě Upřesnit. Používá se například u událostí, kde je datum události pozdější než požadované datum začátku publikování (pokud chceme, aby se informace na stránce zobrazovaly od 1. dne v měsíci, ale událost, o které stránka informuje, je až 10. dne v měsíci).
- **Čas události** - `<div data-iwcm-write="doc_event_time"/>` - Čas události zadaný při úpravě webové stránky na kartě Upřesnit.
- **Webové stránky Perex (anotace)** - `<div data-iwcm-write="perex_data"/>` - Vloží anotaci stránky do šablony HTML.
- **Formátovaný perex (anotace) webové stránky** - `<div data-iwcm-write="perex_pre"/>` - Vloží do šablony HTML formátovanou anotaci stránky. Pokud je anotace zadána bez znaků HTML, znaky nového řádku se převedou na značky. `<br>`
- **Místo** - `<div data-iwcm-write="perex_place"/>` - Místo, na které odkazuje text stránky.
- **Obrázek** - `<div data-iwcm-write="perex_image"/>` - Obrázek související s anotací stránky.

## Autor stránky

Údaje o autorovi stránky (za autora stránky se považuje uživatel, který ji naposledy změnil):
- **ID autora webové stránky** - `<div data-iwcm-write="author_id"/>` - ID autora webové stránky (uživatele, který ji naposledy změnil).
- **Jméno autora webové stránky** - `<div data-iwcm-write="author_name"/>` - Jméno autora webové stránky (uživatele, který ji naposledy změnil).
- **E-mail autora webových stránek** - `<div data-iwcm-write="author_email"/>` - E-mail autora webové stránky (uživatele, který ji naposledy změnil).

## Složka

Podrobnosti o složce, ve které se stránka nachází:
- **ID adresáře** - `<div data-iwcm-write="group_id"/>` - ID adresáře, ve kterém se stránka nachází.
- **Název adresáře** - `<div data-iwcm-write="group_name"/>` - Název adresáře, ve kterém se stránka nachází.
- **Text navigačního panelu** - `<div data-iwcm-write="group_navbar"/>` - Text adresáře pro navigační panel (obvykle stejný jako název adresáře).
- **Kód záhlaví HTML** - `<div data-iwcm-write="group_htmlhead"/>` - Kód HTML pro záhlaví adresáře
- **Kód hlavičky HTML - rekurzivně načtený** - `<div data-iwcm-write="group_htmlhead_recursive"/>` - HTML kód k rekurzivně načtené hlavičce adresáře - pokud je obsah tohoto pole pro aktuální adresář prázdný, je hodnota načtena z nadřazeného adresáře, pokud není uvedena ani tam, je předána přes podadresáře do hlavního adresáře.
- **ID rodiče** - `<div data-iwcm-write="groupParentIds"/>` - ID nadřazených adresářů oddělené čárkou - podle toho je možné v šabloně HTML určit, který jazyk nebo sekce se právě zobrazuje.
- **Vlastní pole** - `<div data-iwcm-write="group_field_a"/>` na `<div data-iwcm-write="group_field_d"/>` - Vlastní pole adresáře.

## Šablona stránky

Podrobnosti o šabloně stránky:
- **ID šablony** - `<div data-iwcm-write="doc_temp_id"/>` - ID šablony, kterou webová stránka používá.
- **Název šablony** - `<div data-iwcm-write="doc_temp_name"/>` - Název šablony, kterou webová stránka používá.
- **Hlavní styl CSS** - `${ninja.temp.baseCssLink}` - Název šablony, kterou webová stránka používá.
- **Sekundární styl CSS** - `${ninja.temp.cssLink}` - Název šablony, kterou webová stránka používá.
- **Objekt šablony** - `${tempDetails}` - Objekt šablony, který webová stránka používá.
- **Objekt skupiny šablon** - `${templatesGroupDetails}` - Objekt skupiny šablon, který webová stránka používá.

## Šablona Ninja

Příklady použití objektů z [Šablony Ninja](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) (příklady jsou v [mops](../../developer/frameworks/pugjs.md) formát). Obvykle se objekt Ninja používá v záhlaví šablony:

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

Při zobrazení stránky WebJET vloží následující objekty, které můžete použít přímo pro výpis pomocí `${objekt.vlastnost}`. Název odkazuje na dokumentaci, kde je k dispozici seznam vlastností objektu:
- `request` - objekt `HttpServletRequest`
- `session` - objekt `HttpSession`
- [ninja](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - další atributy a funkce pro [Šablona Ninja](../ninja-starter-kit/README.md)
- [docDetails](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - objekt zobrazené webové stránky
- [docDetailsOriginal](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - pokud zobrazená přihlašovací stránka obsahuje původní (zaheslovanou) stránku.
- [pageGroupDetails](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html) - objekt adresáře aktuálně zobrazené webové stránky
- [tempDetails](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html) - objekt šablony aktuálně zobrazené webové stránky
- [templatesGroupDetails](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html) - objekt skupiny šablon zobrazené webové stránky

Příklady použití:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.url}" />
```
