# Úvodná obrazovka

## Prihlásenia

Zobrazuje zoznam vašich prihlásení (ak je povolené prihlásenie na viacerých zariadeniach naraz) a zoznam prihlásených administrátorov.

### Moje aktívne prihlásenia

Mini aplikácia **Moje aktívne prihlásenia** zobrazuje zoznam všetkých aktívnych prihlásení do administrácie WebJET CMS pod Vaším používateľským kontom. Vaše aktuálne prihlásenie je označené ikonou <i class="ti ti-current-location fs-6"></i>.

![](sessions.png)

Jednotlivé prihlásenia majú tooltip, ktorý zobrazí ďalšie informácie.

![](sessions-tooltip.png)

Kliknutím na ikonu <i class="ti ti-logout fs-6"></i> môžete ukončiť dané prihlásenie. Ak je to v rámci Vášho aktuálneho uzla clustra, prihlásenie bude okamžite ukončené. Ak je to prihlásenie v inom uzli, prihlásenie bude ukončené po synchronizácii medzi uzlami clustra (typicky do minúty).

Poznámka: údaje sa aktualizujú po prihlásení používateľa. Môžete nastaviť nový záznam do [úlohy na pozadí](../../admin/settings/cronjob/README.md) na častejšiu aktualizáciu údajov, kde ako názov úlohy zadáte hodnotu `sk.iway.iwcm.stat.SessionClusterService`.

### Prihlásení administrátori

Ak máte právo "Úvod - zobrazenie prihlásených administrátorov", zobrazí sa aj zoznam všetkých prihlásených administrátorov. Máte tak prehľad, koľko používateľov aktuálne pracuje v administrácii.


## Záložky

Do mini aplikácie záložky si môžete pridať odkazy na často používané sekcie v administrácii. Po prihlásení tak nemusíte hľadať danú sekciu v menu, ale priamo kliknete na odkaz v záložkách.

![](bookmarks.png)

Kliknutím na oranžovú ikonu naľavo od textu Záložky sa otvorí dialógové okno, v ktorom zadáte názov záložky a adresu, ktorá sa má otvoriť po kliknutí na meno záložky.

![](bookmarks-modal.png)

Predvolene sú zobrazené záložky na zoznam web stránok a formulárov. Tieto sa zobrazia aj keď zmažete všetky záložky.

Upozornenie: zoznam záložiek sa ukladá v prehliadači, ak používate viacero prehliadačov nastavte si záložky vo všetkých.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/G5Ts04jSMX8" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Spätná väzba

Kliknutím na tlačidlo Zaslať spätnú väzbu môžete nám, programátorom, zaslať vašu spätnú väzbu k používaniu WebJET CMS. Pripomienka sa odošle po vyplnení formuláru emailom.

Vaše pripomienky posúdime a pridáme do [mapy rozvoja](../../ROADMAP.md). Môžete tak aj vašim názorom zlepšiť fungovanie WebJET CMS.

![](feedback.png)

V dialógovom okne môžete zadať text vašej pripomienky, komentár, alebo pochvalu. V prípade potreby môžete priložiť aj súbory (napr. fotku obrazovky, alebo dokument s opisom vašej požiadavky).

![](feedback-modal.png)

Ak zvolíte možnosť Zaslať anonymne nebude do odoslaného emailu zadané vaše meno a emailová adresa ako meno a email odosielateľa.