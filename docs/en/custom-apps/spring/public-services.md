# Public services

WebJET offers several "public services" via REST services that you can use when programming custom applications.

Calling these services must be allowed, otherwise the services are unavailable. Permission for IP addresses is set via configuration variables:

- `restAllowedIpAddresses` - ​​common list of allowed IP addresses for public services, which will be used when setting a specific controller to empty
- `restAllowedIpAddresses-DocRestController` - ​​allowed IP addresses for calling the web page text retrieval service
- `restAllowedIpAddresses-PropertiesRestController` - ​​allowed IP addresses for calling the translation key service
- `restAllowedIpAddresses-MarkdownSearchRestController` - ​​allowed IP addresses for [Markdown documentation search](../apps/rag/markdown-search.md)

Entering the character `*` will allow all IP addresses, you can enter multiple beginnings of IP addresses separated by a comma, for example: `192.168.,62.65.161.4,10.10.0.`.

If you have your own REST service that extends the [RestController](../../../../src/main/java/sk/iway/iwcm/rest/RestController.java) class, the class name will be used to obtain a special key for checking IP address authorization.

## Website text

To obtain an `DocDetails` object, the following endpoint of type `GET` is used:

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

The input parameter is the **id** or **virtual path** of the object you want to get.

Usage examples:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Searching in Markdown documentation

The service semantically searches the configured Markdown documentation roots. It returns found documents with text samples and, if `ragAnswerAllowed` is enabled, an AI response compiled from accessible content.

Example of calling an endpoint of type `GET`:

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

Parameters:

- `query` - ​​required question or search text.
- `language` - ​​mandatory documentation language code, for example `sk`, `cs` or `en`.
- `directory` - ​​optional restriction to the directory including subdirectories, for example `/en/redactor/`. The language in the path must match the `language` parameter.

The response is JSON with a field `results`. Each result contains `title`, `url`, `sourcePath`, `snippet`, and `score`. The AI ​​response is in the field `answer` ; if not available, it has the value `null`.

Even after allowing the IP address, a login is required by default; a regular user account is sufficient. For anonymous searches, set `ragMarkdownSearchRequireLogin=false` in addition. File access rights and request limits still apply.

After the IP check, the service verifies the availability of semantic search. If it is not available, it returns HTTP `503` without further processing the request, even if there are results in the cache.

For root settings, indexing, and the full response format, see the [Markdown Search Documentation](../apps/rag/markdown-search.md) documentation.

## Translation keys

The service will only return translation keys whose beginnings are set in the configuration variable `propertiesRestControllerAllowedKeysPrefixes` separated by a newline or comma. By default, the value is empty, before use it is necessary to set a prefix, or the character `*` to allow all keys.

### Obtaining translation texts

To obtain translation texts, an endpoint of type `GET` is used:

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

The input parameters are the **language** (e.g. `sk`, `en`) in which the key values ​​will be translated and the very beginning/prefix of the translation keys.

Usage examples:
- `/rest/properties/sk/components.abtesting`

This endpoint returns a list of pairs as a map of `Map<String, String>`, where each pair consists of a **key with a prefix** and its **value (translation)**.

Example of returned values:
- `components.abtesting.dialog_title = AB testovanie`
- `components.abtesting.allowed = AB testovanie povolene`
- `components.abtesting.ratio = Pomer`
- `components.abtesting.variantName = Nazov varianty`
- `components.abtesting.example = Priklad`

### Get translations by key

To get translations by key with value in the format `Entry<String, String>`, use the following endpoint of type `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

The input parameters are the **language** (e.g. `sk`, `en`) in which the key value will be translated and the **translation key** itself. If the translation key value contains variables `{0}, {1} ...`, they can be filled with additional variables from the path (from the endpoint).

#### Usage and return values ​​examples

When entering the path `/rest/property/sk/calendar.invitation.saveok-A`, the value of the translation key `calendar.invitation.saveok-A` in the Slovak (`sk`) language is returned as `Dakujeme za akceptovanie schodzky.`.

**A complicated example**

When entering a path as `/rest/property/sk/converter.number.invalidNumber/4/test`, we want to get the translation key `converter.number.invalidNumber` with the value translated into the language `sk`. The value of the translation key is `Hodnota ({1}) v poli {0} musi byt cislo`.

Two optional parameters were specified, `4` and `test`. Since the parameter `4` was the first, it will replace the parameter `{0}` and the parameter `test` will replace the parameter `{1}`.

The resulting returned combination is `converter.number.invalidNumber` - ​​`Hodnota (test) v poli 4 musi byt cislo`.
