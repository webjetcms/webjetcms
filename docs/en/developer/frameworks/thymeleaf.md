# Thymeleaf

[Thymeleaf](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#standard-expression-syntax) is a templating framework. In WebJET, it is bundled with Spring MVC.

## URL mapping

The mapping of the admin section URLs is done in [ThymeleafAdminController.java](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) as:

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

If ```subpage``` is not specified, the file ```index.pug``` is searched for. When calling /admin/v9/, the page ```dashboard/overview.pug``` is executed.

All default objects are inserted into the model via [LayoutBean.java](../../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutBean.java).

## Inserting custom objects into the model

If you need to insert your own object into the model for certain administration pages, you can use events. [ThymeleafAdminController.java](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafAdminController.java) publishes the event [ThymeleafEvent](../../../../src/main/java/sk/iway/iwcm/admin/ThymeleafEvent.java), which you can listen to. The ```ModelMap``` object and ```HttpServletRequest``` are published in the event.

An example of usage is [WebPagesListener](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java).

It is important to use the ```@Component``` annotation (which also requires adding a package in the [SpringConfig](../../../../src/main/java/sk/iway/webjet/v9/V9SpringConfig.java) class in the ```@ComponentScan``` annotation) and of course ```@EventListener```. In the conditions, it is necessary to set the value of the ```page``` and ```subpage``` parameters so that event listening is performed only for the specified administration page.

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

You will get the data prepared in this way in the ```pug``` file as follows:

```javascript
let webpagesInitialJson = [(${webpagesInitialJson})];
...
webpagesDatatable = WJ.DataTable({
    url: webpagesUrl,
    initialData: webpagesInitialJson
});
```

!>**Warning:** note the rights check by calling ```user.isEnabledItem("menuWebpages")```, the data is inserted directly into the page and bypasses the rights check of REST services. You must therefore provide rights checks implicitly. The string ```null``` is inserted into the ```webpagesInitialJson``` object as the default value, since it is not possible to set the ```null``` object directly in the model. However, when inserted into the pug file, the string is correctly inserted as the ```null``` value, the HTML code processing fails and the REST call is used (since the value of the ```initialData``` object will be ```null```).

## LayoutService

The [LayoutService] class (../../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) provides the basic objects for displaying the admin section. Important Java methods:

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

The text cannot be printed directly, it must be placed in, for example, a ```span``` wrapper with the ```data-th-text``` attribute, which will replace the content of the ```span``` element (so that it can be prototyped).
Attributes are set similarly, e.g. ```data-th-href=...```

```html
<span data-th-text="${layout.user.fullName}">WebJET User</span>
```

To insert **HTML code (without escaping characters)**, you need to use the ```data-th-utext``` attribute.

in JavaScript code, the value can be assigned as follows:

```javascript
    //standardne vlozenie s escapovanim HTML kodu (bezpecne)
    window.userLng = "[[${layout.lng}]]";
    //bez escapovanie HTML kodu/uvodzoviek (nebezpecne, pouzite len v opravnenych pripadoch)
    window.currentUser = [(${layout.userDto})];
```

## Translation text

The translation text is written in the form ```#{prekladovy.kluc}```. In the pug file, however, it needs to be **escaped with \\** because # is executed directly in the pug files:

```html
<span data-th-text="\#{menu.top.help}">Pomocník</span>
alebo priamo ako text:
[[\#{menu.top.help}]]
```

The translation can also be easily inserted into the JS code in the PUG file by writing:

```javascript
let tabs = [
    { id: 'basic', title: '[[\#{menu.top.help}]]', selected: true },
```

If you are not using PUG but are directly writing an HTML template, there is no need to use the backslash ```\#```, so use:

```javascript
let tabs = [
    { id: 'basic', title: '[[#{menu.top.help}]]', selected: true },
```

If you need to add a new translation key, add it to the file [text-webjet9.properties](../../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties). The keys are organized by pug files. After adding the translation key, refresh the translations by calling the URL [/admin/?userlngr=true](http://iwcm.interway.sk/admin/?userlngr=true). WebJET will refresh the contents of the file ```text-webjet9.properties``` and load the new keys.

In Java code, it is possible to get the object ```Prop.java``` as:

```java
Prop.getInstance(request); //vrati prop objekt inicializovany na jazyk admin casti

String lng = Prop.getLng(request, false); //ziska jazyk aktualne zobrazenej web stranky (nie administracie)
Prop.getInstance(lng); //ziska prop objekt zadaneho jazyka
```

Note: inserting texts from WebJET CMS into `Spring` is provided by the `WebjetMessageSource.java` class.

## Select box

```javascript
select(data-th-field="${layout.header.currentDomain}" onchange="WJ.changeDomain(this);" data-th-data-previous="${layout.header.currentDomain}")
    option(data-th-each="domain : ${layout.header.domains}" data-th-text="${domain}" data-th-value="${domain}")
```

## Connecting chains

```javascript
img(
    data-th-src="${'/admin/v9/dist/images/logo-' + layout.brand + '.png'}"
    data-th-title="\#{admin.top.webjet_version} + ${' '+layout.version}"
   )
```

in addition, it is also possible to use ```Literal substitutions``` https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#literal-substitutions

```html
<span data-th-text="|Welcome to our application, ${layout.user.fullName}!|">
```

!>**Warning:** if you get an error like: ```Could not parse as expression: "aitem--open md-large-menu"```, it's because of ```__```. That's a special pre-processor tag:

https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#preprocessing

and it is necessary to escape it as \\\\_, example:

```javascript
div(data-th-each="menuItem : ${layout.menu}" data-th-class="${menuItem.active} ? 'md-large-menu\\_\\_item--open md-large-menu\\_\\_item--active' : 'md-large-menu__item'")
```

## Conditional tag display

```data-th-if``` zabezpečí zobrazenie ```tagu``` jedine pri splnení zadanej podmienky

```javascript
i(data-th-if="${!subMenuItem.childrens.empty}" class="ti ti-chevron-down")
```

## Getting an object from `Constants`

```javascript
a(data-th-href="${layout.getConstant('adminLogoffLink')}")

//priamo v PUG subore
dateAxis.tooltipDateFormat = "[(${layout.getConstant('dateTimeFormat')})]";
```

The following functions are supported:

- ```String getConstant(String name)``` - ​​returns the value of the constant according to the specified ```name```, if it does not exist it returns "".
- ```String getConstant(String name, String defaultValue)``` - ​​returns the value of the constant according to the specified ```name```, if it does not exist it returns the value specified in ```defaultValue```.
- ```int getConstantInt(String name)``` - ​​returns the integer value of the constant according to the specified ```name```, if it does not exist it returns 0.
- ```int getConstantInt(String name, int defaultValue)``` - ​​returns the integer value of the constant according to the specified ```name```, if it does not exist it returns the value specified in ```defaultValue```.
- ```boolean getConstantBoolean(String name)``` - ​​returns the binary value of the constant according to the specified ```name```, if it does not exist it returns ```false```.
- ```boolean isPerexGroupsRenderAsSelect()``` - ​​returns a binary value to display tags (perex groups) as a multi-select field instead of a list of checkboxes (if there are more than 30 tags).

## Calling a static method

If you need to call more complex code, you can put it in a static method and then call it in the PUG file:

```java
public class MyUtils {
    public static String getMyValue(String param) {
        //complex code
        return value;
    }
}
```

and in the PUG file you call it as follows:

```javascript
span(data-th-text="${T(sk.iway.iwcm.utils.MyUtils).getMyValue('parameter')}")

<p>date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(demoComponent.date)}"></span></p>

<p class="currentDate">current date: <span data-th-text="${T(sk.iway.iwcm.Tools).formatDateTimeSeconds(T(sk.iway.iwcm.Tools).getNow())}"></span></p>
```

## Rights check

When the page is displayed, the right verification is called using the ```ThymeleafAdminController.checkPerms(String forward, String originalUrl, HttpServletRequest request)``` method. The mapping of the URL address to the right is provided by ```MenuService.getPerms(String url)```. This scans the list of modules/applications in which it searches for the specified URL address. If it finds it, it obtains the right from the module.

The acquired right is verified in the logged-in user, if the right is not available, the user is redirected to the ```/admin/403.jsp``` page.

## Verification of the right

To verify the right, you can use the call ```${layout.hasPermission('cmp_form')}```, in combination with conditional display of the tag using ```data-th-if```:

```javascript
li(data-th-if="${layout.hasPermission('cmp_form')}")
    a.dropdown-item(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/components/crypto/admin/keymanagement')")
        i.ti.ti-key
        <span data-th-text="\#{admin.keymanagement.title}" data-th-remove="tag">Sprava sifrovacich klucov</span>
```

## Error TemplateProcessingException: Only variable expressions returning numbers or booleans are allowed in this context

For security reasons, Thymeleaf does not allow you to insert an object value into ```onlick``` and similar attributes due to possible XSS. You need to insert the value into the data attribute, where Thymeleaf will escape it correctly and then use it as a parameter in your code.

```javascript
a.(data-th-data-userid="${layout.user.userId}" onclick="WJ.openPopupDialog('/admin/edituser.do?userid='+$(this).data('userid'))")
```
