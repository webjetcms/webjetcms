# Export dokumentace do formátu PDF

V této části se budeme zabývat tím, jak exportovat dokumentaci docsify do PDF.

použití nástroje **Převod dokumentů do formátu PDF**.

Tento nástroj umožňuje snadno exportovat dokumentaci vytvořenou pomocí aplikace Docsify do formátu PDF, což usnadňuje její sdílení a distribuci.

## Instalace

Instalace se provádí příkazem:

```bash
npm install --save-dev docsify-to-pdf
```

## Použití jako CLI:

### Konfigurační soubor:

Je nutné vytvořit konfigurační soubor `.docsifytopdfrc.<js|json|yaml>` nebo "docsifytopdf" v poli `package.json` (jako rcfile) s tímto nastavením:

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

Chcete-li spustit převodník pomocí příkazu `npm run convert`, musíme přidat skript do `package.json`:

```json
{
  "scripts": {
    "convert": "docsify-to-pdf"
  }
}
```

### Spuštění převodníku:

Spusťte převodník pomocí příkazu:

```bash
npm run convert
```

Po úspěšném exportu se zobrazí výstup SUCCESS spolu s cestou k exportovanému souboru PDF.

### Spuštění pomocí skriptu convert-to-pdf.js

Ve složce `/docs` skript je umístěn `convert-to-pdf.js`. V této složce spustíte příkaz

```bash
npm run pdf
```

je spuštěn skript, který automatizuje převod dokumentace na základě **Docsify** do strukturovaného **PDF** úpravou a reorganizací souborů, generováním obsahu a samotným převodem. Po dokončení také provede operace čištění, aby obnovil systém souborů do původního stavu. Skript vygeneruje dokumentaci k jednotlivým částem: `Redaktor`, `Správca`, `Prevádzka`, `Inštalácia`, `Používanie administrácie a datatabuľky`, `Web stránky a Súbory`, `Aplikácie` v jazycích : `sk`, `en` a `cs`.

Je nutné počkat na dokončení skriptu, jinak nebude souborový systém obnoven do původního stavu a změněné a reorganizované soubory zůstanou ve struktuře.

Doporučujeme použít tento skript místo `npm run convert`.

## Řešení chyb

Při práci s nástrojem **Převod dokumentů do formátu PDF** můžeme narazit na chyby:

### Chyba: `anchors processing error`

Toto upozornění se objevuje při generování obsahu na začátku nebo jiného odkazu v dokumentaci. Je způsobeno tím, že se nepodařilo spárovat odkaz s kotvou v dokumentu. Nejčastější příčinou je odlišný nadpis v dokumentaci (v případě `README.md` ) než v souboru s definicí postranní nabídky (`_sidebar.md`). Tento problém lze vyřešit přepsáním nadpisu nebo vložením kotvy HTML na místo odkazu:

```html
 <a id="sekcia---platnosť"><a>`
```

za chybu:

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

## Možnosti pro loutkáře:

V konfiguračním souboru `.docsifytopdfrc.<js|json|yaml>` můžeme nastavit různé možnosti pro loutkáře.

### Funkce PDF

- `displayHeaderFooter` (nepovinné/`boolean`) - Zda se má zobrazit záhlaví a zápatí. (`false`)
- `footerTemplate` (nepovinné/`string`) - Šablona HTML pro tisk zápatí. Má stejná omezení a podporu speciálních tříd jako šablona `PDFOptions.headerTemplate`. (`-`)
- `format` (nepovinné/`PaperFormat`) - Pokud je nastavena, má přednost před možnostmi šířky a výšky. (`letter`)
- `headerTemplate` (nepovinné/`string`) - Šablona HTML pro tiskovou hlavičku. Mělo by to být platné HTML s následujícími třídami:
  - `date` formátované datum tisku
  - `title` název dokumentu
  - `url` umístění dokumentu
  - `pageNumber` aktuální číslo stránky
  - `totalPages` celkový počet stránek dokumentu (`-`)
- `height` (nepovinné/`string | number`) - Nastaví výšku papíru. Můžete zadat číslo nebo řetězec s jedničkou. (`-`)
- `landscape` (nepovinné/`boolean`) - Zda se má tisknout v orientaci na šířku. (`false`)
- `margin` (nepovinné/`PDFMargin`) - Nastaví okraje PDF souboru. (`undefined`)
- `omitBackground` (nepovinné/`boolean`) - Skryje výchozí bílé pozadí a umožní vám vytvářet soubory PDF s průhledností. (`false`)
- `outline` (nepovinné/`boolean`) - (Experimentální) Generuje osnovu dokumentu. (`false`)
- `pageRanges` (nepovinné/`string`) - Rozsahy stránek pro tisk, např. 1-5, 8, 11-13. Prázdný řetězec, což znamená, že se vytisknou všechny stránky. (`"" (všetky strany)`)
- `path` (nepovinné/`string`) - Cesta, kam se má soubor uložit. (`undefined`)
- `preferCSSPageSize` (nepovinné/`boolean`) - Upřednostní velikost stránky zadanou v CSS @page před šířkou, výškou nebo formátem. (`false`)
- `printBackground` (nepovinné/`boolean`) - Nastavte na `true` posunout grafické prvky na pozadí. (`false`)
- `scale` (nepovinné/`number`) - Měřítko zobrazení webové stránky. Hodnota musí být mezi 0,1 a 2. (`1`)
- `tagged` (nepovinné/`boolean`) - (Experimentální) Vytvoří tagované (přístupné) PDF. (`true`)
- `timeout` (nepovinné/`number`) - Časový limit v milisekundách. Chcete-li časový limit vypnout, nastavte hodnotu 0. (`30_000`)
- `waitForFonts` (nepovinné/`boolean`) - Pokud `true`, čeká na `document.fonts.ready`. (`true`)
- `width` (nepovinné/`string | number`) - Nastaví šířku papíru. Můžete zadat číslo nebo řetězec s jedničkou. (`-`)
