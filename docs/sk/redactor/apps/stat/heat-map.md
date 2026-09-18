# Mapa kliknutí

Mapa kliknutí zobrazuje miesta, na ktoré návštevníci klikajú. Otvorte **Štatistika → Mapa kliknutí**, vyberte obdobie a kliknite na názov stránky. Zoznam obsahuje iba stránky aktuálnej domény so zaznamenanými kliknutiami, ku ktorým máte prístup.

## Zapnutie merania

V nastavení šablóny alebo skupiny šablón vyberte režim **Štatistika + mapa kliknutí**. Skript sa vloží do stránky automaticky. Režim **Štatistika** zaznamenáva bežnú návštevnosť, režim **Vypnuté** vypne meranie stránky. Hodnota **Zdediť nastavenie** použije nastavenie nadradenej úrovne; nastavenie šablóny má prednosť pred skupinou šablón. Globálne vypnutie štatistiky má prednosť.

Konfiguračná premenná `statMode=none` vypne celé meranie. Hodnota `statEnableClickTracking=false` vypne mapu kliknutí aj pri jej zapnutí v šablóne; bežná štatistika zostane zapnutá podľa nastaveného režimu.

Kliknutia sa zaznamenávajú podľa nastavenia súhlasu so štatistickými cookies. Bez povolenia sa mapa kliknutí nezaznamenáva. Bežná štatistika zachováva doterajšie pravidlá anonymizácie.

## Výber šírky a náhľad

Nové kliknutia sa evidujú samostatne pre každú presnú šírku okna v CSS pixeloch. Výber šírky uvádza aj počet kliknutí a predvolene zobrazí najpoužívanejšiu šírku. Mobilné a široké zobrazenie sa tak nepremiešajú.

Náhľad má vlastné posúvanie. Ovládač **Zobrazenie** upravuje iba jeho zmenšenie na obrazovke; vnútorná šírka stránky zostáva zachovaná. Krytie mapy môžete meniť alebo mapu dočasne skryť. Obnovte náhľad, ak sa nepodarilo načítať stránku alebo časť mapy.

Pre náhľad sa vyberá dostupná publikovaná verzia obsahu ku koncu obdobia. Zobrazený je dátum verzie. Ak verziu nemožno spoľahlivo určiť, rozhranie upozorní na použitie aktuálneho obsahu alebo na nedostupný náhľad. Ak sa obsah v období menil, mapa spája kliknutia z viacerých verzií a zobrazí upozornenie.

Čas publikovania sa odvodzuje z dostupných údajov histórie. Pri plánovanom publikovaní ide o približný čas, pretože história neuchováva presný okamih vykonania úlohy.

Šablóna, hlavička, päta, ponuky, aplikácie a obrázky používajú aktuálne dostupné zdroje. Nejde o historickú snímku celého webu. Zmeny obsahu, pohyblivé prvky a odlišná výška okna môžu ovplyvniť presnosť pozícií.

## Prenos a obmedzenia

Kliknutia sa dočasne ukladajú do cookie a odošlú pri ďalšej bežnej požiadavke na server. Každé kliknutie nevyvoláva samostatnú požiadavku. Posledné kliknutia pred odchodom z webu môžu zostať neodoslané. Stránky obslúžené výhradne z vyrovnávacej pamäte pred serverom nemusia cookie odovzdať na spracovanie.

V prehliadači čaká najviac 16 cookies kliknutí s celkovou veľkosťou do 2 KiB a platnosťou 24 hodín. Pri prekročení limitu sa najstaršie neodoslané kliknutia zahodia. Údaje uložené v mesačných databázových tabuľkách sa automaticky nemažú; odstrániť ich môžete existujúcim nástrojom na čistenie databázy.

Mapa nepredstavuje záznam jednotlivých návštev: nesleduje pohyb myši ani nevytvára záznam relácie.

## Pôvodné údaje

Existujúce mesačné tabuľky `stat_clicks_YYYY_M` sa rozšíria o nové údaje a používajú sa aj na ďalšie zaznamenávanie kliknutí. Pôvodné záznamy zostanú zachované. Kliknutiam bez evidovanej šírky okna sa priradí desktopová šírka **1920 px**, pod ktorou ich nájdete vo výbere šírky.

Pôvodné súradnice sa nemenia. Mohli byť merané voči obalovému elementu stránky, ktorého polohu už nemožno spätne zistiť. Staré mapy pri šírke 1920 px sú preto orientačné; táto šírka nepredstavuje dodatočne zistenú veľkosť pôvodného okna.

Doména starých záznamov sa priradí podľa aktuálnej domény zdrojovej stránky. Ak stránku alebo jej doménu nemožno dohľadať, záznamy zostanú uložené, ale v mape kliknutí sa nezobrazia. Historické zmeny domény sa spätne nerekonštruujú.
