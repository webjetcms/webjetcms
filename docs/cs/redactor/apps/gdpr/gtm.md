# Integrace Google Tag Manager

Pokud se na webu používá `Google Tag Manager` ke vkládání skriptů a sledovacích nástrojů, potřebujeme zajistit následující body.

- Inicializace `DataLayer`
- Výchozí nastavení souhlasů
- Aktualizace souhlasů při změně preferencí návštěvníka a potvrzení `cookies` lišty

## Inicializace DataLayer

`DataLayer` musí být vytvořen ještě před vloženým `GTM`.

```javascript
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments)};
```

## Výchozí nastavení souhlasů

`ad_storage` a `analytics_storage` jsou výchozí souhlasy, které umí číst Google nástroje bez potřeby dalších podmínek v `GTM`. Nabýt mohou hodnoty `denied` a `granted`.

```javascript
    gtag('consent', 'default', {
        'ad_storage': 'denied',
        'ad_user_data': 'denied',
        'ad_personalization': 'denied',
        'analytics_storage': 'denied'
    });
```

Jedná se o výchozí nastavení. Při načítání stránky se nastaví na `denied/granted` podle naší cookie `enableCookieCategory`. Přijde-li návštěvník na web poprvé, hodnoty jsou `denied`.

**V principu platí, že**

- `ad_storage` = WebJET marketingová kategorie `cookies`
- `ad_user_data` = WebJET marketingová kategorie `cookies`
- `ad_personalization` = WebJET marketingová kategorie `cookies`
- `analytics_storage` = WebJET statistická kategorie `cookies`

!>**Upozornění:** výchozí nastavení souhlasů musí být v kódu ještě před vloženým `GTM`.

*Zajímavostí je, že pokud jsou souhlasy výše zakázány, Google Analytics se stejně spustí. Nevytváří však `cookies` a neposílá si informace o uživateli, jeho prohlížeči, nesleduje se návštěva a podobně. Spouští se z důvodu, že slibuje do-modelování analytiky na základě chybějících dat uživatelů, kteří nedali souhlas.*

### Další kategorie souhlasů

Kód výše by měl být samozřejmě doplněn io WebJET kategorie `cookies`, které se používají v rámci webu, například. preferenční.

```javascript
    gtag('consent', 'default', {
        'ad_storage': 'denied',
        'ad_user_data': 'denied',
        'ad_personalization': 'denied',
        'analytics_storage': 'denied',
        'preferencne': 'denied',
        'nutne': 'granted'
    });
```

## Aktualizace souhlasů při změně preferencí

Jakmile dojde ke změnám v souhlasech (návštěvník interaguje s cookies lištou), je třeba udělat **aktualizaci souhlasu** + **odeslat do GTM event**.

```javascript
    gtag('consent', 'update', {
        'ad_storage': 'granted',
        'preferencne': 'granted'
        'marketingove': 'denied'
    });
    dataLayer.push({'event': 'consent-update'});
```

V rámci `gtag update` stačí vložit jen ty kategorie, které se změnily.

`DataLayer` push je event kvůli `GTM`, aby se uměli spustit nástroje přímo při udělení souhlasu a nemuselo se čekat na obnovení stránky.

Aktualizaci dat automaticky provádí aplikace `GDPR Cookies` i aplikace `Cookiebar`.

## Definování souhlasů, pokud návštěvník potvrdil své volby

`Consent default` - kategorie (kromě `nutne`) jsou vždy nastaveny na `denied`, i přesto, že jde o opakovanou návštěvu a návštěvník dříve povolil jednotlivé kategorie. No v takovém případě hned za definováním `consent default` následuje v kódu `consent update` (bez DataLayer push eventu - ten se posílá jen při interagování s cookies lištou).

## Modelový příklad

```html
    <html>
      <head>

        <!-- Inicializácia DataLayer a východiskový stav súhlasov -->
        <iwcm:write>!INCLUDE(/components/gdpr/gtm_init.jsp)!</iwcm:write>

        <!-- Include scripty aplikácie, prostredníctvom ktorej sa vloží 1. časť GTM -->
        <iwcm:insertScript position="head"/>

      </head>
      <body>

         <!-- Include scripty aplikácie, prostredníctvom ktorej sa vloží 2. časť GTM -->
        <iwcm:insertScript position="body"/>

        <!-- GDPR modul, v ktorom sa spúšťajú eventy pri aktualizácii súhlasov -->
        <iwcm:write>!INCLUDE(/components/gdpr/cookie_bar.jsp)!</iwcm:write>

      </body>
    </html>
```

## Událost při odeslání formuláře

Po odeslání formuláře přes AJAX je publikována událost `WJ.formSubmit`, na kterou lze poslouchat při napojení na `DataLayer` Např. jako:

```javascript
    window.addEventListener("WJ.formSubmit", function(e) { console.log("DataLayer, submitEvent: ", e); dataLayer.push({"formSubmit": e.detail.formDiv, "formSuccess": e.detail.success}); });
```
