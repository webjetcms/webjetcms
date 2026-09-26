# Nová úvodná stránka WebJET CMS — etapový implementačný plán

## 1. Cieľ a pravidlá pre ďalší vývoj

Úvodná stránka sa zmení na osobný dashboard s užitočnými údajmi a skratkami. Obsah vychádza z oprávnení používateľa, aktívnej domény a jeho nastavení; nepoužívajú sa preddefinované skupiny pracovných rolí.

Prvý funkčný prírastok obsahuje widgetový systém a dva pilotné widgety. Ďalšie widgety pribudnú postupne.

**Skill `wj-dashboard-widget` vznikne na začiatku prvej etapy** v `.agents/skills/wj-dashboard-widget/`:

- `SKILL.md`: stručné záväzné pravidlá a postup vytvorenia alebo úpravy widgetu úvodnej stránky.
- `agents/openai.yaml`: názov, popis a príklad vyvolania; ponechať automatické použitie pri relevantných úlohách.
- `references/widget-contract.md`: konkrétny kontrakt registrácie, konfigurácie, načítania dát a životného cyklu, doplnený podľa skutočne vytvoreného systému.

Skill bude rozlišovať dashboardové widgety od AppStore aplikácií, PageBuilder blokov a všeobecných DataTables. Bude napísaný v angličtine podľa existujúcich vývojárskych skillov.

Postup jeho tvorby:

1. Pred implementáciou systému zachytiť schválené produktové a technické pravidlá.
2. Po vytvorení systému doplniť jeho overené rozhrania a existujúce cesty ku kódu.
3. Pred implementáciou každého pilotu skill načítať a postupovať podľa neho; v aktuálnej relácii aj priamym čítaním súboru.
4. Poznatky z pilotov zapracovať priebežne. Pridať odkazy na oba piloty ako referenčné implementácie.
5. Pri každom ďalšom widgete aktualizovať skill, ak sa objaví všeobecne platné pravidlo.

Skill nebude duplikovať celý plán ani kopírovať rozsiahly zdrojový kód. Musí vysvetľovať najmä povolené rozmery, nastavenia, oprávnenia, doménový kontext, ukladanie, bezpečné vykresľovanie, obnovovanie dát, uvoľnenie zdrojov a potrebné testy.

## 2. Widgetový systém a spoločné správanie

### Rozloženie

Použiť CSS Grid s 12 technickými stĺpcami:

| Šírka viewportu | Logické stĺpce | Správanie |
|---|---:|---|
| Od 1200 px | 6 | Jeden logický stĺpec zodpovedá dvom Bootstrap stĺpcom |
| 768–1199 px | 4 | Trojstĺpcové widgety využijú celú šírku |
| 360–767 px | 2 | Väčšie widgety využijú celú šírku |
| Pod 360 px | 1 | Všetky widgety pod sebou |

Maximálna šírka dashboardu bude 1440 px, medzery 15 px. Základná výšková jednotka približne 112 px; text musí zostať dostupný aj pri zväčšení. Mobil používa prirodzené výšky.

Podporované formáty: **1×1, 2×2, 2×3, 3×2, 3×3 a celá šírka × automatická výška**. Každý typ deklaruje iba svoje zmysluplné varianty.

Použiť prirodzené poradie gridu bez `dense`. Menšie widgety sa môžu skladať vedľa vysokých; občasné medzery sú prijateľné, aby vizuálne poradie zodpovedalo klávesnici a mobilu. Bloky s automatickou výškou oddeľujú jednotlivé gridové úseky.

### Ovládanie

- Menu widgetu ponúkne nastavenia, povolené veľkosti, presun, minimalizovanie a odstránenie.
- Presun bude možný ťahaním aj voľbou „Presunúť pred… / Na koniec“, dostupnou klávesnicou a dotykom.
- Ukladá sa poradie, nie pixelové súradnice ani samostatné mobilné rozloženie.
- Minimalizovaný widget ponechá názov a stručný súhrn; variant 1×1 sa ďalej neminimalizuje.
- Katalóg umožní pridanie a opätovné pridanie. Odstránenie zasiahne iba widget, s možnosťou okamžitého vrátenia.
- Viac inštancií podporia skratky, formuláre, štatistiky a newsletter; ostatné typy budú jedinečné.
- Moje aktívne prihlásenia možno presunúť a minimalizovať, ale nie odstrániť. Správa relácií zostane dostupná.
- Nevyriešené systémové upozornenia nemožno skryť personalizáciou.
- Novinky sa potvrdzujú pre konkrétnu verziu; v zapnutom widgete sa znovu zobrazia pri ďalšej verzii.

### Rozhrania, dáta a uloženie

Register widgetov bude deklarovať identifikátor typu, názov, ikonu, povolené varianty, predvolenú konfiguráciu, dostupnosť, podporu viacerých inštancií a spôsob načítania a vykreslenia dát.

Spoločný systém zabezpečí obal, nastavenia, presúvanie, ukladanie a stavy načítania, prázdneho výsledku a chyby. Jednotlivý widget zabezpečí svoj obsah a uvoľnenie listenerov, časovačov či grafov. Úprava jedného widgetu nesmie prekresliť celý dashboard ani odstrániť systémové hlásenia.

Používateľské nastavenia sa budú ukladať na server do existujúcej tabuľky `user_settings_admin`:

- Spoločné rozloženie naprieč doménami a zariadeniami.
- Nastavenia formulárov, priečinkov a ďalších doménových výberov oddelene podľa domény.
- `overview.layout.v1` pre verziu a poradie inštancií, `overview.widget.<id>` pre konfigurácie a `overview.news` pre potvrdené novinky.
- Najviac 32 inštancií a 2000 znakov na jeden záznam; prekročenie odmietnuť, nikdy potichu neorezávať.

Pridať `GET/PUT /admin/rest/dashboard/settings` s čerstvým čítaním a transakčným zápisom. Používateľa a doménový kontext určuje server. Pri chybe uloženia obnoviť posledný potvrdený stav a zobraziť chybu. Medzi zariadeniami platí posledné úspešné uloženie; živá synchronizácia nebude súčasťou prvej verzie.

Oprávnenia sa kontrolujú aj na dátových endpointoch. Nedostupné widgety sa skryjú bez vymazania používateľovej konfigurácie. Nové voliteľné typy pribúdajú do katalógu bez prepisovania osobného rozloženia.

## 3. Implementačné etapy

### Etapa 1 — skill, systém a dva piloty

- Vytvoriť úvodnú verziu skillu.
- Implementovať register, responzívny grid, katalóg, nastavenia, presúvanie, minimalizovanie, odstránenie a serverové ukladanie.
- Doplniť skutočný kontrakt do skillu ešte pred implementáciou pilotov.
- Podľa skillu implementovať **Skratku do modulu** a **Moje posledné stránky**.
- Výber skratky odvodiť z existujúceho menu filtrovaného oprávneniami.
- Posledné stránky filtrovať podľa práv a domény pred obmedzením počtu; doménu zahrnúť aj do prípadnej cache.
- Zachovať funkčné pôvodné prihlásenia a systémové upozornenia. Ostatné existujúce bloky ponechať v dočasnej oddelenej časti.
- Pôvodné lokálne záložky zachovať bez automatického priradenia k používateľskému účtu.

Predvolené rozloženie pilotu bude obsahovať posledné stránky a skratky na stránky/formuláre podľa práv. Výsledkom bude použiteľný dashboard so skutočnými dátami a overenou perzistenciou.

### Etapa 2 — bezpečnosť, novinky a pomocník

- Previesť aktívne prihlásenia na povinný widget vrátane odhlásenia iných vlastných relácií.
- Systémové upozornenia zobraziť kompaktne v samostatnom stabilnom priestore.
- Nahradiť veľký uvítací text a duplicitné novinky stručným prehľadom s potvrdením prečítania.
- Pridať spoločné vyhľadávacie pole s vždy viditeľným prepínačom **„V administrácii / V dokumentácii“**.
- Administratívne hľadanie použije existujúce vyhľadávanie; dokumentácia sa otvorí v novom okne so zadaným výrazom cez parameter `q`.
- Kontextový Pomocník v hlavičke zostane zachovaný. Samotné sémantické vyhľadávanie dokumentácie je samostatná funkcionalita.

### Etapa 3 — obsah a formuláre

Implementovať schvaľovanie, plán publikovania a formuláre. Každý widget musí rešpektovať práva k jednotlivým záznamom.

Pri formulároch používať skutočný dátum odoslania a dostupné údaje; nezavádzať označenia „neprečítané“ alebo „nevybavené“, keď taký stav systém neeviduje. Plán publikovania nesmie vyžadovať všeobecné právo na audit len kvôli prístupu k vlastnému obsahu.

### Etapa 4 — štatistiky

Implementovať návštevnosť, najnavštevovanejšie stránky, hľadané výrazy, zdroje návštevnosti a chyby 404.

Predvolené obdobie bude posledných sedem ukončených dní, alternatívy 30 a 90 dní. Porovnania používajú rovnako dlhé predchádzajúce obdobie. Pri 404 zachovať dostupné týždenné členenie a jasne pomenovať počítanú metriku.

### Etapa 5 — newsletter a dokončenie migrácie

- Implementovať newsletter.
- Automatický výber kampane: naposledy spustená aktívna, následne najbližšia naplánovaná, následne posledná dokončená; umožniť pripnutie konkrétnej kampane.
- Odstrániť nahradené pôvodné bloky.
- Zvyšné monitorovanie, audit, prihlásených administrátorov a pôvodné záložky sprístupniť v zbaliteľnej časti „Ďalšie prehľady“. Zachovať prístup k spätnej väzbe.
- Dokončiť používateľskú dokumentáciu a aktualizovať skill podľa všetkých implementovaných vzorov.

Prírastok odberateľov newslettera ani ďalšie nerozpracované moduly nepatria do tejto implementácie. Po overení prvej etapy možno nezávislé widgety vyvíjať paralelne.

## 4. Katalóg a povolené veľkosti

Rozmery znamenajú šírku × výšku v logických jednotkách. Počty riadkov sú cieľové maximá; nesmú viesť k orezaniu obsahu pri zväčšení textu.

| Widget | Varianty a obsah |
|---|---|
| Skratka do modulu | **1×1:** ikona a názov; výber dostupného modulu, voliteľný vlastný názov |
| Moje posledné stránky | **2×3:** približne 5 názvov; **3×3:** približne 6 riadkov s názvom, sekciou a dátumom |
| Čaká na schválenie | **1×1:** počet; **3×3:** položka, žiadateľ a čas čakania |
| Plán publikovania | **2×3:** približne 5 najbližších publikovaní alebo ukončení platnosti |
| Formuláre | **1×1:** počet odoslaní, obdobie a rozsah; **3×3:** súhrn a približne 6 odoslaní s formulárom, dátumom a detailom |
| Návštevnosť | **1×1:** hodnota, obdobie a zmena; **3×3:** graf jednej zvolenej metriky s porovnaním |
| Najnavštevovanejšie stránky | **2×3:** približne 5 stránok a počty; **3×3:** približne 6 stránok s cestou, hodnotou a zmenou |
| Čo návštevníci hľadajú | **2×3:** približne 5 výrazov a počet vyhľadaní |
| Odkiaľ návštevníci prišli | **2×3:** zdroje a počty; **3×3:** horizontálny graf s počtami a podielmi evidovaných údajov |
| Newsletter | **2×2:** stav kampane, priebeh a chyby, po dokončení zaznamenané otvorenia a kliknutia; **3×3:** prehľad približne 3 kampaní |
| Chyby 404 | **1×1:** počet chybových požiadaviek za týždeň; **3×3:** adresy a počty výskytov |
| Moje aktívne prihlásenia | **2×3:** približne 3 relácie, označenie aktuálnej, prehliadač, čas a IP; odhlásenie ostatných a prístup ku všetkým |
| Systémové upozornenia | **Celá šírka × auto:** závažnosť, vysvetlenie a nápravná akcia |
| Čo je nové | **3×2:** verzia, 2–3 novinky, detail a potvrdenie prečítania |
| Vyhľadávanie a pomoc | **Celá šírka × auto:** prepínač rozsahu, pole a tlačidlo |

Zoznamy budú stručné náhľady s odkazom na úplný prehľad, bez vnoreného posúvania. Prázdny výsledok sa odlíši od chyby či nedostupných dát.

Dáta sa obnovia pri načítaní, zmene relevantných nastavení alebo domény a manuálne. Iba viditeľný newsletter s aktívnym odosielaním sa bude obnovovať každých 30 sekúnd.

## 5. Overenie a podmienky dokončenia

- **Backend:** vlastníctvo nastavení a relácií, práva a domény, validácia konfigurácií, limity, transakčné uloženie a bezpečné odovzdanie dát frontendu.
- **Frontend:** povolené varianty, viac inštancií, povinné widgety, obnova po chybe uloženia a správne uvoľnenie zdrojov.
- **E2E:** pridanie, nastavenie, presun, minimalizovanie, odstránenie, opätovné pridanie, obnovenie stránky a načítanie v druhej relácii.
- **Regresie:** systémové hlásenia prežijú úpravy dashboardu; odhlásenie relácií zostane funkčné; potvrdenie noviniek sa zachová.
- **Responzivita a prístupnosť:** šírky 320, 390, 768, 1024, 1200, 1337 a 1920 px, hranice breakpointov, klávesnica, fokus, dlhé názvy a 200 % zväčšenie.
- **Skill:** spustiť `quick_validate.py`, overiť odkazy a porovnať pravidlá s oboma pilotmi. Nezávislý agent dostane skill, repozitár a zadanie widgetu Formuláre na skúšobný návrh bez implementácie; zistené nejasnosti opraviť.
- **Build a server:** použiť existujúci frontendový build a relevantné Java a CodeceptJS kontroly. Po zmene Java tried vykonať kontrolovaný reštart so zachovaním aktuálneho profilu servera pred integračným overením.

Každá etapa zahŕňa aktualizáciu skillu a primerané overenie. Všetky zmeny zostanú necommitnuté na kontrolu používateľom.

## Overené implementačné upresnenia

- Predvolené rozloženie bolo rozšírené z pilotov na všetky dostupné typy widgetov. Široké prehľady tvoria dvojice, zoznamy trojicu; systém naďalej filtruje podľa práv a nemení uložené osobné rozloženia.
- Katalóg obsahuje **Resetovať → Obnoviť predvolené**. `DELETE /admin/rest/dashboard/settings` atomicky odstráni dashboardové nastavenia aktuálneho konta vrátane všetkých doménových filtrov a potvrdených noviniek. Ostatné nastavenia a legacy záložky zostávajú zachované. Klient zobrazí predvolené widgety až po úspechu; pri chybe zachová pôvodný stav.
- Grafy používajú existujúce AmCharts cez `window.initAmcharts()` a `ChartTools`. Dáta grafu sú dostupné aj ako tabuľka; graf sa uvoľní pri obnovení, minimalizovaní alebo odstránení widgetu.

- REST požiadavky WebJETu vyžadujú CSRF hlavičku aj pri GET. Všetky nové načítania ju posielajú.
- Runtime používa JSON konvertor Jackson 3. REST DTO preto používa bežné mapy, nie uzly JSON z Jacksonu 2.
- Staršie MySQL/MariaDB inštalácie majú `user_settings_admin` typu MyISAM. Cielená migrácia v `autoupdate-webjet9.xml` zmení iba engine tejto existujúcej tabuľky na InnoDB, aby viacriadkové nastavenia podporovali transakcie. Na ostatných databázach sa schéma nemení. Pred dokončením migrácie server zápis odmieta.
- Okamžité vrátenie odstránenia obnoví aj filtre v ostatných doménach. Ďalšia úspešná zmena zruší ponuku vrátenia a odstráni už nepotrebné uložené filtre.
- Formuláre zahŕňajú aj dnešné odoslania (začiatok dneška mínus počet dní − 1 až po teraz). Pravidlo ukončených dní sa vzťahuje na štatistiky.
- Historické 404 obsahujú týždenné súčty. Počet chybových požiadaviek sa sčíta za týždenné agregáty zasahujúce do vybraného obdobia a widget vždy uvedie skutočný rozsah vrátane prípadného neukončeného aktuálneho týždňa; nejde o počet unikátnych URL. V inštalácii so spoločnými údajmi viacerých domén sa z nich nedá spätne odvodiť presná doména; widget zobrazí vysvetlenie nedostupnosti.


## Stav implementácie a overenie

Implementovaných je všetkých päť etáp: spoločný systém a dva piloty, bezpečnosť/novinky/pomoc, obsah/formuláre, štatistiky a newsletter s dokončením migrácie pôvodných blokov. Katalóg obsahuje 14 typov widgetov; systémové upozornenia majú samostatný stabilný priestor. Nové voliteľné typy sa používateľom s uloženým rozložením ponúkajú cez katalóg.

Overené 26. 9. 2026:

- Frontendový build `npm run dev`; 33 JavaScript testov spoločného systému a widgetov.
- 40 Java testov dashboardu vrátane vlastníctva, domén, limitov, transakcií a vlastných/vzdialených relácií.
- Reálne browser scenáre pridania, nastavenia, ťahania, klávesového presunu, minimalizovania, odstránenia/vrátenia, chyby uloženia a druhého prihlásenia. Testy obnovujú pôvodné efektívne nastavenia konta.
- Dátové endpointy všetkých providerov, celý katalóg, potvrdenie a opätovné zobrazenie noviniek, prepínač dokumentácie a skutočný preklik do schvaľovania. Dokumentačný test zachytáva otvorenie okna bez odoslania externého dotazu.
- Šírky 320, 359, 360, 390, 767, 768, 1024, 1199, 1200, 1337 a 1920 px; 200 % zväčšenie textu vrátane zalamovania a čitateľnosti buniek tabuliek.
- Axe kontrola načítanej úvodnej stránky a katalógu; ovládanie dialógu klávesnicou, zachovanie fokusu a jeho návrat. Zachované lokálne záložky a odhlásenie výlučne relácie vytvorenej testom. Spätná väzba sa pri testoch neodosiela.
- Skill prešiel `quick_validate.py`, nezávislým skúšobným návrhom Formulárov a záverečným porovnaním s implementáciou. Opravené nejasnosti sú zapracované do kontraktu.

Lokálny server používa pôvodný profil `/poolman-local.xml`. Cielená migrácia tabuľky nastavení na InnoDB prebehla štandardným aktualizačným mechanizmom; ostatné nastavenia zostali zachované. Overenie databázových integrácií prebehlo na lokálnej MariaDB; ostatné podporované databázové platformy neboli v tejto relácii spustené.

Po doplnení všetkých predvolených widgetov, resetu a AmCharts prešiel frontendový build, 45 JavaScript testov a 25 cielených Java testov nastavení, repository a REST rozhrania. Reálny reset bol overený na dočasnom účte vrátane chyby uloženia, obnovenia stránky a následnej personalizácie; účet aj jeho nastavenia boli odstránené a pôvodné konto zostalo nezmenené. Browser testy grafov overujú obnovenie, minimalizovanie, zmenu na 1×1, odstránenie/vrátenie a jediný živý AmCharts root na host. Predvolené desktopové aj mobilné rozloženie a grafy prešli vizuálnou kontrolou; celá zostava widgetov prešla aj axe kontrolou.
