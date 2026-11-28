# Obsah webjetcms.sk

Balík pripravený 15. 11. 2026 podľa dohodnutého zamerania na firemné weby, verejnú správu, AI, schvaľovanie a plánovanie obsahu. Dňa 16. 11. 2026 pribudla anglická verzia pre **webjetcms.com** s EN screenshotmi. SK verzia používa aj videá z dokumentácie. Príprava EN balíka ani úprava ciest obrázkov nič nepublikuje na verejný web.

## Ďalšie úpravy pomocou agenta

Na údržbu oficiálnych produktových webov použite [skill `wj-website-content`](../../.agents/skills/wj-website-content/SKILL.md). Je určený iba pre tento repozitár a neexportuje sa do zákazníckych projektov. Vychádza z existujúcich rozhodnutí a podľa zadania prepája texty, dokumentáciu, médiá a overenie výsledku.

- Nová funkcia: „Použi $wj-website-content a podľa dokumentácie k [funkcii] doplň jej prínos a podmienky dostupnosti na vhodné SK aj EN stránky. Zachovaj dizajn a existujúce makrá.“
- Existujúci text: „Použi $wj-website-content a skráť úvodný text v docs/website/sk/benefity.html. Zachovaj potvrdené tvrdenia, CTA a odkazy; uprav iba slovenskú verziu.“

## Výsledné HTML

| Stránka | Súbor na vloženie | Verejná cesta |
| --- | --- | --- |
| Úvod vrátane živých noviniek | [index.html](sk/index.html) | `/` (existujúca stránka Domov) |
| Benefity | [benefity.html](sk/benefity.html) | `/benefity/` |
| Funkcie | [funkcie.html](sk/funkcie.html) | `/funkcie/` |
| Aplikácie | [aplikacie.html](sk/aplikacie.html) | `/aplikacie/` |
| Pre vývojárov | [pre-vyvojarov.html](sk/pre-vyvojarov.html) | `/pre-vyvojarov/` |
| Pre agentúry | [pre-agentury.html](sk/pre-agentury.html) | `/pre-agentury/` |

Anglické fragmenty patria do existujúceho webu **`https://www.webjetcms.com`**, nie do `/en/` na slovenskej doméne. Nasledujúce cesty sú relatívne voči anglickej doméne; `/functions/` a `/licenses/` zachovávajú existujúce URL.

| Stránka | Súbor na vloženie | Verejná cesta |
| --- | --- | --- |
| Home vrátane EN noviniek | [index.html](en/index.html) | `/` (existujúca úvodná stránka) |
| Benefits | [benefits.html](en/benefits.html) | `/benefits/` |
| Features | [functions.html](en/functions.html) | `/functions/` |
| Applications | [applications.html](en/applications.html) | `/applications/` |
| For developers | [for-developers.html](en/for-developers.html) | `/for-developers/` – nová stránka |
| For agencies | [for-agencies.html](en/for-agencies.html) | `/for-agencies/` – nová stránka |

Každý súbor obsahuje **celý obsah na vloženie dovnútra existujúceho `article.ly-article`**. Neobsahuje ďalší `article` ani obal celej HTML stránky. V úvode súboru je malý `style` obmedzený na `.wj-website-content`; nadväzuje na typografiu, farby a komponenty súčasného webu s Bootstrap 4.1.3. Netreba nový CSS alebo JavaScript framework.

Pri zmene zloženia edícií aktualizujte tabuľku `#aplikacie-edicie` v oboch fragmentoch stránky Aplikácie. V administrácii otvorte `/admin/modules_allinfo.jsp` a porovnajte stĺpce podľa názvu edície. Export CSV obsahuje aj kľúč modulu, podľa ktorého spárujete slovenské a anglické názvy. Do prvej skupiny tabuľky patria všetky riadky s označením `X` v stĺpci Community. Na tento zoznam odkazujú [licenčné podmienky projektu](../../LICENSE.md).

## Vloženie do CMS

1. **Najprv nahrajte obrázky, až potom aktualizujte obsah.** Snímky z [sk/images](sk/images/) okrem `logo-*.png` patria do **`/images/webjetcms-2026/sk/`**, snímky z [en/images](en/images/) do **`/images/webjetcms-2026/en/`**. Šesť spoločných `logo-*.png` zostáva v **`/images/webjetcms-2026/`**. Pri nahrávaní zachovajte názvy z príslušného jazykového priečinka; EN snímky majú anglické názvy, napríklad `publication-history.png`. Zoznam potrebných a voliteľných obrázkov je v [manifeste](image-manifest.md). Obrázky musia byť dostupné na príslušnej SK aj EN doméne. **Pôvodné obrázky v koreňovom priečinku nemažte ani nepresúvajte:** už publikované SK stránky ich môžu používať. Aktualizované SK fragmenty už odkazujú na `/sk/`; EN používa `/en/` a spoločné logá z koreňa.
2. SK stránky Pre vývojárov a Pre agentúry už používateľ vytvoril a zaradil do podmenu. Na EN webe založte `/for-developers/` a `/for-agencies/` s existujúcou produktovou šablónou, ak ešte neexistujú. Pred vložením obsahu overte cieľové stránky a odkazy na príslušnej doméne.
3. V zdrojovom HTML režime vložte celý príslušný fragment vrátane `style` namiesto pôvodného obsahu článku. Ak štýly spravujete oddelene, vložte dodaný `style` podľa možností šablóny. Obal `.wj-website-content` musí zostať. **Zachovajte aj značky `!INCLUDE(...)!`**: videá spracuje aplikácia `/components/video/video_player.jsp`, vložený kód noviniek aplikácia `/components/app-htmlembed/embed.jsp`. Nevkladajte ich ako escapovaný text ani ich nenahrádzajte vykresleným HTML z lokálneho náhľadu.
4. **Na úvode ponechajte celú značku `!INCLUDE(/components/app-htmlembed/embed.jsp, html=...)!` s nezmeneným obsahom parametra `html`.** Obsahuje skript noviniek v Base64; po dekódovaní nasleduje prefix `utf8:` a URL-kódované HTML. Toto uloženie chráni medzery skriptu pred zmenami v redakčnom editore. Pri vkladaní ho nerozbaľujte na samostatný `script`. WebJET ho dekóduje pri vykreslení stránky. Nepotrebuje API kľúč, jQuery ani administračné rozhrania. Podrobnosti vrátane CSP sú v [news-feed.md](news-feed.md).
5. Nastavte titulky a meta popisy podľa [redakčných poznámok](editorial-notes.md). Používateľ už na SK webe vytvoril položku **Čo WebJET dokáže** s adresou `/co-webjet-dokaze/` a podmenu Benefity, Funkcie, Aplikácie, Pre vývojárov a Pre agentúry. Potvrdený stav aj pôvodné návrhy odlišujú [poznámky k navigácii](navigation-proposal.md). Presmerovanie rodičovskej stránky na Benefity bolo iba odporúčanie; jeho nasadenie nie je potvrdené. EN menu overujte samostatne.
6. V náhľade CMS skontrolujte obrázky, SK videá vrátane prehratia a celej obrazovky, CTA na `/kontakt/` alebo EN `/contact/`, rozbaľovacie sekcie Funkcií/Aplikácií a novinky. Rozbaľovanie používa natívne `details`, funguje aj bez JavaScriptu. SK videá majú aj priamy odkaz na YouTube; automatické prehrávanie je vypnuté. EN fragmenty namiesto videí zobrazujú zväčšiteľné screenshoty.

EN texty, popisy obrázkov aj odkazy do dokumentácie sú preložené. Technické ID sekcií zostávajú rovnaké ako v SK pre jednoduché párovanie. Screenshoty zobrazujú anglické ovládanie; pôvodné ukážkové názvy, časť obsahu a niektoré popisky v zdrojoch zostali slovenské. Dve úplne slovenské snímky v `docs/en/` boli nahradené vhodnejšími EN zábermi. Podrobnosti sú v [manifeste obrázkov](image-manifest.md).

EN úvod používa existujúci `wjnews.en.json`, dátumy vo formáte `en-GB` a anglickú dokumentáciu ako fallback. Ciele sociálnych odkazov a videá môžu zostať slovenské, na čo upozorňuje text sekcie. Dôvod použitia statických obrázkov namiesto automaticky prekladaných videí vysvetľuje [manifest videí](video-manifest.md#anglická-verzia-snímky-namiesto-videí).

Na úvode sú podľa dohody pôvodné licenčné karty nahradené odkazmi. Stránky Služby, Aktuálna verzia a Licencie sa nemenia. Spring Boot a spoločné plánovanie zmien viacerých stránok sú označené **Pripravujeme**, bez dátumu vydania.

## CSS a JavaScript šablóny

Overené CSS a JavaScript fragmenty pre produktové weby archivujeme v `docs/website/css/` a `docs/website/js/` pod názvom cieľového súboru, napríklad `css/shame.css` alebo `js/page.js`. Pri každom fragmente uvedieme miesto nasadenia; JavaScript súbor pridáme až vtedy, keď bude potrebný.

[css/shame.css](css/shame.css) obsahuje spoločnú úpravu výsuvného menu produktovej šablóny. Nasadzuje sa do `/templates/webjet_products/assets/css/shame.css`: nahrádza predchádzajúci blok úprav menu, ostatné pravidlá cieľového súboru treba zachovať. Podporuje hover a prechod klávesnicou na desktope, zalamovanie a posúvanie dlhého podmenu a viditeľné podpoložky v mobilnom menu. Produktové triedy na `body` určujú iba farbu fokusu. Funkčnosť nasadeného fragmentu potvrdil používateľ.

Tieto súbory sú archívom fragmentov; uloženie do repozitára ich automaticky nenasadzuje na web.

## Lokálny náhľad

Z koreňa repozitára spustite:

```sh
python3 docs/website/preview.py --port 8765
```

Pre EN obsah v anglickej verejnej šablóne spustite samostatne:

```sh
python3 docs/website/preview.py --lang en --port 8766
```

Otvorte [anglický náhľad](http://127.0.0.1:8766/); používa anglické cesty z tabuľky. Predvolený jazyk je `sk`, možno ho zadať aj cez `--lang sk`. Náhľad obsluhuje oba jazykové priečinky obrázkov aj staré SK cesty; každý jazyk zobrazuje vlastnú sadu stránok a vlastnú verejnú šablónu.

Otvorte [lokálny náhľad](http://127.0.0.1:8765/). Fungujú aj ostatné cesty z tabuľky. Server sa viaže len na `127.0.0.1`, nepotrebuje bežiaci WebJET ani inštaláciu Python balíkov. Vyžaduje internet na načítanie verejnej šablóny a jej existujúcich CSS/fontov. Po úprave fragmentu obnovte stránku; server ukončíte cez Ctrl+C.

Náhľad vkladá fragmenty do aktuálnej verejnej šablóny. Pôvodné skripty, formuláre a trackovanie šablóny sa v náhľade odstraňujú; pôvodná mobilná navigácia a kontaktné vyskakovacie okno preto nie sú funkčné. Kontroluje sa článok, nie tieto nezmenené časti. Podporované video značky server nahradí responzívnym prehrávačom YouTube. Značku HTML embed dekóduje podľa postupu `embed.jsp` (Base64, potom pri prefixe `utf8:` URL dekódovanie), takže skript noviniek môže vykonať prehliadač. Zmeny prebiehajú iba v odpovedi náhľadu; server nemení HTML súbory ani nespúšťa JSP či vložený JavaScript. Lokálny náhľad nenahrádza poslednú kontrolu po vložení do konkrétneho CMS editora.

## Podklady

- [Obsahová stratégia](content-strategy.md): publikum, piliere, úlohy stránok a potvrdené rozhodnutia.
- [Redakčné poznámky](editorial-notes.md): metadáta, navigácia a alternatívy nadpisov.
- [Navigácia](navigation-proposal.md): používateľom potvrdené SK zoskupenie Čo WebJET dokáže a jasne oddelený archív pôvodných návrhov.
- [Produktové dôkazy](product-evidence.md): dostupné funkcie, podmienky AI a pripravované publikačné balíky z PR #388.
- [Konkurencia](competitor-analysis.md): BUXUS 2.0, WordPress.org, WordPress.com a WordPress VIP s verejnými zdrojmi.
- [Vývojári a agentúry](developer-agency-evidence.md): Java/Spring, vlastné aplikácie, REST/headless a MultiWeb.
- [Aktuálny web](current-site-audit.md): technické podklady a obsahový audit.
- [Integrácia noviniek](news-feed.md): formát JSON, načítanie a náhradný obsah.
- [Videá z dokumentácie](video-manifest.md): použité videá, zdroje a spôsob vloženia cez CMS aplikáciu.
- [Kontrola výsledku](verification.md): vykonané overenia a ich rozsah.
