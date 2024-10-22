# Thymeleaf

[Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#messages) je šablonovací framework. Ve WebJETu je propojen se Spring MVC.

<!-- @import "[TOC]" {cmd="toc" depthFrom=2 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->
- [Thymeleaf](#thymeleaf)
  - [Mapování adres URL](#mapování-url)
  - [Vkládání vlastních objektů do modelu](#vkládání-vlastních-objektů-do-modelu)
  - [LayoutService](#layoutservice)
  - [Základní výpis textu/atributů](#základní-výpis-textových-atributů)
  - [Text překladu](#text-překladu)
  - [Výběrové pole](#výběrové-pole)
  - [Propojení řetězců](#propojovací-řetězce)
  - [Podmíněné zobrazení značky](#podmíněné-zobrazení-tagů)
  - [Získání objektu z `Constants`](#získání-objektu-z-konstant)
  - [Kontrola práv](#kontrola-práv)
  - [Ověřování práva](#ověření-práva)
  - [Chyba TemplateProcessingException: V tomto kontextu jsou povoleny pouze proměnné výrazy vracející čísla nebo booleany.](#error-templateprocessingexception-v-tomto-kontextu-jsou-povoleny-pouze-proměnné-výrazy-vracející-čísla-nebo-booleany)

<!-- /code_chunk_output -->

## Mapování adres URL

Mapování adres URL části správce se provádí v části [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) Stejně jako:

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

přičemž pokud `subpage` není zadán hledaný soubor `index.pug`. Při volání /admin/v9/ se stránka `dashboard/overview.pug`.

Všechny výchozí objekty jsou do modelu vloženy prostřednictvím [LayoutBean.java](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutBean.java).

## Vkládání vlastních objektů do modelu

Pokud potřebujete do modelu vložit vlastní objekt pro určité stránky správy, můžete použít události. [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) zveřejní událost [ThymeleafEvent](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafEvent.java) které je možné poslouchat. V případě, že je zveřejněna `ModelMap` objekt a `HttpServletRequest`.

Příkladem použití je [WebPagesListener](../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java).

Je důležité používat anotace `@Component` (což také vyžaduje přidání balíčku ve třídě [SpringConfig](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java) v anotaci `@ComponentScan`) a samozřejmě `@EventListener`. V podmínkách je nutné nastavit hodnotu parametru `page` a `subpage` aby se naslouchání událostem provádělo pouze pro zadanou stránku správy.

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

Takto připravená data můžete získat v aplikaci `pug` takto:

```javascript
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson
});
```

**Varování:** upozornění na kontrolu práv na volání `user.isEnabledItem("menuWebpages")`, jsou data vložena přímo do stránky a obejdou kontrolu práv služby REST. Kontrolu práv tedy musíte zajistit implicitně. Ve výchozím nastavení je objekt `webpagesInitialJson` vloží řetězec `null`, protože model není možné nastavit přímo. `null` objekt. Řetězec je však správně vložen do souboru pug jako `null` hodnota, zpracování HTML kódu selže a použije se volání REST (protože hodnota `initialData` objektu bude `null`).

## LayoutService

Třída [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) zajistí, aby byly naplněny základní objekty pro zobrazení části správce. Důležité metody jazyka Java:

```java
//Pridanie zoznamu jazykov do datatabuľky
DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
LayoutService ls = new LayoutService(getRequest());
page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
```

Důležité objekty JavaScriptu:

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

## Základní výpis textu/atributů

Text nelze vypsat přímo, musí se vložit např. do. `span` obalů s atributem `data-th-text` který nahradí obsah `span` prvek (bude vytvořen prototyp). Podobně se nastavují atributy, např. `data-th-href=...`

```html
<span data-th-text="${layout.user.fullName}">WebJET User</span>
```

Pro vložení **Kód HTML (bez escapování znaků)** atribut musí být použit `data-th-utext`.

v kódu JavaScriptu lze hodnotu přiřadit následujícím způsobem:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.userLng = "[[${layout.lng}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.currentUser = [(${layout.userDto})];
```

## Text překladu

Text překladu se píše ve tvaru `#{prekladovy.kluc}`. Ale v souboru pug to musí být. **útěk přes \\** protože # se provádí přímo v souborech pug:

```html
<span data-th-text="\#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[\#{menu.top.help}]]
```

Překlad lze také snadno vložit do kódu JS v souboru PUG prostřednictvím zápisu:

```javascript
let tabs = [
    { id: 'basic', title: '[[\#{menu.top.help}]]', selected: true },
```

Pokud nepoužíváte PUG, ale přímo HTML šablonu, není třeba používat zpětné lomítko. `\#`, tedy používat:

```javascript
let tabs = [
    { id: 'basic', title: '[[#{menu.top.help}]]', selected: true },
```

pokud potřebujete přidat nový překladový klíč, přidejte jej do souboru. [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties). Klíče jsou uspořádány podle souborů pug. Po přidání překladového klíče obnovte překlady voláním adresy URL [/admin/?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true). WebJET obnoví obsah souboru `text-webjet9.properties` a načte nové klíče.

V kódu Javy je možné získat objekt `Prop.java` Stejně jako:

```java
Prop.getInstance(request); //vrati prop objekt inicializovany na jazyk admin casti

String lng = Prop.getLng(request, false); //ziska jazyk aktualne zobrazenej web stranky (nie administracie)
Prop.getInstance(lng); //ziska prop objekt zadaneho jazyka
```

Poznámka: vkládání textů z WebJET CMS do `Spring` poskytované třídou `WebjetMessageSource.java`.

## Výběrové pole

```javascript
select(data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}")
    option(data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}")
```

## Propojení řetězců

```javascript
img(
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
   )
```

kromě toho je možné použít také `Literal substitutions` https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#literal-substitutions

```html
<span data-th-text="|Welcome to our application, ${layout.user.fullName}!|">
```

**Varování:** pokud vyhodí chybu jako: `Could not parse as expression: "aitem--open md-large-menu"`, je to kvůli `__`. Jedná se o speciální značku pro preprocesor:

https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#preprocessing

a je třeba jej escapovat jako \\\\\_, příklad:

```javascript
div(data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'")
```

## Podmíněné zobrazení značky

`data-th-if` zajistit zobrazení `tagu` pouze v případě, že je splněna zadaná podmínka

```javascript
i(data-th-if="${!subMenuItem.childrens.empty}" class="ti ti-chevron-down")
```

## Získání objektu z `Constants`

```javascript
a(data-th-href="${layout.getConstant('adminLogoffLink')}")

//priamo v PUG subore
dateAxis.tooltipDateFormat = "[(${layout.getConstant('dateTimeFormat')})]";
```

jsou podporovány následující funkce:
- `String getConstant(String name)` - vrátí hodnotu konstanty podle zadaného parametru `name`, pokud neexistuje, vrátí "".
- `String getConstant(String name, String defaultValue)` - vrátí hodnotu konstanty podle zadaného parametru `name`, pokud žádná, vrátí hodnotu zadanou v položce `defaultValue`.
- `int getConstantInt(String name)` - vrátí celočíselnou hodnotu konstanty podle zadaného parametru `name`, pokud není, vrátí se 0.
- `int getConstantInt(String name, int defaultValue)` - vrátí celočíselnou hodnotu konstanty podle zadaného parametru `name`, pokud žádná, vrátí hodnotu zadanou v položce `defaultValue`.
- `boolean getConstantBoolean(String name)` - vrátí binární hodnotu konstanty podle zadaného parametru `name` pokud není žádný návrat `false`.
- `boolean isPerexGroupsRenderAsSelect()` - vrací binární hodnotu pro zobrazení tagů (skupin perexů) jako pole s více možnostmi výběru místo seznamu zaškrtávacích políček (pokud je více než 30 tagů).

## Kontrola práv

Když je stránka zobrazena, nazývá se metoda správného ověření. `ThymeleafAdminController.checkPerms(String forward, String originalUrl, HttpServletRequest request)`. Mapování URL vpravo poskytuje `MenuService.getPerms(String url)`. Prohledá seznam modulů/aplikací a vyhledá zadanou adresu URL. Pokud ji najde, získá z modulu právo.

Získané právo se ověřuje u přihlášeného uživatele, pokud právo není k dispozici, dojde k přesměrování na stránku. `/admin/403.jsp`.

## Ověřování práva

Pro ověření práva můžete použít volání `${layout.hasPermission('cmp_form')}`, v kombinaci s podmíněným zobrazením značky pomocí `data-th-if`:

```javascript
li(data-th-if="${layout.hasPermission('cmp_form')}")
    a.dropdown-item(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/components/crypto/admin/keymanagement')")
        i.ti.ti-key
        <span data-th-text="\#{admin.keymanagement.title}" data-th-remove="tag">Sprava sifrovacich klucov</span>
```

## Chyba TemplateProcessingException: V tomto kontextu jsou povoleny pouze proměnné výrazy vracející čísla nebo booleany.

Společnost Thymeleaf z bezpečnostních důvodů neumožňuje. `onlick` a podobné atributy pro vložení hodnoty objektu z důvodu možného XSS. Hodnotu je nutné vložit do atributu data, kde ji Thymeleaf správně escapuje, a poté ji použít v kódu jako parametr.

```javascript
a.(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/admin/edituser.do?userid='+$(this).data('userid'))")
```
