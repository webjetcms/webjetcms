# Export documentation to PDF format

In this section, we will discuss how to export docsify documentation to PDF.

using the **Docsify-to-PDF** tool.

This tool allows easy export of documentation created with Docsify to PDF format, making it easier to share and distribute.

## Installation

We will perform the installation with the command:

```bash
npm install --save-dev docsify-to-pdf
```

## Using as CLI:

### Configuration file:

You need to create a configuration file
 `.docsifytopdfrc.<js|json|yaml>`
or "docsifytopdf" field in `package.json` (as rcfile) with this setting:

### Example content `.docsifytopdfrc.js`:

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

To be able to run the converter using the command `npm run convert`, we need to add a script to `package.json`:

```json
{
  "scripts": {
    "convert": "docsify-to-pdf"
  }
}
```

### Starting the converter:

We start the converter using the command:

```bash
npm run convert
```

After successful export, we see the output SUCCESS along with the path to the exported PDF.

### Running with the convert-to-pdf.js script

The script `convert-to-pdf.js` is located in the folder `/docs`.
In this folder, run the command
```bash
npm run pdf
```
will run a script that automates the conversion of **Docsify** -based documentation into structured **PDF** by modifying and reorganizing files, generating a table of contents, and performing the conversion itself. Once complete, it will also perform cleanup operations to restore the file system to its original state. The script will generate documentation for sections: `Redaktor`, `Správca`, `Prevádzka`, `Inštalácia`, `Používanie administrácie a datatabuľky`, `Web stránky a Súbory`, `Aplikácie` in the languages: `sk`, `en`, and `cs`.

It is necessary to wait for the script to complete execution, otherwise the file system will not be restored to its original state and the modified and reorganized files will remain in the structure.

We recommend using this script instead of `npm run convert`.


## Troubleshooting

When working with the **Docsify-to-PDF** tool, we may encounter some errors:

### Error: `anchors processing error`

This warning occurs when generating a table of contents at the beginning or other link in the documentation.
It is caused by a mismatch between the anchor reference in the document. The most common cause is a different title in the documentation (in `README.md` ) than in the side menu definition file (`_sidebar.md`).
This problem can be solved by rewriting the title or inserting an HTML anchor in place of the link:
```html
 <a id="sekcia---platnosť"><a>`
```
for error:

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

## Options for the puppeteer:

In the configuration file `.docsifytopdfrc.<js|json|yaml>` we can set various options for puppeteer.


### PDF properties

- `displayHeaderFooter` (optional/`boolean`) - Whether to display header and footer. (`false`)
- `footerTemplate` (optional/`string`) - HTML template for the print footer. It has the same restrictions and special class support as `PDFOptions.headerTemplate`. (`-`)
- `format` (optional/`PaperFormat`) - If set, it takes precedence over the width and height options. (`letter`)
- `headerTemplate` (optional/`string`) - HTML template for the print header. It should be valid HTML with the following classes:
  - `date` formatted print date
  - `title` document name
  - `url` document location
  - `pageNumber` current page number
  - `totalPages` total number of pages in the document (`-`)
- `height` (optional/`string | number`) - Sets the paper height. You can enter a number or a string with a unit. (`-`)
- `landscape` (optional/`boolean`) - Whether to print in landscape orientation. (`false`)
- `margin` (optional/`PDFMargin`) - Sets the PDF margins. (`undefined`)
- `omitBackground` (optional/`boolean`) - Hides the default white background and allows generating PDFs with transparency. (`false`)
- `outline` (optional/`boolean`) - (Experimental) Generates a document outline. (`false`)
- `pageRanges` (optional/`string`) - Ranges of pages to print, e.g. 1-5, 8, 11-13. Empty string, meaning all pages are printed. (`"" (všetky strany)`)
- `path` (optional/`string`) - Path to save the file. (`undefined`)
- `preferCSSPageSize` (optional/`boolean`) - Prefers the page size specified in the CSS @page over width, height, or format. (`false`)
- `printBackground` (optional/`boolean`) - Set to `true` to print background graphics. (`false`)
- `scale` (optional/`number`) - Web page display scale. The value must be between 0.1 and 2. (`1`)
- `tagged` (optional/`boolean`) - (Experimental) Generates an annotated (accessible) PDF. (`true`)
- `timeout` (optional/`number`) - Timeout in milliseconds. To disable the timeout, set it to 0. (`30_000`)
- `waitForFonts` (optional/`boolean`) - If `true`, waits for `document.fonts.ready`. (`true`)
- `width` (optional/`string | number`) - Sets the paper width. You can enter a number or a string with a unit. (`-`)