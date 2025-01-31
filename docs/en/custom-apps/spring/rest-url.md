# REST service provisioning

## URL rules

When preparing REST services, please observe the following URL prefixes:
- `/rest` - publicly available REST service
- `/rest/private` or `/private/rest` - rest service, requiring login
- `/admin/rest` - rest service requiring administrator login

For `websocket` it is necessary to use the prefix `/websocket/` otherwise they may not browse the web correctly `firewall`. We recommend using the following prefixes:
- `/websocket/` - publicly available `websocket`
- `/websocket/private/` - `websocket` requiring a login
- `/websocket/admin/` - `websocket` requiring administrator login

## Control of rights

Checking the permissions on individual REST services/methods needs to be done using annotation:

```java
//prihalseny pouzivatel
@PreAuthorize("@WebjetSecurityService.isLogged()")
//admin
@PreAuthorize("@WebjetSecurityService.isAdmin()")
//patri do skupiny pouzivatelov
@PreAuthorize("@WebjetSecurityService.isInUserGroup('nazov-skupiny')")
//ma pravo na modul editDir ALEBO addSubdir
@PreAuthorize("@WebjetSecurityService.hasPermission('editDir|addSubdir')")
//ma pravo na modul cmp_stat A ZAROVEN menuUsers
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat&menuUsers')")
```

URL prefixes and annotations are **mandatory** to ensure that the system **double check**.

## CSRF token

When calling REST administration services `/admin/rest/*` CSRF token setup is required. This is [available as standard](../../developer/frameworks/thymeleaf.md#layoutservice) in the object `window.csrfToke` and is automatically set for all calls through the library `jQuery` v `app.js`:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

Exceptions are calls containing an expression in the URL `/html` or `html/` where it is assumed to return HTML code instead of a JSON object.

By setting the configuration variable `logoffRequireCsrfToken` to the value of `true` it is possible to activate the CSRF token requirement for user logout (from both administration and customer area).

### Inserting a token in a web page

The CSRF token can be embedded in the text of a web page using a macro `!CSRF_INPUT!` which inserts the complete HTML field, or by using `!CSRF_TOKEN!` which will insert the value of the CSRF token.

```html
<form action="/logoff.do?forward=/apps/prihlaseny-pouzivatel/zakaznicka-zona/csrf-logoff.html" method="post">
    <button class="btn btn-primary" id="logoffButtonInput" type="submit">Logoff</button>
    !CSRF_INPUT!
</form>

<form action="/logoff.do?forward=/apps/prihlaseny-pouzivatel/zakaznicka-zona/" method="post" name="userLogoffForm">
    <button class="btn btn-primary" id="logoffButtonToken" type="submit">Logoff</button>
    <input name="__token" type="hidden" value="!CSRF_TOKEN!" />
</form>
```

Alternatively, you can use the class API directly [CSRF](../../../../src/webjet8/java/sk/iway/iwcm/system/stripes/CSRF.java) for generating the value `public static String getCsrfToken(HttpSession session, boolean saveToSession)` or the entire HTML field `public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)`. To add HTTP headers for all AJAX calls via `jQuery` you can use the following code:

```html
<script>
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': '<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(session, true)%>',
    }
});
</script>
```

### URL protection

CSRF protection can be activated on any URL, for example `/private/rest` by setting the configuration variable `csrfRequiredUrls` in which you enter on a new line the beginnings of the URLs for which CSRF protection should be required. A format with characters is also supported `%` for contains and `!` for ends on. Example:

```
/private/rest
%/rest/
%/export.pdf!
```
