# Formulář pro přihlášení a odhlášení

Do web stránky můžete jednoduše přidat formulář pro přihlášení, případně pro odhlášení z hromadného e-mailu.

## Přihlášení

Vytvořte stránku `/prihlasenie-do-mailingu.html` s následujícím HTML kódem. Vložte aplikaci pro přihlášení e-mailu:

```html
<p>Vyberte si prosím informácie, ktoré chcete dostávať emailom:</p>
!INCLUDE(/components/dmail/subscribe.jsp, senderEmail=meno@domena.sk, senderName="Ľuboš Balát")!
```

Při vkládání aplikace pro přihlášení můžete použít jednodušší formulář pro registraci do hromadného emailu. Zobrazuje pouze pole pro zadání emailové adresy, je vhodné jej vložit do patičky stránky.

Registruje do všech emailových skupin, které mají zapnutou možnost **Povolit přidávání/odebírání ze skupiny samotným uživatelům** a **Vyžadovat potvrzení e-mailové adresy**. Neobsahuje `captcha` element, proto je vyžadováno zapnuté potvrzení e-mailové adresy. Formulář využívá `Bootstrap v5` pro zobrazení formuláře a dialogového okna.

Pokud ve web stránce použijete aplikaci **Registrační formulář - jednoduchý** je třeba aby skupina uživatelů pro registraci do hromadného emailu měla nastaveny následující možnosti:
- Povolit přidávání/odebírání ze skupiny samotným uživatelům
- Vyžadovat potvrzení e-mailové adresy

Do skupin, které mají tyto možnosti nastaveny se bude registrovat návštěvník. Pokud neexistuje žádná skupina s takovým nastavením formulář se nezobrazí.

## Odhlášení

Vytvořte stránku `/odhlasenie-z-mailingu.html` s následujícím HTML kódem. Vložte aplikaci pro odhlášení e-mailu:

```html
!INCLUDE(/components/dmail/unsubscribe.jsp, senderEmail=name@your-domain.com, senderName="Your Name", confirmUnsubscribe=true)!
```

Pro odhlášení můžete do emailu přímo vytvořit odkaz na stránku s odhlášením:

```html
<a href="/odhlasenie-z-mailingu.html?email=!RECIPIENT_EMAIL!&save=true">Kliknite pre odhlásenie</a>
```

Při odesílání emailu se automaticky nastaví hlavička emailu pro odhlášení [List-Unsubscribe=One-Click](https://support.google.com/a/answer/81126#subscriptions) v podporovaných email klientech. Odkaz na odhlášení je nastaven podle domény adresy stránky odesílaného emailu, v případě potřeby je možné doménu změnit nastavením konf. proměnné `dmailListUnsubscribeBaseHref`. Pro zobrazení přímého tlačítka pro odhlášení v email klientovi musí email/vaše doména splňovat více kritérií (tato kritéria doporučujeme nastavit i pro lepší doručitelnost emailů):
- Nastaveno [DKIM](https://www.dkim.org) klíče domény s platným [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamem. Doporučujeme použít k odesílání [Amazon SES](../../../../install/config/README.md#nastavení-amazon-ses) a `DKIM` nastavit tam, automaticky se nastaví i `SPF`.
- Nastaven [DMARC](https://dmarc.org) záznam. V DNS vytvořte nový `TXT` záznam pro doménu `_dmarc.vasadomena.sk` s hodnotou minimálně `v=DMARC1; p=none; sp=none`.

Navíc ve schránce `gmail` se tlačítko pro odhlášení zobrazí pouze v případě, že vás jako odesílatele zařadí do kategorie hromadných emailů.

![](../unsubscribed/unsubscribed-form.png)

### Nastavení aplikace

Kromě e-mailu a jména odesílatele lze v editoru aplikace nastavit možnosti:
- **Vždy zobrazit potvrzení odhlášení**: Je-li tato možnost zvolena, uživatel musí potvrdit odhlášení na zobrazeném formuláři. Pokud tato možnost není zvolena, uživatel bude odhlášen přímo po kliknutí na odkaz v emailu (bez dalších kroků).

![](editor.png)

- **Text zobrazený před odhlášením** Může být použit vlastní text na stránce, kde je aplikace vložena. Pokud text necháte prázdný, nezobrazí se žádný text ani tlačítko s nápisem `Nie, chcem zostať`. Libovolný text můžete vložit i do web stránky před aplikaci odhlášení.

## Email s textem k přihlášení

Pokud potřebujete modifikovat text emailu, který je zaslán pro potvrzení přihlášení/odhlášení můžete modifikovat standardní HTML kód v konfiguraci systému v části Editace textů. Klíče s texty jsou `dmail.subscribe.bodyNew` pro přihlášení a `dmail.unsubscribe.bodyNew` pro odhlášení.

Pokud text potřebujete formátovat pokročilým způsobem, lze vytvořit web stránku ve WebJETu s textem emailů. Aplikacím `subscribe.jsp a unsubscibe.jsp` můžete přidat parametr `emailBodyId` s ID stránky s textem emailu. Stránka může vypadat následovně:

```
Váž. p. !name!,

ďakujeme vám za záujem dostávať náš newsletter. Prosím potvrďte vašu voľbu kliknutím na nasledovnú linku:

/prihlasenie-do-mailingu.html?hash=!HASH!

Mohlo sa stať že vašu emailovú adresu zadal niekto iný, v tom prípade môžete ignorovať tento email, žiadne informácie nebudete dostávať.
```
