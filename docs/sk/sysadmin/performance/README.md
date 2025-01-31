# Výkon servera

Pre optimálny výkon servera je potrebné splniť viacero požiadaviek a nastavení. Každá vložená aplikácia (napr. foto galéria, anketa atď.) do web stránky spôsobuje spomalenie. Aplikácie typicky vykonávajú dodatočné databázové požiadavky, alebo potrebujú čítať údaje zo súborového systému.

Výrazný vplyv na výkon môžu mať aj vyhľadávače, ktoré neustále prechádzajú a indexujú web stránky na vašom serveri. Ich návštevnosť nemusí byť vidieť napr. v Google Analytics, ale je vidieť v [štatistike](../../redactor/apps/stat/README.md), ktorú poskytuje WebJET CMS.

## Identifikácia problémov

Ako prvé je potrebné identifikovať miesta, kde dochádza k spomaleniu. Ak viete na prvý pohľad identifikovať web stránku, ktorá sa vám zdá pomalá, môžete použiť URL parameter `?_writePerfStat=true`. Inak zapnite monitorovanie servera, v ktorom viete identifikovať web stránky, ktoré sú vykonávané najdlhšie.

### URL parametrom

Pomocou URL parametra `?_writePerfStat=true` je možné získať výpis aplikácií vložených vo web stránke s časom ich vykonania. Napríklad stránku `/sk/` zobrazíte ako `/sk/?_writePerfStat=true`.

Pri takomto zobrazení web stránky sa do HTML kódu vloží po každej aplikácii výraz typu `PerfStat: 3 ms (+3) !INCLUDE(...)`. V štandardne zobrazenej web stránke nemusí byť dobre vyhľadateľný, preto odporúčame zobraziť zdrojový kód stránky - v Chrome menu Zobraziť-Vývojár-Zobraziť zdrojový kód. Následne použite vyhľadávanie v prehliadači výrazu `PerfStat:`.

Tento výraz je vo formáte `PerfStat: 3 ms (+3)` kde prvé číslo znamená celkový čas vykonania jedného `iwcm:write` výrazu a číslo v zátvorke je čas vykonania tejto aplikácie. Následne nasleduje cesta k aplikácii a jej parametre. Vás teda zaujíma primárne číslo v zátvorke.

Pomocou URL parametra `_disableCache=true` je možné vypnúť vyrovnávaciu pamäť aplikácií.

### Monitorovanie servera

Pre komplexný pohľad môžete zapnúť funkciu [monitorovanie servera](../monitoring/README.md) nastavením nasledovných konfiguračných premenných:

- `serverMonitoringEnable` - zapne funkciu monitorovania servera a zaznamenávania hodnôt
- `serverMonitoringEnablePerformance` - zapne funkciu monitorovania výkonu aplikácií a web stránok
- `serverMonitoringEnableJPA` - zapne funkciu monitorovania SQL požiadaviek

!>**Upozornenie:** monitorovanie výkonu aplikácií a SQL požiadaviek zaťažuje server, neodporúčame mať túto funkciu permanentne zapnutú.

Po nastavení konfiguračných premenných je potrebné vykonať **reštart aplikačného servera**, aby sa pri inicializácii aktivovalo sledovanie výkonu.

Následne v sekcii Monitorovanie servera - Aplikácie/WEB stránky/SQL dotazy viete identifikovať časti, ktoré sa dlho vykonávajú. Zamerajte sa na najčastejšie vykonávané aplikácie/SQL dotazy a ich optimalizáciu.

### Celkový čas generovania web stránky

Existuje aplikácia `/components/_common/generation_time.jsp` ktorú ak vložíte do pätičky šablóny web stránky vygeneruje do HTML kódu celkový čas generovania web stránky.

Je možné nastaviť nasledovné parametre aplikácie:

- `hide` - predvolene `true` - čas generovania sa zobrazí ako komentár v HTML kóde
- `onlyForAdmin` - predvolene `false` - čas generovania sa zobrazí len ak je prihlásený administrátor

Do pätičky (alebo vhodného voľného poľa) šablóny web stránky vložte nasledovný kód:

```html
!INCLUDE(/components/_common/generation_time.jsp, hide=true, onlyForAdmin=false)!
```

Na mieste vloženej aplikácie sa zobrazí informácia o čase vykonania celej web stránky v ms:

```html
<!-- generation time: 4511 ms -->
```

## Meranie výkonu databázového servera a súborového systému

Pre porovnanie výkonu prostredí - napr. testovacie VS produkčné prostredie je možné použiť nižšie uvedené skripty. Ich spustenie vyžaduje právo na aktualizáciu WebJET. Merať a porovnávať prostredia môžete bez záťaže, ale aj počas prevádzky, alebo výkonnostných testov.

- `/admin/update/dbspeedtest.jsp` - meria výkon čítania údajov z databázového servera.

Dobré hodnoty sú napríklad:

```html
Image read, count=445
...
Total time: 649 ms, per item: 1.4584269662921348 ms
Total bytes: 4.8050469E7, per second: 7.403770261941448E7 B/s

Random web page read, count=3716
...
Total time: 3608 ms, per item: 0.9709364908503767 ms
Total bytes: 1371566.0, per second: 380145.78713968955 B/s

Only documents.data web page read, count=3716
...
Total time: 2205 ms, per item: 0.5933799784714747 ms
Total bytes: 685783.0, per second: 311012.6984126984 B/s

Documents read using web page API, count=3716
...
Total time: 1869 ms, per item: 0.5029601722282023 ms
Total bytes: 685783.0, per second: 366925.09363295883 B/s
```

Z dôvodu rozdielneho počtu záznamov v databáze je potrebné porovnávať `per item` hodnoty.

- `/admin/update/fsspeedtest.jsp` - kontroluje rýchlosť čítania zoznamu súborov zo súborového systému, je potrebné overiť hlavne ak používate sieťový súborový systém.

Dobré hodnoty sú napríklad:

```html
Testing mime speed, start=0 ms
has base file object, fullPath=/Users/jeeff/Documents.nosync/workspace-visualstudio/webjet/webjet8v9-hotfix/src/main/webapp/components/_common/mime diff=1 ms
listFiles, size=678, diff=284 ms
listing done, diff=16 ms


Testing modinfo speed, start=0 ms
modinfo list, size=102, diff=1 ms
modinfo listing done, diff=220 ms
Total time=522ms
```

## Optimalizácia databázových požiadaviek

Optimalizovať počet databázových požiadaviek je možné zapnutím vyrovnávacej pamäte - `cache`.

### Web stránky

Každá web stránka má v karte Základné možnosť **Povoliť uloženie stránky do vyrovnávacej pamäte**. Zapnutím tejto možnosti sa obsah web stránky z tabuľky `documents` uloží do vyrovnávacej pamäte. Pri zobrazení web stránky nebude potrebné vykonať databázové volanie pre získanie obsahu web stránky.

Túto možnosť odporúčame zapnúť na najviac navštevovaných web stránkach, ktorých zoznam získate v aplikácii [štatistika](../../redactor/apps/stat/README.md#top-stránky).

![](../../redactor/webpages/editor/tab-basic.png)

### Aplikácie

Podobne ako pre web stránky je možné zapnúť vyrovnávaciu pamäť aj pre aplikácie. Niektoré aplikácie majú túto možnosť dostupnú priamo v [nastavení aplikácie](../../custom-apps/appstore/README.md#karta-zobrazenie) vloženej vo web stránke v karte Zobrazenie ako pole **Čas vyrovnávacej pamäte**.

![](../../custom-apps/appstore/common-settings-tab.png)

Ak aplikácia toto nastavenie nemá dostupné stále môžete parameter nastaviť v HTML kóde textu web stránky pridaním parametra `, cacheMinutes=xxx` k parametrom vloženej aplikácie, napríklad:

```html
!INCLUDE(sk.iway.iwcm.components.reservation.TimeBookApp, reservationObjectIds=2560+2561, device=, cacheMinutes=10)!
```

!>**Upozornenie:** je potrebné si uvedomiť, že vyrovnávacia pamäť je globálna pre celý aplikačný server. Ako kľúč sa použije cesta k súboru aplikácie, jednotlivé parametre zadané v HTML kóde web stránky a jazyk aktuálne zobrazenej web stránky. Neberú sa do úvahy URL parametre web stránky.

Vyrovnávaciu pamäť nie je teda možné použiť napríklad ak sa zobrazí napríklad stránkovanie zoznamu kde číslo strany sa prenáša pomocou URL parametra. Aby ale bolo možné uložiť zoznam noviniek existuje výnimka - pre aplikácie obsahujúce v názve súboru `/news/news` sa použije vyrovnávacia pamäť iba ak v URL adrese nie je zadaný parameter `page`, respektíve hodnota tohto parametra je iná ako `1`. Takto sa vyrovnávacia pamäť použije aj pre zoznam noviniek, ale uloží sa do nej len prvá strana výsledkov. Ďalšie strany nie sú ukladané.

## Optimalizácia súborového systému

Web stránky typicky obsahujú veľa doplnkových súborov - obrázky, CSS štýly, JavaScript súbory a podobne, ktoré je potrebné spolu s web stránkou načítať. Rýchlosť zobrazenia teda závisí aj od počtu a veľkosti týchto súborov.

### Nastavenie vyrovnávacej pamäte

Pre súbory web stránky je možné nastaviť použitie vyrovnávacej pamäte v prehliadači - súbor sa tak nebude opakovane čítať pri každom zobrazení web stránky, ale ak ho už má prehliadač vo vyrovnávacej pamäti, tak sa použije. Zrýchli sa tak zobrazenie web stránky a zníži záťaž na server. Príkladom je obrázok loga, ktorý je typicky na každej stránke, ale jeho zmena je vysoko nepravdepodobná - respektíve mení sa rádovo raz za niekoľko mesiacov.

Je možné nastaviť nasledovné konfiguračné premenné, ktoré ovplyvňujú HTTP hlavičku `Cache-Control`:

- `cacheStaticContentSeconds` - nastavený počet sekúnd, predvolene `300`.
- `cacheStaticContentSuffixes` - zoznam prípon, pre ktoré sa HTTP hlavička `Cache-Control` vygeneruje, predvolene `.gif,.jpg,.png,.swf,.css,.js,.woff,.svg,.woff2`.

Pre presnejšie nastavenie je možné použiť aplikáciu [HTTP hlavičky](../../admin/settings/response-header/README.md), kde môžete nastaviť rozdielne hodnoty pre rozdielne URL adresy.

![](../../admin/settings/response-header/editor-wildcard.png)

## Správanie pre administrátora

Ak je prihlásený administrátor vyrovnávacia pamäť pre aplikácie sa nepoužije (predpokladá sa, že administrátor vždy chce vidieť aktuálny stav).

Toto správanie je možné zmeniť nastavením konfiguračnej premennej `cacheStaticContentForAdmin` na hodnotu `true`. Vhodné je túto hodnotu nastaviť hlavne pre intranet inštalácie, kde sa používatelia overujú voči `SSO/ActiveDirectory` serveru a aj pri bežnej práci v intranet prostredí majú práva administrátora.

## Vyhľadávače

Vyhľadávače a rôzne iné roboty môžu výrazne zaťažiť server. Zvlášť s nástupom učenia umelej inteligencie dochádza k výraznému prehľadávaniu internetu a plneniu databáz pre učenie umelej inteligencie. Roboty často skúšajú rôzne URL parametre pre získanie dodatočných dát.

### Nastavenie robots.txt

Správanie robotov je možné ovplyvniť nastavením v súbore `/robots.txt`. Tento ak neexistuje je generovaný v predvolenom stave. Vami upravenú verziu umiestnite do `/files/robots.txt`, z tejto lokality ho WebJET zobrazí pri volaní `/robots.txt`.

Pomocou súboru [robots.txt](https://en.wikipedia.org/wiki/Robots.txt) môžete ovplyvniť správanie robotov a vyhľadávačov - obmedziť URL adresy, ktoré môžu použiť, nastaviť odstup medzi požiadavkami atď.

## Ostatné nastavenia

### Reverzný DNS server

Štatistika, audit a ďalšie aplikácie môžu získavať reverzný DNS záznam z IP adresy. Používa sa API volanie `InetAddress.getByName(ip).getHostName()`. Na serveroch/v DMZ nemusí ale byť DNS server dostupný a toto volanie môže trvať niekoľko sekúnd kým nastane chyba. Všeobecne takéto volanie spomaľuje vykonanie HTTP požiadavky.

Nastavením konfiguračnej premennej `disableReverseDns` na hodnotu `true` je možné vypnúť získavanie DNS názvu z IP adresy návštevníka a zrýchliť vykonávanie požiadaviek. Do poľa pre hodnotu `hostname` sa vtedy zapíše hodnota IP adresy.

### Vypnutie štatistiky

Zapisovanie údajov štatistiky je asynchrónne, vykonáva sa v dávkach tak, aby zobrazenie web stránky nečakalo na zápis údajov štatistiky do databázy.

Pri vysokej návštevnosti, alebo hľadaní problémov s výkonom môžete dočasne vypnúť zapisovanie štatistík návštevnosti nastavením konfiguračnej premennej `statMode` na hodnotu `none`. Štandardná hodnota je `new`.