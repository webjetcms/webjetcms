# ROADMAP

Zoznam plánovaných verzií WebJET CMS. Verzie sú číslované ako ```rok.týždeň``` čo je aj očakávaný čas dostupnosti danej verzie. Úlohy označené číslom tiketu sú už v riešení, úlohy označené značkou ```[x]``` sú už implementované, úlohy označené značkou ```+``` boli pridané po príprave ```roadmapy``` a ovplyvňujú dodanie ostatných úloh.

Upozornenie: jedná sa o plán, podľa potrieb a kapacít môžu byť do verzie pridané nové úlohy a niektoré naopak pre plánované do neskoršej verzie.

Vysvetlenie použitých piktogramov:

- [ ] úloha je v pláne
- [ ] ak má na konci ID priradeného tiketu už sme na úlohe začali pracovať (#id-tiketu)
- [x] úloha je hotová
- [ ] +ak začína na znak ```+``` bola úloha pridaná do zoznamu až po prvotnom vytvorení plánu, takéto úlohy sú typicky požadované zákazníkmi a ak budú realizované ovplyvnia časový plán. To znamená, že niektoré iné úlohy nemusia byť v danej verzii realizované z nedostatku času.
- [ ] ~~preškrtnuté~~ úloha presunutá do inej verzie, alebo riešená iným spôsobom, alebo zrušená. V opise je vždy číslo verzie kam sa presunula alebo dôvod zrušenia.

## 2025

- [ ] Prechod na `Jakarta EE` - zmena Java packages z `javax.servlet` na `jakarta.servlet`, pripraviť migračný skript.
- [ ] Prechod na aplikačný server Tomcat 11+.
- [ ] Prechod na `Spring` verzia 7.
- [ ] Zaviesť do projektu povinnosť použitia `SonarLint` a formátovania kódu cez `.editorconfig` alebo `Spotless` - príklad https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/build.gradle.
- [ ] Primárne používanie GitHub repozitára na vývoj.
- [ ] Zrušenie generovania artifaktov na starý `iwmsp.sk` repozitár, artefakty budú dostupné už len cez [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms).
- [ ] Aktualizácia knižnice `pd4ml` na novú verziu, jednotlivé weby si budú musieť zaobstarať licenciu samostatne, nebude už poskytovaná WebJET CMS.
- [ ] Prechod najpoužívanejších aplikácii do Spring verzie s využitím Thymeleaf šablón.
- [ ] Zrušenie `Apache Struts` framework, nahradenie `logic:present,logic:iterate,bean:write` buď za `JSTL` variantu, alebo implementácia podobnej funkcionality do `iwcm:present,iwcm:iterate,iwcm:beanrwite`.
- [ ] Presun JSP súborov, Java tried a JavaScript knižníc starej verzie 8 do `obsolete` jar archívu, ktorý nebude štandardnou súčasťou WebJET CMS. Môže byť použitý na starých projektoch, kde zatiaľ nie je vykonaná aktualizácia všetkých zákazníckych aplikácií na `Spring` verzie, ale bez podpory a aktualizácií zo strany WebJET CMS.
- [ ] Štatistika - mapa kliknutí - obnovenie funkcionality, vyriešenie problému responzívnosti (samostatná evidencia podľa šírky okna).
- [ ] Štatistika - možnosť filtrovať botov pre štatistiku chybných stránok.
- [ ] Log súbory - filtrovať podľa mena inštalácie.
- [ ] `quill` - pridať možnosť nastaviť položky menu vrátane farieb.
- [ ] Aplikácie - možnosť nákupu aplikácie pre OpenSource verziu (#55825).
- [ ] Možnosť vykonať Thymeleaf kód v hlavičke/pätičke a možno aj v tele web stránky.
- [ ] Bezpečnosť - pridať podporu generovania `nonce` pre `Content-Security-Policy` hlavičku, viď napr. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.
- [ ] Formuláre - pridať možnosť volať Java triedu pre validáciu formuláru.
- [ ] Značky - filtrovať podľa aktuálnej domény aby to bolo rovnaké ako v iných častiach.
- [ ] Import používateľov - ak nie je zadané heslo, tak vygenerovať (pre nových používateľov), ak nie je je posielaný stav `available` nastaviť na `true`.
- [ ] V testoch nejako automatizovane kontrolovať výskyt `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` čo sú nesprávne čakania bez definovaného času, spôsobia zaseknutie automatizovaných testov.
- [ ] Doplniť aplikáciu pre presmerovanie hlavnej stránky na `/sk/` alebo `/en/` podľa jazyka prehliadača.
- [ ] Upraviť vymazanie konfigurácie tak, že pri vymazaní sa jej nastaví pôvodná hodnota definovaná v `Constants`.
- [ ] Galéria - pri duplikovaní obrázka umožniť zmenu "Priečinok", aby sme vedeli duplikovať obrázky do iného ako aktuálneho priečinka.
- [ ] Hromadný email - auditovať zmeny v skupinách používateľov.
- [x] Archív súborov - prerobiť do dátových tabuliek (#57317).
- [ ] Voliteľné polia - pridať možnosť výberu viac položiek pre napojenie na číselník.
- [ ] Elektronický obchod - integrácia na platobnú bránu `GoPay` (#56609).
- [ ] Pridať možnosť autorizácie cez `OAuth2`, možnosť použiť `mock` server https://github.com/navikt/mock-oauth2-server alebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).
- [ ] Autorizácia cez ```SAML``` - integrovať knižnicu [Spring SAML](https://spring.io/projects/spring-security-saml) pre možnosť autentifikácie voči ```ADFS/SAML``` serveru.
- [x] Rezervácie - nová aplikácia pre celo dennú rezerváciu (#57389).
- [ ] Aplikácie - prerobiť dialóg nastavenia aplikácií v editore web stránok zo starého JSP na dátovú tabuľku (#57409).
- [x] Hromadný email - optimalizácia tvorby zoznamu príjemcov (#57537).
- [ ] Datatable - opraviť počítanie označených riadkov po ich vymazaní.

## 2024

- [x] Prechod na Java 17 - WebJET od verzie 2024.0 bude vyžadovať Java verzie 17. (#54425)
- [x] Výmena sady ikon z `FontAwesome` za sadu `Tabler Icons` (#56397).
- [x] Publikovanie JAR súborov do `Maven Central` (#43144).
- [x] Nová verzia aplikácie Nákupný košík/`eshop`, ukážková verzia web stránky, integrácia cez API na online fakturačné systémy, aktualizované integrácie na platobné brány (#56329,56385,56325).
- [x] Prekladové kľúče - možnosť importovať len neexistujúce kľúče (#56061).
- [ ] ~~`quill` - pridať možnosť nastaviť položky menu vrátane farieb.~~
- [ ] ~~Aplikácie - možnosť nákupu aplikácie pre OpenSource verziu (#55825).~~
- [x] Web stránky - ak je zapnutá konf. premenná `syncGroupAndWebpageTitle` a jedna stránka je nastavená ako hlavná viacerým priečinkom vypnúť premenovávanie názvu priečinku podľa hlavnej stránky. Plus keď je hlavná stránka v inom priečinku, tiež nepremenovávať (#56477).
- [x] Audit - prerobiť Zmenené stránky a Čaká na publikovanie do datatabuliek (#56165).
- [x] Blog - administrácia `bloggerov` prerobiť do datatabuliek (#56169).
- [x] Blog - komentáre - integrovať na sekciu Diskusia, pridať `bloggerom` práva aj na sekciu Diskusia (#56173).
- [x] AB testovanie - prerobenie do DT, využiť možnosti z `news` aplikácie, spraviť nastavenie konf. premenných podľa zadaného prefixu (#56177).
- [x] Kalendár udalostí - neschválené a odporúčanie udalosti - prerobiť do DT, využiť kód pre zoznam udalostí (#56181).
- [x] Editor - pri vytvorení odkazu na email to v okne automaticky pridáva http, aj keď odkaz nakoniec vloží správne s `mailto:` prefixom (#56189).
- [x] Dotazníky - prerobiť aplikáciu do datatabuliek (#55949).
- [x] +PostgreSQL - pridať podporu databázy (#56305).
- [x] Formulár ľahko - pridať možnosti ako majú štandardné formuláre (hlavne nastavenie presmerovania po odoslaní), možnosť odoslať formulár aj na používateľa (pre kontrolu) (#56481).
- [x] +Formulár ľahko - pridať podporu pre wysiwyg polia (#56481).
- [x] +Zmazať nepoužívané a vymeniť málo používané knižnice (#56265).
- [x] +MultiWeb - overiť funkčnosť, doplniť potrebné práva (#56421, #56405, #56425).
- [x] +Hromadný email - pridať podporu odhlásenia jedným klikom (#56409).
- [ ] ~~+Možnosť vykonať Thymeleaf kód v hlavičke/pätičke a možno aj v tele web stránky.~~
- [x] +Vymeniť databázový pool za `HikariCP` (#56821).
- [x] +Úrovne logovania - prerobiť na datatabuľku (#56833).
- [x] +DBPool - prechod z `Apache DBCP` na `HikariCP` (#56821).
- [x] Dokumentácia - preklad do Angličtiny (#56237,#56249,#56773).
- [x] +Dokumentácia - preklad do Češtiny (#57033).
- [x] +A/B Testovanie - vypnúť pre vyhľadávače/boty, indikovať verziu pre Ninja objekt (#56677).
- [x] +Datatabuľky - možnosť preskočiť riadok pri importe keď je chybný (#56465).
- [x] +Presmerovania - optimalizovať získanie presmerovania bez druhotného získavania presmerovacieho kódu z databázy (#53469).
- [ ] ~~+Bezpečnosť - pridať podporu generovania `nonce` pre `Content-Security-Policy` hlavičku, viď napr. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.~~
- [x] +Galéria - pridať možnosť zmeniť veľkosť obrázka.
- [ ] ~~+Formuláre - pridať možnosť volať Java triedu pre validáciu formuláru.~~
- [x] +Indexovanie súborov - doplniť do Perex-Začiatok publikovania dátum poslednej zmeny súboru (#57177).
- [x] +Prekladové kľúče - do REST služby `/rest/properties/` pridať možnosť filtrovať kľúče podľa konf. premennej, aby nebolo možné verejne získať všetky kľúče z WebJET CMS (#57202).
- [x] +Web stránky - auditovať úspešné časové publikovanie web stránky, možnosť poslať notifikáciu autorovi web stránky (#57173).
- [x] +Zobrazovať informáciu o platnosti licencie 2 mesiace pred jej exspiráciou na úvodnej obrazovke (#57169).
- [ ] ~~+Značky - filtrovať podľa aktuálnej domény aby to bolo rovnaké ako v iných častiach.~~
- [x] +Audit - doplniť HTTP hlavičku `Referrer` aby bolo možné identifikovať pôvodnú stránku z ktorej bolo volanie vykonané (#57565).
- [ ] ~~+Import používateľov - ak nie je zadané heslo, tak vygenerovať (pre nových používateľov), ak nie je je posielaný stav `available` nastaviť na `true`.~~
- [ ] ~~+V testoch nejako automatizovane kontrolovať výskyt `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` čo sú nesprávne čakania bez definovaného času, spôsobia zaseknutie automatizovaných testov.~~
- [ ] ~~+Doplniť aplikáciu pre presmerovanie hlavnej stránky na `/sk/` alebo `/en/` podľa jazyka prehliadača.~~
- [ ] ~~+Upraviť vymazanie konfigurácie tak, že pri vymazaní sa jej nastaví pôvodná hodnota definovaná v `Constants`.~~
- [x] +Galéria - pridať pole URL adresa zdroja obrázku s možnosťou zadať adresu, z ktorej sme obrázok získali, automaticky nastaviť pri použití foto banky.
- [ ] ~~+Galéria - pri duplikovaní obrázka umožniť zmenu "Priečinok", aby sme vedeli duplikovať obrázky do iného ako aktuálneho priečinka.~~
- [ ] ~~+Hromadný email - auditovať zmeny v skupinách používateľov.~~
- [ ] ~~+Archív súborov - prerobiť do dátových tabuliek (#57317).~~
- [ ] ~~+Voliteľné polia - pridať možnosť výberu viac položiek pre napojenie na číselník.~~
- [x] +Vyhľadávanie v administrácii - prerobenie do dátovej tabuľky (#57333).
- [x] +Anketa - doplnenie pôvodnej funkcionality zobrazenia štatistiky (#57337).
- [x] +Značky - doplniť možnosť definovať názov vo viacerých jazykoch, doplniť voliteľné polia (#57273,#57449)
- [x] +Aplikácie - prerobiť dialóg nastavenia aplikácií v editore web stránok zo starého JSP na dátovú tabuľku (#57157,#57161,#57409).
- [x] +Web stránky, galéria - doplniť možnosť vyhľadávať v stromovej štruktúre (#57265,#57437)

## 2023.52 / 2024.0

> Verzia 2023.52 / 2024.0 je zameraná na zlepšenie bezpečnosti, prechod z knižnice `Struts` na `Spring`.

- [x] Aktualizácia - prerobiť do nového dizajnu, doplniť možnosť aktualizácie s použitím jar packages (#55797).
- [x] Klonovanie štruktúry - prerobiť na používanie Zrkadlenie štruktúry s možnosťou prekladu (#55733).
- [x] Rating - prerobiť na Spring (#55729).
- [x] Fórum - prerobiť do Spring (#55649).
- [x] Používatelia - pridať stĺpec skupina práv do datatabuľky (#55601).
- [ ] Galéria - nový typ pre zobrazenie malého počtu fotiek s expanziou, možnosť prechodu medzi všetkými fotkami v článku (#55349).
- [ ] ~~Pridať možnosť autorizácie cez `OAuth2`, možnosť použiť `mock` server https://github.com/navikt/mock-oauth2-server alebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).~~
- [ ] ~~Autorizácia cez ```SAML``` - integrovať knižnicu [Spring SAML](https://spring.io/projects/spring-security-saml) pre možnosť autentifikácie voči ```ADFS/SAML``` serveru.~~
- [x] Web stránky - pridať možnosť obnovenia zmazaného priečinka tak, aby sa korektne nastavil web stránkam atribút `available` (#55937).
- [ ] Prieskumník - upraviť do nového dizajnu, aktualizovať JS kód elfinder (#55849).
- [x] Web stránky - pri vytváraní odkazov v dialógovom okne zapamätať posledný adresár a pri pridaní ďalšieho odkazu ho rovno použiť (#54953-29).
- [x] Kalendár udalostí - ak sa zadá udalosť v októbri na november kedy dochádza k posunu času zobrazuje sa s o hodinu posunutým časom (#56857).
- [ ] upraviť posielanie správ medzi administrátormi z vyskakovacieho okna na lepšie používateľské rozhranie.
- [x] Aplikácie - zlepšiť opis každej aplikácie, aktualizovať obrázky aplikácie (#55293).
- [x] Foto galéria - optimalizovať množstvo načítaných dát (#56093).
- [x] Foto galéria - image editor sa inicializuje pri každom otvorení okna, upraviť, aby sa použil už existujúci editor (#55853).
- [x] Datatabuľky - do automatizovaného testu doplniť testovanie funkcie Duplikovať (#56849).
- [ ] Banner - pridať možnosť používania šablón pre zobrazenie banneru, napr. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.
- [ ] Overiť ako fungujú SEO rozšírenia pre WordPress (napr. https://yoast.com/beginners-guide-yoast-seo/) a navrhnúť podobné riešenie pre WebJET CMS. Integrovať niečo ako https://github.com/thinhlam/seo-defect-checker do editora (#56853).
- [ ] Pridať blokom v PageBuilder-i nejaký data atribút s menom bloku a ten potom zobrazovať vo vlastnostiach bloku, aby sa dalo spätne identifikovať aký to je blok.
- [ ] Na úvodnej obrazovke implementovať mini aplikáciu todo.
- [ ] DT - po pridaní záznamu prestránkovať na stranu, kde sa záznam nachádza (typicky posledná strana)
- [x] Editor - pridať možnosť zobraziť pre vybrané web stránky editor typu HTML, ktorý sa nebude dať prepnúť do WYSIWYG režimu. Použité na špeciálne stránky integrujúce rôzne JavaScript aplikácie alebo špeciálne komponenty. Vhodný editor je [Ace-code](https://www.npmjs.com/package/ace-code) (#56129).
- [x] Fulltext - priečinok `files` vo web stránkach s indexom presunúť do karty System (54953-30).
- [x] +Používatelia - zamedziť zmazaniu samého seba (#55917).
- [x] +Novinky - doplniť kontrolu práv na priečinok a zobraziť len také, na ktoré ma používateľ práva (#56661).
- [x] +Aplikácie - možnosť podmienene zobraziť aplikácie podľa typu zariadenia, implementovať genericky, nastaviteľné cez UI pre Banner (#55921).
- [x] +Štatistika - obmedzenie zobrazených štatistík len na priečinky, na ktoré má používateľ právo (#55941).
- [x] +Reštauračné menu - prerobiť aplikáciu do datatabuliek (#55945).
- [x] +Presmerovania - oddeliť presmerovania podľa zvolenej domény (#55957).
- [x] +Perex skupiny - oddeliť administráciu perex skupín podľa práv používateľa na web stránky (#55961).
- [x] +Banner - podmienené zobrazenie podľa zariadenia mobil/tablet/desktop (#55997).
- [x] +Page Builder - generovanie kotiev/horného menu na základe vložených sekcií (#56017).
- [x] +Voliteľné polia - pridať pole typu `UUID` pre generovanie identifikátorov (#55953).
- [x] +Import web stránok - prerobiť do Spring (#55905).
- [x] +Monitorovanie servera - CPU - `oshi-core` nefunguje na Windows 11 (#55865).
- [x] +Hlavičky - možnosť definovať príponu súboru pre ktoré sa hlavička nastaví (#56109).

## 2023.40

> Verzia 2023.40 upravuje používateľské rozhranie na základe nových UX návrhov, zlepšuje použiteľnosť datatabuliek.

- [x] Migrácia na vyššiu verziu Javy s ```LTS``` podporou (Java 11 alebo ideálne až Java 17) (#54425).
- [x] Banner - pridať možnosť nastaviť adresárovú štruktúru stránok v ktorej sa bude banner zobrazovať a možnosť definovať výnimky kde sa nebude (#55285).
- [x] Hlavičky - pridať novú aplikáciu, ktorá umožňuje definovať ľubovoľnú HTTP hlavičku podľa URL adresy (#55165).
- [ ] ~~(presun do 2024.0)upraviť posielanie správ medzi administrátormi z vyskakovacieho okna na lepšie používateľské rozhranie~~
- [ ] ~~(presun do 2024.0)Aplikácie - zlepšiť opis každej aplikácie, aktualizovať obrázky aplikácie (#55293).~~
- [ ] ~~(presun do 2024.0)Foto galéria - optimalizovať množstvo načítaných dát~~
- [ ] ~~(presun do 2024.0)Foto galéria - image editor sa inicializuje pri každom otvorení okna, upraviť, aby sa použil už existujúci editor (#55853)~~
- [ ] ~~(presun do 2024.0)Datatabuľky - do automatizovaného testu doplniť testovanie funkcie Duplikovať.~~
- [ ] ~~(presun do 2024.0)Banner - pridať možnosť používania šablón pre zobrazenie banneru, napr. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.~~
- [ ] ~~(presun do 2024.0)Overiť ako fungujú SEO rozšírenia pre WordPress (napr. https://yoast.com/beginners-guide-yoast-seo/) a navrhnúť podobné riešenie pre WebJET CMS. Integrovať niečo ako https://github.com/thinhlam/seo-defect-checker do editora.~~
- [ ] ~~(presun do 2024.0)Pridať blokom v PageBuilder-i nejaký data atribút s menom bloku a ten potom zobrazovať vo vlastnostiach bloku, aby sa dalo spätne identifikovať aký to je blok.~~
- [x] Číselníky - v exporte sa nenachádza atribút, že je záznam zmazaný a potom po importe sa záznam aktivuje (#55541).
- [ ] ~~(presun do 2024.0)Na úvodnej obrazovke implementovať mini aplikáciu todo.~~
- [ ] ~~(presun do 2024.0)DT - po pridaní záznamu prestránkovať na stranu, kde sa záznam nachádza (typicky posledná strana)~~
- [ ] ~~(presun do 2024.0)Editor - pridať možnosť zobraziť pre vybrané web stránky editor typu HTML, ktorý sa nebude dať prepnúť do WYSIWYG režimu. Použité na špeciálne stránky integrujúce rôzne JavaScript aplikácie alebo špeciálne komponenty. Vhodný editor je [Ace-code](https://www.npmjs.com/package/ace-code).~~
- [x] Publikovanie OpenSource/Community verzie WebJET CMS na ```github.com``` (#55789,#55773,#55829,#55749).
- [x] +Datatabuľky - vo web stránkach/galérii pri nastavení iného pomeru stĺpcov sa zle zobrazujú tlačidlá (#54953-22).
- [x] +Preklady - doplniť do Zrkadlenia štruktúry možnosť prekladať aj text stránky cez [DeppL](https://www.deepl.com/docs-api/html/) (#55709).
- [ ] ~~(presun do 2024.0)+Autorizácia cez ```SAML``` - integrovať knižnicu [Spring SAML](https://spring.io/projects/spring-security-saml) pre možnosť autentifikácie voči ```ADFS/SAML``` serveru.~~
- [ ] ~~(presun do 2024.0)+Prechod zo Struts framework na Spring - postupne prepísať kód všetkých tried do Spring kontrolórov (#55389,#55469,#55489,#55493,#55469,#55501,#55609,#55613,#55617,#55701).~~
- [x] +Prerobiť administráciu zoznamu noviniek do nového dizajnu a datatabuľky, využiť čo najviac kódu z existujúceho zoznamu web stránok (#55441).
- [x] +Aktualizovať knižnicu `amcharts` pre grafy na novšiu verziu 5 (#55405).
- [x] +Prerobiť aplikáciu Novinky do nového dizajnu (#55441).
- [x] +Pridať podporu vývoja cez DevContainers (#55345).
- [x] +Monitorovanie servera - prerobiť sekcie Aplikácie, Web Stránky a SQL dotazy do nových datatabuliek (#55497).
- [x] +Web stránky - schvaľovanie - prerobiť zo `Struts` na `Spring`, doplniť testy (#55493).
- [x] +SEO - prerobiť na repozitáre a datatables (#55537).
- [x] +Autorizácia používateľov - v administrácii chýba pôvodná možnosť z v8 (#55545).
- [x] +DT - vyhľadávanie záznamu podľa ID, lepšie riešenie pre hľadanie web stránky podľa docid keď je v zozname veľa strán (#55581).
- [x] +Diskusia - prerobiť administráciu do datatables (#55501).
- [x] +Monitorovanie servera - prerobiť monitorovanie Aplikácie/Web stránky/SQL dotazy do datatables/nových grafov (#55497).
- [x] +Proxy - pridať podporu pre volanie REST služieb bez vkladania výstupu do web stránky (#55689).
- [x] +Banner - pridať podporu pre video súbory vrátane obsahového bannera (#55817).
- [x] +Prekladové kľúče - možnosť importu len jedného jazyka bez dopadu na preklady v iných jazykoch (import len zadaného stĺpca z Excelu) (#55697).
- [x] +Média - pridať podporu voliteľných polí (#55685).

## 2023.18

> Verzia 2023.18 mení požiadavku na prevádzku riešenia na Java verzie 17. Toto je zásadná zmena a vyžaduje aj zmenu na serveroch. Z toho dôvodu je vykonaná hneď na začiatku roka.

- [ ] ~~(presun do 2023.40)Migrácia na vyššiu verziu Javy s ```LTS``` podporou (Java 11 alebo ideálne až Java 17) (#54425)~~.
- [x] Integrácia kódu verzie 8 do projektu, odstránenie duplicitných stránok, zrušenie možnosti prepnutia do verzie 8, ponechanie len nových verzií stránok (#54993).
- [x] Aktualizácia CK Editor na novšiu verziu (#55093).
- [x] Formuláre - doplniť funkciu archivovať formulár (#54993).
- [ ] ~~(presun do 2023.40)Banner - pridať možnosť nastaviť adresárovú štruktúru stránok v ktorej sa bude banner zobrazovať a možnosť definovať výnimky kde sa nebude.~~
- [x] Vyhľadávanie - ```Lucene``` - pridať indexovanie jazyka na základe jazyka stránky, čiže do indexu by sa dostali len stránky v zadanom jazyku šablóny (#54273-34).
- [ ] ~~(presun do 2023.40)Hlavičky - pridať novú aplikáciu, ktorá umožňuje definovať ľubovoľnú HTTP hlavičku podľa URL adresy (#55165).~~
- [x] Atribúty stránky - doplnená možnosť nastavenia atribútov stránky v editore (#55145).
- [x] Šablóny - pridať možnosť zlúčenia šablón (#55021).
- [ ] +Audit - pridať anotáciu na primárny kľúč ak je iný ako ID, konkrétne pre editáciu prekladových kľúčov sa neaudituje hodnota ```key``` ale ```id``` ktoré je nepodstatné.
- [x] +Galéria - Na multidomain sa používala štruktúra ```/images/DOMENA/gallery``` čo teraz nie je vidieť v galérii. Je potrebne to tam nejako doplniť (#54953-4).
- [x] +Preklady - možnosť dynamickej definície podporovaných jazykov v šablónach, prekladových kľúčoch atď (#MR332).
- [x] +Bezpečnosť - logy servera sú dostupné vrámci audit práva, je potrebné to oddeliť do samostatného práva (#54953-5).
- [x] +Zlepšiť podporu DB servera Oracle, otestovať funkčnosť entít (#54953-6).
- [x] +Web stránky - pri duplikovaní web stránky sa majú duplikovať aj pripojené média (#54953-6).
- [x] +Web stránky/galéria - pridať možnosť nastaviť pomer strán pre zobrazenie stromovej štruktúry a tabuľky (#54953-7).
- [x] +Prekladové kľúče - upraviť editáciu na tabuľkové zobrazenie kde v stĺpcoch sú jednotlivé jazyky pre možnosť naraz editovať preklady vo všetkých jazykoch (#55001).
- [x] +Banner - prerobiť štatistiku zobrazenia bannerov do nového dizajnu (#54989).
- [x] +Prekladové kľúče - doplniť kontrolu počtu prekladových jazykov aby to nepadalo na ```fieldK``` (#MR344).
- [x] +Číselník - prerobiť do nového dizajnu aby bolo možné odstrániť závislosť na starej verzie datatables.net (#55009).
- [ ] +Opraviť chybu v editore napr. v aplikácii galéria - pri posune okna chytením za titulok Aplikácie sa zle posunie a následne neustále sleduje kurzor. Je potrebné potom kliknúť dole pri tlačidle OK pre vypnutie posúvania okna.
- [x] +Proxy - prerobiť do nového dizajnu (#55025).

## 2022.52 / 2023.0

> Verzia 2022.52 predstavuje dlhodobý zoznam úloh, na ktoré nevyšiel čas v podrobnejšom pláne na rok 2022.

- [ ] ~~zatiaľ zrušené: nahrať videá používania WebJET CMS automatizovaným spôsobom - https://dev.to/yannbertrand/automated-screen-recording-with-javascript-18he alebo https://www.macrorecorder.com~~
- [x] pridať štatistiku testov cez modul ```Allure``` - https://codecept.io/plugins/#allure (#54437).
- [x] pridať ```code coverage``` report, napr. cez JaCoCo - https://docs.gradle.org/current/userguide/jacoco_plugin.html (#54909).
- [ ] ~~(presun do 2023.40) upraviť posielanie správ medzi administrátormi z vyskakovacieho okna na lepšie používateľské rozhranie~~
- [ ] ~~(presun do 2023.40) Aplikácie - zlepšiť opis každej aplikácie, aktualizovať obrázky aplikácie.~~
- [x] Galéria - pridať fyzickú zmenu mena súboru (vrátane vygenerovaných s_ a o_ obrázkov) po zmene atribútu Meno súboru v editore (#39751-52).
- [x] +Datatabuľky - zapamätať si počet záznamov na stranu pre každú datatabuľku (#39751-50).
- [ ] +```Command Palette``` - pridať príkazovú paletu s integrovaným vyhľadávaním podobne ako má VS Code - https://trevorsullivan.net/2019/09/18/frictionless-user-experiences-add-a-command-palette-to-your-react-application/
- [ ] ~~(presun do 2023.40) +foto galéria - optimalizovať množstvo načítaných dát~~
- [ ] ~~(presun do 2023.40) +foto galéria - image editor sa inicializuje pri každom otvorení okna, upraviť, aby sa použil už existujúci editor~~
- [ ] ~~(presun do 2023.40) +Datatabuľky - do automatizovaného testu doplniť testovanie funkcie Duplikovať.~~
- [ ] ~~(presun do 2023.40) +Banner - pridať možnosť používania šablón pre zobrazenie banneru, napr. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals.~~
- [ ] ~~(presun do 2023.40) +Overiť ako fungujú SEO rozšírenia pre WordPress (napr. https://yoast.com/beginners-guide-yoast-seo/) a navrhnúť podobné riešenie pre WebJET CMS.~~
- [ ] ~~(presun do 2023.40) +Pridať blokom v PageBuilder-i nejaký data atribút s menom bloku a ten potom zobrazovať vo vlastnostiach bloku, aby sa dalo spätne identifikovať aký to je blok.~~
- [x] Zvážiť možnosť použitia [HotSwapAgent](http://hotswapagent.org/) pre rýchlejší vývoj bez potreby reštartov (vyriešené prestavením ```build.gradle``` pre podporu štandardného HotSwap).
- [ ] ~~(presun do 2023.40)+Číselníky - v exporte sa nenachádza atribút, že je záznam zmazaný a potom po importe sa záznam aktivuje.~~
- [ ] ~~(presun do 2023.40)Na úvodnej obrazovke implementovať mini aplikáciu todo.~~
- [ ] ~~(presun do 2023.40)+DT - po pridaní záznamu pre stránkovať na stranu, kde sa záznam nachádza (typicky posledná strana)~~
- [x] +Doplniť preklady textov z pug súborov (napr. gallery.pug), hľadať napr. písmeno á alebo podobné, ktoré by sa v nepreložených textoch mohlo nachádzať.
- [x] +Pridať možnosť ponechať okno editora otvorené po uložení (viď. https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open, closeOnComplete, https://editor.datatables.net/reference/api/submit()). Realizované pomocou klávesovej skratky ```CTRL+s/CMD+s``` (#54273-26).
- [x] ```Combine``` / mazanie dát - pri zmazaní všetkých dát v cache preniesť korektne aj na ```Cluster``` a tam tiež zmazať rovnako, aktuálne ```ClusterRefresher``` zmaže len cache, nie ```DocDB/GroupsDB``` a nenastaví nový čas pre ```combime``` (#54673).
- [x] +REST - pridať možnosť definovania API kľúčov (tokenov) používaných namiesto generovaných CSRF tokenov pre volanie REST externými službami (#54941).
- [ ] ~~(presun do 2023.18) +Hlavičky - pridať novú aplikáciu, ktorá umožňuje definovať ľubovoľnú HTTP hlavičku podľa URL adresy.~~
- [x] +Skripty - pridať ```autocomplete``` na pole Umiestnenie skriptu v šablóne (#54941).
- [x] +Kontrola odkazov - upraviť pôvodnú verziu Kontrola odkazov na datatabuľku a prepojiť na zoznam web stránok (#54697).
- [x] +Rezervácie a rezervačné objekty - prerobiť administráciu na datatabuľku a Spring (#54701).
- [x] +Anketa - prerobiť administráciu na datatabuľku a Spring (#54705).
- [x] +Atribúty stránky - prerobiť administráciu na datatabuľku a Spring (#54709).
- [x] +Šablóny - pridať kartu so zoznamom web stránok používajúcich šablónu (#54693).
- [x] +Štatistika - prerobiť do nového dizajnu a grafov (#54497, #54585, #54897).
- [x] +Vyhľadávanie - ```Lucene``` - pridať podporu vyhľadávania v českom a anglickom jazyku (#54273-34).
- [x] +Vyriešiť problém s verziami FontAwesome v administrácii a web stránkach (#39751-51).
- [x] +Bannerový systém - doplniť možnosť zobrazenia kampaňového bannera podľa URL parametra (#MR296).
- [x] +Galéria - rozšíriť možnosti editora, pridať horný/dolný index, možnosť vložiť HTML kód, nastavenie farieb. Zlepšiť podporu voči starému editoru/html kódu (#54857-4).
- [x] +Galéria - prispôsobiť počet fotiek veľkosti okna, pamätať si nastavenú veľkosť obrázkov v tabuľke (#54857-4, #market-245).
- [x] +Web stránky - zobrazenie perex skupín ako multi výberové pole s vyhľadávaním namiesto štandardných ```checkbox``` polí (ak existuje veľa perex skupín) (#market-245).
- [x] +Formulár ľahko - pridať typ poľa ```select``` - štandardné výberové pole, pridať možnosť zadať inú hodnotu a zobrazený text (#MR298, #39751-45).
- [x] +Web stránky - pridať zobrazenie priečinkov ako dátovej tabuľky s možnosťou hromadných operácií (#54797).
- [x] +Bezpečnosť - pridať možnosť nastaviť HTTP hlavičku ```Access-Control-Allow-Origin``` (#39751-44).
- [x] +Bannerový systém - doplniť možnosť nastaviť klienta, kontrolovať práva pri zobrazení (#38751-52).
- [x] +Bannerový systém - doplniť možnosť nastaviť voliteľné polia, refaktorovať ```BannerEntity``` a ```BannerBean``` do jedného objektu, aby sa dali použiť nové polia aj v JSP komponente (#39751-52).

## 2022.40

> Verzia 2022.40 je zameraná na zlepšenie bezpečnosti.

- [x] Používatelia - pridať kontrolu hesla z histórie pri ukladaní používateľa, lebo teraz to dovolí, ale prihlásiť sa s ním nedá (#54293).
- [x] Skupiny používateľov - pridať kartu so zoznamom používateľov v skupine práv (#54493).
- [x] Aktualizovať NPM moduly - https://www.npmjs.com/package/npm-check-updates alebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version (#54721).
- [x] Zmeniť ```hash``` funkciu hesiel na ```bcrypt``` (respektíve niečo modernejšie - https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) (#54573).
- [x] Doplniť/overiť kontrolu práv pre editáciu konfiguračných premenných (funkcia editácie všetkých premenných) (#54749).
- [x] Validácie - doplniť lepšie validácie na polia (napr. url adresa, doména) s opisnými chybovými hláseniami a upraviť ```DatatableExceptionHandlerV2``` aby vedel použiť prekladové kľúče priamo z WebJET CMS aj s dodatočnými údajmi (ako min/max hodnota a iné makrá v prekladovom kľúči) (#54597).
- [x] Logovanie - prechod z ```log4j``` na štandard ```slf4j``` - vyriešené prechodom na ```logback``` ().
- [x] Logovanie - pridať systém pre dočasný zápis logov do RAM pamäte pre možnosť pozretia posledných logov cez administráciu, ak nie je prístup k logom na súborovom systéme. Možnosť získať aj logy z nodov clustra napr. cez dočasný zápis do dát ```conf``` tabuľky (#54269).
- [x] Na úvodnú stránku doplniť kontroly aktualizácie a databázy ako vo WebJET 8 + ~~otvorenie pomocníka pri novej verzii~~ (v úvode je odkaz na zoznam zmien) (#54457).
- [x] Upraviť vizuál nevyhovujúceho hesla a zmeny hesla na WebJET 9 (minimálne vymeniť obrázok pozadia) (#54573).
- [x] Bezpečnosť - pridať kontrolu prístupu k administrácii - aktuálne sa kontroluje len volanie REST služby, nie volanie ```Thymeleaf``` stránky (#54649)
- [x] Datatabuľka - pridať do dialógového okna zmazania meno položky, potrebné je myslieť na situáciu keď je označených viac riadkov (#54753).
- [ ] ~~(presun do 2023.18) +Migrácia na vyššiu verziu Javy s ```LTS``` podporou (Java 11 alebo ideálne až Java 17) (#54425).~~
- [ ] ~~(presun do 2023.18) +Formuláre - doplniť funkciu archivovať formulár.~~
- [x] +Web stránky - doplniť funkciu náhľadu stránky pred uložením (#54557).
- [x] +Web stránky - pridať možnosť ```drag&drop``` stránok do adresárovej štruktúry (#54513).
- [x] +Pripraviť ukážkovú aplikáciu pre programátora do administrácie kde bude možné vybrať nejaké možnosti vo formulári a nahrať súbor, ktorý sa serverovo spracuje. Následne sa zobrazí výsledok (#54449).
- [x] +Pripraviť ukážkovú šablónu s použitím Thymeleaf a prípadne PUG súborov s integráciou na PageBuilder (#54345).
- [x] +Prerobiť aplikáciu Hromadný email-Odoslané emaily do nového dizajnu (#54377).
- [x] +Doplniť CZ a EN preklady (#53805).
- [x] +Doplniť k editácii skupiny používateľov kartu so zoznamom používateľov v danej skupine, upraviť zoznam web stránok na serverové stránkovanie (#54993).
- [ ] ~~(presun do 2023.18) +Banner - pridať možnosť nastaviť adresárovú štruktúru stránok v ktorej sa bude banner zobrazovať a možnosť definovať výnimky kde sa nebude.~~
- [x] +Doplniť pamätanie spôsobu usporiadania pre každú datatabuľku (#54513-22).~~
- [x] +Doplniť X na zatvorenie okna aj k ikone maximalizácie.
- [ ] ~~(presun do 2022.52) +Šablóny - pridať kartu so zoznamom web stránok používajúcich šablónu (#54693).~~
- [x] +Web stránky - priečinky je možné označiť viaceré s CTRL, ale nejdú sa takto všetky zmazať (zmaže len prvý) (#54797).
- [x] +Datatabuľky - dynamicky určiť počet stĺpcov tabuľky, aby nezostávalo prázdne miesto, čo evokuje, že tam už nie sú ďalšie záznamy (používateľ si nevšimne, že tam je stránkovanie) (#54273-26).
- [x] +Datatabuľky - pridať možnosť použitia multiselect v datatabuľke a v aplikácii v editore (#54273-25).
- [x] +Kalendár udalostí - prerobiť aplikáciu do nového dizajnu (#54473).
- [x] +Web stránky - pridať možnosť zobraziť ID priečinku a poradie usporiadania v stromovej štruktúre stránok (#54513-1).
- [x] +Web stránky - pridať možnosť uložiť stránku ako A/B test (#54513-2).
- [x] +Formuláre - pridať možnosť exportu od posledného exportu, vybrané záznamy, podľa aktuálneho filtra (#54513-3)
- [x] +Generovať ```POM``` súbor so závislosťami/knižnicami priamo z gradle verzie, namiesto úprav v pôvodnej WJ8 verzii (#54673).
- [ ] ~~(presun do 2022.52) +Skripty - pridať ```autocomplete``` na pole Umiestnenie skriptu v šablóne.~~
- [ ] ~~(presun do 2022.52) +Kontrola odkazov - upraviť pôvodnú verziu Kontrola odkazov na datatabuľku a prepojiť na zoznam web stránok (#54697).~~
- [ ] ~~(presun do 2022.52) +Rezervácie a rezervačné objekty - prerobiť administráciu na datatabuľku a Spring (#54701).~~
- [ ] ~~(presun do 2022.52) +Anketa - prerobiť administráciu na datatabuľku a Spring (#54705).~~
- [ ] ~~(presun do 2022.52) +Atribúty stránky - prerobiť administráciu na datatabuľku a Spring (#54709).~~
- [x] +Možnosť posúvať okno editora (#54513-21).
- [x] +Zapamätať si poradie stĺpcov v tabuľke pre každú tabuľku (#54513-22)
- [x] +Zapamätať si naposledy otvorený priečinok v zozname web stránok (#39751-45)

## 2022.18

> Verzia 2022.18 sa sústreďuje na zlepšenia použiteľnosti.

- [x] editor - zobrazenie stránky v inline editácii (ak je nastavený inline editor na Page Builder) (#54349).
- [x] DT pri klientskom stránkovaní označenie všetkých riadkov sa tvári ako označenie na jednej strane ale zmaže všetko aj na ostatných stranách (#54281).
- [x] Pridať tlačidlo do notifikácie že existuje rozpracovaná verzia na otvorenie (#54357).
- [x] Pridať stĺpec s ikonami na zobrazenie web stránky (#54257).
- [x] Prerobiť DocID/web stránka na ID vo web stránkach (#53805).
- [x] Úvod - pridať aplikáciu do pravého stĺpca so zoznamom Obľúbených URL adries (#54177)
- [x] Úvod - pridať jednoduchý formulár na poslanie pripomienky k WJ (#54181)
- [x] Úvod - sfunkčniť kliknutie na ikony v prvom bloku (návštevy, formuláre...) (#53805)
- [x] Pridať možnosť nastavenie dvoj faktorovej autorizácie administrátorom (#54429)
- [x] Pre WJ Spring komponenty mapovať ```editor_component```. Momentálne to hľadá jsp v ```INCLUDE``` a pre tieto komponenty je tam napr. ```sk.iway.xxx.component.SiteBrowserComponent``` a tak to nenájde a vráti ```appstore``` (#54333).
- [x] Web stránky - doplniť možnosť zmeniť doménu po zadaní ```docid``` ak sa stránka nachádza v inej doméne a automaticky prepnúť záložku na Systém alebo Kôš ak sa nachádza v tejto vetve (#54397).
- [x] Prekladové kľúče - pridať tlačidlo na obnovenia kľúčov z ```properties``` súboru, obnoviť kľúče po zmazaní záznamu a nezobraziť chybu mazania ak kľúč neexistuje (lebo je z properties súboru - id väčšie ako 1000000) (#54401).
- [x] DT Editor - pridať notifikáciu pri odchode zo stránky (#54413).
- [x] Voliteľné polia - doplniť možnosť vybrať adresár (#54433).
- [ ] ~~+(presun do 2022.52) DT - po pridaní záznamu prestránkovať na stranu, kde sa záznam nachádza (typicky posledná strana)~~
- [x] +Doladiť verziu pre Firefox, pridať automatizované testovanie aj pre Firefox (#54437).
- [x] +Po prepnutí do WJ8 verzie chýbajú linky na pridanie presmerovania (a možno aj iné) (#53805).
- [x] +Do WJ8 rozhrania doplniť ikonu na prepnutie na V9 verziu (#53805).
- [ ] ~~+(presun do 2022.52) Doplniť preklady textov z pug súborov (napr. gallery.pug), hľadať napr. písmeno á alebo podobné, ktoré by sa v nepreložených textoch mohlo nachádzať.~~
- [x] +Pri importe konfigurácie s aktualizáciou podľa name to padne na chybe, vhodné by bolo vymeniť to za pôvodný import aj s náhľadom zmien.
- [ ] ~~+ (presun do 2022.36) Pripraviť ukážkovú aplikáciu pre programátora do administrácie kde bude možné vybrať nejaké možnosti vo formulári a nahrať súbor, ktorý sa serverovo spracuje. Následne sa zobrazí výsledok (#54449).~~
- [x] +Aktualizovať Java knižnice na aktuálnu verziu bez zraniteľností.
- [x] +Pridať možnosť použiť ```Thymeleaf``` šablóny pre ```@WebjetComponent``` (#54273).
- [x] +Zriadiť inštanciu s verejným git repozitárom (#54285).
- [ ] ~~+(presun do 2022.52) Pridať možnosť ponechať okno editora otvorené po uložení (viď. https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open).~~
- [x] +V menu časti šablóny zobraziť položky editácie šablón priamo na prvej úrovni (#54345)
- [x] +Doplniť možnosť zadať ```placeholder``` v aplikácii Formulár ľahko (#54381).
- [x] +Overiť spustenie WebJET CMS nad Oracle a Microsoft SQL (#54457).

## 2021.52

> Verzia 2021.52 je zameraná na zlepšenie práce s web stránkami a editorom.

### Web stránky

- [x] editor - doplnené info ikony k jednotlivým poliam s vysvetlením ich významu (#54253)
- [x] editor - dokončenie TODO položiek v editácii web stránky v záložke Prístup - nastavenie zobrazenia navigačnej lišty a mapy stránok (rozšírenie dátového modelu) (#54205)
- [x] editor - upravené pole URL adresa podľa pôvodného UI návrhu (zobrazená len koncová časť s možnosťou prepnutia zobrazenie celej URL) (#54237)
- [x] editor - zobrazenie informácie o schvaľovaní stránky (po uložení, keď má byť schválená) (#54129)
- [x] editor - zobrazenie rozpracovanej verzie stránky (#54129)
- [x] ~~editor - pridať možnosť Uložiť a Uložiť ako~~ - nahradené vlastnosťou Duplikovať datatabuľky
- [x] web stránky - zobrazenie web stránok aj z pod adresárov (#52435)
- [x] Web stránky - adresáre - zobrazovať pole pre zadanie domény len pre koreňové priečinky, automaticky nastaviť doménu pri uložení koreňového priečinka (#54249)
- [x] Web stránky - pridať informáciu o editácii aktuálnej web stránky iným redaktorom (#54193)
- [x] Web stránky - overiť/doplniť kontrolu práv na pridávanie/editáciu/mazanie, skrátené menu a skryté adresáre v administrácii (#54257).
- [ ] ~~stromová štruktúra - podpora klávesových skratiek pre vytvorenie/editáciu novej položky v stromovej štruktúre~~ - nebude sa realizovať, po UX konzultácii sa budú všade používať len tlačidlá
- [ ] ~~stromová štruktúra - kontextové menu - pridané kontextové menu v stromovej štruktúre (po zvážení vhodnosti z pohľadu UI/UX kontextové menu vs ikony v nástrojovej lište)~~ - nebude sa realizovať, po UX konzultácii sa budú všade používať len tlačidlá
- [x] +úprava bunky - pred úpravou načítať dáta zo servera, aby sa získali ```editorFields``` dáta. V zozname používateľov sa inak nedá upravovať napr. meno (lebo chýba heslo). Zobrazujú sa chybne aj niektoré nadpisy (Vstup do admin sekcie) (#54109).
- [x] +generické riešenie problému s ```domain_id``` (#53913)
- [x] +doplnenie poľa ID do každej datatabuľky ako sa používa pre správu web stránok (#53913)
- [x] +upraviť aplikáciu Otázky a odpovede do dizajnu WebJET 2021 (#53913)
- [x] +zlepšenie použiteľnosti na mobilných zariadeniach (#53573, #54133)
- [x] +pridať možnosť web stránku uložiť ako rozpracovanú verziu pomocou zaškrtávacieho poľa (#54161)
- [x] +Zmeniť písmo na ```Open Sans``` (#53689).
- [x] +Integrovať automatický preklad textov cez https://www.deepl.com/translator (napr. pre zrkadlenie štruktúry, prekladové texty atď) (MR146).
- [x] +Web stránky - ak je stránka vo viacerých adresároch nejde editovať stránku v sekundárnom adresári (vrátia sa údaje pôvodnej stránky a nespárujú sa dáta podľa docid/ID riadku).
- [x] +Upraviť dizajn verzie 8 aby sa podobal na verziu 2021 (minimálne správanie menu) (#54233).
- [x] +Doplniť deployment vo formáte pre aktualizáciu cez Nastavenia->Aktualizácia WebJETu (#54225).
- [x] +Doplniť mazanie starých konf. premenných ```statDistinctUsers-``` a ```statSessions-``` v režime ```auto``` clustra, doplniť zobrazenie nodov do Monitorovanie->Aktuálne hodnoty (#54453).

### Aplikácie v novom dizajne

- [x] +Otázky a odpovede - upraviť na nový dizajn (#53913).
- [x] +Bannerový systém - upraviť na nový dizajn (#53917).
- [x] +Hromadný e-mail - Doménové limity - upraviť na nový dizajn (#54153).
- [x] +Export dát - upraviť na nový dizajn (#54149).
- [x] +Tooltip - upraviť na nový dizajn (#53909).

### Datatabuľky

- [x] +Pridať možnosť zadať ID záznamu v ľubovoľnej tabuľke, podobne ako je to pre web stránky.

### Export-import

- [x] +Export-import - opraviť poradie stĺpcov voči hlavičke (viditeľné v zozname používateľov, ale môže byť aj inde) (#54097).
- [x] +Export-import - vyriešiť stĺpce s rovnakým menom ale inými dátami (napr. v zozname používateľov sú 2 adresy, čo nie je reflektované v excel názvoch stĺpcov) (#54097).

### Bezpečnosť

- [x] +Aktualizovať NPM moduly - https://www.npmjs.com/package/npm-check-updates alebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version.
- [x] +Datatabuľky - do REST Controllera pridať automatické nastavovanie podmienky stĺpca ```domain_id``` (#53913).
- [x] +Upraviť spôsob kompilácie Java tried tak, aby sa kompletne pred deploymentom všetko zostavilo nanovo a naraz. Pri upravených triedach teda nemôže nastať problém v kompatibilite.
- [x] +Doplniť kontrolu bezpečnosti v administrácii v poliach, v ktorých je možné vkladať HTML kód (využiť ```org.owasp.html.PolicyFactory```) (#53931).

### Iné úpravy

- [x] +Verzia pre mobilné zariadenia - optimalizovať rozhranie, datatabuľky a editor pre použitie v mobilných zariadeniach.

## 2021.40

> Cieľom verzie 2021.40 je zmigrovať editáciu používateľov a modul GDPR do Datatables Editora. Naďalej budeme pracovať aj na zlepšení použiteľnosti práce s web stránkami - možnosť Drag & Drop a kontextové menu v stromovej štruktúre.

### Nové vlastnosti

- [x] používatelia - prerobené do Datatables Editor (#46873)
- [x] aplikácia GDPR - prerobené do Datatables Editor (#53881, #53905)

### Web stránky

- [x] editor - voľné polia - možnosť nastavenia typov polí ```selectbox```, ```autocomplete```, výber URL obrázka/stránky (#53757)
- [x] stromová štruktúra - podpora drag & drop web stránok a adresárov (#52396)
- [x] +automaticky vytvárať ```System``` priečinok pre doménu a zobrazovať z neho stránky vrátane pod priečinkov (#53685)
- [x] +automatické zmazanie titulku Nová web stránka pri kliknutí do poľa a prenos do navigačnej lišty (#53693)
- [x] +upraviť načítanie zoznamu priečinkov pre skupiny šablón - zobraziť všetky priečinky nielen podľa mena inštalácie pre jednoduchšie použitie šablón (#53693)
- [x] +pridať editáciu perex skupín (#53701)
- [x] +doplniť kontrolu práv pre editáciu prekladových kľúčov (funkcia editácia všetkých kľúčov) (#53757)
- [x] +pri premenovaní domény zmeniť aj prefix domény konfiguračných premenných a prekladových kľúčov (#market-107)
- [x] +ak stromová štruktúra obsahuje max 2 adresáre zobraziť ich rozbalené (#53805)
- [x] +pri nastavovaní rodičovského adresára automaticky rozbaliť koreňový adresár (#53805)
- [x] +pri vytvorení doménového ```System``` adresára automaticky vytvoriť aj pod adresáre pre hlavičky, pätičky a menu (#53805)
- [x] +web stránky - doplniť ikonu pre zobrazenie stránky (#53753)

### Datatables

- [x] +Úprava bunky pre editor obsahujúci viaceré listy (#53585).

### Všeobecné

- [x] aplikácia galéria - upraviť editáciu položky stromovej štruktúry z VUE komponenty na datatables editor (ako je pri web stránkach) (#53561)
- [x] prepísať definíciu datatables z PUG súborov do Java anotácií (#48430)
- [x] +navrhnúť a zdokumentovať spôsob vytvárania zákazníckych aplikácií v novom dizajne WebJET 2021 (#54045)

## 2021.13

> Cieľom verzie 2021.13 je zlepšiť použiteľnosť práce s web stránkami a možnosť použitia v projektoch zákazníkov. Zároveň bude ukončený vývoj nových vlastností WebJET 8 a spísanie sekcie čo je nové vo verzii 8.8.

### Web stránky

- [x] editor - zlepšenie viditeľnosti poľa Poznámka redaktora (#53131)
- [x] editor - zobrazenie a správa médií (záložka Média) (#52351, #52462)
- [x] editor - zobrazenie histórie stránky (#53385)
- [x] web stránky - sfunkčnenie tabu Naposledy upravené a Čakajúce na schválenie (#53493)
- [x] web stránky - import/export - doplnenie pôvodného importu a exportu stránok v XML zip do nástrojovej lišty web stránok (#53497)
- [x] stromová štruktúra - niekedy kliknutie na ikonu editovať nie je vykonané (po predchádzajúcom zatvorení dialógu cez zrušiť) (#51487)
- [x] stromová štruktúra - dokončenie TODO položiek v editácii adresára v záložke Šablóna a Prístup (#51487)
- [x] stromová štruktúra - úprava zobrazených ikon v stromovej štruktúre na základe UI/UX pripomienok (#52396)
- [x] stromová štruktúra - zobrazenie plánovaných verzií a histórie adresára (#53489)
- [x] stromová štruktúra - pridaná ikona pre obnovenie stromovej štruktúry (#52396)
- [x] stromová štruktúra - pridané zobrazenie ```docid/groupid``` a poradie zobrazovania (podľa nastavenia) (#53429, #53565)
- [x] stromová štruktúra - sfunkčnenie kariet Systém a Kôš (#53137)
- [x] +optimalizovať rýchlosť zobrazenia zoznamu web stránok (#53513)

### Datatables editor

- [x] po zatvorení dialógu a otvorení iného zostanú zobrazené chybové hlásenia (#52639)
- [x] po zatvorení dialógu a otvorení zostane obsah posunutý nadol/v pôvodnom mieste (#53119)
- [x] pridaný field type datatable pre možnosť vloženia vnorenej datatabuľky do editora (napr. pre média, históriu stránok a podobne) (#52351)
- [x] doplnenie zobrazenia error hlásenia pri chybe tokenu/odhlásenia používateľa (#53119)
- [x] +doplnenie možnosti autocomplete pre textové polia (#52999)
- [x] +zobrazenie poľa poznámka pre redaktora cez notifikácie (#53131)
- [x] +inicializácia vnorených datatabuliek až po prechode na daný list (#53425)
- [x] +integrácia výberu súboru cez elfinder (#52609)

### Iné

- [x] +Konfigurácia - zobrazenie plánovaných verzií a histórie zmien (#53089)
- [x] +doplniť možnosť prekladať texty v JavaScript súborov (#53128)
- [x] +Načítať údaje vnorenej datatabuľky až po kliknutí na list kde sa nachádza (#53425)
- [x] +kontrolu spojenia so serverom a zobrazenie chybového hlásenia pri chybe spojenia, alebo chybe bezpečnostného (CSRF) tokenu (#53119)
- [x] +aktualizovať testovací framework codeceptjs na aktuálnu verziu (#52444)
- [x] +rozdeliť dokumentáciu na programátorskú, používateľskú, inštalačnú atď. (#52384)
- [x] +zapamätanie nastavenia zobrazených stĺpcov v datatabuľke pre každého používateľa zvlášť (#53545)
- [x] +nový vizuál prihlasovacej obrazovky (#53617)

### Všeobecné

- [x] zlepšenie integrácie do zákazníckych projektov na základe prvých testov (#52996, #53589)
- [x] udržiavanie session so serverom, zobrazenie progress baru odhlásenia ako vo verzii 8 (#53119)
- [ ] spísanie sekcie Čo je nové pre WebJET 8.8
- [x] +úprava dokumentácie na formát ```docsify``` a presun na server http://docs.webjetcms.sk/ (#52384)
- [x] +aktualizácia testovacieho frameworku codeceptjs na verziu 3.0.4 (#52444)

<!-- deepmark-ignore-start -->
<script type="text/javascript">
setTimeout(function() {
    for (var node of document.querySelectorAll("input[type=checkbox]")) {
        node.removeAttribute("disabled");
        // Prevent click events
        node.addEventListener('click', function(event) {
            event.preventDefault();
        });
    }
}, 100);
</script>
<!-- deepmark-ignore-end -->