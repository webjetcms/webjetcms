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

Výber šírky uvádza počet kliknutí a predvolene zobrazí najpoužívanejšiu skupinu. Kontajnerové súradnice sa nemiešajú s pôvodnými súradnicami stránky, ani keď majú skupiny rovnakú číselnú šírku.

Náhľad kontajnerovej skupiny sa otvorí v najčastejšej skutočne zaznamenanej šírke okna danej skupiny; pri rovnosti počtov sa použije menšia šírka. Tak zostane zachovaný príslušný breakpoint. K polohe X sa pripočíta odsadenie kontajnera v náhľade. Ak náhľad neobsahuje zodpovedajúci pevný vycentrovaný kontajner, stránka sa zobrazí bez vrstvy kliknutí a s upozornením. Uložené údaje zostávajú zachované.

Náhľad má vlastné posúvanie. Ovládač **Zobrazenie** upravuje iba jeho zmenšenie na obrazovke; vnútorná šírka stránky zostáva zachovaná. Krytie mapy môžete meniť alebo mapu dočasne skryť. Obnovte náhľad, ak sa nepodarilo načítať stránku alebo časť mapy.

Pre náhľad sa vyberá dostupná publikovaná verzia obsahu ku koncu obdobia. Zobrazený je dátum verzie. Ak verziu nemožno spoľahlivo určiť, rozhranie upozorní na použitie aktuálneho obsahu alebo na nedostupný náhľad. Ak sa obsah v období menil, mapa spája kliknutia z viacerých verzií a zobrazí upozornenie.

Čas publikovania sa odvodzuje z dostupných údajov histórie. Pri plánovanom publikovaní ide o približný čas, pretože história neuchováva presný okamih vykonania úlohy.

Šablóna, hlavička, päta, ponuky, aplikácie a obrázky používajú aktuálne dostupné zdroje. Nejde o historickú snímku celého webu. Zmeny obsahu, pohyblivé prvky a odlišná výška okna môžu ovplyvniť presnosť pozícií. Absolútna poloha Y sa neprepočítava: zmena výšky obsahu nad kliknutím môže posunúť mapu aj pri rovnakej šírke kontajnera.

## Prenos a obmedzenia

Kliknutia sa dočasne ukladajú do cookie a odošlú pri ďalšej bežnej požiadavke na server. Každé kliknutie nevyvoláva samostatnú požiadavku. Posledné kliknutia pred odchodom z webu môžu zostať neodoslané. Stránky obslúžené výhradne z vyrovnávacej pamäte pred serverom nemusia cookie odovzdať na spracovanie.

V prehliadači čaká najviac 16 cookies kliknutí s celkovou veľkosťou do 2 KiB a platnosťou 24 hodín. Pri prekročení limitu sa najstaršie neodoslané kliknutia zahodia. Údaje uložené v mesačných databázových tabuľkách sa automaticky nemažú; odstrániť ich môžete existujúcim nástrojom na čistenie databázy.

Mapa nepredstavuje záznam jednotlivých návštev: nesleduje pohyb myši ani nevytvára záznam relácie.

## Pôvodné údaje

Existujúce mesačné tabuľky `stat_clicks_YYYY_M` sa rozšíria o nové údaje a používajú sa aj na ďalšie zaznamenávanie kliknutí. Pôvodné záznamy zostanú zachované. Kliknutiam bez evidovanej šírky okna sa priradí desktopová šírka **1920 px**, pod ktorou ich nájdete vo výbere šírky.

Pôvodné súradnice sa nemenia. Mohli byť merané voči obalovému elementu stránky, ktorého polohu už nemožno spätne zistiť. Staré mapy pri šírke 1920 px sú preto orientačné; táto šírka nepredstavuje dodatočne zistenú veľkosť pôvodného okna.

Kliknutia zaznamenané pred zavedením kontajnerových skupín sa spätne neprepočítavajú. Zostanú oddelené od kontajnerových skupín. Nové záznamy uchovávajú režim súradníc v `coordinate_mode`, šírku skupiny v `viewport_width` a skutočnú šírku okna v `source_viewport_width`. Čakajúce cookies staršieho formátu sa počas ich platnosti naďalej spracujú ako súradnice stránky.

Doména starých záznamov sa priradí podľa aktuálnej domény zdrojovej stránky. Ak stránku alebo jej doménu nemožno dohľadať, záznamy zostanú uložené, ale v mape kliknutí sa nezobrazia. Historické zmeny domény sa spätne nerekonštruujú.
