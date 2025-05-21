# UPLOAD field

The field allows you to upload a file.

![](field-uploadFile.png)

## Use of annotation

Annotation is used as `DataTableColumnType.UPLOAD`.

Full annotation example:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file"
)
private String file = "";
```

## Frontend

To upload a new file, you can insert the file directly using `Drag&Drop`, or click on the box to bring up the system file picker.

!>**Warning:** You can only insert one file at a time.

After the file is inserted, the file upload animation starts and the event is fired `WJ.AdminUpload.addedfile` to which you can listen.

![](field-uploadFile-loading.png)

When the file is successfully uploaded, the upload confirmation box will appear and the event will be fired `WJ.AdminUpload.success`.

![](field-uploadFile-loaded.png)

This file is uploaded as a temporary file and the key value is stored in an array, e.g. `QUfQEadIJ8B0V8t`. We can then work with this value on BE using the class `AdminUploadServlet`.

If you want to record another file, you can delete the already recorded one or interrupt the recording action by pressing the `X`.

## Backend

Example of use on BE

```java
String tempKey = "QUfQEadIJ8B0V8t";

//Get path to temp file
String filePath = AdminUploadServlet.getTempFilePath( tempKey );

//Get name of uploaded file
String fileName = AdminUploadServlet.getOriginalFileName( tempKey );

//Remove temp file
boolean wasRemoved = AdminUploadServlet.deleteTempFile( fileKey );
```
