# Zoznam atribútov WebJET CMS

Zoznam dostupných atribútov pri zobrazení stránky

## Web stránka

Údaje zobrazenej web stránky

- **ID web stránky** - `<div data-iwcm-write="doc_id"/>` - Vloží docid web stránky, napríklad: `148`
- **Názov web stránky** - `<div data-iwcm-write="doc_title"/>` - Vloží názov web stránky, napríklad: Web JET CMS
- **Text navigačnej lišty** - `<div data-iwcm-write="doc_navbar"/>` - Text web stránky pre navigačnú lištu (zvyčajne rovnaký ako názov web stránky - `doc_title`)
- **Text web stránky** - `<div data-iwcm-write="doc_data"/>` - Samotný text web stránky, ako bol zadaný v editore
- **Hlavička web stránky** - `<div data-iwcm-write="doc_header"/>` - HTML text s hlavičkou web stránky ako je definované vo virtuálnej šablóne, ktorú web stránka používa
- **Pätička web stránky** - `<div data-iwcm-write="doc_footer"/>` - HTML text s pätičkou web stránky ako je definované vo virtuálnej šablóne, ktorú web stránka používa
- **Menu web stránky** - `<div data-iwcm-write="doc_menu"/>` - HTML kód menu, ktorý sa zadáva v admin časti jednotlivým virtuálnym šablónam
- **Pravé menu web stránky** - `<div data-iwcm-write="doc_right_menu"/>` - HTML kód pravého menu, ktorý sa zadáva v admin časti jednotlivým virtuálnym šablónam
- **HTML kód zo šablóny** - `<div data-iwcm-write="after_body"/>` - HTML text z virtuálnej šablóny, ktorú web stránka používa
- **HTML hlavička stránky** - `<div data-iwcm-write="html_head"/>` - HTML kód, ktorý sa zadáva pre stránku v záložke HTML Vlastnosti ako HTML kód hlavičky. Môže obsahovať kód, ktorý sa vkladá do šablóny do sekcie `<head>`
- **HTML kód stránky** - `<div data-iwcm-write="html_data"/>` - HTML kód, ktorý sa zadáva pre stránku v záložke HTML Vlastnosti ako Dodatočný HTML kód.
- **Linka na hlavný kaskádový štýl** - `<div data-iwcm-write="base_css_link"/>` - Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako Hlavný CSS štýl (napr. `/css/page.css`).
- **Linka na hlavný kaskádový štýl** - `<div data-iwcm-write="base_css_link_noext"/>` - Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako Hlavný CSS štýl bez prípony (napr. `/css/page`). Je možné použiť pre vytváranie podmienených css štýlov (napr. `/css/page-dark.css`).
- **Linka na kaskádový štýl** - `<div data-iwcm-write="css_link"/>` - Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako CSS štýl (napr. `/css/custom.css`).
- **Linka na kaskádový štýl** - `<div data-iwcm-write="css_link_noext"/>` - Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako CSS štýl bez prípony (napr. /css/custom). Je možné použiť pre vytváranie podmienených css štýlov (napr. `/css/page-dark.css`).
- **Navigačná lišta** - `<div data-iwcm-write="navbar"/>` - Kompletná navigačná lišta, napr. Interway > Produkty > Web JET. Názvy sú klikateľné, linky sú vytvárané s použitím kaskádového štýlu `navbar`.
- **Dátum poslednej zmeny web stránky** - `<div data-iwcm-write="doc_date_created"/>` - Dátum, kedy bola web stránka naposledy zmenená.
- **Čas poslednej zmeny web stránky** - `<div data-iwcm-write="doc_time_created"/>` - Čas, kedy bola web stránka naposledy zmenená.
- **Vlastné polia** - `<div data-iwcm-write="field_a"/>` až `<div data-iwcm-write="field_l"/>` - Vlastné polia stránky definované v Rozšírených vlastnostiach, záložka Vlastné polia.
- **Voľné objekty šablóny** - `<div data-iwcm-write="template_object_a"/>` až `<div data-iwcm-write="template_object_d"/>` - Voľne použiteľné objekty šablóny. Obsahujú HTML kód priradenej stránky. Používajú sa ak nestačí použitie hlavičky, menu a pätičky.

## Publikovanie stránky

Údaje zadané pri editovaní web stránky v záložke Rozšírené

- **Dátum začiatku publikovania** - `<div data-iwcm-write="doc_publish_start"/>` - Dátum zadaný pri editovaní web stránky v záložke Rozšírené ako Začiatok.
- **Čas začiatku publikovania** - `<div data-iwcm-write="doc_publish_start_time"/>` - Čas zadaný pri editovaní web stránky v záložke Rozšírené ako Začiatok.
- **Dátum konca publikovania** - `<div data-iwcm-write="doc_publish_end"/>` - Dátum zadaný pri editovaní web stránky v záložke Rozšírené ako Koniec.
- **Čas konca publikovania** - `<div data-iwcm-write="doc_publish_end_time"/>` - Čas zadaný pri editovaní web stránky v záložke Rozšírené ako Koniec.
- **Dátum konania udalosti** - `<div data-iwcm-write="doc_event_date"/>` - Dátum konania udalosti zadaný pri editovaní web stránky v záložke Rozšírené. Používa sa napríklad pri akciách, kedy dátum akcie je neskôr ako požadovaný dátum začiatku publikovania (ak chceme, aby sa informácia na stránke zobrazovala od 1 dňa mesiaca, ale akcia o ktorej stránka informuje je až 10 dňa v mesiaci).
- **Čas konania udalosti** - `<div data-iwcm-write="doc_event_time"/>` - Čas konania udalosti zadaný pri editovaní web stránky v záložke Rozšírené.
- **Perex (anotácia) web stránky** - `<div data-iwcm-write="perex_data"/>` - Vloží do HTML šablóny anotáciu stránky.
- **Formátovaný perex (anotácia) web stránky** - `<div data-iwcm-write="perex_pre"/>` - Vloží do HTML šablóny formátovanú anotáciu stránky. Ak je anotácia zadaná bez HTML znakov, sú znaky nového riadku skonvertované na značky `<br>`
- **Miesto** - `<div data-iwcm-write="perex_place"/>` - Miesto, ktorého sa text stránky týka.
- **Obrázok** - `<div data-iwcm-write="perex_image"/>` - Obrázok, ktorý sa týka anotácie stránky.

## Autor stránky

Údaje o autorovi stránky (za autora stránky sa považuje používateľ, ktorý ju naposledy zmenil):

- **ID autora web stránky** - `<div data-iwcm-write="author_id"/>` - ID autora web stránky (používateľa, ktorý ju naposledy zmenil).
- **Meno autora web stránky** - `<div data-iwcm-write="author_name"/>` - Meno autora web stránky (používateľa, ktorý ju naposledy zmenil).
- **Email autora web stránky** - `<div data-iwcm-write="author_email"/>` - Email autora web stránky (používateľa, ktorý ju naposledy zmenil).

## Priečinok

Údaje o priečinku, v ktorom sa stránka nachádza:

- **ID adresára** - `<div data-iwcm-write="group_id"/>` - ID adresára, v ktorom sa nachádza stránka
- **Názov adresára** - `<div data-iwcm-write="group_name"/>` - Názov adresára, v ktorom sa nachádza stránka
- **Text navigačnej lišty** - `<div data-iwcm-write="group_navbar"/>` - Text adresára pre navigačnú lištu (zvyčajne rovnaký ako názov adresára)
- **HTML kód do hlavičky** - `<div data-iwcm-write="group_htmlhead"/>` - HTML kód do hlavičky adresára
- **HTML kód do hlavičky - rekurzívne získaný** - `<div data-iwcm-write="group_htmlhead_recursive"/>` - HTML kód do hlavičky adresára rekurzívne získaný - ak je obsah tohto pola pri aktuálnom adresári prázdny, získava sa hodnota z jeho nadradeného adresára, ak nie je zadaná ani tam, postupuje sa ďalej cez podadresáre k hlavnému adresáru
- **ID rodičov** - `<div data-iwcm-write="groupParentIds"/>` - Čiarkou oddelené ID nadradených adresárov - podla toho je možné v HTML šablóne určovať akú jazykovú mutáciu alebo sekciu práve zobrazujeme
- **Vlastné polia** - `<div data-iwcm-write="group_field_a"/>` až `<div data-iwcm-write="group_field_d"/>` - Vlastné polia adresára.

## Šablóna stránky

Údaje o šablóne stránky:

- **ID šablóny** - `<div data-iwcm-write="doc_temp_id"/>` - ID šablóny, ktorú web stránka používa.
- **Názov šablóny** - `<div data-iwcm-write="doc_temp_name"/>` - Názov šablóny, ktorú web stránka používa.
- **Hlavný CSS štýl** - `${ninja.temp.baseCssLink}` - Názov šablóny, ktorú web stránka používa.
- **Druhoradý CSS štýl** - `${ninja.temp.cssLink}` - Názov šablóny, ktorú web stránka používa.
- **Objekt šablóny** - `${tempDetails}` - Objekt šablóny, ktorú web stránka používa.
- **Objekt skupiny šablóny** - `${templatesGroupDetails}` - Objekt skupiny šablóny, ktorú web stránka používa.

## Ninja šablóna

Príklady použitia objektov z [Ninja šablóny](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) (príklady sú v [pug](../../developer/frameworks/pugjs.md) formáte). Typicky sa Ninja objekt používa v hlavičke šablóny:

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

link(rel='canonical' data-th-href='${ninja.page.canonical}')
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

Pri zobrazení stránky WebJET vkladá nasledovné objekty, tie môžete použiť priamo pre výpis pomocou ```${objekt.vlastnost}```. Názov odkazuje na dokumentáciu, kde vidno zoznam vlastností objektu:

- `request` - objekt `HttpServletRequest`
- `session` - objekt `HttpSession`
- [ninja](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - doplnkové atribúty a funkcie pre [Ninja šablónu](../ninja-starter-kit/README.md)
- [docDetails](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - objekt zobrazenej web stránky
- [docDetailsOriginal](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html) - ak je zobrazená prihlasovacia stránka obsahuje pôvodnú (zaheslovanú) stránku
- [pageGroupDetails](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html) - objekt adresára aktuálne zobrazenej web stránky
- [tempDetails](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html) - objekt šablóny aktuálne zobrazenej web stránky
- [templatesGroupDetails](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html) - objekt skupiny šablón zobrazenej web stránky

Príklady použitia:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.canonical}" />
```
