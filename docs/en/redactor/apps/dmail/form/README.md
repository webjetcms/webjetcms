# Login and logout form

You can easily add a form to the website to opt-in or opt-out of the bulk email.

## Login

Create a page `/prihlasenie-do-mailingu.html` with the following HTML code. Insert the application for the email login:

```html
<p>Vyberte si prosím informácie, ktoré chcete dostávať emailom:</p>
!INCLUDE(/components/dmail/subscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

You can use a simpler form to sign up for bulk email when you upload your application. It only displays a field to enter an email address, it is advisable to insert it in the footer of the page.

Registers to all email groups that have the option enabled `Povoliť pridávanie/odoberanie zo skupiny samotným používateľom` a `Vyžadovať potvrdenie e-mailovej adresy`. Does not include `captcha` element, so email address confirmation is required. The form uses `Bootstrap v5` to display the form and dialog box.

## Logout

Create a page `/odhlasenie-z-mailingu.html` with the following HTML code. Insert the application to unsubscribe from email:

```html
<p>Zadajte váš email pre odhlásenie odberu informačných emailov:</p>
!INCLUDE(/components/dmail/unsubscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

To unsubscribe, you can directly create a link to the unsubscribe page in the email:

`<a href="/odhlasenie-z-mailingu.html?email=!RECIPIENT_EMAIL!&save=true">Kliknite pre odhlásenie</a>`

## Email with text to log in / out

If you need to modify the text of the email that is sent for login/logout confirmation you can modify the standard HTML code in the system configuration in the Text editing section. The text keys are `dmail.subscribe.bodyNew` for logging in and `dmail.unsubscribe.bodyNew` to log out.

If you need to format the text in an advanced way, it is possible to create a web page in WebJET with the text of the emails. Applications `subscribe.jsp a unsubscibe.jsp` you can add a parameter `emailBodyId` with the page ID of the email text. The page may look like this:

```html
Váž. p. !name!, ďakujeme vám za záujem dostávať náš newsletter. Prosím potvrďte vašu voľbu kliknutím na nasledovnú linku: /prihlasenie-do-mailingu.html?hash=!HASH! Mohlo sa stať že vašu emailovú adresu zadal niekto iný, v tom prípade môžete ignorovať tento email, žiadne informácie nebudete dostávať.
```
