# Translator

WebJET integrates text translation capability, currently supported translator [DeepL](https://www.deepl.com/).

Automatic translation is supported in the following applications:
- [Mirroring the structure](../../redactor/apps/docmirroring/README.md)
- [Cloning structure](../../redactor/apps/clone-structure/README.md)

Once configured, you can use the compiler in your Java code as:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

where language is a two-letter language code.

Into an audit record type `TRANSLATION` shall be entered in the column `EntityID` records the amount of credits consumed during the translation. The number of available characters is also audited, the result is stored in the `Cache` and refreshes again in 5 minutes at the earliest (nor `API` does not provide real-time data).

```txt
AUTO_TRANSLATION DeepL used: 13
AUTO_TRANSLATION DeepL remaining: 499453 usage: 0% (547/500000)
```

## Choosing a translator

WebJET supports the possibility of adding (configuring) additional arbitrary translators.

The newly added compiler must inherit from the abstract class `TranslationEngine` and must be defined in the conf. variable `translationEngineClasses`, such as `sk.iway.iwcm.system.translation.DeepL`. You can define multiple translators in a conf. variable, just separate them with a comma.

The translators defined and prepared in this way are automatically processed by the class `TranslationService` which will take advantage of the first **Configured** a translator that still has **Available** characters for translation. If you have multiple translators configured, it will depend on the order they are defined in `translationEngineClasses`.

## DeepL

Translator [DeepL](https://www.deepl.com/) allows you to translate texts between multiple languages via API. Registration is required on the site `For Developers` for access to `DeepL API`. For basic use, the free version is sufficient.

After registering in your account in the `Plan` See `Authentication Key for DeepL API` whose value must be entered into the configuration variable `deepl_auth_key`.

In the conf. variable `deepl_api_url` you can set the service API URL. The default value is for the free version, when using `Pro` versions set to `https://api.deepl.com/v2/translate`.

In the conf. variable `deepl_api_usage_url` you can set the service API URL to get information about the usage of the specified license key. The default value is `/v2/usage`, if no domain name is specified, it is used according to the setting in `deepl_api_url`.

In the conf. variable `deepl_model_type` you can choose which model `DeepL` the translator will be used. You have the following options:
- `latency_optimized`
  - uses "classic" translation models with lower latency that support **All** language pairs
- `quality_optimized`
  - uses "next generation" models for higher quality translations, but latency is higher
  - only supported **Part of** language pairs (only a subset)
  - if you use the model for translation **unsupported** combinations of languages, the request for translation will not be successful
- `prefer_quality_optimized`
  - uses the same models as `quality_optimized`, BUT if the language combination is not supported, it will automatically switch to the model `latency_optimized`
  - it's a more secure option and is **Preloaded**
