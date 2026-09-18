# Automatizovane testovanie

[Presunute do dokumentacie](../../../docs/testing/README.md)

## Spustenie testovania

[Spustenie](https://codecept.io/commands/) všetkých testov:

```sh
cd src/test/webapp/
npx codeceptjs run --steps

#Spustenie konkrétneho testu:
npx codeceptjs run tests/components/gallery_test.js --steps

# Spustenie konkrétneho scenára
# - do mena scenára pridajte text @current a spustite
npx codeceptjs run --steps --grep "@current"

#zapnutie Pause On Fail - zapauzovanie ked nastane chyba
npx codeceptjs run --steps -p pauseOnFail --grep "@current"

#spustenie screenshotera pre manual
npx codeceptjs run --override '{ "tests": "./screenshots/generator/**/*.js"}' --steps
```

## Optimize AppStore screenshots

After generating screenshots for all required languages, run the standalone optimizer from this directory:

```sh
npm run scr:optimize -- --dry-run
npm run scr:optimize
npm run scr:optimize -- --quality 85
npm run scr:optimize -- --max-width 900
npm run scr:optimize -- --dry-run --max-width 900 --quality 85
```

The script recursively scans `src/main/webapp/components` and `src/main/webapp/apps` for
`screenshot*.jpg` and `screenshot*.jpeg` (case-insensitive). Paths are resolved relative to
the script, so it can also be called directly from another working directory.

CodeceptJS screenshot helpers can write PNG content even when the filename ends in `.jpg`.
The optimizer detects the PNG signature and converts these mismatched files to real JPEG.
It also resizes oversized screenshots, including existing JPEG files, to a maximum width of
760 pixels by default. The aspect ratio is preserved without cropping, with the height rounded
to whole pixels; smaller images are never enlarged. Filenames stay unchanged. JPEG encoding
uses quality 90, 4:4:4 chroma and mozjpeg by default. `--quality` accepts an integer from 1 to 100,
and `--max-width` accepts a positive integer in pixels. EXIF orientation is applied before
resizing; image metadata is not copied to the JPEG output.

JPEG files already within the width limit, actual `.png` files and other names/formats are left unchanged.
Animated PNGs, images with transparent pixels and conversions that would not reduce the file
size are skipped. An opaque alpha channel does not prevent conversion. The original file is
replaced via a temporary file only after encoding succeeds; conversion errors leave it intact.

`--dry-run` performs the encoding and reports the expected savings without writing anything.
The output lists converted/skipped files, original and resulting dimensions, the number of
resized images, errors and total savings in decimal MB. A run with
errors returns a nonzero exit code after processing the remaining files. Repeated runs do not
recompress JPEGs that already fit the requested width. To compare different widths or qualities
without accumulating quality loss, regenerate screenshots or restore their originals before
each conversion. Run optimization after screenshot generation has finished.

The optimizer is independent of screenshot generation and the Ant build. It requires the
project Node version and the development dependencies installed with npm.

Run its local tests without starting WebJET or a browser:

```sh
node --test screenshots/optimize.test.js
```
