# Reporty přes Allure

Během automatizovaného testu je také vygenerována zpráva ve formátu pro [Allure](https://docs.qameta.io/allure/). Jsou publikovány pro [chrom](http://docs.webjetcms.sk/allure/chromium/) také pro [firefox](http://docs.webjetcms.sk/allure/firefox/).

Generování vstupních dat pro zprávu zajišťuje rozšíření [codecept-allure](https://codecept.io/plugins/).

![](allure-overview.png)

Zobrazí se následující údaje:
- `Overview` - základní přehled o stavu a historii
- `Categories` - zobrazí kategorizovaný seznam chyb, jejichž kategorie jsou definovány pomocí [regulární výrazy](https://docs.qameta.io/allure/#_categories_2) vrácená chyba v souboru `src/test/webapp/allure/categories.json`
- `Suites` - seznam jednotlivých testů s uvedením provedených kroků
- `Graphs` - grafy současného a historického vývoje
- `Timeline` - zobrazení času provedení každého testu
- `Behaviors` - umožňuje rozdělit testy na `Epic, Feature, Story` co WebJET CMS v současné době nepoužívá
- `Packages` - stromové znázornění jednotlivých testů

## Spuštění zprávy

Proces generování zprávy je složitější kvůli uchování historie. Vyžaduje to načtení předchozí zprávy (pro `history`) před vygenerováním sestavy.

Celý proces je uveden ve scénáři `npx-allure.sh`, který před spuštěním testu stáhne nejnovější výsledky z dokumentačního serveru a po spuštění testu je uloží na dokumentační server.

Skript se používá s parametry:
- `CODECEPT_BROWSER` - název použitého prohlížeče - `chromium` nebo `firefox` (výchozí nastavení `chromium`)
- `CODECEPT_URL` - URL testované domény (výchozí `demotest.webjetcms.sk`)
- `HOST_USER` - Uživatelské jméno SSH pro stažení historie a uložení výsledku
- `HOST_NAME` - název domény serveru pro ukládání historie připojení SSH a výsledků.
- `HOST_DIR` - složka se zprávou na serveru, k názvu složky se přidá název použitého prohlížeče.

```sh
#spustenie s chrome a predvolenou domenou
npx-allure.sh
#spustenie s firefox a predvolenou domenou
npx-allure.sh firefox
#spustenie s firefoxom a domenou iwcm.interway.sk
npx-allure.sh firefox http://iwcm.interway.sk
#spustenie s chrome a domenou demo.webjetcms.sk
npx-allure.sh chromium http://demo.webjetcms.sk
```

## Technické informace

Jak bylo napsáno výše, pro zachování historie je nutné získat složku před generováním zprávy. `history` z předchozí verze. To je ve skriptu zajištěno použitím `rsync` ze strany dokumentace.

Kromě toho se při spuštění generuje soubor. `build/test/environment.properties` s názvem použitého prohlížeče a použité domény. To se zobrazí na kartě `Overview` částečně `environment`.

Provádění testů a generování zpráv zajišťuje CI-CD v souboru `gitlab-ci.yml` spuštěním úlohy gradle `rune2etest` a `rune2etestfirefox`.
