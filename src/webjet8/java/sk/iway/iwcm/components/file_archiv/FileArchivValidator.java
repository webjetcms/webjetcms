package sk.iway.iwcm.components.file_archiv;

import javax.servlet.http.HttpServletRequest;

public interface FileArchivValidator {

    //musia splnat vsetky nahravane subory
    boolean validateActionSave(FileArchivatorBean fab, HttpServletRequest request);
    boolean validateAfterUploadReplace(FileArchivatorBean fab, HttpServletRequest request);
    boolean validatePropertiesAfterUpload(FileArchivatorBean fab, HttpServletRequest request);
}

