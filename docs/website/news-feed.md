# Novinky na úvodnej stránke

Živý zdroj: [wjnews.sk.json](https://docs.webjetcms.sk/json/wjnews.sk.json). Overené **15. 11. 2026 o 15:47 CET**, požiadavkou GET s hlavičkou `Origin: https://www.webjetcms.sk`. Ide o verejný agregovaný obsah WebJET CMS z Facebooku a YouTube, nie o kompletný changelog produktu.

## Overená odpoveď servera

| Kontrola | Výsledok |
| --- | --- |
| HTTP stav | `200` |
| Typ | `application/json;charset=UTF-8` |
| Veľkosť | 8 051 bajtov |
| CORS | `Access-Control-Allow-Origin: *` |
| Cache | `Cache-Control: max-age=86400`, dostupné `ETag` a `Last-Modified` |
| Posledná úprava pri kontrole | `2026-11-15 13:20:02 UTC` |
| Počet položiek | 6, z toho 3 Facebook a 3 YouTube |
| Poradie | Od najnovšej; v overenom výstupe najprv všetky tri Facebook príspevky |

JSON možno načítať z `www.webjetcms.sk` obyčajným `fetch` bez prihlasovacích údajov. Použiť `credentials: 'omit'`; wildcard CORS sa nekombinuje s credentialed požiadavkou. `mode: 'no-cors'` sa nepoužíva, pretože odpoveď by JavaScript nevedel prečítať. Do prehliadača nepatrí Facebook ani YouTube token.

Overené boli aj hlavičky jedného aktuálneho Facebook náhľadu a jedného YouTube náhľadu: oba vrátili HTTP 200, `image/jpeg`, CORS `*` a `Cache-Control: max-age=300`. Pre aktuálne náhľady preto funguje aj `crossorigin="anonymous"`. Ide o kontrolu dvoch vzoriek, nie každého budúceho obrázka. [Facebook náhľad](https://docs.webjetcms.sk/json/social-media/images/ba13b51c3251e3e9e7823724c5b12af8.jpg), [YouTube náhľad](https://docs.webjetcms.sk/json/social-media/images/8f0251d82f0cb40c797d3a3df9770359.jpg)

## Formát a význam polí

Koreň je objekt `{ "news": [...] }`. Generátor zachováva historické základné polia; klient musí vedieť pracovať aj bez rozšírených voliteľných polí.

| Pole | Typ a význam | Použitie na úvode |
| --- | --- | --- |
| `title` | Textový názov; aktuálne Facebook názvy majú 120 znakov vrátane výpustky. | Nadpis karty cez `textContent`. Ďalej ho zbytočne neskracovať na pevný počet riadkov. |
| `perex` | Krátky výňatok do 180 zobrazených znakov, môže byť prázdny. Generátor kóduje HTML entity kvôli staršiemu dashboardu. | Skript raz dekóduje presne entity generátora a vloží text karty cez `textContent`. |
| `perexLong` | Dlhší výňatok do 1 000 zobrazených Unicode znakov, tiež s HTML entitami. | Úvod ho nepoužíva; dashboard ho zobrazuje v rozbalenom detaile. |
| `link` | Cieľ príspevku alebo videa. | V overenom JSON `https://www.facebook.com/.../posts/...` a `https://www.youtube.com/watch?v=...`. |
| `id` | Stabilná identita, napr. `youtube:xkgdP7odoPA` alebo `facebook:237316516345904_1473712274860537`. | Verejný fragment toto pole nepotrebuje; deduplikuje podľa overeného odkazu. |
| `source` | `facebook` alebo `youtube`. | Fragment odvodzuje názov zdroja z overenej domény odkazu, nepoužíva neoverený štítok. |
| `publishedAt` | ISO dátum publikovania s časovým pásmom, napr. `2026-10-26T20:12:47Z`. | Element `time`, slovenský dátum; neplatný dátum jednoducho vynechať. |
| `image` | Relatívna cesta, aktuálne `social-media/images/<hash>.jpg`. | Doplniť cez `new URL(image, feedUrl)`, nie voči URL stránky. |

Napríklad `social-media/images/8f0251d82f0cb40c797d3a3df9770359.jpg` patrí pod `https://docs.webjetcms.sk/json/`. Na homepage sa tieto priebežne menené náhľady nemusia kopírovať do balíka statických dokumentačných obrázkov.

Aktuálne dátumy v poradí: Facebook 11. 11., 10. 11. a 4. 11. 2026; YouTube 26. 10., 21. 10. a 18. 10. 2026. Ide o snímku pri overení, nie hodnoty na zakódovanie do HTML. Pri zobrazení iba prvých troch položiek by aktuálne zmizli všetky YouTube videá. Implementácia preto zobrazuje **najviac šesť kariet** v poradí vydavateľa: tri vedľa seba od breakpointu `lg`, dve od `md` a jednu na menšej obrazovke.

## Zdroj implementácie v repozitári

- [social-news.js](../../src/main/webapp/admin/v9/src/js/dashboard/social-news.js): normalizácia, relatívne náhľady, deduplikácia a zachovanie poradia. Dashboard obmedzuje vstup na 100 položiek a 256 KiB, požiadavku na 8 sekúnd a viditeľný externý zoznam na šesť položiek. Zabezpečuje aj cache a čítanosť, ktoré verejný web nepotrebuje.
- [news-widget.js](../../src/main/webapp/admin/v9/src/js/dashboard/news-widget.js): karty, obrázky s prázdnym `alt` pri prítomnom titulku, lazy loading, zdroj, dátum a odstránenie chybného obrázka. Dashboard uprednostňuje `perexLong`.
- [SocialMediaFeedGenerator.java](../../src/main/java/sk/iway/aceintegration/socialmedia/SocialMediaFeedGenerator.java): najviac tri položky z každej siete; bez dostupných Facebook príspevkov najviac štyri YouTube videá. Celok radí zostupne podľa dátumu, pri zhode podľa ID.
- [Dokumentácia generátora](../sk/developer/install/social-media-feed.md): filtrovanie Facebook videí/Reels a YouTube odkazov, textové výňatky, ukladanie náhľadov a správanie pri výpadkoch.

Na verejnom webe sa nekopírujú interné administračné API, stav prečítania, lokálne novinky nainštalovanej verzie ani celá dashboard knižnica. Z feedu sa tiež nemá odvodiť číslo aktuálnej verzie produktu.

## Implementácia v dodanom HTML

Finálna implementácia je priamo v [sk/index.html](sk/index.html), v izolovanom inline skripte na konci fragmentu. Používa natívne DOM API a `fetch`; nepoužíva jQuery, framework ani ďalší súbor JavaScriptu.

Statické HTML obsahuje nadpis **Čo je nové vo WebJET CMS**, krátky perex, prázdny kontajner `#wj-home-news-list`, záložnú správu s odkazom do dokumentácie a odkaz **Pozrieť všetky zmeny v dokumentácii** na `https://docs.webjetcms.sk/latest/sk/`. Záložná správa sa skryje až po úspešnom vložení aspoň jednej karty. Odkaz na všetky zmeny zostáva viditeľný stále.

### Načítanie a normalizácia

- Jeden kontajner spustí požiadavku najviac raz; `data-loaded` bráni opakovanému vykonaniu skriptu nad rovnakým obsahom. Automatické opakovanie ani pravidelné obnovovanie sa nepoužívajú.
- Bez `fetch`, `AbortController` alebo `URL` sa použijú statické odkazy. Požiadavka má 8-sekundový timeout, `credentials: 'omit'`, `referrerPolicy: 'no-referrer'` a `cache: 'no-cache'`. Posledná voľba overuje aktuálnosť aj pri 24-hodinovej cache servera.
- Odpoveď musí byť úspešná a obsahovať objekt s poľom `news`. Skript odmietne deklarovaný `Content-Length` nad 262 144 bajtov a prijatý text nad 262 144 UTF-16 jednotiek pred parsovaním. To nie je streamový limit stiahnutých bajtov pri chýbajúcej hlavičke; taký limit rieši samostatne dashboard.
- Z prvých 100 záznamov vyberie najviac šesť platných, bez ďalšieho radenia. Povinné sú textový neprázdny `title`, textový `perex` a povolený cieľový odkaz. Opakované overené odkazy vynechá. Nadpis obmedzuje na 300 UTF-16 jednotiek a výňatok na 500; bežný výstup generátora je kratší.
- `summaryText` jedným priechodom dekóduje iba entity vytvorené funkciou `escapePerex`: `&amp;`, `&lt;`, `&gt;`, `&quot;`, `&#39;`, `&#91;` a `&#93;`. Výsledok sa stále vkladá cez `textContent`, takže vzniknuté zátvorky sa nespracujú ako HTML. Nadpis sa nedekóduje, pretože ho generátor posiela ako obyčajný text.
- Cieľové odkazy musia používať HTTPS bez používateľského mena, hesla a neštandardného portu. Povolené sú `facebook.com`, `www.facebook.com`, `m.facebook.com`, `web.facebook.com`, `youtube.com`, `www.youtube.com` a `youtu.be`. Z tejto overenej domény sa odvodí označenie Facebook alebo YouTube.
- Náhľad môže byť relatívny alebo absolútny, ale výsledná adresa musí patriť pod `https://docs.webjetcms.sk/json/social-media/images/`. Cesty sa riešia voči URL JSON. Iné obrázky sa vynechajú.
- Neplatný alebo chýbajúci dátum sa vynechá; platný sa zobrazí po slovensky v pásme `Europe/Bratislava` a element `time` uchová ISO hodnotu.

### Karty a záložné zobrazenie

Každá karta má voliteľný obrázok, zdroj s dátumom, nadpis H3 s odkazom a krátky výňatok. Nadpisy sa neorezávajú na pevný počet riadkov. Odkaz sa otvára v rovnakom okne, má `rel="noopener noreferrer"`. Samostatné tlačidlo ani odkaz na obrázku sa nevytvárajú.

Obrázky používajú `loading="lazy"`, `decoding="async"`, `crossorigin="anonymous"` a `referrerpolicy="no-referrer"`. Prázdny `alt` je zámerný: zmysel karty vysvetľuje susedný odkazovaný nadpis. Pri chybe načítania sa obrázok odstráni a text karty zostane. CSS udržiava náhľad v pomere 16 : 9 pomocou `object-fit: contain`, takže celý obrázok zostáva viditeľný.

Karty sa najprv pripravia v `DocumentFragment` a vložia spolu. Prázdny alebo chybný JSON, nepovolené záznamy, sieťová chyba, CORS alebo timeout nechajú dostupný statický odkaz do dokumentácie. To platí aj pri vypnutom JavaScripte. Návštevníkovi sa nezobrazuje technická chyba ani nekonečný spinner.

Feed sa neukladá do `localStorage`; fragment nespracúva čítanosť ani používateľské nastavenia. Nevkladá YouTube/Facebook iframe ani sociálne widgety. Pri zobrazení sekcie sa kontaktuje docs server kvôli JSON a obrázkom; na sociálnu sieť používateľ prejde až cez odkaz.

### Vloženie do staršieho CMS a CSP

Do `article.ly-article` treba vložiť celý obsah `sk/index.html` vrátane elementov `style` a `script`. V zdrojovom editore overiť, že ich starší CMS po uložení zachoval. Bez skriptu zostane funkčný textový odkaz do dokumentácie, živé karty sa však nevytvoria.

Ak verejný web používa Content Security Policy, jeho existujúce nastavenie musí umožniť tento inline skript a štýly, spojenie na `https://docs.webjetcms.sk` cez `connect-src` a náhľady cez `img-src`. Pri politike založenej na nonce alebo hash treba povoliť konkrétny obsah podľa pravidiel šablóny; fragment CSP sám nemení. Nejde o odporúčanie plošne zapnúť `unsafe-inline`.

## Overenie po vložení

- Na aktuálnom feede sa zobrazia obe sociálne siete a dátumy v poradí zdroja; relatívny obrázok smeruje na docs server.
- Pri vypnutom JavaScripte, blokovanom JSON, HTTP chybe a prázdnom zozname zostávajú použiteľné statické odkazy.
- Chýbajúci alebo chybný obrázok nezakryje titulok ani CTA. Dlhý Facebook titulok sa nezmení na odrezaný nečitateľný fragment.
- Text obsahujúci HTML sa zobrazí ako text, `javascript:` ani neznáma cieľová doména nevytvoria aktívny odkaz.
- Po uložení v staršom CMS skontrolovať, či zdrojový editor zachoval `script` a `style`. Ak web používa CSP, musí povoľovať vlastný inline skript podľa existujúceho nastavenia, `connect-src` pre docs server a `img-src` pre náhľady. Toto nie je pokyn na plošné uvoľnenie CSP.

Tento dokument zachytáva overený kontrakt zdroja a implementované správanie v `sk/index.html`. Kontrola kódu odhalila potrebu jednopriechodového dekódovania výňatkov; tá je už zapracovaná. Browserové a vizuálne overenie sa eviduje pri odovzdaní celého balíka.
