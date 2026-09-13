# Page Builder

Page Builder je speciální režim editace stránek. V tomto režimu se neupravuje celá stránka, ale jen její vybrané části. Page Builder odděluje úpravu textů a obrázků od struktury stránky. Při úpravě textu tak omylem nesmažete strukturální prvky.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/ieaNWY57Exc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Režim je třeba aktivovat, připravit bloky a nastavit šablony. Postup je v sekci pro [web designéra](../../frontend/page-builder/README.md).

Pokud šablona používá Page Builder, při otevření webové stránky v editoru se načte tento režim.

![](pagebuilder.png)

Ve výběru **Editor** můžete přepínat mezi režimy **Page Builder**, **Standardní** a **HTML**. Zvolený režim se zapamatuje i pro další otevřené Page Builder stránky. Pokud chcete znovu skládat stránku z bloků, vyberte **Page Builder**.

Při přepnutí se aktuální obsah přenese do zvoleného editoru. Můžete tak například upravit HTML a pokračovat v Page Builderu. Změny stránky uložíte tlačítkem **Uložit**.

## Základní práce

V Page Builderu vytváříte webovou stránku z připravených bloků. Při přejetí myší přes obsah se zobrazí jemný průsvitný rámeček. Ukazuje hranice bloku, do kterého můžete kliknout; zelený rámeček pomáhá najít části určené k úpravě textu.

Kliknutím do textu vyberete blok a zároveň můžete ihned psát. Vybraný blok má výraznější rámeček a zobrazí se v cestě v horní liště. Přejetí myší na jiný blok výběr nemění. Při psaní se rámeček výběru zjemní a pomocný rámeček pod myší se skryje.

![Vybraný sloupec vlevo a jemný rámeček pod myší vpravo](pagebuilder-hover.png)

Ve výchozím nastavení barva označuje typ bloku:

- Modrá představuje sekci, hlavní stavební blok, obvykle přes celou šířku obrazovky.
- Růžová představuje kontejner, který obsahuje řádky se sloupci a bývá užší než sekce kvůli čitelnosti textu.
- Šedá představuje řádek, který uspořádává sloupce v kontejneru.
- Zelená představuje sloupec nebo samostatný upravitelný text. Sloupec obvykle obsahuje text, obrázky nebo aplikace.
- Oranžová představuje duplikovatelnou položku, například položku seznamu.

![](pagebuilder.png)

Nástroje vybraného bloku jsou v pevné liště pod CKEditorem. Klikatelná cesta, například **Sekce › Kontejner › Řádek › Sloupec**, umožňuje vybrat nadřazenou část bez hledání rámečků v textu. Na úzké obrazovce otevřete seznam nadřazených bloků tlačítkem vedle aktuálního typu bloku. Lišta obsahuje:

- Struktura - otevře strom bloků stránky.
- Přidat blok (`+`) - zobrazí místa pro vložení sekce, kontejneru nebo sloupce přímo ve stránce, bez předchozího výběru bloku.
- Šířka sloupce - zobrazuje aktuální šířku, například `3 / 12`, a otevře její nastavení pro zvolenou velikost zařízení.
- Duplikovat vedle - vloží kopii hned za vybraný blok a označí ji.
- Další akce (`…`) - otevře další operace s vybraným blokem.
- Rámečky (ikona oka nebo vrstev) - přepíná rozsah zobrazených rámečků.

V nabídce **Další akce** najdete podle typu výběru:

- Styl - nastavení pozadí, barev, zarovnání, odsazení a dalších vlastností.
- Vložit blok před / Vložit blok za - otevře knihovnu stejného typu bloku na zvolené straně výběru.
- Posunout výš / Posunout níž - posune blok o jednu pozici v pořadí.
- Přesunout / Duplikovat - zobrazí dostupná cílová místa ve stránce; kliknutím vyberete, kam blok přesunout nebo vložit kopii. Výběr místa zrušíte klávesou **Escape**.
- Přidat do oblíbených - uloží blok mezi vaše oblíbené bloky v knihovně.
- Smazat - smaže označený blok.

Nabídka obsahuje pouze podporované operace. Běžný řádek slouží k orientaci; duplikovatelný řádek a položka podporují přesun, duplikování a smazání. Samostatný upravitelný text nemá nástroje pro úpravu struktury. Duplikovatelné prvky lze přesouvat pouze mezi kompatibilními sourozenci stejného rodiče.

## Nastavení stylu

Přes **Další akce → Styl** otevřete kompaktní okno vlastností. Název, například **Styl sloupce**, a text pod ním označují upravovaný blok. Tlačítko **Najít na stránce** posune obsah k tomuto bloku a krátce ho zvýrazní. Okno můžete přesunout tažením za hlavičku.

![](pagebuilder-style.png)

Vlastnosti jsou v rozbalovacích skupinách: **Identifikace**, **Pozadí**, **Viditelnost**, **Odsazení**, **Zarovnání**, **Rozměry**, **Orámování**, **Zaoblení**, **Stín**, **Propojené styly** a **Z-index**. Při otevření je rozbalená **Identifikace** s ID, CSS třídami a titulkem. Web designér může seznam i pořadí skupin přizpůsobit šabloně.

Můžete rozbalit více skupin současně. Jejich sbalení nezahodí rozpracované hodnoty. Při posouvání vlastností zůstává hlavička i spodní tlačítka dostupná. Obrázek pozadí vyberete tlačítkem u jeho adresy. U odsazení, orámování a zaoblení můžete propojit všechny čtyři hodnoty nebo dvojice; propojené hodnoty se přebírají z prvního pole příslušné skupiny.

![Nastavení odsazení s propojenými hodnotami](pagebuilder-style-settings.png)

Změny průběžně vidíte ve stránce. Tlačítko **Uložit** v tomto okně potvrdí styl v rozpracované stránce; celou stránku následně uložíte hlavním tlačítkem **Uložit** v editoru. **Zrušit**, křížek v hlavičce nebo **Escape** zavřou okno a vrátí nepotvrzené úpravy stylu. **Resetovat** odstraní uživatelský styl daného bloku, aby se znovu uplatnily styly šablony.

## Vkládání bloků

Nový blok přidáte bez předchozího označení sekce nebo sloupce:

1. Klikněte na **Přidat blok** (`+`) v horní liště. Nástroje a cestu k bloku dočasně nahradí modrý pomocník s pokynem k výběru místa a tlačítkem **Ukončit · Esc**.
2. Ve stránce klikněte na plus v místě vložení. Modré pásy označují sekce, růžové kontejnery a zelená plus sloupce. Místa jsou před prvním blokem, mezi sousedními bloky i za posledním blokem. Název u pásu a popis tlačítka pomáhají určit typ a pozici.
3. V knihovně klikněte na kartu požadovaného bloku. Knihovna nabízí příslušný typ a nahoře připomíná místo vložení. Kliknutí blok rovnou vloží bez dalšího potvrzení. Režim se ukončí, nový blok se označí a můžete upravovat jeho obsah.

![Místa pro přidání sekce, kontejneru a sloupce](pagebuilder-insert.png)

Knihovnu můžete otevřít i pro označený blok přes **Další akce → Vložit blok před / Vložit blok za**. Obsahuje karty:

- Základní - jednoduché bloky různých velikostí.
- Knihovna - bloky vytvořené pro váš web.
- Oblíbené - bloky, které jste označili jako oblíbené.

![](pagebuilder-library.png)

Knihovna se otevírá jako úzké okno nad stránkou. Hlavička uvádí například **Vložit sekci** a pod ní místo vložení. Okno přesunete tažením za hlavičku. Zavřete ho křížkem vpravo nahoře nebo klávesou **Escape**.

V kartě **Knihovna** jsou bloky seskupené do kategorií s počtem bloků. Kliknutím kategorii rozbalíte; otevřená zůstává vždy jen jedna. Karty zobrazují název a náhled v původním poměru stran. Kliknutím na náhled nebo název vložíte blok do stránky.

Pole **Hledat blok…** filtruje podle názvu a lze ho kombinovat s jedním štítkem. Při filtrování zůstane otevřená vyhovující kategorie nebo se otevře první s výsledkem. Počty u kategorií zohledňují filtr, počty u štítků označují celkový počet bloků s daným štítkem. **Všechny** zruší pouze štítek a ponechá hledaný text. Pokud se nic nenajde, tlačítko **Vymazat filtry** zruší text i štítek. Při posouvání výsledků zůstává vyhledávání a výběr štítků dostupný.

![Vyhledávání bloků v kombinaci se štítkem](pagebuilder-library-filter.png)

Názvy a štítky definuje web designér v souboru `pagebuilder.properties` při [vytváření bloků](../../frontend/page-builder/blocks.md#název-a-značky-bloku). Patří ke knihovně dané šablony a nemusí se měnit s jazykem administrace. V kartě **Oblíbené** odstraníte uložený blok tlačítkem u jeho názvu; odstranění vyžaduje potvrzení.

Na konci stránky se zobrazuje ikona `+` pro snadné přidání nové sekce.

![](pagebuilder-plusbutton.png)

Při zapnutí vkládání se plynule rozbalí dočasné mezery bez odscrollování viditelných míst. Šířky sloupců se nemění: plus se zobrazují v mezerách mezi nimi, při zalomení mezi řádky a při nedostatku místa nad obsahem. Rámečky výběru i rámečky pod myší se při výběru místa skryjí.

Zavřením knihovny se vrátíte na vybrané plus. Vkládání ukončíte tlačítkem **Ukončit · Esc** nebo klávesou **Escape**. Mezery se plynule sbalí, obnoví se běžná lišta a fokus se vrátí na `+`. Kliknutí do obsahu ukončí režim a vybere daný blok. Pokud máte v systému omezené animace, zobrazení a skrytí proběhne okamžitě. Pomocné pásy se neukládají ani nezobrazují v náhledu.

## Struktura stránky a klidné zobrazení

Tlačítko **Struktura** otevře strom bloků s názvy odvozenými z obsahu. Bloky vyhledáte podle názvu nebo typu. Kliknutí na položku vybere blok, posune stránku na jeho místo a rozbalí zavřenou větev. Opakované kliknutí ji nechá otevřenou; šipkou vedle názvu ji rozbalíte nebo sbalíte bez změny výběru.

Vybraná položka má tenký rámeček v barvě svého typu, který odpovídá rámečku ve stránce. Panel se otevírá nad obsahem a nemění šířku ani zalomení stránky. Na úzké obrazovce se po výběru zavře.

![Strom stránky s označeným sloupcem](pagebuilder-structure.png)

Skryté bloky mají označení **Skrytý**. Jejich výběr nemění viditelnost ani aktivní kartu stránky. Strom nepřidává do HTML nové názvy nebo identifikátory a neumožňuje přesun tažením myší. K přesunu použijte akce horní lišty.

Ve stromu se pohybujete šipkami nahoru a dolů. Větve otevíráte a zavíráte šipkami doprava a doleva. **Enter** nebo mezerník vybere položku a rozbalí zavřenou větev. **Escape** zavře otevřenou nabídku nebo panel, ukončí nastavení šířky nebo zruší výběr místa přesunu či vložení.

Tlačítko s ikonou oka postupně přepíná tři režimy:

- **Rámeček vybraného bloku** (výchozí) - zvýrazní výběr a při přejetí myší jemně naznačí další blok.
- **Žádné rámečky** (přeškrtnuté oko) - skryje výběr i pomocné rámečky pod myší.
- **Rámečky celé hierarchie aktivního bloku** (ikona vrstev) - zobrazí i nadřazenou sekci, kontejner a řádek. Hierarchii ukáže i pod myší jemnějšími čarami. Společné nadřazené bloky nezvýrazňuje dvakrát.

Prohlížeč si volbu pamatuje i po opětovném otevření editoru. Lišta, panel Struktura a zalomení obsahu se přepínáním nemění. V **Náhledu** ani na uložené stránce se pomocné rámečky a ovládací prvky nezobrazují.

## Nastavení šířky sloupců

Editor umožňuje nastavit odlišnou šířku pro mobil, tablet a počítač:

1. U výběru **Editor** zvolte ikonou velikost zařízení.
2. Klikněte do sloupce a v liště vyberte **Šířka sloupce**, například `3 / 12`. Nástroje a cestu nahradí modrý pomocník, podobně jako při vkládání. Vysvětluje upravovaný rozměr, například **MD — Tablet (768–1199 px)**.
3. Šipkami uvnitř sloupců změňte šířku. Hodnota udává počet dílů mřížky; ve výchozí 12dílné mřížce je `3 / 12` čtvrtina a `12 / 12` celá šířka. Dostupná je i hodnota `auto` pro automatické přizpůsobení.
4. Režim ukončíte tlačítkem **Ukončit · Esc** nebo klávesou **Escape**. Obnoví se běžná lišta i původní nastavení rámečků.

Zelené rámečky při nastavení označují všechny sloupce v daném kontejneru, jejichž šířku můžete měnit, i když máte běžné rámečky skryté. Ovladače s jemným zeleným pozadím mají vyhrazené místo uvnitř sloupce nad obsahem. V úzkých sloupcích se hodnota zobrazí nad šipkami. Po ukončení režimu se místo pro ovladače uvolní.

![Nastavení šířky s modrým pomocníkem a ovladači uvnitř sloupců](pagebuilder-width.png)

Zařízení můžete přepnout i během nastavování. Ovladače a pomocník se aktualizují a další změny platí pro zvolené zařízení.

![](pagebuilder-switcher.png)

Zkratka u hodnoty šířky označuje upravovanou velikost zařízení. Výchozí nastavení:

| Zařízení | Označení | Šířka náhledu | CSS třída |
| --- | --- | --- | --- |
| Počítač | `XL` | od 1200 px | `col-xl-` |
| Tablet | `MD` | 768–1199 px | `col-md-` |
| Mobil | bez zkratky | méně než 768 px | `col-` |

Šablona může používat vlastní označení a hranice rozlišení. Pomocník pak ukáže označení podle šablony, například `SM`, bez výchozích hranic z tabulky.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/aru-B1vxReo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

## Rozdělení sloupce

Funkce **Rozdělit sloupec** patří k blokům vkládaným do textu:

1. Umístěte textový kurzor na místo rozdělení.
2. V liště CKEditoru klikněte na **Bloky**.
3. V knihovně přejděte na kartu **Základní** a vyberte **Rozdělit sloupec**.

Obsah se rozdělí do dvou sloupců v místě kurzoru. Nemusíte ručně vytvářet sloupec a přesouvat do něj text. Tlačítko **Bloky** používejte i pro připravené textové části; `+` v liště Page Builderu přidává sekce, kontejnery a sloupce.
