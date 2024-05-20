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

URL prefixes and annotations are **mandatory**to ensure that the system **double check**.

## CSRF token

When calling REST administration services `/admin/rest/*` CSRF token setup is required. This is [available as standard](../../developer/frameworks/thymeleaf.md#layoutservice) in the object `window.csrfToke` and is automatically set for all calls through the library `jQuery` v `app.js`:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

Exceptions are calls containing an expression in the URL `/html` or `html/`where it is assumed to return HTML code instead of a JSON object.
