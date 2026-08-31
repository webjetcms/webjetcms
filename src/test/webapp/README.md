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

Video scenáre sú v priečinku `video`. Playwright ich nahráva do
`build/test/videos/<názov-scenára>.webm`. Opakovaný beh prepíše predošlé video
rovnakého scenára. Nahrávka obsahuje syntetický kurzor aj zvýraznenie kliknutia.

```sh
cd src/test/webapp

# Nahratie všetkých video scenárov
npm run video

# Nahratie jedného súboru
npm run video -- video/293-config-jstree-view.js

# Nahratie scenára označeného ako @current
npm run video:current
```

Oba príkazy používajú viditeľný prehliadač.

Konvencie pre tvorbu scenárov sú v [video/README.md](video/README.md).
