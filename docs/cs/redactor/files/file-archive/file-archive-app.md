# Aplikace Manažer dokumentů

Aplikace slouží k přehlednému zobrazení zvolených (filtrovaných) dokumentů z [Manažéru dokumentů](./README.md).

## Použití aplikace

Aplikaci můžete do své stránky přidat přes obchod s aplikacemi

![](file-archiv-app-insert.png)

nebo přímo jako kód do stránky

```html
!INCLUDE(sk.iway.iwcm.components.file_archiv.FileArchiveApp, dir=&quot;/files/archiv/&quot;, subDirsInclude=true, productCode=, product=, category=, showOnlySelected=false, globalIds=, orderMain=priority, ascMain=true, open=false, archiv=true, order=priority, asc=true, showPatterns=true, orderPatterns=priority, ascPatterns=true)!
```

v kódu si můžete všimnout množství parametrů, které si vysvětlíme v další části.

## Nastavení aplikace

Pomocí nastavení umíte filtrovat zobrazení pouze určitých dokumentů i stylu, jak se mají zobrazovat.

### Karta - Základní

Karta **Základní** slouží k nastavení filtrování dokumentů pro zobrazení.

!>**Upozornění:** dokumenty, které teprve čekají na nahrání, se v aplikaci **nezobrazují**.

- **Adresář** - umíte nastavit, zobrazení dokumentů pouze z konkrétní složky. Přednastavená hodnota je kořenová složka pro nahrání dokumentů. Můžete tuto hodnotu změnit, ale nemůžete jít výše než kořenovou složku.
- **Výpis dokumentů včetně pod-dokumentů** - přednastaveno je zobrazení dokumentů **IBA** ze zvoleného (bez pod-složek). Pokud tuto možnost zvolíte, zobrazí se **všechny** dokumenty ze zvolené složky ale **AJ** pod-složek.

!>**Upozornění:** zvolením možnosti **Výpis dokumentů včetně pod-dokumentů** na složce, která je vysoko ve stromové struktuře se vystavujete riziku práce s velkým množstvím dat. Proto se tato možnost doporučuje při menších množstvích nebo s užším filtrováním.

- **Kód produktu** - zobrazíte pouze dokumenty s daným kódem produktu.
- **Produkt** - pole automatický nabízí zadefinované produkty v manažeru, podle kterých můžete filtrovat.
- **Kategorie** - pole automatický nabízí zadefinované kategorie v manažeru, podle kterých můžete filtrovat.

![](file-archiv-app-tab-base.png)

**Zobrazit pouze vybrané dokumenty** - jedná se o speciální možnost, kdy se budou ignorovat všechny dosavadní filtry jako **Adresář** / **Kód produktu** atd., a zobrazí se pouze dokumenty, které mají zadané `globalId` v poli **Zobrazit jen vybrané dokumenty**.

Všechny dosavadní filtry si zachovají hodnoty, ale nemají žádný vliv a jsou zablokovány. Navíc se ještě zobrazí nová karta [Vybrané dokumenty](#karta---vybrané-dokumenty), kde můžete spravovat které soubory jsou zvoleny k zobrazení (ideální pokud neznáte potřebné `globalId`).

![](file-archiv-app-tab-base_2.png)

### Karta - Vybrané dokumenty

Karta se zobrazí pouze v případě, je-li zvolena možnost **Zobrazit pouze vybrané dokumenty**. V kartě je vnořená tabulka zobrazující **IBA** dokumenty, které lze použít jako vybrané dokumenty. Tyto dokumenty musí splňovat:

- musí to být hlavní dokument (ne historická verze)
- nesmí to být vzorový dokument
- nemůže to být dokument naplánovaný na nahrání v budoucnosti

Dokumenty v tabulce můžete označit a zvolit tlačítkem <button class="btn btn-sm btn-success" type="button"><span><i class="ti ti-plus"></i></span></button> čímž se hodnoty jejich `globalId` (neboli Globální Id) zapíše do pole **Zobrazit jen vybrané dokumenty** v kartě **Základní**. Naopak tlačítkem<button class="btn btn-sm btn-danger" type="button"><span><i class="ti ti-x"></i></span></button> se daný dokument z výběru odebere. Díky této kartě můžete jednoduše zvolit, které dokumenty chcete zobrazit bez nutnosti znát je `globalId`. Logika je samozřejmě ošetřena vůči duplicitě a pod.

Zvolené dokumenty (jejichž hodnota `globalId` je zapsána v poli **Zobrazit pouze vybrané dokumenty**) jsou v tabulce barevně označeny.

![](file-archiv-app-tab-selected.png)

### Karta - Rozšířené

Karta slouží k nastavení stylu zobrazení a řazení dokumentů.

- **Zobrazovat hlavní dokumenty podle** - výběr hodnoty, podle které se mají hlavní dokumenty seřadit
- **Vzestupné pořadí výpisu hlavních dokumentů** - pokud je možnost zvolena, hlavní dokumenty se budou řadit vzestupně, jinak naopak
- **Položka manažera předotevřená** - pokud možnost zvolíte, manažované položky se při zobrazování automaticky otevřou

---

- **Zobrazit historické verze** - zvolením možnosti se povolí zobrazení historický verzí dokumentů (pokud nějaké má). Také se odkryjí možnosti pro řazení těchto historických verzí
- **Zobrazovat historické verze podle** - výběr hodnoty, podle které se mají historické verze seřadit
- **Vzestupné pořadí výpisu historických verzí** - pokud je možnost zvolena, historické verzí se budou řadit vzestupně, jinak naopak

---

- **Zobrazit vzory** - zvolením možnosti se povolí zobrazení vzorů daného dokumentu (pokud nějaké má). Zobrazují se pouze hlavní vzory, nikoli jejich historické verze. Také se odkryjí možnosti pro řazení těchto vzorů
- **Zobrazovat vzory podle** - výběr hodnoty, podle které se mají vzory seřadit
- **Vzestupné pořadí výpisu vzorů** - pokud je možnost zvolena, vzorové dokumenty se budou řadit vzestupně, jinak naopak

![](file-archiv-app-tab-advanced.png)

## Aplikace

Samotná aplikace ve stránce zobrazuje zadané nebo vy-filtrované hlavní dokumenty.

![](app-base.png)

Pokud jste nezvolili možnost **Položka manažera předotevřená**, položka za uzavřená a až po kliknutí na ni se zobrazí dostupné **Historické dokumenty** a **Vzory dokumentu**.

![](app-expanded.png)

Samozřejmě, tyto tabulky se zobrazí pouze v případě, že:

- je povolena možnost **Zobrazit historické verze** případně **Zobrazit vzory**
- daný hlavní dokument má nějaké historické dokumenty nebo vzory

Hlavní dokument může mít obě tabulky současně, nebo ani jednu, v závislosti na tom, zda jsou povoleny nebo mají nějaké hodnoty.

**Zobrazit vzory** zobrazuje pouze hlavní vzory, jelikož i vzory mohou mít historické verze.

Ikona<i class="ti ti-download" style="color: #00a3e0;"></i> je dostupná pro hlavní dokumenty, historické dokumenty i pro vzory. Klepnutím na ni se zobrazí daný dokument v novém okně, kde si jej umíme prohlédnout nebo stáhnout.

![](app-download.png)