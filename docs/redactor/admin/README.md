# Hlavné ovládacie prvky

Rozloženie administrácie je štandardné. V hornej časti sa nachádza hlavička, v ľavej časti menu.

![](welcome.png)

## Hlavička

V hornej časti sa nachádza hlavička:

![](header.png)

obsahuje nasledovné možnosti:

- Výber domény s ktorou pracujete (pri viac doménovej inštalácii).
- Odkaz na otvorenie pomocníka.
- ![](icon-switch8.png ":no-zoom") Ikonu na prepnutie do starej verzie 8. Kliknutím na túto ikonu sa celá administrácia prepne do starej verzie 8. Pre prechod nazad do nového dizajnu zadajte do prehliadača adresu ```/admin/v9/```.
- Meno aktuálne prihláseného používateľa, kliknutím na meno zobrazíte nasledovné možnosti:
  - Profil - upraviť vlastný profil (meno, email... - po zmene profilu je potrebné sa odhlásiť a znova prihlásiť).
  - Dvojstupňové overovanie - možnosť aktivovať dvoj stupňové overovanie pomocou aplikácie ```Google Authenticate``` pri prihlasovaní do administrácie. Zvyšuje sa tak bezpečnosť vášho konta, pretože okrem hesla je pre prihlásenie potrebné zadať aj kód z vášho mobilného zariadenia. Odporúčame nastaviť na všetky kontá, cez ktoré je možné spravovať používateľské účty a práva. Ak používate overovanie voči `ActiveDirectory/SSO` serveru môžete menu položku vypnúť nastavením konf. premennej `2factorAuthEnabled` na hodnotu `false`.
  - Správa šifrovacích kľúčov - umožňuje vytvoriť nový šifrovací kľúč pre šifrovanie formulárov a zadať existujúci kľúč pre ich dešifrovanie. Vyžaduje právo Formuláre.
  - Odhlásenie - odhlásenie z administrácie.
- ![](icon-logoff.png ":no-zoom") Ikonu na odhlásenie z administrácie.

## Menu

V ľavej časti pod logom WebJETu sú ikony reprezentujúce hlavné sekcie menu. Rozhodli sme sa pre takúto reprezentáciu prvej úrovne menu, aby sme nemuseli mať hlboko vnorené menu položky:

![](menu-main-sections.png)

Kliknutím na ikonu hlavnej sekcie sa zobrazia menu položky zvolenej sekcie:

![](menu-items.png)

Menu položky sa zobrazujú v tmavšej a svetlejšej farbe. Tmavšie ešte nie sú prerobené do nového dizajnu, vidno to na odkaze na aplikáciu štatistiky. Kliknutím na túto položku sa zobrazí aplikácia v starom dizajne. Kliknutím na už prerobenú časť, napr. Úvod sa zo starej verzie prepnem nazad do novej.

Prepínanie medzi starou a novou verziou je automatické podľa toho, či je už aplikácia prerobená, alebo nie. V prípade potreby je možné sa úplne prepnúť do starej verzie kliknutím na ikonu ![](icon-switch8.png ":no-zoom") Prepnúť na verziu 8 v hlavičke.

## Zobrazenie na mobilných zariadeniach

Administrácia sa prispôsobuje mobilným zariadeniam. Pri šírke okna menej ako 1200 bodov sa schová hlavička stránky a ľavé menu:

![](welcome-tablet.png)

hlavičku a menu zobrazíte kliknutím na ikonu hamburger menu ![](icon-hamburger.png ":no-zoom") vľavo hore. Následne sa menu a hlavička zobrazí ponad stránku:

![](welcome-tablet-showmenu.png)

menu zatvoríte kliknutím na ikonu zatvorenia menu ![](icon-hamburger-show.png ":no-zoom").

Editor v datatabuľke pri šírke okna menej ako 992 bodov (tabletové zobrazenie) sa zobrazí na plnú veľkosť okna:

![](editor-tablet.png)

Pri okne užšom ako 576 bodov sa presunú aj názvy polí z ľavej strany nad pole pre lepšie zobrazenie napr. na mobilnom telefóne:

![](editor-phone.png)