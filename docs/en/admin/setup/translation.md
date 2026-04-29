# Translator

WebJET integrates the ability to translate texts, currently the [DeepL](https://www.deepl.com/) translator is supported.

Automatic translation is supported in the following applications:

- [Structure Mirroring](../../redactor/apps/docmirroring/README.md)
- [Clone Structure](../../redactor/apps/clone-structure/README.md)

Once configured, you can use the interpreter in your Java code as:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

where language is the two-letter language code.

In the audit record of type `TRANSLATION`, the amount of credits used during translation is written in the column `EntityID`. The number of available characters is also audited, the result is stored in `Cache` and is updated again in 5 minutes at the earliest (even `API` does not provide real-time data).

```txt
AUTO_TRANSLATION DeepL used: 13
AUTO_TRANSLATION DeepL remaining: 499453 usage: 0% (547/500000)
```

## Choosing a translator

WebJET supports the ability to add (configure) additional translators.

The newly added compiler must inherit from the abstract class `TranslationEngine` and must be defined in the config variable `translationEngineClasses`, for example as `sk.iway.iwcm.system.translation.DeepL`. You can define multiple compilers in the config variable, just separate them with a comma.

Translators defined and prepared in this way are automatically processed by the `TranslationService` class, which will use the first **configured** translator that still has **free** characters to translate. If you have multiple translators configured, the order in which they are defined in `translationEngineClasses` will matter.

## DeepL

The [DeepL](https://www.deepl.com/) translator allows you to translate texts between multiple languages ​​via an API. You need to register on the site ```For Developers``` to access ```DeepL API```. The free version is sufficient for basic use.

After registering in your account in the ```Plan``` section, you will find ```Authentication Key for DeepL API``` whose value must be entered into the configuration variable ```deepl_auth_key```.

In the config variable `deepl_api_url` you can set the URL address of the API service. The default value is for the free version, when using the `Pro` version set it to `https://api.deepl.com/v2/translate`.

In the conf. variable `deepl_api_usage_url` you can set the URL address of the API service to obtain information about the usage of the specified license key. The default value is `/v2/usage`, if the domain name is not specified, it will be used according to the setting in `deepl_api_url`.

In the config variable `deepl_model_type` you can choose which `DeepL` compiler model to use. You have the following options:

- `latency_optimized`
  - uses "classic" lower-latency translation models that support **all** language pairs
- `quality_optimized`
  - uses "next generation" models for higher quality translations, but latency is higher
  - only **part** of language pairs are supported (only a certain subset)
  - if you use the model to translate an **unsupported** language combination, the translation request will not be successful
- `prefer_quality_optimized`
  - uses the same models as `quality_optimized`, BUT if the language combination is not supported, it will automatically switch to the `latency_optimized` model
  - it is a more secure option and is **default**