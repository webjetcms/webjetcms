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

## Nahrávanie prezentačných videí

Video scenáre sú v priečinku `video`. Playwright ich nahráva do `build/test/videos/<názov-scenára>.webm`. Opakovaný úspešný beh prepíše predošlé úspešné video rovnakého scenára. Neúspešný beh sa uloží ako `build/test/videos/<názov-scenára>.failed.webm`, takže posledné úspešné video zostane zachované. Nahrávka obsahuje syntetický kurzor aj zvýraznenie kliknutia.

```sh
cd src/test/webapp

# Nahratie všetkých video scenárov
npm run video

# Nahratie jedného súboru
npm run video -- video/293-config-jstree-view.js

# Nahratie scenára označeného ako @current
npm run video:current
```

Oba príkazy používajú viditeľný prehliadač. Predvolenú silu krivky kurzora nastavuje `CODECEPT_VIDEO_CURVE_STRENGTH` vo video príkazoch v `package.json` s predvolenou hodnotou `0.3`. Explicitný druhý parameter `I.videoClick(locator, curveStrength)` má vždy prednosť.

Rozlíšenie videa a zväčšenie obsahu stránky nastavujú video príkazy v `package.json` pomocou `CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT` a `CODECEPT_VIDEO_ZOOM`.

Konvencie pre tvorbu scenárov sú v dokumentácii [Nahrávanie prezentačných videí](../../../docs/sk/developer/testing/video.md).
