# Kontrola zranitelnosti knihoven

Použití nástroje [Kontrola závislostí OWASP](https://jeremylong.github.io/DependencyCheck/index.html) můžete snadno zkontrolovat zranitelnosti v knihovnách Java a JavaScript webové aplikace. Doporučujeme je pravidelně kontrolovat.

Pokud máte přístup ke zdrojovému kódu/gradu projektu, můžete analýzu spustit přímo pomocí příkazu [příkaz gradlew](../../developer/backend/security.md#skenování-zranitelností-v-knihovnách).

Nástroj však lze spustit i nad vygenerovanými `war` archiv webových aplikací. Nainstalujte verzi nástroje pro [příkazový řádek](https://jeremylong.github.io/DependencyCheck/dependency-check-cli/index.html).

Kontrolu pak můžete spustit pomocí příkazu:

```sh
dependency-check --project "Meno projektu" --suppression dependency-check-suppressions.xml --suppression dependency-check-suppressions-project.xml --scan build/libs/*.war
```

jsou nastaveny parametry:
- `--project` - název projektu, který se zobrazí v sestavě.
- `--suppression` - způsob, jak [soubor s výjimkami](../../developer/backend/security.md#skenování-zranitelností-v-knihovnách), obvykle je tento soubor součástí repozitáře git.
- `--scan` - cesta k analyzovanému souboru/adresáři.

Výsledkem je sada `dependency-check-report.html` v aktuálním adresáři.

![](dependency-check-cli.png)
