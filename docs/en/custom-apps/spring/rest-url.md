# Securing REST services

## URL rules

When preparing REST services, please adhere to the following URL prefixes:

- ```/rest``` - ​​publicly available REST service
- ```/rest/private``` or ```/private/rest``` - rest service requiring login
- ```/admin/rest``` - ​​rest service requiring administrator login

For ```websocket``` it is necessary to use the prefix ```/websocket/``` otherwise they may not correctly navigate through the ```firewall``` website. We recommend using the following prefixes:

- ```/websocket/``` - ​​publicly available ```websocket```
- ```/websocket/private/``` - ​​```websocket``` requiring login
- ```/websocket/admin/``` - ​​```websocket``` requiring administrator login

## Rights check

Checking the rights on individual REST services/methods needs to be done using the annotation:

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

URL prefixes and annotations are **required** to ensure **double checking** in the system.

## CSRF token

When calling the REST administration services ```/admin/rest/*```, a CSRF token is required. It is [available by default](../../developer/frameworks/thymeleaf.md#layoutservice) in the ```window.csrfToke``` object and is automatically set for all calls via the ```jQuery``` library in ```app.js```:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

An exception is calls containing the expression ```/html``` or ```html/``` in the URL address, where HTML code is expected to be returned instead of a JSON object.

By setting the configuration variable `logoffRequireCsrfToken` to the value `true`, it is possible to activate the requirement of a CSRF token for user logout (from both the administration and customer zones).

### Inserting a token in a web page

The CSRF token can be inserted into the text of a web page using the macro `!CSRF_INPUT!`, which inserts a complete HTML field, or using `!CSRF_TOKEN!`, which inserts the value of the CSRF token.

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

Alternatively, you can directly use the [CSRF] class API (../../../../src/main/java/sk/iway/iwcm/system/stripes/CSRF.java) to generate the value `public static String getCsrfToken(HttpSession session, boolean saveToSession)` or the entire HTML field `public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)`. To add an HTTP header for all AJAX calls via `jQuery`, you can use the following code:

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

CSRF protection can be activated for any URL, for example `/private/rest` by setting the configuration variable `csrfRequiredUrls` in which you enter on a new line the beginnings of the URLs for which CSRF protection should be required. The format with the characters `%` for contains and `!` for ends with is also supported. Example:

```
/private/rest
%/rest/
%/export.pdf!
```
