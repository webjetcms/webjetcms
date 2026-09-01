# Nahrávanie prezentačných videí

Priečinok `src/test/webapp/video` obsahuje opakovateľné scenáre ovládania prehliadača určené na tvorbu produktových videí. Od regresných testov sú oddelené preto, že poradie krokov a vizuálna kompozícia sú súčasťou výsledného videa.

## Pomenovanie scenára

Pre názov súboru aj názov `Scenario` použite formát `<PR-ID>-<branch>.js`. Z názvu vetvy odstráňte úvodný prefix `feature/` alebo `hotfix/`. Napríklad pull request 293 z vetvy `feature/config-jstree-view` použije názov:

```text
293-config-jstree-view.js
```

Použite `Feature("video.<scenario-name>")`, aby sa dal zdroj scenára jednoducho nájsť v reportoch.

## Tvorba scenára

- Ak je možné vlastnosť predviesť bez zmeny údajov, scenár ponechajte iba na čítanie.
- Používajte stabilné CSS selektory alebo `data` atribúty, ktoré už využívajú regresné testy.
- Kroky synchronizujte pomocou `waitFor*`, stavu aplikácie alebo `DT.waitForLoader()`. Pevné čakanie nepoužívajte na synchronizáciu aplikácie.
- Pre dôležité kliknutia používajte `I.videoClick(locator)`. Vykreslený kurzor sa presunie po variabilnej prirodzenej dráhe s väčším počiatočným oblúkom, malou korekciou pred cieľom a plynulým zrýchlením a spomalením. Pred kliknutím pridá krátky vizuálny predstih. Voliteľný druhý parameter určuje silu zakrivenia:

  ```javascript
  I.videoClick(locator);      // Predvolená hodnota z prostredia, záložná hodnota je 1.
  I.videoClick(locator, 0);   // Priama dráha.
  I.videoClick(locator, 0.5); // Mierne zakrivenie.
  I.videoClick(locator, 1.5); // Výraznejšie zakrivenie.
  ```

  Sila musí byť konečné nezáporné číslo. Pre prirodzený pohyb sa odporúčajú hodnoty od `0` do `2`. Väčšie hodnoty môžu pôsobiť prehnane a pri priblížení dráhy k okraju plochy prehliadača sa automaticky obmedzia. Volania bez druhého parametra použijú `CODECEPT_VIDEO_CURVE_STRENGTH`. Skripty `video` a `video:current` v súbore `package.json` nastavujú predvolenú hodnotu na `0.3`; jej zmenou upravíte všetky nahrávania. Explicitne zadaný parameter má vždy prednosť pred hodnotou z prostredia.

- Manuálne zábery ponechajte v sprievodnom pláne záberov namiesto simulovania nespoľahlivej akcie prehliadača.

## Nahrávanie

V priečinku `src/test/webapp` spustite:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

Oba príkazy vytvoria kvalitný WebM súbor s rozlíšením `1920 × 1080`, pomenovaný podľa scenára, v priečinku `build/test/videos`, napríklad `293-config-jstree-view.webm`. Pomocník určený pre nahrávanie videa zvyšuje kvalitu snímok Chrome na 100 a nahrádza predvolený cieľový dátový tok Playwright 1 Mb/s hodnotou 50 Mb/s, používa CRF 0 a maximálny kvantizátor 4, aby zachoval detaily používateľského rozhrania. Opakované spustenie rovnakého scenára nahradí predchádzajúci súbor.

Oba príkazy zobrazia prehliadač. Je to užitočné pri ladení scenára alebo pri použití externého nástroja na nahrávanie obrazovky.

Skutočný dátový tok závisí od obsahu obrazu. Nastavený profil využíva viac procesora a vytvára výrazne väčšie súbory, preto video scenáre spúšťajte sériovo a na nahrávacom počítači skontrolujte plynulosť pohybu.

Do výsledného videa sa uloží iba stránka, ktorá je aktívna na konci scenára. Dôležité prechody medzi viacerými kartami alebo akcie mimo prehliadača pripravte ako manuálne zábery.

Pri použití externého nahrávača vypnite zachytávanie systémového kurzora. Video scenár vykresľuje vlastný kurzor aj efekt kliknutia, takže zachytenie oboch kurzorov by vytvorilo rušivú duplicitu.

WebM obsahuje plochu stránky v prehliadači bez hovoreného slova. Nahovorený text vytvorte v ElevenLabs a s nahrávkou ho spojte vo video editore.

WebM je natívny kontajner VP8 enkódera pribaleného k Playwright. Premenovanie súboru na `.mp4` alebo `.mov` ho nekonvertuje. Ak video editor vyžaduje iný formát, skonvertujte kvalitný WebM pomocou plnej inštalácie FFmpeg. Konverzia môže zlepšiť kompatibilitu s editorom, ale nemôže doplniť detaily, ktoré neboli zachytené v zdrojovej nahrávke.

## Nastavenia nahrávania

Rozlíšenie je možné zmeniť pomocou premenných `CODECEPT_VIDEO_WIDTH` a `CODECEPT_VIDEO_HEIGHT`.

Čas pred kliknutím je možné nastaviť v rozsahu od 0 do 2000 milisekúnd pomocou `CODECEPT_VIDEO_CLICK_DELAY`. Každé volanie `I.videoClick` ponechá po kliknutí 500 milisekúnd na jednoduchší strih. Túto hodnotu môžete pomocou `CODECEPT_VIDEO_POST_CLICK_DELAY` zvýšiť až na 2000 milisekúnd.

Pohyb kurzora sa medzi kliknutiami mení, ale generátor používa ako základ názov scenára, takže opakované nahrávky zostávajú reprodukovateľné. Nastavením `CODECEPT_VIDEO_CURSOR_SEED` na inú hodnotu vytvoríte odlišný, ale opakovateľný variant pohybu.
