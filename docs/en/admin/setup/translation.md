# Translator

WebJET integrates text translation capability, currently supported translator [DeepL](https://www.deepl.com/).

Automatic translation is supported in the following applications:
- [Mirroring the structure](../../redactor/apps/docmirroring/README.md)

Once configured, you can use the compiler in your Java code as:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

where language is a two-letter language code.

## DeepL

Translator [DeepL](https://www.deepl.com/) allows you to translate texts between multiple languages via API. Registration is required on the site `For Developers` for access to `DeepL API`. For basic use, the free version is sufficient.

After registering in your account in the `Plan` See `Authentication Key for DeepL API` whose value must be entered into the configuration variable `deepl_auth_key`.

In the conf. variable `deepl_api_url` you can set the service API URL. The default value is for the free version, when using `Pro` versions set to `https://api.deepl.com/v2/translate`.
