# Přihlašovací a odhlašovací formulář

Na webové stránky můžete snadno přidat formulář pro přihlášení nebo odhlášení z hromadného e-mailu.

## Přihlášení

Vytvoření stránky `/prihlasenie-do-mailingu.html` s následujícím kódem HTML. Vložte aplikaci pro přihlášení e-mailem:

```html
<p>Vyberte si prosím informácie, ktoré chcete dostávať emailom:</p>
!INCLUDE(/components/dmail/subscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

Při nahrávání žádosti můžete použít jednodušší formulář pro přihlášení k odběru hromadných e-mailů. Zobrazí se pouze pole pro zadání e-mailové adresy, je vhodné jej vložit do zápatí stránky.

Registruje se do všech e-mailových skupin, které mají tuto možnost povolenou. **Povolit přidání/odebrání ze skupiny samotným uživatelem** a **Vyžadovat potvrzení e-mailové adresy**. Nezahrnuje `captcha` prvek, takže je vyžadováno potvrzení e-mailové adresy. Formulář používá `Bootstrap v5` zobrazit formulář a dialogové okno.

## Odhlášení

Vytvoření stránky `/odhlasenie-z-mailingu.html` s následujícím kódem HTML. Vložte aplikaci pro odhlášení odběru e-mailu:

```html
<p>Zadajte váš email pre odhlásenie odberu informačných emailov:</p>
!INCLUDE(/components/dmail/unsubscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

Chcete-li se z odběru odhlásit, můžete v e-mailu přímo vytvořit odkaz na stránku pro odhlášení:

`<a href="/odhlasenie-z-mailingu.html?email=!RECIPIENT_EMAIL!&save=true">Kliknite pre odhlásenie</a>`

Při odesílání e-mailu se v záhlaví e-mailu automaticky nastaví možnost odhlášení odběru. [List-Unsubscribe=Jedním kliknutím](https://support.google.com/a/answer/81126#subscriptions) v podporovaných e-mailových klientech. Odhlašovací odkaz je nastaven podle domény adresy stránky odeslaného e-mailu, v případě potřeby lze doménu změnit nastavením proměnné conf. `dmailListUnsubscribeBaseHref`. Aby se v e-mailovém klientovi zobrazilo přímé tlačítko pro odhlášení, musí e-mail/vaše doména splňovat několik kritérií (doporučujeme tato kritéria nastavit pro lepší doručitelnost e-mailů):
- Nastavení [DKIM](https://www.dkim.org) klíče domény s platnými [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamu. Doporučujeme používat pro zasílání [Amazon SES](../../../../install/config/README.md#nastavení-amazon-ses) a `DKIM` automaticky nastaví také `SPF`.
- Nastavení [DMARC](https://dmarc.org) záznam. V systému DNS vytvořte nový `TXT` záznam pro doménu `_dmarc.vasadomena.sk` s hodnotou alespoň `v=DMARC1; p=none; sp=none`.

Kromě toho v poštovní schránce `gmail` tlačítko pro odhlášení se zobrazí pouze v případě, že jste zařazeni jako odesílatel do kategorie hromadných e-mailů.

## E-mail s textem pro přihlášení / odhlášení

Pokud potřebujete upravit text e-mailu, který je odesílán pro potvrzení přihlášení/odhlášení, můžete upravit standardní kód HTML v konfiguraci systému v části Úprava textu. Klíče pro zadávání textu jsou následující `dmail.subscribe.bodyNew` pro přihlášení a `dmail.unsubscribe.bodyNew` k odhlášení.

Pokud potřebujete text formátovat pokročilým způsobem, je možné ve WebJETu vytvořit webovou stránku s textem e-mailů. Aplikace `subscribe.jsp a unsubscibe.jsp` můžete přidat parametr `emailBodyId` s ID stránky textu e-mailu. Stránka může vypadat takto:

```
Váž. p. !name!,

ďakujeme vám za záujem dostávať náš newsletter. Prosím potvrďte vašu voľbu kliknutím na nasledovnú linku:

/prihlasenie-do-mailingu.html?hash=!HASH!

Mohlo sa stať že vašu emailovú adresu zadal niekto iný, v tom prípade môžete ignorovať tento email, žiadne informácie nebudete dostávať.
```