# Seznam produktů

Aplikace Seznam produktů poskytuje přehled a správu dostupných produktů pro elektronický obchod.

![](datatable.png)

Zobrazené produkty jsou filtrovány podle zvolené **kategorie produktů**. Taková kategorie produktů je reprezentována složkou. V levém horním rohu aplikace se nachází filtr kategorie ![](select.png ":no-zoom") čímž se vlastně odfiltrují data pro danou složku ale i všechny podsložky.

Dostupné složky reprezentující kategorie jsou uspořádány do tzn. stromu, kde pod-složky jsou vždy po příslušnou rodičovskou složku.

![](select-options.png)

První možnost v seznamu je vždy hlavní sekce, která reprezentuje všechny kategorie (všechny produkty budou zobrazeny). Při zvolení kategorie (složka) se zobrazí data dané složky i všech podsložek.

## Přidání nové kategorie produktů

Přidáním nové kategorie se vytvoří podsložka, která bude umístěna pod právě zvolenou složku (kategorii).

Příklad. Máme-li právě zvolená složka ![](select-phones.png ":no-zoom") a vytvoříme nový s názvem **Android**, tak nám vznikne nová složka na adrese ![](select-phones-android.png ":no-zoom")

Novou složku přidáme tlačítkem <button class="btn btn-sm btn-outline-secondary" type="button"><span><i class="ti ti-folder-plus" ></i></span></button>. Po jeho stisknutí se zobrazí okno pro přidání složky

![](toaster-new-folder.png)

Okno obsahuje také informaci o tom, pod jakou složku se tato nová vytvoří. Po (ne)vyplnění pole v okně a potvrzení tlačítkem <button class="btn btn-primary" type="button">Potvrdit</button> mohou nastat čtyři situace:
- pokud název nové kategorie nebude zadán, vytvoření se nezdaří a zobrazena bude hláška

![](toaster-new-folder-A.png)

- pokud název nové kategorie nebude unikátní (unikátní pro danou složku), vytvoření se nezdaří a zobrazena bude hláška

![](toaster-new-folder-B.png)

- nastane-li jiná chyba, bude zobrazena hláška

![](toaster-new-folder-A.png)

- pokud vše proběhne úspěšně

![](toaster-new-folder-C.png)

## Správa produktů

Produkty jsou reprezentovány stránkami, které můžete přidávat pod konkrétní kategorie. Nadřazená složka se ve stránce automaticky přednastaví podle aktuálně zvolené kategorie (ale je možné ji změnit). Nad produkty (stránkami) je možné provádět všechny operace jak vytvořit/upravit/klonovat/importovat...

![](new-product.png)

## Důležitá nastavení

### Karta **Perex**

V kartě **Perex** je důležité nastavení:
- hodnoty **Obrázek**. Tento obrázek se zobrazí v elektronickém obchodě jako náhled produktu.

![](new-product-image.png)

- hodnoty **Značky**. Pomocí těchto značek lze v elektronickém obchodě produkty snadno filtrovat.

![](new-product-perex.png)

### Karta Atributy

V kartě atributy využijeme výběr skupiny telefonu na specifikaci zboží. Jak je to patrné na obrázku níže, pro skupinu `iPhone X` je možné nastavení barvy a paměťové varianty zařízení. Tato výběrová pole se zobrazují v elektronickém obchodě, u detailu produktu.

![](new-product-attr.png)

Více o atributech se dočtete v části [Atributy stránky](../../../webpages/doc-attributes/README.md).
