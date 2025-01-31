# Import a export webových stránek

Export webových stránek exportuje webové stránky včetně jejich textu, stromové struktury a vložených obrázků a souborů. Umožňuje snadný přenos obsahu mezi více instancemi systému WebJET CMS, např. mezi produkčním a testovacím prostředím. Při importu se zobrazí okno, ve kterém se porovnávají jednotlivé stránky, s možností výběru skutečně importovaných stránek. Data tak můžete před importem zkontrolovat a potvrdit.

Pokud připravujete nový web, můžete si předem připravit stromovou strukturu webu a importovat ji do systému WebJET CMS.

![](import-export-window.png)

## Exportování webových stránek

Nejprve se ujistěte, že se nacházíte v adresáři, který chcete exportovat. Stiskněte tlačítko ![](import-export-button.png ":no-zoom") a počkejte, až se zobrazí okno. Pro standardní export obsahu vyberte v tomto okně možnost **Export webových stránek do archivu ZIP (xml)** a spustit export ve vybrané větvi stromu stránek tlačítkem s textem **OK**.

Výstup by měl vypadat podobně jako na následujícím obrázku. Na samém konci výstupu se zobrazí odkaz na soubor ZIP ke stažení. Nabízený soubor stáhnete do svého počítače. Tento soubor pak budete moci použít k importu v jiném prostředí systému WebJET CMS.

![](exported-window.png)

## Import webových stránek z archivu ZIP

Nejprve se ujistěte, že se nacházíte v adresáři, do kterého chcete import použít. Stiskněte tlačítko ![](import-export-button.png ":no-zoom") a počkejte, až se zobrazí okno. V okně vyberte možnost **Import webových stránek z archivu ZIP (xml)** a stiskněte tlačítko s textem **OK**. Budete vyzváni k nahrání souboru ZIP, který by měl být ve stejném stavu, v jakém byl exportován prostřednictvím nástroje. **Export webových stránek do archivu ZIP (xml)**. Jakékoli experimentování s jeho obsahem může vést k nesprávnému fungování importu s následkem poškození výsledného obsahu webových stránek.

Máte také možnost vybrat hodnotu **Synchronizace stránek podle** rozhodnout, který parametr bude použit pro kontrolu, zda stránka již existuje, nebo ne. Máte na výběr z následujících možností:
- **Název stránky nebo adresa URL**, je považována za existující, pokud je její název nebo adresa URL totožná s jinou existující stránkou.
- **Adresy URL**, je považována za existující, pokud se shoduje v adrese URL s jinou existující stránkou.
- **Žádné**, nezáleží na stránce, ona bude **se vždy považují za nové**, takže můžete přidávat duplikáty stávajících stránek.
- **Nepovinné pole A** / **Nepovinné pole B** / **Nepovinné pole C**, máte možnost porovnávat stránky podle konkrétních zadaných hodnot, jako např. [speciálně vygenerované ID](../../frontend/webpages/customfields/README.md#jedinečný-identifikátor). Pokud na začátku vytváření webové stránky nastavíte možnost Jedinečný identifikátor, dostane každá stránka jedinečný srovnávací řetězec.

![](import-zip-window.png)

Po nahrání souboru zip a potvrzení vložení klikněte na tlačítko s textem **OK** počkat, až systém soubor zpracuje, dokud se nezobrazí. **srovnávací tabulka pro import**.

### Srovnávací tabulka pro import

V tabulce jsou nejprve uvedeny webové stránky, poté soubory a následně další údaje modulu, pokud byly v exportu přítomny. Tabulka obsahuje 4 sloupce:
- **Vzdálená adresa** - název stránky/souboru v souboru ZIP.
- **Stav** - informace o tom, zda stejný objekt již existuje v cílovém úložišti.
- **Synchronizace** - zaškrtávací políčko, které definuje synchronizaci pouze s určitými položkami v seznamu.
- **Místní adresa** - název a umístění stránky/souboru v cílovém úložišti (ve WebJETu, do kterého importuji).

Nad tabulkou jsou dvě zaškrtávací políčka:
- **Vytvoření chybějících šablon**
- **Vytvoření chybějících skupin uživatelů**

!> Jejich zaškrtnutí nechávám na zvážení, ale doporučuje se je mít zaškrtnuté.

**Statistické záhlaví**

Záhlaví stránky obsahuje přehled statistik importu. Zobrazuje přehled o tom, kolik složek/stránek/souborů ... bylo vybráno k synchronizaci. Tyto statistiky jsou aktualizovány při každé změně. Záhlaví také nabízí několik užitečných tlačítek, jako např:
- ![](selectAllBtn.png ":no-zoom"), označí v tabulce všechny dostupné možnosti
- ![](deselectAllBtn.png ":no-zoom"), zruší zaškrtnutí všech dostupných možností v tabulce.
- ![](closeAllFoldersBtn.png ":no-zoom"), skryje všechny webové stránky patřící do složky v tabulce.
- ![](openAllFoldersBtn.png ":no-zoom"), odhalí (zobrazí) všechny webové stránky patřící do složky v tabulce.

Po projití celého seznamu a zaškrtnutí políček u položek, které chcete synchronizovat, můžete kliknout na tlačítko . **Synchronizace** v dolní části okna. Tím se data synchronizují, nový obsah se zveřejní na webových stránkách a původní soubory se přepíší novými.

![](imported-zip-window.png)

Použité obrázky a soubory se také exportují na webovou stránku. Pokud je na stránce použita aplikace Banner, Galerie nebo Anketa, jsou exportována také základní data těchto aplikací. Můžete vybrat možnosti importu dat pro danou aplikaci.

## Import struktury ze souboru aplikace Excel

Před importem se ujistěte, že se nacházíte v adresáři, do kterého chcete import použít. Stiskněte tlačítko ![](import-export-button.png ":no-zoom") a počkejte, až se zobrazí okno. V okně vyberte možnost **Import struktury ze souboru aplikace Excel** a stiskněte tlačítko s textem **OK**. Zobrazí se nastavení importu, které vás vyzve k zadání. `XLS` struktura souboru. Nezapomeňte, že **soubor musí být typu XLS**. Nemůže být typu `XLSX` ani `XLSM` pouze XLS. K dispozici jsou i další možnosti importu:
- ID složky, kam importovat soubory, tato možnost bude přednastavena podle dříve vybrané složky, ale lze ji změnit (změnit cílovou složku).
- Nastavení priority podle úrovně - priorita rozložení se nastavuje podle vnoření do stromové struktury, čím je stránka hlouběji, tím vyšší je číslo priority. Význam to má při vyhledávání a řazení výsledků podle priority, takže stránky nižší úrovně jsou ve výsledcích vyhledávání dříve - předpokládá se, že stránka sekce je důležitější než její podstránka.
- Stáhnout existující stránku ze serveru - u vytvořených stránek je možné stáhnout text z existujícího webu. Je také možné zadat počáteční a koncový ořezový kód HTML, podle kterého se ve staženém kódu HTML identifikuje vlastní text stránky. Stahuje se pouze samotný text bez obrázků a připojených souborů.

![](import-excel-window.png)

Po nahrání souboru a případné úpravě nastavení importu se proces spustí stisknutím tlačítka s textem **OK**. Podle struktury v souboru Excel se naimportuje adresářová struktura a v každém adresáři se vytvoří prázdné stránky se stejným názvem jako název adresáře.Jednotlivé vytvořené stránky (včetně celé adresy ve struktuře) se postupně vypisují. Vyčkejte, dokud se celý proces nedokončí a nezobrazí se zpráva o dokončení importu.

![](imported-excel-window.png)
