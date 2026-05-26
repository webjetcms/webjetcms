# Položky formuláře

Přes kartu položky formuláře umíte vytvářet vícekrokové formuláře. Tyto formuláře umožňují rozdělení formulářů na více kroků, což zlepšuje uživatelskou zkušenost a zvyšuje přehlednost. Samozřejmě můžete vytvářet jednoduché formuláře pouze s jedním krokem.

## Vytvoření formuláře

Formulář můžete vytvořit v sekci **Aplikace > Formuláře**, přesněji v kartě [Seznam formulářů](../form/README.md). Seznam obsahuje formuláře všech typů, včetně vícekrokových, ale při vytvoření nového se vždy vytvoří vícekrokový formulář.

![](forms-list.png)

Klepnutím na tlačítko přidat se zobrazí dialog pro [vytvoření nového formuláře](../form/README.md#vytvoření-formuláře).

![](form-create-dialog.png)

Musíte zadat unikátní název formuláře a chcete-li odesílat odpovědi na email i vaši email adresu. Více informací k polím formuláře naleznete v sekci [Vytvoření formuláře](../form/README.md#vytvoření-formuláře).

## Položky formuláře

V seznamu odeslaných formulářů se pro formulář typu vícekrokový zobrazuje karta **Položky formuláře**.

![](form-detail.png)

Klepnutím na tuto kartu se zobrazí sekce, kde můžeme formuláři přidávat položky a kroky.

![](default-form.png)

Karta **Položky formuláře** je rozdělena na tři sloupce:

- **Kroky formuláře** - tabulka obsahující seznam kroků formuláře.
- **Položky formuláře** - tabulka obsahující seznam položek (jednotlivá formulářová pole) formuláře patřících do právě vybraného kroku.
- **Náhled formuláře** - zobrazení náhledu právě zvoleného kroku formuláře.

![](real-form.png)

## Kroky formuláře

První sloupec zobrazuje seznam kroků formuláře. V tomto seznamu můžeme přidávat, duplikovat, mazat, upravovat a měnit pořadí kroků formuláře. Každý vytvořený krok tedy představuje reálný krok ve formuláři, který se zobrazí uživateli. K jednotlivým krokům můžete přidávat položky formuláře, které se zobrazí v daném kroku.

![](form-step-editor.png)

Můžete vyplnit Úvodní text, který se zobrazí na začátku kroku. V kartě Pokročilé můžete změnit text tlačítka pro přechod na další krok (nebo odeslání formuláře, pokud jde o poslední krok). V kartě Kód skriptu můžete vložit vlastní HTML/JavaScript kód, který se vloží a provede při zobrazení kroku.

### Duplikování

Při duplikování kroku se zkopírují všechny položky, které se v daném kroku nacházejí, do nového kroku. Tímto způsobem můžeme rychle vytvořit podobné kroky bez nutnosti přidávat všechny položky znovu.

### Mazání

Při mazání kroku se odstraní všechny položky, které se v daném kroku nacházejí. Před mazáním je vhodné zkontrolovat, zda v daném kroku nejsou žádné důležité položky, které bychom chtěli zachovat.

## Položky formuláře

Druhý sloupec obsahuje položky formuláře (nebo položky kroku formuláře), které se zobrazí až po zvolení kroku v prvním sloupci. V této tabulce můžete přidávat, duplikovat, upravovat, mazat a měnit pořadí položek formuláře. Položky se přidávají do právě zvoleného kroku (takže jednotlivé kroky formulář se nebudou navzájem ovlivňovat).

![](form-item-editor.png)

### Přidávání a úprava položek

Editor pro přidávání a úpravu položek je speciální tím, že mění svůj obsah podle zvoleného typu pole (položky). To znamená, že pro každý typ položky (např. Jméno, Příjmení atd.) se zobrazí různá nastavení a možnosti konfigurace. Podle zvoleného typu položky můžete nastavit parametry jako:

- **Typ pole** - určuje jaký druh vstupu bude položka představovat (např. textové pole, výběr z rozbalovacího seznamu, zaškrtávací políčko atd.). Jednotlivá pole [připravuje designér web sídla](../formsimple/README.md#informace-pro-web-designera) a jsou stejné jako pro aplikaci formulář snadno.
- **Povinné pole** - zda je položka povinná k vyplnění.
- **Povolená hodnota** - pro pokročilou validaci vstupu uživatele umíte zvolit libovolný počet regulárních výrazů, které musí být splněny, aby byl vstup platný. Více se o nich dozvíte v sekci [Regulární výrazy](../form/regexps.md).
- **Název pole** - název, který se zobrazí uživateli. Pokud není zadán použije se název shodný s typem pole.

V kartě **Pokročilé** můžete nastavit další volitelné parametry jako:

- **Krok formuláře** - krok, ke kterému položka patří, můžete tak snadno položku přesunout do jiného kroku.
- **Pořadí** - určuje pořadí položky v rámci kroku.
- **Předvyplněná hodnota** - hodnota, která se zobrazí vyplněná v poli, uživatel tak nemusí nastavit hodnotu, pokud je všeobecně známá. Pro pole typu výběrové pole sem zadejte čárkovou oddělovaný seznam hodnot, například `začiatočník,pokročilý,expert`.
- **Zástupní text** - text, který se zobrazí v poli jako nápověda pro uživatele pokud není pole vyplněno (je prázdné).
- **Tooltip** - pokud zadáte hodnotu tooltipu, zobrazí se při názvu pole informační bublina.

Chcete-li definovat vlastní položky formulářů, nebo chcete změnit existující, či změnit jaká nastavení jsou dostupná pro jednotlivé typy položek, podívejte se do dokumentace v sekci [Položky formulářů](../formsimple/README.md#informace-pro-web-designera).

!>**Upozornění:** při úpravě položky formuláře nedoporučujeme měnit typ položky, ale raději nahradit původní položku novou.

### Řádkové zobrazení

Můžete se setkat se situací, kdy Vám editor nedovolí přidat zvolenou položku. To může nastat, pokud se jedná o speciální položku určenou pro řádkové zobrazení formuláře (obecně mají tyto položky v názvu "řádkové zobrazení"). Dokud formulář nebude v režimu řádkového zobrazení, tyto položky sice ve výběru uvidíte, ale nebudete je moci použít.

![](row-view.png)

Formulář přepnete do režimu řádkového zobrazení v [nastaveních formuláře](../form/README.md#vytvoření-formuláře).

### Značky

Pokud chcete v položce formuláře použít informace o aktuálním přihlášeném uživateli (např. jeho jméno, email, atd.), můžete použít speciální značky. Tyto značky se automaticky nahradí příslušnými hodnotami při zobrazení formuláře uživateli. Pro nepřihlášené uživatele budou tyto značky nahrazeny prázdnou hodnotou. Hodnotu zadejte do pole **Předvyplněná hodnota** položky formuláře.

Dostupné značky jsou:

- ```!LOGGED_USER_NAME!``` - ​​jméno a příjmení (je-li konf. proměnná ```fullNameIncludeTitle``` nastavena na true obsahuje i titul)
- ```!LOGGED_USER_FIRSTNAME!``` - ​​jméno
- ```!LOGGED_USER_LASTNAME!``` - ​​příjmení
- ```!LOGGED_USER_TITLE!``` - ​​titul
- ```!LOGGED_USER_LOGIN!``` - ​​přihlašovací jméno
- ```!LOGGED_USER_EMAIL!``` - ​​emailová adresa
- ```!LOGGED_USER_COMPANY!``` - ​​firma
- ```!LOGGED_USER_CITY!``` - ​​město
- ```!LOGGED_USER_ADDRESS!``` - ​​adresa (ulice)
- ```!LOGGED_USER_COUNTRY!``` - ​​stát
- ```!LOGGED_USER_PHONE!``` - ​​telefon
- ```!LOGGED_USER_ZIP!``` - ​​PSČ
- ```!LOGGED_USER_ID!``` - ​​ID uživatele
- ```!LOGGED_USER_BIRTH_DATE!``` - ​​datum narození
- ```!LOGGED_USER_FIELDA!``` - ​​volné pole A
- ```!LOGGED_USER_FIELDB!``` - ​​volné pole B
- ```!LOGGED_USER_FIELDC!``` - ​​volné pole C
- ```!LOGGED_USER_FIELDD!``` - ​​volné pole D
- ```!LOGGED_USER_FIELDE!``` - ​​volné pole E
- `!LOGGED_USER_GROUPS!` - ​​seznam skupin uživatelů

### Podmíněné zobrazení/validování položky

Pro každou položku formuláře můžete nastavit pravidla, která dynamicky mění její chování podle hodnot jiných položek.

K dispozici jsou dvě samostatné karty:

- **Zobrazení** - určují, zda se položka zobrazí nebo skryje (podmínky zobrazení).
- **Povinnost** - určují, zda bude položka povinná nebo nepovinná (podmínky povinnosti).

Obě karty používají stejný typ pravidel a stejný způsob vyhodnocování. Liší se pouze výsledným efektem. Nastavení z jedné karty neovlivňují nastavení druhé karty.

![](tab-visibilityConditions.png)

!>**Upozornění:** Systém nekontroluje, zda jsou zadané podmínky reálně splnitelné, proto je nastavujte tak, aby mohly nastat. Vyhněte se i situacím, kdy má položka podmíněné zobrazení a zároveň se její hodnota používá v další podmínce, protože může vzniknout mrtvá situace. Rovněž nedoporučujeme měnit pořadí kroků, pokud již máte nastavené podmínky, protože může nastat situace, kdy pole z kroku 1 čeká na hodnotu pole z kroku 3, což nebude fungovat správně.

#### Kdy jsou podmínky dostupné

- Karty s podmínkami se zobrazí pouze při editaci stávající položky (ne při vytváření nové).
- Dostupnost karet závisí na typu pole:
  - Pokud položka nepodporuje nastavení **Povinné pole**, nebude dostupná ani karta **Podmínky povinnosti**.
  - U grafických položek (např. mezera, nový řádek, prázdná buňka) podmínky nejsou dostupné, protože se nejedná o interaktivní vstupní pole.
- Jsou-li nastaveny **Podmínky povinnosti**, hodnota přepínače **Povinné pole** se ignoruje.
- V podmínkách můžete použít pouze položky ze stejného nebo z předchozích kroků formuláře.

#### Jak vytvořit podmínku

Výsledné pravidlo vytvoříte kombinací jednotlivých řádků v tabulce podmínek. Každý řádek představuje jednu podmínku s více parametry. Na jednu položku můžete přidat více podmínek.

Jedná se o plochou strukturu (bez závorek), takže není podpořeno vnořování podmínek.

Každá podmínka obsahuje tyto parametry:

- **Pole formuláře** - položka, jejíž hodnota se použije při vyhodnocení podmínky. Možnosti jsou seřazeny stejně jako ve formuláři.
- **Podmínka porovnání** - způsob porovnání hodnoty pole:
  - **rovná se**
  - **nerovná se**
  - **obsahuje**
  - **neobsahuje**
  - **začíná na**
  - **končí na**
  - **je prázdné**
  - **není prázdné**
- **Srovnávaná hodnota** - hodnota, vůči které se porovná obsah vybraného pole.
- **Ignorovat velikost písmen** - porovnání nebude citlivé na velká/malá písmena.
- **Logický operátor** - spojení s následující podmínkou:
  - `AND` - ​​musí platit obě podmínky,
  - `OR` - ​​stačí, pokud platí alespoň jedna podmínka.

![](tab-visibilityConditions-create.png)

!>**Upozornění:** U operátorů **je prázdné** a **není prázdné** se pole **Srovnávaná hodnota** a **Ignorovat velikost písmen** automaticky skryjí, protože se testuje pouze existence obsahu pole, nikoli jeho konkrétní hodnota.

#### Vyhodnocování pravidel

- Podmínky se vyhodnocují průběžně během vyplňování kroku.
- Jsou-li splněny **Podmínky zobrazení**, pole se zobrazí, jinak se skryje.
- Jsou-li splněny **Podmínky povinnosti**, pole je povinné, jinak je nepovinné.
- Skryté pole se neodesílá a nevaliduje.
- Pokud je pole skryto, nikdy nemůže být zároveň povinné.

#### Důležitá omezení

- Podmínka nemůže odkazovat sama na sebe. Jedná se o nepovolený stav a podmínka nebude možné uložit.
- Položku, která je použita v podmínkách jiných položek, nelze odstranit.
- Při úpravě takové položky editor zobrazí upozornění na závislé položky. Uložení můžete povolit v kartě **Pokročilé** volbou **Uložit iu existujících závislých položek**.

Při pokusu o úpravu nebo odstranění položky, která je použita v podmínce jiné položky, se zobrazí chyba i notifikace se seznamem závislých položek.

![](save-condition-error.png)

### Statistika

Karta **Statistika** umožňuje zapnout statistiku odpovědí pro danou položku formuláře. Statistiku aktivujete zvolením možnosti **Zobrazit statistiku**. Po jejím zapnutí se zobrazí všechna dostupná nastavení statistiky pro danou položku.

!>**Upozornění:** Povolením/zamítnutím možnosti **Zobrazit statistiku** ovlivníte pouze statistiku této jedné položky formuláře, ne ostatních položek.

Dostupná nastavení:

- **Zobrazit statistiku** – po zvolení se zobrazí graf reprezentující odpovědi na toto pole v sekci **Statistiky formuláře**.
- **Typ grafu** – určuje, jakým typem grafu chcete data reprezentovat.
- **Počet hodnot** – počet nejčastějších hodnot, které se zobrazí v grafu.
- **Zobrazit "Ostatní"** – po zvolení se v grafu zobrazí zbývající hodnoty (ty, které nepatří mezi top X podle nastavení **Počet hodnot**) jako jedna položka s názvem "Ostatní".
- **Zobrazit "Nezodpovězeno"** – po zvolení se v grafu zobrazí i nezodpovězené odpovědi jako položka "Nezodpovězeno". Užitečné zejména tehdy, pokud pole není povinné a uživatel mohl nechat pole nevyplněno.
- **Srovnávat laxně** – po zvolení se při seskupování odpovědí ignoruje velikost písmen a diakritika (např. `Áno` a `ano` se sečtou dohromady jako stejná odpověď).
- **Vybrat barevné schéma pro graf** – po zvolení se zpřístupní výběr barevného schématu grafu. Každé barevné schéma obsahuje 5 barev. Pokud graf obsahuje více než 5 hodnot, barvy se začnou opakovat. Při velkém počtu hodnot doporučujeme schéma nenastavovat – použije se výchozí schéma, které vzniká kombinací tří 5-barevných schémat s různými odstíny.

![](form-item-editor-stat.png)

## Náhled formuláře

Třetí sloupec sekce **Obsah formuláře** je náhled formuláře. Tento náhled zobrazuje aktuální krok formuláře tak, podobně jako jej uvidí uživatel. Náhled se aktualizuje vždy, když nastane nějaká změna v tabulce kroky nebo položky formuláře. Tímto způsobem můžete orientačně vidět, jak bude formulář vypadat po provedení změn.

Na konci každého kroku se automaticky vygeneruje tlačítko, jehož text se mění podle toho, zda je krok poslední nebo ne. Pokud není poslední, tlačítko bude mít text **Přejít na další krok**, jinak **Odeslat formulář**. Text tlačítka můžete změnit v nastavení kroku, například na **Pokračovat** nebo **Registrovat se**.

![](real-form.png)

!>**Upozornění:** Náhled formuláře je orientační a může se kompozičně i graficky lišit od skutečného zobrazení na stránce. V editoru se zobrazují všechny položky bez ohledu na nastavené podmínky, abyste uměli formulář lépe navrhnout a zkontrolovat. Na reálné stránce se však formulář mění dynamicky podle podmínek zobrazení (některá pole se mohou skrýt nebo zobrazit podle hodnot jiných polí) a zároveň podle použité šablony a stylů stránky, do které je formulář vložen.

## Vložení formuláře do stránky

Vytvořený formulář můžete vložit do web stránky pomocí aplikace Formulář ve které vyberete název vytvořeného formuláře.

![](app-editor.png)

## Konfigurační proměnné

Dostupné konfigurační proměnné pro vícekrokové formuláře:

- `multistepform_nameFields` - ​​seznam názvů polí, která budou považována za pole pro jméno. Mezi těmito poli se bude hledat jméno, které by se použilo jako oslovení v emailech. Použije se pouze **první** nalezené neprázdné jméno.
- `multistepform_emailFields` - ​​seznam názvů polí, která budou považována za pole pro emailovou adresu. Mezi těmito poli se bude hledat emailová adresa, na kterou se odešle potvrzení o přijetí formuláře. Použije se **všechny** nalezeny a validní emailové adresy.
- `multistepform_attachmentDefaultName` - ​​přednastavený název přílohy v emailech, který se použije pokud se nepodaří získat skutečný název souboru přílohy.
- `multistepform_subjectDefaultValue` - ​​přednastavený překladový klíč pro předmět emailu, který se použije pokud není zadaný předmět v nastaveních/atributech formuláře.