# Překladač

WebJET integruje možnost překladu textů, aktuálně je podporován překladač [DeepL](https://www.deepl.com/).

Automatický překlad je podporován v následujících aplikacích:
- [Zrcadlení struktury](../../redactor/apps/docmirroring/README.md)
- [Klonování struktury](../../redactor/apps/clone-structure/README.md)

Po konfiguraci můžete ve vašem Java kódu použít překladač jako:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

kde jazyk je dvou písmenový kód jazyka.

Do audit záznamu typu `TRANSLATION` se do sloupce `EntityID` zapíše spotřebované množství kreditů při překladu. Audituje se i počet dostupných znaků, výsledek je uložen do `Cache` a aktualizuje se znovu nejdříve o 5 minut (ani `API` neposkytuje údaje v reálném čase).

```txt
AUTO_TRANSLATION DeepL used: 13
AUTO_TRANSLATION DeepL remaining: 499453 usage: 0% (547/500000)
```

## Výběr překladače

WebJET podporuje možnost přidání (konfigurace) dalších libovolných překladačů.

Nově přidaný překladač musí dědit z abstraktní třídy `TranslationEngine` a musí být definován v konf. proměnné `translationEngineClasses`, například jako `sk.iway.iwcm.system.translation.DeepL`. V konf. proměnné můžete definovat více překladačů, stačí je oddělit čárkou.

Takto definované a připravené překladače jsou automaticky zpracovány třídou `TranslationService`, která využije první **nakonfigurován** překladač, který ještě má **volné** znaky k překladu. Máte-li více nakonfigurovaných překladačů, bude záležet na pořadí než jsou definovány v `translationEngineClasses`.

## DeepL

Překladač [DeepL](https://www.deepl.com/) umožňuje přes API překládat texty mezi více jazyky. Na stránce je třeba provést registraci `For Developers` pro přístup k `DeepL API`. Pro základní použití postačuje verze zdarma.

Po registraci ve vašem kontě v sekci `Plan` najdete `Authentication Key for DeepL API` jehož hodnotu je třeba zadat do konfigurační proměnné `deepl_auth_key`.

V konf. proměnné `deepl_api_url` můžete nastavit URL adresu API služby. Výchozí je hodnota pro volnou verzi, při použití `Pro` verze nastavte na `https://api.deepl.com/v2/translate`.

V konf. proměnné `deepl_api_usage_url` můžete nastavit URL adresu API služby, pro získání informací o využití zadaného licenčního klíče. Výchozí je hodnota `/v2/usage`, není-li zadáno doménové jméno, použije se podle nastavení v `deepl_api_url`.

V konf. proměnné `deepl_model_type` můžete zvolit, který model `DeepL` překladače se použije. Na výběr máte možnosti:
- `latency_optimized`
  - používá „klasické“ překladové modely s nižší latencí, které podporují **všechny** jazykové páry
- `quality_optimized`
  - používá modely „nové generace“ pro překlady s vyšší kvalitou, ale latence je větší
  - podporována je pouze **část** jazykových párů (pouze určitá podmnožina)
  - pokud model využijete k překladu **nepodporované** kombinace jazyků, požadavek na překlad nebude úspěšný
- `prefer_quality_optimized`
  - používá stejné modely jako `quality_optimized`, ALE pokud kombinace jazyků není podporována, automaticky se přepne na model `latency_optimized`
  - je to více bezpečná možnost a je **přednastavená**
