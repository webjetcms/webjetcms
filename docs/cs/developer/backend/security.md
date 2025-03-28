# Bezpečnost

## Kontrola zranitelností v knihovnách

V projektu je integrovaný nástroj [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/index.html), který dokáže kontrolovat zranitelnosti v Java i JavaScript knihovnách. Kontrolu zranitelností spustíte příkazem:

```sh
gradlew --info dependencyCheckAnalyze
```

který vytvoří report ve formátu HTML do souboru `build/reports/dependency-check-report.html`. Tento report můžete snadno otevřít ve web prohlížeči. Kontrolu doporučujeme provádět před každým `release` nové verze.

![](dependency-check.png)

Analýza může obsahovat falešné nálezy. Existují následující soubory, ve kterých se nastavují výjimky:
- `/dependency-check-suppressions.xml` - soubor obsahuje výjimky pro standardní WebJET CMS, nikdy soubor nemodifikujte.
- `dependency-check-suppressions-project.xml` - do souboru můžete přidávat výjimky pro váš projekt. Přímo v reportu je tlačítko `suppress` na které když klepnete zobrazí se vám XML kód výjimky. Ten jednoduše zkopírujte do souboru dovnitř značky `suppressions`.

Kontrolu lze provádět i přímo nad vygenerovaným `war` archivem pomocí [cli verze](../../sysadmin/dependency-check/README.md).

## Nebezpečný HTML kód

Pokud máte na frontendu pole, které umožňuje HTML formátování, může do něj být vložen potenciálně nebezpečný kód. V datatabulce se jedná např. o pole typu `DataTableColumnType.QUILL`. Standardně při získání JPA objektu z databáze jsou HTML značky jako `<, >` konvertováno na HTML entity typu `&lt;, &gt;`. Zajišťuje to třída `XssAttributeConverter` která má nastavený atribut `@Converter(autoApply = true)`.

Pokud potřebujete pracovat s HTML kódem je třeba daný atribut anotovat:
- `@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)` - povolí veškerý HTML kód, doporučujeme používat v minimální míře, respektive pouze v případě, kdy HTML kód skutečně má obsahovat i např. JavaScript nebo jiný potenciálně nebezpečný kód.
- `@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)` - povolí pouze základní HTML formátování podle doporučení [OWASP](https://owasp.org/www-project-java-html-sanitizer/). Tento konvertor doporučujeme používat na všechny vstupy, kde je používán jednoduchý WYSIWYG editor typu `DataTableColumnType.QUILL`.
