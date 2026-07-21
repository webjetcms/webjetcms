# Pole UPLOAD

Pole umožňuje nahrání souboru.

![](field-uploadFile.png)

## Použití anotace

Anotace se používá jako ```DataTableColumnType.UPLOAD```.

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

Pro nahrání nového souboru můžete soubor vložit přímo pomocí `Drag&Drop`, nebo kliknutím na pole, čímž vyvoláte systémový výběr souborů.

Ve výchozím nastavení můžete najednou vložit pouze jeden soubor. Počet souborů lze upravit konfigurací pole, viz část [Konfigurace počtu souborů](#konfigurace-počtu-souborů).

Po vložení souboru se spustí animace nahrávání souboru a vyvolá se událost `WJ.AdminUpload.addedfile`, na kterou můžete poslouchat.

![](field-uploadFile-loading.png)

Až bude soubor úspěšně nahrán, objeví se v poli potvrzení nahrání a vyvolá se událost `WJ.AdminUpload.success`. Při chybě nahrávání se vyvolá událost `WJ.AdminUpload.error`.

![](field-uploadFile-loaded.png)

Tento soubor se nahraje jako dočasný soubor a hodnota klíče se uloží do pole. `QUfQEadIJ8B0V8t`. S touto hodnotou už následně umíme na BE pracovat pomocí třídy `AdminUploadServlet`.

Chcete-li nahrát jiný soubor, můžete smazat již nahraný, nebo přerušit samotnou akci nahrávání pomocí tlačítka `X`.

## Konfigurace počtu souborů

Pole `UPLOAD` je ve výchozím nastavení nastaveno na jeden soubor. Maximální počet souborů se určuje v tomto pořadí:

- `data-dt-field-upload-max-files` - ​​číselný limit, má nejvyšší prioritu.
- `data-dt-field-upload-mode` - ​​hodnota `single` nebo `multiple`.
- `className` - ​​zpětná kompatibilita přes CSS třídy `wjupload-single` nebo `wjupload-multiple`.

Příklad pole s podporou více souborů:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file",
    className = "wjupload-multiple"
)
private String file = "";
```

Příklad s explicitním limitem 3 soubory:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file",
    attr = {
        @DataTableColumnEditorAttr(key = "data-dt-field-upload-mode", value = "multiple"),
        @DataTableColumnEditorAttr(key = "data-dt-field-upload-max-files", value = "3")
    }
)
private String file = "";
```

!>**Upozornění:** hodnota editor pole je stále jeden řetězec s klíčem dočasného souboru. U více souborů se do pole uloží klíč posledního úspěšně nahraného souboru. Pokud potřebujete zpracovat všechny nahrané soubory, poslouchejte událost `WJ.AdminUpload.success` a ukládejte si hodnoty `e.detail.key`.

## Izolace instancí a události

Každé `UPLOAD` pole má vlastní instanci `Dropzone`, vlastní kontejner oznámení a vlastní šablonu pro zobrazení nahrávání. Při opakovaném otevření editoru se předchozí instance zruší, odstraní se přiřazené event listenery a vyčistí se dočasný stav pole. Díky tomu může být na jedné stránce více upload polí nebo samostatných upload zón bez konfliktu nad společnými HTML `id`.

Události `WJ.AdminUpload.addedfile`, `WJ.AdminUpload.success` a `WJ.AdminUpload.error` obsahují v `detail.uploader` referenci na konkrétní instanci uploadu. Při poslechu událostí vždy ověřte, že událost patří vaší instanci:

```javascript
let myUpload = window.AdminUpload({
    element: "#my-upload",
    destinationFolder: "/files/protected/upload/",
    writeDirectlyToDestination: false
});

window.addEventListener("WJ.AdminUpload.success", function(e) {
    if (e.detail == null || e.detail.uploader !== myUpload) return;

    console.log("Dočasný kľúč súboru", e.detail.key);
});
```

Objekt `detail` obsahuje podle typu události zejména:

- `uploader` - ​​instance `AdminUpload` /`Dropzone`, která událost vyvolala.
- `file` - ​​objekt souboru z `Dropzone`.
- `key` - ​​klíč dočasně nahraného souboru, dostupný při úspěšném nahrání.
- `response` - ​​JSON odpověď serveru při úspěšném nahrání.
- `errorMessage` - ​​chybová zpráva při události `WJ.AdminUpload.error`.

## Samostatné použití AdminUpload

`window.AdminUpload` můžete použít i mimo DataTable Editor pole, například pro celostránkové `drag&drop` nahrávání. Podporuje více nezávislých upload zón na jedné stránce. HTML prvky upload rozhraní se hledají relativně k elementu zadanému v `options.element`, nejprve mezi sourozenci a poté uvnitř rodičovského elementu. Proto může mít každá upload zóna vlastní:

- `.upload-wrapper` nebo `#upload-wrapper`,
- `.toast-container-upload` nebo `#toast-container-upload`,
- `.upload-toastr-template` nebo `#upload-toastr-template`.

Příklad:

```html
<div id="my-upload" class="drop-zone-box dropzone"></div>
<div class="upload-wrapper" style="display: none;">
    <div class="toast-container-progress"></div>
    <div id="my-upload-toasts" class="toast-container-upload"></div>
</div>
<div class="upload-toastr-template" style="display: none">
    <i class="ti ti-file"></i>
    <span>{FILE_NAME}</span>
</div>
```

```javascript
let myUpload = window.AdminUpload({
    element: "#my-upload",
    maxFiles: null,
    acceptedFiles: ".pdf,.docx",
    uploadType: "fileArchive",
    destinationFolder: "/files/archiv/",
    writeDirectlyToDestination: true,
    overwriteMode: ""
});
```

Podporované důležité volby:

- `element` - ​​CSS selektor nebo DOM element pro `Dropzone`.
- `maxFiles` - ​​maximální počet souborů, `null` znamená bez limitu.
- `acceptedFiles` - ​​povolené přípony nebo MIME typy.
- `uploadType` - ​​typ nahrávání, například `fileArchive`.
- `destinationFolder` - ​​cílová složka.
- `writeDirectlyToDestination` - ​​pokud je `true`, soubor se zapisuje přímo do cílové složky.
- `overwriteMode` - ​​výchozí akce při konfliktu souborů, například `skip`, `overwrite` nebo `keepboth`.

Pro skrytý `input[type=file]` generovaný knihovnou `Dropzone` se automaticky doplní deterministická CSS třída ve tvaru `dz-hidden-input-<id_elementu>`. Testy a vlastní kód tak mohou cílit konkrétní upload zónu i tehdy, když je na stránce více uploadů najednou.

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
