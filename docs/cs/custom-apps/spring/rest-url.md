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

Nastavením konfigurační proměnné `logoffRequireCsrfToken` na hodnotu `true` je možné aktivovat požadavek na CSRF token pro odhlášení uživatele (z administrační i zákaznické oblasti).

### Vložení tokenu na webovou stránku

Token CSRF lze vložit do textu webové stránky pomocí makra. `!CSRF_INPUT!` který vloží celé pole HTML, nebo pomocí příkazu `!CSRF_TOKEN!` který vloží hodnotu tokenu CSRF.

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

Případně můžete použít přímo rozhraní API třídy [CSRF](../../../../src/webjet8/java/sk/iway/iwcm/system/stripes/CSRF.java) pro generování hodnoty `public static String getCsrfToken(HttpSession session, boolean saveToSession)` nebo celé pole HTML `public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)`. Přidání hlaviček HTTP pro všechna volání AJAX prostřednictvím `jQuery` můžete použít následující kód:

```html
<script>
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': '<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(session, true)%>',
    }
});
</script>
```

### Ochrana URL

Ochranu CSRF lze aktivovat na libovolné adrese URL, např. `/private/rest` nastavením konfigurační proměnné `csrfRequiredUrls` do kterého na novém řádku zadáte začátky adres URL, pro které by měla být vyžadována ochrana CSRF. Podporován je také formát se znaky `%` obsahuje a `!` pro konce na. Příklad:

```
/private/rest
%/rest/
%/export.pdf!
```
