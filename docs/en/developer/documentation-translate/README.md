# Translation of documentation

In this section, we will discuss how to provide a translation of existing documentation.

## deepmark

For automated file translation `markdown` we will use the available free tool `deepmark`which uses `DeepL` Translator. The necessary installation information as well as more details can be found at [here](https://github.com/izznatsir/deepmark).

### Installation

Same `deepmark` installs in the location where the documentation is located, which in our case is the folder `docs`.

`npm install -D deepmark` - installation command

### Environment variable

Since `deepmark` uses a translator to translate `DeepL`, you need to set the environment variable `DEEPL_AUTH_KEY ` with the authorisation key to `DeepL`.

### Configuration file `deepmark`

In the configuration file we set the location from where we will `markdown` translate, as well as the location where these translated files will be stored. We also define from which and into which language the translation should be done.

**Attention:** this configuration file **must be** called as `deepmark.config.mjs` and must be in the same location where we installed `deepmark`. In our case, this is the folder `docs`.
```javascript
/** @type {import("deepmark").UserConfig} */
export default {
	sourceLanguage: "sk",
	outputLanguages: ["en-US"],
	directories: [["sk", "en"]],
};
```

**Attention:** if the target translation location is not created, it is created automatically. If the target location already contains files with the same name they will be overwritten. Otherwise new files will be created.
### Starting the translation

Before we start, we have to say that the translation itself can work in three modes `hybrid, offline, online`. We strongly recommend using the mode `hybrid`where the already translated phrases are stored in a local database, saving both tea and the number of characters used.

**Attention:** note that the number of translations is limited and it is therefore recommended to use this mode to save the number of translations.
You do the translation via the console, where in the location `docs` to start the process, use the command `npx deepmark translate --mode hybrid`. No message is displayed when the translation is complete.

## Possible problem

If you will be running this translation process on a Windows OS device, you can set the console to this error message.

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

The message will claim that the path to the configuration file is not valid.

**Solution**

V `docs` you will find a folder with modules named `node_modules`. In this folder, you open the file in the following path `docs/node_modules/deepmark/dist/cli.js`.

At the end of the file, you will find the method `getThenResolveConfig`where this problem occurred.

```javascript
async function getThenResolveConfig(path) {
	const configFilePath = path ? (path.startsWith("/") ? path : np.resolve(process.cwd(), path)) : np.resolve(process.cwd(), "deepmark.config.mjs");

	const userConfig = (await import(configFilePath)).default;

	return resolveConfig(userConfig);
}
```

Add a new method before this method `getConfigFilePath` and then edit this code.

```javascript
async function getConfigFilePath(path, doWindowsEdit = false) {
	let configFilePathTmp = path ? (path.startsWith("/") ? path : np.resolve(process.cwd(), path)) : np.resolve(process.cwd(), "deepmark.config.mjs");

	if (doWindowsEdit === true) {
		configFilePathTmp = configFilePathTmp.replaceAll("\\", "/");
		if (configFilePathTmp.startsWith("file://") === false) configFilePathTmp = "file://" + configFilePathTmp;
	}

	return configFilePathTmp;
}

async function getThenResolveConfig(path) {
	const configFilePath = await getConfigFilePath(path, true);

	const userConfig = (await import(configFilePath)).default;

	return resolveConfig(userConfig);
}
```

Method `getConfigFilePath` modifies the path to the configuration file to suit the Windows OS.

## Copying images

One of the features of the translation, is to copy all non-translatable files from the source folder to the destination folder. These files include `.png, .jpg, .jpge, .ico, .css` and others. Actually all files that are not of type `markdown`. This behaviour can be convenient as you do not have to manually copy all dependent files.

**Warning:** the disadvantage is that if you add specific modified images for the English version to the EN documentation, they are overwritten with the original images from the Slovak version every time they are translated.

**Solution**

You will go to the following file `docs/node_modules/deepmark/dist/config.js`.

You will find the following conditions around line 240.

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

The solution is to add a new condition, to skip a file with an extension for images e.g. `.png, .jpg, .jpge`. This way, files with the given extensions will not be copied during translation.

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

## Problematic source file format

When translating, you may encounter that either the translation fails or the resulting translation breaks the file structure. This problem can occur if your `markdown` files contain syntax that `deepmark` can't process. In the next section, we will show some problem syntaxes.

### Tables

The structure of the tables always breaks during translation and can compromise the translation of the whole file. Therefore, we do not recommend using the subsequent format in your files.

```
| Metódy                            | Gettery               | Settery                   |
| -----------                       | -----------           | -----------               |
| [load()](#load)                   | [language](#language) | [urlLoad](#urlload)       |
| [onBeforeLoad()](#onbeforeload)   | [date](#date)         | [urlUpdate](#urlupdate)   |
| [onAfterLoad()](#onafterload)     |
| [translate()](#translate)         |
| [htmlTranslate()](#htmltranslate) |
```

We recommend using classic lists.

### Combining list types

If you combine, for example, a numeric list with a classic bulleted list, the text will be translated, but the list structure may break down. Therefore, we do not get to use the subsequent format.

```
1. aaaa
2. bbbb
   - cccc
   - dddd
```

We recommend using uncombined list types.

### Special characters

`<` a `>``<``>`

### Consecutive quotations

If you work with `markdown` files, you know that the use of \`\` tags makes the text between them a quotation, and such quotations are not translated. The problem arises when 2 or more quotations follow each other in the text and are not separated by any symbol.

E.g. \`Quote\_1\` \`Quote\_2\`. Such a notation can lead to translation failures, so we recommend adding a character between the quotes, e.g. \`Quote\_1\`, \`Quote\_2\`. The same problem occurs with other combinations such as citations and links.

Problem combinations to avoid (order doesn't matter):
- \`Quote\_1\` \`Quote\_2\`
- \`Quote\_1\` \[Text here]\(Link here)
- \*\*Bold\*\* \`Quote\_1\`
- [Text here]\(Link here) \*\*Bold\*\*
- [Text here]\(Link here) \_\[Text here]\_
- ### Unmarked code
A big problem occurs if the source files contain unlabeled programming source code. Such code can break the translation, or completely mess up the resulting file format. **Each code must be properly cited** and add the name of the programming language in which it was created.







## Error formatting of the translated file

It may happen that you have done everything as you should, but the resulting translated file still has incorrect formatting. The fault lies with the `deepmark`who has a problem with this.

These include:
- redundant addition of rows between list options
- damage (wrong closing) of iframe tag
- ...

**Solution**

The solution is again implemented in a file `docs/node_modules/deepmark/dist/config.js`. Find the following code that saves to the output file `markdown`.

```javascript
await fs.outputFile(outputFilePath.replace(/\$langcode\$/, targetLanguage), getMarkdown(_mdast), { encoding: "utf-8" });
```

This `markdown` adjust as it suits you. Here is a possible solution that will format the resulting `markdown` so that it resembles the original set as closely as possible.

```javascript
let markdown = getMarkdown(_mdast);
//remove redundant new lines
markdown = markdown.replace(/(^[\S]*\*.*)\n\n/gm, "$1\n");
markdown = markdown.replace(/(^[\s]*\*.*)\n\n/gm, "$1\n");
//replace * with - (and remove redundant spaces) to deep of 4 levels
markdown = markdown.replace(/(\n\*[\s]{3})(.*)/gm, "\n- $2");
markdown = markdown.replace(/(\n[\s]{4}\*[\s]{3})(.*)/gm, "\n\t- $2");
markdown = markdown.replace(/(\n[\s]{8}\*[\s]{3})(.*)/gm, "\n\t\t- $2");
markdown = markdown.replace(/(\n[\s]{12}\*[\s]{3})(.*)/gm, "\n\t\t\t- $2");
//Headline fix - add new line before headline
markdown = markdown.replace(/(^[\S]*-[\S]*.*)\n([\#]+.*)/gm, "$1\n\n$2"); //fix bold headlines
//Fix Bold healines - add new line before and after bold text (that is headline)
markdown = markdown.replace(/([^\n])\n(^\*\*[0-1a-zA-Z ]+\*\*)/gm, "$1\n\n$2");
markdown = markdown.replace(/(^\*\*[0-1a-zA-Z ]+\*\*)\n([^\n])/gm, "$1\n\n$2");
//Fix image links - add new line before and after image (if necessary)
markdown = markdown.replace(/([^\n])\n(^!\[\]\([^()]+\))/gm, "$1\n\n$2");
markdown = markdown.replace(/(^!\[\]\([^()]+\))\n([^\n])/gm, "$1\n\n$2");
//Fix image tags - add new line before and after image (if necessary)
markdown = markdown.replace(/([^\n])\n(^<img.*\/>)/gm, "$1\n\n$2");
markdown = markdown.replace(/(^<img.*\/>)\n([^\n])/gm, "$1\n\n$2");
//Fix list header, so between list and header is NOT line and list header is separated from rest of text
/*

 * Something something:
 * - something
 * - something
 */
markdown = markdown.replace(/(^.*:\n)\n(^[\s]*-)/gm, "$1$2");
markdown = markdown.replace(/([^\n])\n(^.*:\n)/gm, "$1\n\n$2");
//FIX - markdown gonna fuck the iframe end tag, need to be fixed or everything after this error not gonna show
markdown = markdown.replace(/(^<div class="video-container">\n[\s]*)(<iframe.*)\/>(\n<\/div>)/gm, "$1$2></iframe>$3");
//FIX fucking end tags
markdown = markdown.replace(/(<[^<>]*)[\s]>/gm, "$1>");
//Special case, if file start with * (in _sidebar.md)
markdown = markdown.replace(/^\*[\s]*([^*]*)\n/gm, "- $1");
//Special case, \[ to [ (in ROADMAP.md
//For safety, we will replace only if there is combination '- \[ ]' or '- \[x]' at START of line
markdown = markdown.replace(/^-\s\\\[\s\]/gm, "- [ ]");
markdown = markdown.replace(/^-\s\\\[x\]/gm, "- [x]");

//Special case for OLD changelog - 2020
markdown = markdown.replace(/^-\s\\\[([a-zA-Z ]+])/gm, "- [$1"); //First replace - \[TEXT AND space
markdown = markdown.replace(/^-\s\\\#([0-9]+)/gm, "- #$1"); //Second replace - \#NUMBER
markdown = markdown.replace(/^[\s]*\\\#([0-9]+)/gm, "#$1"); //Third replace \#NUMBER
markdown = markdown.replace(/(^-\s#[0-9]+)\s\\\[([a-zA-Z ]+])/gm, "$1 [$2"); //Fourth replace - #NUMBER \[TEXT an spac
await fs.outputFile(outputFilePath.replace(/\$langcode\$/, targetLanguage), markdown, { encoding: "utf-8" });
```
