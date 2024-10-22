# Testování importu do datové tabulky

Přidali jsme také možnost automatického testování importu dat do datové tabulky. Test provede následující operace:
- ověří, zda v tabulce nejsou zadána žádná data.
- importuje data a ověřuje jejich zobrazení/filtraci.
- upravuje povinná pole (přidává výraz `-changed`) a po uložení ověří jejich zobrazení
- reimportuje data se shodou podle zadaného sloupce, ověří, zda data již neobsahují. `-changed` text

Implementace je podobná jako u [Automatické testování DataTables](datatable.md). Pro přípravu potřebujete:
- vytvořit testovací záznam v tabulkovém procesoru (ideálně s co největším počtem vyplněných údajů).
- exportovat tabulku do Excelu a ponechat pouze záhlaví a testovací záznam.
- v aplikaci Excel upravte data následujícím způsobem:
  - zachovat hodnotu ID sloupce, tím se ověří přepsání původního záznamu (nesmí být přepsán importem).
  - upravit ostatní sloupce odpovídajícím způsobem, doporučujeme přidat výraz `-import-test`
  - zadejte jeden jedinečný sloupec (např. jméno), který bude použit k ověření Aktualizovat existující záznamy.

Pro pořádek uložte připravený soubor Excelu do stejného adresáře jako testovací skript a pojmenujte jej stejně (samozřejmě jen s příponou .xlsx). Příklad: `insert-script.js` a `insert-script.xlsx`.

![](test-import-excel.png)

Příklad testu:

```javascript
Scenario('insert script-import', async ({ I, DataTables }) => {
     I.waitForText('Zoznam skriptov', 5);
     await DataTables.importTest({
          dataTable: 'insertScriptTable',
          requiredFields: ['name', 'position'], //pre tuto tabulku mame fixne definovane, aby sa vyplnili len tieto atributy, pokuste sa nechat prazdne aby sa vyplnili vsetky
          file: 'tests/components/insert-script.xlsx',
          updateBy: 'Názov / popis skriptu - name',
          rows: [
               {
                    name: "Test import"
               }
          ]
     });
});
```

Kromě standardních parametrů [automatizovaný test datových souborů](datatable.md) jsou použity další parametry:
- `file` - cesta k souboru xlsx s importovanými testovacími daty
- `updateBy` - hodnota použitá pro testování Aktualizovat existující záznamy
- `rows` - pole obsahující název sloupce a hodnotu, která bude po importu použita pro kontrolu/filtraci v tabulce.
- `preserveColumns` - seznam sloupců, které nejsou v souboru aplikace Excel. Během změny budou nastaveny na náhodnou hodnotu a při aktualizaci importem bude ověřeno, že hodnota nebyla přepsána/zachována. Například. `preserveColumns: [ 'title', 'deliveryFirstName','deliveryLastName' ]`.

Důležitým parametrem je `rows` ve kterém definujete seznam sloupců, které budou použity k filtrování záznamů po importu. Hodnota musí odpovídat hodnotě v souboru aplikace Excel.
