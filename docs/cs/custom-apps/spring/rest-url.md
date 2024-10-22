# Poskytování služeb REST

## Pravidla URL

Při přípravě služeb REST dodržujte následující předpony URL:
- `/rest` - veřejně dostupná služba REST
- `/rest/private` nebo `/private/rest` - klidová služba, vyžadující přihlášení
- `/admin/rest` - klidová služba vyžadující přihlášení správce

Pro `websocket` je nutné použít předponu `/websocket/` jinak by nemusely správně procházet web. `firewall`. Doporučujeme používat následující předpony:
- `/websocket/` - veřejně dostupné `websocket`
- `/websocket/private/` - `websocket` vyžadující přihlášení
- `/websocket/admin/` - `websocket` vyžadující přihlášení správce

## Kontrola práv

Kontrolu oprávnění k jednotlivým službám/metodám REST je třeba provádět pomocí anotace:

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

Předpony URL a anotace jsou **povinné** zajistit, aby systém **dvojitá kontrola**.

## Token CSRF

Při volání služeb správy REST `/admin/rest/*` Je vyžadováno nastavení tokenu CSRF. To je [k dispozici ve standardní výbavě](../../developer/frameworks/thymeleaf.md#layoutservice) v objektu `window.csrfToke` a je automaticky nastaven pro všechna volání prostřednictvím knihovny `jQuery` v `app.js`:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

Výjimkou jsou volání obsahující výraz v adrese URL. `/html` nebo `html/` kde se očekává vrácení kódu HTML namísto objektu JSON.
