# Testování importu do datatabulky

Připravili jsme také možnost automaticky testovat import dat do datatabulky. Test provede následující operace:
- ověří, že v tabulce nejsou zadaná data
- naimportuje data a ověří jejich zobrazení/filtrování
- upraví povinná pole (přidá výraz `-changed`) a ověří jejich zobrazení po uložení
- znovu naimportuje data s párováním podle zadaného sloupce, ověří že data již neobsahují `-changed` text

Implementace je podobná jako pro [Automatické testování DataTables](datatable.md). Pro přípravu je třeba:
- vytvořit testovací záznam v tabulce (ideální, aby měl vyplněných co nejvíce údajů)
- exportovat tabulku do Excelu, v něm ponechte jen hlavičku a testovací záznam
- v Excelu upravte data následovně:
  - hodnotu ID sloupce ponechte, bude se tak ověřovat přepsání původního záznamu (nesmí se importem přepsat)
  - ostatní sloupce patřičně upravte, doporučujeme doplnit výraz `-import-test`
  - určit jeden unikátní sloupec (např. jméno) který se bude používat k ověření Aktualizovat stávající záznamy

Takto připravený Excel soubor pro řád uložte do stejného adresáře jako máte testovací script a také jej rovněž pojmenujte (jen samozřejmě s .xlsx příponou). Příkladem je `insert-script.js` a `insert-script.xlsx`.

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

Kromě standardních parametrů [automatizovaného testu datatabulky](datatable.md) jsou použity doplňující parametry:
- `file` - cesta k xlsx souboru s testovacími daty importu
- `updateBy` - hodnota použitá pro testování Aktualizovat stávající záznamy
- `rows` - pole obsahující jméno sloupce a hodnotu, která se použije pro kontrolu/filtrování v tabulce po importu
- `preserveColumns` - seznam sloupců, které se nenacházejí v Excel souboru. Budou během změny nastaveny na náhodnou hodnotu a následně při aktualizaci importem se ověří, že hodnota se nepřepsala/zachovala. Např. `preserveColumns: [ 'title', 'deliveryFirstName','deliveryLastName' ]`.

Důležitý je parametr `rows` ve kterém definujete seznam sloupců, které se použijí pro filtrování záznamů po importu. Hodnota se musí shodovat s hodnotou v Excel souboru.
