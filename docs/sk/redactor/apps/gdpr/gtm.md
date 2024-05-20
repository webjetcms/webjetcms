# Integrácia Google Tag Manager

Ak sa na webe používa ```Google Tag Manager``` k vkladaniu skriptov a sledovacích nástrojov, potrebujeme zabezpečiť nasledujúce body.

* Inicializácia ```DataLayer```
* Predvolené nastavenie súhlasov
* Aktualizácia súhlasov pri zmene preferencií návštevníka a potvrdení ```cookies``` lišty

## Inicializácia DataLayer

```DataLayer``` musí byť vytvorený ešte pred vloženým ```GTM```.

    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments)};

## Predvolené nastavenie súhlasov

```ad_storage``` a ```analytics_storage``` sú predvolené súhlasy, ktoré vedia čítať Google nástroje bez potreby ďalších podmienok v ```GTM```. Nadobudnúť môžu hodnoty ```denied``` a ```granted```.

```javascript
    gtag('consent', 'default', {
        'ad_storage': 'denied',
        'ad_user_data': 'denied',
        'ad_personalization': 'denied',
        'analytics_storage': 'denied'
    });
```

Ide o východiskové nastavenie. Pri načítaní stránky sa nastaví na ```denied/granted``` podľa našej cookie ```enableCookieCategory```. Ak príde návštevník na web prvýkrát, hodnoty sú ```denied```.

**V princípe platí, že**

- ```ad_storage``` = WebJET marketingová kategória ```cookies```
- ```ad_user_data``` = WebJET marketingová kategória ```cookies```
- ```ad_personalization``` = WebJET marketingová kategória ```cookies```
- ```analytics_storage``` = WebJET štatistická kategória ```cookies```

**Upozornenie:** predvolené nastavenie súhlasov musí byť v kóde ešte pred vloženým ```GTM```.

*Zaujímavosťou je, že ak sú súhlasy vyššie zakázané, Google Analytics sa aj tak spustí. Nevytvára však ```cookies``` a neposiela si informácie o používateľovi, jeho prehliadači, nesleduje sa návšteva a podobne. Spúšťa sa z dôvodu, že sľubuje do-modelovanie analytiky na základe chýbajúcich dát používateľov, ktorí nedali súhlas.*

### Ďalšie kategórie súhlasov

Kód vyššie by mal byť samozrejme doplnený aj o WebJET kategórie ```cookies```, ktoré sa používajú v rámci webu, napr. preferenčné.

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

## Aktualizácia súhlasov pri zmene preferencií

Akonáhle dôjde k zmenám v súhlasoch (návštevník interaguje s cookies lištou), je potrebné spraviť **aktualizáciu súhlasu** + **odoslať do GTM event**.

```javascript
    gtag('consent', 'update', {
        'ad_storage': 'granted',
        'preferencne': 'granted'
        'marketingove': 'denied'
    });
    dataLayer.push({'event': 'consent-update'});
```

V rámci ```gtag update``` stačí vložiť len tie kategórie, ktoré sa zmenili.

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
