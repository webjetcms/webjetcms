# Field Type - datatable

Pole datatable umožňuje v editore stránok zobraziť vnorenú datatabuľku (napr. zoznam Médii v editore stránok). Je potrebné si uvedomiť, že datatabuľka je inicializovaná s vlastnou URL adresou. V samotnom objekte rodičovskej tabuľky nie je potrebné dáta posielať ani ich následne prijímať, dáta sa menia automaticky pri volaní REST služby vnorenej datatabuľky. Technicky by bolo možné pracovať priamo s JSON dátami z rodičovskej tabuľky, zatiaľ táto možnosť nie je implementovaná.

![](../../redactor/webpages/media.png)

Keďže aktuálne vložená datatabuľka pracuje so samostatnými REST službami vrátené dáta sú prázdne pole ```[]```.

## Použitie anotácie

Anotácia sa používa ako ```DataTableColumnType.DATATABLE```, pričom je potrebné nastaviť nasledovné atribúty editora:

- ```data-dt-field-dt-url``` - URL adresa REST služby, môže obsahovať makrá pre vloženie hodnoty z rodičovského editora, napr.: ```/admin/rest/audit/notify?docid={docId}&groupId={groupId}```
- ```data-dt-field-dt-columns``` - meno triedy (vrátane packages) z ktorej sa použije [definícia stĺpcov datatabuľky](datatable-columns.md), napr. ```sk.iway.iwcm.system.audit.AuditNotifyEntity```
- `data-dt-field-dt-columns-customize` - meno JavaScript funkcie, ktorá môže byť použitá na úpravu `columns` objektu, napr. `removeEditorFields`. Funkcia musí byť dostupná priamo vo `windows` objekte, ako parameter dostane `columns` objekt a očakáva sa, že ho vráti upravený. Príklad `function removeEditorFields(columns) { return columsn; }`.
- `data-dt-field-dt-tabs` - zoznam kariet pre editor v JSON formáte. Všetky názvy aj hodnoty JSON objektu je potrebné obaliť do `'`, preklady sú nahradené automaticky. Príklad: `@DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'fields', 'title': '[[#{editor.tab.fields}]]' }]")`.

Kompletný príklad anotácie:

```java
@DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
    tab = "media",
    editor = { @DataTableColumnEditor(
        attr = {
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/audit/notify"),
            @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.system.audit.AuditNotifyEntity")
        }
    )
})
private List<Media> media;
```

Pomocou data atribútov je možné nastavovať aj ďalšie konfiguračné možnosti datatabuľky. Údaje sú posielané ako reťazec. Hodnoty ```true``` a ```false``` sa konvertujú na ```boolean``` objekt. Nastavenie atribútu ```order``` umožňuje nastaviť usporiadanie len pre jeden stĺpec. Ostatné možnosti sa prenesú ako reťazec.

K menu nastavovanej možnosti pridajte predponu ```data-dt-field-dt-```, čiže pre nastavenie možnosti ```serverSide``` použite kľúč ```data-dt-field-dt-serverSide```. Príklad doplnkových anotácií:

```java
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"), //vypnutie serveroveho strankovania/vyhladavania
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-order", value = "2,desc"), //nastavenie usporiadania podla 2. stlpca
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,remove,import,celledit") //vypnutie zobrazenia uvedenych tlacidiel
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-forceVisibleColumns", value = "groupId,fullPath"), //vynuti zobrazenie len uvedenych stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-dt-updateColumnsFunction", value = "updateColumnsGroupDetails"), //JS funkcia ktora sa zavola pre upravu zoznamu stlpcov
    @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "user.group.groups_title") //nadpis nad datatabulkou na celu sirku okna
```

**API a udalosti**

Vytvorená datatabuľka sa sprístupní ako:

- ```conf.datatable``` na pôvodnom ```conf``` objekte stĺpca datatabuľky
- ```window``` objekt s názvom ```datatableInnerTable_fieldName``` - objekt je možné použiť pre automatizované testovanie alebo iné JavaScript operácie.

Po vytvorení vnorenej datatabuľky je vyvolaná udalosť ```WJ.DTE.innerTableInitialized``` kde v objekte ```event.detail.conf``` je prenesená konfigurácia.

## Poznámky k implementácii

Implementácia je v súbore [field-type-datatable.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-datatable.js) a v [index.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/index.js) nastavené ako ```$.fn.dataTable.Editor.fieldTypes.datatable = fieldTypeDatatable.typeDatatable();```.

Funkcia ```resizeDatatable``` sa používa na výpočet veľkosti datatabuľky (aby scrolovali len riadky), prepočet je volaný pri inicializácii poľa, intervalom každých 20 sekúnd (pre istotu), pri zmene veľkosti okna a pri kliknutí na tab v editore. Prepočet sa vykoná len keď je pole viditeľné.

Pri kliknutí na tab v editore sa testuje meno tabu voči tabu kde je vložená datatabuľka a ak sa zhoduje vykoná sa nastavenie šírky stĺpcov volaním ```conf.datatable.columns.adjust();```. Datatabuľka sa môže znova použiť pre rôzne dáta a toto zabezpečí korektné nastavenie šírky stĺpcov v hlavičke podľa obsahu tabuľky.

Funkcia ```getUrlWithParams``` dokáže v URL adrese nahradiť polia z json objektu. Ak URL adresa datatabuľky obsahuje ```?docid={docId}&groupId={groupId}``` tak hodnota ```{docId}``` a ```{groupId}``` je nahradená hodnotami z JSON objektu ```EDITOR.currentJson```.

