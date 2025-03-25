# Kontrola zranitelností knihoven

Pomocí nástroje [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/index.html) můžete jednoduše kontrolovat zranitelnosti v Java a JavaScript knihovnách web aplikace. Ty doporučujeme kontrolovat na pravidelné bázi.

Máte-li přístup ke zdrojovému kódu/gradle projektu můžete spustit analýzu přímo pomocí [gradlew příkazu](../../developer/backend/security.md#kontrola-zranitelností-v-knihovnách).

Nástroj je ale možné spustit i nad vygenerovaným `war` archivem web aplikace. Nainstalujte si verzi nástroje pro [příkazový řádek](https://jeremylong.github.io/DependencyCheck/dependency-check-cli/index.html).

Následně můžete spustit kontrolu pomocí příkazu:

```sh
dependency-check --project "Meno projektu" --suppression dependency-check-suppressions.xml --suppression dependency-check-suppressions-project.xml --scan build/libs/*.war
```

parametry se nastavuje:
- `--project` - jméno projektu, které se zobrazí v reportu.
- `--suppression` - cesta k [souborem s výjimkami](../../developer/backend/security.md#kontrola-zranitelností-v-knihovnách), typicky je tento soubor součástí git repozitáře.
- `--scan` - cesta k souboru/adresáři, který má být analyzován.

Výsledkem je soubor `dependency-check-report.html` v aktuálním adresáři.

![](dependency-check-cli.png)
