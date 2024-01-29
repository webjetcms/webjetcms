# Thymeleaf

[Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#messages) je šablónovací framework. Vo WebJETe je spojený so Spring MVC.


<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

- [Thymeleaf](#thymeleaf)
  - [Mapovanie URL](#mapovanie-url)
  - [Vloženie vlastných objektov do modelu](#vloženie-vlastných-objektov-do-modelu)
  - [LayoutService](#layoutservice)
  - [Základný výpis textu / atribútu](#základný-výpis-textu--atribútu)
  - [Prekladový text](#prekladový-text)
  - [Select box](#select-box)
  - [Spájanie reťazcov](#spájanie-reťazcov)
  - [Podmienené zobrazenie tagu](#podmienené-zobrazenie-tagu)
  - [Získanie objektu z Constants](#získanie-objektu-z-constants)
  - [Kontrola práv](#kontrola-práv)
  - [Overenie práva](#overenie-práva)
  - [Chyba TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context](#chyba-templateprocessingexception-only-variable-expressions-returning-numbers-or-booleans-are-allowed-in-this-context)

<!-- /code_chunk_output -->

## Mapovanie URL

Mapovanie URL adries admin časti sa vykonáva v [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) ako:

```java
@GetMapping({ "/admin/v9/", "/admin/v9/{page}/", "/admin/v9/{page}/{subpage}" })
...
    // /admin/v9/ == zobraz dashboard
    if (Tools.isEmpty(page))
    {
        page = "dashboard";
        subpage = "overview";
    }

    if (Tools.isEmpty(subpage)) subpage = "index";

    final String forward = "admin/v9/dist/views/" + page + "/" + subpage;
```

pričom ak ```subpage``` nie je zadané hľadá sa súbor ```index.pug```. Pri volaní /admin/v9/ sa vykoná stránka ```dashboard/overview.pug```.

Všetky predvolené objekty sa vkladajú do modelu cez [LayoutBean.java](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutBean.java).

## Vloženie vlastných objektov do modelu

Ak potrebujete pre určité stránky administrácie vložiť vlastný objekt do modelu je možné využiť udalosti. [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) publikuje udalosť [ThymeleafEvent](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafEvent.java), na ktorú je možné počúvať. V udalosti je publikovaný ```ModelMap``` objekt aj ```HttpServletRequest```.

Príkladom použitia je [WebPagesListener](../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java).

Dôležité je použiť anotáciu ```@Component``` (čo vyžaduje aj pridanie package v triede [SpringConfig](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java) v anotácii ```@ComponentScan```) a samozrejme ```@EventListener```. V podmienkach je potrebné nastaviť hodnotu parametra ```page``` a ```subpage```, aby počúvanie udalosti bolo vykonané len pre zadanú stránku administrácie.

```java
@Component
public class WebPagesListener {
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='web-pages-list'")
    private void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();
            Identity user = UsersDB.getCurrentUser(event.getSource().getRequest());

            String webpagesInitialJson = "null";

            if (user.isEnabledItem("menuWebpages")) {
                //data pre web stranku
                webpagesInitialJson = JsonTools.objectToJSON(WebpagesRestController.getAllItems(Constants.getInt("rootGroupId"), event.getSource().getRequest()));
            }

            model.addAttribute("webpagesInitialJson", webpagesInitialJson);

         } catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }
    }
}
```

Takto pripravené dáta získate v ```pug``` súbore nasledovne:

```javascript
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson
});
```

**POZOR**: všimnite si kontrolu práv volaním ```user.isEnabledItem("menuWebpages")```, dáta sú vkladané priamo do stránky a obchádzajú kontrolu práv REST služieb. Kontroly práv musíte teda zabezpečiť implicitne. Ako predvolená hodnota sa do objektu ```webpagesInitialJson``` vloží reťazec ```null```, keďže priamo do modelu nie je možné nastaviť ```null``` objekt. Reťazec sa ale pri vložení do pug súboru korektne vloží ako ```null``` hodnota, nepadne spracovanie HTML kódu a použije sa REST volanie (keďže hodnota ```initialData``` objektu bude ```null```).

## LayoutService

Trieda [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) zabezpečuje naplnenie základných objektov pre zobrazenie admin časti. Dôležité Java metódy:

```java
//Pridanie zoznamu jazykov do datatabuľky
DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
LayoutService ls = new LayoutService(getRequest());
page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
```

Dôležité JavaScript objekty:

```javascript
//jazyk nastavený pri prihlásení do admin časti
window.userLng

//aktualny CSRF token, automaticky je nastaveny cez jQuery.ajaxSetup do HTTP hlavicky X-CSRF-Token
window.csrfToken

//aktualne prihlaseny pouzivatel
window.currentUser

//verzia bootstrapu z konf. premennej bootstrapVersion
window.bootstrapVersion

//datum poslednej zmeny prekladovych textov
window.propertiesLastModified
```

## Základný výpis textu / atribútu

Text nie je možné vypísať priamo, je nutné ho dať do napr. ```span``` obaľovača s atribútom ```data-th-text```, čo nahradí obsah ```span``` elementu (aby sa dalo prototypovať).
Podobne sa nastavujú aj atribúty, čiže napr. ```data-th-href=...```

```html
<span data-th-text="${layout.user.fullName}">WebJET User</span>
```

Pre vloženie **HTML kódu (bez escapingu znakov)** je potrebné použiť atribút ```data-th-utext```.

v JavaScript kóde je možné hodnotu priradiť nasledovne:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.userLng = "[[${layout.lng}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.currentUser = [(${layout.userDto})];
```

## Prekladový text

Prekladový text sa zapisuje vo forme ```#{prekladovy.kluc}```. V pug súbore je ale potrebné ho **escapnuť cez \\** lebo # sa vykonáva priamo v pug súboroch:

```html
<span data-th-text="\#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[\#{menu.top.help}]]
```

Preklad je možné jednoducho vložiť aj do JS kódu v PUG súbore cez zápis:

```javascript
let tabs = [
    { id: 'basic', title: '[[\#{menu.top.help}]]', selected: true },
```

Ak nepoužívate PUG ale priamo píšete HTML šablónu nie je potrebné použiť spätnú lomku ```\#```, čiže použite:

```javascript
let tabs = [
    { id: 'basic', title: '[[#{menu.top.help}]]', selected: true },
```

ak potrebujete pridať nový prekladový kľúč pridajte ho do súboru [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties). Kľúče sú organizované podľa pug súborov. Po pridaní prekladového kľúča obnovte preklady zavolaním URL adresy [/admin/?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true). WebJET si obnoví obsah súboru ```text-webjet9.properties``` a načíta nové kľúče.

V Java kóde je možné získať objekt ```Prop.java``` ako:

```java
Prop.getInstance(request); //vrati prop objekt inicializovany na jazyk admin casti

String lng = Prop.getLng(request, false); //ziska jazyk aktualne zobrazenej web stranky (nie administracie)
Prop.getInstance(lng); //ziska prop objekt zadaneho jazyka
```

Poznámka: vkladanie textov z WebJET CMS do Spring zabezpečuje trieda `WebjetMessageSource.java`.

## Select box

```javascript
select(data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}")
    option(data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}")
```

## Spájanie reťazcov

```javascript
img(
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
   )
```
okrem toho je možné použiť aj ```Literal substitutions``` https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#literal-substitutions

```html
<span data-th-text="|Welcome to our application, ${layout.user.fullName}!|">
```

POZOR: ak vám vyhodí chybu typu: ```Could not parse as expression: "aitem--open md-large-menu"```, je to kvôli ```__```. To je špeciálna značka pre pre-procesor:

https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#preprocessing

a je potrebne to escapovať ako \\\\_, príklad:

```javascript
div(data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'")
```

## Podmienené zobrazenie tagu

```data-th-if``` zabezpečí zobrazenie ```tagu``` jedine pri splnení zadanej podmienky

```javascript
i(data-th-if="${!subMenuItem.childrens.empty}" class="fas fa-chevron-down")
```

## Získanie objektu z Constants

```javascript
a(data-th-href="${layout.getConstant('adminLogoffLink')}")

//priamo v PUG subore
dateAxis.tooltipDateFormat = "[(${layout.getConstant('dateTimeFormat')})]";
```

podporované sú nasledovné funkcie:

- ```String getConstant(String name)``` - vráti hodnotu konštanty podľa zadaného ```name```, ak neexistuje vráti "".
- ```String getConstant(String name, String defaultValue)``` - vráti hodnotu konštanty podľa zadaného ```name```, ak neexistuje vráti hodnotu zadanú v ```defaultValue```.
- ```int getConstantInt(String name)``` - vráti celo číselnú hodnotu konštanty podľa zadaného ```name```, ak neexistuje vráti 0.
- ```int getConstantInt(String name, int defaultValue)``` - vráti celo číselnú hodnotu konštanty podľa zadaného ```name```, ak neexistuje vráti hodnotu zadanú v ```defaultValue```.
- ```boolean getConstantBoolean(String name)``` - vráti binárnu hodnotu konštanty podľa zadaného ```name```, ak neexistuje vráti ```false```.
- ```boolean isPerexGroupsRenderAsSelect()``` - vráti binárnu hodnotu pre zobrazenie značiek (perex skupín) ako multi výberové pole namiesto zoznamu zaškrtávacích polí (ak existuje viac ako 30 značiek).

## Kontrola práv

Pri zobrazení stránky sa volá overenie práva metódou ```ThymeleafAdminController.checkPerms(String forward, String originalUrl, HttpServletRequest request)```. Mapovanie URL adresy na právo zabezpečuje ```MenuService.getPerms(String url)```. To prehľadá zoznam modulov/aplikácií v ktorých hľadá zadanú URL adresu. Ak ju nájde, získa z modulu právo.

Získané právo sa overí v prihlásenom používateľovi, ak právo nemá presmeruje sa na stránku ```/admin/403.jsp```.

## Overenie práva

Pre overenie práva je možné použiť volanie ```${layout.hasPermission('cmp_form')}```, v kombinácii s podmieneným zobrazením tagu pomocou ```data-th-if```:

```javascript
li(data-th-if="${layout.hasPermission('cmp_form')}")
    a.dropdown-item(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/components/crypto/admin/keymanagement')")
        i.far.fa-key
        <span data-th-text="\#{admin.keymanagement.title}" data-th-remove="tag">Sprava sifrovacich klucov</span>
```

## Chyba TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context

Thymeleaf z bezpečnostných dôvodov neumožňuje do ```onlick``` a podobných atribútov vkladať hodnotu objektu kvôli možnému XSS. Je potrebné hodnotu vložiť do data atribútu, kde ju Thymeleaf korektne escapne a tú následne použiť v kóde ako parameter.

```javascript
a.(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/admin/edituser.do?userid='+$(this).data('userid'))")
```
