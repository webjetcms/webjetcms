# Translations in JavaScript files

To enable the use of translation keys in JavaScript files, we have created a separate library encapsulating the acquisition and use of translation keys.

## Backend REST services

The REST service for obtaining translation keys is implemented in the class [AdminPropRestController.java](../../../src/main/java/sk/iway/iwcm/admin/AdminPropRestController.java). It provides the following addresses:
- `/admin/rest/properties/{lng}/` - getting the standard list of keys for the language specified in the path `{lng}`.
- `/admin/rest/properties/{lng}/{prefix:.+}` - getting the list of keys for the language specified in the path `{lng}` containing both standard keys and keys specified by a prefix `{prefix}`. Multiple keys can be specified in the prefix, separated by a comma (all keys starting with the specified prefix are added).
- `/admin/rest/properties/lastmodified/{lng}/` - gets the date of the last translation change.

### Standard list of keys

The REST service returns a list of keys according to the following rules:
- key ending in expression `.js`, for example `datatables.error.system.js`
- keys specified in the configuration variable `propertiesAdminKeys` which contains a list of keys (each key on a new line, or separated by a comma). A well-defined key is added, or if the key ends in a character `*` all keys starting with the given prefix are added.

### Date of last change

If a parameter is sent when calling the REST service `since` of type timestamp is the returned list of keys:
- if no change has occurred since the specified timestamp, an empty object is returned
- if there is a change, all keys are returned and the key with the name `lastmodified` containing the timestamp of the last change on the server

The date of the last change is also in [head.pug](../../../src/main/webapp/admin/v9/views/partials/head.pug) available as `window.propertiesLastModified`. This saves one call to the REST service if the keys do not need to be updated.

### Translation texts

Translation texts for WebJET 2021 are in the file [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties), new translation keys need to be added to this file.

For new keys, we recommend using the suffix `.js` on behalf of the key for its automatic inclusion in the REST service response (e.g. `datatables.error.system.js`).

If possible, it is a good idea to reuse existing translation keys from WebJET 8 - this saves the cost of translating new texts. These can be found in the file [translations.properties](../../../src/main/webapp/files/preklady.properties). Do not modify this file, it is just a static list of translation keys from WebJET 8.

When using an existing key, its prefix must be set in a configuration variable named `propertiesAdminKeys` to add the key to the REST service response.

**Remark:** if you are translating an existing page from WebJET version 8 you can view it with the URL parameter `?showTextKeys=true` which causes the translation keys to be displayed before the text. The page will probably be broken from a design point of view (as the text will be too long), but you can look at the keys through the inspector.

## Frontend library

Library [Translator](../libraries/translator.md) implements translation key to text conversion using system translations in WebJET.

To provide translation, simply call the function `WJ.translate('translation.key')`:

```javascript
const preklad = WJ.translate('translation.key');
```

If you need to use parameters in the translation text, you can use the expression `{1}` to `{X}` to insert parameters:

```javascript
const preklad = WJ.translate(key, ...params);
```

E.g. for key `datatables.pageLength.auto.js=Automaticky ({1})` use the call `var pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);` which will display text e.g. `Automaticky (14)`.
