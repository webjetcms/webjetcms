# Klonovací struktura

Pomocí klonování struktury můžeme klonovat celý obsah adresáře na stránkách do jiného adresáře, aniž bychom museli znovu vytvářet celou strukturu adresáře. Tato možnost je k dispozici v **Webové stránky** Stejně jako **Klonovací struktura**. Po výběru této možnosti se zobrazí okno akce klonování. Obvykle se používá k vytvoření nové jazykové verze webu ze stávající verze. Jazyk je převzat z nastavení zdrojové a cílové složky.

![](clone_structure.png)

Chcete-li provést klonování, musíte zadat ID zdrojové složky (která složka se má klonovat) a ID cílové složky (kam se má zdrojová složka klonovat). ID složek můžete zadat přímo, pokud si je pamatujete, nebo můžete použít příkaz **Vyberte**, čímž se otevře nové okno se stromovou strukturou složek, kde můžete vybrat konkrétní složku kliknutím na její název.

Samotné klonování využívá [Zrcadlení struktury](../docmirroring/README.md) a [Automatický překlad](../../../admin/setup/translation.md). To znamená, že při spuštění klonování se vybrané složky (pokud již nejsou) automaticky propojí pomocí konfigurační proměnné `structureMirroringConfig`. Ze zdrojové složky se do cílové složky naklonují všechny podsložky (a všechny jejich vnořené složky) a webové stránky, přičemž původní a klonované složky/stránky se propojí. Jazyk se přebírá z nastavení zdrojové a cílové složky. Také tyto složky/stránky jsou automaticky přeloženy, pokud je nastaven překladač.

## Možnosti

### ID zdrojového adresáře

Nastavte ID složky, ze které se má klonovat.

### ID cílového adresáře

Nastavte ID složky, do které se má klonovat. V této složce budou vytvořeny stránky a podsložky podle zdrojové složky.

### Udržujte zrcadlení aktivní

Pokud zvolíte možnost **Po klonování ponechte aktivní zrcadlení struktury.** zachovává množinu [zrcadlení](../docmirroring/README.md) mezi zdrojovou a cílovou složkou. Když pak vytvoříte novou složku nebo webovou stránku, přenese se mezi zrcadlenými složkami.

Nastavení můžete také později odpojit úpravou proměnné conf. `structureMirroringConfig` ze kterého odstraníte řádek s nastavenými ID složek.

### Zanechte adresu URL

Výběrem možnosti **Zanechte adresu URL** lze předpokládat, že adresy URL stránek a složek nebudou přeloženy do jazyka cílové složky. To znamená, že nová jazyková mutace bude mít **stejné adresy URL, ale jiný prefix, kterým tyto adresy URL začínají**.

Příklad: Mějme složky SK (se slovenskou jazykovou sadou) a EN (se slovenskou jazykovou sadou). Složka SK obsahuje podsložku **Majetek** která má hlavní stránku se stejným názvem. Adresa takové stránky je **/sk/majetok/**. Pokud použijeme strukturu klonování **bez opuštění adresy URL**, ze složky SK do složky EN, bude mít kopie této stránky adresu URL. **/en/property/**. Pokud použijeme strukturu klonování **ponechání adresy URL**, ze složky SK do složky EN, bude mít kopie této stránky adresu URL. **/en/majetok/**. Jak vidíme, url adresa nebyla přeložena, pouze se změnil prefix z /sk na /en, který představuje nadřazenou složku.

![](clone_structure_result.png)
