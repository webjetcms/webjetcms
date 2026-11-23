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

Před hlavním scénářem uchovávejte mluvený text a plán záběrů v samostatných metadatových scénářích. Při plánu s typem `head` mezi ně přidejte i scénář `ElevenLabs Head` popsaný níže. `ElevenLabs` musí obsahovat jediné volání `I.generateAudio` a značku `@audio`. `Shot plan` nadále používá `I.say` a nemá značku. Ani jeden z nich nesmí přihlašovat uživatele, otevírat prohlížeč nebo provádět kroky aplikace. Nepoužívejte globální přihlašovací `Before` ; objekt `login` vložte až do hlavního scénáře označeného `@video`.

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

## Miniatura pro YouTube

Miniaturu vytvoříte samostatným scénářem označeným pouze `@title`. Připravte v něm obrazovku z videa a zavolejte `I.videoTitle(text, style)`. Helper zachytí aktuální pohled prohlížeče včetně iframe, doplní titulek a uloží JPG. Scénář 308 sdílí přípravu dočasného obsahu s videem a po vytvoření obrázku editor zavře bez uložení změn.

```shell
cd src/test/webapp
npm run video:title video/308-pb-redesign.js
npm run video:title -- video/308-pb-redesign.js --text "Page Builder po novom" --style glow
npm run video:title -- video/308-pb-redesign.js --style clean
npm run video:title -- video/308-pb-redesign.js --style bold
```

| Styl | Vzhled |
| --- | --- |
| `glow` (výchozí) | Tmavé pozadí, bílý titulek s modrofialovou září a mírně natočený screenshot. |
| `clean` | Světlé pozadí, modrý titulek a rovný screenshot s jemným stínem. |
| `bold` | Modré pozadí, velká bílá písmena s výrazným stínem, žlutý akcent a natočený screenshot. |

Všechny styly používají přímo SVG logo z `src/main/webapp/admin/v9/src/images/logo-cms.svg`. Ve světlém stylu `clean` má bílé logo tmavý podklad. Kompozice se vykreslí ve vyšším rozlišení a před exportem se kvalitně zmenší, aby byly natočené čáry screenshotu vyhlazeny.

Výstup má rozlišení **1920 × 1080**, poměr **16:9**, formát **JPG** a velikost pod **2 MB**. Ukládá se do `docs/feature-video/<scenario-name>-title-<style>.jpg`, například `308-pb-redesign-title-glow.jpg`. Každý styl má vlastní soubor; další úspěšné generování nahradí pouze stejný styl. Při chybě zůstane předchozí obrázek zachován. Obrázek můžete nahrát jako vlastní miniaturu v YouTube Studio. Aktuální požadavky jsou v [pomoci YouTube](https://support.google.com/youtube/answer/72431).

```javascript
Scenario("YouTube thumbnail", async ({ I, login }) => {
    login("admin");
    await I.amOnPage("/admin/v9/webpages/web-pages-list/");
    await I.waitForVisible("#datatableInit", 20);
    await I.videoTitle("Page Builder\nNew experience", "glow");
}).tag("@title");
```

Text podporuje diakritiku a `\n` pro ruční zalomení řádků. Velikost písma se přizpůsobí dostupnému prostoru. Nejlepší funguje krátký titulek; limit je 160 znaků a text, který se nevejde ani po zmenšení, skončí chybou. Hodnoty `--text` a `--style` přepíší výchozí hodnoty scénáře. Alternativně použijte proměnné `VIDEO_TITLE_TEXT` a `VIDEO_TITLE_STYLE` ; parametry příkazu mají přednost před proměnnými.

Příkaz spustí pouze `@title`, bez nahrávání WebM a bez generování audia či mluvící postavy. `VIDEO_SHOT` se při miniatuře ignoruje. Volitelné `--dry-run` vypíše kroky bez otevření prohlížeče. Scénář musí počkat na požadovaný stav aplikace před voláním helperu; změnu podkladového screenshotu provedete změnou jeho přípravných kroků. Není třeba nejprve nahrát celé video.

Stávající `I.videoTitle(shot)` a volání pouze s textem v běžném nahrávání nadále zobrazují střihový mezititulek. V režimu `video:title` stačí i `I.videoTitle(text)` a použije se `glow`. Text se zadaným stylem generuje miniaturu i mimo tento režim.

## Přehled textů a plánu záběrů

Scénář se společným objektem `const videoPlan` si můžete přečíst v přehledné podobě bez spuštění nahrávání. Ve složce `src/test/webapp` spusťte:

```shell
npm run video:plan video/308-pb-redesign.js
```

Příkaz vypíše souhrn s jazykem, počtem záběrů, odhadovanou délkou, počtem slov a znaků. Následuje část `NARRATION` s celým mluveným textem v pořadí záběrů a část `SHOT PLAN` s časovou osou, typy záběrů, ID, názvy, poznámkami a textem každého záběru. Časy vycházejí z `durationSeconds` a označují odhad sestříhaného videa. Jazyk určuje `videoPlan.language`, výchozí hodnota je `sk`.

Není potřeba API klíč ani spuštěná instance WebJET CMS. Příkaz pouze staticky přečte objekt: nespouští CodeceptJS, prohlížeč, generování audia ani funkce `prepare` a `shot`. Deklarace `Scenario("Shot plan")` může zůstat v souboru, ale příkaz ji nepotřebuje ani neprovádí. Funguje také pro rozpracovaný plán bez audio scénáře či připravených obrázků pro `head`. Starší ručně psaný text v `I.say` bez objektu `const videoPlan` tento příkaz nečte.

Přehled můžete uložit do textového souboru. Parametr `--silent` potlačí úvodní výpis npm:

```shell
npm run --silent video:plan video/308-pb-redesign.js > /tmp/308-pb-redesign-plan.txt
```

## Nahrávání

Ve složce `src/test/webapp` spusťte:

```shell
npm run video -- video/293-config-jstree-view.js
npm run video:current
```

Oba příkazy ve výchozím nastavení vytvoří kvalitní WebM soubor s rozlišením `1920 × 1080`, pojmenovaný podle scénáře, ve složce `docs/feature-video` v kořenové složce repozitáře, například `293-config-jstree-view.webm`. Pomocník určený pro nahrávání videa zvyšuje kvalitu snímků Chrome na 100 a nahrazuje výchozí cílový datový tok Playwright 1 Mb/s hodnotou 50 Mb/s, používá CRF 0 a maximální kvantizátor 4, aby zachoval detaily uživatelského rozhraní. Opakované úspěšné spuštění stejného scénáře nahradí předchozí úspěšný soubor. Neúspěšná nahrávka se uloží samostatně s příponou `.failed.webm`, například `293-config-jstree-view.failed.webm`, a poslední úspěšnou nahrávku nenahradí.

Složka `docs/feature-video` je lokální a ignorována přes `.gitignore`, takže vygenerovaná média se nepřidají do Gitu. Finální MP3, MP4 a WebM soubory i pracovní soubory vznikají v této složce a přežijí vyčištění `build/test`. Playwright nejprve nahrává do podsložky `.video-raw`. Po dokončení se jeho UUID soubor atomově přejmenuje na stabilní název a prázdná pracovní složka se odstraní. Při chybě zůstane raw nahrávka zachována pro diagnostiku.

Standardní příkazy používají v `package.json` výchozí rozlišení `1920 × 1080` a zvětšení obsahu stránky na poměr `24/17`, tedy přibližně `141,18 %`. Videonápověda zapíše tuto hodnotu jako výchozí přiblížení do dočasného profilu Chromium ještě před spuštěním prohlížeče. Jedná se o stejný mechanismus, jaký používá přiblížení přes menu Chrome. Aplikace proto již během inicializace pracuje s logickým viewportem `1360 × 765`, zatímco výsledek se vykreslí přímo do Full HD videa. Texty a ovládací prvky zůstávají dobře čitelné bez dodatečného zvětšování obrazu ve video editoru.

Oba příkazy ve výchozím nastavení zobrazí prohlížeč. Je to užitečné při ladění scénáře nebo při použití externího nástroje pro nahrávání obrazovky. Pro jedno spuštění jej můžete skrýt pomocí `CODECEPT_SHOW=false`.

Skutečný datový tok závisí na obsahu obrazu. Nastavený profil využívá více procesoru a vytváří výrazně větší soubory, proto video scénáře spouštějte sériově a na nahrávacím počítači zkontrolujte plynulost pohybu.

Do výsledného videa se uloží pouze stránka, která je aktivní na konci scénáře. Důležité přechody mezi více kartami nebo akce mimo prohlížeč připravte jako manuální záběry.

Při použití externího nahrávače vypněte zachytávání systémového kurzoru. Video scénář vykresluje vlastní kurzor i efekt kliknutí, takže zachycení obou kurzorů by vytvořilo rušivou duplicitu.

WebM obsahuje plochu stránky v prohlížeči bez mluveného slova. Vygenerovaný MP3 soubor s mluveným textem spojte s nahrávkou ve video editoru.

WebM je nativní kontejner VP8 enkodéru přibaleného k Playwright. Přejmenování souboru na `.mp4` nebo `.mov` jej nekonvertuje. Pokud video editor vyžaduje jiný formát, zkonvertujte kvalitní WebM pomocí plné instalace FFmpeg. Konverze může zlepšit kompatibilitu s editorem, ale nemůže doplnit detaily, které nebyly zachyceny ve zdrojové nahrávce.

### Opakované nahrání jednoho záběru

U scénářů se společným `videoPlan` a runnerem `recordVideoPlan` můžete nahrát pouze jeden záběr pomocí proměnné `VIDEO_SHOT`. Zadejte přesné `id` záběru z plánu, například `outro`:

```shell
VIDEO_SHOT=outro npm run video video/308-pb-redesign.js
```

Výsledek se uloží jako `docs/feature-video/308-pb-redesign-outro.webm` ; neúspěšný běh jako `308-pb-redesign-outro.failed.webm`. Opakované spuštění nahradí pouze retake stejného záběru a stejného stavu. Celé video i poslední úspěšný retake při neúspěchu zůstanou zachovány. Proměnná funguje také s `npm run video:current`.

Společný `setup` se provede jednou. Potom se provede pouze příprava, titulky, akce a cleanup vybraného záběru. Po odstranění dvousekundového titulku SHOT následují **3 sekundy čistého obrazu před akcí** na střihové přechody; po akci zůstává dvousekundová rezerva. Tyto rezervy platí i při nahrávání celého plánu a nemění odhadované délky záběrů. Ostatní snímky se nespustí. Titulky a výpisy zachovávají původní číslo záběru a celkový počet z celého plánu; retake nemění jeho časovou osu. Výběr `manual` nebo `head` zobrazí pouze existující varovnou tabulku po společném setupu.

Samostatný WebM začíná přímo titulkem `Shot N/total: id`, včetně jeho zobrazení. Při ukládání se odstraní přihlašování, SETUP i příprava před tímto titulkem. Oříznutí používá skutečné pozice snímků Chromium a FFmpeg přibalený k Playwright se stejným profilem kvality VP8. Celá nahrávka bez `VIDEO_SHOT` zůstává včetně SETUP. Pokud záběr selže ještě před titulkem, jeho `.failed.webm` ponechá celou přípravu na diagnostiku. Při chybě oříznutí se zachová raw video a původní výstup se nenahradí.

Nenastavená nebo prázdná hodnota znamená celý plán; okolní mezery se oříznou. ID musí používat malá písmena, číslice a případné spojovníky, například `text-editing`. Neplatný formát se odmítne při načtení konfigurace. Neznámé ID ukončí běh před společným setupem a vypíše dostupné ID. I při retake se ověřuje platnost celého plánu a všech automatických callbacků.

`VIDEO_SHOT` ovlivňuje pouze nahrávání prohlížeče. Příkazy `audio`, `head` a `video:plan` nadále zpracují celý plán.

### Všechny snímky do samostatných souborů

```shell
npm run video:shots video/308-pb-redesign.js
```

Příkaz načte `videoPlan` bez spuštění jeho kódu a postupně spustí každý záběr přes existující příkaz `video` s vlastním `VIDEO_SHOT`. Výstupy jsou například `308-pb-redesign-hierarchy.webm` a `308-pb-redesign-preview.webm` v `docs/feature-video`. Každý začíná svým titulkem SHOT bez přípravy. Pořadí, číslování a nastavení nahrávání zůstávají zachovány. Případná zděděná hodnota `VIDEO_SHOT` se pro každý záběr nahradí jeho ID.

Každý záběr má vlastní proces prohlížeče a společný setup, stejně jako ruční retake. Při chybě záběru dávka pokračuje dalšími; na konci vypíše neúspěšné ID a vrátí nenulový návratový kód. Přerušení procesu dávku zastaví. Typy `manual` a `head` vytvoří samostatné varovné tabulky, které je třeba při střihu nahradit; příkaz negeneruje placené audio ani video s mluvící postavou.

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

Příkaz spustí pouze scénář označený `@audio` přes samostatnou konfiguraci CodeceptJS. Neotevře prohlížeč, nepřihlásí uživatele a nespustí scénář videa ani plán záběrů. Výsledek ve formátu `mp3_44100_128` uloží do `docs/feature-video` v kořenové složce repozitáře. Soubory mají názvy `293-config-jstree-view-sk-1.mp3`, `293-config-jstree-view-sk-2.mp3` atp. podle zvoleného jazyka a počtu částí. Pořadové číslo je přítomno i při jediném souboru. Starší scénáře s textovým řetězcem namísto plánu vytvoří `293-config-jstree-view-1.mp3` bez označení jazyka.

Nápověda postupně spojuje celé texty snímků v pořadí `shots`. Když by přidáním dalšího záběru překročil limit modelu, začne novou část. Počítá také mezery a dva konce řádku mezi záběry. Pro `eleven_v3` je limit 5 000 znaků; jeden záběr se nikdy nerozdělí mezi soubory. Pokud samotný záběr překročí limit, kontrola před spuštěním vypíše jeho ID a počet znaků. Takový záběr je třeba v plánu rozdělit na menší záběry. Texty `manual` a `head` zůstávají součástí nadace, prázdné texty nevytvářejí prázdné soubory. Starší textový řetězec se považuje za jednu nedělitelnou část.

Před voláním ElevenLabs konzole vypíše číslo každé části, její záběry, počet znaků, název souboru a celý text pod označením `[ElevenLabs audio] Text to generate:`. Generování částí probíhá postupně; každý požadavek má časový limit 10 minut. `I.generateAudio` vrátí pole cest k souborům ve správném pořadí a zaregistruje je jako artefakty `audio-1`, `audio-2` atp. Na konci vypíše jeden souhrn kreditů za celé spuštění.

### API klíč ElevenLabs

1. Přihlaste se do ElevenLabs a otevřete **Developers > API Keys**.
2. Vytvořte omezený klíč, povolte mu `text_to_speech` a nastavte kreditní limit. Pro přehled kreditů povolte také `user_read` (čtení uživatelských údajů/předplatného); na mluvící videa i `image_video_generation`.
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

Nápověda ještě před voláním API ověří texty a vytvoří dočasné soubory pro všechny části. Při každém požadavku načte celou odpověď, ověří zvukový formát a až úplným dočasným souborem atomově nahradí příslušný MP3 soubor. Chyba zastaví další požadavky a uvede číslo části i název souboru. Již dokončené části zůstávají dostupné; předchozí soubory neúspěšných nebo nespuštěných částí se nemění. Požadavek se automaticky neopakuje, aby nejasná síťová chyba nezpůsobila druhé účtování kreditů. Při střihu používejte soubory vypsané aktuálním spuštěním: staré soubory bez čísla nebo nadbytečné části z předchozího delšího plánu se automaticky nemažou.


## Mluvící videa (`head`)

Společný `videoPlan` je statický JavaScript objekt. Pořadí jeho `shots` určuje číslování, časovou osu i pořadí mluveného slova. Každý snímek obsahuje jedinečné `id`, typ `auto`, `manual` nebo `head`, název `title`, kladný celočíselný odhad `durationSeconds` a lokalizovaný `text-sk`, případně `text-cs` a `text-en`. Automatické kroky patří do inline funkcí `shot` a volitelně `prepare`.

Typ `head` vytvoří samostatný klip s mluvící postavou. V hlavní nahrávce se na jeho místě zobrazí dvousekundová tabule `WARNING: head video` s číslem, názvem, celým lokalizovaným textem a poznámkami. Přeskočí se příprava, akce i cleanup tohoto záběru. Záběr zůstává součástí časové osy a číslovaných MP3 z `npm run audio` ; číslování i celkový počet v SETUP a titulcích zahrnují všechny typy záběrů. Například sedmý záběr ze šestnácti má `SETUP shot 7/16` i `Shot 7/16: ...`. Tabulku při střihu nahraďte vygenerovaným MP4.

```javascript
const videoPlan = {
    language: "sk",
    shots: [{
        id: "intro",
        type: "head",
        title: "Introduce the benefit",
        durationSeconds: 8,
        "text-sk": "Predstavujeme vám novinky vo WebJET CMS.",
        notes: "Insert the generated talking-head clip."
    }]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("ElevenLabs Head", ({ I }) => {
    I.generateHead(videoPlan);
}).tag("@head");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});
```

Za tyto scénáře patří hlavní scénář `@video` používající `recordVideoPlan`. `ElevenLabs Head` musí obsahovat jediné volání `I.generateHead` a pouze značku `@head`. Jeho runner používá stejnou statickou kontrolu zdrojového kódu jako audio; při čtení metadat neprovádí callbacky.

Ze složky `src/test/webapp` spusťte:

```shell
npm run head video/308-pb-redesign.js
```

Spustí se pouze `@head`, bez prohlížeče a přihlašování. Výchozí nastavení jsou obrázek **Jack / Home Vlog Style** připravený na poměr 16:9 v `video/assets/head/jack-home-vlog-style.png`, model `creatify-aurora` a explicitní rozlišení `720p`. TTS používá stejné výchozí hodnoty jako audio: `eleven_v3` a Luki Zajo, včetně proměnných `ELEVENLABS_MODEL_ID` a `ELEVENLABS_VOICE_ID`. Ostatní nastavení Aurora zůstávají výchozí. Dostupnost Image & Video API vyžaduje podporovaný placený program ElevenLabs (aktuálně Pro nebo vyšší).

Společná nastavení lze přepsat parametrem nápovědy:

```javascript
I.generateHead(videoPlan, {
    language: "en",
    imagePath: "assets/my-presenter.png",
    modelId: "creatify-aurora",
    resolution: "720p",
    audio: { modelId: "eleven_v3", voiceId: "<voice-id>" }
});
```

Jednotlivý snímek může obsahovat `head: { imagePath, modelId, resolution, audio: { modelId, voiceId } }`. Každá jeho hodnota přepíše společné nastavení; `language` se nastavuje pouze pro celý běh. Relativní `imagePath` se vyhodnotí vůči souboru scénáře. Podporovány jsou PNG, JPEG a WebP do 25 MB; poměr stran určíte referenčním obrázkem. Podporovaná rozlišení jsou `480p` a `720p`. Jiný `modelId` musí podporovat stejný Lip Sync vstup; ověřen je Aurora. Avatar a scénu veřejné API nevybírá podle jména, proto se používá uložený obrázek.

Před API voláními se ověří celý plán, překlady, neprázdné texty `head`, nastavení, obrázky a možnost vytvořit všechny výstupy. Plán bez `head` skončí informativně bez API volání. Záběry se zpracují postupně: samostatné TTS, odeslání obrázku a MP3 jako `inline_base64` do `POST /v1/flows/video`, kontrola `GET /v1/flows/video/{id}` a stažení MP4. API klíč se neposílá úložišti výsledků. Kontroly stavu mají intervaly 10, 20, 40 a poté 60 sekund; limit je 30 minut na video.

Výstupy jsou v ignorovaném `docs/feature-video`: `<scenario>-<shot-id>-<language>.mp3` a `.mp4`. Intro scénář 308 vytvoří `308-pb-redesign-intro-sk.mp4`. Každé spuštění generuje nově a spotřebovává kredity. Placené požadavky se automaticky neopakují. Chyba zastaví další snímky a při vytvořené úloze uvede její ID. Dokončené soubory se nahradí atomově; při chybě videa zůstává poslední úspěšný MP4 i již vygenerovaný nový MP3. Délku klipu určuje audio, `durationSeconds` je jen odhad pro střih. Automatické skládání finálního filmu není součástí generování.

Podrobnosti vstupů a stavů uvádí [ElevenLabs Video API](https://elevenlabs.io/docs/api-reference/flows/video/create).

## Spotřeba a zbývající kredity

Po `audio` i `head` se vypíše souhrn kreditů. Před a po generování se načte [předplatné](https://elevenlabs.io/docs/api-reference/user/subscription/get/). Zbývající kredity aktuálního limitu jsou `max(0, character_limit - character_count)`.

- **Audio:** skutečně účtovaná spotřeba pochází z hlavičky `character-cost` odpovědi TTS. Pokud chybí, výpis použije označený orientační rozdíl spotřeby účtu. Počet znaků vstupního textu se jako odhad nepoužívá.
- **Head:** orientační spotřeba je rozdíl `character_count` po a před celým během. Zahrnuje TTS i video všech zpracovaných záběrů. Souběžná aktivita účtu a opožděné účtování ji mohou ovlivnit.
- Souhrn se vypíše i po částečném selhání. Při nedokončené nebo nejisté vzdálené úloze je označen jako průběžný. Chyba načítání, chybějící údaje či oprávnění `user_read` zobrazí `unavailable` s důvodem. Změna fakturačního období zneplatní rozdíl spotřeby účtu; přímo účtovaná TTS spotřeba a platný aktuální zůstatek zůstávají použitelné.

Chyba reportování nezneplatní vytvořená média ani nepřekryje původní chybu generování. Záhlaví TTS popisuje [úvod k ElevenLabs API](https://elevenlabs.io/docs/api-reference/introduction).

## Ověření pomocníků bez placeného generování

```shell
npm run audio:test
npm run head:test
npm run video:test
CODECEPT_AUDIO_FILE="$PWD/video/308-pb-redesign.js" npx codeceptjs dry-run -c codecept.audio.conf.js --steps --grep '@audio'
CODECEPT_HEAD_FILE="$PWD/video/308-pb-redesign.js" npx codeceptjs dry-run -c codecept.head.conf.js --steps --grep '@head'
CODECEPT_VIDEO=true npx codeceptjs dry-run -c codecept.video.conf.js --steps -p autoLogin video/308-pb-redesign.js
```

Dry-run neprovádí generování ani kreditní API a nepotřebuje API klíč. Běžné video nahrávání, audio a head mají navzájem oddělené spouštění. Reálné generování ověřujte vědomě pro konkrétní scénář; po dokončení zkontrolujte obraz, zvuk a synchronizaci rtů.
