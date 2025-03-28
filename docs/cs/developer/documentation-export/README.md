# Export dokumentace do formátu PDF

V této části si rozebereme, jak exportovat docsify dokumentaci do PDF

pomocí nástroje **Docsify-to-PDF**.

Tento nástroj umožňuje snadný export dokumentace vytvořené pomocí Docsify do formátu PDF, čímž usnadňuje sdílení a distribuci.

## Instalace

Instalaci provedeme příkazem:

```bash
npm install --save-dev docsify-to-pdf
```

## Použití jako CLI:

### Konfigurační soubor:

Je třeba vytvořit konfigurační soubor `.docsifytopdfrc.<js|json|yaml>` nebo "docsifytopdf" pole v `package.json` (jako rcfile) s tímto nastavením:

### Příklad obsahu `.docsifytopdfrc.js`:

```js
module.exports = {
  contents: [ "docs/_sidebar.md" ], // cesta k súboru _sidebar.md, kde je definovaný obsah menu
  pathToPublic: "pdf/readme.pdf", // cesta a názov PDF súboru po exportovaní
  pdfOptions: "<options for puppeteer.pdf()>", // možnosti pre puppeteer
  removeTemp: true, // automatické vymazanie pomocného súboru, ktoré sú použité pre export PDF
  emulateMedia: "screen", // typ exportovaného média (screen, print, handheld)
}
```

### Přidání skriptu do `package.json`:

K tomu, abychom mohli spouštět konvertor pomocí příkazu `npm run convert`, musíme přidat skript do `package.json`:

```json
{
  "scripts": {
    "convert": "docsify-to-pdf"
  }
}
```

### Spuštění konvertoru:

Konvertor spustíme pomocí příkazu:

```bash
npm run convert
```

Po úspěšném exportování vidíme výstup SUCCESS spolu s cestou k exportovanému PDF.

### Spuštění pomocí skriptu convert-to-pdf.js

Ve složce `/docs` se nachází skript `convert-to-pdf.js`. V této složce spuštěním příkazu

```bash
npm run pdf
```

se spustí skript, který zajistí automatizaci konverze dokumentace založené na **Docsify** do strukturovaného **PDF** tím, že modifikuje a reorganizuje soubory, generuje seznam obsahu a provádí samotnou konverzi. Po dokončení také provede čistící operace, aby obnovil souborový systém do jeho původního stavu. Skript vygeneruje dokumentaci pro části: `Redaktor`, `Správca`, `Prevádzka`, `Inštalácia`, `Používanie administrácie a datatabuľky`, `Web stránky a Súbory`, `Aplikácie` v jazycích : `sk`, `en` a `cs`.

Je třeba počkat na dokončení běhu skriptu, jinak se neobnoví souborový systém do původního stavu a zůstanou modifikované a reorganizované soubory ve struktuře.

Doporučujeme používat tento skript namísto `npm run convert`.

## Řešení chyb

Při práci s nástrojem **Docsify-to-PDF** můžeme narazit na některé chyby:

### Chyba: `anchors processing error`

Toto upozornění nastává při generování seznamu obsahu na začátku nebo jiného propojení v dokumentaci. Je způsobena nespárováním odkazu na kotvu v dokumentu. Nejčastější příčinou je jiný nadpis v dokumentaci (v `README.md` ) než je v souboru pro definování postranního menu (`_sidebar.md`). Tento problém lze vyřešit přepsáním nadpisu nebo vložením HTML kotvy na místo odkazování:

```html
 <a id="sekcia---platnosť"><a>`
```

pro chybu:

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

## Možnosti pro puppeteer:

V konfiguračním souboru `.docsifytopdfrc.<js|json|yaml>` umíme nastavit různé možnosti pro puppeteer.

### Vlastnosti PDF

- `displayHeaderFooter` (volitelné/`boolean`) - Zda se má zobrazit hlavička a patička. (`false`)
- `footerTemplate` (volitelné/`string`) - HTML šablona pro tiskovou patičku. Má stejná omezení a podporu speciálních tříd jako `PDFOptions.headerTemplate`. (`-`)
- `format` (volitelné/`PaperFormat`) - Je-li nastaveno, má to prioritu před možnostmi šířky a výšky. (`letter`)
- `headerTemplate` (volitelné/`string`) - HTML šablona pro tiskovou hlavičku. Měla by být platným HTML s následujícími třídami:
  - `date` formátované datum tisku
  - `title` název dokumentu
  - `url` umístění dokumentu
  - `pageNumber` aktuální číslo strany
  - `totalPages` celkový počet stran v dokumentu (`-`)
- `height` (volitelné/`string | number`) - Nastaví výšku papíru. Můžete zadat číslo nebo řetězec s jednotkou. (`-`)
- `landscape` (volitelné/`boolean`) - Zda se má tisknout v orientaci na šířku. (`false`)
- `margin` (volitelné/`PDFMargin`) - Nastaví okraje PDF. (`undefined`)
- `omitBackground` (volitelné/`boolean`) - Skryje výchozí bílé pozadí a umožní generovat PDF s průhledností. (`false`)
- `outline` (volitelné/`boolean`) - (Experimentálně) Generuje osnovu dokumentu. (`false`)
- `pageRanges` (volitelné/`string`) - Rozsahy stran pro tisk, například. 1–5, 8, 11–13. Prázdný řetězec, což znamená, že se tisknou všechny strany. (`"" (všetky strany)`)
- `path` (volitelné/`string`) - Cesta, kam uložit soubor. (`undefined`)
- `preferCSSPageSize` (volitelné/`boolean`) - Dává přednost velikosti stránky uvedené v CSS @page před šířkou, výškou nebo formátem. (`false`)
- `printBackground` (volitelné/`boolean`) - Nastavte na `true`, aby se tiskly grafické prvky pozadí. (`false`)
- `scale` (volitelné/`number`) - Měřítko zobrazení webové stránky. Hodnota musí být mezi 0.1 a 2. (`1`)
- `tagged` (volitelné/`boolean`) - (Experimentální) Generuje označené (přístupné) PDF. (`true`)
- `timeout` (volitelné/`number`) - Časový limit v milisekundách. Pro vypnutí časového limitu nastavte 0. (`30_000`)
- `waitForFonts` (volitelné/`boolean`) - Je-li `true`, čeká na `document.fonts.ready`. (`true`)
- `width` (volitelné/`string | number`) - Nastaví šířku papíru. Můžete zadat číslo nebo řetězec s jednotkou. (`-`)
