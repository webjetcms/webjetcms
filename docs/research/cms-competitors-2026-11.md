# WebJET CMS a päť návrhov rozšírenia

Stav posúdený k 1. novembru 2026 podľa dokumentácie `docs/sk/`, relevantného kódu aktuálneho checkoutu a oficiálnych zdrojov konkurencie. Odporúčam päť rozšírení: centrum kvality obsahu, riadené preklady, optimalizáciu interného vyhľadávania, experimenty s konverznými cieľmi a publikačné balíky kampaní. Najskôr by som realizoval centrum kvality a optimalizáciu vyhľadávania: využijú existujúce základy a majú menší zásah do publikačného jadra.

Konkurenčné funkcie sú overené v dokumentácii, nie praktickým testovaním produktov. Odhady a poradie sú návrhom na základe technickej analýzy; nie sú potvrdeným backlogom ani záväzným termínom. Platené doplnky a verziové obmedzenia konkurencie sú rozlíšené pri jednotlivých zdrojoch.

## Čo už WebJET poskytuje

Za nový návrh nemožno považovať samotné AI generovanie, preklad, sémantické vyhľadávanie, A/B varianty, schvaľovanie, plánovanie ani headless API. Tieto základy už aktuálna dokumentácia opisuje.

| Existujúci základ | Podstatná hranica pre návrhy |
| --- | --- |
| [AI asistenti](../sk/redactor/ai/README.md), [používanie v editore](../sk/redactor/ai/datatables/README.md), [generovanie PageBuilder HTML](../sk/redactor/ai/instructions/README.md) | Hodnotu pridá riadenie a kontrola práce nad obsahom, nie ďalšie generovacie tlačidlo. |
| [Sémantické, hybridné a RAG vyhľadávanie](../sk/redactor/apps/semantic-search/README.md) | Vektorový index ani odpovedajúceho asistenta netreba navrhovať od začiatku. |
| [A/B testovanie](../sk/redactor/apps/abtesting/README.md), [konfigurácia](../sk/redactor/apps/abtesting/abtesting.md) | Existujú varianty, split testy a porovnanie návštevnosti. Nastavenia a identifikácia variantu sú globálne; chýbajúce rozšírenie je samostatný experiment s konverzným cieľom a vlastným meraním. |
| [Zrkadlenie a automatický preklad](../sk/redactor/apps/docmirroring/README.md), [prehľad jazykových väzieb](../sk/redactor/webpages/mirroring/docs.md) | `syncId` prepája stránky. Opakovaný preklad závisí od autora alebo dostupnosti cieľovej stránky. Chráni ručné úpravy, ale nenahrádza evidenciu zdrojovej revízie, zastarania a lokalizačných úloh. |
| [Schvaľovanie stránok a priečinkov](../sk/redactor/webpages/approve/README.md), [história a porovnanie](../sk/redactor/webpages/history.md) | Existuje aj druhá úroveň schvaľovania. Nové funkcie majú tento proces rešpektovať. História obsahuje aj pracovné verzie a môže sa čistiť. |
| [Plánované publikovanie s chránenými súbormi](../sk/redactor/webpages/planned-publishing.md) | Jeden plán na stránku; atribúty a priradenie do ďalších priečinkov sa neplánujú. Súbory, stránka a médiá sa nezapisujú v jednej transakcii. |
| [Kontrola odkazov](../sk/redactor/webpages/linkcheck.md), [SEO analýza](../sk/redactor/apps/seo/README.md), [a11y testy](../sk/developer/testing/a11y.md) | Link checker kontroluje lokálne odkazy. Vývojárske axe testy nie sú redakčným pracovným postupom s uchovanými nálezmi. |
| [Prispôsobiteľný dashboard](../sk/redactor/admin/welcome.md), [formulárové štatistiky](../sk/redactor/apps/multistep-form/stat.md), [headless API](../sk/frontend/headless/README.md) | Nové prehľady môžu použiť existujúce widgety a grafy. Formulárové počítadlá samy neposkytujú atribúciu experimentu. |

Technická realizácia môže zostať na aktuálnom Java 17/Spring MVC, EclipseLink/JPA a DataTables stacku. Nové entity, REST služby a administrácia nepotrebujú migráciu na Spring Boot ani výmenu editora. Deklarácie stacku sú v [build.gradle](../../build.gradle), mechanizmy rozšírenia v [DataTables dokumentácii](../sk/developer/datatables/README.md) a [udalostiach](../sk/developer/backend/events.md). Zistenie, že ucelený pracovný postup chýba, sa vzťahuje na preskúmanú dokumentáciu a relevantné implementácie; nevylučuje zákaznícke rozšírenia mimo tohto repozitára.

## Päť odporúčaných funkcií

Odhady sa týkajú produkčne použiteľnej prvej verzie pre **dvoch vývojárov so znalosťou WebJET a priebežnou podporou QA**. Zahŕňajú administráciu, backend, migrácie a relevantné testy. Nezahŕňajú migrácie starých zákazníckych inštalácií, prestavbu šablón ani neskoršie rozšírenia uvedené mimo rozsahu. Bez rozpisu úloh treba počítať približne s 30 % neistotou; pri publikačných balíkoch môže byť vyššia.

| Funkcia | Hlavný prínos | Prvá verzia | Technické riziko |
| --- | --- | --- | --- |
| 1. Centrum kvality obsahu | Menej chýb na webe a jasná zodpovednosť za opravu | 5–7 týždňov | Stredné: pokrytie publikačných ciest a správne vyhodnotenie nálezov |
| 2. Riadené preklady | Aktualizácie jazykov bez straty ručných korektúr | 5–7 týždňov | Stredné: konflikt revízií a HTML preklady |
| 3. Centrum úspešného vyhľadávania | Návštevník nájde odpoveď, redaktor vie opraviť zlyhávajúci dotaz | 4–6 týždňov | Nižšie až stredné: jednotné meranie rôznych vyhľadávacích ciest |
| 4. Experimenty s konverznými cieľmi | Rozhodovanie podľa dokončených akcií | 6–8 týždňov | Stredné: atribúcia, cookies a cache |
| 5. Publikačné balíky kampaní | Riadené spustenie viacerých pripravených stránok | 7–10 týždňov | Najvyššie: koordinácia schedulerov a čiastočné zlyhanie |

### 1. Centrum kvality obsahu

**Použitie a prínos.** Redaktor vidí pri stránke konkrétne problémy, vie otvoriť miesto opravy a pozná podmienky publikovania. Správca dostane prehľad za celý web: nefunkčné interné odkazy, chýbajúce základné SEO údaje, automaticky zistiteľné chyby prístupnosti a obsah po termíne revízie. Každá stránka môže mať zodpovedného vlastníka a ďalší termín kontroly. Pre verejný sektor a veľké firemné weby je hodnotou predovšetkým to, že problém dostane vlastníka, termín a overenú opravu.

**Rozdiel oproti dnešku.** Spojí existujúce čiastkové kontroly s históriou výsledkov, redakčnými úlohami a pravidlami publikovania. Inšpiráciou sú Storyblok Accessibility checker, PublishPress Checklists a Umbraco Content Reviews; ich konkrétne možnosti a licencovanie sú uvedené nižšie v bodoch 1–2 konkurenčného prieskumu.

**Implementácia.** Pridať `ContentQualityService`, entity pre nálezy a revízie obsahu, DataTable prehľad a widget. Z `LinkCheckService` oddeliť znovupoužiteľnú kontrolu jednej stránky; pre ďalšie HTML pravidlá použiť už prítomný Jsoup. Dávkové kontroly spúšťať cez existujúce automatizované úlohy, s pagináciou a limitom práce na jeden beh. Výsledky viazať na hash obsahu a verziu pravidiel, aby sa starý úspešný test nepoužil na novú verziu.

Redakčný náhľad doplniť o `axe-core` kontrolu vykreslenej stránky a uloženie nálezov s časom kontroly. To vyžaduje novú produktovú integráciu; existujúci CodeceptJS helper slúži ako technický vzor a testovacia infraštruktúra. V prvej verzii sú výsledky prehliadačovej kontroly poradné. Povinné pravidlá server overí pred okamžitým zverejnením, finálnym schválením aj časovaným publikovaním. Samotný JavaScript v editore ani listener po uložení nie sú dostatočná publikačná brána. Uloženie rozpracovanej stránky musí zostať možné.

**Rozsah a hranice.** Prvá verzia: približne 10–15 zrozumiteľných pravidiel, interné odkazy, vlastníci, termíny revízie, upozornenia a auditovaná výnimka pre oprávnenú rolu. Automatická kontrola nie je certifikácia WCAG. Zmeny šablóny môžu zneplatniť výsledok náhľadu; kontrola musí ukazovať aj svoj rozsah a aktuálnosť. Crawling externých webov, kontrola PDF a nepretržitý produkčný browser runner sú ďalšie etapy.

**Overenie hodnoty.** Sledovať počet kritických nálezov na publikovaných stránkach, čas od nálezu po opravu a podiel stránok po termíne kontroly. Akceptačný test: chyba zablokuje publikovanie cez editor aj scheduler, pracovná verzia sa uloží a oprava starý nález uzavrie.

**Oporné body v kóde:** [LinkCheckService](../../src/main/java/sk/iway/iwcm/editor/service/LinkCheckService.java), [EditorService](../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java), [ApproveService](../../src/main/java/sk/iway/iwcm/editor/approve/ApproveService.java), [DocPublishService](../../src/main/java/sk/iway/iwcm/doc/DocPublishService.java).

### 2. Riadené preklady a aktualizácie jazykových verzií

**Použitie a prínos.** Po zmene slovenského produktu sa anglická a nemecká verzia označia ako zastarané. Koordinátor vytvorí prekladovú dávku, pridelí kontrolóra a sleduje stavy „na preklad“, „na kontrolu“, „aktuálne“. Prekladateľ vidí starý a nový zdroj, existujúci preklad a návrh. Ručne upravená živá stránka zostáva zachovaná, kým človek nepotvrdí novú pracovnú verziu.

**Rozdiel oproti dnešku.** WebJET už obsah prekladá a prepája cez `syncId`. Nová hodnota je evidencia toho, **ku ktorej zdrojovej revízii preklad patrí**, detekcia zastarania a dokončenie práce po jazykovej korektúre. WPML translation memory a glosár, Storyblok/Smartling úlohy aj kontrola návrhov v Contentful ukazujú relevantné konkurenčné postupy; nejde o návrh nového prekladového enginu.

**Implementácia.** Nové `TranslationJob` a `TranslationItem` evidujú zdrojový a cieľový `docId`, explicitne zvolený zdrojový jazyk, snapshot/hash oboch vstupov, stav a kontrolóra. `syncId` použijeme na dohľadanie väzieb; nesmie sa predpokladať, že je vždy jednoznačná jedna stránka na jazyk, pretože dnešná administrácia vie evidovať aj chybné mapovanie. Zmenu zdroja zachytí existujúci publikačný/ukladací tok podľa nastavenej politiky, bez vytvárania prekladových slučiek medzi jazykmi.

Spracovanie dávky pôjde cez perzistentnú DB frontu a existujúci `TranslationService`; výstup najskôr do návrhu, potom cez `EditorFacade` do pracovnej verzie. Keď sa počas prekladu zmení zdroj alebo cieľ, položka vyžaduje nové porovnanie. Doplniť treba režim, v ktorom automatické zrkadlenie neprepíše cieľ riadený novou úlohou. Snímky vstupov uchovávať aj v úlohe, lebo pracovná história sa môže vymazať.

**Rozsah a hranice.** Prvá verzia sleduje zmeny po poliach: názov, navigačný názov, perex, telo a zvolené textové voliteľné polia. Pri tele stránky ukáže diff a vyžiada kontrolu celého návrhu. Spoľahlivé automatické zlúčenie zmenených odsekov v ľubovoľnom HTML nepatrí do prvej verzie. Rovnaké schválené vstupy možno opätovne použiť podľa presného hashu; plná segmentová pamäť, fuzzy matching, externé TMS konektory a univerzálny glosár pre všetkých providerov sú ďalšie etapy. HTML a `!INCLUDE(...)!` treba validovať; lokálny model podporujúci iba čistý text nie je náhradou HTML prekladača.

**Overenie hodnoty.** Merať čas medzi zmenou zdroja a dokončením prekladov, počet zastaraných jazykových verzií a objem opakovaných prekladov. Akceptačný test: zmena cieľa počas bežiacej úlohy nespôsobí jeho prepísanie; nová pracovná verzia prejde existujúcim schvaľovaním.

**Oporné body v kóde:** [DocMirroringServiceV9](../../src/main/java/sk/iway/iwcm/components/structuremirroring/DocMirroringServiceV9.java), [TranslationService](../../src/main/java/sk/iway/iwcm/system/translation/TranslationService.java), [MirroringRestController](../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringRestController.java), [EditorFacade](../../src/main/java/sk/iway/iwcm/editor/facade/EditorFacade.java).

### 3. Centrum úspešného vyhľadávania

**Použitie a prínos.** Správca uvidí napríklad, že návštevníci často hľadajú „zrušiť účet“, ale odpoveď je vedená ako „ukončenie zmluvy“. Z jedného prehľadu môže nastaviť synonymum, pripnúť správnu stránku alebo priradiť úlohu na doplnenie obsahu. Po úprave vidí, či ubudli nulové výsledky a či ľudia používajú odporúčanú odpoveď. Hodnota je zvlášť vysoká pre servisné portály, rozsiahle firemné weby a verejnú správu.

**Rozdiel oproti dnešku.** Existuje vyhľadávací engine, RAG aj štatistika hľadaných výrazov. `StatDB.addStatSearchLocal` dnes zaznamenáva dotaz a stránku vyhľadávania, nie výsledkový počet a kliknutie na odpoveď. Nový celok uzavrie postup od zistenia problému po zásah a meranie. Konkurenčným vzorom je Optimizely Search & Navigation pre CMS 12 aj novší beta Search Management portal pre Graph; verziové odlišnosti sú rozlíšené v bode 11 nižšie.

**Implementácia.** Pridať spoločnú službu `SearchInsightsService`, ktorú volajú databázové, Lucene a sémantické vyhľadávacie cesty. Zaznamenať jednu udalosť na nové vyhľadávanie, doménu/jazyk, počet dostupných výsledkov, použitý engine, technickú chybu a pri sémantike samostatne podobnosť. Identifikátor vyhľadávania spojí výsledok s kliknutím; stránkovanie nesmie vytvárať ďalšie prvé vyhľadávania. Nové agregácie uložiť do samostatných tabuliek v hlavnej DB a vizualizovať existujúcimi grafmi.

Pravidlá `SearchRule` pre presnú frázu, synonymum a odporúčaný dokument vyhodnotiť v spoločnej vrstve. Pripnutý výsledok musí prejsť rovnakou kontrolou domény, jazyka, prístupu, publikovania a priečinkového rozsahu ako organické výsledky. Zásah nemá obísť oprávnenia ani automaticky vnútiť dokument ako dôkaz do RAG odpovede. Technické zlyhanie enginu sa eviduje oddelene od úspešného vyhľadávania s nulovým výsledkom.

**Rozsah a hranice.** Prvá verzia: prehľad problémových výrazov, nulové výsledky, kliknutia, presné odporúčania a jednoduché synonymá pre verejné CMS vyhľadávanie. Pokryť aj príslušné headless vstupy alebo v UI výslovne označiť rozsah merania. RAG spätná väzba a automatické zoskupovanie tém môžu nasledovať. Podobnosť nie je pravdepodobnosť správnej odpovede a nízke CTR môže znamenať, že odpoveď používateľ prečítal už vo výsledku. Vyhľadávacie frázy môžu obsahovať osobné údaje: zaviesť obmedzenú retenciu, maskovanie zjavne citlivých údajov a možnosť ich text vôbec neuchovávať.

**Overenie hodnoty.** Sledovať podiel nulových výsledkov, výsledky pred/po zmene konkrétneho pravidla a dobrovoľné hodnotenie užitočnosti. Akceptačný test: pripnutie nikdy neodhalí skrytú alebo zaheslovanú stránku; rovnaký dotaz má oddelené pravidlá pre domény a jazyky.

**Oporné body v kóde:** [SearchAction](../../src/main/java/sk/iway/iwcm/doc/SearchAction.java), [LuceneSearchAction](../../src/main/java/sk/iway/iwcm/doc/LuceneSearchAction.java), [SemanticSearchAction](../../src/main/java/sk/iway/iwcm/doc/SemanticSearchAction.java), [SemanticSearchService](../../src/main/java/sk/iway/iwcm/rag/search/SemanticSearchService.java), [StatDB](../../src/main/java/sk/iway/iwcm/stat/StatDB.java), [HeadlessSearchService](../../src/main/java/sk/iway/iwcm/headless/service/HeadlessSearchService.java).

### 4. Experimenty s konverznými cieľmi

**Použitie a prínos.** Marketing zvolí cieľ „úspešne odoslaný formulár Žiadosť o ponuku“, pripraví dve verzie stránky a spustí experiment. V CMS vidí návštevníkov vystavených variantu, konverzie, konverzný pomer a neistotu výsledku. Po ukončení môže víťaznú verziu predložiť na bežné schválenie a publikovanie. Výsledkom je rozhodovanie podľa dokončenej akcie, nie len podľa počtu zobrazení.

**Rozdiel oproti dnešku.** Už existujú A/B kópie, pomer návštevnosti, split testy a štatistiky. Nové sú samostatný životný cyklus experimentu, vlastná identifikácia návštevníka v experimente, konverzný cieľ a atribúcia. Storyblok Experiments má podobné riadenie variantov, ale výsledky importuje z analytickej platformy. WebJET má príležitosť priamo spojiť vlastné formuláre, renderovanie a vyhodnotenie.

**Implementácia.** Entity `Experiment`, `ExperimentVariant` a `ConversionGoal`; atribúcia a udalosti oddelené od existujúcich globálnych štatistík. Rozšíriť rozhodovanie pri routovaní v `ABTesting.getVirtualPathDocId`, ktoré volá `PathFilter`, o voliteľnú konfiguráciu konkrétneho experimentu. Pre stránky mimo nového režimu zachovať dnešné správanie. Priradenie variantu bude stabilné v rámci experimentu; záznam expozície vznikne až pri skutočnom zobrazení vhodnej stránky.

Prvý konverzný typ napojiť na úspešne prijaté odoslanie viackrokového formulára, s deduplikáciou podľa odoslania a s podmienkou predchádzajúcej expozície. Druhý jednoduchý typ môže byť návšteva určenej cieľovej stránky. Meranie viazať na návštevu s explicitným atribučným oknom; bez cross-device profilovania. Variant a meranie musia rešpektovať nastavenie súhlasov a cache/CDN. Pre verejné read-only uzly použiť podporovanú zapisovaciu cestu; nespoliehať sa na existujúce formulárové počítadlá, ktoré majú v takom nasadení obmedzenia.

**Rozsah a hranice.** Dva varianty, jedna cieľová metrika, jeden aktívny experiment na stránku, štandardne pevné trvanie a manuálne ukončenie. Primárne serverovo renderované stránky; samostatný headless frontend potrebuje osobitné zapojenie priradenia a expozícií. Výsledok ukáže aj „nedostatok dát“ a interval neistoty; automatické vyhlasovanie víťaza priebežným sledovaním p-hodnoty nie je vhodné. Štatistický postup a pravidlá zastavenia treba zvoliť pred implementáciou. Multivariačné testy, adaptívne prideľovanie návštevnosti a CDP sú mimo prvej verzie.

**Overenie hodnoty.** Merať počet dokončených experimentov s použiteľnými dátami a rozdiel konverzných pomerov podľa vopred dohodnutého postupu. Akceptačný test: opakovaný callback formulára nepridá druhú konverziu, experimenty si neprepisujú priradenia a nasadenie víťaza neobíde schvaľovanie.

**Oporné body v kóde:** [ABTesting](../../src/main/java/sk/iway/iwcm/components/abtesting/ABTesting.java), [PathFilter](../../src/main/java/sk/iway/iwcm/PathFilter.java), [SaveFormService](../../src/main/java/sk/iway/iwcm/components/multistep_form/rest/SaveFormService.java), [StatWriteBuffer](../../src/main/java/sk/iway/iwcm/stat/StatWriteBuffer.java).

### 5. Publikačné balíky kampaní

**Použitie a prínos.** Tím pripraví napríklad produktovú kampaň s dvadsiatimi stránkami, jazykovými verziami a prílohami. Balík ukazuje konkrétne revízie, stav schválenia, náhľady, konflikty a termín spustenia. Posunutie kampane alebo zistenie, kto ešte nedodal obsah, je jedna operácia nad balíkom. Pri publikovaní je viditeľný výsledok každej položky a prípadné zlyhanie.

**Rozdiel oproti dnešku.** Plánovanie jednotlivých stránok aj chránené súbory existujú. Nová hodnota je spoločný životný cyklus a koordinácia. Contentful Launch, Sanity Releases, Storyblok Releases a Drupal Workspaces potvrdzujú tento produktový smer; úplný model paralelných workspaces by však prekročil navrhovaný rozsah.

**Implementácia.** `ContentRelease` a `ReleaseItem` uchovajú termín, doménu, zoznam konkrétnych `docId/historyId`, hash/snímku schváleného obsahu a stav každého kroku. Balík rezervuje jeden čakajúci plán na stránku. Zmena priradenej revízie, súboru alebo termínu zneplatní pripravenosť; aktívne referencie ochrániť aj pred čistením histórie. Kontrola pripravenosti overí oprávnenia, schválenie, revízie, prílohy a konflikty cieľových ciest pred začiatkom publikovania.

Koordinátor bude volať znovupoužitý publikačný postup z `DocPublishService` a `PlannedChangesService`. Aktuálne `copyDHistory` nie je verejné orchestračné API a má kontrolu splatnosti aj lokálny JVM zámok; treba ho primerane refaktorovať. Pôvodný automatický publisher musí spravované revízie vynechať alebo delegovať koordinátoru. DB zámok/lease zabráni súbežnému spusteniu na dvoch uzloch. Postup zapíše dokončené kroky, pri chybe zastaví ďalšie a umožní bezpečné opakovanie. Cache a nadväzné indexy obnoví cez existujúce mechanizmy.

**Rozsah a hranice.** Prvá verzia: jeden web/doména, existujúce stránky a ich podporované plánované polia a médiá, prehľad náhľadov jednotlivých revízií, spoločný termín a kontrola pripravenosti. Zmeny stromu, URL, atribútov, šablón, e-shopu a ľubovoľných aplikačných dát sú mimo balíka. Existujúci zákaz odkazovania na plánované súbory inej stránky zostáva rešpektovaný. Úplný budúci web s prepojenou navigáciou nie je automaticky výsledkom existujúceho historického náhľadu.

Publikovanie je **koordinované, nie atómové**: počas spracovania môžu návštevníci vidieť kombináciu starých a nových stránok. Stav „čiastočne publikované“ je súčasť produktu. História stránky sama neumožňuje úplný rollback prepísaných súborov, médií a už odoslaných udalostí; prvá verzia preto sľubuje obnoviteľný postup a opakovanie, nie univerzálne tlačidlo návratu celej kampane. Toto je funkcia s najväčšou neistotou odhadu.

**Overenie hodnoty.** Merať čas prípravy a preplánovania kampane, počet chýb pred spustením a čas obnovy z čiastočného zlyhania. Pred realizáciou vyčleniť 3–5 dní technického overenia: pád po publikovaní prvej položky, reštart, druhý uzol a súbežný pôvodný scheduler. Akceptačný test musí preukázať, že položka sa nevykoná dvakrát a novšia živá úprava sa bez vyriešenia konfliktu neprepíše.

**Oporné body v kóde:** [DocPublishService](../../src/main/java/sk/iway/iwcm/doc/DocPublishService.java), [PlannedChangesService](../../src/main/java/sk/iway/iwcm/doc/PlannedChangesService.java), [DocHistoryRepository](../../src/main/java/sk/iway/iwcm/doc/DocHistoryRepository.java).

## Odporúčané poradie realizácie

Začať centrom kvality a centrom vyhľadávania. Pri postupnej práci uvedeného tímu ide orientačne o 9–13 týždňov pre obe prvé verzie. Pre zákazníkov s veľkým podielom viacjazyčného obsahu môže mať prekladové centrum vyššiu prioritu než vyhľadávanie. Experimenty nasadiť najprv u zákazníka s dostatočnou návštevnosťou a jasným formulárovým cieľom. Publikačné balíky zaradiť po technickom overení koordinácie.

Nové moduly majú používať existujúce DataTables, oprávnenia, audit, automatizované úlohy a widgety. Doménový aj tenantový rozsah treba odvodiť na serveri a aplikovať tiež na ID operácie a úlohy na pozadí. Vlastný dátový model nestačí na automatické oddelenie domén. Pri ukladaní obsahu zachovať kompletný editorový a schvaľovací tok. Spoločné časti vytvárať až z konkrétnych potrieb prvých modulov; univerzálny workflow engine nie je podmienkou žiadneho návrhu.

Vývojové testy majú pokryť domény a oprávnenia, súbežné zmeny a konkrétne zlyhania opísané vyššie. Pri kampaniach navyše cluster a opakovanie; pri experimentoch expozície a deduplikáciu; pri prekladoch ochranu ručnej práce. Tento dokument je návrh, nie implementácia týchto rozšírení.

## Dvanásť pozorovaní z konkurencie

### 1. Kontrola obsahu má byť súčasťou publikovania

Storyblok má v editore kontrolu prístupnosti. Zobrazuje porušenia, upozornenia vyžadujúce manuálnu kontrolu a úspešné kontroly; nálezy pochádzajúce z obsahu vie prepojiť na príslušný blok. Vyžaduje zapojenie JavaScript knižnice vývojárom. Výsledky neuchováva: ďalšia kontrola alebo obnovenie stránky ich nahradí. Predvolene kontroluje pravidlá od WCAG 2.0 A po WCAG 2.2 AA. To nie je záruka úplnej zhody celého webu. [Storyblok Accessibility checker](https://www.storyblok.com/docs/manuals/accessibility-checker).

WordPress získava podobnú publikačnú disciplínu cez doplnok PublishPress Checklists, nie cez samotné jadro. Úlohy môžu byť odporúčané alebo povinné; povinné blokujú publikovanie. Autor dokumentuje kontroly odkazov, alternatívnych textov a integrácie so SEO doplnkami. Niektoré pravidlá, napríklad zakázané nadpisy či slová, sú platené. [PublishPress Checklists](https://wordpress.org/plugins/publishpress-checklists/).

**Príležitosť:** spoločný prehľad kvality, uchovanie výsledkov na konkrétnej revízii a jasná cesta od nálezu k oprave. Automatické kontroly odlíšiť od ľudského posúdenia.

### 2. Správa obsahu pokračuje aj po publikovaní

Umbraco Workflow Content Reviews sleduje termín ďalšej revízie, poslednú revíziu, interval a skupinu zodpovedných recenzentov. Dashboard filtruje obsah po termíne a recenzent môže potvrdiť kontrolu či upraviť ďalší termín. Ide o oficiálny doplnok Workflow. Má bezplatnú aj licencovanú verziu; bezplatná obmedzuje napríklad konfiguráciu revízií podľa typu dokumentu a počet schvaľovacích skupín. [Content Reviews](https://docs.umbraco.com/umbraco-workflow/workflow-section/content-reviews), [licencovanie Workflow](https://docs.umbraco.com/umbraco-workflow/installation/licensing).

**Príležitosť:** obsahový vlastník a termín kontroly spolu s problémami kvality pomôžu veľkým webom udržať správne kontakty, podmienky a služby. Samotný zoznam starých stránok je slabší produkt než pridelenie a uzavretie práce.

### 3. Kampaň je skupina zmien s vlastným životným cyklom

Contentful Launch spája položky a médiá do release, umožňuje plánovanie, kalendár a sledovanie workflow. Releases API je podľa dokumentácie dostupné všetkým zákazníkom, ale príslušné webové rozhranie vyžaduje zakúpenú aplikáciu Launch. API obmedzuje release na 200 položiek alebo médií. [Launch](https://www.contentful.com/help/launch/), [Releases API](https://www.contentful.com/developers/docs/references/content-management-api/releases/).

Sanity Content Releases má samostatné verzie dokumentov, spoločný náhľad, validáciu a plánovanie. Dostupnosť je viazaná na vybrané Enterprise plány. Dokumentácia uvádza maximum 1 000 dokumentov a 100 MB JSON dát v release; veľké releases publikuje po dávkach, takže medzi sprístupnením položiek môžu byť malé oneskorenia. Ani konkurenčný názov „release“ teda sám osebe neznamená atómovú viditeľnosť všetkých zmien. [Sanity Content Releases](https://www.sanity.io/docs/user-guides/content-releases).

Storyblok Releases pridáva porovnanie proti aktuálnemu obsahu, riešenie nezhôd, prístup vybraných rolí a token pre náhľad release. Je to aplikácia vyžadujúca Premium plán. [Dokumentácia Releases](https://www.storyblok.com/docs/apps/releases), [dostupnosť aplikácie](https://www.storyblok.com/apps/releases_only).

**Príležitosť:** ucelená príprava a kontrolované spustenie kampane. Pre WebJET môže byť realistický orchestrátor nad existujúcim publikovaním, so stavom každej položky a riešením čiastočného zlyhania.

### 4. Drupal oddeľuje rozpracovaný súbor zmien od živého webu

Core modul Workspaces umožňuje prepínať pracovné priestory, meniť v nich obsah a publikovať obsah priestoru do Live. Dokumentácia popisuje aj konflikt: ak bola entita upravená v inom priestore, nemožno ju zároveň upravovať v Live. Workflows a Content Moderation sú ďalšie moduly jadra pre stavy a prechody revízií. [Using workspaces](https://www.drupal.org/docs/8/core/modules/workspace/using-workspaces), [Workflows overview](https://www.drupal.org/docs/8/core/modules/workflows/overview).

**Príležitosť:** prehľad zmien a konfliktov má obchodnú hodnotu, ale kopírovať celý model workspaces by bolo podstatne náročnejšie než obmedzený release balík stránok. Staršiu stránku modulu `Workspace` v katalógu doplnkov nemožno zamieňať s core `Workspaces`.

### 5. Preklady potrebujú pamäť, terminológiu a riadenie aktualizácií

WPML je doplnok WordPressu. Jeho translation memory opakovane používa hotové preklady viet pre rovnakú jazykovú dvojicu. Advanced Translation Editor podporuje aj približné zhody; Classic editor presné zhody. Pamäť sa aktualizuje až po dokončení prekladovej dávky. [WPML Translation Memory](https://wpml.org/documentation/translating-your-contents/how-wpml-keeps-track-of-your-translations/).

Glosár určuje preferované preklady, neprekladané výrazy a kontext. Podporujú ho PTC a DeepL, nie Google a Microsoft. Zmena glosára sama neopraví už hotové preklady; treba spustiť ich aktualizáciu. To je konkrétny príklad rozdielu medzi prekladacím tlačidlom a prevádzkou viacjazyčného obsahu. [WPML Glossary](https://wpml.org/documentation/translating-your-contents/glossary/).

**Príležitosť:** vo WebJET nadviazať na jazykové väzby a existujúce prekladové enginy. Najprv evidovať, z ktorej zdrojovej revízie preklad vznikol, odhaliť zastaranie a vyžiadať kontrolu. Segmentová pamäť môže nasledovať po spoľahlivom mapovaní obsahu.

### 6. Automatický preklad nevyrieši celý lokalizačný proces

Storyblok AI Translations prekladá označené textové polia; dokumentácia ho uvádza ako beta funkciu Labs. Podporuje slovenčinu aj češtinu. Neprekladá metadata assetov, napríklad titulky a copyright. Aplikácia Smartling zasa umožňuje zadať prekladovú úlohu pre priečinok, jazyk a cieľový workflow stav, pričom výsledok importuje asynchrónne. Je to integrácia vyžadujúca účet Smartling. [AI Assistance](https://www.storyblok.com/docs/manuals/ai-assistance.html), [Smartling app](https://new-www.storyblok.com/docs/apps/smartling).

**Príležitosť:** jasne ukázať rozsah prekladovej úlohy, nepreložené časti a stav kontroly. Import výsledku musí zistiť, či sa medzičasom nezmenil zdroj alebo cieľový preklad.

### 7. Silnejší AI produkt je správa návrhov nad celou sadou obsahu

Contentful bulk AI Actions spracúva obsah na pozadí a ukladá návrhy do spoločnej obrazovky na kontrolu. Tím môže návrhy schváliť, odmietnuť alebo upraviť v diff pohľade pred aplikovaním. Systém zisťuje zastarané vstupy pri súbežných zmenách. Dokumentácia uvádza najviac 200 položiek v operácii, uchovanie výsledkov do siedmich dní, oprávnenia na spustenie a samostatné editačné práva na aplikovanie. Použitie spotrebúva platené AI jednotky. [Run AI Actions in bulk](https://www.contentful.com/help/ai-automations/ai-actions/run-ai-actions-in-bulk/).

**Príležitosť:** tieto mechanizmy sú vhodné ako spoločný základ pre prekladové a opravné úlohy. Samotné generovanie textu by vzhľadom na už existujúce AI schopnosti WebJET nebolo dostatočne novým návrhom.

### 8. Experiment potrebuje cieľ, varianty, prevádzkové stavy a výsledok

Storyblok Experiments má váhy variantov, stavy draft/running/paused/finished a možnosť vybrať víťaza. Výsledky dostáva z analytickej platformy cez API; samotná existencia variantov teda neznamená hotové meranie. Je dostupný len na niektorých plánoch a stále v beta režime Labs. Jedna story môže byť v jednom experimente; varianty majú vlastné workflow. Dokumentácia uvádza obmedzenia spolupráce s Releases, Pipelines aj zálohami. [Storyblok Experiments](https://www.storyblok.com/docs/manuals/experiments).

**Príležitosť:** k existujúcim WebJET A/B variantom doplniť definovaný konverzný cieľ, expozície, vyhodnotenie a kontrolované nasadenie víťaza. V prvej verzii sa vyhnúť automatickému rozhodovaniu pri nedostatku dát.

### 9. Hodnota obsahu sa meria aj dokončenou akciou

Parse.ly umožňuje sledovať konverzie, ako registráciu newslettera, nákup či stiahnutie dokumentu, a priraďovať ich obsahu pomocou atribučných modelov. Dokumentácia FAQ rozlišuje last touch, last post touched, first touch a linear; upozorňuje napríklad na závislosť last post touched od správneho typu stránky. Parse.ly je analytický produkt, nie funkcia jadra WordPressu. [Conversions](https://docs.parse.ly/user-handbook/measuring-reporting-performance/calculations/conversions/), [Conversion FAQs](https://docs.parse.ly/user-handbook/dashboard/optimization-menu/conversions-tab/faqs/).

**Príležitosť:** výsledky kampane a experimentu ukázať redaktorovi v CMS. Pre prvú verziu stačí zrozumiteľná atribúcia podľa dohodnutého pravidla, napríklad konverzia počas návštevy po zobrazení variantu; netreba budovať plnú CDP.

### 10. Opakované použitie obsahu potrebuje zobrazenie dopadov

Xperience by Kentico používa odkazy na obsahové položky namiesto kopírovania. Content Reuse Locator zobrazuje, ktoré stránky alebo položky obsah používajú, aby redaktor pred úpravou či odstránením poznal jej dosah. [Manage content in Content hub](https://docs.kentico.com/modules/content-management-essentials/manage-content-hub-essentials).

**Príležitosť:** prepojenia a analýza dopadov sú užitočný doplnok kvality alebo release balíkov. Úplný všeobecný Content Hub by však vyžadoval väčší zásah do dátového modelu, preto nie je automaticky vhodným krátkodobým návrhom pre WebJET.

### 11. Optimalizácia vyhľadávania spája analytiku s redakčným zásahom

Optimizely Search & Navigation spája štatistiku vyhľadávania s optimalizáciou. Dokumentácia odporúča reagovať na chýbajúce výsledky doplnením obsahu, na odlišné pomenovanie synonymom a na potrebu zvýrazniť odpoveď cez best bet. [Use Search & Navigation](https://docs.optimizely.com/cms-12/docs/use-search-and-navigation), [Optimize search results manually](https://docs.developers.optimizely.com/content-management-system/v1.1.0-search-and-navigation/docs/optimize-search-results-manually). Pri hodnotení WebJET metrík treba rátať aj s tým, že nízke CTR samo osebe nemusí znamenať problém: návštevník môže odpoveď prečítať už vo výsledku.

Best bets možno obmedziť na web a jazyk, upraviť ich titulok a popis a smerovať na interný aj externý obsah. Vývojár ich musí zapnúť; zhodujú sa s presnou frázou, bez stemmingu. [Promote content with best bets](https://docs.optimizely.com/cms-12/docs/promote-content-with-best-bets).

**Verziová hranica:** Search & Navigation je služba pre existujúce CMS 12 implementácie. Oficiálne FAQ uvádza, že CMS 13 vyžaduje prechod na Optimizely Graph a balíky Find sa na CMS 13 neprenášajú. [CMS 13 FAQ](https://www.optimizely.com/contentassets/0328dc2288d04d18a98c50cd69beda5e/cms-13-qa.pdf). Graph už má aj beta **Search Management portal**, v ktorom marketér spravuje pripnuté výsledky. Vyžaduje Opti ID, prístup ku Graph a jednorazové zapojenie vývojárom; dokumentácia tiež opisuje zapnutie trackingu pre Overview dashboard. Nemožno ho označiť za riešenie dostupné iba cez API ani zamieňať s pôvodným CMS 12 rozhraním. [Pinned results in Search Management portal](https://docs.optimizely.com/graph/docs/pinned-results-in-search-management-portal).

**Príležitosť:** WebJET môže nad existujúcim vyhľadávaním ponúknuť jednotný postup „dotaz zlyháva → oprav synonymum/pripni odpoveď/zadaj doplnenie obsahu → over zlepšenie“. Pri RAG rozlišovať nulové výsledky od nízkeho skóre a netvrdiť, že skóre je pravdepodobnosť správnej odpovede.

### 12. Vyhľadávací engine a redakčné nástroje nemusia byť jeden produkt

Aktuálne Xperience by Kentico podľa vlastnej migračnej dokumentácie nemá natívne vyhľadávanie; používa integrácie Lucene, Azure AI Search alebo Algolia. Oficiálna vzdelávacia mapa opisuje code-first indexovanie a pri Lucene lokálny index aplikácie. Nemožno mu pripisovať staré Smart Search UI z Kentico 8 či Xperience 13 len preto, že je staršia dokumentácia stále dostupná. [Choose a search integration](https://docs.kentico.com/modules/upgrade-overview/adjust-your-code-and-adapt), [Developer learning map](https://docs.kentico.com/guides/development/developer-learning-map).

**Príležitosť:** pre WebJET môže byť konkurenčnou výhodou práve editorovo dostupné zlepšovanie vyhľadávania nad existujúcim enginom bez povinnej samostatnej služby.

