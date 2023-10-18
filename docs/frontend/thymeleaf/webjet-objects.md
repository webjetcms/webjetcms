# Zoznam atribútov WebJET CMS

Zoznam dostupných atribútov pri zobrazení stránky

## Web stránka

<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="50%" class="head">Názov a značka</td>
		<td width="50%" class="head">Popis</td>
	</tr>
</thead>
<tbody>
   <tr>
		<td class="t_body"><b>ID web stránky</b> <br>&lt;div data-iwcm-write="doc_id"/&gt;</td>
		<td class="t_body">Vloží docid web stránky, napríklad:<br> 148</td>
	</tr>
	<tr>
		<td class="t_body"><b>Názov web stránky</b> <br>&lt;div data-iwcm-write="doc_title"/&gt;</td>
		<td class="t_body">Vloží názov web stránky, napríklad:<br> Web JET CMS</td>
	</tr>
	<tr>
		<td class="t_body"><b>Text navigačnej lišty</b><br> &lt;div data-iwcm-write="doc_navbar"/&gt;</td>
		<td class="t_body">Text web stránky pre navigačnú lištu (zvyčajne rovnaký ako názov web stránky - doc_title)</td>
	</tr>
	<tr>
		<td class="t_body"><b>Text web stránky</b> <br>&lt;div data-iwcm-write="doc_data"/&gt;</td>
		<td class="t_body">Samotný text web stránky, ako bol zadaný v editore</td>
	</tr>
	<tr>
		<td class="t_body"><b>Hlavička web stránky</b> <br>&lt;div data-iwcm-write="doc_header"/&gt;</td>
		<td class="t_body">HTML text s hlavičkou web stránky ako je definované vo virtuálnej šablóne, ktorú web stránka používa</td>
	</tr>
	<tr>
		<td class="t_body"><b>Pätička web stránky</b> <br>&lt;div data-iwcm-write="doc_footer"/&gt;</td>
		<td class="t_body">HTML text s pätičkou web stránky ako je definované vo virtuálnej šablóne, ktorú web stránka používa</td>
	</tr>
	<tr>
		<td class="t_body"><b>Menu web stránky</b> <br>&lt;div data-iwcm-write="doc_menu"/&gt;</td>
		<td class="t_body">HTML kód menu, ktorý sa zadáva v admin časti jednotlivým virtuálnym šablónam</td>
	</tr>
	<tr>
		<td class="t_body"><b>Pravé menu web stránky</b> <br>&lt;div data-iwcm-write="doc_right_menu"/&gt;</td>
		<td class="t_body">HTML kód pravého menu, ktorý sa zadáva v admin časti jednotlivým virtuálnym šablónam</td>
	</tr>
	<tr>
		<td class="t_body"><b>HTML kód zo šablóny</b> <br>&lt;div data-iwcm-write="after_body"/&gt;</td>
		<td class="t_body">HTML text z virtuálnej šablóny, ktorú web stránka používa</td>
	</tr>
	<tr>
		<td class="t_body"><b>HTML hlavička stránky</b> <br>&lt;div data-iwcm-write="html_head"/&gt;</td>
		<td class="t_body">HTML kód, ktorý sa zadáva pre stránku v záložke HTML Vlastnosti ako HTML kód hlavičky. Môže obsahovať kód, ktorý sa vkladá do šablóny do sekcie &lt;head&gt;</td>
	</tr>
	<tr>
		<td class="t_body"><b>HTML kód stránky</b> <br>&lt;div data-iwcm-write="html_data"/&gt;</td>
		<td class="t_body">HTML kód, ktorý sa zadáva pre stránku v záložke HTML Vlastnosti ako Dodatočný HTML kód.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Linka na hlavný kaskádový štýl</b> <br>&lt;div data-iwcm-write="base_css_link"/&gt;</td>
		<td class="t_body">Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako Hlavný CSS štýl (napr. /css/page.css).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Linka na hlavný kaskádový štýl</b> <br>&lt;div data-iwcm-write="base_css_link_noext"/&gt;</td>
		<td class="t_body">Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako Hlavný CSS štýl bez prípony (napr. /css/page). Je možné použiť pre vytváranie podmienených css štýlov (napr. /css/page-ie6.css).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Linka na kaskádový štýl</b> <br>&lt;div data-iwcm-write="css_link"/&gt;</td>
		<td class="t_body">Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako CSS štýl (napr. /css/custom.css).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Linka na kaskádový štýl</b> <br>&lt;div data-iwcm-write="css_link_noext"/&gt;</td>
		<td class="t_body">Linka na kaskádový štýl, ktorý sa zadáva v admin časti virtuálnym šablónam ako CSS štýl bez prípony (napr. /css/custom). Je možné použiť pre vytváranie podmienených css štýlov (napr. /css/custom-ie6.css).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Navigačná lišta</b> <br>&lt;div data-iwcm-write="navbar"/&gt;</td>
		<td class="t_body">Kompletná navigačná lišta, napr. Interway &gt; Produkty &gt; Web JET<br> Názvy sú klikateľné, linky sú vytvárané s použitím kaskádového štýlu navbar.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Dátum poslednej zmeny web stránky</b> <br>&lt;div data-iwcm-write="doc_date_created"/&gt;</td>
		<td class="t_body">Dátum, kedy bola web stránka naposledy zmenená.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Čas poslednej zmeny web stránky</b> <br>&lt;div data-iwcm-write="doc_time_created"/&gt;</td>
		<td class="t_body">Čas, kedy bola web stránka naposledy zmenená.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Vlastné polia</b> <br>&lt;div data-iwcm-write="field_a"/&gt; až &lt;div data-iwcm-write="field_l"/&gt;</td>
		<td class="t_body">Vlastné polia stránky definované v Rozšírených vlastnostiach, záložka Vlastné polia.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Voľné objekty šablóny</b> <br>&lt;div data-iwcm-write="template_object_a"/&gt; až &lt;div data-iwcm-write="template_object_d"/&gt;</td>
		<td class="t_body">Voľne použiteľné objekty šablóny. Obsahujú HTML kód priradenej stránky. Používajú sa ak nestačí použitie hlavičky, menu a pätičky.</td>
	</tr>
</tbody>
</table>

## Publikovanie stránky

Údaje zadané pri editovaní web stránky v záložke Rozšírené

<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="50%" class="head">Názov a značka</td>
		<td width="50%" class="head">Popis</td>
	</tr>
</thead>
<tbody>
   <tr>
		<td class="t_body"><b>Dátum začiatku publikovania</b> <br>&lt;div data-iwcm-write="doc_publish_start"/&gt;</td>
		<td class="t_body">Dátum zadaný pri editovaní web stránky v záložke Rozšírené ako Začiatok.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Čas začiatku publikovania</b> <br>&lt;div data-iwcm-write="doc_publish_start_time"/&gt;</td>
		<td class="t_body">Čas zadaný pri editovaní web stránky v záložke Rozšírené ako Začiatok.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Dátum konca publikovania</b> <br>&lt;div data-iwcm-write="doc_publish_end"/&gt;</td>
		<td class="t_body">Dátum zadaný pri editovaní web stránky v záložke Rozšírené ako Koniec.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Čas konca publikovania</b> <br>&lt;div data-iwcm-write="doc_publish_end_time"/&gt;</td>
		<td class="t_body">Čas zadaný pri editovaní web stránky v záložke Rozšírené ako Koniec.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Dátum konania udalosti</b> <br>&lt;div data-iwcm-write="doc_event_date"/&gt;</td>
		<td class="t_body">Dátum konania udalosti zadaný pri editovaní web stránky v záložke Rozšírené. Používa sa napríklad pri akciách, kedy dátum akcie je neskôr ako požadovaný dátum začiatku publikovania (ak chceme, aby sa informácia na stránke zobrazovala od 1 dňa mesiaca, ale akcia o ktorej stránka informuje je až 10 dňa v mesiaci).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Čas konania udalosti</b> <br>&lt;div data-iwcm-write="doc_event_time"/&gt;</td>
		<td class="t_body">Čas konania udalosti zadaný pri editovaní web stránky v záložke Rozšírené.</td>
	</tr>
   <tr>
		<td class="t_body"><b>Perex (anotácia) web stránky</b> <br>&lt;div data-iwcm-write="perex_data"/&gt;</td>
		<td class="t_body">Vloží do HTML šablóny anotáciu stránky.</td>
	</tr>
   <tr>
		<td class="t_body"><b>Formátovaný perex (anotácia) web stránky</b> <br>&lt;div data-iwcm-write="perex_pre"/&gt;</td>
		<td class="t_body">Vloží do HTML šablóny formátovanú anotáciu stránky. Ak je anotácia zadaná bez HTML znakov, sú znaky nového riadku skonvertované na značky &lt;br&gt;</td>
	</tr>
	<tr>
		<td class="t_body"><b>Miesto</b> <br>&lt;div data-iwcm-write="perex_place"/&gt;</td>
		<td class="t_body">Miesto, ktorého sa text stránky týka.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Obrázok</b> <br>&lt;div data-iwcm-write="perex_image"/&gt;</td>
		<td class="t_body">Obrázok, ktorý sa týka anotácie stránky.</td>
	</tr>
</tbody>
</table>

## Autor stránky

Údaje o autorovi stránky (za autora stránky sa považuje používateľ, ktorý ju naposledy zmenil):

<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="50%" class="head">Názov a značka</td>
		<td width="50%" class="head">Popis</td>
	</tr>
</thead>
<tbody>
   <tr>
		<td class="t_body"><b>ID autora web stránky</b> <br>&lt;div data-iwcm-write="author_id"/&gt;</td>
		<td class="t_body">ID autora web stránky (používateľa, ktorý ju naposledy zmenil).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Meno autora web stránky</b> <br>&lt;div data-iwcm-write="author_name"/&gt;</td>
		<td class="t_body">Meno autora web stránky (používateľa, ktorý ju naposledy zmenil).</td>
	</tr>
	<tr>
		<td class="t_body"><b>Email autora web stránky</b> <br>&lt;div data-iwcm-write="author_email"/&gt;</td>
		<td class="t_body">Email autora web stránky (používateľa, ktorý ju naposledy zmenil).</td>
	</tr>
</tbody>
</table>

## Priečinok

Údaje o priečinku, v ktorom sa stránka nachádza:

<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="50%" class="head">Názov a značka</td>
		<td width="50%" class="head">Popis</td>
	</tr>
</thead>
<tbody>
	<tr>
		<td class="t_body"><b>ID adresára</b> <br>&lt;div data-iwcm-write="group_id"/&gt;</td>
		<td class="t_body">ID adresára, v ktorom sa nachádza stránka</td>
	</tr>
	<tr>
		<td class="t_body"><b>Názov adresára</b> <br>&lt;div data-iwcm-write="group_name"/&gt;</td>
		<td class="t_body">Názov adresára, v ktorom sa nachádza stránka</td>
	</tr>
	<tr>
		<td class="t_body"><b>Text navigačnej lišty</b> <br>&lt;div data-iwcm-write="group_navbar"/&gt;</td>
		<td class="t_body">Text adresára pre navigačnú lištu (zvyčajne rovnaký ako názov adresára)</td>
	</tr>
	<tr>
		<td class="t_body"><b>HTML kód do hlavičky</b> <br>&lt;div data-iwcm-write="group_htmlhead"/&gt;</td>
		<td class="t_body">HTML kód do hlavičky adresára</td>
	</tr>
	<tr>
		<td class="t_body"><b>HTML kód do hlavičky - rekurzívne získaný</b> <br>&lt;div data-iwcm-write="group_htmlhead_recursive"/&gt;</td>
		<td class="t_body">HTML kód do hlavičky adresára rekurzívne získaný - ak je obsah tohto pola pri aktuálnom adresári prázdny, získava sa hodnota z jeho nadradeného adresára, ak nie je zadaná ani tam, postupuje sa ďalej cez podadresáre k hlavnému adresáru</td>
	</tr>
	<tr>
		<td class="t_body"><b>groupParentIds</b> <br>&lt;div data-iwcm-write="groupParentIds"/&gt;</td>
		<td class="t_body">Čiarkou oddelené ID nadradených adresárov - podla toho je možné v HTML šablóne určovať akú jazykovú mutáciu alebo sekciu práve zobrazujeme</td>
	</tr>
	<tr>
		<td class="t_body"><b>Vlastné polia</b> <br>&lt;div data-iwcm-write="group_field_a"/&gt; až &lt;div data-iwcm-write="group_field_d"/&gt;</td>
		<td class="t_body">Vlastné polia adresára.</td>
	</tr>
</tbody>
</table>

## Šablóna stránky

Údaje o šablóne stránky:

<table width="100%" class="ramikTable">
<thead>
	<tr>
		<td width="50%" class="head">Názov a značka</td>
		<td width="50%" class="head">Popis</td>
	</tr>
</thead>
<tbody>
   <tr>
		<td class="t_body"><b>ID šablóny</b> <br>&lt;div data-iwcm-write="doc_temp_id"/&gt;</td>
		<td class="t_body">ID šablóny, ktorú web stránka používa.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Názov šablóny</b> <br>&lt;div data-iwcm-write="doc_temp_name"/&gt;</td>
		<td class="t_body">Názov šablóny, ktorú web stránka používa.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Hlavný CSS štýl</b> <br>${ninja.temp.baseCssLink}</td>
		<td class="t_body">Názov šablóny, ktorú web stránka používa.</td>
	</tr>
	<tr>
		<td class="t_body"><b>Druhoradý CSS štýl</b> <br>${ninja.temp.cssLink}</td>
		<td class="t_body">Názov šablóny, ktorú web stránka používa.</td>
	</tr>
   <tr>
       <td class="t_body"><b>Objekt šablóny</b> <br>(TemplateDetails)request.getAttribute("templateDetails")</td>
       <td class="t_body">Objekt šablóny, ktorú web stránka používa.</td>
   </tr>
   <tr>
       <td class="t_body"><b>Objekt skupiny šablóny</b> <br>(TemplatesGroupBean)request.getAttribute("templatesGroupDetails")</td>
       <td class="t_body">Objekt skupiny šablóny, ktorú web stránka používa.</td>
   </tr>
</tbody>
</table>

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

Pri zobrazení stránky WebJET vkladá nasledovné objekty, tie môžete použiť priamo pre výpis pomocou ```${objekt.vlastnost}```. Názov odkazuje na dokumentáciu, kde vidno zoznam vlastností objektu:

- [ninja](../../developer/javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) - doplnkové atribúty a funkcie pre [Ninja šablónu](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/)
- [docDetails](/developer/javadoc/index.html?sk/iway/iwcm/doc/DocBasic.html) - objekt zobrazenej web stránky
- [docDetailsOriginal](/developer/javadoc/index.html?sk/iway/iwcm/doc/DocBasic.html) - ak je zobrazená prihlasovacia stránka obsahuje pôvodnú (zaheslovanú) stránku
- [groupDetails](/developer/javadoc/index.html?sk/iway/iwcm/doc/GroupDetails.html) - objekt adresára aktuálne zobrazenej web stránky
- [tempDetails](/developer/javadoc/index.html?sk/iway/iwcm/doc/TemplateDetails.html) - objekt šablóny aktuálne zobrazenej web stránky

Príklady použitia:

```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.url}" />
```
