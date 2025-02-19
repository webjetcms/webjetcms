# Hlavní ovládací prvky

Rozložení administrace je standardní. V horní části je záhlaví a vlevo menu. Třetí úroveň nabídky je zobrazena jako navigační karty v záhlaví.

![](welcome.png)

## Záhlaví

Záhlaví se nachází v horní části:

![](header.png)

obsahuje následující možnosti:
- Odkaz na otevření nápovědy.
- ![](icon-search.png ":no-zoom") Ikona pro otevření stránky [Vyhledávání](search/README.md)
- Jméno aktuálně přihlášeného uživatele, po kliknutí na jméno se zobrazí následující možnosti:
  - Profil - úprava vlastního profilu (jméno, e-mail... - po změně profilu se musíte odhlásit a znovu přihlásit).
  - Dvoufázové ověřování - možnost aktivovat dvoufázové ověřování pomocí aplikace. `Google Authenticate` při přihlašování do administrace. Tím se zvýší bezpečnost vašeho účtu, protože kromě hesla musíte pro přihlášení zadat také kód z mobilního zařízení. Doporučujeme toto nastavení u všech účtů, jejichž prostřednictvím lze spravovat uživatelské účty a práva. Pokud používáte ověřování proti `ActiveDirectory/SSO` serveru, můžete tuto položku nabídky zakázat nastavením proměnné conf. `2factorAuthEnabled` na hodnotu `false`.
  - Správa šifrovacích klíčů - umožňuje vytvořit nový šifrovací klíč pro šifrování formulářů a zadat stávající klíč pro jejich dešifrování. Vyžaduje právo Formuláře.
  - Odhlášení - odhlášení ze správy.
- ![](icon-logoff.png ":no-zoom") Ikona pro odhlášení ze správy.

V záhlaví lze zobrazit karty pro přechod na třetí úroveň nabídky.

## Nabídka

V levé části pod logem WebJET jsou ikony představující hlavní sekce nabídky. Rozhodli jsme se takto znázornit první úroveň nabídky, abychom nemuseli mít hluboce vnořené položky nabídky:

![](menu-main-sections.png)

Kliknutím na ikonu hlavní sekce zobrazíte položky nabídky vybrané sekce:

![](menu-items.png)

V dolní části levého menu vyberte doménu, se kterou pracujete (v případě instalace s více doménami).

![](domain-selector.png)

## Zobrazení na mobilních zařízeních

Správa se přizpůsobuje mobilním zařízením. Pokud je šířka okna menší než 1200 pixelů, záhlaví stránky a levé menu se skryjí:

![](welcome-tablet.png)

záhlaví a menu lze zobrazit kliknutím na ikonu hamburger menu. ![](icon-hamburger.png ":no-zoom") vlevo nahoře. Pak se nad stránkou zobrazí nabídka a záhlaví. Navigační karty se zobrazí jako výběrové nabídky v záhlaví.

![](welcome-tablet-showmenu.png)

Chcete-li zavřít nabídku, klikněte na ikonu zavřít nabídku. ![](icon-hamburger-show.png ":no-zoom").

Editor v datové tabulce se při šířce okna menší než 992 pixelů (zobrazení na tabletu) zobrazí v plné velikosti okna:

![](editor-tablet.png)

V případě okna užšího než 576 pixelů se názvy polí přesunou z levé strany nad pole, aby se lépe zobrazovaly např. na mobilním telefonu:

![](editor-phone.png)
