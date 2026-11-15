# Overenie pripraveného obsahu

Vykonané 15. 11. 2026. Kontrola sa týka šiestich fragmentov v `sk/` a ich lokálneho náhľadu v existujúcej verejnej šablóne webjetcms.sk.

## HTML, obrázky a odkazy

- Šesť fragmentov má správne vnorené a uzavreté elementy, práve jeden H1 a jeden vložený `style`. Žiadny neobsahuje obal `article`, `html`, `head` alebo `body`.
- Iba úvod obsahuje jeden `script` pre živé novinky. Jeho syntax prešla `node --check`.
- Skontrolované sú jedinečné ID a vnútorné aj medzistránkové kotvy. Odkazy na AI a schvaľovanie smerujú na existujúce ID na stránke Funkcie.
- Po doplnení videí zostáva 10 statických použití obrázkov (9 jedinečných súborov); majú existujúci súbor, alt text a rozmery zodpovedajúce PNG. Pätnásť dodaných obrázkov vrátane šiestich voliteľných alternatív k videám je popísaných v [manifeste](image-manifest.md); deväť dokumentačných originálov je binárne zhodných so zdrojom.
- Všetkých 10 video značiek používa požadovaný formát `!INCLUDE` s `percentageWidth=100` a `autoplay=0`. Päť jedinečných ID je podložených dokumentáciou a overených cez YouTube oEmbed (HTTP 200). Každé umiestnenie má vecný popis a priamy odkaz na video; podklady sú v [manifeste videí](video-manifest.md).
- Náhľad rozpoznal všetkých 10 značiek a nahradil ich responzívnymi iframe. Parsovanie oboch variantov HTML prešlo bez neuzavretých elementov. Python syntax náhľadu bola overená cez `compile()`.
- Všetkých 28 jedinečných odkazov na dokumentáciu má existujúci podkladový Markdown dokument na verejnom serveri, overený HTTP 200 a obsahom dokumentu. Pri Docsify sa používajú adresáre pre README a cesty bez prípony pre ostatné stránky; `.html` by tu vracalo 404.
- Lokálne odkazy v Markdown podkladoch boli skontrolované voči súborom. Neoverovali sa automaticky všetky odvodené Markdown kotvy.
- `git diff --check` nehlási problémy. Nové súbory boli zahrnuté aj vo vlastnej kontrole HTML a odkazov. Agent nevytváral commity.

## Prehliadač

Použitý náhľad na `http://127.0.0.1:8765/` s aktuálnou CSS šablónou webjetcms.sk a Bootstrapom 4.1.3.

- Všetkých šesť stránok: kontrola šírky 320, 390 a 1280 px. Nebol zistený horizontálny presah článku ani stránky.
- Úvod a stránka Pre agentúry boli vizuálne skontrolované na mobile; úvod a novinky na desktope.
- Novinky majú jeden stĺpec pri 390 px, dva pri 768 px a tri pri 1280 px.
- Reálny JSON sa načítal cez CORS; zobrazilo sa šesť položiek v poradí zdroja. Všetkých šesť obrázkov sa úspešne načítalo. Viditeľné sú dátumy a označenia Facebook/YouTube.
- Natívny detail AI funkcií sa otvoril klávesom Enter; zostal bez horizontálneho presahu aj pri 320 px.
- Dočasná zmena veľkosti prehliadača bola po kontrole zrušená.

Po doplnení videí sa zopakovala kontrola všetkých šiestich stránok pri 390 a 1280 px. Žiadny článok nemal horizontálny presah, všetky prehrávače mali rozmery zodpovedajúce pomeru 16 : 9 a šírke stĺpca. V náhľade nezostala nevykreslená značka `!INCLUDE`. Úvod bol opäť vizuálne skontrolovaný na mobile aj desktope; náhľady PageBuilder a AI videa sa načítali z YouTube.

PageBuilder video sa v lokálnom náhľade úspešne spustilo kliknutím, priebežne zobrazovalo titulky a uplynulý čas, následne bolo pozastavené. Ostatné videá boli overené cez dokumentáciu, oEmbed a rozmery prehrávačov; neprehrávali sa celé.

Uložené snímky: [úvod na desktope](sk/previews/uvod-desktop.jpg), [novinky na desktope](sk/previews/novinky-desktop.jpg), [úvod na mobile](sk/previews/uvod-mobil.jpg). Sú to kontrolné náhľady, nie obrázky určené na nahratie do produkčného priečinka `/images/webjetcms-2026/`.

## Chybové scenáre noviniek

Skutočný inline skript bol extrahovaný z `sk/index.html` a spustený v dočasnom Node/jsdom prostredí bez inštalácie ďalších balíkov. **12 z 12 scenárov prešlo.** Táto kontrola dopĺňa reálne načítanie v prehliadači.

Overené boli poradie a limit šiestich kariet, duplicitné a nepovolené odkazy, text obsahujúci HTML/XSS, jednopriechodové dekódovanie entít vo výňatku, chýbajúce a neplatné obrázky/dátumy, chyba načítania obrázka, prázdny a chybný JSON, sieťová/HTTP chyba, veľkostné limity, timeout vrátane neskorej odpovede, chýbajúce `fetch` a opakované spustenie skriptu. Kontrola zahŕňala aj `credentials: omit`, `referrerPolicy: no-referrer` a `cache: no-cache`.

Pri chybe zostáva dostupný statický odkaz na dokumentáciu a aktuálnu verziu. Dáta zo zdroja sa nezapisujú ako HTML a novinky nevkladajú video prehrávače. Pri doplnení videoukážok do produktových sekcií sa skript noviniek nezmenil; porovnanie s predchádzajúcou verziou potvrdilo zhodu, preto sa jeho chybové scenáre neopakovali.

## Rozsah a obmedzenia

Kontrola nepoužila redakčný editor starej produkčnej inštalácie. Po vložení treba overiť, že editor zachoval `style`, `script`, `details` a `!INCLUDE`, že aplikácia videá správne vykreslí a prehrá, že sú obrázky na dohodnutej ceste a prípadná CSP povoľuje načítanie JSON, náhľadov a prehrávačov YouTube. Tieto kroky sú popísané v [návode na vloženie](README.md).

Lokálny náhľad odstraňuje pôvodné skripty, formuláre a trackovanie šablóny. Neoveruje nezmenenú mobilnú navigáciu, kontaktné vyskakovacie okno, odosielanie kontaktu ani produkčné meranie. Nové stránky ani zmeny menu sa na verejnom webe nezakladali. Produktové funkcie CMS neboli v rámci obsahovej úpravy runtime testované; tvrdenia boli porovnané s dokumentáciou a vybranými implementačnými podkladmi.
