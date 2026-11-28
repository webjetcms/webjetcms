# Podklady pre vývojárov a agentúry

Stav prieskumu: 15. 11. 2026. Zdrojom je pracovná vetva `feature/website-content`, dokumentácia a overené vstupné body implementácie. Ide o podklad k obsahovej stratégii, nie o schválené verejné texty. Konkrétne nasadenie môže používať staršiu verziu; schopnosť v aktuálnom zdrojovom kóde nie je prísľubom dostupnosti vo všetkých licenciách a verziách.

## Záver pre obsah

Samostatná cesta **Pre vývojárov** má oporu v produkte: WebJET CMS umožňuje vytvárať Java/Spring aplikácie, administráciu nad JPA dátami a vlastné prezentačné vrstvy cez REST. Má zdrojový kód, základný zákaznícky projekt, dokumentáciu a skilly pre AI nástroje. Toto je konkrétnejší dôvod na návštevu stránky než všeobecné tvrdenie o „moderných technológiách“.

Stránka **Pre agentúry** môže vysvetliť správu viacerých klientskych webov, odovzdanie redakčnej práce klientovi a rozšírenie o zákaznícke aplikácie. Ponuka spolupráce však potrebuje obchodné rozhodnutie: z podkladov nemožno odvodiť partnerský program, zľavy, SLA, migráciu z WordPressu na kľúč ani školenie zdarma.

Primárne publikum hlavnej stránky zostáva podľa zadania veľká organizácia a verejná správa. Vývojársky a agentúrny obsah má mať vlastný vstup a konkrétny ďalší krok, aby úvodná stránka nevysvetľovala tri odlišné nákupné procesy naraz.

## Overené technické tvrdenia

| Oblasť | Čo podklady potvrdzujú | Použiteľný obsahový dôsledok | Zdroj a obmedzenie |
| --- | --- | --- | --- |
| Java a Spring | Aktuálny build deklaruje Java 17, Spring Framework/MVC `7.0.+`, Spring Data `4.0.+`, Spring Security `7.1.+`. | Vývojári môžu pracovať s Java/Spring stackom a používať existujúce znalosti tímu. | [build.gradle](../../build.gradle), [SpringAppInitializer](../../src/main/java/sk/iway/iwcm/system/spring/SpringAppInitializer.java). Čísla sú deklarácie aktuálnej vetvy, nie garancia pre staré inštalácie. |
| Spring Boot | Dokumentácia označuje `2026.0-boot-SNAPSHOT` ako vývojovú verziu so Spring Boot 4. Aktuálny build používa servlet inicializátor `WebApplicationInitializer`. | Java/Spring možno komunikovať teraz; Boot iba ako pripravovanú alebo vývojovú verziu podľa dohody s vlastníkom produktu. | [Predpoklady a verzie](../sk/install/versions.md). Nepísať, že celá aktuálna produktová ponuka už štandardne beží na Spring Boot. Nesľubovať dátum uvedenia. |
| Vlastné aplikácie | `@WebjetComponent` je Spring komponent. Aplikácia rozširuje `WebjetComponentAbstract`, jej metódy obsluhujú požiadavky a vracajú pohľady. | Zákaznícke funkcie možno vytvárať v Java/Spring a vložiť do spravovaného obsahu. | [Spring MVC aplikácia](../sk/custom-apps/spring-mvc/README.md), [WebjetComponent](../../src/main/java/sk/iway/iwcm/system/annotations/WebjetComponent.java), implementácia [MultistepFormApp](../../src/main/java/sk/iway/iwcm/components/multistep_form/mvc/MultistepFormApp.java). |
| AppStore | Anotácia `@WebjetAppStore` poskytuje názov, opis, ikonu, nastavenia a zobrazenie zákazníckej aplikácie v editore. | Vývojár pripraví aplikáciu; redaktor ju potom vkladá a nastavuje v rozhraní CMS. | [Zobrazenie v zozname aplikácií](../sk/custom-apps/appstore/README.md). AppStore tu znamená katalóg v CMS, nie prísľub verejného marketplace alebo predaja aplikácií tretích strán. |
| JPA a administrácia | Dokumentovaný vzor tvorí JPA entita, Spring Data repozitár, REST controller a DataTable editor. Validácie a audit majú existujúce mechanizmy. | Pri vlastných evidenciách môže tím nadviazať na hotové administračné prvky. | [Položka v menu admin časti](../sk/custom-apps/admin-menu-item/README.md), [Práca s JPA](../sk/developer/backend/jpa.md), [UserDetailsRepository](../../src/main/java/sk/iway/iwcm/components/users/userdetail/UserDetailsRepository.java). Neznamená to hotovú konkrétnu CRM/ERP integráciu. |
| Thymeleaf | Verejné aplikácie a šablóny podporujú Thymeleaf; v systéme zároveň existujú JSP a ďalšie pohľady. | Java tímy môžu pripravovať serverom renderované stránky a vlastné aplikácie. | [Spring MVC aplikácia](../sk/custom-apps/spring-mvc/README.md), [build.gradle](../../build.gradle). Netvrdiť, že celý produkt je výlučne Thymeleaf alebo bez legacy častí. |
| REST integrácie | Verejné služby a autorizácia API kľúčom sú dokumentované; Swagger možno zapnúť. | Vlastné prepojenia možno postaviť cez REST API. | [Verejné služby](../sk/custom-apps/spring/public-services.md), [API autorizácia](../sk/custom-apps/spring/api-auth.md), [ApiTokenAuthFilter](../../src/main/java/sk/iway/iwcm/system/spring/ApiTokenAuthFilter.java). API potrebuje nastavenie prístupov a autorizácie. |
| Headless | REST pod `/rest/headless/v1/` poskytuje obsah, navigáciu, novinky, vyhľadávanie a formuláre; existuje dokumentovaná ukážka frontendu. | Redakcia používa CMS, vývojár si môže vybrať vlastnú technológiu frontendu. | [Headless režim](../sk/frontend/headless/README.md), [HeadlessPageRestController](../../src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java), [HeadlessActionsRestController](../../src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java), [HeadlessNewsRestController](../../src/main/java/sk/iway/iwcm/headless/rest/HeadlessNewsRestController.java). Niektoré existujúce aplikácie generujú HTML a potrebujú jQuery/Bootstrap; nie je to sľub bezpracnej kompatibility každej aplikácie s každým frontendom. |
| Zdrojový kód | Dokumentácia odkazuje na repozitáre `webjetcms/webjetcms` a `webjetcms/basecms`. | Vývojár má konkrétny vstup ku kódu a základnému zákazníckemu projektu. | [Programovanie zákazníckych aplikácií](../sk/custom-apps/README.md). Verejná dostupnosť kódu sa nesmie zameniť za jednotnú bezplatnú licenciu celého produktu. |
| Licenčná hranica | `LICENSE.md` uvádza dvojité licencovanie: Apache 2.0 len pre vymedzené moduly; ostatné používanie podlieha komerčným podmienkam. | Použiť presné tvrdenie o dostupnom zdrojovom kóde a odkaz na aktuálne podmienky. | [LICENSE.md](../../LICENSE.md). V tomto zadaní sa licenčná stránka nemení. |
| AI nástroje pre vývoj | Existujú oficiálne WebJET CMS skills pre Codex, GitHub Copilot a Claude Code; dokumentácia opisuje inštaláciu a pravidlá projektov. | AI časť môže osloviť aj vývojárov: postupy pre aplikácie, DataTables, PageBuilder, testy a dokumentáciu. | [WebJET CMS skills pre AI agentov](../sk/custom-apps/skills/README.md). Skills nie sú produktovou AI funkciou pre redaktora a neznamenajú podporu každej novej schopnosti v starej verzii CMS. |

## Agentúrne scenáre a ich hranice

### Viac klientskych webov

[MultiWeb](../sk/install/multiweb/README.md) je režim jednej inštalácie s riadiacou doménou a jednotlivými tenantmi. Každá doména má vlastný zoznam používateľov. Prístup k šablónam, obsahu a vybraným funkciám sa obmedzuje podľa domény a oprávnení. Riadiaca doména spravuje globálne nastavenia a funkcie s dosahom na celú inštaláciu.

Je doložiteľné povedať, že agentúra môže spravovať viac klientskych webov v jednej inštalácii. Nie je presné prezentovať to ako fyzicky nezávislé servery, samostatné databázy alebo bezpodmienečnú izoláciu všetkých modulov. Ani počet „neobmedzene webov“ či úsporu v percentách podklady nepotvrdzujú.

[Konfigurácia MultiWeb](../sk/install/multiweb/config.md) opisuje vytvorenie novej domény vrátane základnej štruktúry, skupiny šablón, šablóny a administrátora. To podporuje praktický obsah o opakovateľnom zavádzaní klientskych webov. Funkčné použitie stále vyžaduje konfiguráciu a vhodné licenčné podmienky.

### Odovzdanie obsahu klientovi

[Skupiny práv](../sk/admin/users/perm-groups.md) umožňujú pripraviť skupinu redaktorov s prístupom ku konkrétnym priečinkom, stránkam, súborom a aplikáciám. Web môže mať pripravené šablóny a zákaznícke aplikácie, ktoré redaktor používa v editore. Praktický prínos: klient upravuje zverený obsah; agentúra pripravuje a rozvíja riešenie.

Netvrdiť, že klient nepotrebuje žiadne zaškolenie alebo že agentúre úplne odpadá údržba. Existencia prístupových práv tiež sama osebe nedokazuje zhodu celého nasadenia so zákonom či bezpečnostným štandardom.

### Vlastné portály a integrácie

Java/Spring rozšírenia, DataTable administrácia a REST poskytujú základ pre zákaznícke funkcie. Tvrdenie má opisovať možnosti implementácie. Konkrétne prepojenie na CRM, ERP, bankový systém alebo identitu zákazníka musí byť podložené existujúcim konektorom alebo definované ako zákaznícky vývoj.

### Prvý projekt

Pre prvé zákaznícke nasadenie dokumentácia výslovne odporúča repozitár [basecms](https://github.com/webjetcms/basecms), nie vývojový repozitár jadra. [Zriadenie novej inštalácie](../sk/install/setup/README.md) opisuje databázu a prvotné nastavenie; [predpoklady a verzie](../sk/install/versions.md) rozlišujú podporované varianty.

Technické predpoklady na rôznych miestach dokumentácie nie sú úplne jednotné: všeobecný setup uvádza aj Tomcat 9, aktuálny build a verzia Jakarta používajú Tomcat 11. Na marketingovej stránke preto držať vysvetlenie pri Java/Spring a posielať záujemcu do verziovanej dokumentácie. Nepísať neoverený univerzálny návod „spustite za päť minút“.

## Možnosti navigácie na rozhodnutie

| Možnosť | Prínos | Podmienka |
| --- | --- | --- |
| Samostatné **Pre vývojárov** v hlavnom menu | Viditeľný vstup pre Java/Spring publikum aj agentúrnych technických lídrov. Znesie konkrétne API, kód a dokumentáciu. | Odporúčaná možnosť, ak sa má vývojárske publikum aktívne získavať. Vyžaduje samostatný HTML fragment navyše k štyrom zadaným stránkam. |
| Samostatné **Pre agentúry** | Priama cesta pre obchodný cieľ rozšíriť partnerský ekosystém. Vysvetlí MultiWeb, klientsku redakciu a prvý projekt. | Treba potvrdiť, čo je ďalší krok a akú podporu vie InterWay reálne poskytnúť. |
| Spoločné **Pre partnerov** s rozdelením na agentúry/vývojárov | Menej položiek v navigácii. | Názov môže naznačovať existujúci formálny partnerský program; bez jeho potvrdenia je menej vhodný. |
| Zatiaľ bloky na úvodnej stránke a stránke Funkcie | Zostane pôvodný rozsah a každý segment dostane odkaz na dokumentáciu/kontakt. | Slabšia vlastná vstupná stránka pre vyhľadávanie a kampane; vhodné ako dočasný krok. |

Odporúčanie pre ďalšiu diskusiu: osobitné **Pre vývojárov**; **Pre agentúry** buď osobitne, alebo ako jasná cesta z úvodnej stránky podľa potvrdenej obchodnej ponuky. V navigácii nerozširovať technické názvy do podoby „Java Spring Boot CMS“; vyhľadávací zámer možno obslúžiť nadpisom a obsahom stránky.

## Materiál pre osnovu, nie finálne texty

**Pre vývojárov:** Java/Spring základ → vlastná aplikácia vložená redaktorom → vlastná administrácia nad JPA → REST/headless → basecms a dokumentácia → AI skills → jasne označený stav Spring Boot.

**Pre agentúry:** vlastné klientské weby → klient si spravuje obsah → MultiWeb a prístupové práva → rozšírenia a integrácie → príprava prvého projektu → obchodný kontakt s ponukou potvrdenou v rozhovore.

Vhodné výzvy pre vývojárov sú „Pozrieť dokumentáciu“, „Začať s basecms“ a „Pozrieť zdrojový kód“. Pre agentúry zatiaľ iba neutrálna konzultácia projektu; názvy ako „Staňte sa certifikovaným partnerom“ alebo „Získajte partnerskú zľavu“ nemajú v prieskume podklad.

## Obrázky z dokumentácie

Rozmery sú overené zo súborov. Výber a prípadný výrez sa majú skontrolovať vizuálne pred publikovaním; táto fáza iba inventarizuje existujúce podklady.

| Súbor | Rozmery | Vhodné použitie |
| --- | --- | --- |
| [appstore.png](../sk/custom-apps/appstore/appstore.png) | 1280 × 760 | Prepojenie vývojára a redaktora: vlastné aplikácie v katalógu CMS. |
| [contact.png](../sk/custom-apps/admin-menu-item/contact.png) | 1280 × 760 | Zákaznícka evidencia v existujúcej administrácii. |
| [home.png](../sk/frontend/headless/home.png) | 1280 × 720 | Konkrétna ukážka headless webu. |
| [permissiongroups-editor-dirs.png](../sk/admin/users/permissiongroups-editor-dirs.png) | 900 × 492 | Obmedzenie redakčných práv na vybrané priečinky a stránky. |
| [domain-selector.png](../sk/redactor/admin/domain-selector.png) | 220 × 40 | Len malý doplnkový detail prepínača domén, nevhodný ako hlavný ilustračný obrázok. |

## Chýbajúce obchodné rozhodnutia

- Priorita vývojárskeho publika voči agentúram a konkrétne zaradenie do menu.
- Pripravenosť ponuky pre agentúry: konzultácia, pilotný projekt, školenie, obchodná alebo technická spolupráca.
- Spôsob komunikácie Spring Boot: vývojový preview, pripravovaná verzia, prípadne vynechanie do uvedenia.
- Konkrétna konverzia stránky: technická dokumentácia a samostatné spustenie, alebo vedená konzultácia.

Vyhľadanie súborov `*product-marketing*` a `*marketing-context*` v repozitári nenašlo samostatný produktový marketingový kontext. Obchodný rámec je preto prevzatý zo zadania používateľa; výskumné zistenia vyššie tento rámec dopĺňajú.
