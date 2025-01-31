# Public services

WebJET offers several "public services" over REST services that you can use when programming customer applications.

Calling these services must be enabled, otherwise the services are unavailable. The permission for IP addresses is set via configuration variables:
- `restAllowedIpAddresses` - list of allowed IP addresses for calling all public services
- `restAllowedIpAddresses-DocRestController` - allowed IP addresses for calling the retrieve web page text service
- `restAllowedIpAddresses-PropertiesRestController` - allowed IP addresses for calling the key translation service

Entering a character `*` all IP addresses are enabled, you can specify multiple comma-separated starting IP addresses, for example: `192.168.,62.65.161.4,10.10.0.`.

If you have a custom REST service that extends a class [RestController](../../../../src/webjet8/java/sk/iway/iwcm/rest/RestController.java) the class name is used to obtain a special key for IP address permission checking.

## Web page text

To obtain `DocDetails` object is served by the following endpoint type `GET` :

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

The input parameter is **id** or **virtual journey** the object you want to acquire.

Examples of use:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Translation keys

The service returns only translation keys whose beginnings are set in the configuration variable `propertiesRestControllerAllowedKeysPrefixes` separated by a new line or comma. By default the value is empty, before using it you need to set a prefix or a character `*` to enable all keys.

### Obtaining translation texts

To obtain translation texts, an endpoint of type `GET` :

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

The input parameters are **language** (e.g. `sk`, `en`), in which the key values and the start/prefix of the translation keys themselves will be translated.

Examples of use:
- `/rest/properties/sk/components.abtesting`

This endpoint returns the list of pairs as a map `Map<String, String>` where each pair consists of **key with prefix** and his **value (translation)**.

Example of returned values:
- `components.abtesting.dialog_title = AB testovanie`
- `components.abtesting.allowed = AB testovanie povolene`
- `components.abtesting.ratio = Pomer`
- `components.abtesting.variantName = Nazov varianty`
- `components.abtesting.example = Priklad`

### Getting translations by key

To get translations by key with a value in the format `Entry<String, String>` the following endpoint type is used `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

The input parameters are **language** (e.g. `sk`, `en`), which will translate the key value and the **translation key**. If the translation key value contains variables `{0}, {1} ...` they can be filled with additional variables from the path (from the endpoint).

#### Examples of ingestion and return values

When you enter a path `/rest/property/sk/calendar.invitation.saveok-A` the value of the translation key is returned `calendar.invitation.saveok-A` in Slovak (`sk`) language as `Dakujeme za akceptovanie schodzky.`.

**A complex example**

When you specify a path as `/rest/property/sk/converter.number.invalidNumber/4/test` we want to get a translation key `converter.number.invalidNumber` with the value translated into the language `sk`. The value of the translation key is `Hodnota ({1}) v poli {0} musi byt cislo`.

2  optional parameters were specified, namely `4` a `test`. Since the parameter `4` was first, replacing the parameter `{0}` a parameter `test` will replace `{1}`.

The resulting returned combination is `converter.number.invalidNumber` - `Hodnota (test) v poli 4 musi byt cislo`.
