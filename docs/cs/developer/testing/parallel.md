# Paralelní spuštění testů

Z důvodu ušetření času provedení testů je možné je [spustit paralelně](https://codecept.io/parallel/#parallel-execution-by-workers). Jsou připraveny skripty `parallel4,parallel6,parallel8,parallel12` pro paralelní provedení ve 4 až 12 oken prohlížeče najednou.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/dkXVqNnZPWg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

## Použití

Z důvodu návaznosti testů v jednom scénáři jsou mezi jednotlivá okna prohlížeče děleny testy podle `Feature` (zajišťuje to přepínač `--suites`), tedy typicky podle jména souboru. Scénáře v jednom souboru se následně provedou postupně za sebou jako při standardním spuštění.

Některé testy ale nemohou být paralelně provedeny, protože např. kontrolují stav podle IP adresy a podobně. Příkladem je kontrola špatně zadaného hesla, která pokud se provede, tak znemožní přihlášení na určený čas. Pokud by se toto událo v paralelně spuštěném testu, nedá se přihlásit ani v dalších oknech prohlížeče, což pokazí testy.

Z toho důvodu je připraven skript `singlethread`, který spustí pouze testy označené značkou `@singlethread` v názvu scénáře. Zároveň takto označené testy jsou vyloučeny z paralelního spuštění.

```javascript
Feature('admin.login');

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
    ...
});
```

Pro kompletní test je tedy třeba spustit skripty za sebou:

```bash
npm run singlethread
npm run parallel8
```

## Detaily implementace

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
