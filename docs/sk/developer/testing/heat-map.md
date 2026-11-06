# Testovacie údaje pre mapu kliknutí

Test `src/test/webapp/tests/apps/stat-heat-map-articles.js` vytvára kliknutia na publikovaných stránkach v priečinku `122825`, ktoré používajú šablónu **Article-Heatmap**. Pri spustení načíta zoznam stránok z administrácie. Jeden scenár potom v cykle prejde všetky zadané šírky okna. CodeceptJS za celý cyklus vykáže jeden výsledok.

Scenár sa spúšťa aj v bežnej a paralelnej E2E sade. Ak ho chcete vybrať pomocou značky `@current` v jeho názve, spustite lokálny WebJET na adrese `http://iwcm.interway.sk` a použite rovnaké prihlasovacie premenné ako pri ostatných CodeceptJS testoch.

```shell
cd src/test/webapp
npm run current
```

Príkaz `npm run current` vyberie všetky scenáre označené `@current`. Ak chcete spustiť iba súbor s mapou kliknutí bez ohľadu na značku, použite `npm run all -- tests/apps/stat-heat-map-articles.js`.

## Počet návštev a rozlíšenia

Predvolene test vykoná **5 návštev každej stránky pri každej zo šírok 1280, 1366, 1440, 1536, 1920 a 2560 px**. Výška okna je 900 px. Pri siedmich stránkach ide o 210 návštev. Šírky aj počet návštev môžete zmeniť:

```shell
# Viac údajov pri všetkých predvolených šírkach
CODECEPT_HEATMAP_VISITS=10 npm run current

# Jedna návšteva každej stránky pri šírke okna 1920 px
CODECEPT_HEATMAP_WIDTHS=1920 CODECEPT_HEATMAP_VISITS=1 npm run current
```

`CODECEPT_HEATMAP_VISITS` znamená počet návštev **na stránku a šírku**, nie celkový počet návštev. Podporuje hodnoty 1 až 100. `CODECEPT_HEATMAP_WIDTHS` obsahuje šírky od 360 do 2560 px oddelené čiarkami.

## Správanie návštevníka

Test nastaví súhlas so štatistickými cookies a odhlási sa z administrácie. Každá návšteva vyberie 4 až 8 rôznych miest v obsahu článku, prípadne menej, ak stránka nemá dostatok prvkov.

Test častejšie vyberá obrázky, odkazy a tlačidlá než bežný text. Na kartách môže rozbaliť a zbaliť podrobnosti, v úvode otvoriť redakčnú poznámku alebo prejsť na odkazovanú časť stránky. Mení sa výber prvkov, riadkov textu aj súradníc: väčšina kliknutí smeruje bližšie k stredu prvku, zvyšok sa rozptýli po jeho ploche. Ide o syntetické údaje pre vizuálne overenie.

CodeceptJS posunie prvok do zobrazenia a klikne skutočnou udalosťou myši. Návštevu môže ukončiť kliknutím na odkaz na iný článok z priečinka. Formuláre, presmerovania reklamných bannerov, sťahovanie súborov, galérie otvárajúce lightbox a skriptované odkazy sa nevyberajú. Obsah stránok ani konfigurácia CMS sa nemenia.

Pri prechode na ďalší článok alebo pri obnovení stránky server prevezme čakajúce cookies kliknutí. Po dokončení šírky test cez REST API overí prírastok uložených kliknutí pre príslušné okno alebo kontajner na každej navštívenej stránke. Výpis obsahuje ID stránky, šírku okna, počet kliknutí a výsledné skupiny šírok. Údaje zostávajú uložené pre následné prezeranie mapy.

## Kontrola v administrácii

Otvorte **Štatistika → Mapa kliknutí**, nastavte obdobie zahŕňajúce dnešok a vyberte niektorú z navštívených stránok. V poli **Šírka** môžete porovnať vytvorené skupiny.

Počet položiek nemusí zodpovedať počtu testovaných šírok okna. Napríklad okná široké 1440 a 1536 px používajú rovnaký **Kontajner 1320 px**. Širšie okná na upravených článkoch majú vlastné kontajnery: 1560 px pri okne 1920 px a 1760 px pri okne 2560 px. Podrobnosti opisuje [Mapa kliknutí](../../redactor/apps/stat/heat-map.md).
