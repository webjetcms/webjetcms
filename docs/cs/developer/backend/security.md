# Zabezpečení

## Skenování zranitelností v knihovnách

Projekt integruje nástroj [Kontrola závislostí OWASP](https://jeremylong.github.io/DependencyCheck/index.html) který dokáže kontrolovat zranitelnosti v knihovnách Java i JavaScript. Chcete-li spustit kontrolu zranitelností, spusťte následující příkaz:

```sh
gradlew --info dependencyCheckAnalyze
```

který vytvoří zprávu ve formátu HTML do souboru `build/reports/dependency-check-report.html`. Tuto zprávu můžete snadno otevřít ve webovém prohlížeči. Doporučujeme ji zkontrolovat před každým `release` nová verze.

![](dependency-check.png)

Analýza může obsahovat falešně pozitivní výsledky. Existují následující soubory, ve kterých jsou nastaveny výjimky:
- `/dependency-check-suppressions.xml` - soubor obsahuje výjimky pro standardní WebJET CMS, nikdy jej neupravujte.
- `dependency-check-suppressions-project.xml` - můžete do souboru přidat výjimky pro svůj projekt. Přímo ve zprávě je k dispozici tlačítko `suppress` který po kliknutí zobrazí kód XML výjimky. Jednoduše jej zkopírujte do souboru uvnitř značky `suppressions`.

Kontrolu lze provádět také přímo nad vygenerovaným `war` archivovat pomocí [verze cli](../../sysadmin/dependency-check/README.md).

## Nebezpečný kód HTML

Pokud máte na frontendovém rozhraní pole, které umožňuje formátování HTML, lze do něj vložit potenciálně nebezpečný kód. Například v datové tabulce je pole typu `DataTableColumnType.QUILL`. Ve výchozím nastavení se při načítání objektu JPA z databáze použijí značky HTML, jako např. `<, >` převedeny na entity typu HTML `&lt;, &gt;`. To zajišťuje třída `XssAttributeConverter` který má sadu atributů `@Converter(autoApply = true)`.

Pokud potřebujete pracovat s kódem HTML, musíte atribut opatřit poznámkou:
- `@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)` - umožňuje veškerý kód HTML, doporučujeme jej používat minimálně nebo pouze v případech, kdy má kód HTML skutečně obsahovat JavaScript nebo jiný potenciálně nebezpečný kód.
- `@javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)` - umožňuje pouze základní formátování HTML podle doporučení [OWASP](https://owasp.org/www-project-java-html-sanitizer/). Tento převodník doporučujeme používat pro všechny vstupy, kde se používá jednoduchý WYSIWYG editor. `DataTableColumnType.QUILL`.
