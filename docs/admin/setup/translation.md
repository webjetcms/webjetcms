# Prekladač

WebJET integruje možnosť prekladu textov, aktuálne je podporovaný prekladač [DeepL](https://www.deepl.com/).

Automatický preklad je podporovaný v nasledovných aplikáciách:

- [Zrkadlenie štruktúry](../../redactor/apps/docmirroring/README.md)

Po konfigurácii môžete vo vašom Java kóde použiť prekladač ako:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

kde jazyk je dvoj písmenový kód jazyka.

## DeepL

Prekladač [DeepL](https://www.deepl.com/) umožňuje cez API prekladať texty medzi viacerými jazykmi. Na stránke je potrebné vykonať registráciu ```For Developers``` pre prístup k ```DeepL API```. Pre základné použitie postačuje verzia zdarma.

Po registrácii vo vašom konte v sekcii ```Plan``` nájdete ```Authentication Key for DeepL API``` ktorého hodnotu je potrebné zadať do konfiguračnej premennej ```deepl_auth_key```.
