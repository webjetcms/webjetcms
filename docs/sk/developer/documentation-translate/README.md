# Preklad dokumentácie

V tejto časti si rozoberieme, ako zabezpečiť preklad už existujúcej dokumentácie.

## Deepmark

K automatizovanému prekladu súborov `markdown` využijeme **vlastnú verziu** dostupného bezplatného nástroja [deepmark](https://github.com/izznatsir/deepmark), ktorý využíva [DeepL](https://www.deepl.com/cs/translator) prekladač.

Túto vlastnú upravenú verziu originálneho `Deepmark` nájdete publikovanú ako [@webjetcms/deepmark](https://www.npmjs.com/package/@webjetcms/deepmark).

### Inštalácia

Samotný `deepmark` nainštalujete v lokácií, kde sa nachádza dokumentácia, čo je v našom prípade priečinok `docs`.

Nainštalujete ho príkazom `npm i @webjetcms/deepmark`, čím sa nainštaluje práve aktuálna verzia.

**ALEBO**

Ak chcete využívať konkrétnu verziu môžete ho pridať ako `dependencie` do súboru `package.json` v priečinku s dokumentáciou ako

```json
"dependencies": {
  "@webjetcms/deepmark": "^0.1.4"
}
```

a následne ho nainštalovať príkazom `npm install`.

### Premenná prostredia

Nakoľko `deepmark` využíva k prekladu prekladač `DeepL`, musíte nastaviť premennú prostredia `DEEPL_AUTH_KEY ` s autorizačným kľúčom k `DeepL`.

### Konfiguračný súbor `deepmark`

V konfiguračnom súbore nastavíme lokalitu, odkiaľ budeme `markdown` prekladať, ako aj lokalitu, kde sa tieto preložené súbory uložia. Taktiež zadefinujeme z akého a do akého jazyka sa má preklad vykonať.

!>**Upozornenie:** tento konfiguračný súbor **musí byť** nazvaný ako `deepmark.config.mjs` a musí byť v rovnakej lokácií, kde sme inštalovali `deepmark`. V našom prípade ide o priečinok `docs`.

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
!>**Upozornenie:** ak cieľová lokácie prekladov vytvorená nie je vytvorí sa automaticky. Ak cieľová lokácia už obsahuje súbory s rovnakým názvom budú prepísané. Inak budú vytvorené nové súbory.

## Spustenie prekladu

Pre samotným spustením si musíme povedať, že samotný preklad môže fungovať v troch režimoch `hybrid, offline, online`. Silne odporúčame používať mód `hybrid`, kde sa už preložené frázy uložia do lokálnej databázy, čím sa šetrí čaj ako aj počet použitých znakov.

!>**Upozornenie:** treba si uvedomiť, že počet prekladov je limitovaný a preto sa odporúča používať tento mód k šetreniu počtu prekladov.

Preklad vykonáte cez konzolu, kde v lokácií `docs` proces spustíte príkazom `npm run translate`. V konzole by ste mali vidieť informácie, ako sa jednotlivé súbory prekladajú. Taktiež je vypísaný **začiatok** a **koniec** celého procesu prekladu.

![](translation-log.PNG)

## Rozdiely medzi `deepmark` a `@webjetcms/deepmark`

Medzi našou upravenou a originálnou verziou je niekoľko dôležitých rozdielov, ktoré si rozoberieme v jednotlivých pod-kapitolách.

Takmer všetka naša vlastná logika je v nami vytvorenom súbore na adrese `docs\node_modules\@webjetcms\deepmark\dist\webjet-logic.js`.

### Kopírovanie obrázkov

Jedna z vlastností prekladu, je skopírovanie všetkých nepreložiteľných súborov zo zdrojového priečinka do cieľového. Medzi tieto súbory sa radia `.png, .jpg, .jpge, .ico, .css` a ďalšie. Vlastne všetky súbory, ktoré nie sú typu `markdown`. Toto správanie môže byť výhodné, nakoľko nemusíte ručne kopírovať všetky závislé súbory.

!>**Upozornenie:** nevýhodou je fakt, že ak si do EN dokumentácie pridáte špecifický upravené obrázky pre anglickú variantu, tak sa pri každom preklade znovu prepíšu pôvodnými obrázkami zo slovenskej varianty. Z tohto dôvodu bola táto logika upravená tak, aby sa pri preklade **obrázky nikdy nekopírovali**.

Úprava bol aplikovaná v súbore `docs/node_modules/deepmark/dist/config.js`, okolo riadku 240. Pridaná bola nová podmienka, na preskočenie súbor s príponou pre obrázky napr. `.png, .jpg, .jpge`. Takto sa súbory s danými príponami nebudú pri preklade kopírovať.

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

### Preskočenie prekladu časti súboru

Ak mate v `markdown` súbore nejaké časti, ktoré sa nemajú prekladať ako napríklad kódy, samotný `markdown` ponúka možnosť preskočiť preklad časti súboru, ak text je obalený v **\`text\`** alebo kód obalený ako **\`\`\`kód\`\`\`**. Problém nastáva v prípade, že kód v súbore neslúži na ilustračné účely, ale má vykonať nejakú logiku.

!>**Upozornenie:**, v prípade že ho neobalíte ako **\`\`\`kód\`\`\`**, tak sa tento kód môže čiastočne preložiť, čím sa stane nefunkčný. Ak ten kód obalíte ako **\`\`\`kód\`\`\`**, nepreloží sa ale **ani sa nevykoná**.

**Riešením** bolo vytvorenie vlastných značiek `<!-- deepmark-ignore-start -->` a `<!-- deepmark-ignore-end -->`, ktoré zabezpečujú, že kódy (alebo aj text) obalené v týchto značkách sa nepreložia **ale** samotný kód sa vykoná. Logika za týmito značkami ešte pred samotným prekladom vyberie označené časti kódu zo súboru a až po preklade ich vloží späť do súboru a to **bezo zmeny**.

```javascript
<!-- deepmark-ignore-start -->
YOUR CODE HERE PLEASE
<!-- deepmark-ignore-end -->
```

**Výpis preskočených častí**

Ak sa preskakuje výpis časti súboru pomocou spomenutých značiek, do konzoly sa daná preskočená časť automatický vypíše. Ak by táto funkcia bola nežiadúca, je potrebné v súbore `docs\node_modules\@webjetcms\deepmark\dist\cli.js` odstrániť volanie funkcie `logIgnoredContentInfo(ignoredContent);`, ktorá zabezpečuje výpis preskočených častí.

### Formátovanie súboru

Originálny `deepmark` má problém so správnym formátovaním preložených súborov. Pridáva nadbytočné medzery/riadky, nepreloží korektne niektoré symboly alebo zlým odsadením zničí štruktúru zoznamov či tabuliek. Tento problém je v našej verzií `@webjetcms/deepmark` riešený niekoľkými krokmi aby sa zabránilo týmto chybám v preložených súborov. Nebude rozoberať všetky úpravy ale spomenieme si, že najdôležitejšia je metóda `customizeTranslatedMarkdown`, ktorá upravuje/formátuje už preložené súbory v niekoľkých krokoch.

### Výber služby poskytujúcej preklady

V konfiguračnom súbore **`deepmark.config.mjs`** môžeme nastaviť službu poskytujúcu preklady pomocou premennej **`translationEngine`**. Táto premenná môže nadobúdať jednu z dvoch hodnôt:

- **[`deepl`](https://www.deepl.com/docs-api)** – **DeepL** predvolená služba, ak premenná nie je nastavená
- **[`google`](https://cloud.google.com/translate)** – **Google Translate API** alternatívna služba pre preklady

Ak premenná **`translationEngine`** nie je definovaná, systém automaticky použije **DeepL**.

#### Premenná prostredia

Ak chcete použiť **Google Translate API**, musíte nastaviť premennú prostredia `GOOGLE_AUTH_KEY` s autorizačným kľúčom ku **Google Translate API**.

#### Príklad konfigurácie

Nižšie je ukážka konfigurácie s použitím **Google Translate**:

```javascript
/** @type {import("deepmark").UserConfig} */
export default {
    sourceLanguage: 'sk',
    outputLanguages: ['en-US'],
    directories: [
        ['sk', '$langcode$'],
    ],
    translationEngine: "google"    // Voľba prekladovej služby
};
```

### Verzie závislostí

Nakoľko `deepmark` využíva závislosti, ktorých verzie sú už zastarané, aktualizovali sme väčšinu závislostí na novšie verzie. Nie všade sme použili najviac aktuálne verzie, nakoľko nie všetky boli kompatibilné buď s inými závislosťami alebo so samotným použitým formátom v `deepmark`.

Zoznam **pôvodných** závislostí:

```json
"dependencies": {
		"acorn": "^8.8.2",
		"acorn-jsx": "^5.3.2",
		"astring": "^1.8.4",
		"better-sqlite3": "^8.0.1",
		"commander": "^10.0.0",
		"deepl-node": "^1.8.0",
		"fs-extra": "^11.1.0",
		"mdast-util-from-markdown": "^1.3.0",
		"mdast-util-frontmatter": "^1.0.1",
		"mdast-util-html-comment": "^0.0.4",
		"mdast-util-mdx": "^2.0.1",
		"mdast-util-to-markdown": "^1.5.0",
		"micromark-extension-frontmatter": "^1.0.0",
		"micromark-extension-html-comment": "^0.0.1",
		"micromark-extension-mdxjs": "^1.0.0",
		"prettier": "^2.8.3",
		"yaml": "^2.2.1"
	},
```

Zoznam **aktualizovaných** závislostí:

```json
"dependencies": {
	"acorn": "^8.12.1",
	"acorn-jsx": "^5.3.2",
	"astring": "^1.8.6",
	"better-sqlite3": "^8.7.0",
	"commander": "^11.1.0",
	"deepl-node": "^1.13.1",
	"fs-extra": "^11.2.0",
	"mdast-util-from-markdown": "^2.0.1",
	"mdast-util-frontmatter": "^2.0.1",
	"mdast-util-html-comment": "^0.0.4",
	"mdast-util-mdx": "^3.0.0",
	"mdast-util-to-markdown": "^2.1.0",
	"micromark-extension-frontmatter": "^2.0.0",
	"micromark-extension-html-comment": "^0.0.1",
	"micromark-extension-mdxjs": "^3.0.0",
	"prettier": "^3.3.3",
	"yaml": "^2.5.0",
	"@google-cloud/translate": "^8.5.1"
},
```

## Problémový formát zdrojových súborov

Pri preklade sa môžete stretnúť s tým, že výsledný preklad rozbil štruktúru súboru. Tento problém môže nastať v prípade, že Vaše `markdown` súbory obsahujú syntax, ktorý `deepmark` nedokáže spracovať. V nasledujúcej časti si ukážeme niekoľko problémových syntaxí.

!>**Upozornenie:**, mnohé problémy sa vyriešili vo verzií `@webjetcms/deepmark`, avšak stále sú situácie, kedy určitý formát súboru spôsobí problémy pri preklade a následnom formátovaní.

### Kombinovanie typov zoznamov

Ak skombinujete napríklad číselný zoznam s klasickým cez odrážky, text sa síce preloží, ale štruktúra zoznamu sa **môže** rozpadnúť. Preto nedopočujeme využívať ich kombináciu.

```javascript
1. aaaa
2. bbbb
   - cccc
   - dddd
```

Odporúčame použiť nekombinované typy zoznamov.

### Špeciálne znaky

Vaše súbory môžu obsahovať mnohé špeciálne znaky, ktoré majú v súboroch `markdown` špeciálnu rolu. Takéto špeciálne znaky môžu následne poškodiť formát. Odporúčame takéto znaky **vždy** citovať.

Napr. pokiaľ hovoríte o číselnej hodnote nepoužívajte znaky `<` a `>`. Radšej použite slovnú verziu menší/väčší. Alebo znaky citujte \``<`\` a \``>`\`.

### Po sebe idúce citácie

Ak pracujete s `markdown` súbormi viete, že využitím značiek **\`\`** sa stáva text medzi nimi citáciou a takéto citácie sa neprekladajú. Problém nastáva, ak v texte nasledujú za sebou 2 alebo viac citácií, ktoré nie sú oddelené žiadnym symbolom.

Napr. **\`Quote_1\` \`Quote_2\`**. Takýto zápis môže vieš k zlyhaniu prekladu a preto odporúčame pridať medzi citácie nejaký znak, napr. **\`Quote_1\`**, **\`Quote_2\`**, alebo citácie spojiť **\`Quote_1 Quote_2\`**, prípadne **\`Quote_1, Quote_2\`**. Rovnaký problém nastáva aj pri iných kombináciách, ako sú napríklad citácie a linky.

Problémové kombinácie, ktorým sa musíte vyhnúť (na poradí nezáleží):


```
- `Quote_1` `Quote_2\`
- `Quote_1` [Text here](Link here)
- **Bold** `Quote_1`
- [Text here](Link here) **Bold**
- [Text here](Link here) _[Text here\]_
- **Bold** _Text here_
```

### Neoznačený kód

Veľký problém nastáva, ak zdrojové súbory obsahujú neoznačený programovací zdrojový kód. Takýto kód môže prerušiť preklad, alebo úplne pokaziť výsledný formát súboru. **Každý kód, musí byť riadne citovaný** a pridáme aj názov programovacieho jazyka, v ktorom vznikol.

---

\`\`\`javascript

VÁŠ KÓD TU

\`\`\`

---

\`\`\`html

VÁŠ KÓD TU

\`\`\`

---

\`\`\`java

VÁŠ KÓD TU

\`\`\`

---

### Nevedomosť `markdown` formátu

Najčastejšia chyba bude asi nesprávny formát z dôvodu nevedomosti formátu súborov `markdown`.  O základnej syntaxi sa môžete dočítať na stránke [Basic Syntax](https://www.markdownguide.org/basic-syntax/).