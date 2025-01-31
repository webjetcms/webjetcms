# Předpřipravené bloky

Editor stránek nabízí možnost vkládat přednastavené bloky (`HTML` objektů) na stránku. Např. tabulka, text, kontaktní formulář atd. Do aktuální stránky můžete také vložit obsah jiné stránky (např. opakující se formulář).

Chcete-li zobrazit bloky, klikněte na ikonu ![](htmlbox_icon.png ":no-zoom") v editoru stránky, který zobrazí dialogové okno s kategoriemi bloků.

## Karta - Vaše

Na kartě **Váš** stránky vygenerované z adresáře **Systém** -> **Šablony**. Každý adresář v adresáři **Šablony** představuje jeden seznam v poli pro výběr modulu. Obsah lze vkládat dynamicky nebo staticky. Pokud je obsah vložen dynamicky, při pozdější úpravě obsahu (jedné stránky) se změna projeví na všech místech, kam byl obsah dynamicky vložen. Při statickém vložení se obsah duplikuje a vloží se jako kopie, která se vždy upravuje pouze na určitém místě webové stránky.

Karta Vaše se po otevření zobrazí jako vybraná, pokud složka obsahuje Systém->Šablony ve složkách nebo pokud jsou ve složce více než 2 stránky.

### Nastavení aplikace - OSTATNÍ

Seznam dostupných bloků se načítá ze složky Templates (standardně v podsložce System, ID složky je nastaveno v proměnné conf. `tempGroupId`)

- Dynamický blok - HTML kód bloku je vložen pomocí dynamického odkazu, pokud je obsah bloku změněn, automaticky se změní ve všech vložených částech.
- Statický blok - HTML kód bloku je vložen přímo do stránky jako kopie, změna původního bloku nemá vliv na vloženou verzi.
- Výběr stránky
  - Zobrazí se seznam dostupných bloků, např. Normální stránka, Stránka s nadpisem a 2 sloupci atd.

![](htmlbox_dialog.png)

### Nastavení aplikace - DALŠÍ STRÁNKA s `DocID`

Umožňuje vybrat libovolnou webovou stránku pro vložení.

- Způsob vložení
  - Přímo do stránky - vloží se kopie textu vybrané webové stránky.
  - Dynamický odkaz - HTML kód bloku je vložen pomocí dynamického odkazu, pokud je obsah bloku změněn, automaticky se změní ve všech vložených částech.
- `DocID` - výběr ID stránky, která se má vložit

![](editor-our.png)

### Složky

Pokud složka Systém/Šablony na webových stránkách obsahuje podsložky, zobrazí se ve výběrovém poli názvy těchto složek, tj. kromě složky Ostatní/Ostatní stránka s ID dokumentu se ve výběrovém poli zobrazí i jednotlivé složky s připravenými bloky.

## Karta - Obecné

Na kartě **Obecné** obsahuje všechny předpřipravené prvky a moduly, které editor nemůže měnit ani přidávat - odebírat. Jedná se o předpřipravený seznam obsahující obsahové prvky webové stránky, které má editor webu k dispozici.

### Nastavení aplikace

Zobrazení bloků připravených návrhářem webu, načtených ze souborů ve složce `/components/INSTALL_NAME/htmlbox/objects` Kde: `INSTALL_NAME` je název instalace (konf. proměnná `installName`). Pokud složka neexistuje, načtou se standardní bloky z této složky. `/components/htmlbox/objects`. Ve složce mohou být podsložky, jednotlivé bloky jsou ve složkách. `html` soubory. Se stejným názvem je třeba vytvořit také `jpg` soubor s ukázkovým blokem.

Ve výchozím nastavení jsou k dispozici následující skupiny/kategorie bloků:
- `Columns`
- `Contact`
- `Content`
- `Download`
- `Header`

![](editor-general.png)

## Zobrazení aplikace

![](htmlbox.png)
