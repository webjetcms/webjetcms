# Google Tag Manager integration

If ```Google Tag Manager``` is used on the website to embed scripts and tracking tools, we need to ensure the following points.

- Initialization ```DataLayer```
- Default consent settings
- Update consents when changing visitor preferences and confirmations ```cookies``` bar

## Initializing the DataLayer

```DataLayer``` musí byť vytvorený ešte pred vloženým ```GTM```.

```javascript
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments)};
```

## Default consent settings

```ad_storage``` a ```analytics_storage``` sú predvolené súhlasy, ktoré vedia čítať Google nástroje bez potreby ďalších podmienok v ```GTM```. Nadobudnúť môžu hodnoty ```denied``` a ```granted```.

```javascript
    gtag('consent', 'default', {
        'ad_storage': 'denied',
        'ad_user_data': 'denied',
        'ad_personalization': 'denied',
        'analytics_storage': 'denied'
    });
```

This is the default setting. When the page loads, it is set to ```denied/granted``` according to our cookie ```enableCookieCategory```. If the visitor comes to the site for the first time, the values ​​are ```denied```.

**In principle, the following applies**

- ```ad_storage``` = WebJET marketing category ```cookies```
- ```ad_user_data``` = WebJET marketing category ```cookies```
- ```ad_personalization``` = WebJET marketing category ```cookies```
- ```analytics_storage``` = WebJET statistical category ```cookies```

!>**Warning:** the default permissions setting must be in the code before the ```GTM``` is inserted.

*Interestingly, if the consents above are disabled, Google Analytics will still run. However, it does not create ```cookies``` and does not send information about the user, their browser, the visit is not tracked, etc. It is run because it promises to do-model analytics based on the missing data of users who did not give consent.*

### Other categories of consent

The code above should of course be supplemented with WebJET categories ```cookies``` that are used within the website, e.g. preferential.

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

## Updating consents when preferences change

Once there are changes in consents (visitor interacts with the cookie bar), it is necessary to **update consent** + **send to GTM event**.

```javascript
    gtag('consent', 'update', {
        'ad_storage': 'granted',
        'preferencne': 'granted'
        'marketingove': 'denied'
    });
    dataLayer.push({'event': 'consent-update'});
```

Within ```gtag update```, you only need to insert the categories that have changed.

```DataLayer``` push je event kvôli ```GTM```, aby sa vedeli spustiť nástroje priamo pri udelení súhlasu a nemuselo sa čakať na obnovenie stránky.

Aktualizáciu údajov automaticky vykonáva aplikácia ```GDPR Cookies``` aj aplikácia ```Cookiebar```.

## Definovanie súhlasov, ak návštevník potvrdil svoje voľby

`Consent default` - kategórie (okrem `nutne`) sú vždy nastavené na `denied`, aj napriek tomu, že ide o opakovanú návštevu a návštevník predtým povolil jednotlivé kategórie. No v takomto prípade hneď za definovaním `consent default` nasleduje v kóde `consent update` (bez DataLayer push eventu - ten sa posiela len pri interagovaní s cookies lištou).

## Modelový príklad

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

## Form submission event

After submitting the form via AJAX, an event `WJ.formSubmit` is published, which can be listened to when connected to `DataLayer`, e.g. like this:

```javascript
    window.addEventListener("WJ.formSubmit", function(e) { console.log("DataLayer, submitEvent: ", e); dataLayer.push({"formSubmit": e.detail.formDiv, "formSuccess": e.detail.success}); });
```
