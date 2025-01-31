# Login and logout form

You can easily add a form to the website to opt-in or opt-out of the bulk email.

## Login

Create a page `/prihlasenie-do-mailingu.html` with the following HTML code. Insert the application for the email login:

```html
<p>Vyberte si prosím informácie, ktoré chcete dostávať emailom:</p>
!INCLUDE(/components/dmail/subscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

You can use a simpler form to sign up for bulk email when you upload your application. It only displays a field to enter an email address, it is advisable to insert it in the footer of the page.

Registers to all email groups that have the option enabled **Allow adding/removing from a group by the user themselves** a **Require email address confirmation**. Does not include `captcha` element, so email address confirmation is required. The form uses `Bootstrap v5` to display the form and dialog box.

If you use the app on the website **Registration form - simple** it is necessary that the group of users for registration to the bulk email has the following options set:
- Allow adding/removing from a group by the user themselves
- Require email address confirmation

Visitors will register for groups that have these options set. If there is no group with this setting the form will not be displayed.

## Logout

Create a page `/odhlasenie-z-mailingu.html` with the following HTML code. Insert the application to unsubscribe from email:

```html
!INCLUDE(/components/dmail/unsubscribe.jsp, senderEmail=name@your-domain.com, senderName="Your Name", confirmUnsubscribe=true)!
```

To unsubscribe, you can directly create a link to the unsubscribe page in the email:

```html
<a href="/odhlasenie-z-mailingu.html?email=!RECIPIENT_EMAIL!&save=true">Kliknite pre odhlásenie</a>
```

When you send an email, the email header is automatically set for unsubscribing [List-Unsubscribe=One-Click](https://support.google.com/a/answer/81126#subscriptions) in supported email clients. The unsubscribe link is set according to the domain of the page address of the sent email, if necessary the domain can be changed by setting the conf. variable `dmailListUnsubscribeBaseHref`. In order to display a direct unsubscribe button in the email client, the email/your domain must meet several criteria (we recommend setting these criteria for better email deliverability):
- Settings [DKIM](https://www.dkim.org) domain keys with valid [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) of record. We recommend to use for sending [Amazon SES](../../../../install/config/README.md#amazon-ses-settings) a `DKIM` set there, it will also automatically set `SPF`.
- Settings [DMARC](https://dmarc.org) record. In DNS, create a new `TXT` record for the domain `_dmarc.vasadomena.sk` with a value of at least `v=DMARC1; p=none; sp=none`.

Additionally in the mailbox `gmail` the unsubscribe button will only appear if it puts you as a sender in the bulk email category.

![](../unsubscribed/unsubscribed-form.png)

### Application settings

In addition to the email and sender name, options can be set in the application editor:
- **Always show confirmation of logout**: If this option is selected, the user must confirm the logout on the form displayed. If this option is not selected, the user will be logged out directly after clicking on the link in the email (no further steps).

![](editor.png)

- **Text displayed before logging out** Custom text can be used on the page where the app is embedded. If you leave the text blank, no text or button will appear `Nie, chcem zostať`. You can also insert any text into the web page before the check-out application.

## Email with text to log in

If you need to modify the text of the email that is sent for login/logout confirmation you can modify the standard HTML code in the system configuration in the Text editing section. The text keys are `dmail.subscribe.bodyNew` for logging in and `dmail.unsubscribe.bodyNew` to log out.

If you need to format the text in an advanced way, it is possible to create a web page in WebJET with the text of the emails. Applications `subscribe.jsp a unsubscibe.jsp` you can add a parameter `emailBodyId` with the page ID of the email text. The page may look like this:

```
Váž. p. !name!,

ďakujeme vám za záujem dostávať náš newsletter. Prosím potvrďte vašu voľbu kliknutím na nasledovnú linku:

/prihlasenie-do-mailingu.html?hash=!HASH!

Mohlo sa stať že vašu emailovú adresu zadal niekto iný, v tom prípade môžete ignorovať tento email, žiadne informácie nebudete dostávať.
```
