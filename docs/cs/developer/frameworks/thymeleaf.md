# Thymeleaf

[Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#messages) je šablonovací framework. Ve WebJETu je spojen se Spring MVC.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Thymeleaf](#thymeleaf)
  - [Mapování URL](#mapování-url)
  - [Vložení vlastních objektů do modelu](#vložení-vlastních-objektů-do-modelu)
  - [LayoutService](#layoutservice)
  - [Základní výpis textu / atributu](#základní-výpis-textu--atributu)
  - [Překladový text](#překladový-text)
  - [Select box](#select-box)
  - [Spojování řetězců](#spojování-řetězců)
  - [Podmíněné zobrazení tagu](#podmíněné-zobrazení-tagu)
  - [Získání objektu z `Constants`](#získání-objektu-z-constants)
  - [Kontrola práv](#kontrola-práv)
  - [Ověření práva](#ověření-práva)
  - [Chyba TemplateProcessingException: Only variable expressions returning numbers or booleans allowed in this context](#chyba-templateprocessingexception-only-variable-expressions-returning-numbers-or-booleans-allowed-in-this-context)

<!-- /code_chunk_output -->

## Mapování URL

Mapování URL adres admin části se provádí v [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) jako:

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

přičemž pokud `subpage` není zadané hledá se soubor `index.pug`. Při volání /admin/v9/ se provede stránka `dashboard/overview.pug`.

Všechny výchozí objekty se vkládají do modelu přes [LayoutBean.java](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutBean.java).

## Vložení vlastních objektů do modelu

Pokud potřebujete pro určité stránky administrace vložit vlastní objekt do modelu, lze využít události. [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) publikuje událost [ThymeleafEvent](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafEvent.java), na kterou lze poslouchat. V události je publikován `ModelMap` objekt i `HttpServletRequest`.

Příkladem použití je [WebPagesListener](../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java).

Důležité je použít anotaci `@Component` (což vyžaduje i přidání package ve třídě [SpringConfig](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java) v anotaci `@ComponentScan`) a samozřejmě `@EventListener`. V podmínkách je třeba nastavit hodnotu parametru `page` a `subpage`, aby poslech události byl proveden pouze pro zadanou stránku administrace.

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

Takto připravená data získáte v `pug` souboru následovně:

```javascript
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson
});
```

!>**Upozornění:** všimněte si kontrolu práv voláním `user.isEnabledItem("menuWebpages")`, data jsou vkládána přímo do stránky a obcházejí kontrolu práv REST služeb. Kontroly práv musíte tedy zajistit implicitně. Jako výchozí hodnota se do objektu `webpagesInitialJson` vloží řetězec `null`, protože přímo do modelu nelze nastavit `null` objekt. Řetězec se ale při vložení do pug souboru korektně vloží jako `null` hodnota, nepadne zpracování HTML kódu a použije se REST volání (jelikož hodnota `initialData` objektu bude `null`).

## LayoutService

Třída [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) zajišťuje naplnění základních objektů pro zobrazení admin části. Důležité Java metody:

```java
//Pridanie zoznamu jazykov do datatabuľky
DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
LayoutService ls = new LayoutService(getRequest());
page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
```

Důležité JavaScript objekty:

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

## Základní výpis textu / atributu

Text nelze vypsat přímo, je nutné jej dát do např. `span` obalovače s atributem `data-th-text`, co nahradí obsah `span` elementu (aby se dalo prototypovat). Podobně se nastavují i atributy, tzn. `data-th-href=...`

```html
<span data-th-text="${layout.user.fullName}">WebJET User</span>
```

Pro vložení **HTML kódu (bez escapingu znaků)** je třeba použít atribut `data-th-utext`.

v JavaScript kódu lze hodnotu přiřadit následovně:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.userLng = "[[${layout.lng}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.currentUser = [(${layout.userDto})];
```

## Překladový text

Překladový text se zapisuje ve formě `#{prekladovy.kluc}`. V pug souboru je ale třeba jej **escapnout přes \\** neboť # se provádí přímo v pug souborech:

```html
<span data-th-text="\#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[\#{menu.top.help}]]
```

Překlad lze jednoduše vložit i do JS kódu v PUG souboru přes zápis:

```javascript
let tabs = [
    { id: 'basic', title: '[[\#{menu.top.help}]]', selected: true },
```

Pokud nepoužíváte PUG ale přímo píšete HTML šablonu není třeba použít zpětný lomítko `\#`, čili použijte:

```javascript
let tabs = [
    { id: 'basic', title: '[[#{menu.top.help}]]', selected: true },
```

pokud potřebujete přidat nový překladový klíč přidejte jej do souboru [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties). Klíče jsou organizovány podle pug souborů. Po přidání překladového klíče obnovte překlady zavoláním URL adresy [/admin/?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true). WebJET si obnoví obsah souboru `text-webjet9.properties` a načte nové klíče.

V Java kódu lze získat objekt `Prop.java` jako:

```java
Prop.getInstance(request); //vrati prop objekt inicializovany na jazyk admin casti

String lng = Prop.getLng(request, false); //ziska jazyk aktualne zobrazenej web stranky (nie administracie)
Prop.getInstance(lng); //ziska prop objekt zadaneho jazyka
```

Poznámka: vkládání textů z WebJET CMS do `Spring` zajišťuje třída `WebjetMessageSource.java`.

## Select box

```javascript
select(data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}")
    option(data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}")
```

## Spojování řetězců

```javascript
img(
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
   )
```

kromě toho lze použít i `Literal substitutions` https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#literal-substitutions

```html
<span data-th-text="|Welcome to our application, ${layout.user.fullName}!|">
```

!>**Upozornění:** pokud vám vyhodí chybu typu: `Could not parse as expression: "aitem--open md-large-menu"`, je to kvůli `__`. To je speciální značka pro pre-procesor:

https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#preprocessing

a je třeba to escapovat jako \\\\\_, příklad:

```javascript
div(data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'")
```

## Podmíněné zobrazení tagu

`data-th-if` zajistí zobrazení `tagu` jedině při splnění zadané podmínky

```javascript
i(data-th-if="${!subMenuItem.childrens.empty}" class="ti ti-chevron-down")
```

## Získání objektu z `Constants`

```javascript
a(data-th-href="${layout.getConstant('adminLogoffLink')}")

//priamo v PUG subore
dateAxis.tooltipDateFormat = "[(${layout.getConstant('dateTimeFormat')})]";
```

podporovány jsou následující funkce:
- `String getConstant(String name)` - vrátí hodnotu konstanty podle zadaného `name`, pokud neexistuje vrátí "".
- `String getConstant(String name, String defaultValue)` - vrátí hodnotu konstanty podle zadaného `name`, pokud neexistuje vrátí hodnotu zadanou v `defaultValue`.
- `int getConstantInt(String name)` - vrátí celo číselnou hodnotu konstanty podle zadaného `name`, pokud neexistuje vrátí 0.
- `int getConstantInt(String name, int defaultValue)` - vrátí celo číselnou hodnotu konstanty podle zadaného `name`, pokud neexistuje vrátí hodnotu zadanou v `defaultValue`.
- `boolean getConstantBoolean(String name)` - vrátí binární hodnotu konstanty podle zadaného `name`, pokud neexistuje vrátí `false`.
- `boolean isPerexGroupsRenderAsSelect()` - vrátí binární hodnotu pro zobrazení značek (perex skupin) jako multi výběrové pole namísto seznamu zaškrtávacích polí (pokud existuje více než 30 značek).

## Kontrola práv

Při zobrazení stránky se jmenuje ověření práva metodou `ThymeleafAdminController.checkPerms(String forward, String originalUrl, HttpServletRequest request)`. Mapování URL adresy na právo zajišťuje `MenuService.getPerms(String url)`. To prohledá seznam modulů/aplikací ve kterých hledá zadanou URL adresu. Pokud ji najde, získá z modulu právo.

Získané právo se ověří v přihlášeném uživateli, pokud právo nemá přesměruje se na stránku `/admin/403.jsp`.

## Ověření práva

Pro ověření práva je možné použít volání `${layout.hasPermission('cmp_form')}`, v kombinaci s podmíněným zobrazením tagu pomocí `data-th-if`:

```javascript
li(data-th-if="${layout.hasPermission('cmp_form')}")
    a.dropdown-item(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/components/crypto/admin/keymanagement')")
        i.ti.ti-key
        <span data-th-text="\#{admin.keymanagement.title}" data-th-remove="tag">Sprava sifrovacich klucov</span>
```

## Chyba TemplateProcessingException: Only variable expressions returning numbers or booleans allowed in this context

Thymeleaf z bezpečnostních důvodů neumožňuje do `onlick` a podobných atributů vkládat hodnotu objektu kvůli možnému XSS. Je třeba hodnotu vložit do data atributu, kde ji Thymeleaf korektně escapne a tu následně použít v kódu jako parametr.

```javascript
a.(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/admin/edituser.do?userid='+$(this).data('userid'))")
```
