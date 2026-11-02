# 04. Uloženie vetvy, konflikty a publikovanie

Stav: **technický návrh pred implementáciou**. Vychádza z [potvrdených rozhodnutí](01-rozhodnutia.md), [dátového modelu](02-datovy-model.md), [rezervácie ID](02a-identifikatory.md) a [náhľadu](03-nahlad-a-java.md). Existujúce správanie a zdrojové odkazy sumarizuje [analýza zápisu](analyza-zapis-a-publikovanie.md).

Najjednoduchší zvolený merge porovnáva **celý obsahový objekt**, nie jednotlivé polia. Stránka, priečinok, médium alebo súbor majú spoločný základ, pracovnú verziu a aktuálnu produkciu. Rozdiely po poliach pomáhajú človeku čítať zmenu; nevytvárajú automaticky novú kombinovanú verziu.

## 1. Zápis pracovnej verzie

`ContentBranchSaveService` dostane používateľa, vetvu, objekt a hash verzie zobrazenej v editore. Uložiť možno iba editovateľnú vetvu. Overí existujúce práva na objekt aj cieľ presunu, doménu, povolené polia a závislosti.

Pri prvej zmene existujúcej položky porovná hash načítaný editorom s aktuálnou produkciou. Ak sa medzitým zmenila, vráti `409` a údaje na obnovenie/porovnanie. Neuloží starý formulár s novším základom. Pri ďalšom uložení porovná hash pracovnej verzie; základ prvej úpravy zostáva rovnaký. Táto kontrola bráni bežnému prepísaniu neaktuálnym formulárom bez dlhého zámku editora.

Po spoločnej validácii a normalizácii uloží pracovné stĺpce, pri prvej zmene aj `base_snapshot`/`base_hash`, zvýši `content_branch.revision` a zneplatní predchádzajúce rozhodnutie konfliktu danej položky. Krátka DB operácia musí skontrolovať stav vetvy a uložiť dáta spolu; nesmie uložiť obsah po tom, čo iný request prepol vetvu na `MERGING`.

Nové objekty použijú rezerváciu konečného ID z dokumentu 02a. Kladné ID nového objektu nie je dôkaz, že už existuje v produkcii. Médiá novej stránky sa priradia priamo k jej rezervovanému `doc_id`, nie k spoločnému dočasnému priestoru používateľa.

Pracovný zápis nevytvára produkčnú históriu, individuálny plán publikovania ani produkčné udalosti; neindexuje, nezrkadlí, neprekladá a nemení multigroup či atribúty. Audit môže zaznamenať „uložená pracovná zmena vo vetve X“. Je oddelený od auditu skutočného zverejnenia.

Pre Novinky uloží podporované priradenia existujúcich perex skupín iba do pracovného `perex_group`. Pracovné `available` predstavuje cieľovú dostupnosť, nie technický spôsob skrytia pred produkciou. Skutočné redakčné uloženie aktualizuje pracovné `date_created`/`author_id`; merge ich následne zachová zo zvolenej verzie. Nepoužije všeobecný save, ktorý by novinke nastavil dátum a autora publishera a tým zmenil výpis. Dátumy a predvolené hodnoty nových stránok určuje [03a](03a-novinky.md).

## 2. Čo oddeliť z dnešných save metód

Nie je potrebný nový všeobecný workflow systém. Potrebné sú tri jasné cesty: príprava údajov, zápis pracovného obsahu a kontrolované publikovanie.

| Existujúci vstup | Potrebná úprava |
| --- | --- |
| [EditorFacade.save](../../../src/main/java/sk/iway/iwcm/editor/facade/EditorFacade.java) | Oddeliť konverziu `editorFields` od produkčného ukladania médií, atribútov, default stránky a multigroup. Pracovná cesta použije len podporované časti. |
| [EditorService.saveEditedDoc](../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java) | Znovupoužiť overenie práv, kontrolu úplného HTML, normalizáciu názvu/tela/URL a odvodené polia bez spustenia histórie, plánovania a všeobecných save udalostí. |
| `EditorService.docSave` | Oddeliť výpočet `file_name`, koreňových priečinkov a ďalších odvodených údajov od produkčného insert/update a súvisiacich zápisov. |
| [GroupsRestController](../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java), [GroupsDB.setGroup](../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java) | Oddeliť validáciu a normalizáciu priečinka od synchronizácie názvu stránky, histórie, prepočtu potomkov, udalostí a cache. `publishEvents=false` samo osebe nestačí. |
| `EditorService.deleteWebpageLogic`, `GroupsDB.deleteGroup` | Použiť explicitnú operáciu presunu do koša; nikdy náhodne neprejsť do vetvy trvalého mazania, ani pri vypnutej histórii. |
| [PlannedChangesService](../../../src/main/java/sk/iway/iwcm/doc/PlannedChangesService.java) | Vyčleniť použiteľné file preflight/backup/replace/cache primitíva. Nevolať celý per-page postup, ktorý obnovuje produkčný priečinok/URL a pre médiá vytvára nové ID. |

Navrhované služby: `ContentBranchSaveService`, `ContentBranchMergeService` pre porovnanie a kontroly, `ContentBranchPublishService` pre vykonanie, jednoduchý `ContentRowWriter` pre DB zápis a `ContentPublishEffects` pre známe následky. Scheduler iba vyvolá tie isté služby. DTO a pomocné mapovanie majú zostať pri tejto funkcii.

Podľa [návrhu rozšírení](08-rozsirovanie-modulov.md) koordinátor prijíma spoločné položky `BranchChange` a používa register `ContentBranchTypeHandler`. Handler pozná snapshot, práva, konkrétny zápis a účinky svojho typu; spoločná služba vlastní porovnanie, stav a checkpoint. Rozhodovanie sa nesmie rozrásť na samostatný merge engine pre každý modul. Dnešné typované tabuľky zostávajú zdrojom jadra, spoločná JSON tabuľka príde až s prvým ďalším modulom. Súborové kroky naďalej používajú svoju netransakčnú file službu a porovnanie hashov.

## 3. Trojcestné porovnanie celého objektu

`B` je základ prvej úpravy, `W` pracovný výsledok, `L` aktuálna produkcia. Hash sa počíta z kanonického podporovaného obsahového DTO podľa dokumentu 02. Neobsahuje počítadlá návštevnosti, odvodené vyhľadávacie polia ani prevádzkové príznaky. HTML jedného objektu aj binárny súbor sa porovnávajú ako celok.

Pri médiu je súčasťou snapshotu, hashu a zobrazeného diffu aj zoradený zoznam jeho priradení do existujúcich kategórií. Tento zoznam možno vo vetve upraviť a nové médium môže vybrať existujúce kategórie; nemenia sa definície `media_groups`. Produkčná zmena členstva sa preto nestratí: ak sa zmenila aj pracovná verzia média, vznikne konflikt celého média. Voľba `WORK` vedome použije aj zobrazené pracovné členstvo; `LIVE` alebo no-op ponechá aktuálne produkčné väzby.

Pri stránke rovnaké pravidlo zahŕňa normalizované priradenia perex skupín v `perex_group`. Preflight overí existenciu/dostupnosť skupín; zaniknutá skupina zabráni merge s konkrétnym vysvetlením. Voľba LIVE alebo no-op neobnovuje väzby zo starej pracovnej verzie.

| Porovnanie | Výsledok |
| --- | --- |
| `W = B` | Vetva daný objekt obsahovo nezmenila: zachovať `L`, bez zápisu. |
| `L = B` | Produkcia zostala pôvodná: použiť `W`. |
| `L = W` | Požadovaný obsah už je v produkcii: bez zápisu. |
| Inak | Konflikt celého objektu: používateľ vyberie produkciu alebo pracovnú verziu. |

Aj keď produkcia zmenila iba názov a vetva iba perex, ide o konflikt. Je to zámerné zjednodušenie. UI ukáže rozdiely po poliach, pri HTML čitateľný diff a pri súboroch dostupný náhľad/verziu, ale automaticky neskladá nový obsah.

Ak používateľ potrebuje kombináciu, upraví pracovnú verziu v bežnom editore a potom výslovne zvolí jej použitie voči aktuálnej produkcii. Samostatný tretí merge editor netreba.

Rozhodnutie sa viaže na konkrétny `resolved_live_hash` a `resolved_work_hash`. Po zmene ktoréhokoľvek vstupu sa stará voľba nepoužije. Zvolený výsledok je v `resolved_snapshot`; pri konečnej príprave sa tam uloží aj automaticky určený výsledok položiek bez konfliktu. Pracovné stĺpce sa tým nemenia.

### Existencia, zmazanie a nové objekty

Existencia je súčasťou porovnania, nie prázdna hodnota HTML. Kanonický stav rozlišuje aktívny objekt, objekt v koši a neexistujúci objekt; pri súbore sa porovnáva existencia aj hash bajtov.

- Nový pracovný objekt s voľným rezervovaným ID sa vloží s týmto ID. Neočakávaný produkčný riadok pod rezervovaným ID je konflikt identity; nesmie sa prepísať. Výnimkou je vlastný preukázateľne dokončený krok pri opakovaní po chybe.
- Pracovné zmazanie nezmeneného produkčného objektu sa vykoná. Ak sa produkcia zmenila, treba rozhodnutie človeka. Ak požadované zmazanie už nastalo, ide o no-op.
- Produkčné zmazanie pri pracovnej úprave je konflikt. Zachovanie produkcie ponechá zmazanie; použitie práce musí mať výslovne overenú podporu obnovenia pod rovnakým ID a práva na cieľ. Chýbajúca podmienka operáciu zablokuje.
- Ak používateľ vyberie produkciu pre všetky konflikty a ostatné položky sú no-op, vetva sa môže úspešne uzavrieť bez obsahového zápisu.

## 4. Príprava úplného plánu

`prepare` je kontrola, nie publikovanie. Číta produkciu explicitne mimo vetvového overlay. Vráti revíziu vetvy, zoznam výsledných položiek, konflikty, chyby a identifikátor porovnania odvodený z aktuálnych vstupných hashov.

Konfliktné hashe sa načítajú z aktuálnej databázy, nie zo starého spravovaného JPA objektu alebo globálnej obsahovej cache. Rovnaké pravidlo platí pre finálnu kontrolu pred zápisom.

Pred spustením treba overiť:

1. Existujúce právo priameho publikovania **každej** položky vrátane cieľových priečinkov, súborových ciest a odvodených zmien. Koordinátor bez práva nemôže obísť dnešný schvaľovací proces. Uloženie jednej položky iba ako draft nesmie byť označené za úspešný merge.
2. Výsledný strom: rodičia, cykly, default stránky, vlastníci médií, dostupné šablóny a obsahové referencie. Vybrané verzie musia tvoriť platný celok aj vtedy, keď časť konfliktov vyhrala produkcia.
3. Kolízie výsledných URL v rovnakej doméne, cieľových súborových ciest a identít. Kontrola pracuje s výsledkom celého plánu, nie iba s dnešnou produkčnou URL mapou.
4. Zdrojové pracovné súbory, ich hashe, zapisovateľné cieľové umiestnenie a podporované úložisko. Neporovnávať iba mtime/veľkosť.
5. Individuálne budúce plány stránok/priečinkov, čakajúce schválenia a naplánované zneprístupnenie dotknutých stránok. Kolízia sa musí vyriešiť vedome v pôvodnom nástroji; balík ju potichu nezruší ani neprevezme.
6. Rozsah podporovaných dát a známych vedľajších účinkov. Zdieľané System stránky mimo domény sú iba na čítanie. Súbory používajú dnešné doménové mapovanie a adresárové práva; pri spoločnom fyzickom adresári UI upozorní, že účinok môže presiahnuť doménu. Nepridáva sa nový register vlastníctva ani skenovanie všetkých referencií.

Presun/premenovanie priečinka rozvinie ovplyvnené stránky, odkazy a médiá na viditeľné položky už pri príprave pracovných zmien. Pred merge sa rozsah znovu porovná s produkciou. Nový potomok alebo nová závislosť, ktorá by vyžadovala ďalší zápis, znamená nové posúdenie; nezahrnie sa potichu až počas publikovania.

Dnešné `fixRenamedVirtualPath` mení aj odkazy médií a zapisuje pravidlo cez `UrlRedirectDB.addRedirect`. Preto sa nevolá ako jeden nekontrolovaný následok. Zmeny stránok/médií sú položky plánu; vytváranie trvalých redirect pravidiel je mimo prvého rozsahu. UI pri zmene URL tento rozdiel uvedie.

Pri mazaní priečinka sa vytvorí explicitný zoznam dotknutých priečinkov, stránok a podporovaných závislostí. Pred vykonaním sa odhalia noví produkční potomkovia. Neexistuje implicitné „zmaž všetko, čo tam práve je“.

## 5. Životný cyklus

| Stav | Význam a povolený ďalší krok |
| --- | --- |
| `DRAFT` | Úpravy, porovnanie, riešenie konfliktov, plánovanie alebo okamžitý merge. |
| `SCHEDULED` | Pracovný obsah a vybrané rozhodnutia sú uzavreté pre určený termín. Pred úpravou treba odplánovať do `DRAFT`. |
| `NEEDS_ATTENTION` | Automatická kontrola zistila problém pred prvým zápisom. Termín sa už automaticky nespúšťa; používateľ obnoví prípravu. |
| `MERGING` | Vykonáva sa konkrétny plán. Obsah sa neupravuje. |
| `MERGE_FAILED` | Nastala chyba po začatí vykonávania; časť zmien už môže byť verejná. Možno skontrolovať stav a pokračovať. |
| `MERGED` | Všetky položky a dokončovacie účinky sú úspešné. Jednorazová vetva je uzavretá. |
| `CANCELLED` | Zrušená nezverejnená vetva. Rezervované ID sa neuvoľňujú. |

`READY` je odvodený výsledok aktuálneho `prepare`, nie ďalší DB stav. Je platný len pre uvedené vstupy a revíziu. Úspešná vetva sa znovu neotvorí na ďalšiu kampaň; používateľ vytvorí novú z aktuálnej produkcie.

Vetvu s už aplikovanými položkami nemožno označiť za bezpečne zrušenú tak, že sa skryje čiastočný výsledok. Po chybe ostáva evidencia `MERGE_FAILED`, kým sa situácia nevyrieši. Oprava už publikovaného výsledku je vedomá produkčná úprava alebo ďalšia vetva, nie automatický rollback celého balíka.

V `MERGE_FAILED` sa pracovný obsah ani množina závislostí nemenia. Voľbu `LIVE/WORK` možno znovu vyriešiť iba pre ešte nedotknutú položku (`PENDING`, prípadne `ERROR` pred úspešným dátovým krokom). Vlastná kombinácia po čiastočnom publikovaní vznikne výslovnou úpravou produkcie a následnou voľbou `LIVE`, alebo až v novej vetve po dokončení obnovy. Nevzniká druhý režim editora na prestavbu rozpublikovaného balíka.

## 6. Spustenie bez duplicitného publikovania

Jeden určený aplikačný uzol je publisher balíkov. V klastri sa scheduler tejto funkcie spúšťa len tam; ostatné uzly obsluhujú editor a náhľad. Manuálne spustenie tiež musí doraziť tomuto publisheru podľa existujúceho smerovania admin požiadaviek. Netreba distribuovanú frontu ani voľbu lídra.

Prechod na `MERGING` je jeden podmienený DB update podľa ID, očakávanej revízie a povoleného predošlého stavu. Úspech musí znamenať práve jeden zmenený riadok. Druhé kliknutie alebo druhé spustenie toho istého balíka nezíska oprávnenie začať ďalší beh.

Pred prechodom sa kontroluje aktuálne porovnanie. Po prechode sú presné ciele, vstupné hashe a množina položiek uložené a nemeniteľné počas daného behu. Žiadny ďalší krok nepridáva skryté obsahové položky. Publisher spracúva balíky postupne; editor ostatných stránok ďalej funguje.

Vykonávanie obslúži jeden jednoduchý background executor publisher uzla. API po úspešnom uložení pripraveného stavu a odovzdaní práce vráti `202`; UI načítava `/progress`. Worker dostane ID vetvy a používateľa, nie HTTP request/session. Zatvorenie prehliadača ho preto nezastaví. Nepridáva sa externý queue systém; trvalým záznamom práce sú stav vetvy a checkpointy. Ak odovzdanie workeru zlyhá, uloží sa chyba a možnosť pokračovania, nie falošný úspech.

Pri páde procesu môže zostať `MERGING`. Po overení, že starý publisher už nebeží, správca použije explicitné pokračovanie; automatické prevzatie podľa krátkeho timeoutu sa nezavádza. Nejednoznačný stav sa nevydáva za úspech.

## 7. Poradie aplikovania

| Poradie | Operácia |
| --- | --- |
| 1 | Vytvoriť nové priečinky od rodičov k potomkom s rezervovanými ID. Odkaz na ešte nevytvorenú default stránku dočasne ponechať neutrálny; finálny odkaz je uložený v pláne. |
| 2 | Zapísať podporované zmeny existujúcich priečinkov. Závislé default referencie sa dokončia po stránkach. Použiť výslovne vypočítané zmeny, nie opätovný neobmedzený prepočet potomkov. |
| 3 | Zverejniť nové/zmenené súbory. Pred nahradením zachovať pôvodnú kópiu a použiť overený temp súbor + nahradenie jedného cieľa. |
| 4 | Vložiť nové stránky s rezervovanými ID a aktualizovať existujúce stránky podľa ich pôvodných ID. Prepočítať odvodené polia zo zvoleného výsledku. |
| 5 | Zapísať médiá po existencii vlastníckych stránok, po jednotlivých `media_id`, spolu so zvoleným zoznamom priradení do existujúcich kategórií. Nevytvárať novú sadu ID ani meniť definície kategórií. |
| 6 | Dokončiť default stránky a ostatné odložené podporované referencie priečinkov. |
| 7 | Vykonať explicitné mazania až po zmene referencií: médiá, stránky do koša, priečinky do koša od potomkov, samostatné súborové položky. |
| 8 | Dokončiť potrebné indexovanie a obnovu cache vrátane ostatných uzlov; až potom stav `MERGED`. |

Existujúce objekty sa aktualizujú, nie odstraňujú a znovu vytvárajú. Nové objekty používajú explicitný insert writer z dokumentu 02a; JPA `save()` s kladným ID nesmie rozhodnúť o inom ID alebo nesprávnom UPDATE.

Budúci modul doplní svoje konkrétne miesto do tohto pevného poradia: jeho súbory a nové referencované stránky/priečinky musia existovať pred zápisom modulu; odstránenie referencií pred zmazaním cieľov. Na tento účel netreba všeobecný grafový workflow. Závislosti, skupiny súborov a obmedzenia prvých modulov určuje [08](08-rozsirovanie-modulov.md).

Stránky a priečinky sa pri zmazaní presúvajú do koša so zachovaním ID. Trvalé mazanie nie je operácia tejto verzie. Médium nemá rovnaký kôš: vymaže sa konkrétny riadok a jeho kategorizácia, pričom snapshot zostane vo vetve. To samo osebe nemaže jeho súbor. Súborové DELETE zachová zálohu podľa existujúcich file-history primitív.

Postupné publikovanie môže počas behu krátko ukazovať kombináciu starých a nových údajov. Návrh nesľubuje atómové prepnutie. Cache sa po dokončení aj po prerušení zosúladí so skutočne zapísaným stavom.

## 8. Krátke zápisy a pokračovanie po chybe

DB položka používa krátku transakciu na jednom spojení: finálne načítanie a overenie očakávaného stavu, obsahový zápis, príslušná história/audit a checkpoint položky. Krátke uzamknutie konkrétneho zapisovaného riadka medzi kontrolou a UPDATE je prípustné; nie je to zámok počas editovania, prípravy balíka ani práce so súbormi. Bez verzie v produkčnej tabuľke by samotné „prečítaj hash, neskôr vykonaj UPDATE“ nedávalo takúto ochranu.

Nový insert chráni PK; nepotrebuje vopred uzamykať neexistujúci riadok. Všetky DB časti jedného kroku vrátane checkpointu musia použiť tú istú skutočnú transakciu, nie zmes nezávislého JDBC a JPA spojenia. Návrh počíta s potvrdenou podmienkou InnoDB; transakcia neobopína filesystem ani celý balík.

Zápis stránky zahŕňa aj obnovu odvodeného `perex_group_doc` podľa zvoleného `perex_group` a dnešnej konfigurácie, iba pre daný `doc_id`. `PerexGroupsService` potrebuje úzku operáciu prijímajúcu rovnakú `Connection`; dnešné nezávislé repository volania nie sú náhradou. Nevolať `DocDB.udpdatePerexGroupDoc` s jeho automatickým rozšírením na multigroup kópie. Pri presune stránky do koša zachovať dnešnú jednopoložkovú kategorizáciu v súlade s výsledným riadkom, bez mazania väzieb iných stránok.

To platí aj pre históriu a audit. Dnešný [Adminlog.add](../../../src/main/java/sk/iway/iwcm/Adminlog.java) otvára vlastné spojenie; pre publisher treba oddeliť zápis s prijatým `Connection`, pričom bežné verejné API zostane zachované. Podobne sa zapojí história dokumentov a historický záznam priečinka v `groups_scheduler`; nevytvorí sa budúci individuálny plán. Kontrola pripravenosti musí overiť engine všetkých týchto reálne zapisovaných tabuliek, nie iba troch obsahových tabuliek. Pri nepodporovanej konfigurácii sa funkcia nezapne; audit/história sa potichu nevynechajú.

Pevný postup položky používa existujúce stĺpce `apply_state`, `apply_phase`, `applied_hash`, `apply_error`:

- `PENDING`: cieľ je pripravený, zatiaľ neaplikovaný.
- `APPLYING`: začatý konkrétny krok. Fázy sú obmedzené podľa typu, napr. `DATA`, `REFERENCES`, `EFFECTS`; nevzniká všeobecný workflow jazyk.
- `DONE`: cieľ a príslušné účinky dokončené. No-op má fázu `NOOP` a nevyvoláva obsahový zápis ani duplicitnú históriu.
- `ERROR`: uložená konkrétna chyba a posledná dokončená fáza.

Pri viacfázovom priečinku sa uchová aj hash skutočného medzistavu po vytvorení bez default stránky. Finálny cieľ zostáva v `resolved_snapshot`. Tým sa pri pokračovaní rozlíši vlastný medzikrok od cudzej produkčnej úpravy.

`resume` znovu overí aktuálne práva, závislosti a vstupy. Pre každý nedokončený krok platí:

| Aktuálny produkčný stav | Pokračovanie |
| --- | --- |
| Zodpovedá očakávanému stavu pred krokom | Vykonať krok. |
| Zodpovedá známemu vlastnému výsledku alebo uloženému medzistavu | Neopakovať hotový obsahový zápis; pokračovať potrebnou fázou. |
| Je iný | Konflikt s novšou produkčnou zmenou; zastaviť bez prepísania. |

Ak sa zmenila už čiastočne aplikovaná položka, prvá verzia ju nevyrieši opätovným všeobecným `WORK` zápisom. Zobrazí konkrétny medzistav a potrebu zásahu správcu. Pokračovať možno až po overení súladu so známym cieľom a závislosťami; opravný zásah do produkcie je výslovný a auditovaný. Tento prípad nemá automatický rollback ani univerzálny recovery editor.

Pri súbore nahradenie a DB checkpoint netvoria jednu transakciu. Po páde sa preto porovná produkčný hash s pôvodným a cieľovým hashom. Zhodný cieľ umožní dokončiť checkpoint bez opätovného nahradenia; iný obsah vyžaduje rozhodnutie. Pracovné originály a zálohy sa počas neúspešného merge nemažú.

Hotové DB zápisy sa pri chybe indexovania alebo cache nevykonávajú znovu. Príslušné obnovenie podľa ID je opakovateľné. Ak sú všetky položky hotové a zlyhalo iba záverečné obnovenie, `resume` zopakuje dokončenie a až potom uzavrie vetvu. Žiadny automatický rollback už zverejnených položiek.

## 9. Kontrolované publikačné účinky

`ContentBranchPublishService` beží v explicitnom produkčnom režime. Pridá produkčný audit s ID vetvy a vykoná históriu podľa existujúcich pravidiel/konfigurácie, odvodené údaje, cache a potrebné indexovanie. História, ktorá je vypnutá konfiguráciou, nesmie zmeniť požiadavku na zachovanie ID pri presune do koša.

Keďže writer zapisuje cez JDBC, dokončenie musí zneplatniť aj príslušné JPA/EclipseLink cache dokumentov, médií a ich kategorizácie, nielen `DocDB`/`GroupsDB`. Použijú sa čerstvé čítacie kontexty; už načítaná spravovaná entita nesmie pri neskoršom flush prepísať výsledok. Overenie začne zahriatím produkčného repository aj verejnej cache, potom vykoná merge a skontroluje obe čítacie cesty. Podpora jednotlivých persistence jednotiek a ostatných uzlov patrí do konkrétneho dokončovacieho helpera, nie do náhodného volania všetkých save listenerov.

Nespúšťa všeobecný reťazec všetkých dnešných `ON_START`/`AFTER_SAVE` listenerov nad každou položkou. Potrebné účinky sa oddelia a zavolajú výslovne. Zrkadlenie a automatický preklad sú pri merge vypnuté podľa Q14. Multigroup, atribúty, galéria a ostatné nepodporované zápisy sa nevykonávajú.

Existujúce väzby sa zachovajú. Ak by podporovaná zmena na konkrétnom prepojenom objekte nešla vykonať bez nepodporovaného následku, validácia ju zablokuje s vysvetlením; nevytvorí tichú nekonzistenciu. Zákaznícky hook sa pripája až explicitným adaptérom s deklarovanými účinkami, nie automatickým spustením všetkých neznámych listenerov.

## 10. Jeden termín a existujúce schedulery

Naplánovanie uloží termín, aktuálnu revíziu obsahu a `scheduled_by`. Pracovné položky sa uzavrú do `SCHEDULED`; ich zmena vyžaduje odplánovanie. Zmena termínu je jedna operácia nad vetvou po opätovnej kontrole pripravenosti.

V termíne publisher načíta identitu `scheduled_by` a aktuálne oprávnenia z DB. Používateľ nemusí byť online. Služba vytvorí potrebný doménový/publikačný kontext explicitne; nevyrába prihlasovaciu session a nespolieha sa na request návštevníka, ktorý spustil kontrolu plánov.

Použije rovnaké `prepare` a vykonanie ako ručný merge. Pracovný obsah nemení. Ak produkcia už zodpovedá požadovanému výsledku, položka môže byť no-op. Nový konflikt, neplatná ručná voľba, zmena práv, chýbajúci súbor alebo kolidujúci individuálny plán prepne vetvu do `NEEDS_ATTENTION` **pred prvým zápisom**. Ak chyba vznikne až počas aplikovania, výsledok je `MERGE_FAILED` s presným priebehom.

Balík nevytvára paralelné publikačné histórie pre dnešný `DocPublishService` ani záznamy s budúcim spustením pre `GroupPublisher`. Tieto mechanizmy ďalej vlastnia svoje existujúce individuálne plány. Kolízie zahŕňajú budúci štart aj budúce zneprístupnenie, ktoré by krátko po kampani prepisovalo jej výsledok. Zrušenie takého plánu je výslovná akcia používateľa, nie vedľajší efekt merge.

## 11. Navrhované API a dokončenie

Všetky mutácie používajú existujúcu autentifikáciu, CSRF ochranu, obsahové práva a očakávanú `revision`. Riadiace endpointy pracujú s explicitným ID vetvy a produkčným technickým kontextom podľa dokumentu 03.

| Endpoint pod `/admin/rest/content-branches/{id}` | Kontrakt |
| --- | --- |
| `POST /prepare` | Overí `{ expectedRevision }`; vráti položky, chyby, konflikty, výslednú revíziu a `preparationHash` pre zobrazené porovnanie. Bez publikovania. |
| `POST /resolve` | `{ expectedRevision, type, objectId, choice: LIVE\|WORK, liveHash, workHash }`; uloží voľbu len pre rovnaké vstupy a vráti novú revíziu. |
| `POST /merge` | `{ expectedRevision, preparationHash }`; skontroluje aktuálnosť, prevezme vetvu podmieneným stavovým update a spustí pripravený plán. |
| `POST /schedule` | `{ expectedRevision, preparationHash, publishAt }`; overí a uloží jeden termín a aktéra. |
| `POST /unschedule` | `{ expectedRevision }`; vráti nespustenú naplánovanú vetvu do `DRAFT`. |
| `POST /reopen` | `{ expectedRevision }`; vráti `NEEDS_ATTENTION` bez aplikovaných položiek do `DRAFT` na opravu a nové naplánovanie. |
| `POST /resume` | `{ expectedRevision, preparationHash }`; pokračuje iba po kontrole skutočného priebehu a aktuálnych konfliktov. |
| `GET /progress` | Stav vetvy, dokončené/pending/chybné položky a posledná chyba. UI môže priebeh pravidelne načítať. |

`objectId` pre súbor znamená jeho jednoznačný pracovný riadok/logický kľúč podľa dátového modelu, nie klientom zvolenú fyzickú cestu. Neaktuálna revízia alebo hash vráti `409` s potrebou obnoviť porovnanie. Nedostatočné právo vráti `403`; nie čiastočný súhlas s publikovaním zvyšku balíka.

Dokončenie návrhu sa overí najmä scenármi: zachovanie existujúcich aj nových ID, pracovné uloženie bez produkčných následkov, všetky štyri základné porovnania, konflikt zmazania, pribudnutý potomok, kolízia URL, zlyhanie po súbore/pred checkpointom, zlyhanie indexu po úspešnom DB zápise, novšia produkčná zmena pred `resume`, zmena práv pred termínom a kolízia so starým schedulerom. Podrobné implementačné kroky a testy nadväzujú v orchestračných dokumentoch.
