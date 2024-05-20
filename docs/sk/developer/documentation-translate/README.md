# Preklad dokumentácie

V tejto časti si rozoberieme, ako zabezpečiť preklad už existujúcej dokumentácie.

## deepmark

K automatizovanému prekladu súborov `markdown` využijeme dostupný bezplatný nástroj `deepmark`, ktorý využíva `DeepL` prekladač. Potrebné informácie k inštalácií ako aj bližšie informácie nájdete [tu](https://github.com/izznatsir/deepmark).

### Inštalácia

Samotný `deepmark` nainštaluje v lokácií, kde sa nachádza dokumentácia, čo je v našom prípade priečinok `docs`.

`npm install` - príkaz k inštalácií

### Premenná prostredia

Nakoľko `deepmark` využíva k prekladu prekladač `DeepL`, musíte nastaviť premennú prostredia `DEEPL_AUTH_KEY ` s autorizačným kľúčom k `DeepL`.

### Konfiguračný súbor `deepmark`

V konfiguračnom súbore nastavíme lokalitu, odkiaľ budeme `markdown` prekladať, ako aj lokalitu, kde sa tieto preložené súbory uložia. Taktiež zadefinujeme z akého a do akého jazyka sa má preklad vykonať.

**Pozor:** tento konfiguračný súbor **musí byť** nazvaný ako `deepmark.config.mjs` a musí byť v rovnakej lokácií, kde sme inštalovali `deepmark`. V našom prípade ide o priečinok `docs`.

```javascript
/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US'],
    directories: [
        ['sk', 'en']
    ]
};
```
**Pozor:** ak cieľová lokácie prekladov vytvorená nie je vytvorí sa automaticky. Ak cieľová lokácia už obsahuje súbory s rovnakým názvom budú prepísané. Inak budú vytvorené nové súbory.

### Spustenie prekladu

Pre samotným spustením si musíme povedať, že samotný preklad môže fungovať v troch režimoch `hybrid, offline, online`. Silne odporúčame používať mód `hybrid`, kde sa už preložené frázy uložia do lokálnej databázy, čím sa šetrí čaj ako aj počet použitých znakov.

**Pozor:** treba si uvedomiť, že počet prekladov je limitovaný a preto sa odporúča používať tento mód k šetreniu počtu prekladov.

Preklad vykonáte cez konzolu, kde v lokácií `docs` proces spustíte príkazom `npm run translate`. Po ukončení prekladu sa nevypíše žiadna hláška.

## Možný problém

Ak tento proces prekladu budete spúšťať na zariadení s OS typu Windows, môžete nastaviť v konzole na túto chybovú hlášku.

```
Error [ERR_UNSUPPORTED_ESM_URL_SCHEME]: Only URLs with a scheme in: file, data are supported by the default ESM loader. On Windows, absolute paths must be valid file:// URLs. Received protocol 'c:'
    at new NodeError (node:internal/errors:387:5)
    at throwIfUnsupportedURLScheme (node:internal/modules/esm/resolve:1116:11)
    at defaultResolve (node:internal/modules/esm/resolve:1196:3)
    at nextResolve (node:internal/modules/esm/loader:165:28)
    at ESMLoader.resolve (node:internal/modules/esm/loader:844:30)
    at ESMLoader.getModuleJob (node:internal/modules/esm/loader:431:18)
    at ESMLoader.import (node:internal/modules/esm/loader:528:22)
    at importModuleDynamically (node:internal/modules/esm/translators:110:35)
    at importModuleDynamicallyCallback (node:internal/process/esm_loader:35:14)
    at getThenResolveConfig (file:///C:/Interway/webjet8v9/docs/node_modules/deepmark/dist/cli.js:157:22) {
  code: 'ERR_UNSUPPORTED_ESM_URL_SCHEME'
}
```
Hláška bude tvrdiť, že cesta ku konfiguračnému súboru nie je platná.

**Riešenie**

V `docs` nájdete priečinok s modulmi s názvom `node_modules`. V tomto priečinku si otvoríte súbor na nasledujúcej ceste `docs/node_modules/deepmark/dist/cli.js`.

Na konci súboru, nájdete metódu `getThenResolveConfig`, kde tento problém nastal.

```javascript
async function getThenResolveConfig(path) {
  const configFilePath = path ? path.startsWith("/") ? path : np.resolve(process.cwd(), path) : np.resolve(process.cwd(), "deepmark.config.mjs");

  const userConfig = (await import(configFilePath)).default;

  return resolveConfig(userConfig);
}
```

Pred túto metódu pridáte novú metódu `getConfigFilePath` a následne upravíte tento kód.

```javascript
async function getConfigFilePath(path, doWindowsEdit = false) {
  let configFilePathTmp = path ? path.startsWith("/") ? path : np.resolve(process.cwd(), path) : np.resolve(process.cwd(), "deepmark.config.mjs");

  if(doWindowsEdit === true) {
    configFilePathTmp = configFilePathTmp.replaceAll("\\", "/");
    if(configFilePathTmp.startsWith("file://") === false) configFilePathTmp = "file://" + configFilePathTmp;
  }

  return configFilePathTmp;
}

async function getThenResolveConfig(path) {
  const configFilePath = await getConfigFilePath(path, true);

  const userConfig = (await import(configFilePath)).default;

  return resolveConfig(userConfig);
}
```

Metóda `getConfigFilePath` upraví cestu ku konfiguračnému súboru tak, aby to vyhovovalo Windows OS.

## Kopírovanie obrázkov

Jedna z vlastností prekladu, je skopírovanie všetkých nepreložiteľných súborov zo zdrojového priečinka do cieľového. Medzi tieto súbory sa radia `.png, .jpg, .jpge, .ico, .css` a ďalšie. Vlastne všetky súbory, ktoré nie sú typu `markdown`. Toto správanie môže byť výhodné, nakoľko nemusíte ručne kopírovať všetky závislé súbory.

**Upozornenie:** nevýhodou je fakt, že ak si do EN dokumentácie pridáte špecifický upravené obrázky pre anglickú variantu, tak sa pri každom preklade znovu prepíšu pôvodnými obrázkami zo slovenskej varianty.

**Riešenie**

Pôjdete do súboru nasledujúceho súboru `docs/node_modules/deepmark/dist/config.js`.

Okolo riadku 240 nájdete nasledujúce podmienky.

```javascript
if (path.endsWith(".md") || path.endsWith(".mdx")) {
  paths.md.push(filePaths);
  continue;
}
if (path.endsWith(".json")) {
  paths.json.push(filePaths);
  continue;
}
if (path.endsWith(".yaml") || path.endsWith(".yml")) {
  paths.yaml.push(filePaths);
  continue;
}
```
Riešením je pridanie novej podmienky, na preskočenie súbor s príponou pre obrázky napr. `.png, .jpg, .jpge`. Takto sa súbory s danými príponami nebudú pri preklade kopírovať.

```javascript
if (path.endsWith(".md") || path.endsWith(".mdx")) {
  paths.md.push(filePaths);
  continue;
}
if (path.endsWith(".json")) {
  paths.json.push(filePaths);
  continue;
}
if (path.endsWith(".yaml") || path.endsWith(".yml")) {
  paths.yaml.push(filePaths);
  continue;
}
//Exclude pictures
if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")) {
  continue;
}
```
## Problémový formát zdrojových súborov

Pri preklade sa môžete stretnúť s tým, že preklad buď zlyhá, alebo výsledný preklad rozbil štruktúru súboru. Tento problém môže nastať v prípade, že Vaše `markdown` súbory obsahujú syntax, ktorý `deepmark` nedokáže spracovať. V nasledujúcej časti si ukážeme niekoľko problémových syntaxí.

### Tabuľky

Štruktúra tabuliek sa pri preklade vždy rozbije a môže ohroziť preklad celého súboru. Preto nedoporučujeme využívať následný formát vo vašich súboroch.

```
| Metódy                            | Gettery               | Settery                   |
| -----------                       | -----------           | -----------               |
| [load()](#load)                   | [language](#language) | [urlLoad](#urlload)       |
| [onBeforeLoad()](#onbeforeload)   | [date](#date)         | [urlUpdate](#urlupdate)   |
| [onAfterLoad()](#onafterload)     |
| [translate()](#translate)         |
| [htmlTranslate()](#htmltranslate) |
```
Odporúčame použiť klasické zoznamy.

### Kombinovanie typov zoznamov

Ak skombinujete napríklad číselný zoznam s klasickým cez odrážky, text sa síce preloží, ale štruktúra zoznamu sa môže rozpadnúť. Preto nedopočujeme využívať následný formát.

```
1. aaaa
2. bbbb
   - cccc
   - dddd
```

Odporúčame použiť nekombinované typy zoznamov.

### Špeciálne znaky

Pokiaľ hovoríte o číselnej hodnote nepoužívajte znaky `<` a `>`. Radšej použite slovnú verziu menší/väčší. Alebo znaky citujte \``<`\` a \``>`\`.

### Po sebe idúce citácie

Ak pracujete s `markdown` súbormi viete, že využitím značiek \`\` sa stáva text medzi nimi citáciou, a takéto citácie sa neprekladajú. Problém nastáva, ak v texte nasledujú za sebou 2 alebo viac citácií, ktoré nie sú oddelené žiadnym symbolom.

Napr. \`Quote_1\` \`Quote_2\`. Takýto zápis môže vieš k zlyhaniu prekladu a preto odporúčame pridať medzi citácie nejaký znak, napr. \`Quote_1\`, \`Quote_2\`. Rovnaký problém nastáva aj pri iných kombináciách, ako sú napríklad citácie a linky.

Problémové kombinácie, ktorým sa musíte vyhnúť (na poradí nezáleží):
- \`Quote_1\` \`Quote_2\`
- \`Quote_1\` \[Text here\]\(Link here\)
- \*\*Bold\*\*  \`Quote_1\`
- \[Text here\]\(Link here\) \*\*Bold\*\*
- \[Text here\]\(Link here\) \_\[Text here\]\_
- \*\*Bold\*\* \_Text here\_

### Neoznačený kód

Veľký problém nastáva, ak zdrojové súbory obsahujú neoznačený programovací zdrojový kód. Takýto kód môže prerušiť preklad, alebo úplne pokaziť výsledný formát súboru. **Každý kód, musí byť riadne citovaný** a pridáme aj názov programovacieho jazyka, v ktorom vznikol.

\`javascript
 Váš kód sem.
\`

\`html
 Váš kód sem.
\`

\`java
 Váš kód sem.
\`

## Chybné formátovanie preloženého súboru

Môže nastať situácia, že všetko ste splnili ako ste mali, ale výsledný preložený súbor má aj tak chybné formátovanie. Na vine je samotný `deepmark`, ktorý má s týmto problém.

Patrí sem:
- redundantné pridávanie riadkov medzi možnosti zoznamu
- poškodenie/zlé uzavretie `iframe` značky
- ...

**Riešenie**

Riešenie opäť implementujeme v súbore `docs/node_modules/deepmark/dist/config.js`. Nájdite nasledujúci kód, ktorý ukladá do výstupného súboru `markdown`.

```javascript
await fs.outputFile(
  outputFilePath.replace(/\$langcode\$/, targetLanguage),
  getMarkdown(_mdast),
  { encoding: "utf-8" }
);
```

Tento `markdown` upravte tak, ako to Vám vyhovuje. Prikladáme možné riešenie, ktoré naformátuje výsledný `markdown` tak, aby sa podobal čo najviac originálnemu súboru.

```javascript
let markdown = getMarkdown(_mdast);
//remove redundant new lines
markdown = markdown.replace(/(^[\S]*\*.*)\n\n/gm, '$1\n');
markdown = markdown.replace(/(^[\s]*\*.*)\n\n/gm, '$1\n');
//replace * with - (and remove redundant spaces) to deep of 4 levels
markdown = markdown.replace(/(\n\*[\s]{3})(.*)/gm,         '\n- $2');
markdown = markdown.replace(/(\n[\s]{4}\*[\s]{3})(.*)/gm,  '\n\t- $2');
markdown = markdown.replace(/(\n[\s]{8}\*[\s]{3})(.*)/gm,  '\n\t\t- $2');
markdown = markdown.replace(/(\n[\s]{12}\*[\s]{3})(.*)/gm, '\n\t\t\t- $2');
//Headline fix - add new line before headline
markdown = markdown.replace(/(^[\S]*-[\S]*.*)\n([\#]+.*)/gm, '$1\n\n$2'); //fix bold headlines
//Fix Bold healines - add new line before and after bold text (that is headline)
markdown = markdown.replace(/([^\n])\n(^\*\*[0-1a-zA-Z ]+\*\*)/gm, '$1\n\n$2');
markdown = markdown.replace(/(^\*\*[0-1a-zA-Z ]+\*\*)\n([^\n])/gm, '$1\n\n$2');
//Fix image links - add new line before and after image (if necessary)
markdown = markdown.replace(/([^\n])\n(^!\[\]\([^()]+\))/gm, '$1\n\n$2');
markdown = markdown.replace(/(^!\[\]\([^()]+\))\n([^\n])/gm, '$1\n\n$2');
//Fix image tags - add new line before and after image (if necessary)
markdown = markdown.replace(/([^\n])\n(^<img.*\/>)/gm, '$1\n\n$2');
markdown = markdown.replace(/(^<img.*\/>)\n([^\n])/gm, '$1\n\n$2');
//Fix list header, so between list and header is NOT line and list header is separated from rest of text
/*
 * Something something:
  * - something
  * - something
*/
markdown = markdown.replace(/(^.*:\n)\n(^[\s]*-)/gm, '$1$2');
markdown = markdown.replace(/([^\n])\n(^.*:\n)/gm, '$1\n\n$2');
//FIX - markdown gonna fuck the iframe end tag, need to be fixed or everything after this error not gonna show
markdown = markdown.replace(/(^<div class="video-container">\n[\s]*)(<iframe.*)\/>(\n<\/div>)/gm, '$1$2></iframe>$3');
//FIX fucking end tags
markdown = markdown.replace(/(<[^<>]*)[\s]>/gm, '$1>');
//Special case, if file start with * (in _sidebar.md)
markdown = markdown.replace(/^\*[\s]*([^*]*)\n/gm, '- $1');
//Special case, \[ to [ (in ROADMAP.md
//For safety, we will replace only if there is combination '- \[ ]' or '- \[x]' at START of line
markdown = markdown.replace(/^-\s\\\[\s\]/gm, '- [ ]');
markdown = markdown.replace(/^-\s\\\[x\]/gm, '- [x]');

//Special case for OLD changelog - 2020
markdown = markdown.replace(/^-\s\\\[([a-zA-Z ]+])/gm, '- [$1'); //First replace - \[TEXT AND space
markdown = markdown.replace(/^-\s\\\#([0-9]+)/gm, '- #$1'); //Second replace - \#NUMBER
markdown = markdown.replace(/^[\s]*\\\#([0-9]+)/gm, '#$1'); //Third replace \#NUMBER
markdown = markdown.replace(/(^-\s#[0-9]+)\s\\\[([a-zA-Z ]+])/gm, '$1 [$2'); //Fourth replace - #NUMBER \[TEXT an spac
await fs.outputFile(
  outputFilePath.replace(/\$langcode\$/, targetLanguage),
  markdown,
  { encoding: "utf-8" }
);
```