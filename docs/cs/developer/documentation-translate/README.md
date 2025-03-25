# Překlad dokumentace

V této části si rozebereme, jak zajistit překlad již existující dokumentace.

## Deepmark

K automatizovanému překladu souborů `markdown` využijeme **vlastní verzi** dostupného bezplatného nástroje [deepmark](https://github.com/izznatsir/deepmark), který využívá [DeepL](https://www.deepl.com/cs/translator) překladač.

Tuto vlastní upravenou verzi originálního `Deepmark` najdete publikovanou jako [@webjetcms/deepmark](https://www.npmjs.com/package/@webjetcms/deepmark).

### Instalace

Samotný `deepmark` nainstalujete v lokaci, kde se nachází dokumentace, což je v našem případě složka `docs`.

Nainstalujete ho příkazem `npm i @webjetcms/deepmark`, čímž se nainstaluje právě aktuální verze.

**NEBO**

Chcete-li využívat konkrétní verzi můžete jej přidat jako `dependencie` do souboru `package.json` ve složce s dokumentací jako

```json
"dependencies": {
  "@webjetcms/deepmark": "^0.1.4"
}
```

a následně jej nainstalovat příkazem `npm install`.

### Proměnná prostředí

Nakolik `deepmark` využívá k překladu překladač `DeepL`, musíte nastavit proměnnou prostředí `DEEPL_AUTH_KEY ` s autorizačním klíčem k ` DeepL`.

### Konfigurační soubor `deepmark`

V konfiguračním souboru nastavíme web, odkud budeme `markdown` překládat i web, kde se tyto přeložené soubory uloží. Také definujeme z jakého a do jakého jazyka se má překlad provést.

!>**Upozornění:** tento konfigurační soubor **musí být** nazvaný jako `deepmark.config.mjs` a musí být ve stejné lokaci, kde jsme instalovali `deepmark`. V našem případě se jedná o složku `docs`.

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

!>**Upozornění:** pokud cílová lokace překladů vytvořena není vytvoří se automaticky. Pokud cílová lokace již obsahuje soubory se stejným názvem budou přepsány. Jinak budou vytvořeny nové soubory.

## Spuštění překladu

Pro samotným spuštěním si musíme říci, že samotný překlad může fungovat ve třech režimech `hybrid, offline, online`. Silně doporučujeme používat mód `hybrid`, kde se již přeložené fráze uloží do lokální databáze, čímž se šetří čaj i počet použitých znaků.

!>**Upozornění:** je třeba si uvědomit, že počet překladů je limitován a proto se doporučuje používat tento mód k šetření počtu překladů.

Překlad provedete přes konzoli, kde v lokaci `docs` proces spustíte příkazem `npm run translate`. V konzole byste měli vidět informace, jak se jednotlivé soubory překládají. Také je vypsán **začátek** a **konec** celého procesu překladu.

![](translation-log.PNG)

## Rozdíly mezi `deepmark` a `@webjetcms/deepmark`

Mezi naší upravenou a originální verzí je několik důležitých rozdílů, které si rozebereme v jednotlivých podkapitolách.

Téměř všechna naše vlastní logika je v námi vytvořeném souboru na adrese `docs\node_modules\@webjetcms\deepmark\dist\webjet-logic.js`.

### Kopírování obrázků

Jedna z vlastností překladu, je zkopírování všech nepřeložitelných souborů ze zdrojové složky do cílové. Mezi tyto soubory se řadí `.png, .jpg, .jpge, .ico, .css` a další. Vlastně všechny soubory, které nejsou typu `markdown`. Toto chování může být výhodné, protože nemusíte ručně kopírovat všechny závislé soubory.

!>**Upozornění:** nevýhodou je fakt, že pokud si do EN dokumentace přidáte specifický upravené obrázky pro anglickou variantu, tak se při každém překladu znovu přepíší původními obrázky ze slovenské varianty. Z tohoto důvodu byla tato logika upravena tak, aby se při překladu **obrázky nikdy nekopírovali**.

Úprava byl aplikována v souboru `docs/node_modules/deepmark/dist/config.js`, kolem řádku 240. Přidána byla nová podmínka, k přeskočení soubor s příponou pro obrázky Např. `.png, .jpg, .jpge`. Takto se soubory s danými příponami nebudou při překladu kopírovat.

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

### Přeskočení překladu části souboru

Pokud máte v `markdown` souboru nějaké části, které se nemají překládat jako například kódy, samotný `markdown` nabízí možnost přeskočit překlad části souboru, pokud text je obalen v **\`text\`** nebo kód obalený jako **\`\`\`kód\`\`\`**. Problém nastává v případě, že kód v souboru neslouží pro ilustrační účely, ale má provést nějakou logiku.

!>**Upozornění:**, v případě že ho neobalíte jako **\`\`\`kód\`\`\`**, tak se tento kód může částečně přeložit, čímž se stane nefunkční. Pokud ten kód obalíte jako **\`\`\`kód\`\`\`**, nepřeloží se ale **ani se neprovede**.

**Řešením** bylo vytvoření vlastních značek `<!-- deepmark-ignore-start -->` a `<!-- deepmark-ignore-end -->`, které zajišťují, že kódy (nebo i text) obalené v těchto značkách se nepřeloží **ale** samotný kód se provede. Logika za těmito značkami ještě před samotným překladem vybere označené části kódu ze souboru a teprve po překladu je vloží zpět do souboru a to **beze změny**.

```javascript
<!-- deepmark-ignore-start -->
YOUR CODE HERE PLEASE
<!-- deepmark-ignore-end -->
```

**Výpis přeskočených částí**

Pokud se přeskakuje výpis části souboru pomocí zmíněných značek, do konzoly se daná přeskočená část automaticky vypíše. Pokud by tato funkce byla nežádoucí, je třeba v souboru `docs\node_modules\@webjetcms\deepmark\dist\cli.js` odstranit volání funkce `logIgnoredContentInfo(ignoredContent);`, která zajišťuje výpis přeskočených částí.

### Formátování souboru

Originální `deepmark` má problém se správným formátováním přeložených souborů. Přidává nadbytečné mezery/řádky, nepřeloží korektně některé symboly nebo špatným odsazením zničí strukturu seznamů či tabulek. Tento problém je v naší verzi `@webjetcms/deepmark` řešen několika kroky aby se zabránilo těmto chybám v přeložených souborů. Nebude rozebírat všechny úpravy, ale vzpomeneme si, že nejdůležitější je metoda `customizeTranslatedMarkdown`, která upravuje/formátuje již přeložené soubory v několika krocích.

### Výběr služby poskytující překlady

V konfiguračním souboru **`deepmark.config.mjs`** můžeme nastavit službu poskytující překlady pomocí proměnné **`translationEngine`**. Tato proměnná může nabývat jedné ze dvou hodnot:
- **[`deepl`](https://www.deepl.com/docs-api) ** – ** DeepL** výchozí služba, pokud proměnná není nastavena
- **[`google`](https://cloud.google.com/translate) ** – ** Google Translate API** alternativní služba pro překlady

Pokud proměnná **`translationEngine`** není definována, systém automaticky použije **DeepL**.

#### Proměnná prostředí

Chcete-li použít **Google Translate API**, musíte nastavit proměnnou prostředí `GOOGLE_AUTH_KEY` s autorizačním klíčem ke **Google Translate API**.

#### Příklad konfigurace

Níže je ukázka konfigurace s použitím **Google Translate**:

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

### Verze závislostí

Nakolik `deepmark` využívá závislosti, jejichž verze jsou již zastaralé, aktualizovali jsme většinu závislostí na novější verze. Ne všude jsme použili nejvíce aktuální verze, jelikož ne všechny byly kompatibilní buď s jinými závislostmi nebo se samotným použitým formátem v `deepmark`.

Seznam **původních** závislostí:

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

Seznam **aktualizováno** závislostí:

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

## Problémový formát zdrojových souborů

Při překladu se můžete setkat s tím, že výsledný překlad rozbil strukturu souboru. Tento problém může nastat v případě, že Vaše `markdown` soubory obsahují syntax, který `deepmark` nedokáže zpracovat. V následující části si ukážeme několik problémových syntaxí.

!>**Upozornění:**, mnohé problémy se vyřešily ve verzi `@webjetcms/deepmark`, avšak stále jsou situace, kdy určitý formát souboru způsobí problémy při překladu a následném formátování.

### Kombinování typů seznamů

Pokud zkombinujete například číselný seznam s klasickým přes odrážky, text se sice přeloží, ale struktura seznamu se **může** rozpadnout. Proto nedoslechneme využívat jejich kombinaci.

```javascript
1. aaaa
2. bbbb
   - cccc
   - dddd
```

Doporučujeme použít nekombinované typy seznamů.

### Speciální znaky

Vaše soubory mohou obsahovat mnoho speciálních znaků, které mají v souborech `markdown` speciální roli. Takové speciální znaky mohou následně poškodit formát. Doporučujeme takové znaky **vždy** citovat.

Např. pokud mluvíte o číselné hodnotě nepoužívejte znaky `<` a `>`. Raději použijte slovní verzi menší/větší. Nebo znaky citujte \``<`\` a \``>`\`.

### Po sobě jdoucí citace

Pokud pracujete s `markdown` soubory víte, že využitím značek **\`\`** se stává text mezi nimi citací a takové citace se nepřekládají. Problém nastává, pokud v textu následují za sebou 2 nebo více citací, které nejsou odděleny žádným symbolem.

Např. **\`Quote\_1\` \`Quote\_2\`**. Takový zápis může víš k selhání překladu a proto doporučujeme přidat mezi citace nějaký znak, například. **\`Quote\_1\`**, **\`Quote\_2\`**, nebo citace spojit **\`Quote\_1 Quote\_2\`**, případně **\`Quote\_1, Quote\_2\`**. Stejný problém nastává iu jiných kombinací, jako jsou například citace a linky.

Problémové kombinace, kterým se musíte vyhnout (na pořadí nezáleží):

```
- `Quote_1` `Quote_2\`
- `Quote_1` [Text here](Link here)
- **Bold** `Quote_1`
- [Text here](Link here) **Bold**
- [Text here](Link here) _[Text here\]_
- **Bold** _Text here_
```

### Neoznačený kód

Velký problém nastává, pokud zdrojové soubory obsahují neoznačený programovací zdrojový kód. Takový kód může přerušit překlad, nebo úplně pokazit výsledný formát souboru. **Každý kód, musí být řádně citován** a přidáme i název programovacího jazyka, ve kterém vznikl.

***

\`\`\`javascript

VÁŠ KÓD ZDE

\`\`\`

***

\`\`\`html

VÁŠ KÓD ZDE

\`\`\`

***

\`\`\`java

VÁŠ KÓD ZDE

\`\`\`

***

### Nevědomost `markdown` formátu

Nejčastější chyba bude asi nesprávný formát z důvodu nevědomosti formátu souborů `markdown`. O základní syntaxi se můžete dočíst na stránce [Basic Syntax](https://www.markdownguide.org/basic-syntax/).
