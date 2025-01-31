# Export dokumentácie do formátu PDF

V tejto časti si rozoberieme, ako exportovať docsify dokumentáciu do PDF

pomocou nástroja **Docsify-to-PDF**.

Tento nástroj umožňuje jednoduchý export dokumentácie vytvorenej pomocou Docsify do formátu PDF, čím uľahčuje zdieľanie a distribúciu.

## Inštalácia

Inštaláciu vykonáme príkazom:

```bash
npm install --save-dev docsify-to-pdf
```

## Použitie ako CLI:

### Konfiguračný súbor:

Je potrebné vytvoriť konfiguračný súbor
 `.docsifytopdfrc.<js|json|yaml>`
alebo "docsifytopdf" pole v `package.json` (ako rcfile) s týmto nastavením:

### Príklad obsahu `.docsifytopdfrc.js`:

```js
module.exports = {
  contents: [ "docs/_sidebar.md" ], // cesta k súboru _sidebar.md, kde je definovaný obsah menu
  pathToPublic: "pdf/readme.pdf", // cesta a názov PDF súboru po exportovaní
  pdfOptions: "<options for puppeteer.pdf()>", // možnosti pre puppeteer
  removeTemp: true, // automatické vymazanie pomocného súboru, ktoré sú použité pre export PDF
  emulateMedia: "screen", // typ exportovaného média (screen, print, handheld)
}
```

### Pridanie skriptu do `package.json`:
Na to, aby sme mohli spúšťať konvertor pomocou príkazu `npm run convert`, musíme pridať skript do `package.json`:

```json
{
  "scripts": {
    "convert": "docsify-to-pdf"
  }
}
```

### Spustenie konvertora:
Konvertor spustíme pomocou príkazu:

```bash
npm run convert
```

Po úspešnom exportovaní vidíme výstup SUCCESS spolu s cestou k exportovanému PDF.

### Spustenie pomocou skriptu convert-to-pdf.js

V priečinku `/docs` sa nachádza skript `convert-to-pdf.js`.
V tomto priečinku spustením príkazu
```bash
npm run pdf
```
sa spustí skript, ktorý zabezpečí automatizáciu konverzie dokumentácie založenej na **Docsify** do štruktúrovaného **PDF** tým, že modifikuje a reorganizuje súbory, generuje zoznam obsahu a vykonáva samotnú konverziu. Po dokončení tiež vykoná čistiace operácie, aby obnovil súborový systém do jeho pôvodného stavu. Skript vygeneruje dokumentáciu pre časti: `Redaktor`, `Správca`, `Prevádzka`, `Inštalácia`, `Používanie administrácie a datatabuľky`, `Web stránky a Súbory`, `Aplikácie` v jazykoch : `sk`, `en` a `cs`.

Je potrebné počkať na dokončenie behu skriptu, inak sa neobnoví súborový systém do pôvodného stavu a zostanú modifikované a reorganizované súbory v štruktúre.

Odporúčame používať tento skript namiesto `npm run convert`.


## Riešenie chýb

Pri práci s nástrojom **Docsify-to-PDF** môžeme naraziť na niektoré chyby:

### Chyba: `anchors processing error`
Toto upozornenie nastáva pri generovaní zoznamu obsahu na začiatku alebo iného prepojenia v dokumentácii.
Je spôsobená nespárovaním odkazu na kotvu v dokumente. Najčastejšou príčinou je iný nadpis v dokumentácii (v `README.md` ) než je v súbore na definovanie postranného menu (`_sidebar.md`).
Tento problém je možné vyriešiť prepísaním nadpisu alebo vložením HTML kotvy na miesto odkazovania:
```html
 <a id="sekcia---platnosť"><a>`
```
pre chybu:

```javascript
WARNING:
anchors processing errors
[
  {
    "processingAnchor": "sekcia---platnosť",
    "error": "Cannot set property 'id' of null",
    "stack": "TypeError: Cannot set property 'id' of null\n
  }
]
```

## Možnosti pre puppeteer:

V konfiguračnom súbore `.docsifytopdfrc.<js|json|yaml>` vieme nastaviť rôzne možnosti pre puppeteer.


### Vlastnosti PDF

- `displayHeaderFooter` (voliteľné/`boolean`) - Či sa má zobraziť hlavička a pätička. (`false`)
- `footerTemplate` (voliteľné/`string`) - HTML šablóna pre tlačovú pätičku. Má rovnaké obmedzenia a podporu špeciálnych tried ako `PDFOptions.headerTemplate`. (`-`)
- `format` (voliteľné/`PaperFormat`) - Ak je nastavené, má to prioritu pred možnosťami šírky a výšky. (`letter`)
- `headerTemplate` (voliteľné/`string`) - HTML šablóna pre tlačovú hlavičku. Mala by byť platným HTML s nasledujúcimi triedami:
  - `date` formátovaný dátum tlače
  - `title` názov dokumentu
  - `url` umiestnenie dokumentu
  - `pageNumber` aktuálne číslo strany
  - `totalPages` celkový počet strán v dokumente (`-`)
- `height` (voliteľné/`string | number`) - Nastaví výšku papiera. Môžete zadať číslo alebo reťazec s jednotkou. (`-`)
- `landscape` (voliteľné/`boolean`) - Či sa má tlačiť v orientácii na šírku. (`false`)
- `margin` (voliteľné/`PDFMargin`) - Nastaví okraje PDF. (`undefined`)
- `omitBackground` (voliteľné/`boolean`) - Skryje predvolené biele pozadie a umožní generovať PDF s priehľadnosťou. (`false`)
- `outline` (voliteľné/`boolean`) - (Experimentálne) Generuje osnovu dokumentu. (`false`)
- `pageRanges` (voliteľné/`string`) - Rozsahy strán na tlač, napr. 1-5, 8, 11-13. Prázdny reťazec, čo znamená, že sa tlačia všetky strany. (`"" (všetky strany)`)
- `path` (voliteľné/`string`) - Cesta, kam uložiť súbor. (`undefined`)
- `preferCSSPageSize` (voliteľné/`boolean`) - Dáva prednosť veľkosti stránky uvedenej v CSS @page pred šírkou, výškou alebo formátom. (`false`)
- `printBackground` (voliteľné/`boolean`) - Nastavte na `true`, aby sa tlačili grafické prvky pozadia. (`false`)
- `scale` (voliteľné/`number`) - Mierka zobrazenia webovej stránky. Hodnota musí byť medzi 0.1 a 2. (`1`)
- `tagged` (voliteľné/`boolean`) - (Experimentálne) Generuje označené (prístupné) PDF. (`true`)
- `timeout` (voliteľné/`number`) - Časový limit v milisekundách. Pre vypnutie časového limitu nastavte 0. (`30_000`)
- `waitForFonts` (voliteľné/`boolean`) - Ak je `true`, čaká na `document.fonts.ready`. (`true`)
- `width` (voliteľné/`string | number`) - Nastaví šírku papiera. Môžete zadať číslo alebo reťazec s jednotkou. (`-`)