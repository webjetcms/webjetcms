# Seznam atributů systému WebJET CMS

Seznam dostupných atributů při zobrazení stránky

## Webová stránka

Zobrazená data webové stránky

<table width="100%" class="ramikTable">
  <thead>
    <tr>
      <td width="50%" class="head">Název a značka</td>Popis<td width="50%" class="head">ID webové stránky</td>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td class="t_body"><b>\<div data-iwcm-write="doc\_id"/></b>Vloží například docid webové stránky:<br> 148</td>Název webové stránky<td class="t_body">\<div data-iwcm-write="doc\_title"/><br>Vloží název webové stránky, například:</td>
    </tr><tr>
      <td class="t_body"><b> Web JET CMS</b>Text navigačního panelu<br> \<div data-iwcm-write="doc\_navbar"/></td>Text webové stránky pro navigační panel (obvykle stejný jako název webové stránky - doc\_title).<td class="t_body">Text webové stránky<br>\<div data-iwcm-write="doc\_data"/></td>
    </tr><tr>
      <td class="t_body"><b>Skutečný text webové stránky zadaný v editoru</b><br>Záhlaví webové stránky</td>\<div data-iwcm-write="doc\_header"/><td class="t_body">Text HTML s hlavičkou webové stránky definovanou ve virtuální šabloně použité webovou stránkou.</td>
    </tr><tr>
      <td class="t_body"><b>Zápatí webové stránky</b>\<div data-iwcm-write="doc\_footer"/><br>Text HTML se zápatím webové stránky definovaný ve virtuální šabloně použité webovou stránkou.</td>Nabídka webových stránek<td class="t_body">\<div data-iwcm-write="doc\_menu"/></td>
    </tr><tr>
      <td class="t_body"><b>HTML kód menu, který se zadává v sekci správce každé virtuální šablony.</b>Pravé menu webové stránky<br>\<div data-iwcm-write="doc\_right\_menu"/></td>HTML kód pravého menu, který se zadává v sekci správce každé virtuální šablony.<td class="t_body">Kód HTML ze šablony</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="after\_body"/></b>Text HTML z virtuální šablony použité webovou stránkou<br>Záhlaví stránky HTML</td>\<div data-iwcm-write="html\_head"/><td class="t_body">Kód HTML, který je pro stránku zadán na kartě Vlastnosti HTML jako kód záhlaví HTML. Může obsahovat kód, který je vložen do šablony v záložce \<head></td>
    </tr><tr>
      <td class="t_body"><b>Kód HTML stránky</b>\<div data-iwcm-write="html\_data"/><br>Kód HTML, který je pro stránku zadán na kartě Vlastnosti HTML jako Další kód HTML.</td>Řádek k hlavnímu kaskádovému stylu<td class="t_body">\<div data-iwcm-write="base\_css\_link"/></td>
    </tr><tr>
      <td class="t_body"><b>Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako Hlavní styl CSS (např. /css/page.css).</b>Řádek k hlavnímu kaskádovému stylu<br>\<div data-iwcm-write="base\_css\_link\_noext"/></td>Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako Hlavní styl CSS bez přípony (např. /css/page). Lze použít k vytvoření podmíněných stylů css (např. /css/page-ie6.css).<td class="t_body">Linka ve stylu kaskády</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="css\_link"/></b>Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako styl CSS (např. /css/custom.css).<br>Linka ve stylu kaskády</td>\<div data-iwcm-write="css\_link\_noext"/><td class="t_body">Odkaz na kaskádový styl, který je zadán v části správce virtuálních šablon jako styl CSS bez přípony (např. /css/custom). Lze použít k vytvoření podmíněných stylů css (např. /css/custom-ie6.css).</td>
    </tr><tr>
      <td class="t_body"><b>Navigační panel</b>\<div data-iwcm-write="navbar"/><br>Kompletní navigační panel, např. Interway > Produkty > Web JET</td> Nadpisy jsou klikací, odkazy jsou vytvořeny pomocí kaskádového stylu navbar.<td class="t_body">Datum poslední změny webových stránek</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="doc\_date\_created"/></b>Datum poslední změny webové stránky.<br>Čas poslední změny webové stránky</td>\<div data-iwcm-write="doc\_time\_created"/><td class="t_body">Čas poslední změny webové stránky.</td>
    </tr><tr>
      <td class="t_body"><b>Vlastní pole</b>\<div data-iwcm-write="field\_a"/> na \<div data-iwcm-write="field\_l"/><br>Vlastní pole stránky definovaná v části Upřesnit vlastnosti, karta Vlastní pole.</td>Bezplatné objekty šablon<td class="t_body">\<div data-iwcm-write="template\_object\_a"/> na \<div data-iwcm-write="template\_object\_d"/></td>
    </tr><tr>
      <td class="t_body"><b>Volně použitelné objekty šablon. Obsahují kód HTML přidružené stránky. Používají se, pokud nestačí použití záhlaví, menu a zápatí.</b>Zveřejnění stránky<br>Údaje zadané při úpravě webové stránky na kartě Upřesnit</td>Název a značka<td class="t_body">Popis</td>
    </tr><tr>
      <td class="t_body"><b>Datum zahájení zveřejňování</b>\<div data-iwcm-write="doc\_publish\_start"/><br>Datum zadané při úpravě webové stránky na kartě Upřesnit jako Začátek.</td>Čas zahájení zveřejnění<td class="t_body">\<div data-iwcm-write="doc\_publish\_start\_time"/></td>
    </tr><tr>
      <td class="t_body"><b>Čas zadaný při úpravě webové stránky na kartě Upřesnit jako Start.</b>Datum ukončení zveřejnění<br>\<div data-iwcm-write="doc\_publish\_end"/></td>Datum zadané při úpravě webové stránky na kartě Upřesnit jako Konec.<td class="t_body">Čas ukončení zveřejnění</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="doc\_publish\_end\_time"/></b>Čas zadaný při úpravě webové stránky na kartě Upřesnit jako Konec.<br>Datum konání akce</td>\<div data-iwcm-write="doc\_event\_date"/><td class="t_body">Datum události zadané při úpravě webové stránky na kartě Upřesnit. Používá se například u událostí, kde je datum události pozdější než požadované datum začátku publikování (pokud chceme, aby se informace na stránce zobrazovaly od 1. dne v měsíci, ale událost, o které stránka informuje, je až 10. dne v měsíci).<br>Čas události</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="doc\_event\_time"/></b>Čas události zadaný při úpravě webové stránky na kartě Upřesnit.<br>Webové stránky Perex (anotace)</td>\<div data-iwcm-write="perex\_data"/><td class="t_body">Vloží anotaci stránky do šablony HTML.</td>
    </tr><tr>
      <td class="t_body"><b>Formátovaný perex (anotace) webové stránky</b>\<div data-iwcm-write="perex\_pre"/><br>Vloží do šablony HTML formátovanou anotaci stránky. Pokud je anotace zadána bez znaků HTML, znaky nového řádku se převedou na značky. 
</td>Místo<td class="t_body">\<div data-iwcm-write="perex\_place"/></td>
    </tr><tr>
      <td class="t_body"><b>Místo, na které odkazuje text stránky.</b>Obrázek<br>\<div data-iwcm-write="perex\_image"/></td>Obrázek související s anotací stránky.<td class="t_body">Autor stránky</td>
    </tr><tr>
      <td class="t_body"><b>Údaje o autorovi stránky (za autora stránky se považuje uživatel, který ji naposledy změnil):</b>Název a značka<br>Popis</td>ID autora webové stránky<td class="t_body">\<div data-iwcm-write="author\_id"/></td>
    </tr>
  </tbody>
</table>

## ID autora webové stránky (uživatele, který ji naposledy změnil).

Jméno autora webové stránky

<table width="100%" class="ramikTable">
  <thead>
    <tr>
      <td width="50%" class="head">\<div data-iwcm-write="author\_name"/></td>Jméno autora webové stránky (uživatele, který ji naposledy změnil).<td width="50%" class="head">E-mail autora webových stránek</td>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td class="t_body"><b>\<div data-iwcm-write="author\_email"/></b>E-mail autora webové stránky (uživatele, který ji naposledy změnil).<br>Složka</td>Podrobnosti o složce, ve které se stránka nachází:<td class="t_body">Název a značka</td>
    </tr><tr>
      <td class="t_body"><b>Popis</b>ID adresáře<br>\<div data-iwcm-write="group\_id"/></td>ID adresáře, ve kterém se stránka nachází.<td class="t_body">Název adresáře</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="group\_name"/></b>Název adresáře, ve kterém se stránka nachází.<br>Text navigačního panelu</td>\<div data-iwcm-write="group\_navbar"/><td class="t_body">Text adresáře pro navigační panel (obvykle stejný jako název adresáře).</td>
    </tr><tr>
      <td class="t_body"><b>Kód záhlaví HTML</b>\<div data-iwcm-write="group\_htmlhead"/><br>Kód HTML pro záhlaví adresáře</td>Kód hlavičky HTML - rekurzivně načtený<td class="t_body">\<div data-iwcm-write="group\_htmlhead\_recursive"/></td>
    </tr><tr>
      <td class="t_body"><b>HTML kód k rekurzivně načtené hlavičce adresáře - pokud je obsah tohoto pole pro aktuální adresář prázdný, načte se hodnota z nadřazeného adresáře, pokud není zadána ani tam, předá se přes podadresáře do hlavního adresáře.</b>groupParentIds<br>\<div data-iwcm-write="groupParentIds"/></td>ID nadřazených adresářů oddělené čárkou - podle toho lze v šabloně HTML určit, který jazyk nebo sekce se aktuálně zobrazuje.<td class="t_body">Vlastní pole</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="group\_field\_a"/> na \<div data-iwcm-write="group\_field\_d"/></b>Vlastní pole adresáře.<br>Šablona stránky</td>Podrobnosti o šabloně stránky:<td class="t_body">Název a značka</td>
    </tr><tr>
      <td class="t_body"><b>Popis</b>ID šablony<br>\<div data-iwcm-write="doc\_temp\_id"/></td>ID šablony, kterou webová stránka používá.<td class="t_body">Název šablony</td>
    </tr><tr>
      <td class="t_body"><b>\<div data-iwcm-write="doc\_temp\_name"/></b>Název šablony, kterou webová stránka používá.<br>Hlavní styl CSS</td>$<td class="t_body">Název šablony, kterou webová stránka používá.</td>
    </tr><tr>
      <td class="t_body"><b>Sekundární styl CSS</b>$<br>Název šablony, kterou webová stránka používá.</td>Objekt šablony<td class="t_body">$</td>
    </tr><tr>
      <td class="t_body"><b>Objekt šablony, který webová stránka používá.</b>Objekt skupiny šablon<br>$</td>Objekt skupiny šablon, který webová stránka používá.<td class="t_body">Šablona Ninja</td>
    </tr>
  </tbody>
</table>

## Příklady použití objektů z&#x20;

Šablony Ninja

<table width="100%" class="ramikTable">
  <thead>
    <tr>
      <td width="50%" class="head"> (příklady jsou v </td>mops<td width="50%" class="head"> formát). Obvykle se objekt Ninja používá v záhlaví šablony:</td>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td class="t_body"><b>Dostupné objekty</b>Při zobrazení stránky WebJET vloží následující objekty, které můžete použít přímo pro výpis pomocí <br>. Název odkazuje na dokumentaci, kde je k dispozici seznam vlastností objektu:</td> - objekt <td class="t_body"> - objekt </td>
    </tr><tr>
      <td class="t_body"><b>ninja</b> - další atributy a funkce pro <br>Šablona Ninja</td>docDetails<td class="t_body"> - objekt zobrazené webové stránky</td>
    </tr><tr>
      <td class="t_body"><b>docDetailsOriginal</b> - pokud zobrazená přihlašovací stránka obsahuje původní (zaheslovanou) stránku.<br>pageGroupDetails</td> - objekt adresáře aktuálně zobrazené webové stránky<td class="t_body">tempDetails</td>
    </tr>
  </tbody>
</table>

## &#x20;- objekt šablony aktuálně zobrazené webové stránky

templatesGroupDetails

<table width="100%" class="ramikTable">
  <thead>
    <tr>
      <td width="50%" class="head"> - objekt skupiny šablon zobrazené webové stránky</td>Příklady použití:<td width="50%" class="head"></td>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr>
  </tbody>
</table>

##



<table width="100%" class="ramikTable">
  <thead>
    <tr>
      <td width="50%" class="head"></td><td width="50%" class="head"></td>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br></td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br>{ninja.temp.baseCssLink}</td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br>{ninja.temp.cssLink}</td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br>{tempDetails}</td><td class="t_body"></td>
    </tr><tr>
      <td class="t_body"><b></b><br>{templatesGroupDetails}</td><td class="t_body"></td>
    </tr>
  </tbody>
</table>

##

[](http://docs.webjetcms.sk/v8/#/ninja-starter-kit/) [](../../developer/frameworks/pugjs.md)

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

##

`${objekt.vlastnost}`

- `request` `HttpServletRequest`
- `session` `HttpSession`
- [](../../../javadoc/sk/iway/iwcm/doc/ninja/Ninja.html) [](../ninja-starter-kit/README.md)
- [](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html)
- [](../../../javadoc/sk/iway/iwcm/doc/DocBasic.html)
- [](../../../javadoc/sk/iway/iwcm/doc/GroupDetails.html)
- [](../../../javadoc/sk/iway/iwcm/doc/TemplateDetails.html)
- [](../../../javadoc/sk/iway/iwcm/doc/TemplatesGroupBean.html)



```html
    <span data-th-text="${docDetails.title}">Titulok stránky</span>
    <body data-th-class="${docDetails.fieldA}">
    <meta name="author" data-th-content="${ninja.temp.group.author}" />
    <link rel="canonical" data-th-href="${ninja.page.url}" />
```
