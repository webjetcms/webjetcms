# Formulár pre prihlásenia a odhlásenie

Do web stránky môžete jednoducho pridať formulár pre prihlásenie, prípadne pre odhlásenie z hromadného e-mailu.

## Prihlásenie

Vytvorte stránku ```/prihlasenie-do-mailingu.html``` s nasledovným HTML kódom. Vložte aplikáciu pre prihlásenie e-mailu:

```html
<p>Vyberte si prosím informácie, ktoré chcete dostávať emailom:</p>
!INCLUDE(/components/dmail/subscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

Pri vkladaní aplikácie pre prihlásenie môžete použiť jednoduchší formulár pre registráciu do hromadného emailu. Zobrazuje len pole pre zadanie emailovej adresy, je vhodné ho vložiť do pätičky stránky.

Registruje do všetkých emailových skupín, ktoré majú zapnutú možnosť ```Povoliť pridávanie/odoberanie zo skupiny samotným používateľom``` a ```Vyžadovať potvrdenie e-mailovej adresy```. Neobsahuje ```captcha``` element, preto je vyžadované zapnuté potvrdenie e-mailovej adresy. Formulár využíva ```Bootstrap v5``` pre zobrazenie formuláru a dialógového okna.

## Odhlásenie

Vytvorte stránku ```/odhlasenie-z-mailingu.html``` s nasledovným HTML kódom. Vložte aplikáciu pre odhlásenie e-mailu:

```html
<p>Zadajte váš email pre odhlásenie odberu informačných emailov:</p>
!INCLUDE(/components/dmail/unsubscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```
Pre odhlásenie môžete do emailu priamo vytvoriť odkaz na stránku s odhlásením:

```<a href="/odhlasenie-z-mailingu.html?email=!RECIPIENT_EMAIL!&save=true">Kliknite pre odhlásenie</a>```

Pri odosielaní emailu sa automaticky nastaví hlavička emailu pre odhlásenie [List-Unsubscribe=One-Click](https://support.google.com/a/answer/81126#subscriptions) v podporovaných email klientoch. Odkaz na odhlásenie je nastavený podľa domény adresy stránky odosielaného emailu, v prípade potreby je možné doménu zmeniť nastavením konf. premennej `dmailListUnsubscribeBaseHref`. Pre zobrazenie priameho tlačidla na odhlásenie v email klientovi musí email/vaša doména spĺňať viacero kritérií (tieto kritéria odporúčame nastaviť aj pre lepšiu doručiteľnosť emailov):

- Nastavené [DKIM](https://www.dkim.org) kľúče domény s platným [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamom. Odporúčame použiť na odosielanie [Amazon SES](../../../../install/config/README.md#nastavenie-amazon-ses) a `DKIM` nastaviť tam, automaticky sa nastaví aj `SPF`.
- Nastavený [DMARC](https://dmarc.org) záznam. V DNS vytvorte nový `TXT` záznam pre doménu `_dmarc.vasadomena.sk` s hodnotou minimálne `v=DMARC1; p=none; sp=none`.

Naviac v schránke `gmail` sa tlačidlo na odhlásenie zobrazí len v prípade, že vás ako odosielateľa zaradí do kategórie hromadných emailov.

## Email s textom na prihlásenie / odhlásenie

Ak potrebujete modifikovať text emailu, ktorý je zaslaný pre potvrdenie prihlásenia / odhlásenia môžete modifikovať štandardný HTML kód v konfigurácii systému v časti Editácia textov. Kľúče s textami sú ```dmail.subscribe.bodyNew``` pre prihlásenie a ```dmail.unsubscribe.bodyNew``` pre odhlásenie.

Ak text potrebujete formátovať pokročilým spôsobom, je možné vytvoriť web stránku vo WebJETe s textom emailov. Aplikáciam ```subscribe.jsp a unsubscibe.jsp``` môžete pridať parameter ```emailBodyId``` s ID stránky s textom emailu. Stránka môže vyzerať nasledovne:

```html
Váž. p. !name!,

ďakujeme vám za záujem dostávať náš newsletter. Prosím potvrďte vašu voľbu kliknutím na nasledovnú linku:

/prihlasenie-do-mailingu.html?hash=!HASH!

Mohlo sa stať že vašu emailovú adresu zadal niekto iný, v tom prípade môžete ignorovať tento email, žiadne informácie nebudete dostávať.
```