package sk.iway.iwcm.components.file_archiv;

import java.util.List;

/**
 * Title        webjet8
 * Company      Interway s.r.o. (www.interway.sk)
 * Copyright    Interway s.r.o. (c) 2001-2010
 * @author       $Author: prau $
 * @version      $Revision: 1.3 $
 * created      Date: 07.03.2018 12:51:21
 * modified     $Date: 2004/08/16 06:26:11 $
 */


public class ResultArchivBean {
    private boolean success;
    private List<String> errors;
    private FileArchivatorBean fab; //ready to save

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public FileArchivatorBean getFab() {
        return fab;
    }

    public void setFab(FileArchivatorBean fab) {
        this.fab = fab;
    }
}
