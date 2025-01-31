# AB testování

## Úvod

Aplikace AB testování umožňuje provádět AB testování verze webu. Kliknutím na tlačítko Uložit jako AB test je možné vytvořit verzi B stránky. Vytvoří se kopie stránky s upravenou adresou URL, ve které můžete testovat verzi B stránky. WebJET pak automaticky zajistí, aby se verze A a B stránky zobrazovaly na původní adrese URL v definovaném poměru. Návštěvník tedy stále vidí původní adresu URL, ale místo verze A se mu automaticky zobrazí verze B stránky.

## Co je AB testování

- porovnání jedné významné změny
- měříme konverzi akce
- doporučený dostatečný vzorek alespoň 1000 návštěvníků/obrázků.
- `split` test = testování komplexní změny

![](how-it-works.png)

**Výhody:**

- test je jednoduchý a rychlý
- identifikuje pro nás důležité objekty
- je snadno měřitelný

**Nevýhody:**

- může často zklamat
- je nutné správně navrhnout, co testujeme a jaký by měl být výsledek.
- velkou změnu budeme testovat krok za krokem po delší dobu.

## Jak připravit test

Připravte hypotézu, která by měla zlepšit stav

- využívat statistiky (vysoká míra výpadku).
- brainstorming
- Průzkum

**Co je vhodné testovat:**

- záhlaví (stránky, tlačítka)
  - stručný vs. popisný
  - orientace na funkci nebo užitek
- Obrázky
  - usmívající se lidé vs. fotografie produktu
  - barevné provedení, velikost
  - video
- text stránky
  - pod nadpisy, zvýraznění slov
  - odrážky vs. odstavce
  - výhody vs. funkce
- barva
  - tlačítka, odkazy, nadpisy
- formuláře
  - počet polí
  - povinné vs. nepovinné
  - rozdělení do více kroků
- `CTA` prvky (prvky, na které návštěvník klikne a provede akci).
  - text, barva, poloha, způsob působení
- změnit rozložení objektů
- použitelnost
  - co bude lepší?
- nezapomeňme na stránku po konverzi
  - získejme od návštěvníka více informací
  - sociální ikony, newsletter
  - bonusová nabídka

## Vytvoření testu

Chcete-li vytvořit verzi B stránky, vyberte původní verzi stránky a klikněte na ikonu . <i class="ti ti-a-b" ></i> Uložit jako test AB. Poté vytvoříte kopii stránky, která bude zobrazovat ikonu ve sloupci Stav. <i class="ti ti-a-b" ></i>. Ve verzi B můžete upravit text stránky a otestovat dopad změn.

![](datatable.png)

Je také možné otestovat tzv. `split` Testy. Návštěvník se vygeneruje při prvním přístupu `cookie` který určuje, jaká verze stránky se mu zobrazí. Pokud je vygenerována verze B, všechny následující stránky, které mají verzi B, se také zobrazí ve verzi B. Návštěvník tak může přecházet mezi více stránkami verze B.

V aplikaci AB Testing je možné nastavit poměr mezi verzí A a B, technické názvy URL. `suffixu` a název souboru cookie, platnost souboru cookie a především zapnutí a vypnutí testování AB.

## Měření výsledků

- měříme stránku/akci po konverzi
- split test umožní verzi B děkovné stránky
- před testem si ujasněme, co budeme měřit a jak to budeme měřit.
- i snížení konverze je úspěchem testu

V aplikaci AB Testing se v tabulce zobrazí seznam webů, které mají verzi B, s možností zobrazit graf porovnání verzí. Pokud nemáte poměr verzí AB 50:50, WebJET také automaticky provede přepočet poměru návštěv jednotlivých verzí, aby bylo možné čísla porovnat.

![](stat-percent.png)
