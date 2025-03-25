# Správa blogerů

Sekce Seznam blogerů slouží k vytváření a správě uživatelů typu **bloger**.

Přístup k této sekcí mají pouze **administrátoři blogerů**. Administrátor blogerů musí mít právo Správa blogerů a neměl by patřit do skupiny uživatelů **Blog**. Při procesu vytváření blogeru se vytvoří nový uživatel s potřebnými právy pro vytváření článků.

Kromě toho musí existovat [skupina uživatelů](../../../admin/users/user-groups.md) s názvem `Blog`. Pokud neexistuje, vytvořte ji před přidáním prvního blogera.

!>**Upozornění:** administrátora blogerů nepřidávejte do skupiny uživatelů Blog.

Chcete-li aby měl administrátor blogerů také možnost zasahovat do struktury a článků jednotlivých blogerů ([Seznam článků](./README.md)) musí mít také právo Blog/Seznam článků.

## Přidání a editování blogera

Bloger se přidává přímo přes editor, obsahuje následující pole:
- Přihlašovací jméno - toto jméno musí být unikátní mezi všemi uživateli (nejen mezi bloggery).
- E-mailová adresa - e-mailová adresa blogera.
- Jméno.
- Příjmení.
- Heslo - pro nově vytvořeného uživatele ponechte prázdné, WebJET heslo vygeneruje a odešle na email. Znění emailu můžete upravit nastavením ID stránky s textem e-mailu ve skupině uživatelů Blog.
- Kořenová složka - rodičovská (nebo `root`) složku, kam se přidají blogerové složky, například. `/Blog`. Pro nově vytvořeného uživatele následně vznikne složka `/Blog/LOGIN`.

![](blogger_create.png)

Vytvořeného Blogera lze editovat, avšak již není možné změnit hodnoty Adresář web stránek a Přihlašovací jméno.

Chcete-li vidět/nastavit další informace o blogerovi, můžete tak učinit v části [Seznam uživatelů](../../../admin/users/README.md), kde je **bloger** veden jako každý jiný uživatel.

Blogeru lze z bezpečnostních důvodů smazat pouze v sekci Uživatelé jako standardního uživatele a následně je třeba smazat i jeho články.

### Základní nastavení

- Kromě použití základních informací jako Jméno, Příjmení, Heslo... se bloger automaticky nastaví jako **Schválený uživatel**.
- Nakolik se jedná o uživatele typu **bloger**, je přidán do Skupiny uživatelů **Blog**.
- Získává práva na vstup do admin sekce (správa web sídla).
- Získává práva na Nahrávání souborů do adresářů
  - `/images/blog/LOGIN`
  - `/files/blog/LOGIN`
  - `/images/gallery/blog/LOGIN`
- Získává přístupová práva, jako například. některá práva k web stránkám a adresářům. Konkrétně tato práva potřebuje pro práci s články. Více o článcích se dočtete v [Seznam článků](./README.md).
- Bonusová práva lze přidat, pokud před vytvořením blogeru upravíte konstantu `bloggerAppPermissions` o právu, které chcete aby mu byly po vytvoření povoleny.

### Struktura blogeru

Pro každého vytvořeného nového uživatele typu **bloger** se automaticky vytvoří struktura složek, kde může přidávat nové články nebo tuto strukturu dále rozšiřovat.

Složka, kde se tato struktura vytvoří, byla dána při vytváření blogera parametrem Kořenová složka. V této složce se vytvoří pod-složka s názvem odpovídající hodnotě Přihlašovací jméno daného blogera. Protože každý uživatel musí mít unikátní přihlašovací jméno (`login`), nemůže nastat situace s kolizí jmen složek. Pro každou takovou vytvořenou složku se vytvoří ještě jedna podsložka/sekce s názvem **Nezařazeno**.

Příklad:

Pokud jak **Kořenová složka** byl zvolen `/Aplikácie/Blog/` tak po vytvoření blogera s přihlašovacím jménem `bloggerPerm`, vznikne taková struktura v sekci web stránky:

```
- /Aplikácie/Blog/
  - /Aplikácie/Blog/bloggerPerm
    - /Aplikácie/Blog/bloggerPerm/Nezaradené
```

V těchto složkách budou automaticky vytvořeny 3 web stránky (články). 2 budou mít stejné jméno jako složky ve kterých se vytvoří s tím, že budou obsahovat aplikaci pro výpis článků v dané sekci, jedna stránka je ukázkový článek blogu.

V hlavní stránce každé složky se nachází aplikace se seznamem článků, což je technicky [seznam novinek](../news/README.md). Standardní parametry lze upravit v [Překladové klíče](../../../admin/settings/translation-keys/README.md) s klíčem `components.blog.atricles-code`.

### Práva na adresáře a stránky

Protože bloger musí být schopen vidět adresáře a články, které byly pro něj vytvořeny, automaticky získává právo na jeho hlavní složku. To znamená, že bude schopen vidět a upravovat existující strukturu, která byla pro něj vytvořena, ale jinde se nedostane. Jeho hlavní složka je ta, jejíž název je shodný s přihlašovacím jménem.

Více o úpravě struktury a článek se dozvíte zde [Seznam článků](./README.md).

### Vstup do sekce Diskuse

Každý uživatel typu bloger má povolen také vstup do sekce [Diskuse](../forum/README.md) pro správu diskuse pod jednotlivými články.

## Nastavení šablony

Jednotlivé články vznikají ve standardní stromové struktuře web stránek. Doporučujeme pro blog připravit samostatnou šablonu. Chcete-li používat i diskusi, vložte ji v šabloně do některého volného pole/patičky, aby se diskuse zobrazila v každém článku.

Do stránky s diskusi vložte následující kód:

```html
!INCLUDE(/components/forum/forum.jsp, type=open, noPaging=true, sortAscending=true, isBlog=true)!
```

Parametr `isBlog=true` způsobí vypnutí diskuse pro hlavní stránky složky (kde se typicky nachází seznam článků ve složce).

Ve stránce s hlavičkou můžete použít kód pro generování menu posunuté od kořenové složky parametrem `startOffset`:

```html
!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=1, startOffset=1, maxLevel=1, menuIncludePerex=false, classes=basic, generateEmptySpan=false, openAllItems=false, onlySetVariables=false, rootUlId=menu, menuInfoDirName=)!
```

to způsobí generování menu s posunem o zadaný počet složek, budou se tedy zobrazovat v menu jen sekce/složky pro aktuálně zobrazený blog.

V sekci Blog se při editaci článku nezobrazuje karta Šablona, bloger tedy nemůže standardně šablonu změnit/nastavit. Použije se výchozí pro kořenovou složku.
