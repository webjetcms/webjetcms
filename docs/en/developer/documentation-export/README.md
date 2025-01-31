# Export documentation to PDF format

In this section we will discuss how to export docsify documentation to PDF

using the tool **Docsify-to-PDF**.

This tool makes it easy to export documentation created with Docsify to PDF format, making it easy to share and distribute.

## Installation

The installation is performed by the command:

```bash
npm install --save-dev docsify-to-pdf
```

## Use as CLI:

### Configuration file:

It is necessary to create a configuration file `.docsifytopdfrc.<js|json|yaml>` or "docsifytopdf" field in `package.json` (as rcfile) with this setting:

### Example of content `.docsifytopdfrc.js`:

```js
module.exports = {
  contents: [ "docs/_sidebar.md" ], // cesta k súboru _sidebar.md, kde je definovaný obsah menu
  pathToPublic: "pdf/readme.pdf", // cesta a názov PDF súboru po exportovaní
  pdfOptions: "<options for puppeteer.pdf()>", // možnosti pre puppeteer
  removeTemp: true, // automatické vymazanie pomocného súboru, ktoré sú použité pre export PDF
  emulateMedia: "screen", // typ exportovaného média (screen, print, handheld)
}
```

### Adding a script to `package.json`:

In order to run the converter using the command `npm run convert`, we need to add the script to `package.json`:

```json
{
  "scripts": {
    "convert": "docsify-to-pdf"
  }
}
```

### Starting the converter:

Run the converter using the command:

```bash
npm run convert
```

After a successful export, we see the SUCCESS output along with the path to the exported PDF.

### Run with convert-to-pdf.js script

In the folder `/docs` script is located `convert-to-pdf.js`. In this folder, by running the command

```bash
npm run pdf
```

a script is run to automate the conversion of documentation based on **Docsify** into a structured **PDF** by modifying and reorganizing files, generating a table of contents, and performing the conversion itself. Once complete, it also performs cleanup operations to restore the file system to its original state. The script will generate documentation for the parts: `Redaktor`, `Správca`, `Prevádzka`, `Inštalácia`, `Používanie administrácie a datatabuľky`, `Web stránky a Súbory`, `Aplikácie` in languages : `sk`, `en` a `cs`.

It is necessary to wait for the script to finish running, otherwise the file system will not be restored to its original state and the modified and reorganized files will remain in the structure.

We recommend using this script instead of `npm run convert`.

## Troubleshooting errors

When working with the tool **Docsify-to-PDF** we may come across some errors:

### Error: `anchors processing error`

This warning occurs when generating a table of contents at the beginning or other link in the documentation. It is caused by a failure to pair a link to an anchor in the document. The most common cause is a different heading in the documentation (in `README.md` ) than is in the side menu definition file (`_sidebar.md`). This problem can be solved by rewriting the title or inserting an HTML anchor in place of the link:

```html
 <a id="sekcia---platnosť"><a>`
```

for the error:

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

## Options for puppeteer:

In the configuration file `.docsifytopdfrc.<js|json|yaml>` we can set different options for puppeteer.

### PDF Features

- `displayHeaderFooter` (optional/`boolean`) - Whether to display the header and footer. (`false`)
- `footerTemplate` (optional/`string`) - HTML template for print footer. It has the same restrictions and support for special classes as `PDFOptions.headerTemplate`. (`-`)
- `format` (optional/`PaperFormat`) - If set, this takes priority over the width and height options. (`letter`)
- `headerTemplate` (optional/`string`) - HTML template for the print header. It should be valid HTML with the following classes:
  - `date` formatted print date
  - `title` the title of the document
  - `url` document location
  - `pageNumber` current page number
  - `totalPages` the total number of pages in the document (`-`)
- `height` (optional/`string | number`) - Sets the paper height. You can specify a number or a string with one. (`-`)
- `landscape` (optional/`boolean`) - Whether to print in landscape orientation. (`false`)
- `margin` (optional/`PDFMargin`) - Sets the margins of the PDF. (`undefined`)
- `omitBackground` (optional/`boolean`) - Hides the default white background and allows you to generate PDFs with transparency. (`false`)
- `outline` (optional/`boolean`) - (Experimental) Generates a document outline. (`false`)
- `pageRanges` (optional/`string`) - Page ranges for printing, e.g. 1-5, 8, 11-13. Empty string, which means that all pages are printed. (`"" (všetky strany)`)
- `path` (optional/`string`) - The path where to save the file. (`undefined`)
- `preferCSSPageSize` (optional/`boolean`) - Gives preference to the page size specified in CSS @page over width, height, or format. (`false`)
- `printBackground` (optional/`boolean`) - Set to `true` to push the background graphic elements. (`false`)
- `scale` (optional/`number`) - Scale of the web page display. The value must be between 0.1 and 2. (`1`)
- `tagged` (optional/`boolean`) - (Experimental) Generates a tagged (accessible) PDF. (`true`)
- `timeout` (optional/`number`) - Timeout in milliseconds. To disable the timeout, set to 0. (`30_000`)
- `waitForFonts` (optional/`boolean`) - If `true`, waiting for `document.fonts.ready`. (`true`)
- `width` (optional/`string | number`) - Sets the paper width. You can specify a number or a string with one. (`-`)
