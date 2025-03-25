# Překladač

WebJET integruje možnost překladu textů, aktuálně je podporován překladač [DeepL](https://www.deepl.com/).

Automatický překlad je podporován v následujících aplikacích:
- [Zrcadlení struktury](../../redactor/apps/docmirroring/README.md)

Po konfiguraci můžete ve vašem Java kódu použít překladač jako:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

kde jazyk je dvou písmenový kód jazyka.

## DeepL

Překladač [DeepL](https://www.deepl.com/) umožňuje přes API překládat texty mezi více jazyky. Na stránce je třeba provést registraci `For Developers` pro přístup k `DeepL API`. Pro základní použití postačuje verze zdarma.

Po registraci ve vašem kontě v sekci `Plan` najdete `Authentication Key for DeepL API` jehož hodnotu je třeba zadat do konfigurační proměnné `deepl_auth_key`.

V konf. proměnné `deepl_api_url` můžete nastavit URL adresu API služby. Výchozí je hodnota pro volnou verzi, při použití `Pro` verze nastavte na `https://api.deepl.com/v2/translate`.
