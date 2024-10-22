# Překladatel

WebJET integruje možnost překladu textu, v současnosti podporovaný překladač [DeepL](https://www.deepl.com/).

Automatický překlad je podporován v následujících aplikacích:
- [Zrcadlení struktury](../../redactor/apps/docmirroring/README.md)

Po konfiguraci můžete překladač používat ve svém kódu v jazyce Java jako:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

kde jazyk je dvoupísmenný kód jazyka.

## DeepL

Překladatel [DeepL](https://www.deepl.com/) umožňuje překládat texty mezi více jazyky prostřednictvím rozhraní API. Na webu je vyžadována registrace `For Developers` pro přístup k `DeepL API`. Pro základní použití je bezplatná verze dostačující.

Po registraci do svého účtu v `Plan` Viz . `Authentication Key for DeepL API` jejíž hodnota musí být zadána do konfigurační proměnné `deepl_auth_key`.

V konf. proměnné `deepl_api_url` můžete nastavit adresu API služby. Výchozí hodnota je pro bezplatnou verzi, při použití `Pro` verze nastavené na `https://api.deepl.com/v2/translate`.
