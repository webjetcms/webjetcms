# Integrace Google Tag Manager

Pokud stránka používá `Google Tag Manager` vložit skripty a sledovací nástroje, musíme zajistit následující body.

- Iniciace `DataLayer`
- Výchozí nastavení souhlasu
- Aktualizace souhlasů při změně preferencí a potvrzení návštěvníků `cookies` kolejnice

## Inicializace vrstvy DataLayer

`DataLayer` musí být vytvořen před vložením `GTM`.

```javascript
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments)};
```

## Výchozí nastavení souhlasu

`ad_storage` a `analytics_storage` jsou výchozí souhlasy, které mohou nástroje Google číst bez nutnosti zadávat dodatečné podmínky. `GTM`. Mohou nabývat hodnot `denied` a `granted`.

```javascript
    gtag('consent', 'default', {
        'ad_storage': 'denied',
        'ad_user_data': 'denied',
        'ad_personalization': 'denied',
        'analytics_storage': 'denied'
    });
```

Toto je výchozí nastavení. Po načtení stránky je nastaveno na hodnotu `denied/granted` podle našich souborů cookie `enableCookieCategory`. Pokud návštěvník přichází na stránky poprvé, hodnoty jsou následující. `denied`.

**V zásadě platí, že**

- `ad_storage` = marketingová kategorie WebJET `cookies`
- `ad_user_data` = marketingová kategorie WebJET `cookies`
- `ad_personalization` = marketingová kategorie WebJET `cookies`
- `analytics_storage` = statistická kategorie WebJET `cookies`

!>**Varování:** výchozí nastavení souhlasů musí být v kódu před vložením `GTM`.

*Zajímavé je, že pokud jsou výše uvedené souhlasy zakázány, služba Google Analytics bude stále fungovat. Nevytváří však `cookies` a neodesílá informace o uživateli, jeho prohlížeči, nesleduje návštěvu atd. Spouští se proto, že slibuje do-modelovat analytiku na základě chybějících údajů od uživatelů, kteří k tomu nedali souhlas.*

### Další kategorie souhlasu

Výše uvedený kód by měl být samozřejmě doplněn také o kategorie WebJETu `cookies` které se používají na webu, např. předvolby.

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

Jakmile dojde ke změně souhlasů (návštěvník interaguje s panelem cookie), je nutné spravovat. **aktualizace souhlasu** + **Odeslat do události GTM**.

```javascript
    gtag('consent', 'update', {
        'ad_storage': 'granted',
        'preferencne': 'granted'
        'marketingove': 'denied'
    });
    dataLayer.push({'event': 'consent-update'});
```

V rámci `gtag update` stačí vložit pouze kategorie, které se změnily.

`DataLayer` push je událost způsobená `GTM` aby bylo možné po udělení souhlasu spustit nástroje přímo a nečekat na obnovení stránky.

Data jsou automaticky aktualizována aplikací `GDPR Cookies` a aplikace `Cookiebar`.

## Definování souhlasů, pokud návštěvník potvrdil svou volbu.

`Consent default` - kategorie (kromě `nutne`) jsou vždy nastaveny na hodnotu `denied`, přestože se jedná o opakovanou návštěvu a návštěvník již dříve povolil jednotlivé kategorie. V tomto případě však hned po definování `consent default` následuje v kódu `consent update` (bez události DataLayer push - je odeslána pouze při interakci s panelem cookie).

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

## Událost odeslání formuláře

Po odeslání formuláře pomocí AJAXu je zveřejněna událost. `WJ.formSubmit`, které lze poslouchat po připojení k síti `DataLayer`, např. jako:

```javascript
    window.addEventListener("WJ.formSubmit", function(e) { console.log("DataLayer, submitEvent: ", e); dataLayer.push({"formSubmit": e.detail.formDiv, "formSuccess": e.detail.success}); });
```
