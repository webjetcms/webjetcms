# Zoznam zmien verzia 2024

## 2024.0-SNAPSHOT

> Upozornenie: na spustenie verzie 2024 je potrebné mať na serveri inštalovanú Java verzie 17.

### Prechod na Java 17

WebJET CMS verzie 2024 prešiel na Java verzie 17. Obsahuje nasledovné zmeny:

- Aktualizované viaceré knižnice, napr. `AspectJ 1.9.19, lombok 1.18.28`.
- Aktualizovaná knižnica Eclipselink na štandardnú verziu, použitie WebJET CMS `PkeyGenerator` nastavené pomocou triedy `JpaSessionCustomizer` a `WJGenSequence`.
- Aktualizovaný `gradle` na verziu 8.1.
- Odstránená stará knižnica ```ch.lambdaj```, použite štandardné Java Lambda výrazy (#54425).

### Bezpečnosť

- 404 - pridaná možnosť vypnúť ochranu volania 404 stránky (počet požiadaviek) podobne ako iné spam ochrany nastavením IP adresy do konf. premennej `spamProtectionDisabledIPs`. Pre danú IP adresu sa vypnú aj ďalšie SPAM ochrany (pre  opakované volania).

### Systémové zmeny

- WebJET CMS je dostupný priamo v [repozitári maven central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/), GitHub projekty [basecms](https://github.com/webjetcms/basecms) a [democms](https://github.com/webjetcms/democms) upravené na použitie priamo tohto repozitára. Zostavenie je mierne odlišné od pôvodného zostavenia, knižnice `wj*.jar` sú spojené do `webjet-VERZIA-libs.jar`. Použitá knižnica [pd4ml](https://pd4ml.com/support-topics/maven/) je vo verzii 4, pre generovanie PDF súborov vyžaduje zadanie licencie do súboru `pd4ml.lic` v [pracovnom priečinku](https://pd4ml.com/support-topics/pd4ml-v4-programmers-manual/) servera alebo priečinku kde sa nachádza `pd4ml.jar`. Neskôr bude doplnená možnosť zadať licenčné číslo cez konfiguračnú premennú (#43144).
- Zrušená podpora plno textového indexovania `rar` archívov (#43144).
- NTLM - pridaná konf. premenná `ntlmLogonAction.charsetEncoding` s názvom kódovania znakov pre LDAP prihlásenie. Ak je prázdne, nepoužije sa a znaky sa ponechajú v kódovaní ako ich vráti LDAP server.

### Oprava chýb

2024.0.52

- Web stránky - opravené nastavenie značiek pri nastavenej konfiguračnej premennej `perexGroupUseJoin=true` (#57453).
- Štatistika - chybné stránky - zväčšený limit maximálneho počtu záznamov z 1000 na hodnotu podľa konfiguračnej premennej `datatablesExportMaxRows`, predvolene `50000`. Tabuľka upravená na stránkovanie a vyhľadávanie na serveri (#57453).
- Stripes - opravené formátovanie dátumu a času za použitia `Tools.formatDate/Time` pre konzistentné formáty dátumov a času (#57405).
- Bezpečnosť - opravená možnosť presmerovať na externú doménu pri odhlásení používateľa (#57521).
- Bezpečnosť - pridaná možnosť zakázať `basic` a `api-token` autorizáciu [pre REST služby](sysadmin/pentests/README.md#konfigurácia) nastavením konfiguračnej premennej `springSecurityAllowedAuths` (#57521).
- Bezpečnosť - pridaná možnosť chrániť odhlásenie používateľa [CSRF tokenom](custom-apps/spring/rest-url.md) nastavením konfiguračnej premennej `logoffRequireCsrfToken` na hodnotu `true` (#57521).
- Bezpečnosť - pridaná možnosť [vyžadovať CSRF token](custom-apps/spring/rest-url.md#csrf-token) pre zadané URL adresy nastavením konfiguračnej premennej `csrfRequiredUrls` (#57521).
- Administrácia - upravené zvýrazňovanie menu položiek pre podporu `#hash-tag` v URL adrese pre aplikácie v `Angular/Vue` v administrácii (#57557)
- Vyhľadávanie - opravené vyhľadávanie stránok z iných ako aktuálnej domény (#57573).
- Používatelia - opravená možnosť výberu doménového priečinka - priečinok s názvom domény nie je skutočný priečinok v databáze (#54273-54).
- Používatelia - doplnený dátum registrácie a dátum posledného prihlásenia do  exportu, polia sa zobrazia (needitovateľne) aj v editácii používateľa v karte Základné (#56393-19).
- Používatelia - opravené zobrazenie zoznamu používateľov ak niektorý používateľ obsahuje schvaľovanie zmazaného priečinka (#56393-21).
- Konfigurácia - opravený import z XML ak názov premennej obsahuje špeciálne znaky `[].` (#54273-54).
- Konfigurácia - upravený import z Excelu - pridaná možnosť aktualizovať záznam podľa mena, importovať iba nové záznamy, zrušené nepotrebné stĺpce z exportu (#54273-54).
- Web stránky - pri editácii odkazu, ktorý obsahuje URL parametre typu `/odhlasenie-z-odberu-noviniek.html?email=!RECIPIENT_EMAIL!` sú pri úprave odkazu, alebo jeho zmene zachované URL parametre. Môžete tak ľahko zmeniť odkaz na inú web stránku so zachovaním potrebných parametrov (#57529).
- Bezpečnosť - aktualizovaná knižnica `logback`.
- Úvod - opravené čítanie ilustračného obrázka z externej domény, upravené načítanie zoznamu noviniek WebJET na oneskorené pre rýchlejšie zobrazenie celej stránky.
- `Multi Domain` - upravené získanie doménového mena, ak v HTTP hlavičke obsahuje aj port (niekedy ho tam vkladá proxy server).
- Web stránky - opravené vloženie odkazu na stránku, ktorá v URL adrese/parametri obsahuje znak `:` tak, aby aj protokol zostal zobrazený (#56393-19).

2024.0.47

- Datatabuľky - opravené zobrazené meno stĺpca pri nastavení zobrazenia stĺpcov ak je upravené ich poradie (#56393-14).
- Export do HTML - opravená kontrola práv, opravené zobrazenie generovaných súborov v priečinku `/html` (#57141).
- Persistent cache objekty - opravené uloženie záznamu - nastavenie správneho typu (#56393-15).
- Úlohy na pozadí - opravený reštart úloh na pozadí po zmazaní úlohy (#56393-14).
- Web stránky - opravené uloženie web stránok, ktorých názov je jedno písmenový `N,B,S,P` (#56393-15).
- Web stránky - Page Builder - zlepšená klávesová skratka `CTRL/CMD+S` pre uloženie stránky bez zatvorenia editora, je aktívna aj mimo zelených častí s editorom.
- Zálohovanie systému - opravená kontrola práv (#57141).
- Značky - upravené zobrazenie priečinkov a ich výber tak, aby bolo možné voliť značku zo všetkých domén (#56393-15).
- `DatatableRestControllerV2` presunuté volanie `afterDelete` mimo metódy `deleteItem` aby pri preťažení tejto metódy bolo `afterDelete` korektne zavolané.
- Formuláre - opravené nastavenie jazyka pri presmerovaní formuláru na stránku, ktorá obsahuje `Spring` aplikáciu (#56393-15).
- Web stránky - Editor - opravené nastavenie jazyka v náhľade vloženej `Spring` aplikácie (#56393-15).
- Audit - Notifikácie - opravené uloženie novej notifikácie pri použití MicroSoft SQL databázy, doplnené zmazanie cache zoznamu notifikácií pri úprave záznamu (#57225).
- Galéria - opravené zobrazenie možnosti pridania priečinku ak má používateľ obmedzené práva na priečinky (#56393-17).
- Galéria - pridaná možnosť nastavenia vodoznaku rekurzívne aj na pod adresáre a pre generovanie obrázkov po zmene vodoznaku (#MR181).
- Galéria - vytvorená dokumentácia pre [nastavenie vodoznaku](redactor/apps/gallery/watermark.md) v galérii (#MR181).
- Galéria - opravená kontrola práv na presun priečinka pomocou Drag&Drop (#MR11).
- Galéria - opravená chyba zobrazenia obrázkov pri presune priečinka pomocou Drag&Drop (#MR11).
- Monitorovanie - doplnené monitorovanie `Spring` aplikácií (#67357).
- Automatizované úlohy - opravené stránkovanie a zobrazenie viac ako 25 úloh (#56393-18).
- Aplikácie - pre Spring aplikácie používajúce výber priečinka `dt-tree-dir-simple` pridaná možnosť priamo zadať hodnotu z klávesnice (#56393-18).
- Web stránky - opravené vloženie odkazu na stránku, ktorá v URL adrese/parametri obsahuje znak `:` (#56393-18).
- Web stránky - opravené vkladanie `FontAwesome` ikon. Ak vaša šablóna používa `FontAwesome` nastavte konfiguračnú premennú `editorEnableFontAwesome` na `true` pre zobrazenie možnosti vkladať ikony v editore (#56393-18).
- Formuláre - opravený regulárny výraz pre kontrolu email adresy typu `email@domena,com` (#56393-18).
- Video - upravené nastavenie `referrerpolicy` pre YouTube videá, ktoré spôsobovalo, že niektoré videá sa nedajú prehrávať (#56393-18).
- Aktualizované Java knižnice, doplnené výnimky pre `DependencyCheck` (#56393-18).

2024.0.34

- Audit - opravené zobrazenie opisu auditu v prehliadači Firefox.
- Bezpečnosť - pri chybe nahrávania súboru nebude zobrazená chyba zo servera ale generická chybová správa (#56277-13).
- Číselníky - optimalizované načítanie údajov, upravené nastavenie rodiča číselníka na `autocomplete` pre optimálnejšie čítanie dát (#57017).
- Datatabuľky - opravené vyhľadávanie podľa ID záznamu - hľadá sa typ rovná sa, nie obsahuje pri tabuľkách bez serverového stránkovania (#56993).
- Galéria - opravené vyhľadávanie - hľadá sa len v aktuálne zobrazenom priečinku nie všetkých priečinkoch (#56945).
- GDPR/Cookies - opravené nastavenie cookies v jedno doménovom WebJETe (duplikovanie nastavených cookies).
- Datatabuľky - vypnutá možnosť filtrovania podľa ID v tabuľkách, kde ID nie je primárny kľúč, napr. Konfigurácia, Mazanie dát, Prekladové kľúče (#56277-12).
- Formuláre - opravené zobrazenie stĺpca Dátum potvrdenia súhlasu pri formulároch s nastaveným [potvrdením email adresy](redactor/apps/form/README.md#nastavenie-potvrdenia-emailovej-adresy) (#56393-7).
- Formuláre - opravené zobrazenie textu "prázdne" v tabuľke (#56277-10).
- Formuláre - upravený export čísel - čísla s desatinným miestom oddeleným znakom čiarka sú skonvertované na oddeľovač bodka a na číselnú reprezentáciu pre správny formát v Exceli. Nepoužije sa na čísla začínajúce znakom + alebo 0 (#56277-10).
- Formuláre - opravený duplicitný export pri prechode medzi viacerými formulármi bez obnovenia stránky (#56277-10).
- Formuláre - pri vypnutej spam ochrane `spamProtection=false` sa už nebude kontrolovať CSRF token pri odoslaní formuláru (#56277-13).
- Galéria - opravené zmazanie priečinka galérie vytvorenej cez web stránku pri vkladaní obrázku (#56393-8).
- Galéria - opravené nastavenie parametrov priečinka galérie ak rodičovský priečinok nemá uložené nastavenia (je biely). Hľadá sa uložené nastavenie priečinka smerom ku koreňu (#56393-10).
- Galéria/Editor obrázkov - doplnená chýbajúca funkcia na zmenu veľkosti obrázka.
- Hromadný email - opravená chyba vloženia príjemcu zo skupiny používateľov, ktorý nemá povolené prihlásenie (je deaktivovaný, alebo nemá platné dátumy prihlásenia od-do) (#56701).
- Klonovanie štruktúry - opravené nastavenie prepojenia priečinkov pri klonovaní (mohlo dochádzať k neúplnému klonovaniu priečinkov) (#56277-7).
- Mapa stránok - opravené generovanie súboru `/sitemap.xml` podľa nastavených atribútov zobrazenia web stránky v Mape stránok (karta Navigácia web stránky) (#56993).
- Prekladové kľúče - upravené zobrazenie aby sa zobrazil v tabuľke prípadný HTML kód hodnoty kľúča (#56993).
- Skripty, Bannerový systém, Skupiny práv - opravená funkcia duplikovať záznam (#56849).
- Štatistika - pridaná možnosť [nastaviť licenčné číslo](install/config/README.md#licencie) pre knižnicu amcharts na zobrazenie grafov (#56277-7).
- Štatistika - upravené zaznamenávanie chybných URL adries - odstránený identifikátor session `jsessionid`, ktorý môžu pridávať do URL adresy niektoré roboty (#56277-11).
- Úlohy na pozadí - opravený reštart úloh na pozadí po uložení úlohy.
- Úrovne logovania - opravené nastavenie úrovní do `Logger` objektu (#56277-12).
- Video - pridaná podpora vkladania odkazov na `YouTube Shorts` stránku (#56993).
- Web stránky - opravené otvorenie priečinka zadaním jeho ID, ak sa priečinok nachádza v inej doméne (#56277-7).
- Web stránky - PageBuilder - opravené vkladanie odkazu (duplikovanie okna súborov), vkladanie formulárových polí a upravený vizuál podľa aktuálnej verzie (#56277-9).
- Web stránky - v okne vloženia obrázku pridaná podpora zobrazenia cesty v stromovej štruktúre k existujúcemu obrázku s prefixom `/thumb` (#56277-9).
- Web stránky - opravené zobrazenie prekladových kľúčov na základe prefixu ID šablóny (#56393-7).
- Web stránky - opravené zmazanie stránky, ktorá má nastavené aj publikovanie do budúcna/notifikáciu (a pred zmazaním bola zobrazená v editore stránok) (#56393-8).
- Web stránky - Page Builder - opravené vkladanie video súborov (odkazov na YouTube video) (#56993).
- Web stránky - pri vkladaní odkazu na web stránku sú filtrované priečinky `images,files` s plno textovým indexom aj keď sa nejedná o koreňový priečinok (#56981).

2024.0.21

UPOZORNENIE: upravené čítanie a ukladanie hesiel používateľov, po nasadení overte prácu s používateľským kontom, hlavne zmenu hesla, zabudnuté heslo atď. Použite skript `/admin/update/update-2023-18.jsp` pre základnú úpravu súborov.

- Bezpečnosť - opravená kontrola prístupu k súborom v priečinku `/files/protected/` pri použití externých súborov - nastavená konf. premenná `cloudStaticFilesDir` (#56277-6).
- Bezpečnosť - opravená kontrola typov súborov pri nahrávaní vo formulároch a použití `/XhrFileUpload` (#56633).
- Elektronický obchod - opravený import cenníka
- Hromadný email - vrátená trieda `EMailAction` pre použitie v úlohách na pozadí pre odosielanie hromadného emailu.
- Inštalácia - upravená detekcia `JarPackaging` pri štarte ak neexistuje súbor `poolman.xml`.
- Klonovanie štruktúry - opravené klonovanie v jedno doménovej inštalácii.
- Klonovanie štruktúry - pri klonovaní priečinka doplnené kopírovanie všetkých atribútov pôvodného priečinka (html kód do hlavičky, meno inštalácie, prístupové práva, zobrazenie v mape stránok a navigačnej lište) (#56633).
- Plno textové vyhľadávanie - doplnená kontrola nastavenia zaškrtávacieho poľa Indexovať súbory pre vyhľadávanie v nastavení priečinka. Ak pole nie je zaškrtnuté, súbory v priečinku sa nebudú indexovať. Pôvodná verzia kontrolovala len existenciu priečinka `/files` v karte System vo web stránkach (#56277-6).
- Používatelia - opravené uloženie hesla bez šifrovania pri použití API `UsersDB.getUser/UsersDB.saveUser` pri prechode cez GUI. Predpokladalo sa, že heslá budú pri API volaní vopred zašifrované, čo sa neudialo. Kód doplnený o detekciu `hash`, pri čítaní z databázy sa heslá, salt a API kľúč nečíta a nastaví sa hodnota "Heslo nezmenené". Pri zmene hesla dôjde k odhláseniu ostatných relácií toho istého používateľa. (#56277-6).
- Vyhľadávanie - vylúčené indexovanie súborov z priečinka začínajúce na `/files/protected/`, pre `Lucene` vyhľadávanie doplnená kontrola na túto cestu, odkaz nebude do vyhľadania zaradený (štandardné databázové vyhľadávanie podmienku už obsahovalo) (#56277-6).
- Zrkadlenie štruktúry/Klonovanie - doplnené kopírovanie voľných polí priečinka (#56637).
- Web stránky - upravené načítanie stránok z podadresárov - filtrovaný je zoznam stránok plno textového vyhľadávania, ak sa nachádza v hlavnom priečinku domény (#56277-6).

2024.0.17

- Bezpečnosť - opravené zraniteľnosti z penetračných testov (#55193-5).
- Bezpečnosť - upravené vkladanie objektov pomocou zápisu `!REQUEST` tak, aby boli [filtrované špeciálne HTML znaky](frontend/thymeleaf/text-replaces.md#parametre-a-atribúty) (#55193-6).
- Bezpečnosť - trieda `BrowserDetector` vráti hodnoty s filtrovanými špeciálnymi HTML znakmi (#55193-6).
- Bezpečnosť - opravené generovanie QR kódu pre dvoj faktorovú autorizáciu, opravené uloženie autorizačného tokenu pri vynútenej dvoj faktorovej autorizácii po prihlásení (keď je nastavená konf. premenná `isGoogleAuthRequiredForAdmin` na hodnotu true) (#56593).
- Datatabuľky - pridaná možnosť preskočiť chybné záznamy pri importe z xlsx, chybové správy sú kumulované do jednej spoločnej notifikácie (#56465).
- Datatabuľky - opravený import iba nových záznamov (#56393-4).
- Formuláre - opravené prepínanie kariet kliknutím na šípky na klávesnici pri zadávaní textu v kartách Rozšírené nastavenia alebo Limity na súbory (#56393-3).
- Formuláre - doplnená možnosť vytlačiť formulár v zozname formulárov (#56393-3).
- Formuláre - opravené zobrazenie náhľadu formulára odoslaného ako email bez formátovania (#55193-6).
- HTTP hlavička `Strict-Transport-Security` je predvolene nastavená na hodnotu `max-age=31536000` (#55193-5).
- Hromadný email - opravené získanie web stránky z URL adresy (#56393-3).
- Hromadný email - opravené nastavenie príjemcov pre novo vytváraný email (#56409).
- Hromadný email - pridaná možnosť manuálne zadať viacerých príjemcov emailu oddelených aj znakom medzera (podporované je oddelenie znakom čiarka, bodkočiarka, medzera alebo nový riadok) a preskočiť chybné emaily (#56465).
- Hromadný email - pri znova uložení kampane/spustení a zastavení odosielania sú z kampane zmazaný aktuálne odhlásený príjemcovia (aby nedošlo k opätovnému odoslaniu emailu po odhlásení), zlepšená kontrola duplicít pri manuálnom pridaní a importe z xlsx (#56465).
- Hromadný email - upravená kontrola email adresy, povolené aj jedno písmenové domény a email adresy (#56465).
- Mazanie dát - cache objekty - upravená dostupnosť tlačidiel zmazať všetko len pre prípad, keď nie je označený ani jeden riadok.
- Média - výber média skupiny, ktorá má obmedzené práva.
- Notifikácie - pridaná možnosť posúvania zoznamu notifikácii - pre prípad zobrazenia dlhej notifikácie, alebo veľkého množstva notifikácií (#56465).
- PDF - opravené generovanie PDF súborov s vloženým obrázkom cez httpS protokol - knižnica `pd4ml` chybne deteguje verziu Javy z druhého čísla podľa pôvodného číslovania `1.8`, pričom aktuálne sa používa `17.0`. Upravené dočasnou zmenou druhého čísla na hodnotu 8 (#56393-2).
- Používatelia - zlepšený import používateľov - automatické generovanie prihlasovacieho mena, hesla a doplnené číslo riadku pri chybnom zázname (#56465).
- Štatistika - opravený jazyk a formát dátumov v grafoch štatistiky podľa zvoleného jazyka prihlásenia (#56381).
- Otázky a odpovede - opravené zobrazenie stĺpca Otázka už bola zodpovedaná, pri uložení odpovede sa skopíruje odpoveď do emailu do odpovede na web stránku ako bolo vo verzii 8 (#56533).
- Vloženie dokumentu - doplnené opakované získanie náhľadu dokumentu, ak sa ho nepodarí načítať na prvý pokus (#56393-3).
- Web stránky - zrušená klávesová skratka `ctrl+shift+v` pre vloženie ako čistý text, keďže túto možnosť už štandardne poskytuje priamo prehliadač (#56393-3).

2024.0.9

- Datatabuľky - opravený export dát v štatistike (pri tabuľke so `serverSide=false`) (#56277-3).
- Galéria - opravené načítanie zoznamu fotografií pri zobrazení galérie so zadaného priečinku (napr. vo web stránke) (#56277-1).
- Používatelia - zobrazenie práv na web stránky a priečinky upravené pre zobrazenie každého záznamu na novom riadku pre lepší prehľad (#56269).
- Používatelia - upravený export a import pre podporu doménových mien pri nastavení práv na web stránky a priečinky (#56269).
- Web stránky - opravené nastavenie priečinka podľa titulku stránky pri ešte neuloženej web stránke a pretiahnutí obrázku priamo do editora (#56277-1)
- Web stránky - pridaná možnosť zadať do odkazu telefónne číslo vo forme `tel:0903xxxyyy` (#56277-4)
- SEO - oprava zaznamenania prístupu robota bez odsúhlaseného GDPR (štatistika robota sa zaznamená bez ohľadu na súhlas) (#56277-5).

## 2024.0

> Verzia 2024.0 obsahuje novú verziu **aktualizácie s opisom zmien**, **klonovanie štruktúry** integrované s funkciou zrkadlenia (vrátane možnosti prekladov), pridáva možnosť **obnoviť** web stránku, alebo **celý priečinok z koša**, pridáva **editor typu HTML** a možnosť nastavenia typu editora priamo pre šablónu, **aplikáciam** je možné **zapnúť zobrazenie len pre zvolené typy zariadení** mobil, tablet, PC a samozrejme zlepšuje bezpečnosť a komfort práce.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/YGvWne70czo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Zoznam zmien je zhodný s verziou [2023.53-java17](CHANGELOG-2023.md).