# Zabezpečenie REST služieb

## Pravidlá URL adries

Pri príprave REST služieb prosím dodržiavajte nasledovne URL prefixy:

- ```/rest``` - verejne dostupná REST služba
- ```/rest/private``` alebo ```/private/rest```  - rest služba, vyžadujúca prihlásenie
- ```/admin/rest``` - rest služba vyžadujúca prihlásenie administrátora

Pre ```websocket``` je potrebné používať prefix ```/websocket/``` inak nemusia korektne prechádzať cez web ```firewall```. Odporúčame používať nasledovné prefixy:

- ```/websocket/``` - verejne dostupný ```websocket```
- ```/websocket/private/``` - ```websocket``` vyžadujúci prihlásenie
- ```/websocket/admin/``` - ```websocket``` vyžadujúci prihlásenie administrátora

## Kontrola práv

Kontrolu práv na jednotlivých REST službách/metódach je potrebné vykonať pomocou anotácie:

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

URL prefixy a anotácie sú **povinné**, aby v systéme bola zabezpečená **dvojitá kontrola**.

## CSRF token

Pri volaní REST služieb administrácie ```/admin/rest/*``` je vyžadované nastavenie CSRF tokenu. Ten je [štandardne dostupný](../../developer/frameworks/thymeleaf.md#layoutservice) v objekte ```window.csrfToke``` a je automaticky nastavený pre všetky volania cez knižnicu ```jQuery``` v ```app.js```:

```JavaScript
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    }
});
```

Výnimku majú volania obsahujúce v URL adrese výraz ```/html``` alebo ```html/```, kde sa predpokladá vrátenie HTML kódu namiesto JSON objektu.

Nastavením konfiguračnej premennej `logoffRequireCsrfToken` na hodnotu `true` je možné aktivovať vyžadovanie CSRF tokenu pre odhlásenie používateľa (z administrácie aj zákazníckej zóny).

### Vloženie tokenu vo web stránke

CSRF token je možné vložiť do textu web stránky pomocou makra `!CSRF_INPUT!`, ktoré vloží kompletné HTML pole, alebo pomocou `!CSRF_TOKEN!`, ktoré vloží hodnotu CSRF tokenu.

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

Prípadne môžete využiť priamo API triedy [CSRF](../../../../src/main/java/sk/iway/iwcm/system/stripes/CSRF.java) pre generovanie hodnoty `public static String getCsrfToken(HttpSession session, boolean saveToSession)` alebo celého HTML poľa `public static String getCsrfTokenInputFiled(HttpSession session, boolean saveToSession)`. Pre pridanie HTTP hlavičky pre všetky AJAX volania cez `jQuery` môžete použiť nasledovný kód:

```html
<script>
$.ajaxSetup({
    headers: {
        'X-CSRF-Token': '<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(session, true)%>',
    }
});
</script>
```

### Ochrana URL adries

CSRF ochranu je možné aktivovať na ľubovoľné URL adresy, napríklad `/private/rest` nastavením konfiguračnej premennej `csrfRequiredUrls` do ktorej zadáte na nový riadok začiatky URL adries pre ktoré má byť CSRF ochrana vyžadovaná. Podporovaný je aj formát so znakmi `%` pre obsahuje a `!` pre končí na. Príklad:

```
/private/rest
%/rest/
%/export.pdf!
```
