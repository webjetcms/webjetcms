# Souběžné provádění testů

Aby se ušetřil čas potřebný k provedení testu, mohou být testy [běží paralelně](https://codecept.io/parallel/#parallel-execution-by-workers). Skripty jsou připraveny `parallel4,parallel6,parallel8,parallel12` pro paralelní provádění ve 4 až 12 oknech prohlížeče současně.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/dkXVqNnZPWg" title="Přehrávač videí YouTube" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

## Použití

Z důvodu kontinuity testů v rámci jednoho scénáře jsou testy rozděleny mezi okna prohlížeče podle `Feature` (to zajišťuje přepínač `--suites`), tj. obvykle podle názvu souboru. Scénáře v jednom souboru se pak provádějí postupně za sebou jako při standardním běhu.

Některé testy však nelze provádět paralelně, protože kontrolují stav podle IP adresy apod. Příkladem může být kontrola chybného hesla, která, pokud se provede, zabrání přihlášení po stanovenou dobu. Pokud by k tomu došlo v paralelně prováděném testu, nebylo by možné se přihlásit ani v jiných oknech prohlížeče, což by testy přerušilo.

Z tohoto důvodu je připraven skript `singlethread` který spustí pouze testy označené `@singlethread` v názvu scénáře. Zároveň jsou takto označené testy vyloučeny z paralelního provádění.

```javascript
Feature('admin.login');

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
    ...
});
```

Aby byl test kompletní, je třeba spouštět skripty postupně:

```bash
npm run singlethread
npm run parallel8
```

## Podrobnosti o provádění

Jednotlivé skripty jsou definovány v `package.json`:

```json
{
    "scripts": {
        "parallel4": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 4 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel6": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 6 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel8": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 8 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel12": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 12 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "singlethread": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run -p allure --grep '@singlethread'",
    }
}
```
