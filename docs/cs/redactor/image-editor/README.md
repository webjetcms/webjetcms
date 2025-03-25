# Editor obrázků

Editor obrázků umožňuje přímo v prohlížeči provést pokročilé úpravy obrázků na serveru. Použitý je v galerii, lze jej však vyvolat při editaci web stránky kliknutím na obrázek a následným kliknutím na ikonu editace obrázku, nebo z průzkumníka pravým kliknutím na obrázek a zvolením možnosti Upravit.

![](editor-preview.png)

Dostupné jsou následující nástroje:
- ![](tie-btn-resize.png ":no-zoom"), **Změnit velikost**: můžete upravit velikost obrázku. Upozorňujeme, že ve výchozím nastavení je obrázek zobrazen na maximální velikost 1000 bodů na šířku, takže úprava rozměru nemusí být viditelná, protože obrázek může být reálně větší než zobrazená velikost.
  - Velikost lze změnit na základě definovaných šablon velikostí (pokud opakovaně potřebujete nastavit přesný rozměr obrázku). Seznam možností se nastavuje v konfigurační proměnné `imageEditorSizeTemplates`, ve výchozím nastavení na hodnotu `80x80;640x480;800x600;`.
  - Možnost svázané rozměry zachová poměr stran, v případě použití šablony se nastaví šířka a výška se dopočítá podle poměru stran aktuálního obrázku.
- ![](tie-btn-crop.png ":no-zoom"), **Oříznout**: Po kliknutí na tuto funkci můžete oříznout fotografii v galerii.
  - Fotografie lze oříznout do libovolného obdélníku, nebo si můžete vybrat z předdefinovaných poměrů stran: čtverec, 3:2, 4:3, 5:4, 7:5, 16:9.
  - Poměry stran lze definovat v konfigurační proměnné `imageEditorRatio`, ve výchozím nastavení na hodnotu `3:2, 4:3, 5:4, 7:5, 16:9`.
- ![](tie-btn-flip.png ":no-zoom"), **Převrátit**: Tato funkce umožňuje převrátit fotografii horizontálně (Převrátit X) nebo vertikálně (Převrátit Y), čímž vytvoříte zrcadlový obraz.
- ![](tie-btn-rotate.png ":no-zoom"), **Otočit**: V této funkci můžete pomocí posuvníku vybrat hodnotu ve stupních od -360 do 360, nebo využít tlačítka, která rotují fotografii o 30 stupňů ve směru nebo proti směru hodinových ručiček.
- ![](tie-btn-draw.png ":no-zoom"), **Kreslení**: Tato funkce vám umožňuje kreslit na fotografii volně nebo rovnou čáru. Můžete si vybrat barvu a tloušťku čáry (rozsah).
- ![](tie-btn-shape.png ":no-zoom"), **Tvar**: Pomocí této funkce můžete přidávat na fotografii různé tvary jako například kruhy, obdélníky nebo trojúhelníky. Barvu tahu a výplně můžete měnit podle potřeby, přičemž lze specifikovat tloušťku tahu. Vytvořené objekty lze přesouvat, zmenšovat, zvětšovat a rotovat, včetně objektů vytvořených pomocí funkce Kreslit.
- ![](tie-btn-icon.png ":no-zoom"), **Ikona**: Funkce Ikona umožňuje přidávat na fotografii různé ikony a nálepky z předdefinované knihovny nebo vlastní nahrané ikony. Barvu můžete měnit dle potřeby. Vytvořené objekty lze přesouvat, zmenšovat, zvětšovat a rotovat.
- ![](tie-btn-text.png ":no-zoom"), **Text**: Pomocí této funkce můžete přidávat text na fotografii. Text můžete formátovat na tlustý, kurzívu nebo podtržený. Zvolit lze barva a velikost.
- ![](tie-btn-mask.png ":no-zoom"), **Maska**: Funkce Maska umožňuje aplikovat různé masky na fotografii, které mohou sloužit ke skrytí nebo zvýraznění částí fotografie.
- ![](tie-btn-filter.png ":no-zoom"), **Filtr**: Použitím této funkce můžete aplikovat na fotografii různé filtry, které upraví její vzhled. Mezi filtry, ze kterých lze vybírat, patří: černobílý filtr, sépie, rozmazání, ražení, negativ, zaostření. Nastavit umíte odstranění bílé, jas, šum, zrnitost a filtr barev. Dále lze nastavit Tint, Multiply a Blend pro každou barvu.
  - **Tónování**: Umožňuje aplikovat na fotografii barevný odstín. Můžete nastavit intenzitu barvy pomocí opacity (průhlednosti).
  - **Násobení**: Tento efekt zvyšuje tmavost obrazu kombinováním barev fotografie s barevnou vrstvou. Výsledná barva je vždy tmavší.
  - **Míchání**: Umožňuje kombinovat dvě vrstvy obrazu pomocí různých režimů:
    - **Add**: Přidává barvy dvou vrstev. Výsledek je světlejší.
    - **Diff**: Zobrazuje rozdíly mezi vrstvami.
    - **Subtract**: Odečítá barvy jedné vrstvy od druhé. Výsledek je tmavší.
    - **Multiply**: Kombinuje barvy vrstev a vytváří tmavší výsledek.
    - **Screen**: Invertuje barvy, vynásobí je, a pak opět invertuje. Výsledek je světlejší.
    - **Lighten**: Zobrazuje světlejší barvy ze dvou vrstev.
    - **Darken**: Zobrazuje tmavší barvy ze dvou vrstev.
