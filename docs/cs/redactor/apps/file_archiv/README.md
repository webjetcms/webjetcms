# Archivní soubory

Jediná aplikace pro správu souborů a jejich verzí. Umožňuje také nastavit budoucí publikování souborů a kategorizovat a přiřazovat soubory k produktům.

## Seznam souborů v archivu

V zobrazeném seznamu vidíme všechny soubory, které byly nahrány do archivu souborů v aktuálně vybrané doméně.

![](file_list.png)

### Filtrování souborů

Prostřednictvím filtru můžete zobrazené soubory odfiltrovat na základě:
- **id souboru** - Jedinečné ID souboru
- **Virtuální název** - Název, který se zobrazí při výpisu souborů na stránce.
- **Skutečné jméno** - Skutečný název souboru s příponou ( např. Zmuva.pdf )
- **Adresář** - Adresář, ve kterém se soubor nachází
- **Produkt, kategorie, kód** - Informace, které jste nastavili při nahrávání souboru.
- **Hlavní soubory** - jsou zobrazeny pouze hlavní soubory bez jejich starších verzí.
- **Zobrazit** - zobrazené soubory, které mají povoleno zobrazení

Filtrovat můžete pomocí více podmínek najednou. Např. produkt "ovoce" kategorie "jablko" kód "2015". Zobrazí se seznam souborů, které budou splňovat všechny tři uvedené podmínky filtru (nebo nebude nalezen žádný odpovídající soubor).

![](filter.png)

### Akce nad soubory

Prostřednictvím seznamu souborů můžeme nad vloženými soubory provádět různé funkce, jako je úprava, přejmenování, odstranění nebo vložení novější verze souboru, případně zrušení poslední změny. Tyto funkce naleznete ve sloupci nástrojů při výpisu souboru v archivu souborů:
- ![](upload.png ":no-zoom"), **Nahrání nové verze** pokud chcete nahrát novou (aktuální) verzi již nahraného souboru, tento nástroj vám to umožní. Po kliknutí se zobrazí standardní okno s nastavením vlastností souboru, které však již budou předvyplněny podle starého souboru, který chcete aktualizovat. Nedoporučujeme tato předvyplněná pole měnit, protože by měla být totožná s vlastnostmi starého souboru. (z důvodu filtrovaného výpisu na stránce, protože výpis souborů umožňuje zobrazit na stránce i starší verze souboru, nejen tu aktuální).
- ![](edit.png ":no-zoom"), **Upravit soubor** pokud chcete změnit některá nastavení souboru nebo zjistit, jaké vlastnosti soubor má, tento nástroj vám s tím pomůže.
- ![](rename.png ":no-zoom"), **Přejmenování souboru** : Nástroj umožňuje přejmenovat název souboru
- ![](file_history.png ":no-zoom"), **Zobrazit seznam verzí**, tento nástroj se zobrazí pouze v případě, že je vložena alespoň jedna verze, Zobrazit všechny archivní verze souboru.
- ![](rollback.png ":no-zoom"), **Vrátit poslední změnu**, tento nástroj se zobrazí pouze v případě, že je vložena alespoň jedna verze.
- ![](delete.png ":no-zoom"), **Odstranit soubor**, trvale odstraní soubor z archivu.

### Karta Čekající soubory

V kartách **Soubory čekající na vyřízení** zobrazí soubory, které byly nastaveny k pozdějšímu nahrání, když byly nahrány.

![](file_list_awaiting.png)

## Vložení nového souboru do archivu

Pokud chcete do archivu přidat nový soubor, můžete tak učinit kliknutím na ikonu umístěnou nad seznamem souborů.

![](file_insert.png)

V dialogovém okně vyberte soubor, který chcete nahrát, kliknutím na tlačítko **Vyberte soubor**. Je třeba vyplnit minimálně následující pole **Název virtuálního souboru**, ostatní pole jsou nepovinná:

![](dialog.png)

- **Cílový adresář pro nahrávání souborů** - vyberte adresář, do kterého chcete soubor nahrát (to bude užitečné později při filtrování zobrazení souborů na stránce).
- **Soubor k nahrání** - vyberte soubor, který chcete nahrát do archivu.
- **Název virtuálního souboru** - zadejte název souboru, který se zobrazí na stránce (jako odkaz na soubor).
- **Produkt** - zadejte libovolný název produktu (bude užitečné později při filtrování zobrazení souborů na stránce).
- **Kategorie** - zadejte libovolný název kategorie (bude užitečné později při filtrování zobrazení souborů na stránce).
- **Kód produktu** - zadejte libovolný kód produktu (bude užitečné později při filtrování zobrazení souborů na stránce).
- **Zobrazit** - nastavení zobrazení souboru na stránce (pokud nechcete soubor na stránce zobrazit, zrušte zaškrtnutí tohoto nastavení).
- **Datum začátku platnosti** - nastavení počátečního data platnosti souboru
- **Datum vypršení platnosti** - nastavení data vypršení platnosti souboru
- **Priorita** - pomocí priority můžete nastavit pořadí zobrazování souborů na stránce.
- **Hlavní soubor** - pokud nechcete nahraný soubor použít jako hlavní soubor, ale pouze jako vzor (vzor jiného hlavního souboru), zadejte vstupní údaje **Hlavní soubor** k němuž bude právě nahrávaný soubor ukázkou (tento soubor se pak zobrazí na stránce ve výpisu souborů spolu s hlavním souborem, jehož odkaz jste vyplnili). Chcete-li získat odkaz na hlavní soubor, najděte jej v seznamu a klikněte v nástrojích na tlačítko **Upravit podle** ( ![](edit.png ":no-zoom") ), čímž se otevře okno s nastavenými vlastnostmi souboru. Odtud zkopírujte **Odkaz pro referenci** a vložte ji do vstupu **Odkaz na hlavní soubor**.
- **Poznámka** - poznámka se zobrazí na stránce při odkazu na soubor.
- **Nahrát soubor později** - pokud potřebujete soubor nahrát do archivu v určitý čas a datum, můžete nastavit, aby se soubor v budoucnu nahrál automaticky. Výběrem této možnosti se zobrazí skrytá pole
  - **Datum pozdějšího nahrání** - vyberte den, kdy chcete soubor nahrát.
  - **Čas pozdějšího nahrání** - vyberte čas, kdy chcete soubor nahrát.
  - **E-maily oddělené čárkou** - zadejte e-maily oddělené čárkou, na které chcete zasílat oznámení o úspěšném/neúspěšném nahrání souboru do archivu.

Po úspěšném odeslání mohou nastat následující podmínky:
- Soubor je úspěšně nahrán a zobrazí se v seznamu nahraných souborů.
- Soubor byl úspěšně nahrán a zobrazí se ve výpisu. **čekající soubory** - tento stav nastane, pokud ve vlastnostech zkontrolujete a nastavíte **Nahrát soubor později**
- Budete upozorněni, že soubor se stejným obsahem, jaký nahráváte, se již v archivu nachází. (Modul porovnává skutečný obsah souboru, nikoli jeho název). V takovém případě máte dvě možnosti:
  - **Odstranit soubor** - aplikace zobrazí odkaz na soubor, který se již nachází v archivu a má stejný obsah jako soubor, který jste chtěli nahrát. Tímto způsobem můžete tento odkaz použít na stránce a nemusíte soubor nahrávat duplicitně. V takovém případě můžete soubor odstranit.
  - **Uchovávejte soubor** - pokud chcete soubor nahrát do archivu a nechat jej duplikovat, klikněte na tlačítko **Uchovávejte soubor**

## Vložení souboru z archivu do nastavení webové stránky a aplikace

Na stránku [vložit aplikaci](../../webpages/working-in-editor/README.md#vložení-aplikace) Archiv souborů.

![](apps-insert.png)

Po kliknutí na tlačítko "Vložit do stránky" se aplikace vloží do stránky a otevře se nastavení. Tato nastavení slouží jako filtr při výpisu souborů na stránce.

![](editor.png)

Dialogové okno nastavení se skládá ze tří částí:

1. sekce pro filtrování výpisu souborů na stránce, jsou to následující atributy:
    - **Cílový adresář pro nahrávání souborů** - vyberte složku, ze které chcete soubory zobrazit.
    - **Kód produktu** - zadejte kód (jako filtr), který chcete zobrazit.
    - **Produkt** - zadejte produkt (jako filtr), který chcete zobrazit.
    - **Kategorie** - zadejte kategorii (jako filtr), kterou chcete zobrazit.
    - **Výpis souborů včetně dílčích souborů** - Pokud tuto možnost zaškrtnete, stránka zobrazí také soubory, které jsou ve vnořených podsložkách cílového adresáře, který jste nastavili.
    - **Vzestupné pořadí výpisu hlavního souboru** - hlavní soubory budou na stránce seřazeny vzestupně.
    - **Zobrazit hlavní soubory podle** - řazení hlavních souborů v archivu podle:
      - Priorita - priorita, kterou si sami nastavíte při nahrávání nového souboru.
      - Datum nahrání - časový údaj o přidání souboru do archivu
      - Virtuální název souboru - abecední pořadí názvů souborů
    - **Zobrazit archiv** - pokud je zaškrtnuto, zobrazí se na stránce také archivy hlavního souboru.
    - **Vzestupné pořadí výpisu archivních souborů** - archivní soubory budou na stránce řazeny vzestupně.
    - **Zobrazit záznamy v archivu podle** - seřadit soubory v archivu podle:
      - Reference - třídí podle ID záznamu
      - Priorita - priorita, kterou si sami nastavíte při nahrávání nového souboru.
      - Orders - pořadí, v jakém je soubor nahrán.
      - Datum nahrání - časový údaj o přidání souboru do archivu
      - Virtuální název souboru - abecední pořadí názvů souborů
    - **Archivní položka před otevřením** - Pořadové číslo, které určuje, které z vláken souborů má být otevřeno jako první.
    - **Zobrazit pouze vybrané soubory** - můžete také ručně přidat soubory, které nevyhovují filtru, ale přesto je chcete zobrazit s výpisem.

2. Vyberte soubory, které chcete přidat do seznamu aplikací. Soubory lze filtrovat podle globálního id, virtuálního názvu, skutečného názvu, adresáře, produktu, kategorie, kódu.

3. Výsledek/tabulka z pomocného vyhledávání. Tato část obsahuje očekávané soubory, kliknutím na Přidat přidáte soubor do seznamu vybraných souborů (pokud se mají zobrazit pouze vybrané soubory).

## Zobrazení aplikace

Konečný vzhled aplikace po přidání na stránku. Názvy jednotlivých souborů jsou odkazy na jejich stažení.

![](file_archiv.png)
