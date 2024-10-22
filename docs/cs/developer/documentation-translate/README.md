# Překlad dokumentace

V této části se budeme zabývat tím, jak zajistit překlad stávající dokumentace.

## Deepmark

Pro automatický překlad souborů `markdown` používáme **vlastní verze** zdarma dostupný nástroj [deepmark](https://github.com/izznatsir/deepmark) který používá [DeepL](https://www.deepl.com/cs/translator) Překladatel.

Tato vlastní upravená verze původního `Deepmark` najdete zveřejněné jako [@webjetcms/deepmark](https://www.npmjs.com/package/@webjetcms/deepmark).

### Instalace

Stejné `deepmark` instalovat do umístění, kde je umístěna dokumentace, což je v našem případě složka `docs`.

Chcete-li jej nainstalovat, použijte příkaz `npm i @webjetcms/deepmark`, který nainstaluje aktuální verzi.

**NEBO**

Pokud chcete použít konkrétní verzi, můžete ji přidat jako `dependencie` do souboru `package.json` ve složce dokumentace jako

```json
"dependencies": {
  "@webjetcms/deepmark": "^0.1.4"
}
```

a poté jej nainstalujte příkazem `npm install`.

### Proměnná prostředí

Vzhledem k tomu, že `deepmark` používá překladatele k překladu `DeepL`, je třeba nastavit proměnnou prostředí `DEEPL_AUTH_KEY ` s autorizačním klíčem na ` DeepL`.

### Konfigurační soubor `deepmark`

V konfiguračním souboru nastavíme místo, odkud budeme `markdown` přeložit, a také umístění, kde budou tyto přeložené soubory uloženy. Rovněž určíme, z jakého jazyka a do jakého jazyka má být překlad proveden.

**Varování:** tento konfigurační soubor **musí být** nazvaný jako `deepmark.config.mjs` a musí být na stejném místě, kde jsme nainstalovali `deepmark`. V našem případě je to složka `docs`.

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

**Varování:** pokud není cílové místo překladu vytvořeno, vytvoří se automaticky. Pokud cílové umístění již obsahuje soubory se stejným názvem, budou přepsány. V opačném případě budou vytvořeny nové soubory.

## Zahájení překladu

Než začneme, musíme říci, že samotný překlad může pracovat ve třech režimech. `hybrid, offline, online`. Důrazně doporučujeme používat režim `hybrid` kde jsou již přeložené fráze uloženy v místní databázi, což šetří jak čaj, tak počet použitých znaků.

**Varování:** Všimněte si, že počet překladů je omezen, a proto se doporučuje používat tento režim pro úsporu počtu překladů.

Překlad provedete prostřednictvím konzoly, kde v umístění `docs` pro spuštění procesu použijte příkaz `npm run translate`. V konzole by se měly zobrazit informace o tom, jak jsou jednotlivé soubory překládány. Zobrazí se také seznam **Start** a **Konec** celý proces překladu.

![](translation-log.PNG)

## Rozdíly mezi `deepmark` a `@webjetcms/deepmark`

Mezi naší upravenou verzí a původní verzí je několik důležitých rozdílů, které si rozebereme v jednotlivých podkapitolách.

Téměř veškerá naše vlastní logika je v souboru, který jsme vytvořili na adrese `docs\node_modules\@webjetcms\deepmark\dist\webjet-logic.js`.

### Kopírování obrázků

Jednou z funkcí překladu je zkopírování všech nepřeložitelných souborů ze zdrojové složky do cílové složky. Mezi tyto soubory patří `.png, .jpg, .jpge, .ico, .css` a další. Ve skutečnosti všechny soubory, které nejsou typu `markdown`. Toto chování může být výhodné, protože nemusíte ručně kopírovat všechny závislé soubory.

**Varování:** nevýhodou je, že pokud do EN dokumentace přidáte konkrétní upravené obrázky pro anglickou verzi, budou při každém překladu přepsány původními obrázky ze slovenské verze. Z tohoto důvodu byla tato logika upravena tak, že při překladu **nikdy nekopírovali obrázky**.

Úprava byla použita v souboru `docs/node_modules/deepmark/dist/config.js`, kolem řádku 240. Byla přidána nová podmínka pro vynechání souboru s příponou pro obrázky, např. `.png, .jpg, .jpge`. Tímto způsobem se soubory s danými příponami při překladu nezkopírují.

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

Pokud se páříte v `markdown` některé části souboru, které nemají být přeloženy, jako jsou kódy,... `markdown` nabízí možnost přeskočit překlad části souboru, pokud je text zabalen do **\`text\`** nebo kód zabalený jako **\`\`Kód\`\`**. Problém nastává, když kód v souboru neslouží k ilustračním účelům, ale k provedení nějaké logiky.

**Varování:**, v případě, že ji nezabalíte jako **\`\`Kód\`\`**, může být kód částečně přeložen, čímž se stane nefunkčním. Pokud kód zabalíte jako **\`\`Kód\`\`**, ale nepřekládá se **ani se nesmí provádět**.

**Řešení** byla tvorba vlastních značek `<!-- deepmark-ignore-start -->` a `<!-- deepmark-ignore-end -->` které zajišťují, že kódy (nebo dokonce text) obsažené v těchto značkách nejsou překládány. **Ale** se provede samotný kód. Logika těchto značek vybírá označené části kódu ze souboru ještě před vlastním překladem a teprve po překladu je do souboru vrací, a sice **beze změny**.

```javascript
<!-- deepmark-ignore-start -->
YOUR CODE HERE PLEASE
<!-- deepmark-ignore-end -->
```

**Seznam vynechaných částí**

Pokud přeskočíte výpis části souboru pomocí výše uvedených značek, přeskočená část se automaticky vypíše v konzole. Pokud je tato funkce nežádoucí, je třeba `docs\node_modules\@webjetcms\deepmark\dist\cli.js` odebrat volání funkce `logIgnoredContentInfo(ignoredContent);`, který zajišťuje výpis vynechaných částí.

### Formátování souborů

Originál `deepmark` má problém se správným formátováním přeložených souborů. Přidává zbytečné mezery/řádky, nepřekládá správně některé symboly nebo ničí strukturu seznamů či tabulek špatným odsazením. Tento problém je v naší verzi `@webjetcms/deepmark` vyřešeno několika kroky, aby se těmto chybám v přeložených souborech předešlo. Nebudeme se zabývat všemi úpravami, ale zmíníme, že nejdůležitější je metoda `customizeTranslatedMarkdown` který v několika krocích upraví/formátuje již přeložené soubory.

### Verze závislostí

Vzhledem k tomu, že `deepmark` používá závislosti, jejichž verze jsou již zastaralé, většinu závislostí jsme aktualizovali na novější verze. Ne všude jsme použili nejnovější verze, protože ne všechny byly kompatibilní buď s ostatními závislostmi, nebo s aktuálním formátem použitým v programu. `deepmark`.

Seznam **Originál** závislosti:

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

Seznam **Aktualizováno** závislosti:

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
	"yaml": "^2.5.0"
},
```

## Problematický formát zdrojového souboru

Při překladu se můžete setkat s tím, že výsledný překlad porušil strukturu souboru. Tento problém může nastat, pokud váš `markdown` soubory obsahují syntaxi, která `deepmark` nedokáže zpracovat. V další části si ukážeme některé problémové syntaxe.

**Varování:**, mnoho problémů bylo vyřešeno ve verzi `@webjetcms/deepmark`, ale stále existují situace, kdy určitý formát souboru způsobí problémy při překladu a následném formátování.

### Kombinace typů seznamů

Pokud například zkombinujete číselný seznam s klasickým seznamem s odrážkami, text se přeloží, ale struktura seznamu se změní. **může** rozpadnout. Proto jejich kombinaci nevyužíváme.

```javascript
1. aaaa
2. bbbb
   - cccc
   - dddd
```

Doporučujeme používat nekombinované typy seznamů.

### Speciální znaky

Vaše soubory mohou obsahovat mnoho speciálních znaků, které mají v souborech `markdown` zvláštní role. Tyto speciální znaky mohou následně poškodit formát. Doporučujeme takové znaky **vždy** Citace.

Např. pokud mluvíte o číselné hodnotě, nepoužívejte znaky `<` a `>`. Raději použijte slovo verze menší/větší. Nebo citujte znaky \``<`\` a \``>`\`.

### Po sobě jdoucí citace

Pokud pracujete s `markdown` soubory, které víte, že pomocí značek **\`\`** text mezi nimi se stává citátem a takové citáty se nepřekládají. Problém nastává, když v textu následují 2 nebo více citátů za sebou a nejsou odděleny žádným symbolem.

Například. **\`Citace\_1\` \`Citace\_2\`**. Takový zápis může vést k chybám při překladu, proto doporučujeme přidat mezi uvozovky znak, např. **\`Quote\_1\`**, **\`Quote\_2\`**, nebo citace na odkaz **\`Citace\_1 Citace\_2\`**, případně **\`Citace\_1, Citace\_2\`**. Stejný problém se vyskytuje i u jiných kombinací, jako jsou citace a odkazy.

Problémové kombinace, kterým je třeba se vyhnout (na pořadí nezáleží):

```
- `Quote_1` `Quote_2\`
- `Quote_1` [Text here](Link here)
- **Bold** `Quote_1`
- [Text here](Link here) **Bold**
- [Text here](Link here) _[Text here\]_
- **Bold** _Text here_
```

### Neoznačený kód

Velký problém nastane, pokud zdrojové soubory obsahují neoznačený zdrojový kód. Takový kód může narušit překlad nebo zcela zkazit výsledný formát souboru. **Každý kód musí být řádně citován** a přidejte název programovacího jazyka, ve kterém byl vytvořen.

***

\`\`javascript

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

### Nevědomost `markdown` formát

Nejčastější chybou bude pravděpodobně nesprávný formát v důsledku neznalosti formátu souboru. `markdown`. Základní syntaxi si můžete přečíst na adrese [Základní syntaxe](https://www.markdownguide.org/basic-syntax/).

**Příklad zneužití**
