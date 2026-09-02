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

# Generovanie hovoreného slova pre jeden video súbor
npm run audio video/293-config-jstree-view.js
```

Príkazy `video` a `video:current` predvolene používajú viditeľný prehliadač; pre jedno spustenie ho môžete skryť pomocou `CODECEPT_SHOW=false`. Predvolenú silu krivky kurzora nastavuje `CODECEPT_VIDEO_CURVE_STRENGTH` vo video príkazoch v `package.json` s hodnotou `0.3`. Hodnota zadaná pred `npm run` má prednosť a explicitný druhý parameter `I.videoClick(locator, curveStrength)` má vždy prednosť pred hodnotou z prostredia.

Rozlíšenie videa, zväčšenie obsahu stránky a cieľovú adresu nastavujú video príkazy v `package.json` pomocou `CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT`, `CODECEPT_VIDEO_ZOOM` a `CODECEPT_URL`. Predvolené hodnoty môžete pre jedno nahrávanie prepísať zadaním premenných pred `npm run video` alebo `npm run video:current`.

Audio príkaz vyžaduje práve jeden existujúci `.js` súbor z priečinka `video` a spustí iba jeho scenár `ElevenLabs` označený `@audio`. Nepoužíva prehliadač ani prihlásenie. API kľúč číta výhradne z `ELEVENLABS_API_KEY`; súbory `.env` sa automaticky nenačítavajú. Predvolene používa model `eleven_v3`, hlas Luki Zajo `Zai7B4Aol2bJtneyq0L1` a výsledok uloží vo formáte `mp3_44100_128` do `build/test/videos/<názov-scenára>.mp3`. Model a hlas môžete prepísať cez `ELEVENLABS_MODEL_ID`, `ELEVENLABS_VOICE_ID` alebo explicitné parametre `I.generateAudio`, ktoré majú najvyššiu prioritu. Generovanie spúšťajte iba vedome, pretože volanie ElevenLabs API môže spotrebovať kredity.

Postup vytvorenia obmedzeného ElevenLabs API kľúča, konvencie audio scenára a podrobné nastavenia sú v dokumentácii [Nahrávanie prezentačných videí](../../../docs/sk/developer/testing/video.md).
