# Mapa kliknutí

Mapa kliknutí zobrazuje místa, na která návštěvníci klikají. Otevřete **Statistika → Mapa kliknutí**, vyberte období a klikněte na název stránky. Seznam obsahuje pouze stránky aktuální domény se zaznamenanými kliknutími, ke kterým máte přístup.

## Zapnutí měření

V nastavení šablony nebo skupiny šablon vyberte režim **Statistika + mapa kliknutí**. Skript se vloží automaticky. Režim **Statistika** zaznamenává běžnou návštěvnost, **Vypnuto** vypne měření stránky. **Zdědit nastavení** použije nadřazenou úroveň; šablona má přednost před skupinou šablon. Globální vypnutí statistiky má přednost.

Konfigurační proměnná `statMode=none` vypne celé měření. Hodnota `statEnableClickTracking=false` vypne mapu kliknutí i při jejím zapnutí v šabloně; běžná statistika zůstane zapnutá podle nastaveného režimu.

Mapa respektuje nastavení souhlasu se statistickými cookies. Bez povolení se kliknutí nezaznamenávají. Běžná statistika zachovává dosavadní pravidla anonymizace.

V existující instalaci přidejte ve správě cookies definici pro mapu kliknutí, pokud ještě neexistuje:

- **Název cookie:** `wj_hm_*`
- **Kategorie:** Statistické
- **Typ:** `http`
- **Platnost:** 24 hodin
- **Účel:** Dočasně uchovává kliknutí na stránce do jejich odeslání pro vytvoření mapy kliknutí.

Definice zastupuje cookies jednotlivých kliknutí s náhodnou příponou. Cookie lišta nabízí kategorie podle evidovaných definic cookies; po přidání proto otevřete nastavení souhlasu na webu a povolte statistické cookies. Nové instalace obsahují tuto definici mezi výchozími záznamy.

## Náhled

Konfigurační proměnná `statHeatMapMainContainerSelector` určuje CSS selektor měřených kontejnerů. Výchozí hodnota je `.container`, používaná v Bootstrapu. Již uložené vlastní hodnoty zůstávají platné. Prázdná hodnota zapne měření celé stránky podle přesné šířky okna.

Při vyplněném selektoru se měří pouze kliknutí v nejbližším odpovídajícím kontejneru. Kliknutí mimo jeho hranice se ignorují, i když jde o obsah přesahující kontejner. Pokud selektor na stránce nic nenajde nebo není platný, stránka kliknutí nezaznamenává. Původní selektory využívající rozšíření jQuery je třeba nahradit standardními CSS selektory.

Pevné kontejnery vycentrované vůči stránce se seskupují podle naměřené šířky. Například okna široká 1223 a 1369 px se stejným kontejnerem širokým 1140 px vytvoří jednu skupinu **Kontejner 1140 px**. Poloha X se měří od levého okraje kontejneru včetně jeho vnitřního odsazení; poloha Y zůstává vůči začátku stránky. Pevná šířka se rozpoznává podle dosažené hodnoty CSS `max-width` nebo `width` s deklaracemi v pixelech.

Pokud prohlížeč nemůže přečíst některý načtený styl, například CSS z cizí domény bez povoleného přístupu, na celé stránce se použije přesná šířka okna. Platí to i pro samostatný externí styl s fonty. Přesná šířka okna se použije i tehdy, když se pro danou vlastnost kombinují pixelové a relativní deklarace. Stejně tak se neslučují šířky odvozené přes `vw`, CSS proměnné či výpočty a kontejnery s aktivní CSS animací.

Plynulé kontejnery a kontejnery, které nejsou vycentrované vůči stránce, zachovávají původní souřadnice i přesnou šířku okna. Mobilní šířky 390 a 430 px tak zůstávají v samostatných skupinách **Okno 390 px** a **Okno 430 px**. I v tomto režimu se při vyplněném selektoru ignorují kliknutí mimo kontejnery.

Výběr šířky uvádí počet kliknutí a ve výchozím nastavení zobrazí nejpoužívanější skupinu. Při obnovení náhledu se až 30 sekund mohou zobrazit počty z předchozího načtení. Kontejnerové souřadnice se nemíchají s původními souřadnicemi stránky, ani když mají skupiny stejnou číselnou šířku.

Náhled kontejnerové skupiny se otevře v nejčastější skutečně zaznamenané šířce okna dané skupiny; při rovnosti počtů se použije menší šířka. Tím zůstane zachován příslušný breakpoint. K poloze X se přičte odsazení kontejneru v náhledu. Pokud náhled neobsahuje odpovídající pevný vycentrovaný kontejner, stránka se zobrazí bez vrstvy kliknutí a s upozorněním. Uložené údaje zůstávají zachovány.

Náhled má vlastní posouvání. Když se obsah posune pod připnutou hlavičku, hlavička zakryje i značky kliknutí na tomto obsahu. Ovladač **Zobrazení** upravuje pouze jeho zmenšení na obrazovce; vnitřní šířka stránky zůstává zachována. Krytí mapy můžete měnit nebo mapu dočasně skrýt. Obnovte náhled, pokud se nepodařilo načíst stránku nebo část mapy.

Náhled ve výchozím nastavení zobrazí publikovanou verzi stránky ke konci vybraného období. Pokud ji nelze určit, zobrazí se upozornění na použití aktuálního obsahu nebo na nedostupný náhled.

Pokud se stránka během období měnila, je v poli **Verze stránky** vybrána možnost **Všechny verze**. Filtr můžete použít i tehdy, když jediná dostupná verze platila jen po část vybraného období. Filtr nabízí nejvýše 100 nejnovějších publikovaných verzí za vybrané období. Pokud hledáte starší verzi, vyberte dřívější období. Vyberte verzi podle čísla a času publikování. Náhled zobrazí její obsah. Mapa i počty u jednotlivých šířek zahrnují všechna kliknutí ze dnů, kdy byla verze ve vybraném období zveřejněna. Pokud nová verze vyšla v poledne, obě verze zobrazí všechna kliknutí z tohoto dne. V zobrazení **Všechny verze** se každé kliknutí započítá jednou.

Pokud v těchto dnech nebyla zaznamenána žádná kliknutí, její uložený obsah si můžete prohlédnout v každé šířce zaznamenané v daném období. U každé šířky se zobrazí nula kliknutí.

Čas publikování se odvozuje z dostupných údajů historie. U plánovaného publikování jde o přibližný čas, protože historie neuchovává přesný okamžik provedení úlohy.

Šablona, hlavička, patička, nabídky, aplikace a obrázky používají aktuální zdroje. Nejde o historický snímek celého webu. Změny obsahu, pohyblivé prvky a jiná výška okna mohou ovlivnit přesnost pozic. Absolutní poloha Y se nepřepočítává: změna výšky obsahu nad kliknutím může posunout mapu i při stejné šířce kontejneru.

## Přenos a omezení

Kliknutí se dočasně ukládají do cookie a odešlou při dalším běžném požadavku na server. Každé kliknutí nevyvolává samostatný požadavek. Poslední kliknutí před odchodem z webu mohou zůstat neodeslaná. Stránky obsloužené výhradně z mezipaměti před serverem nemusí cookie předat ke zpracování.

V prohlížeči čeká nejvýše 16 cookies kliknutí o celkové velikosti do 2 KiB s platností 24 hodin. Při překročení limitu se nejstarší neodeslaná kliknutí zahodí. Správce může po skončení měsíce zmenšit měsíční tabulky [automatizovanou úlohou pro slučování kliknutí](../../../admin/settings/cronjob/README.md#slučování-historických-kliknutí). Úloha zapíše součet kliknutí ve skupině do řádku s nejnižším ID a zachová jeho souřadnice. K odstranění údajů za zvolené období slouží nástroj pro čištění databáze.

Velikost buňky pro vykreslování i slučování určuje `statHeatMapCellSize` (výchozí hodnota `5`, tedy 5 × 5 CSS pixely). Hodnotou `4` nastavíte 4 × 4 a hodnotou `10` nastavíte 10 × 10. Větší buňky snižují přesnost polohy; pozdější zmenšení neobnoví souřadnice již sloučených kliknutí.

Mapa nesleduje pohyb myši ani nezaznamenává jednotlivé relace.

## Původní údaje

Existující měsíční tabulky `stat_clicks_YYYY_M` se rozšíří o nové údaje a používají se i k dalšímu zaznamenávání kliknutí. Původní záznamy zůstanou zachovány. Kliknutím bez evidované šířky okna se přiřadí desktopová šířka **1920 px**, pod kterou je najdete ve výběru šířky.

Původní souřadnice se nemění. Mohly být měřeny vůči obalovému prvku stránky, jehož polohu už nelze zpětně zjistit. Staré mapy při šířce 1920 px jsou proto orientační; tato šířka nepředstavuje dodatečně zjištěnou velikost původního okna.

Kliknutí zaznamenaná před zavedením kontejnerových skupin se zpětně nepřepočítávají. Zůstanou oddělená od kontejnerových skupin. Nové záznamy uchovávají režim souřadnic v `coordinate_mode`, šířku skupiny v `viewport_width` a skutečnou šířku okna v `source_viewport_width`. Čekající cookies staršího formátu se po dobu své platnosti nadále zpracovávají jako souřadnice stránky.

Doména starých záznamů se přiřadí podle aktuální domény zdrojové stránky. Pokud stránku nebo její doménu nelze dohledat, záznamy zůstanou uložené, ale v mapě kliknutí se nezobrazí. Historické změny domény se zpětně nerekonstruují.
