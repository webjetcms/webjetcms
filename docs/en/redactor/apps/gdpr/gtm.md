# Google Tag Manager integration

If the site uses `Google Tag Manager` to insert scripts and tracking tools, we need to ensure the following points.

- Initiation `DataLayer`
- Default consent settings
- Update consents when visitor preferences and confirmations change `cookies` rails

## DataLayer initialization

`DataLayer` must be created before the inserted `GTM`.

window.dataLayer = window.dataLayer || \[]; function gtag(){dataLayer.push(arguments)};

## Default consent settings

`ad_storage` a `analytics_storage` are the default consents that Google tools can read without the need for additional conditions in `GTM`. They can acquire values `denied` a `granted`.

```javascript
gtag("consent", "default", {
	ad_storage: "denied",
	analytics_storage: "denied",
});
```

This is the default setting. When the page is loaded, it is set to `denied/granted` according to our cookie `enableCookieCategory`. If the visitor comes to the site for the first time, the values are `denied`.

**In principle, the**
- `ad_storage` = WebJET marketing category `cookies`
- `analytics_storage` = WebJET statistical category `cookies`
**Warning:** the default setting of consents must be in the code before the inserted `GTM`.
*Interestingly, if the consents above are disabled, Google Analytics will still run. However, it does not create `cookies` and does not send information about the user, his browser, does not track the visit, etc. It is being launched because it promises to do-model analytics based on missing data from users who have not given consent.*
### Other categories of consent

The code above should of course also be supplemented with WebJET categories `cookies`that are used within the site, e.g. preferences.

```javascript
gtag("consent", "default", {
	ad_storage: "denied",
	analytics_storage: "denied",
	preferencne: "denied",
});
```

## Updating consents when changing preferences

As soon as changes are made to the consents (the visitor interacts with the cookie bar), it is necessary to manage **updating consent** + **Send to GTM event**.

```javascript
    gtag('consent', 'update', {
        'ad_storage': 'granted',
        'preferencne': 'granted'
        'marketingove': 'denied'
    });
    dataLayer.push({'event': 'consent-update'});
```

In the framework of `gtag update` you only need to insert the categories that have changed.

`DataLayer` push is an event due to `GTM`to be able to launch the tools directly when consent is granted and not wait for the page to refresh.

The data is automatically updated by the application `GDPR Cookies` and the app `Cookiebar`.

## Model example

```html
<html>
	<head>
		<!-- Inicializácia DataLayer a východiskový stav súhlasov -->
		<iwcm:write>!INCLUDE(/components/gdpr/gtm_init.jsp)!</iwcm:write>

		<!-- Include scripty aplikácie, prostredníctvom ktorej sa vloží 1. časť GTM -->
		<iwcm:insertScript position="head" />
	</head>
	<body>
		<!-- Include scripty aplikácie, prostredníctvom ktorej sa vloží 2. časť GTM -->
		<iwcm:insertScript position="body" />

		<!-- GDPR modul, v ktorom sa spúšťajú eventy pri aktualizácii súhlasov -->
		<iwcm:write>!INCLUDE(/components/gdpr/cookie_bar.jsp)!</iwcm:write>
	</body>
</html>
```
