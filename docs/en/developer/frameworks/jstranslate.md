# Translations in JavaScript files

To enable the use of translation keys in JavaScript files, we created a separate library encapsulating the acquisition and use of translation keys.

## Backend REST service

The REST service for obtaining translation keys is implemented in the class [AdminPropRestController.java](../../../../src/main/java/sk/iway/iwcm/admin/AdminPropRestController.java). It provides the following addresses:

- ```/admin/rest/properties/{lng}/``` - ​​get the standard list of keys for the language specified in the path ```{lng}```.
- ```/admin/rest/properties/{lng}/{prefix:.+}``` - ​​get a list of keys for the language specified in the path ```{lng}``` containing both standard keys and keys specified by the prefix ```{prefix}```. Multiple keys can be specified in the prefix, separated by a comma (all keys starting with the specified prefix will be added).
- ```/admin/rest/properties/lastmodified/{lng}/``` - ​​gets the date of the last change of translations.

### Standard key list

The REST service returns a list of keys according to the following rules:

- a key ending with the expression ```.js```, for example ```datatables.error.system.js```
- keys specified in the configuration variable ```propertiesAdminKeys``` which is a list of keys (each key on a new line, or separated by a comma). A precisely defined key is added, or if the key ends with the character ```*```, all keys starting with the given prefix are added.

### Last modified date

If the ```since``` timestamp parameter is sent when calling a REST service, a list of keys is returned:

- if there has been no change since the specified timestamp, an empty object is returned
- if a change has occurred, all keys are returned and a key named ```lastmodified``` is also returned, which contains the timestamp of the last change on the server

The last modification date is also available in [head.pug](../../../../src/main/webapp/admin/v9/views/partials/head.pug) as ```window.propertiesLastModified```. This saves one REST service call if the keys do not need to be updated.

### Translation texts

Translation texts for WebJET 2021 are in the file [text-webjet9.properties](../../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties), new translation keys need to be added to this file.

For new keys, we recommend using the suffix ```.js``` in the key name to automatically include it in the REST service response (e.g. ```datatables.error.system.js```).

If possible, it is a good idea to reuse existing translation keys from WebJET 8 - this will save the cost of translating new texts. These can be found in the file [text.properties](../../../../src/main/webapp/WEB-INF/classes/text.properties). Do not modify this file, it is just a static list of translation keys from WebJET 8.

When using an existing key, its prefix must be set to a configuration variable named ```propertiesAdminKeys``` so that the key is added to the REST service response.

**Note:** if you are translating an existing page from WebJET version 8, you can view it with the URL parameter ```?showTextKeys=true``` which will cause the translation keys to be displayed before the text. The page will probably be broken from a design perspective (since the texts will be too long), but you can view the keys via the inspector.

## Frontend library

The [Translator](../libraries/translator.md) library implements the translation key to text conversion using system translations in WebJET.

You can perform the translation by simply calling the ```WJ.translate('translation.key')``` function:

```javascript
const preklad = WJ.translate('translation.key');
```

If you need to use parameters in the translation text, you can use the expression ```{1}``` to ```{X}``` to insert parameters:

```javascript
const preklad = WJ.translate(key, ...params);
```

For example, for the key ```datatables.pageLength.auto.js=Automaticky ({1})``` you use the call ```var pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);``` which will display the text e.g. ```Automaticky (14)```.