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

