# Obsah webjetcms.sk

Balík pripravený 15. 11. 2026 podľa dohodnutého zamerania na firemné weby, verejnú správu, AI, schvaľovanie a plánovanie obsahu. K tematickým sekciám sú doplnené videá z dokumentácie. Na verejnom webe nebolo nič publikované.

## Výsledné HTML

| Stránka | Súbor na vloženie | Verejná cesta |
| --- | --- | --- |
| Úvod vrátane živých noviniek | [index.html](sk/index.html) | `/` (existujúca stránka Domov) |
| Benefity | [benefity.html](sk/benefity.html) | `/benefity/` |
| Funkcie | [funkcie.html](sk/funkcie.html) | `/funkcie/` |
| Aplikácie | [aplikacie.html](sk/aplikacie.html) | `/aplikacie/` |
| Pre vývojárov | [pre-vyvojarov.html](sk/pre-vyvojarov.html) | `/pre-vyvojarov/` – nová stránka |
| Pre agentúry | [pre-agentury.html](sk/pre-agentury.html) | `/pre-agentury/` – nová stránka |

Každý súbor obsahuje **celý obsah na vloženie dovnútra existujúceho `article.ly-article`**. Neobsahuje ďalší `article` ani obal celej HTML stránky. V úvode súboru je malý `style` obmedzený na `.wj-website-content`; nadväzuje na typografiu, farby a komponenty súčasného webu s Bootstrap 4.1.3. Netreba nový CSS alebo JavaScript framework.

## Vloženie do CMS

1. Nahrajte používané obrázky z [sk/images](sk/images/) do **`/images/webjetcms-2026/`** na webe. Zachovajte názvy súborov. [Manifest](image-manifest.md) odlišuje deväť potrebných súborov od šiestich voliteľných snímok nahradených videami; možno nahrať aj celý priečinok.
2. Založte stránky `/pre-vyvojarov/` a `/pre-agentury/` s existujúcou produktovou šablónou. Tým začnú fungovať aj nové odkazy z ostatných stránok.
3. V zdrojovom HTML režime vložte celý príslušný fragment vrátane `style` namiesto pôvodného obsahu článku. Ak štýly spravujete oddelene, vložte dodaný `style` podľa možností šablóny. Obal `.wj-website-content` musí zostať. **Zachovajte aj značky `!INCLUDE(...)!`**: WebJET ich spracuje aplikáciou `/components/video/video_player.jsp`. Nevkladajte ich ako escapovaný text ani ich nenahrádzajte iframe z lokálneho náhľadu.
4. **Na úvode ponechajte aj koncový `script`.** Načítava novinky z verejného JSON. Ak ho starší editor odstráni, vložte rovnaký skript do poľa pre skripty danej stránky alebo za článok v šablóne. Nepotrebuje API kľúč, jQuery ani administračné rozhrania. Podrobnosti vrátane CSP sú v [news-feed.md](news-feed.md).
5. Nastavte titulky a meta popisy podľa [redakčných poznámok](editorial-notes.md). Podľa dohody ponechajte stránky Pre agentúry a Pre vývojárov zatiaľ mimo horného menu; už sú prelinkované v obsahu. Pre neskoršiu úpravu šablóny je pripravený [návrh preskupenia navigácie](navigation-proposal.md).
6. V náhľade CMS skontrolujte načítanie obrázkov, videá vrátane prehratia a celej obrazovky, CTA na `/kontakt/`, rozbaľovacie sekcie Funkcií/Aplikácií a šesť položiek noviniek. Rozbaľovanie používa natívne `details`, funguje aj bez JavaScriptu. Pri videách zostáva aj priamy odkaz na YouTube; automatické prehrávanie je vypnuté.

Na úvode sú podľa dohody pôvodné licenčné karty nahradené odkazmi. Stránky Služby, Aktuálna verzia a Licencie sa nemenia. Spring Boot a spoločné plánovanie zmien viacerých stránok sú označené **Pripravujeme**, bez dátumu vydania.

## Lokálny náhľad

Z koreňa repozitára spustite:

```sh
python3 docs/website/preview.py --port 8765
```

Otvorte [lokálny náhľad](http://127.0.0.1:8765/). Fungujú aj ostatné cesty z tabuľky. Server sa viaže len na `127.0.0.1`, nepotrebuje bežiaci WebJET ani inštaláciu Python balíkov. Vyžaduje internet na načítanie verejnej šablóny a jej existujúcich CSS/fontov. Po úprave fragmentu obnovte stránku; server ukončíte cez Ctrl+C.

Náhľad vkladá fragmenty do aktuálnej verejnej šablóny. Pôvodné skripty, formuláre a trackovanie šablóny sa v náhľade odstraňujú; pôvodná mobilná navigácia a kontaktné vyskakovacie okno preto nie sú funkčné. Kontroluje sa článok, nie tieto nezmenené časti. Skript živých noviniek zostáva aktívny. Podporované video značky server nahradí responzívnym prehrávačom YouTube iba v odpovedi náhľadu; HTML súbory nemení a JSP nespúšťa. Lokálny náhľad nenahrádza poslednú kontrolu po vložení do konkrétneho CMS editora.

## Podklady

- [Obsahová stratégia](content-strategy.md): publikum, piliere, úlohy stránok a potvrdené rozhodnutia.
- [Redakčné poznámky](editorial-notes.md): metadáta, navigácia a alternatívy nadpisov.
- [Návrh navigácie](navigation-proposal.md): budúce preskupenie menu; aktuálne zostávajú nové stránky mimo horného menu.
- [Produktové dôkazy](product-evidence.md): dostupné funkcie, podmienky AI a pripravované publikačné balíky z PR #388.
- [Konkurencia](competitor-analysis.md): BUXUS 2.0, WordPress.org, WordPress.com a WordPress VIP s verejnými zdrojmi.
- [Vývojári a agentúry](developer-agency-evidence.md): Java/Spring, vlastné aplikácie, REST/headless a MultiWeb.
- [Aktuálny web](current-site-audit.md): technické podklady a obsahový audit.
- [Integrácia noviniek](news-feed.md): formát JSON, načítanie a náhradný obsah.
- [Videá z dokumentácie](video-manifest.md): použité videá, zdroje a spôsob vloženia cez CMS aplikáciu.
- [Kontrola výsledku](verification.md): vykonané overenia a ich rozsah.
