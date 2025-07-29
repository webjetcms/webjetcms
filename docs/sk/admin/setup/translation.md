# Prekladač

WebJET integruje možnosť prekladu textov, aktuálne je podporovaný prekladač [DeepL](https://www.deepl.com/).

Automatický preklad je podporovaný v nasledovných aplikáciách:

- [Zrkadlenie štruktúry](../../redactor/apps/docmirroring/README.md)
- [Klonovanie štruktúry](../../redactor/apps/clone-structure/README.md)

Po konfigurácii môžete vo vašom Java kóde použiť prekladač ako:

```java
import sk.iway.iwcm.system.translation.TranslationService;

String translated = TranslationService.translate(text, fromLanguage, toLanguage);
```

kde jazyk je dvoj písmenový kód jazyka.

Do audit záznamu typu `TRANSLATION` sa do stĺpca `EntityID` zapíše spotrebované množstvo kreditov pri preklade. Audituje sa aj počet dostupných znakov, výsledok je uložený do `Cache` a aktualizuje sa znova najskôr o 5 minút (ani `API` neposkytuje údaje v reálnom čase).

```txt
AUTO_TRANSLATION DeepL used: 13
AUTO_TRANSLATION DeepL remaining: 499453 usage: 0% (547/500000)
```

## Výber prekladača

WebJET podporuje možnosť pridania (konfigurácie) ďalších ľubovoľných prekladačov.

Novo pridaný prekladač musí dediť z abstraktnej triedy `TranslationEngine` a musí byť zadefinovaný v konf. premennej `translationEngineClasses`, napríklad ako `sk.iway.iwcm.system.translation.DeepL`. V konf. premennej môžete zadefinovať viacero prekladačov, stačí ich oddeliť čiarkou.

Takto zadefinované a pripravené prekladače sú automaticky spracované triedou `TranslationService`, ktorá využije prvý **nakonfigurovaný** prekladač, ktorý ešte má **voľné** znaky na preklad. Ak máte viac nakonfigurovaných prekladačov, bude záležať na poradí ako sú zadefinované v `translationEngineClasses`.

## DeepL

Prekladač [DeepL](https://www.deepl.com/) umožňuje cez API prekladať texty medzi viacerými jazykmi. Na stránke je potrebné vykonať registráciu ```For Developers``` pre prístup k ```DeepL API```. Pre základné použitie postačuje verzia zdarma.

Po registrácii vo vašom konte v sekcii ```Plan``` nájdete ```Authentication Key for DeepL API``` ktorého hodnotu je potrebné zadať do konfiguračnej premennej ```deepl_auth_key```.

V konf. premennej `deepl_api_url` môžete nastaviť URL adresu API služby. Predvolená je hodnota pre voľnú verziu, pri použití `Pro` verzie nastavte na `https://api.deepl.com/v2/translate`.

V konf. premennej `deepl_api_usage_url` môžete nastaviť URL adresu API služby, pre získanie informácií o využití zadaného licenčného kľúča. Predvolená je hodnota `/v2/usage`, doménové meno sa použije podľa nastavenia v `deepl_api_url`.

V konf. premennej `deepl_model_type` môžete zvoliť, ktorý model `DeepL` prekladača sa použije. Na výber máte možnosti:

- `latency_optimized`
  - používa „klasické“ prekladové modely s nižšou latenciou, ktoré podporujú **všetky** jazykové páry
- `quality_optimized`
  - používa modely „novej generácie“ pre preklady s vyššou kvalitou, ale latencia je väčšia
  - podporovaná je iba **časť** jazykových párov (iba určitá podmnožina)
  - ak model využijete na preklad **nepodporovanej** kombinácie jazykov, požiadavka na preklad nebude úspešná
- `prefer_quality_optimized`
  - používa rovnaké modely ako `quality_optimized`, ALE ak kombinácia jazykov nie je podporovaná, automaticky sa prepne na model `latency_optimized`
  - je to viac bezpečná možnosť a je **prednastavená**