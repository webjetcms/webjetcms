# Zabezpečení REST služeb

## Pravidla URL adres

Při přípravě REST služeb prosím dodržujte následovně URL prefixy:
- `/rest` - veřejně dostupná REST služba
- `/rest/private` nebo `/private/rest` - rest služba, vyžadující přihlášení
- `/admin/rest` - rest služba vyžadující přihlášení administrátora

Pro `websocket` je třeba používat prefix `/websocket/` jinak nemusí korektně procházet přes web `firewall`. Doporučujeme používat následující prefixy:
- `/websocket/` - veřejně dostupný `websocket`
- `/websocket/private/` - `websocket` vyžadující přihlášení
- `/websocket/admin/` - `websocket` vyžadující přihlášení administrátora

## Kontrola práv

Kontrolu práv na jednotlivých REST službách/metodách je třeba provést pomocí anotace:

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

URL prefixy a anotace jsou **povinné**, aby v systému byla zabezpečena **dvojitá kontrola**.

## CSRF token

Při volání REST služeb administrace `/admin/rest/*` je vyžadováno nastavení CSRF tokenu. Ten je [standardně dostupný](../../developer/frameworks/thymeleaf.md#layoutservice) v objektu `window.csrfToke` a je automaticky nastaven pro všechna volání přes knihovnu `jQuery` v `app.js`:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

Výjimku mají volání obsahující v URL adrese výraz `/html` nebo `html/`, kde se předpokládá vrácení HTML kódu namísto JSON objektu.

Nastavením konfigurační proměnné `logoffRequireCsrfToken` na hodnotu `true` je možné aktivovat vyžadování CSRF tokenu pro odhlášení uživatele (z administrace i zákaznické zóny).

### Vložení tokenu ve web stránce

CSRF token lze vložit do textu web stránky pomocí makra `!CSRF_INPUT!`, které vloží kompletní HTML pole, nebo pomocí `!CSRF_TOKEN!`, které vloží hodnotu CSRF tokenu.

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

Případně můžete využít přímo API třídy [CSRF](../../../../src/webjet8/java/sk/iway/iwcm/system/stripes/CSRF.java) pro generování hodnoty `public static String getCsrfToken(HttpSession session, boolean saveToSession)` nebo celého HTML pole `public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)`. Pro přidání HTTP hlavičky pro všechny AJAX volání přes `jQuery` můžete použít následující kód:

```html
<script>
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': '<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(session, true)%>',
    }
});
</script>
```

### Ochrana URL adres

CSRF ochranu lze aktivovat na libovolné URL adresy, například `/private/rest` nastavením konfigurační proměnné `csrfRequiredUrls` do které zadáte na nový řádek začátky URL adres pro které má být CSRF ochrana vyžadována. Podporován je i formát se znaky `%` pro obsahuje a `!` pro končí na. Příklad:

```
/private/rest
%/rest/
%/export.pdf!
```
