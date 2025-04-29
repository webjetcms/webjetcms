# Pole UPLOAD

Pole umožňuje nahrání souboru.

![](field-uploadFile.png)

## Použití anotace

Anotace se používá jako `DataTableColumnType.UPLOAD`.

Kompletní příklad anotace:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file"
)
private String file = "";
```

## Frontend

Pro nahrání nového souboru můžete soubor vložit přímo pomocí `Drag&Drop`, nebo klepnutím na pole, čímž vyvoláte systémový výběr souborů.

!>**Upozornění:** najednou můžete vložit pouze jeden soubor.

Po vložení souboru se spustí animace nahrávání souboru a vyvolá se událost`WJ.AdminUpload.addedfile`, na kterou můžete poslouchat.

![](field-uploadFile-loading.png)

Až bude soubor úspěšně nahrán, objeví se v poli potvrzení nahrání a vyvolá se událost `WJ.AdminUpload.success`.

![](field-uploadFile-loaded.png)

Tento soubor se nahraje jako dočasný soubor a hodnota klíče se uloží do pole. `QUfQEadIJ8B0V8t`. S touto hodnotou už následně umíme na BE pracovat pomocí třídy `AdminUploadServlet`.

Chcete-li nahrát jiný soubor, můžete smazat již nahraný, nebo přerušit samotnou akci nahrávání pomocí tlačítka `X`.

## Backend

Příklad použití na BE

```java
String tempKey = "QUfQEadIJ8B0V8t";

//Get path to temp file
String filePath = AdminUploadServlet.getTempFilePath( tempKey );

//Get name of uploaded file
String fileName = AdminUploadServlet.getOriginalFileName( tempKey );

//Remove temp file
boolean wasRemoved = AdminUploadServlet.deleteTempFile( fileKey );
```
