# Mapa kliknutí

Mapa kliknutí ukazuje, na které části stránky návštěvníci klikají. Otevřete **Statistika → Mapa kliknutí**, vyberte období a klikněte na název stránky. Seznam obsahuje stránky aktuální domény se zaznamenanými kliknutími, ke kterým máte přístup.

## Zapnutí měření

V šabloně nebo skupině šablon vyberte **Statistika + mapa kliknutí**. Skript se vloží automaticky. **Statistika** měří pouze návštěvnost, **Vypnuto** zastaví měření a **Zdědit nastavení** použije nadřazenou úroveň. Šablona má přednost před skupinou šablon.

Globální nastavení `statMode=none` vypne celé měření; `statEnableClickTracking=false` vypne pouze mapu kliknutí i při jejím zapnutí v šabloně.

Kliknutí se zaznamenávají pouze se souhlasem se statistickými cookies. V existující instalaci doplňte chybějící definici cookie:

- **Název cookie:** `wj_hm_*`
- **Kategorie:** Statistické
- **Typ:** `http`
- **Platnost:** 24 hodin
- **Účel:** Dočasně uchovává kliknutí do jejich odeslání pro vytvoření mapy.

Potom na webu povolte statistické cookies. Nové instalace již definici obsahují.

## Náhled

Náhled zobrazuje mapu kliknutí a panel nejklikanějších oblastí. **Šířka mapy** nabízí skupiny s počty kliknutí; ve výchozím nastavení vybere nejpoužívanější. Pevné vycentrované kontejnery seskupuje podle šířky kontejneru, ostatní podle přesné šířky okna. Skupiny zůstávají oddělené i při stejné číselné šířce. Obnovené počty mohou mít zpoždění do 30 sekund.

Rozsah měření určuje CSS selektor `statHeatMapMainContainerSelector` (výchozí hodnota `.container`). Zaznamenávají se pouze kliknutí v odpovídajících kontejnerech. Prázdná hodnota měří celou stránku; neplatný selektor nebo chybějící kontejner znamená, že se kliknutí nezaznamenávají. Selektor musí používat standardní CSS syntaxi bez rozšíření jQuery.

Pokud nelze spolehlivě určit pevnou šířku kontejneru, použije se šířka okna. Pokud v náhledu chybí odpovídající kontejner, zobrazí se upozornění a mapa se skryje; uložené údaje zůstávají zachovány.

- **Zobrazení** mění měřítko náhledu, nikoli šířku stránky.
- Náhled posouvejte kolečkem myši. Připnutá hlavička zakrývá i značky kliknutí pod ní.
- **Krytí mapy** upravuje intenzitu barevné vrstvy; **Zobrazit mapu** ji skryje nebo zobrazí.
- Při chybě načítání použijte **Obnovit náhled**.
- Tlačítkem v pravém horním rohu náhled zvětšíte na celou obrazovku nebo vrátíte zpět.

Barvy od modré po červenou vyjadřují rostoucí hustotu kliknutí. Škála se přizpůsobuje vybraným údajům a při posouvání se nemění. Barva nevyjadřuje procentuální podíl kliknutí v oblasti.

## Verze stránky

Náhled ve výchozím nastavení zobrazuje publikovaný obsah ke konci období. Pokud není dostupný, upozorní na použití aktuálního obsahu nebo nedostupnost náhledu. V poli **Verze stránky** vyberte konkrétní verzi nebo **Všechny verze**. Seznam obsahuje nejvýše 100 nejnovějších publikovaných verzí za období; starší hledejte v dřívějším období.

Kliknutí se přiřazují po celých dnech. Verze publikované ve stejný den proto mohou obsahovat stejná kliknutí; **Všechny verze** započítá každé jednou. Verzi bez kliknutí lze prohlížet s nulovým počtem. Čas plánovaného publikování je přibližný.

Šablona, hlavička, patička, nabídky, aplikace a obrázky používají aktuální zdroje. Změny rozložení či výšky obsahu mohou posunout mapu oproti původním kliknutím.

## Porovnání dvou verzí

Pokud období obsahuje alespoň dvě publikované verze, klikněte na **Porovnat verze**. V polích **Verze A** a **Verze B** vyberte odlišné verze. Náhledy sdílejí šířku, měřítko, krytí a viditelnost mapy. Posouvat je můžete samostatně.

Každá verze má vlastní počet kliknutí a barevnou škálu; stejná barva neznamená stejný počet. Počty se neupravují podle návštěvnosti ani délky platnosti verze. Verze bez kliknutí se zobrazí bez barevné vrstvy. Opětovným kliknutím na **Porovnat verze** obnovíte původní náhled.

## Počet kliknutí v oblasti

Zapněte **Vybrat oblast** a myší označte obdélník. Pod náhledem uvidíte počet a podíl kliknutí pro vybrané období, verzi a šířku. Během výběru můžete stránku posouvat kolečkem. Při porovnávání verzí je výběr nedostupný.

Výběr zrušíte vypnutím **Vybrat oblast**, tlačítkem **Zrušit oblast**, změnou verze či šířky nebo obnovením náhledu.

## Nejklikanější oblasti

Tlačítkem <button class="btn btn-sm btn-primary" type="button" title="Oblasti" aria-label="Oblasti"><i class="ti ti-list-numbers" aria-hidden="true"></i></button> zobrazíte nebo skryjete nejvýše pět nejklikanějších nepřekrývajících se oblastí s počty a podíly kliknutí pro aktuální výběr. Kliknutím na řádek se přesunete na označenou oblast. Skrytím panelu získáte více prostoru pro náhled. Při porovnávání verzí je panel skrytý.

## Přenos a omezení

Kliknutí čekají v cookies na další požadavek na server. Poslední kliknutí před odchodem nebo při obsluze z mezipaměti se nemusí odeslat. Limit je 16 cookies, celkem 2 KiB, s platností 24 hodin; při překročení se nejstarší neodeslaná kliknutí zahodí. Mapa nezaznamenává pohyb myši ani relace.

Správce může [sloučit historická kliknutí](../../../admin/settings/cronjob/README.md#slučování-historických-kliknutí) nebo odstranit údaje nástrojem pro čištění databáze. `statHeatMapCellSize` určuje velikost buňky při vykreslování i slučování (výchozí hodnota `5`, tedy 5 × 5 CSS pixelů). Větší buňky snižují přesnost; pozdější zmenšení neobnoví sloučené souřadnice.

## Původní údaje

Staré záznamy zůstávají zachovány. Kliknutí bez šířky okna najdete pod **1920 px**; jejich poloha je orientační. Zpětně se nepřepočítávají ani neslučují s kontejnerovými skupinami.

Doména se přiřadí podle aktuální domény zdrojové stránky. Záznamy bez dohledatelné stránky nebo domény zůstanou uložené, ale nezobrazí se. Historické změny domény se neobnovují.
