# Úvodná obrazovka

Úvodná obrazovka je osobný prehľad zložený z widgetov. Ponuka zodpovedá vašim oprávneniam - obsahové údaje sa vzťahujú na práve zvolenú doménu. Rozloženie je spoločné pre vaše konto vo všetkých doménach a prehliadačoch. Konkrétny výber formulára alebo kampane sa pamätá osobitne pre každú doménu.

Pri prvom otvorení sa podľa vašich oprávnení zobrazia predvolené widgety **Návštevnosť**, **Formuláre**, **Na schválenie**, **Chyby 404**, **Pokračujte v práci**, **Odkiaľ návštevníci prišli**, **Najbližšie publikovanie** a **Newsletter**. Ďalšie widgety si môžete pridať cez katalóg. V hornej časti je privítanie so skratkami a novinkami, aktívne prihlásenia a vyhľadávanie. Skratky predvolene smerujú na webové stránky a formuláre podľa vašich práv. Ak tieto moduly nie sú dostupné, zobrazí sa skratka na prvý dostupný modul.

![](dashboard.png)

Pri privítaní sa zobrazuje aj označenie prostredia, napríklad **PROD**, **UAT**, **INT** alebo **DEV**. Správca môže upraviť jeho text, ikonu, farbu a obrázok pozadia podľa [konfigurácie úvodnej obrazovky](../../admin/setup/configuration/dashboard.md).

## Prispôsobenie prehľadu

Tlačidlom **Upraviť prehľad** zobrazíte možnosti **Pridať widget**, **Obnoviť** a **Hotovo**.

![](dashboard-edit.png)

Tlačidlo **Pridať widget** otvorí katalóg.

![](dashboard-catalogue.png)

Pri widgete použite menu s tromi bodkami:

- **Nastavenia widgetu**: zvoľte dostupnú veľkosť a ďalšie údaje, napríklad obdobie alebo formulár. Nie každý widget ponúka viac veľkostí.
- **Presunúť widget**: vyberte, pred ktorú kartu sa má presunúť, alebo zvoľte koniec. Na počítači môžete použiť aj rukoväť na ťahanie.
- **Odstrániť widget**: odstráni kartu z vášho prehľadu. Údaje v aplikácii zostávajú zachované. Bezprostredne po odstránení je dostupné **Vrátiť späť**, po ďalšej úspešne uloženej úprave táto možnosť zanikne. Widget môžete kedykoľvek pridať znova.
- **Obnoviť údaje**: načíta čerstvé údaje danej karty.

Dialóg nastavení widgetu:

![](dashboard-widget-settings.png)

Na menšej obrazovke sa karty automaticky usporiadajú pod seba pri zachovaní poradia. Rozloženie môže obsahovať najviac 48 položiek vrátane skratiek, prihlásení, noviniek a vyhľadávania.

Tlačidlo **Obnoviť** nájdete v lište úprav za tlačidlom **Pridať widget**. Po potvrdení v dialógu sa obnoví predvolený výber widgetov, ich veľkosti, poradie a nastavenia. Vymažú sa aj filtre widgetov vo všetkých doménach a potvrdenie prečítania noviniek. Vaše skratky a ostatné nastavenia účtu zostanú zachované.

Ak pri kliknutí na **Obnoviť** podržíte kláves **Shift**, po potvrdení sa prehľad nahradí všetkými dostupnými widgetmi v každej podporovanej veľkosti. Nepotrebné varianty môžete následne odstrániť.

## Dostupné informácie

Widgety v môžete pridať opakovane s rôznymi veľkosťami alebo nastaveniami. Ich dostupnosť závisí od vašich oprávnení.

Štatistické widgety predvolene používajú posledných sedem ukončených dní, možno zvoliť aj 30 alebo 90 dní. Porovnanie používa rovnako dlhé predchádzajúce obdobie. Karta zobrazuje skutočný rozsah dátumov. Osobitné pravidlá pre obdobie formulárov a chýb 404 sú uvedené pri príslušných widgetoch.

### Skratka do modulu

Otvorí vybranú sekciu administrácie.

### Pokračujte v práci

Zobrazuje najviac šesť vašich posledných upravovaných stránok s náhľadom obrázka, umiestnením a dátumom úpravy. Kliknutím otvoríte editor stránky. Pri menšej veľkosti karty môžete zoznam posúvať.

### Na schválenie

Zobrazuje požiadavky dostupné v karte **Neschválené** vo Web stránkach. Väčší variant obsahuje šesť najnovších požiadaviek na zmenu alebo zmazanie stránky či priečinka. Ikona pri názve rozlišuje stránku a priečinok.

Kliknutím na položku otvoríte jej schvaľovanie v novom okne. Pri priečinku sa otvorí príslušný priečinok vo Web stránkach spolu so schvaľovacím dialógom. Nadpis a celkový počet otvoria celý zoznam požiadaviek.

### Najbližšie publikovanie

Zobrazuje plánované publikovanie a ukončenie platnosti stránok s dátumom a časom zmeny. Kliknutím na nadpis otvoríte úplný plán publikovania.

### Formuláre

Zobrazuje odoslané formuláre za posledných 7, 30 alebo 90 dní vrátane dnešných odoslaní. V nastaveniach môžete vybrať konkrétny formulár alebo všetky dostupné formuláre.

### Návštevnosť

Zobrazuje návštevnosť za zvolené obdobie. V nastaveniach môžete vybrať počet zobrazení, návštev alebo unikátnych návštevníkov.

Graf porovnáva aktuálne a predchádzajúce obdobie. Presné hodnoty nájdete v popisoch po podržaní kurzora nad grafom. Tabuľka s údajmi je dostupná čítačkám obrazovky.

### Najnavštevovanejšie stránky

Zobrazuje najnavštevovanejšie stránky za zvolené obdobie s počtom zobrazení. Kliknutím na stránku otvoríte jej podrobnú štatistiku.

### Čo návštevníci hľadajú

Zobrazuje hľadané výrazy a ich počty za zvolené obdobie.

### Odkiaľ návštevníci prišli

Zobrazuje zdroje návštevnosti za zvolené obdobie. Graf zobrazuje percentuálny podiel jednotlivých evidovaných zdrojov. Presné hodnoty nájdete v popisoch po podržaní kurzora nad grafom. Tabuľka s údajmi je dostupná čítačkám obrazovky.

### Chyby 404

Zobrazuje počet chybových požiadaviek, nie počet rôznych adries. Chyby sa evidujú po týždňoch, preto sa zahrnú celé týždne zasahujúce do zvoleného obdobia. Karta zobrazí skutočný rozsah dátumov a aktuálny týždeň obsahuje údaje dostupné do tohto okamihu.

Ak historické údaje nemožno oddeliť podľa domény, karta oznámi ich nedostupnosť.

### Newsletter

Zobrazuje stav hromadného emailu. Automaticky vyberá aktívnu kampaň, najbližšiu naplánovanú alebo poslednú dokončenú. Môžete zvoliť aj konkrétnu kampaň.

Pri odosielaní sa údaje viditeľnej karty obnovujú každých 30 sekúnd. Počty otvorení a kliknutí predstavujú zaznamenané udalosti.

### Čo je nové

Novinky aktuálnej verzie sú v uvítacom paneli. Tlačidlom **Zbaliť novinky** potvrdíte ich prečítanie a ponecháte stručný súhrn s tlačidlom **Viac info** na opätovné rozbalenie. Systémové upozornenia zostávajú viditeľné do vyriešenia ich príčiny. Kliknutím na nadpis upozornenia rozbalíte jeho vysvetlenie a dostupnú nápravnú akciu.

### Vyhľadávanie a pomoc

Umožňuje hľadať **V administrácii** alebo **V dokumentácii**. Hľadanie v dokumentácii otvorí nové okno so zadaným výrazom. Kontextový Pomocník v hlavičke zostáva dostupný.

### Moje aktívne prihlásenia

Zobrazuje vaše aktívne relácie a umožňuje odhlásiť ostatné relácie. Zostáva vždy v hornej časti prehľadu a nemožno ho odstrániť. Podrobnosti nájdete v časti [Prihlásenia](#prihlásenia).

### Prihlásení admini

Zobrazuje zoznam všetkých prihlásených administrátorov podľa vašich oprávnení. Podrobnosti nájdete v časti [Prihlásení administrátori](#prihlásení-administrátori).

### Zmenené stránky

Zobrazuje posledné úpravy dostupných stránok v aktuálnej doméne aj s autorom zmeny.

### Audit

Zobrazuje posledné udalosti auditu z celého servera podľa vašich oprávnení.

### Obsadenosť pamäte

Zobrazuje graf a číselné hodnoty využitia pamäte servera. Údaje viditeľného widgetu sa aktualizujú každých 5 sekúnd. Podrobnosti nájdete v časti [Zmenené stránky, audit a monitorovanie](#zmenené-stránky-audit-a-monitorovanie).

### Zaťaženie CPU

Zobrazuje graf a číselné hodnoty zaťaženia procesora servera. Údaje viditeľného widgetu sa aktualizujú každých 5 sekúnd. Podrobnosti nájdete v časti [Zmenené stránky, audit a monitorovanie](#zmenené-stránky-audit-a-monitorovanie).

## Prihlásenia

Vaše aktívne prihlásenia zostávajú v hornej časti prehľadu. Zoznam ostatných prihlásených administrátorov je samostatný widget **Prihlásení admini**, dostupný podľa oprávnení.

### Moje aktívne prihlásenia

Panel **Moje aktívne prihlásenia** zobrazuje všetky vaše aktívne relácie s prehliadačom, časom a IP adresou. Pri dlhšom zozname môžete jeho obsah posúvať. Vaša aktuálna relácia je prvá a má zelenú bodku s popisom **Toto prihlásenie**. Ostatné relácie môžete odhlásiť priamo v zozname. Tento panel zostáva vždy v hornej časti a nemožno ho odstrániť.

![](sessions.png)

Pri inej vlastnej relácii môžete zvoliť **Odhlásiť túto reláciu**. V aktuálnom uzle clustra sa ukončí okamžite, v inom uzle sa zobrazí informácia o čakaní na synchronizáciu medzi uzlami (typicky do minúty). Doména a uzol sú uvedené v pomocnom texte po podržaní kurzora nad záznamom.

Poznámka: údaje sa aktualizujú po prihlásení používateľa. Môžete nastaviť nový záznam do [úlohy na pozadí](../../admin/settings/cronjob/README.md) na častejšiu aktualizáciu údajov, kde ako názov úlohy zadáte hodnotu `sk.iway.iwcm.stat.SessionClusterService`. Interval zadajte podľa potreby, napr. každých 10 minút. Pri úlohe na pozadí sa z databázy zmažú záznamy staršie ako 60 minút. Ak nie je úloha na pozadí nastavená, záznamy sa mažú pri prihlásení používateľa, ak sú staršie ako 24 hodín.

### Prihlásení administrátori

Ak máte právo "Úvod - zobrazenie prihlásených administrátorov", môžete cez katalóg pridať widget **Prihlásení admini** so zoznamom všetkých prihlásených administrátorov. Máte tak prehľad, koľko používateľov aktuálne pracuje v administrácii.

Kliknutím na ikonu <i class="ti ti-mail fs-6"></i> môžete danému administrátorovi odoslať email.

## Vaše skratky

Skratky pod privítaním nahrádzajú pôvodné záložky. Tlačidlom **Upraviť skratky** zobrazíte možnosti **Pridať skratku**, **Obnoviť** a **Hotovo**. Pri pridávaní vyberiete dostupný modul administrácie alebo zadáte vlastnú URL a názov. Môžete nastaviť aj ikonu a farbu pozadia. Skratky sa ukladajú na vaše konto a sú dostupné vo všetkých prehliadačoch.

![](dashboard-shortcut-settings.png)

Tlačidlo **Obnoviť** v úprave skratiek obnoví predvolené odkazy a zachová widgety, ich filtre aj stav noviniek. Po odstránení všetkých skratiek zostane zoznam prázdny až do pridania novej skratky alebo obnovy predvolených odkazov.

## Zmenené stránky, audit a monitorovanie

Pôvodná sekcia **Ďalšie prehľady** bola odstránená. Jej informácie sú dostupné ako samostatné widgety **Zmenené stránky**, **Audit**, **Prihlásení admini**, **Obsadenosť pamäte** a **Zaťaženie CPU**. Podľa vašich oprávnení si ich môžete pridať cez katalóg. Bežná obnova prehľadu používa predvolený výber widgetov uvedený v úvode.

**Zmenené stránky** zobrazujú posledné úpravy dostupných stránok v aktuálnej doméne aj s autorom zmeny. **Audit** zobrazuje posledné udalosti auditu. Grafy pamäte a CPU majú farebne odlíšené pozadie a predvolenú veľkosť **3×3**, aby boli lepšie čitateľné. V nastaveniach widgetu môžete zvoliť aj kompaktnejšiu veľkosť **3×2**. Grafy aj číselné hodnoty viditeľného widgetu sa aktualizujú každých 5 sekúnd. Živé vzorky sa zbierajú od otvorenia widgetu, aj keď je ukladanie historického monitorovania vypnuté. Pri skrytí karty prehliadača alebo widgetu sa pravidelné načítavanie pozastaví. Úplný prehľad otvoríte cez názov widgetu. Audit, prihlásení administrátori a monitorovanie zobrazujú údaje celého servera podľa príslušných oprávnení.

## Spätná väzba

Tlačidlo **Zaslať spätnú väzbu** nájdete v hornej lište úvodného prehľadu.

Kliknutím na tlačidlo Zaslať spätnú väzbu môžete nám, programátorom, zaslať vašu spätnú väzbu k používaniu WebJET CMS. Pripomienka sa odošle po vyplnení formuláru emailom.

Vaše pripomienky posúdime a pridáme do [mapy rozvoja](../../ROADMAP.md). Môžete tak aj vašim názorom zlepšiť fungovanie WebJET CMS.

![](feedback.png)

V dialógovom okne môžete zadať text vašej pripomienky, komentár, alebo pochvalu. V prípade potreby môžete priložiť aj súbory (napr. fotku obrazovky, alebo dokument s opisom vašej požiadavky).

![](feedback-modal.png)

Ak zvolíte možnosť Zaslať anonymne nebude do odoslaného emailu zadané vaše meno a emailová adresa ako meno a email odosielateľa.
