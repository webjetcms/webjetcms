# Thymeleaf

[Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#messages) is a templating framework. In WebJET it is connected with Spring MVC.

## URL mapping

The mapping of the URLs of the admin part is done in [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) Like:

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

whereby if `subpage` not specified a file is being searched for `index.pug`. When /admin/v9/ is called, the page `dashboard/overview.pug`.

All default objects are inserted into the model via [LayoutBean.java](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutBean.java).

## Inserting custom objects into the model

If you need to insert a custom object into the model for certain administration pages, you can use events. [ThymeleafAdminController.java](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) publishes the event [ThymeleafEvent](../../../src/main/java/sk/iway/iwcm/admin/ThymeleafEvent.java) to which it is possible to listen. In the event is published `ModelMap` object and `HttpServletRequest`.

An example of use is [WebPagesListener](../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java).

It is important to use annotation `@Component` (which also requires adding a package in the class [SpringConfig](../../../src/main/java/sk/iway/webjet/v9/SpringConfig.java) in the annotation `@ComponentScan`) and of course `@EventListener`. In the conditions it is necessary to set the value of the parameter `page` a `subpage` so that event listening is done only for the specified administration page.

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

You can get the data prepared in this way in `pug` file as follows:

```javascript
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson
});
```

!>**Warning:** note the checking of calling rights `user.isEnabledItem("menuWebpages")`, the data is inserted directly into the page and bypasses the REST services rights check. So you must provide rights checks implicitly. By default, the object `webpagesInitialJson` inserts a string `null`, as it is not possible to set the model directly `null` object. But the string is correctly inserted into the pug file as `null` value, HTML code processing fails and the REST call is used (since the value `initialData` of the object will be `null`).

## LayoutService

Class [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) ensures that basic objects are populated to display the admin part. Important Java methods:

```java
//Pridanie zoznamu jazykov do datatabuľky
DatatablePageImpl<TranslationKeyEntity> page = new DatatablePageImpl<>(translationKeyService.getTranslationKeys(getRequest(), pageable));
LayoutService ls = new LayoutService(getRequest());
page.addOptions("lng", ls.getLanguages(false, true), "label", "value", false);
```

Important JavaScript objects:

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

## Basic text/attribute listing

The text cannot be typed out directly, it must be put into e.g. `span` wrappers with attribute `data-th-text` which will replace the content `span` element (to be prototyped). Similarly, attributes are set, e.g. `data-th-href=...`

```html
<span data-th-text="${layout.user.fullName}">WebJET User</span>
```

For insertion **HTML code (without escaping characters)** the attribute must be used `data-th-utext`.

in JavaScript code, the value can be assigned as follows:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.userLng = "[[${layout.lng}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.currentUser = [(${layout.userDto})];
```

## Translation text

The translation text shall be written in the form `#{prekladovy.kluc}`. But in the pug file it needs to be **escape through \\** because # is done directly in pug files:

```html
<span data-th-text="\#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[\#{menu.top.help}]]
```

The translation can also be easily inserted into the JS code in the PUG file via write:

```javascript
let tabs = [
    { id: 'basic', title: '[[\#{menu.top.help}]]', selected: true },
```

If you don't use PUG but directly write HTML template there is no need to use backslash `\#`, that is, use:

```javascript
let tabs = [
    { id: 'basic', title: '[[#{menu.top.help}]]', selected: true },
```

if you need to add a new translation key, add it to the file [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties). Keys are organized by pug files. After adding a translation key, refresh the translations by calling the URL [/admin/?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true). WebJET restores the contents of the file `text-webjet9.properties` and loads the new keys.

In Java code it is possible to get an object `Prop.java` Like:

```java
Prop.getInstance(request); //vrati prop objekt inicializovany na jazyk admin casti

String lng = Prop.getLng(request, false); //ziska jazyk aktualne zobrazenej web stranky (nie administracie)
Prop.getInstance(lng); //ziska prop objekt zadaneho jazyka
```

Note: inserting texts from WebJET CMS into `Spring` provided by the class `WebjetMessageSource.java`.

## Select box

```javascript
select(data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}")
    option(data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}")
```

## Linking strings

```javascript
img(
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
   )
```

in addition, it is also possible to use `Literal substitutions` https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#literal-substitutions

```html
<span data-th-text="|Welcome to our application, ${layout.user.fullName}!|">
```

!>**Warning:** if it throws an error like: `Could not parse as expression: "aitem--open md-large-menu"`, it's because of `__`. This is a special brand for the pre-processor:

https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#preprocessing

and it needs to be escaped as \\\\\_, example:

```javascript
div(data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'")
```

## Conditional tag display

`data-th-if` ensure the display of `tagu` only when the specified condition is met

```javascript
i(data-th-if="${!subMenuItem.childrens.empty}" class="ti ti-chevron-down")
```

## Obtaining an object from `Constants`

```javascript
a(data-th-href="${layout.getConstant('adminLogoffLink')}")

//priamo v PUG subore
dateAxis.tooltipDateFormat = "[(${layout.getConstant('dateTimeFormat')})]";
```

the following functions are supported:
- `String getConstant(String name)` - returns the value of the constant according to the specified `name`, if it does not exist returns "".
- `String getConstant(String name, String defaultValue)` - returns the value of the constant according to the specified `name`, if none returns the value specified in `defaultValue`.
- `int getConstantInt(String name)` - returns the integer value of the constant according to the specified `name`, if none returns 0.
- `int getConstantInt(String name, int defaultValue)` - returns the integer value of the constant according to the specified `name`, if none returns the value specified in `defaultValue`.
- `boolean getConstantBoolean(String name)` - returns the binary value of the constant according to the specified `name` if there is no return `false`.
- `boolean isPerexGroupsRenderAsSelect()` - returns a binary value for displaying tags (perex groups) as a multi-select field instead of a list of checkboxes (if there are more than 30 tags).

## Control of rights

When the page is displayed, it is called the right verification method `ThymeleafAdminController.checkPerms(String forward, String originalUrl, HttpServletRequest request)`. URL mapping to the right provides `MenuService.getPerms(String url)`. It scans the list of modules/applications looking for the specified URL. If it finds it, it gets the right from the module.

The acquired right is verified in the logged in user, if the right is not available it redirects to the page `/admin/403.jsp`.

## Verification of law

To verify the right, you can use the call `${layout.hasPermission('cmp_form')}`, combined with conditional tag display using `data-th-if`:

```javascript
li(data-th-if="${layout.hasPermission('cmp_form')}")
    a.dropdown-item(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/components/crypto/admin/keymanagement')")
        i.ti.ti-key
        <span data-th-text="\#{admin.keymanagement.title}" data-th-remove="tag">Sprava sifrovacich klucov</span>
```

## Chyba TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context

For security reasons, Thymeleaf does not allow the `onlick` and similar attributes to insert the value of the object because of possible XSS. It is necessary to insert the value into the data attribute, where Thymeleaf correctly escapes it and then use it in the code as a parameter.

```javascript
a.(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/admin/edituser.do?userid='+$(this).data('userid'))")
```
