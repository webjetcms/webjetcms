package sk.iway.iwcm.components.file_archiv;

import javax.servlet.http.HttpServletRequest;

public class FileArchivDefaultValidator implements FileArchivValidator{
    public boolean validateActionSave(FileArchivatorBean fab, HttpServletRequest request)
    {
        return true;
    }
    public boolean validateAfterUploadReplace(FileArchivatorBean fab, HttpServletRequest request)
    {
        return true;
    }
    public boolean validatePropertiesAfterUpload(FileArchivatorBean fab, HttpServletRequest request) { return true; }
}
