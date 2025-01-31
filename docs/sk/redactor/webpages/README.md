# Web stránky

## Základná práca

### Výber domény

Vo vrchnej časti sa pre viac doménové web sídla zobrazuje výber domény. V stromovej štruktúre stránok sa zobrazujú len priečinky so zvolenej domény a priečinky, ktoré nemajú doménu nastavenú.

![](domain-select.png)

### Zobrazenie priečinkov Systém a Kôš

Špeciálne priečinky Systém (obsahuje stránky potrebné pre šablónu ako hlavička a pätička) a Kôš (obsahuje zmazané stránky) sa zobrazujú v karte Systém alebo Kôš.

Zobrazenie v karte Systém je závislé od konfigurácie WebJETu:

- štandardne zobrazí obsah priečinka ```/System``` (globálny priečinok pre všetky domény)
- ak je zapnutý režim **lokálneho Systém priečinka** (nastavená konfiguračná premenná ```templatesUseDomainLocalSystemFolder``` na ```true```) a pre aktuálne zvolenú doménu existuje lokálny System priečinok zobrazí jeho obsah
- ak je zapnutý režim hľadania Systém priečinka **rekurzívne v stromovej štruktúre** (nastavená konfiguračná premenná ```templatesUseRecursiveSystemFolder``` na ```true```) zobrazí priečinkovú štruktúru obsahujúcu priečinok System

Okrem Systém priečinka sa v tejto karte zobrazuje aj priečinok `/files` s plno textovým indexom pre vyhľadávanie v súboroch (ak je plno textové vyhľadávanie aktívne). V tomto priečinku sa nachádzajú texty získané zo súborov, pričom text sa používa pri vyhľadávaní v súboroch.

V karte Kôš sa zobrazuje obsah priečinka ```/System/Kôš```.

Tieto priečinky sa nezobrazujú v karte Priečinky (sú filtrované), ak ale z nejakého dôvodu potrebujete vidieť presnú stromovú štruktúru bez filtrovania, kliknite na kartu Priečinky so stlačenou klávesou `shift`. V takom prípade sa vypne filtrovanie a zobrazia sa všetky priečinky v aktuálne zvolenej doméne a priečinky, ktoré doménu nemajú nastavenú.

![](system-folder.png)

### Zapamätanie naposledy otvoreného priečinka

Zoznam web stránok si v rámci jedného prihlásenia pamätá naposledy otvorený priečinok, pri návrate na zoznam web stránok sa priečinok znova otvorí. Zapamätanie priečinka sa zmaže pri prepnutí domény, alebo pri zadaní adresy ```/admin/v9/webpages/web-pages-list/?groupid=0```, čiže adresa stránky s parametrom ```groupid=0```.

Zároveň pri prechádzaní stromovej štruktúry sa v adresnom riadku prehliadača zobrazuje adresa stránky s parametrom ```groupid```, ktorý reprezentuje ID priečinka. Pri obnove stránky, alebo zaslaní odkazu, sa otvorí priečinková štruktúra podľa ID v adresnom riadku. Na úvodnej stránke si môžete do bloku Záložky [pridať adresu stránky aj s ID priečinka](https://youtu.be/G5Ts04jSMX8) a vytvoriť tak na úvodnej stránke odkaz do vnorenej priečinkovej štruktúry.

### Karty web stránok

V pravej sekcii je možné zobraziť nasledovné karty:

- **Web stránky** - zobrazuje štandardný zoznam web stránok vo vybranom priečinku v stromovej štruktúre.
- **Naposledy upravené** - zobrazuje zoznam vašich posledné upravených stránok.
- **Čakajúce na schválenie** - ak schvaľujete zmeny vo web stránkach zobrazia sa v tejto karte stránky, ktoré čakajú na vaše schválenie.
- **Priečinky** - prepne zobrazenie zo zoznamu web stránok na zoznam priečinkov. Kliknutím na priečinok v stromovej štruktúre sa zobrazí zvolený priečinok a jeho pod priečinky. Ak označíte v stromovej štruktúre viaceré priečinky (napr. pomocou stlačenia klávesy ```CTRL```) zobrazia sa označené priečinky. Tabuľkové zobrazenie priečinkov umožňuje napr. vykonávať hromadné operácie s priečinkami (napr. zmena šablóny), použiť funkciu Upraviť v zobrazení mriežky alebo funkciu Duplikovať.

!>**Upozornenie**: zobrazenie priečinkov musíte najskôr zapnúť v [nastavenia zobrazenia stromovej štruktúry](#nastavenie-zobrazenia-stromovej-štruktúry).

![](../../_media/changelog/2021q1/2021-13-awaiting-approve.png)

### Zobraziť stránky aj z podadresárov

V prípade potreby môžete zobraziť web stránky aj z podadresárov prepnutím prepínača **Zobraziť stránky aj z podadresárov** v hlavičke datatabuľky. Po prepnutí do režimu zobrazenia stránok z podadresárov sa zobrazia stránky z aktuálne zvoleného adresára v stromovej štruktúre vrátane jeho podadresárov. V stromovej štruktúre môžete kliknúť na iný adresár, čo znova spôsobí zobrazenie stránok z vybraného adresára a jeho podadresárov.

V nastavení tabuľky si môžete zapnúť zobrazenie stĺpca **Nadradený priečinok** v ktorom budete vidieť adresár v ktorom sa stránka nachádza.

![](recursive-list.png)

### Obnovenie web stránok a adresárov z koša

Zoznam web stránok ponúka taktiež špeciálnu ikonu ![](recover-button.png ":no-zoom") na obnovenie web stránky alebo celého priečinka z koša. Tieto ikony sa zobrazujú iba za špecifických okolností. Bližšie objasnená logika sa nachádza v sekcii [Obnovenie web stránok a priečinkov z koša](./recover.md)

### Špeciálne ikony

Data tabuľka v zozname stránok obsahuje nasledovné špeciálne ikony:

- <i class="ti ti-eye fa-btn" role="presentation"></i> - Zobraz stránku - po označení jedného alebo viacerých riadkov a kliknutí na ikonu sa otvorí v novom okne/karte zvolená web stránka.
- <i class="ti ti-a-b fa-btn" role="presentation"></i> - Uložiť ako AB test - vytvorí B verziu stránky pre [AB testovanie](../apps/abtesting/README.md).
- <i class="ti ti-chart-line fa-btn" role="presentation"></i> - Štatistika stránky - zobrazí [návštevnosť](../apps/stat/README.md) označenej web stránky.
- <i class="ti ti-link-off fa-btn" role="presentation"></i> - Kontrola odkazov a prázdnych stránok - skontroluje [platnosť odkazov](linkcheck.md) v stránkach v aktuálnom priečinku a podpriečinkoch, zobrazí stránky, ktoré nemajú zadaný žiaden text.
- ![](icon-recursive.png ":no-zoom") - Zobraziť stránky aj z podadresárov - prepnutím prepínača do zapnutej polohy zobrazíte v tabuľke aj web stránky z podadresárov

### Ikony a farby v stromovej štruktúre a zozname stránok

V stromovej štruktúre priečinkov a stránok sa môžu zobraziť nasledovné typy ikon a farieb:

- <i class="ti ti-folder-filled" role="presentation"></i> plná ikonka priečinku = priečinok je zobrazený v menu
- <i class="ti ti-folder" role="presentation"></i> prázdna ikonka priečinku = nie je nezobrazený v menu
- <i class="ti ti-map-pin" role="presentation"></i> stránka je zobrazená v menu
- <i class="ti ti-map-pin-off" role="presentation"></i> stránka nie je zobrazená v menu
- <i class="ti ti-folder-x" role="presentation"></i> nemáte práva na editáciu/zmazanie priečinku, v tomto priečinku nebudete vidieť ani žiaden zoznam web stránok (aj keď priečinok v skutočnosti web stránky obsahuje). Používa sa v prípade, keď máte povolené práva len na niektorý z podpriečinkov.
- <i class="ti ti-lock" role="presentation"></i> zámok = dostupné len pre prihláseného návštevníka
- <span style="color: #FF4B58">červená farba</span> = nedostupné pre verejnosť (interný adresár) alebo stránka s vypnutým zobrazením
- <i class="ti ti-star"></i>, **tučné písmo** = hlavná stránka adresára
- <i class="ti ti-external-link"></i> šípka von = stránka je presmerovaná
- <i class="ti ti-eye-off"></i> preškrtnuté oko = stránka sa nedá vyhľadať
- <i class="ti ti-a-b"></i> B variant stránky pre aplikáciu [AB testovanie](../apps/abtesting/README.md)

## Nastavenie zobrazenia stromovej štruktúry

V prípade potreby môžete v stromovej štruktúre kliknutím na ikonu <i class="ti ti-adjustments-horizontal"></i> Nastavenia zobraziť dialógové okno nastavení:

- **ID** - Pred názvom zobrazí aj ID adresára vo forme #ID. Zobrazenie je vhodné, ak potrebujete manuálne do niektorej aplikácie zadať ID priečinka, alebo migrujete stránky medzi prostrediami a potrebujete rýchlo skontrolovať nastavenie vložených aplikácií.
- **Poradie usporiadania** - Za názvom zobrazí poradie usporiadania vo forme (poradie).
- **Web Stránky** - Zobrazí v stromovej štruktúre aj web stránky. **Upozornenie:** znižuje výkon a rýchlosť načítania údajov. Možnosť odporúčame zapnúť len ak potrebujete presúvať web stránky pomocou funkcie ```Drag&Drop```.
- **Priečinky stromovej štruktúry ako tabuľku** - Zobrazí kartu Priečinky v datatabuľke. Umožňuje používať funkcie datatabuľky ako hromadné operácie, duplikovať, upraviť v zobrazení mriežky atď. s priečinkami stromovej štruktúry.
- **Pomer šírky stĺpcov strom:tabuľka** - Nastaví pomer šírky stĺpcov zobrazenej stromovej štruktúry a datatabuľky pre lepšie využitie šírky monitora. Štandardný pomer je 4:8. Upozornenie: pri niektorých pomeroch a nevhodnej veľkosti monitora môže dôjsť k nesprávnemu zobrazeniu nástrojovej lišty/tlačidiel.
- **Zoradiť strom podľa** - Výber parametra adresára, podľa ktorého sa má strom priečinkov usporiadať. Výberové pole podporuje nasledujúce parametre
  - **Priorita**
  - **Názov**
  - **Dátum vytvorenie**
- **Zoradiť strom smerom** - Prepínanie medzi smerom usporiadania stromu priečinkov. Výberom možnosti sa použije smer **Vzostupne (ASC)** a nezvolením možnosti sa použije smer **Zostupne (DESC)**.

![](jstree-settings.png)

## Vyhľadávanie v stromovej štruktúre

Filter nad stromovou štruktúrou Vám umožní rýchle vyhľadávanie priečinkov podľa ich názvu. Vyhľadávanie funguje v **celej stromovej štruktúre**, čiže nie je potrebné otvárať priečinky pre ich prehľadanie. Vyhľadávanie medzi jednotlivými kartami **Priečinok** / **Systém** / **Kôš** je oddelené, čiže v karte **Systém** vyhľadávaním nenájdete priečinky patriace do karty **Kôš** atď.

![](jstree-search-form.png)

Po zadaní hodnoty do poľa sa filtrovanie spustí stlačením klávesy `Enter` alebo ikony ![](jstree-search-button.png ":no-zoom"). Pre lepší prehľad kde sa nájdený priečinok nachádza, zobrazujeme celú cestu až ku koreňovému priečinku. Každý priečinok vyhovujúci hľadanému výrazu je zvýraznený.

![](jstree-search-result.png)

Vyhľadávanie sa zruší stlačením tlačidla ![](jstree-search-cancel-button.png ":no-zoom") alebo ak skúsite vyhľadať prázdny reťazec.

!>**Upozornenie:** Ak máte aktívne vyhľadávanie (práve ste vyhľadávali nejaký reťazec) v jednej karte, tak pri prepnutí do druhej karty sa vyhľadávanie zruší. Čiže vyhľadávaný reťazec napr. "blog" sa pri prepnutí do inej karty odstráni z poľa.
