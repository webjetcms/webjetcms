# Nahrávání prezentačních videí

Složka `src/test/webapp/video` obsahuje opakovatelné scénáře ovládání prohlížeče určené pro tvorbu produktových videí. Od regresních testů jsou odděleny proto, že pořadí kroků a vizuální kompozice jsou součástí výsledného videa.

## Pojmenování scénáře

Pro název souboru i název `Scenario` použijte formát `<PR-ID>-<branch>.js`. Z názvu větve odstraňte úvodní prefix `feature/` nebo `hotfix/`. Například pull request 293 z větve `feature/config-jstree-view` použije název:

```text
293-config-jstree-view.js
```

Použijte `Feature("video.<scenario-name>")`, aby se dal zdroj scénáře snadno najít v reportech.

## Tvorba scénáře

- Pokud lze vlastnost předvést beze změny údajů, scénář ponechejte pouze ke čtení.
- Používejte stabilní CSS selektory nebo `data` atributy, které již využívají regresní testy.
- Kroky synchronizujte pomocí `waitFor*`, stavu aplikace nebo `DT.waitForLoader()`. Pevné čekání nepoužívejte k synchronizaci aplikace.
- Pro důležitá kliknutí používejte `I.videoClick(locator)`. Vykreslený kurzor se přesune po variabilní přirozené dráze s větším počátečním obloukem, malou korekcí před cílem a plynulým zrychlením a zpomalením. Před klepnutím přidá krátký vizuální předstih. Volitelný druhý parametr určuje sílu zakřivení:

  ```javascript
  I.videoClick(locator);      // Predvolená hodnota z prostredia, záložná hodnota je 1.
  I.videoClick(locator, 0);   // Priama dráha.
  I.videoClick(locator, 0.5); // Mierne zakrivenie.
  I.videoClick(locator, 1.5); // Výraznejšie zakrivenie.
  ```

  Síla musí být konečné nezáporné číslo. Pro přirozený pohyb se doporučují hodnoty od `0` do `2`. Větší hodnoty mohou působit přehnaně a při přiblížení dráhy k okraji plochy prohlížeče se automaticky omezí. Volání bez druhého parametru použijí `CODECEPT_VIDEO_CURVE_STRENGTH`. Skripty `video` a `video:current` v souboru `package.json` používají výchozí hodnotu `0.3`, pouze pokud proměnná není nastavena nebo je prázdná. Hodnotou zadanou před `npm run` ji přepíšete pro jedno nahrávání; explicitně zadaný parametr má vždy přednost před hodnotou z prostředí.

- Manuální záběry ponechte v doprovodném plánu záběrů namísto simulování nespolehlivé akce prohlížeče.

Před hlavním scénářem uchovávejte mluvený text a plán záběrů ve dvou samostatných metadatových scénářích. `ElevenLabs` musí obsahovat jediné volání `I.generateAudio` a značku `@audio`. `Shot plan` nadále používá `I.say` a nemá značku. Ani jeden z nich nesmí přihlašovat uživatele, otevírat prohlížeč nebo provádět kroky aplikace. Nepoužívejte globální přihlašovací `Before` ; objekt `login` vložte až do hlavního scénáře označeného `@video`.

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

## Nahrávání

Ve složce `src/test/webapp` spusťte:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

Oba příkazy ve výchozím nastavení vytvoří kvalitní WebM soubor s rozlišením `1920 × 1080`, pojmenovaný podle scénáře, ve složce `docs/feature-video` v kořenové složce repozitáře, například `293-config-jstree-view.webm`. Pomocník určený pro nahrávání videa zvyšuje kvalitu snímků Chrome na 100 a nahrazuje výchozí cílový datový tok Playwright 1 Mb/s hodnotou 50 Mb/s, používá CRF 0 a maximální kvantizátor 4, aby zachoval detaily uživatelského rozhraní. Opakované úspěšné spuštění stejného scénáře nahradí předchozí úspěšný soubor. Neúspěšná nahrávka se uloží samostatně s příponou `.failed.webm`, například `293-config-jstree-view.failed.webm`, a poslední úspěšnou nahrávku nenahradí.

Složka `docs/feature-video` je lokální a ignorována přes `.gitignore`, takže vygenerovaná média se nepřidají do Gitu. Finální MP3 a WebM soubory i pracovní soubory vznikají v této složce a přežijí vyčištění `build/test`. Playwright nejprve nahrává do podsložky `.video-raw`. Po dokončení se jeho UUID soubor atomově přejmenuje na stabilní název a prázdná pracovní složka se odstraní. Při chybě zůstane raw nahrávka zachována pro diagnostiku.

Standardní příkazy používají v `package.json` výchozí rozlišení `1920 × 1080` a zvětšení obsahu stránky na poměr `24/17`, tedy přibližně `141,18 %`. Videonápověda zapíše tuto hodnotu jako výchozí přiblížení do dočasného profilu Chromium ještě před spuštěním prohlížeče. Jedná se o stejný mechanismus, jaký používá přiblížení přes menu Chrome. Aplikace proto již během inicializace pracuje s logickým viewportem `1360 × 765`, zatímco výsledek se vykreslí přímo do Full HD videa. Texty a ovládací prvky zůstávají dobře čitelné bez dodatečného zvětšování obrazu ve video editoru.

Oba příkazy ve výchozím nastavení zobrazí prohlížeč. Je to užitečné při ladění scénáře nebo při použití externího nástroje pro nahrávání obrazovky. Pro jedno spuštění jej můžete skrýt pomocí `CODECEPT_SHOW=false`.

Skutečný datový tok závisí na obsahu obrazu. Nastavený profil využívá více procesoru a vytváří výrazně větší soubory, proto video scénáře spouštějte sériově a na nahrávacím počítači zkontrolujte plynulost pohybu.

Do výsledného videa se uloží pouze stránka, která je aktivní na konci scénáře. Důležité přechody mezi více kartami nebo akce mimo prohlížeč připravte jako manuální záběry.

Při použití externího nahrávače vypněte zachytávání systémového kurzoru. Video scénář vykresluje vlastní kurzor i efekt kliknutí, takže zachycení obou kurzorů by vytvořilo rušivou duplicitu.

WebM obsahuje plochu stránky v prohlížeči bez mluveného slova. Vygenerovaný MP3 soubor s mluveným textem spojte s nahrávkou ve video editoru.

WebM je nativní kontejner VP8 enkodéru přibaleného k Playwright. Přejmenování souboru na `.mp4` nebo `.mov` jej nekonvertuje. Pokud video editor vyžaduje jiný formát, zkonvertujte kvalitní WebM pomocí plné instalace FFmpeg. Konverze může zlepšit kompatibilitu s editorem, ale nemůže doplnit detaily, které nebyly zachyceny ve zdrojové nahrávce.

## Nastavení nahrávání

Výchozí hodnoty `CODECEPT_VIDEO_WIDTH`, `CODECEPT_VIDEO_HEIGHT`, `CODECEPT_VIDEO_ZOOM`, `CODECEPT_VIDEO_CURVE_STRENGTH`, `CODECEPT_URL` a `CODECEPT_SHOW` se použijí pouze tehdy, když příslušná proměnná není nastavena nebo je prázdná. Hodnoty zadané před `npm run video` nebo `npm run video:current` mají přednost.

Rozlišení lze změnit pomocí proměnných `CODECEPT_VIDEO_WIDTH` a `CODECEPT_VIDEO_HEIGHT`. Standardní hodnoty jsou uvedeny přímo ve skriptech `video` a `video:current` v souboru `package.json`, kde je můžete upravit pro všechna nahrávání.

Zvětšení obsahu stránky nastavuje `CODECEPT_VIDEO_ZOOM`. Standardní hodnota `1.411764705882353` představuje přesný poměr `24/17` a logickou plochu `1360 × 765`. Hodnota `1` vypne zvětšení. Podporován je také procentuální zápis, například `140%`. Hodnota se nastaví jako nativní výchozí přiblížení v dočasném profilu Chromium; zvětšení se již dodatečně nenastavuje přes CSS. Rozlišení výsledného videa ani velikost syntetického kurzoru se nemění. Chcete-li místo Full HD videa se zvětšením nahrávat přímo v rozlišení `1360 × 768`, nastavte šířku na `1360`, výšku na `768` a zvětšení na `1`.

```shell
CODECEPT_VIDEO_WIDTH=1360 CODECEPT_VIDEO_HEIGHT=768 CODECEPT_VIDEO_ZOOM=1 npm run video -- video/293-config-jstree-view.js
```

Cílovou instanci můžete pro jedno nahrávání změnit pomocí `CODECEPT_URL` ; výchozí hodnota je `http://iwcm.interway.sk`.

```shell
CODECEPT_VIDEO_CURVE_STRENGTH=0.5 CODECEPT_URL=http://custom.webjetcms.test CODECEPT_SHOW=false npm run video:current
```

Čas před kliknutím lze nastavit v rozsahu od 0 do 2000 milisekund pomocí `CODECEPT_VIDEO_CLICK_DELAY`. Každé volání `I.videoClick` ponechá po kliknutí 500 milisekund na jednodušší střih. Tuto hodnotu můžete pomocí `CODECEPT_VIDEO_POST_CLICK_DELAY` zvýšit až na 2000 milisekund.

Pohyb kurzoru se mezi klepnutími mění, ale generátor používá jako základ název scénáře, takže opakované nahrávky zůstávají reprodukovatelné. Nastavením `CODECEPT_VIDEO_CURSOR_SEED` na jinou hodnotu vytvoříte odlišnou, ale opakovatelnou variantu pohybu.

## Generování mluveného slova

Generování zvuku používá placené API služby ElevenLabs, proto jej spouštějte pouze vědomě a pro konkrétní soubor. Ve složce `src/test/webapp` zadejte právě jeden existující JavaScript soubor ze složky `video`:

```shell
npm run audio video/293-config-jstree-view.js
```

Příkaz spustí pouze scénář označený `@audio` přes samostatnou konfiguraci CodeceptJS. Neotevře prohlížeč, nepřihlásí uživatele a nespustí scénář videa ani plán záběrů. Výsledek ve formátu `mp3_44100_128` uloží jako `docs/feature-video/293-config-jstree-view.mp3` v kořenové složce repozitáře.

### API klíč ElevenLabs

1. Přihlaste se do ElevenLabs a otevřete **Developers > API Keys**.
2. Vytvořte omezený klíč, povolte mu pouze oprávnění `text_to_speech` a nastavte kreditní limit.
3. Klíč po vytvoření hned zkopírujte. ElevenLabs zobrazí jeho úplnou hodnotu pouze jednou.
4. Uchovávejte jej jako tajemství mimo repozitář a nastavte jej do proměnné prostředí `ELEVENLABS_API_KEY`.

Podrobný postup je v [oficiální dokumentaci autorizace ElevenLabs](https://elevenlabs.io/docs/help-center/technical/how-do-i-authorize-myself-using-an-api-key). Projekt soubory `.env` automaticky nenačítá. API klíč proto nevkládejte do `.env` s očekáváním automatického použití, do JavaScript scénáře, parametru nápovědy ani argumentu příkazového řádku.

```shell
export ELEVENLABS_API_KEY="<váš-api-kľúč>"
npm run audio video/293-config-jstree-view.js
```

### Model a hlas

Výchozí model je Eleven v3 s identifikátorem `eleven_v3`. Výchozí hlas je `Luki Zajo` s identifikátorem `Zai7B4Aol2bJtneyq0L1`. Model a hlas můžete změnit proměnnými prostředí pro celé spuštění:

```shell
ELEVENLABS_MODEL_ID=eleven_multilingual_v2 npm run audio video/293-config-jstree-view.js
ELEVENLABS_VOICE_ID="<voice-id>" npm run audio video/293-config-jstree-view.js
```

Nebo je nastavte pouze pro jedno volání pomocníka:

```javascript
I.generateAudio(`
<text hovoreného slova>
`, {
    modelId: "eleven_multilingual_v2",
    voiceId: "<voice-id>",
});
```

Explicitní parametr `modelId` nebo `voiceId` má přednost před neprázdnou proměnnou prostředí, ta má přednost před výchozí hodnotou. API klíč lze zadat výhradně přes `ELEVENLABS_API_KEY`. Nápověda neposílá `voice_settings`, takže ElevenLabs použije uložená nebo výchozí nastavení hlasu. Dostupné modely popisují [dokumentace modelů](https://elevenlabs.io/docs/overview/models) a formát požadavku [Text to Speech API](https://elevenlabs.io/docs/api-reference/text-to-speech/convert `Luki Zajo` je hlas z komunitní knihovny. Jeho použití přes API závisí na dostupnosti hlasu a programu účtu a nemusí být dostupné v bezplatném programu. V takovém případě použijte program, který povoluje API přístup k hlasům z [Voice Library](https://elevenlabs.io/docs/eleven-creative/voices/voice-library), nebo nastavte `ELEVENLABS_VOICE_ID` na hlas dostupný pro váš účet. Uložení hlasu do **My Voices** je volitelné a samo o sobě API přístup v bezplatném programu neodemkne. Seznam hlasů vhodných pro češtinu naleznete na stránce [Czech Text to Speech](https://elevenlabs.io/text-to-speech/czech).

Nápověda ještě před voláním API ověří, že může v cílové složce vytvořit dočasný soubor. Potom načte celou odpověď, ověří zvukový formát a až úplným dočasným souborem atomově nahradí výsledný MP3 soubor. Při chybě API, sítě, časového limitu nebo zápisu zůstane poslední úspěšný soubor zachován. Požadavek se automaticky neopakuje, aby nejasná síťová chyba nezpůsobila druhé účtování kreditů.
