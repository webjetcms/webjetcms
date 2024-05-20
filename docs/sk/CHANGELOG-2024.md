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
- Vyhľadávanie - vylúčené indexovanie súborov z priečinka začínajúce na `/files/protected/`, pre `Lucene` vyhľadávanie doplnená kontrola na túto cestu, odkaz nebude do vyhľadania zaradený (štadardné databázové vyhľadávanie podmienku už obsahovalo) (#56277-6).
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