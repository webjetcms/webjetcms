# UPLOAD field

The field allows you to upload a file.

![](field-uploadFile.png)

## Using annotation

The annotation is used as ```DataTableColumnType.UPLOAD```.

Complete example of annotation:

```java
@DataTableColumn(
    inputType = DataTableColumnType.UPLOAD,
    tab = "basic",
    title = "fbrowse.file"
)
private String file = "";
```

## Frontend

To upload a new file, you can insert the file directly using `Drag&Drop`, or by clicking on the field to bring up the system file selector.

!>**Warning:** You can only upload one file at a time.

After inserting a file, the file upload animation starts and an event `WJ.AdminUpload.addedfile` is raised that you can listen to.

![](field-uploadFile-loading.png)

When the file is successfully uploaded, an upload confirmation will appear in the field and the `WJ.AdminUpload.success` event will be raised.

![](field-uploadFile-loaded.png)

This file is uploaded as a temporary file and the key value is stored in an array, e.g. `QUfQEadIJ8B0V8t`. We can then work with this value in BE using the `AdminUploadServlet` class.

If you want to upload another file, you can delete the already uploaded file or interrupt the upload process using the `X` button.

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