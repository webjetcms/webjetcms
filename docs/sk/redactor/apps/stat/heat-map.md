# Mapa kliknutí

Mapa kliknutí zobrazuje miesta, na ktoré návštevníci klikajú. Otvorte **Štatistika → Mapa kliknutí**, vyberte obdobie a kliknite na názov stránky. Zoznam obsahuje iba stránky aktuálnej domény so zaznamenanými kliknutiami, ku ktorým máte prístup.

## Zapnutie merania

V nastavení šablóny alebo skupiny šablón vyberte režim **Štatistika + mapa kliknutí**. Skript sa vloží do stránky automaticky. Režim **Štatistika** zaznamenáva bežnú návštevnosť, režim **Vypnuté** vypne meranie stránky. Hodnota **Zdediť nastavenie** použije nastavenie nadradenej úrovne; nastavenie šablóny má prednosť pred skupinou šablón. Globálne vypnutie štatistiky má prednosť.

Konfiguračná premenná `statMode=none` vypne celé meranie. Hodnota `statEnableClickTracking=false` vypne mapu kliknutí aj pri jej zapnutí v šablóne; bežná štatistika zostane zapnutá podľa nastaveného režimu.

Kliknutia sa zaznamenávajú podľa nastavenia súhlasu so štatistickými cookies. Bez povolenia sa mapa kliknutí nezaznamenáva. Bežná štatistika zachováva doterajšie pravidlá anonymizácie.

V existujúcej inštalácii pridajte v správe cookies definíciu pre mapu kliknutí, ak ešte neexistuje:

- **Názov cookie:** `wj_hm_*`
- **Kategória:** Štatistické
- **Typ:** `http`
- **Platnosť:** 24 hodín
- **Účel:** Dočasne uchováva kliknutia na stránke do ich odoslania na vytvorenie mapy kliknutí.

Definícia zastupuje cookies jednotlivých kliknutí s náhodnou príponou. Cookie lišta ponúka kategórie podľa evidovaných definícií cookies; po pridaní preto otvorte nastavenia súhlasu na webe a povoľte štatistické cookies. Nové inštalácie obsahujú túto definíciu medzi predvolenými záznamami.

## Výber šírky a náhľad

Konfiguračná premenná `statHeatMapMainContainerSelector` určuje CSS selektor meraných kontajnerov. Predvolená hodnota je `.container`, používaná v Bootstrape. Už uložené vlastné hodnoty zostávajú platné. Prázdna hodnota zapne meranie celej stránky podľa presnej šírky okna.

Pri vyplnenom selektore sa merajú iba kliknutia v najbližšom zodpovedajúcom kontajneri. Kliknutia mimo jeho hraníc sa ignorujú, aj keď ide o obsah vyčnievajúci z kontajnera. Ak selektor na stránke nič nenájde alebo nejde o platný CSS selektor, stránka kliknutia nezaznamenáva. Pôvodné selektory využívajúce rozšírenia jQuery treba nahradiť štandardným CSS selektorom.

Pevné kontajnery vycentrované voči stránke sa zoskupujú podľa nameranej šírky. Napríklad okná široké 1223 a 1369 px s rovnakým kontajnerom širokým 1140 px vytvoria jednu skupinu **Kontajner 1140 px**. Poloha X sa meria od ľavého okraja kontajnera vrátane jeho vnútorného odsadenia; poloha Y zostáva voči začiatku stránky. Pevná šírka sa rozpoznáva podľa dosiahnutej hodnoty CSS `max-width` alebo `width` s deklaráciami v pixeloch.

Ak prehliadač nemôže prečítať niektorý načítaný štýl, napríklad CSS z cudzej domény bez povoleného prístupu, na celej stránke sa použije presná šírka okna. Platí to aj pre samostatný externý štýl s fontmi. Presná šírka okna sa použije aj vtedy, keď sa pre danú vlastnosť kombinujú pixelové a relatívne deklarácie. Rovnako sa nezlučujú šírky odvodené cez `vw`, CSS premenné či výpočty a kontajnery s aktívnou CSS animáciou.

Plynulé kontajnery a kontajnery, ktoré nie sú vycentrované voči stránke, zachovávajú pôvodné súradnice aj presnú šírku okna. Mobilné šírky 390 a 430 px tak zostávajú v samostatných skupinách **Okno 390 px** a **Okno 430 px**. Aj v tomto režime sa pri vyplnenom selektore ignorujú kliknutia mimo kontajnerov.

Výber šírky uvádza počet kliknutí a predvolene zobrazí najpoužívanejšiu skupinu. Pri obnovení náhľadu sa až 30 sekúnd môžu zobraziť počty z predchádzajúceho načítania. Kontajnerové súradnice sa nemiešajú s pôvodnými súradnicami stránky, ani keď majú skupiny rovnakú číselnú šírku.

Náhľad kontajnerovej skupiny sa otvorí v najčastejšej skutočne zaznamenanej šírke okna danej skupiny; pri rovnosti počtov sa použije menšia šírka. Tak zostane zachovaný príslušný breakpoint. K polohe X sa pripočíta odsadenie kontajnera v náhľade. Ak náhľad neobsahuje zodpovedajúci pevný vycentrovaný kontajner, stránka sa zobrazí bez vrstvy kliknutí a s upozornením. Uložené údaje zostávajú zachované.

Náhľad má vlastné posúvanie. Keď obsah prejde pod pripnutú hlavičku, hlavička zakryje aj značky kliknutí na tomto obsahu. Ovládač **Zobrazenie** upravuje iba jeho zmenšenie na obrazovke; vnútorná šírka stránky zostáva zachovaná. Krytie mapy môžete meniť alebo mapu dočasne skryť. Obnovte náhľad, ak sa nepodarilo načítať stránku alebo časť mapy.

Farba vyjadruje miestnu hustotu kliknutí. Kliknutia v okolí sa sčítajú s plynulo klesajúcou váhou do vzdialenosti približne 32 pixelov. Najvyššia hustota v celej mape pre vybranú stránku, verziu, obdobie, šírku a súradnicový režim určuje červený koniec škály. Ostatné miesta používajú pomernú intenzitu od modrej po červenú. Pri výbere konkrétnej verzie sa škála vypočíta iba z jej kliknutí v zadanom období, takže aj menej navštevovaná verzia zvýrazní svoje najvyťaženejšie miesta. Možnosť **Všetky verzie** použije kliknutia za celé vybrané obdobie. Posúvanie náhľadu škálu nemení. Referenčná hustota sa ukladá do vyrovnávacej pamäte na čas `statHeatMapImageTimeout`, rovnako ako obrázky mapy.

Percento vybranej oblasti je podiel všetkých zaznamenaných kliknutí v obdĺžniku. Farba nevyjadruje toto percento: malá oblasť s tesne sústredenými kliknutiami môže byť teplejšia než väčšia oblasť s vyšším počtom rozptýlených kliknutí.

Náhľad predvolene zobrazuje publikovanú verziu stránky ku koncu vybraného obdobia. Ak ju nemožno určiť, zobrazí sa upozornenie na použitie aktuálneho obsahu alebo na nedostupný náhľad.

Ak sa stránka počas obdobia menila, v poli **Verzia stránky** je vybraná možnosť **Všetky verzie**. Filter môžete použiť aj vtedy, keď jediná dostupná verzia platila iba časť vybraného obdobia. Filter ponúka najviac 100 najnovších publikovaných verzií za vybrané obdobie. Ak hľadáte staršiu verziu, vyberte skoršie obdobie. Vyberte verziu podľa čísla a času publikovania. Náhľad zobrazí jej obsah. Mapa aj počty pri jednotlivých šírkach zahŕňajú všetky kliknutia z dní, keď bola verzia vo vybranom období zverejnená. Ak nová verzia vyšla na obed, obe verzie zobrazia všetky kliknutia z tohto dňa. V zobrazení **Všetky verzie** sa každé kliknutie započíta raz.

Ak sa v týchto dňoch nezaznamenali žiadne kliknutia, jej uložený obsah si môžete pozrieť v každej šírke zaznamenanej v danom období. Pri každej šírke sa zobrazí nula kliknutí.

Čas publikovania sa odvodzuje z dostupných údajov histórie. Pri plánovanom publikovaní ide o približný čas, pretože história neuchováva presný okamih vykonania úlohy.

Šablóna, hlavička, päta, ponuky, aplikácie a obrázky používajú aktuálne dostupné zdroje. Nejde o historickú snímku celého webu. Zmeny obsahu, pohyblivé prvky a odlišná výška okna môžu ovplyvniť presnosť pozícií. Absolútna poloha Y sa neprepočítava: zmena výšky obsahu nad kliknutím môže posunúť mapu aj pri rovnakej šírke kontajnera.

## Porovnanie dvoch verzií

Ak sú vo vybranom období dostupné aspoň dve publikované verzie stránky, tlačidlo **Porovnať verzie** otvorí dva náhľady. V poliach **Verzia A** a **Verzia B** v hlavičkách náhľadov vyberte odlišné verzie tej istej stránky. Na úzkej obrazovke sa náhľady zobrazia pod sebou.

Oba náhľady majú spoločnú **Šírku mapy**, **Zobrazenie**, **Krytie mapy** a prepínač **Zobraziť mapu**. Aj pri prispôsobení priestoru používajú rovnakú mierku. Pri kontajnerových skupinách sa pre oba náhľady použije rovnaká skutočná šírka okna, vybraná zo záznamov za celé obdobie. Náhľady možno posúvať samostatne. Pri zmene verzie sa načíta iba príslušný náhľad; druhý si zachová mapu aj pozíciu posunu.

Pod každým náhľadom sa zobrazuje jeho počet kliknutí. Ak verzia nemá kliknutia pre vybranú šírku, zobrazí sa jej obsah s nulovým počtom a bez farebnej vrstvy. Každý náhľad má vlastnú škálu podľa najvyššej hustoty kliknutí svojej verzie. Rovnaká farba preto označuje podobne vyťažené miesto v rámci danej verzie, nie rovnaký počet kliknutí v oboch náhľadoch. Počty sa neprepočítavajú podľa dĺžky platnosti verzie ani návštevnosti; verzie publikované v rovnaký deň môžu zahŕňať tie isté denné kliknutia.

Opätovným stlačením **Porovnať verzie** sa vrátite k jednému náhľadu a pôvodnému výberu verzie.

## Počet kliknutí v oblasti

Zapnutie výberu oblasti nemení polohu ani veľkosť náhľadu. Počas výberu môžete stránku posúvať kolieskom myši.

Kliknite na **Vybrať oblasť** a potiahnutím myši označte obdĺžnik v náhľade. Pod náhľadom sa zobrazí počet kliknutí v oblasti a ich percentuálny podiel pre zvolené obdobie, verziu a šírku mapy. Pri porovnaní verzií má každý náhľad vlastnú oblasť a výsledok. Výber odstránite vypnutím **Vybrať oblasť** alebo tlačidlom **Zrušiť oblasť**; zruší sa aj pri zmene verzie, šírky alebo obnovení náhľadu.

## Najklikanejšie oblasti

Tlačidlom <button class="btn btn-sm btn-primary" type="button" title="Oblasti" aria-label="Oblasti"><i class="ti ti-list-numbers" aria-hidden="true"></i></button> zobrazíte alebo skryjete panel s najviac piatimi najklikanejšími neprekrývajúcimi sa oblasťami, ich počtami a podielmi kliknutí pre vybranú stránku, obdobie, verziu a šírku mapy. Kliknutím na riadok posuniete náhľad na oblasť a zvýrazníte ju číslovanou značkou. Oblasti sa určujú podľa blízkosti uložených kliknutí. Pri porovnávaní verzií je panel skrytý.

## Prenos a obmedzenia

Kliknutia sa dočasne ukladajú do cookie a odošlú pri ďalšej bežnej požiadavke na server. Každé kliknutie nevyvoláva samostatnú požiadavku. Posledné kliknutia pred odchodom z webu môžu zostať neodoslané. Stránky obslúžené výhradne z vyrovnávacej pamäte pred serverom nemusia cookie odovzdať na spracovanie.

V prehliadači čaká najviac 16 cookies kliknutí s celkovou veľkosťou do 2 KiB a platnosťou 24 hodín. Pri prekročení limitu sa najstaršie neodoslané kliknutia zahodia. Správca môže po skončení mesiaca zmenšiť mesačné tabuľky [automatizovanou úlohou na zlučovanie kliknutí](../../../admin/settings/cronjob/README.md#zlučovanie-historických-kliknutí). Úloha zapíše súčet kliknutí v skupine do riadka s najnižším ID a zachová jeho súradnice. Na odstránenie údajov za zvolené obdobie slúži nástroj na čistenie databázy.

Veľkosť bunky pre vykresľovanie aj zlučovanie určuje `statHeatMapCellSize` (predvolene `5`, teda 5 × 5 CSS pixely). Hodnotou `4` nastavíte 4 × 4 a hodnotou `10` nastavíte 10 × 10. Väčšie bunky znižujú presnosť polohy; neskoršie zmenšenie neobnoví súradnice už zlúčených kliknutí.

Pri aktualizácii sa do mesačných tabuliek doplnia indexy na rýchlejšie načítanie počtov podľa šírky a viditeľnej časti mapy. V MySQL/MariaDB sa pred vytvorením indexov prevedú staršie InnoDB formáty riadkov `COMPACT` a `REDUNDANT` na `DYNAMIC`, ktorý podporuje indexovanie celej domény v kódovaní `utf8mb4`. Na dlhých stránkach tak databáza pri načítaní časti mapy nemusí prechádzať kliknutia z celej výšky stránky. Nové mesačné tabuľky ich obsahujú automaticky. Indexy nemenia počty ani presnosť kliknutí; zaberajú ďalšie miesto v databáze a aktualizujú sa pri zápise kliknutí. Ich prvé vytvorenie môže pri veľkých tabuľkách predĺžiť štart po aktualizácii.

Mapa nepredstavuje záznam jednotlivých návštev: nesleduje pohyb myši ani nevytvára záznam relácie.

## Pôvodné údaje

Existujúce mesačné tabuľky `stat_clicks_YYYY_M` sa rozšíria o nové údaje a používajú sa aj na ďalšie zaznamenávanie kliknutí. Rozšírenie tabuľky zachová pôvodné záznamy. Kliknutiam bez evidovanej šírky okna sa priradí desktopová šírka **1920 px**, pod ktorou ich nájdete vo výbere šírky.

Pôvodné súradnice sa nemenia. Mohli byť merané voči obalovému elementu stránky, ktorého polohu už nemožno spätne zistiť. Staré mapy pri šírke 1920 px sú preto orientačné; táto šírka nepredstavuje dodatočne zistenú veľkosť pôvodného okna.

Kliknutia zaznamenané pred zavedením kontajnerových skupín sa spätne neprepočítavajú. Zostanú oddelené od kontajnerových skupín. Nové záznamy uchovávajú režim súradníc v `coordinate_mode`, šírku skupiny v `viewport_width` a skutočnú šírku okna v `source_viewport_width`. Čakajúce cookies staršieho formátu sa počas ich platnosti naďalej spracujú ako súradnice stránky.

Doména starých záznamov sa priradí podľa aktuálnej domény zdrojovej stránky. Ak stránku alebo jej doménu nemožno dohľadať, záznamy zostanú uložené, ale v mape kliknutí sa nezobrazia. Historické zmeny domény sa spätne nerekonštruujú.
