# Úvodná obrazovka

Na úvodnej obrazovke môžete rýchlo skontrolovať návštevnosť, odoslané formuláre či stránky čakajúce na schválenie a vrátiť sa k rozpracovaným stránkam.

<div class="video-container">
    <iframe width="790" height="444" src="https://www.youtube.com/embed/X2GNFn8IpCI" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Prehľad tvoria **widgety**, teda karty s konkrétnymi údajmi, napríklad grafom návštevnosti alebo zoznamom požiadaviek na schválenie. Vyberte si tie, ktoré využívate, a usporiadajte ich podľa toho, čo chcete sledovať ako prvé. Pomocou [skratiek](#vaše-skratky) si otvoríte často používanú časť administrácie alebo konkrétny priečinok bez hľadania v menu.

Pri prvom otvorení sa podľa vašich oprávnení zobrazia predvolené widgety **Návštevnosť**, **Formuláre**, **Na schválenie**, **Chyby 404**, **Pokračujte v práci**, **Odkiaľ návštevníci prišli**, **Najbližšie publikovanie** a **Newsletter**. Ďalšie widgety si môžete sami pridať. V hornej časti je privítanie so skratkami a novinkami, aktívne prihlásenia a vyhľadávanie.

Ponuka widgetov zodpovedá vašim oprávneniam. Rozloženie sa ukladá na vaše konto a je spoločné pre všetky domény a prehliadače. Výber konkrétneho formulára alebo kampane sa pamätá osobitne pre každú doménu.

![](dashboard.png)

Pred názvom stránky v hlavičke celej administrácie a na prihlasovacej stránke sa zobrazuje označenie prostredia: **PROD**, **TEST**, **CIT**, **INT**, **UAT**, **DEMO**, **LOCAL** alebo **DEV**. Je aj v titulku karty prehliadača, aby ste rozlíšili otvorené prostredia. Správca môže upraviť text, ikonu, farbu, štýl a popis označenia podľa [konfigurácie prostredia](../../admin/setup/configuration/dashboard.md).

## Prispôsobenie prehľadu

Na začiatok prehľadu umiestnite widgety s údajmi, ktoré kontrolujete najčastejšie. Ak napríklad spracúvate formuláre a schvaľujete obsah, vyberte widgety **Formuláre** a **Na schválenie**. Widgety, ktoré nepotrebujete, môžete odstrániť.

Kliknite na **Upraviť prehľad**. Zobrazia sa možnosti **Pridať widget**, **Obnoviť** a **Hotovo**.

![](dashboard-edit.png)

Tlačidlom **Pridať widget** otvorte katalóg. Vyhľadajte požadovaný widget podľa názvu a pri ňom kliknite na **Pridať widget**.

![](dashboard-catalogue.png)

Pri widgete použite menu s tromi bodkami:

- **Nastavenia widgetu**: zvoľte dostupnú veľkosť a ďalšie nastavenia, napríklad obdobie alebo formulár. Nie každý widget ponúka viac veľkostí.
- **Presunúť widget**: vyberte, pred ktorú kartu sa má presunúť, alebo ho presuňte na koniec prehľadu. Na počítači môžete použiť aj rukoväť na ťahanie.
- **Odstrániť widget**: odstráni kartu z vášho prehľadu. Údaje v aplikácii zostávajú zachované. Bezprostredne po odstránení je dostupná možnosť **Vrátiť späť**. Po ďalšej úspešne uloženej úprave táto možnosť zanikne. Widget môžete kedykoľvek pridať znova.
- **Obnoviť údaje**: načíta aktuálne údaje danej karty.

V dialógu **Nastavenia widgetu** upravte dostupné možnosti a potvrďte ich tlačidlom **Uložiť**. Väčšiu kartu zvoľte, keď chcete vidieť podrobnejší graf alebo zoznam. Menšia karta zaberie v prehľade menej miesta.

![](dashboard-widget-settings.png)

Po dokončení úprav kliknite na **Hotovo**.

Na menšej obrazovke sa karty automaticky usporiadajú pod seba pri zachovaní poradia. Rozloženie môže obsahovať najviac 48 položiek vrátane skratiek, prihlásení, noviniek a vyhľadávania.

Tlačidlo **Obnoviť** nájdete v lište úprav za tlačidlom **Pridať widget**. Po potvrdení v dialógu sa obnoví predvolený výber widgetov, ich veľkosti, poradie a nastavenia. Vymažú sa aj filtre widgetov vo všetkých doménach a potvrdenie prečítania noviniek. Vaše skratky a ostatné nastavenia účtu zostanú zachované.

Ak pri kliknutí na **Obnoviť** podržíte kláves **Shift**, po potvrdení sa prehľad nahradí všetkými dostupnými widgetmi v každej podporovanej veľkosti. Nepotrebné varianty môžete následne odstrániť.

## Dostupné informácie

Widgety z katalógu **môžete pridať opakovane** s rôznymi veľkosťami alebo nastaveniami. Môžete tak napríklad vedľa seba **sledovať dva rôzne formuláre** alebo **návštevnosť za 7 a 30 dní**. Ich dostupnosť závisí od vašich oprávnení.

Štatistické widgety predvolene používajú posledných sedem ukončených dní, možno zvoliť aj 30 alebo 90 dní. Porovnanie používa rovnako dlhé predchádzajúce obdobie. Karta zobrazuje skutočný rozsah dátumov. Osobitné pravidlá pre obdobie formulárov a chýb 404 sú uvedené pri príslušných widgetoch.

### Skratka do modulu

Často používanú časť administrácie otvoríte bez hľadania v menu či v stromovej štruktúre priečinkov. Skratka môže smerovať aj na vlastnú URL adresu. Postup pridania a príklad odkazu na konkrétny priečinok nájdete v časti [Vaše skratky](#vaše-skratky).

### Pokračujte v práci

K rozpracovanej stránke sa vrátite bez hľadania jej priečinka. Widget zobrazuje najviac šesť vašich naposledy upravovaných stránok s náhľadom obrázka, umiestnením a dátumom úpravy. Kliknutím otvoríte editor stránky. Pri menšej veľkosti karty môžete zoznam rolovať.

### Na schválenie

Upozorňuje na obsah, ktorý čaká na rozhodnutie schvaľovateľa. Zobrazuje požiadavky dostupné v karte **Neschválené** vo Web stránkach. Väčší variant obsahuje šesť najnovších požiadaviek na zmenu alebo zmazanie stránky či priečinka. Ikona pri názve rozlišuje stránku a priečinok.

Kliknutím na položku otvoríte jej schvaľovanie v novom okne. Pri priečinku sa otvorí príslušný priečinok vo Web stránkach spolu so schvaľovacím dialógom. Kliknutím na nadpis alebo celkový počet otvoríte celý zoznam požiadaviek.

### Najbližšie publikovanie

Skontrolujete, ktoré stránky sa majú v najbližšom čase **zverejniť alebo prestať zobrazovať**. Pri každej zmene vidíte dátum a čas plánovaného publikovania alebo ukončenia platnosti. Kliknutím na nadpis otvoríte úplný plán publikovania.

### Formuláre

Skontrolujete tu údaje o odoslaných formulároch, napríklad dopytoch alebo kontaktných správach. V nastaveniach môžete **vybrať konkrétny formulár** alebo všetky dostupné formuláre.

Pri zobrazení všetkých formulárov vidíte **celkový počet odoslaných odpovedí za celé obdobie**. Veľký widget zobrazí desať formulárov s najnovším odoslaním, zoradených od najnovšieho. Pri každom je názov a dátum posledného odoslania. Kliknutím na názov otvoríte jeho odpovede.

Pri výbere konkrétneho formulára sa vo veľkom widgete zobrazí posledných desať odoslaní **za zvolené obdobie**. Ak sa zoznam nezmestí do výšky karty, môžete ho rolovať. V stĺpci **Formulár** uvidíte meno, priezvisko a e-mail z vyplnených údajov. Ak formulár takéto polia nemá, zobrazia sa prvé tri vyplnené údaje v poradí stĺpcov formulára. Kliknutím na tieto údaje otvoríte detail konkrétneho odoslania.

### Návštevnosť

Umožňuje sledovať, ako sa mení návštevnosť webu. V nastaveniach vyberte obdobie a sledovaný údaj: počet zobrazení, návštev alebo unikátnych návštevníkov.

Graf porovnáva aktuálne a predchádzajúce obdobie. Presné hodnoty nájdete v popisoch po podržaní kurzora nad grafom. Tabuľka s údajmi je dostupná čítačkám obrazovky.

### Najnavštevovanejšie stránky

Zobrazuje najnavštevovanejšie stránky za zvolené obdobie s počtom zobrazení. Podľa návštevnosti môžete určiť, ktorému obsahu venovať pozornosť pri aktualizácii. Kliknutím na stránku otvoríte jej podrobnú štatistiku.

### Čo návštevníci hľadajú

Zobrazuje hľadané výrazy a ich počty za zvolené obdobie. Podľa výrazov môžete zistiť, o aké témy majú návštevníci záujem, a skontrolovať, či k nim máte na webe aktuálne informácie.

### Odkiaľ návštevníci prišli

Za zvolené obdobie môžete porovnať zdroje návštevnosti a zistiť, odkiaľ prichádza najviac návštevníkov.

Graf zobrazuje percentuálny podiel jednotlivých evidovaných zdrojov. Presné hodnoty nájdete v popisoch po podržaní kurzora nad grafom. Tabuľka s údajmi je dostupná čítačkám obrazovky.

### Chyby 404

Pomáha nájsť adresy, ktoré neexistujú. Podľa počtu chýb môžete určiť, ktoré odkazy alebo presmerovania treba preveriť ako prvé.

Zobrazuje počet chybových požiadaviek, nie počet rôznych adries. Chyby sa evidujú po týždňoch, preto sa zahrnú celé týždne zasahujúce do zvoleného obdobia. Karta zobrazí skutočný rozsah dátumov a aktuálny týždeň obsahuje údaje dostupné do tohto okamihu.

Ak historické údaje nemožno oddeliť podľa domény, karta oznámi ich nedostupnosť.

### Newsletter

Umožňuje sledovať priebeh odosielania hromadného emailu a zaznamenané otvorenia či kliknutia. Automaticky vyberá aktívnu kampaň, najbližšiu naplánovanú alebo poslednú dokončenú. V nastaveniach môžete zvoliť aj konkrétnu kampaň.

Pri odosielaní sa údaje viditeľnej karty obnovujú každých 30 sekúnd. Počty otvorení a kliknutí predstavujú zaznamenané udalosti.

### Čo je nové

V uvítacom paneli nájdete novinky aktuálnej verzie WebJET CMS. Tlačidlom **Zbaliť novinky** potvrdíte ich prečítanie a ponecháte stručný súhrn. Tlačidlom **Viac info** ich znova rozbalíte.

Systémové upozornenia informujú o stave, ktorý vyžaduje pozornosť, a zostávajú viditeľné do vyriešenia príčiny. Kliknutím na nadpis upozornenia rozbalíte jeho vysvetlenie a dostupnú nápravnú akciu.

### Vyhľadávanie a pomoc

Použite ho, keď chcete nájsť stránku v administrácii alebo návod na prácu s WebJET CMS. Zvoľte **V administrácii** alebo **V dokumentácii** a zadajte hľadaný výraz. Hľadanie v dokumentácii otvorí nové okno so zadaným výrazom. Pomoc k práve otvorenej časti administrácie nájdete aj cez kontextového Pomocníka v hlavičke.

### Moje aktívne prihlásenia

Umožňuje skontrolovať vaše aktívne prihlásenia a odhlásiť sa z iného zariadenia alebo prehliadača, ktorý už nepoužívate. Je vždy v hornej časti prehľadu a nemožno ho odstrániť. Podrobnosti nájdete v časti [Prihlásenia](#prihlásenia).

### Prihlásení admini

Zobrazuje, ktorí administrátori práve pracujú v systéme. Môžete tak nájsť kolegu, s ktorým potrebujete dohodnúť úpravu obsahu, a odoslať mu email. Dostupnosť závisí od vašich oprávnení. Podrobnosti nájdete v časti [Prihlásení administrátori](#prihlásení-administrátori).

### Zmenené stránky

Pri kontrole práce na obsahu tu nájdete posledné úpravy dostupných stránok v aktuálnej doméne aj s autorom zmeny.

### Audit

Zobrazuje posledné udalosti auditu z celého servera podľa vašich oprávnení. Použite ho na kontrolu nedávnych operácií alebo pri zisťovaní, čo predchádzalo problému.

### Obsadenosť pamäte

Zobrazuje graf a číselné hodnoty využitia pamäte servera. Správca tak môže sledovať jej obsadenosť, napríklad pri preverovaní spomalenia systému. Údaje viditeľného widgetu sa aktualizujú každých 5 sekúnd. Podrobnosti nájdete v časti [Zmenené stránky, audit a monitorovanie](#zmenené-stránky-audit-a-monitorovanie).

### Zaťaženie CPU

Zobrazuje graf a číselné hodnoty zaťaženia procesora servera. Správca môže sledovať, či sa počas pomalých odoziev zvyšuje aj zaťaženie CPU. Údaje viditeľného widgetu sa aktualizujú každých 5 sekúnd. Podrobnosti nájdete v časti [Zmenené stránky, audit a monitorovanie](#zmenené-stránky-audit-a-monitorovanie).

## Prihlásenia

V hornej časti prehľadu môžete skontrolovať svoje aktívne prihlásenia a odhlásiť sa z iného zariadenia alebo prehliadača. Zoznam ostatných prihlásených administrátorov je samostatný widget **Prihlásení admini**, dostupný podľa oprávnení.

### Moje aktívne prihlásenia

Panel **Moje aktívne prihlásenia** zobrazuje všetky vaše aktívne relácie, teda prihlásenia pod vaším kontom, s prehliadačom, časom a IP adresou. Pri dlhšom zozname môžete jeho obsah posúvať. Vaša aktuálna relácia je prvá a má zelenú bodku s popisom **Toto prihlásenie**. Ostatné relácie môžete odhlásiť priamo v zozname, napríklad keď ste sa zabudli odhlásiť na inom počítači. Tento panel je vždy v hornej časti a nemožno ho odstrániť.

![](sessions.png)

Pri inej vlastnej relácii môžete zvoliť **Odhlásiť túto reláciu**. V aktuálnom uzle clustra sa ukončí okamžite, v inom uzle sa zobrazí informácia o čakaní na synchronizáciu medzi uzlami (typicky do minúty). Doména a uzol sú uvedené v pomocnom texte po podržaní kurzora nad záznamom.

Údaje sa aktualizujú po prihlásení používateľa. Ak potrebujete častejšiu aktualizáciu, správca môže pridať [úlohu na pozadí](../../admin/settings/cronjob/README.md) s názvom `sk.iway.iwcm.stat.SessionClusterService` a nastaviť interval, napríklad každých 10 minút.

Úloha na pozadí z databázy zmaže záznamy staršie ako 60 minút. Ak nie je nastavená, pri prihlásení používateľa sa zmažú záznamy staršie ako 24 hodín.

### Prihlásení administrátori

Ak máte právo "Úvod - zobrazenie prihlásených administrátorov", môžete cez katalóg pridať widget **Prihlásení admini** so zoznamom všetkých prihlásených administrátorov. Máte tak prehľad, koľko používateľov aktuálne pracuje v administrácii.

Kliknutím na ikonu <i class="ti ti-mail fs-6"></i> môžete danému administrátorovi odoslať email.

## Vaše skratky

Skratkami pod privítaním otvoríte aj vnorenú časť administrácie alebo konkrétny priečinok vo Web stránkach. Často používané miesta tak nemusíte pri každom návrate hľadať v menu alebo rozbaľovať strom priečinkov.

Skratka môže smerovať na dostupný modul administrácie alebo na vlastnú URL adresu vrátane parametrov. Pri odkaze s parametrami otvorí cieľ v stave, ktorý daná adresa určuje, napríklad s vybraným priečinkom. Skratky sa ukladajú na vaše konto a sú dostupné vo všetkých prehliadačoch.

Skratky predvolene smerujú na webové stránky a formuláre podľa vašich oprávnení. Ak tieto moduly nie sú dostupné, zobrazí sa skratka na prvý dostupný modul.

Novú skratku pridáte takto:

1. Kliknite na **Pridať skratku** priamo za zoznamom skratiek.
2. Do poľa **Kam má skratka viesť?** napíšte názov sekcie, karty alebo webovej stránky. Výsledky zobrazujú ikonu, názov a cestu; cieľ vyberte kliknutím alebo šípkami a klávesom **Enter**. Pre vlastný odkaz kliknite na **Použiť vlastnú adresu URL…** a vyplňte **URL adresu**.
3. Podľa potreby vyplňte **Názov skratky**. Ak zostane prázdny, použije sa názov vybraného cieľa. Pri vlastnej URL adrese je názov povinný.
4. Vyberte **Ikonu** a **Farbu**. Prvá ikona vychádza z vybraného cieľa. Voľba **Vlastná…** umožňuje zadať názov ikony z knižnice Tabler; náhľad sa zmení hneď a neexistujúci názov nemožno uložiť. Farba sa použije iba na pozadie ikony. Paleta obsahuje aj možnosť **Bez farby**.
5. Skontrolujte náhľad a kliknite na **Pridať skratku**.

Dlhé názvy sa skrátia trojbodkou. Celý názov sa zobrazí pri podržaní myši alebo pri zameraní klávesnicou. Skratka na nedostupnú položku menu zostáva zobrazená s vysvetlením; v režime úprav ju môžete opraviť alebo odstrániť.

Režim úprav zapnete tlačidlom s ceruzkou **Upraviť skratky** za zoznamom. Ceruzku nahradí tlačidlo **Hotovo** napravo od privítania, nad skratkami. Kliknutie na skratku potom otvorí jej nastavenia. Po zmene cieľa, názvu, ikony alebo farby kliknite na **Uložiť zmeny**. Oznámenie ponúkne **Späť** na 8 sekúnd. Rovnaký formulár obsahuje aj tlačidlo **Odstrániť skratku**. Po dokončení úprav kliknite na **Hotovo**.

Skratku presuniete úchytom pred ikonou. Počas presunu zostáva na pôvodnom mieste prerušovaný obrys a biela čiara označuje miesto vloženia. Pri ovládaní klávesnicou presuňte fokus na úchyt: **medzerník** skratku zdvihne, **šípky vľavo/vpravo** zmenia pozíciu, **Enter** potvrdí presun a **Esc** ho zruší. Kliknutím na úchyt môžete pozíciu vybrať aj v dialógovom okne.

![](dashboard-shortcut-settings.png)

Ak chcete skratku na konkrétny priečinok, otvorte ho vo Web stránkach a skopírujte adresu z adresného riadka prehliadača aj s parametrom `groupid`. Pri pridávaní skratky zvoľte **Použiť vlastnú adresu URL…**, vložte skopírovanú adresu a zadajte názov, napríklad **Aktuality**.

Adresa môže mať napríklad tvar `/admin/v9/webpages/web-pages-list/?groupid=123`, kde `123` je ID požadovaného priečinka. Použite ID zo svojej skopírovanej adresy. Kliknutím na takúto skratku otvoríte priamo daný priečinok, aj keď je vnorený hlbšie v štruktúre webu.

Krížik **×** odstráni skratku bez potvrdenia. Oznámenie ponúkne **Späť** na 8 sekúnd. Odstrániť môžete aj poslednú skratku; zobrazí sa **Žiadna skratka** a tlačidlo na pridanie. Widgety, ich filtre aj stav noviniek zostávajú zachované.

## Zmenené stránky, audit a monitorovanie

Pri kontrole práce s obsahom a stavu servera môžete využiť widgety **Zmenené stránky**, **Audit**, **Prihlásení admini**, **Obsadenosť pamäte** a **Zaťaženie CPU**. Podľa vašich oprávnení si ich pridajte cez katalóg. Tlačidlo **Obnoviť** v úprave prehľadu nastaví predvolený výber widgetov uvedený v úvode.

**Zmenené stránky** zobrazujú posledné úpravy dostupných stránok v aktuálnej doméne aj s autorom zmeny. **Audit** zobrazuje posledné udalosti auditu. Audit, prihlásení administrátori a monitorovanie zobrazujú údaje celého servera podľa príslušných oprávnení.

Grafy pamäte a CPU majú farebne odlíšené pozadie a predvolenú veľkosť **3×3**. V nastaveniach widgetu môžete zvoliť aj menšiu veľkosť **3×2**. Grafy aj číselné hodnoty viditeľného widgetu sa aktualizujú každých 5 sekúnd, takže môžete priebežne sledovať zmeny zaťaženia. Kliknutím na názov widgetu otvoríte úplný prehľad.

Živé vzorky sa zbierajú od otvorenia widgetu, aj keď je ukladanie historického monitorovania vypnuté. Pri skrytí karty prehliadača alebo widgetu sa pravidelné načítavanie pozastaví.

## Spätná väzba

Pomocou spätnej väzby môžete vývojovému tímu WebJET CMS poslať pripomienku, návrh na zlepšenie alebo pochvalu. Kliknite na **Zaslať spätnú väzbu** v hornej lište úvodného prehľadu. Vyplnený formulár sa odošle emailom.

![](feedback.png)

Vaše pripomienky posúdime pri plánovaní ďalšieho vývoja. Plánované zmeny nájdete v [mape rozvoja](../../ROADMAP.md).

V dialógovom okne opíšte, čo potrebujete zmeniť alebo pri akej práci ste narazili na problém. Môžete priložiť aj súbory, napríklad snímku obrazovky alebo dokument s opisom požiadavky.

![](feedback-modal.png)

Ak zvolíte možnosť **Zaslať anonymne**, odoslaný email nebude obsahovať vaše meno a emailovú adresu v údajoch odosielateľa.
