# Autorizace API

Pokud potřebujete přistupovat ke službám REST/`API WebJET CMS` z externího systému, můžete použít možnost autorizace klíčem API. Ten se odesílá v hlavičce HTTP `x-auth-token` při volání služby REST. Pro takovou autorizaci není třeba odesílat token CSRF.

## Nastavení klíče

Klíč API je spojen se skutečným uživatelským účtem. V editaci uživatele na kartě Osobní údaje zadejte klíč do pole Klíč API. Doporučujeme zadat znak `*` vygenerovat náhodný klíč API. Po vygenerování se klíč zobrazí v oznámení, stejně jako hodnota zadaná v hlavičce HTTP.

![](api-key-notification.png)

## Odeslání klíče

Zadaný klíč API je odeslán v hlavičce HTTP `x-auth-token` (název záhlaví lze změnit v proměnné conf. `logonTokenHeaderName`) ve formátu `base64(login:token)`. Přesná hodnota se zobrazí při generování náhodného tokenu.

Příklady:

```shell
#zoznam web stranok v priecinku 25
curl -X GET \
  'http://iwcm.interway.sk/admin/rest/web-pages/all?groupId=25' \
  --header 'x-auth-token: dGVzdGVyOkJiO3VLQFA2WlNGYnI4IS9jSmI0QGcyM2A0PkN1RjJw'

#web stranka 4
curl -X GET \
  'http://iwcm.interway.sk/admin/rest/web-pages/4' \
  --header 'x-auth-token: dGVzdGVyOkJiO3VLQFA2WlNGYnI4IS9jSmI0QGcyM2A0PkN1RjJw'
```

## Seznam všech služeb REST

Seznam všech služeb REST získáte po lokálním spuštění WebJETu na adrese URL. `/admin/swagger-ui/index.html` (vyžaduje nastavení konfigurační proměnné `swaggerEnabled` na adrese `true`).

![](swagger.png)

## Technické informace

Autorizaci poskytuje `SpringSecurity` filtr implementovaný ve třídě `sk.iway.iwcm.system.spring.ApiTokenAuthFilter`. Filtr je inicializován v `sk.iway.webjet.v9.V9SpringConfig.configureSecurity`. Technicky proběhne standardní přihlášení zadaného uživatele a po provedení požadavku HTTP se provede příkaz `session` vypnuto.
