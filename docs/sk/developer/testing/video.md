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

  Sila musí byť konečné nezáporné číslo. Pre prirodzený pohyb sa odporúčajú hodnoty od `0` do `2`. Väčšie hodnoty môžu pôsobiť prehnane a pri priblížení dráhy k okraju plochy prehliadača sa automaticky obmedzia. Volania bez druhého parametra použijú `CODECEPT_VIDEO_CURVE_STRENGTH`. Skripty `video` a `video:current` v súbore `package.json` používajú predvolenú hodnotu `0.3`, iba ak premenná nie je nastavená alebo je prázdna. Hodnotou zadanou pred `npm run` ju prepíšete pre jedno nahrávanie; explicitne zadaný parameter má vždy prednosť pred hodnotou z prostredia.

- Manuálne zábery ponechajte v sprievodnom pláne záberov namiesto simulovania nespoľahlivej akcie prehliadača.

Pred hlavným scenárom uchovávajte hovorený text a plán záberov v dvoch samostatných metadátových scenároch. `ElevenLabs` musí obsahovať jediné volanie `I.generateAudio` a značku `@audio`. `Shot plan` naďalej používa `I.say` a nemá značku. Ani jeden z nich nesmie prihlasovať používateľa, otvárať prehliadač alebo vykonávať kroky aplikácie. Nepoužívajte globálny prihlasovací `Before`; objekt `login` vložte až do hlavného scenára označeného `@video`.

```javascript
Feature("video.293-config-jstree-view");

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(`
<text hovoreného slova>
`);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(`
<časový plán záberov>
`);
});

Scenario("293-config-jstree-view", ({ I, login }) => {
    login("admin");
    // Kroky nahrávania videa.
}).tag("@video");
```

## Nahrávanie

V priečinku `src/test/webapp` spustite:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

Oba príkazy predvolene vytvoria kvalitný WebM súbor s rozlíšením `1920 × 1080`, pomenovaný podľa scenára, v priečinku `docs/feature-video` v koreňovom priečinku repozitára, napríklad `293-config-jstree-view.webm`. Pomocník určený pre nahrávanie videa zvyšuje kvalitu snímok Chrome na 100 a nahrádza predvolený cieľový dátový tok Playwright 1 Mb/s hodnotou 50 Mb/s, používa CRF 0 a maximálny kvantizátor 4, aby zachoval detaily používateľského rozhrania. Opakované úspešné spustenie rovnakého scenára nahradí predchádzajúci úspešný súbor. Neúspešná nahrávka sa uloží samostatne s príponou `.failed.webm`, napríklad `293-config-jstree-view.failed.webm`, a poslednú úspešnú nahrávku nenahradí.

Priečinok `docs/feature-video` je lokálny a ignorovaný cez `.gitignore`, takže vygenerované médiá sa nepridajú do Gitu. Finálne MP3 a WebM súbory aj pracovné súbory vznikajú v tomto priečinku a prežijú vyčistenie `build/test`. Playwright najprv nahráva do podpriečinka `.video-raw`. Po dokončení sa jeho UUID súbor atómovo premenuje na stabilný názov a prázdny pracovný priečinok sa odstráni. Pri chybe zostane raw nahrávka zachovaná na diagnostiku.

Štandardné príkazy používajú v `package.json` predvolené rozlíšenie `1920 × 1080` a zväčšenie obsahu stránky na pomer `24/17`, teda približne `141,18 %`. Video pomocník zapíše túto hodnotu ako predvolené priblíženie do dočasného profilu Chromium ešte pred spustením prehliadača. Ide o rovnaký mechanizmus, aký používa priblíženie cez menu Chrome. Aplikácia preto už počas inicializácie pracuje s logickým viewport `1360 × 765`, zatiaľ čo výsledok sa vykreslí priamo do Full HD videa. Texty a ovládacie prvky zostávajú dobre čitateľné bez dodatočného zväčšovania obrazu vo video editore.

Oba príkazy predvolene zobrazia prehliadač. Je to užitočné pri ladení scenára alebo pri použití externého nástroja na nahrávanie obrazovky. Pre jedno spustenie ho môžete skryť pomocou `CODECEPT_SHOW=false`.

Skutočný dátový tok závisí od obsahu obrazu. Nastavený profil využíva viac procesora a vytvára výrazne väčšie súbory, preto video scenáre spúšťajte sériovo a na nahrávacom počítači skontrolujte plynulosť pohybu.

Do výsledného videa sa uloží iba stránka, ktorá je aktívna na konci scenára. Dôležité prechody medzi viacerými kartami alebo akcie mimo prehliadača pripravte ako manuálne zábery.

Pri použití externého nahrávača vypnite zachytávanie systémového kurzora. Video scenár vykresľuje vlastný kurzor aj efekt kliknutia, takže zachytenie oboch kurzorov by vytvorilo rušivú duplicitu.

WebM obsahuje plochu stránky v prehliadači bez hovoreného slova. Vygenerovaný MP3 súbor s hovoreným textom spojte s nahrávkou vo video editore.

WebM je natívny kontajner VP8 enkódera pribaleného k Playwright. Premenovanie súboru na `.mp4` alebo `.mov` ho nekonvertuje. Ak video editor vyžaduje iný formát, skonvertujte kvalitný WebM pomocou plnej inštalácie FFmpeg. Konverzia môže zlepšiť kompatibilitu s editorom, ale nemôže doplniť detaily, ktoré neboli zachytené v zdrojovej nahrávke.

## Nastavenia nahrávania

Predvolené hodnoty `CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT`, `CODECEPT_VIDEO_ZOOM`, `CODECEPT_VIDEO_CURVE_STRENGTH`, `CODECEPT_URL` a `CODECEPT_SHOW` sa použijú iba vtedy, keď príslušná premenná nie je nastavená alebo je prázdna. Hodnoty zadané pred `npm run video` alebo `npm run video:current` majú prednosť.

Rozlíšenie je možné zmeniť pomocou premenných `CODECEPT_VIDEO_WIDTH` a `CODECEPT_VIDEO_HEIGHT`. Štandardné hodnoty sú uvedené priamo v skriptoch `video` a `video:current` v súbore `package.json`, kde ich môžete upraviť pre všetky nahrávania.

Zväčšenie obsahu stránky nastavuje `CODECEPT_VIDEO_ZOOM`. Štandardná hodnota `1.411764705882353` predstavuje presný pomer `24/17` a logickú plochu `1360 × 765`. Hodnota `1` vypne zväčšenie. Podporovaný je aj percentuálny zápis, napríklad `140%`. Hodnota sa nastaví ako natívne predvolené priblíženie v dočasnom profile Chromium; zväčšenie sa už dodatočne nenastavuje cez CSS. Rozlíšenie výsledného videa ani veľkosť syntetického kurzora sa nemenia. Ak chcete namiesto Full HD videa so zväčšením nahrávať priamo v rozlíšení `1360 × 768`, nastavte šírku na `1360`, výšku na `768` a zväčšenie na `1`.

```shell
CODECEPT_VIDEO_WIDTH=1360 CODECEPT_VIDEO_HEIGHT=768 CODECEPT_VIDEO_ZOOM=1 npm run video -- video/293-config-jstree-view.js
```

Cieľovú inštanciu môžete pre jedno nahrávanie zmeniť pomocou `CODECEPT_URL`; predvolená hodnota je `http://iwcm.interway.sk`.

```shell
CODECEPT_VIDEO_CURVE_STRENGTH=0.5 CODECEPT_URL=http://custom.webjetcms.test CODECEPT_SHOW=false npm run video:current
```

Čas pred kliknutím je možné nastaviť v rozsahu od 0 do 2000 milisekúnd pomocou `CODECEPT_VIDEO_CLICK_DELAY`. Každé volanie `I.videoClick` ponechá po kliknutí 500 milisekúnd na jednoduchší strih. Túto hodnotu môžete pomocou `CODECEPT_VIDEO_POST_CLICK_DELAY` zvýšiť až na 2000 milisekúnd.

Pohyb kurzora sa medzi kliknutiami mení, ale generátor používa ako základ názov scenára, takže opakované nahrávky zostávajú reprodukovateľné. Nastavením `CODECEPT_VIDEO_CURSOR_SEED` na inú hodnotu vytvoríte odlišný, ale opakovateľný variant pohybu.

## Generovanie hovoreného slova

Generovanie zvuku používa platené API služby ElevenLabs, preto ho spúšťajte iba vedome a pre konkrétny súbor. V priečinku `src/test/webapp` zadajte práve jeden existujúci JavaScript súbor z priečinka `video`:

```shell
npm run audio video/293-config-jstree-view.js
```

Príkaz spustí iba scenár označený `@audio` cez samostatnú konfiguráciu CodeceptJS. Neotvorí prehliadač, neprihlási používateľa a nespustí scenár videa ani plán záberov. Výsledok vo formáte `mp3_44100_128` uloží ako `docs/feature-video/293-config-jstree-view.mp3` v koreňovom priečinku repozitára.

### API kľúč ElevenLabs

1. Prihláste sa do ElevenLabs a otvorte **Developers > API Keys**.
2. Vytvorte obmedzený kľúč, povoľte mu iba oprávnenie `text_to_speech` a nastavte kreditný limit.
3. Kľúč po vytvorení hneď skopírujte. ElevenLabs zobrazí jeho úplnú hodnotu iba raz.
4. Uchovávajte ho ako tajomstvo mimo repozitára a nastavte ho do premennej prostredia `ELEVENLABS_API_KEY`.

Podrobný postup je v [oficiálnej dokumentácii autorizácie ElevenLabs](https://elevenlabs.io/docs/help-center/technical/how-do-i-authorize-myself-using-an-api-key). Projekt súbory `.env` automaticky nenačítava. API kľúč preto nevkladajte do `.env` s očakávaním automatického použitia, do JavaScript scenára, parametra pomocníka ani argumentu príkazového riadka.

```shell
export ELEVENLABS_API_KEY="<váš-api-kľúč>"
npm run audio video/293-config-jstree-view.js
```

### Model a hlas

Predvolený model je Eleven v3 s identifikátorom `eleven_v3`. Predvolený hlas je `Luki Zajo` s identifikátorom `Zai7B4Aol2bJtneyq0L1`. Model a hlas môžete zmeniť premennými prostredia pre celé spustenie:

```shell
ELEVENLABS_MODEL_ID=eleven_multilingual_v2 npm run audio video/293-config-jstree-view.js
ELEVENLABS_VOICE_ID="<voice-id>" npm run audio video/293-config-jstree-view.js
```

Alebo ich nastavte iba pre jedno volanie pomocníka:

```javascript
I.generateAudio(`
<text hovoreného slova>
`, {
    modelId: "eleven_multilingual_v2",
    voiceId: "<voice-id>",
});
```

Explicitný parameter `modelId` alebo `voiceId` má prednosť pred neprázdnou premennou prostredia, tá má prednosť pred predvolenou hodnotou. API kľúč je možné zadať výhradne cez `ELEVENLABS_API_KEY`. Pomocník neposiela `voice_settings`, takže ElevenLabs použije uložené alebo predvolené nastavenia hlasu. Dostupné modely opisuje [dokumentácia modelov](https://elevenlabs.io/docs/overview/models) a formát požiadavky [Text to Speech API](https://elevenlabs.io/docs/api-reference/text-to-speech/convert).

`Luki Zajo` je hlas z komunitnej knižnice. Jeho použitie cez API závisí od dostupnosti hlasu a programu účtu a nemusí byť dostupné v bezplatnom programe. V takom prípade použite program, ktorý povoľuje API prístup k hlasom z [Voice Library](https://elevenlabs.io/docs/eleven-creative/voices/voice-library), alebo nastavte `ELEVENLABS_VOICE_ID` na hlas dostupný pre váš účet. Uloženie hlasu do **My Voices** je voliteľné a samo osebe API prístup v bezplatnom programe neodomkne. Zoznam hlasov vhodných pre slovenčinu nájdete na stránke [Slovak Text to Speech](https://elevenlabs.io/text-to-speech/slovak).

Pomocník ešte pred volaním API overí, že môže v cieľovom priečinku vytvoriť dočasný súbor. Potom načíta celú odpoveď, overí zvukový formát a až úplným dočasným súborom atómovo nahradí výsledný MP3 súbor. Pri chybe API, siete, časového limitu alebo zápisu zostane posledný úspešný súbor zachovaný. Požiadavka sa automaticky neopakuje, aby nejasná sieťová chyba nespôsobila druhé účtovanie kreditov.
