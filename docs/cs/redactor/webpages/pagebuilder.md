# Page Builder

Page Builder je speciální režim editace stránek. V tomto režimu není editována celá stránka ale jen její vybrané části. Page Builder odděluje editaci textů/obrázků a struktury stránek. Nestane se tak to, že omylem smažete strukturální elementy web stránky při editaci jejího textu.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/ieaNWY57Exc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Režim je třeba aktivovat, připravit bloky a nastavit šablony, postup je v sekci pro [web designéra](../../frontend/page-builder/README.md).

Při nastavení možnosti použití Page Builder pro šablonu se při otevření webové stránky v editoru načte režim Page Builder.

![](pagebuilder.png)

V případě potřeby je v okně možnost přepnout editor do standardního režimu. Přepnutí z PageBuilder režimu na režim Standardní se zapamatuje do znovu obnovení stránky. I jiná PageBuilder stránka se následně zobrazí ve standardním režimu. Přepnutí zpět na režim PageBuilder musíte provést přepnutím výběrového pole nebo obnovením celé stránky.

!>**Upozornění:** obsah editorů není během vašich úprav synchronizován. Oba načítají stejný obsah pouze při otevření okna. Nemůžete tedy začít provádět změny v Page Builder a pak se přepnout do standardního editoru a tam provést další změny.

## Základní práce

Při použití Page Builder vytváříte web stránku z předem připravených bloků. Přechodem kurzoru přes blok se zvýrazní jednotlivé části, kde:
- Modrá barva reprezentuje sekci – hlavní stavební blok, obvykle na celou šířku obrazovky.
- Červená barva reprezentuje kontejner - blok určený pro vkládání sloupců, je obvykle užší než sekce pro lepší čitelnost textů na stránce.
- Zelená barva reprezentuje sloupec – ten již obsahuje typicky editovatelný text, obrázky nebo aplikace.

![](pagebuilder.png)

Každá zvýrazněná část zobrazuje ikonu ozubeného kola, klepnutím na ozubené kolo se zobrazí nástrojová lišta. Ta obsahuje možnosti:
- Styl – umožní pokročile nastavit styl/vlastnosti bloku jako obrázek pozadí, barvy, zarovnání, odsazení a podobně.
- Šířka sloupce - nastavuje šířku sloupce, lze nastavit různé šířky pro různá zařízení (telefon, tablet, počítač).
- Přesun bloku – umožňuje přesunout blok na jiné místo ve stránce, po kliknutí na ikonu se zobrazí možnosti kam je blok možné přesunout.
- Duplikovat - duplikuje označený blok.
- Oblíbené - přidá blok do seznamu oblíbených bloků.
- Smazat - smaže označený blok.

![](pagebuilder-style.png)

Po kliknutí na ozubené kolo se také zobrazí ikony + pro vložení nového bloku. Klepnutím na příslušné místo se zobrazí výběr bloku. Ten obsahuje karty:
- Základní – jednoduché bloky různých velikostí.
- Knihovna - bloky vytvořené pro vaši web stránku.
- Oblíbené – bloky, které jste označili jako oblíbené.

![](pagebuilder-library.png)

## Nastavení šířky sloupců

Editor umožňuje nastavit šířky sloupce podle zvoleného zařízení. V nástrojové liště u přepínače typu editoru je možnost nastavit velikost (šířku) zařízení.

![](pagebuilder-switcher.png)

- Desktop - je určen pro šířku větší/rovnou než 1200 bodů (nastavuje CSS třídu `col-xl`).
- Tablet - je určen pro šířku 768-1199 bodů (nastavuje CSS třídu `col-md`)
- Mobil - je určen pro šířku menší než 768 bodů (nastavuje CSS třídu `col-`)

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/aru-B1vxReo" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>
