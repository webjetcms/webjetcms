# Paralelné spustenie testov

Z dôvodu ušetrenia času vykonania testov je možné ich [spustiť paralelne](https://codecept.io/parallel/#parallel-execution-by-workers). Sú pripravené skripty ```parallel4,parallel6,parallel8,parallel12``` pre paralelné vykonanie v 4 až 12 okien prehliadača naraz.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/dkXVqNnZPWg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

## Použitie

Z dôvodu náväznosti testov v jednom scenári sú medzi jednotlivé okná prehliadača delené testy podľa ```Feature``` (zabezpečuje to prepínač ```--suites```), teda typicky podľa mena súboru. Scenáre v jednom súbore sa následne vykonajú postupne za sebou ako pri štandardnom spustení.

Niektoré testy ale nemôžu byť paralelne vykonané, pretože napr. kontrolujú stav podľa IP adresy a podobne. Príkladom je kontrola zle zadaného hesla, ktorá ak sa vykoná, tak znemožní prihlásenie na určený čas. Ak by sa toto udialo v paralelne spustenom teste, nedá sa prihlásiť ani v ďalších oknách prehliadača, čo pokazí testy.

Z toho dôvodu je pripravený skript ```singlethread```, ktorý spustí len testy označené značkou ```@singlethread``` v názve scenára. Zároveň takto označené testy sú vylúčené z paralelného spustenia.

```javascript
Feature('admin.login');

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
    ...
});
```

Pre kompletný test je teda potrebné spustiť skripty za sebou:

```bash
npm run singlethread
npm run parallel8
```

## Detaily implementácie

Jednotlivé skripty sú definované v ```package.json```:

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