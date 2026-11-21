# Overenie pripraveného obsahu

Základné overenie bolo vykonané 15. 11. 2026; kompatibilita náhľadu s HTML embed bola doplnená 16. 11. 2026. Kontrola sa týka šiestich fragmentov v `sk/` a ich lokálneho náhľadu v existujúcej verejnej šablóne webjetcms.sk.

## HTML, obrázky a odkazy

- Šesť fragmentov má správne vnorené a uzavreté elementy, práve jeden H1 a jeden vložený `style`. Žiadny neobsahuje obal `article`, `html`, `head` alebo `body`.
- Iba úvod obsahuje skript živých noviniek, aktuálne uložený v Base64 parametri aplikácie HTML embed. V zdrojovom fragmente nie je priamy element `script`; po dekódovaní sa vykreslí jeden. Jeho syntax prešla `node --check` aj po úprave náhľadu 16. 11. 2026.
- Skontrolované sú jedinečné ID a vnútorné aj medzistránkové kotvy. Odkazy na AI a schvaľovanie smerujú na existujúce ID na stránke Funkcie.
- Po doplnení sekcií headless a viackrokových formulárov zostáva 11 statických použití obrázkov (9 jedinečných súborov); majú existujúci súbor, alt text a rozmery zodpovedajúce PNG. Pätnásť dodaných obrázkov vrátane šiestich voliteľných alternatív k videám je popísaných v [manifeste](image-manifest.md); deväť dokumentačných originálov je binárne zhodných so zdrojom.
- Všetkých 11 video značiek používa požadovaný formát `!INCLUDE` s `percentageWidth=100` a `autoplay=0`. Päť jedinečných ID je podložených dokumentáciou a overených cez YouTube oEmbed (HTTP 200). Každé umiestnenie má vecný popis a priamy odkaz na video; podklady sú v [manifeste videí](video-manifest.md).
- Náhľad rozpoznal všetkých 11 značiek a nahradil ich responzívnymi iframe. Parsovanie oboch variantov HTML prešlo bez neuzavretých elementov. Python syntax náhľadu bola overená cez `compile()`.
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

Po zvýraznení headless režimu a viackrokových formulárov boli Úvod a Benefity skontrolované pri 320, 390 a 1280 px bez horizontálneho presahu. Na úvode sú dva stručné bloky vedľa seba, na mobile pod sebou. V Benefitoch sa načítala existujúca headless snímka a prehrávač formulárov. Nové kotvy medzi stránkami, ID a `aria-labelledby` prešli kontrolou; CSS aj skript noviniek zostali nezmenené. Použité tvrdenia boli overené voči dokumentácii, bez doplnenia sľubov konverzií, trvalého uloženia rozpracovaného formulára či automatickej kompatibility frontendov.

Náhľady doplnených sekcií: [úvod – headless a formuláre](sk/previews/uvod-headless-formulare.jpg), [Benefity – headless a formuláre](sk/previews/benefity-headless-formulare.jpg).

Uložené snímky: [úvod na desktope](sk/previews/uvod-desktop.jpg), [novinky na desktope](sk/previews/novinky-desktop.jpg), [úvod na mobile](sk/previews/uvod-mobil.jpg). Sú to kontrolné náhľady, nie obrázky určené na nahratie do produkčného priečinka `/images/webjetcms-2026/`.

## Chybové scenáre noviniek

Dňa 15. 11. 2026 bol skript, vtedy uložený priamo ako element `script` v `sk/index.html`, extrahovaný a spustený v dočasnom Node/jsdom prostredí bez inštalácie ďalších balíkov. **12 z 12 scenárov prešlo.** Táto historická kontrola dopĺňa reálne načítanie v prehliadači. Aktuálny dekódovaný skript je s touto podobou zhodný; zmenu spôsobu uloženia opisuje nasledujúca sekcia.

Overené boli poradie a limit šiestich kariet, duplicitné a nepovolené odkazy, text obsahujúci HTML/XSS, jednopriechodové dekódovanie entít vo výňatku, chýbajúce a neplatné obrázky/dátumy, chyba načítania obrázka, prázdny a chybný JSON, sieťová/HTTP chyba, veľkostné limity, timeout vrátane neskorej odpovede, chýbajúce `fetch` a opakované spustenie skriptu. Kontrola zahŕňala aj `credentials: omit`, `referrerPolicy: no-referrer` a `cache: no-cache`.

Pri chybe zostáva dostupný statický odkaz na všetky zmeny v dokumentácii. Dáta zo zdroja sa nezapisujú ako HTML a novinky nevkladajú video prehrávače. Pri doplnení videoukážok do produktových sekcií sa skript noviniek nezmenil; porovnanie s predchádzajúcou verziou potvrdilo zhodu, preto sa jeho chybové scenáre neopakovali.

## HTML embed a lokálny náhľad, 16. 11. 2026

Používateľ uložil skript noviniek do `!INCLUDE(/components/app-htmlembed/embed.jsp, html=...)!`, aby editor nemenil jeho medzery. [Náhľad](preview.py) teraz podporuje tento formát podľa [embed.jsp](../../src/main/webapp/components/app-htmlembed/embed.jsp) a `Tools.URLDecode`: Base64 dekódovanie, potom pri prefixe `utf8:` jedno URL dekódovanie vrátane prevodu `+` na medzeru. Dekódovaný text iba vloží do odpovede; JSP ani JavaScript na serveri nevykonáva.

- Všetkých šesť fragmentov prešlo spracovaním bez zvyšnej značky `!INCLUDE`: zostalo 11 video prehrávačov a na úvode pribudol jeden dekódovaný element `script`.
- Overené boli diakritika, rozlíšenie `+` a `%2B`, jednopriechodové dekódovanie `%2520` a zachovanie nesúvisiacej značky aplikácie. Kód vložený do testovacieho skriptu sa na serveri nevykonal.
- Python zdroj prešiel `compile()` a skutočný dekódovaný skript noviniek `node --check`.
- Dekódovaný JavaScript je presne zhodný s predchádzajúcim priamym skriptom v histórii súboru (`5c12a2dd5`). Jeho 12 chybových scenárov sa preto neopakovalo.
- Spracovanie náhľadu nezmenilo zdrojové HTML súbory; hash používateľovej značky HTML embed zostal zachovaný. Táto kontrola nepredstavuje nové overenie produkčného CMS editora ani prehratia videí.

## Doplnenie Open Source odkazu, 16. 11. 2026

Na úvode pribudol textový odkaz **Open Source verzia na GitHube** na `https://github.com/webjetcms`, vedľa odkazu na AI a nad prvým videom. V lokálnom prehliadači boli overené jeho cieľ, zalomenie pri šírke 390 px a zobrazenie pri 1328 px bez horizontálneho presahu. Po dekódovaní HTML embed sa načítalo všetkých šesť noviniek; zdrojová značka používateľa zostala nezmenená. Uložený je [detail úvodnej sekcie](sk/previews/uvod-opensource.jpg). Kontrola sa týka obsahu článku; navigácia verejnej šablóny zostáva mimo rozsahu.

## Rozsah a obmedzenia

Kontrola nepoužila redakčný editor starej produkčnej inštalácie. Po vložení treba overiť, že editor zachoval `style`, `details` a značky `!INCLUDE` vrátane Base64 parametra HTML embed. CMS musí vykresliť dekódovaný skript aj videá; treba skontrolovať ich funkčnosť, obrázky na dohodnutej ceste a prípadnú CSP pre skript, JSON, náhľady a prehrávače YouTube. Tieto kroky sú popísané v [návode na vloženie](README.md).

Lokálny náhľad odstraňuje pôvodné skripty, formuláre a trackovanie šablóny. Neoveruje nezmenenú mobilnú navigáciu, kontaktné vyskakovacie okno, odosielanie kontaktu ani produkčné meranie. Nové stránky ani zmeny menu sa na verejnom webe nezakladali. Produktové funkcie CMS neboli v rámci obsahovej úpravy runtime testované; tvrdenia boli porovnané s dokumentáciou a vybranými implementačnými podkladmi.


## Anglická verzia a jazykové priečinky obrázkov (16. 11. 2026)

Pribudlo šesť fragmentov v `en/`, deväť dokumentačných obrázkov a EN voľba lokálneho náhľadu. Overenie sa týka pripraveného balíka, nie vloženia do produkčného CMS. Existujúce SK články sa menia iba v cestách troch používaných screenshotov; logá a video značky zostávajú zachované.

- Nezávislé porovnanie so SK podkladmi overilo zachovanie sekcií, zoznamov, rozbaľovacích detailov a technických ID. EN obsah má preložené popisy, alt texty, navigačné popisy a CTA; vlastné názvy slovenských referenčných značiek zostávajú pôvodné.
- Overené je vnorenie HTML, jeden H1 a style na fragment, jedinečné ID, väzby `aria-labelledby`, vnútorné a medzistránkové kotvy, existujúce súbory cieľovej EN dokumentácie a rozmery PNG. EN fragmenty majú `lang="en"` a neobsahujú video include. Všetkých jedenásť SK videoumiestnení má EN obrazovú náhradu.
- Všetkých deväť EN obrázkov je binárne zhodných s uvedeným zdrojom v `docs/en/`; boli vizuálne prezreté. Dve pôvodné snímky z EN dokumentácie boli celé slovenské, preto sa použili vhodnejšie zábery AI četu a používateľských oprávnení. Zvyšný slovenský ukážkový obsah a popisky sa evidujú v [manifeste](image-manifest.md).
- EN doména a existujúce cesty sa overili podľa verejnej navigácie `https://www.webjetcms.com/`; `/en/` na `webjetcms.sk` vrátilo 404. Nové stránky pre agentúry a vývojárov sú súčasťou balíka a pred zverejnením odkazov ich treba vytvoriť v CMS.
- Všetkých **28 jedinečných EN odkazov do dokumentácie** bolo overených na skutočných `.md`/`README.md` endpointoch: HTTP 200 a Markdown obsah, nie iba úspešné načítanie Docsify HTML obalu. Lokálna kontrola prešla pre 12 fragmentov, 97 odkazov v každom jazyku, 11 SK a 22 EN použití statických obrázkov. Obidve jazykové mapy náhľadu a 39 ciest obrázkov prešli skúšobnými požiadavkami s lokálnou testovacou šablónou. Python syntax prešla `compile()` a oba dekódované skripty noviniek `node --check`.
- EN skript noviniek sa po dekódovaní od SK líši iba URL feedu a formátom dátumu. Zachováva pôvodné obmedzenia a náhradné zobrazenie. Používa `wjnews.en.json`, `en-GB` a pásmo `Europe/Bratislava`.
- Náhľad na `http://127.0.0.1:8766/` načítal anglickú šablónu s koreňovým `lang="en-GB"`. Všetkých šesť EN stránok bolo skontrolovaných pri šírkach **320, 390 a 1280 px** bez horizontálneho presahu celej stránky. Mriežka Bootstrapu má záporné okraje voči obsahovému obalu, ale nevyvoláva vodorovné posúvanie stránky.
- Úvod bol vizuálne skontrolovaný na desktope aj mobile. AI detail na stránke Features sa otvoril klávesom Enter aj pri 320 px a zostal v šírke stránky. V novinkách sa cez skutočný EN feed zobrazilo šesť kariet, načítalo sa všetkých šesť náhľadov a záložná správa sa skryla. Dátumy aj texty kariet boli anglické; niektoré obrázky a cieľové príspevky zostávajú slovenské.
- SK náhľad na porte 8767 načítal obidve snímky na stránke Benefity z nových `/sk/` ciest a zachoval tri video prehrávače. Dočasné nastavenie šírky prehliadača bolo po kontrole zrušené.

Uložené náhľady: [EN úvod na desktope](en/previews/home-desktop.jpg), [EN úvod na mobile](en/previews/home-mobile.jpg), [EN novinky](en/previews/news-desktop.jpg). Priečinok `previews/` sa na produkčný web nenahráva.

Po nahratí obrázkov a vložení článkov zostáva overiť konkrétny CMS editor, nové EN stránky, dostupnosť jazykových priečinkov na oboch doménach a nezmenené časti šablóny. Pôvodné súbory v `/images/webjetcms-2026/` sa pri migrácii nesmú odstrániť, kým ich používajú publikované články. Žiadne produkčné nasadenie ani Git commit neboli súčasťou tejto práce.


### Anglické názvy obrázkov

EN balík používa anglické názvy súborov: `ai-chat.png`, `publication-history.png`, `user-permissions.png`, `document-manager.png`, `multi-step-form.png` a `custom-application.png`. Odkazy `src` aj `href` v EN HTML a manifestoch boli aktualizované. Pôvodne anglické názvy, SK súbory a spoločné logá zostávajú zachované. Premenovanie nemení obsah obrázkov; binárna zhoda bola overená pred aj po zmene. Náhľad odvodzuje cesty zo súborov v jazykových priečinkoch, preto nevyžaduje úpravu kódu.
