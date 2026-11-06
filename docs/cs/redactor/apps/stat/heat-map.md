# Mapa kliknutí

Mapa kliknutí zobrazuje místa, na která návštěvníci klikají. Otevřete **Statistika → Mapa kliknutí**, vyberte období a klikněte na název stránky. Seznam obsahuje pouze stránky aktuální domény se zaznamenanými kliknutími, ke kterým máte přístup.

## Zapnutí měření

V nastavení šablony nebo skupiny šablon vyberte režim **Statistika + mapa kliknutí**. Skript se vloží automaticky. Režim **Statistika** zaznamenává běžnou návštěvnost, **Vypnuto** vypne měření stránky. **Zdědit nastavení** použije nadřazenou úroveň; šablona má přednost před skupinou šablon. Globální vypnutí statistiky má přednost.

Konfigurační proměnná `statMode=none` vypne celé měření. Hodnota `statEnableClickTracking=false` vypne mapu kliknutí i při jejím zapnutí v šabloně; běžná statistika zůstane zapnutá podle nastaveného režimu.

Mapa respektuje nastavení souhlasu se statistickými cookies. Bez povolení se kliknutí nezaznamenávají. Běžná statistika zachovává dosavadní pravidla anonymizace.

## Náhled

Nová kliknutí se evidují samostatně pro každou přesnou šířku okna v CSS pixelech. Výběr šířky uvádí i počet kliknutí a předvolí nejpoužívanější šířku. Při obnovení náhledu se až 30 sekund mohou zobrazit počty z předchozího načtení. Náhled má vlastní posouvání. Zmenšení náhledu zachovává vnitřní šířku stránky. Krytí mapy lze měnit, mapu skrýt a náhled obnovit.

Náhled ve výchozím nastavení zobrazí publikovanou verzi stránky ke konci vybraného období. Pokud ji nelze určit, zobrazí se upozornění na použití aktuálního obsahu nebo na nedostupný náhled.

Pokud se stránka během období měnila, je v poli **Verze stránky** vybrána možnost **Všechny verze**. Filtr můžete použít i tehdy, když jediná dostupná verze platila jen po část vybraného období. Filtr nabízí nejvýše 100 nejnovějších publikovaných verzí za vybrané období. Pokud hledáte starší verzi, vyberte dřívější období. Vyberte verzi podle čísla a času publikování. Náhled zobrazí její obsah. Mapa i počty u jednotlivých šířek zahrnují všechna kliknutí ze dnů, kdy byla verze ve vybraném období zveřejněna. Pokud nová verze vyšla v poledne, obě verze zobrazí všechna kliknutí z tohoto dne. V zobrazení **Všechny verze** se každé kliknutí započítá jednou.

Pokud v těchto dnech nebyla zaznamenána žádná kliknutí, její uložený obsah si můžete prohlédnout v každé šířce zaznamenané v daném období. U každé šířky se zobrazí nula kliknutí.

Čas publikování se odvozuje z dostupných údajů historie. U plánovaného publikování jde o přibližný čas, protože historie neuchovává přesný okamžik provedení úlohy.

Šablona, hlavička, patička, nabídky, aplikace a obrázky používají aktuální zdroje. Nejde o historický snímek celého webu. Změny obsahu, pohyblivé prvky a jiná výška okna mohou ovlivnit přesnost pozic.

## Přenos a omezení

Kliknutí se dočasně ukládají do cookie a odešlou při dalším běžném požadavku na server. Každé kliknutí nevyvolává samostatný požadavek. Poslední kliknutí před odchodem z webu mohou zůstat neodeslaná. Stránky obsloužené výhradně z mezipaměti před serverem nemusí cookie předat ke zpracování.

V prohlížeči čeká nejvýše 16 cookies kliknutí o celkové velikosti do 2 KiB s platností 24 hodin. Při překročení limitu se nejstarší neodeslaná kliknutí zahodí. Údaje uložené v měsíčních databázových tabulkách se automaticky nemažou; odstranit je můžete existujícím nástrojem pro čištění databáze.

Mapa nesleduje pohyb myši ani nezaznamenává jednotlivé relace.

## Původní údaje

Existující měsíční tabulky `stat_clicks_YYYY_M` se rozšíří o nové údaje a používají se i k dalšímu zaznamenávání kliknutí. Původní záznamy zůstanou zachovány. Kliknutím bez evidované šířky okna se přiřadí desktopová šířka **1920 px**, pod kterou je najdete ve výběru šířky.

Původní souřadnice se nemění. Mohly být měřeny vůči obalovému prvku stránky, jehož polohu už nelze zpětně zjistit. Staré mapy při šířce 1920 px jsou proto orientační; tato šířka nepředstavuje dodatečně zjištěnou velikost původního okna.

Doména starých záznamů se přiřadí podle aktuální domény zdrojové stránky. Pokud stránku nebo její doménu nelze dohledat, záznamy zůstanou uložené, ale v mapě kliknutí se nezobrazí. Historické změny domény se zpětně nerekonstruují.
