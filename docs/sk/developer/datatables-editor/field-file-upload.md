# Pole UPLOAD

Pole umožňuje nahratie súboru.

![](field-uploadFile.png)

## Použitie anotácie

Anotácia sa používa ako ```DataTableColumnType.UPLOAD```.

Kompletný príklad anotácie:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file"
)
private String file = "";
```

## Frontend

Pre nahratie nového súboru môžete súbor vložiť priamo pomocou `Drag&Drop`, alebo kliknutím na pole, čím vyvoláte systémový výber súborov.

!>**Upozornenie:** naraz môžete vložiť iba jeden súbor.

Po vložení súboru sa spustí animácia nahrávania súboru a vyvolá sa udalosť `WJ.AdminUpload.addedfile`, na ktorú môžete počúvať.

![](field-uploadFile-loading.png)

Až bude súbor úspešne nahraný, objaví sa v poli potvrdenie nahratia a vyvolá sa udalosť `WJ.AdminUpload.success`.

![](field-uploadFile-loaded.png)

Tento súbor sa nahrá ako dočasný súbor a hodnota kľúča sa uloží do poľa, napr. `QUfQEadIJ8B0V8t`. S touto hodnotou už následne vieme na BE pracovať pomocou triedy `AdminUploadServlet`.

Ak chcete nahrať iný súbor, môžete zmazať už nahratý, alebo prerušiť samotnú akciu nahrávania pomocou tlačidla `X`.

## Backend

Príklad použitia na BE

```java
String tempKey = "QUfQEadIJ8B0V8t";

//Get path to temp file
String filePath = AdminUploadServlet.getTempFilePath( tempKey );

//Get name of uploaded file
String fileName = AdminUploadServlet.getOriginalFileName( tempKey );

//Remove temp file
boolean wasRemoved = AdminUploadServlet.deleteTempFile( fileKey );
```